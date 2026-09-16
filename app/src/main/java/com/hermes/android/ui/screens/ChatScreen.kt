// Hermes Android Client - Chat Screen
// This file is part of the Hermes Android project

package com.hermes.android.ui.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.android.model.ChatState
import com.hermes.android.model.FullSession
import com.hermes.android.model.Message
import com.hermes.android.network.HermesApi
import com.hermes.android.network.SseClient
import com.hermes.android.repository.ChatRepository
import com.hermes.android.ui.components.MessageBubble
import com.hermes.android.ui.components.StreamingMessage
import com.hermes.android.repository.ChatStartState
import com.hermes.android.viewmodel.ChatViewModel
import com.hermes.android.viewmodel.ChatViewModelFactory
import okhttp3.OkHttpClient

/**
 * Chat screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    api: HermesApi?,
    sseClient: SseClient?,
    okHttpClient: OkHttpClient?,
    onBack: () -> Unit,
    onDisconnect: () -> Unit
) {
    if (api == null || sseClient == null || okHttpClient == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Not connected to server")
            Button(onClick = onDisconnect) {
                Text("Back to Connection")
            }
        }
        return
    }

    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            api = api!!,
            sseClient = sseClient!!
        )
    )

    val messages by viewModel.messagesState.collectAsState()
    val streamingMessage by viewModel.streamingMessageState.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Chat") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        val listState = rememberLazyListState()

        @OptIn(ExperimentalFoundationApi::class)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }

            streamingMessage?.let { text ->
                if (text.isNotBlank()) {
                    item {
                        StreamingMessage(text = text)
                    }
                }
            }
        }

        errorState?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Message") },
                placeholder = { Text("Type a message...") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (messageText.isNotBlank()) {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                            focusManager.clearFocus()
                        }
                    }
                ),
                singleLine = false,
                minLines = 1,
                maxLines = 5,
                modifier = Modifier.weight(1f),
                enabled = chatState == ChatState.IDLE || chatState == ChatState.ERROR
            )

            when (chatState) {
                ChatState.STREAMING -> {
                    IconButton(
                        onClick = { viewModel.stopStream() },
                        enabled = true
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
                else -> {
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
