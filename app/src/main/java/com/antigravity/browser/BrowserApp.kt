package com.antigravity.browser

import android.app.Application
import com.antigravity.browser.core.GeckoEngine
import com.antigravity.browser.data.GeminiRepository

class BrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Gecko Engine
        GeckoEngine.initialize(this)
        // Initialize Gemini Repository
        GeminiRepository.initialize(this)
    }
}
