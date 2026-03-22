package com.eclipse.browser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EclipseDarkScheme = darkColorScheme(
    primary = AccentOrange,
    secondary = AccentGold,
    background = EclipseBlack,
    surface = EclipseSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun EclipseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EclipseDarkScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
