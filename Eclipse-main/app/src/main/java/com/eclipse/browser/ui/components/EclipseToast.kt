package com.eclipse.browser.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*

/**
 * Section 23: Toast / Notification
 *
 * Must appear:
 * - Bottom CENTER of screen (not top left — that was the bug)
 *
 * Animation:
 * 1. Slide up from bottom
 * 2. Stay visible for short duration
 * 3. Fade away smoothly
 *
 * Must be smooth and noticeable. Not abrupt.
 */
@Composable
fun EclipseToast(
    message: String?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // Section 23: Bottom center placement
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 96.dp), // above bottom nav
        contentAlignment = Alignment.BottomCenter
    ) {
        // Section 23: Slide up from bottom + fade away
        AnimatedVisibility(
            visible = !message.isNullOrBlank(),
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = 400f
                ),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(300, easing = FastOutLinearInEasing),
                targetOffsetY = { it / 2 }
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF1A1A2E))
                    .border(
                        1.dp,
                        accentColor.copy(alpha = 0.25f),
                        RoundedCornerShape(100.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message ?: "",
                    fontFamily = Outfit,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
