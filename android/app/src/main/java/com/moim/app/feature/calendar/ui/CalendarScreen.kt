package com.moim.app.feature.calendar.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.core.model.Friend
import com.moim.app.core.model.Schedule
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.Visibility
import com.moim.app.core.model.givenName
import com.moim.app.feature.calendar.domain.usecase.ObserveSchedulesByDateUseCase
import com.moim.app.feature.calendar.ui.component.AddScheduleActions
import com.moim.app.feature.calendar.ui.component.AddScheduleSheetContent
import com.moim.app.feature.calendar.ui.component.MonthCalendarCard
import com.moim.app.feature.calendar.ui.component.ProfileFilterRow
import com.moim.app.feature.calendar.ui.component.ScheduleDayCard
import com.moim.app.feature.calendar.ui.component.ScheduleLegend
import com.moim.app.ui.component.MoimAppTopBar
import com.moim.app.ui.component.MoimTopBarTitle
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.ShadowNavy
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DayTitleFormat = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)

/** 캘린더 진입점 */
@Composable
fun CalendarRoute(
    onBack: () -> Unit,
    onBackToChat: () -> Unit,
    onDmClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        uiState = uiState,
        onBack = onBack,
        onDmClick = onDmClick,
        onProfileClick = onProfileClick,
        onAddFriendClick = onAddFriendClick,
        onOwnerSelect = viewModel::onOwnerSelect,
        onDateSelect = viewModel::onDateSelect,
        onPrevMonth = viewModel::onPrevMonth,
        onNextMonth = viewModel::onNextMonth,
        onToggleEditMode = viewModel::onToggleEditMode,
        onAddClick = viewModel::onAddClick,
        onDeleteSchedule = viewModel::onDeleteSchedule,
        onMessageShown = viewModel::onMessageShown,
        addActions = AddScheduleActions(
            onTitleChange = viewModel::onTitleChange,
            onTypeSelect = viewModel::onTypeSelect,
            onDateChange = viewModel::onDateChange,
            onTimeChange = viewModel::onTimeChange,
            onLocationChange = viewModel::onLocationChange,
            onReminderToggle = viewModel::onReminderToggle,
            onVisibilitySelect = viewModel::onVisibilitySelect,
            onGroupToggle = viewModel::onGroupToggle,
            onSave = viewModel::onSave,
            onClose = viewModel::onFormDismiss,
            onCancelToChat = {
                viewModel.onFormDismiss()
                onBackToChat()
            },
        ),
    )
}

/**
 * 7. 캘린더 첫 화면 (Stitch: moim_캘린더_첫페이지)
 * 공통 상단바 / [←] [< 2026년 11월 >] [설정] / 프로필 줄 / 월 달력 / 범례 / 선택한 날 일정 / [+]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onBack: () -> Unit,
    onDmClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOwnerSelect: (String?) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleEditMode: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onMessageShown: () -> Unit,
    addActions: AddScheduleActions,
    modifier: Modifier = Modifier,
    onAddFriendClick: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.main_coming_soon)
    val showComingSoon: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(comingSoon) } }

    val messageText = uiState.message?.let {
        stringResource(
            when (it) {
                CalendarMessage.SAVED -> R.string.add_saved
                CalendarMessage.SAVE_FAILED -> R.string.add_save_failed
                CalendarMessage.DELETED -> R.string.calendar_deleted
            },
        )
    }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            onMessageShown()
            snackbarHostState.showSnackbar(messageText)
        }
    }

    val friendName = uiState.selectedFriend?.givenName

    Scaffold(
        modifier = modifier,
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MoimAppTopBar(
                title = { MoimTopBarTitle(stringResource(R.string.calendar_title)) },
                onCalendarClick = null, // 지금 화면
                onDmClick = onDmClick,
                onProfileClick = onProfileClick,
            )
        },
        floatingActionButton = {
            // 친구 캘린더는 보기만 가능
            if (uiState.isMyCalendar) {
                FloatingActionButton(onClick = onAddClick, containerColor = Navy, contentColor = White, shape = CircleShape) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.calendar_add))
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp), // FAB에 가리지 않게
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "month-bar") {
                MonthBar(
                    month = uiState.visibleMonth,
                    onBack = onBack,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    onSettingsClick = showComingSoon, // TODO: 캘린더 표시 설정
                )
            }
            item(key = "profiles") {
                ProfileFilterRow(
                    myName = uiState.myName,
                    friends = uiState.friends,
                    selectedFriendId = uiState.selectedFriendId,
                    onSelect = onOwnerSelect,
                    onAddFriendClick = onAddFriendClick,
                )
            }
            item(key = "calendar") {
                MonthCalendarCard(
                    month = uiState.visibleMonth,
                    selectedDate = uiState.selectedDate,
                    today = uiState.today,
                    schedulesByDate = uiState.schedulesByDate,
                    onDateSelect = onDateSelect,
                    modifier = Modifier.padding(horizontal = MoimDimens.ScreenPadding),
                )
            }
            item(key = "legend") { ScheduleLegend() }
            item(key = "day-header") {
                DayHeader(
                    date = uiState.selectedDate,
                    count = uiState.selectedSchedules.size,
                    showEdit = uiState.isMyCalendar && uiState.selectedSchedules.isNotEmpty(),
                    isEditMode = uiState.isEditMode,
                    onToggleEditMode = onToggleEditMode,
                )
            }

            if (uiState.selectedSchedules.isEmpty()) {
                item(key = "empty") { EmptyDay(friendName) }
            } else {
                items(uiState.selectedSchedules, key = { it.id }) { schedule ->
                    // 친구의 비공개 일정은 서버에서 내용이 지워진 채로 온다 → 이름만 표시
                    val hiddenOwner = friendName?.takeIf { schedule.visibility == Visibility.PRIVATE }
                    ScheduleDayCard(
                        schedule = schedule,
                        showDelete = uiState.isMyCalendar && uiState.isEditMode,
                        onDelete = { onDeleteSchedule(schedule.id) },
                        hiddenOwnerName = hiddenOwner,
                        modifier = Modifier.padding(horizontal = MoimDimens.ScreenPadding),
                    )
                }
            }
        }
    }

    // 새 일정 등록 바텀시트
    if (uiState.form != null) {
        ModalBottomSheet(
            onDismissRequest = addActions.onClose,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = White,
        ) {
            AddScheduleSheetContent(form = uiState.form, actions = addActions, groups = uiState.shareGroups)
        }
    }
}

/** [←]   [< 2026년 11월 >]   [설정] */
@Composable
private fun MonthBar(
    month: YearMonth,
    onBack: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoimDimens.ScreenPadding, vertical = MoimDimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Navy)
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .shadow(4.dp, CircleShape, ambientColor = ShadowNavy, spotColor = ShadowNavy)
                .clip(CircleShape)
                .background(White)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.calendar_prev_month),
                    tint = TextSecondary,
                )
            }
            Text(
                text = stringResource(R.string.calendar_month_title, month.year, month.monthValue),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.calendar_next_month),
                    tint = TextSecondary,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        CircleIconButton(onClick = onSettingsClick) {
            Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.calendar_settings), tint = Navy)
        }
    }
}

