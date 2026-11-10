package com.moim.app.feature.calendar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarMathTest {

    @Test
    fun weekStartsOnSunday() {
        // 2026-11-12(목) → 그 주 일요일은 11-08 (Stitch 디자인의 8~14)
        val week = CalendarMath.weekOf(LocalDate.of(2026, 11, 12))
        assertEquals(LocalDate.of(2026, 11, 8), week.first())
        assertEquals(LocalDate.of(2026, 11, 14), week.last())
        assertEquals(DayOfWeek.SUNDAY, week.first().dayOfWeek)
    }

    @Test
    fun monthGridCoversWholeWeeks() {
        val grid = CalendarMath.monthGrid(YearMonth.of(2026, 11))
        // 2026-11-01은 일요일, 11-30은 월요일 → 11/1 ~ 12/5, 5주
        assertEquals(35, grid.size)
        assertEquals(LocalDate.of(2026, 11, 1), grid.first())
        assertEquals(LocalDate.of(2026, 12, 5), grid.last())
        assertEquals(0, grid.size % 7)
    }

    @Test
    fun monthGridStartingMidWeek() {
        // 2026-10-01은 목요일 → 9/27(일)부터 시작
        val grid = CalendarMath.monthGrid(YearMonth.of(2026, 10))
        assertEquals(LocalDate.of(2026, 9, 27), grid.first())
        assertEquals(LocalDate.of(2026, 10, 31), grid.last())
    }

    @Test
    fun academicWeekMatchesDesign() {
        // Stitch 디자인: 11/8~14 주 = 2학기 11주차
        assertEquals(AcademicWeek(2, 11), AcademicCalendar.weekOf(LocalDate.of(2026, 11, 12)))
        assertEquals(AcademicWeek(2, 1), AcademicCalendar.weekOf(LocalDate.of(2026, 9, 1)))
        assertEquals(AcademicWeek(1, 1), AcademicCalendar.weekOf(LocalDate.of(2026, 3, 3)))
    }

    @Test
    fun vacationHasNoWeek() {
        assertNull(AcademicCalendar.weekOf(LocalDate.of(2026, 1, 15)))
        assertNull(AcademicCalendar.weekOf(LocalDate.of(2026, 7, 20)))
    }
}
