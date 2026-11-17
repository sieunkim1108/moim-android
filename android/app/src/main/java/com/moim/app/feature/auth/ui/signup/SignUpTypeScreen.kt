package com.moim.app.feature.auth.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Visibility as VisibilityIcon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.core.model.NotificationPrefs
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.ReminderChoices
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.ui.component.MascotBubble
import com.moim.app.ui.component.MoimSegmentedControl
import com.moim.app.ui.component.MoimTopBar
import com.moim.app.ui.component.StepProgressHeader
import com.moim.app.ui.component.highlightedText
import com.moim.app.ui.theme.InfoBanner
import com.moim.app.ui.theme.InfoBannerBorder
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.Outline
import com.moim.app.ui.theme.OutlineVariant
import com.moim.app.ui.theme.SelectedCard
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.SurfaceContainer
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White

/** 계획형 맞춤 알림에서 고를 수 있는 시점 (며칠 전) */

/**
 * 4. 회원가입 2단계 - 일정 성향 설정 (Stitch: moim_회원가입_2_1 ~ 2_4)
 */
@Composable
fun SignUpTypeRoute(
    viewModel: SignUpViewModel,
    onBack: () -> Unit,
    onSignUpComplete: (UserProfile) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 가입 성공 → 완료 화면으로 한 번만 이동
    LaunchedEffect(uiState.completedProfile) {
        uiState.completedProfile?.let {
            onSignUpComplete(it)
            viewModel.onNavigatedToDone()
        }
    }

    SignUpTypeScreen(
        uiState = uiState,
        onBack = onBack,
        onPlanningTypeChange = viewModel::onPlanningTypeChange,
        onReminderDaysChange = viewModel::onReminderDaysChange,
        onVisibilityChange = viewModel::onVisibilityChange,
        onSubmit = viewModel::onSubmit,
        onSubmitErrorShown = viewModel::onSubmitErrorShown,
    )
}

