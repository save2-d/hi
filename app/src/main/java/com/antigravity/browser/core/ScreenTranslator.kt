package com.antigravity.browser.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object ScreenTranslator {
    private val client = OkHttpClient()

    suspend fun translateText(text: String, targetLanguage: String = "en"): String =
        withContext(Dispatchers.IO) {
            try {
                // This is a placeholder for integrating Gemini's translation capability
                // In production, use GeminiRepository.generateContent with a translation prompt
                val prompt = "Translate the following text to $targetLanguage:\n$text"
                return@withContext com.antigravity.browser.data.GeminiRepository.generateContent(prompt)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "Translation failed: ${e.message}"
            }
        }

    suspend fun extractText(htmlContent: String): String = withContext(Dispatchers.IO) {
        try {
            // Remove HTML tags and extract plain text
            val plainText = htmlContent
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            return@withContext plainText
        } catch (e: Exception) {
            return@withContext "Extraction failed: ${e.message}"
        }
    }
}
