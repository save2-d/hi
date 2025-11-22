package com.antigravity.browser.ui.browser

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
            currentScreen = ScreenState.SETUP
        }
    }

    when (currentScreen) {
        ScreenState.SETUP -> {
            ApiKeySetupScreen(
                viewModel = viewModel,
                onFinished = { currentScreen = ScreenState.BROWSER }
            )
        }
        ScreenState.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = ScreenState.BROWSER }
            )
        }
        ScreenState.BROWSER -> {
            Scaffold(
                topBar = {
                    // Minimal top bar for menu access
                    // In a real browser, this would be the URL bar
                    // For now, we just need a way to get to settings
                    Box(modifier = Modifier.fillMaxSize()) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = { 
                                    showMenu = false
                                    currentScreen = ScreenState.SETTINGS 
                                },
                                leadingIcon = { Icon(Icons.Default.Settings, null) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // GeckoView
                    AndroidView(
                        factory = { ctx ->
                            GeckoView(ctx).apply {
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.MATCH_PARENT
                                )
                                // Try to set session immediately if available
                                viewModel.activeSession.value?.let { 
                                    setSession(it)
                                    android.util.Log.d("BrowserScreen", "Factory: Set initial session: $it")
                                }
                            }
                        },
                        update = { geckoView ->
                            val session = activeSession
                            android.util.Log.d("BrowserScreen", "AndroidView update: session=$session, geckoView.session=${geckoView.session}")
                            if (session != null && geckoView.session != session) {
                                geckoView.setSession(session)
                                android.util.Log.d("BrowserScreen", "Set session on GeckoView: $session")
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Magenta)
                    )
                    
                    // AI Overlay
                    AiOverlay(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
