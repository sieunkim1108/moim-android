package com.moim.app.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.core.model.NotificationPrefs
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.model.ProfileUpdate
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.domain.usecase.LogoutUseCase
import com.moim.app.feature.auth.domain.usecase.ObserveCurrentUserUseCase
import com.moim.app.feature.auth.ui.signup.ReminderCategory
import com.moim.app.feature.profile.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 화면에서 고치고 있는 값 (저장 전) */
data class ProfileDraft(
    val name: String,
    val college: String,
    val major: String?,
    val planningType: PlanningType,
    val notificationPrefs: NotificationPrefs,
    val defaultVisibility: Visibility,
)

/** 화면 아래 잠깐 뜨는 안내 */
enum class ProfileMessage { SAVED, NAME_EMPTY, MAJOR_EMPTY, FAILED }

data class ProfileUiState(
    val user: UserProfile? = null, // 학번·이메일처럼 못 바꾸는 값은 여기서 읽는다
    val draft: ProfileDraft? = null, // null이면 아직 불러오는 중
    val isSaving: Boolean = false,
    val message: ProfileMessage? = null,
    val savedAndClose: Boolean = false, // 저장 후 메인으로
    val loggedOut: Boolean = false,     // 로그아웃 후 시작 화면으로
    val showLogoutConfirm: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val logout: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // 처음 한 번만 현재 프로필을 편집용으로 복사한다 (입력 중에 덮어쓰지 않게)
        viewModelScope.launch {
            val user = observeCurrentUser().filterNotNull().first()
            _uiState.update { it.copy(user = user, draft = user.toDraft()) }
        }
    }

    private fun updateDraft(block: (ProfileDraft) -> ProfileDraft) =
        _uiState.update { state -> state.copy(draft = state.draft?.let(block)) }

    fun onNameChange(value: String) = updateDraft { it.copy(name = value.take(20)) }

    fun onCollegeChange(college: String) = updateDraft {
        // 학부가 바뀌면 전공을 다시 고르게 한다
        if (it.college == college) it else it.copy(college = college, major = null)
    }

    fun onMajorChange(major: String) = updateDraft { it.copy(major = major) }

    /** 성향을 바꾸면 알림 기본값도 그 성향에 맞춘다 */
    fun onPlanningTypeChange(type: PlanningType) = updateDraft {
        it.copy(planningType = type, notificationPrefs = type.defaultPrefs)
    }

    fun onReminderChange(category: ReminderCategory, days: Int) = updateDraft {
        val prefs = it.notificationPrefs
        it.copy(
            notificationPrefs = when (category) {
                ReminderCategory.EXAM -> prefs.copy(examDaysBefore = days)
                ReminderCategory.ASSIGNMENT -> prefs.copy(assignmentDaysBefore = days)
                ReminderCategory.APPOINTMENT -> prefs.copy(appointmentDaysBefore = days)
            },
        )
    }

    fun onVisibilityChange(visibility: Visibility) = updateDraft { it.copy(defaultVisibility = visibility) }

    fun onSave() {
        val state = _uiState.value
        val draft = state.draft ?: return
        if (state.isSaving) return
        val major = draft.major
        when {
            draft.name.isBlank() -> return _uiState.update { it.copy(message = ProfileMessage.NAME_EMPTY) }
            major == null -> return _uiState.update { it.copy(message = ProfileMessage.MAJOR_EMPTY) }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateProfile(
                ProfileUpdate(
                    name = draft.name,
                    college = draft.college,
                    major = requireNotNull(major),
                    planningType = draft.planningType,
                    notificationPrefs = draft.notificationPrefs,
                    defaultVisibility = draft.defaultVisibility,
                ),
            ).onSuccess { saved ->
                _uiState.update { it.copy(isSaving = false, user = saved, message = ProfileMessage.SAVED, savedAndClose = true) }
            }.onFailure {
                _uiState.update { it.copy(isSaving = false, message = ProfileMessage.FAILED) }
            }
        }
    }

    fun onLogoutClick() = _uiState.update { it.copy(showLogoutConfirm = true) }

    fun onLogoutDismiss() = _uiState.update { it.copy(showLogoutConfirm = false) }

    /** 로그아웃: DataStore 자동 로그인 정보 삭제 → 시작 화면(S-01) */
    fun onLogoutConfirm() {
        viewModelScope.launch {
            logout()
            _uiState.update { it.copy(showLogoutConfirm = false, loggedOut = true) }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onNavigated() = _uiState.update { it.copy(savedAndClose = false, loggedOut = false) }
}

private fun UserProfile.toDraft() = ProfileDraft(
    name = name,
    college = college,
    major = major,
    planningType = planningType,
    notificationPrefs = notificationPrefs,
    defaultVisibility = defaultVisibility,
)
