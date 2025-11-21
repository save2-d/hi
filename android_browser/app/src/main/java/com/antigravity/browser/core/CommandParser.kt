package com.antigravity.browser.core

import android.util.Log

/**
 * Command types that can be executed
 */
sealed class BrowserCommand {
    data class OpenUrl(val url: String) : BrowserCommand()
    data class Search(val query: String) : BrowserCommand()
    data class Scroll(val direction: ScrollDirection, val amount: Float = 1.0f) : BrowserCommand()
    object ScrollToTop : BrowserCommand()
    object ScrollToBottom : BrowserCommand()
    data class SetVideoSpeed(val speed: Float) : BrowserCommand()
    object PlayPause : BrowserCommand()
    data class Seek(val seconds: Int) : BrowserCommand()
    object CreateTab : BrowserCommand()
    object CloseTab : BrowserCommand()
    data class SwitchTab(val index: Int) : BrowserCommand()
    data class ToggleExtension(val extension: ExtensionType, val enable: Boolean) : BrowserCommand()
    data class SwitchModel(val model: String) : BrowserCommand()
    object Unknown : BrowserCommand()
}

enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}

enum class ExtensionType {
    UBLOCK, VIDEO_SPEED
}

/**
 * Multi-language command parser supporting English, Hindi, Punjabi, Hinglish
 */
class CommandParser {
    private val TAG = "CommandParser"
    
    // URL patterns
    private val urlPattern = Regex("""(https?://)?([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})(:\d+)?(/\S*)?""")
    
    // Video speed patterns (supports decimal numbers)
    private val speedPattern = Regex("""\b(\d+\.?\d*)[xX]?\s*(speed|taraf|veg)?\b""")
    
    /**
     * Parse command from natural language input
     */
    fun parse(input: String): BrowserCommand {
        val normalized = input.lowercase().trim()
        
        Log.d(TAG, "Parsing command: $input")
        
        // Check for URL opening (multi-language)
        if (containsAny(normalized, listOf("open", "khol", "kholo", "kho", "खोल", "شروع کرو"))) {
            val url = extractUrl(input)
            if (url != null) {
                return BrowserCommand.OpenUrl(url)
            }
            // Might be searching instead
            val searchQuery = extractAfterKeywords(normalized, listOf("open", "khol", "kholo", "खोल"))
            if (searchQuery.isNotBlank()) {
                return BrowserCommand.Search(searchQuery)
            }
        }
        
        // Search command
        if (containsAny(normalized, listOf("search", "dhundo", "ढूंढो", "تلاش کرو", "find"))) {
            val query = extractAfterKeywords(normalized, listOf("search", "dhundo", "find", "for"))
            if (query.isNotBlank()) {
                return BrowserCommand.Search(query)
            }
        }
        
        // Video speed control
        if (containsAny(normalized, listOf("speed", "taraf", "veg", "गति", "رفتار"))) {
            // Check for "normal" speed (2.1x as per requirement)
            if (containsAny(normalized, listOf("normal", "usually", "aam", "عام"))) {
                return BrowserCommand.SetVideoSpeed(2.1f)
            }
            
            // Check for "slow" or "1x"
            if (containsAny(normalized, listOf("slow", "dhire", "धीरे", "آہستہ", "1x", "1"))) {
                return BrowserCommand.SetVideoSpeed(1.0f)
            }
            
            // Extract specific speed number
            val speedMatch = speedPattern.find(normalized)
            if (speedMatch != null) {
                val speedValue = speedMatch.groupValues[1].toFloatOrNull()
                if (speedValue != null) {
                    return BrowserCommand.SetVideoSpeed(speedValue)
                }
            }
        }
        
        // Scroll commands
        when {
            containsAny(normalized, listOf("scroll down", "niche", "नीचे", "تیز کرو")) -> {
                return BrowserCommand.Scroll(ScrollDirection.DOWN)
            }
            containsAny(normalized, listOf("scroll up", "upar", "ऊपर", "اوپر")) -> {
                return BrowserCommand.Scroll(ScrollDirection.UP)
            }
            containsAny(normalized, listOf("scroll left", "baye", "बाएं", "بائیں")) -> {
                return BrowserCommand.Scroll(ScrollDirection.LEFT)
            }
            containsAny(normalized, listOf("scroll right", "daye", "दाएं", "دائیں")) -> {
                return BrowserCommand.Scroll(ScrollDirection.RIGHT)
            }
            containsAny(normalized, listOf("top", "sabse upar", "सबसे ऊपर")) -> {
                return BrowserCommand.ScrollToTop
            }
            containsAny(normalized, listOf("bottom", "sabse niche", "सबसे नीचे")) -> {
                return BrowserCommand.ScrollToBottom
            }
        }
        
        // Video playback control
        when {
            containsAny(normalized, listOf("play", "chala", "चलाओ", "چلائیں")) -> {
                return BrowserCommand.PlayPause
            }
            containsAny(normalized, listOf("pause", "roko", "रोको", "روکو")) -> {
                return BrowserCommand.PlayPause
            }
            containsAny(normalized, listOf("forward", "aage", "आगे", "آگے")) -> {
                val seconds = extractNumber(normalized) ?: 10
                return BrowserCommand.Seek(seconds)
            }
            containsAny(normalized, listOf("backward", "piche", "पीछे", "پیچھے", "rewind")) -> {
                val seconds = -(extractNumber(normalized) ?: 10)
                return BrowserCommand.Seek(seconds)
            }
        }
        
        // Tab management
        when {
            containsAny(normalized, listOf("new tab", "naya tab", "नया टैब", "نیا ٹیب")) -> {
                return BrowserCommand.CreateTab
            }
            containsAny(normalized, listOf("close tab", "band karo tab", "बंद करो", "بندکرو")) -> {
                return BrowserCommand.CloseTab
            }
            containsAny(normalized, listOf("switch tab", "change tab", "badlo tab")) -> {
                val index = extractNumber(normalized) ?: 0
                return BrowserCommand.SwitchTab(index)
            }
        }
        
        // Extension control
        when {
            containsAny(normalized, listOf("enable ublock", "ublock on", "adblock on")) -> {
                return BrowserCommand.ToggleExtension(ExtensionType.UBLOCK, true)
            }
            containsAny(normalized, listOf("disable ublock", "ublock off", "adblock off")) -> {
                return BrowserCommand.ToggleExtension(ExtensionType.UBLOCK, false)
            }
        }
        
        // Model switching
        when {
            containsAny(normalized, listOf("use pro", "switch to pro", "pro model")) -> {
                return BrowserCommand.SwitchModel("pro")
            }
            containsAny(normalized, listOf("use flash", "switch to flash", "flash model", "back to flash")) -> {
                return BrowserCommand.SwitchModel("flash")
            }
        }
        
        Log.w(TAG, "Could not parse command: $input")
        return BrowserCommand.Unknown
    }
    
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
    
    private fun extractUrl(text: String): String? {
        val match = urlPattern.find(text)
        if (match != null) {
            var url = match.value
            // Add https:// if not present
            if (!url.startsWith("http")) {
                url = "https://$url"
            }
            return url
        }
        return null
    }
    
    private fun extractAfterKeywords(text: String, keywords: List<String>): String {
        for (keyword in keywords) {
            val index = text.indexOf(keyword)
            if (index != -1) {
                return text.substring(index + keyword.length).trim()
            }
        }
        return ""
    }
    
    private fun extractNumber(text: String): Int? {
        val numberPattern = Regex("""\b(\d+)\b""")
        return numberPattern.find(text)?.value?.toIntOrNull()
    }
}
