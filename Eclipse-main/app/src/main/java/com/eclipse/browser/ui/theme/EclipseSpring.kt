package com.eclipse.browser.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object EclipseSpring {
    val standard = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    val snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    val float = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 120f
    )
}
