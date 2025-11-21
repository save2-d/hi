package com.antigravity.browser.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.browser.core.BrowserViewModel
import com.antigravity.browser.core.ExtensionType
import com.antigravity.browser.data.ai.ApiKeyEntity
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val apiKeyManager = viewModel.apiKeyManager
    val scope = rememberCoroutineScope()
    
    var apiKeys by remember { mutableStateOf<List<ApiKeyEntity>>(emptyList()) }
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    
    // Load keys initially
    LaunchedEffect(Unit) {
        apiKeys = apiKeyManager.getAllKeys()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // AI Model Settings
            item {
                Text(
                    "AI Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gemini 2.5 Flash (Thinking)")
                            Button(onClick = { viewModel.switchModel("flash") }) {
                                Text("Select")
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gemini 2.5 Pro")
                            Button(onClick = { viewModel.switchModel("pro") }) {
                                Text("Select")
                            }
                        }
                    }
                }
            }

            // Usage Stats
            item {
                Text(
                    "Usage Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val stats by apiKeyManager.usageStats.collectAsState(initial = emptyMap())
                
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (stats.isNullOrEmpty()) {
                            Text("No usage data available")
                        } else {
                            stats?.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key)
                                    Text(value.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // API Keys
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "API Keys (${apiKeys.size}/5)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (apiKeys.size < 5) {
                        FilledTonalButton(onClick = { showAddKeyDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Key")
                        }
                    }
                }
            }

            itemsIndexed(apiKeys) { index, entity ->
                ApiKeyCard(
                    index = index,
                    key = entity.key,
                    isActive = entity.isActive,
                    onDelete = {
                        scope.launch {
                            apiKeyManager.removeKey(index)
                            apiKeys = apiKeyManager.getAllKeys()
                        }
                    },
                    onActivate = {
                        viewModel.setManualKey(index)
                        // Refresh keys to update active status
                        scope.launch {
                            apiKeys = apiKeyManager.getAllKeys()
                        }
                    }
                )
            }

            // Extensions
            item {
                Text(
                    "Extensions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ExtensionCard(
                    name = "uBlock Origin",
                    description = "Efficient ad blocker",
                    onToggle = { enabled -> 
                        viewModel.toggleExtension(ExtensionType.UBLOCK, enabled) 
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ExtensionCard(
                    name = "Video Speed Controller",
                    description = "Control playback speed on any video",
                    onToggle = { enabled -> 
                        viewModel.toggleExtension(ExtensionType.VIDEO_SPEED, enabled) 
                    }
                )
            }
        }
    }
    
    if (showAddKeyDialog) {
        AlertDialog(
            onDismissRequest = { showAddKeyDialog = false },
            title = { Text("Add API Key") },
            text = {
                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    label = { Text("Enter Gemini API Key") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank()) {
                            scope.launch {
                                apiKeyManager.addKey(newKey)
                                apiKeys = apiKeyManager.getAllKeys()
                                newKey = ""
                                showAddKeyDialog = false
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ApiKeyCard(
    index: Int,
    key: String,
    isActive: Boolean,
    onDelete: () -> Unit,
    onActivate: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Key #${index + 1}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ends with ...${key.takeLast(4)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Row {
                if (!isActive) {
                    TextButton(onClick = onActivate) {
                        Text("Activate")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
fun ExtensionCard(
    name: String,
    description: String,
    onToggle: (Boolean) -> Unit
) {
    var enabled by remember { mutableStateOf(true) }
    
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { 
                    enabled = it
                    onToggle(it)
                }
            )
        }
    }
}
