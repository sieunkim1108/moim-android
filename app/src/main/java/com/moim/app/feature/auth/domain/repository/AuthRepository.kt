package com.moim.app.feature.auth.domain.repository

import com.moim.app.feature.auth.domain.model.ProfileUpdate
import com.moim.app.feature.auth.domain.model.SignUpForm
import com.moim.app.feature.auth.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * 인증 기능의 "약속(인터페이스)".
 * 화면과 UseCase는 이 인터페이스만 알고, 실제 구현은 Hilt가 넣어준다.
 * local.properties에 Supabase 키가 있으면 SupabaseAuthRepository, 없으면 FakeAuthRepository(메모리 목업).
 */
interface AuthRepository {
    /** 지금 로그인한 사용자 (로그아웃 상태면 null) */
    val currentUser: StateFlow<UserProfile?>

    /** 학번을 쓸 수 있으면 true, 이미 가입돼 있으면 false */
    suspend fun isStudentIdAvailable(studentId: String): Result<Boolean>

    /** 학교 웹메일(@sookmyung.ac.kr)로 6자리 인증번호를 보낸다 */
    suspend fun requestEmailCode(email: String): Result<Unit>

    /** 인증번호가 맞으면 true, 틀리면 false */
    suspend fun verifyEmailCode(email: String, code: String): Result<Boolean>

    suspend fun signUp(form: SignUpForm): Result<UserProfile>

    /** keepSignedIn = 자동 로그인 체크 여부 */
    suspend fun login(studentId: String, password: String, keepSignedIn: Boolean): Result<UserProfile>

    /** 앱 시작 시: 자동 로그인이 켜져 있고 세션이 살아 있으면 그 사용자, 아니면 null */
    suspend fun restoreSession(): UserProfile?

    /** 프로필 수정 저장 (profiles, notification_prefs 갱신) */
    suspend fun updateProfile(update: ProfileUpdate): Result<UserProfile>

    /** 로그아웃: 세션과 자동 로그인 정보를 지운다 */
    suspend fun logout()
}
