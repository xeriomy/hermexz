// Hermes Android Client - Server ViewModel
// This file is part of the Hermes Android project

package com.hermes.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.model.ConnectionStatus
import com.hermes.android.model.ServerConfig
import com.hermes.android.network.ApiClient
import com.hermes.android.network.HermesApi
import com.hermes.android.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing server connection
 */
class ServerViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ServerViewModel"
    }

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private var api: HermesApi? = null
    private var authRepository: AuthRepository? = null
    private var serverConfig: ServerConfig? = null

    init {
        loadSavedServerUrl()
    }

    /**
     * Set server URL
     */
    fun setServerUrl(url: String) {
        _serverUrl.value = url.trim()
        _errorState.value = null
    }

    /**
     * Connect to server
     */
    fun connect() {
        viewModelScope.launch {
            val url = _serverUrl.value.trim()

            if (url.isEmpty()) {
                _errorState.value = "Server URL is required"
                return@launch
            }

            if (!isValidUrl(url)) {
                _errorState.value = "Invalid server URL"
                return@launch
            }

            _connectionState.value = ConnectionState.Connecting
            _errorState.value = null

            try {
                api = ApiClient.createHermesApi(application, url)
                authRepository = AuthRepository(api!!)

                val result = withContext(Dispatchers.IO) {
                    api?.healthCheck()
                }

                if (result?.isSuccessful == true) {
                    val healthData = result.body()
                    val isHealthy = healthData?.get("status") == "ok"

                    if (isHealthy) {
                        serverConfig = ServerConfig(
                            baseUrl = url,
                            isConnected = true,
                            connectionStatus = ConnectionStatus.CONNECTED
                        )
                        _connectionState.value = ConnectionState.Connected(url)

                        saveServerUrl(url)
                        checkAuthStatus()
                    } else {
                        serverConfig = ServerConfig(
                            baseUrl = url,
                            isConnected = false,
                            connectionStatus = ConnectionStatus.ERROR,
                            lastError = "Server not healthy"
                        )
                        _connectionState.value = ConnectionState.Error("Server not healthy")
                        _errorState.value = "Server not healthy"
                    }
                } else {
                    val error = result?.errorBody()?.string() ?: "Connection failed"
                    serverConfig = ServerConfig(
                        baseUrl = url,
                        isConnected = false,
                        connectionStatus = ConnectionStatus.ERROR,
                        lastError = error
                    )
                    _connectionState.value = ConnectionState.Error(error)
                    _errorState.value = error
                }

            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                serverConfig = ServerConfig(
                    baseUrl = url,
                    isConnected = false,
                    connectionStatus = ConnectionStatus.ERROR,
                    lastError = e.message
                )
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                _errorState.value = e.message ?: "Connection failed"
            }
        }
    }

    /**
     * Disconnect from server
     */
    fun disconnect() {
        api = null
        authRepository = null
        serverConfig = null
        _connectionState.value = ConnectionState.Disconnected
        _errorState.value = null
    }

    /**
     * Check authentication status
     */
    fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                authRepository?.checkAuthStatus()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auth status", e)
            }
        }
    }

    /**
     * Get Application context
     */
    fun getApplication(): Application {
        return super.getApplication()
    }

    /**
     * Get API instance
     */
    fun getApi(): HermesApi? {
        return api
    }

    /**
     * Get auth repository
     */
    fun getAuthRepository(): AuthRepository? {
        return authRepository
    }

    /**
     * Get server config
     */
    fun getServerConfig(): ServerConfig? {
        return serverConfig
    }

    /**
     * Validate URL format
     */
    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    /**
     * Save server URL to preferences
     */
    private fun saveServerUrl(url: String) {
        val prefs = application.getSharedPreferences("HermesPrefs", 0)
        prefs.edit().putString("server_url", url).apply()
    }

    /**
     * Load saved server URL from preferences
     */
    private fun loadSavedServerUrl() {
        val prefs = application.getSharedPreferences("HermesPrefs", 0)
        val savedUrl = prefs.getString("server_url", "")
        if (!savedUrl.isNullOrEmpty()) {
            _serverUrl.value = savedUrl
        }
    }
}

/**
 * Connection state for UI
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val serverUrl: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
