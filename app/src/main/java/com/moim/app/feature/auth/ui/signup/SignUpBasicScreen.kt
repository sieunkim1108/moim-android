package com.moim.app.feature.auth.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.feature.auth.domain.model.Departments
import com.moim.app.ui.component.FieldLabel
import com.moim.app.ui.component.FieldMessage
import com.moim.app.ui.component.FieldMessageType
import com.moim.app.ui.component.MascotBubble
import com.moim.app.ui.component.MoimPrimaryButton
import com.moim.app.ui.component.MoimSelectField
import com.moim.app.ui.component.MoimTextField
import com.moim.app.ui.component.MoimTopBar
import com.moim.app.ui.component.StepProgressHeader
import com.moim.app.ui.component.highlightedText
import com.moim.app.ui.theme.ErrorRed
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.Outline
import com.moim.app.ui.theme.SelectedCard
import com.moim.app.ui.theme.Surface
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val BirthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/**
 * 3. 회원가입 1단계 - 기본 정보 (Stitch: moim_회원가입_1)
 */
@Composable
fun SignUpBasicRoute(
    viewModel: SignUpViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onLoginClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 검사를 통과하면 한 번만 2단계로 이동
    LaunchedEffect(uiState.goToTypeStep) {
        if (uiState.goToTypeStep) {
            viewModel.onNavigatedToTypeStep()
            onNext()
        }
    }

    SignUpBasicScreen(
        uiState = uiState,
        onBack = onBack,
        onStudentIdChange = viewModel::onStudentIdChange,
        onCheckStudentId = viewModel::onCheckStudentIdClick,
        onEmailChange = viewModel::onEmailChange,
        onRequestEmailCode = viewModel::onRequestEmailCode,
        onEmailCodeChange = viewModel::onEmailCodeChange,
        onVerifyEmailCode = viewModel::onVerifyEmailCode,
        onNameChange = viewModel::onNameChange,
        onBirthDateChange = viewModel::onBirthDateChange,
        onCollegeChange = viewModel::onCollegeChange,
        onMajorChange = viewModel::onMajorChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordConfirmChange = viewModel::onPasswordConfirmChange,
        onTogglePasswordVisible = viewModel::onTogglePasswordVisible,
        onAgreeChange = viewModel::onAgreeChange,
        onNext = viewModel::onBasicInfoNext,
        onNextBlockedShown = viewModel::onNextBlockedShown,
        onLoginClick = onLoginClick,
    )
}

