package com.antigravity.browser.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.antigravity.browser.data.GeminiRepository

@Composable
fun FirstLaunchScreen(onComplete: () -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to ZZZ Browser", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "This browser features AI-powered features using Google Gemini API.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("(Optional) Add your Gemini API Key", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            label = { Text("Gemini API Key") },
            visualTransformation = if (showPassword) PasswordVisualTransformation() else PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Checkbox(
                checked = showPassword,
                onCheckedChange = { showPassword = it },
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Text("Show key", modifier = Modifier.align(Alignment.CenterVertically))
        }
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (apiKey.isNotBlank()) {
                        GeminiRepository.addApiKey(apiKey)
                    }
                    onComplete()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Continue")
            }
            TextButton(onClick = onComplete, modifier = Modifier.weight(1f)) {
                Text("Skip")
            }
        }
    }
}
