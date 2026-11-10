package com.moim.app.core.reminder

import com.moim.app.core.model.PlannedReminder

/** 알림 예약 담당. 지금은 WorkManager 구현 하나 (테스트에서는 가짜로 바꿔 끼울 수 있다). */
interface ReminderScheduler {
    /** 예약된 알림을 [reminders]로 통째로 바꾼다 */
    fun replaceAll(reminders: List<PlannedReminder>)

    /** 예약된 알림을 모두 지운다 (로그아웃) */
    fun cancelAll()
}
