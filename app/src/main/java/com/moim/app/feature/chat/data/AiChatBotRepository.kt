package com.moim.app.feature.chat.data

import com.moim.app.core.ai.AiServerClient
import com.moim.app.feature.chat.domain.BotContext
import com.moim.app.feature.chat.domain.ChatBotRepository
import com.moim.app.feature.chat.domain.model.BotOutput
import com.moim.app.feature.chat.domain.model.BotTurn
import com.moim.app.feature.chat.domain.model.PendingQuestion
import com.moim.app.feature.chat.domain.model.ScheduleDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 서버(FastAPI + LangGraph + Claude)에 묻는 눈송이.
 * - 일정 등록·일반 대화: AI 서버 /chat
 * - 첫 인사, 등록 후 알림 제안, 알림 제안에 대한 대답: 기기 안 규칙(FakeChatBotRepository)으로 바로 처리
 * - AI 서버가 실패하면 규칙 기반 눈송이가 대신 답한다 (앱이 멈추지 않게)
 */
@Singleton
class AiChatBotRepository @Inject constructor(
    private val server: AiServerClient,
    private val local: FakeChatBotRepository,
) : ChatBotRepository {
    /** 최근 대화 (서버가 "3시야" 같은 짧은 대답의 앞뒤를 알 수 있게) */
    private val history = ArrayDeque<ChatTurnBody>()
    private val historyLock = Mutex()

    override suspend fun greet(context: BotContext): BotTurn = local.greet(context)

    override suspend fun onSchedulesRegistered(drafts: List<ScheduleDraft>, context: BotContext): BotTurn =
        local.onSchedulesRegistered(drafts, context)

    override suspend fun onEditRequested(context: BotContext): BotTurn = local.onEditRequested(context)

    override suspend fun reply(userText: String, context: BotContext): BotTurn {
        // "알림 보내줄까?"에 대한 대답은 기기 안에서 처리
        if (context.pending is PendingQuestion.ReminderOffer) {
            return local.reply(userText, context)
        }
        val turn = try {
            askServer(userText, context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return local.reply(userText, context)

        remember(ChatTurnBody("user", userText))
        turn.outputs.filterIsInstance<BotOutput.Text>().forEach { remember(ChatTurnBody("bot", it.text)) }
        return turn
    }

    private suspend fun askServer(userText: String, context: BotContext): BotTurn? {
        val user = context.user ?: return null
        val body = ChatRequestBody(
            message = userText,
            user = UserContextBody(
                name = user.name,
                planningType = user.planningType.dbValue,
                notificationPrefs = PrefsBody(
                    exam = user.notificationPrefs.examDaysBefore,
                    assignment = user.notificationPrefs.assignmentDaysBefore,
                    appointment = user.notificationPrefs.appointmentDaysBefore,
                ),
                today = context.now.toLocalDate().toString(),
            ),
            history = historyLock.withLock { history.toList() },
        )
        val lines = server.postSse("/chat", AiServerProtocol.json.encodeToString(body)) ?: return null
        return AiServerProtocol.toBotTurn(AiServerProtocol.parseSse(lines.asSequence()))
    }

    private suspend fun remember(turn: ChatTurnBody) = historyLock.withLock {
        history.addLast(turn)
        while (history.size > MAX_HISTORY) history.removeFirst()
    }

    private companion object {
        const val MAX_HISTORY = 10
    }
}
