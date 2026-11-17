package com.moim.app.core.data

import com.moim.app.core.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메모리에만 저장하는 목업 일정 저장소 (앱을 끄면 초기화).
 * 새로 가입한 사용자처럼 빈 상태로 시작한다. (예시 일정 없음)
 */
@Singleton
class FakeScheduleRepository @Inject constructor() : ScheduleRepository {

    // 처음엔 비어 있고, 눈송이 대화나 + 버튼으로 등록한 일정만 쌓인다
    private val schedules = MutableStateFlow<List<Schedule>>(emptyList())

    override fun observeSchedules(): Flow<List<Schedule>> =
        schedules.asStateFlow().map { list -> list.sortedBy { it.startAt } }

    override fun observeSchedulesOn(date: LocalDate): Flow<List<Schedule>> =
        observeSchedules().map { list ->
            list.filter { !it.startAt.toLocalDate().isAfter(date) && !it.endAt.toLocalDate().isBefore(date) }
        }

    override suspend fun addSchedules(schedules: List<Schedule>): Result<Unit> {
        this.schedules.update { it + schedules }
        return Result.success(Unit)
    }

    override suspend fun deleteSchedule(id: String): Result<Unit> {
        schedules.update { list -> list.filterNot { it.id == id } }
        return Result.success(Unit)
    }
}
