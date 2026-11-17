package com.moim.app.feature.auth.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.ui.component.MoimOutlinedButton
import com.moim.app.ui.component.MoimPrimaryButton
import com.moim.app.ui.theme.MoimBrushes
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.NavyDeep
import com.moim.app.ui.theme.TextSecondary

/**
 * 1. 시작 화면 (Stitch: moim_시작페이지)
 * 흰색 → 하늘색 그라데이션 배경, 눈송이, moim 로고, [로그인] [회원가입]
 * 상태가 없는 화면이라 ViewModel 없이 클릭 콜백만 받는다.
 */
@Composable
fun StartScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoimBrushes.SkyFade),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 448.dp) // Stitch max-w-md (태블릿에서 너무 넓어지지 않게)
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = MoimDimens.ScreenPadding, vertical = MoimDimens.SpaceXl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 눈송이 마스코트 (width 200dp, 최대 높이 220dp)
            Image(
                painter = painterResource(R.drawable.img_nunsongi),
                contentDescription = stringResource(R.string.mascot_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(MoimDimens.MascotWidthLarge)
                    .heightIn(max = 220.dp),
            )
            Spacer(Modifier.height(MoimDimens.SpaceMd))

            // moim 로고 + 눈꽃 아이콘
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = NavyDeep,
                )
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = null, // 장식용
                    tint = Navy,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(6.dp))

            // 부제
            Text(
                text = stringResource(R.string.start_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(52.dp)) // Stitch mb-5 + mt-8

            // 버튼 (간격 12dp = space-y-3)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoimPrimaryButton(text = stringResource(R.string.start_login), onClick = onLoginClick)
                MoimOutlinedButton(text = stringResource(R.string.start_signup), onClick = onSignUpClick)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StartScreenPreview() {
    MoimTheme {
        StartScreen(onLoginClick = {}, onSignUpClick = {})
    }
}
