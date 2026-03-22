package com.eclipse.browser.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.components.*
import com.eclipse.browser.ui.theme.*

@Composable
fun IncognitoScreen(
    onSearch: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "void")
    val orbFloat by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbFloat"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Void background glow
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .offset(y = (-80).dp + orbFloat.dp)
                .alpha(0.12f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(VoidPurple, Color.Transparent),
                        radius = 400f
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Void Orb
            EclipseEnterAnimation(index = 0) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .offset(y = orbFloat.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        VoidPurple.copy(alpha = 0.5f),
                                        VoidPurple.copy(alpha = 0.15f),
                                        Color.Transparent
                                    ),
                                    radius = 140f
                                ),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black, CircleShape)
                    )
                    // Purple ring
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Transparent, CircleShape)
                            .then(
                                Modifier.background(
                                    Brush.radialGradient(
                                        colors = listOf(Color.Transparent, VoidPurple.copy(alpha = 0.6f)),
                                        radius = 65f
                                    ),
                                    CircleShape
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // VOID MODE title
            EclipseEnterAnimation(index = 1) {
                Text(
                    text = "VOID MODE",
                    fontFamily = Outfit,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = VoidPurpleLight,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            EclipseEnterAnimation(index = 2) {
                Text(
                    text = "No history. No traces. You are invisible.",
                    fontFamily = SpaceMono,
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pills
            EclipseEnterAnimation(index = 2) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoidPill("🔒 No History")
                    VoidPill("👁 No Tracking")
                    VoidPill("🌑 Blackhole Session")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Search bar (purple accent)
            EclipseEnterAnimation(index = 3) {
                SearchBar(
                    accentColor = VoidPurple,
                    isIncognito = true,
                    onSearch = onSearch
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Pulsing tagline
            Text(
                text = "LEAVE NO TRACE",
                fontFamily = SpaceMono,
                fontSize = 10.sp,
                color = VoidPurple,
                letterSpacing = 3.sp,
                modifier = Modifier.alpha(pulseAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exit button
            EclipseEnterAnimation(index = 4) {
                Text(
                    text = "EXIT VOID",
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = VoidPurple,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VoidPurple.copy(alpha = 0.1f))
                        .clickable { onExit() }
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun VoidPill(text: String) {
    Text(
        text = text,
        fontFamily = SpaceMono,
        fontSize = 11.sp,
        color = VoidPurple.copy(alpha = 0.7f),
        letterSpacing = 1.5.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(VoidPurple.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
