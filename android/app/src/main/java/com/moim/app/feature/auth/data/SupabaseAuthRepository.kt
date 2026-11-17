package com.moim.app.feature.auth.data

import com.moim.app.core.data.SessionPreferences
import com.moim.app.core.supabase.SupabaseConfig
import com.moim.app.feature.auth.domain.model.AuthException
import com.moim.app.feature.auth.domain.model.ProfileUpdate
import com.moim.app.feature.auth.domain.model.SignUpForm
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase로 동작하는 인증 저장소.
 * - 로그인: Supabase Auth (이메일 자리에 "학번@moim.app"). 비밀번호는 Supabase Auth가 해시로 관리하고 앱은 저장하지 않는다.
 * - 학교 메일 인증: Edge Function(send-email-code / verify-email-code)이 service_role로 처리
 * - 프로필 생성: 가입하면 DB 트리거(handle_new_user)가 metadata로 profiles·notification_prefs를 만든다
 */
@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val session: SessionPreferences,
) : AuthRepository {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    override val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun isStudentIdAvailable(studentId: String): Result<Boolean> = call {
        supabase.postgrest.rpc(
            "is_student_id_available",
            buildJsonObject { put("p_student_id", studentId) },
        ).decodeAs<Boolean>()
    }

    override suspend fun requestEmailCode(email: String): Result<Unit> = call {
        // 메일 인증을 꺼 둔 동안은 보내지 않고 바로 다음 단계로
        if (!SupabaseConfig.EMAIL_VERIFICATION_ENABLED) return@call
        try {
            supabase.functions.invoke("send-email-code", body = buildJsonObject { put("email", email.lowercase()) })
        } catch (e: RestException) {
            // Edge Function이 409를 주면 이미 가입된 메일
            if (e.statusCode == 409) throw AuthException.DuplicateEmail else throw e
        }
        Unit
    }

    override suspend fun verifyEmailCode(email: String, code: String): Result<Boolean> = call {
        if (!SupabaseConfig.EMAIL_VERIFICATION_ENABLED) return@call true
        val response = supabase.functions.invoke(
            "verify-email-code",
            body = buildJsonObject {
                put("email", email.lowercase())
                put("code", code)
            },
        )
        json.decodeFromString<VerifyEmailResponse>(response.bodyAsText()).verified
    }

    override suspend fun signUp(form: SignUpForm): Result<UserProfile> = call {
        try {
            supabase.auth.signUpWith(Email) {
                email = SupabaseConfig.loginEmail(form.studentId)
                password = form.password
                // DB 트리거가 이 값으로 프로필을 만든다
                data = buildJsonObject {
                    put("student_id", form.studentId)
                    put("school_email", form.schoolEmail.lowercase())
                    put("name", form.name)
                    put("birth_date", form.birthDate.toString())
                    put("college", form.college)
                    put("major", form.major)
                    put("planning_type", form.planningType.dbValue)
                    put("default_visibility", form.defaultVisibility.dbValue)
                    put("exam_days_before", form.notificationPrefs.examDaysBefore)
                    put("assignment_days_before", form.notificationPrefs.assignmentDaysBefore)
                    put("appointment_days_before", form.notificationPrefs.appointmentDaysBefore)
                }
            }
        } catch (e: AuthRestException) {
            throw when (e.errorCode) {
                AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> AuthException.DuplicateStudentId
                // 트리거가 막으면(예: 학교 메일 중복, 인증 미완료) Auth는 DB 오류로 돌려준다
                else -> AuthException.EmailNotVerified
            }
        }
        session.save(form.studentId, autoLogin = true)
        loadMyProfile()
    }

    override suspend fun login(studentId: String, password: String, keepSignedIn: Boolean): Result<UserProfile> = call {
        try {
            supabase.auth.signInWith(Email) {
                email = SupabaseConfig.loginEmail(studentId)
                this.password = password
            }
        } catch (e: AuthRestException) {
            throw AuthException.InvalidCredentials
        }
        session.save(studentId, autoLogin = keepSignedIn)
        loadMyProfile()
    }

    override suspend fun restoreSession(): UserProfile? {
        // supabase-kt가 저장된 토큰을 다 불러올 때까지 기다린다
        val status = supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        if (status !is SessionStatus.Authenticated) return null
        val saved = session.read()
        if (saved?.autoLogin != true) {
            // 자동 로그인을 끈 사용자는 앱을 다시 켜면 로그인부터
            runCatching { supabase.auth.signOut() }
            return null
        }
        return runCatching { loadMyProfile() }.getOrNull()
    }

    override suspend fun updateProfile(update: ProfileUpdate): Result<UserProfile> = call {
        val uid = myId()
        supabase.from("profiles").update(
            ProfilePatch(
                name = update.name,
                college = update.college,
                major = update.major,
                planningType = update.planningType.dbValue,
                defaultVisibility = update.defaultVisibility.dbValue,
            ),
        ) { filter { eq("id", uid) } }
        supabase.from("notification_prefs").update(update.notificationPrefs.toRow()) {
            filter { eq("user_id", uid) }
        }
        loadMyProfile()
    }

    override suspend fun logout() {
        runCatching { supabase.auth.signOut() }
        session.clear()
        _currentUser.value = null
    }

    /** 내 profiles + notification_prefs를 읽어 currentUser에 넣는다 (RLS: 본인 행만 보임) */
    private suspend fun loadMyProfile(): UserProfile {
        val uid = myId()
        val profile = supabase.from("profiles").select { filter { eq("id", uid) } }.decodeSingle<ProfileRow>()
        val prefs = supabase.from("notification_prefs")
            .select { filter { eq("user_id", uid) } }
            .decodeSingleOrNull<NotificationPrefsRow>()
        return profile.toModel(prefs).also { _currentUser.value = it }
    }

    private fun myId(): String =
        supabase.auth.currentUserOrNull()?.id ?: throw AuthException.InvalidCredentials

    /** 예외를 화면이 아는 AuthException으로 바꿔 Result로 감싼다 */
    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: AuthException) {
        Result.failure(e)
    } catch (e: HttpRequestException) {
        Result.failure(AuthException.Network)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AuthException.Network)
    }
}
