package com.antigravity.browser.data

import android.content.Context
import android.content.SharedPreferences
import com.antigravity.browser.data.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object GeminiRepository {
    private const val PREFS_NAME = "gemini_prefs"
    private const val KEY_API_KEYS = "api_keys"
    private const val KEY_CURRENT_INDEX = "current_key_index"
    private const val KEY_LAST_ROTATION = "last_rotation_timestamp"
    
    // Models
    const val MODEL_FLASH = "gemini-2.5-flash"
    const val MODEL_PRO = "gemini-2.5-pro"

    private lateinit var prefs: SharedPreferences
    private val _apiKeys = MutableStateFlow<List<String>>(emptyList())
    val apiKeys: StateFlow<List<String>> = _apiKeys

    private var currentKeyIndex = 0
    private var lastRotationTime = 0L
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        
    private val service = retrofit.create(GeminiService::class.java)

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadKeys()
    }

    private fun loadKeys() {
        val keysString = prefs.getString(KEY_API_KEYS, "") ?: ""
        if (keysString.isNotEmpty()) {
            _apiKeys.value = keysString.split(",").filter { it.isNotBlank() }
        }
        currentKeyIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
        lastRotationTime = prefs.getLong(KEY_LAST_ROTATION, 0L)
        
        checkRotationTime()
    }

    fun addApiKey(key: String) {
        val currentList = _apiKeys.value.toMutableList()
        if (currentList.size < 4) {
            currentList.add(key)
            saveKeys(currentList)
        }
    }
    
    fun removeApiKey(key: String) {
        val currentList = _apiKeys.value.toMutableList()
        currentList.remove(key)
        saveKeys(currentList)
    }

    private fun saveKeys(keys: List<String>) {
        _apiKeys.value = keys
        prefs.edit().putString(KEY_API_KEYS, keys.joinToString(",")).apply()
    }

    fun getCurrentKey(): String? {
        val keys = _apiKeys.value
        if (keys.isEmpty()) return null
        if (currentKeyIndex >= keys.size) currentKeyIndex = 0
        return keys[currentKeyIndex]
    }

    fun rotateKey(isError: Boolean = false) {
        val keys = _apiKeys.value
        if (keys.isEmpty()) return

        if (isError) {
            // Error-based rotation
            currentKeyIndex = (currentKeyIndex + 1) % keys.size
        } else {
            // Time-based rotation check
             checkRotationTime()
        }
        
        prefs.edit()
            .putInt(KEY_CURRENT_INDEX, currentKeyIndex)
            .putLong(KEY_LAST_ROTATION, System.currentTimeMillis())
            .apply()
    }
    
    private fun checkRotationTime() {
        val daysSinceRotation = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastRotationTime)
        if (daysSinceRotation >= 35) {
            // Cycle back to 0 after 35 days
            currentKeyIndex = 0
             prefs.edit()
                .putInt(KEY_CURRENT_INDEX, currentKeyIndex)
                .putLong(KEY_LAST_ROTATION, System.currentTimeMillis())
                .apply()
        }
    }
    
    suspend fun generateContent(prompt: String, model: String = MODEL_FLASH): String {
        val key = getCurrentKey() ?: return "Please add a Gemini API Key in settings."
        
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            val request = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt))))
            )
            
            val response = service.generateContent(key, url, request)
            return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response"
        } catch (e: Exception) {
            rotateKey(isError = true)
            // Retry with new key if available? For now just return error
            return "Error: ${e.message}. Key rotated."
        }
    }
}
