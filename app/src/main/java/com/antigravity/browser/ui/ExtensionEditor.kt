package com.antigravity.browser.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.browser.core.GeckoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ExtensionEditor(onClose: () -> Unit) {
    var extensionUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Extension Editor", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = extensionUrl, onValueChange = { extensionUrl = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Extension XPI URL or file:// path") })
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Button(onClick = {
                // Attempt to install extension via ExtensionManager
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        GeckoEngine.getExtensionManager().installExtension(extensionUrl)
                        message = "Installation started"
                    } catch (e: Exception) {
                        message = "Error: ${e.message}"
                    }
                }
            }) {
                Text("Install")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onClose) { Text("Close") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(message)
    }
}
