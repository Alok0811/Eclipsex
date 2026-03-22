package com.eclipse.browser.ui.sheets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.components.EclipseEnterAnimation
import com.eclipse.browser.ui.theme.*
import com.eclipse.browser.ui.viewmodel.EclipseUiState
import com.eclipse.browser.ui.viewmodel.Screen

data class MenuItem(val icon: String, val label: String, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(
    state: EclipseUiState,
    onDismiss: () -> Unit,
    onOpenCustomize: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenExtensions: () -> Unit,
    onAddBookmark: () -> Unit,
    onIncognito: () -> Unit,
    onRefresh: () -> Unit,        // Section 20: Refresh from menu
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Stats row (ad block count)
            EclipseEnterAnimation(index = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    state.accentColor.copy(alpha = 0.1f),
                                    state.accentColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.dp, state.accentColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("🛡", "${state.adsBlocked}", "Ads Blocked")
                    StatItem("🔍", "${state.trackersBlocked}", "Trackers")
                    StatItem("◑", "${state.tabs.size}", "Open Tabs")
                }
            }

            Spacer(Modifier.height(16.dp))

            val menuItems = buildList {
                // Section 20: Refresh — only shown when browsing, not on home screen
                if (state.currentScreen == Screen.WEBVIEW) {
                    add(MenuItem("↺", "Refresh Page", onRefresh))
                }
                add(MenuItem("★", "Add Bookmark", onAddBookmark))
                add(MenuItem("🎨", "Customize", onOpenCustomize))
                add(MenuItem("🕒", "History", onOpenHistory))
                add(MenuItem("🔖", "Bookmarks", onOpenBookmarks))
                // Section 18: Extensions in menu
                add(MenuItem("🧩", "Extensions", onOpenExtensions))
                add(MenuItem("👁", "Void Mode", onIncognito))
                add(MenuItem("ℹ", "About Eclipse", onOpenAbout))
            }

            menuItems.forEachIndexed { index, item ->
                EclipseEnterAnimation(index = index + 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDismiss()
                                item.action()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(EclipseSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item.icon, fontSize = 17.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = item.label,
                            fontFamily = Outfit,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }
                }
                if (index < menuItems.size - 1) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun StatItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontFamily = SpaceMono,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontFamily = SpaceMono,
            fontSize = 8.sp,
            letterSpacing = 1.sp,
            color = TextMuted2
        )
    }
}
