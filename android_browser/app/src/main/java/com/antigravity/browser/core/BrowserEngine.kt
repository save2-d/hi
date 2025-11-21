package com.antigravity.browser.core

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class BrowserEngine(private val context: Context) {

    val runtime: GeckoRuntime by lazy {
        GeckoRuntime.create(context)
    }

    fun createSession(): GeckoSession {
        val session = GeckoSession()
        session.open(runtime)
        return session
    }
}
