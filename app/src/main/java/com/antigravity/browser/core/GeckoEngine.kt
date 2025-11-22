package com.antigravity.browser.core

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoRuntimeSettings

object GeckoEngine {
    private var runtime: GeckoRuntime? = null

    fun initialize(context: Context) {
        if (runtime == null) {
            val settings = GeckoRuntimeSettings.Builder()
                .aboutConfigEnabled(true)
                .consoleOutput(true)
                .build()
            runtime = GeckoRuntime.create(context, settings)
        }
    }

    fun getRuntime(): GeckoRuntime {
        return runtime ?: throw IllegalStateException("GeckoEngine not initialized")
    }

    fun createSession(): GeckoSession {
        val session = GeckoSession()
        session.open(getRuntime())
        return session
    }

    fun getExtensionManager(): ExtensionManager {
        return ExtensionManager(getRuntime())
    }
}
