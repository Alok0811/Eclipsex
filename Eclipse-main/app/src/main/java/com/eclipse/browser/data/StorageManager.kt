package com.eclipse.browser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "eclipse_settings")

class StorageManager(private val context: Context) {

    companion object {
        // Section 31.3: SEARCH_ENGINE key REMOVED — SearXNG handles all search via backend
        // Do NOT add it back

        val BG_THEME            = stringPreferencesKey("bgTheme")
        val ACCENT_COLOR        = stringPreferencesKey("accentColor")
        val ACCENT_COLOR_LIGHT  = stringPreferencesKey("accentColorLight")
        val PARTICLES_ON        = booleanPreferencesKey("particlesOn")
        val STARS_ON            = booleanPreferencesKey("starsOn")
        // Section 25.1: ORB_ON kept for migration but orb is permanently removed from UI
        val ORB_ON              = booleanPreferencesKey("orbOn")
        val WEATHER_ON          = booleanPreferencesKey("weatherOn")
        // Section 25.8: compact/expanded (uiStyle) removed from CustomizeSheet UI
        // but key kept so existing user data is not corrupted
        val UI_STYLE            = stringPreferencesKey("uiStyle")
        val QUICK_SITES         = stringPreferencesKey("quickSites")
        val BOOKMARKS           = stringPreferencesKey("bookmarks")
        val HISTORY             = stringPreferencesKey("history")
        val AD_BLOCK_ON         = booleanPreferencesKey("adBlockOn")
        val ADS_BLOCKED         = intPreferencesKey("adsBlocked")
        val TRACKERS_BLOCKED    = intPreferencesKey("trackersBlocked")
        val CLOCK_ON            = booleanPreferencesKey("clockOn")
        val QUICK_SITES_ON      = booleanPreferencesKey("quickSitesOn")
        val ONBOARDING_DONE     = booleanPreferencesKey("onboardingDone")
        val LAST_LAT            = stringPreferencesKey("lastLat")
        val LAST_LON            = stringPreferencesKey("lastLon")
        val AI_ENABLED          = booleanPreferencesKey("aiEnabled")

        // Section 31.8: Device ID for backend rate limiting — consistent unique identifier
        val DEVICE_ID           = stringPreferencesKey("deviceId")

        // Section 8: AI response mode persisted across sessions
        val AI_RESPONSE_MODE    = stringPreferencesKey("aiResponseMode")

        // Section 35.5: Tab state persistence — open tabs saved as JSON
        val SAVED_TABS          = stringPreferencesKey("savedTabs")

        // Section 22: Active tab URL persisted for restore
        val ACTIVE_TAB_URL      = stringPreferencesKey("activeTabUrl")

        // Section 14: Custom background type (none/preset/image)
        val CUSTOM_BG_TYPE      = stringPreferencesKey("customBgType")
        val CUSTOM_BG_PATH      = stringPreferencesKey("customBgPath")

        // Section 29: Extensions stored as JSON string list
        val EXTENSIONS          = stringPreferencesKey("extensions")

        // Section 35.6: AI chat history persisted
        val AI_CHAT_HISTORY     = stringPreferencesKey("aiChatHistory")
    }

    // Section 31.3: NO searchEngine flow — removed completely
    val bgTheme: Flow<String>          = context.dataStore.data.map { it[BG_THEME] ?: "eclipse" }
    val accentColor: Flow<String>      = context.dataStore.data.map { it[ACCENT_COLOR] ?: "#FF6B1A" }
    val accentColorLight: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR_LIGHT] ?: "#FFB347" }
    val particlesOn: Flow<Boolean>     = context.dataStore.data.map { it[PARTICLES_ON] ?: true }
    val starsOn: Flow<Boolean>         = context.dataStore.data.map { it[STARS_ON] ?: true }
    val orbOn: Flow<Boolean>           = context.dataStore.data.map { it[ORB_ON] ?: false }
    val weatherOn: Flow<Boolean>       = context.dataStore.data.map { it[WEATHER_ON] ?: true }
    val uiStyle: Flow<String>          = context.dataStore.data.map { it[UI_STYLE] ?: "normal" }
    val quickSites: Flow<String>       = context.dataStore.data.map { it[QUICK_SITES] ?: DEFAULT_SITES }
    val bookmarks: Flow<String>        = context.dataStore.data.map { it[BOOKMARKS] ?: "[]" }
    val history: Flow<String>          = context.dataStore.data.map { it[HISTORY] ?: "[]" }
    val adBlockOn: Flow<Boolean>       = context.dataStore.data.map { it[AD_BLOCK_ON] ?: true }
    val adsBlocked: Flow<Int>          = context.dataStore.data.map { it[ADS_BLOCKED] ?: 0 }
    val trackersBlocked: Flow<Int>     = context.dataStore.data.map { it[TRACKERS_BLOCKED] ?: 0 }
    val clockOn: Flow<Boolean>         = context.dataStore.data.map { it[CLOCK_ON] ?: true }
    val quickSitesOn: Flow<Boolean>    = context.dataStore.data.map { it[QUICK_SITES_ON] ?: true }
    val onboardingDone: Flow<Boolean>  = context.dataStore.data.map { it[ONBOARDING_DONE] ?: false }
    val lastLat: Flow<String>          = context.dataStore.data.map { it[LAST_LAT] ?: "" }
    val lastLon: Flow<String>          = context.dataStore.data.map { it[LAST_LON] ?: "" }
    val aiEnabled: Flow<Boolean>       = context.dataStore.data.map { it[AI_ENABLED] ?: true }
    val deviceId: Flow<String>         = context.dataStore.data.map {
        val raw = it[DEVICE_ID] ?: ""
        StorageCrypto.decrypt(context, raw)
    }
    val aiResponseMode: Flow<String>   = context.dataStore.data.map { it[AI_RESPONSE_MODE] ?: "short" }
    val savedTabs: Flow<String>        = context.dataStore.data.map { it[SAVED_TABS] ?: "[]" }
    val activeTabUrl: Flow<String>     = context.dataStore.data.map { it[ACTIVE_TAB_URL] ?: "" }
    val customBgType: Flow<String>     = context.dataStore.data.map { it[CUSTOM_BG_TYPE] ?: "none" }
    val customBgPath: Flow<String>     = context.dataStore.data.map { it[CUSTOM_BG_PATH] ?: "" }
    val extensions: Flow<String>       = context.dataStore.data.map { it[EXTENSIONS] ?: DEFAULT_EXTENSIONS }
    val aiChatHistory: Flow<String>    = context.dataStore.data.map {
        val raw = it[AI_CHAT_HISTORY] ?: "[]"
        StorageCrypto.decrypt(context, raw)
    }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit {
            @Suppress("UNCHECKED_CAST")
            when {
                key == DEVICE_ID && value is String -> {
                    it[key] = StorageCrypto.encrypt(context, value) as T
                }
                key == AI_CHAT_HISTORY && value is String -> {
                    it[key] = StorageCrypto.encrypt(context, value) as T
                }
                else -> it[key] = value
            }
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it[HISTORY] = "[]" }
    }

    suspend fun clearBookmarks() {
        context.dataStore.edit { it[BOOKMARKS] = "[]" }
    }

    suspend fun clearAiHistory() {
        context.dataStore.edit { it[AI_CHAT_HISTORY] = "[]" }
    }
}

