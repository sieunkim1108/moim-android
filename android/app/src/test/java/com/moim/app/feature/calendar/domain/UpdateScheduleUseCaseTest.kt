package com.moim.app.feature.calendar.domain

import com.moim.app.core.data.FakeScheduleRepository
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleSource
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.Visibility
import com.moim.app.feature.calendar.domain.usecase.NewSchedule
import com.moim.app.feature.calendar.domain.usecase.UpdateScheduleUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class UpdateScheduleUseCaseTest {

    private val start = LocalDateTime.of(2026, 11, 26, 13, 30)

    private val original = Schedule(
        id = "s1",
        title = "모소 팀플",
        type = ScheduleType.APPOINTMENT,
        startAt = start,
        endAt = start.plusHours(2), // 2시간짜리
        visibility = Visibility.FRIENDS,
        source = ScheduleSource.CHAT,
        memo = "챗봇에서 등록",
        location = "명신관 201호",
        reminderDaysBefore = listOf(1),
    )

    private fun input(
        title: String = "모소 팀플 회의",
        type: ScheduleType = ScheduleType.APPOINTMENT,
        startAt: LocalDateTime = start.plusDays(1),
        visibility: Visibility = Visibility.FRIENDS,
        rooms: Set<String> = emptySet(),
    ) = NewSchedule(
        title = title,
        type = type,
        startAt = startAt,
        location = "  순헌관  ",
        reminderDaysBefore = listOf(3, 1, 3),
        visibility = visibility,
        sharedRoomIds = rooms,
    )

    private fun repoWithOriginal() = FakeScheduleRepository().apply {
        runBlocking { addSchedules(listOf(original)) }
    }

    @Test
    fun updatesFieldsAndKeepsIdentity() = runBlocking {
        val repo = repoWithOriginal()
        val result = UpdateScheduleUseCase(repo)(original, input()).getOrThrow()

        assertEquals("s1", result.id)
        assertEquals(ScheduleSource.CHAT, result.source) // 만든 곳은 그대로
        assertEquals("챗봇에서 등록", result.memo) // 메모도 그대로
        assertEquals("모소 팀플 회의", result.title)
        assertEquals("순헌관", result.location) // 앞뒤 공백 정리
        assertEquals(listOf(1, 3), result.reminderDaysBefore) // 중복 제거 + 정렬
        assertEquals(result, repo.observeSchedules().first().single())
    }

    @Test
    fun keepsOriginalDuration() = runBlocking {
        val result = UpdateScheduleUseCase(repoWithOriginal())(original, input()).getOrThrow()
        assertEquals(result.startAt.plusHours(2), result.endAt)
    }

    @Test
    fun becomesDeadlineWhenTypeIsAssignment() = runBlocking {
        val result = UpdateScheduleUseCase(repoWithOriginal())(original, input(type = ScheduleType.ASSIGNMENT)).getOrThrow()
        assertTrue(result.isDeadline)
        assertEquals(result.startAt, result.endAt)
    }

    @Test
    fun deadlineToAppointmentGetsOneHour() = runBlocking {
        val deadline = original.copy(type = ScheduleType.ASSIGNMENT, endAt = start, isDeadline = true)
        val repo = FakeScheduleRepository().apply { addSchedules(listOf(deadline)) }
        val result = UpdateScheduleUseCase(repo)(deadline, input()).getOrThrow()
        assertEquals(false, result.isDeadline)
        assertEquals(result.startAt.plusHours(1), result.endAt)
    }

    @Test
    fun dropsRoomsWhenNotGroup() = runBlocking {
        val grouped = original.copy(visibility = Visibility.GROUP, sharedRoomIds = setOf("room1"))
        val repo = FakeScheduleRepository().apply { addSchedules(listOf(grouped)) }
        val result = UpdateScheduleUseCase(repo)(grouped, input(visibility = Visibility.PRIVATE, rooms = setOf("room1"))).getOrThrow()
        assertTrue(result.sharedRoomIds.isEmpty())
    }

    @Test
    fun rejectsBlankTitleAndEmptyGroup() = runBlocking {
        val useCase = UpdateScheduleUseCase(repoWithOriginal())
        assertTrue(useCase(original, input(title = "  ")).isFailure)
        assertTrue(useCase(original, input(visibility = Visibility.GROUP)).isFailure)
    }

    @Test
    fun failsWhenScheduleIsMissing() = runBlocking {
        val result = UpdateScheduleUseCase(FakeScheduleRepository())(original, input())
        assertTrue(result.isFailure)
    }
}
