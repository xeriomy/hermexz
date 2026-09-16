// Hermes Android Client - Server Connection Screen
// This file is part of the Hermes Android project

package com.hermes.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hermes.android.ui.viewmodel.ServerViewModel
import com.hermes.android.viewmodel.ConnectionState
import kotlinx.coroutines.launch

/**
 * Server connection screen
 */
@Composable
fun ServerConnectionScreen(
    viewModel: ServerViewModel,
    onConnectSuccess: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hermes Android",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Server URL",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { viewModel.setServerUrl(it) },
                    label = { Text("Enter Hermes WebUI URL") },
                    placeholder = { Text("e.g., http://192.168.1.100:8788") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    singleLine = true,
                    isError = errorState != null,
                    modifier = Modifier.fillMaxWidth()
                )

                errorState?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.connect()
                        }
                    },
                    enabled = serverUrl.isNotEmpty() && connectionState !is ConnectionState.Connecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (connectionState) {
                        ConnectionState.Connecting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Connecting...")
                        }
                        else -> {
                            Text("Connect")
                        }
                    }
                }

                ConnectionStatusIndicator(connectionState = connectionState)
            }
        }

        LaunchedEffect(connectionState) {
            if (connectionState is ConnectionState.Connected) {
                onConnectSuccess()
            }
        }
    }
}

/**
 * Connection status indicator
 */
@Composable
fun ConnectionStatusIndicator(connectionState: ConnectionState) {
    val (text, icon, color) = when (connectionState) {
        ConnectionState.Disconnected -> {
            Triple("Disconnected", Icons.Default.Close, MaterialTheme.colorScheme.error)
        }
        ConnectionState.Connecting -> {
            Triple("Connecting...", Icons.Default.Warning, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is ConnectionState.Connected -> {
            Triple("Connected", Icons.Default.Check, MaterialTheme.colorScheme.primary)
        }
        is ConnectionState.Error -> {
            Triple(connectionState.message, Icons.Default.Close, MaterialTheme.colorScheme.error)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
