package com.moim.app.core.data

import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleSource
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.Visibility
import com.moim.app.core.supabase.fromDbTime
import com.moim.app.core.supabase.toDbTime
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase schedules 테이블을 쓰는 일정 저장소.
 * RLS 정책상 이 테이블에서는 "내 일정"만 보인다 (친구 일정은 FriendRepository의 friend_schedules 함수).
 * 읽은 목록은 메모리(StateFlow)에 들고 있다가, 추가·수정·삭제하면 다시 불러온다.
 * TODO(5단계): Realtime 구독으로 다른 기기에서 바뀐 것도 바로 반영
 */
@Singleton
class SupabaseScheduleRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : ScheduleRepository {

    private val cache = MutableStateFlow<List<Schedule>>(emptyList())

    override fun observeSchedules(): Flow<List<Schedule>> =
        cache.onStart { refresh() }

    override fun observeSchedulesOn(date: LocalDate): Flow<List<Schedule>> =
        observeSchedules().map { list ->
            list.filter { !it.startAt.toLocalDate().isAfter(date) && !it.endAt.toLocalDate().isBefore(date) }
        }

    override suspend fun addSchedules(schedules: List<Schedule>): Result<Unit> = runCatching {
        val ownerId = supabase.auth.currentUserOrNull()?.id ?: error("로그인이 필요해요")
        schedules.forEach { schedule ->
            val saved = supabase.from(TABLE)
                .insert(schedule.toInsertRow(ownerId)) { select() }
                .decodeSingle<ScheduleRow>()
            insertChildren(saved.id, schedule)
        }
        refresh()
    }

    /**
     * 일정 수정. schedules 한 줄을 update하고,
     * 알림(reminders)·공개 대상 방(schedule_shared_rooms)은 지웠다가 새 값으로 다시 넣는다.
     * RLS 정책상 내 일정만 바뀐다 (남의 id면 0줄이 바뀌어 실패로 처리).
     */
    override suspend fun updateSchedule(schedule: Schedule): Result<Unit> = runCatching {
        val updated = supabase.from(TABLE)
            .update(schedule.toUpdateRow()) {
                select()
                filter { eq("id", schedule.id) }
            }
            .decodeList<ScheduleRow>()
        if (updated.isEmpty()) error("수정할 일정을 찾지 못했어요")

        supabase.from("reminders").delete { filter { eq("schedule_id", schedule.id) } }
        supabase.from("schedule_shared_rooms").delete { filter { eq("schedule_id", schedule.id) } }
        insertChildren(schedule.id, schedule)
        refresh()
    }

    /** 일정에 딸린 공개 대상 방과 알림을 넣는다 (추가·수정 공통) */
    private suspend fun insertChildren(scheduleId: String, schedule: Schedule) {
        if (schedule.visibility == Visibility.GROUP && schedule.sharedRoomIds.isNotEmpty()) {
            supabase.from("schedule_shared_rooms").insert(
                schedule.sharedRoomIds.map { SharedRoomRow(scheduleId = scheduleId, roomId = it) },
            )
        }
        val reminders = schedule.reminderDaysBefore.distinct().map { days ->
            ReminderInsertRow(scheduleId = scheduleId, remindAt = remindAt(schedule, days).toDbTime())
        }
        if (reminders.isNotEmpty()) supabase.from("reminders").insert(reminders)
    }

    override suspend fun deleteSchedule(id: String): Result<Unit> = runCatching {
        // 공개 대상 방·알림은 on delete cascade로 같이 지워진다
        supabase.from(TABLE).delete { filter { eq("id", id) } }
        refresh()
    }

    private suspend fun refresh() {
        runCatching {
            supabase.from(TABLE)
                .select(Columns.raw("*, reminders(remind_at), schedule_shared_rooms(room_id)")) {
                    order("start_at", Order.ASCENDING)
                }
                .decodeList<ScheduleRow>()
                .map { it.toModel() }
        }.onSuccess { cache.value = it }
    }

