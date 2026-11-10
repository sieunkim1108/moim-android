package com.moim.app.feature.auth.data

import com.moim.app.core.model.NotificationPrefs
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.model.UserProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Supabase 테이블 한 줄을 그대로 옮긴 데이터 클래스 (DTO).
 * 화면에서 쓰는 모델(UserProfile)과 분리해 두면, DB 컬럼 이름이 바뀌어도 여기만 고치면 된다.
 */

@Serializable
internal data class ProfileRow(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("school_email") val schoolEmail: String,
    val name: String,
    val college: String,
    val major: String,
    @SerialName("planning_type") val planningType: String,
    @SerialName("default_visibility") val defaultVisibility: String,
)

@Serializable
internal data class NotificationPrefsRow(
    @SerialName("exam_days_before") val examDaysBefore: Int,
    @SerialName("assignment_days_before") val assignmentDaysBefore: Int,
    @SerialName("appointment_days_before") val appointmentDaysBefore: Int,
)

/** profiles 수정용 (학번·학교 이메일은 넣지 않는다) */
@Serializable
internal data class ProfilePatch(
    val name: String,
    val college: String,
    val major: String,
    @SerialName("planning_type") val planningType: String,
    @SerialName("default_visibility") val defaultVisibility: String,
)

/** Edge Function verify-email-code 응답 */
@Serializable
internal data class VerifyEmailResponse(val verified: Boolean)

internal fun planningTypeOf(db: String) = PlanningType.entries.firstOrNull { it.dbValue == db } ?: PlanningType.EARLY
internal fun visibilityOf(db: String) = Visibility.entries.firstOrNull { it.dbValue == db } ?: Visibility.FRIENDS

internal fun NotificationPrefsRow.toModel() = NotificationPrefs(examDaysBefore, assignmentDaysBefore, appointmentDaysBefore)
internal fun NotificationPrefs.toRow() = NotificationPrefsRow(examDaysBefore, assignmentDaysBefore, appointmentDaysBefore)

internal fun ProfileRow.toModel(prefs: NotificationPrefsRow?): UserProfile {
    val type = planningTypeOf(planningType)
    return UserProfile(
        id = id,
        studentId = studentId,
        name = name,
        college = college,
        major = major,
        planningType = type,
        defaultVisibility = visibilityOf(defaultVisibility),
        schoolEmail = schoolEmail,
        notificationPrefs = prefs?.toModel() ?: type.defaultPrefs,
    )
}
