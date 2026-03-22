package com.eclipse.browser.ui.sheets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*
import com.eclipse.browser.ui.viewmodel.TabInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsSheet(
    tabs: List<TabInfo>,
    activeTabId: Int,
    accentColor: Color,
    onTabClick: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A0A14),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(EclipseBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TABS",
                    fontFamily = Outfit,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                    letterSpacing = 3.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${tabs.size}",
                        fontFamily = SpaceMono,
                        fontSize = 12.sp,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⊕ Void",
                        fontFamily = SpaceMono,
                        fontSize = 11.sp,
                        color = VoidPurple,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(VoidPurple.copy(alpha = 0.1f))
                            .clickable {
                                onNewIncognitoTab()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ NEW",
                        fontFamily = SpaceMono,
                        fontSize = 11.sp,
                        color = accentColor,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.1f))
                            .clickable {
                                onNewTab()
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(tabs) { _, tab ->
                    val isActive = tab.id == activeTabId
                    val isIncognito = tab.incognito
                    val borderColor = when {
                        isActive -> accentColor
                        isIncognito -> VoidPurple.copy(alpha = 0.4f)
                        else -> EclipseBorder
                    }
                    val bgColor = when {
                        isActive -> accentColor.copy(alpha = 0.08f)
                        isIncognito -> VoidPurple.copy(alpha = 0.05f)
                        else -> EclipseSurface
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable {
                                onTabClick(tab.id)
                                onDismiss()
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = tab.title.ifBlank { "New Tab" },
                                    fontFamily = Outfit,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isIncognito) VoidPurpleLight else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "✕",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    modifier = Modifier
                                        .clickable { onCloseTab(tab.id) }
                                        .padding(start = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (tab.url == "home") "Home" else try {
                                    java.net.URI(tab.url).host?.replace("www.", "") ?: tab.url.take(25)
                                } catch (_: Exception) { tab.url.take(25) },
                                fontFamily = SpaceMono,
                                fontSize = 10.sp,
                                color = TextMuted2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isIncognito) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "VOID",
                                    fontFamily = SpaceMono,
                                    fontSize = 8.sp,
                                    color = VoidPurple,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Done button
            Text(
                text = "Done",
                fontFamily = Outfit,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .clickable { onDismiss() }
                    .padding(vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
