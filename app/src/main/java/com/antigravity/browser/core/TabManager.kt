package com.antigravity.browser.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.geckoview.GeckoSession

object TabManager {
    private val _tabs = MutableStateFlow<List<GeckoSession>>(emptyList())
    val tabs: StateFlow<List<GeckoSession>> = _tabs

    private val _currentTab = MutableStateFlow<GeckoSession?>(null)
    val currentTab: StateFlow<GeckoSession?> = _currentTab

    fun createTab() {
        val session = GeckoEngine.createSession()
        val currentList = _tabs.value.toMutableList()
        currentList.add(session)
        _tabs.value = currentList
        _currentTab.value = session
        
        // Load default page
        session.loadUri("https://www.google.com")
    }

    fun closeTab(session: GeckoSession) {
        session.close()
        val currentList = _tabs.value.toMutableList()
        currentList.remove(session)
        _tabs.value = currentList
        
        if (_currentTab.value == session) {
            _currentTab.value = currentList.lastOrNull()
        }
    }

    fun switchToTab(session: GeckoSession) {
        _currentTab.value = session
    }
}
