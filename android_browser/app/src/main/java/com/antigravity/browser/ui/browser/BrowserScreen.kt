package com.antigravity.browser.ui.browser

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.browser.core.BrowserViewModel
import com.antigravity.browser.ui.ai.AiOverlay
import com.antigravity.browser.ui.settings.SettingsScreen
import com.antigravity.browser.ui.setup.ApiKeySetupScreen
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoView

enum class ScreenState {
    BROWSER, SETTINGS, SETUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel
) {
    val context = LocalContext.current
    val apiKeyManager = viewModel.apiKeyManager
    val scope = rememberCoroutineScope()
    
    var currentScreen by remember { mutableStateOf(ScreenState.BROWSER) }
    var showMenu by remember { mutableStateOf(false) }
    
    // Check if we need to show setup
    LaunchedEffect(Unit) {
        if (apiKeyManager.getAllKeys().isEmpty()) {
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
                            }
                        },
                        update = { geckoView ->
                            val session = viewModel.activeSession.value
                            android.util.Log.d("BrowserScreen", "AndroidView update: session=$session, geckoView.session=${geckoView.session}")
                            if (session != null && geckoView.session != session) {
                                geckoView.setSession(session)
                                android.util.Log.d("BrowserScreen", "Set session on GeckoView: $session")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
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