@Composable
fun SignUpBasicScreen(
    uiState: SignUpUiState,
    onBack: () -> Unit,
    onStudentIdChange: (String) -> Unit,
    onCheckStudentId: () -> Unit,
    onEmailChange: (String) -> Unit,
    onRequestEmailCode: () -> Unit,
    onEmailCodeChange: (String) -> Unit,
    onVerifyEmailCode: () -> Unit,
    onNameChange: (String) -> Unit,
    onBirthDateChange: (LocalDate) -> Unit,
    onCollegeChange: (String) -> Unit,
    onMajorChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onAgreeChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNextBlockedShown: () -> Unit = {},
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val blockedText = uiState.nextBlockedRes?.let { stringResource(it) }
    LaunchedEffect(uiState.nextBlockedRes) {
        if (blockedText != null) {
            onNextBlockedShown()
            snackbarHostState.showSnackbar(blockedText)
        }
    }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val showErrors = uiState.showErrors

    Scaffold(
        modifier = modifier.imePadding(),
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
                MoimPrimaryButton(
                    text = stringResource(R.string.signup_next),
                    onClick = onNext,
                    enabled = uiState.studentIdCheck != StudentIdCheck.Checking,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.signup_have_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    TextButton(onClick = onLoginClick) {
                        Text(
                            text = stringResource(R.string.signup_go_login),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                        )
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
                stepLabel = stringResource(R.string.signup1_step),
                percentLabel = stringResource(R.string.signup1_percent),
                progress = 0.5f,
            )

            MascotBubble(
                text = highlightedText(
                    highlight = stringResource(R.string.signup1_bubble_highlight),
                    end = "\n" + stringResource(R.string.signup1_bubble_rest),
                ),
                withCard = true,
            )

            // 학번 + [중복 확인]
            StudentIdField(uiState = uiState, onStudentIdChange = onStudentIdChange, onCheck = onCheckStudentId)

            // 학교 이메일 + [인증요청] → 인증번호 + 남은 시간 + [확인]
            SchoolEmailField(
                uiState = uiState,
                onEmailChange = onEmailChange,
                onRequestCode = onRequestEmailCode,
                onCodeChange = onEmailCodeChange,
                onVerify = onVerifyEmailCode,
            )

            // 이름
            MoimTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.signup1_name),
                placeholder = stringResource(R.string.signup1_name_placeholder),
                message = if (showErrors && uiState.name.isBlank()) stringResource(R.string.signup1_name_error) else null,
                messageType = FieldMessageType.Error,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            // 생년월일 (누르면 달력)
            MoimTextField(
                value = uiState.birthDate?.format(BirthFormatter).orEmpty(),
                onValueChange = {},
                label = stringResource(R.string.signup1_birth),
                placeholder = stringResource(R.string.signup1_birth_placeholder),
                trailingContent = {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(R.string.signup1_birth_pick),
                        tint = Navy,
                    )
                },
                onClick = { showDatePicker = true },
                message = if (showErrors && uiState.birthDate == null) stringResource(R.string.signup1_birth_error) else null,
                messageType = FieldMessageType.Error,
            )

            // 학부
            MoimSelectField(
                selected = uiState.college,
                options = Departments.colleges,
                onSelect = onCollegeChange,
                placeholder = stringResource(R.string.common_select),
                label = stringResource(R.string.signup1_college),
                message = if (showErrors && uiState.college == null) stringResource(R.string.signup1_college_error) else null,
                messageType = FieldMessageType.Error,
            )

            // 학과/전공 (학부를 골라야 목록이 나옴)
            MoimSelectField(
                selected = uiState.major,
                options = Departments.majorsOf(uiState.college),
                onSelect = onMajorChange,
                placeholder = stringResource(
                    if (uiState.college == null) R.string.signup1_major_need_college else R.string.common_select,
                ),
                label = stringResource(R.string.signup1_major),
                enabled = uiState.college != null,
                message = if (showErrors && uiState.major == null) stringResource(R.string.signup1_major_error) else null,
                messageType = FieldMessageType.Error,
            )

            // 비밀번호
            val passwordTransformation =
                if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
            val passwordInvalid = uiState.password.isNotEmpty() && !uiState.isPasswordValid
            MoimTextField(
                value = uiState.password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.signup1_password),
                placeholder = stringResource(R.string.signup1_password_placeholder),
                trailingContent = {
                    IconButton(onClick = onTogglePasswordVisible) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(
                                if (uiState.isPasswordVisible) R.string.common_hide_password else R.string.common_show_password,
                            ),
                            tint = TextSecondary,
                        )
                    }
                },
                message = if (passwordInvalid || (showErrors && !uiState.isPasswordValid)) {
                    stringResource(R.string.signup1_password_error)
                } else {
                    null
                },
                messageType = FieldMessageType.Error,
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )

            // 비밀번호 확인 (일치하면 체크 표시)
            val confirmMismatch = uiState.passwordConfirm.isNotEmpty() && !uiState.isPasswordConfirmed
            MoimTextField(
                value = uiState.passwordConfirm,
                onValueChange = onPasswordConfirmChange,
                label = stringResource(R.string.signup1_password_confirm),
                placeholder = stringResource(R.string.signup1_password_confirm_placeholder),
                trailingContent = if (uiState.isPasswordConfirmed) {
                    {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SelectedCard),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = stringResource(R.string.signup1_password_match),
                                tint = Navy,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    null
                },
                message = if (confirmMismatch || (showErrors && !uiState.isPasswordConfirmed)) {
                    stringResource(R.string.signup1_password_confirm_error)
                } else {
                    null
                },
                messageType = FieldMessageType.Error,
                visualTransformation = passwordTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )

            // 개인정보 동의
            PrivacyAgreement(
                agreed = uiState.agreedToPrivacy,
                onAgreeChange = onAgreeChange,
                onViewClick = { showPrivacy = true },
                showError = showErrors && !uiState.agreedToPrivacy,
            )

            Spacer(Modifier.height(MoimDimens.SpaceSm))
        }
    }

    if (showDatePicker) {
        BirthDatePickerDialog(
            initial = uiState.birthDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onBirthDateChange(it)
                showDatePicker = false
            },
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(text = stringResource(R.string.signup1_privacy_title)) },
            text = { Text(text = stringResource(R.string.signup1_privacy_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onAgreeChange(true)
                    showPrivacy = false
                }) { Text(text = stringResource(R.string.common_confirm), color = Navy) }
            },
            containerColor = White,
        )
    }
}

