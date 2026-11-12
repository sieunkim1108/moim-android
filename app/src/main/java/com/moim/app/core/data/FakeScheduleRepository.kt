package com.moim.app.core.data

import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleSource
import com.moim.app.core.model.ScheduleType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메모리에만 저장하는 목업 일정 저장소 (앱을 끄면 초기화).
 * 화면 확인용으로 "오늘" 일정 2개를 미리 넣어 둔다.
 */
@Singleton
class FakeScheduleRepository @Inject constructor(
    clock: Clock,
) : ScheduleRepository {

    private val schedules = MutableStateFlow(sampleSchedules(LocalDate.now(clock)))

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

    private fun sampleSchedules(today: LocalDate): List<Schedule> = weeklyClasses(today) + listOf(
        Schedule(
            id = UUID.randomUUID().toString(),
            title = "인공지능 수업",
            type = ScheduleType.CLASS,
            startAt = today.atTime(LocalTime.of(13, 0)),
            endAt = today.atTime(LocalTime.of(14, 15)),
            source = ScheduleSource.TIMETABLE,
        ),
        Schedule(
            id = UUID.randomUUID().toString(),
            title = "지원이랑 저녁 약속",
            type = ScheduleType.APPOINTMENT,
            startAt = today.atTime(LocalTime.of(18, 30)),
            endAt = today.atTime(LocalTime.of(20, 0)),
            source = ScheduleSource.MANUAL,
        ),
        Schedule(
            id = UUID.randomUUID().toString(),
            title = "데이터마이닝 3차 과제 제출",
            type = ScheduleType.ASSIGNMENT,
            startAt = today.plusDays(1).atTime(LocalTime.of(23, 59)),
            endAt = today.plusDays(1).atTime(LocalTime.of(23, 59)),
            isDeadline = true,
            source = ScheduleSource.MANUAL,
            location = "스노우보드 온라인 제출",
        ),
        Schedule(
            id = UUID.randomUUID().toString(),
            title = "시은이랑 점심",
            type = ScheduleType.APPOINTMENT,
            startAt = today.plusDays(3).atTime(LocalTime.of(12, 30)),
            endAt = today.plusDays(3).atTime(LocalTime.of(13, 30)),
            source = ScheduleSource.MANUAL,
            location = "학생회관 지하 샐러디",
        ),
    )

    /** 내 시간표 예시: 이번 주부터 8주 동안 화·목 데이터사이언스 수업 */
    private fun weeklyClasses(today: LocalDate): List<Schedule> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L until 8L).flatMap { week ->
            listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY).map { day ->
                val date = monday.plusWeeks(week).with(TemporalAdjusters.nextOrSame(day))
                Schedule(
                    id = "my-class-$date",
                    title = "데이터사이언스",
                    type = ScheduleType.CLASS,
                    startAt = date.atTime(LocalTime.of(10, 0)),
                    endAt = date.atTime(LocalTime.of(11, 30)),
                    source = ScheduleSource.TIMETABLE,
                    location = "명신관 302호",
                )
            }
        }
    }
}
