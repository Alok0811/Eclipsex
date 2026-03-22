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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eclipse.browser.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    accentColor: Color,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("file:///android_asset/eclipse_logo.png")
                    .crossfade(true)
                    .build(),
                contentDescription = "Eclipse Logo",
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ECLIPSE",
                fontFamily = Outfit,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = TextPrimary,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "BROWSER",
                fontFamily = SpaceMono,
                fontSize = 12.sp,
                color = TextMuted,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Version badge
            Text(
                text = "UMBRA 1.0",
                fontFamily = SpaceMono,
                fontSize = 11.sp,
                color = accentColor,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.1f))
                    .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Description
            Text(
                text = "The browser that sees beyond.\nPrivacy-first, beautiful, and fast.",
                fontFamily = Outfit,
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Roadmap
            Text(
                text = "ROADMAP",
                fontFamily = SpaceMono,
                fontSize = 10.sp,
                color = TextMuted2,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 12.dp)
            )

            val roadmap = listOf(
                "Dawn 0.1" to "Core browser + home screen" to true,
                "Dawn 0.2" to "Real ad-block counting" to true,
                "Dawn 0.3" to "Geolocation weather" to true,
                "Dawn 0.4" to "Settings persistence + polish" to true,
                "Dawn 0.5" to "Widget toggles + onboarding" to true,
                "Umbra 1.0" to "Groq AI chat + Eclipse AI" to true,
                "Umbra 1.1" to "AI Answer Mode" to true,
                "Umbra 1.2" to "Custom search card UI" to true,
                "Umbra 1.3" to "Play Store ready" to false,
                "Penumbra 2.0" to "Drag-and-drop home layout" to false,
                "Penumbra 2.2" to "Community themes" to false,
                "Corona 3.0" to "Theme marketplace" to false,
                "Totality 4.0" to "Eclipse search engine" to false,
                "Sirius 5.0" to "Multi-platform + iOS" to false
            )

            roadmap.forEach { (pair, completed) ->
                val (version, description) = pair
                val isCurrent = version == "Umbra 1.0"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Timeline dot
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(6.dp)
                            .background(
                                when {
                                    isCurrent -> accentColor
                                    completed -> accentColor.copy(alpha = 0.4f)
                                    else -> TextMuted2
                                },
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = version + if (completed && !isCurrent) " ✓" else "",
                            fontFamily = SpaceMono,
                            fontSize = 11.sp,
                            color = when {
                                isCurrent -> accentColor
                                completed -> accentColor.copy(alpha = 0.6f)
                                else -> TextPrimary
                            },
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = description,
                            fontFamily = Outfit,
                            fontSize = 12.sp,
                            color = if (completed) TextMuted2.copy(alpha = 0.7f) else TextMuted2
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Share & GitHub
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "SHARE",
                    fontFamily = SpaceMono,
                    fontSize = 11.sp,
                    color = accentColor,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Check out Eclipse Browser — a space-inspired, privacy-first Android browser.\nhttps://github.com/TN-ARES0818/Eclipse")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Eclipse"))
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
                Text(
                    text = "GITHUB",
                    fontFamily = SpaceMono,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(EclipseSurface)
                        .border(1.dp, EclipseBorder, RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/TN-ARES0818/Eclipse"))
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Made with obsession",
                fontFamily = SpaceMono,
                fontSize = 10.sp,
                color = TextMuted2,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
