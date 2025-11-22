package com.antigravity.browser.data.ai

import android.util.Log
import com.antigravity.browser.data.db.ApiKeyDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ApiKeyManager(private val apiKeyDao: ApiKeyDao) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG = "ApiKeyManager"
    
    // Rate limiters for each key (keyed by API key string)
    private val rateLimiters = mutableMapOf<String, RateLimitTracker>()
    
    // Current model being used
    private val _currentModel = MutableStateFlow(GeminiClient.MODEL_FLASH_THINKING)
    val currentModel = _currentModel.asStateFlow()
    
    // Usage stats
    private val _usageStats = MutableStateFlow<UsageStats?>(null)
    val usageStats = _usageStats.asStateFlow()

    /**
     * Get the active API key with all safety checks
     */
    suspend fun getActiveKey(): String? {
        return withContext(Dispatchers.IO) {
            val activeKeyEntity = apiKeyDao.getActiveKey()
            
            if (activeKeyEntity == null) {
                Log.w(TAG, "No active API key found")
                return@withContext null
            }
            
            // Check 35-day rule
            if (activeKeyEntity.activatedDate > 0) {
                val daysActive = TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - activeKeyEntity.activatedDate
                )
                if (daysActive >= 35) {
                    Log.i(TAG, "35 days passed, reverting to primary key")
                    revertToPrimary()
                    return@withContext apiKeyDao.getActiveKey()?.key
                }
            }
            
            // Get or create rate limiter for this key
            val limiter = getRateLimiter(activeKeyEntity.key)
            _usageStats.value = limiter.getUsageStats()
            
            activeKeyEntity.key
        }
    }
    
    /**
     * Check if current key can make a request, auto-rotate if needed
     */
    suspend fun canMakeRequest(estimatedTokens: Int = 1000): Boolean {
        return withContext(Dispatchers.IO) {
            val activeKey = getActiveKey() ?: return@withContext false
            val limiter = getRateLimiter(activeKey)
            
            if (!limiter.canMakeRequest(estimatedTokens)) {
                Log.w(TAG, "Rate limit reached, attempting to switch key")
                // Try to switch to next key
                reportError(isRateLimit = true)
                // Check if new key can make request
                val newKey = getActiveKey() ?: return@withContext false
                getRateLimiter(newKey).canMakeRequest(estimatedTokens)
            } else {
                true
            }
        }
    }
    
    /**
     * Record a request after it's made
     */
    suspend fun recordRequest(tokensUsed: Int) {
        withContext(Dispatchers.IO) {
            val activeKey = getActiveKey() ?: return@withContext
            val limiter = getRateLimiter(activeKey)
            limiter.recordRequest(tokensUsed)
            _usageStats.value = limiter.getUsageStats()
        }
    }

    suspend fun getActiveKeyIndex(): Int {
        return withContext(Dispatchers.IO) {
            val active = apiKeyDao.getActiveKey() ?: return@withContext -1
            val all = apiKeyDao.getAllKeys()
            all.indexOfFirst { it.id == active.id }
        }
    }

    suspend fun setManualKey(index: Int) {
        withContext(Dispatchers.IO) {
            val all = apiKeyDao.getAllKeys()
            if (index in all.indices) {
                activateKey(all[index])
                // Reset rate limiter for newly activated key
                getRateLimiter(all[index].key).reset()
            }
        }
    }

    suspend fun reportError(isRateLimit: Boolean) {
        withContext(Dispatchers.IO) {
            val current = apiKeyDao.getActiveKey() ?: return@withContext
            
            Log.w(TAG, "Error reported for key (rate limit: $isRateLimit)")
            
            // Update error count
            val updatedCurrent = current.copy(errorCount = current.errorCount + 1)
            apiKeyDao.updateKey(updatedCurrent)
            
            // Switch to next key
            val allKeys = apiKeyDao.getAllKeys()
            val currentIndex = allKeys.indexOfFirst { it.id == current.id }
            
            if (currentIndex != -1 && currentIndex < allKeys.size - 1) {
                val nextKey = allKeys[currentIndex + 1]
                Log.i(TAG, "Switching to next key (index ${currentIndex + 1})")
                activateKey(nextKey)
            } else {
                Log.e(TAG, "All keys exhausted or last key failed")
            }
        }
    }
    
    /**
     * Get all API keys
     */
    suspend fun getAllKeys(): List<ApiKeyEntity> {
        return withContext(Dispatchers.IO) {
            apiKeyDao.getAllKeys()
        }
    }
    
    /**
     * Check if any keys are configured
     */
    suspend fun hasKeys(): Boolean {
        return withContext(Dispatchers.IO) {
            apiKeyDao.getAllKeys().isNotEmpty()
        }
    }

    private suspend fun activateKey(key: ApiKeyEntity) {
        apiKeyDao.deactivateAll()
        val updatedKey = key.copy(
            isActive = true, 
            activatedDate = System.currentTimeMillis(),
            errorCount = 0
        )
        apiKeyDao.updateKey(updatedKey)
        
        // Reset rate limiter for activated key
        getRateLimiter(key.key).reset()
    }

    private suspend fun revertToPrimary() {
        val allKeys = apiKeyDao.getAllKeys()
        if (allKeys.isNotEmpty()) {
            activateKey(allKeys[0])
        }
    }

    suspend fun addKey(keyString: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val existingKeys = apiKeyDao.getAllKeys()
                
                // Limit to 5 keys
                if (existingKeys.size >= 5) {
                    Log.w(TAG, "Cannot add more than 5 API keys")
                    return@withContext false
                }
                
                // Check for duplicates
                if (existingKeys.any { it.key == keyString }) {
                    Log.w(TAG, "API key already exists")
                    return@withContext false
                }
                
                val newKey = ApiKeyEntity(key = keyString)
                apiKeyDao.insertKey(newKey)
                
                // If it's the first key, make it active
                if (existingKeys.isEmpty()) {
                    activateKey(newKey)
                }
                
                Log.i(TAG, "API key added successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error adding API key", e)
                false
            }
        }
    }
    
    /**
     * Remove a key by index
     */
    suspend fun removeKey(index: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val allKeys = apiKeyDao.getAllKeys()
                if (index in allKeys.indices) {
                    val keyToRemove = allKeys[index]
                    val wasActive = keyToRemove.isActive
                    
                    apiKeyDao.deleteKey(keyToRemove)
                    rateLimiters.remove(keyToRemove.key)
                    
                    // If removed key was active, activate the first available key
                    if (wasActive) {
                        val remainingKeys = apiKeyDao.getAllKeys()
                        if (remainingKeys.isNotEmpty()) {
                            activateKey(remainingKeys[0])
                        }
                    }
                    
                    Log.i(TAG, "API key removed successfully")
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing API key", e)
                false
            }
        }
    }
    
    /**
     * Switch model (Flash or Pro)
     */
    fun switchModel(modelName: String) {
        _currentModel.value = when (modelName.lowercase()) {
            "flash", "2.5 flash", "gemini 2.5 flash" -> GeminiClient.MODEL_FLASH_THINKING
            "pro", "2.5 pro", "gemini 2.5 pro" -> GeminiClient.MODEL_PRO
            "flash-thinking" -> GeminiClient.MODEL_FLASH_THINKING
            else -> GeminiClient.MODEL_FLASH_THINKING
        }
        GeminiClient.switchModel(modelName)
        Log.i(TAG, "Switched to model: ${_currentModel.value}")
    }
    
    /**
     * Get or create rate limiter for a key
     */
    private fun getRateLimiter(apiKey: String): RateLimitTracker {
        return rateLimiters.getOrPut(apiKey) {
            // Default to Flash limits, adjust based on current model
            val config = if (_currentModel.value == GeminiClient.MODEL_PRO) {
                RateLimitConfig.PRO
            } else {
                RateLimitConfig.FLASH
            }
            RateLimitTracker(config)
        }
    }
}
