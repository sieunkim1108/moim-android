package com.moim.app.feature.meetup.data

import com.moim.app.core.ai.AiServerClient
import com.moim.app.core.supabase.toDbTime
import com.moim.app.feature.meetup.domain.MeetupExplainer
import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.model.MeetupMember
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * AI 서버(/meetup/explain)의 Claude가 추천 이유를 쓴다.
 * 보내는 것: 후보 시간, 가능 인원, 안 되는 사람 이름. 일정 제목은 보내지 않는다.
 * 점수·순서는 바꾸지 않는다. 서버가 실패하거나 빠뜨린 후보는 규칙 문장으로 채운다.
 */
class AiMeetupExplainer @Inject constructor(
    private val server: AiServerClient,
    private val fallback: FakeMeetupExplainer,
) : MeetupExplainer {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun explain(candidates: List<MeetupCandidate>, members: List<MeetupMember>): List<String> {
        if (candidates.isEmpty()) return emptyList()
        val local = fallback.explain(candidates, members)
        val body = ExplainBody(
            title = "모임",
            candidates = candidates.mapIndexed { i, c ->
                val missing = members.filter { it.id !in c.availableMemberIds }.map { it.name }
                CandidateBody(
                    id = i.toString(),
                    startAt = c.date.atTime(c.start).toDbTime(),
                    endAt = c.date.atTime(c.end).toDbTime(),
                    score = c.availableCount.toDouble() / c.totalMembers.coerceAtLeast(1),
                    availableCount = c.availableCount,
                    memberCount = c.totalMembers,
                    notes = if (missing.isEmpty()) listOf("모두 가능") else listOf("안 되는 사람: ${missing.joinToString()}"),
                )
            },
        )
        val reasons = runCatching {
            server.postJson("/meetup/explain", json.encodeToString(body))
                ?.let { json.decodeFromString<ExplainResponse>(it).reasons.associate { r -> r.id to r.reason } }
        }.getOrNull().orEmpty()
        return candidates.indices.map { i -> reasons[i.toString()]?.takeIf { it.isNotBlank() } ?: local[i] }
    }
}

@Serializable
private data class ExplainBody(val title: String, val candidates: List<CandidateBody>)

@Serializable
private data class CandidateBody(
    val id: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    val score: Double,
    @SerialName("available_count") val availableCount: Int,
    @SerialName("member_count") val memberCount: Int,
    val notes: List<String>,
)

@Serializable
private data class ExplainResponse(val reasons: List<ReasonRow>)

@Serializable
private data class ReasonRow(val id: String, val reason: String)
