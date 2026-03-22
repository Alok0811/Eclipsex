package com.eclipse.browser.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

/**
 * WeatherCanvas — Section 25.2
 * Replaces Eclipse Orb with realistic animated weather visualization.
 * Design: minimalist, premium, soft gradient lighting — NOT cartoonish.
 * Placed exactly where orb was on home screen right side.
 */
@Composable
fun WeatherCanvas(
    icon: String,
    condition: String,
    temp: String,
    isNight: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather")

    // Master time for all animations
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wTime"
    )

    // Glow pulse for sun/moon
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Rain/snow particles
    val rainParticles = remember { generateRainParticles(20) }
    val snowParticles = remember { generateSnowParticles(15) }
    val starParticles = remember { generateStarParticles(12) }

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = minOf(size.width, size.height) * 0.38f

        when {
            // ── THUNDERSTORM ──
            icon.startsWith("11") -> {
                drawRainCloud(cx, cy, r, time)
                drawRaindrops(rainParticles, time, size, Color(0xFF6BAED6))
                drawLightning(cx, cy, r, time)
            }

            // ── RAIN / DRIZZLE ──
            icon.startsWith("09") || icon.startsWith("10") -> {
                drawRainCloud(cx, cy, r, time)
                drawRaindrops(rainParticles, time, size, Color(0xFF9ECAE1))
            }

            // ── SNOW ──
            icon.startsWith("13") -> {
                drawCloudBase(cx, cy * 0.75f, r * 0.85f, Color(0xFFDEEBF7), time)
                drawSnowflakes(snowParticles, time, size)
            }

            // ── MIST / FOG / HAZE ──
            icon.startsWith("50") -> {
                drawMistLayers(cx, cy, r, time)
            }

            // ── CLEAR DAY ──
            icon == "01d" -> {
                drawSun(cx, cy, r, glowPulse, accentColor)
            }

            // ── CLEAR NIGHT ──
            icon == "01n" -> {
                drawMoon(cx, cy, r, glowPulse)
                drawStars(starParticles, time, size)
            }

            // ── FEW CLOUDS DAY ──
            icon == "02d" -> {
                drawSunPartial(cx, cy, r * 0.75f, glowPulse, accentColor)
                drawCloudBase(cx + r * 0.25f, cy + r * 0.15f, r * 0.65f, Color(0xFFEFF3FF), time)
            }

            // ── FEW CLOUDS NIGHT ──
            icon == "02n" -> {
                drawMoonPartial(cx - r * 0.2f, cy - r * 0.1f, r * 0.65f, glowPulse)
                drawCloudBase(cx + r * 0.3f, cy + r * 0.2f, r * 0.6f, Color(0xFF6E6E8E), time)
                drawStars(starParticles.take(6), time, size)
            }

            // ── SCATTERED / BROKEN CLOUDS ──
            icon.startsWith("03") || icon.startsWith("04") -> {
                drawCloudBase(cx - r * 0.15f, cy - r * 0.1f, r * 0.9f, Color(0xFFBFD3E6), time)
                drawCloudBase(cx + r * 0.2f, cy + r * 0.15f, r * 0.65f, Color(0xFFD0D9E8), time)
            }

            else -> {
                // Fallback — simple clean orb placeholder
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.3f),
                            accentColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

// ── FULL-SCREEN WEATHER ANIMATION LAYER ──
// Section 25.5: Weather animations integrate with background
// Rain/snow/stars appear across entire home screen
@Composable
fun WeatherAnimationLayer(
    weatherIcon: String,
    isNight: Boolean,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    if (weatherIcon.isBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "fullWeather")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fullTime"
    )

    // Only show full-screen effects for rain, snow, thunderstorm
    when {
        weatherIcon.startsWith("09") || weatherIcon.startsWith("10") || weatherIcon.startsWith("11") -> {
            val particles = remember { generateRainParticles(40) }
            Canvas(modifier = modifier) {
                drawRaindrops(particles, time, size, Color(0xFF9ECAE1).copy(alpha = 0.15f))
            }
        }
        weatherIcon.startsWith("13") -> {
            val particles = remember { generateSnowParticles(30) }
            Canvas(modifier = modifier) {
                drawSnowflakes(particles, time, size, alpha = 0.2f)
            }
        }
    }
}

// ── DRAWING FUNCTIONS ──

