// Hermes Android Client - App Navigation
// This file is part of the Hermes Android project

package com.hermes.android.ui.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hermes.android.network.ApiClient
import com.hermes.android.network.HermesApi
import com.hermes.android.network.SseClient
import com.hermes.android.ui.viewmodel.ServerViewModel
import com.hermes.android.viewmodel.ConnectionState
import okhttp3.OkHttpClient

/**
 * Main navigation for the app
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val serverViewModel: ServerViewModel = viewModel()

    val connectionState by serverViewModel.connectionState.collectAsState()

    var api: HermesApi? by remember { mutableStateOf(null) }
    var sseClient: SseClient? by remember { mutableStateOf(null) }
    var okHttpClient: OkHttpClient? by remember { mutableStateOf(null) }

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ConnectionState.Connected -> {
                val serverConfig = serverViewModel.getServerConfig()
                serverConfig?.baseUrl?.let { baseUrl ->
                    try {
                        okHttpClient = ApiClient.createOkHttpClient(
                            serverViewModel.getApplication(),
                            baseUrl
                        )
                        api = ApiClient.createHermesApi(serverViewModel.getApplication(), baseUrl)
                        sseClient = SseClient(okHttpClient!!, baseUrl)
                    } catch (e: Exception) {
                        Log.e("AppNavigation", "Error creating API client", e)
                    }
                }
            }
            else -> {
                api = null
                sseClient = null
                okHttpClient = null
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.ServerConnection.route
    ) {
        composable(Screen.ServerConnection.route) {
            ServerConnectionScreen(
                viewModel = serverViewModel,
                onConnectSuccess = {
                    navController.navigate(Screen.Sessions.route) {
                        popUpTo(Screen.ServerConnection.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Sessions.route) {
            SessionsScreen(
                serverViewModel = serverViewModel,
                api = api,
                onSessionSelected = { sessionId ->
                    navController.navigate("${Screen.Chat.route}/$sessionId")
                },
                onNewSession = { sessionId ->
                    navController.navigate("${Screen.Chat.route}/$sessionId")
                },
                onDisconnect = {
                    navController.navigate(Screen.ServerConnection.route) {
                        popUpTo(Screen.Sessions.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable("${Screen.Chat.route}/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""

            ChatScreen(
                sessionId = sessionId,
                api = api,
                sseClient = sseClient,
                okHttpClient = okHttpClient,
                onBack = {
                    navController.popBackStack()
                },
                onDisconnect = {
                    navController.navigate(Screen.ServerConnection.route) {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}

/**
 * Screen destinations
 */
sealed class Screen(val route: String) {
    object ServerConnection : Screen("server_connection")
    object Sessions : Screen("sessions")
    object Chat : Screen("chat")
}
