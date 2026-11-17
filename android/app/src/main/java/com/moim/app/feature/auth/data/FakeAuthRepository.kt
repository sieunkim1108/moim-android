package com.moim.app.feature.auth.data

import com.moim.app.core.data.SessionPreferences
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.model.AuthException
import com.moim.app.feature.auth.domain.model.ProfileUpdate
import com.moim.app.feature.auth.domain.model.SignUpForm
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 없이 화면 흐름을 확인하기 위한 목업 저장소. 앱을 끄면 데이터가 사라진다.
 * 테스트 계정: 학번 1234567 / 비밀번호 moim1234
 * 이메일 인증번호: 어떤 메일이든 123456 (실제 메일은 안 보냄)
 * TODO: Supabase 연결 단계에서 SupabaseAuthRepository로 교체 (AuthModule의 @Binds 한 줄만 바꾸면 됨)
 */
@Singleton
class FakeAuthRepository @Inject constructor(
    private val session: SessionPreferences,
) : AuthRepository {

    private data class Account(val password: String, val profile: UserProfile)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val mutex = Mutex()
    private val requestedEmails = mutableSetOf<String>()
    private val verifiedEmails = mutableSetOf<String>()
    private val accounts = mutableMapOf(
        DEMO_STUDENT_ID to Account(
            password = DEMO_PASSWORD,
            profile = UserProfile(
                id = UUID.randomUUID().toString(),
                studentId = DEMO_STUDENT_ID,
                name = "김눈송",
                college = "소프트웨어학부",
                major = "컴퓨터과학전공",
                planningType = PlanningType.EARLY,
                defaultVisibility = Visibility.FRIENDS,
                schoolEmail = "snowflake@sookmyung.ac.kr",
            ),
        ),
    )

    override suspend fun isStudentIdAvailable(studentId: String): Result<Boolean> {
        delay(FAKE_DELAY_MS) // 네트워크 흉내
        return mutex.withLock { Result.success(studentId !in accounts) }
    }

    override suspend fun requestEmailCode(email: String): Result<Unit> {
        delay(FAKE_DELAY_MS)
        return mutex.withLock {
            if (accounts.values.any { it.profile.schoolEmail.equals(email, ignoreCase = true) }) {
                Result.failure(AuthException.DuplicateEmail)
            } else {
                requestedEmails += email.lowercase()
                Result.success(Unit)
            }
        }
    }

    override suspend fun verifyEmailCode(email: String, code: String): Result<Boolean> {
        delay(FAKE_DELAY_MS)
        return mutex.withLock {
            val ok = email.lowercase() in requestedEmails && code == DEMO_EMAIL_CODE
            if (ok) verifiedEmails += email.lowercase()
            Result.success(ok)
        }
    }

    override suspend fun signUp(form: SignUpForm): Result<UserProfile> {
        delay(FAKE_DELAY_MS)
        return mutex.withLock {
            if (form.studentId in accounts) {
                Result.failure(AuthException.DuplicateStudentId)
            } else if (form.schoolEmail.lowercase() !in verifiedEmails) {
                // 서버도 "인증을 마친 메일"인지 다시 확인한다 (화면만 믿지 않기)
                Result.failure(AuthException.EmailNotVerified)
            } else {
                val profile = UserProfile(
                    id = UUID.randomUUID().toString(),
                    studentId = form.studentId,
                    name = form.name,
                    college = form.college,
                    major = form.major,
                    planningType = form.planningType,
                    defaultVisibility = form.defaultVisibility,
                    schoolEmail = form.schoolEmail,
                    notificationPrefs = form.notificationPrefs,
                )
                accounts[form.studentId] = Account(form.password, profile)
                _currentUser.value = profile // 가입하면 바로 로그인 상태
                session.save(form.studentId, autoLogin = true)
                Result.success(profile)
            }
        }
    }

    override suspend fun login(studentId: String, password: String, keepSignedIn: Boolean): Result<UserProfile> {
        delay(FAKE_DELAY_MS)
        return mutex.withLock {
            val account = accounts[studentId]
            if (account != null && account.password == password) {
                _currentUser.value = account.profile
                session.save(studentId, autoLogin = keepSignedIn)
                Result.success(account.profile)
            } else {
                Result.failure(AuthException.InvalidCredentials)
            }
        }
    }

    /**
     * 목업: 저장된 학번이 메모리 계정에 있으면 로그인 상태로 되돌린다.
     * (앱을 끄면 새로 가입한 계정은 사라지므로 테스트 계정만 자동 로그인됨)
     * Supabase에서는 supabase-kt가 저장해 둔 세션 토큰을 불러오는 것으로 바뀐다.
     */
    override suspend fun restoreSession(): UserProfile? {
        val saved = session.read() ?: return null
        if (!saved.autoLogin) return null
        return mutex.withLock {
            accounts[saved.studentId]?.profile?.also { _currentUser.value = it }
        }
    }

    override suspend fun updateProfile(update: ProfileUpdate): Result<UserProfile> {
        delay(FAKE_DELAY_MS)
        return mutex.withLock {
            val current = _currentUser.value ?: return@withLock Result.failure(AuthException.InvalidCredentials)
            val updated = current.copy(
                name = update.name,
                college = update.college,
                major = update.major,
                planningType = update.planningType,
                notificationPrefs = update.notificationPrefs,
                defaultVisibility = update.defaultVisibility,
            )
            accounts[current.studentId]?.let { accounts[current.studentId] = it.copy(profile = updated) }
            _currentUser.value = updated
            Result.success(updated)
        }
    }

    override suspend fun logout() {
        session.clear()
        _currentUser.value = null
    }

    private companion object {
        const val DEMO_STUDENT_ID = "1234567"
        const val DEMO_PASSWORD = "moim1234"
        const val DEMO_EMAIL_CODE = "123456"
        const val FAKE_DELAY_MS = 600L
    }
}
