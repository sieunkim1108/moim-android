package com.moim.app.feature.meetup.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.feature.meetup.domain.MeetupScheduler
import com.moim.app.feature.meetup.domain.model.BusyBlock
import com.moim.app.feature.meetup.domain.model.MeetupMember
import com.moim.app.feature.meetup.domain.model.TimeRange
import com.moim.app.ui.component.InitialAvatar
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.PastelMint
import com.moim.app.ui.theme.PastelMintText
import com.moim.app.ui.theme.SurfaceContainerHigh
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

private const val NAME_WIDTH_DP = 72
private val TOTAL_MINUTES = Duration.between(MeetupScheduler.DAY_START, MeetupScheduler.DAY_END).toMinutes().toFloat()

/**
 * 멤버별 하루 타임라인 (오전 9시 ~ 오후 10시).
 * 회색 = 바쁜 시간 (비공개는 🔒만), 초록 = 전원 가능 시간대
 */
@Composable
fun MemberTimeline(
    members: List<MeetupMember>,
    date: LocalDate,
    commonFree: List<TimeRange>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HourAxis(commonFree)
        members.forEach { member ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.width(NAME_WIDTH_DP.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InitialAvatar(member.name, member.id, size = 28.dp)
                    Text(
                        text = if (member.isMe) stringResource(R.string.calendar_me) else member.name.takeLast(2),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                    )
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerLow),
                ) {
                    val width = maxWidth
                    commonFree.forEach { range ->
                        Segment(range.start, range.end, width) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .background(PastelMint),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(stringResource(R.string.meetup_free), style = MaterialTheme.typography.labelSmall, color = PastelMintText)
                            }
                        }
                    }
                    member.busy.filter { it.date == date }.forEach { block -> BusySegment(block, width) }
                }
            }
        }
    }
}

/** 상단 시간 눈금: 09 10 ... 21 (전원 가능 시간은 굵게) */
@Composable
private fun HourAxis(commonFree: List<TimeRange>) {
    Row(Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(NAME_WIDTH_DP.dp))
        val hours = (MeetupScheduler.DAY_START.hour until MeetupScheduler.DAY_END.hour)
        Row(Modifier.weight(1f)) {
            hours.forEach { h ->
                val bold = commonFree.any { LocalTime.of(h, 0) >= it.start && LocalTime.of(h, 0) < it.end }
                Text(
                    text = "%02d".format(h),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                    color = if (bold) Navy else TextSecondary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BusySegment(block: BusyBlock, width: Dp) {
    val start = maxOf(block.start, MeetupScheduler.DAY_START)
    val end = minOf(block.end, MeetupScheduler.DAY_END)
    if (!start.isBefore(end)) return
    Segment(start, end, width) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainerHigh)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (block.label == null) {
                Icon(Icons.Outlined.Lock, contentDescription = stringResource(R.string.meetup_private), tint = TextSecondary, modifier = Modifier.size(12.dp))
            } else {
                Text(
                    text = block.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** start~end 위치에 content를 놓는다 (전체 폭 width 기준) */
@Composable
private fun Segment(start: LocalTime, end: LocalTime, width: Dp, content: @Composable () -> Unit) {
    val from = Duration.between(MeetupScheduler.DAY_START, start).toMinutes() / TOTAL_MINUTES
    val length = Duration.between(start, end).toMinutes() / TOTAL_MINUTES
    Box(
        Modifier
            .offset(x = width * from)
            .width(width * length)
            .fillMaxHeight(),
    ) { content() }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MemberTimelinePreview() {
    val day = LocalDate.of(2026, 11, 14)
    MoimTheme {
        MemberTimeline(
            members = listOf(
                MeetupMember("me", "나", true, listOf(BusyBlock(day, LocalTime.of(10, 0), LocalTime.of(12, 0), "데산 수업"))),
                MeetupMember("a", "김시은", false, listOf(BusyBlock(day, LocalTime.of(9, 30), LocalTime.of(11, 30), "알고리즘"))),
                MeetupMember("b", "박지원", false, listOf(BusyBlock(day, LocalTime.of(17, 0), LocalTime.of(19, 0), null))),
            ),
            date = day,
            commonFree = listOf(TimeRange(LocalTime.of(15, 0), LocalTime.of(17, 0))),
            modifier = Modifier.padding(16.dp),
        )
    }
}