/** 학번 입력칸 + [중복 확인] 버튼 + 상태 문구 */
@Composable
private fun StudentIdField(
    uiState: SignUpUiState,
    onStudentIdChange: (String) -> Unit,
    onCheck: () -> Unit,
) {
    val (message, type) = when {
        uiState.studentIdCheck == StudentIdCheck.Duplicate ->
            stringResource(R.string.error_duplicate_student_id) to FieldMessageType.Error
        uiState.studentIdCheck == StudentIdCheck.Available ->
            stringResource(R.string.signup1_student_id_available) to FieldMessageType.Success
        uiState.showErrors && !uiState.isStudentIdFormatValid ->
            stringResource(R.string.signup1_student_id_invalid) to FieldMessageType.Error
        uiState.showErrors && uiState.studentIdCheck == StudentIdCheck.NotChecked ->
            stringResource(R.string.signup1_student_id_need_check) to FieldMessageType.Error
        else -> stringResource(R.string.signup1_student_id_help) to FieldMessageType.Help
    }
    Column {
        FieldLabel(text = stringResource(R.string.signup1_student_id))
        Spacer(Modifier.height(MoimDimens.SpaceSm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoimTextField(
                value = uiState.studentId,
                onValueChange = onStudentIdChange,
                placeholder = stringResource(R.string.signup1_student_id_placeholder),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(MoimDimens.SpaceSm))
            Button(
                onClick = onCheck,
                modifier = Modifier.height(MoimDimens.ButtonHeight),
                shape = RoundedCornerShape(MoimDimens.ButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
            ) {
                if (uiState.studentIdCheck == StudentIdCheck.Checking) {
                    CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(text = stringResource(R.string.signup1_check_duplicate), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        FieldMessage(text = message, type = type)
    }
}

/**
 * 학교 이메일(웹메일 인증) 칸.
 * [인증요청]을 누르면 아래에 인증번호 칸 + 남은 시간(빨강) + [확인]이 나온다.
 * 인증을 마치면 인증번호 칸은 사라지고 "인증 완료" 문구만 남는다.
 */
@Composable
private fun SchoolEmailField(
    uiState: SignUpUiState,
    onEmailChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
) {
    val step = uiState.emailStep
    val codeVisible = step == EmailVerifyStep.Sent || step == EmailVerifyStep.Verifying
    val (message, type) = when {
        uiState.isEmailVerified -> stringResource(R.string.signup1_email_verified) to FieldMessageType.Success
        uiState.emailErrorRes != null -> stringResource(uiState.emailErrorRes) to FieldMessageType.Error
        uiState.isCodeExpired -> stringResource(R.string.signup1_email_code_expired) to FieldMessageType.Error
        uiState.emailCodeWrong -> stringResource(R.string.signup1_email_code_wrong) to FieldMessageType.Error
        uiState.showErrors && !uiState.isEmailFormatValid ->
            stringResource(R.string.signup1_email_invalid) to FieldMessageType.Error
        uiState.showErrors && !uiState.isEmailVerified ->
            stringResource(R.string.signup1_email_need_verify) to FieldMessageType.Error
        else -> stringResource(R.string.signup1_email_help) to FieldMessageType.Help
    }
    Column {
        FieldLabel(text = stringResource(R.string.signup1_email))
        Spacer(Modifier.height(MoimDimens.SpaceSm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoimTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                placeholder = stringResource(R.string.signup1_email_placeholder),
                readOnly = uiState.isEmailVerified,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(MoimDimens.SpaceSm))
            Button(
                onClick = onRequestCode,
                enabled = !uiState.isEmailVerified,
                modifier = Modifier.height(MoimDimens.ButtonHeight),
                shape = RoundedCornerShape(MoimDimens.ButtonRadius),
                colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = White),
            ) {
                when {
                    step == EmailVerifyStep.Sending ->
                        CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    uiState.isEmailVerified ->
                        Text(text = stringResource(R.string.signup1_email_done), style = MaterialTheme.typography.labelLarge)
                    step == EmailVerifyStep.NotSent ->
                        Text(text = stringResource(R.string.signup1_email_request), style = MaterialTheme.typography.labelLarge)
                    else ->
                        Text(text = stringResource(R.string.signup1_email_resend), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (codeVisible) {
            Spacer(Modifier.height(MoimDimens.SpaceSm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoimTextField(
                    value = uiState.emailCode,
                    onValueChange = onCodeChange,
                    placeholder = stringResource(R.string.signup1_email_code_placeholder),
                    trailingContent = {
                        Text(
                            text = formatRemaining(uiState.codeRemainingSec),
                            style = MaterialTheme.typography.labelLarge,
                            color = ErrorRed,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(MoimDimens.SpaceSm))
                // 디자인: 연한 하늘색 바탕 + 남색 글자
                Button(
                    onClick = onVerify,
                    enabled = !uiState.isCodeExpired && step == EmailVerifyStep.Sent,
                    modifier = Modifier.height(MoimDimens.ButtonHeight),
                    shape = RoundedCornerShape(MoimDimens.ButtonRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = SelectedCard, contentColor = Navy),
                ) {
                    if (step == EmailVerifyStep.Verifying) {
                        CircularProgressIndicator(color = Navy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(text = stringResource(R.string.common_confirm), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        FieldMessage(text = message, type = type)
    }
}

/** 180 → "03:00" */
private fun formatRemaining(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

/** [✓] 개인정보 수집 및 이용에 동의합니다 (필수) ........ 보기 */
@Composable
private fun PrivacyAgreement(
    agreed: Boolean,
    onAgreeChange: (Boolean) -> Unit,
    onViewClick: () -> Unit,
    showError: Boolean,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MoimDimens.InputRadius))
                .background(SurfaceContainerLow)
                .clickable { onAgreeChange(!agreed) }
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = agreed,
                onCheckedChange = onAgreeChange,
                colors = CheckboxDefaults.colors(checkedColor = Navy, uncheckedColor = Outline),
            )
            Text(
                text = highlightedText(
                    start = stringResource(R.string.signup1_agree) + " ",
                    highlight = stringResource(R.string.signup1_agree_required),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.signup1_agree_view),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable(onClick = onViewClick)
                    .padding(8.dp),
            )
        }
        if (showError) {
            FieldMessage(
                text = stringResource(R.string.signup1_agree_error),
                type = FieldMessageType.Error,
            )
        }
    }
}

/** 생년월일 달력 팝업 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    // DatePicker는 UTC 기준 밀리초를 쓴다
    val initialMillis = (initial ?: LocalDate.of(2004, 1, 1))
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        yearRange = 1950..LocalDate.now().year,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                } ?: onDismiss()
            }) { Text(text = stringResource(R.string.common_confirm), color = Navy) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.common_cancel), color = TextSecondary) }
        },
    ) {
        DatePicker(state = state)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun SignUpBasicScreenPreview() {
    MoimTheme {
        SignUpBasicScreen(
            uiState = SignUpUiState(
                studentId = "2316916",
                studentIdCheck = StudentIdCheck.Available,
                email = "snowflake@sookmyung.ac.kr",
                emailStep = EmailVerifyStep.Sent,
                emailCode = "482910",
                codeRemainingSec = 165,
                name = "이눈송",
                birthDate = LocalDate.of(2004, 3, 15),
                college = "소프트웨어학부",
                major = "인공지능공학전공",
                password = "moim1234",
                passwordConfirm = "moim1234",
                agreedToPrivacy = true,
            ),
            onBack = {},
            onStudentIdChange = {},
            onCheckStudentId = {},
            onEmailChange = {},
            onRequestEmailCode = {},
            onEmailCodeChange = {},
            onVerifyEmailCode = {},
            onNameChange = {},
            onBirthDateChange = {},
            onCollegeChange = {},
            onMajorChange = {},
            onPasswordChange = {},
            onPasswordConfirmChange = {},
            onTogglePasswordVisible = {},
            onAgreeChange = {},
            onNext = {},
            onLoginClick = {},
        )
    }
}