private fun DrawScope.drawSun(cx: Float, cy: Float, r: Float, pulse: Float, accentColor: Color) {
    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE066).copy(alpha = 0.35f * pulse),
                Color(0xFFFF9A2E).copy(alpha = 0.12f * pulse),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = r * 1.4f
        ),
        radius = r * 1.4f,
        center = Offset(cx, cy)
    )
    // Main sun disc — warm gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF176),
                Color(0xFFFFCA28),
                Color(0xFFFF8F00)
            ),
            center = Offset(cx - r * 0.12f, cy - r * 0.12f),
            radius = r
        ),
        radius = r * 0.7f,
        center = Offset(cx, cy)
    )
    // Sun rays — 8 soft rays
    for (i in 0 until 8) {
        val angle = i * (PI / 4).toFloat()
        val rayStart = r * 0.82f
        val rayEnd = r * 1.12f
        drawLine(
            color = Color(0xFFFFCA28).copy(alpha = 0.5f * pulse),
            start = Offset(cx + cos(angle) * rayStart, cy + sin(angle) * rayStart),
            end = Offset(cx + cos(angle) * rayEnd, cy + sin(angle) * rayEnd),
            strokeWidth = r * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSunPartial(cx: Float, cy: Float, r: Float, pulse: Float, accentColor: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF176), Color(0xFFFFCA28), Color(0xFFFF8F00)),
            center = Offset(cx - r * 0.1f, cy - r * 0.1f),
            radius = r
        ),
        radius = r * 0.6f,
        center = Offset(cx, cy)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE066).copy(alpha = 0.3f * pulse), Color.Transparent),
            center = Offset(cx, cy),
            radius = r
        ),
        radius = r,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawMoon(cx: Float, cy: Float, r: Float, pulse: Float) {
    // Moon glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE8E0D0).copy(alpha = 0.2f * pulse),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = r * 1.3f
        ),
        radius = r * 1.3f,
        center = Offset(cx, cy)
    )
    // Moon disc — crescent effect using clipping shadow
    drawCircle(
        color = Color(0xFFE8E0C8),
        radius = r * 0.72f,
        center = Offset(cx, cy)
    )
    // Shadow disc offset to create crescent
    drawCircle(
        color = Color(0xFF0B0B18),
        radius = r * 0.62f,
        center = Offset(cx + r * 0.28f, cy - r * 0.08f)
    )
    // Subtle surface texture glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.15f), Color.Transparent),
            center = Offset(cx - r * 0.2f, cy - r * 0.25f),
            radius = r * 0.45f
        ),
        radius = r * 0.45f,
        center = Offset(cx - r * 0.2f, cy - r * 0.25f)
    )
}

private fun DrawScope.drawMoonPartial(cx: Float, cy: Float, r: Float, pulse: Float) {
    drawCircle(color = Color(0xFFE0D8C8), radius = r * 0.6f, center = Offset(cx, cy))
    drawCircle(color = Color(0xFF0B0B18), radius = r * 0.52f, center = Offset(cx + r * 0.24f, cy - r * 0.06f))
}

private fun DrawScope.drawCloudBase(cx: Float, cy: Float, r: Float, cloudColor: Color, time: Float) {
    val drift = sin(time * 0.00008f) * r * 0.05f
    // Cloud shape — multiple overlapping circles
    val circles = listOf(
        Triple(cx + drift, cy, r * 0.5f),
        Triple(cx - r * 0.38f + drift, cy + r * 0.1f, r * 0.38f),
        Triple(cx + r * 0.38f + drift, cy + r * 0.1f, r * 0.38f),
        Triple(cx - r * 0.18f + drift, cy + r * 0.2f, r * 0.44f),
        Triple(cx + r * 0.18f + drift, cy + r * 0.2f, r * 0.44f)
    )
    // Shadow bottom
    circles.forEach { (x, y, radius) ->
        drawCircle(
            color = cloudColor.copy(alpha = 0.3f),
            radius = radius,
            center = Offset(x, y + radius * 0.12f)
        )
    }
    // Main cloud
    circles.forEach { (x, y, radius) ->
        drawCircle(color = cloudColor, radius = radius, center = Offset(x, y))
    }
    // Highlight top
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
            center = Offset(cx + drift, cy - r * 0.15f),
            radius = r * 0.3f
        ),
        radius = r * 0.3f,
        center = Offset(cx + drift, cy - r * 0.15f)
    )
}

private fun DrawScope.drawRainCloud(cx: Float, cy: Float, r: Float, time: Float) {
    drawCloudBase(cx, cy * 0.7f, r * 0.85f, Color(0xFF7C9DB8), time)
}

