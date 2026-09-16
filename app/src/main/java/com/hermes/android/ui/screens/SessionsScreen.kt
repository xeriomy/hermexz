// Hermes Android Client - Sessions Screen
// This file is part of the Hermes Android project

package com.hermes.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.android.model.Session
import com.hermes.android.network.HermesApi
import com.hermes.android.repository.SessionCreationState
import com.hermes.android.ui.components.SessionCard
import com.hermes.android.viewmodel.ServerViewModel
import com.hermes.android.viewmodel.SessionListState
import com.hermes.android.viewmodel.SessionViewModel
import com.hermes.android.viewmodel.SessionViewModelFactory
import kotlinx.coroutines.launch

/**
 * Sessions screen
 */
@Composable
fun SessionsScreen(
    serverViewModel: ServerViewModel,
    api: HermesApi?,
    onSessionSelected: (String) -> Unit,
    onNewSession: (String) -> Unit,
    onDisconnect: () -> Unit
) {
    if (api == null) {
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

    val viewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(serverViewModel.getApplication(), api)
    )

    val sessionsState by viewModel.sessionsState.collectAsState()
    val errorState by viewModel.errorState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var newSessionTitle by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Sessions") },
            navigationIcon = {
                IconButton(onClick = onDisconnect) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refreshSessions() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = { showNewSessionDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "New Session")
                }
            }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search sessions") },
            placeholder = { Text("Enter search term") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        when (val state = sessionsState) {
            SessionListState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading sessions...")
                }
            }
            SessionListState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is SessionListState.Success -> {
                val filteredSessions = if (searchQuery.isBlank()) {
                    state.sessions
                } else {
                    state.sessions.filter {
                        it.title.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (filteredSessions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No sessions found")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSessions) { session ->
                            SessionCard(
                                session = session,
                                onClick = { onSessionSelected(session.sessionId) }
                            )
                        }
                    }
                }
            }
            is SessionListState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadSessions() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    if (showNewSessionDialog) {
        AlertDialog(
            onDismissRequest = { showNewSessionDialog = false },
            title = { Text("Create New Session") },
            text = {
                OutlinedTextField(
                    value = newSessionTitle,
                    onValueChange = { newSessionTitle = it },
                    label = { Text("Session Title") },
                    placeholder = { Text("Enter title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSessionTitle.isNotBlank()) {
                            viewModel.createSession(newSessionTitle)
                            showNewSessionDialog = false
                            newSessionTitle = ""
                        }
                    },
                    enabled = newSessionTitle.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val sessionCreationState by viewModel.sessionCreationState.collectAsState()

    LaunchedEffect(sessionCreationState) {
        when (sessionCreationState) {
            is com.hermes.android.repository.SessionCreationState.Success -> {
                val newSession = (sessionCreationState as com.hermes.android.repository.SessionCreationState.Success)
                onNewSession(newSession.response.sessionId)
            }
            else -> {}
        }
    }
}
