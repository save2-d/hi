package com.antigravity.browser.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antigravity.browser.core.GeckoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

@Composable
fun JavaScriptInjectorDialog(session: GeckoSession?, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("JavaScript Injector") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Enter JavaScript code to inject into the page:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("console.log('Hello');") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (output.isNotEmpty()) {
                    Text("Output: $output", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (code.isNotBlank() && session != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            session.evaluateJavascript(code, null)
                            output = "Code executed"
                        } catch (e: Exception) {
                            output = "Error: ${e.message}"
                        }
                    }
                }
            }) { Text("Execute") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
