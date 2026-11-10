package com.moim.app.core.data

import com.moim.app.core.model.Friend
import com.moim.app.core.model.FriendRelation
import com.moim.app.core.model.FriendRequest
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleSource
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.StudentProfile
import com.moim.app.core.model.Visibility
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 화면 확인용 가짜 친구 3명과 그 일정 (오늘 기준으로 만든다).
 * 매주 반복되는 수업(시간표) + 그때그때 일정.
 * 친구 추가(S-12) 확인용: 학번 2314582(김민서)를 검색해 요청할 수 있고, 받은 요청 2건이 있다.
 */
@Singleton
class FakeFriendRepository @Inject constructor(
    clock: Clock,
) : FriendRepository {

    private val today = LocalDate.now(clock)
    private val now = LocalDateTime.now(clock)

    /** 가짜 "숙명인 명단" (profiles 테이블 흉내). 검색은 여기서 찾는다. */
    private val directory = listOf(
        StudentProfile(FRIEND_SIEUN, "김시은", "2216001", "소프트웨어학부"),
        StudentProfile(FRIEND_JIWON, "박지원", "2216002", "통계학과"),
        StudentProfile(FRIEND_MINSEO, "최민서", "2216003", "인공지능공학부"),
        StudentProfile("student-kms", "김민서", "2314582", "컴퓨터과학전공"),
        StudentProfile("student-ljw", "이지원", "2418820", "컴퓨터과학전공"),
        StudentProfile("student-jhn", "정하늘", "2412345", "데이터사이언스전공"),
    )

    private val friends = MutableStateFlow(
        listOf(
            Friend(id = FRIEND_SIEUN, name = "김시은", major = "소프트웨어학부"),
            Friend(id = FRIEND_JIWON, name = "박지원", major = "통계학과"),
            Friend(id = FRIEND_MINSEO, name = "최민서", major = "인공지능공학부"),
        ),
    )

    /** 내가 보낸 요청 (상대 id) */
    private val sentRequests = MutableStateFlow<Set<String>>(emptySet())

    /** 나에게 온 요청 */
    private val incoming = MutableStateFlow(
        listOf(
            FriendRequest(
                id = "req-ljw",
                from = directory.first { it.id == "student-ljw" },
                requestedAt = now.minusDays(1),
                message = "빅데이터분석론 같은 조 화이팅해요!",
            ),
            FriendRequest(
                id = "req-jhn",
                from = directory.first { it.id == "student-jhn" },
                requestedAt = now.minusDays(3),
                viaRoomName = "눈송이 해커톤 24기",
            ),
        ),
    )

    private val schedules: Map<String, List<Schedule>> = mapOf(
        FRIEND_SIEUN to weekly("알고리즘", listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY), 9, 30, 11, 0) +
            listOf(
                schedule("s1", "시은이랑 점심", ScheduleType.APPOINTMENT, today.atTime(12, 30), location = "순헌관 학생식당"),
                schedule("s2", "운영체제 쪽지시험", ScheduleType.EXAM, today.plusDays(2).atTime(10, 0), location = "명신관 402호"),
                schedule("s3", "병원", ScheduleType.APPOINTMENT, today.plusDays(1).atTime(16, 0), visibility = Visibility.PRIVATE),
            ),
        FRIEND_JIWON to weekly("통계학", listOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), 11, 0, 13, 30) +
            listOf(
                schedule("j1", "모바일 소프트웨어 팀플", ScheduleType.MEETUP, today.plusDays(1).atTime(15, 0), location = "순헌관 301호"),
                schedule("j2", "알바", ScheduleType.APPOINTMENT, today.atTime(18, 0), hours = 3, visibility = Visibility.PRIVATE),
            ),
        FRIEND_MINSEO to weekly("딥러닝", listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 13, 0, 14, 15) +
            listOf(
                schedule("m1", "동아리", ScheduleType.APPOINTMENT, today.plusDays(3).atTime(17, 0), hours = 3, visibility = Visibility.PRIVATE),
            ),
    )

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

    /** 이번 주부터 8주 동안 매주 같은 요일·시간 수업 */
    private fun weekly(
        title: String,
        days: List<DayOfWeek>,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
    ): List<Schedule> {
        val firstWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L until 8L).flatMap { week ->
            days.map { day ->
                val date = firstWeek.plusWeeks(week).with(TemporalAdjusters.nextOrSame(day))
                Schedule(
                    id = "$title-$date",
                    title = title,
                    type = ScheduleType.CLASS,
                    startAt = date.atTime(LocalTime.of(startHour, startMinute)),
                    endAt = date.atTime(LocalTime.of(endHour, endMinute)),
                    source = ScheduleSource.TIMETABLE,
                )
            }
        }
    }

    private fun schedule(
        id: String,
        title: String,
        type: ScheduleType,
        start: LocalDateTime,
        location: String? = null,
        hours: Long = 1,
        visibility: Visibility = Visibility.FRIENDS,
    ) = Schedule(
        id = id,
        title = title,
        type = type,
        startAt = start,
        endAt = start.plusHours(hours),
        visibility = visibility,
        source = ScheduleSource.MANUAL,
        location = location,
    )

    companion object {
        const val FRIEND_SIEUN = "friend-sieun"
        const val FRIEND_JIWON = "friend-jiwon"
        const val FRIEND_MINSEO = "friend-minseo"
        private const val FAKE_DELAY_MS = 400L
    }
}