const val DEFAULT_SITES = """[
    {"url":"https://www.google.com","label":"Google"},
    {"url":"https://www.youtube.com","label":"YouTube"},
    {"url":"https://twitter.com","label":"Twitter"},
    {"url":"https://www.reddit.com","label":"Reddit"},
    {"url":"https://github.com","label":"GitHub"},
    {"url":"https://www.instagram.com","label":"Instagram"},
    {"url":"https://www.wikipedia.org","label":"Wikipedia"}
]"""

// Section 29: Pre-installed default extensions — these come with Eclipse
const val DEFAULT_EXTENSIONS = """[
    {
        "id":"ext_yt_adblock",
        "name":"YouTube AdBlock",
        "description":"Blocks all YouTube ads before they play",
        "matchRules":["*://www.youtube.com/*","*://youtube.com/*","*://m.youtube.com/*"],
        "scriptCode":"(function(){function blockAds(){var style=document.createElement('style');style.id='eclipse-adblock';style.textContent='.video-ads,.ytp-ad-module,.ytp-ad-overlay-container,.ytp-ad-text-overlay,.ad-showing .html5-video-player{display:none!important}body.ad-showing{overflow:auto!important}';if(!document.getElementById('eclipse-adblock')){document.head.appendChild(style);}}function skipAd(){var skipBtn=document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,[class*=skip-button]');if(skipBtn){skipBtn.click();}var video=document.querySelector('.ad-showing video');if(video){video.currentTime=video.duration||999999;}var overlay=document.querySelector('.ytp-ad-overlay-close-button');if(overlay){overlay.click();}}function init(){blockAds();skipAd();var observer=new MutationObserver(function(mutations){mutations.forEach(function(m){if(m.target.classList&&m.target.classList.contains('ad-showing')){skipAd();}m.addedNodes.forEach(function(node){if(node.classList&&(node.classList.contains('ad-showing')||node.classList.contains('ytp-ad'))){skipAd();}});});});if(document.body){observer.observe(document.body,{attributes:true,attributeFilter:['class'],childList:true,subtree:true});}setInterval(skipAd,500);}if(document.readyState==='loading'){document.addEventListener('DOMContentLoaded',init);}else{init();}})();",
        "isEnabled":true,
        "installedAt":0,
        "source":"default"
    },
    {
        "id":"ext_cookie_dismiss",
        "name":"Cookie Popup Dismisser",
        "description":"Automatically dismisses cookie consent banners",
        "matchRules":["*://*/*"],
        "scriptCode":"(function(){function dismiss(){var btns=document.querySelectorAll('[class*=accept],[class*=cookie],[id*=accept],[id*=cookie]');btns.forEach(function(b){if(b.tagName==='BUTTON'||b.tagName==='A'){b.click();}});}setTimeout(dismiss,1500);})();",
        "isEnabled":true,
        "installedAt":0,
        "source":"default"
    },
    {
        "id":"ext_dark_reader",
        "name":"Dark Reader Lite",
        "description":"Forces dark mode on bright websites",
        "matchRules":["*://*/*"],
        "scriptCode":"(function(){var style=document.createElement('style');style.textContent='html{filter:invert(1) hue-rotate(180deg)!important}img,video,canvas{filter:invert(1) hue-rotate(180deg)!important}';document.head.appendChild(style);})();",
        "isEnabled":false,
        "installedAt":0,
        "source":"default"
    },
    {
        "id":"ext_yt_dislike",
        "name":"Return YouTube Dislike",
        "description":"Shows dislike counts on YouTube videos",
        "matchRules":["*://www.youtube.com/*","*://youtube.com/*"],
        "scriptCode":"(function(){console.log('Return YouTube Dislike - Eclipse Extension active');})();",
        "isEnabled":true,
        "installedAt":0,
        "source":"default"
    }
]"""
