package com.moim.app.feature.friend.domain.usecase

import com.moim.app.core.data.FriendRepository
import com.moim.app.core.model.FriendRelation
import com.moim.app.core.model.FriendRequest
import com.moim.app.core.model.FriendSearchResult
import com.moim.app.core.model.StudentProfile
import com.moim.app.feature.auth.domain.SignUpValidator
import com.moim.app.feature.auth.domain.repository.AuthRepository
import com.moim.app.feature.dm.domain.DmRepository
import com.moim.app.feature.dm.domain.model.RoomType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 학번으로 친구 찾기 (S-12).
 * 1) 학번 형식 검사 2) 내 학번이면 ME 3) 저장소에서 검색 4) 둘 다 속한 단체방 이름(공통 모임)을 붙인다.
 * 결과가 없으면 success(null).
 */
class SearchFriendUseCase @Inject constructor(
    private val friendRepository: FriendRepository,
    private val dmRepository: DmRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(studentId: String): Result<FriendSearchResult?> {
        val id = studentId.trim()
        if (!SignUpValidator.isValidStudentId(id)) return Result.failure(IllegalArgumentException("invalid student id"))

        val me = authRepository.currentUser.value
        if (me != null && me.studentId == id) {
            val myProfile = StudentProfile(id = me.id, name = me.name, studentId = me.studentId, major = me.major)
            return Result.success(FriendSearchResult(myProfile, FriendRelation.ME))
        }

        return friendRepository.searchByStudentId(id).map { found ->
            found?.let { (profile, relation) ->
                FriendSearchResult(
                    profile = profile,
                    relation = relation,
                    commonRoomNames = commonGroupRooms(profile.id),
                )
            }
        }
    }

    /** 공통 모임 = 그 사람이 멤버로 있는 내 단체 DM 방 (room_members로 계산) */
    private suspend fun commonGroupRooms(userId: String): List<String> =
        dmRepository.observeRooms().first()
            .filter { room -> room.type == RoomType.GROUP && room.members.any { it.id == userId } }
            .map { it.title }
}

class SendFriendRequestUseCase @Inject constructor(
    private val repository: FriendRepository,
) {
    suspend operator fun invoke(userId: String): Result<Unit> = repository.sendRequest(userId)
}

/** 받은 친구 요청 (최근 것이 위로) */
class ObserveFriendRequestsUseCase @Inject constructor(
    private val repository: FriendRepository,
) {
    operator fun invoke(): Flow<List<FriendRequest>> = repository.observeIncomingRequests()
}

class RespondFriendRequestUseCase @Inject constructor(
    private val repository: FriendRepository,
) {
    suspend fun accept(requestId: String): Result<Unit> = repository.acceptRequest(requestId)

    suspend fun decline(requestId: String): Result<Unit> = repository.declineRequest(requestId)
}
