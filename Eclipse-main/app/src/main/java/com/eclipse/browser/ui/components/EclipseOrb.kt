package com.eclipse.browser.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun EclipseOrb(
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val infiniteTransition = rememberInfiniteTransition(label = "orbFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbY"
    )

    Canvas(modifier = modifier.size(90.dp).graphicsLayer { translationY = floatY * density }) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.width * 0.41f

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x88FFE88A),
                    Color(0x2EFFAA20),
                    Color.Transparent
                ),
                center = Offset(cx * 0.52f, cy),
                radius = radius * 1.3f
            ),
            center = Offset(cx * 0.52f, cy),
            radius = radius * 1.3f
        )

        // Main ring — golden stroke
        drawCircle(
            color = Color(0x99D4A840),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 8f)
        )

        // Inner thin ring
        drawCircle(
            color = Color(0xD9D4A840),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 1.3f)
        )

        // Outer subtle ring
        drawCircle(
            color = Color(0x66FFE878),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 0.8f)
        )

        // Shadow disc (eclipse effect)
        drawCircle(
            color = Color(0xFF060609),
            radius = radius * 0.975f,
            center = Offset(cx + radius * 0.18f, cy)
        )

        // Glow ring behind shadow
        drawCircle(
            color = Color(0x2EFFE878),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 4f)
        )

        // Small stars around orb
        val starPositions = listOf(
            Offset(cx * 0.26f, cy * 0.58f) to 1.1f,
            Offset(cx * 1.7f, cy * 0.42f) to 1.3f,
            Offset(cx * 1.8f, cy * 1.38f) to 0.9f,
            Offset(cx * 0.28f, cy * 1.55f) to 1.2f,
            Offset(cx * 1.58f, cy * 1.72f) to 0.8f,
            Offset(cx * 0.4f, cy * 1.12f) to 0.7f,
            Offset(cx * 1.64f, cy * 0.72f) to 1.0f,
        )
        starPositions.forEach { (pos, r) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = r,
                center = pos
            )
        }
    }
}
