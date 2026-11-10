package com.moim.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.moim.app.core.model.Availability
import com.moim.app.core.model.ScheduleType

/** 일정 유형 → 대표 색 (캘린더 점, 카드 왼쪽 띠 등에 사용) */
fun ScheduleType.color(): Color = when (this) {
    ScheduleType.EXAM -> ScheduleExam
    ScheduleType.ASSIGNMENT -> ScheduleAssignment
    ScheduleType.APPOINTMENT -> ScheduleAppointment
    ScheduleType.MEETUP -> ScheduleMeetup
    ScheduleType.CLASS -> ScheduleClass
    ScheduleType.ETC -> ScheduleClass
}

/** 일정 유형 칩(예: [시험])의 배경색과 글자색 */
fun ScheduleType.chipColors(): Pair<Color, Color> = when (this) {
    ScheduleType.EXAM -> PastelPink to PastelPinkText
    ScheduleType.ASSIGNMENT -> PastelYellow to PastelYellowText
    ScheduleType.APPOINTMENT -> SkyBlue to Navy
    ScheduleType.MEETUP -> PastelMint to PastelMintText
    ScheduleType.CLASS, ScheduleType.ETC -> SurfaceContainerHigh to TextSecondary
}

/** 가능 여부 → 색 */
fun Availability.color(): Color = when (this) {
    Availability.ALL -> AvailabilityAll
    Availability.SOME -> AvailabilitySome
    Availability.NONE -> AvailabilityNone
    Availability.UNKNOWN -> AvailabilityUnknown
}
