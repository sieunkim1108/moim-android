package com.moim.app.feature.meetup.data

import com.moim.app.core.data.ScheduleRepository
import com.moim.app.core.supabase.fromDbTime
import com.moim.app.core.supabase.toDbTime
import com.moim.app.feature.meetup.domain.MeetupRepository
import com.moim.app.feature.meetup.domain.model.BusyBlock
import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.model.MeetupMember
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase 모임 저장소.
 * - 바쁜 시간: room_busy_blocks() 함수. 같은 방 멤버의 시간만 오고, 제목은 공개 범위가 허락할 때만 온다
 *   (비공개·다른 방 그룹공개 일정은 label = null → 화면에 🔒).
 * - 확정: meetups·meetup_candidates에 기록한 뒤 confirm_meetup() 함수가
 *   멤버 전원 캘린더에 모임 일정 + 전날 알림 + 눈송이 확정 메시지를 한 번에(트랜잭션) 넣는다.
 */
@Singleton
class SupabaseMeetupRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val scheduleRepository: ScheduleRepository,
    private val clock: Clock,
) : MeetupRepository {

    /** 내 일정이 바뀌면(새로 등록 등) 바쁜 시간을 다시 받는다 */
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMembers(roomId: String): Flow<List<MeetupMember>> =
        scheduleRepository.observeSchedules().mapLatest { load(roomId) }

    private suspend fun load(roomId: String): List<MeetupMember> {
        val myId = supabase.auth.currentUserOrNull()?.id
        val now = LocalDateTime.now(clock)
        val members = supabase.postgrest.rpc("room_member_profiles", buildJsonObject { put("p_room", roomId) })
            .decodeList<MemberRow>()
        val blocks = supabase.postgrest.rpc(
            "room_busy_blocks",
            buildJsonObject {
                put("p_room", roomId)
                put("p_from", now.toLocalDate().atStartOfDay().toDbTime())
                put("p_to", now.plusDays(SEARCH_DAYS).toDbTime())
            },
        ).decodeList<BusyRow>().groupBy { it.userId }

        return members.map { m ->
            MeetupMember(
                id = m.id,
                name = m.name,
                isMe = m.id == myId,
                busy = blocks[m.id].orEmpty().mapNotNull { it.toBlock() },
            )
        }
    }

    override suspend fun confirm(roomId: String, title: String, candidate: MeetupCandidate, reason: String): Result<Unit> = runCatching {
        val me = supabase.auth.currentUserOrNull()?.id ?: error("로그인이 필요해요")
        val minutes = Duration.between(candidate.start, candidate.end).toMinutes().toInt().coerceIn(30, 240)
        val meetup = supabase.from("meetups").insert(
            MeetupInsert(
                roomId = roomId,
                title = title,
                periodStart = candidate.date.toString(),
                periodEnd = candidate.date.toString(),
                durationMin = minutes,
                createdBy = me,
            ),
        ) { select() }.decodeSingle<IdRow>()
        val saved = supabase.from("meetup_candidates").insert(
            CandidateInsert(
                meetupId = meetup.id,
                startAt = candidate.date.atTime(candidate.start).toDbTime(),
                endAt = candidate.date.atTime(candidate.end).toDbTime(),
                // 점수는 알고리즘(MeetupScheduler)이 정한 순서를 그대로: 가능 인원 비율
                score = candidate.availableCount.toDouble() / candidate.totalMembers.coerceAtLeast(1),
                availableCount = candidate.availableCount,
                reason = reason.ifBlank { null },
            ),
        ) { select() }.decodeSingle<IdRow>()
        supabase.postgrest.rpc(
            "confirm_meetup",
            buildJsonObject {
                put("p_meetup", meetup.id)
                put("p_candidate", saved.id)
            },
        )
        Unit
    }

    private companion object {
        const val SEARCH_DAYS = 31L
    }
}

@Serializable
private data class MemberRow(val id: String, val name: String)

@Serializable
private data class BusyRow(
    @SerialName("user_id") val userId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    val label: String? = null,
) {
    /** 하루 안에 끝나는 일정만 (밤을 넘기는 일정은 화면 타임라인에서 다루지 않는다) */
    fun toBlock(): BusyBlock? {
        val start = startAt.fromDbTime()
        val end = endAt.fromDbTime()
        if (start.toLocalDate() != end.toLocalDate()) return null
        return BusyBlock(date = start.toLocalDate(), start = start.toLocalTime(), end = end.toLocalTime(), label = label)
    }
}

@Serializable
private data class IdRow(val id: String)

@Serializable
private data class MeetupInsert(
    @SerialName("room_id") val roomId: String,
    val title: String,
    @SerialName("period_start") val periodStart: String,
    @SerialName("period_end") val periodEnd: String,
    @SerialName("duration_min") val durationMin: Int,
    @SerialName("created_by") val createdBy: String,
)

@Serializable
private data class CandidateInsert(
    @SerialName("meetup_id") val meetupId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    val score: Double,
    @SerialName("available_count") val availableCount: Int,
    val reason: String?,
)
