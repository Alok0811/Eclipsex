package com.eclipse.browser.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val startY: Float,
    val size: Float,
    val driftX: Float,
    val duration: Float,
    val born: Float
)

@Composable
fun ParticleSystem(
    accentColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTime"
    )

    val particles = remember {
        mutableStateListOf<Particle>().apply {
            repeat(10) { i ->
                add(
                    Particle(
                        x = Random.nextFloat() * 0.9f + 0.05f,
                        startY = 1f + Random.nextFloat() * 0.1f,
                        size = Random.nextFloat() * 2.5f + 0.8f,
                        driftX = (Random.nextFloat() - 0.5f) * 0.1f,
                        duration = 7000f + Random.nextFloat() * 8000f,
                        born = i * 900f
                    )
                )
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val elapsed = (time - p.born).mod(p.duration)
            val progress = elapsed / p.duration
            if (progress < 0f || progress > 1f) return@forEach

            val y = p.startY - progress * 1.3f
            val x = p.x + p.driftX * kotlin.math.sin(progress * Math.PI.toFloat() * 2f)
            val alpha = if (progress < 0.1f) progress / 0.1f
            else if (progress > 0.8f) (1f - progress) / 0.2f
            else 1f

            drawCircle(
                color = accentColor.copy(alpha = alpha * 0.7f),
                radius = p.size * 2f,
                center = Offset(x * w, y * h)
            )
            drawCircle(
                color = accentColor.copy(alpha = alpha * 0.4f),
                radius = p.size * 5f,
                center = Offset(x * w, y * h)
            )
        }
    }
}
