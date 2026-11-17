package com.moim.app.feature.chat.data

import com.moim.app.core.model.ScheduleType
import com.moim.app.feature.chat.domain.model.BotOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AiServerProtocolTest {

    private fun sse(vararg events: Pair<String, String>): Sequence<String> =
        events.flatMap { (name, data) -> listOf("event: $name", "data: $data", "") }.asSequence()

    @Test
    fun `등록 응답은 공감 문장과 확인 카드`() {
        val lines = sse(
            "intent" to """{"intent": "register"}""",
            "reply" to """{"text": "시험 화이팅!"}""",
            "card" to """{"card_type": "schedule_confirm", "schedules": [
                {"title": "데이터 분석 시험", "type": "exam", "date": "2026-11-09", "start_time": "10:00:00",
                 "end_time": null, "all_day": false, "is_deadline": false, "location": null, "reminder_days_before": [7]},
                {"title": "과제 제출", "type": "assignment", "date": "2026-11-13", "start_time": null,
                 "all_day": false, "is_deadline": true}]}""".replace("\n", ""),
            "done" to "{}",
        )
        val turn = AiServerProtocol.toBotTurn(AiServerProtocol.parseSse(lines))!!
        assertEquals("시험 화이팅!", (turn.outputs[0] as BotOutput.Text).text)
        val drafts = (turn.outputs[1] as BotOutput.ConfirmCard).drafts
        assertEquals(ScheduleType.EXAM, drafts[0].type)
        assertEquals(LocalDate.of(2026, 11, 9), drafts[0].date)
        assertEquals(LocalTime.of(10, 0), drafts[0].time)
        // 시간 없는 마감은 23:59
        assertEquals(LocalTime.of(23, 59), drafts[1].time)
        assertTrue(drafts.all { it.isComplete })
    }

    @Test
    fun `대화는 delta를 이어 붙인다`() {
        val lines = sse(
            "intent" to """{"intent": "chat"}""",
            "delta" to """{"text": "안녕"}""",
            "delta" to """{"text": "! 오늘도 화이팅"}""",
            "done" to "{}",
        )
        val turn = AiServerProtocol.toBotTurn(AiServerProtocol.parseSse(lines))!!
        assertEquals(1, turn.outputs.size)
        assertEquals("안녕! 오늘도 화이팅", (turn.outputs[0] as BotOutput.Text).text)
    }

    @Test
    fun `되묻기는 시간 빠른답장과 함께`() {
        val lines = sse("clarify" to """{"question": "몇 시야?"}""", "done" to "{}")
        val text = AiServerProtocol.toBotTurn(AiServerProtocol.parseSse(lines))!!.outputs.single() as BotOutput.Text
        assertEquals("몇 시야?", text.text)
        assertEquals(3, text.quickReplies.size)
    }

    @Test
    fun `서버 오류나 빈 응답이면 null로 규칙 기반 눈송이에게 넘긴다`() {
        assertNull(AiServerProtocol.toBotTurn(AiServerProtocol.parseSse(sse("error" to """{"text": "앗"}"""))))
        assertNull(AiServerProtocol.toBotTurn(AiServerProtocol.parseSse(sse("done" to "{}"))))
    }

    @Test
    fun `keep-alive 주석과 CRLF 줄바꿈을 무시한다`() {
        val lines = sequenceOf(": ping", "event: reply\r", "data: {\"text\": \"응\"}\r", "\r")
        val turn = AiServerProtocol.toBotTurn(AiServerProtocol.parseSse(lines))!!
        assertEquals("응", (turn.outputs.single() as BotOutput.Text).text)
    }
}
