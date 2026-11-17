package com.moim.app.feature.chat.domain

import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/**
 * "담주 월욜 10시", "금요일까지", "내일 오후 3시 반" 같은 한국어 날짜/시간 표현을 실제 날짜로 바꾼다.
 *
 * 프로젝트 규칙: AI가 날짜를 해석해도 실제 날짜 계산은 코드로 다시 한다.
 * → 나중에 AI 서버를 붙여도 이 파서로 AI가 준 날짜를 검증할 수 있다.
 * 안드로이드에 의존하지 않는 순수 Kotlin이라 단위 테스트로 확인한다 (KoreanDateParserTest).
 */
object KoreanDateParser {

    /** 해석 결과. range는 원문에서 날짜/시간 표현이 차지한 위치 (제목을 뽑을 때 지우는 용도) */
    data class Match<T>(val value: T, val range: IntRange)

    private val weekdays = mapOf(
        '월' to DayOfWeek.MONDAY, '화' to DayOfWeek.TUESDAY, '수' to DayOfWeek.WEDNESDAY,
        '목' to DayOfWeek.THURSDAY, '금' to DayOfWeek.FRIDAY, '토' to DayOfWeek.SATURDAY,
        '일' to DayOfWeek.SUNDAY,
    )

    private val monthDayRegex = Regex("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")
    private val slashDateRegex = Regex("(?<![\\d/])(\\d{1,2})/(\\d{1,2})(?![\\d/])")
    private val inDaysRegex = Regex("(\\d{1,2})\\s*일\\s*(?:뒤|후)")
    private val relativeDayRegex = Regex("(오늘|내일|낼|모레|글피)")
    private val weekdayRegex =
        Regex("(?:(다다음\\s?주|다음\\s?주|담주|이번\\s?주|저번\\s?주)\\s*)?([월화수목금토일])(?:요일|욜)")

    private val clockTimeRegex = Regex("(오전|오후|아침|낮|저녁|밤|새벽)?\\s*(\\d{1,2}):(\\d{2})")
    private val koreanTimeRegex =
        Regex("(오전|오후|아침|낮|저녁|밤|새벽)?\\s*(\\d{1,2})\\s*시(?![간험])(?:\\s*(반)|\\s*(\\d{1,2})\\s*분)?")
    private val noonRegex = Regex("정오")

    /**
     * 문장에서 날짜를 찾는다. 여러 표현이 있으면 더 구체적인 것(11월 9일 > 11/9 > 내일 > 금요일) 우선.
     * 같이 쓰인 다른 날짜 표현의 위치도 consumed에 담아 제목에서 지울 수 있게 한다.
     */
    fun findDate(text: String, today: LocalDate): Match<LocalDate>? {
        val candidates = mutableListOf<Match<LocalDate>>()

        monthDayRegex.findAll(text).forEach { m ->
            val (month, day) = m.destructured
            safeDate(today, month.toInt(), day.toInt())?.let { candidates += Match(it, m.range) }
        }
        slashDateRegex.findAll(text).forEach { m ->
            val (month, day) = m.destructured
            safeDate(today, month.toInt(), day.toInt())?.let { candidates += Match(it, m.range) }
        }
        relativeDayRegex.findAll(text).forEach { m ->
            val date = when (m.value) {
                "오늘" -> today
                "내일", "낼" -> today.plusDays(1)
                "모레" -> today.plusDays(2)
                else -> today.plusDays(3) // 글피
            }
            candidates += Match(date, m.range)
        }
        inDaysRegex.findAll(text).forEach { m ->
            candidates += Match(today.plusDays(m.groupValues[1].toLong()), m.range)
        }
        weekdayRegex.findAll(text).forEach { m ->
            val dayOfWeek = weekdays.getValue(m.groupValues[2].single())
            candidates += Match(resolveWeekday(today, dayOfWeek, m.groupValues[1]), m.range)
        }

        if (candidates.isEmpty()) return null
        // 처음 등록한 순서(구체적인 표현)가 우선
        return candidates.first()
    }

    /** 문장 속 모든 날짜 표현의 위치 (제목에서 지울 때 사용) */
    fun dateRanges(text: String): List<IntRange> =
        listOf(monthDayRegex, slashDateRegex, relativeDayRegex, inDaysRegex, weekdayRegex)
            .flatMap { regex -> regex.findAll(text).map { it.range }.toList() }

    /** 문장에서 시간을 찾는다. 오전/오후가 없고 1~7시면 오후로 본다 (예: "3시 수업" = 15시). */
    fun findTime(text: String): Match<LocalTime>? {
        noonRegex.find(text)?.let { return Match(LocalTime.NOON, it.range) }

        clockTimeRegex.find(text)?.let { m ->
            val hour = m.groupValues[2].toInt()
            val minute = m.groupValues[3].toInt()
            return toTime(m.groupValues[1], hour, minute)?.let { Match(it, m.range) }
        }
        koreanTimeRegex.find(text)?.let { m ->
            val hour = m.groupValues[2].toInt()
            val minute = when {
                m.groupValues[3].isNotEmpty() -> 30
                m.groupValues[4].isNotEmpty() -> m.groupValues[4].toInt()
                else -> 0
            }
            return toTime(m.groupValues[1], hour, minute)?.let { Match(it, m.range) }
        }
        return null
    }

    fun timeRanges(text: String): List<IntRange> =
        listOf(noonRegex, clockTimeRegex, koreanTimeRegex)
            .flatMap { regex -> regex.findAll(text).map { it.range }.toList() }

    /** "다음 주 월요일" 같은 요일 표현 → 날짜. 주는 월요일부터 시작한다. */
    internal fun resolveWeekday(today: LocalDate, dayOfWeek: DayOfWeek, qualifier: String): LocalDate {
        val mondayThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return when (qualifier.replace(" ", "")) {
            "다음주", "담주" -> mondayThisWeek.plusWeeks(1).with(TemporalAdjusters.nextOrSame(dayOfWeek))
            "다다음주" -> mondayThisWeek.plusWeeks(2).with(TemporalAdjusters.nextOrSame(dayOfWeek))
            "이번주" -> mondayThisWeek.with(TemporalAdjusters.nextOrSame(dayOfWeek))
            "저번주" -> mondayThisWeek.minusWeeks(1).with(TemporalAdjusters.nextOrSame(dayOfWeek))
            else -> today.with(TemporalAdjusters.nextOrSame(dayOfWeek)) // 그냥 "금요일" = 오늘 포함 다가오는 금요일
        }
    }

    private fun toTime(period: String, rawHour: Int, minute: Int): LocalTime? {
        if (rawHour > 24 || minute > 59) return null
        val hour = when (period) {
            "오전", "아침", "새벽" -> if (rawHour == 12) 0 else rawHour
            "오후", "낮", "저녁", "밤" -> if (rawHour < 12) rawHour + 12 else rawHour
            else -> if (rawHour in 1..7) rawHour + 12 else rawHour % 24
        }
        return LocalTime.of(hour % 24, minute)
    }

    /** 월/일 → 날짜. 한 달 넘게 지난 날짜면 내년으로 본다. 없는 날짜(2/30)는 null. */
    private fun safeDate(today: LocalDate, month: Int, day: Int): LocalDate? = try {
        val thisYear = LocalDate.of(today.year, month, day)
        if (thisYear.isBefore(today.minusDays(30))) thisYear.plusYears(1) else thisYear
    } catch (e: DateTimeException) {
        null
    }
}
