package com.eclipse.browser.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.eclipse.browser.EclipseConfig
import com.eclipse.browser.ui.components.EclipseEnterAnimation
import com.eclipse.browser.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

// ── DATA MODEL ──
// Section 31.11: imageBase64 added for image generation display
data class AiMessage(
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentType: String? = null,
    val imageBase64: String? = null  // for generated/analyzed images
)

@Composable
fun AiChatScreen(
    messages: List<AiMessage>,
    isLoading: Boolean,
    accentColor: Color,
    remainingMessages: Int,
    responseMode: String,
    onSendMessage: (String) -> Unit,
    onSendImageMessage: (String, String) -> Unit,
    onSendFileMessage: (String, String, String) -> Unit,
    onReloadMessage: () -> Unit,
    onResponseModeChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var inputText by remember { mutableStateOf("") }
    var pendingImageBase64 by remember { mutableStateOf<String?>(null) }
    var pendingAttachmentName by remember { mutableStateOf<String?>(null) }
    var pendingDocText by remember { mutableStateOf<String?>(null) }
    var pendingDocFileName by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Camera URI
    val imageUri = remember {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "eclipse_ai_photo.jpg")
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            scope.launch {
                val base64 = withContext(Dispatchers.IO) { uriToBase64(context, imageUri) }
                if (base64 != null) {
                    pendingImageBase64 = base64
                    pendingAttachmentName = "Photo"
                    pendingDocText = null
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) cameraLauncher.launch(imageUri) }

    // File picker — images and documents
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pickedUri ->
            scope.launch {
                val mime = context.contentResolver.getType(pickedUri) ?: ""
                when {
                    mime.startsWith("image/") -> {
                        val base64 = withContext(Dispatchers.IO) { uriToBase64(context, pickedUri) }
                        if (base64 != null) {
                            pendingImageBase64 = base64
                            pendingAttachmentName = "Image"
                            pendingDocText = null
                            pendingDocFileName = null
                        }
                    }
                    else -> {
                        // Section 31.10: Extract text from document
                        val fileName = getFileName(context, pickedUri)
                        val text = withContext(Dispatchers.IO) {
                            extractDocumentText(context, pickedUri, mime)
                        }
                        if (!text.isNullOrBlank()) {
                            pendingDocText = text
                            pendingDocFileName = fileName
                            pendingAttachmentName = fileName
                            pendingImageBase64 = null
                        }
                    }
                }
            }
        }
    }

    fun clearAttachment() {
        pendingImageBase64 = null
        pendingDocText = null
        pendingAttachmentName = null
        pendingDocFileName = null
    }

    fun handleSend() {
        val text = inputText.trim()
        if (text.isEmpty() && pendingImageBase64 == null && pendingDocText == null) return
        if (isLoading) return

        when {
            pendingImageBase64 != null -> {
                // Section 31.9: image message → /vision endpoint
                onSendImageMessage(text.ifEmpty { "What's in this image?" }, pendingImageBase64!!)
            }
            pendingDocText != null -> {
                // Section 31.10: file message → /files endpoint
                onSendFileMessage(
                    text.ifEmpty { "Please analyze this document." },
                    pendingDocText!!,
                    pendingDocFileName ?: "document"
                )
            }
            else -> onSendMessage(text)
        }
        inputText = ""
        clearAttachment()
        focusManager.clearFocus()
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Section 28: Keyboard fix — use imePadding to push content above keyboard
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding() // Section 28: critical — moves input above keyboard
    ) {

        // ── HEADER ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    // Section 13: Glassmorphism header
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF08080F),
                            Color(0xFF08080F).copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(EclipseSurface)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = TextPrimary, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Eclipse AI logo + title
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "◐",
                        fontSize = 16.sp,
                        color = accentColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ECLIPSE AI",
                        fontFamily = SpaceMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "Powered by Eclipse Backend",
                    fontFamily = Outfit,
                    fontSize = 10.sp,
                    color = TextMuted2
                )
            }

            // Section 31.12: remaining messages warning
            if (remainingMessages <= EclipseConfig.WARN_MESSAGES_THRESHOLD) {
                Text(
                    text = "$remainingMessages left",
                    fontFamily = SpaceMono,
                    fontSize = 9.sp,
                    color = if (remainingMessages <= EclipseConfig.DANGER_MESSAGES_THRESHOLD)
                        Color(0xFFFF4444) else accentColor.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }

            // Pulsing AI live dot
            val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "p"
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = pulseAlpha))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EclipseBorder)
        )

        // ── MESSAGES ──
        if (messages.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(40.dp)
                ) {
                    val glowAlpha by rememberInfiniteTransition(label = "orbGlow").animateFloat(
                        initialValue = 0.4f, targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "g"
                    )
                    Text(
                        text = "◐",
                        fontSize = 56.sp,
                        color = accentColor.copy(alpha = glowAlpha)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Eclipse AI",
                        fontFamily = Outfit,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Ask anything, generate images,\nanalyze photos or documents.",
                        fontFamily = Outfit,
                        fontSize = 14.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(24.dp))
                    // Quick suggestions
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        listOf(
                            "Write a short poem about space 🌑",
                            "Explain quantum physics simply",
                            "Generate an image of a nebula"
                        ).forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(EclipseSurface)
                                    .border(1.dp, EclipseBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        inputText = suggestion
                                        handleSend()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    fontFamily = Outfit,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(
                        message = message,
                        accentColor = accentColor,
                        onCopy = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Eclipse AI", text))
                        },
                        onReload = onReloadMessage,
                        isLastAiMessage = messages.lastOrNull { it.role == "assistant" } == message
                    )
                }
                if (isLoading) {
                    item { TypingIndicator(accentColor) }
                }
            }
        }

        // ── INPUT AREA ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF08080F))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EclipseBorder)
            )

            // Section 8: Response mode selector — above input, minimal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode:",
                    fontFamily = SpaceMono,
                    fontSize = 9.sp,
                    color = TextMuted2,
                    letterSpacing = 1.sp
                )
                listOf(
                    EclipseConfig.MODE_SHORT to "Short",
                    EclipseConfig.MODE_DETAILED to "Detailed",
                    EclipseConfig.MODE_PRECISE to "Precise"
                ).forEach { (mode, label) ->
                    val isActive = responseMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isActive) accentColor.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isActive) accentColor.copy(alpha = 0.4f) else EclipseBorder,
                                RoundedCornerShape(100.dp)
                            )
                            .clickable { onResponseModeChange(mode) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontFamily = SpaceMono,
                            fontSize = 8.sp,
                            letterSpacing = 1.sp,
                            color = if (isActive) accentColor else TextMuted2
                        )
                    }
                }
            }

            // Pending attachment preview
            if (pendingAttachmentName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pendingImageBase64 != null) "📷" else "📄",
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pendingAttachmentName ?: "",
                        fontFamily = Outfit,
                        fontSize = 12.sp,
                        color = accentColor,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "✕",
                        color = TextMuted,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable { clearAttachment() }
                            .padding(4.dp)
                    )
                }
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Attach button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EclipseSurface)
                        .clickable {
                            if (ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                cameraLauncher.launch(imageUri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷", fontSize = 16.sp)
                }

                // File picker button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EclipseSurface)
                        .clickable { fileLauncher.launch("*/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📎", fontSize = 16.sp)
                }

                // Text input
                // Section 28: no fixed height — grows with content
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Message Eclipse AI...",
                            fontFamily = Outfit,
                            fontSize = 14.sp,
                            color = TextMuted2
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { handleSend() }),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = EclipseBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = accentColor,
                        focusedContainerColor = EclipseSurface,
                        unfocusedContainerColor = EclipseSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = Outfit,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                )

                // Send button
                val canSend = (inputText.isNotBlank() || pendingImageBase64 != null || pendingDocText != null) && !isLoading
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) accentColor
                            else accentColor.copy(alpha = 0.2f)
                        )
                        .clickable(enabled = canSend) { handleSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↑",
                        fontSize = 20.sp,
                        color = if (canSend) Color.Black else TextMuted2,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── CHAT BUBBLE ──
// Section 9: Modern space-themed bubble design
// Section 10: Copy and Reload actions below AI messages
@Composable
private fun ChatBubble(
    message: AiMessage,
    accentColor: Color,
    onCopy: (String) -> Unit,
    onReload: () -> Unit,
    isLastAiMessage: Boolean
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Sender label
        Text(
            text = if (isUser) "YOU" else "ECLIPSE AI",
            fontFamily = SpaceMono,
            fontSize = 9.sp,
            color = if (isUser) TextMuted2 else accentColor.copy(alpha = 0.7f),
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        )

        // Section 31.11: Show generated image if present
        if (message.attachmentType == "generated_image" && message.imageBase64 != null) {
            GeneratedImageBubble(
                base64 = message.imageBase64,
                accentColor = accentColor
            )
        } else {
            // Text bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isUser) accentColor.copy(alpha = 0.14f)
                        else Color(0xFF0C0C1A)
                    )
                    .border(
                        1.dp,
                        if (isUser) accentColor.copy(alpha = 0.22f)
                        else EclipseBorder,
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    if (message.attachmentType != null && message.attachmentType != "generated_image") {
                        Text(
                            text = when (message.attachmentType) {
                                "image" -> "📷 Photo attached"
                                "document" -> "📄 Document attached"
                                else -> "📎 File attached"
                            },
                            fontFamily = SpaceMono,
                            fontSize = 9.sp,
                            color = accentColor.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Text(
                        text = message.content,
                        fontFamily = Outfit,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Section 10: Copy and Reload buttons — only for AI messages, minimal design
        // No boxes, no containers, small text buttons only
        if (!isUser && message.attachmentType != "generated_image") {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Copy",
                    fontFamily = SpaceMono,
                    fontSize = 9.sp,
                    color = TextMuted2,
                    letterSpacing = 1.sp,
                    modifier = Modifier.clickable { onCopy(message.content) }
                )
                if (isLastAiMessage) {
                    Text(
                        text = "Reload",
                        fontFamily = SpaceMono,
                        fontSize = 9.sp,
                        color = TextMuted2,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable { onReload() }
                    )
                }
            }
        }
    }
}

// Section 31.11: Display generated image in chat
@Composable
private fun GeneratedImageBubble(base64: String, accentColor: Color) {
    val bitmap = remember(base64) {
        try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }

    Box(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated image",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(EclipseSurface),
                contentAlignment = Alignment.Center
            ) {
                Text("🖼", fontSize = 40.sp)
            }
        }
    }
}

