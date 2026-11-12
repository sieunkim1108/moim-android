package com.moim.app.feature.dm.domain.model

import java.time.LocalDateTime

/** 채팅방 종류: 1:1 / 단체 / 눈송이(AI 비서) */
enum class RoomType { DIRECT, GROUP, BOT }

/** 채팅방 참여자 */
data class RoomMember(
    val id: String,
    val name: String,
    val isMe: Boolean = false,
)

/** 채팅 목록 한 줄 */
data class ChatRoom(
    val id: String,
    val type: RoomType,
    val title: String,
    val subtitle: String? = null, // 1:1 방: 상대 학과
    val members: List<RoomMember>,
    val lastSenderName: String? = null,
    val lastMessage: String,
    val lastAt: LocalDateTime,
    val unreadCount: Int = 0,
    val hasActiveVote: Boolean = false, // "모임 투표중" 배지
)

/** 채팅방 안 메시지 */
sealed interface RoomMessage {
    val id: String
    val sentAt: LocalDateTime

    /** 사람이 보낸 글 */
    data class Text(
        override val id: String,
        override val sentAt: LocalDateTime,
        val senderId: String,
        val senderName: String,
        val text: String,
        val isMine: Boolean,
    ) : RoomMessage

    /** 눈송이(AI 일정 비서)가 보낸 글 */
    data class Bot(
        override val id: String,
        override val sentAt: LocalDateTime,
        val text: String,
    ) : RoomMessage

    /** 눈송이 추천 모임 시간 카드. 시간 계산은 화면을 열 때 알고리즘이 새로 한다. */
    data class MeetupSuggestion(
        override val id: String,
        override val sentAt: LocalDateTime,
    ) : RoomMessage
}
