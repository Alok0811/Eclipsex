package com.eclipse.browser.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eclipse.browser.ui.components.*
import com.eclipse.browser.ui.theme.*
import com.eclipse.browser.ui.viewmodel.EclipseUiState
import com.eclipse.browser.ui.viewmodel.WeatherData

@Composable
fun HomeScreen(
    state: EclipseUiState,
    onSearch: (String) -> Unit,
    onTabSelected: (String) -> Unit,
    onSiteClick: (String) -> Unit,
    onSiteLongPress: (Int) -> Unit,
    onAddSiteClick: () -> Unit,
    onOpenAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Section 38.8: LazyColumn for proper landscape scrolling
    val scrollState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {

        // Custom background — image or video
        if (state.customBgType == "image" && state.customBgPath.isNotBlank()) {
            AsyncImage(
                model = state.customBgPath,
                contentDescription = "Custom background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (state.customBgType == "video" && state.customBgPath.isNotBlank()) {
            VideoBackground(
                videoPath = state.customBgPath,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Background layers
        if (state.starsOn) {
            StarFieldCanvas(
                isNight = state.isNight,
                bgTheme = state.bgTheme
            )
        }
        if (state.particlesOn) {
            ParticleSystem(accentColor = state.accentColor)
        }

        // Section 25.5: Full-screen weather animations (rain, snow, etc.)
        // These layer on TOP of the background but BELOW UI elements
        if (state.weatherOn) {
            WeatherAnimationLayer(
                weatherIcon = state.weather.icon,
                isNight = state.isNight
            )
        }

        // Section 38.8: LazyColumn — properly fills size, scrolls in landscape
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {

            // ── TOP BAR ── (item 0)
            item {
                EclipseEnterAnimation(index = 0) {
                    TopBar(
                        adBlockOn = state.adBlockOn,
                        adsBlocked = state.adsBlocked,
                        accentColor = state.accentColor,
                        onOpenAi = onOpenAi
                    )
                }
            }

            // ── HERO BLOCK ── (item 1)
            // Section 1: Date+Time on same line, then Greeting, then Search bar — no extra gaps
            // Section 25: Weather replaces orb on right side
            item {
                EclipseEnterAnimation(index = 1) {
                    HeroBlock(
                        greeting = state.greeting,
                        greetingWord = state.greetingWord,
                        date = state.date,
                        clock = state.clock,
                        isNight = state.isNight,
                        accentColor = state.accentColor,
                        weather = state.weather,
                        weatherOn = state.weatherOn,
                        clockOn = state.clockOn
                    )
                }
            }

            // ── SEARCH BAR ── (item 2)
            // Section 1: No extra space above — directly follows greeting
            item {
                EclipseEnterAnimation(index = 2) {
                    SearchBar(
                        accentColor = state.accentColor,
                        onSearch = onSearch,
                        modifier = Modifier.padding(top = 0.dp) // Section 1: no additional top padding
                    )
                }
            }

            // ── SPACER — pushes quick access to bottom area ──
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }

            // ── QUICK SITES ── (item 4)
            if (state.quickSitesOn) {
                item {
                    EclipseEnterAnimation(index = 4) {
                        QuickSitesGrid(
                            sites = state.quickSites,
                            accentColor = state.accentColor,
                            onSiteClick = onSiteClick,
                            onSiteLongPress = onSiteLongPress,
                            onAddClick = onAddSiteClick
                        )
                    }
                }
            }
        }
    }
}

// ── TOP BAR ──
// Section 2: Ask AI placed in center, shield text stays (icon replaced with blinking dot)
// Section 19 from conversation: shield icon removed, blinking accent dot in its place
@Composable
private fun TopBar(
    adBlockOn: Boolean,
    adsBlocked: Int,
    accentColor: Color,
    onOpenAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand text — left
        Text(
            text = "ECLIPSE",
            style = BrandText,
            color = accentColor
        )

        // Section 2: Ask AI — center of top bar
        Text(
            text = "◐  Ask AI",
            fontFamily = SpaceMono,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            color = accentColor,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(accentColor.copy(alpha = 0.09f))
                .border(
                    1.dp,
                    accentColor.copy(alpha = 0.18f),
                    RoundedCornerShape(20.dp)
                )
                .clickable { onOpenAi() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )

        // Right side: blinking dot (replaces shield icon) + shield text
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Section conversation: blinking accent dot replaces shield icon
            val pulseAlpha by rememberInfiniteTransition(label = "shieldDot").animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accentColor.copy(alpha = pulseAlpha))
            )
            Spacer(Modifier.width(5.dp))
            // User requirement: Shield text only shows ON/OFF, no extra info. Dynamic color based on accentColor.
            Text(
                text = if (adBlockOn) "Shield ON" else "Shield OFF",
                color = accentColor,
                fontSize = 10.sp,
                fontFamily = SpaceMono,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── HERO BLOCK ──
// Section 1: Date + Time on SAME line, then Greeting, no extra spacing between them
// Section 25: Weather canvas replaces the orb on the right side
@Composable
private fun HeroBlock(
    greeting: String,
    greetingWord: String,
    date: String,
    clock: String,
    isNight: Boolean,
    accentColor: Color,
    weather: WeatherData,
    weatherOn: Boolean,
    clockOn: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp), // Section 1: reduced bottom padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {

            // Section 1: Date + Time on SAME LINE
            // "FRIDAY • MAR 13   9:05 PM" — time is continuation of date row
            if (clockOn) {
                Text(
                    text = "$date   $clock", // date and clock same line, same style
                    style = DateLabel,       // same font, size, opacity for both
                    color = TextMuted2
                )
            } else {
                Text(
                    text = date.uppercase(),
                    style = DateLabel,
                    color = TextMuted2
                )
            }

            // User Requirement: Add a line gap between date/time and greeting section
            Spacer(modifier = Modifier.height(8.dp))

            // Section 1: Greeting directly below date
            val greetingText = if (isNight) {
                buildAnnotatedString {
                    append("Explore the ")
                    withStyle(SpanStyle(color = accentColor)) { append("Night") }
                }
            } else {
                buildAnnotatedString {
                    append("Good ")
                    withStyle(SpanStyle(color = accentColor)) { append(greetingWord) }
                    append(", Explorer")
                }
            }

            var greetingVisible by remember { mutableStateOf(false) }
            LaunchedEffect(greeting) {
                greetingVisible = false
                kotlinx.coroutines.delay(80)
                greetingVisible = true
            }
            val greetingAlpha by animateFloatAsState(
                targetValue = if (greetingVisible) 1f else 0f,
                animationSpec = tween(500),
                label = "greetAlpha"
            )
            val greetingOffset by animateFloatAsState(
                targetValue = if (greetingVisible) 0f else 10f,
                animationSpec = tween(500),
                label = "greetOffset"
            )
            Text(
                text = greetingText,
                style = GreetingMain,
                color = TextPrimary,
                modifier = Modifier.graphicsLayer {
                    alpha = greetingAlpha
                    translationY = greetingOffset
                }
            )

            // User Requirement: Remove "Find the unseen from night" tagline (NightTagline removed)
        }

        // Section 25.1/25.2: Orb REMOVED — Weather visualization takes its place
        // Section 25.3: Weather appears in the right-side area where orb was
        if (weatherOn) {
            WeatherOrb(
                weatherData = weather,
                accentColor = accentColor,
                isNight = isNight
            )
        }
    }
}

// ── WEATHER ORB — replaces Eclipse Orb ──
// Section 25.2: Realistic, premium, minimalist weather icon
// Placed exactly where orb was — right side of hero block
@Composable
private fun WeatherOrb(
    weatherData: WeatherData,
    accentColor: Color,
    isNight: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weatherOrb")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbY"
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .offset(y = floatY.dp),
        contentAlignment = Alignment.Center
    ) {
        WeatherCanvas(
            icon = weatherData.icon,
            condition = weatherData.condition,
            temp = weatherData.temp,
            isNight = isNight,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}
