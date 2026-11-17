package com.moim.app.feature.chat.data

import com.moim.app.core.model.ChatReminder
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.ScheduleType
import com.moim.app.feature.chat.domain.BotContext
import com.moim.app.feature.chat.domain.ChatBotRepository
import com.moim.app.feature.chat.domain.KoreanDateParser
import com.moim.app.feature.chat.domain.ScheduleExtractor
import com.moim.app.feature.chat.domain.model.BotOutput
import com.moim.app.feature.chat.domain.model.BotTurn
import com.moim.app.feature.chat.domain.model.PendingQuestion
import com.moim.app.feature.chat.domain.model.QuickReply
import com.moim.app.feature.chat.domain.model.ScheduleDraft
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 서버 없이 동작하는 규칙 기반 눈송이.
 * 말투 규칙: 다정한 반말, 짧은 문장, 💙 가끔 / 순서: 공감 → 일정 정리 → 확인 질문·알림 제안
 * AI 서버 주소가 없을 때 쓰이고, AI 서버가 있어도 인사·알림 제안·서버 실패 시 대신 답하는 데 쓰인다.
 */
@Singleton
class FakeChatBotRepository @Inject constructor() : ChatBotRepository {

    override suspend fun greet(context: BotContext): BotTurn {
        val name = context.user?.name?.let(::givenName) ?: "친구"
        val today = context.now.toLocalDate()
        val todayCount = context.schedules.count { it.startAt.toLocalDate() == today }
        val text = if (todayCount > 0) {
            "안녕 ${name}${vocative(name)}! 오늘 일정은 ${todayCount}개야 ☀️"
        } else {
            "안녕 ${name}${vocative(name)}! 오늘은 일정이 없어. 편하게 쉬어 ☀️"
        }
        return BotTurn(
            outputs = listOf(
                BotOutput.Text(
                    text = text,
                    quickReplies = listOf(
                        QuickReply("오늘 일정 알려줘"),
                        QuickReply("내일 3시에 팀플 회의 있어"),
                        QuickReply("금요일까지 과제 있어"),
                    ),
                ),
            ),
        )
    }

    override suspend fun reply(userText: String, context: BotContext): BotTurn {
        delay(THINKING_DELAY_MS) // 생각하는 척
        val today = context.now.toLocalDate()

        // 1) 직전 질문에 대한 대답인지 먼저 확인
        when (val pending = context.pending) {
            is PendingQuestion.MissingInfo -> if (!ScheduleExtractor.hasScheduleKeyword(userText)) {
                return fillMissingInfo(pending.drafts, userText, today)
            }
            is PendingQuestion.ReminderOffer -> answerReminderOffer(pending, userText)?.let { return it }
            null -> Unit
        }

        // 2) 일정 조회 ("오늘 일정 알려줘", "이번 주 일정 뭐야?")
        if (userText.contains("일정") && !ScheduleExtractor.hasScheduleKeyword(userText)) {
            return listSchedules(userText, context)
        }

        // 3) 새 일정 뽑기
        val drafts = ScheduleExtractor.extract(userText, today)
        if (drafts.isEmpty()) {
            return BotTurn(
                listOf(
                    BotOutput.Text(
                        "음, 일정이 있으면 날짜랑 같이 말해줘! 💙\n예) \"내일 3시에 팀플 회의\", \"금요일까지 과제\"",
                    ),
                ),
            )
        }
        return confirmOrAsk(drafts, empathy(drafts))
    }

