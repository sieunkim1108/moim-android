package com.moim.app.feature.calendar.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleType
import com.moim.app.feature.calendar.domain.CalendarMath
import com.moim.app.ui.theme.ErrorRed
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.ShadowNavy
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import com.moim.app.ui.theme.color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * 월간 달력 카드 (Stitch: moim_캘린더_첫페이지).
 * 오늘 = 연하늘 동그라미, 선택한 날 = 남색 동그라미, 일요일 = 빨강, 토요일 = 남색.
 * 날짜 아래 점 = 그날 일정 유형 색 (최대 3개). 다른 달 날짜 칸은 비워 둔다.
 */
@Composable
fun MonthCalendarCard(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    schedulesByDate: Map<LocalDate, List<Schedule>>,
    onDateSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = White,
        shape = RoundedCornerShape(MoimDimens.CardRadius),
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(MoimDimens.CardRadius), ambientColor = ShadowNavy, spotColor = ShadowNavy),
    ) {
        Column(Modifier.padding(MoimDimens.SpaceMd)) {
            WeekdayLabels()
            Spacer(Modifier.height(MoimDimens.SpaceSm))
            CalendarMath.monthGrid(month).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (YearMonth.from(date) == month) {
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                isToday = date == today,
                                schedules = schedulesByDate[date].orEmpty(),
                                onClick = { onDateSelect(date) },
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayLabels() {
    val labels = stringArrayResource(R.array.calendar_weekdays)
    Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val color = when (index) {
                0 -> ErrorRed
                6 -> Navy
                else -> TextSecondary
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (index == 0 || index == 6) FontWeight.Bold else FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    schedules: List<Schedule>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isSelected -> Navy
        isToday -> SecondaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> White
        isToday -> Navy
        date.dayOfWeek == DayOfWeek.SUNDAY -> ErrorRed
        date.dayOfWeek == DayOfWeek.SATURDAY -> Navy
        else -> TextPrimary
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(if (isSelected) Modifier.shadow(4.dp, CircleShape, spotColor = ShadowNavy) else Modifier)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            schedules.map { it.type }.distinct().take(3).forEach { type ->
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(type.color()),
                )
            }
        }
    }
}

/** 달력 아래 범례: ● 시험 ● 과제 ● 약속 ● 모임 */
@Composable
fun ScheduleLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm, Alignment.CenterHorizontally),
    ) {
        listOf(ScheduleType.EXAM, ScheduleType.ASSIGNMENT, ScheduleType.APPOINTMENT, ScheduleType.MEETUP).forEach { type ->
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(White)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(type.color()),
                )
                Text(
                    text = stringResource(type.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FF)
@Composable
private fun MonthCalendarCardPreview() {
    MoimTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MonthCalendarCard(
                month = YearMonth.of(2026, 11),
                selectedDate = LocalDate.of(2026, 11, 9),
                today = LocalDate.of(2026, 11, 2),
                schedulesByDate = emptyMap(),
                onDateSelect = {},
            )
            ScheduleLegend()
        }
    }
}
