package com.moim.app.feature.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.core.model.NotificationPrefs
import com.moim.app.core.model.PlanningType
import com.moim.app.core.model.ReminderChoices
import com.moim.app.core.model.Visibility
import com.moim.app.feature.auth.domain.model.Departments
import com.moim.app.feature.auth.domain.model.UserProfile
import com.moim.app.feature.auth.ui.signup.ReminderCategory
import com.moim.app.feature.auth.ui.signup.labelRes
import com.moim.app.feature.auth.ui.signup.reminderDaysLabel
import com.moim.app.feature.auth.ui.signup.toUi
import com.moim.app.ui.component.FieldLabel
import com.moim.app.ui.component.MoimBackTopBar
import com.moim.app.ui.component.MoimSegmentedControl
import com.moim.app.ui.component.MoimSelectField
import com.moim.app.ui.component.MoimTextField
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.NavyDeep
import com.moim.app.ui.theme.PastelMint
import com.moim.app.ui.theme.PastelMintText
import com.moim.app.ui.theme.ScheduleAppointment
import com.moim.app.ui.theme.ScheduleAssignment
import com.moim.app.ui.theme.ScheduleExam
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.SurfaceContainer
import com.moim.app.ui.theme.SurfaceContainerHigh
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import kotlinx.coroutines.launch

/**
 * S-11 프로필 수정 (보고서 화면 설계서 + Stitch: moim_프로필수정)
 * 사진 / 기본 정보(학번·이메일 수정 불가) / 일정 성향 / 맞춤 알림 / 공개 범위 기본값 / [저장하기] / 로그아웃
 */
@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.savedAndClose, uiState.loggedOut) {
        when {
            uiState.loggedOut -> {
                viewModel.onNavigated()
                onLoggedOut()
            }
            uiState.savedAndClose -> {
                viewModel.onNavigated()
                onSaved()
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        actions = ProfileActions(
            onBack = onBack,
            onNameChange = viewModel::onNameChange,
            onCollegeChange = viewModel::onCollegeChange,
            onMajorChange = viewModel::onMajorChange,
            onPlanningTypeChange = viewModel::onPlanningTypeChange,
            onReminderChange = viewModel::onReminderChange,
            onVisibilityChange = viewModel::onVisibilityChange,
            onSave = viewModel::onSave,
            onLogoutClick = viewModel::onLogoutClick,
            onLogoutConfirm = viewModel::onLogoutConfirm,
            onLogoutDismiss = viewModel::onLogoutDismiss,
            onMessageShown = viewModel::onMessageShown,
        ),
    )
}

/** 프로필 화면 콜백 묶음 */
data class ProfileActions(
    val onBack: () -> Unit,
    val onNameChange: (String) -> Unit,
    val onCollegeChange: (String) -> Unit,
    val onMajorChange: (String) -> Unit,
    val onPlanningTypeChange: (PlanningType) -> Unit,
    val onReminderChange: (ReminderCategory, Int) -> Unit,
    val onVisibilityChange: (Visibility) -> Unit,
    val onSave: () -> Unit,
    val onLogoutClick: () -> Unit,
    val onLogoutConfirm: () -> Unit,
    val onLogoutDismiss: () -> Unit,
    val onMessageShown: () -> Unit,
)

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val photoSoon = stringResource(R.string.profile_photo_soon)

    val messageText = uiState.message?.let {
        stringResource(
            when (it) {
                ProfileMessage.SAVED -> R.string.profile_saved
                ProfileMessage.NAME_EMPTY -> R.string.signup1_name_error
                ProfileMessage.MAJOR_EMPTY -> R.string.signup1_major_error
                ProfileMessage.FAILED -> R.string.error_unknown
            },
        )
    }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            actions.onMessageShown()
            snackbarHostState.showSnackbar(messageText)
        }
    }

    Scaffold(
        modifier = modifier.imePadding(),
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MoimBackTopBar(
                title = stringResource(R.string.profile_title),
                onBack = actions.onBack,
                onProfileClick = null,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .navigationBarsPadding()
                    .padding(horizontal = MoimDimens.ScreenPadding, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = actions.onSave,
                    enabled = uiState.draft != null && !uiState.isSaving,
                    shape = RoundedCornerShape(MoimDimens.ButtonRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MoimDimens.ButtonHeight),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(MoimDimens.SpaceSm))
                        Text(stringResource(R.string.profile_save), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = actions.onLogoutClick) {
                    Text(
                        text = stringResource(R.string.profile_logout),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }
        },
    ) { innerPadding ->
        val user = uiState.user
        val draft = uiState.draft
        if (user == null || draft == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Navy)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoimDimens.ScreenPadding, vertical = MoimDimens.SpaceSm),
            verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceMd),
        ) {
            ProfileHeader(
                name = draft.name.ifBlank { user.name },
                college = draft.college,
                major = draft.major,
                onPhotoClick = { scope.launch { snackbarHostState.showSnackbar(photoSoon) } },
            )
            BasicInfoCard(user = user, draft = draft, actions = actions)
            PlanningTypeCard(selected = draft.planningType, onSelect = actions.onPlanningTypeChange)
            ReminderCard(prefs = draft.notificationPrefs, onChange = actions.onReminderChange)
            VisibilityCard(name = user.name, selected = draft.defaultVisibility, onSelect = actions.onVisibilityChange)
            Spacer(Modifier.height(MoimDimens.SpaceSm))
        }
    }

    if (uiState.showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = actions.onLogoutDismiss,
            containerColor = White,
            title = { Text(stringResource(R.string.profile_logout_title)) },
            text = { Text(stringResource(R.string.profile_logout_body)) },
            confirmButton = {
                TextButton(onClick = actions.onLogoutConfirm) { Text(stringResource(R.string.profile_logout), color = Navy) }
            },
            dismissButton = {
                TextButton(onClick = actions.onLogoutDismiss) { Text(stringResource(R.string.common_cancel), color = TextSecondary) }
            },
        )
    }
}

