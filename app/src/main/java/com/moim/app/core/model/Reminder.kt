package com.moim.app.core.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 눈송이 대화에서 "좋아! 알림 보내줘"라고 답해서 생긴 준비 알림.
 * [from]부터 [eventDate] 전날까지 매일 오전 9시에 알린다.
 * ("하루 전만"이면 from = eventDate - 1일)
 */
data class ChatReminder(
    val title: String,
    val eventDate: LocalDate,
    val from: LocalDate,
)

/** 실제로 휴대폰에 예약할 알림 하나 */
data class PlannedReminder(
    val key: String, // 같은 알림을 다시 예약할 때 덮어쓰기 위한 고유 이름
    val triggerAt: LocalDateTime,
    val title: String,
    val body: String,
)
