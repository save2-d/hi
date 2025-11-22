package com.antigravity.browser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.browser.core.GeckoEngine
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class BrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Session managed by TabManager

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrowserScreen()
                }
            }
        }
    }
}

@Composable
@Composable
fun BrowserScreen() {
    val tabs by com.antigravity.browser.core.TabManager.tabs.collectAsState()
    val currentTab by com.antigravity.browser.core.TabManager.currentTab.collectAsState()
    
    var showAiOverlay by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) {
            com.antigravity.browser.core.TabManager.createTab()
        }
    }

    Scaffold(
        topBar = {
            // Address Bar
            TextField(
                value = "https://www.google.com", // TODO: Observe current URL
                onValueChange = { currentTab?.loadUri(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Address") },
                trailingIcon = {
                    IconButton(onClick = { showTabs = true }) {
                        Text(tabs.size.toString(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = { showAiOverlay = true }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Face, contentDescription = "AI")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { com.antigravity.browser.core.TabManager.createTab() }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "New Tab")
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            currentTab?.let { session ->
                AndroidView(
                    factory = { ctx ->
                        GeckoView(ctx).apply {
                            setSession(session)
                        }
                    },
                    update = { view ->
                        view.setSession(session)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showAiOverlay) {
        currentTab?.let { session ->
            AIOverlay(
                onDismiss = { showAiOverlay = false },
                onExecuteScript = { code -> session.evaluateJavascript(code, null) }
            )
        }
    }
    
    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
    
    if (showTabs) {
        // Simple Tab Switcher Dialog
        AlertDialog(
            onDismissRequest = { showTabs = false },
            title = { Text("Tabs") },
            text = {
                LazyColumn {
                    items(tabs.size) { index ->
                        val session = tabs[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    com.antigravity.browser.core.TabManager.switchToTab(session)
                                    showTabs = false
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tab ${index + 1}")
                            IconButton(onClick = { com.antigravity.browser.core.TabManager.closeTab(session) }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, "Close")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTabs = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun AIOverlay(onDismiss: () -> Unit, onExecuteScript: (String) -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Gemini AI", style = MaterialTheme.typography.headlineSmall)
            
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                item { Text(response) }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask AI...") }
                )
                IconButton(onClick = {
                    scope.launch {
                        isLoading = true
                        // Simple intent matching for demo
                        if (prompt.contains("install ublock", ignoreCase = true)) {
                            try {
                                GeckoEngine.getExtensionManager().installExtension(com.antigravity.browser.core.ExtensionManager.UBLOCK_ORIGIN_URL)
                                response = "Installing uBlock Origin..."
                            } catch (e: Exception) {
                                response = "Error installing extension: ${e.message}"
                            }
                        } else if (prompt.contains("scroll down", ignoreCase = true)) {
                            onExecuteScript("window.scrollBy(0, 500);")
                            response = "Scrolling down..."
                        } else {
                            response = com.antigravity.browser.data.GeminiRepository.generateContent(prompt)
                        }
                        isLoading = false
                        prompt = ""
                    }
                }) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Icon(androidx.compose.material.icons.Icons.Default.Send, "Send")
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    val currentKey = com.antigravity.browser.data.GeminiRepository.getCurrentKey() ?: "None"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column {
                Text("Current API Key: ${currentKey.take(8)}...")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Add API Key") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                    scope.launch {
                        GeckoEngine.getExtensionManager().installExtension(com.antigravity.browser.core.ExtensionManager.UBLOCK_ORIGIN_URL)
                    }
                }) {
                    Text("Install uBlock Origin Manually")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (apiKey.isNotBlank()) {
                    com.antigravity.browser.data.GeminiRepository.addApiKey(apiKey)
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
