package com.moim.app.feature.dm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.usecase.ExplainedCandidate
import com.moim.app.ui.theme.AvailabilityAll
import com.moim.app.ui.theme.AvailabilitySome
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.OutlineVariant
import com.moim.app.ui.theme.PastelMint
import com.moim.app.ui.theme.PastelMintText
import com.moim.app.ui.theme.PastelYellow
import com.moim.app.ui.theme.PastelYellowText
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CandidateFormat = DateTimeFormatter.ofPattern("M/d(E) a h:mm", Locale.KOREAN)

/** 단체방 안 "눈송이 추천 모임 시간" 카드 (Stitch: moim_단체채팅방) */
@Composable
fun MeetupSuggestionCard(
    memberCount: Int,
    suggestions: List<ExplainedCandidate>?,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onMakeVote: () -> Unit,
    onViewCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = White,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SecondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Navy)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.room_suggest_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, tint = Navy, modifier = Modifier.size(14.dp))
                        Text(
                            stringResource(R.string.room_suggest_synced, memberCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }

            when {
                suggestions == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    CircularProgressIndicator(color = Navy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.room_suggest_loading), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                suggestions.isEmpty() -> Text(
                    stringResource(R.string.room_suggest_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                else -> suggestions.forEachIndexed { index, item ->
                    CandidateRow(item, selected = index == selectedIndex, onClick = { onSelect(index) })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm), modifier = Modifier.padding(top = 4.dp)) {
                Button(
                    onClick = onMakeVote,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Icon(Icons.Outlined.HowToVote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.room_make_vote), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onViewCalendar,
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryContainer, contentColor = Navy),
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.room_view_calendar), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(item: ExplainedCandidate, selected: Boolean, onClick: () -> Unit) {
    val c = item.candidate
    val (chipBg, chipFg) = if (c.isEveryone) PastelMint to PastelMintText else PastelYellow to PastelYellowText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (c.isEveryone) AvailabilityAll else AvailabilitySome),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = c.date.atTime(c.start).format(CandidateFormat),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            if (item.reason.isNotBlank()) {
                Text(item.reason, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
            }
        }
        Text(
            text = stringResource(R.string.room_available_count, c.availableCount),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = chipFg,
            modifier = Modifier
                .clip(CircleShape)
                .background(chipBg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(10.dp))
        // 동그라미 라디오
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(White)
                .border(if (selected) 7.dp else 1.dp, if (selected) Navy else OutlineVariant, CircleShape),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFEDF5FF)
@Composable
private fun MeetupSuggestionCardPreview() {
    MoimTheme {
        MeetupSuggestionCard(
            memberCount = 3,
            suggestions = listOf(
                ExplainedCandidate(
                    MeetupCandidate(LocalDate.of(2026, 11, 14), LocalTime.of(14, 0), LocalTime.of(16, 0), listOf("a", "b", "c"), 3),
                    "주말 오후라 모두 여유롭게 만날 수 있어요.",
                ),
                ExplainedCandidate(
                    MeetupCandidate(LocalDate.of(2026, 11, 12), LocalTime.of(19, 0), LocalTime.of(21, 0), listOf("a", "b"), 3),
                    "시은 일정이 겹쳐서 조율이 필요해요.",
                ),
            ),
            selectedIndex = 0,
            onSelect = {},
            onMakeVote = {},
            onViewCalendar = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
