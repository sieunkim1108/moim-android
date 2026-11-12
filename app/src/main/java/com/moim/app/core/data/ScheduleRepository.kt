package com.moim.app.core.data

import com.moim.app.core.model.Schedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * 일정 저장소 약속. 챗봇(일정 등록), 캘린더(조회), 모임(확정 일정 등록)이 함께 쓴다.
 * Supabase 키가 있으면 SupabaseScheduleRepository, 없으면 FakeScheduleRepository(메모리).
 */
interface ScheduleRepository {
    /** 모든 내 일정 (바뀌면 자동으로 새 값이 흘러나옴) */
    fun observeSchedules(): Flow<List<Schedule>>

    /** 특정 날짜에 걸친 일정 */
    fun observeSchedulesOn(date: LocalDate): Flow<List<Schedule>>

    suspend fun addSchedules(schedules: List<Schedule>): Result<Unit>

    suspend fun deleteSchedule(id: String): Result<Unit>
}
