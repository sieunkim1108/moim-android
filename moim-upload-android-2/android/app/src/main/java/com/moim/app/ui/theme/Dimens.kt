package com.moim.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 간격·크기 토큰 (Stitch spacing 기준, 1rem = 16dp) */
object MoimDimens {
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 16.dp
    val SpaceLg = 24.dp
    val SpaceXl = 32.dp
    val ScreenPadding = 16.dp     // margin-mobile
    val ButtonHeight = 56.dp      // Stitch 시작 화면 h-14
    val CardRadius = 16.dp
    val InputRadius = 12.dp
    val ButtonRadius = 16.dp      // Stitch rounded-2xl
    val MascotWidthLarge = 200.dp

    // 채팅 화면
    val ChatAvatarSize = 40.dp     // 눈송이 프로필 동그라미
    val ChatBubbleRadius = 20.dp   // 말풍선 큰 모서리
    val ChatBubbleTail = 4.dp      // 말풍선 꼬리 쪽 작은 모서리
    val ChatBubbleMaxWidth = 280.dp
    val ChatInputHeight = 48.dp
}

/** 모서리 모양. small = 입력칸, medium/large = 카드·버튼 */
val MoimShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(MoimDimens.InputRadius),
    medium = RoundedCornerShape(MoimDimens.CardRadius),
    large = RoundedCornerShape(MoimDimens.CardRadius),
    extraLarge = RoundedCornerShape(24.dp),
)
