package com.eclipse.browser.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String
)

private val pages = listOf(
    OnboardingPage(
        emoji = "◐",
        title = "Welcome to Eclipse",
        description = "A space-inspired browser built with obsession.\nPrivacy-first. Beautiful. Yours."
    ),
    OnboardingPage(
        emoji = "🛡",
        title = "Eclipse Shield",
        description = "Built-in ad and tracker blocker.\nNo extensions needed. Browse faster and safer."
    ),
    OnboardingPage(
        emoji = "🎨",
        title = "Deep Customization",
        description = "6 accent colors, 4 backgrounds,\nparticle effects, and toggle every widget on or off."
    ),
    OnboardingPage(
        emoji = "👁",
        title = "Void Mode",
        description = "Private browsing, but dramatic.\nNo history. No traces. You are invisible."
    ),
    OnboardingPage(
        emoji = "🌌",
        title = "You're Ready",
        description = "Tap below to enter the Eclipse.\nMake it yours."
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    accentColor: Color,
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val page = pages[pageIndex]

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    Text(
                        text = page.emoji,
                        fontSize = 56.sp,
                        modifier = Modifier.graphicsLayer {
                            val offset = pagerState.currentPageOffsetFraction
                            alpha = 1f - kotlin.math.abs(offset).coerceAtMost(1f)
                            scaleX = 1f - kotlin.math.abs(offset) * 0.3f
                            scaleY = 1f - kotlin.math.abs(offset) * 0.3f
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = page.title,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = page.description,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.Light,
                        fontSize = 15.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == pagerState.currentPage) accentColor
                                else TextMuted2
                            )
                    )
                }
            }

            // Button
            Text(
                text = if (isLastPage) "ENTER ECLIPSE" else "NEXT",
                fontFamily = SpaceMono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLastPage) Color.Black else accentColor,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .then(
                        if (isLastPage)
                            Modifier.background(accentColor)
                        else
                            Modifier
                                .background(accentColor.copy(alpha = 0.1f))
                                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(50.dp))
                    )
                    .clickable {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                    .padding(horizontal = 40.dp, vertical = 14.dp)
            )

            // Skip
            if (!isLastPage) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "SKIP",
                    fontFamily = SpaceMono,
                    fontSize = 10.sp,
                    color = TextMuted2,
                    letterSpacing = 2.sp,
                    modifier = Modifier.clickable { onFinish() }
                )
            }
        }
    }
}
