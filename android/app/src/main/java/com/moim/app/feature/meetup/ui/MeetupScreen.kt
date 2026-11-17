package com.moim.app.feature.meetup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.core.model.Availability
import com.moim.app.feature.meetup.domain.MeetupScheduler
import com.moim.app.feature.meetup.domain.model.MeetupCandidate
import com.moim.app.feature.meetup.domain.model.TimeRange
import com.moim.app.feature.meetup.domain.usecase.ExplainedCandidate
import com.moim.app.feature.meetup.ui.component.AvailabilityCalendar
import com.moim.app.feature.meetup.ui.component.AvailabilityLegend
import com.moim.app.feature.meetup.ui.component.MemberTimeline
import com.moim.app.ui.component.NunsongiAvatar
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.NavyDeep
import com.moim.app.ui.theme.OutlineVariant
import com.moim.app.ui.theme.PastelMint
import com.moim.app.ui.theme.PastelMintText
import com.moim.app.ui.theme.PastelPink
import com.moim.app.ui.theme.PastelPinkText
import com.moim.app.ui.theme.PastelYellow
import com.moim.app.ui.theme.PastelYellowText
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import kotlin.math.roundToLong
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayFormat = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
private val HourFormat = DateTimeFormatter.ofPattern("a h시", Locale.KOREAN)

@Composable
fun MeetupRoute(
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
    viewModel: MeetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.confirmed) {
        if (uiState.confirmed) {
            viewModel.onNavigatedBack()
            onConfirmed()
        }
    }
    MeetupScreen(
        uiState = uiState,
        onBack = onBack,
        onDateSelect = viewModel::onDateSelect,
        onPrevMonth = viewModel::onPrevMonth,
        onNextMonth = viewModel::onNextMonth,
        onRecommendationSelect = viewModel::onRecommendationSelect,
        onConfirm = viewModel::onConfirm,
        onMessageShown = viewModel::onMessageShown,
        onDurationChange = viewModel::onDurationChange,
    )
}

/**
 * 10. 모임 일정 잡기 (Stitch: moim_단톡방 모임 일정 잡기)
 * 달력(날짜별 가능 여부) → 멤버별 타임라인 → 눈송이 추천 → [이 시간으로 모임 만들기]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetupScreen(
    uiState: MeetupUiState,
    onBack: () -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRecommendationSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
    onDurationChange: (Long) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val failText = stringResource(R.string.meetup_confirm_failed)
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) {
            onMessageShown()
            snackbarHostState.showSnackbar(failText)
        }
    }
    val memberCount = uiState.members.size

    Scaffold(
        modifier = modifier,
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = TextPrimary)
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.meetup_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyDeep)
                        uiState.room?.let {
                            Text(
                                stringResource(R.string.meetup_subtitle, it.title, it.members.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = White),
            )
        },
        bottomBar = {
            ConfirmBar(
                enabled = !uiState.recommendations.isNullOrEmpty() && !uiState.isConfirming,
                isLoading = uiState.isConfirming,
                onConfirm = onConfirm,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(MoimDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceMd),
        ) {
            // 1. 달력 카드
            item(key = "calendar") {
                WhiteCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.calendar_month_title, uiState.visibleMonth.year, uiState.visibleMonth.monthValue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.calendar_prev_month), tint = TextSecondary)
                        }
                        IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.calendar_next_month), tint = TextSecondary)
                        }
                        Spacer(Modifier.weight(1f))
                        Pill(stringResource(R.string.meetup_synced, memberCount), SecondaryContainer, Navy)
                    }
                    AvailabilityCalendar(
                        month = uiState.visibleMonth,
                        selectedDate = uiState.selectedDate,
                        availability = uiState.availability,
                        onDateSelect = onDateSelect,
                    )
                    AvailabilityLegend(uiState.availabilityCounts)
                    SelectedDateBanner(uiState.selectedDate, uiState.bestCountOnSelected, memberCount)
                }
            }

            // 2. 멤버별 타임라인
            item(key = "timeline") {
                WhiteCard {
                    Text(
                        stringResource(R.string.meetup_timeline_title, uiState.selectedDate.format(DayFormat)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(stringResource(R.string.meetup_timeline_sub), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    CommonFreeBanner(uiState.commonFree)
                    MemberTimeline(members = uiState.members, date = uiState.selectedDate, commonFree = uiState.commonFree)
                }
            }

            // 2-1. 희망 소요시간: 이보다 짧은 후보는 추천에서 빠진다
            item(key = "duration") {
                WhiteCard {
                    DurationSlider(
                        minutes = uiState.durationMinutes,
                        recommended = uiState.recommendedMinutes,
                        onChange = onDurationChange,
                    )
                }
            }

            // 3. 눈송이 추천
            item(key = "recommend") {
                WhiteCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NunsongiAvatar(size = 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.meetup_ai_label), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Navy)
                            Text(stringResource(R.string.meetup_ai_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                    val list = uiState.recommendations
                    when {
                        list == null -> CircularProgressIndicator(color = Navy, modifier = Modifier.align(Alignment.CenterHorizontally))
                        list.isEmpty() -> Text(stringResource(R.string.room_suggest_empty), color = TextSecondary)
                        else -> list.forEachIndexed { i, item ->
                            RecommendationItem(
                                item = item,
                                isBest = i == 0,
                                selected = i == uiState.selectedRecommendation,
                                onClick = { onRecommendationSelect(i) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = White, shape = RoundedCornerShape(24.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** 📅 선택: 11월 14일 (목) ........ [4명 모두 조율 가능!] */
