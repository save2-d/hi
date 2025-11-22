package com.antigravity.browser.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import org.mozilla.geckoview.GeckoSession

object TabManager {
    private val _tabs = MutableStateFlow<List<GeckoSession>>(emptyList())
    val tabs: StateFlow<List<GeckoSession>> = _tabs

    private val _currentTab = MutableStateFlow<GeckoSession?>(null)
    val currentTab: StateFlow<GeckoSession?> = _currentTab

    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl

    private val sessionUrls = ConcurrentHashMap<GeckoSession, String>()

    fun createTab(startUrl: String = "https://www.google.com") {
        val session = GeckoEngine.createSession()
        val currentList = _tabs.value.toMutableList()
        currentList.add(session)
        _tabs.value = currentList
        _currentTab.value = session
        
        // Load default page
        loadUrl(session, startUrl)
    }

    fun closeTab(session: GeckoSession) {
        session.close()
        val currentList = _tabs.value.toMutableList()
        currentList.remove(session)
        _tabs.value = currentList
        
        if (_currentTab.value == session) {
            _currentTab.value = currentList.lastOrNull()
            _currentUrl.value = _currentTab.value?.let { sessionUrls[it] }
        }
    }

    fun switchToTab(session: GeckoSession) {
        _currentTab.value = session
        _currentUrl.value = sessionUrls[session]
    }

    fun loadUrl(session: GeckoSession, url: String) {
        if (AdBlocker.isBlocked(url)) {
            // Block the URL by navigating to about:blank to avoid loading trackers/ads
            session.loadUri("about:blank")
            sessionUrls[session] = "about:blank"
            _currentUrl.value = "about:blank"
        } else {
            session.loadUri(url)
            sessionUrls[session] = url
            _currentUrl.value = url
        }
    }
}
