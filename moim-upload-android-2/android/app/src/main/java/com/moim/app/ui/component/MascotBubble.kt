package com.moim.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moim.app.R
import com.moim.app.ui.theme.MoimDimens
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.ShadowNavy
import com.moim.app.ui.theme.SkyBlueLight
import com.moim.app.ui.theme.TextPrimary
import com.moim.app.ui.theme.White

/**
 * 동그란 눈송이 + 말풍선. 회원가입 화면 상단 안내에 쓴다.
 * withCard = true면 옅은 하늘색 카드 안에 넣는다 (회원가입 1단계 디자인).
 */
@Composable
fun MascotBubble(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    withCard: Boolean = false,
) {
    val content = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .shadow(2.dp, CircleShape, ambientColor = ShadowNavy, spotColor = ShadowNavy)
                    .clip(CircleShape)
                    .background(White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.img_nunsongi),
                    contentDescription = stringResource(R.string.mascot_description),
                    modifier = Modifier.size(58.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                modifier = Modifier
                    .weight(1f)
                    .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = ShadowNavy, spotColor = ShadowNavy)
                    .clip(RoundedCornerShape(16.dp))
                    .background(White)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
    if (withCard) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MoimDimens.CardRadius))
                .background(SkyBlueLight)
                .padding(12.dp),
        ) { content() }
    } else {
        Box(modifier = modifier.fillMaxWidth()) { content() }
    }
}

/** "앞부분 + [강조] + 뒷부분" 형태의 말풍선 문장을 만든다. 강조는 남색 굵은 글씨. */
fun highlightedText(
    start: String = "",
    highlight: String,
    end: String = "",
): AnnotatedString = buildAnnotatedString {
    append(start)
    withStyle(SpanStyle(color = Navy, fontWeight = FontWeight.Bold)) { append(highlight) }
    append(end)
}

@Preview(showBackground = true)
@Composable
private fun MascotBubblePreview() {
    MoimTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MascotBubble(
                text = highlightedText(highlight = "너에 대해 알려줘!", end = "\n맞춤 일정을 짜줄게 ✨"),
                withCard = true,
            )
            MascotBubble(
                text = highlightedText(start = "평소에 일정을 어떻게 준비하는 편이야? ", highlight = "너의 페이스", end = "에 맞춰 알려줄게! ✨"),
            )
        }
    }
}
