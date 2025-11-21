package com.antigravity.browser.data.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,
    val isActive: Boolean = false,
    val lastUsedTimestamp: Long = 0,
    val errorCount: Int = 0,
    val activatedDate: Long = 0 // For 35-day rule
)
