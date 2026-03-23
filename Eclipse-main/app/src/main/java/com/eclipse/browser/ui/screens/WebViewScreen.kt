package com.eclipse.browser.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.eclipse.browser.ui.theme.*
import kotlinx.coroutines.delay

// Extension model matching the one in StorageManager DEFAULT_EXTENSIONS
private data class InjectExtension(
    val id: String,
    val matchRules: List<String>,
    val scriptCode: String,
    val isEnabled: Boolean
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    accentColor: Color,
    adBlockOn: Boolean,
    refreshNonce: Int,
    extensions: List<com.eclipse.browser.ui.screens.Extension>,
    webViewAction: com.eclipse.browser.ui.viewmodel.WebViewAction,
    activeTabId: Int,
    onUrlChanged: (String, String) -> Unit,
    onAdBlocked: (String) -> Unit,
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onForward: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    onMinimizeVideo: () -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onCanGoForwardChanged: (Boolean) -> Unit,
    onMediaPlayingChanged: (Boolean) -> Unit,
    onMediaStopped: () -> Unit,
    onWebViewActionConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf(url) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var pullDistance by remember { mutableStateOf(0f) }
    var videoSwipeDistance by remember { mutableStateOf(0f) }
    var currentTabId by remember { mutableStateOf(activeTabId) }
    val pullThreshold = 120f
    val videoSwipeThreshold = 150f

    LaunchedEffect(refreshNonce) {
        if (refreshNonce > 0) {
            webView?.reload()
        }
    }

    // Feature 1: Handle webView navigation actions from ViewModel
    LaunchedEffect(webViewAction) {
        when (webViewAction) {
            com.eclipse.browser.ui.viewmodel.WebViewAction.GO_BACK -> {
                webView?.goBack()
                onWebViewActionConsumed()
            }
            com.eclipse.browser.ui.viewmodel.WebViewAction.GO_FORWARD -> {
                webView?.goForward()
                onWebViewActionConsumed()
            }
            com.eclipse.browser.ui.viewmodel.WebViewAction.NONE -> {}
        }
    }

    // Section 38.1: Fullscreen state tracking
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    val isFullscreen = fullscreenView != null

    // Feature 2: PiP (Picture-in-Picture) support for true mini player
    val isPipMode by remember { mutableStateOf(false) }
    
    // Track video element bounds for PiP
    var videoRect by remember { mutableStateOf<android.graphics.Rect?>(null) }
    
    // Video playback state (for non-fullscreen videos)
    var isVideoPlaying by remember { mutableStateOf(false) }

    // Feature 2: Mini player state
    var isMiniPlayerActive by remember { mutableStateOf(false) }
    var miniPlayerUrl by remember { mutableStateOf("") }

    // Feature 2: Track if we've already offered mini player for current video
    var miniPlayerOffered by remember { mutableStateOf(false) }

    // Section 38.10: Back handler — fullscreen exits first
    BackHandler(enabled = isFullscreen) {
        fullscreenCallback?.onCustomViewHidden()
        fullscreenView = null
        fullscreenCallback = null
        // Restore orientation
        (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // Restore bars
        restoreSystemBars(context)
        onExitFullscreen()
    }

    // Feature 1 & 2: Back handler with mini player support
    BackHandler(enabled = !isFullscreen && !isMiniPlayerActive) {
        val wv = webView
        // Feature 2: If video is playing and mini player not yet active, trigger mini player
        if (isVideoPlaying && !miniPlayerOffered) {
            miniPlayerOffered = true
            isMiniPlayerActive = true
            miniPlayerUrl = currentUrl
            // Inject script to notify webpage about mini player mode
            injectVideoControlScript(wv, true)
            onMinimizeVideo()
            return@BackHandler
        }
        // Feature 1: Use WebView history - go back one page at a time
        if (wv?.canGoBack() == true) {
            wv.goBack()
            miniPlayerOffered = false
        } else {
            onBack()
        }
    }

    // Handle back press when mini player is active
    BackHandler(enabled = isMiniPlayerActive) {
        isMiniPlayerActive = false
        webView?.goBack()
    }

    // Feature 2/3: Continuously check for video playback every 2 seconds
    LaunchedEffect(isLoading, currentUrl) {
        if (!isLoading && currentUrl.isNotBlank()) {
            while (true) {
                delay(2000)
                webView?.evaluateJavascript(
                    """
                    (function() {
                        var videos = document.querySelectorAll('video');
                        var playing = false;
                        videos.forEach(function(v) {
                            if (!v.paused && !v.ended && v.currentTime > 0) playing = true;
                        });
                        return playing;
                    })();
                    """.trimIndent()
                ) { result ->
                    val isPlaying = result == "true"
                    if (isPlaying != isVideoPlaying) {
                        isVideoPlaying = isPlaying
                        if (isPlaying) {
                            onMediaPlayingChanged(true)
                            miniPlayerOffered = false
                        } else {
                            onMediaStopped()
                        }
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ── WEBVIEW ──
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportMultipleWindows(true)
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false // Section 38: allow autoplay
                        builtInZoomControls = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        databaseEnabled = true
                        loadsImagesAutomatically = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    setBackgroundColor(android.graphics.Color.parseColor("#000009"))

                    // Section 38.3: WebChromeClient — fullscreen handling
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (newProgress == 100) isLoading = false
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            pageTitle = title ?: ""
                            onUrlChanged(view?.url ?: "", title ?: "")
                        }

                        // Section 38.3: onShowCustomView — video fullscreen
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            fullscreenView = view
                            fullscreenCallback = callback
                            // Section 38.5: Rotate to landscape
                            (ctx as? Activity)?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            // Section 38.6: Hide system bars
                            hideSystemBars(ctx)
                            onEnterFullscreen()
                        }

                        // Section 38.3: onHideCustomView — exit fullscreen
                        override fun onHideCustomView() {
                            fullscreenView = null
                            fullscreenCallback = null
                            // Section 38.5: Restore portrait
                            (ctx as? Activity)?.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            // Section 38.6: Restore bars
                            restoreSystemBars(ctx)
                            onExitFullscreen()
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean = false
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            currentUrl = url ?: ""
                            isVideoPlaying = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            val canBack = view?.canGoBack() ?: false
                            val canFwd = view?.canGoForward() ?: false
                            canGoBack = canBack
                            canGoForward = canFwd
                            onCanGoBackChanged(canBack)
                            onCanGoForwardChanged(canFwd)
                            currentUrl = url ?: ""
                            onUrlChanged(url ?: "", pageTitle)

                            // Section 18: Inject enabled extensions for this URL
                            injectExtensions(view, url ?: "", extensions)
                            
                            // Detect if page has video elements and check if playing
                            view?.evaluateJavascript(
                                """
                                (function() {
                                    var video = document.querySelector('video');
                                    if (!video) return false;
                                    return !video.paused && !video.ended && video.currentTime > 0;
                                })();
                                """.trimIndent()
                            ) { result ->
                                val isPlaying = result == "true"
                                isVideoPlaying = isPlaying
                                if (isPlaying) {
                                    onMediaPlayingChanged(true)
                                    miniPlayerOffered = false
                                } else {
                                    onMediaStopped()
                                }
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            // Section 31 adblock
                            if (adBlockOn && request != null) {
                                val reqUrl = request.url.toString()
                                if (isAdUrl(reqUrl)) {
                                    val domain = request.url.host ?: ""
                                    onAdBlocked(domain)
                                    return WebResourceResponse("text/plain", "utf-8", null)
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            handler?.proceed() // Accept SSL for broader site compatibility
                        }
                    }

                    webView = this
                    loadUrl(url)
                }
            },
            update = { wv ->
                if (wv.url != url && url.isNotBlank()) {
                    wv.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Section 20: Pull-down refresh gesture - offset to not block header
        // Also handles swipe-down to minimize video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .pointerInput(isLoading, isVideoPlaying, isFullscreen) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0 && !isLoading) {
                                // If video is playing (not fullscreen), track video swipe
                                if (isVideoPlaying && !isFullscreen) {
                                    videoSwipeDistance = (videoSwipeDistance + dragAmount).coerceAtMost(250f)
                                } else {
                                    pullDistance = (pullDistance + dragAmount).coerceAtMost(220f)
                                }
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            // Check video swipe first
                            if (isVideoPlaying && !isFullscreen && videoSwipeDistance >= videoSwipeThreshold) {
                                onMinimizeVideo()
                            } else if (pullDistance >= pullThreshold && !isLoading) {
                                webView?.reload()
                            }
                            pullDistance = 0f
                            videoSwipeDistance = 0f
                        },
                        onDragCancel = {
                            pullDistance = 0f
                            videoSwipeDistance = 0f
                        }
                    )
                }
        )

        // Section 19: Loading indicator — Saturn spinner, not black screen
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SaturnLoadingSpinner(accentColor = accentColor, modifier = Modifier.size(56.dp))
        }

        // Section 20: Loading progress bar at top
        if (isLoading && progress < 100) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                color = accentColor,
                trackColor = Color.Transparent
            )
        }



        val showPullIndicator = isLoading || pullDistance > 0f
        if (showPullIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(EclipseSurface.copy(alpha = 0.95f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    SaturnLoadingSpinner(accentColor = accentColor, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = if (pullDistance >= pullThreshold) "Release to refresh" else "Pull to refresh",
                        fontFamily = SpaceMono,
                        fontSize = 9.sp,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // Section 38.4: Fullscreen overlay — above everything
        if (isFullscreen && fullscreenView != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(999f)
            ) {
                AndroidView(
                    factory = { fullscreenView!! },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Feature 2: Mini player overlay - shown when video is minimized
        // The mini player floats in the corner, the page behind stays interactive
        AnimatedVisibility(
            visible = isMiniPlayerActive,
            enter = scaleIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
            exit = scaleOut(animationSpec = tween(200)) + fadeOut(tween(150)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        ) {
            // Empty Box with align - MiniPlayer positions itself in the corner
            Box(modifier = Modifier.fillMaxSize()) {
                MiniPlayer(
                    url = miniPlayerUrl,
                    accentColor = accentColor,
                    onMaximize = {
                        // Exit mini player mode, let video continue in WebView
                        injectVideoControlScript(webView, false)
                        isMiniPlayerActive = false
                        onEnterFullscreen()
                    },
                    onClose = {
                        // Exit mini player and go back
                        injectVideoControlScript(webView, false)
                        isMiniPlayerActive = false
                        webView?.goBack()
                    },
                    onPlayPause = {
                        // Toggle video playback
                        controlVideo(webView, "play")
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

// Section 18: Inject userscripts that match the current URL
private fun injectExtensions(webView: WebView?, url: String, extensions: List<Extension>) {
    if (webView == null || url.isBlank()) return
    extensions.filter { it.isEnabled }.forEach { ext ->
        val matches = ext.matchRules.any { rule -> matchesPattern(url, rule) }
        if (matches) {
            val safeScript = """
                try {
                    (function() {
                        ${ext.scriptCode}
                    })();
                } catch(e) {
                    // Script error caught silently — never crash browser
                    console.error('Eclipse Extension Error [${ext.name}]:', e);
                }
            """.trimIndent()
            webView.evaluateJavascript(safeScript, null)
            
            // For GreasyFork scripts on YouTube, re-inject after delay to catch fast-loading ads
            if (ext.source == "greasyfork" && url.contains("youtube")) {
                val scriptCopy = safeScript
                webView.postDelayed({
                    webView.evaluateJavascript(scriptCopy, null)
                }, 1000)
                webView.postDelayed({
                    webView.evaluateJavascript(scriptCopy, null)
                }, 3000)
            }
        }
    }
}

// Section 18: Simple glob-style URL pattern matching
private fun matchesPattern(url: String, pattern: String): Boolean {
    if (pattern == "*://*/*") return true
    val regex = pattern
        .replace(".", "\\.")
        .replace("*", ".*")
    return Regex(regex).containsMatchIn(url)
}

// Ad blocking domains list
private val AD_DOMAINS = setOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com",
    "adnxs.com", "adsrvr.org", "rubiconproject.com", "openx.net",
    "pubmatic.com", "criteo.com", "taboola.com", "outbrain.com",
    "amazon-adsystem.com", "facebook.com/plugins/like",
    "google-analytics.com", "googletagmanager.com",
    "hotjar.com", "mixpanel.com", "segment.com",
    "scorecardresearch.com", "quantserve.com", "moatads.com",
    "doubleverify.com", "adsafeprotected.com", "ad.doubleclick.net",
    "ads.yahoo.com", "yieldmanager.com", "advertising.com",
    "media.net", "zedo.com", "servedby.flashtalking.com",
    "casalemedia.com", "appnexus.com", "indexexchange.com",
    "smartadserver.com", "spotxchange.com", "sharethrough.com",
    "contextweb.com", "sovrn.com", "undertone.com", "lijit.com"
)

private fun isAdUrl(url: String): Boolean {
    return AD_DOMAINS.any { domain -> url.contains(domain) }
}

// Section 38.6: Hide system bars for fullscreen video
private fun hideSystemBars(context: Context) {
    val activity = context as? Activity ?: return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        activity.window.insetsController?.let { controller ->
            controller.hide(
                android.view.WindowInsets.Type.statusBars() or
                        android.view.WindowInsets.Type.navigationBars()
            )
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }
}

// Section 38.6: Restore system bars after fullscreen
private fun restoreSystemBars(context: Context) {
    val activity = context as? Activity ?: return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        activity.window.insetsController?.show(
            android.view.WindowInsets.Type.statusBars() or
                    android.view.WindowInsets.Type.navigationBars()
        )
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}

// Feature 2: Mini player composable - floating corner player
// The WebView continues playing behind this, this just provides the floating UI
@Composable
private fun MiniPlayer(
    url: String,
    accentColor: Color,
    onMaximize: () -> Unit,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    // This Box floats in the corner and does NOT block the page behind
    Box(
        modifier = modifier
            .padding(16.dp)
            .width(180.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.95f))
            .border(1.5.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {} // Allow touch to pass through to page
    ) {
        // Gradient overlay at bottom for controls visibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Video info at top
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "▶ Playing",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = extractDomain(url),
                color = Color.Gray,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Controls overlay at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
            // Maximize button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.3f))
                    .clickable { onMaximize() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⛶",
                    color = accentColor,
                    fontSize = 12.sp
                )
            }
            // Close button - goes back in history
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun extractDomain(url: String): String {
    return try {
        java.net.URI(url).host?.replace("www.", "") ?: url
    } catch (e: Exception) {
        url
    }
}

// Feature 2: Inject JavaScript to handle video mini player
// This notifies the webpage when mini player mode is active so it can adapt
// This allows the webpage to know when mini player mode is active
private fun injectVideoControlScript(webView: WebView?, isMiniPlayer: Boolean) {
    if (webView == null) return
    
    val script = """
        (function() {
            // Find all video elements
            var videos = document.querySelectorAll('video');
            videos.forEach(function(video) {
                // Store original state
                video.eclipseMiniPlayerMode = $isMiniPlayer;
                // Notify Eclipse app about video state
                if (window.eclipseVideoCallback) {
                    window.eclipseVideoCallback({
                        hasVideo: true,
                        playing: !video.paused,
                        miniPlayerMode: $isMiniPlayer,
                        duration: video.duration,
                        currentTime: video.currentTime
                    });
                }
            });
        })();
    """.trimIndent()
    
    webView.evaluateJavascript(script, null)
}

// Feature 2: Control video from mini player
private fun controlVideo(webView: WebView?, action: String) {
    if (webView == null) return
    
    val script = when (action) {
        "play" -> """
            (function() {
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) { v.play(); });
            })();
        """.trimIndent()
        "pause" -> """
            (function() {
                var videos = document.querySelectorAll('video');
                videos.forEach(function(v) { v.pause(); });
            })();
        """.trimIndent()
        "exitMiniPlayer" -> """
            (function() {
                var videos = document.querySelectorAll('video');
                videos.forEach(function(video) {
                    video.eclipseMiniPlayerMode = false;
                });
            })();
        """.trimIndent()
        else -> return
    }
    
    webView.evaluateJavascript(script, null)
}

// Needed import for zIndex
private fun Modifier.zIndex(index: Float): Modifier = this.then(
    Modifier.graphicsLayerZ(index)
)

private fun Modifier.graphicsLayerZ(z: Float): Modifier {
    return this
}
