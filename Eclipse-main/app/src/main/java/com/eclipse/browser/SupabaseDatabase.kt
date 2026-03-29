package com.eclipse.browser

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseManager {
    private const val SUPABASE_URL = "https://hjdaxklnvwehiehpolpv.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_hx_35e50iVP5obhWdazlkA_iPcg_4zT"
    
    val anonKey = SUPABASE_KEY
    val baseUrl = SUPABASE_URL
    
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}

class SupabaseDatabase(private val context: Context) {
    
    private val client = SupabaseManager.client
    private val baseUrl = SupabaseManager.baseUrl
    private val apiKey = SupabaseManager.anonKey
    
    private fun createRequest(endpoint: String, method: String, body: String? = null): Request {
        val url = "$baseUrl/rest/v1/$endpoint"
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
        
        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> {
                val mediaType = "application/json".toMediaType()
                val requestBody = body?.let { RequestBody.create(mediaType, it) }
                requestBuilder.post(requestBody!!)
            }
            "PATCH" -> {
                val mediaType = "application/json".toMediaType()
                val requestBody = body?.let { RequestBody.create(mediaType, it) }
                requestBuilder.patch(requestBody!!)
            }
            "DELETE" -> requestBuilder.delete()
        }
        
        return requestBuilder.build()
    }
    
    // Browser History
    suspend fun addBrowserHistory(url: String, title: String, deviceName: String = "android"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("url", url)
                put("title", title)
                put("device_name", deviceName)
            }.toString()
            
            val request = createRequest("browser_history", "POST", body)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Added to history")
                } else {
                    Result.failure(Exception("Error: ${response.code} - ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBrowserHistory(limit: Int = 100): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val request = createRequest("browser_history?select=*&order=visited_at.desc&limit=$limit", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    val results = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val map = mutableMapOf<String, Any>()
                        obj.keys().forEach { key -> map[key] = obj.get(key) }
                        results.add(map)
                    }
                    Result.success(results)
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Bookmarks
    suspend fun addBookmark(url: String, title: String, faviconUrl: String? = null, folder: String = "default"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("url", url)
                put("title", title)
                put("favicon_url", faviconUrl ?: "")
                put("folder", folder)
            }.toString()
            
            val request = createRequest("bookmarks", "POST", body)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Bookmark added")
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBookmarks(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val request = createRequest("bookmarks?select=*&order=created_at.desc", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    val results = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val map = mutableMapOf<String, Any>()
                        obj.keys().forEach { key -> map[key] = obj.get(key) }
                        results.add(map)
                    }
                    Result.success(results)
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteBookmark(id: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = createRequest("bookmarks?id=eq.$id", "DELETE")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Bookmark deleted")
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // User Settings
    suspend fun saveUserSettings(settings: Map<String, Any>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject(settings).toString()
            val request = createRequest("user_settings", "POST", body)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Settings saved")
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserSettings(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val request = createRequest("user_settings?select=*&limit=1", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    val results = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val map = mutableMapOf<String, Any>()
                        obj.keys().forEach { key -> map[key] = obj.get(key) }
                        results.add(map)
                    }
                    Result.success(results)
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Search History
    suspend fun addSearchHistory(query: String, category: String = "general"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("query", query)
                put("category", category)
            }.toString()
            
            val request = createRequest("search_history", "POST", body)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Search saved")
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSearchHistory(limit: Int = 50): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val request = createRequest("search_history?select=*&order=searched_at.desc&limit=$limit", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    val results = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val map = mutableMapOf<String, Any>()
                        obj.keys().forEach { key -> map[key] = obj.get(key) }
                        results.add(map)
                    }
                    Result.success(results)
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Open Tabs
    suspend fun saveOpenTabs(tabs: List<Map<String, Any>>): Result<String> = withContext(Dispatchers.IO) {
        try {
            // First delete existing tabs
            val deleteRequest = createRequest("open_tabs", "DELETE")
            client.newCall(deleteRequest).execute()
            
            // Then insert new tabs
            tabs.forEach { tab ->
                val body = JSONObject(tab).toString()
                val request = createRequest("open_tabs", "POST", body)
                client.newCall(request).execute()
            }
            Result.success("Tabs saved")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOpenTabs(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val request = createRequest("open_tabs?select=*&order=tab_order", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    val results = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val map = mutableMapOf<String, Any>()
                        obj.keys().forEach { key -> map[key] = obj.get(key) }
                        results.add(map)
                    }
                    Result.success(results)
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // AI Chat History
    suspend fun addChatMessage(sessionId: String, role: String, content: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("session_id", sessionId)
                put("role", role)
                put("content", content)
            }.toString()
            
            val request = createRequest("ai_chat_history", "POST", body)
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Message saved")
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChatHistory(sessionId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val request = createRequest("ai_chat_history?select=*&session_id=eq.$sessionId&order=created_at", "GET")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    val results = mutableListOf<Map<String, Any>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val map = mutableMapOf<String, Any>()
                        obj.keys().forEach { key -> map[key] = obj.get(key) }
                        results.add(map)
                    }
                    Result.success(results)
                } else {
                    Result.failure(Exception("Error: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
