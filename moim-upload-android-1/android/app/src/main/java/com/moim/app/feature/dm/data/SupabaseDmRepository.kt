package com.moim.app.feature.dm.data

import com.moim.app.core.supabase.fromDbTime
import com.moim.app.core.supabase.toDbTime
import com.moim.app.feature.dm.domain.DmRepository
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMember
import com.moim.app.feature.dm.domain.model.RoomMessage
import com.moim.app.feature.dm.domain.model.RoomType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase 채팅방 저장소.
 * - 방 목록: my_rooms() 함수 (마지막 메시지·안 읽은 수·투표 중 여부를 한 번에)
 * - 메시지: messages 테이블 + Realtime 구독 (새 글이 오면 바로 화면에 붙는다)
 * - RLS 때문에 내가 속한 방·메시지만 보인다
 * - 눈송이 글은 DB에 앱이 쓸 수 없다(서버만 씀). 그래서 "일정 비교해 볼게" 같은 안내는 이 기기에만 보여 준다.
 *   모임 확정 안내는 서버 함수(confirm_meetup)가 DB에 남겨서 모두에게 보인다.
 */
@Singleton
class SupabaseDmRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val clock: Clock,
) : DmRepository {

    private val rooms = MutableStateFlow<List<ChatRoom>>(emptyList())

    /** 이 기기에만 보이는 눈송이 글 (방 id → 메시지) */
    private val localBot = MutableStateFlow<Map<String, List<RoomMessage>>>(emptyMap())

    private val myId: String? get() = supabase.auth.currentUserOrNull()?.id

    override fun observeRooms(): Flow<List<ChatRoom>> = channelFlow {
        refreshRooms()
        // 어느 방이든 새 글이 오면 목록(마지막 글·안 읽은 수)을 다시 불러온다
        val channel = supabase.channel("my-rooms-${UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") { table = "messages" }
        launch { changes.collect { refreshRooms() } }
        channel.subscribe()
        try {
            rooms.map { listOf(botRoom()) + it }.collect { send(it) }
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }

    override fun observeRoom(roomId: String): Flow<ChatRoom?> =
        observeRooms().map { list -> list.firstOrNull { it.id == roomId } }

    override fun observeMessages(roomId: String): Flow<List<RoomMessage>> = channelFlow {
        val members = loadMembers(roomId).associateBy { it.id }
        val messages = MutableStateFlow<List<RoomMessage>>(emptyList())

        val channel = supabase.channel("room-$roomId-${UUID.randomUUID()}")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("room_id", FilterOperator.EQ, roomId)
        }
        launch {
            inserts.collect { action ->
                val row = runCatching { action.decodeRecord<MessageRow>() }.getOrNull() ?: return@collect
                messages.update { list -> if (list.any { it.id == row.id }) list else list + row.toModel(members) }
            }
        }
        channel.subscribe()

        // 구독을 먼저 걸고 나서 지난 글을 읽어야 그 사이에 온 글을 놓치지 않는다
        val history = supabase.from("messages")
            .select { filter { eq("room_id", roomId) }; order("created_at", Order.ASCENDING); limit(200) }
            .decodeList<MessageRow>()
            .map { it.toModel(members) }
        messages.update { live -> (history + live).distinctBy { it.id } }

        try {
            combine(messages, localBot) { remote, local ->
                (remote + local[roomId].orEmpty()).sortedBy { it.sentAt }
            }.collect { send(it) }
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(channel) }
        }
    }

    override suspend fun sendMessage(roomId: String, text: String): Result<Unit> = runCatching {
        val me = myId ?: error("로그인이 필요해요")
        supabase.from("messages").insert(MessageInsert(roomId = roomId, senderId = me, content = text))
        Unit
    }

    override suspend fun postBotMessages(roomId: String, messages: List<RoomMessage>): Result<Unit> {
        localBot.update { map -> map + (roomId to map[roomId].orEmpty() + messages) }
        return Result.success(Unit)
    }

    override suspend fun markRead(roomId: String) {
        val me = myId ?: return
        runCatching {
            supabase.from("room_members").update(
                buildJsonObject { put("last_read_at", LocalDateTime.now(clock).toDbTime()) },
            ) {
                filter {
                    eq("room_id", roomId)
                    eq("user_id", me)
                }
            }
            refreshRooms()
        }
    }

    private suspend fun refreshRooms() {
        runCatching { supabase.postgrest.rpc("my_rooms").decodeList<RoomRow>() }
            .onSuccess { rows -> rooms.value = rows.map { it.toModel(myId) } }
    }

    private suspend fun loadMembers(roomId: String): List<MemberRow> =
        runCatching {
            supabase.postgrest.rpc("room_member_profiles", buildJsonObject { put("p_room", roomId) })
                .decodeList<MemberRow>()
        }.getOrDefault(emptyList())

    /** 눈송이 1:1 방은 메인 채팅(bot_messages)이라 목록 맨 위에 고정으로 보여 준다 */
    private fun botRoom() = ChatRoom(
        id = FakeDmRepository.ROOM_BOT,
        type = RoomType.BOT,
        title = "AI 눈송이",
        members = emptyList(),
        lastMessage = "일정이 생기면 눈송이에게 말해 줘 💙",
        lastAt = LocalDateTime.now(clock),
    )

    private fun MessageRow.toModel(members: Map<String, MemberRow>): RoomMessage {
        val at = createdAt.fromDbTime()
        return when {
            senderId == null && cardType == "meetup_suggestion" -> RoomMessage.MeetupSuggestion(id, at)
            senderId == null -> RoomMessage.Bot(id, at, content)
            else -> RoomMessage.Text(
                id = id,
                sentAt = at,
                senderId = senderId,
                senderName = members[senderId]?.name?.let { if (it.length >= 3) it.drop(1) else it } ?: "알 수 없음",
                text = content,
                isMine = senderId == myId,
            )
        }
    }
}

@Serializable
private data class RoomRow(
    val id: String,
    val name: String? = null,
    @SerialName("is_group") val isGroup: Boolean,
    val members: List<MemberRow> = emptyList(),
    @SerialName("last_sender_name") val lastSenderName: String? = null,
    @SerialName("last_content") val lastContent: String = "",
    @SerialName("last_at") val lastAt: String,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("has_active_vote") val hasActiveVote: Boolean = false,
) {
    fun toModel(myId: String?): ChatRoom {
        val other = members.firstOrNull { it.id != myId }
        return ChatRoom(
            id = id,
            type = if (isGroup) RoomType.GROUP else RoomType.DIRECT,
            // 1:1 방은 이름이 없어서 상대 이름을 제목으로
            title = name ?: other?.name ?: "채팅방",
            subtitle = if (isGroup) null else other?.major,
            members = members.map { RoomMember(it.id, it.name, isMe = it.id == myId) },
            lastSenderName = lastSenderName?.let { if (it.length >= 3 && it != "눈송이") it.drop(1) else it },
            lastMessage = lastContent,
            lastAt = lastAt.fromDbTime(),
            unreadCount = unreadCount,
            hasActiveVote = hasActiveVote,
        )
    }
}

@Serializable
private data class MemberRow(val id: String, val name: String, val major: String? = null)

@Serializable
private data class MessageRow(
    val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_id") val senderId: String? = null,
    val content: String = "",
    @SerialName("card_type") val cardType: String? = null,
    @SerialName("card_payload") val cardPayload: JsonObject? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
private data class MessageInsert(
    @SerialName("room_id") val roomId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String,
)
