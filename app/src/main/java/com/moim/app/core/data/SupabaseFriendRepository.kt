package com.moim.app.core.data

import com.moim.app.core.model.Friend
import com.moim.app.core.model.FriendRelation
import com.moim.app.core.model.FriendRequest
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleSource
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.StudentProfile
import com.moim.app.core.model.Visibility
import com.moim.app.core.supabase.fromDbTime
import com.moim.app.core.supabase.toDbTime
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase 친구 저장소.
 * 친구 목록·검색·받은 요청·친구 일정은 모두 DB 함수(security definer)로 받는다.
 * 그래서 앱은 다른 사람의 profiles/schedules 원본 행을 직접 읽을 수 없고,
 * 함수가 허락한 최소 정보(이름·학번·전공, 가려진 일정)만 받는다.
 */
@Singleton
class SupabaseFriendRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val clock: Clock,
) : FriendRepository {

    private val friends = MutableStateFlow<List<Friend>>(emptyList())
    private val incoming = MutableStateFlow<List<FriendRequest>>(emptyList())

    override fun observeFriends(): Flow<List<Friend>> = friends.onStart { refreshFriends() }

    override fun observeIncomingRequests(): Flow<List<FriendRequest>> = incoming.onStart { refreshIncoming() }

    override fun observeFriendSchedules(friendId: String): Flow<List<Schedule>> = flow {
        // 지난달 ~ 석 달 뒤까지 (캘린더에서 넘겨 보는 범위)
        val now = LocalDateTime.now(clock)
        val rows = supabase.postgrest.rpc(
            "friend_schedules",
            buildJsonObject {
                put("p_friend", friendId)
                put("p_from", now.minusMonths(1).toDbTime())
                put("p_to", now.plusMonths(3).toDbTime())
            },
        ).decodeList<FriendScheduleRow>()
        emit(rows.map { it.toModel() })
    }

    override suspend fun searchByStudentId(studentId: String): Result<Pair<StudentProfile, FriendRelation>?> = runCatching {
        supabase.postgrest.rpc("search_student", buildJsonObject { put("p_student_id", studentId) })
            .decodeList<SearchRow>()
            .firstOrNull()
            ?.let { StudentProfile(it.id, it.name, it.studentId, it.major) to it.relationModel() }
    }

    override suspend fun sendRequest(userId: String): Result<Unit> = runCatching {
        supabase.from("friendships").insert(FriendshipInsert(userId = myId(), friendId = userId))
        Unit
    }

    override suspend fun acceptRequest(requestId: String): Result<Unit> = runCatching {
        // requestId = 요청 보낸 사람 id. RLS상 받은 사람(나)만 accepted로 바꿀 수 있다
        supabase.from("friendships").update({ set("status", "accepted") }) {
            filter {
                eq("user_id", requestId)
                eq("friend_id", myId())
            }
        }
        refreshIncoming()
        refreshFriends()
    }

    override suspend fun declineRequest(requestId: String): Result<Unit> = runCatching {
        supabase.from("friendships").delete {
            filter {
                eq("user_id", requestId)
                eq("friend_id", myId())
            }
        }
        refreshIncoming()
    }

    private suspend fun refreshFriends() {
        runCatching { supabase.postgrest.rpc("my_friends").decodeList<FriendRow>() }
            .onSuccess { rows -> friends.value = rows.map { Friend(it.id, it.name, it.major) } }
    }

    private suspend fun refreshIncoming() {
        runCatching { supabase.postgrest.rpc("incoming_friend_requests").decodeList<IncomingRow>() }
            .onSuccess { rows ->
                incoming.value = rows.map {
                    FriendRequest(
                        id = it.requesterId,
                        from = StudentProfile(it.requesterId, it.name, it.studentId, it.major),
                        requestedAt = it.createdAt.fromDbTime(),
                        message = it.message,
                    )
                }
            }
    }

    private fun myId(): String = supabase.auth.currentUserOrNull()?.id ?: error("로그인이 필요해요")
}

@Serializable
private data class FriendRow(val id: String, val name: String, val major: String? = null)

@Serializable
private data class SearchRow(
    val id: String,
    val name: String,
    @SerialName("student_id") val studentId: String,
    val major: String,
    val relation: String,
    @SerialName("common_rooms") val commonRooms: List<String> = emptyList(),
) {
    fun relationModel() = when (relation) {
        "me" -> FriendRelation.ME
        "friend" -> FriendRelation.FRIEND
        "requested" -> FriendRelation.REQUESTED
        "incoming" -> FriendRelation.INCOMING
        else -> FriendRelation.NONE
    }
}

@Serializable
private data class IncomingRow(
    @SerialName("requester_id") val requesterId: String,
    val name: String,
    @SerialName("student_id") val studentId: String,
    val major: String,
    val message: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class FriendshipInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String,
)

@Serializable
private data class FriendScheduleRow(
    val id: String,
    val title: String,
    val type: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("all_day") val allDay: Boolean = false,
    @SerialName("is_deadline") val isDeadline: Boolean = false,
    val visibility: String,
    val location: String? = null,
    @SerialName("is_hidden") val isHidden: Boolean,
) {
    fun toModel() = Schedule(
        id = id,
        // 가려진 일정은 Fake와 같이 제목을 비워 두고, 화면이 "OOO님의 일정입니다"로 표시한다
        title = if (isHidden) "" else title,
        type = ScheduleType.entries.firstOrNull { it.dbValue == type } ?: ScheduleType.ETC,
        startAt = startAt.fromDbTime(),
        endAt = endAt.fromDbTime(),
        allDay = allDay,
        isDeadline = isDeadline,
        visibility = if (isHidden) Visibility.PRIVATE else
            Visibility.entries.firstOrNull { it.dbValue == visibility } ?: Visibility.FRIENDS,
        source = ScheduleSource.MANUAL,
        location = location,
    )
}
