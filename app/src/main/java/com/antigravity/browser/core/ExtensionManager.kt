package com.antigravity.browser.core

import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import kotlinx.coroutines.guava.await

class ExtensionManager(private val runtime: GeckoRuntime) {

    suspend fun installExtension(url: String) {
        try {
            runtime.webExtensionController.install(url).await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun listExtensions(): List<WebExtension> {
        return try {
            runtime.webExtensionController.list().await()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun enableExtension(extension: WebExtension, enable: Boolean) {
        if (enable) {
            runtime.webExtensionController.enable(extension).await()
        } else {
            runtime.webExtensionController.disable(extension).await()
        }
    }
    
    companion object {
        // URL for uBlock Origin (stable release)
        const val UBLOCK_ORIGIN_URL = "https://addons.mozilla.org/firefox/downloads/latest/ublock-origin/latest.xpi"
    }
}
