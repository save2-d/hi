package com.antigravity.browser.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.browser.core.BrowserViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySetupScreen(
    viewModel: BrowserViewModel,
    onFinished: () -> Unit
) {
    var apiKeys by remember { mutableStateOf(List(5) { "" }) }
    var showError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val apiKeyManager = viewModel.apiKeyManager

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welcome to Gemini Browser") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Setup AI Features",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Add your Gemini API keys to enable AI features. You can add up to 5 keys for redundancy.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // API Key inputs
            apiKeys.forEachIndexed { index, key ->
                OutlinedTextField(
                    value = key,
                    onValueChange = { newValue ->
                        apiKeys = apiKeys.toMutableList().apply { set(index, newValue) }
                    },
                    label = { Text("API Key ${index + 1}${if (index == 0) " (Required)" else " (Optional)"}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && index == 0 && key.isBlank()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (showError) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "At least one API key is required",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (apiKeys[0].isBlank()) {
                        showError = true
                    } else {
                        scope.launch {
                            apiKeys.filter { it.isNotBlank() }.forEach { key ->
                                apiKeyManager.addKey(key)
                            }
                            onFinished()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Continue")
            }
            
            TextButton(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now (No AI features)")
            }
        }
    }
}
