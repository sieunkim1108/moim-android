package com.moim.app.feature.chat.domain

import com.moim.app.core.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** 기준일: 2026-11-02 (월) */
class KoreanDateParserTest {

    private val today = LocalDate.of(2026, 11, 2)

    private fun date(text: String) = KoreanDateParser.findDate(text, today)?.value
    private fun time(text: String) = KoreanDateParser.findTime(text)?.value

    @Test
    fun relativeDays() {
        assertEquals(today, date("오늘 저녁에"))
        assertEquals(LocalDate.of(2026, 11, 3), date("내일 수업"))
        assertEquals(LocalDate.of(2026, 11, 4), date("모레 약속"))
        assertEquals(LocalDate.of(2026, 11, 9), date("7일 뒤에"))
    }

    @Test
    fun weekdays() {
        assertEquals(LocalDate.of(2026, 11, 6), date("금요일까지"))
        assertEquals(LocalDate.of(2026, 11, 6), date("금욜까지"))
        assertEquals(today, date("월요일"))
        assertEquals(LocalDate.of(2026, 11, 9), date("다음 주 월요일"))
        assertEquals(LocalDate.of(2026, 11, 9), date("담주 월욜"))
        assertEquals(LocalDate.of(2026, 11, 13), date("다음주 금요일"))
        assertEquals(LocalDate.of(2026, 11, 16), date("다다음 주 월요일"))
        assertEquals(LocalDate.of(2026, 11, 8), date("이번 주 일요일"))
    }

    @Test
    fun absoluteDates() {
        assertEquals(LocalDate.of(2026, 11, 20), date("11월 20일"))
        assertEquals(LocalDate.of(2026, 12, 3), date("12/3"))
        assertEquals(LocalDate.of(2027, 1, 5), date("1월 5일")) // 지난 날짜면 내년
        assertNull(date("2월 30일"))
    }

    @Test
    fun times() {
        assertEquals(LocalTime.of(10, 0), time("10시에"))
        assertEquals(LocalTime.of(15, 0), time("3시 수업"))       // 오전/오후 없으면 1~7시는 오후
        assertEquals(LocalTime.of(9, 0), time("오전 9시"))
        assertEquals(LocalTime.of(21, 30), time("밤 9시 반"))
        assertEquals(LocalTime.of(14, 20), time("2시 20분"))
        assertEquals(LocalTime.of(23, 59), time("23:59까지"))
        assertEquals(LocalTime.NOON, time("정오에"))
        assertNull(time("3시간 동안"))
        assertNull(time("데이터 분석 시험"))
    }

    @Test
    fun extractMultipleSchedulesFromOneMessage() {
        val drafts = ScheduleExtractor.extract("다음 주 월요일 10시에 데이터 분석 시험이야ㅠㅠ 과제도 금요일까지고", today)

        assertEquals(2, drafts.size)
        assertEquals(ScheduleType.EXAM, drafts[0].type)
        assertEquals("데이터 분석", drafts[0].title)
        assertEquals(LocalDate.of(2026, 11, 9), drafts[0].date)
        assertEquals(LocalTime.of(10, 0), drafts[0].time)

        assertEquals(ScheduleType.ASSIGNMENT, drafts[1].type)
        assertEquals("과제 제출", drafts[1].title)
        assertEquals(LocalDate.of(2026, 11, 6), drafts[1].date)
        assertEquals(LocalTime.of(23, 59), drafts[1].time)
    }

    @Test
    fun secondScheduleInheritsDate() {
        val drafts = ScheduleExtractor.extract("내일 10시에 알고리즘 시험이고 3시에 팀플 회의", today)
        assertEquals(2, drafts.size)
        assertEquals("알고리즘", drafts[0].title)
        assertEquals(LocalDate.of(2026, 11, 3), drafts[1].date)
        assertEquals(LocalTime.of(15, 0), drafts[1].time)
        assertEquals(ScheduleType.MEETUP, drafts[1].type)
    }

    @Test
    fun missingTimeStaysNull() {
        val drafts = ScheduleExtractor.extract("목요일에 중간고사 있어", today)
        assertEquals(1, drafts.size)
        assertEquals(LocalDate.of(2026, 11, 5), drafts[0].date)
        assertNull(drafts[0].time) // 시간을 추측하지 않고 되묻는다
    }
}