@Composable
fun SignUpTypeScreen(
    uiState: SignUpUiState,
    onBack: () -> Unit,
    onPlanningTypeChange: (PlanningType) -> Unit,
    onReminderDaysChange: (ReminderCategory, Int) -> Unit,
    onVisibilityChange: (Visibility) -> Unit,
    onSubmit: () -> Unit,
    onSubmitErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 가입 실패 문구는 아래 스낵바로
    LaunchedEffect(uiState.submitErrorRes) {
        uiState.submitErrorRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            onSubmitErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Surface,
        topBar = { MoimTopBar(title = stringResource(R.string.signup_title), onBack = onBack, containerColor = White) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .navigationBarsPadding()
                    .padding(horizontal = MoimDimens.ScreenPadding, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Outline, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.signup2_profile_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Outline,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MoimDimens.ButtonHeight),
                    shape = RoundedCornerShape(MoimDimens.ButtonRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = White,
                        disabledContainerColor = Navy.copy(alpha = 0.6f),
                        disabledContentColor = White,
                    ),
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Text(text = stringResource(R.string.signup2_submit), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(MoimDimens.SpaceSm))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoimDimens.ScreenPadding, vertical = MoimDimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StepProgressHeader(
                stepLabel = stringResource(R.string.signup2_step),
                percentLabel = stringResource(R.string.signup2_percent),
                progress = 1f,
            )

            MascotBubble(
                text = highlightedText(
                    start = stringResource(R.string.signup2_bubble_start),
                    highlight = stringResource(R.string.signup2_bubble_highlight),
                    end = stringResource(R.string.signup2_bubble_end),
                ),
            )

            // 나의 작업 & 시험 스타일 (2 x 2 카드)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.signup2_style_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.signup2_style_count),
                        style = MaterialTheme.typography.labelMedium,
                        color = Outline,
                    )
                }
                PlanningType.entries.chunked(2).forEach { rowTypes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowTypes.forEach { type ->
                            PlanningTypeCard(
                                type = type,
                                selected = uiState.planningType == type,
                                onClick = { onPlanningTypeChange(type) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // 성향별 알림 안내 (계획형은 직접 고르는 카드)
            if (uiState.planningType == PlanningType.PLANNER) {
                Text(
                    text = stringResource(R.string.planning_planner_banner),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                )
                CustomReminderCard(prefs = uiState.notificationPrefs, onChange = onReminderDaysChange)
            } else {
                ReminderBanner(type = uiState.planningType)
            }

            // 내 일정 공개 범위
            VisibilitySection(
                name = uiState.name.ifBlank { stringResource(R.string.signup2_default_name) },
                selected = uiState.visibility,
                onSelect = onVisibilityChange,
            )

            Spacer(Modifier.height(MoimDimens.SpaceSm))
        }
    }
}

/** 성향 카드 하나: 이모지 / 이름 / 설명, 선택되면 하늘색 + 남색 테두리 + 체크 */
@Composable
private fun PlanningTypeCard(
    type: PlanningType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui = type.toUi()
    val shape = RoundedCornerShape(MoimDimens.CardRadius)
    Column(
        modifier = modifier
            .height(176.dp)
            .clip(shape)
            .background(if (selected) SelectedCard else White)
            .border(width = 2.dp, color = if (selected) Navy else Color.Transparent, shape = shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = ui.emoji, fontSize = 32.sp)
            Spacer(Modifier.weight(1f))
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Navy),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(ui.title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Navy else TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(if (selected) ui.selectedDescription else ui.description),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            minLines = 2,
            maxLines = 2,
        )
    }
}

/** 미리미리형/3일 집중형/벼락치기형: 알림 안내 배너 */
@Composable
private fun ReminderBanner(type: PlanningType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(InfoBanner)
            .border(1.dp, InfoBannerBorder, RoundedCornerShape(MoimDimens.CardRadius))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Navy)
        Spacer(Modifier.width(12.dp))
        Column {
            if (type == PlanningType.CRAMMING) {
                Text(
                    text = stringResource(R.string.planning_cramming_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                )
            }
            Text(
                text = stringResource(
                    when (type) {
                        PlanningType.EARLY -> R.string.planning_early_banner
                        PlanningType.THREE_DAY -> R.string.planning_three_day_banner
                        else -> R.string.planning_cramming_banner
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

/** 계획형: 시험/과제/기타 약속마다 1·3·7일 전 중 선택 */
@Composable
private fun CustomReminderCard(
    prefs: NotificationPrefs,
    onChange: (ReminderCategory, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(White)
            .border(1.dp, OutlineVariant, RoundedCornerShape(MoimDimens.CardRadius))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Navy)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.signup2_custom_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.signup2_custom_hint),
                style = MaterialTheme.typography.labelSmall,
                color = Outline,
            )
        }
        ReminderRow(R.string.signup2_custom_exam, prefs.examDaysBefore, ReminderChoices.examOrAssignment) {
            onChange(ReminderCategory.EXAM, it)
        }
        ReminderRow(R.string.signup2_custom_assignment, prefs.assignmentDaysBefore, ReminderChoices.examOrAssignment) {
            onChange(ReminderCategory.ASSIGNMENT, it)
        }
        ReminderRow(R.string.signup2_custom_appointment, prefs.appointmentDaysBefore, ReminderChoices.appointment) {
            onChange(ReminderCategory.APPOINTMENT, it)
        }
    }
}

@Composable
private fun ReminderRow(
    labelRes: Int,
    selectedDays: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        options.forEach { days ->
            val isSelected = days == selectedDays
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Navy else SurfaceContainer)
                    .clickable(role = Role.RadioButton) { onSelect(days) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = reminderDaysLabel(days),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) White else TextSecondary,
                )
            }
        }
    }
}

/** 내 일정 공개 범위: 전체 공개 / 친구 공개 / 비공개 */
@Composable
private fun VisibilitySection(
    name: String,
    selected: Visibility,
    onSelect: (Visibility) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VisibilityIcon, contentDescription = null, tint = Navy)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.signup2_visibility_title),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
        }
        MoimSegmentedControl(
            options = Visibility.signUpChoices,
            selected = selected,
            onSelect = onSelect,
            label = { stringResource(it.labelRes()) },
        )
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.signup2_visibility_note, name),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        Text(
            text = stringResource(R.string.signup2_visibility_default_note),
            style = MaterialTheme.typography.bodySmall,
            color = Outline,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1300)
@Composable
private fun SignUpTypeScreenPreview() {
    MoimTheme {
        SignUpTypeScreen(
            uiState = SignUpUiState(name = "황채원", planningType = PlanningType.PLANNER, notificationPrefs = PlanningType.PLANNER.defaultPrefs),
            onBack = {},
            onPlanningTypeChange = {},
            onReminderDaysChange = { _, _ -> },
            onVisibilityChange = {},
            onSubmit = {},
            onSubmitErrorShown = {},
        )
    }
}
