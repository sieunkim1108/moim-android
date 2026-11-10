package com.moim.app.feature.chat.domain.model

import com.moim.app.core.model.ChatReminder
import com.moim.app.core.model.ScheduleType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 눈송이가 대화에서 뽑아낸 "아직 저장 전" 일정.
 * 규칙: AI(눈송이)가 만든 일정은 사용자가 [등록하기]를 눌러야 저장된다.
 */
data class ScheduleDraft(
    val id: String,
    val title: String,
    val type: ScheduleType,
    val date: LocalDate?,
    val time: LocalTime?,
    val isDeadline: Boolean = false,
) {
    val isComplete: Boolean get() = date != null && time != null
}

/** 일정 확인 카드 상태 */
enum class CardStatus { PENDING, REGISTERING, REGISTERED, DISMISSED }

/** 말풍선 아래 빠른 답장 버튼. 누르면 text가 내 메시지로 보내진다. */
data class QuickReply(val label: String, val text: String = label)

/** 채팅 화면에 쌓이는 항목 하나 */
sealed interface ChatMessage {
    val id: String
    val sentAt: LocalDateTime

    data class User(
        override val id: String,
        override val sentAt: LocalDateTime,
        val text: String,
    ) : ChatMessage

    data class Bot(
        override val id: String,
        override val sentAt: LocalDateTime,
        val text: String,
        val quickReplies: List<QuickReply> = emptyList(),
    ) : ChatMessage

    data class ScheduleCard(
        override val id: String,
        override val sentAt: LocalDateTime,
        val drafts: List<ScheduleDraft>,
        val status: CardStatus = CardStatus.PENDING,
    ) : ChatMessage
}

/** 눈송이가 한 번에 내놓는 응답 조각 */
sealed interface BotOutput {
    data class Text(val text: String, val quickReplies: List<QuickReply> = emptyList()) : BotOutput
    data class ConfirmCard(val drafts: List<ScheduleDraft>) : BotOutput
}

/** 눈송이가 대답을 기다리는 질문 (다음 메시지를 어떻게 해석할지 정함) */
sealed interface PendingQuestion {
    /** 날짜나 시간이 빠진 일정을 채우는 중 */
    data class MissingInfo(val drafts: List<ScheduleDraft>) : PendingQuestion

    /** "공부 알림 보내줄까?"에 대한 답을 기다리는 중 */
    data class ReminderOffer(
        val remindDate: LocalDate,
        val eventDate: LocalDate,
        val title: String = "",
    ) : PendingQuestion
}

/** 눈송이의 한 턴: 보낼 응답들 + 다음에 기다릴 질문 + (있으면) 새로 예약할 준비 알림 */
data class BotTurn(
    val outputs: List<BotOutput>,
    val pending: PendingQuestion? = null,
    val reminder: ChatReminder? = null,
)
