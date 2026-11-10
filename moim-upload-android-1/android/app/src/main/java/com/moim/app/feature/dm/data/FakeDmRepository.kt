package com.moim.app.feature.dm.data

import com.moim.app.core.data.FakeFriendRepository
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
 * 메모리에만 있는 가짜 채팅방들 (Stitch "단톡방 목록" 디자인 내용).
 * 친구 id는 FakeFriendRepository와 같아서 모임 일정 계산에 그 친구 일정이 쓰인다.
 */
@Singleton
class FakeDmRepository @Inject constructor(
    clock: Clock,
    private val authRepository: AuthRepository,
) : DmRepository {

    private val now = LocalDateTime.now(clock)
    private val clock = clock

    private val me get() = RoomMember(ME_ID, authRepository.currentUser.value?.name ?: "나", isMe = true)
    private val sieun = RoomMember(FakeFriendRepository.FRIEND_SIEUN, "김시은")
    private val jiwon = RoomMember(FakeFriendRepository.FRIEND_JIWON, "박지원")
    private val minseo = RoomMember(FakeFriendRepository.FRIEND_MINSEO, "최민서")

    private val rooms = MutableStateFlow(
        listOf(
            ChatRoom(
                id = ROOM_SW, type = RoomType.GROUP, title = "SW 모바일 팀플방",
                members = listOf(placeholderMe(), sieun, jiwon, minseo),
                lastSenderName = "시은", lastMessage = "이번주 목요일 7시 회의 가능한 사람 투표해 줘!",
                lastAt = now.minusMinutes(5), unreadCount = 3, hasActiveVote = true,
            ),
            ChatRoom(
                id = ROOM_BOT, type = RoomType.BOT, title = "AI 눈송이",
                members = listOf(placeholderMe()),
                lastMessage = "내일 '데이터사이언스 과제' 마감 1일 전이야! 오늘 조금만 해 두자 💙",
                lastAt = now.minusMinutes(90), unreadCount = 1,
            ),
            ChatRoom(
                id = ROOM_STUDY, type = RoomType.GROUP, title = "데이터분석 스터디",
                members = listOf(placeholderMe(), jiwon, sieun),
                lastSenderName = "지원", lastMessage = "스터디룸 중앙도서관 4층으로 예약할까?",
                lastAt = now.minusHours(3), unreadCount = 5, hasActiveVote = true,
            ),
            ChatRoom(
                id = ROOM_SIEUN, type = RoomType.DIRECT, title = "김시은", subtitle = "소프트웨어학부",
                members = listOf(placeholderMe(), sieun),
                lastMessage = "오늘 학생식당 순헌관 앞에서 만나는 거 맞지?",
                lastAt = now.minusDays(1),
            ),
            ChatRoom(
                id = ROOM_MOBILE, type = RoomType.GROUP, title = "모바일 소프트웨어 조모임",
                members = listOf(placeholderMe(), sieun, jiwon, minseo),
                lastSenderName = "민서", lastMessage = "교수님이 공지 올려주신 레포트 양식 공유할게!",
                lastAt = now.minusDays(1).minusHours(2), unreadCount = 2,
            ),
            ChatRoom(
                id = ROOM_JIWON, type = RoomType.DIRECT, title = "박지원", subtitle = "통계학과",
                members = listOf(placeholderMe(), jiwon),
                lastMessage = "고마워! 다음 주 열람실에서 같이 공부하자 ㅎㅎ",
                lastAt = now.minusDays(6),
            ),
        ),
    )

    private val messages = MutableStateFlow(
        mapOf(
            ROOM_SW to listOf(
                text(sieun, "이번 달에 한번 놀자!! 🌸", now.minusMinutes(9)),
                text(jiwon, "좋아 언제 돼?", now.minusMinutes(8)),
                RoomMessage.Text(UUID.randomUUID().toString(), now.minusMinutes(6), ME_ID, "나", "나 시험 끝나고 아무 때나!", isMine = true),
                RoomMessage.Bot(UUID.randomUUID().toString(), now.minusMinutes(6), "내가 넷 일정 비교해 볼게! 👀"),
                RoomMessage.MeetupSuggestion(UUID.randomUUID().toString(), now.minusMinutes(6)),
                text(sieun, "이번주 목요일 7시 회의 가능한 사람 투표해 줘!", now.minusMinutes(5)),
            ),
            ROOM_STUDY to listOf(text(jiwon, "스터디룸 중앙도서관 4층으로 예약할까?", now.minusHours(3))),
            ROOM_SIEUN to listOf(text(sieun, "오늘 학생식당 순헌관 앞에서 만나는 거 맞지?", now.minusDays(1))),
            ROOM_MOBILE to listOf(text(minseo, "교수님이 공지 올려주신 레포트 양식 공유할게!", now.minusDays(1).minusHours(2))),
            ROOM_JIWON to listOf(text(jiwon, "고마워! 다음 주 열람실에서 같이 공부하자 ㅎㅎ", now.minusDays(6))),
        ),
    )

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

    private fun text(member: RoomMember, body: String, at: LocalDateTime) =
        RoomMessage.Text(UUID.randomUUID().toString(), at, member.id, member.name.drop(1), body, isMine = false)

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