    override suspend fun onSchedulesRegistered(drafts: List<ScheduleDraft>, context: BotContext): BotTurn {
        delay(SHORT_DELAY_MS)
        val today = context.now.toLocalDate()
        val planningType = context.user?.planningType ?: PlanningType.THREE_DAY
        val prefs = context.user?.notificationPrefs ?: planningType.defaultPrefs

        // 가장 가까운 시험 → 과제 → 그 외 순으로 알림을 제안
        val target = drafts.filter { it.date != null }.sortedWith(
            compareBy<ScheduleDraft> { typePriority(it.type) }.thenBy { it.date },
        ).firstOrNull() ?: return BotTurn(listOf(BotOutput.Text("캘린더에 넣어뒀어! 💙")))

        val eventDate = requireNotNull(target.date)
        val daysBefore = when (target.type) {
            ScheduleType.EXAM -> prefs.examDaysBefore
            ScheduleType.ASSIGNMENT -> prefs.assignmentDaysBefore
            else -> prefs.appointmentDaysBefore
        }
        val remindDate = eventDate.minusDays(daysBefore.toLong())
        if (!remindDate.isAfter(today)) {
            return BotTurn(listOf(BotOutput.Text("캘린더에 넣어뒀어! 얼마 안 남았으니까 오늘부터 같이 준비하자 💙")))
        }

        val what = if (target.type == ScheduleType.EXAM) "공부" else "준비"
        val question = when (planningType) {
            PlanningType.EARLY -> "평소에 일주일 전부터 미리 준비하는 편이지? ${md(remindDate)}부터 $what 알림 보내줄까?"
            PlanningType.THREE_DAY -> "평소에 ${typeWord(target.type)} 3일 전부터 ${what}하는 편이지? ${md(remindDate)}부터 $what 알림 보내줄까?"
            PlanningType.CRAMMING -> "하루 전에 확 집중하는 스타일이지? ${md(remindDate)}에 알림 보내줄까?"
            PlanningType.PLANNER -> "네가 정한 대로 ${daysBefore}일 전인 ${md(remindDate)}에 알림 보내줄까?"
        }
        return BotTurn(
            outputs = listOf(
                BotOutput.Text(
                    text = question,
                    quickReplies = listOf(QuickReply("좋아! ⏰", "좋아!"), QuickReply("하루 전만"), QuickReply("괜찮아")),
                ),
            ),
            pending = PendingQuestion.ReminderOffer(remindDate = remindDate, eventDate = eventDate, title = target.title),
        )
    }

    override suspend fun onEditRequested(context: BotContext): BotTurn {
        delay(SHORT_DELAY_MS)
        return BotTurn(listOf(BotOutput.Text("알겠어! 바뀐 내용으로 다시 말해줘 💙\n예) \"시험 11시로 바꿔서 다음 주 월요일\"")))
    }

    // ---------- 내부 규칙 ----------

    /** 날짜·시간이 다 있으면 확인 카드, 빠졌으면 되묻기 (추측하지 않는다) */
    private fun confirmOrAsk(drafts: List<ScheduleDraft>, lead: String?): BotTurn {
        val missing = drafts.firstOrNull { !it.isComplete }
        if (missing == null) {
            val outputs = buildList {
                add(BotOutput.Text(listOfNotNull(lead, "이렇게 등록해 둘게").joinToString(" ")))
                add(BotOutput.ConfirmCard(drafts))
            }
            return BotTurn(outputs)
        }
        val question = if (missing.date == null) {
            "${missing.title} 언제야? 날짜를 알려줘!"
        } else {
            "${missing.title} 몇 시야?"
        }
        val chips = if (missing.date == null) {
            listOf(QuickReply("내일"), QuickReply("이번 주 금요일"), QuickReply("다음 주 월요일"))
        } else {
            listOf(QuickReply("오전 10시"), QuickReply("오후 2시"), QuickReply("오후 7시"))
        }
        return BotTurn(
            outputs = listOf(BotOutput.Text(listOfNotNull(lead, question).joinToString(" "), chips)),
            pending = PendingQuestion.MissingInfo(drafts),
        )
    }

    /** "몇 시야?"에 대한 대답으로 빠진 날짜/시간을 채운다 */
    private fun fillMissingInfo(drafts: List<ScheduleDraft>, userText: String, today: LocalDate): BotTurn {
        val date = KoreanDateParser.findDate(userText, today)?.value
        val time = KoreanDateParser.findTime(userText)?.value
        if (date == null && time == null) {
            return BotTurn(
                listOf(BotOutput.Text("미안, 잘 못 알아들었어 😢 \"내일 오후 2시\"처럼 말해줄래?")),
                pending = PendingQuestion.MissingInfo(drafts),
            )
        }
        // 빠진 칸만 채운다 (이미 있는 값은 그대로)
        val filled = drafts.map { draft ->
            val newDate = draft.date ?: date
            val newTime = draft.time
                ?: time
                ?: if (draft.isDeadline && newDate != null) ScheduleExtractor.DEFAULT_DEADLINE else null
            draft.copy(date = newDate, time = newTime)
        }
        return confirmOrAsk(filled, lead = "좋아!")
    }

