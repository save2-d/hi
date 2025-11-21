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
import com.antigravity.browser.data.ai.ApiKeyEntity

@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    var apiKeys by remember { mutableStateOf<List<ApiKeyEntity>>(emptyList()) }
    var showAddKeyDialog by remember { mutableStateOf(false) }
    val activeKeyIndex by viewModel.activeKeyIndex.collectAsState()
    val currentModel by viewModel.apiKeyManager.currentModel.collectAsState()
    val usageStats by viewModel.apiKeyManager.usageStats.collectAsState()
    
    LaunchedEffect(Unit) {
        apiKeys = viewModel.apiKeyManager.getAllKeys()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Model section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Model",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            "Current: ${currentModel.substringAfter("gemini-")}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = currentModel.contains("flash"),
                                onClick = { viewModel.switchModel("flash") },
                                label = { Text("Flash") }
                            )
                            FilterChip(
                                selected = currentModel.contains("pro"),
                                onClick = { viewModel.switchModel("pro") },
                                label = { Text("Pro") }
                            )
                        }
                    }
                }
            }
            
            // Usage stats
            item {
                usageStats?.let { stats ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Usage Statistics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            UsageIndicator("Requests/Min", stats.requestsThisMinute, stats.rpmLimit)
                            Spacer(modifier = Modifier.height(8.dp))
                            UsageIndicator("Tokens/Min", stats.tokensThisMinute, stats.tpmLimit)
                            Spacer(modifier = Modifier.height(8.dp))
                            UsageIndicator("Requests/Day", stats.requestsToday, stats.rpdLimit)
                        }
                    }
                }
            }
            
            // API Keys section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "API Keys (${apiKeys.size}/5)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
            
            itemsIndexed(apiKeys) { index, key ->
                ApiKeyCard(
                    key = key,
                    index = index,
                    isActive = index == activeKeyIndex,
                    onActivate = { viewModel.setManualKey(index) },
                    onDelete = {
                        kotlinx.coroutines.GlobalScope.launch {
                            viewModel.apiKeyManager.removeKey(index)
                            apiKeys = viewModel.apiKeyManager.getAllKeys()
                        }
                    }
                )
            }
            
            // Extensions section
            item {
                Text(
                    "Extensions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                ExtensionToggle(
                    name = "uBlock Origin",
                    description = "Ad blocker",
                    enabled = true,
                    onToggle = { /* Toggle uBlock */ }
                )
            }
            
            item {
                ExtensionToggle(
                    name = "Video Speed Controller",
                    description = "Control video playback speed",
                    enabled = true,
                    onToggle = { /* Toggle video speed */ }
                )
            }
        }
    }
    
    if (showAddKeyDialog) {
        AddKeyDialog(
            onDismiss = { showAddKeyDialog = false },
            onAdd = { newKey ->
                kotlinx.coroutines.GlobalScope.launch {
                    viewModel.apiKeyManager.addKey(newKey)
                    apiKeys = viewModel.apiKeyManager.getAllKeys()
                }
                showAddKeyDialog = false
            }
        )
    }
}

@Composable
fun ApiKeyCard(
    key: ApiKeyEntity,
    index: Int,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Key ${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "•••${key.key.takeLast(4)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isActive) {
                    Text(
                        "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (key.errorCount > 0) {
                    Text(
                        "${key.errorCount} errors",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Row {
                if (!isActive) {
                    IconButton(onClick = onActivate) {
                        Icon(Icons.Default.PlayArrow, "Activate")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun UsageIndicator(label: String, current: Int, limit: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$current / $limit", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = (current.toFloat() / limit).coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ExtensionToggle(
    name: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun AddKeyDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var keyText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add API Key") },
        text = {
            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                label = { Text("Gemini API Key") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(keyText) },
                enabled = keyText.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
