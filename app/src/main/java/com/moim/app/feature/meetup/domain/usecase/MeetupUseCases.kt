package com.moim.app.feature.meetup.domain.usecase

import com.moim.app.feature.meetup.domain.MeetupExplainer
import com.moim.app.feature.meetup.domain.MeetupRepository
import com.moim.app.feature.meetup.domain.MeetupScheduler
import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.model.MeetupMember
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

class ObserveMeetupMembersUseCase @Inject constructor(private val repository: MeetupRepository) {
    operator fun invoke(roomId: String): Flow<List<MeetupMember>> = repository.observeMembers(roomId)
}

/** 추천 시간 + 설명 한 쌍 */
data class ExplainedCandidate(val candidate: MeetupCandidate, val reason: String)

/** 앞으로 days일 중 추천 시간 3개를 고르고(알고리즘) 설명을 붙인다(AI) */
class RecommendMeetupUseCase @Inject constructor(
    private val explainer: MeetupExplainer,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        members: List<MeetupMember>,
        durationMinutes: Long = DEFAULT_DURATION_MINUTES,
        days: Int = 14,
    ): List<ExplainedCandidate> {
        val now = LocalDateTime.now(clock)
        val dates = (0 until days).map { now.toLocalDate().plusDays(it.toLong()) }
        val candidates = MeetupScheduler.recommend(members, dates, durationMinutes, now)
        val reasons = runCatching { explainer.explain(candidates, members) }.getOrDefault(emptyList())
        return candidates.mapIndexed { i, c -> ExplainedCandidate(c, reasons.getOrElse(i) { "" }) }
    }

    companion object {
        const val DEFAULT_DURATION_MINUTES = 120L
    }
}

/**
 * [이 시간으로 모임 만들기]: 사용자가 직접 확정했을 때만 캘린더에 저장하고 방에 안내를 남긴다.
 * 실제 저장 방식은 저장소가 정한다 (목업: 내 캘린더만 / Supabase: 서버 함수가 멤버 전원 캘린더에).
 */
class ConfirmMeetupUseCase @Inject constructor(
    private val repository: MeetupRepository,
) {
    suspend operator fun invoke(roomId: String, roomTitle: String, candidate: MeetupCandidate, reason: String = ""): Result<Unit> =
        repository.confirm(roomId, "$roomTitle 모임", candidate, reason)
}
