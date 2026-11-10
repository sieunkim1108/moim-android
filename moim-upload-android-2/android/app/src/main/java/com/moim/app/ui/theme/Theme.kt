package com.moim.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/*
 * moim은 라이트 모드 하나만 지원한다 (디자인이 라이트 기준).
 * Material3 컴포넌트(Button, TextField 등)가 이 색을 자동으로 가져다 쓴다.
 */
private val MoimColorScheme = lightColorScheme(
    primary = Navy,
    onPrimary = White,
    primaryContainer = Navy,
    onPrimaryContainer = White,
    secondary = Secondary,
    onSecondary = White,
    secondaryContainer = SkyBlue,
    onSecondaryContainer = Navy,
    background = SkyBlue,
    onBackground = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    surfaceVariant = Surface,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = ErrorRed,
    onError = White,
)

@Composable
fun MoimTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MoimColorScheme,
        typography = MoimTypography,
        shapes = MoimShapes,
        content = content,
    )
}
