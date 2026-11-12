package com.moim.app.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.core.data.ScheduleRepository
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.usecase.ObserveCurrentUserUseCase
import com.moim.app.feature.chat.domain.BotContext
import com.moim.app.feature.chat.domain.ChatBotRepository
import com.moim.app.feature.chat.domain.model.BotOutput
import com.moim.app.feature.chat.domain.model.BotTurn
import com.moim.app.feature.chat.domain.model.CardStatus
import com.moim.app.feature.chat.domain.model.ChatMessage
import com.moim.app.feature.chat.domain.model.PendingQuestion
import com.moim.app.feature.chat.domain.usecase.RegisterDraftsUseCase
import com.moim.app.feature.chat.domain.usecase.SaveChatReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/** 메인(눈송이 챗봇) 화면 상태 */
data class MainUiState(
    val today: LocalDate,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isBotTyping: Boolean = false,
    val showRegisterError: Boolean = false,
)

/**
 * 메인 화면 ViewModel.
 * 흐름: 사용자 메시지 → 눈송이 응답(공감 + 확인 카드) → [등록하기] → 일정 저장 → 알림 제안
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatBot: ChatBotRepository,
    private val registerDrafts: RegisterDraftsUseCase,
    private val saveChatReminder: SaveChatReminderUseCase,
    scheduleRepository: ScheduleRepository,
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val currentUser = observeCurrentUser()

    // 내 일정 목록 (눈송이가 "오늘 일정 2개야"를 말할 때 사용)
    private val schedules: StateFlow<List<Schedule>> = scheduleRepository.observeSchedules()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _uiState = MutableStateFlow(MainUiState(today = LocalDate.now(clock)))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** 눈송이가 직전에 물어본 질문 (다음 메시지 해석에 사용) */
    private var pending: PendingQuestion? = null

    init {
        viewModelScope.launch {
            runBotTurn { chatBot.greet(context()) }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun onSend() = sendText(_uiState.value.input)

    /** 빠른 답장 칩을 누르면 그 문장을 바로 보낸다 */
    fun onQuickReply(text: String) = sendText(text)

    private fun sendText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || _uiState.value.isBotTyping) return
        appendMessage(ChatMessage.User(newId(), now(), text))
        _uiState.update { it.copy(input = "") }
        viewModelScope.launch {
            runBotTurn { chatBot.reply(text, context()) }
        }
    }

    /** 확인 카드 [등록하기] */
    fun onRegisterCard(cardId: String) {
        val card = findCard(cardId) ?: return
        if (card.status != CardStatus.PENDING) return
        updateCard(cardId, CardStatus.REGISTERING)
        viewModelScope.launch {
            // 기본값이 "그룹공개"면 고른 그룹이 없으니, 아무에게도 내용이 보이지 않게 비공개로 저장한다
            // (카드의 [수정]을 눌러 등록 시트에서 그룹을 고르면 그룹공개로 저장할 수 있다)
            val visibility = (currentUser.value?.defaultVisibility ?: Visibility.FRIENDS)
                .let { if (it == Visibility.GROUP) Visibility.PRIVATE else it }
            registerDrafts(card.drafts, visibility)
                .onSuccess {
                    updateCard(cardId, CardStatus.REGISTERED)
                    runBotTurn { chatBot.onSchedulesRegistered(card.drafts, context()) }
                }
                .onFailure {
                    updateCard(cardId, CardStatus.PENDING)
                    _uiState.update { it.copy(showRegisterError = true) }
                }
        }
    }

    /** 확인 카드 [수정] */
    fun onEditCard(cardId: String) {
        if (findCard(cardId)?.status != CardStatus.PENDING) return
        updateCard(cardId, CardStatus.DISMISSED)
        viewModelScope.launch {
            runBotTurn { chatBot.onEditRequested(context()) }
        }
    }

    fun onRegisterErrorShown() = _uiState.update { it.copy(showRegisterError = false) }

    // ---------- 내부 ----------

    /** 눈송이 응답을 받아 채팅에 붙인다. 받는 동안 "생각 중" 표시 */
    private suspend fun runBotTurn(block: suspend () -> BotTurn) {
        _uiState.update { it.copy(isBotTyping = true) }
        val turn = runCatching { block() }.getOrElse {
            BotTurn(listOf(BotOutput.Text("앗, 잠깐 문제가 생겼어 😢 다시 말해줄래?")))
        }
        pending = turn.pending
        turn.reminder?.let { runCatching { saveChatReminder(it) } }
        turn.outputs.forEach { output ->
            val message = when (output) {
                is BotOutput.Text -> ChatMessage.Bot(newId(), now(), output.text, output.quickReplies)
                is BotOutput.ConfirmCard -> ChatMessage.ScheduleCard(newId(), now(), output.drafts)
            }
            appendMessage(message)
        }
        _uiState.update { it.copy(isBotTyping = false) }
    }

    private fun context() = BotContext(
        user = currentUser.value,
        now = now(),
        pending = pending,
        schedules = schedules.value,
    )

    private fun appendMessage(message: ChatMessage) {
        _uiState.update { it.copy(messages = it.messages + message) }
    }

    private fun findCard(cardId: String): ChatMessage.ScheduleCard? =
        _uiState.value.messages.firstOrNull { it.id == cardId } as? ChatMessage.ScheduleCard

    private fun updateCard(cardId: String, status: CardStatus) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg is ChatMessage.ScheduleCard && msg.id == cardId) msg.copy(status = status) else msg
                },
            )
        }
    }

    private fun now(): LocalDateTime = LocalDateTime.now(clock)

    private fun newId() = UUID.randomUUID().toString()
}
