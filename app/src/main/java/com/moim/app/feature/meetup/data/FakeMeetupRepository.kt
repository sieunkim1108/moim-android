package com.moim.app.feature.meetup.data

import com.moim.app.core.data.FriendRepository
import com.moim.app.core.data.ScheduleRepository
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleSource
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.Visibility
import com.moim.app.feature.dm.domain.DmRepository
import com.moim.app.feature.dm.domain.model.RoomMessage
import com.moim.app.feature.meetup.domain.MeetupRepository
import com.moim.app.feature.meetup.domain.model.BusyBlock
import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.model.MeetupMember
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * 방 참여자 → 각자의 일정 → 바쁜 시간으로 바꾼다.
 * 나: 내 일정 전체 / 친구: 친구 일정 (비공개는 이미 내용이 지워져 옴)
 * Supabase에서는 SupabaseMeetupRepository가 서버 함수(room_busy_blocks)로 바쁜 시간만 받는다.
 */
class FakeMeetupRepository @Inject constructor(
    private val dmRepository: DmRepository,
    private val scheduleRepository: ScheduleRepository,
    private val friendRepository: FriendRepository,
    private val clock: Clock,
) : MeetupRepository {

    /** 목업: 내 캘린더에만 넣고 방에 눈송이 안내를 남긴다 */
    override suspend fun confirm(roomId: String, title: String, candidate: MeetupCandidate, reason: String): Result<Unit> {
        val schedule = Schedule(
            id = UUID.randomUUID().toString(),
            title = title,
            type = ScheduleType.MEETUP,
            startAt = candidate.date.atTime(candidate.start),
            endAt = candidate.date.atTime(candidate.end),
            visibility = Visibility.FRIENDS,
            source = ScheduleSource.MEETUP,
            reminderDaysBefore = listOf(1),
        )
        return scheduleRepository.addSchedules(listOf(schedule)).onSuccess {
            val day = candidate.date.format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
            val time = DateTimeFormatter.ofPattern("a h시", Locale.KOREAN)
            val text = "📅 $day ${candidate.start.format(time)} ~ ${candidate.end.format(time)} 모임으로 정했어! 캘린더에 넣어 뒀어 💙"
            dmRepository.postBotMessages(roomId, listOf(RoomMessage.Bot(UUID.randomUUID().toString(), LocalDateTime.now(clock), text)))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMembers(roomId: String): Flow<List<MeetupMember>> =
        dmRepository.observeRoom(roomId).flatMapLatest { room ->
            if (room == null) return@flatMapLatest flowOf(emptyList())
            val flows = room.members.map { member ->
                val schedules = if (member.isMe) {
                    scheduleRepository.observeSchedules()
                } else {
                    friendRepository.observeFriendSchedules(member.id)
                }
                combine(schedules, flowOf(member)) { list, m ->
                    MeetupMember(id = m.id, name = m.name, isMe = m.isMe, busy = list.toBusyBlocks(m.isMe, roomId))
                }
            }
            combine(flows) { it.toList() }
        }

    /** 마감 일정(과제 23:59 등)은 시간을 차지하지 않으니 뺀다 */
    private fun List<Schedule>.toBusyBlocks(isMe: Boolean, roomId: String): List<BusyBlock> =
        filter { !it.isDeadline && !it.allDay && it.startAt.toLocalDate() == it.endAt.toLocalDate() }
            .map {
                // 비공개 + 이 방에 공개하지 않은 그룹공개 일정은 제목 없이 바쁨만 보인다
                val hidden = !isMe && (
                    it.visibility == Visibility.PRIVATE ||
                        (it.visibility == Visibility.GROUP && roomId !in it.sharedRoomIds)
                    )
                BusyBlock(
                    date = it.startAt.toLocalDate(),
                    start = it.startAt.toLocalTime(),
                    end = it.endAt.toLocalTime(),
                    label = if (hidden) null else it.title,
                )
            }
}