/** 프로필 사진(눈송이) + 카메라 버튼, 이름 + 재학 칩, 학부·전공 */
@Composable
private fun ProfileHeader(
    name: String,
    college: String,
    major: String?,
    onPhotoClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(White)
                    .border(2.dp, SecondaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.img_nunsongi),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Navy)
                    .clickable(onClick = onPhotoClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = stringResource(R.string.profile_photo_change),
                    tint = White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(MoimDimens.SpaceSm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyDeep)
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.profile_enrolled),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Navy,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SecondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text(
            text = listOfNotNull(college, major).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

/** 흰 카드 + 제목 줄 공통 틀 */
@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(White)
            .padding(MoimDimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            trailing?.invoke()
        }
        content()
    }
}

/** 기본 정보: 이름(수정), 학번(잠금), 학교 이메일(잠금 + 인증완료), 학부/전공(수정) */
@Composable
private fun BasicInfoCard(
    user: UserProfile,
    draft: ProfileDraft,
    actions: ProfileActions,
) {
    SectionCard(
        title = stringResource(R.string.profile_basic_title),
        trailing = {
            Text(
                text = stringResource(R.string.profile_school),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        },
    ) {
        MoimTextField(
            value = draft.name,
            onValueChange = actions.onNameChange,
            label = stringResource(R.string.signup1_name),
            placeholder = stringResource(R.string.signup1_name_placeholder),
        )
        LockedField(
            label = stringResource(R.string.profile_student_id),
            value = user.studentId,
            trailing = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
        )
        LockedField(
            label = stringResource(R.string.profile_email),
            value = user.schoolEmail.ifBlank { stringResource(R.string.profile_email_none) },
            trailing = {
                if (user.schoolEmail.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(PastelMint)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = PastelMintText, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.signup1_email_done), style = MaterialTheme.typography.labelSmall, color = PastelMintText)
                    }
                }
            },
        )
        MoimSelectField(
            selected = draft.college,
            options = Departments.colleges,
            onSelect = actions.onCollegeChange,
            placeholder = stringResource(R.string.common_select),
            label = stringResource(R.string.signup1_college),
        )
        MoimSelectField(
            selected = draft.major,
            options = Departments.majorsOf(draft.college),
            onSelect = actions.onMajorChange,
            placeholder = stringResource(R.string.common_select),
            label = stringResource(R.string.signup1_major),
        )
    }
}

