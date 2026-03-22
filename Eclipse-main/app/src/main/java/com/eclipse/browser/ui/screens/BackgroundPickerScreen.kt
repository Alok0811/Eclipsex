package com.eclipse.browser.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.eclipse.browser.ui.components.EclipseEnterAnimation
import com.eclipse.browser.ui.theme.*
import java.io.File

// Section 14: Dedicated separate screen for background customization

// Section 15: Simple image/video picker (no animation toggle)
// Section 16: Cropping UI with light overlay indicators
// Section 17: Custom bg disables default theme color on background
@Composable
fun BackgroundPickerScreen(
    currentBgType: String,  // "none", "preset", "image", "video"
    currentBgPath: String,
    accentColor: Color,
    onSave: (type: String, path: String, animate: Boolean) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var pickedMime by remember { mutableStateOf("") }
    var showCropPreview by remember { mutableStateOf(false) }

    // Section 15: Image picker — accepts images and videos
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pickedUri = it
            val mime = context.contentResolver.getType(it) ?: ""
            pickedMime = mime
            showCropPreview = true
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── HEADER ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF08080F))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape)
                    .background(EclipseSurface).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = TextPrimary, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Custom Background",
                    fontFamily = Outfit, fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp, color = TextPrimary
                )
                Text(
                    text = "Upload your own image or video",
                    fontFamily = SpaceMono, fontSize = 9.sp, color = TextMuted2, letterSpacing = 1.sp
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(EclipseBorder))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── CURRENT STATUS ──
            EclipseEnterAnimation(index = 0) {
                if (currentBgType == "image" || currentBgType == "video") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(EclipseSurface)
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    ) {
                        if (currentBgPath.isNotBlank()) {
                            AsyncImage(
                                model = currentBgPath,
                                contentDescription = "Current background",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // Section 17 notice
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓ Custom background active — theme color applies to text/icons only",
                                fontFamily = SpaceMono, fontSize = 8.sp,
                                color = accentColor, letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── UPLOAD BUTTON ──
            EclipseEnterAnimation(index = 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EclipseSurface)
                        .border(1.dp, EclipseBorder, RoundedCornerShape(16.dp))
                        .clickable { pickerLauncher.launch("*/*") }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Choose Image or Video",
                            fontFamily = Outfit, fontWeight = FontWeight.Medium,
                            fontSize = 15.sp, color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "JPG, PNG, WEBP, MP4, MKV supported",
                            fontFamily = Outfit, fontSize = 12.sp, color = TextMuted
                        )
                    }
                }
            }

            // ── PICKED IMAGE PREVIEW + CROP ──
            AnimatedVisibility(
                visible = showCropPreview && pickedUri != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // Section 16: Cropping UI with light overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = pickedUri,
                            contentDescription = "Background preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Section 16: Very faint top bar indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .align(Alignment.TopCenter)
                                .background(Color.Black.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "Top bar area",
                                fontFamily = SpaceMono, fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Section 16: Very faint bottom bar indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "Bottom nav area",
                                fontFamily = SpaceMono, fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Visible area label
                        Box(
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(
                                text = "Visible area",
                                fontFamily = SpaceMono, fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    // ── APPLY BUTTON ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(accentColor)
                            .clickable {
                                val type = if (pickedMime.startsWith("video/")) "video" else "image"
                                val animate = pickedMime.startsWith("video/")
                                
                                // Copy file to app's internal storage for persistence
                                val savedPath = pickedUri?.let { uri ->
                                    copyToInternalStorage(context, uri, type)
                                } ?: ""
                                
                                onSave(type, savedPath, animate)
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Apply Background",
                            fontFamily = SpaceMono, fontSize = 11.sp,
                            letterSpacing = 2.sp, color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── CLEAR BUTTON (if custom bg active) ──
            if (currentBgType == "image" || currentBgType == "video") {
                EclipseEnterAnimation(index = 3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(100.dp))
                            .background(EclipseSurface)
                            .border(1.dp, EclipseBorder, RoundedCornerShape(100.dp))
                            .clickable { onClear() }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Remove Custom Background",
                            fontFamily = SpaceMono, fontSize = 10.sp,
                            letterSpacing = 1.sp, color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

private fun copyToInternalStorage(context: android.content.Context, uri: Uri, type: String): String {
    return try {
        val bgDir = File(context.filesDir, "backgrounds")
        if (!bgDir.exists()) bgDir.mkdirs()
        
        // Clean up old backgrounds
        bgDir.listFiles()?.forEach { it.delete() }
        
        val extension = when (type) {
            "video" -> {
                val mime = context.contentResolver.getType(uri) ?: "video/mp4"
                when {
                    mime.contains("mp4") -> "mp4"
                    mime.contains("mkv") -> "mkv"
                    mime.contains("webm") -> "webm"
                    else -> "mp4"
                }
            }
            else -> {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                when {
                    mime.contains("png") -> "png"
                    mime.contains("webp") -> "webp"
                    else -> "jpg"
                }
            }
        }
        
        val fileName = if (type == "video") "background.$extension" else "background.$extension"
        val destFile = File(bgDir, fileName)
        
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        uri.toString() // Fallback to original URI
    }
}
