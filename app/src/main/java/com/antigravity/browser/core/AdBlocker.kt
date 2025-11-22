package com.antigravity.browser.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object AdBlocker {
    private val filters = mutableSetOf(
        "doubleclick.net",
        "pagead2.googlesyndication.com",
        "googlesyndication",
        "adservice.google.com",
        "ads.youtube.com",
        "adserver",
        "ads"
    )

    fun isBlocked(url: String): Boolean {
        val lower = url.lowercase()
        return filters.any { lower.contains(it) }
    }

    fun addFilter(filter: String) {
        if (filter.isNotBlank()) filters.add(filter.lowercase())
    }

    fun removeFilter(filter: String) {
        filters.remove(filter.lowercase())
    }

    private val client = OkHttpClient()

    suspend fun loadFilterListFromUrl(filterUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(filterUrl).get().build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext false
            val body = resp.body?.string() ?: return@withContext false
            body.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!") ) return@forEach
                // Simplistic parsing: extract domain-like parts
                val domain = trimmed
                    .removePrefix("||")
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .split('/')[0]
                    .split('^')[0]
                if (domain.isNotBlank()) filters.add(domain.lowercase())
            }
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
