package com.antigravity.browser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
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

    val address = remember { mutableStateOf("https://www.google.com") }
    Scaffold(
        topBar = {
            // Address Bar
            TextField(
                value = address.value,
                onValueChange = { address.value = it },
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(onGo = { currentTab?.let { com.antigravity.browser.core.TabManager.loadUrl(it, address.value) } }),
                singleLine = true,
                maxLines = 1,
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
                IconButton(onClick = { currentTab?.let { com.antigravity.browser.core.TabManager.loadUrl(it, "javascript:history.back()") } }) {
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back")
                }
                IconButton(onClick = { currentTab?.let { com.antigravity.browser.core.TabManager.loadUrl(it, "javascript:history.forward()") } }) {
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowForward, contentDescription = "Forward")
                }
                IconButton(onClick = { currentTab?.let { com.antigravity.browser.core.TabManager.loadUrl(it, "javascript:location.reload(true)") } }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = "Reload")
                }
                val currentUrl by com.antigravity.browser.core.TabManager.currentUrl.collectAsState()
                var address by remember { mutableStateOf(currentUrl ?: "https://www.google.com") }
                IconButton(onClick = { showAiOverlay = true }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Face, contentDescription = "AI")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { com.antigravity.browser.core.TabManager.createTab() }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "New Tab")
                    var address by remember { mutableStateOf(currentUrl ?: "https://www.google.com") }
                IconButton(onClick = { showSettings = true }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    ) { padding ->
                    val currentUrl by com.antigravity.browser.core.TabManager.currentUrl.collectAsState()
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
                        } else if (prompt.contains("use pro", ignoreCase = true) || prompt.contains("switch to pro", ignoreCase = true)) {
                            com.antigravity.browser.data.GeminiRepository.setActiveModel(com.antigravity.browser.data.GeminiRepository.MODEL_PRO)
                            response = "Switched to Gemini 2.5 Pro until restart or further instruction."
                        } else if (prompt.contains("use flash", ignoreCase = true) || prompt.contains("switch to flash", ignoreCase = true)) {
                            com.antigravity.browser.data.GeminiRepository.setActiveModel(com.antigravity.browser.data.GeminiRepository.MODEL_FLASH)
                            response = "Switched to Gemini 2.5 Flash."
                        } else if (prompt.startsWith("add api key ", ignoreCase = true)) {
                            val key = prompt.substringAfter("add api key ").trim()
                            if (key.isNotBlank()) {
                                com.antigravity.browser.data.GeminiRepository.addApiKey(key)
                                response = "Added API Key"
                            } else {
                                response = "No API key provided"
                            }
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
    var showExtensionEditor by remember { mutableStateOf(false) }
    val keys by com.antigravity.browser.data.GeminiRepository.apiKeys.collectAsState()

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
                Spacer(modifier = Modifier.height(8.dp))
                if (keys.isNotEmpty()) {
                    Text("Saved Keys")
                    keys.forEachIndexed { idx, key ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(key.take(8) + "...", modifier = Modifier.weight(1f))
                            TextButton(onClick = { com.antigravity.browser.data.GeminiRepository.rotateKey(isError = false) }) { Text("Set Active") }
                            TextButton(onClick = { com.antigravity.browser.data.GeminiRepository.removeApiKey(key) }) { Text("Delete") }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                    scope.launch {
                        GeckoEngine.getExtensionManager().installExtension(com.antigravity.browser.core.ExtensionManager.UBLOCK_ORIGIN_URL)
                    }
                }) {
                    Text("Install uBlock Origin Manually")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    showExtensionEditor = true
                }) {
                    Text("Open Extension Editor")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
                    scope.launch {
                        val ok = com.antigravity.browser.core.AdBlocker.loadFilterListFromUrl("https://easylist.to/easylist/easylist.txt")
                        // This is a simple way to notify user — in production use better feedback
                    }
                }) {
                    Text("Load EasyList Filters")
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

    if (showExtensionEditor) {
        ExtensionEditor(onClose = { showExtensionEditor = false })
    }
}
