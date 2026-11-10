package com.moim.app.feature.chat.data

import com.moim.app.core.model.ScheduleType
import com.moim.app.feature.chat.domain.ScheduleExtractor
import com.moim.app.feature.chat.domain.model.BotOutput
import com.moim.app.feature.chat.domain.model.BotTurn
import com.moim.app.feature.chat.domain.model.QuickReply
import com.moim.app.feature.chat.domain.model.ScheduleDraft
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/*
 * AI 서버(/chat)와 주고받는 모양. 서버 코드는 ai-server/app/schemas.py.
 * 응답은 SSE(Server-Sent Events): "event: 이름" + "data: JSON" 줄이 이어서 온다.
 * 안드로이드에 의존하지 않는 순수 Kotlin이라 단위 테스트로 확인한다 (AiServerProtocolTest).
 */

@Serializable
internal data class ChatRequestBody(
    val message: String,
    val user: UserContextBody,
    val history: List<ChatTurnBody>,
)

@Serializable
internal data class UserContextBody(
    val name: String,
    @SerialName("planning_type") val planningType: String,
    @SerialName("notification_prefs") val notificationPrefs: PrefsBody,
    val today: String,
)

@Serializable
internal data class PrefsBody(
    @SerialName("exam_days_before") val exam: Int,
    @SerialName("assignment_days_before") val assignment: Int,
    @SerialName("appointment_days_before") val appointment: Int,
)

@Serializable
internal data class ChatTurnBody(val role: String, val content: String)

@Serializable
internal data class CandidateBody(
    val title: String,
    val type: String,
    val date: String,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("all_day") val allDay: Boolean = false,
    @SerialName("is_deadline") val isDeadline: Boolean = false,
)

/** SSE 이벤트 하나 */
internal data class SseEvent(val name: String, val data: JsonObject)

internal object AiServerProtocol {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** SSE 줄들 → 이벤트 목록. 빈 줄이 이벤트 끝. */
    fun parseSse(lines: Sequence<String>): List<SseEvent> {
        val events = mutableListOf<SseEvent>()
        var name: String? = null
        val data = StringBuilder()
        fun flush() {
            if (name != null && data.isNotEmpty()) {
                runCatching { json.parseToJsonElement(data.toString()) as JsonObject }
                    .onSuccess { events += SseEvent(name!!, it) }
            }
            name = null
            data.clear()
        }
        for (raw in lines) {
            val line = raw.trimEnd('\r')
            when {
                line.isEmpty() -> flush()
                line.startsWith(":") -> Unit // 주석(keep-alive)
                line.startsWith("event:") -> name = line.removePrefix("event:").trim()
                line.startsWith("data:") -> {
                    if (data.isNotEmpty()) data.append('\n')
                    data.append(line.removePrefix("data:").trimStart())
                }
            }
        }
        flush()
        return events
    }

    /**
     * 이벤트 → 눈송이 한 턴.
     * card는 확인 카드로만 보여 준다 (저장은 사용자가 [등록하기]를 눌렀을 때 앱이 한다).
     * 서버가 실패(error)하거나 아무 말도 없으면 null → 호출한 쪽이 규칙 기반 눈송이로 대신 답한다.
     */
    fun toBotTurn(events: List<SseEvent>): BotTurn? {
        if (events.any { it.name == "error" }) return null
        val outputs = mutableListOf<BotOutput>()
        val streamed = StringBuilder()
        for (event in events) {
            when (event.name) {
                "delta" -> streamed.append(event.text())
                "reply" -> event.text().takeIf { it.isNotBlank() }?.let { outputs += BotOutput.Text(it) }
                "clarify" -> outputs += BotOutput.Text(
                    event.data["question"]?.jsonPrimitive?.content.orEmpty(),
                    listOf(QuickReply("오전 10시"), QuickReply("오후 2시"), QuickReply("오후 7시")),
                )
                "card" -> {
                    val drafts = event.data["schedules"]?.let {
                        json.decodeFromJsonElement<List<CandidateBody>>(it)
                    }.orEmpty().map { it.toDraft() }
                    if (drafts.isNotEmpty()) outputs += BotOutput.ConfirmCard(drafts)
                }
            }
        }
        if (streamed.isNotBlank()) outputs.add(0, BotOutput.Text(streamed.toString().trim()))
        return if (outputs.isEmpty()) null else BotTurn(outputs)
    }

    private fun SseEvent.text() = data["text"]?.jsonPrimitive?.content.orEmpty()

    private fun CandidateBody.toDraft() = ScheduleDraft(
        id = UUID.randomUUID().toString(),
        title = title,
        type = ScheduleType.entries.firstOrNull { it.dbValue == type } ?: ScheduleType.ETC,
        date = runCatching { LocalDate.parse(date) }.getOrNull(),
        time = startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            ?: when {
                isDeadline -> ScheduleExtractor.DEFAULT_DEADLINE // 마감은 시간이 없으면 23:59
                allDay -> LocalTime.MIDNIGHT
                else -> null
            },
        isDeadline = isDeadline,
    )
}
