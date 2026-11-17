package com.moim.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.Outline
import com.moim.app.ui.theme.SurfaceContainerHigh
import com.moim.app.ui.theme.TextPrimary

/**
 * 화면 맨 위 바: [←] 제목. 상태바 높이만큼 자동으로 내려온다.
 */
@Composable
fun MoimTopBar(
    title: String?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = TextPrimary,
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        actions()
    }
}

/**
 * 회원가입 단계 표시: "기본 정보 (1/2 단계) ........ 50% 완료" + 진행 바
 */
@Composable
fun StepProgressHeader(
    stepLabel: String,
    percentLabel: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Navy,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = percentLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (progress >= 1f) Navy else Outline,
            )
        }
        Spacer(Modifier.height(MoimDimens.SpaceSm))
        // 진행 바: 회색 트랙 위에 남색 막대
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(SurfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Navy),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoimHeaderPreview() {
    MoimTheme {
        Column {
            MoimTopBar(title = "회원가입", onBack = {})
            StepProgressHeader(
                stepLabel = "기본 정보 (1/2 단계)",
                percentLabel = "50% 완료",
                progress = 0.5f,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
