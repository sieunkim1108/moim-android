package com.moim.app.feature.auth.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moim.app.R
import com.moim.app.ui.component.FieldMessageType
import com.moim.app.ui.component.MoimTextField
import com.moim.app.ui.component.MoimTopBar
import com.moim.app.ui.theme.ErrorRed
import com.moim.app.ui.theme.MoimBrushes
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.Outline
import com.moim.app.ui.theme.ShadowNavy
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White
import kotlinx.coroutines.launch

/**
 * 2. 로그인 (Stitch: moim_로그인_최종)
 * Route: ViewModel과 연결하는 부분 / Screen: 상태만 받아서 그리는 부분 (Preview 가능)
 */
@Composable
fun LoginRoute(
    onBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 로그인 성공 → 메인으로 한 번만 이동
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
            viewModel.onNavigatedToMain()
        }
    }

    LoginScreen(
        uiState = uiState,
        onBack = onBack,
        onStudentIdChange = viewModel::onStudentIdChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisible = viewModel::onTogglePasswordVisible,
        onAutoLoginChange = viewModel::onAutoLoginChange,
        onLoginClick = viewModel::onLoginClick,
        onSignUpClick = onSignUpClick,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onBack: () -> Unit,
    onStudentIdChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onAutoLoginChange: (Boolean) -> Unit,
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val forgotPasswordMessage = stringResource(R.string.login_forgot_password_soon)
    val errorText = uiState.errorRes?.let { stringResource(it) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoimBrushes.SkyFade),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { MoimTopBar(title = null, onBack = onBack) },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MoimDimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(MoimDimens.SpaceXl))

                // 눈송이 + "로그인 해줘!" 말풍선
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.img_nunsongi),
                        contentDescription = stringResource(R.string.mascot_description),
                        modifier = Modifier.size(width = 112.dp, height = 140.dp),
                    )
                    Spacer(Modifier.width(MoimDimens.SpaceMd))
                    Text(
                        text = stringResource(R.string.login_bubble),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy,
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(24.dp), ambientColor = ShadowNavy, spotColor = ShadowNavy)
                            .clip(RoundedCornerShape(24.dp))
                            .background(White)
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                    )
                }

                Spacer(Modifier.height(MoimDimens.SpaceXl))

                // 학번
                MoimTextField(
                    value = uiState.studentId,
                    onValueChange = onStudentIdChange,
                    label = stringResource(R.string.login_student_id),
                    placeholder = stringResource(R.string.login_student_id_placeholder),
                    leadingIcon = Icons.Outlined.Badge,
                    trailingContent = if (uiState.studentId.isNotEmpty()) {
                        {
                            IconButton(onClick = { onStudentIdChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Cancel,
                                    contentDescription = stringResource(R.string.common_clear),
                                    tint = TextSecondary,
                                )
                            }
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                )

                Spacer(Modifier.height(MoimDimens.SpaceMd))

                // 비밀번호 (틀리면 아이콘이 빨갛게 + 아래 에러 문구)
                MoimTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.login_password),
                    placeholder = stringResource(R.string.login_password_placeholder),
                    leadingIcon = Icons.Outlined.Lock,
                    trailingContent = {
                        IconButton(onClick = onTogglePasswordVisible) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = stringResource(
                                    if (uiState.isPasswordVisible) R.string.common_hide_password else R.string.common_show_password,
                                ),
                                tint = if (errorText != null) ErrorRed else TextSecondary,
                            )
                        }
                    },
                    message = errorText,
                    messageType = FieldMessageType.Error,
                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
                )

                Spacer(Modifier.height(MoimDimens.SpaceSm))

                // 자동 로그인 / 비밀번호 찾기
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.autoLogin,
                        onCheckedChange = onAutoLoginChange,
                        colors = CheckboxDefaults.colors(checkedColor = Navy, uncheckedColor = Outline),
                    )
                    Text(
                        text = stringResource(R.string.login_auto),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { scope.launch { snackbarHostState.showSnackbar(forgotPasswordMessage) } }) {
                        Text(
                            text = stringResource(R.string.login_forgot_password),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(MoimDimens.SpaceLg))

                // 로그인 버튼 (로딩 중엔 동그라미)
                Button(
                    onClick = onLoginClick,
                    enabled = uiState.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MoimDimens.ButtonHeight),
                    shape = RoundedCornerShape(MoimDimens.ButtonRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy,
                        contentColor = White,
                        disabledContainerColor = Navy.copy(alpha = 0.4f),
                        disabledContentColor = White,
                    ),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Text(text = stringResource(R.string.login_button), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(MoimDimens.SpaceSm))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }

                Spacer(Modifier.height(MoimDimens.SpaceLg))

                // 회원가입으로
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.login_no_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    TextButton(onClick = onSignUpClick) {
                        Text(
                            text = stringResource(R.string.login_go_signup),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                        )
                    }
                }
                Spacer(Modifier.height(MoimDimens.SpaceLg))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    MoimTheme {
        LoginScreen(
            uiState = LoginUiState(studentId = "2316916", password = "wrongpass", errorRes = R.string.error_invalid_credentials),
            onBack = {},
            onStudentIdChange = {},
            onPasswordChange = {},
            onTogglePasswordVisible = {},
            onAutoLoginChange = {},
            onLoginClick = {},
            onSignUpClick = {},
        )
    }
}
