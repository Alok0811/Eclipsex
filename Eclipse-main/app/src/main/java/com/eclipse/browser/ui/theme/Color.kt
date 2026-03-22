package com.eclipse.browser.ui.theme

import androidx.compose.ui.graphics.Color

// Backgrounds (darkest to lightest)
val EclipseBlack = Color(0xFF000000)
val EclipseSurface = Color(0x0EFFFFFF)
val EclipseSurface2 = Color(0x17FFFFFF)
val EclipseBorder = Color(0x1AFFFFFF)
val EclipseBorder2 = Color(0x0FFFFFFF)

// Text
val TextPrimary = Color(0xFFF0F0F0)
val TextMuted = Color(0x66FFFFFF)
val TextMuted2 = Color(0x38FFFFFF)

// Default accent
val AccentOrange = Color(0xFFFF6B1A)
val AccentOrangeLight = Color(0xFFFFB347)
val AccentGold = Color(0xFFE8C840)

// Incognito
val VoidPurple = Color(0xFF7C00FF)
val VoidPurpleLight = Color(0xFFC87EFF)

// Glass
val GlassSurface = Color(0x8C0C0C18)
val GlassBorder = Color(0x1FFFFFFF)
val GlassFallback = Color(0xCC0C0C18)
val GlassFallbackBorder = Color(0x1AFFFFFF)
val SheetBg = Color(0xFF0C0C18)

// Bottom bar
val BottomBarBg = Color(0xFF08080F)

// Predefined accent colors
data class AccentPair(val primary: Color, val light: Color, val hex: String)

val AccentColors = listOf(
    AccentPair(Color(0xFFFF6B1A), Color(0xFFFFB347), "#FF6B1A"),
    AccentPair(Color(0xFF6C63FF), Color(0xFF9D99FF), "#6C63FF"),
    AccentPair(Color(0xFF00D4AA), Color(0xFF5FFFD9), "#00D4AA"),
    AccentPair(Color(0xFFFF3D9A), Color(0xFFFF85C2), "#FF3D9A"),
    AccentPair(Color(0xFFFFD700), Color(0xFFFFE966), "#FFD700"),
    AccentPair(Color(0xFF00CFFF), Color(0xFF7FE8FF), "#00CFFF"),
    AccentPair(Color(0xFFE8C840), Color(0xFFFFE878), "#E8C840"),
)
