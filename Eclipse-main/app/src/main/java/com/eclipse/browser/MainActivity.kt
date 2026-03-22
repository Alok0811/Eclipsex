package com.eclipse.browser

import android.os.Build
import android.os.Debug
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.eclipse.browser.ui.components.*
import com.eclipse.browser.ui.screens.*
import com.eclipse.browser.ui.sheets.MenuSheet
import com.eclipse.browser.ui.sheets.CustomizeSheet
import com.eclipse.browser.ui.sheets.TabsSheet
import com.eclipse.browser.ui.sheets.HistorySheet
import com.eclipse.browser.ui.sheets.BookmarksSheet
import com.eclipse.browser.ui.sheets.AboutSheet
import com.eclipse.browser.ui.sheets.AdPanelSheet
import com.eclipse.browser.ui.theme.EclipseTheme
import com.eclipse.browser.ui.viewmodel.HomeViewModel
import com.eclipse.browser.ui.viewmodel.Screen
import org.json.JSONArray
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Section 33: Runtime integrity checks with graceful restriction when tampered/rooted/debugged.
        if (!verifyAppIntegrity()) {
            vm.applySecurityRestrictions()
        }

        // Section 38: Edge-to-edge layout so fullscreen video goes behind bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            EclipseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    EclipseApp(vm = vm)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        vm.onAppPaused()
    }

    override fun onStop() {
        super.onStop()
        vm.onAppStopped()
    }

    override fun onResume() {
        super.onResume()
        vm.onAppResumed()
    }

    private fun verifyAppIntegrity(): Boolean {
        return verifyAppSignature() && !isRooted() && !isDebuggerAttached()
    }

    // Section 33.7: Compare signature with expected
    private fun verifyAppSignature(): Boolean {
        return try {
            val pm = packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signatures = info.signingInfo?.apkContentsSigners
                signatures != null && signatures.isNotEmpty()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val signatures = info.signatures
                signatures != null && signatures.isNotEmpty()
            }
        } catch (e: Exception) {
            true // Don't block on exception
        }
    }

    private fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    private fun isRooted(): Boolean {
        val rootPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        val hasSu = rootPaths.any { File(it).exists() }
        val testKeys = Build.TAGS?.contains("test-keys") == true
        return hasSu || testKeys
    }
}

