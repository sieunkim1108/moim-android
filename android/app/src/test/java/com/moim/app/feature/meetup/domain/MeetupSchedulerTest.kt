package com.moim.app.feature.meetup.domain

import com.moim.app.core.model.Availability
import com.moim.app.feature.meetup.domain.model.BusyBlock
import com.moim.app.feature.meetup.domain.model.MeetupMember
import com.moim.app.feature.meetup.domain.model.TimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MeetupSchedulerTest {

    private val day = LocalDate.of(2026, 11, 12)
    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)
    private fun busy(from: Int, to: Int, date: LocalDate = day) = BusyBlock(date, t(from), t(to), "수업")

    private val me = MeetupMember("me", "나", true, listOf(busy(9, 12), busy(17, 22)))
    private val sieun = MeetupMember("s", "시은", false, listOf(busy(9, 13)))
    private val jiwon = MeetupMember("j", "지원", false, listOf(busy(12, 15)))

    @Test
    fun commonFreeRangeIsIntersection() {
        // 나: 12~17 비움, 시은: 13~ 비움, 지원: 15~ 비움 → 15~17
        val ranges = MeetupScheduler.commonFreeRanges(listOf(me, sieun, jiwon), day)
        assertEquals(listOf(TimeRange(t(15), t(17))), ranges)
    }

    @Test
    fun bestSlotPrefersEveryone() {
        val best = MeetupScheduler.bestSlotOn(listOf(me, sieun, jiwon), day, 120)!!
        assertEquals(t(15), best.start)
        assertEquals(t(17), best.end)
        assertTrue(best.isEveryone)
    }

    @Test
    fun dayAvailabilityLevels() {
        val today = day
        assertEquals(Availability.ALL, MeetupScheduler.dayAvailability(listOf(me, sieun, jiwon), day, 120, today))
        // 3시간짜리는 전원 불가 → 2명 이상 가능하면 일부 가능
        assertEquals(Availability.SOME, MeetupScheduler.dayAvailability(listOf(me, sieun, jiwon), day, 180, today))
        assertEquals(Availability.UNKNOWN, MeetupScheduler.dayAvailability(listOf(me), day.minusDays(1), 60, today))
    }

    @Test
    fun recommendSkipsPastTimesAndSortsByPeople() {
        val tomorrow = day.plusDays(1)
        val now = LocalDateTime.of(day, t(16)) // 오늘 15~17 슬롯은 이미 지남
        val result = MeetupScheduler.recommend(listOf(me, sieun, jiwon), listOf(day, tomorrow), 120, now)
        // 내일은 아무 일정이 없으니 전원 가능, 오후 3시 시작이 선호
        assertEquals(tomorrow, result.first().date)
        assertEquals(t(15), result.first().start)
        assertTrue(result.first().isEveryone)
    }

    @Test
    fun snapDuration_roundsTo30MinutesWithinRange() {
        assertEquals(30L, MeetupScheduler.snapDuration(10))
        assertEquals(60L, MeetupScheduler.snapDuration(50))
        assertEquals(90L, MeetupScheduler.snapDuration(89))
        assertEquals(240L, MeetupScheduler.snapDuration(300))
    }

    @Test
    fun recommendedDuration_isLongestCommonFreeFloored() {
        val ranges = listOf(
            TimeRange(LocalTime.of(10, 0), LocalTime.of(11, 0)),
            TimeRange(LocalTime.of(15, 0), LocalTime.of(17, 20)), // 2시간 20분 → 2시간
        )
        assertEquals(120L, MeetupScheduler.recommendedDuration(ranges))
        assertEquals(240L, MeetupScheduler.recommendedDuration(listOf(TimeRange(LocalTime.of(9, 0), LocalTime.of(22, 0)))))
        assertNull(MeetupScheduler.recommendedDuration(listOf(TimeRange(LocalTime.of(9, 0), LocalTime.of(9, 20)))))
        assertNull(MeetupScheduler.recommendedDuration(emptyList()))
    }
}
