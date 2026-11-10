package com.moim.app.feature.meetup.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.core.model.Availability
import com.moim.app.feature.calendar.domain.CalendarMath
import com.moim.app.ui.theme.ErrorRed
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.OutlineVariant
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import com.moim.app.ui.theme.color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** 칸 배경: 가능 여부 색을 아주 연하게 */
private fun Availability.cellBackground(): Color = when (this) {
    Availability.UNKNOWN -> SurfaceContainerLow
    else -> color().copy(alpha = 0.18f)
}

/**
 * 모임용 달력: 날짜 칸 색 = 그날 멤버 가능 여부 (전원/일부/겹침/미입력)
 */
@Composable
fun AvailabilityCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    availability: Map<LocalDate, Availability>,
    onDateSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels = stringArrayResource(R.array.calendar_weekdays)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (i) {
                        0 -> ErrorRed
                        6 -> Navy
                        else -> TextSecondary
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        CalendarMath.monthGrid(month).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { date ->
                    val inMonth = YearMonth.from(date) == month
                    val status = availability[date] ?: Availability.UNKNOWN
                    val selected = date == selectedDate
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.95f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    !inMonth -> Color.Transparent
                                    selected -> Navy
                                    else -> status.cellBackground()
                                },
                            )
                            .clickable(enabled = inMonth) { onDateSelect(date) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                !inMonth -> OutlineVariant
                                selected -> White
                                date.dayOfWeek == DayOfWeek.SUNDAY -> ErrorRed
                                date.dayOfWeek == DayOfWeek.SATURDAY -> Navy
                                else -> TextPrimary
                            },
                        )
                        if (inMonth) {
                            Box(
                                Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (status == Availability.UNKNOWN) OutlineVariant else status.color()),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** ● 전원 가능(4) ● 일부 가능(3) ● 시간 겹침 ● 미입력 */
@Composable
fun AvailabilityLegend(counts: Map<Availability, Int>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        listOf(
            Availability.ALL to R.string.meetup_legend_all,
            Availability.SOME to R.string.meetup_legend_some,
            Availability.NONE to R.string.meetup_legend_none,
            Availability.UNKNOWN to R.string.meetup_legend_unknown,
        ).forEach { (status, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (status == Availability.UNKNOWN) OutlineVariant else status.color()),
                )
                val count = counts[status]
                val text = stringResource(label) +
                    if (count != null && (status == Availability.ALL || status == Availability.SOME)) "($count)" else ""
                Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AvailabilityCalendarPreview() {
    MoimTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val month = YearMonth.of(2026, 11)
            AvailabilityCalendar(
                month = month,
                selectedDate = LocalDate.of(2026, 11, 14),
                availability = (1..30).associate { d ->
                    month.atDay(d) to Availability.entries[d % 4]
                },
                onDateSelect = {},
            )
            AvailabilityLegend(mapOf(Availability.ALL to 4, Availability.SOME to 3))
        }
    }
}
