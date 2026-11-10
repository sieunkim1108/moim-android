package com.moim.app.core.reminder

import com.moim.app.core.model.ChatReminder
import com.moim.app.core.model.NotificationPrefs
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderPlannerTest {

    // 2026-11-10(화) 오전 8시 기준
    private val now = LocalDateTime.of(2026, 11, 10, 8, 0)
    private val prefs = NotificationPrefs(examDaysBefore = 3, assignmentDaysBefore = 1, appointmentDaysBefore = 0)

    private fun schedule(
        id: String,
        type: ScheduleType,
        start: LocalDateTime,
        deadline: Boolean = false,
        custom: List<Int> = emptyList(),
    ) = Schedule(
        id = id,
        title = "제목$id",
        type = type,
        startAt = start,
        endAt = start.plusHours(1),
        isDeadline = deadline,
        reminderDaysBefore = custom,
    )

    @Test
    fun `시험은 프로필 설정대로 3일 전 오전 9시`() {
        val exam = schedule("1", ScheduleType.EXAM, LocalDateTime.of(2026, 11, 20, 10, 0))
        val result = ReminderPlanner.plan(listOf(exam), prefs, emptyList(), now)
        assertEquals(listOf(LocalDateTime.of(2026, 11, 17, 9, 0)), result.map { it.triggerAt })
        assertTrue(result.single().body.contains("D-3"))
    }

    @Test
    fun `일정에 따로 정한 알림이 있으면 그것만 쓴다`() {
        val exam = schedule("1", ScheduleType.EXAM, LocalDateTime.of(2026, 11, 20, 10, 0), custom = listOf(1, 7))
        val result = ReminderPlanner.plan(listOf(exam), prefs, emptyList(), now)
        assertEquals(
            listOf(LocalDateTime.of(2026, 11, 13, 9, 0), LocalDateTime.of(2026, 11, 19, 9, 0)),
            result.map { it.triggerAt },
        )
    }

    @Test
    fun `당일 약속은 시작 1시간 전, 지난 시각은 뺀다`() {
        val today = schedule("1", ScheduleType.APPOINTMENT, LocalDateTime.of(2026, 11, 10, 15, 0))
        val past = schedule("2", ScheduleType.APPOINTMENT, LocalDateTime.of(2026, 11, 10, 8, 30))
        val result = ReminderPlanner.plan(listOf(today, past), prefs, emptyList(), now)
        assertEquals(listOf(LocalDateTime.of(2026, 11, 10, 14, 0)), result.map { it.triggerAt })
    }

    @Test
    fun `수업은 따로 정하지 않으면 알리지 않는다`() {
        val lecture = schedule("1", ScheduleType.CLASS, LocalDateTime.of(2026, 11, 12, 9, 0))
        assertTrue(ReminderPlanner.plan(listOf(lecture), prefs, emptyList(), now).isEmpty())
    }

    @Test
    fun `대화 알림은 시작일부터 전날까지 매일, 일정 알림과 겹치는 날은 하나만`() {
        val exam = schedule("1", ScheduleType.EXAM, LocalDateTime.of(2026, 11, 20, 10, 0))
        val chat = ChatReminder(title = "제목1", eventDate = LocalDate.of(2026, 11, 20), from = LocalDate.of(2026, 11, 17))
        val result = ReminderPlanner.plan(listOf(exam), prefs, listOf(chat), now)
        // 17일(일정 알림) + 18, 19일(대화 알림)
        assertEquals(
            listOf(17, 18, 19),
            result.map { it.triggerAt.dayOfMonth },
        )
        assertEquals(1, result.count { it.triggerAt.dayOfMonth == 17 })
    }
}
