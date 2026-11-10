package com.moim.app.feature.meetup.data

import com.moim.app.feature.meetup.domain.MeetupExplainer
import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.model.MeetupMember
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/** AI 설명 흉내: 규칙으로 문장을 만든다 (서버 AI 연결 전까지) */
class FakeMeetupExplainer @Inject constructor() : MeetupExplainer {
    override suspend fun explain(candidates: List<MeetupCandidate>, members: List<MeetupMember>): List<String> =
        candidates.map { c ->
            val weekend = c.date.dayOfWeek == DayOfWeek.SATURDAY || c.date.dayOfWeek == DayOfWeek.SUNDAY
            val missing = members.filter { it.id !in c.availableMemberIds }.map { it.name.takeLast(2) }
            when {
                missing.isNotEmpty() -> "${missing.joinToString()} 일정이 겹쳐서 조율이 필요해요."
                weekend -> "주말 오후라 모두 여유롭게 만날 수 있어요."
                c.start >= LocalTime.of(17, 0) -> "수업이 다 끝난 저녁이라 저녁 먹으면서 하기 좋아요."
                else -> "모두 수업이 끝난 직후라 순헌관/도서관 이동이 편해요!"
            }
        }
}
