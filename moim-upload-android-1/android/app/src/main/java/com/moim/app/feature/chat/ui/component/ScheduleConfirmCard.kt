package com.moim.app.feature.chat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.core.model.ScheduleType
import com.moim.app.feature.chat.domain.model.CardStatus
import com.moim.app.feature.chat.domain.model.ScheduleDraft
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.OutlineVariant
import com.moim.app.ui.theme.PastelMintText
import com.moim.app.ui.theme.SurfaceContainerHigh
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import com.moim.app.ui.theme.chipColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CardTimeFormat = DateTimeFormatter.ofPattern("M/d(E) a h:mm", Locale.KOREAN)
private val CardDeadlineFormat = DateTimeFormatter.ofPattern("M/d(E) HH:mm", Locale.KOREAN)

/**
 * 눈송이가 뽑은 일정 확인 카드.
 * 규칙: AI가 만든 일정은 여기서 [등록하기]를 눌러야 저장된다.
 */
@Composable
fun ScheduleConfirmCard(
    drafts: List<ScheduleDraft>,
    status: CardStatus,
    onRegister: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = White,
        shape = RoundedCornerShape(MoimDimens.CardRadius),
        shadowElevation = 1.dp,
        modifier = modifier.widthIn(max = MoimDimens.ChatBubbleMaxWidth),
    ) {
        Column(
            modifier = Modifier.padding(MoimDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
        ) {
            drafts.forEach { DraftItem(it) }

            HorizontalDivider(color = OutlineVariant, modifier = Modifier.padding(vertical = MoimDimens.SpaceXs))

            when (status) {
                CardStatus.PENDING -> CardButtons(onRegister = onRegister, onEdit = onEdit)
                CardStatus.REGISTERING -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Navy, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
                CardStatus.REGISTERED -> CardResult(
                    text = stringResource(R.string.card_registered),
                    color = PastelMintText,
                    icon = Icons.Outlined.CheckCircle,
                )
                CardStatus.DISMISSED -> CardResult(
                    text = stringResource(R.string.card_dismissed),
                    color = TextSecondary,
                    icon = null,
                )
            }
        }
    }
}

@Composable
private fun DraftItem(draft: ScheduleDraft) {
    val (chipBg, chipText) = draft.type.chipColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.InputRadius))
            .background(SurfaceContainerLow)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(chipBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(draft.type.icon(), contentDescription = null, tint = chipText, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(draft.type.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    color = chipText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(chipBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                Text(
                    text = draft.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (draft.isDeadline) Icons.Outlined.Timer else Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = draft.whenText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun CardButtons(onRegister: () -> Unit, onEdit: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm)) {
        Button(
            onClick = onRegister,
            colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
            shape = RoundedCornerShape(MoimDimens.InputRadius),
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
        ) {
            Text(stringResource(R.string.card_register), style = MaterialTheme.typography.labelLarge)
        }
        Button(
            onClick = onEdit,
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHigh, contentColor = TextPrimary),
            shape = RoundedCornerShape(MoimDimens.InputRadius),
            modifier = Modifier.height(44.dp),
        ) {
            Text(stringResource(R.string.card_edit), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CardResult(text: String, color: androidx.compose.ui.graphics.Color, icon: ImageVector?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

/** "11/9(월) 오전 10:00" 또는 "11/6(금) 23:59 마감" */
@Composable
private fun ScheduleDraft.whenText(): String {
    val date = date ?: return ""
    val time = time ?: return ""
    val dateTime = date.atTime(time)
    return if (isDeadline) {
        stringResource(R.string.card_deadline_suffix, dateTime.format(CardDeadlineFormat))
    } else {
        dateTime.format(CardTimeFormat)
    }
}

private fun ScheduleType.icon(): ImageVector = when (this) {
    ScheduleType.EXAM -> Icons.Outlined.School
    ScheduleType.ASSIGNMENT -> Icons.Outlined.EditCalendar
    ScheduleType.APPOINTMENT -> Icons.Outlined.Event
    ScheduleType.MEETUP -> Icons.Outlined.Groups
    ScheduleType.CLASS -> Icons.AutoMirrored.Outlined.MenuBook
    ScheduleType.ETC -> Icons.Outlined.Event
}

private fun ScheduleType.labelRes(): Int = when (this) {
    ScheduleType.EXAM -> R.string.schedule_type_exam
    ScheduleType.ASSIGNMENT -> R.string.schedule_type_assignment
    ScheduleType.APPOINTMENT -> R.string.schedule_type_appointment
    ScheduleType.MEETUP -> R.string.schedule_type_meetup
    ScheduleType.CLASS -> R.string.schedule_type_class
    ScheduleType.ETC -> R.string.schedule_type_etc
}

private val PreviewDrafts = listOf(
    ScheduleDraft("1", "데이터 분석 시험", ScheduleType.EXAM, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0)),
    ScheduleDraft("2", "과제 제출", ScheduleType.ASSIGNMENT, LocalDate.of(2026, 11, 6), LocalTime.of(23, 59), isDeadline = true),
)

@Preview(showBackground = true, backgroundColor = 0xFFEDF5FF)
@Composable
private fun ScheduleConfirmCardPreview() {
    MoimTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScheduleConfirmCard(PreviewDrafts, CardStatus.PENDING, onRegister = {}, onEdit = {})
            ScheduleConfirmCard(PreviewDrafts.take(1), CardStatus.REGISTERED, onRegister = {}, onEdit = {})
        }
    }
}
