package com.moim.app.feature.meetup.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * 바쁜 시간 한 칸.
 * label이 null이면 비공개 일정 (친구에게는 내용 없이 "비공개"로만 보임)
 */
data class BusyBlock(
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val label: String?,
)

/** 모임 참여자 한 명과 그 사람의 바쁜 시간 */
data class MeetupMember(
    val id: String,
    val name: String,
    val isMe: Boolean,
    val busy: List<BusyBlock>,
)

data class TimeRange(val start: LocalTime, val end: LocalTime) {
    val minutes: Long get() = java.time.Duration.between(start, end).toMinutes()
}

/** 알고리즘이 고른 추천 시간 하나 */
data class MeetupCandidate(
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val availableMemberIds: List<String>,
    val totalMembers: Int,
) {
    val availableCount: Int get() = availableMemberIds.size
    val isEveryone: Boolean get() = availableCount == totalMembers
}
