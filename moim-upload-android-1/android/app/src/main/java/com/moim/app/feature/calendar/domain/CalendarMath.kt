package com.moim.app.feature.calendar.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 달력 계산 (안드로이드와 무관한 순수 Kotlin → 단위 테스트로 검증).
 * 한 주는 일요일부터 토요일까지 (Stitch 디자인: 일 월 화 수 목 금 토).
 */
object CalendarMath {

    /** date가 속한 주의 일요일 */
    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

    /** date가 속한 주 7일 (일~토) */
    fun weekOf(date: LocalDate): List<LocalDate> {
        val start = weekStart(date)
        return (0L until 7L).map { start.plusDays(it) }
    }

    /**
     * 월 달력 칸. 앞뒤 빈칸은 이전/다음 달 날짜로 채워 항상 7의 배수 개를 돌려준다.
     * (화면에서 다른 달 날짜는 흐리게 표시)
     */
    fun monthGrid(month: YearMonth): List<LocalDate> {
        val first = weekStart(month.atDay(1))
        val last = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
        val days = ChronoUnit.DAYS.between(first, last) + 1
        return (0L until days).map { first.plusDays(it) }
    }
}

/** "2학기 11주차" 표시용 */
data class AcademicWeek(val semester: Int, val week: Int)

/**
 * 학기 주차 계산. 1학기는 3월 1일, 2학기는 9월 1일이 속한 주를 1주차로 센다 (16주).
 * 방학 기간이면 null.
 * TODO: 학교별 실제 개강일을 서버 설정에서 받아오기
 */
object AcademicCalendar {
    private const val WEEKS_PER_SEMESTER = 16

    fun weekOf(date: LocalDate): AcademicWeek? {
        val candidates = listOf(
            1 to LocalDate.of(date.year, Month.MARCH, 1),
            2 to LocalDate.of(date.year, Month.SEPTEMBER, 1),
        )
        for ((semester, opening) in candidates) {
            val start = CalendarMath.weekStart(opening)
            val days = ChronoUnit.DAYS.between(start, CalendarMath.weekStart(date))
            if (days < 0) continue
            val week = (days / 7).toInt() + 1
            if (week <= WEEKS_PER_SEMESTER) return AcademicWeek(semester, week)
        }
        return null
    }
}
