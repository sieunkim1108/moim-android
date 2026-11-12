package com.moim.app.feature.meetup.domain

import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.model.MeetupMember
import kotlinx.coroutines.flow.Flow

/** 채팅방 참여자들의 바쁜 시간 (친구 비공개 일정은 내용 없이 시간만) */
interface MeetupRepository {
    fun observeMembers(roomId: String): Flow<List<MeetupMember>>

    /**
     * 사용자가 [이 시간으로 모임 만들기]를 눌렀을 때만 부른다.
     * 방 멤버 모두의 캘린더에 모임 일정(+전날 알림)을 넣고 방에 눈송이 안내를 남긴다.
     */
    suspend fun confirm(roomId: String, title: String, candidate: MeetupCandidate, reason: String): Result<Unit>
}

/**
 * 추천 시간마다 한 줄 설명을 붙여 준다.
 * 규칙: 점수·순위는 MeetupScheduler(코드)가 정하고, 여기(AI)는 설명만 한다.
 * AI 서버가 있으면 서버의 Claude가, 없으면 규칙(FakeMeetupExplainer)이 문장을 만든다.
 */
interface MeetupExplainer {
    suspend fun explain(candidates: List<MeetupCandidate>, members: List<MeetupMember>): List<String>
}
