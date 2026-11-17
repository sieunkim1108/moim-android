package com.moim.app.feature.calendar.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.Visibility
import com.moim.app.ui.theme.ErrorRed
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.OutlineVariant
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import com.moim.app.ui.theme.chipColors
import com.moim.app.ui.theme.color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TimeFormat = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
private val ShortDate = DateTimeFormatter.ofPattern("M/d")

/**
 * 하루 일정 카드 (Stitch: moim_캘린더_첫페이지).
 * 1줄: [유형] 제목 ........ 🕑 시간
 * 2줄: 🔔 알림 (또는 공개 범위) ........ [📍 장소]
 *
 * hiddenOwnerName이 있으면 친구의 비공개 일정 → "OOO님의 일정입니다" + 시간만 보여 준다.
 */
@Composable
fun ScheduleDayCard(
    schedule: Schedule,
    showDelete: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    hiddenOwnerName: String? = null,
) {
    val isHidden = hiddenOwnerName != null
    Surface(
        color = White,
        shape = RoundedCornerShape(MoimDimens.CardRadius),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(if (isHidden) OutlineVariant else schedule.type.color()),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MoimDimens.SpaceMd, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 1줄: 유형 + 제목 + 시간
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isHidden) {
                        TypeChip(schedule.type)
                        Spacer(Modifier.width(MoimDimens.SpaceSm))
                    }
                    Text(
                        text = hiddenOwnerName?.let { stringResource(R.string.calendar_hidden_title, it) } ?: schedule.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isHidden) TextSecondary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = schedule.startAt.format(TimeFormat),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                    )
                }
                // 2줄: 알림/공개 범위 + 장소
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, text) = schedule.detailLine(isHidden)
                    Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    schedule.location?.let { place -> LocationPill(place) }
                }
            }
            if (showDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterVertically)) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.calendar_delete),
                        tint = ErrorRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeChip(type: ScheduleType) {
    val (bg, fg) = type.chipColors()
    Text(
        text = stringResource(type.labelRes()),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun LocationPill(place: String) {
    Row(
        modifier = Modifier
            .padding(start = MoimDimens.SpaceSm)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Navy, modifier = Modifier.size(14.dp))
        Text(place, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
    }
}

/** 2줄 왼쪽: 알림이 있으면 "3일 전 알림 (11/6 시작)", 없으면 공개 범위 */
@Composable
private fun Schedule.detailLine(isHidden: Boolean): Pair<ImageVector, String> {
    if (isHidden) return Icons.Outlined.Lock to stringResource(R.string.calendar_hidden_detail)
    val maxDays = reminderDaysBefore.maxOrNull()
    if (maxDays != null) {
        val start = startAt.toLocalDate().minusDays(maxDays.toLong()).format(ShortDate)
        return Icons.Outlined.NotificationsActive to stringResource(R.string.calendar_reminder, maxDays, start)
    }
    return when (visibility) {
        Visibility.PUBLIC -> Icons.Outlined.Public to stringResource(R.string.visibility_public)
        Visibility.FRIENDS -> Icons.Outlined.Group to stringResource(R.string.visibility_friends)
        Visibility.GROUP -> Icons.Outlined.Forum to stringResource(R.string.visibility_group)
        Visibility.PRIVATE -> Icons.Outlined.Lock to stringResource(R.string.visibility_private)
    }
}

/** 일정 유형 이름 (시험, 과제 ...). 범례·새 일정 등록 칩에서도 쓴다. */
@StringRes
fun ScheduleType.labelRes(): Int = when (this) {
    ScheduleType.EXAM -> R.string.schedule_label_exam
    ScheduleType.ASSIGNMENT -> R.string.schedule_label_assignment
    ScheduleType.APPOINTMENT -> R.string.schedule_label_appointment
    ScheduleType.MEETUP -> R.string.schedule_label_meetup
    ScheduleType.CLASS -> R.string.schedule_label_class
    ScheduleType.ETC -> R.string.schedule_label_etc
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FF)
@Composable
private fun ScheduleDayCardPreview() {
    MoimTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScheduleDayCard(
                schedule = Schedule(
                    id = "1",
                    title = "데이터 분석",
                    type = ScheduleType.EXAM,
                    startAt = LocalDateTime.of(2026, 11, 9, 10, 0),
                    endAt = LocalDateTime.of(2026, 11, 9, 11, 0),
                    location = "명신관 402호",
                    reminderDaysBefore = listOf(3),
                ),
                showDelete = false,
                onDelete = {},
            )
            ScheduleDayCard(
                schedule = Schedule(
                    id = "2",
                    title = "시은이랑 점심",
                    type = ScheduleType.APPOINTMENT,
                    startAt = LocalDateTime.of(2026, 11, 9, 12, 30),
                    endAt = LocalDateTime.of(2026, 11, 9, 13, 30),
                    location = "순헌관 학생식당",
                ),
                showDelete = true,
                onDelete = {},
            )
            ScheduleDayCard(
                schedule = Schedule(
                    id = "3",
                    title = "",
                    type = ScheduleType.ETC,
                    startAt = LocalDateTime.of(2026, 11, 9, 16, 0),
                    endAt = LocalDateTime.of(2026, 11, 9, 17, 0),
                    visibility = Visibility.PRIVATE,
                ),
                showDelete = false,
                onDelete = {},
                hiddenOwnerName = "시은",
            )
        }
    }
}
