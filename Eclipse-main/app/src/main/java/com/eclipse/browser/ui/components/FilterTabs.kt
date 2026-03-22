package com.eclipse.browser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*

// Section 30.12: Minimal line icons — not cartoonish, not emojis
// All tab → globe line icon
// Images → grid line icon
// Videos → play circle line icon
// News → feed line icon
// Section 2: AI tab REMOVED from here — Ask AI is in top bar
data class FilterTab(val id: String, val icon: String, val label: String)

val defaultTabs = listOf(
    FilterTab("all",    "⊕",  "All"),       // globe-like minimal icon
    FilterTab("news",   "≡",  "News"),      // feed/lines icon
    FilterTab("images", "⊞",  "Images"),    // grid icon
    FilterTab("videos", "▷",  "Videos"),    // play icon
    // AI tab REMOVED — Ask AI moved to top bar per Section 2
)

// Incognito tabs — same but no AI
val incognitoTabs = listOf(
    FilterTab("all",    "⊕",  "All"),
    FilterTab("news",   "≡",  "News"),
    FilterTab("images", "⊞",  "Images"),
    FilterTab("videos", "▷",  "Videos"),
)

@Composable
fun FilterTabs(
    activeTab: String,
    accentColor: Color,
    isIncognito: Boolean = false,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabColor = if (isIncognito) VoidPurple else accentColor
    val tabs = if (isIncognito) incognitoTabs else defaultTabs

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val isActive = tab.id == activeTab

            val bgColor by animateColorAsState(
                targetValue = if (isActive) tabColor.copy(alpha = 0.14f) else Color.Transparent,
                animationSpec = tween(250),
                label = "tabBg_${tab.id}"
            )
            val textColor by animateColorAsState(
                targetValue = if (isActive) tabColor else TextMuted,
                animationSpec = tween(250),
                label = "tabText_${tab.id}"
            )
            val borderCol by animateColorAsState(
                targetValue = if (isActive) tabColor.copy(alpha = 0.28f) else Color.Transparent,
                animationSpec = tween(250),
                label = "tabBorder_${tab.id}"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .clickable { onTabSelected(tab.id) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${tab.icon} ${tab.label}",
                    color = textColor,
                    fontSize = 12.sp,
                    fontFamily = Outfit
                )
            }
        }
    }
}
