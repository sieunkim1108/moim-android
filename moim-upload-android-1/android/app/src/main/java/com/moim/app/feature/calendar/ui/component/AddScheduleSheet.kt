package com.moim.app.feature.calendar.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.core.model.ScheduleType
import com.moim.app.core.model.ShareGroup
import com.moim.app.core.model.Visibility
import com.moim.app.feature.calendar.ui.AddScheduleForm
import com.moim.app.ui.theme.ErrorRed
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.SkyBlue
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import com.moim.app.ui.theme.color
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateTimeText = DateTimeFormatter.ofPattern("yyyy.MM.dd (E) a hh:mm", Locale.KOREAN)

/** Stitch 디자인의 유형 칩 4개 (모임·수업은 모임 기능/시간표에서 자동으로 생김) */
private val SelectableTypes = listOf(
    ScheduleType.EXAM,
    ScheduleType.ASSIGNMENT,
    ScheduleType.APPOINTMENT,
    ScheduleType.ETC,
)

private val ReminderOptions = listOf(1, 3, 7)

/** 바텀시트 안에 들어가는 콜백 묶음 (인자가 너무 많아지지 않게) */
data class AddScheduleActions(
    val onTitleChange: (String) -> Unit,
    val onTypeSelect: (ScheduleType) -> Unit,
    val onDateChange: (LocalDate) -> Unit,
    val onTimeChange: (LocalTime) -> Unit,
    val onLocationChange: (String) -> Unit,
    val onReminderToggle: (Int) -> Unit,
    val onVisibilitySelect: (Visibility) -> Unit,
    val onGroupToggle: (String) -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
    val onCancelToChat: () -> Unit,
)

/**
 * 새 일정 등록 폼 (ModalBottomSheet 안의 내용).
 * 시트 자체는 CalendarScreen에서 띄운다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduleSheetContent(
    form: AddScheduleForm,
    actions: AddScheduleActions,
    modifier: Modifier = Modifier,
    groups: List<ShareGroup> = emptyList(),
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MoimDimens.ScreenPadding)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceLg),
    ) {
        SheetHeader(onClose = actions.onClose)

        // 1. 일정 이름
        Section(label = stringResource(R.string.add_name_label)) {
            PillTextField(
                value = form.title,
                onValueChange = actions.onTitleChange,
                placeholder = stringResource(R.string.add_name_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MoimDimens.CardRadius))
                    .background(SurfaceContainerLow)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            )
            if (form.showTitleError) {
                Text(
                    text = stringResource(R.string.add_name_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        // 2. 일정 유형
        Section(
            label = stringResource(R.string.add_type_label),
            hint = { Text(stringResource(R.string.add_type_hint), style = MaterialTheme.typography.labelMedium, color = TextSecondary) },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm)) {
                SelectableTypes.forEach { type ->
                    TypeChip(
                        type = type,
                        selected = form.type == type,
                        onClick = { actions.onTypeSelect(type) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 3. 일정 시간 및 장소
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceContainerLow)
                .padding(MoimDimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.add_when_where_label),
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            InfoRow(
                icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Navy) },
                label = stringResource(R.string.add_datetime_label),
                onClick = { showDatePicker = true },
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                },
            ) {
                Text(
                    text = form.date.atTime(form.time).format(DateTimeText),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                )
            }
            InfoRow(
                icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Navy) },
                label = stringResource(R.string.add_location_label),
            ) {
                PillTextField(
                    value = form.location,
                    onValueChange = actions.onLocationChange,
                    placeholder = stringResource(R.string.add_location_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 4. 알림 시점 (여러 개 선택 가능)
        Section(
            label = stringResource(R.string.add_reminder_label),
            hint = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = Navy, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.add_reminder_hint), style = MaterialTheme.typography.labelMedium, color = Navy)
                }
            },
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm)) {
                ReminderOptions.forEach { days ->
                    val selected = days in form.reminderDays
                    Text(
                        text = stringResource(R.string.add_reminder_days, days),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) White else TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (selected) Navy else SurfaceContainerLow)
                            .clickable { actions.onReminderToggle(days) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }

        // 5. 공개 범위
        Section(label = stringResource(R.string.add_visibility_label)) {
            VisibilitySegment(selected = form.visibility, onSelect = actions.onVisibilitySelect)
            // "그룹공개"를 고를 때만 그룹 선택 영역이 펼쳐진다
            AnimatedVisibility(visible = form.visibility == Visibility.GROUP) {
                GroupSelector(
                    groups = groups,
                    selectedIds = form.sharedRoomIds,
                    onToggle = actions.onGroupToggle,
                )
            }
            if (form.showGroupError) {
                Text(
                    text = stringResource(R.string.add_group_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(
                        if (form.visibility == Visibility.GROUP) R.string.add_group_info else R.string.add_visibility_info,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        // 6. 버튼
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = actions.onSave,
                enabled = !form.isSaving,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MoimDimens.ButtonHeight),
            ) {
                if (form.isSaving) {
                    CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(MoimDimens.SpaceSm))
                    Text(stringResource(R.string.add_save), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = actions.onCancelToChat) {
                Text(stringResource(R.string.add_cancel_to_chat), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
            Spacer(Modifier.height(MoimDimens.SpaceMd))
        }
    }

    // 날짜 → 시간 순서로 고른다
    if (showDatePicker) {
        ScheduleDatePickerDialog(
            initial = form.date,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                actions.onDateChange(it)
                showDatePicker = false
                showTimePicker = true
            },
        )
    }
    if (showTimePicker) {
        ScheduleTimePickerDialog(
            initial = form.time,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                actions.onTimeChange(it)
                showTimePicker = false
            },
        )
    }
}

@Composable
private fun SheetHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(Navy),
        )
        Spacer(Modifier.width(MoimDimens.SpaceSm))
        Text(
            text = stringResource(R.string.add_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.add_close), tint = TextSecondary)
        }
    }
}

@Composable
private fun Section(
    label: String,
    hint: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            hint?.invoke()
        }
        content()
    }
}

/** 테두리 없는 입력칸 (배경은 바깥 Modifier가 정함) */
@Composable
private fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TextSecondary.copy(alpha = 0.6f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(Navy),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TypeChip(
    type: ScheduleType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) Navy else SurfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(type.color()),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(type.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) White else TextPrimary,
        )
    }
}

