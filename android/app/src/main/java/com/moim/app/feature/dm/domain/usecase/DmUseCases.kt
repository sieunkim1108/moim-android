package com.moim.app.feature.dm.domain.usecase

import com.moim.app.core.data.FriendRepository
import com.moim.app.core.model.Friend
import com.moim.app.core.model.givenName
import com.moim.app.feature.dm.domain.DmRepository
import com.moim.app.feature.dm.domain.model.ChatRoom
import com.moim.app.feature.dm.domain.model.RoomMessage
import com.moim.app.feature.dm.domain.model.RoomType
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

class ObserveRoomsUseCase @Inject constructor(private val repository: DmRepository) {
    operator fun invoke(): Flow<List<ChatRoom>> = repository.observeRooms()
}

class ObserveRoomUseCase @Inject constructor(private val repository: DmRepository) {
    operator fun invoke(roomId: String): Flow<ChatRoom?> = repository.observeRoom(roomId)
    fun messages(roomId: String): Flow<List<RoomMessage>> = repository.observeMessages(roomId)
}

class MarkRoomReadUseCase @Inject constructor(private val repository: DmRepository) {
    suspend operator fun invoke(roomId: String) = repository.markRead(roomId)
}

/**
 * 단체방에 글 보내기.
 * 모임 얘기("언제", "놀자", "모임" ...)가 나오면 눈송이가 일정 비교를 제안한다.
 * TODO: 실제로는 서버의 AI가 대화 흐름을 보고 판단
 */
class SendRoomMessageUseCase @Inject constructor(
    private val repository: DmRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(room: ChatRoom, text: String): Result<Unit> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.success(Unit)
        val result = repository.sendMessage(room.id, trimmed)
        if (result.isSuccess && room.type == RoomType.GROUP && isMeetupTalk(trimmed)) {
            val now = LocalDateTime.now(clock)
            repository.postBotMessages(
                room.id,
                listOf(
                    RoomMessage.Bot(UUID.randomUUID().toString(), now, "내가 ${room.members.size}명 일정 비교해 볼게! 👀"),
                    RoomMessage.MeetupSuggestion(UUID.randomUUID().toString(), now),
                ),
            )
        }
        return result
    }

    companion object {
        private val MEETUP_WORDS = listOf("언제", "놀자", "모임", "만나", "회의", "시간 돼", "시간돼")

        fun isMeetupTalk(text: String): Boolean = MEETUP_WORDS.any { it in text }
    }
}

/** 단체방에 초대할 수 있는 사람 = 내 친구 (RLS도 친구만 초대 가능) */
class ObserveInvitableFriendsUseCase @Inject constructor(private val friendRepository: FriendRepository) {
    operator fun invoke(): Flow<List<Friend>> = friendRepository.observeFriends()
}

/**
 * 단체 채팅방 만들기.
 * - 친구를 한 명 이상 골라야 한다
 * - 방 이름을 비워 두면 친구 이름으로 자동으로 짓는다 (예: "시은, 지원")
 */
class CreateGroupRoomUseCase @Inject constructor(private val repository: DmRepository) {
    suspend operator fun invoke(name: String, friends: List<Friend>): Result<String> {
        val invitees = friends.distinctBy { it.id }
        if (invitees.isEmpty()) return Result.failure(IllegalArgumentException("no friends selected"))
        val title = name.trim().ifEmpty { defaultName(invitees) }.take(MAX_NAME_LENGTH)
        return repository.createGroupRoom(title, invitees)
    }

    companion object {
        const val MAX_NAME_LENGTH = 30

        fun defaultName(friends: List<Friend>): String {
            val joined = friends.joinToString(", ") { it.givenName }
            return if (joined.length <= MAX_NAME_LENGTH) joined else joined.take(MAX_NAME_LENGTH - 1) + "…"
        }
    }
}
