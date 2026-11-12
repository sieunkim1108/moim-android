package com.moim.app.core.model

import java.time.LocalDateTime

/** 친구 한 명 (friendships로 연결된 profiles 한 줄의 공개 정보) */
data class Friend(
    val id: String,
    val name: String,
    val major: String? = null, // 채팅 목록에 "소프트웨어학부"처럼 표시
)

/** 이름에서 "성"을 뺀 부르는 이름 (예: 김시은 → 시은). 두 글자 이하는 그대로. */
val Friend.givenName: String get() = if (name.length >= 3) name.drop(1) else name

/**
 * 친구 검색에 보이는 최소 정보 (S-12).
 * RLS 규칙: 친구가 아닌 사람에게는 이름, 학번, 전공만 공개한다.
 */
data class StudentProfile(
    val id: String,
    val name: String,
    val studentId: String,
    val major: String,
    val isVerified: Boolean = true, // 학교 이메일 인증을 마친 재학생
)

/** "2314582" → "23" (학번대) */
val StudentProfile.admissionYear: String get() = studentId.take(2)

/** 검색한 사람과 나의 관계 */
enum class FriendRelation {
    NONE,      // [친구 요청] 가능
    REQUESTED, // 내가 보낸 요청 대기 중 → "요청됨"
    INCOMING,  // 상대가 나에게 요청함 → 아래 받은 요청에서 수락
    FRIEND,    // 이미 친구
    ME,        // 내 학번
}

/** 학번 검색 결과 */
data class FriendSearchResult(
    val profile: StudentProfile,
    val relation: FriendRelation,
    val commonRoomNames: List<String> = emptyList(), // 둘 다 속한 단체 DM 방 이름 (room_members로 계산)
)

/** 받은 친구 요청 (friendships.status = pending, friend_id = 나) */
data class FriendRequest(
    val id: String,
    val from: StudentProfile,
    val requestedAt: LocalDateTime,
    val message: String? = null,      // 한 줄 메시지
    val viaRoomName: String? = null,  // 연결 경로 (예: 모임 '눈송이 해커톤 24기')
)
