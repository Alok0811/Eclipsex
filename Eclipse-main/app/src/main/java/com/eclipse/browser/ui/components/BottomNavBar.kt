package com.eclipse.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*

@Composable
fun BottomNavBar(
    accentColor: Color,
    tabCount: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isIncognito: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BottomBarBg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back
        NavButton(
            text = "‹",
            fontSize = 22,
            enabled = canGoBack,
            onClick = onBack
        )

        // Forward
        NavButton(
            text = "›",
            fontSize = 22,
            enabled = canGoForward,
            onClick = onForward
        )

        // Section 3: Saturn-style home icon
        // Circle (planet) with ring around it — both change color with accent
        SaturnHomeButton(
            accentColor = if (isIncognito) VoidPurple else accentColor,
            onClick = onHome
        )

        // Tabs with badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable { onTabs() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(1.5.dp, TextMuted, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabCount.toString(),
                    fontFamily = SpaceMono,
                    fontSize = 10.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Menu
        NavButton(
            text = "⋯",
            fontSize = 20,
            enabled = true,
            onClick = onMenu
        )
    }
}

// Section 3: Saturn design — circle (planet) + ring around it
// Both circle and ring change color with accent theme
@Composable
private fun SaturnHomeButton(
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Planet core
            drawCircle(
                color = accentColor.copy(alpha = 0.15f),
                radius = size.minDimension * 0.28f,
                center = Offset(cx, cy)
            )
            // Planet dot (filled center)
            drawCircle(
                color = accentColor,
                radius = size.minDimension * 0.14f,
                center = Offset(cx, cy)
            )

            // Saturn ring — tilted ellipse drawn as two arcs
            // Ring passes behind planet on bottom, in front on top
            val ringWidth = size.minDimension * 0.44f
            val ringHeight = size.minDimension * 0.18f

            // Back arc (behind planet) — drawn first, muted
            drawOval(
                color = accentColor.copy(alpha = 0.3f),
                topLeft = Offset(cx - ringWidth, cy - ringHeight),
                size = androidx.compose.ui.geometry.Size(ringWidth * 2f, ringHeight * 2f),
                style = Stroke(width = 2.2f)
            )

            // Front arc (top half, over planet)
            // Clip top half by drawing from left to right on top
            drawArc(
                color = accentColor.copy(alpha = 0.9f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - ringWidth, cy - ringHeight),
                size = androidx.compose.ui.geometry.Size(ringWidth * 2f, ringHeight * 2f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun NavButton(
    text: String,
    fontSize: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (enabled) TextPrimary else TextMuted2,
        animationSpec = spring(),
        label = "navColor"
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            color = color,
            fontWeight = FontWeight.Light
        )
    }
}
