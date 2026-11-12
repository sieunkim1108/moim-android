package com.moim.app.feature.calendar.domain.usecase

import com.moim.app.core.data.FriendRepository
import com.moim.app.core.data.ScheduleRepository
import com.moim.app.core.model.Friend
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleSource
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.ShareGroup
import com.moim.app.core.model.Visibility
import com.moim.app.feature.dm.domain.DmRepository
import com.moim.app.feature.dm.domain.model.RoomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * 일정을 날짜별로 묶어서 본다 (달력 점 표시 + 하루 목록).
 * ownerId가 null이면 내 일정, 아니면 그 친구의 일정 (비공개는 이미 가려진 채로 옴)
 */
class ObserveSchedulesByDateUseCase @Inject constructor(
    private val repository: ScheduleRepository,
    private val friendRepository: FriendRepository,
) {
    operator fun invoke(ownerId: String? = null): Flow<Map<LocalDate, List<Schedule>>> {
        val source = if (ownerId == null) {
            repository.observeSchedules()
        } else {
            friendRepository.observeFriendSchedules(ownerId)
        }
        return source.map { list -> groupByDate(list) }
    }

    companion object {
        /** 여러 날에 걸친 일정은 걸친 날마다 넣는다 */
        fun groupByDate(list: List<Schedule>): Map<LocalDate, List<Schedule>> {
            val result = mutableMapOf<LocalDate, MutableList<Schedule>>()
            list.sortedBy { it.startAt }.forEach { schedule ->
                var day = schedule.startAt.toLocalDate()
                val end = schedule.endAt.toLocalDate()
                while (!day.isAfter(end)) {
                    result.getOrPut(day) { mutableListOf() }.add(schedule)
                    day = day.plusDays(1)
                }
            }
            return result
        }
    }
}

/** 캘린더에서 직접 입력한 일정 */
data class NewSchedule(
    val title: String,
    val type: ScheduleType,
    val startAt: LocalDateTime,
    val location: String?,
    val reminderDaysBefore: List<Int>,
    val visibility: Visibility,
    val sharedRoomIds: Set<String> = emptySet(),
)

/** 직접 입력한 일정 저장. 과제는 마감 일정(시작=끝), 나머지는 기본 1시간. */
class AddScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(input: NewSchedule): Result<Schedule> {
        val title = input.title.trim()
        if (title.isEmpty()) return Result.failure(IllegalArgumentException("title is blank"))
        // 그룹공개인데 그룹을 하나도 안 고르면 아무에게도 안 보이니 막는다
        if (input.visibility == Visibility.GROUP && input.sharedRoomIds.isEmpty()) {
            return Result.failure(IllegalArgumentException("group is empty"))
        }
        val isDeadline = input.type == ScheduleType.ASSIGNMENT
        val schedule = Schedule(
            id = UUID.randomUUID().toString(),
            title = title,
            type = input.type,
            startAt = input.startAt,
            endAt = if (isDeadline) input.startAt else input.startAt.plusHours(1),
            isDeadline = isDeadline,
            visibility = input.visibility,
            source = ScheduleSource.MANUAL,
            location = input.location?.trim()?.ifEmpty { null },
            reminderDaysBefore = input.reminderDaysBefore.sorted(),
            sharedRoomIds = if (input.visibility == Visibility.GROUP) input.sharedRoomIds else emptySet(),
        )
        return repository.addSchedules(listOf(schedule)).map { schedule }
    }
}

/** 일정 삭제 */
class DeleteScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteSchedule(id)
}

/** 캘린더 위 프로필 줄에 보일 친구 목록 */
class ObserveFriendsUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
) {
    operator fun invoke(): Flow<List<Friend>> = friendRepository.observeFriends()
}

/** 그룹공개 때 고를 수 있는 그룹 = 내가 속한 단체 DM 방 */
class ObserveShareGroupsUseCase @Inject constructor(
    private val dmRepository: DmRepository,
) {
    operator fun invoke(): Flow<List<ShareGroup>> = dmRepository.observeRooms().map { rooms ->
        rooms.filter { it.type == RoomType.GROUP }.map { ShareGroup(id = it.id, name = it.title) }
    }
}
