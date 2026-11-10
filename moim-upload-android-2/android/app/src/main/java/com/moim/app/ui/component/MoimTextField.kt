package com.moim.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.ui.theme.ErrorRed
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.Outline
import com.moim.app.ui.theme.ShadowNavy
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.White

/** 입력칸 아래 안내 문구의 종류 */
enum class FieldMessageType { Help, Error, Success }

/**
 * moim 공통 입력칸: 흰 배경, 12dp 둥근 모서리, 옅은 남색 그림자, 밑줄 없음.
 * label(위 제목), leadingIcon(왼쪽 아이콘), trailingContent(오른쪽 버튼/아이콘), message(아래 안내)를 선택적으로 붙인다.
 * onClick을 주면 직접 입력 대신 클릭만 받는다 (드롭다운, 날짜 선택용).
 */
@Composable
fun MoimTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = Navy,
    trailingContent: (@Composable () -> Unit)? = null,
    message: String? = null,
    messageType: FieldMessageType = FieldMessageType.Help,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val isError = messageType == FieldMessageType.Error && message != null
    val shape = RoundedCornerShape(MoimDimens.InputRadius)

    Column(modifier = modifier) {
        if (label != null) {
            FieldLabel(text = label)
            Spacer(Modifier.height(MoimDimens.SpaceSm))
        }
        Box {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 3.dp, shape = shape, ambientColor = ShadowNavy, spotColor = ShadowNavy),
                shape = shape,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = placeholder?.let { { Text(text = it, color = Outline) } },
                leadingIcon = leadingIcon?.let {
                    { Icon(imageVector = it, contentDescription = null, tint = if (isError) ErrorRed else leadingIconTint) }
                },
                trailingIcon = trailingContent,
                isError = isError,
                readOnly = readOnly || onClick != null,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = White,
                    unfocusedContainerColor = White,
                    disabledContainerColor = White,
                    errorContainerColor = White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = Navy,
                    errorCursorColor = ErrorRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    errorTextColor = TextPrimary,
                ),
            )
            if (onClick != null) {
                // 입력칸 전체를 덮는 투명 영역: 키보드 대신 클릭 이벤트만 받는다
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        ),
                )
            }
        }
        if (message != null) {
            FieldMessage(text = message, type = messageType)
        }
    }
}

/** 입력칸 위 제목 */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = modifier,
    )
}

/** 입력칸 아래 안내/에러/성공 문구 */
@Composable
fun FieldMessage(text: String, type: FieldMessageType, modifier: Modifier = Modifier) {
    val color = when (type) {
        FieldMessageType.Help -> Outline
        FieldMessageType.Error -> ErrorRed
        FieldMessageType.Success -> Navy
    }
    Row(
        modifier = modifier.padding(top = 6.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (type == FieldMessageType.Error) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

/**
 * 목록에서 하나를 고르는 칸 (학부, 학과 선택).
 * 누르면 아래로 메뉴가 펼쳐진다.
 */
@Composable
fun MoimSelectField(
    selected: String?,
    options: List<String>,
    onSelect: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    message: String? = null,
    messageType: FieldMessageType = FieldMessageType.Help,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        if (label != null) {
            FieldLabel(text = label)
            Spacer(Modifier.height(MoimDimens.SpaceSm))
        }
        Box {
            MoimTextField(
                value = selected.orEmpty(),
                onValueChange = {},
                placeholder = placeholder,
                trailingContent = {
                    Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = Navy)
                },
                onClick = { if (enabled) expanded = true },
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = White,
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        if (message != null) {
            FieldMessage(text = message, type = messageType)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FF)
@Composable
private fun MoimTextFieldPreview() {
    MoimTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MoimTextField(
                value = "2316916",
                onValueChange = {},
                label = "학번",
                leadingIcon = Icons.Outlined.Badge,
                message = "학번은 로그인 아이디로 사용돼요",
            )
            Spacer(Modifier.height(16.dp))
            MoimTextField(
                value = "",
                onValueChange = {},
                label = "이름",
                placeholder = "이름 입력",
                message = "이름을 입력해 주세요",
                messageType = FieldMessageType.Error,
            )
            Spacer(Modifier.height(16.dp))
            MoimSelectField(
                selected = "소프트웨어학부",
                options = listOf("소프트웨어학부", "공과대학"),
                onSelect = {},
                placeholder = "선택해 주세요",
                label = "학부",
            )
        }
    }
}