    /** "알림 보내줄까?"에 대한 대답. 받아들이면 BotTurn.reminder로 알림 예약을 요청한다 */
    private fun answerReminderOffer(offer: PendingQuestion.ReminderOffer, userText: String): BotTurn? {
        val title = offer.title.ifBlank { "일정" }
        val dayBefore = offer.eventDate.minusDays(1)
        return when {
            userText.contains("하루") -> BotTurn(
                listOf(BotOutput.Text("알겠어! ${md(dayBefore)} 오전 9시에만 알려줄게 💙")),
                reminder = ChatReminder(title = title, eventDate = offer.eventDate, from = dayBefore),
            )
            userText.contains("괜찮") || userText.contains("아니") || userText.contains("됐") ->
                BotTurn(listOf(BotOutput.Text("알겠어! 필요하면 언제든 말해줘 💙")))
            userText.contains("좋아") || userText.contains("응") || userText.contains("그래") -> BotTurn(
                listOf(BotOutput.Text("좋아! ${md(offer.remindDate)}부터 매일 오전 9시에 챙겨줄게 💙")),
                reminder = ChatReminder(title = title, eventDate = offer.eventDate, from = offer.remindDate),
            )
            else -> null // 다른 이야기면 일반 대화로 처리
        }
    }

    private fun listSchedules(userText: String, context: BotContext): BotTurn {
        val today = context.now.toLocalDate()
        val (label, range) = when {
            userText.contains("내일") -> "내일" to (today.plusDays(1)..today.plusDays(1))
            userText.contains("주") -> "이번 주" to (today..today.plusDays(6))
            else -> "오늘" to (today..today)
        }
        val items = context.schedules.filter { it.startAt.toLocalDate() in range }
        if (items.isEmpty()) return BotTurn(listOf(BotOutput.Text("$label 일정은 없어! 여유롭다 ☀️")))
        val timeFormat = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
        val lines = items.joinToString("\n") { s ->
            val day = if (range.start == range.endInclusive) "" else "${md(s.startAt.toLocalDate())} "
            "• $day${s.startAt.format(timeFormat)} ${s.title}"
        }
        return BotTurn(listOf(BotOutput.Text("$label 일정은 ${items.size}개야!\n$lines")))
    }

    /** 공감 한마디 */
    private fun empathy(drafts: List<ScheduleDraft>): String = when {
        drafts.size >= 2 && drafts.any { it.type == ScheduleType.EXAM } -> "헉 한꺼번에 몰렸구나, 힘내자! 💙"
        drafts.size >= 2 -> "할 게 많구나! 하나씩 챙기자 💙"
        drafts.first().type == ScheduleType.EXAM -> "시험이구나! 같이 준비해 보자 💙"
        drafts.first().type == ScheduleType.ASSIGNMENT -> "과제 생겼구나, 미리 챙기자!"
        drafts.first().type == ScheduleType.MEETUP -> "오 모임이구나!"
        drafts.first().type == ScheduleType.APPOINTMENT -> "오 약속 생겼구나! 💙"
        else -> "알겠어!"
    }

    private fun typePriority(type: ScheduleType) = when (type) {
        ScheduleType.EXAM -> 0
        ScheduleType.ASSIGNMENT -> 1
        else -> 2
    }

    private fun typeWord(type: ScheduleType) = if (type == ScheduleType.EXAM) "시험" else "마감"

    /** 11/6(금) */
    private fun md(date: LocalDate): String =
        "${date.monthValue}/${date.dayOfMonth}(${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"

    private fun givenName(full: String): String = if (full.length >= 3) full.substring(1) else full

    private fun vocative(name: String): String {
        val last = name.lastOrNull() ?: return "야"
        if (last !in '가'..'힣') return "야"
        return if ((last.code - '가'.code) % 28 != 0) "아" else "야"
    }

    private companion object {
        const val THINKING_DELAY_MS = 700L
        const val SHORT_DELAY_MS = 400L
    }
}