@Composable
private fun SelectedDateBanner(date: LocalDate, bestCount: Int, memberCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SecondaryContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = Navy)
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.meetup_selected, date.format(DayFormat)),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier.weight(1f),
        )
        val text = when {
            memberCount > 0 && bestCount == memberCount -> stringResource(R.string.meetup_selected_all, memberCount)
            bestCount > 0 -> stringResource(R.string.meetup_selected_some, bestCount)
            else -> stringResource(R.string.meetup_selected_none)
        }
        Pill(text, White, Navy)
    }
}

/** ✨ 전원 가능 시간대: 오후 3시 ~ 오후 5시  [2시간 확보] */
@Composable
private fun CommonFreeBanner(ranges: List<TimeRange>) {
    val longest = ranges.maxByOrNull { it.minutes }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (longest != null) PastelMint.copy(alpha = 0.5f) else SurfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = PastelMintText, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        if (longest == null) {
            Text(stringResource(R.string.meetup_no_common), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        } else {
            Text(
                stringResource(R.string.meetup_common_free, "${longest.start.format(HourFormat)} ~ ${longest.end.format(HourFormat)}"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = PastelMintText,
                modifier = Modifier.weight(1f),
            )
            Pill(stringResource(R.string.meetup_common_hours, durationText(longest.minutes)), White, PastelMintText)
        }
    }
}

@Composable
private fun durationText(minutes: Long): String {
    val h = (minutes / 60).toInt()
    val m = (minutes % 60).toInt()
    return when {
        h == 0 -> stringResource(R.string.meetup_minutes, m)
        m == 0 -> stringResource(R.string.meetup_hours, h)
        else -> stringResource(R.string.meetup_hours_minutes, h, m)
    }
}

/**
 * 희망 소요시간 슬라이더 (30분 ~ 4시간+, 30분 단위).
 * 전원 가능 시간대에서 계산한 "추천" 값과 같으면 (추천) 표시, 다르면 추천 값 칩을 눌러 맞출 수 있다.
 */
@Composable
private fun DurationSlider(
    minutes: Long,
    recommended: Long?,
    onChange: (Long) -> Unit,
) {
    val min = MeetupScheduler.MIN_DURATION_MINUTES
    val max = MeetupScheduler.MAX_DURATION_MINUTES
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Timer, contentDescription = null, tint = Navy, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.meetup_duration_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        val valueText = if (minutes >= max) stringResource(R.string.meetup_duration_max) else durationText(minutes)
        Text(
            text = if (minutes == recommended) stringResource(R.string.meetup_duration_recommended, valueText) else valueText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Navy,
        )
    }
    Slider(
        value = minutes.toFloat(),
        onValueChange = { onChange(it.roundToLong()) },
        valueRange = min.toFloat()..max.toFloat(),
        steps = ((max - min) / min - 1).toInt(), // 30분 간격 → 사이 눈금 6개
        colors = SliderDefaults.colors(
            thumbColor = Navy,
            activeTrackColor = Navy,
            inactiveTrackColor = SecondaryContainer,
            activeTickColor = White,
            inactiveTickColor = Navy.copy(alpha = 0.3f),
        ),
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(durationText(min), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.weight(1f))
        if (recommended != null && recommended != minutes) {
            Text(
                text = stringResource(R.string.meetup_duration_use_recommended, durationText(recommended)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PastelMintText,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PastelMint)
                    .clickable { onChange(recommended) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
        }
        Text(stringResource(R.string.meetup_duration_max), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
    Text(stringResource(R.string.meetup_duration_note), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
}

@Composable
private fun RecommendationItem(item: ExplainedCandidate, isBest: Boolean, selected: Boolean, onClick: () -> Unit) {
    val c = item.candidate
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainerLow)
            .then(if (selected) Modifier.border(1.5.dp, Navy, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(White)
                .border(if (selected) 6.dp else 1.5.dp, if (selected) Navy else OutlineVariant, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${c.date.format(DayFormat)} ${c.start.format(HourFormat)} ~ ${c.end.format(HourFormat)} (${durationText(Duration.between(c.start, c.end).toMinutes())})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isBest) Pill(stringResource(R.string.meetup_best), PastelPink, PastelPinkText)
                if (c.isEveryone) {
                    Pill(stringResource(R.string.meetup_everyone, c.totalMembers), PastelMint, PastelMintText)
                } else {
                    Pill(stringResource(R.string.meetup_partial, c.availableCount, c.totalMembers - c.availableCount), PastelYellow, PastelYellowText)
                }
            }
            if (item.reason.isNotBlank()) {
                Text(item.reason, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ConfirmBar(enabled: Boolean, isLoading: Boolean, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .navigationBarsPadding()
            .padding(horizontal = MoimDimens.ScreenPadding, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Button(
            onClick = onConfirm,
            enabled = enabled,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
            modifier = Modifier
                .fillMaxWidth()
                .height(MoimDimens.ButtonHeight),
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(stringResource(R.string.meetup_confirm), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Text(stringResource(R.string.meetup_confirm_note), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1800)
@Composable
private fun MeetupScreenPreview() {
    val day = LocalDate.of(2026, 11, 14)
    MoimTheme {
        MeetupScreen(
            uiState = MeetupUiState(
                today = LocalDate.of(2026, 11, 3),
                visibleMonth = YearMonth.of(2026, 11),
                selectedDate = day,
                availability = mapOf(day to Availability.ALL),
                recommendations = listOf(
                    ExplainedCandidate(
                        MeetupCandidate(day, LocalTime.of(15, 0), LocalTime.of(17, 0), listOf("a", "b", "c", "d"), 4),
                        "모두 수업이 끝난 직후라 순헌관/도서관 이동이 편해요!",
                    ),
                    ExplainedCandidate(
                        MeetupCandidate(day.plusDays(6), LocalTime.of(17, 0), LocalTime.of(19, 0), listOf("a", "b", "c"), 4),
                        "지원 일정이 겹쳐서 조율이 필요해요.",
                    ),
                ),
            ),
            onBack = {},
            onDateSelect = {},
            onPrevMonth = {},
            onNextMonth = {},
            onRecommendationSelect = {},
            onConfirm = {},
            onMessageShown = {},
        )
    }
}
