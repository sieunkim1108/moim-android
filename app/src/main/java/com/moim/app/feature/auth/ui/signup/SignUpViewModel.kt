package com.moim.app.feature.auth.ui.signup

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moim.app.R
import com.moim.app.core.model.NotificationPrefs
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.SignUpValidator
import com.moim.app.feature.auth.domain.model.SignUpForm
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.domain.usecase.CheckStudentIdUseCase
import com.moim.app.feature.auth.domain.usecase.RequestEmailCodeUseCase
import com.moim.app.feature.auth.domain.usecase.SignUpUseCase
import com.moim.app.feature.auth.domain.usecase.VerifyEmailCodeUseCase
import com.moim.app.feature.auth.ui.toAuthMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** 학번 중복 확인 상태 */
enum class StudentIdCheck { NotChecked, Checking, Available, Duplicate }

/** 학교 이메일 인증 진행 상태 */
enum class EmailVerifyStep { NotSent, Sending, Sent, Verifying, Verified }

/** 알림 시점을 고를 항목 (계획형 맞춤 알림 설정) */
enum class ReminderCategory { EXAM, ASSIGNMENT, APPOINTMENT }

/**
 * 회원가입 1단계 + 2단계 화면이 함께 쓰는 상태.
 * 두 화면이 같은 ViewModel을 공유하므로 1단계에서 입력한 값이 2단계까지 유지된다.
 */
