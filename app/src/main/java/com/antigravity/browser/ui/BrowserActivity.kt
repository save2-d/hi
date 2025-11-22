package com.antigravity.browser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.browser.core.*
import com.antigravity.browser.data.GeminiRepository
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoView

class BrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GeckoEngine.initialize(this)
        GeminiRepository.initialize(this)

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
    val tabs by TabManager.tabs.collectAsState()
    val currentTab by TabManager.currentTab.collectAsState()
    val currentUrl by TabManager.currentUrl.collectAsState()

    var showFirstLaunch by remember { mutableStateOf(false) }
    var showAiOverlay by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var showJsInjector by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf(currentUrl ?: "https://www.google.com") }

    LaunchedEffect(Unit) {
        if (tabs.isEmpty()) TabManager.createTab()
        if (GeminiRepository.apiKeys.value.isEmpty()) showFirstLaunch = true
    }

    LaunchedEffect(currentUrl) {
        address = currentUrl ?: address
    }

    if (showFirstLaunch) {
        FirstLaunchScreen(onComplete = { showFirstLaunch = false })
        return
    }

    Scaffold(
        topBar = {
            TextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Address") },
                singleLine = true,
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Go),
                keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
                    onGo = { currentTab?.let { TabManager.loadUrl(it, address) } }
                ),
                trailingIcon = {
                    IconButton(onClick = { showTabs = true }) {
                        Text(tabs.size.toString(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                IconButton(onClick = { currentTab?.let { TabManager.loadUrl(it, "javascript:history.back()") } }) {
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back")
                }
                IconButton(onClick = { currentTab?.let { TabManager.loadUrl(it, "javascript:history.forward()") } }) {
                    Icon(androidx.compose.material.icons.Icons.Default.ArrowForward, contentDescription = "Forward")
                }
                IconButton(onClick = { currentTab?.let { TabManager.loadUrl(it, "javascript:location.reload(true)") } }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = "Reload")
                }
                IconButton(onClick = { showAiOverlay = true }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Face, contentDescription = "AI")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { TabManager.createTab() }) {
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
                    factory = { ctx -> GeckoView(ctx).apply { setSession(session) } },
                    update = { view -> view.setSession(session) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showAiOverlay) {
        AIOverlay(onDismiss = { showAiOverlay = false }, onExecuteScript = { code ->
            currentTab?.evaluateJavascript(code, null)
        })
    }

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false }, onShowJsInjector = { showJsInjector = true })
    }

    if (showJsInjector) {
        JavaScriptInjectorDialog(session = currentTab, onDismiss = { showJsInjector = false })
    }

    if (showTabs) {
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
                                .clickable { TabManager.switchToTab(session); showTabs = false }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tab ${index + 1}")
                            IconButton(onClick = { TabManager.closeTab(session) }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, "Close")
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTabs = false }) { Text("Close") } }
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
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().heightIn(max = 200.dp)) {
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
                        if (prompt.contains("install ublock", ignoreCase = true)) {
                            try {
                                GeckoEngine.getExtensionManager().installExtension(ExtensionManager.UBLOCK_ORIGIN_URL)
                                response = "Installing uBlock Origin..."
                            } catch (e: Exception) {
                                response = "Error: ${e.message}"
                            }
                        } else if (prompt.contains("scroll down", ignoreCase = true)) {
                            onExecuteScript("window.scrollBy(0, 500);")
                            response = "Scrolling down..."
                        } else if (prompt.contains("use pro", ignoreCase = true)) {
                            GeminiRepository.setActiveModel(GeminiRepository.MODEL_PRO)
                            response = "Switched to Gemini Pro"
                        } else if (prompt.contains("use flash", ignoreCase = true)) {
                            GeminiRepository.setActiveModel(GeminiRepository.MODEL_FLASH)
                            response = "Switched to Gemini Flash"
                        } else {
                            response = GeminiRepository.generateContent(prompt)
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
fun SettingsDialog(onDismiss: () -> Unit, onShowJsInjector: () -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    val currentKey = GeminiRepository.getCurrentKey() ?: "None"
    val keys by GeminiRepository.apiKeys.collectAsState()
    var showExtensionEditor by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Current API Key: ${currentKey.take(8)}...")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("Add API Key") })
                Spacer(modifier = Modifier.height(8.dp))
                if (keys.isNotEmpty()) {
                    Text("Saved Keys (${keys.size}/4)")
                    keys.forEach { key ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(key.take(8) + "...", modifier = Modifier.weight(1f))
                            TextButton(onClick = { GeminiRepository.removeApiKey(key) }) { Text("Delete") }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onShowJsInjector) { Text("JavaScript Injector") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showExtensionEditor = true }) { Text("Extension Editor") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (apiKey.isNotBlank()) GeminiRepository.addApiKey(apiKey)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showExtensionEditor) {
        ExtensionEditor(onClose = { showExtensionEditor = false })
    }
}