private fun DrawScope.drawRaindrops(
    particles: List<RainParticle>,
    time: Float,
    size: androidx.compose.ui.geometry.Size,
    color: Color
) {
    particles.forEach { p ->
        val progress = ((time * p.speed + p.offset) % 1f)
        val y = progress * (size.height + 20f) - 20f
        val x = p.x * size.width
        drawLine(
            color = color.copy(alpha = p.alpha),
            start = Offset(x, y),
            end = Offset(x - 2f, y + 8f),
            strokeWidth = 1.2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSnowflakes(
    particles: List<SnowParticle>,
    time: Float,
    size: androidx.compose.ui.geometry.Size,
    alpha: Float = 1f
) {
    particles.forEach { p ->
        val progress = ((time * p.speed + p.offset) % 1f)
        val y = progress * (size.height + p.radius * 2)
        val drift = sin(time * p.driftSpeed + p.driftOffset) * 8f
        val x = p.x * size.width + drift
        drawCircle(
            color = Color.White.copy(alpha = p.alpha * alpha),
            radius = p.radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawStars(
    particles: List<StarParticle>,
    time: Float,
    size: androidx.compose.ui.geometry.Size
) {
    particles.forEach { p ->
        val twinkle = 0.3f + 0.7f * (0.5f + 0.5f * sin(time * p.speed + p.offset))
        drawCircle(
            color = Color.White.copy(alpha = p.alpha * twinkle),
            radius = p.radius,
            center = Offset(p.x * size.width, p.y * size.height)
        )
    }
}

private fun DrawScope.drawLightning(cx: Float, cy: Float, r: Float, time: Float) {
    // Occasional lightning flash
    val flash = sin(time * 0.003f)
    if (flash > 0.8f) {
        val intensity = (flash - 0.8f) / 0.2f
        drawLine(
            color = Color(0xFFFFFFAA).copy(alpha = intensity * 0.8f),
            start = Offset(cx, cy),
            end = Offset(cx + r * 0.3f, cy + r * 0.7f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFFFFAA).copy(alpha = intensity * 0.6f),
            start = Offset(cx + r * 0.3f, cy + r * 0.7f),
            end = Offset(cx + r * 0.1f, cy + r * 1.1f),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawMistLayers(cx: Float, cy: Float, r: Float, time: Float) {
    val drift = sin(time * 0.00006f) * r * 0.15f
    for (i in 0..3) {
        val y = cy + (i - 1.5f) * r * 0.3f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFB0C4D8).copy(alpha = 0.25f),
                    Color(0xFFB0C4D8).copy(alpha = 0.35f),
                    Color(0xFFB0C4D8).copy(alpha = 0.25f),
                    Color.Transparent
                ),
                startX = 0f,
                endX = size.width
            ),
            start = Offset(drift, y),
            end = Offset(size.width + drift, y),
            strokeWidth = r * 0.2f,
            cap = StrokeCap.Round
        )
    }
}

// ── PARTICLE GENERATORS ──

data class RainParticle(val x: Float, val speed: Float, val offset: Float, val alpha: Float)
data class SnowParticle(val x: Float, val y: Float, val radius: Float, val speed: Float, val offset: Float, val alpha: Float, val driftSpeed: Float, val driftOffset: Float)
data class StarParticle(val x: Float, val y: Float, val radius: Float, val speed: Float, val offset: Float, val alpha: Float)

private fun generateRainParticles(count: Int): List<RainParticle> {
    return List(count) {
        RainParticle(
            x = Random.nextFloat(),
            speed = 0.00015f + Random.nextFloat() * 0.0001f,
            offset = Random.nextFloat(),
            alpha = 0.3f + Random.nextFloat() * 0.4f
        )
    }
}

private fun generateSnowParticles(count: Int): List<SnowParticle> {
    return List(count) {
        SnowParticle(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            radius = 2f + Random.nextFloat() * 3f,
            speed = 0.00004f + Random.nextFloat() * 0.00003f,
            offset = Random.nextFloat(),
            alpha = 0.4f + Random.nextFloat() * 0.4f,
            driftSpeed = 0.00008f + Random.nextFloat() * 0.00005f,
            driftOffset = Random.nextFloat() * 10f
        )
    }
}

private fun generateStarParticles(count: Int): List<StarParticle> {
    return List(count) {
        StarParticle(
            x = Random.nextFloat(),
            y = Random.nextFloat() * 0.6f, // upper area
            radius = 0.8f + Random.nextFloat() * 1.5f,
            speed = 0.00005f + Random.nextFloat() * 0.00004f,
            offset = Random.nextFloat() * 10f,
            alpha = 0.4f + Random.nextFloat() * 0.5f
        )
    }
}
