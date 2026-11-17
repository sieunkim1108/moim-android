package com.moim.app.core.data

import com.moim.app.core.model.Friend
import com.moim.app.core.model.FriendRelation
import com.moim.app.core.model.FriendRequest
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.StudentProfile
import com.moim.app.core.model.Visibility
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 메모리에만 있는 목업 친구 저장소. 처음엔 친구가 0명이다.
 * directory는 Supabase의 profiles 테이블 흉내(검색 대상 숙명인 명단)라서,
 * 학번(예: 2314582)으로 검색해 친구 요청 흐름을 확인할 수 있다.
 */
@Singleton
class FakeFriendRepository @Inject constructor() : FriendRepository {

    /** 가짜 "숙명인 명단" (profiles 테이블 흉내). 검색은 여기서 찾는다. */
    private val directory = listOf(
        StudentProfile(FRIEND_SIEUN, "김시은", "2216001", "소프트웨어학부"),
        StudentProfile(FRIEND_JIWON, "박지원", "2216002", "통계학과"),
        StudentProfile(FRIEND_MINSEO, "최민서", "2216003", "인공지능공학부"),
        StudentProfile("student-kms", "김민서", "2314582", "컴퓨터과학전공"),
        StudentProfile("student-ljw", "이지원", "2418820", "컴퓨터과학전공"),
        StudentProfile("student-jhn", "정하늘", "2412345", "데이터사이언스전공"),
    )

    // 처음엔 친구·받은 요청·친구 일정 모두 없음 (새 가입자 상태)
    private val friends = MutableStateFlow<List<Friend>>(emptyList())

    /** 내가 보낸 요청 (상대 id) */
    private val sentRequests = MutableStateFlow<Set<String>>(emptySet())

    /** 나에게 온 요청 */
    private val incoming = MutableStateFlow<List<FriendRequest>>(emptyList())

    /** 친구별 일정 (가짜 친구가 없으니 비어 있음) */
    private val schedules: Map<String, List<Schedule>> = emptyMap()

    override fun observeFriends(): Flow<List<Friend>> = friends.asStateFlow()

    override suspend fun searchByStudentId(studentId: String): Result<Pair<StudentProfile, FriendRelation>?> {
        delay(FAKE_DELAY_MS)
        val profile = directory.firstOrNull { it.studentId == studentId } ?: return Result.success(null)
        val relation = when {
            friends.value.any { it.id == profile.id } -> FriendRelation.FRIEND
            profile.id in sentRequests.value -> FriendRelation.REQUESTED
            incoming.value.any { it.from.id == profile.id } -> FriendRelation.INCOMING
            else -> FriendRelation.NONE
        }
        return Result.success(profile to relation)
    }

    override suspend fun sendRequest(userId: String): Result<Unit> {
        delay(FAKE_DELAY_MS)
        sentRequests.update { it + userId }
        return Result.success(Unit)
    }

    override fun observeIncomingRequests(): Flow<List<FriendRequest>> = incoming.asStateFlow()

    override suspend fun acceptRequest(requestId: String): Result<Unit> {
        val request = incoming.value.firstOrNull { it.id == requestId }
            ?: return Result.failure(NoSuchElementException(requestId))
        incoming.update { list -> list.filterNot { it.id == requestId } }
        friends.update { it + Friend(id = request.from.id, name = request.from.name, major = request.from.major) }
        return Result.success(Unit)
    }

    override suspend fun declineRequest(requestId: String): Result<Unit> {
        incoming.update { list -> list.filterNot { it.id == requestId } }
        return Result.success(Unit)
    }

    override fun observeFriendSchedules(friendId: String): Flow<List<Schedule>> =
        flowOf(schedules[friendId].orEmpty().map(::hidePrivate))

    /**
     * 서버가 해 줄 일을 흉내: 비공개 일정은 시간만 남긴다.
     * 그룹공개 일정은 서버(RLS)가 "내가 속한 방에 공개된 것"만 제목을 보여 준다.
     * 가짜 친구들은 그룹공개 일정이 없어서 여기서는 따로 처리하지 않는다.
     */
    private fun hidePrivate(schedule: Schedule): Schedule =
        if (schedule.visibility != Visibility.PRIVATE) {
            schedule
        } else {
            schedule.copy(title = "", type = ScheduleType.ETC, location = null, memo = null, reminderDaysBefore = emptyList())
        }

    companion object {
        const val FRIEND_SIEUN = "friend-sieun"
        const val FRIEND_JIWON = "friend-jiwon"
        const val FRIEND_MINSEO = "friend-minseo"
        private const val FAKE_DELAY_MS = 400L
    }
}
