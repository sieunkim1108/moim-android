package com.moim.app

import android.app.Application
import com.moim.app.core.reminder.ReminderNotifications
import com.moim.app.core.reminder.ReminderSyncer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 앱 전체의 시작점.
 * @HiltAndroidApp 을 붙이면 Hilt가 앱 전체에서 쓸 의존성 그래프(객체 공장)를 만든다.
 */
@HiltAndroidApp
class MoimApplication : Application() {

    @Inject
    lateinit var reminderSyncer: ReminderSyncer

    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.createChannel(this)
        reminderSyncer.start() // 일정이 바뀌면 알림 예약도 자동으로 맞춘다
    }
}
