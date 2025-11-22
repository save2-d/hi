package com.antigravity.browser.core

import org.mozilla.geckoview.GeckoSession
import java.util.concurrent.CopyOnWriteArrayList

object AutomationRecorder {
    private val actions = CopyOnWriteArrayList<String>()

    fun recordAction(jsSnippet: String) {
        actions.add(jsSnippet)
    }

    fun clear() {
        actions.clear()
    }

    suspend fun replay(session: GeckoSession) {
        for (snippet in actions) {
            session.evaluateJavascript(snippet, null)
        }
    }
}
