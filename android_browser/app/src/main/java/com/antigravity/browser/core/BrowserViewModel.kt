package com.antigravity.browser.core

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.browser.BrowserApp
import com.antigravity.browser.data.ai.ApiKeyManager
import com.antigravity.browser.data.ai.GeminiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val browserApp = application as BrowserApp
    private val apiKeyManager = browserApp.apiKeyManager
    private val browserEngine = BrowserEngine(application)
    private val extensionManager = ExtensionManager(application, browserEngine.getRuntime())
    
    // State
    private val _currentUrl = MutableStateFlow("https://www.google.com")
    val currentUrl = _currentUrl.asStateFlow()
    
    private val _tabs = MutableStateFlow<List<GeckoSession>>(emptyList())
    val tabs = _tabs.asStateFlow()
    
    private val _activeSession = MutableStateFlow<GeckoSession?>(null)
    val activeSession = _activeSession.asStateFlow()

    private val _activeKeyIndex = MutableStateFlow(0)
    val activeKeyIndex = _activeKeyIndex.asStateFlow()

    init {
        extensionManager.installExtensions()
        createNewTab()
        refreshActiveKeyIndex()
    }

    private fun refreshActiveKeyIndex() {
        viewModelScope.launch {
            _activeKeyIndex.value = apiKeyManager.getActiveKeyIndex()
        }
    }

    fun setManualKey(index: Int) {
        viewModelScope.launch {
            apiKeyManager.setManualKey(index)
            refreshActiveKeyIndex()
        }
    }

    fun createNewTab(url: String = "https://www.google.com") {
        val session = browserEngine.createSession()
        session.loadUri(url)
        
        val currentTabs = _tabs.value.toMutableList()
        currentTabs.add(session)
        _tabs.value = currentTabs
        
        _activeSession.value = session
    }

    fun closeTab(session: GeckoSession) {
        session.close()
        val currentTabs = _tabs.value.toMutableList()
        currentTabs.remove(session)
        _tabs.value = currentTabs
        
        if (_activeSession.value == session) {
            _activeSession.value = currentTabs.lastOrNull()
        }
    }

    fun loadUrl(url: String) {
        _activeSession.value?.loadUri(url)
        _currentUrl.value = url
    }

    // AI Commands
    fun processAiCommand(prompt: String) {
        viewModelScope.launch {
            // 1. Get Active Key
            var apiKey = apiKeyManager.getActiveKey()
            if (apiKey == null) {
                // Notify user to add key
                return@launch
            }

            // 2. Call Gemini API with Retry Logic
            var success = false
            var attempt = 0
            while (attempt < 3 && !success) {
                try {
                    // Simulate API Call
                    // val response = GeminiClient.generateResponse(apiKey, prompt)
                    success = true
                } catch (e: Exception) {
                    attempt++
                    if (attempt < 3) {
                        kotlinx.coroutines.delay(8000) // 8 seconds delay
                    } else {
                        // All 3 retries failed, report error and switch key
                        apiKeyManager.reportError(true)
                        // Optional: Try once more with new key or just stop
                    }
                }
            }
            
            if (!success) return@launch

            // 3. Parse Response and Act
            when {
                prompt.contains("open", ignoreCase = true) -> {
                    val url = prompt.substringAfter("open").trim()
                    loadUrl(if (url.startsWith("http")) url else "https://$url")
                }
                prompt.contains("speed", ignoreCase = true) -> {
                    // Extract number
                    val speed = prompt.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 1.0f
                    extensionManager.setVideoSpeed(speed)
                }
                prompt.contains("close tab", ignoreCase = true) -> {
                    _activeSession.value?.let { closeTab(it) }
                }
            }
        }
    }
}
