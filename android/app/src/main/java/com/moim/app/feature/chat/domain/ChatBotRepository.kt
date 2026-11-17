package com.moim.app.feature.chat.domain

import com.moim.app.core.model.Schedule
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.chat.domain.model.BotTurn
import com.moim.app.feature.chat.domain.model.PendingQuestion
import com.moim.app.feature.chat.domain.model.ScheduleDraft
import java.time.LocalDateTime

/** 눈송이가 대답할 때 알아야 하는 것들 */
data class BotContext(
    val user: UserProfile?,
    val now: LocalDateTime,            // 한국 시간 기준 현재 시각
    val pending: PendingQuestion?,     // 직전에 눈송이가 물어본 것
    val schedules: List<Schedule>,     // 내 일정 (오늘 일정 안내 등에 사용)
)

/**
 * 눈송이 챗봇의 약속.
 * 지금은 FakeChatBotRepository(규칙 기반), 나중에 AI 서버(FastAPI + LangGraph + Claude) 구현으로 교체한다.
 */
interface ChatBotRepository {
    /** 채팅 화면을 처음 열었을 때 인사 */
    suspend fun greet(context: BotContext): BotTurn

    /** 사용자 메시지에 대한 응답 */
    suspend fun reply(userText: String, context: BotContext): BotTurn

    /** [등록하기]를 눌러 일정이 저장된 뒤 (알림 제안 등) */
    suspend fun onSchedulesRegistered(drafts: List<ScheduleDraft>, context: BotContext): BotTurn

    /** 카드에서 [수정]을 눌렀을 때 */
    suspend fun onEditRequested(context: BotContext): BotTurn
}
