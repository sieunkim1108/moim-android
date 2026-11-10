package com.moim.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * 글꼴. 지금은 시스템 기본 글꼴을 쓴다.
 * Pretendard를 쓰려면:
 *  1) Pretendard-Regular/Medium/SemiBold/Bold.otf를 res/font 에 소문자 이름으로 넣고
 *  2) 아래 MoimFontFamily를 FontFamily(Font(R.font.pretendard_regular, FontWeight.Normal), ...)로 바꾸면 끝.
 */
val MoimFontFamily: FontFamily = FontFamily.Default

// Stitch DESIGN.md의 typography 토큰을 Material3 Typography에 대응시킴
val MoimTypography = Typography(
    // headline-lg-mobile: 26px / 34px / 700
    headlineLarge = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em
    ),
    // headline-md: 22px / 28px / 600
    headlineMedium = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.015).em
    ),
    // headline-sm: 18px / 24px / 600
    headlineSmall = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em
    ),
    titleMedium = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp
    ),
    // body-lg: 16px / 24px
    bodyLarge = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em
    ),
    // body-md: 14px / 21px
    bodyMedium = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = (-0.005).em
    ),
    // body-sm: 13px / 18px
    bodySmall = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp
    ),
    // label-lg: 14px / 18px / 600 (버튼 글자)
    labelLarge = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.01.em
    ),
    // label-md: 12px / 16px / 600
    labelMedium = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em
    ),
    // label-sm: 11px / 14px / 500
    labelSmall = TextStyle(
        fontFamily = MoimFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.02.em
    ),
)
