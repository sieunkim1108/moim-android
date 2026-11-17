package com.moim.app.core.reminder

import com.moim.app.core.data.ScheduleRepository
import com.moim.app.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 일정·알림 설정·대화 알림이 바뀔 때마다 휴대폰에 예약된 알림을 다시 맞춘다.
 * 앱이 켜질 때 MoimApplication에서 한 번 start() 한다.
 */
@Singleton
class ReminderSyncer @Inject constructor(
    private val authRepository: AuthRepository,
    private val scheduleRepository: ScheduleRepository,
    private val chatReminderStore: ChatReminderStore,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    // 앱을 막 켰을 때는 자동 로그인 확인 전이라 사용자가 잠깐 null이다.
    // 그때 예약을 지우면 안 되므로, 로그인했던 사용자가 "로그아웃"했을 때만 지운다.
    private var hadUser = false

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        authRepository.currentUser
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(null)
                } else {
                    combine(scheduleRepository.observeSchedules(), chatReminderStore.observe()) { schedules, chat ->
                        ReminderPlanner.plan(schedules, user.notificationPrefs, chat, LocalDateTime.now(clock))
                    }
                }
            }
            .debounce(DEBOUNCE_MS) // 일정을 여러 개 한꺼번에 등록할 때 한 번만 다시 예약
            .onEach { planned ->
                if (planned == null) {
                    if (hadUser) {
                        scheduler.cancelAll()
                        chatReminderStore.clear()
                    }
                    hadUser = false
                } else {
                    hadUser = true
                    scheduler.replaceAll(planned)
                }
            }
            .launchIn(scope)
    }

    private companion object {
        const val DEBOUNCE_MS = 500L
    }
}
