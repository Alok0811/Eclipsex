package com.eclipse.browser.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eclipse.browser.ui.theme.*
import com.eclipse.browser.ui.viewmodel.QuickSite

@Composable
fun QuickSitesGrid(
    sites: List<QuickSite>,
    accentColor: Color,
    onSiteClick: (String) -> Unit,
    onSiteLongPress: (Int) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUICK ACCESS",
                style = LabelText,
                color = TextMuted2,
            )
            Text(
                text = "hold to remove",
                color = TextMuted2,
                fontSize = 10.sp,
                fontFamily = SpaceMono
            )
        }

        Spacer(Modifier.height(12.dp))

        // Grid: 4 columns
        val totalItems = sites.size + 1 // +1 for add button
        val rows = (totalItems + 3) / 4
        val gridHeight = (rows * 92).dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(sites) { index, site ->
                SiteItem(
                    site = site,
                    onClick = { onSiteClick(site.url) },
                    onLongPress = { onSiteLongPress(index) }
                )
            }
            item {
                AddSiteItem(accentColor = accentColor, onClick = onAddClick)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SiteItem(
    site: QuickSite,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val domain = try {
        java.net.URI(site.url).host ?: ""
    } catch (_: Exception) { "" }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(EclipseSurface)
                .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://www.google.com/s2/favicons?domain=$domain&sz=128")
                    .crossfade(true)
                    .build(),
                contentDescription = site.label,
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = site.label,
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = Outfit,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
private fun AddSiteItem(
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 1.5.dp,
                    color = accentColor.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "＋",
                color = accentColor.copy(alpha = 0.7f),
                fontSize = 20.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Add",
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = Outfit
        )
    }
}
