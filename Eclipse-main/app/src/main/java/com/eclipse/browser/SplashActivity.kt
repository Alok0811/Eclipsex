package com.eclipse.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            SplashScreen {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
    }
}

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }

    // Timings: 0=stars appear, 1=logo, 2=tagline, 3=navigate
    LaunchedEffect(Unit) {
        delay(400)
        phase = 1  // show logo
        delay(800)
        phase = 2  // show tagline
        delay(1200)
        phase = 3  // navigate
        delay(300)
        onFinished()
    }

    // Logo scale
    val logoScale by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(500),
        label = "logoAlpha"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(600),
        label = "taglineAlpha"
    )
    val brandAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 0.4f else 0f,
        animationSpec = tween(800),
        label = "brandAlpha"
    )

    // Star data
    val stars = remember {
        List(80) {
            SplashStar(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2f + 0.5f,
                speed = Random.nextFloat() * 0.5f + 0.5f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splashStars")
    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Star field
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEach { star ->
                val alpha = ((kotlin.math.sin((twinkle + star.speed) * Math.PI * 2).toFloat() + 1f) / 2f)
                    .coerceIn(0.2f, 0.9f)
                drawCircle(
                    color = Color.White.copy(alpha = alpha * if (phase >= 1) 1f else 0.3f),
                    radius = star.size,
                    center = Offset(star.x * size.width, star.y * size.height)
                )
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-20).dp)
        ) {
            // Eclipse orb
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    // Glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentGold.copy(alpha = 0.3f),
                                AccentGold.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = size.minDimension * 0.7f
                        ),
                        radius = size.minDimension * 0.5f
                    )
                    // Dark disc
                    drawCircle(
                        color = Color.Black,
                        radius = size.minDimension * 0.28f
                    )
                    // Golden ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                AccentGold,
                                AccentGold.copy(alpha = 0.6f),
                                AccentGold.copy(alpha = 0.3f),
                                AccentGold
                            )
                        ),
                        radius = size.minDimension * 0.32f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ECLIPSE text
            Text(
                text = "ECLIPSE",
                fontFamily = Outfit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = TextPrimary,
                letterSpacing = 8.sp,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "SEE BEYOND",
                fontFamily = SpaceMono,
                fontSize = 11.sp,
                color = AccentGold,
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(taglineAlpha)
            )
        }

        // Bottom brand
        Text(
            text = "DAWN 0.1",
            fontFamily = SpaceMono,
            fontSize = 10.sp,
            color = TextMuted,
            letterSpacing = 2.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(brandAlpha)
        )
    }
}

private data class SplashStar(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float
)
