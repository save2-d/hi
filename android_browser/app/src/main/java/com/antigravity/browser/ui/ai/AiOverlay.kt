package com.antigravity.browser.ui.ai

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.browser.core.BrowserViewModel
import com.antigravity.browser.core.ChatMessage

@Composable
fun AiChatOverlay(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val isVisible by viewModel.aiOverlayVisible.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isProcessing by viewModel.isAiProcessing.collectAsState()
    val currentModel by viewModel.apiKeyManager.currentModel.collectAsState()
    val activeKeyIndex by viewModel.activeKeyIndex.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Floating AI button
        AnimatedVisibility(
            visible = !isVisible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.toggleAiOverlay() },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Open AI Chat",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Chat overlay
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
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
                                    text = "🤖 AI Assistant",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentModel.substringAfter("gemini-")} • Key ${activeKeyIndex + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            
                            IconButton(onClick = { viewModel.toggleAiOverlay() }) {
                                Icon(Icons.Default.Close, "Close")
                            }
                        }
                    }
                    
                    // Messages
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatMessages) { message ->
                            ChatBubble(message)
                        }
                        
                        if (isProcessing) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Thinking...")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Input area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Ask AI or give command...") },
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        FilledIconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    viewModel.processAiInput(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && !isProcessing,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(Icons.Default.Send, "Send")
                        }
                    }
                    
                    // Quick actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = { viewModel.processAiInput("summarize this page") },
                            label = { Text("Summarize") }
                        )
                        SuggestionChip(
                            onClick = { viewModel.processAiInput("speed 2x") },
                            label = { Text("Speed 2x") }
                        )
                        SuggestionChip(
                            onClick = { viewModel.processAiInput("scroll down") },
                            label = { Text("Scroll") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
