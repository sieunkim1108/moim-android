package com.moim.app.core.reminder

import com.moim.app.core.model.ChatReminder
import com.moim.app.core.model.NotificationPrefs
import com.moim.app.core.model.PlannedReminder
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleType
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 일정 목록 + 알림 설정 → "언제 무슨 알림을 보낼지" 계산 (순수 함수라 테스트하기 쉽다).
 *
 * 규칙
 * - 일정에 따로 정한 알림(reminderDaysBefore)이 있으면 그것만, 없으면 프로필 설정(시험/과제/약속)을 따른다.
 * - 수업(CLASS)·기타(ETC)는 따로 정하지 않으면 알리지 않는다 (시간표 수업이 매일 울리지 않게).
 * - N일 전 알림은 그날 오전 9시. 당일(0일 전) 알림은 시작 1시간 전, 하루 종일·마감 일정은 당일 오전 9시.
 * - 이미 지난 시각, [HORIZON_DAYS]일보다 먼 알림은 빼고, 가까운 순으로 최대 [MAX_COUNT]개만 예약한다.
 */
object ReminderPlanner {
    val MORNING: LocalTime = LocalTime.of(9, 0)
    const val HORIZON_DAYS = 60L
    const val MAX_COUNT = 64

    fun plan(
        schedules: List<Schedule>,
        prefs: NotificationPrefs,
        chatReminders: List<ChatReminder>,
        now: LocalDateTime,
    ): List<PlannedReminder> {
        val fromSchedules = schedules.flatMap { planSchedule(it, prefs) }
        // 일정 알림과 같은 날 같은 제목이면 대화 알림은 겹치지 않게 뺀다
        val taken = fromSchedules.map { it.title to it.triggerAt.toLocalDate() }.toSet()
        val fromChat = chatReminders.flatMap(::planChat)
            .filterNot { (it.title to it.triggerAt.toLocalDate()) in taken }

        val limit = now.plusDays(HORIZON_DAYS)
        return (fromSchedules + fromChat)
            .filter { it.triggerAt.isAfter(now) && !it.triggerAt.isAfter(limit) }
            .distinctBy { it.key }
            .sortedBy { it.triggerAt }
            .take(MAX_COUNT)
    }

    private fun planSchedule(schedule: Schedule, prefs: NotificationPrefs): List<PlannedReminder> {
        val days = schedule.reminderDaysBefore.ifEmpty { listOfNotNull(defaultDays(schedule.type, prefs)) }
        return days.distinct().filter { it >= 0 }.mapNotNull { d ->
            val eventDate = schedule.startAt.toLocalDate()
            val trigger = if (d == 0) {
                if (schedule.allDay || schedule.isDeadline) eventDate.atTime(MORNING) else schedule.startAt.minusHours(1)
            } else {
                eventDate.minusDays(d.toLong()).atTime(MORNING)
            }
            // 마감이 아닌 일정은 시작 뒤에 울리면 의미가 없다
            if (!schedule.isDeadline && trigger.isAfter(schedule.startAt)) return@mapNotNull null
            PlannedReminder(
                key = "s-${schedule.id}-$d",
                triggerAt = trigger,
                title = schedule.title,
                body = scheduleBody(schedule, d),
            )
        }
    }

    private fun planChat(reminder: ChatReminder): List<PlannedReminder> {
        val days = ChronoUnit.DAYS.between(reminder.from, reminder.eventDate)
        return (0 until days).map { offset ->
            val date = reminder.from.plusDays(offset)
            val left = ChronoUnit.DAYS.between(date, reminder.eventDate)
            PlannedReminder(
                key = "c-${reminder.title}-${reminder.eventDate}-$date",
                triggerAt = date.atTime(MORNING),
                title = reminder.title,
                body = "${reminder.title} D-$left! 오늘도 조금씩 준비하자 💙",
            )
        }
    }

    private fun defaultDays(type: ScheduleType, prefs: NotificationPrefs): Int? = when (type) {
        ScheduleType.EXAM -> prefs.examDaysBefore
        ScheduleType.ASSIGNMENT -> prefs.assignmentDaysBefore
        ScheduleType.APPOINTMENT, ScheduleType.MEETUP -> prefs.appointmentDaysBefore
        ScheduleType.CLASS, ScheduleType.ETC -> null
    }

    private val timeFormat = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

    private fun scheduleBody(schedule: Schedule, daysBefore: Int): String {
        val title = schedule.title
        return when {
            daysBefore == 0 && schedule.isDeadline -> "오늘까지 $title 마감이야! 잊지 마 💙"
            daysBefore == 0 && schedule.allDay -> "오늘 $title 있는 날이야 ☀️"
            daysBefore == 0 -> "${schedule.startAt.format(timeFormat)}에 $title 있어! 슬슬 준비하자 💙"
            schedule.type == ScheduleType.EXAM -> "$title D-$daysBefore! 오늘부터 같이 공부해 보자 💙"
            schedule.isDeadline || schedule.type == ScheduleType.ASSIGNMENT -> "$title 마감 D-$daysBefore! 미리 챙기자 💙"
            daysBefore == 1 -> "내일 $title 있어! 잊지 마 💙"
            else -> "$title D-$daysBefore! 💙"
        }
    }
}