@Composable
private fun InfoRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(White)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(MoimDimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SkyBlue),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            value()
        }
        trailing?.invoke()
    }
}

/** 공개 범위: 선택된 칸이 흰색으로 떠오르는 세그먼트 (Stitch 일정 등록 디자인) */
@Composable
private fun VisibilitySegment(selected: Visibility, onSelect: (Visibility) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(SurfaceContainerLow)
            .padding(4.dp),
    ) {
        Visibility.entries.forEach { option ->
            val isSelected = option == selected
            Text(
                text = stringResource(option.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Navy else TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) White else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

/**
 * 그룹공개일 때만 보이는 "공개할 그룹 선택" 영역.
 * 그룹 = 내가 속한 단체 DM 방. 칩은 여러 개 고를 수 있다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupSelector(
    groups: List<ShareGroup>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MoimDimens.SpaceSm)
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(SurfaceContainerLow)
            .padding(MoimDimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.add_group_label), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Text(
                text = stringResource(R.string.add_group_count, selectedIds.size),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Navy,
            )
        }
        if (groups.isEmpty()) {
            Text(stringResource(R.string.add_group_empty), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
            ) {
                groups.forEach { group ->
                    val selected = group.id in selectedIds
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) White else TextSecondary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) Navy else White)
                            .clickable { onToggle(group.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private fun Visibility.labelRes(): Int = when (this) {
    Visibility.PUBLIC -> R.string.visibility_public
    Visibility.FRIENDS -> R.string.visibility_friends
    Visibility.GROUP -> R.string.visibility_group
    Visibility.PRIVATE -> R.string.visibility_private
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // DatePicker는 UTC 기준 밀리초를 쓴다
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(stringResource(R.string.common_confirm), color = Navy) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = TextSecondary) }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.common_confirm), color = Navy)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = TextSecondary) }
        },
        text = { TimePicker(state = state) },
    )
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun AddScheduleSheetPreview() {
    MoimTheme {
        AddScheduleSheetContent(
            form = AddScheduleForm(
                title = "지원이와 모바일 소프트웨어 팀플 회의",
                date = LocalDate.of(2026, 11, 12),
                location = "순헌관 301호 스터디룸",
                visibility = Visibility.GROUP,
                sharedRoomIds = setOf("sw"),
            ),
            actions = AddScheduleActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}),
            groups = listOf(
                ShareGroup("sw", "SW 모바일 팀플방"),
                ShareGroup("study", "데이터분석 스터디"),
                ShareGroup("mobile", "모바일 조모임"),
            ),
            modifier = Modifier.background(White),
        )
    }
}
