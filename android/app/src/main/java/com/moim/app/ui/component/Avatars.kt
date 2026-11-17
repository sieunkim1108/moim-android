package com.moim.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moim.app.R
import com.moim.app.ui.theme.MoimTheme
import com.moim.app.ui.theme.Navy
import com.moim.app.ui.theme.PastelMint
import com.moim.app.ui.theme.PastelPink
import com.moim.app.ui.theme.PastelYellow
import com.moim.app.ui.theme.SecondaryContainer
import com.moim.app.ui.theme.White
import kotlin.math.abs

private val AvatarColors = listOf(PastelPink, SecondaryContainer, PastelMint, PastelYellow)

/**
 * 프로필 사진이 생기기 전까지 쓰는 동그라미: 이름 첫 글자(성 제외) + 파스텔 배경.
 * 같은 id는 항상 같은 색.
 */
@Composable
fun InitialAvatar(
    name: String,
    id: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val given = if (name.length >= 3) name.drop(1) else name
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(AvatarColors[abs(id.hashCode()) % AvatarColors.size]),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = given.take(1),
            style = MaterialTheme.typography.titleMedium,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Bold,
            color = Navy,
        )
    }
}

/** 눈송이(AI) 동그라미 */
@Composable
fun NunsongiAvatar(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(White)
            .border(1.dp, SecondaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_nunsongi),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .padding(size * 0.12f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AvatarsPreview() {
    MoimTheme {
        Row(Modifier.padding(8.dp)) {
            InitialAvatar("김시은", "a")
            InitialAvatar("박지원", "b")
            NunsongiAvatar()
        }
    }
}
