package com.antigravity.browser.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.antigravity.browser.core.BrowserViewModel

@Composable
fun AiOverlay(viewModel: BrowserViewModel, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gemini AI", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                
                var prompt by remember { mutableStateOf("") }
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text("Ask me to open websites, control video speed, or summarize content.")
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("API Key Settings", style = MaterialTheme.typography.titleMedium)
                        val activeIndex = viewModel.activeKeyIndex.collectAsState().value
                        Text("Active Key: #${activeIndex + 1}")
                        Row {
                            (0..4).forEach { index ->
                                Button(
                                    onClick = { viewModel.setManualKey(index) },
                                    modifier = Modifier.padding(4.dp),
                                    enabled = index != activeIndex
                                ) {
                                    Text("${index + 1}")
                                }
                            }
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a command...") }
                    )
                    Button(onClick = { 
                        viewModel.processAiCommand(prompt)
                        prompt = ""
                    }) {
                        Text("Send")
                    }
                }
            }
        }
    }
}
