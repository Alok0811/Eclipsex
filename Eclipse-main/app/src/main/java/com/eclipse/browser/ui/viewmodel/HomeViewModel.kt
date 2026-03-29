package com.eclipse.browser.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eclipse.browser.EclipseConfig
import com.eclipse.browser.SupabaseDatabase
import com.eclipse.browser.data.DEFAULT_EXTENSIONS
import com.eclipse.browser.data.StorageManager
import com.eclipse.browser.ui.screens.DiscoverScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.eclipse.browser.ui.screens.AiMessage

// ── DATA MODELS ──

data class TabInfo(
    val id: Int,
    var url: String,
    var title: String,
    val incognito: Boolean = false
)

data class QuickSite(val url: String, val label: String)

data class BookmarkItem(val url: String, val title: String, val time: Long)

data class HistoryItem(val url: String, val title: String, val time: Long)

data class WeatherData(
    val temp: String = "--°",
    val condition: String = "",
    val icon: String = "01d",
    val city: String = ""
)

// Section 30 — Search result from backend /search endpoint
data class SearchResult(
    val title: String,
    val url: String,
    val displayUrl: String,
    val snippet: String,
    val favicon: String,
    val thumbnail: String? = null,
    val publishedDate: String? = null
)

// Section 30 — Full search response
data class SearchResponse(
    val results: List<SearchResult>,
    val aiAnswer: String?,
    val query: String,
    val category: String,
    val page: Int
)

data class EclipseUiState(
    val greeting: String = "Good Morning",
    val greetingWord: String = "Morning",
    val clock: String = "--:-- --",
    val date: String = "",
    val isNight: Boolean = false,
    val accentColor: Color = Color(0xFFFF6B1A),
    val accentColorLight: Color = Color(0xFFFFB347),
    val accentHex: String = "#FF6B1A",
    val accentLightHex: String = "#FFB347",
    val bgTheme: String = "eclipse",
    // Section 31.3: searchEngine REMOVED from state — SearXNG handles all search
    val particlesOn: Boolean = true,
    val starsOn: Boolean = true,
    // Section 25.1: orbOn kept for data compat but orb is not shown in UI
    val orbOn: Boolean = false,
    val weatherOn: Boolean = true,
    val clockOn: Boolean = true,
    val quickSitesOn: Boolean = true,
    val uiStyle: String = "normal",
    val adBlockOn: Boolean = true,
    val adsBlocked: Int = 0,
    val trackersBlocked: Int = 0,
    val weather: WeatherData = WeatherData(),
    val quickSites: List<QuickSite> = emptyList(),
    val bookmarks: List<BookmarkItem> = emptyList(),
    val history: List<HistoryItem> = emptyList(),
    val toastMessage: String? = null,
    val currentScreen: Screen = Screen.HOME,
    val isIncognito: Boolean = false,
    val tabs: List<TabInfo> = listOf(TabInfo(1, "home", "New Tab")),
    val activeTabId: Int = 1,
    // Section 30 filter tabs: all / images / videos / news (ai tab removed — AI is a separate screen)
    val activeSearchTab: String = "all",
    val webViewUrl: String? = null,
    val showCustomize: Boolean = false,
    val showTabs: Boolean = false,
    val showMenu: Boolean = false,
    val showHistory: Boolean = false,
    val showBookmarks: Boolean = false,
    val showAbout: Boolean = false,
    val showAdPanel: Boolean = false,
    val onboardingDone: Boolean = false,
    val aiMessages: List<AiMessage> = emptyList(),
    val aiLoading: Boolean = false,
    val aiEnabled: Boolean = true,
    val customBgType: String = "none",
    val customBgPath: String = "",
    val extensionsJson: String = DEFAULT_EXTENSIONS,
    // Section 31.12: remaining message count from backend
    val aiRemainingMessages: Int = 200,
    // Section 8: response mode
    val aiResponseMode: String = EclipseConfig.MODE_SHORT,
    // Section 31.6: search results screen state
    val searchResults: SearchResponse? = null,
    val searchLoading: Boolean = false,
    val currentSearchQuery: String = "",
    // Section 19: loading state for webview
    val isWebViewLoading: Boolean = false,
    val webRefreshNonce: Int = 0,
    // Section 38: fullscreen video state
    val isVideoFullscreen: Boolean = false,
    // Section 30.13: star opacity reduced during search
    val searchActive: Boolean = false,
    // Hide bottom nav during fullscreen landscape video
    val isInFullscreen: Boolean = false,
    // Feature 1: Navigation history stack (for ALL screens)
    val backStack: List<ScreenHistoryEntry> = emptyList(),
    val forwardStack: List<ScreenHistoryEntry> = emptyList(),
    // Feature 1: WebView navigation state
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    // Feature 2: Mini player state
    val isVideoMinimized: Boolean = false,
    // Feature 3/4/5: Media playback state for background playback
    val isMediaPlaying: Boolean = false,
    val mediaPlayingTabId: Int? = null,
    // Feature 1: WebView navigation actions
    val webViewAction: WebViewAction = WebViewAction.NONE
)

// Data class for screen history entries
data class ScreenHistoryEntry(
    val screen: Screen,
    val url: String? = null,
    val searchQuery: String? = null
)

enum class WebViewAction {
    NONE, GO_BACK, GO_FORWARD
}

