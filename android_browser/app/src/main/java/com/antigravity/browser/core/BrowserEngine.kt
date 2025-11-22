package com.antigravity.browser.core

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class BrowserEngine(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: GeckoRuntime? = null

        fun getRuntime(context: Context): GeckoRuntime {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    GeckoRuntime.create(context.applicationContext)
                } catch (e: Exception) {
                    android.util.Log.e("BrowserEngine", "Failed to create GeckoRuntime", e)
                    throw e
                }.also { INSTANCE = it }
            }
        }
    }

    val runtime: GeckoRuntime = getRuntime(context)

    fun createSession(): GeckoSession {
        val session = GeckoSession()
        session.open(runtime)
        return session
    }
}