// ── TYPING INDICATOR ──
@Composable
private fun TypingIndicator(accentColor: Color) {
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ), label = "d1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(200)
        ), label = "d2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(400)
        ), label = "d3"
    )

    Row(
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ECLIPSE AI",
            fontFamily = SpaceMono,
            fontSize = 9.sp,
            color = accentColor.copy(alpha = 0.6f),
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.width(8.dp))
        listOf(dot1, dot2, dot3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = alpha))
            )
        }
    }
}

// ── HELPER FUNCTIONS ──

private fun uriToBase64(context: Context, uri: Uri, maxSize: Int = 1024): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null
        val bitmap = if (original.width > maxSize || original.height > maxSize) {
            val ratio = minOf(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height)
            android.graphics.Bitmap.createScaledBitmap(
                original,
                (original.width * ratio).toInt(),
                (original.height * ratio).toInt(),
                true
            )
        } else original
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    } catch (_: Exception) { null }
}

// Section 31.10: Extract text from document
private fun extractDocumentText(context: Context, uri: Uri, mime: String): String? {
    return try {
        when {
            mime == "application/pdf" -> {
                // Use Android PDF renderer for PDFs
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
                val renderer = android.graphics.pdf.PdfRenderer(pfd)
                val text = StringBuilder()
                for (i in 0 until minOf(renderer.pageCount, 10)) {
                    // We can't extract text directly from PdfRenderer — just note the page count
                    // Real text extraction requires a PDF library
                }
                renderer.close()
                pfd.close()
                "PDF Document (${renderer.pageCount} pages) — text extraction requires parsing."
            }
            mime.startsWith("text/") -> {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText()?.take(8000)
            }
            else -> {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText()?.take(8000)
            }
        }
    } catch (_: Exception) { null }
}

private fun getFileName(context: Context, uri: Uri): String {
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else "document"
            } else "document"
        } ?: "document"
    } catch (_: Exception) { "document" }
}
