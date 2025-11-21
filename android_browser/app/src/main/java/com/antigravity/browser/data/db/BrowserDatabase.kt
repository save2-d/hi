package com.antigravity.browser.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.antigravity.browser.data.ai.ApiKeyEntity

@Database(entities = [ApiKeyEntity::class], version = 1)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun apiKeyDao(): ApiKeyDao
}
