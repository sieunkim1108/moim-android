package com.moim.app.core.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** 예약한 시각이 되면 WorkManager가 실행: 알림 하나를 띄운다 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val body = inputData.getString(KEY_BODY) ?: return Result.failure()
        ReminderNotifications.show(applicationContext, id, body)
        return Result.success()
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