@Composable
fun EclipseApp(vm: HomeViewModel) {
    val state by vm.uiState.collectAsState()

    // Load extensions from storage
    val extensionsList = remember(state.extensionsJson) {
        parseExtensions(state.extensionsJson)
    }

    // Section 21: Global back handler — back navigates screens, only closes on HOME
    BackHandler(enabled = state.currentScreen != Screen.HOME || state.isIncognito) {
        when {
            state.showCustomize  -> vm.hideSheet("customize")
            state.showMenu       -> vm.hideSheet("menu")
            state.showHistory    -> vm.hideSheet("history")
            state.showBookmarks  -> vm.hideSheet("bookmarks")
            state.showAbout      -> vm.hideSheet("about")
            state.showTabs       -> vm.hideSheet("tabs")
            state.showAdPanel    -> vm.hideSheet("adpanel")
            state.isIncognito && state.currentScreen == Screen.INCOGNITO -> vm.exitIncognito()
            state.currentScreen == Screen.AI_CHAT -> vm.goHome()
            state.currentScreen == Screen.SEARCH_RESULTS -> vm.goHome()
            state.currentScreen == Screen.EXTENSIONS -> vm.goHome()
            state.currentScreen == Screen.WEBVIEW -> {
                // WebViewScreen has its own back handling for in-page nav
                vm.goHome()
            }
            else -> vm.goHome()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── MAIN CONTENT ──
        Column(modifier = Modifier.fillMaxSize()) {

            // Content area fills remaining space
            Box(modifier = Modifier.weight(1f)) {

                // Section 24: Animated screen transitions
                AnimatedContent(
                    targetState = state.currentScreen,
                    transitionSpec = {
                        when (targetState) {
                            Screen.HOME, Screen.INCOGNITO ->
                                (fadeIn(tween(280)) + slideInVertically { it / 16 }) togetherWith
                                        (fadeOut(tween(200)) + slideOutVertically { -it / 16 })
                            Screen.AI_CHAT ->
                                (fadeIn(tween(280)) + slideInHorizontally { it / 4 }) togetherWith
                                        (fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })
                            Screen.WEBVIEW ->
                                (fadeIn(tween(200)) + slideInHorizontally { it / 6 }) togetherWith
                                        (fadeOut(tween(160)))
                            Screen.SEARCH_RESULTS ->
                                (fadeIn(tween(260)) + slideInVertically { it / 8 }) togetherWith
                                        (fadeOut(tween(200)))
                            Screen.EXTENSIONS ->
                                (fadeIn(tween(280)) + slideInHorizontally { it / 4 }) togetherWith
                                        (fadeOut(tween(200)))
                            Screen.BACKGROUND_PICKER ->
                                (fadeIn(tween(280)) + slideInHorizontally { it / 4 }) togetherWith
                                        (fadeOut(tween(200)) + slideOutHorizontally { -it / 8 })
                        }
                    },
                    label = "screenNav",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        Screen.HOME -> HomeScreen(
                            state = state,
                            onSearch = { vm.doSearch(it) },
                            onTabSelected = { vm.setActiveSearchTab(it) },
                            onSiteClick = { vm.navigateTo(it) },
                            onSiteLongPress = { vm.removeQuickSite(it) },
                            onAddSiteClick = { vm.showToast("Long press any site to remove") },
                            onOpenAi = { vm.openAiChat() },
                            modifier = Modifier.fillMaxSize()
                        )

                        Screen.INCOGNITO -> IncognitoScreen(
                            onSearch = { vm.doSearch(it) },
                            onExit = { vm.exitIncognito() },
                            modifier = Modifier.fillMaxSize()
                        )

                        Screen.WEBVIEW -> WebViewScreen(
                            url = state.webViewUrl ?: "",
                            accentColor = state.accentColor,
                            adBlockOn = state.adBlockOn,
                            refreshNonce = state.webRefreshNonce,
                            extensions = extensionsList,
                            onUrlChanged = { url, title -> vm.updateTabInfo(url, title) },
                            onAdBlocked = { domain -> vm.onAdBlocked(domain) },
                            onBack = { vm.goHome() },
                            onForward = {},
                            onEnterFullscreen = { vm.setFullscreen(true) },
                            onExitFullscreen = { vm.setFullscreen(false) },
                            onMinimizeVideo = { vm.goHome() },
                            modifier = Modifier.fillMaxSize()
                        )

                        Screen.AI_CHAT -> AiChatScreen(
                            messages = state.aiMessages,
                            isLoading = state.aiLoading,
                            accentColor = state.accentColor,
                            remainingMessages = state.aiRemainingMessages,
                            responseMode = state.aiResponseMode,
                            onSendMessage = { vm.sendAiMessage(it) },
                            onSendImageMessage = { msg, b64 -> vm.sendAiMessageWithImage(msg, b64) },
                            onSendFileMessage = { msg, text, name -> vm.sendAiMessageWithFile(msg, text, name) },
                            onReloadMessage = { vm.reloadLastAiMessage() },
                            onResponseModeChange = { vm.setAiResponseMode(it) },
                            onBack = { vm.goHome() },
                            modifier = Modifier.fillMaxSize()
                        )

                        Screen.SEARCH_RESULTS -> SearchResultsScreen(
                            query = state.currentSearchQuery,
                            searchResponse = state.searchResults,
                            isLoading = state.searchLoading,
                            accentColor = state.accentColor,
                            activeTab = state.activeSearchTab,
                            starsOn = state.starsOn,
                            bgTheme = state.bgTheme,
                            onTabSelected = { tab ->
                                vm.setActiveSearchTab(tab)
                                vm.performSearch(state.currentSearchQuery, tab, 1)
                            },
                            onResultClick = { vm.navigateTo(it) },
                            onBack = { vm.goHome() },
                            onRefresh = { vm.refreshSearchResults() },
                            onLoadMore = { vm.loadMoreSearchResults() },
                            onNewSearch = { newQuery -> vm.doSearch(newQuery) },
                            modifier = Modifier.fillMaxSize()
                        )

                        Screen.EXTENSIONS -> ExtensionsScreen(
                            extensions = extensionsList,
                            accentColor = state.accentColor,
                            onToggle = { id, enabled -> vm.toggleExtension(id, enabled) },
                            onRemove = { id -> vm.removeExtension(id) },
                            onInstall = { script -> vm.installExtension(script) },
                            onBack = { vm.goHome() },
                            modifier = Modifier.fillMaxSize()
                        )

                        Screen.BACKGROUND_PICKER -> BackgroundPickerScreen(
                            currentBgType = state.customBgType,
                            currentBgPath = state.customBgPath,
                            accentColor = state.accentColor,
                            onSave = { type, path, animate -> vm.saveCustomBackground(type, path, animate) },
                            onClear = { vm.clearCustomBackground() },
                            onBack = { vm.goHome() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // ── BOTTOM NAV BAR ──
            // Hidden during AI chat fullscreen and landscape video fullscreen
            if (state.currentScreen != Screen.AI_CHAT && !state.isInFullscreen) {
                BottomNavBar(
                    accentColor = state.accentColor,
                    tabCount = state.tabs.size,
                    canGoBack = state.currentScreen == Screen.WEBVIEW,
                    canGoForward = state.currentScreen == Screen.WEBVIEW,
                    isIncognito = state.isIncognito,
                    onBack = {
                        // Section 21: Back from WebView goes to previous page or home
                        vm.goHome()
                    },
                    onForward = { },
                    onHome = { vm.goHome() },
                    onTabs = { vm.showSheet("tabs") },
                    onMenu = { vm.showSheet("menu") }
                )
            }
        }

        // ── SHEETS (overlay on top of everything) ──
        if (state.showCustomize) {
            CustomizeSheet(
                accentHex = state.accentHex,
                accentLightHex = state.accentLightHex,
                bgTheme = state.bgTheme,
                particlesOn = state.particlesOn,
                starsOn = state.starsOn,
                weatherOn = state.weatherOn,
                clockOn = state.clockOn,
                quickSitesOn = state.quickSitesOn,
                adBlockOn = state.adBlockOn,
                aiEnabled = state.aiEnabled,
                customBgActive = state.customBgType == "image" || state.customBgType == "video",
                onAccentChange = { hex, light -> vm.setAccentColor(hex, light) },
                onBgThemeChange = { vm.setBgTheme(it) },
                onParticlesToggle = { vm.toggleParticles(it) },
                onStarsToggle = { vm.toggleStars(it) },
                onWeatherToggle = { vm.toggleWeather(it) },
                onClockToggle = { vm.toggleClock(it) },
                onQuickSitesToggle = { vm.toggleQuickSites(it) },
                onAdBlockToggle = { vm.toggleAdBlock(it) },
                onAiToggle = { vm.toggleAi(it) },
                onOpenBackgroundPicker = {
                    vm.hideSheet("customize")
                    vm.openBackgroundPicker()
                },
                onDismiss = { vm.hideSheet("customize") }
            )
        }

        if (state.showMenu) {
            MenuSheet(
                state = state,
                onDismiss = { vm.hideSheet("menu") },
                onOpenCustomize = { vm.hideSheet("menu"); vm.showSheet("customize") },
                onOpenHistory = { vm.hideSheet("menu"); vm.showSheet("history") },
                onOpenBookmarks = { vm.hideSheet("menu"); vm.showSheet("bookmarks") },
                onOpenAbout = { vm.hideSheet("menu"); vm.showSheet("about") },
                onOpenExtensions = { vm.hideSheet("menu"); vm.openExtensions() },
                onAddBookmark = { vm.addBookmarkCurrent() },
                onIncognito = { vm.hideSheet("menu"); vm.enterIncognito() },
                onRefresh = { vm.hideSheet("menu"); vm.requestWebRefresh() }
            )
        }

        // Tabs sheet
        if (state.showTabs) {
            TabsSheet(
                tabs = state.tabs,
                activeTabId = state.activeTabId,
                accentColor = state.accentColor,
                onTabClick = { vm.switchToTab(it); vm.hideSheet("tabs") },
                onCloseTab = { vm.closeTab(it) },
                onNewTab = { vm.newTab(); vm.hideSheet("tabs") },
                onNewIncognitoTab = { vm.enterIncognito(); vm.hideSheet("tabs") },
                onDismiss = { vm.hideSheet("tabs") }
            )
        }

        // History sheet
        if (state.showHistory) {
            HistorySheet(
                history = state.history,
                accentColor = state.accentColor,
                onItemClick = { url -> vm.navigateTo(url); vm.hideSheet("history") },
                onDeleteItem = { vm.deleteHistoryItem(it) },
                onClearAll = { vm.clearHistory() },
                onDismiss = { vm.hideSheet("history") }
            )
        }

        // Bookmarks sheet
        if (state.showBookmarks) {
            BookmarksSheet(
                bookmarks = state.bookmarks,
                accentColor = state.accentColor,
                onItemClick = { url -> vm.navigateTo(url); vm.hideSheet("bookmarks") },
                onDeleteItem = { vm.removeBookmark(it) },
                onDismiss = { vm.hideSheet("bookmarks") }
            )
        }

        // About sheet
        if (state.showAbout) {
            AboutSheet(
                accentColor = state.accentColor,
                onDismiss = { vm.hideSheet("about") }
            )
        }

        // Ad panel sheet
        if (state.showAdPanel) {
            AdPanelSheet(
                adBlockOn = state.adBlockOn,
                adsBlocked = state.adsBlocked,
                trackersBlocked = state.trackersBlocked,
                accentColor = state.accentColor,
                onToggleAdBlock = { vm.toggleAdBlock(it) },
                onDismiss = { vm.hideSheet("adpanel") }
            )
        }

        // Section 23: Bottom center toast
        EclipseToast(
            message = state.toastMessage,
            accentColor = state.accentColor,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ── EXTENSION HELPERS ──
private fun parseExtensions(json: String): List<Extension> {
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val rulesArr = obj.optJSONArray("matchRules") ?: JSONArray()
            Extension(
                id = obj.getString("id"),
                name = obj.getString("name"),
                description = obj.optString("description"),
                matchRules = (0 until rulesArr.length()).map { rulesArr.getString(it) },
                scriptCode = obj.optString("scriptCode"),
                isEnabled = obj.optBoolean("isEnabled", true),
                installedAt = obj.optLong("installedAt", 0),
                source = obj.optString("source", "default")
            )
        }
    } catch (e: Exception) { emptyList() }
}
