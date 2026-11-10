package com.moim.app.feature.auth.domain.usecase

import com.moim.app.feature.auth.domain.model.SignUpForm
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/*
 * UseCase: "앱이 하는 일 하나"를 나타내는 작은 클래스.
 * ViewModel은 Repository를 직접 부르지 않고 UseCase를 통해 부른다. (규칙이 생기면 여기에 모은다)
 */

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(studentId: String, password: String, keepSignedIn: Boolean): Result<UserProfile> =
        repository.login(studentId.trim(), password, keepSignedIn)
}

/** 앱 시작 시 자동 로그인 시도 */
class RestoreSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): UserProfile? = repository.restoreSession()
}

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() = repository.logout()
}

class CheckStudentIdUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(studentId: String): Result<Boolean> =
        repository.isStudentIdAvailable(studentId.trim())
}

/** 학교 웹메일로 인증번호 보내기 */
class RequestEmailCodeUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> =
        repository.requestEmailCode(email.trim().lowercase())
}

/** 인증번호 확인 (맞으면 true) */
class VerifyEmailCodeUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, code: String): Result<Boolean> =
        repository.verifyEmailCode(email.trim().lowercase(), code.trim())
}

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(form: SignUpForm): Result<UserProfile> =
        repository.signUp(
            form.copy(
                studentId = form.studentId.trim(),
                schoolEmail = form.schoolEmail.trim().lowercase(),
                name = form.name.trim(),
            ),
        )
}

/** 지금 로그인한 사용자를 구독한다 (다른 기능에서 이름·성향을 쓸 때) */
class ObserveCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): StateFlow<UserProfile?> = repository.currentUser
}
