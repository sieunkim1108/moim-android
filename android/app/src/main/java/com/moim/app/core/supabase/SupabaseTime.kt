package com.moim.app.core.supabase

import com.moim.app.core.di.TimeModule
import java.time.LocalDateTime
import java.time.OffsetDateTime

/*
 * DB(timestamptz)는 시각을 시간대와 함께 저장하고, 앱 모델은 한국 시간 LocalDateTime을 쓴다.
 * 오갈 때 항상 Asia/Seoul 기준으로 바꾼다.
 */
internal fun LocalDateTime.toDbTime(): String =
    atZone(TimeModule.SeoulZone).toOffsetDateTime().toString()

internal fun String.fromDbTime(): LocalDateTime =
    OffsetDateTime.parse(normalizeDbTime(this)).atZoneSameInstant(TimeModule.SeoulZone).toLocalDateTime()

/**
 * Postgres가 주는 시각 문자열을 ISO 형식으로 맞춘다.
 * "2026-11-10 04:20:11.12+00" (Realtime) → "2026-11-10T04:20:11.12+00:00"
 */
internal fun normalizeDbTime(raw: String): String {
    var s = raw.trim().replace(' ', 'T')
    if (s.endsWith("Z")) return s
    val offset = Regex("[+-]\\d{2}(:?\\d{2})?$").find(s) ?: return "${s}Z"
    if (offset.value.length == 3) s += ":00" // +00 → +00:00
    else if (!offset.value.contains(':')) s = s.dropLast(2) + ":" + s.takeLast(2) // +0900 → +09:00
    return s
}
