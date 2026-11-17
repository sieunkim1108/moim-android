package com.moim.app.feature.chat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.SkyBlue
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White

/**
 * 채팅 입력창: [+](시간표 캡처) / 입력칸 / [보내기]
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(color = White, shadowElevation = 8.dp, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(MoimDimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onAttachClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = SkyBlue, contentColor = Navy),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.main_attach))
            }

            // 알약 모양 입력칸. BasicTextField로 높이·여백을 Stitch 디자인에 맞춘다.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MoimDimens.ChatInputHeight)
                    .clip(CircleShape)
                    .background(SurfaceContainerLow)
                    .padding(horizontal = MoimDimens.SpaceMd, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.main_input_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Navy),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 항상 보내기 버튼. 글자가 있을 때만 눌린다.
            val canSend = text.isNotBlank() && enabled
            IconButton(
                onClick = onSend,
                enabled = canSend,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Navy,
                    contentColor = White,
                    disabledContainerColor = Navy.copy(alpha = 0.4f),
                    disabledContentColor = White,
                ),
                modifier = Modifier.size(44.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.main_send))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputBarPreview() {
    MoimTheme {
        ChatInputBar(text = "", onTextChange = {}, onSend = {}, onAttachClick = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputBarTypingPreview() {
    MoimTheme {
        ChatInputBar(text = "내일 3시에 팀플 회의", onTextChange = {}, onSend = {}, onAttachClick = {})
    }
}