    private companion object {
        const val TABLE = "schedules"
        val MORNING: LocalTime = LocalTime.of(9, 0)

        /** N일 전 오전 9시에 알림. 당일(0)은 시작 1시간 전 */
        fun remindAt(schedule: Schedule, daysBefore: Int) =
            if (daysBefore == 0) schedule.startAt.minus(Duration.ofHours(1))
            else schedule.startAt.toLocalDate().minusDays(daysBefore.toLong()).atTime(MORNING)
    }
}

@Serializable
private data class ScheduleRow(
    val id: String,
    val title: String,
    val type: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("all_day") val allDay: Boolean = false,
    @SerialName("is_deadline") val isDeadline: Boolean = false,
    val visibility: String = "friends",
    val source: String = "manual",
    val location: String? = null,
    val memo: String? = null,
    val reminders: List<ReminderRow> = emptyList(),
    @SerialName("schedule_shared_rooms") val sharedRooms: List<SharedRoomIdRow> = emptyList(),
)

@Serializable
private data class ReminderRow(@SerialName("remind_at") val remindAt: String)

@Serializable
private data class SharedRoomIdRow(@SerialName("room_id") val roomId: String)

@Serializable
private data class ScheduleInsertRow(
    @SerialName("owner_id") val ownerId: String,
    val title: String,
    val type: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("all_day") val allDay: Boolean,
    @SerialName("is_deadline") val isDeadline: Boolean,
    val visibility: String,
    val source: String,
    val location: String?,
    val memo: String?,
)

/** 수정할 때 바꾸는 칸만 (owner_id·source는 그대로 둔다) */
@Serializable
private data class ScheduleUpdateRow(
    val title: String,
    val type: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("all_day") val allDay: Boolean,
    @SerialName("is_deadline") val isDeadline: Boolean,
    val visibility: String,
    val location: String?,
    val memo: String?,
)

@Serializable
private data class SharedRoomRow(
    @SerialName("schedule_id") val scheduleId: String,
    @SerialName("room_id") val roomId: String,
)

@Serializable
private data class ReminderInsertRow(
    @SerialName("schedule_id") val scheduleId: String,
    @SerialName("remind_at") val remindAt: String,
)

private fun Schedule.toInsertRow(ownerId: String) = ScheduleInsertRow(
    ownerId = ownerId,
    title = title,
    type = type.dbValue,
    startAt = startAt.toDbTime(),
    endAt = endAt.toDbTime(),
    allDay = allDay,
    isDeadline = isDeadline,
    visibility = visibility.dbValue,
    source = source.dbValue,
    location = location,
    memo = memo,
)

private fun Schedule.toUpdateRow() = ScheduleUpdateRow(
    title = title,
    type = type.dbValue,
    startAt = startAt.toDbTime(),
    endAt = endAt.toDbTime(),
    allDay = allDay,
    isDeadline = isDeadline,
    visibility = visibility.dbValue,
    location = location,
    memo = memo,
)

private fun ScheduleRow.toModel(): Schedule {
    val start = startAt.fromDbTime()
    return Schedule(
        id = id,
        title = title,
        type = ScheduleType.entries.firstOrNull { it.dbValue == type } ?: ScheduleType.ETC,
        startAt = start,
        endAt = endAt.fromDbTime(),
        allDay = allDay,
        isDeadline = isDeadline,
        visibility = Visibility.entries.firstOrNull { it.dbValue == visibility } ?: Visibility.FRIENDS,
        source = ScheduleSource.entries.firstOrNull { it.dbValue == source } ?: ScheduleSource.MANUAL,
        memo = memo,
        location = location,
        // 저장된 알림 시각 → "며칠 전"으로 되돌림 (당일 알림은 0)
        reminderDaysBefore = reminders
            .map { ChronoUnit.DAYS.between(it.remindAt.fromDbTime().toLocalDate(), start.toLocalDate()).toInt() }
            .filter { it >= 0 }
            .distinct()
            .sorted(),
        sharedRoomIds = sharedRooms.map { it.roomId }.toSet(),
    )
}
