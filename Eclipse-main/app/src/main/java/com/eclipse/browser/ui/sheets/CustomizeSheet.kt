package com.eclipse.browser.ui.sheets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.components.EclipseEnterAnimation
import com.eclipse.browser.ui.theme.*

// Section 30.1: Search engine selector COMPLETELY REMOVED — no placeholder, no empty space
// Section 25.8: Compact/Expanded mode REMOVED — no placeholder, no empty space
// Section 17: Custom background color does not affect bg when custom image is active

data class AccentPreset(val hex: String, val lightHex: String, val name: String)

val accentPresets = listOf(
    AccentPreset("#FF6B1A", "#FFB347", "Ember"),
    AccentPreset("#6C63FF", "#9D99FF", "Void"),
    AccentPreset("#00D4AA", "#5FFFD9", "Aurora"),
    AccentPreset("#FF3D9A", "#FF85C2", "Nova"),
    AccentPreset("#FFD700", "#FFE966", "Gold"),
    AccentPreset("#00CFFF", "#7FE8FF", "Ice"),
    AccentPreset("#E8C840", "#FFE878", "Stardust")
)

data class BgTheme(val key: String, val name: String, val drawableRes: Int)

// Note: drawableRes values match bg_eclipse, bg_nebula etc. in res/drawable
val bgThemes = listOf(
    BgTheme("eclipse",   "Eclipse",   0),
    BgTheme("nebula",    "Nebula",    0),
    BgTheme("aurora",    "Aurora",    0),
    BgTheme("milkyway",  "Milky Way", 0),
    BgTheme("blackhole", "Blackhole", 0),
    BgTheme("cosmos",    "Cosmos",    0),
    BgTheme("galaxy",    "Galaxy",    0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSheet(
    accentHex: String,
    accentLightHex: String,
    bgTheme: String,
    particlesOn: Boolean,
    starsOn: Boolean,
    weatherOn: Boolean,
    clockOn: Boolean,
    quickSitesOn: Boolean,
    adBlockOn: Boolean,
    aiEnabled: Boolean,
    customBgActive: Boolean, // Section 17
    onAccentChange: (String, String) -> Unit,
    onBgThemeChange: (String) -> Unit,
    onParticlesToggle: (Boolean) -> Unit,
    onStarsToggle: (Boolean) -> Unit,
    onWeatherToggle: (Boolean) -> Unit,
    onClockToggle: (Boolean) -> Unit,
    onQuickSitesToggle: (Boolean) -> Unit,
    onAdBlockToggle: (Boolean) -> Unit,
    onAiToggle: (Boolean) -> Unit,
    onOpenBackgroundPicker: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        dragHandle = {
            Box(
                modifier = Modifier.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp).height(4.dp)
                        .clip(CircleShape)
                        .background(EclipseBorder)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {

            Text(
                text = "CUSTOMIZE",
                fontFamily = SpaceMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 4.sp,
                color = TextPrimary
            )
            Spacer(Modifier.height(20.dp))

            // ── ACCENT COLOR ──
            // Section 30.1: NO search engine selector here — REMOVED
            // Section 25.8: NO compact/expanded — REMOVED
            EclipseEnterAnimation(index = 0) {
                SectionLabel("Accent Color")
            }
            Spacer(Modifier.height(10.dp))
            EclipseEnterAnimation(index = 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(accentPresets) { preset ->
                        val isActive = preset.hex.equals(accentHex, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onAccentChange(preset.hex, preset.lightHex) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 44.dp else 40.dp)
                                    .shadow(if (isActive) 8.dp else 2.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(preset.hex)))
                                    .then(
                                        if (isActive) Modifier.border(
                                            2.dp, Color.White.copy(alpha = 0.6f), CircleShape
                                        ) else Modifier
                                    )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = preset.name,
                                fontFamily = SpaceMono,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isActive) Color(android.graphics.Color.parseColor(preset.hex))
                                else TextMuted2
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            DividerLine()
            Spacer(Modifier.height(20.dp))

            // ── BACKGROUND THEME ──
            // Section 17: When custom bg is active, default themes are disabled
            EclipseEnterAnimation(index = 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Space Background")
                    if (customBgActive) {
                        Text(
                            text = "Custom BG Active",
                            fontFamily = SpaceMono,
                            fontSize = 8.sp,
                            color = TextMuted2,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            EclipseEnterAnimation(index = 3) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(bgThemes) { theme ->
                        val isActive = theme.key == bgTheme && !customBgActive
                        // Section 17: Disable default themes when custom bg active
                        val isDisabled = customBgActive
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.then(
                                if (!isDisabled) Modifier.clickable { onBgThemeChange(theme.key) }
                                else Modifier
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when (theme.key) {
                                            "eclipse"   -> Color(0xFF1a0800)
                                            "nebula"    -> Color(0xFF080028)
                                            "aurora"    -> Color(0xFF001208)
                                            "milkyway"  -> Color(0xFF0a0a1e)
                                            "blackhole" -> Color(0xFF0a001a)
                                            "cosmos"    -> Color(0xFF1a0010)
                                            "galaxy"    -> Color(0xFF001a08)
                                            else        -> Color(0xFF1a0800)
                                        }
                                    )
                                    .then(
                                        if (isActive) Modifier.border(
                                            2.dp,
                                            Color(android.graphics.Color.parseColor(accentHex)),
                                            RoundedCornerShape(12.dp)
                                        ) else Modifier.border(1.dp, EclipseBorder, RoundedCornerShape(12.dp))
                                    )
                                    .then(if (isDisabled) Modifier.then(Modifier) else Modifier)
                            ) {
                                if (isDisabled) {
                                    Box(
                                        modifier = Modifier.fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = theme.name,
                                fontFamily = SpaceMono,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isActive) Color(android.graphics.Color.parseColor(accentHex))
                                else if (isDisabled) TextMuted2.copy(alpha = 0.4f)
                                else TextMuted2
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Section 14: Custom background button
            EclipseEnterAnimation(index = 4) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EclipseSurface)
                        .border(
                            1.dp,
                            if (customBgActive) Color(android.graphics.Color.parseColor(accentHex)).copy(alpha = 0.4f)
                            else EclipseBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onOpenBackgroundPicker() }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (customBgActive) "✓ Custom Background Active" else "📷 Upload Custom Background",
                            fontFamily = Outfit,
                            fontSize = 13.sp,
                            color = if (customBgActive)
                                Color(android.graphics.Color.parseColor(accentHex))
                            else TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            DividerLine()
            Spacer(Modifier.height(20.dp))

            // ── TOGGLES ──
            // Section 25.8: Compact/Expanded REMOVED — no mention of it
            EclipseEnterAnimation(index = 5) { SectionLabel("App Features") }
            Spacer(Modifier.height(10.dp))

            val toggles = listOf(
                Triple("Star Field", starsOn, onStarsToggle),
                Triple("Particle System", particlesOn, onParticlesToggle),
                Triple("Weather Animation", weatherOn, onWeatherToggle),
                Triple("Clock & Date", clockOn, onClockToggle),
                Triple("Quick Sites", quickSitesOn, onQuickSitesToggle),
                Triple("Eclipse Shield (Ad Block)", adBlockOn, onAdBlockToggle),
                Triple("Eclipse AI", aiEnabled, onAiToggle),
            )

            toggles.forEachIndexed { index, (label, state, onToggle) ->
                EclipseEnterAnimation(index = index + 6) {
                    ToggleRow(
                        label = label,
                        isOn = state,
                        accentColor = Color(android.graphics.Color.parseColor(accentHex)),
                        onToggle = onToggle
                    )
                }
                if (index < toggles.size - 1) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontFamily = SpaceMono,
        fontSize = 9.sp,
        letterSpacing = 3.sp,
        color = TextMuted2
    )
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(EclipseBorder)
    )
}

// Section 13: Skeuomorphic toggle row
@Composable
private fun ToggleRow(
    label: String,
    isOn: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EclipseSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = Outfit,
            fontSize = 13.sp,
            color = TextPrimary
        )
        // Skeuomorphic toggle
        val thumbOffset by animateFloatAsState(
            targetValue = if (isOn) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
            label = "tglThumb"
        )
        val trackBg by animateColorAsState(
            targetValue = if (isOn) accentColor else Color(0xFF2A2A3A),
            animationSpec = tween(180), label = "tglBg"
        )
        Box(
            modifier = Modifier
                .width(44.dp).height(24.dp)
                .shadow(2.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(trackBg)
                .border(1.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .clickable { onToggle(!isOn) },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .offset(x = 20.dp * thumbOffset)
                    .size(18.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