data class SignUpUiState(
    // 1단계: 기본 정보
    val studentId: String = "",
    val studentIdCheck: StudentIdCheck = StudentIdCheck.NotChecked,
    val email: String = "",
    val emailStep: EmailVerifyStep = EmailVerifyStep.NotSent,
    val emailCode: String = "",
    val codeRemainingSec: Int = 0, // 인증번호 남은 시간 (0이면 만료)
    val emailCodeWrong: Boolean = false,
    @param:StringRes val emailErrorRes: Int? = null, // 인증요청 실패 이유 (이미 가입된 메일 등)
    val name: String = "",
    val birthDate: LocalDate? = null,
    val college: String? = null,
    val major: String? = null,
    val password: String = "",
    val passwordConfirm: String = "",
    val isPasswordVisible: Boolean = false,
    val agreedToPrivacy: Boolean = false,
    val showErrors: Boolean = false, // [다음]을 누른 뒤부터 빈 칸 에러를 보여준다
    @param:StringRes val nextBlockedRes: Int? = null, // [다음]이 막혔을 때 아래에 잠깐 띄울 이유
    val goToTypeStep: Boolean = false, // true면 화면이 2단계로 이동
    // 2단계: 성향 설정
    val planningType: PlanningType = PlanningType.EARLY,
    val notificationPrefs: NotificationPrefs = PlanningType.EARLY.defaultPrefs,
    val visibility: Visibility = Visibility.FRIENDS,
    val isSubmitting: Boolean = false,
    @param:StringRes val submitErrorRes: Int? = null,
    val completedProfile: UserProfile? = null, // 가입 성공 시 채워짐 → 완료 화면으로 이동
) {
    val isStudentIdFormatValid: Boolean get() = SignUpValidator.isValidStudentId(studentId)
    val isEmailFormatValid: Boolean get() = SignUpValidator.isValidSchoolEmail(email)
    val isEmailVerified: Boolean get() = emailStep == EmailVerifyStep.Verified
    val isCodeExpired: Boolean get() = emailStep == EmailVerifyStep.Sent && codeRemainingSec <= 0
    val isPasswordValid: Boolean get() = SignUpValidator.isValidPassword(password)
    val isPasswordConfirmed: Boolean get() = passwordConfirm.isNotEmpty() && password == passwordConfirm

    val isBasicInfoValid: Boolean
        get() = isStudentIdFormatValid &&
            studentIdCheck == StudentIdCheck.Available &&
            isEmailVerified &&
            SignUpValidator.isValidName(name) &&
            birthDate != null &&
            college != null &&
            major != null &&
            isPasswordValid &&
            isPasswordConfirmed &&
            agreedToPrivacy

    /** 위에서부터 첫 번째로 잘못된 칸의 안내 문구 (화면 위쪽 칸은 스크롤에 가려 안 보일 수 있어서) */
    @get:StringRes
    val firstBasicInfoErrorRes: Int?
        get() = when {
            !isStudentIdFormatValid -> R.string.signup1_student_id_invalid
            studentIdCheck == StudentIdCheck.Duplicate -> R.string.error_duplicate_student_id
            studentIdCheck != StudentIdCheck.Available -> R.string.signup1_student_id_need_check
            !isEmailFormatValid -> R.string.signup1_email_invalid
            !isEmailVerified -> R.string.signup1_email_need_verify
            !SignUpValidator.isValidName(name) -> R.string.signup1_name_error
            birthDate == null -> R.string.signup1_birth_error
            college == null -> R.string.signup1_college_error
            major == null -> R.string.signup1_major_error
            !isPasswordValid -> R.string.signup1_password_error
            !isPasswordConfirmed -> R.string.signup1_password_confirm_error
            !agreedToPrivacy -> R.string.signup1_agree_error
            else -> null
        }
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val checkStudentId: CheckStudentIdUseCase,
    private val requestEmailCode: RequestEmailCodeUseCase,
    private val verifyEmailCode: VerifyEmailCodeUseCase,
    private val signUp: SignUpUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private var codeTimerJob: Job? = null

    // ---------- 1단계 ----------

    fun onStudentIdChange(value: String) {
        // 학번이 바뀌면 중복 확인을 다시 해야 한다
        _uiState.update {
            it.copy(studentId = value.filter(Char::isDigit).take(7), studentIdCheck = StudentIdCheck.NotChecked)
        }
    }

    fun onCheckStudentIdClick() {
        val state = _uiState.value
        if (!state.isStudentIdFormatValid || state.studentIdCheck == StudentIdCheck.Checking) {
            _uiState.update { it.copy(showErrors = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(studentIdCheck = StudentIdCheck.Checking) }
            val result = checkStudentId(state.studentId)
            _uiState.update {
                it.copy(
                    studentIdCheck = when (result.getOrNull()) {
                        true -> StudentIdCheck.Available
                        false -> StudentIdCheck.Duplicate
                        null -> StudentIdCheck.NotChecked // 네트워크 실패 등 → 다시 누르게
                    },
                )
            }
        }
    }

    // ---------- 학교 이메일 인증 ----------

    fun onEmailChange(value: String) {
        // 메일 주소가 바뀌면 인증을 처음부터 다시 한다
        codeTimerJob?.cancel()
        _uiState.update {
            it.copy(
                email = value.trim().take(60),
                emailStep = EmailVerifyStep.NotSent,
                emailCode = "",
                codeRemainingSec = 0,
                emailCodeWrong = false,
                emailErrorRes = null,
            )
        }
    }

    /** [인증요청] / [재요청] */
    fun onRequestEmailCode() {
        val state = _uiState.value
        if (state.emailStep == EmailVerifyStep.Sending || state.isEmailVerified) return
        if (!state.isEmailFormatValid) {
            _uiState.update { it.copy(emailErrorRes = R.string.signup1_email_invalid) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(emailStep = EmailVerifyStep.Sending, emailErrorRes = null, emailCodeWrong = false) }
            requestEmailCode(state.email)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            emailStep = EmailVerifyStep.Sent,
                            emailCode = "",
                            codeRemainingSec = SignUpValidator.EMAIL_CODE_VALID_SECONDS,
                        )
                    }
                    startCodeTimer()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(emailStep = EmailVerifyStep.NotSent, emailErrorRes = e.toAuthMessageRes()) }
                }
        }
    }

    /** 1초마다 남은 시간을 줄인다. 0이 되면 [확인]을 막고 재요청을 안내한다. */
    private fun startCodeTimer() {
        codeTimerJob?.cancel()
        codeTimerJob = viewModelScope.launch {
            while (_uiState.value.codeRemainingSec > 0) {
                delay(1_000)
                _uiState.update { it.copy(codeRemainingSec = (it.codeRemainingSec - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun onEmailCodeChange(value: String) = _uiState.update {
        it.copy(emailCode = value.filter(Char::isDigit).take(6), emailCodeWrong = false)
    }

    /** 인증번호 [확인] */
    fun onVerifyEmailCode() {
        val state = _uiState.value
        if (state.emailStep != EmailVerifyStep.Sent || state.isCodeExpired) return
        if (!SignUpValidator.isValidEmailCode(state.emailCode)) {
            _uiState.update { it.copy(emailCodeWrong = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(emailStep = EmailVerifyStep.Verifying) }
            val ok = verifyEmailCode(state.email, state.emailCode).getOrDefault(false)
            if (ok) codeTimerJob?.cancel()
            _uiState.update {
                if (ok) {
                    it.copy(emailStep = EmailVerifyStep.Verified, emailCodeWrong = false)
                } else {
                    it.copy(emailStep = EmailVerifyStep.Sent, emailCodeWrong = true)
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value.take(20)) }

    fun onBirthDateChange(date: LocalDate) = _uiState.update { it.copy(birthDate = date) }

    fun onCollegeChange(college: String) {
        // 학부가 바뀌면 이전 학과 선택은 지운다
        _uiState.update { if (it.college == college) it else it.copy(college = college, major = null) }
    }

    fun onMajorChange(major: String) = _uiState.update { it.copy(major = major) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

    fun onPasswordConfirmChange(value: String) = _uiState.update { it.copy(passwordConfirm = value) }

    fun onTogglePasswordVisible() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun onAgreeChange(agreed: Boolean) = _uiState.update { it.copy(agreedToPrivacy = agreed) }

    /**
     * [다음] 버튼.
     * 학번 중복 확인을 아직 안 눌렀으면 여기서 대신 확인한 뒤 이어서 검사한다.
     * 모두 올바르면 goToTypeStep = true, 아니면 첫 번째 문제를 안내한다.
     */
    fun onBasicInfoNext() {
        val state = _uiState.value
        if (state.studentIdCheck == StudentIdCheck.Checking) return
        if (state.isStudentIdFormatValid && state.studentIdCheck == StudentIdCheck.NotChecked) {
            viewModelScope.launch {
                _uiState.update { it.copy(studentIdCheck = StudentIdCheck.Checking) }
                val result = checkStudentId(state.studentId).getOrNull()
                _uiState.update {
                    it.copy(
                        studentIdCheck = when (result) {
                            true -> StudentIdCheck.Available
                            false -> StudentIdCheck.Duplicate
                            null -> StudentIdCheck.NotChecked
                        },
                    )
                }
                validateAndGoNext()
            }
        } else {
            validateAndGoNext()
        }
    }

    private fun validateAndGoNext() {
        _uiState.update {
            if (it.isBasicInfoValid) {
                it.copy(goToTypeStep = true)
            } else {
                it.copy(showErrors = true, nextBlockedRes = it.firstBasicInfoErrorRes)
            }
        }
    }

    fun onNextBlockedShown() = _uiState.update { it.copy(nextBlockedRes = null) }

    fun onNavigatedToTypeStep() = _uiState.update { it.copy(goToTypeStep = false) }

    // ---------- 2단계 ----------

    fun onPlanningTypeChange(type: PlanningType) {
        // 성향을 고르면 그 성향의 기본 알림 시점으로 맞춘다
        _uiState.update { it.copy(planningType = type, notificationPrefs = type.defaultPrefs) }
    }

    /** 계획형일 때만: 항목별 알림 시점 직접 선택 */
    fun onReminderDaysChange(category: ReminderCategory, days: Int) {
        _uiState.update {
            val prefs = it.notificationPrefs
            it.copy(
                notificationPrefs = when (category) {
                    ReminderCategory.EXAM -> prefs.copy(examDaysBefore = days)
                    ReminderCategory.ASSIGNMENT -> prefs.copy(assignmentDaysBefore = days)
                    ReminderCategory.APPOINTMENT -> prefs.copy(appointmentDaysBefore = days)
                },
            )
        }
    }

    fun onVisibilityChange(visibility: Visibility) = _uiState.update { it.copy(visibility = visibility) }

    fun onSubmit() {
        val s = _uiState.value
        if (s.isSubmitting || !s.isBasicInfoValid) return
        val form = SignUpForm(
            studentId = s.studentId,
            schoolEmail = s.email,
            name = s.name,
            birthDate = requireNotNull(s.birthDate),
            college = requireNotNull(s.college),
            major = requireNotNull(s.major),
            password = s.password,
            planningType = s.planningType,
            notificationPrefs = s.notificationPrefs,
            defaultVisibility = s.visibility,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitErrorRes = null) }
            signUp(form)
                .onSuccess { profile -> _uiState.update { it.copy(isSubmitting = false, completedProfile = profile) } }
                .onFailure { e -> _uiState.update { it.copy(isSubmitting = false, submitErrorRes = e.toAuthMessageRes()) } }
        }
    }

    fun onSubmitErrorShown() = _uiState.update { it.copy(submitErrorRes = null) }

    fun onNavigatedToDone() = _uiState.update { it.copy(completedProfile = null) }
}
