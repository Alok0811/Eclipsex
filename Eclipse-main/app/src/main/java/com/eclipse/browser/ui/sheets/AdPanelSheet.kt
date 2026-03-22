package com.eclipse.browser.ui.sheets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eclipse.browser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdPanelSheet(
    adBlockOn: Boolean,
    adsBlocked: Int,
    trackersBlocked: Int,
    accentColor: Color,
    onToggleAdBlock: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SHIELD STATS",
                fontFamily = Outfit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = TextPrimary,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Shield icon
            Text(
                text = "🛡",
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = if (adBlockOn) "ACTIVE" else "DISABLED",
                fontFamily = SpaceMono,
                fontSize = 12.sp,
                color = if (adBlockOn) accentColor else Color(0xFFFF4444),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBlock(
                    number = adsBlocked.toString(),
                    label = "ADS BLOCKED",
                    accentColor = accentColor
                )
                StatBlock(
                    number = trackersBlocked.toString(),
                    label = "TRACKERS",
                    accentColor = accentColor
                )
                StatBlock(
                    number = "${(adsBlocked * 1.2).toInt()}s",
                    label = "TIME SAVED",
                    accentColor = accentColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EclipseSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ad Shield",
                        fontFamily = Outfit,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Block ads and trackers",
                        fontFamily = SpaceMono,
                        fontSize = 10.sp,
                        color = TextMuted2
                    )
                }
                Switch(
                    checked = adBlockOn,
                    onCheckedChange = onToggleAdBlock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.25f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = EclipseSurface2
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Eclipse blocks ads at the network level\nfor faster, cleaner browsing.",
                fontFamily = Outfit,
                fontSize = 12.sp,
                color = TextMuted2,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatBlock(
    number: String,
    label: String,
    accentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(EclipseSurface)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = number,
            fontFamily = SpaceMono,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = SpaceMono,
            fontSize = 10.sp,
            color = TextMuted,
            letterSpacing = 1.5.sp
        )
    }
}
