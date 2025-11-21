package com.antigravity.browser.ui.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.antigravity.browser.data.ai.ApiKeyManager

@Composable
fun ApiKeySetupScreen(
    apiKeyManager: ApiKeyManager,
    onSetupComplete: () -> Unit
) {
    var apiKeys by remember { mutableStateOf(List(5) { "" }) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = "🤖 AI Browser Setup",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Add up to 5 Gemini API keys for automatic rotation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (index < 4) ImeAction.Next else ImeAction.Done
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Error message
        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "✨ Features:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Auto-rotation every 35 days\n• Rate limit protection\n• Multi-language commands\n• Video speed control",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Skip button
            OutlinedButton(
                onClick = { onSetupComplete() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("Skip")
            }
            
            // Continue button
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    
                    // Validate and add keys
                    val validKeys = apiKeys.filter { it.isNotBlank() }
                    if (validKeys.isEmpty()) {
                        errorMessage = "Please add at least one API key"
                        isLoading = false
                        return@Button
                    }
                    
                    // Add keys to manager
                    kotlinx.coroutines.GlobalScope.launch {
                        var addedCount = 0
                        validKeys.forEach { key ->
                            if (apiKeyManager.addKey(key.trim())) {
                                addedCount++
                            }
                        }
                        
                        isLoading = false
                        if (addedCount > 0) {
                            onSetupComplete()
                        } else {
                            errorMessage = "Failed to add API keys"
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && apiKeys.any { it.isNotBlank() }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Get API key link
        TextButton(
            onClick = { /* Open browser to get API key */ }
        ) {
            Text("Get API Key from Google AI Studio →")
        }
    }
}
