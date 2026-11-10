package com.moim.app.feature.dm.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMessage
import com.moim.app.feature.dm.domain.usecase.MarkRoomReadUseCase
import com.moim.app.feature.dm.domain.usecase.ObserveRoomUseCase
import com.moim.app.feature.dm.domain.usecase.SendRoomMessageUseCase
import com.moim.app.feature.meetup.domain.usecase.ExplainedCandidate
import com.moim.app.feature.meetup.domain.usecase.ObserveMeetupMembersUseCase
import com.moim.app.feature.meetup.domain.usecase.RecommendMeetupUseCase
import com.moim.app.navigation.MoimRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class DmRoomUiState(
    val today: LocalDate,
    val room: ChatRoom? = null,
    val messages: List<RoomMessage> = emptyList(),
    val input: String = "",
    val suggestions: List<ExplainedCandidate>? = null, // null = 계산 중
    val selectedSuggestion: Int = 0,
)

private data class RoomLocal(
    val input: String = "",
    val suggestions: List<ExplainedCandidate>? = null,
    val selectedSuggestion: Int = 0,
)

@HiltViewModel
class DmRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeRoom: ObserveRoomUseCase,
    observeMembers: ObserveMeetupMembersUseCase,
    private val sendMessage: SendRoomMessageUseCase,
    private val recommend: RecommendMeetupUseCase,
    markRead: MarkRoomReadUseCase,
    clock: Clock,
) : ViewModel() {

    val roomId: String = savedStateHandle.toRoute<MoimRoute.DmRoom>().roomId
    private val today = LocalDate.now(clock)
    private val local = MutableStateFlow(RoomLocal())

    val uiState: StateFlow<DmRoomUiState> = combine(
        observeRoom(roomId),
        observeRoom.messages(roomId),
        local,
    ) { room, messages, l ->
        DmRoomUiState(
            today = today,
            room = room,
            messages = messages,
            input = l.input,
            suggestions = l.suggestions,
            selectedSuggestion = l.selectedSuggestion,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DmRoomUiState(today = today))

    init {
        viewModelScope.launch { markRead(roomId) }
        // 참여자 일정이 바뀌면 추천 시간을 다시 계산 (알고리즘 → AI 설명)
        observeMembers(roomId)
            .distinctUntilChanged()
            .onEach { members ->
                val result = recommend(members)
                local.update { it.copy(suggestions = result, selectedSuggestion = 0) }
            }
            .launchIn(viewModelScope)
    }

    fun onInputChange(text: String) = local.update { it.copy(input = text) }

    fun onSend() {
        val room = uiState.value.room ?: return
        val text = local.value.input
        if (text.isBlank()) return
        local.update { it.copy(input = "") }
        viewModelScope.launch { sendMessage(room, text) }
    }

    fun onSuggestionSelect(index: Int) = local.update { it.copy(selectedSuggestion = index) }
}
