package com.moim.app.feature.chat.domain

import com.moim.app.core.model.ScheduleType
import com.moim.app.feature.chat.domain.model.ScheduleDraft
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * 구어체 메시지에서 일정 여러 개를 뽑는다 (AI 서버를 붙이기 전 규칙 기반 버전).
 * 예) "다음 주 월요일 10시에 데이터 분석 시험이야ㅠㅠ 과제도 금요일까지고"
 *   → [시험] 데이터 분석 11/9 10:00, [과제] 과제 제출 11/6 23:59
 */
object ScheduleExtractor {

    /** 과제 마감 시간이 없으면 그날 23:59로 */
    val DEFAULT_DEADLINE: LocalTime = LocalTime.of(23, 59)

    private data class Keyword(val word: String, val type: ScheduleType)

    // 앞에 있을수록 우선 (예: "팀플 과제" → 과제)
    private val keywords = listOf(
        Keyword("중간고사", ScheduleType.EXAM),
        Keyword("기말고사", ScheduleType.EXAM),
        Keyword("쪽지시험", ScheduleType.EXAM),
        Keyword("시험", ScheduleType.EXAM),
        Keyword("퀴즈", ScheduleType.EXAM),
        Keyword("과제", ScheduleType.ASSIGNMENT),
        Keyword("레포트", ScheduleType.ASSIGNMENT),
        Keyword("리포트", ScheduleType.ASSIGNMENT),
        Keyword("보고서", ScheduleType.ASSIGNMENT),
        Keyword("숙제", ScheduleType.ASSIGNMENT),
        Keyword("팀플", ScheduleType.MEETUP),
        Keyword("모임", ScheduleType.MEETUP),
        Keyword("회의", ScheduleType.MEETUP),
        Keyword("스터디", ScheduleType.MEETUP),
        Keyword("수업", ScheduleType.CLASS),
        Keyword("강의", ScheduleType.CLASS),
        Keyword("약속", ScheduleType.APPOINTMENT),
        Keyword("만나", ScheduleType.APPOINTMENT),
        Keyword("데이트", ScheduleType.APPOINTMENT),
        Keyword("병원", ScheduleType.APPOINTMENT),
        Keyword("알바", ScheduleType.APPOINTMENT),
        Keyword("영화", ScheduleType.APPOINTMENT),
        Keyword("밥", ScheduleType.APPOINTMENT),
    )

    // 문장을 일정 단위로 자르는 기준: ㅠㅠ, 문장부호, "그리고", "~고 " 연결
    private val segmentSplitter = Regex("[ㅠㅜ]+|[,.!?~;\\n]+|\\s그리고\\s|(?<=고)\\s+")

    private val fillerWords = setOf("나", "난", "나는", "저", "저는", "우리", "그리고", "또", "아", "헐", "헉", "진짜", "완전")
    private val particleOnly = setOf("에", "에는", "까지", "까지고", "부터", "도", "은", "는", "이", "가", "을", "를", "엔", "이야", "야")

    /** 메시지에 일정으로 볼 단어가 하나라도 있는지 */
    fun hasScheduleKeyword(text: String): Boolean = keywords.any { text.contains(it.word) }

    fun extract(text: String, today: LocalDate): List<ScheduleDraft> {
        val drafts = mutableListOf<ScheduleDraft>()
        var lastDate: LocalDate? = null

        for (segment in text.split(segmentSplitter).map { it.trim() }.filter { it.isNotEmpty() }) {
            val keyword = keywords.firstOrNull { segment.contains(it.word) } ?: continue
            // 날짜가 없으면 바로 앞 일정의 날짜를 이어받는다 ("내일 10시 시험이고 3시에 팀플")
            val date = KoreanDateParser.findDate(segment, today)?.value ?: lastDate
            val isDeadline = keyword.type == ScheduleType.ASSIGNMENT
            val time = KoreanDateParser.findTime(segment)?.value
                ?: if (isDeadline && date != null) DEFAULT_DEADLINE else null

            drafts += ScheduleDraft(
                id = UUID.randomUUID().toString(),
                title = buildTitle(segment, keyword),
                type = keyword.type,
                date = date,
                time = time,
                isDeadline = isDeadline,
            )
            if (date != null) lastDate = date
        }
        return drafts
    }

    /** 키워드 앞부분에서 날짜/시간/조사를 지우고 남은 말로 제목을 만든다 */
    private fun buildTitle(segment: String, keyword: Keyword): String {
        val masked = StringBuilder(segment)
        (KoreanDateParser.dateRanges(segment) + KoreanDateParser.timeRanges(segment)).forEach { range ->
            for (i in range) if (i < masked.length) masked.setCharAt(i, ' ')
        }
        val keywordIndex = masked.indexOf(keyword.word).coerceAtLeast(0)
        val prefix = masked.substring(0, keywordIndex)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in fillerWords && it !in particleOnly }
            .joinToString(" ")

        return when (keyword.type) {
            ScheduleType.EXAM ->
                if (keyword.word == "시험") prefix.ifBlank { "시험" } else "$prefix ${keyword.word}".trim()
            ScheduleType.ASSIGNMENT ->
                if (prefix.isBlank()) "${keyword.word} 제출" else "$prefix ${keyword.word}"
            ScheduleType.APPOINTMENT ->
                if (keyword.word == "만나") "$prefix 약속".trim() else "$prefix ${keyword.word}".trim()
            else -> "$prefix ${keyword.word}".trim()
        }
    }
}