@Composable
private fun CircleIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = White),
        modifier = Modifier
            .size(44.dp)
            .shadow(2.dp, CircleShape, ambientColor = ShadowNavy, spotColor = ShadowNavy),
    ) { content() }
}

/** 11월 9일 (월) [총 2개의 일정] ........ 수정 */
@Composable
private fun DayHeader(
    date: LocalDate,
    count: Int,
    showEdit: Boolean,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MoimDimens.ScreenPadding, end = MoimDimens.SpaceSm, top = MoimDimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date.format(DayTitleFormat),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.width(MoimDimens.SpaceSm))
        Text(
            text = stringResource(R.string.calendar_total, count),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Navy,
            modifier = Modifier
                .clip(CircleShape)
                .background(SecondaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        if (showEdit) {
            TextButton(onClick = onToggleEditMode) {
                Text(
                    text = stringResource(if (isEditMode) R.string.calendar_edit_done else R.string.calendar_edit),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isEditMode) Navy else TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun EmptyDay(friendName: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MoimDimens.SpaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceMd),
    ) {
        Image(
            painter = painterResource(R.drawable.img_nunsongi),
            contentDescription = null,
            modifier = Modifier.size(88.dp),
        )
        Text(
            text = if (friendName != null) {
                stringResource(R.string.calendar_friend_empty, friendName)
            } else {
                stringResource(R.string.calendar_empty)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------- 미리보기 ----------

private fun previewState(): CalendarUiState {
    val day = LocalDate.of(2026, 11, 9)
    val list = listOf(
        Schedule(
            "1", "데이터 분석", ScheduleType.EXAM, day.atTime(10, 0), day.atTime(11, 0),
            location = "명신관 402호", reminderDaysBefore = listOf(3),
        ),
        Schedule("2", "시은이랑 점심", ScheduleType.APPOINTMENT, day.atTime(12, 30), day.atTime(13, 30), location = "순헌관 학생식당"),
        Schedule("3", "과제 제출", ScheduleType.ASSIGNMENT, day.minusDays(3).atTime(23, 59), day.minusDays(3).atTime(23, 59)),
    )
    return CalendarUiState(
        today = LocalDate.of(2026, 11, 2),
        selectedDate = day,
        visibleMonth = YearMonth.of(2026, 11),
        myName = "김눈송",
        friends = listOf(Friend("a", "박시은"), Friend("b", "이지원")),
        schedulesByDate = ObserveSchedulesByDateUseCase.groupByDate(list),
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun CalendarScreenPreview() {
    MoimTheme {
        CalendarScreen(
            uiState = previewState(),
            onBack = {},
            onDmClick = {},
            onProfileClick = {},
            onOwnerSelect = {},
            onDateSelect = {},
            onPrevMonth = {},
            onNextMonth = {},
            onToggleEditMode = {},
            onAddClick = {},
            onDeleteSchedule = {},
            onMessageShown = {},
            addActions = AddScheduleActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
