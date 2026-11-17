package com.moim.app.feature.meetup.domain

import com.moim.app.core.model.Availability
import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.model.MeetupMember
import com.moim.app.feature.meetup.domain.model.TimeRange
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

/**
 * 모임 시간 계산 (규칙: 점수 계산은 코드 알고리즘이, AI는 설명만).
 * 하루를 오전 9시 ~ 오후 10시, 30분 칸으로 나눠서 각 칸에 누가 비는지 센다.
 */
object MeetupScheduler {
    val DAY_START: LocalTime = LocalTime.of(9, 0)
    val DAY_END: LocalTime = LocalTime.of(22, 0)
    private const val STEP_MINUTES = 30L

    /** 오후 1시~7시 시작을 선호 (수업 끝난 뒤, 너무 늦지 않게) */
    private val PREFERRED_CENTER: LocalTime = LocalTime.of(15, 0)

    /** start~end 동안 일정이 하나도 안 겹치는 사람들 */
    fun freeMembers(members: List<MeetupMember>, date: LocalDate, start: LocalTime, end: LocalTime): List<MeetupMember> =
        members.filter { member ->
            member.busy.none { it.date == date && it.start < end && start < it.end }
        }

    /** 그날 전원이 비는 시간대 (30분 칸을 이어 붙인 것) */
    fun commonFreeRanges(members: List<MeetupMember>, date: LocalDate): List<TimeRange> {
        val ranges = mutableListOf<TimeRange>()
        var rangeStart: LocalTime? = null
        var t = DAY_START
        while (t < DAY_END) {
            val next = t.plusMinutes(STEP_MINUTES)
            val allFree = freeMembers(members, date, t, next).size == members.size
            if (allFree && rangeStart == null) rangeStart = t
            if (!allFree && rangeStart != null) {
                ranges += TimeRange(rangeStart, t)
                rangeStart = null
            }
            t = next
        }
        if (rangeStart != null) ranges += TimeRange(rangeStart, DAY_END)
        return ranges
    }

    /** 그날 durationMinutes 길이로 잡을 수 있는 가장 좋은 칸 (사람 수 → 선호 시간 순) */
    fun bestSlotOn(
        members: List<MeetupMember>,
        date: LocalDate,
        durationMinutes: Long,
        notBefore: LocalDateTime? = null,
    ): MeetupCandidate? {
        var best: MeetupCandidate? = null
        var bestScore = Long.MIN_VALUE
        var start = DAY_START
        while (!start.plusMinutes(durationMinutes).isAfter(DAY_END)) {
            val end = start.plusMinutes(durationMinutes)
            if (notBefore == null || date.atTime(start).isAfter(notBefore)) {
                val free = freeMembers(members, date, start, end)
                val score = free.size * 10_000L - abs(minutesBetween(PREFERRED_CENTER, start))
                if (free.isNotEmpty() && score > bestScore) {
                    bestScore = score
                    best = MeetupCandidate(date, start, end, free.map { it.id }, members.size)
                }
            }
            if (end == DAY_END) break
            start = start.plusMinutes(STEP_MINUTES)
        }
        return best
    }

    /**
     * 달력 색: 전원 가능 / 일부 가능(절반 이상) / 시간 겹침 / 미입력(지난 날)
     */
    fun dayAvailability(
        members: List<MeetupMember>,
        date: LocalDate,
        durationMinutes: Long,
        today: LocalDate,
    ): Availability {
        if (date.isBefore(today) || members.isEmpty()) return Availability.UNKNOWN
        val best = bestSlotOn(members, date, durationMinutes) ?: return Availability.NONE
        return when {
            best.isEveryone -> Availability.ALL
            best.availableCount * 2 >= members.size -> Availability.SOME
            else -> Availability.NONE
        }
    }

    /** 여러 날짜 중 추천 시간 limit개 (하루에 하나씩, 사람 많은 순 → 선호 시간 → 빠른 날짜) */
    fun recommend(
        members: List<MeetupMember>,
        dates: List<LocalDate>,
        durationMinutes: Long,
        now: LocalDateTime,
        limit: Int = 3,
    ): List<MeetupCandidate> =
        dates.filter { !it.isBefore(now.toLocalDate()) }
            .mapNotNull { bestSlotOn(members, it, durationMinutes, notBefore = now) }
            .sortedWith(
                compareByDescending<MeetupCandidate> { it.availableCount }
                    .thenBy { abs(minutesBetween(PREFERRED_CENTER, it.start)) }
                    .thenBy { it.date },
            )
            .take(limit)

    /** 희망 소요시간 슬라이더 범위 (30분 ~ 4시간+) */
    const val MIN_DURATION_MINUTES = 30L
    const val MAX_DURATION_MINUTES = 240L

    /** 슬라이더 값을 30분 단위로 맞추고 범위 안으로 자른다 */
    fun snapDuration(minutes: Long): Long {
        val rounded = ((minutes + STEP_MINUTES / 2) / STEP_MINUTES) * STEP_MINUTES
        return rounded.coerceIn(MIN_DURATION_MINUTES, MAX_DURATION_MINUTES)
    }

    /** 전원 가능 시간대 중 가장 긴 것을 30분 단위로 내림 (최대 4시간). 30분 미만이면 null */
    fun recommendedDuration(commonFree: List<TimeRange>): Long? {
        val longest = commonFree.maxOfOrNull { it.minutes } ?: return null
        val floored = (longest / STEP_MINUTES) * STEP_MINUTES
        return if (floored < MIN_DURATION_MINUTES) null else floored.coerceAtMost(MAX_DURATION_MINUTES)
    }

    private fun minutesBetween(a: LocalTime, b: LocalTime): Long =
        java.time.Duration.between(a, b).toMinutes()
}
