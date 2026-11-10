package com.moim.app.core.data

import com.moim.app.core.model.Friend
import com.moim.app.core.model.FriendRelation
import com.moim.app.core.model.FriendRequest
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.StudentProfile
import kotlinx.coroutines.flow.Flow

/**
 * 친구 목록, 친구 일정, 친구 요청.
 * 규칙: 친구의 "비공개" 일정은 제목·장소·유형 없이 시간만 내려온다.
 * (Supabase에서는 RLS + 가려진 뷰로 서버에서 막는다. 앱에서 가리는 게 아님)
 */
interface FriendRepository {
    fun observeFriends(): Flow<List<Friend>>

    /** 친구 일정 (비공개 일정은 이미 내용이 지워진 상태) */
    fun observeFriendSchedules(friendId: String): Flow<List<Schedule>>

    /** 학번으로 숙명인 찾기. 없으면 null. 관계(친구/요청됨 등)도 함께 준다 */
    suspend fun searchByStudentId(studentId: String): Result<Pair<StudentProfile, FriendRelation>?>

    /** 친구 요청 보내기 (friendships에 pending으로 저장) */
    suspend fun sendRequest(userId: String): Result<Unit>

    /** 나에게 온 친구 요청 */
    fun observeIncomingRequests(): Flow<List<FriendRequest>>

    /** 수락 → accepted, 친구 목록과 캘린더 프로필 줄에 바로 반영 */
    suspend fun acceptRequest(requestId: String): Result<Unit>

    /** 거절 → 요청 삭제 */
    suspend fun declineRequest(requestId: String): Result<Unit>
}
