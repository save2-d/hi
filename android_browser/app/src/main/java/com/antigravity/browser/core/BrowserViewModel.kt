package com.antigravity.browser.core

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.browser.BrowserApp
import com.antigravity.browser.data.ai.ApiKeyManager
import com.antigravity.browser.data.ai.GeminiRequest
import com.antigravity.browser.data.ai.Content
import com.antigravity.browser.data.ai.TextPart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BrowserViewModel"
    private val browserApp = application as BrowserApp
    private val apiKeyManager = browserApp.apiKeyManager
    private val browserEngine = BrowserEngine(application)
    private val extensionManager = ExtensionManager(application, browserEngine.runtime)
    private val commandParser = CommandParser()
    private val browserAutomation = BrowserAutomation(application)
    
    // State
    private val _currentUrl = MutableStateFlow("https://www.google.com")
    val currentUrl = _currentUrl.asStateFlow()
    
    private val _tabs = MutableStateFlow<List<GeckoSession>>(emptyList())
    val tabs = _tabs.asStateFlow()
    
    private val _activeSession = MutableStateFlow<GeckoSession?>(null)
    val activeSession = _activeSession.asStateFlow()

    private val _activeKeyIndex = MutableStateFlow(0)
    val activeKeyIndex = _activeKeyIndex.asStateFlow()
    
    // AI chat state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()
    
    private val _isAiProcessing = MutableStateFlow(false)
    val isAiProcessing = _isAiProcessing.asStateFlow()
    
    private val _aiOverlayVisible = MutableStateFlow(false)
    val aiOverlayVisible = _aiOverlayVisible.asStateFlow()

    init {
        extensionManager.installExtensions()
        createNewTab()
        refreshActiveKeyIndex()
        // Observe usage stats
        viewModelScope.launch {
            apiKeyManager.usageStats.collect { stats ->
                Log.d(TAG, "Usage stats: $stats")
            }
        }
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

    /**
     * Process AI command from user input
     */
    fun processAiCommand(input: String) {
        viewModelScope.launch {
            try {
                _isAiProcessing.value = true
                addChatMessage(input, isUser = true)
                
                // Parse command
                val command = commandParser.parse(input)
                Log.d(TAG, "Parsed command: $command")
                
                // Execute direct browser commands
                when (command) {
                    is BrowserCommand.OpenUrl -> {
                        loadUrl(command.url)
                        addChatMessage("Opening ${command.url}", isUser = false)
                    }
                    is BrowserCommand.Search -> {
                        val searchUrl = "https://www.google.com/search?q=${command.query}"
                        loadUrl(searchUrl)
                        addChatMessage("Searching for: ${command.query}", isUser = false)
                    }
                    is BrowserCommand.CreateTab -> {
                        createNewTab()
                        addChatMessage("Created new tab", isUser = false)
                    }
                    is BrowserCommand.CloseTab -> {
                        val activeIndex = _tabs.value.indexOf(_activeSession.value)
                        if (activeIndex >= 0) {
                            closeTab(activeIndex)
                            addChatMessage("Closed tab", isUser = false)
                        }
                    }
                    is BrowserCommand.SwitchTab -> {
                        switchToTab(command.index)
                        addChatMessage("Switched to tab ${command.index + 1}", isUser = false)
                    }
                    is BrowserCommand.ToggleExtension -> {
                        extensionManager.toggleExtension(command.extension, command.enable)
                        val action = if (command.enable) "enabled" else "disabled"
                        addChatMessage("${command.extension} $action", isUser = false)
                    }
                    is BrowserCommand.SwitchModel -> {
                        apiKeyManager.switchModel(command.model)
                        addChatMessage("Switched to ${command.model} model", isUser = false)
                    }
                    else -> {
                        // For automation commands, execute on active session
                        val session = _activeSession.value
                        if (session != null) {
                            val success = browserAutomation.executeCommand(session, command)
                            if (success) {
                                addChatMessage("Command executed", isUser = false)
                            } else {
                                // If not a direct command, send to Gemini AI
                                handleAiChat(input)
                            }
                        } else {
                            handleAiChat(input)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing AI command", e)
                addChatMessage("Error: ${e.message}", isUser = false)
            } finally {
                _isAiProcessing.value = false
            }
        }
    }
    
    private suspend fun handleAiChat(query: String) {
        try {
            // TODO: Implement Gemini AI chat when needed
            // For now, just acknowledge
            addChatMessage("AI chat not yet implemented", isUser = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error in AI chat", e)
            addChatMessage("AI Error: ${e.message}", isUser = false)
        }
    }
    
    private fun addChatMessage(content: String, isUser: Boolean) {
        _chatMessages.value = _chatMessages.value + ChatMessage(content, isUser)
    }
    
    fun toggleAiOverlay() {
        _aiOverlayVisible.value = !_aiOverlayVisible.value
    }

    fun loadUrl(url: String) {
        _currentUrl.value = url
        val session = _activeSession.value
        if (session != null) {
            session.loadUri(url)
        }
    }

    fun reload() {
        _activeSession.value?.reload()
    }

    fun goBack() {
        _activeSession.value?.goBack()
    }

    fun goForward() {
        _activeSession.value?.goForward()
    }

    fun createNewTab() {
        val newSession = browserEngine.createSession()
        _tabs.value = _tabs.value + newSession
        switchToSession(newSession)
    }

    fun closeTab(index: Int) {
        val currentTabs = _tabs.value
        if (index in currentTabs.indices) {
            val tabToClose = currentTabs[index]
            tabToClose.close()
            _tabs.value = currentTabs.filterIndexed { i, _ -> i != index }
            
            if (_activeSession.value == tabToClose) {
                val newActiveIndex = (index - 1).coerceAtLeast(0)
                if (_tabs.value.isNotEmpty()) {
                    switchToSession(_tabs.value[newActiveIndex])
                } else {
                    createNewTab()
                }
            }
        }
    }

    fun switchToTab(index: Int) {
        val currentTabs = _tabs.value
        if (index in currentTabs.indices) {
            switchToSession(currentTabs[index])
        }
    }

    private fun switchToSession(session: GeckoSession) {
        _activeSession.value = session
    }

    override fun onCleared() {
        super.onCleared()
        _tabs.value.forEach { it.close() }
    }
}
