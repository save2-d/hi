package com.antigravity.browser.ui.browser

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.browser.core.BrowserViewModel
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(browserViewModel: BrowserViewModel = viewModel()) {
    val currentUrl by browserViewModel.currentUrl.collectAsState()
    val activeSession by browserViewModel.activeSession.collectAsState()
    var showAiOverlay by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    var text by remember { mutableStateOf(currentUrl) }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                actions = {
                    Button(onClick = { browserViewModel.loadUrl(currentUrl) }) {
                        Text("Go")
                    }
                    Button(onClick = { showAiOverlay = !showAiOverlay }) {
                        Text("AI")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (activeSession != null) {
                GeckoViewContainer(session = activeSession!!)
            } else {
                Text("No tabs open")
            }
        }
        
        if (showAiOverlay) {
            com.antigravity.browser.ui.ai.AiOverlay(viewModel = browserViewModel, onDismiss = { showAiOverlay = false })
        }
    }
}

@Composable
fun GeckoViewContainer(session: GeckoSession) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            GeckoView(context).apply {
                setSession(session)
            }
        },
        update = { view ->
            view.setSession(session)
        }
    )
}
