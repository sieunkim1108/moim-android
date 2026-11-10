package com.moim.app.feature.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.core.model.Friend
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.ShareGroup
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.usecase.ObserveCurrentUserUseCase
import com.moim.app.feature.calendar.domain.usecase.AddScheduleUseCase
import com.moim.app.feature.calendar.domain.usecase.DeleteScheduleUseCase
import com.moim.app.feature.calendar.domain.usecase.NewSchedule
import com.moim.app.feature.calendar.domain.usecase.ObserveFriendsUseCase
import com.moim.app.feature.calendar.domain.usecase.ObserveSchedulesByDateUseCase
import com.moim.app.feature.calendar.domain.usecase.ObserveShareGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

/** 새 일정 등록 바텀시트 입력값 */
data class AddScheduleForm(
    val title: String = "",
    val type: ScheduleType = ScheduleType.APPOINTMENT,
    val date: LocalDate,
    val time: LocalTime = LocalTime.of(14, 0),
    val location: String = "",
    val reminderDays: Set<Int> = setOf(1),
    val visibility: Visibility = Visibility.FRIENDS,
    val sharedRoomIds: Set<String> = emptySet(), // 그룹공개일 때 고른 단체방
    val showTitleError: Boolean = false,
    val showGroupError: Boolean = false,
    val isSaving: Boolean = false,
)

/** 캘린더 화면 상태 */
data class CalendarUiState(
    val today: LocalDate,
    val selectedDate: LocalDate,
    val visibleMonth: YearMonth,
    val myName: String = "",
    val friends: List<Friend> = emptyList(),
    val shareGroups: List<ShareGroup> = emptyList(), // 그룹공개 때 고를 단체방
    val selectedFriendId: String? = null, // null이면 내 캘린더
    val isEditMode: Boolean = false,
    val schedulesByDate: Map<LocalDate, List<Schedule>> = emptyMap(),
    val form: AddScheduleForm? = null, // null이면 바텀시트 닫힘
    val message: CalendarMessage? = null,
) {
    val selectedSchedules: List<Schedule> get() = schedulesByDate[selectedDate].orEmpty()
    val selectedFriend: Friend? get() = friends.firstOrNull { it.id == selectedFriendId }
    val isMyCalendar: Boolean get() = selectedFriendId == null
}

/** 화면 아래 잠깐 뜨는 안내 */
enum class CalendarMessage { SAVED, SAVE_FAILED, DELETED }

