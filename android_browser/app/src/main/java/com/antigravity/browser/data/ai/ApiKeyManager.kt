package com.antigravity.browser.data.ai

import com.antigravity.browser.data.db.ApiKeyDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ApiKeyManager(private val apiKeyDao: ApiKeyDao) {

    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Limits for Gemini 2.5 Flash
    private val RPM_LIMIT_FLASH = 8
    private val TPM_LIMIT_FLASH = 230000
    private val RPD_LIMIT_FLASH = 230

    // Current usage counters (reset periodically in a real app, simplified here)
    private var currentRpm = 0
    private var currentTpm = 0
    private var currentRpd = 0
    private var lastRequestTime = 0L

    suspend fun getActiveKey(): String? {
        return withContext(Dispatchers.IO) {
            val activeKeyEntity = apiKeyDao.getActiveKey() ?: return@withContext null
            
            // Check 35-day rule
            if (activeKeyEntity.activatedDate > 0) {
                val daysActive = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - activeKeyEntity.activatedDate)
                if (daysActive >= 35) {
                    // Try to revert to Key 1 (ID 1)
                    revertToPrimary()
                    return@withContext apiKeyDao.getActiveKey()?.key
                }
            }
            
            activeKeyEntity.key
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
            }
        }
    }

    suspend fun reportError(isRateLimit: Boolean) {
        withContext(Dispatchers.IO) {
            val current = apiKeyDao.getActiveKey() ?: return@withContext
            
            // If error, switch to next key
            val allKeys = apiKeyDao.getAllKeys()
            val currentIndex = allKeys.indexOfFirst { it.id == current.id }
            
            if (currentIndex != -1 && currentIndex < allKeys.size - 1) {
                val nextKey = allKeys[currentIndex + 1]
                activateKey(nextKey)
            } else {
                // All keys exhausted or last key failed
                // In a real app, we might cycle back or notify user
            }
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
    }

    private suspend fun revertToPrimary() {
        val allKeys = apiKeyDao.getAllKeys()
        if (allKeys.isNotEmpty()) {
            activateKey(allKeys[0])
        }
    }

    suspend fun addKey(keyString: String) {
        withContext(Dispatchers.IO) {
            val newKey = ApiKeyEntity(key = keyString)
            apiKeyDao.insertKey(newKey)
            // If it's the first key, make it active
            if (apiKeyDao.getAllKeys().size == 1) {
                activateKey(newKey)
            }
        }
    }
}
