package com.moim.app.feature.chat.domain.usecase

import com.moim.app.core.data.ScheduleRepository
import com.moim.app.core.model.ChatReminder
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleSource
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.Visibility
import com.moim.app.core.reminder.ChatReminderStore
import com.moim.app.feature.chat.domain.model.ScheduleDraft
import javax.inject.Inject

/**
 * 확인 카드의 일정들을 실제로 저장한다.
 * 저장 직전에 한 번 더 검사: 날짜·시간이 빠진 일정은 저장하지 않는다.
 */
class RegisterDraftsUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) {
    suspend operator fun invoke(drafts: List<ScheduleDraft>, visibility: Visibility): Result<List<Schedule>> {
        val schedules = drafts.filter { it.isComplete }.map { it.toSchedule(visibility) }
        if (schedules.isEmpty()) return Result.failure(IllegalArgumentException("저장할 일정이 없어요"))
        return scheduleRepository.addSchedules(schedules).map { schedules }
    }

    private fun ScheduleDraft.toSchedule(visibility: Visibility): Schedule {
        val start = requireNotNull(date).atTime(requireNotNull(time))
        // 마감 일정은 시작=끝, 나머지는 기본 1시간 (수업은 75분)
        val end = when {
            isDeadline -> start
            type == ScheduleType.CLASS -> start.plusMinutes(75)
            else -> start.plusHours(1)
        }
        return Schedule(
            id = id,
            title = title,
            type = type,
            startAt = start,
            endAt = end,
            isDeadline = isDeadline,
            visibility = visibility,
            source = ScheduleSource.CHAT,
        )
    }
}

/** 눈송이의 알림 제안을 받아들였을 때: 준비 알림을 저장한다 (예약은 ReminderSyncer가 알아서 맞춘다) */
class SaveChatReminderUseCase @Inject constructor(
    private val store: ChatReminderStore,
) {
    suspend operator fun invoke(reminder: ChatReminder) = store.add(reminder)
}
