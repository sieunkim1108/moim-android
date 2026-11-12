package com.moim.app.feature.auth.ui.done

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.ui.component.MoimPrimaryButton
import com.moim.app.ui.theme.MoimBrushes
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.NavyDeep
import com.moim.app.ui.theme.Outline
import com.moim.app.ui.theme.OutlineVariant
import com.moim.app.ui.theme.PastelMint
import com.moim.app.ui.theme.PastelMintText
import com.moim.app.ui.theme.PastelPinkText
import com.moim.app.ui.theme.SelectedCard
import com.moim.app.ui.theme.SurfaceContainer
import com.moim.app.ui.theme.SurfaceContainerLow
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.TextSecondary
import com.moim.app.ui.theme.White

/**
 * 5. 가입 완료 (Stitch: moim_회원가입_성공)
 * 상태가 거의 없어서 ViewModel 없이 이름/학부/전공만 받아서 그린다.
 */
@Composable
fun SignUpDoneScreen(
    name: String,
    college: String,
    major: String,
    onStartChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MoimBrushes.SkyFade)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MoimDimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 상단: 학교 칩 + 닫기(X)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MoimDimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SurfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.School, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.done_school_chip),
                    style = MaterialTheme.typography.labelLarge,
                    color = Navy,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onStartChat,
                colors = IconButtonDefaults.iconButtonColors(containerColor = SurfaceContainer),
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = TextPrimary)
            }
        }

        Spacer(Modifier.height(MoimDimens.SpaceLg))

        // 눈송이 + "환영합니다!" 말풍선
        // TODO: 축하하는 눈송이 이미지(표정별 에셋)가 정리되면 교체
        Box(contentAlignment = Alignment.TopEnd) {
            Image(
                painter = painterResource(R.drawable.img_nunsongi),
                contentDescription = stringResource(R.string.mascot_description),
                modifier = Modifier
                    .padding(top = 24.dp, end = 40.dp)
                    .heightIn(max = 240.dp),
            )
            Text(
                text = stringResource(R.string.done_welcome_bubble),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyDeep,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(White)
                    .border(2.dp, Navy, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        Spacer(Modifier.height(MoimDimens.SpaceLg))

        // 학부 · 전공 칩
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(SelectedCard)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Navy),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.done_department, college, major),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
            )
        }

        Spacer(Modifier.height(MoimDimens.SpaceMd))

        Text(
            text = stringResource(R.string.done_title, givenName(name), vocativeSuffix(givenName(name))),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = NavyDeep,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MoimDimens.SpaceSm))
        Text(
            text = stringResource(R.string.done_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(MoimDimens.SpaceLg))

        BriefingCard()

        Spacer(Modifier.height(MoimDimens.SpaceLg))

        MoimPrimaryButton(text = stringResource(R.string.done_start_chat) + " →", onClick = onStartChat)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.done_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = Outline,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(MoimDimens.SpaceLg))
    }
}

/** "첫 일정 브리핑 준비 완료" 카드: 예시 질문 + 기능 2개 */
@Composable
private fun BriefingCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoimDimens.CardRadius))
            .background(White)
            .border(1.dp, OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(MoimDimens.CardRadius))
            .padding(MoimDimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.done_card_title),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.done_card_badge),
                style = MaterialTheme.typography.labelMedium,
                color = PastelMintText,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PastelMint)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceContainerLow)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Navy),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = White)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.done_card_example_q),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep,
                )
                Text(
                    text = stringResource(R.string.done_card_example_a),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeatureTile(Icons.Outlined.EventAvailable, Navy, stringResource(R.string.done_feature_timetable), Modifier.weight(1f))
            FeatureTile(Icons.Outlined.NotificationsActive, PastelPinkText, stringResource(R.string.done_feature_alarm), Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeatureTile(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

/** "황채원" → "채원" (세 글자 이상이면 성을 뺀다) */
internal fun givenName(fullName: String): String {
    val trimmed = fullName.trim()
    return if (trimmed.length >= 3) trimmed.substring(1) else trimmed
}

/** 받침이 있으면 "아"(채원아), 없으면 "야"(지아야) */
internal fun vocativeSuffix(name: String): String {
    val last = name.lastOrNull() ?: return "야"
    if (last !in '가'..'힣') return "야"
    val hasBatchim = (last.code - '가'.code) % 28 != 0
    return if (hasBatchim) "아" else "야"
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun SignUpDoneScreenPreview() {
    MoimTheme {
        SignUpDoneScreen(name = "황채원", college = "소프트웨어학부", major = "데이터사이언스전공", onStartChat = {})
    }
}
