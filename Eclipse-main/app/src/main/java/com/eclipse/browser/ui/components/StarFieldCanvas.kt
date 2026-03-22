package com.eclipse.browser.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alphaLow: Float,
    val alphaHigh: Float,
    val speed: Float,
    val delay: Float
)

@Composable
fun StarFieldCanvas(
    isNight: Boolean = false,
    bgTheme: String = "eclipse",
    modifier: Modifier = Modifier
) {
    val starCount = if (isNight) 110 else 75
    val stars = remember(isNight) {
        (0 until starCount).map {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 2.2f + 0.4f,
                alphaLow = 0.04f + Random.nextFloat() * 0.15f,
                alphaHigh = if (isNight) 0.45f + Random.nextFloat() * 0.55f else 0.35f + Random.nextFloat() * 0.65f,
                speed = 2000f + Random.nextFloat() * 4000f,
                delay = Random.nextFloat() * 4000f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starField")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Theme gradient overlays (radial gradients fading to transparent, matching original canvas)
        when (bgTheme) {
            "nebula" -> {
                // Purple glow at 30%/30%
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x4D500078), Color.Transparent),
                        center = Offset(w * 0.3f, h * 0.3f),
                        radius = w * 0.55f
                    ),
                    radius = w * 0.55f,
                    center = Offset(w * 0.3f, h * 0.3f)
                )
                // Blue glow at 70%/60%
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x400032A0), Color.Transparent),
                        center = Offset(w * 0.7f, h * 0.6f),
                        radius = w * 0.45f
                    ),
                    radius = w * 0.45f,
                    center = Offset(w * 0.7f, h * 0.6f)
                )
            }
            "aurora" -> {
                // Green glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2E00C864), Color.Transparent),
                        center = Offset(w * 0.2f, h * 0.3f),
                        radius = h * 0.45f
                    ),
                    radius = h * 0.45f,
                    center = Offset(w * 0.2f, h * 0.3f)
                )
                // Teal glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x240096C8), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.3f),
                        radius = h * 0.45f
                    ),
                    radius = h * 0.45f,
                    center = Offset(w * 0.5f, h * 0.3f)
                )
                // Purple glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x296432C8), Color.Transparent),
                        center = Offset(w * 0.8f, h * 0.3f),
                        radius = h * 0.45f
                    ),
                    radius = h * 0.45f,
                    center = Offset(w * 0.8f, h * 0.3f)
                )
            }
        }

        // Stars
        stars.forEach { star ->
            val phase = ((time + star.delay) % star.speed) / star.speed
            val alpha = star.alphaLow + (star.alphaHigh - star.alphaLow) *
                    (0.5f + 0.5f * kotlin.math.sin(phase * 2f * Math.PI.toFloat()))
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.radius,
                center = Offset(star.x * w, star.y * h)
            )
        }
    }
}
