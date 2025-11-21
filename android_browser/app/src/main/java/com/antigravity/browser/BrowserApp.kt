package com.antigravity.browser

import android.app.Application
import com.antigravity.browser.data.ai.ApiKeyManager
import com.antigravity.browser.data.db.BrowserDatabase

class BrowserApp : Application() {
    
    lateinit var database: BrowserDatabase
    lateinit var apiKeyManager: ApiKeyManager

    override fun onCreate() {
        super.onCreate()
        database = androidx.room.Room.databaseBuilder(
            applicationContext,
            BrowserDatabase::class.java, "browser-db"
        ).build()
        
        apiKeyManager = ApiKeyManager(database.apiKeyDao())
    }
}
