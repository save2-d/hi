package com.antigravity.browser.core

import java.util.concurrent.CopyOnWriteArrayList

object ReaderMode {
    private val readableContent = mutableMapOf<String, String>()

    fun extractReadableContent(htmlContent: String): String {
        // Simplified extraction: remove scripts, styles, and HTML markup
        val cleaned = htmlContent
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCH_MULTILINE), "")
            .replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCH_MULTILINE), "")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned
    }

    fun cacheContent(url: String, content: String) {
        readableContent[url] = content
    }

    fun getContent(url: String): String? {
        return readableContent[url]
    }
}
