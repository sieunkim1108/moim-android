package com.moim.app.core.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.moim.app.core.model.PlannedReminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager로 알림을 예약한다. 앱이 꺼져 있거나 휴대폰을 껐다 켜도 예약이 유지된다.
 * 정확히 그 분에 울리는 알람(AlarmManager exact)은 특별 권한이 필요해서 쓰지 않는다.
 * 대신 몇 분 정도 늦게 울릴 수 있다.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
) : ReminderScheduler {

    private val workManager get() = WorkManager.getInstance(context)

    override fun replaceAll(reminders: List<PlannedReminder>) {
        // WorkManager 작업은 넣은 순서대로 처리되므로, 전부 지운 뒤 새로 넣어도 안전하다
        workManager.cancelAllWorkByTag(TAG)
        val now = ZonedDateTime.now(clock)
        reminders.forEach { reminder ->
            val delay = Duration.between(now, reminder.triggerAt.atZone(clock.zone)).toMillis()
            if (delay <= 0) return@forEach
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_ID to reminder.key,
                        ReminderWorker.KEY_TITLE to reminder.title,
                        ReminderWorker.KEY_BODY to reminder.body,
                    ),
                )
                .addTag(TAG)
                .build()
            workManager.enqueueUniqueWork(reminder.key, ExistingWorkPolicy.REPLACE, request)
        }
    }

    override fun cancelAll() {
        workManager.cancelAllWorkByTag(TAG)
    }

    private companion object {
        const val TAG = "moim-reminder"
    }
}
