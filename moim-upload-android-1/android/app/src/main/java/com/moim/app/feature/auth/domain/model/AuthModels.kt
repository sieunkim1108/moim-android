package com.moim.app.feature.auth.domain.model

import com.moim.app.core.model.NotificationPrefs
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.Visibility
import java.time.LocalDate

/** 회원가입 때 서버로 보낼 정보 전체 */
data class SignUpForm(
    val studentId: String,
    val schoolEmail: String, // 인증을 마친 학교 웹메일
    val name: String,
    val birthDate: LocalDate,
    val college: String,
    val major: String,
    val password: String,
    val planningType: PlanningType,
    val notificationPrefs: NotificationPrefs,
    val defaultVisibility: Visibility,
)

/** 로그인한 사용자 정보 (profiles 테이블 한 줄) */
data class UserProfile(
    val id: String,
    val studentId: String,
    val name: String,
    val college: String,
    val major: String,
    val planningType: PlanningType,
    val defaultVisibility: Visibility,
    val schoolEmail: String = "",
    val notificationPrefs: NotificationPrefs = planningType.defaultPrefs, // 시험/과제/약속별 알림 시점
)

/** 프로필 수정(S-11)에서 바꿀 수 있는 값. 학번·학교 이메일은 바꿀 수 없다. */
data class ProfileUpdate(
    val name: String,
    val college: String,
    val major: String,
    val planningType: PlanningType,
    val notificationPrefs: NotificationPrefs,
    val defaultVisibility: Visibility,
)

/** 인증 과정에서 생길 수 있는 실패. 화면에서는 종류별로 다른 문구를 보여준다. */
sealed class AuthException : Exception() {
    data object InvalidCredentials : AuthException()
    data object DuplicateStudentId : AuthException()
    data object DuplicateEmail : AuthException()
    data object EmailNotVerified : AuthException()
    data object Network : AuthException()
}