/** 화면 쪽에서만 바뀌는 상태 (일정 목록 제외) */
private data class LocalState(
    val selectedDate: LocalDate,
    val visibleMonth: YearMonth,
    val selectedFriendId: String? = null,
    val isEditMode: Boolean = false,
    val form: AddScheduleForm? = null,
    val message: CalendarMessage? = null,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    observeSchedulesByDate: ObserveSchedulesByDateUseCase,
    observeFriends: ObserveFriendsUseCase,
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeShareGroups: ObserveShareGroupsUseCase,
    private val addSchedule: AddScheduleUseCase,
    private val deleteSchedule: DeleteScheduleUseCase,
    clock: Clock,
) : ViewModel() {

    private val today = LocalDate.now(clock)
    private val currentUser = observeCurrentUser()

    private val local = MutableStateFlow(LocalState(selectedDate = today, visibleMonth = YearMonth.from(today)))

    // 고른 사람(나/친구)이 바뀌면 그 사람의 일정으로 갈아탄다
    @OptIn(ExperimentalCoroutinesApi::class)
    private val schedulesByDate = local
        .map { it.selectedFriendId }
        .distinctUntilChanged()
        .flatMapLatest { ownerId -> observeSchedulesByDate(ownerId) }

    // 일정 목록(저장소)과 화면 상태를 합쳐 하나의 UiState로 만든다
    val uiState: StateFlow<CalendarUiState> = combine(
        local,
        schedulesByDate,
        observeFriends(),
        currentUser,
        observeShareGroups(),
    ) { l, byDate, friends, user, groups ->
        CalendarUiState(
            today = today,
            selectedDate = l.selectedDate,
            visibleMonth = l.visibleMonth,
            myName = user?.name.orEmpty(),
            friends = friends,
            shareGroups = groups,
            selectedFriendId = l.selectedFriendId,
            isEditMode = l.isEditMode,
            schedulesByDate = byDate,
            form = l.form,
            message = l.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(today = today, selectedDate = today, visibleMonth = YearMonth.from(today)),
    )

    // ---------- 달력 ----------

    fun onDateSelect(date: LocalDate) = local.update {
        it.copy(selectedDate = date, visibleMonth = YearMonth.from(date))
    }

    /** 프로필 줄: null = 나, 아니면 친구 id. 친구 캘린더는 보기만 가능 */
    fun onOwnerSelect(friendId: String?) = local.update { it.copy(selectedFriendId = friendId, isEditMode = false) }

    /** [수정] ↔ [완료]: 수정 모드에서는 카드마다 삭제 버튼이 보인다 */
    fun onToggleEditMode() = local.update { it.copy(isEditMode = !it.isEditMode) }

    fun onPrevMonth() = moveMonth(-1)

    fun onNextMonth() = moveMonth(1)

    /** 달을 옮기면 선택 날짜도 그 달의 같은 날(없으면 말일)로 옮긴다 */
    private fun moveMonth(delta: Long) = local.update {
        val month = it.visibleMonth.plusMonths(delta)
        val day = minOf(it.selectedDate.dayOfMonth, month.lengthOfMonth())
        it.copy(visibleMonth = month, selectedDate = month.atDay(day))
    }

    fun onDeleteSchedule(id: String) {
        viewModelScope.launch {
            if (local.value.selectedFriendId != null) return@launch // 친구 일정은 지울 수 없음
            deleteSchedule(id).onSuccess { local.update { it.copy(message = CalendarMessage.DELETED) } }
        }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    // ---------- 새 일정 등록 ----------

    fun onAddClick() = local.update {
        if (it.selectedFriendId != null) return@update it
        it.copy(
            form = AddScheduleForm(
                date = it.selectedDate,
                visibility = currentUser.value?.defaultVisibility ?: Visibility.FRIENDS,
            ),
        )
    }

    fun onFormDismiss() = local.update { it.copy(form = null) }

    fun onTitleChange(text: String) = updateForm { it.copy(title = text, showTitleError = false) }

    fun onTypeSelect(type: ScheduleType) = updateForm { it.copy(type = type) }

    fun onDateChange(date: LocalDate) = updateForm { it.copy(date = date) }

    fun onTimeChange(time: LocalTime) = updateForm { it.copy(time = time) }

    fun onLocationChange(text: String) = updateForm { it.copy(location = text) }

    /** 알림 시점은 여러 개 고를 수 있다 (누를 때마다 켜고 끔) */
    fun onReminderToggle(days: Int) = updateForm {
        it.copy(reminderDays = if (days in it.reminderDays) it.reminderDays - days else it.reminderDays + days)
    }

    fun onVisibilitySelect(visibility: Visibility) = updateForm { it.copy(visibility = visibility, showGroupError = false) }

    /** 그룹공개: 그룹 칩은 여러 개 고를 수 있다 */
    fun onGroupToggle(roomId: String) = updateForm {
        val ids = if (roomId in it.sharedRoomIds) it.sharedRoomIds - roomId else it.sharedRoomIds + roomId
        it.copy(sharedRoomIds = ids, showGroupError = false)
    }

    fun onSave() {
        val form = local.value.form ?: return
        if (form.isSaving) return
        if (form.title.isBlank()) {
            updateForm { it.copy(showTitleError = true) }
            return
        }
        if (form.visibility == Visibility.GROUP && form.sharedRoomIds.isEmpty()) {
            updateForm { it.copy(showGroupError = true) }
            return
        }
        updateForm { it.copy(isSaving = true) }
        viewModelScope.launch {
            addSchedule(
                NewSchedule(
                    title = form.title,
                    type = form.type,
                    startAt = form.date.atTime(form.time),
                    location = form.location,
                    reminderDaysBefore = form.reminderDays.toList(),
                    visibility = form.visibility,
                    sharedRoomIds = form.sharedRoomIds,
                ),
            ).onSuccess {
                // 저장한 날짜로 이동해서 바로 보이게
                local.update {
                    it.copy(
                        form = null,
                        selectedDate = form.date,
                        visibleMonth = YearMonth.from(form.date),
                        message = CalendarMessage.SAVED,
                    )
                }
            }.onFailure {
                updateForm { it.copy(isSaving = false) }
                local.update { it.copy(message = CalendarMessage.SAVE_FAILED) }
            }
        }
    }

    private fun updateForm(block: (AddScheduleForm) -> AddScheduleForm) = local.update { state ->
        state.copy(form = state.form?.let(block))
    }
}
