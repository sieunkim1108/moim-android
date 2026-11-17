package com.moim.app.feature.dm.data

import com.moim.app.feature.auth.domain.repository.AuthRepository
import com.moim.app.feature.dm.domain.DmRepository
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMember
import com.moim.app.feature.dm.domain.model.RoomMessage
import com.moim.app.feature.dm.domain.model.RoomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메모리에만 있는 목업 채팅방 저장소.
 * 새로 가입한 사용자처럼 눈송이 방 하나만 있는 빈 상태로 시작한다.
 */
@Singleton
class FakeDmRepository @Inject constructor(
    clock: Clock,
    private val authRepository: AuthRepository,
) : DmRepository {

    private val now = LocalDateTime.now(clock)
    private val clock = clock

    private val me get() = RoomMember(ME_ID, authRepository.currentUser.value?.name ?: "나", isMe = true)

    // 처음엔 눈송이 1:1 방만 있고, 친구 방·메시지는 없다 (새 가입자 상태)
    private val rooms = MutableStateFlow(
        listOf(
            ChatRoom(
                id = ROOM_BOT, type = RoomType.BOT, title = "AI 눈송이",
                members = listOf(placeholderMe()),
                lastMessage = "일정이 생기면 눈송이에게 말해 줘 💙",
                lastAt = now,
            ),
        ),
    )

    private val messages = MutableStateFlow<Map<String, List<RoomMessage>>>(emptyMap())

    // 로그인한 사람 이름이 바뀌어도 "나"가 맞게 보이도록 매번 채워 넣는다
    override fun observeRooms(): Flow<List<ChatRoom>> =
        combine(rooms, authRepository.currentUser) { list, _ ->
            list.map { room -> room.copy(members = room.members.map { if (it.isMe) me else it }) }
                .sortedByDescending { it.lastAt }
        }

    override fun observeRoom(roomId: String): Flow<ChatRoom?> =
        observeRooms().map { list -> list.firstOrNull { it.id == roomId } }

    override fun observeMessages(roomId: String): Flow<List<RoomMessage>> =
        messages.map { it[roomId].orEmpty() }

    override suspend fun sendMessage(roomId: String, text: String): Result<Unit> {
        val sentAt = LocalDateTime.now(clock)
        val message = RoomMessage.Text(UUID.randomUUID().toString(), sentAt, ME_ID, me.name, text, isMine = true)
        append(roomId, listOf(message))
        rooms.update { list ->
            list.map { if (it.id == roomId) it.copy(lastSenderName = null, lastMessage = text, lastAt = sentAt) else it }
        }
        return Result.success(Unit)
    }

    override suspend fun postBotMessages(roomId: String, messages: List<RoomMessage>): Result<Unit> {
        append(roomId, messages)
        val lastText = messages.filterIsInstance<RoomMessage.Bot>().lastOrNull()?.text
        if (lastText != null) {
            rooms.update { list ->
                list.map {
                    if (it.id == roomId) it.copy(lastSenderName = "눈송이", lastMessage = lastText, lastAt = LocalDateTime.now(clock)) else it
                }
            }
        }
        return Result.success(Unit)
    }

    override suspend fun markRead(roomId: String) {
        rooms.update { list -> list.map { if (it.id == roomId) it.copy(unreadCount = 0) else it } }
    }

    private fun append(roomId: String, newMessages: List<RoomMessage>) {
        messages.update { map -> map + (roomId to map[roomId].orEmpty() + newMessages) }
    }

    private fun placeholderMe() = RoomMember(ME_ID, "나", isMe = true)

    companion object {
        const val ME_ID = "me"
        const val ROOM_SW = "room-sw"
        const val ROOM_BOT = "room-bot"
        const val ROOM_STUDY = "room-study"
        const val ROOM_SIEUN = "room-sieun"
        const val ROOM_MOBILE = "room-mobile"
        const val ROOM_JIWON = "room-jiwon"
    }
}
