package com.antigravity.browser.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.antigravity.browser.data.ai.ApiKeyEntity

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY id ASC")
    suspend fun getAllKeys(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveKey(): ApiKeyEntity?

    @Insert
    suspend fun insertKey(apiKey: ApiKeyEntity)

    @Update
    suspend fun updateKey(apiKey: ApiKeyEntity)

    @Query("UPDATE api_keys SET isActive = 0")
    suspend fun deactivateAll()
}
