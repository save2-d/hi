package com.antigravity.browser.core

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.browser.BrowserApp
import com.antigravity.browser.data.ai.ApiKeyManager
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
    val apiKeyManager = browserApp.apiKeyManager
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

    // Navigation state
    private val _canGoBack = MutableStateFlow(false)
    val canGoBack = _canGoBack.asStateFlow()

    private val navigationDelegate = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(session: GeckoSession, url: String?, perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>) {
            url?.let { _currentUrl.value = it }
            Log.d(TAG, "Location changed: $url")
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            _canGoBack.value = canGoBack
        }
    }

    init {
        extensionManager.installExtensions()
        createNewTab()
        Log.d(TAG, "Loading initial URL: ${_currentUrl.value}")
        loadUrl(_currentUrl.value)
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
    
    private val screenCaptureService = ScreenCaptureService(application)

    private suspend fun handleAiChat(query: String) {
        try {
            val apiKey = apiKeyManager.getActiveKey()
            if (apiKey == null) {
                addChatMessage("Error: No API key available", isUser = false)
                return
            }

            // Check for vision request
            val lowerQuery = query.lowercase()
            if (lowerQuery.contains("see") || lowerQuery.contains("look") || lowerQuery.contains("screen") || lowerQuery.contains("what is on")) {
                val session = _activeSession.value
                val bitmapBase64 = screenCaptureService.captureScreen(session)
                
                if (bitmapBase64 != null) {
                    addChatMessage("Analyzing screen...", isUser = false)
                    val result = com.antigravity.browser.data.ai.GeminiClient.generateWithVision(
                        apiKey = apiKey,
                        prompt = query,
                        imageBase64 = bitmapBase64
                    )
                    
                    result.fold(
                        onSuccess = { text ->
                            addChatMessage(text, isUser = false)
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Vision API error", e)
                            addChatMessage("Vision Error: ${e.message}", isUser = false)
                        }
                    )
                    return
                } else {
                    addChatMessage("Could not capture screen. Proceeding with text only.", isUser = false)
                }
            }

            val result = com.antigravity.browser.data.ai.GeminiClient.generateWithFunctions(
                apiKey = apiKey,
                prompt = query,
                functions = com.antigravity.browser.data.ai.BrowserTools.getTools()
            )

            result.fold(
                onSuccess = { response ->
                    val candidate = response.candidates.firstOrNull()
                    val part = candidate?.content?.parts?.firstOrNull()
                    
                    if (part?.functionCall != null) {
                        val call = part.functionCall
                        Log.d(TAG, "AI requested function call: ${call.name}")
                        executeTool(call.name, call.args)
                    } else if (part?.text != null) {
                        addChatMessage(part.text, isUser = false)
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Gemini API error", e)
                    addChatMessage("AI Error: ${e.message}", isUser = false)
                    // Report error to manager for rotation
                    apiKeyManager.reportError(isRateLimit = e.message?.contains("429") == true)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in AI chat", e)
            addChatMessage("AI Error: ${e.message}", isUser = false)
        }
    }

    private fun executeTool(name: String, args: Map<String, Any>) {
        viewModelScope.launch {
            try {
                var resultMessage = "Executed $name"
                
                when (name) {
                    "open_url" -> {
                        val url = args["url"] as? String
                        if (url != null) {
                            loadUrl(url)
                            resultMessage = "Opened $url"
                        }
                    }
                    "search_google" -> {
                        val query = args["query"] as? String
                        if (query != null) {
                            val searchUrl = "https://www.google.com/search?q=$query"
                            loadUrl(searchUrl)
                            resultMessage = "Searched for $query"
                        }
                    }
                    "scroll" -> {
                        val directionStr = args["direction"] as? String
                        val amount = (args["amount"] as? Number)?.toFloat() ?: 0.5f
                        if (directionStr != null) {
                            val direction = when(directionStr.uppercase()) {
                                "UP" -> ScrollDirection.UP
                                "DOWN" -> ScrollDirection.DOWN
                                "LEFT" -> ScrollDirection.LEFT
                                "RIGHT" -> ScrollDirection.RIGHT
                                "TOP" -> ScrollDirection.UP // Fallback
                                "BOTTOM" -> ScrollDirection.DOWN // Fallback
                                else -> ScrollDirection.DOWN
                            }
                            val session = _activeSession.value
                            if (session != null) {
                                browserAutomation.executeCommand(
                                    session, 
                                    BrowserCommand.Scroll(direction, amount)
                                )
                                resultMessage = "Scrolled $directionStr"
                            }
                        }
                    }
                    "video_control" -> {
                        val action = args["action"] as? String
                        val seconds = (args["seconds"] as? Number)?.toInt() ?: 10
                        val session = _activeSession.value
                        if (session != null && action != null) {
                            when (action) {
                                "PLAY_PAUSE" -> browserAutomation.executeCommand(session, BrowserCommand.PlayPause)
                                "SEEK_FORWARD" -> browserAutomation.executeCommand(session, BrowserCommand.Seek(seconds))
                                "SEEK_BACKWARD" -> browserAutomation.executeCommand(session, BrowserCommand.Seek(-seconds))
                            }
                            resultMessage = "Video action: $action"
                        }
                    }
                    "set_video_speed" -> {
                        val speed = (args["speed"] as? Number)?.toFloat() ?: 1.0f
                        val session = _activeSession.value
                        if (session != null) {
                            browserAutomation.executeCommand(session, BrowserCommand.SetVideoSpeed(speed))
                            // Also update extension state if needed
                            extensionManager.setVideoSpeed(speed)
                            resultMessage = "Set video speed to ${speed}x"
                        }
                    }
                    "manage_tabs" -> {
                        val action = args["action"] as? String
                        val index = (args["index"] as? Number)?.toInt() ?: 0
                        when (action) {
                            "CREATE" -> {
                                createNewTab()
                                resultMessage = "Created new tab"
                            }
                            "CLOSE" -> {
                                closeTab(index)
                                resultMessage = "Closed tab $index"
                            }
                            "SWITCH" -> {
                                switchToTab(index)
                                resultMessage = "Switched to tab $index"
                            }
                        }
                    }
                    "toggle_extension" -> {
                        val extName = args["extension"] as? String
                        val enable = args["enable"] as? Boolean ?: true
                        if (extName != null) {
                            val type = when(extName.uppercase()) {
                                "UBLOCK" -> ExtensionType.UBLOCK
                                "VIDEO_SPEED" -> ExtensionType.VIDEO_SPEED
                                else -> null
                            }
                            if (type != null) {
                                toggleExtension(type, enable)
                                resultMessage = "${if(enable) "Enabled" else "Disabled"} $extName"
                            }
                        }
                    }
                    "get_page_content" -> {
                        val session = _activeSession.value
                        if (session != null) {
                            val content = browserAutomation.getPageContent(session)
                            // We need to send this back to the AI.
                            // Since this is a one-way tool execution in this architecture,
                            // we will just display it or feed it back as a new user prompt context if needed.
                            // Ideally, we should have a multi-turn conversation where the tool output goes back to model.
                            // For now, we'll display a summary and ask the AI to process it if it was a direct request.
                            
                            // Hack: Send the content back to AI as a new prompt "Here is the page content: ..."
                            // But to avoid loops, we'll just show it or summarize it.
                            
                            // Better: Call AI again with the content.
                            addChatMessage("Read page content (${content.length} chars). Summarizing...", isUser = false)
                            
                            val apiKey = apiKeyManager.getActiveKey()
                            if (apiKey != null) {
                                val summaryResult = com.antigravity.browser.data.ai.GeminiClient.generateText(
                                    apiKey = apiKey,
                                    prompt = "Here is the content of the page the user is looking at. Please summarize it or answer their previous question based on it:\n\n${content.take(5000)}", // Limit context
                                    enableThinking = true
                                )
                                summaryResult.onSuccess { summary ->
                                    addChatMessage(summary, isUser = false)
                                }
                            }
                            resultMessage = "Processed page content"
                        } else {
                            resultMessage = "No active page to read"
                        }
                    }
                }
                if (name != "get_page_content") { // Avoid double messaging for content
                    addChatMessage(resultMessage, isUser = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing tool $name", e)
                addChatMessage("Error executing $name: ${e.message}", isUser = false)
            }
        }
    }
    
    private fun addChatMessage(content: String, isUser: Boolean) {
        _chatMessages.value = _chatMessages.value + ChatMessage(content, isUser)
    }
    
    fun toggleAiOverlay() {
        _aiOverlayVisible.value = !_aiOverlayVisible.value
    }

    fun toggleExtension(extension: ExtensionType, enable: Boolean) {
        extensionManager.toggleExtension(extension, enable)
        viewModelScope.launch {
            val action = if (enable) "enabled" else "disabled"
            addChatMessage("${extension.name} $action", isUser = false)
        }
    }

    fun switchModel(modelName: String) {
        apiKeyManager.switchModel(modelName)
        viewModelScope.launch {
            addChatMessage("Switched to $modelName model", isUser = false)
        }
    }

    fun processAiInput(query: String) {
        processAiCommand(query)
    }

    fun loadUrl(url: String) {
        _currentUrl.value = url
        val session = _activeSession.value
        Log.d(TAG, "loadUrl called: url=$url, session=$session")
        if (session != null) {
            session.loadUri(url)
            Log.d(TAG, "URL loaded into session: $url")
        } else {
            Log.e(TAG, "No active session to load URL into!")
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
        newSession.navigationDelegate = navigationDelegate
        Log.d(TAG, "Created new session: $newSession")
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