/** 수정할 수 없는 칸: 회색 바탕 + 오른쪽 표시 */
@Composable
private fun LockedField(
    label: String,
    value: String,
    trailing: @Composable () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(text = label)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.profile_locked), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(Modifier.height(MoimDimens.SpaceSm))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MoimDimens.InputRadius))
                .background(SurfaceContainerHigh)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

/** 내 일정 관리 성향: 2×2 카드, 선택하면 Navy 채움 */
@Composable
private fun PlanningTypeCard(selected: PlanningType, onSelect: (PlanningType) -> Unit) {
    SectionCard(
        title = stringResource(R.string.profile_planning_title),
        subtitle = stringResource(R.string.profile_planning_sub),
        trailing = { Icon(Icons.Outlined.Psychology, contentDescription = null, tint = Navy) },
    ) {
        PlanningType.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm)) {
                row.forEach { type ->
                    val isSelected = type == selected
                    val ui = type.toUi()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Navy else SurfaceContainerLow)
                            .clickable(role = Role.RadioButton) { onSelect(type) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(ui.title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) White else TextPrimary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(ui.emoji)
                    }
                }
            }
        }
    }
}

/** 맞춤 알림 설정: 시험·과제 1/3/7일 전, 약속 당일/1/3일 전 */
@Composable
private fun ReminderCard(prefs: NotificationPrefs, onChange: (ReminderCategory, Int) -> Unit) {
    SectionCard(
        title = stringResource(R.string.profile_reminder_title),
        subtitle = stringResource(R.string.profile_reminder_sub),
        trailing = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Navy) },
    ) {
        ReminderRow(R.string.signup2_custom_exam, ScheduleExam, prefs.examDaysBefore, ReminderChoices.examOrAssignment) {
            onChange(ReminderCategory.EXAM, it)
        }
        ReminderRow(R.string.signup2_custom_assignment, ScheduleAssignment, prefs.assignmentDaysBefore, ReminderChoices.examOrAssignment) {
            onChange(ReminderCategory.ASSIGNMENT, it)
        }
        ReminderRow(R.string.profile_reminder_appointment, ScheduleAppointment, prefs.appointmentDaysBefore, ReminderChoices.appointment) {
            onChange(ReminderCategory.APPOINTMENT, it)
        }
    }
}

@Composable
private fun ReminderRow(
    labelRes: Int,
    dotColor: androidx.compose.ui.graphics.Color,
    selectedDays: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        options.forEach { days ->
            val isSelected = days == selectedDays
            Text(
                text = reminderDaysLabel(days),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) White else TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Navy else SurfaceContainer)
                    .clickable(role = Role.RadioButton) { onSelect(days) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** 공개 범위(기본 설정): 4종. 그룹공개는 여기서 그룹을 고르지 않고 안내만 한다. */
@Composable
private fun VisibilityCard(name: String, selected: Visibility, onSelect: (Visibility) -> Unit) {
    SectionCard(
        title = stringResource(R.string.profile_visibility_title),
        subtitle = stringResource(R.string.profile_visibility_sub),
    ) {
        MoimSegmentedControl(
            options = Visibility.entries,
            selected = selected,
            onSelect = onSelect,
            label = { stringResource(it.labelRes()) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerLow)
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (selected == Visibility.PRIVATE) Icons.Outlined.Lock else Icons.Outlined.Info,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (selected) {
                    Visibility.GROUP -> stringResource(R.string.profile_visibility_group_note)
                    Visibility.PRIVATE -> stringResource(R.string.signup2_visibility_note, name)
                    else -> stringResource(R.string.profile_visibility_default_note)
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1600)
@Composable
private fun ProfileScreenPreview() {
    val user = UserProfile(
        id = "me", studentId = "2316916", name = "황채원", college = "소프트웨어학부", major = "인공지능공학전공",
        planningType = PlanningType.EARLY, defaultVisibility = Visibility.GROUP, schoolEmail = "cw.hwang@sookmyung.ac.kr",
    )
    MoimTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                user = user,
                draft = ProfileDraft(
                    name = user.name,
                    college = user.college,
                    major = user.major,
                    planningType = PlanningType.THREE_DAY,
                    notificationPrefs = PlanningType.THREE_DAY.defaultPrefs,
                    defaultVisibility = Visibility.GROUP,
                ),
            ),
            actions = ProfileActions({}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {}),
        )
    }
}
