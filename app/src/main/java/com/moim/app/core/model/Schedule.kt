package com.moim.app.core.model

import java.time.LocalDateTime

/** 일정 유형 (schedules.type). 색은 ui/theme/ScheduleColors.kt에서 정한다. */
enum class ScheduleType(val dbValue: String) {
    EXAM("exam"),
    ASSIGNMENT("assignment"),
    APPOINTMENT("appointment"),
    MEETUP("meetup"),
    CLASS("class"),
    ETC("etc"),
}

/** 일정이 어디서 만들어졌는지 (schedules.source) */
enum class ScheduleSource(val dbValue: String) {
    CHAT("chat"),
    MANUAL("manual"),
    TIMETABLE("timetable"),
    MEETUP("meetup"),
}

/** 모임 조율 화면에서 날짜/시간대별 가능 여부 */
enum class Availability { ALL, SOME, NONE, UNKNOWN }

/** 저장된 일정 하나 (schedules 테이블 한 줄) */
data class Schedule(
    val id: String,
    val title: String,
    val type: ScheduleType,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val allDay: Boolean = false,
    val isDeadline: Boolean = false, // 과제 마감처럼 "이때까지"인 일정
    val visibility: Visibility = Visibility.FRIENDS,
    val source: ScheduleSource = ScheduleSource.MANUAL,
    val memo: String? = null,
    val location: String? = null,
    val reminderDaysBefore: List<Int> = emptyList(), // 며칠 전에 알릴지 (예: [1, 3])
    val sharedRoomIds: Set<String> = emptySet(), // 그룹공개일 때 공개할 단체 DM 방 id들
)