enum class Screen { HOME, WEBVIEW, INCOGNITO, AI_CHAT, SEARCH_RESULTS, EXTENSIONS, BACKGROUND_PICKER }

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val storage = StorageManager(application)
    
    // Supabase cloud database
    val supabaseDb = SupabaseDatabase(application)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var tabIdCounter = 1
    private var deviceId: String = ""

    private val _uiState = MutableStateFlow(EclipseUiState())
    val uiState: StateFlow<EclipseUiState> = _uiState.asStateFlow()

    init {
        updateClock()
        collectSettings()
        startClockUpdater()
        fetchWeather()
        ensureDeviceId()
        restoreTabsFromStorage()
        restoreAiHistoryFromStorage()
        startAiHistoryAutoSave()
        checkBackendHealth()
    }

    // ── DEVICE ID ──
    // Section 31.8: consistent unique identifier — never real device info
    private fun ensureDeviceId() {
        viewModelScope.launch {
            storage.deviceId.first().let { savedId ->
                if (savedId.isNotBlank()) {
                    deviceId = savedId
                } else {
                    // Generate a UUID and store it permanently
                    val newId = try {
                        Settings.Secure.getString(
                            getApplication<Application>().contentResolver,
                            Settings.Secure.ANDROID_ID
                        ) ?: UUID.randomUUID().toString()
                    } catch (e: Exception) {
                        UUID.randomUUID().toString()
                    }
                    // Hash it for extra privacy before storing
                    val hashedId = "eclipse_${newId.hashCode().toLong() and 0xFFFFFFFFL}"
                    storage.save(StorageManager.DEVICE_ID, hashedId)
                    deviceId = hashedId
                }
            }
        }
    }

    // ── SETTINGS COLLECTION ──
    private fun collectSettings() {
        viewModelScope.launch {
            storage.accentColor.collect { hex ->
                val color = parseColor(hex)
                _uiState.update { it.copy(accentColor = color, accentHex = hex) }
            }
        }
        viewModelScope.launch {
            storage.accentColorLight.collect { hex ->
                val color = parseColor(hex)
                _uiState.update { it.copy(accentColorLight = color, accentLightHex = hex) }
            }
        }
        viewModelScope.launch { storage.bgTheme.collect { v -> _uiState.update { it.copy(bgTheme = v) } } }
        // Section 31.3: NO searchEngine collection — removed
        viewModelScope.launch { storage.particlesOn.collect { v -> _uiState.update { it.copy(particlesOn = v) } } }
        viewModelScope.launch { storage.starsOn.collect { v -> _uiState.update { it.copy(starsOn = v) } } }
        viewModelScope.launch { storage.weatherOn.collect { v -> _uiState.update { it.copy(weatherOn = v) } } }
        viewModelScope.launch { storage.uiStyle.collect { v -> _uiState.update { it.copy(uiStyle = v) } } }
        viewModelScope.launch { storage.adBlockOn.collect { v -> _uiState.update { it.copy(adBlockOn = v) } } }
        viewModelScope.launch { storage.adsBlocked.collect { v -> _uiState.update { it.copy(adsBlocked = v) } } }
        viewModelScope.launch { storage.trackersBlocked.collect { v -> _uiState.update { it.copy(trackersBlocked = v) } } }
        viewModelScope.launch { storage.clockOn.collect { v -> _uiState.update { it.copy(clockOn = v) } } }
        viewModelScope.launch { storage.quickSitesOn.collect { v -> _uiState.update { it.copy(quickSitesOn = v) } } }
        viewModelScope.launch { storage.onboardingDone.collect { v -> _uiState.update { it.copy(onboardingDone = v) } } }
        viewModelScope.launch { storage.aiEnabled.collect { v -> _uiState.update { it.copy(aiEnabled = v) } } }
        viewModelScope.launch { storage.customBgType.collect { v -> _uiState.update { it.copy(customBgType = v) } } }
        viewModelScope.launch { storage.customBgPath.collect { v -> _uiState.update { it.copy(customBgPath = v) } } }
        viewModelScope.launch { storage.extensions.collect { v -> _uiState.update { it.copy(extensionsJson = v) } } }
        viewModelScope.launch {
            storage.aiResponseMode.collect { v ->
                _uiState.update { it.copy(aiResponseMode = v) }
            }
        }
        viewModelScope.launch {
            storage.quickSites.collect { json ->
                _uiState.update { it.copy(quickSites = parseQuickSites(json)) }
            }
        }
        viewModelScope.launch {
            storage.bookmarks.collect { json ->
                _uiState.update { it.copy(bookmarks = parseBookmarks(json)) }
            }
        }
        viewModelScope.launch {
            storage.history.collect { json ->
                _uiState.update { it.copy(history = parseHistory(json)) }
            }
        }
    }

    // ── CLOCK ──
    private fun startClockUpdater() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                updateClock()
            }
        }
    }

    fun updateClock() {
        val now = Calendar.getInstance()
        val h = now.get(Calendar.HOUR_OF_DAY)
        val m = now.get(Calendar.MINUTE)
        val ampm = if (h >= 12) "PM" else "AM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        val clock = "$h12:${m.toString().padStart(2, '0')} $ampm"

        val (greeting, word) = when {
            h in 5..11  -> "Good Morning" to "Morning"
            h in 12..16 -> "Good Afternoon" to "Afternoon"
            h in 17..20 -> "Good Evening" to "Evening"
            else         -> "Explore the Night" to "Night"
        }
        val isNight = h >= 21 || h < 5

        val sdf = SimpleDateFormat("EEEE • MMM d", Locale.getDefault())
        val date = sdf.format(now.time).uppercase()

        _uiState.update {
            it.copy(
                clock = clock,
                greeting = greeting,
                greetingWord = word,
                date = date,
                isNight = isNight
            )
        }
    }

    // ── URL DETECTION ──
    private val urlPattern = Regex("^(https?://|www\\.)", RegexOption.IGNORE_CASE)

    fun isLikelyUrl(input: String): Boolean {
        if (urlPattern.containsMatchIn(input)) return true
        return input.contains(".") && !input.contains(" ") && input.length < 80
    }

    // ── SEARCH — Section 31.4/31.5 NEW FLOW ──
    // Old flow removed: no more buildSearchUrl, no engines map, no search engine URLs
    // New flow: query → backend /search → SearchResultsScreen
    fun doSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        if (isLikelyUrl(trimmed)) {
            val url = if (trimmed.startsWith("http")) trimmed else "https://$trimmed"
            navigateTo(url)
            return
        }

        // Add current screen to history before navigating to search
        val currentEntry = ScreenHistoryEntry(
            screen = _uiState.value.currentScreen,
            url = _uiState.value.webViewUrl
        )

        // Section 31.6: navigate to search results screen
        _uiState.update {
            it.copy(
                currentScreen = Screen.SEARCH_RESULTS,
                currentSearchQuery = trimmed,
                searchResults = null,
                searchLoading = true,
                searchActive = true,
                backStack = if (it.currentScreen != Screen.SEARCH_RESULTS) it.backStack + currentEntry else it.backStack,
                forwardStack = emptyList(),
                canGoBack = true,
                canGoForward = false
            )
        }

        // Sync to Supabase cloud
        viewModelScope.launch {
            supabaseDb.addSearchHistory(trimmed)
        }

        performSearch(trimmed, _uiState.value.activeSearchTab, 1)
    }

    // Section 31.5: Send search to backend
    fun performSearch(query: String, tab: String, page: Int) {
        viewModelScope.launch {
            try {
                // Section 30.3: category mapping
                val category = when (tab) {
                    "images" -> "images"
                    "videos" -> "videos"
                    "news"   -> "news"
                    else     -> "general"
                }

                val requestBody = JSONObject().apply {
                    put("query", query)
                    put("category", category)
                    put("page", page)
                    put("deviceId", deviceId)
                }.toString()

                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(EclipseConfig.ENDPOINT_SEARCH)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }

                val bodyStr = response.body?.string()

                if (!response.isSuccessful || bodyStr == null) {
                    _uiState.update { it.copy(searchLoading = false) }
                    showToast("Search unavailable. Try again!")
                    return@launch
                }

                val json = JSONObject(bodyStr)
                val resultsArr = json.optJSONArray("results") ?: JSONArray()
                val results = mutableListOf<SearchResult>()
                for (i in 0 until resultsArr.length()) {
                    val r = resultsArr.getJSONObject(i)
                    results.add(
                        SearchResult(
                            title       = r.optString("title"),
                            url         = r.optString("url"),
                            displayUrl  = r.optString("displayUrl"),
                            snippet     = r.optString("snippet"),
                            favicon     = r.optString("favicon"),
                            thumbnail   = r.optString("thumbnail").ifBlank { null },
                            publishedDate = r.optString("publishedDate").ifBlank { null }
                        )
                    )
                }

                val searchResponse = SearchResponse(
                    results  = if (page > 1 && _uiState.value.searchResults != null) {
                        (_uiState.value.searchResults?.results ?: emptyList()) + results
                    } else {
                        results
                    },
                    aiAnswer = json.optString("aiAnswer").ifBlank { null },
                    query    = json.optString("query", query),
                    category = json.optString("category", category),
                    page     = json.optInt("page", page)
                )

                _uiState.update {
                    it.copy(
                        searchResults = searchResponse,
                        searchLoading = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(searchLoading = false) }
                showToast("No internet connection")
            }
        }
    }

    // Load more search results (Section 30.9: infinite scroll)
    fun loadMoreSearchResults() {
        val current = _uiState.value.searchResults ?: return
        val nextPage = current.page + 1
        performSearch(current.query, current.category, nextPage)
    }

    fun refreshSearchResults() {
        val currentQuery = _uiState.value.currentSearchQuery
        if (currentQuery.isNotBlank()) {
            _uiState.update { it.copy(searchLoading = true) }
            performSearch(currentQuery, _uiState.value.activeSearchTab, 1)
        }
    }

    // ── AI CHAT — Section 31.7/31.8 ──
    // All direct Groq calls REMOVED. Everything goes through backend.

    fun sendAiMessage(content: String) {
        // Section 36: Detect conversational intent before sending
        val detectedIntent = detectIntent(content)
        val lowerContent = content.lowercase().trim()

        // Section 34: Identity rules — always attribute Eclipse AI to Eclipse development team.
        identityResponseFor(lowerContent)?.let { identityReply ->
            val userMsg = AiMessage(role = "user", content = content)
            _uiState.update {
                it.copy(
                    aiMessages = it.aiMessages + userMsg + AiMessage(role = "assistant", content = identityReply),
                    aiLoading = false
                )
            }
            return
        }

        // Section 12: Humor response only when user explicitly asks to open AI chat while already in chat
        if (lowerContent == "open ai chat" || lowerContent == "open ai" || lowerContent == "launch ai" || lowerContent == "start chat") {
            val userMsg = AiMessage(role = "user", content = content)
            _uiState.update {
                it.copy(
                    aiMessages = it.aiMessages + userMsg + AiMessage(
                        role = "assistant",
                        content = "Say something funny, I'm in front of you 😄"
                    ),
                    aiLoading = false
                )
            }
            return
        }

        val userMsg = AiMessage(role = "user", content = content)
        _uiState.update {
            it.copy(
                aiMessages = it.aiMessages + userMsg,
                aiLoading = true
            )
        }

        viewModelScope.launch {
            try {
                // Section 31.11: Check if image generation requested
                val isImageGen = EclipseConfig.IMAGE_GEN_KEYWORDS.any { kw -> lowerContent.contains(kw) }

                if (isImageGen) {
                    // Extract prompt — remove the generation keyword phrase
                    val prompt = extractImagePrompt(content)
                    requestImageGeneration(prompt)
                    return@launch
                }

                // Build messages array with full conversation history (Section 31.8)
                val messagesArr = JSONArray()
                val recentMessages = _uiState.value.aiMessages.takeLast(20)
                recentMessages.forEach { msg ->
                    messagesArr.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }

                // Section 36.6: Include detected intent in context
                val currentMode = _uiState.value.aiResponseMode

                val requestBody = JSONObject().apply {
                    put("messages", messagesArr)
                    put("deviceId", deviceId)
                    put("responseMode", currentMode)
                    if (detectedIntent.isNotBlank()) put("intent", detectedIntent)
                }.toString()

                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(EclipseConfig.ENDPOINT_CHAT)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }

                val bodyStr = response.body?.string()

                if (response.code == 429) {
                    // Rate limit reached
                    val errorJson = bodyStr?.let { JSONObject(it) }
                    val msg = errorJson?.optString("message") ?: "Daily limit reached. Come back tomorrow!"
                    _uiState.update {
                        it.copy(
                            aiMessages = it.aiMessages + AiMessage(role = "assistant", content = msg),
                            aiLoading = false,
                            aiRemainingMessages = 0
                        )
                    }
                    return@launch
                }

                if (!response.isSuccessful || bodyStr == null) {
                    handleAiError()
                    return@launch
                }

                val json = JSONObject(bodyStr)
                val reply = json.optString("response", "").trim()
                val remaining = json.optInt("remainingMessages", 200)

                // Section 11: Parse and execute browser commands from AI response
                val cleanReply = parseBrowserCommands(reply)

                _uiState.update {
                    it.copy(
                        aiMessages = it.aiMessages + AiMessage(role = "assistant", content = cleanReply),
                        aiLoading = false,
                        aiRemainingMessages = remaining
                    )
                }

            } catch (e: Exception) {
                handleAiError()
            }
        }
    }

    // Section 31.9: Image/Vision message
    fun sendAiMessageWithImage(content: String, imageBase64: String) {
        val text = content.ifBlank { "What's in this image?" }
        val userMsg = AiMessage(role = "user", content = text, attachmentType = "image")
        _uiState.update {
            it.copy(
                aiMessages = it.aiMessages + userMsg,
                aiLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val requestBody = JSONObject().apply {
                    put("base64Image", imageBase64)
                    put("mimeType", "image/jpeg")
                    put("question", text)
                    put("deviceId", deviceId)
                }.toString()

                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(EclipseConfig.ENDPOINT_VISION)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }

                val bodyStr = response.body?.string()

                if (!response.isSuccessful || bodyStr == null) {
                    handleAiError()
                    return@launch
                }

                val json = JSONObject(bodyStr)
                val reply = json.optString("response", "").trim()
                val remaining = json.optInt("remainingMessages", 200)

                _uiState.update {
                    it.copy(
                        aiMessages = it.aiMessages + AiMessage(role = "assistant", content = reply),
                        aiLoading = false,
                        aiRemainingMessages = remaining
                    )
                }

            } catch (e: Exception) {
                handleAiError()
            }
        }
    }

    // Section 31.10: File/document processing
    fun sendAiMessageWithFile(content: String, fileText: String, fileName: String) {
        val text = content.ifBlank { "Please summarize this file." }
        val userMsg = AiMessage(role = "user", content = text, attachmentType = "document")
        _uiState.update {
            it.copy(
                aiMessages = it.aiMessages + userMsg,
                aiLoading = true
            )
        }

        viewModelScope.launch {
            try {
                val requestBody = JSONObject().apply {
                    put("fileText", fileText)
                    put("fileName", fileName)
                    put("question", text)
                    put("responseMode", _uiState.value.aiResponseMode)
                    put("deviceId", deviceId)
                }.toString()

                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(EclipseConfig.ENDPOINT_FILES)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }

                val bodyStr = response.body?.string()

                if (!response.isSuccessful || bodyStr == null) {
                    handleAiError()
                    return@launch
                }

                val json = JSONObject(bodyStr)
                val reply = json.optString("response", "").trim()
                val remaining = json.optInt("remainingMessages", 200)

                _uiState.update {
                    it.copy(
                        aiMessages = it.aiMessages + AiMessage(role = "assistant", content = reply),
                        aiLoading = false,
                        aiRemainingMessages = remaining
                    )
                }

            } catch (e: Exception) {
                handleAiError()
            }
        }
    }

    // Section 31.11: Image generation
    private fun requestImageGeneration(prompt: String) {
        viewModelScope.launch {
            try {
                val requestBody = JSONObject().apply {
                    put("prompt", prompt)
                    put("deviceId", deviceId)
                }.toString()

                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(EclipseConfig.ENDPOINT_GENERATE)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody.toRequestBody("application/json".toMediaType()))
                        .build()
                    client.newCall(request).execute()
                }

                val bodyStr = response.body?.string()

                if (bodyStr != null) {
                    val json = JSONObject(bodyStr)
                    val error = json.optString("error")

                    if (error == "model_loading") {
                        // Section 31.11: model_loading special case
                        _uiState.update {
                            it.copy(
                                aiMessages = it.aiMessages + AiMessage(
                                    role = "assistant",
                                    content = "Image model is warming up. Try again in 20 seconds! 🌑"
                                ),
                                aiLoading = false
                            )
                        }
                        return@launch
                    }

                    val imageBase64 = json.optString("image")
                    val remaining = json.optInt("remainingMessages", 200)

                    if (imageBase64.isNotBlank()) {
                        _uiState.update {
                            it.copy(
                                aiMessages = it.aiMessages + AiMessage(
                                    role = "assistant",
                                    content = "[IMAGE]",
                                    attachmentType = "generated_image",
                                    imageBase64 = imageBase64
                                ),
                                aiLoading = false,
                                aiRemainingMessages = remaining
                            )
                        }
                        return@launch
                    }
                }

                handleAiError()

            } catch (e: Exception) {
                handleAiError()
            }
        }
    }

    // Section 10: Reload — delete last AI message and regenerate
    fun reloadLastAiMessage() {
        val messages = _uiState.value.aiMessages.toMutableList()
        // Remove last AI message
        val lastAiIdx = messages.indexOfLast { it.role == "assistant" }
        if (lastAiIdx >= 0) {
            messages.removeAt(lastAiIdx)
            _uiState.update { it.copy(aiMessages = messages) }
            // Find the last user message and resend
            val lastUserMsg = messages.lastOrNull { it.role == "user" }
            if (lastUserMsg != null) {
                sendAiMessage(lastUserMsg.content)
            }
        }
    }

    // ── BROWSER CONTROL — Section 11 ──
    private fun parseBrowserCommands(response: String): String {
        val cmdPattern = Regex("<ECLIPSE_CMD>(.*?)</ECLIPSE_CMD>", RegexOption.DOT_MATCHES_ALL)
        val match = cmdPattern.find(response) ?: return response

        try {
            val cmdJson = JSONObject(match.groupValues[1].trim())
            val action = cmdJson.optString("action")

            when (action) {
                "openUrl" -> {
                    val url = cmdJson.optString("url")
                    if (url.isNotBlank()) navigateTo(url)
                }
                "search" -> {
                    val query = cmdJson.optString("query")
                    if (query.isNotBlank()) doSearch(query)
                }
                "newTab" -> newTab()
                "goBack" -> { /* handled by WebView */ }
                "goForward" -> { /* handled by WebView */ }
                "clearHistory" -> clearHistory()
                "addBookmark" -> addBookmarkCurrent()
                "closeTab" -> {
                    val activeId = _uiState.value.activeTabId
                    closeTab(activeId)
                }
            }
        } catch (e: Exception) {
            // Invalid command JSON — just clean the response
        }

        // Return response with command tags removed
        return response.replace(cmdPattern, "").trim()
    }

    // Section 36: Intent detection for conversational understanding
    private fun detectIntent(message: String): String {
        val lower = message.lowercase().trim()
        return when {
            lower.contains("good news") || lower.contains("guess what") ||
            lower.contains("have something") || lower.contains("wait till") ->
                "Suspense/Announcement"
            lower == "hi" || lower == "hello" || lower == "hey" || lower.startsWith("hey ") ->
                "Greeting"
            lower.endsWith("?") -> "Question"
            lower.contains("help me") || lower.contains("can you") ->
                "Request"
            lower.contains("once upon") || lower.contains("there was") ->
                "Storytelling"
            else -> ""
        }
    }

    private fun identityResponseFor(lowerMessage: String): String? {
        val asksCreator = (
            lowerMessage.contains("who made you") ||
            lowerMessage.contains("who created you") ||
            lowerMessage.contains("who developed you") ||
            lowerMessage.contains("who built you") ||
            lowerMessage.contains("who built this ai")
        )
        if (!asksCreator) return null

        return "The Eclipse AI assistant was developed by the Eclipse development team. I was created as part of the Eclipse browser project. 🌙"
    }

    // Section 31.11: Extract image prompt from generation request
    private fun extractImagePrompt(content: String): String {
        val lower = content.lowercase()
        EclipseConfig.IMAGE_GEN_KEYWORDS.forEach { kw ->
            if (lower.contains(kw)) {
                val idx = lower.indexOf(kw) + kw.length
                val prompt = content.substring(idx).trim().trimStart(':', ' ')
                if (prompt.isNotBlank()) return prompt
            }
        }
        return content
    }

    private fun handleAiError() {
        _uiState.update {
            it.copy(
                aiMessages = it.aiMessages + AiMessage(
                    role = "assistant",
                    content = "Eclipse AI is taking a quick break. Try again in a moment! 🌑"
                ),
                aiLoading = false
            )
        }
    }

    // Section 8: Set AI response mode
    fun setAiResponseMode(mode: String) {
        _uiState.update { it.copy(aiResponseMode = mode) }
        viewModelScope.launch { storage.save(StorageManager.AI_RESPONSE_MODE, mode) }
    }

    // ── BACKEND HEALTH CHECK — Section 31.14 ──
    private fun checkBackendHealth() {
        viewModelScope.launch {
            delay(2000) // Don't block startup
            try {
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(EclipseConfig.ENDPOINT_HEALTH)
                        .get()
                        .build()
                    client.newCall(request).execute()
                }
                if (!response.isSuccessful) {
                    // Subtle warning — do not block user
                    // AI chat will show warning when user tries to use it
                }
            } catch (e: Exception) {
                // Backend check failed silently — app continues to work
            }
        }
    }

    // ── WEATHER ──
    fun fetchWeather() {
        viewModelScope.launch {
            try {
                val lat = storage.lastLat.first()
                val lon = storage.lastLon.first()
                if (lat.isNotBlank() && lon.isNotBlank()) {
                    fetchWeatherForCoords(lat.toDouble(), lon.toDouble())
                } else {
                    requestLocationAndFetchWeather()
                }
            } catch (e: Exception) {
                requestLocationAndFetchWeather()
            }
        }
    }

    private fun requestLocationAndFetchWeather() {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) return

        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    lm.removeUpdates(this)
                    viewModelScope.launch {
                        storage.save(StorageManager.LAST_LAT, loc.latitude.toString())
                        storage.save(StorageManager.LAST_LON, loc.longitude.toString())
                        fetchWeatherForCoords(loc.latitude, loc.longitude)
                    }
                }
                override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }
            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> return
            }
            lm.requestLocationUpdates(provider, 0, 0f, listener)
        } catch (e: Exception) { }
    }

    private fun fetchWeatherForCoords(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val apiKey = "01b6f7ef5529fb2a06c717f4df5ade5b"
                val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).get().build()
                    client.newCall(request).execute()
                }
                val body = response.body?.string() ?: return@launch
                val json = JSONObject(body)
                val temp = json.getJSONArray("weather").getJSONObject(0)
                val main = json.getJSONObject("main")
                val icon = temp.getString("icon")
                val condition = temp.getString("main")
                val tempVal = main.getDouble("temp").toInt()
                val city = json.optString("name", "")
                _uiState.update {
                    it.copy(
                        weather = WeatherData(
                            temp = "$tempVal°",
                            condition = condition,
                            icon = icon,
                            city = city
                        )
                    )
                }
            } catch (e: Exception) { }
        }
    }

    // ── NAVIGATION ──
    fun navigateTo(url: String) {
        // Add current screen to history before navigating
        val currentEntry = ScreenHistoryEntry(
            screen = _uiState.value.currentScreen,
            url = _uiState.value.webViewUrl
        )

        _uiState.update {
            it.copy(
                webViewUrl = url,
                currentScreen = Screen.WEBVIEW,
                searchActive = false,
                backStack = if (it.currentScreen != Screen.WEBVIEW) it.backStack + currentEntry else it.backStack,
                forwardStack = emptyList(),
                canGoBack = true,
                canGoForward = false
            )
        }
        addToHistory(url, "")
    }

    fun goHome() {
        val currentEntry = ScreenHistoryEntry(
            screen = _uiState.value.currentScreen,
            url = _uiState.value.webViewUrl
        )
        _uiState.update {
            it.copy(
                currentScreen = if (it.isIncognito) Screen.INCOGNITO else Screen.HOME,
                webViewUrl = null,
                searchActive = false,
                isVideoMinimized = false,
                backStack = if (it.currentScreen != Screen.HOME && it.currentScreen != Screen.INCOGNITO) {
                    it.backStack + currentEntry
                } else it.backStack,
                forwardStack = emptyList(),
                canGoBack = true,
                canGoForward = false
            )
        }
        updateClock()
    }

    // Feature 1: Navigate back - works across ALL screens
    fun goBack() {
        val state = _uiState.value
        
        // First try WebView history
        if (state.canGoBack && state.currentScreen == Screen.WEBVIEW) {
            _uiState.update { it.copy(webViewAction = WebViewAction.GO_BACK) }
            return
        }
        
        // Then try app-level history stack
        if (state.backStack.isNotEmpty()) {
            val currentEntry = ScreenHistoryEntry(screen = state.currentScreen, url = state.webViewUrl)
            val previousEntry = state.backStack.last()
            val newBackStack = state.backStack.dropLast(1)
            
            _uiState.update {
                it.copy(
                    currentScreen = previousEntry.screen,
                    webViewUrl = previousEntry.url,
                    searchActive = previousEntry.screen == Screen.SEARCH_RESULTS,
                    backStack = newBackStack,
                    forwardStack = it.forwardStack + currentEntry,
                    canGoBack = newBackStack.isNotEmpty(),
                    canGoForward = true,
                    isVideoMinimized = false
                )
            }
            return
        }
        
        // No more history - go home
        goHome()
    }

    // Feature 1: Navigate forward - works across ALL screens
    fun goForward() {
        val state = _uiState.value
        
        // First try WebView forward
        if (state.canGoForward && state.currentScreen == Screen.WEBVIEW) {
            _uiState.update { it.copy(webViewAction = WebViewAction.GO_FORWARD) }
            return
        }
        
        // Then try app-level forward stack
        if (state.forwardStack.isNotEmpty()) {
            val currentEntry = ScreenHistoryEntry(screen = state.currentScreen, url = state.webViewUrl)
            val nextEntry = state.forwardStack.last()
            val newForwardStack = state.forwardStack.dropLast(1)
            
            _uiState.update {
                it.copy(
                    currentScreen = nextEntry.screen,
                    webViewUrl = nextEntry.url,
                    searchActive = nextEntry.screen == Screen.SEARCH_RESULTS,
                    backStack = it.backStack + currentEntry,
                    forwardStack = newForwardStack,
                    canGoBack = true,
                    canGoForward = newForwardStack.isNotEmpty(),
                    isVideoMinimized = false
                )
            }
        }
    }

    // Feature 1: Clear the action after it's consumed
    fun clearWebViewAction() {
        _uiState.update { it.copy(webViewAction = WebViewAction.NONE) }
    }

    // Feature 1: Update canGoBack state from WebView (combined with app-level history)
    fun setCanGoBack(canGoBack: Boolean) {
        val state = _uiState.value
        val hasAppHistory = state.backStack.isNotEmpty()
        _uiState.update { it.copy(canGoBack = canGoBack || hasAppHistory) }
    }

    // Feature 1: Update canGoForward state from WebView (combined with app-level history)
    fun setCanGoForward(canGoForward: Boolean) {
        val state = _uiState.value
        val hasAppForward = state.forwardStack.isNotEmpty()
        _uiState.update { it.copy(canGoForward = canGoForward || hasAppForward) }
    }

    // Feature 2: Handle video minimized to mini player
    fun onVideoMinimized() {
        _uiState.update { it.copy(isVideoMinimized = true) }
    }

    fun openAiChat() {
        _uiState.update { it.copy(currentScreen = Screen.AI_CHAT) }
    }

    fun openExtensions() {
        _uiState.update { it.copy(currentScreen = Screen.EXTENSIONS) }
    }

    fun openBackgroundPicker() {
        _uiState.update { it.copy(currentScreen = Screen.BACKGROUND_PICKER) }
    }

    fun openSearchResults(query: String) {
        _uiState.update {
            it.copy(
                currentScreen = Screen.SEARCH_RESULTS,
                currentSearchQuery = query,
                searchLoading = true,
                searchActive = true
            )
        }
    }

    // ── TABS — Section 22: Persistence ──
    private fun restoreTabsFromStorage() {
        viewModelScope.launch {
            try {
                val savedJson = storage.savedTabs.first()
                val activeUrl = storage.activeTabUrl.first()
                if (savedJson == "[]" || savedJson.isBlank()) return@launch

                val arr = JSONArray(savedJson)
                val tabs = mutableListOf<TabInfo>()
                var maxId = 1
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getInt("id")
                    if (id > maxId) maxId = id
                    tabs.add(
                        TabInfo(
                            id = id,
                            url = obj.getString("url"),
                            title = obj.optString("title", "Tab")
                        )
                    )
                }
                if (tabs.isNotEmpty()) {
                    tabIdCounter = maxId
                    val activeTab = tabs.firstOrNull { it.url == activeUrl } ?: tabs.last()
                    val screen = if (activeTab.url == "home") Screen.HOME else Screen.WEBVIEW
                    _uiState.update {
                        it.copy(
                            tabs = tabs,
                            activeTabId = activeTab.id,
                            webViewUrl = if (screen == Screen.WEBVIEW) activeTab.url else null,
                            currentScreen = screen
                        )
                    }
                }
            } catch (e: Exception) { }
        }
    }

    private fun persistTabs() {
        viewModelScope.launch {
            try {
                val arr = JSONArray()
                _uiState.value.tabs.filter { !it.incognito }.forEach { tab ->
                    arr.put(JSONObject().apply {
                        put("id", tab.id)
                        put("url", tab.url)
                        put("title", tab.title)
                    })
                }
                storage.save(StorageManager.SAVED_TABS, arr.toString())
                val activeTab = _uiState.value.tabs.find { it.id == _uiState.value.activeTabId }
                activeTab?.let { storage.save(StorageManager.ACTIVE_TAB_URL, it.url) }
            } catch (e: Exception) { }
        }
    }

    fun enterIncognito() {
        val id = ++tabIdCounter
        val newTabs = _uiState.value.tabs + TabInfo(id, "home", "Void Tab", incognito = true)
        _uiState.update { it.copy(isIncognito = true, currentScreen = Screen.INCOGNITO, tabs = newTabs, activeTabId = id) }
    }

    fun exitIncognito() {
        val tabs = _uiState.value.tabs.filter { !it.incognito }
        val activeId = tabs.lastOrNull()?.id ?: 1
        _uiState.update { it.copy(isIncognito = false, currentScreen = Screen.HOME, tabs = tabs, activeTabId = activeId) }
    }

    fun newTab() {
        val id = ++tabIdCounter
        val newTabs = _uiState.value.tabs + TabInfo(id, "home", "New Tab")
        _uiState.update { it.copy(tabs = newTabs, activeTabId = id, currentScreen = Screen.HOME, webViewUrl = null) }
        persistTabs()
    }

    fun closeTab(tabId: Int) {
        val tabs = _uiState.value.tabs.toMutableList()
        if (tabs.size <= 1) { showToast("Cannot close last tab"); return }
        tabs.removeAll { it.id == tabId }
        val activeId = if (_uiState.value.activeTabId == tabId) tabs.last().id else _uiState.value.activeTabId
        
        // Feature 3: If closing the tab with media playing, stop media
        val wasMediaTab = _uiState.value.mediaPlayingTabId == tabId
        
        _uiState.update { 
            it.copy(
                tabs = tabs, 
                activeTabId = activeId,
                isMediaPlaying = if (wasMediaTab) false else it.isMediaPlaying,
                mediaPlayingTabId = if (wasMediaTab) null else it.mediaPlayingTabId
            ) 
        }
        
        // Stop background service if closing the media tab
        if (wasMediaTab) {
            // The actual service stop is handled by MainActivity
            _uiState.update { it.copy(isMediaPlaying = false, mediaPlayingTabId = null) }
        }
        
        persistTabs()
    }

    fun switchToTab(tabId: Int) {
        val tab = _uiState.value.tabs.find { it.id == tabId } ?: return
        if (tab.incognito) {
            _uiState.update { it.copy(activeTabId = tabId, isIncognito = true, currentScreen = Screen.INCOGNITO) }
        } else {
            if (tab.url == "home") {
                _uiState.update { it.copy(activeTabId = tabId, isIncognito = false, currentScreen = Screen.HOME, webViewUrl = null) }
            } else {
                _uiState.update { it.copy(activeTabId = tabId, isIncognito = false, currentScreen = Screen.WEBVIEW, webViewUrl = tab.url) }
            }
        }
    }

    fun updateTabInfo(url: String, title: String) {
        val tabs = _uiState.value.tabs.toMutableList()
        val idx = tabs.indexOfFirst { it.id == _uiState.value.activeTabId }
        if (idx >= 0) {
            tabs[idx] = tabs[idx].copy(url = url, title = title)
            _uiState.update { it.copy(tabs = tabs) }
            persistTabs()
        }
    }

    fun setActiveSearchTab(tab: String) {
        _uiState.update { it.copy(activeSearchTab = tab) }
    }

    // Section 38: Fullscreen state
    fun setVideoFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isVideoFullscreen = fullscreen) }
    }

    // Set fullscreen state to hide/show bottom nav
    fun setFullscreen(isFullscreen: Boolean) {
        _uiState.update { it.copy(isInFullscreen = isFullscreen) }
    }

    // ── QUICK SITES ──
    fun removeQuickSite(index: Int) {
        viewModelScope.launch {
            val sites = _uiState.value.quickSites.toMutableList()
            if (index in sites.indices) {
                sites.removeAt(index)
                storage.save(StorageManager.QUICK_SITES, quickSitesToJson(sites))
            }
        }
    }

    fun addQuickSite(url: String, label: String) {
        viewModelScope.launch {
            val sites = _uiState.value.quickSites.toMutableList()
            val normalizedUrl = if (url.startsWith("http")) url else "https://$url"
            val name = label.ifBlank {
                try { java.net.URI(normalizedUrl).host?.replace("www.", "") ?: url.take(15) } catch (_: Exception) { url.take(15) }
            }
            sites.add(QuickSite(normalizedUrl, name))
            storage.save(StorageManager.QUICK_SITES, quickSitesToJson(sites))
            showToast("$name added")
        }
    }

    // ── BOOKMARKS ──
    fun addBookmarkCurrent() {
        val tab = _uiState.value.tabs.find { it.id == _uiState.value.activeTabId }
        if (tab == null || tab.url == "home") { showToast("No page to bookmark"); return }
        addBookmark(tab.url, tab.title)
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch {
            val bookmarks = _uiState.value.bookmarks.toMutableList()
            if (bookmarks.any { it.url == url }) { showToast("Already bookmarked"); return@launch }
            bookmarks.add(BookmarkItem(url, title.ifBlank { url }, System.currentTimeMillis()))
            storage.save(StorageManager.BOOKMARKS, bookmarksToJson(bookmarks))
            
            // Sync to Supabase cloud
            supabaseDb.addBookmark(url, title.ifBlank { url })
            
            showToast("★ Bookmarked")
        }
    }

    fun removeBookmark(url: String) {
        viewModelScope.launch {
            val bookmarks = _uiState.value.bookmarks.filter { it.url != url }
            storage.save(StorageManager.BOOKMARKS, bookmarksToJson(bookmarks))
            showToast("Removed")
        }
    }

    // ── HISTORY ──
    fun addToHistory(url: String, title: String) {
        if (url.startsWith("file://") || _uiState.value.isIncognito) return
        viewModelScope.launch {
            val history = _uiState.value.history.toMutableList()
            history.removeAll { it.url == url }
            history.add(0, HistoryItem(url, title.ifBlank { url }, System.currentTimeMillis()))
            if (history.size > 150) history.subList(150, history.size).clear()
            storage.save(StorageManager.HISTORY, historyToJson(history))
            
            // Sync to Supabase cloud
            supabaseDb.addBrowserHistory(url, title.ifBlank { url })
        }
    }

    fun deleteHistoryItem(index: Int) {
        viewModelScope.launch {
            val history = _uiState.value.history.toMutableList()
            if (index in history.indices) {
                history.removeAt(index)
                storage.save(StorageManager.HISTORY, historyToJson(history))
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            storage.clearHistory()
            showToast("History cleared")
        }
    }

    fun clearAiMessages() {
        _uiState.update { it.copy(aiMessages = emptyList()) }
    }

    // ── SETTINGS TOGGLES ──
    // Section 31.3: setSearchEngine REMOVED — no search engine selection
    fun toggleParticles(on: Boolean) { viewModelScope.launch { storage.save(StorageManager.PARTICLES_ON, on) } }
    fun toggleStars(on: Boolean) { viewModelScope.launch { storage.save(StorageManager.STARS_ON, on) } }
    fun toggleWeather(on: Boolean) { viewModelScope.launch { storage.save(StorageManager.WEATHER_ON, on) } }
    fun toggleClock(on: Boolean) { viewModelScope.launch { storage.save(StorageManager.CLOCK_ON, on) } }
    fun toggleQuickSites(on: Boolean) { viewModelScope.launch { storage.save(StorageManager.QUICK_SITES_ON, on) } }
    fun toggleAi(on: Boolean) { viewModelScope.launch { storage.save(StorageManager.AI_ENABLED, on) } }
    fun toggleAdBlock(on: Boolean) { viewModelScope.launch { storage.save(StorageManager.AD_BLOCK_ON, on) } }
    fun setAccentColor(hex: String, lightHex: String) {
        viewModelScope.launch {
            storage.save(StorageManager.ACCENT_COLOR, hex)
            storage.save(StorageManager.ACCENT_COLOR_LIGHT, lightHex)
        }
    }
    fun setBgTheme(theme: String) { viewModelScope.launch { storage.save(StorageManager.BG_THEME, theme) } }
    fun markOnboardingDone() { viewModelScope.launch { storage.save(StorageManager.ONBOARDING_DONE, true) } }

    fun requestWebRefresh() {
        _uiState.update { it.copy(webRefreshNonce = it.webRefreshNonce + 1) }
    }

    fun saveCustomBackground(type: String, path: String, animate: Boolean) {
        viewModelScope.launch {
            storage.save(StorageManager.CUSTOM_BG_TYPE, type)
            storage.save(StorageManager.CUSTOM_BG_PATH, path)
            showToast(if (type == "video") "Video background applied" else "Image background applied")
            _uiState.update { it.copy(currentScreen = Screen.HOME) }
        }
    }

    fun clearCustomBackground() {
        viewModelScope.launch {
            storage.save(StorageManager.CUSTOM_BG_TYPE, "none")
            storage.save(StorageManager.CUSTOM_BG_PATH, "")
            showToast("Custom background removed")
        }
    }

    fun toggleExtension(id: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                val arr = JSONArray(_uiState.value.extensionsJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("id") == id) {
                        obj.put("isEnabled", enabled)
                        break
                    }
                }
                storage.save(StorageManager.EXTENSIONS, arr.toString())
                showToast(if (enabled) "Extension enabled" else "Extension disabled")
            } catch (_: Exception) {
                showToast("Failed to update extension")
            }
        }
    }

    fun removeExtension(id: String) {
        viewModelScope.launch {
            try {
                val arr = JSONArray(_uiState.value.extensionsJson)
                val updated = JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("id") != id) updated.put(obj)
                }
                storage.save(StorageManager.EXTENSIONS, updated.toString())
                showToast("Extension removed")
            } catch (_: Exception) {
                showToast("Failed to remove extension")
            }
        }
    }

    fun installExtension(script: DiscoverScript) {
        viewModelScope.launch {
            try {
                val arr = JSONArray(_uiState.value.extensionsJson)
                val id = "gf_${script.id}"
                val exists = (0 until arr.length()).any { i -> arr.getJSONObject(i).optString("id") == id }
                if (exists) {
                    showToast("Already installed")
                    return@launch
                }

                // Fetch actual script code from GreasyFork
                var scriptCode = "(function(){console.log('Eclipse extension active: ${'$'}{script.name}');})();"
                try {
                    val codeUrl = "https://greasyfork.org/scripts/${script.id}/code.json"
                    val codeResponse = withContext(Dispatchers.IO) {
                        val request = Request.Builder()
                            .url(codeUrl)
                            .get()
                            .build()
                        client.newCall(request).execute()
                    }
                    if (codeResponse.isSuccessful) {
                        val codeBody = codeResponse.body?.string()
                        if (codeBody != null) {
                            val codeJson = JSONObject(codeBody)
                            scriptCode = codeJson.optString("code", scriptCode)
                        }
                    }
                } catch (_: Exception) {
                    // Use placeholder if fetch fails
                }

                arr.put(
                    JSONObject().apply {
                        put("id", id)
                        put("name", script.name)
                        put("description", script.description)
                        put("matchRules", JSONArray().put("*://*/*"))
                        put("scriptCode", scriptCode)
                        put("isEnabled", true)
                        put("installedAt", System.currentTimeMillis())
                        put("source", "greasyfork")
                    }
                )

                storage.save(StorageManager.EXTENSIONS, arr.toString())
                showToast("Extension installed ✓")
            } catch (_: Exception) {
                showToast("Install failed")
            }
        }
    }

    // ── SHEETS ──
    fun showSheet(sheet: String) {
        _uiState.update {
            when (sheet) {
                "customize"  -> it.copy(showCustomize = true)
                "tabs"       -> it.copy(showTabs = true)
                "menu"       -> it.copy(showMenu = true)
                "history"    -> it.copy(showHistory = true)
                "bookmarks"  -> it.copy(showBookmarks = true)
                "about"      -> it.copy(showAbout = true)
                "adpanel"    -> it.copy(showAdPanel = true)
                else -> it
            }
        }
    }

    fun hideSheet(sheet: String) {
        _uiState.update {
            when (sheet) {
                "customize"  -> it.copy(showCustomize = false)
                "tabs"       -> it.copy(showTabs = false)
                "menu"       -> it.copy(showMenu = false)
                "history"    -> it.copy(showHistory = false)
                "bookmarks"  -> it.copy(showBookmarks = false)
                "about"      -> it.copy(showAbout = false)
                "adpanel"    -> it.copy(showAdPanel = false)
                else -> it
            }
        }
    }

    // ── TOAST ──
    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
        viewModelScope.launch {
            delay(2200)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    // ── APP LIFECYCLE / STABILITY — Section 35 ──
    fun onAppPaused() {
        persistSessionState()
    }

    fun onAppStopped() {
        persistSessionState()
        // Feature 3/4/5: Stop background service when app is stopped
        // (Note: The actual service stop is handled in MainActivity.onTrimMemory)
    }

    fun onAppResumed() {
        if (_uiState.value.tabs.isEmpty()) {
            restoreTabsFromStorage()
        }
        if (_uiState.value.aiMessages.isEmpty()) {
            restoreAiHistoryFromStorage()
        }
        // Feature 3: Do NOT auto-resume media on cold start - this is handled by not restoring playback state
    }

    // Feature 3/4/5: Track media playback state with tab ID
    fun setMediaPlaying(isPlaying: Boolean, tabId: Int? = null) {
        _uiState.update { 
            it.copy(
                isMediaPlaying = isPlaying,
                mediaPlayingTabId = if (isPlaying) (tabId ?: it.activeTabId) else null
            ) 
        }
    }

    // Feature 3: Stop media playback
    fun stopMediaPlayback() {
        _uiState.update { 
            it.copy(
                isMediaPlaying = false,
                mediaPlayingTabId = null
            ) 
        }
    }

    fun applySecurityRestrictions() {
        _uiState.update { it.copy(aiEnabled = false) }
        viewModelScope.launch {
            storage.save(StorageManager.AI_ENABLED, false)
        }
        showToast("Security warning: sensitive features restricted")
    }

    private fun persistSessionState() {
        persistTabs()
        persistAiHistory()
    }

    private fun startAiHistoryAutoSave() {
        viewModelScope.launch {
            _uiState
                .map { it.aiMessages }
                .distinctUntilChanged()
                .collect { persistAiHistory() }
        }
    }

    private fun persistAiHistory() {
        viewModelScope.launch {
            try {
                val arr = JSONArray()
                _uiState.value.aiMessages.takeLast(80).forEach { m ->
                    arr.put(
                        JSONObject().apply {
                            put("role", m.role)
                            put("content", m.content)
                            put("timestamp", m.timestamp)
                            put("attachmentType", m.attachmentType ?: "")
                            put("imageBase64", m.imageBase64 ?: "")
                        }
                    )
                }
                storage.save(StorageManager.AI_CHAT_HISTORY, arr.toString())
            } catch (_: Exception) { }
        }
    }

    private fun restoreAiHistoryFromStorage() {
        viewModelScope.launch {
            try {
                val raw = storage.aiChatHistory.first()
                if (raw.isBlank() || raw == "[]") return@launch
                val arr = JSONArray(raw)
                val restored = mutableListOf<AiMessage>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    restored.add(
                        AiMessage(
                            role = obj.optString("role", "assistant"),
                            content = obj.optString("content", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            attachmentType = obj.optString("attachmentType", "").ifBlank { null },
                            imageBase64 = obj.optString("imageBase64", "").ifBlank { null }
                        )
                    )
                }
                if (restored.isNotEmpty()) {
                    _uiState.update { it.copy(aiMessages = restored) }
                }
            } catch (_: Exception) { }
        }
    }

    override fun onCleared() {
        persistSessionState()
        super.onCleared()
    }

    // ── AD TRACKING ──
    fun onAdBlocked(domain: String) {
        val trackerDomains = setOf(
            "google-analytics.com","analytics.yahoo.com","facebook.com/tr",
            "connect.facebook.net","analytics.twitter.com","hotjar.com",
            "mouseflow.com","fullstory.com","mixpanel.com","segment.com",
            "amplitude.com","newrelic.com","nr-data.net","sentry.io",
            "scorecardresearch.com","quantserve.com","moatads.com",
            "doubleverify.com","adsafeprotected.com"
        )
        viewModelScope.launch {
            val ads = _uiState.value.adsBlocked + 1
            storage.save(StorageManager.ADS_BLOCKED, ads)
            if (trackerDomains.any { domain.contains(it) }) {
                val trackers = _uiState.value.trackersBlocked + 1
                storage.save(StorageManager.TRACKERS_BLOCKED, trackers)
            }
        }
    }

    // ── PARSING HELPERS ──
    private fun parseColor(hex: String): Color {
        return try { Color(android.graphics.Color.parseColor(hex)) }
        catch (_: Exception) { Color(0xFFFF6B1A) }
    }

    private fun parseQuickSites(json: String): List<QuickSite> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                QuickSite(obj.getString("url"), obj.getString("label"))
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseBookmarks(json: String): List<BookmarkItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                BookmarkItem(obj.getString("url"), obj.optString("title", obj.getString("url")), obj.optLong("time", 0))
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun parseHistory(json: String): List<HistoryItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                HistoryItem(obj.getString("url"), obj.optString("title", obj.getString("url")), obj.optLong("time", 0))
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun quickSitesToJson(sites: List<QuickSite>): String {
        val arr = JSONArray()
        sites.forEach { s -> arr.put(JSONObject().put("url", s.url).put("label", s.label)) }
        return arr.toString()
    }

    private fun bookmarksToJson(items: List<BookmarkItem>): String {
        val arr = JSONArray()
        items.forEach { b -> arr.put(JSONObject().put("url", b.url).put("title", b.title).put("time", b.time)) }
        return arr.toString()
    }

    private fun historyToJson(items: List<HistoryItem>): String {
        val arr = JSONArray()
        items.forEach { h -> arr.put(JSONObject().put("url", h.url).put("title", h.title).put("time", h.time)) }
        return arr.toString()
    }
}
