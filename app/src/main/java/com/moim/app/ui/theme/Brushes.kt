package com.moim.app.ui.theme

import androidx.compose.ui.graphics.Brush

/** 반복해서 쓰는 그라데이션 */
object MoimBrushes {
    // Stitch 시작/로그인 화면: linear-gradient(white 0%, white 42%, #EDF5FF 72%, #DCEEFF 100%)
    val SkyFade: Brush = Brush.verticalGradient(
        0.0f to White,
        0.42f to White,
        0.72f to SkyBlueLight,
        1.0f to SkyBlue,
    )

    // 회원가입 화면: 아주 옅은 하늘색 배경
    val SoftSky: Brush = Brush.verticalGradient(
        0.0f to Surface,
        1.0f to SkyBlueLight,
    )
}
