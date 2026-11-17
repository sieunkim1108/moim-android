package com.moim.app.core.model

/*
 * 여러 기능(회원가입, 프로필, 캘린더)에서 같이 쓰는 사용자 설정 모델.
 * dbValue는 Supabase 테이블에 저장할 문자열 값이다.
 */

/** 항목별로 며칠 전에 알림을 받을지 (notification_prefs 테이블) */
data class NotificationPrefs(
    val examDaysBefore: Int,
    val assignmentDaysBefore: Int,
    val appointmentDaysBefore: Int,
)

/** 알림 시점 선택지: 시험·과제는 1/3/7일 전, 약속은 당일(0)/1/3일 전 (S-11 디자인) */
object ReminderChoices {
    val examOrAssignment = listOf(1, 3, 7)
    val appointment = listOf(0, 1, 3)
}

/** 일정 성향 4종 (profiles.planning_type) */
enum class PlanningType(val dbValue: String, val defaultPrefs: NotificationPrefs) {
    EARLY("early", NotificationPrefs(examDaysBefore = 7, assignmentDaysBefore = 7, appointmentDaysBefore = 1)),
    THREE_DAY("three_day", NotificationPrefs(examDaysBefore = 3, assignmentDaysBefore = 3, appointmentDaysBefore = 1)),
    CRAMMING("cramming", NotificationPrefs(examDaysBefore = 1, assignmentDaysBefore = 1, appointmentDaysBefore = 1)),
    PLANNER("planner", NotificationPrefs(examDaysBefore = 3, assignmentDaysBefore = 3, appointmentDaysBefore = 1)),
}

/** 일정 공개 범위 (profiles.default_visibility, schedules.visibility) */
enum class Visibility(val dbValue: String) {
    PUBLIC("public"),
    FRIENDS("friends"),
    GROUP("group"), // 고른 단체 DM 방의 멤버에게만 공개 (schedule_shared_rooms)
    PRIVATE("private"),
    ;

    companion object {
        /** 회원가입(S-04)에서 고르는 기본 공개 범위. 그룹공개는 일정마다 그룹을 골라야 해서 뺀다. */
        val signUpChoices = listOf(PUBLIC, FRIENDS, PRIVATE)
    }
}
