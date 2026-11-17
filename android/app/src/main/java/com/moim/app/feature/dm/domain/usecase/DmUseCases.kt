package com.moim.app.feature.dm.domain.usecase

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
