package com.eclipse.browser.ui.sheets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eclipse.browser.ui.theme.*
import com.eclipse.browser.ui.viewmodel.BookmarkItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksSheet(
    bookmarks: List<BookmarkItem>,
    accentColor: Color,
    onItemClick: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
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
            Text(
                text = "BOOKMARKS",
                fontFamily = Outfit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = TextPrimary,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "★", fontSize = 28.sp, color = TextMuted2)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No bookmarks yet",
                            fontFamily = Outfit,
                            fontSize = 16.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Save pages to find them later",
                            fontFamily = SpaceMono,
                            fontSize = 11.sp,
                            color = TextMuted2
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(bookmarks) { _, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onItemClick(item.url)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Favicon
                            val domain = try {
                                java.net.URI(item.url).host ?: ""
                            } catch (_: Exception) { "" }
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data("https://www.google.com/s2/favicons?domain=$domain&sz=64")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 0.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title.ifBlank { item.url },
                                    fontFamily = Outfit,
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = try {
                                        java.net.URI(item.url).host?.replace("www.", "") ?: item.url
                                    } catch (_: Exception) { item.url },
                                    fontFamily = SpaceMono,
                                    fontSize = 10.sp,
                                    color = TextMuted2,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "✕",
                                fontSize = 12.sp,
                                color = TextMuted2,
                                modifier = Modifier
                                    .clickable { onDeleteItem(item.url) }
                                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
