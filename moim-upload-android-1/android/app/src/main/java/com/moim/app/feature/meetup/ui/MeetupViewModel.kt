package com.moim.app.feature.meetup.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.moim.app.core.model.Availability
import com.moim.app.feature.calendar.domain.CalendarMath
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.usecase.ObserveRoomUseCase
import com.moim.app.feature.meetup.domain.MeetupScheduler
import com.moim.app.feature.meetup.domain.model.MeetupMember
import com.moim.app.feature.meetup.domain.model.TimeRange
import com.moim.app.feature.meetup.domain.usecase.ConfirmMeetupUseCase
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class MeetupMessage { CONFIRM_FAILED }

data class MeetupUiState(
    val today: LocalDate,
    val room: ChatRoom? = null,
    val members: List<MeetupMember> = emptyList(),
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val availability: Map<LocalDate, Availability> = emptyMap(),
    val recommendations: List<ExplainedCandidate>? = null, // null = 계산 중
    val selectedRecommendation: Int = 0,
    val durationMinutes: Long = RecommendMeetupUseCase.DEFAULT_DURATION_MINUTES, // 희망 소요시간 (슬라이더)
    val isConfirming: Boolean = false,
    val confirmed: Boolean = false, // true면 화면이 채팅방으로 돌아간다
    val message: MeetupMessage? = null,
) {
    /** 선택한 날 전원이 비는 시간대 */
    val commonFree: List<TimeRange> get() = MeetupScheduler.commonFreeRanges(members, selectedDate)

    /** 선택한 날 희망 소요시간 기준 최대 가능 인원 */
    val bestCountOnSelected: Int
        get() = MeetupScheduler.bestSlotOn(members, selectedDate, durationMinutes)?.availableCount ?: 0

    /** "추천" 소요시간 = 선택한 날 전원 가능 시간대 중 가장 긴 것 (30분 단위, 30분~4시간). 없으면 null */
    val recommendedMinutes: Long? get() = MeetupScheduler.recommendedDuration(commonFree)

    val availabilityCounts: Map<Availability, Int> get() = availability.values.groupingBy { it }.eachCount()
}

private data class MeetupLocal(
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val dateChosenByUser: Boolean = false,
    val recommendations: List<ExplainedCandidate>? = null,
    val selectedRecommendation: Int = 0,
    val durationMinutes: Long = RecommendMeetupUseCase.DEFAULT_DURATION_MINUTES,
    val isConfirming: Boolean = false,
    val confirmed: Boolean = false,
    val message: MeetupMessage? = null,
)

@OptIn(ExperimentalCoroutinesApi::class) // mapLatest
@HiltViewModel
class MeetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeRoom: ObserveRoomUseCase,
    observeMembers: ObserveMeetupMembersUseCase,
    private val recommend: RecommendMeetupUseCase,
    private val confirmMeetup: ConfirmMeetupUseCase,
    clock: Clock,
) : ViewModel() {

    private val roomId = savedStateHandle.toRoute<MoimRoute.Meetup>().roomId
    private val today = LocalDate.now(clock)
    private val local = MutableStateFlow(MeetupLocal(YearMonth.from(today), today))
    private val members = observeMembers(roomId).distinctUntilChanged()

    val uiState: StateFlow<MeetupUiState> = combine(observeRoom(roomId), members, local) { room, m, l ->
        MeetupUiState(
            today = today,
            room = room,
            members = m,
            visibleMonth = l.visibleMonth,
            selectedDate = l.selectedDate,
            availability = CalendarMath.monthGrid(l.visibleMonth).associateWith { date ->
                MeetupScheduler.dayAvailability(m, date, l.durationMinutes, today)
            },
            recommendations = l.recommendations,
            selectedRecommendation = l.selectedRecommendation,
            durationMinutes = l.durationMinutes,
            isConfirming = l.isConfirming,
            confirmed = l.confirmed,
            message = l.message,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MeetupUiState(today = today, visibleMonth = YearMonth.from(today), selectedDate = today),
    )

    init {
        // 추천 시간: 알고리즘이 고르고 AI가 설명. 처음엔 1순위 날짜를 달력에서 선택해 둔다.
        // 멤버 일정이나 희망 소요시간이 바뀌면 다시 계산한다 (그보다 짧은 후보는 자연히 빠짐)
        val duration = local.map { it.durationMinutes }.distinctUntilChanged()
        combine(members, duration) { m, d -> m to d }.mapLatest { (m, d) ->
            recommend(m, durationMinutes = d)
        }.onEach { result ->
            local.update { l ->
                val first = result.firstOrNull()?.candidate?.date
                if (!l.dateChosenByUser && first != null) {
                    l.copy(recommendations = result, selectedRecommendation = 0, selectedDate = first, visibleMonth = YearMonth.from(first))
                } else {
                    l.copy(recommendations = result, selectedRecommendation = 0)
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onDateSelect(date: LocalDate) = local.update {
        it.copy(selectedDate = date, visibleMonth = YearMonth.from(date), dateChosenByUser = true)
    }

    /** 희망 소요시간 슬라이더 (30분 단위) */
    fun onDurationChange(minutes: Long) = local.update {
        val snapped = MeetupScheduler.snapDuration(minutes)
        if (snapped == it.durationMinutes) it else it.copy(durationMinutes = snapped, recommendations = null)
    }

    fun onPrevMonth() = local.update { it.copy(visibleMonth = it.visibleMonth.minusMonths(1)) }

    fun onNextMonth() = local.update { it.copy(visibleMonth = it.visibleMonth.plusMonths(1)) }

    /** 추천을 고르면 달력·타임라인도 그 날짜로 이동 */
    fun onRecommendationSelect(index: Int) = local.update { l ->
        val date = l.recommendations?.getOrNull(index)?.candidate?.date ?: return@update l
        l.copy(selectedRecommendation = index, selectedDate = date, visibleMonth = YearMonth.from(date), dateChosenByUser = true)
    }

    /** [이 시간으로 모임 만들기] — 사용자가 눌렀을 때만 캘린더에 저장 */
    fun onConfirm() {
        val state = uiState.value
        val room = state.room ?: return
        val picked = state.recommendations?.getOrNull(state.selectedRecommendation) ?: return
        if (state.isConfirming) return
        local.update { it.copy(isConfirming = true) }
        viewModelScope.launch {
            confirmMeetup(room.id, room.title, picked.candidate, picked.reason)
                .onSuccess { local.update { it.copy(isConfirming = false, confirmed = true) } }
                .onFailure { local.update { it.copy(isConfirming = false, message = MeetupMessage.CONFIRM_FAILED) } }
        }
    }

    fun onNavigatedBack() = local.update { it.copy(confirmed = false) }

    fun onMessageShown() = local.update { it.copy(message = null) }
}
