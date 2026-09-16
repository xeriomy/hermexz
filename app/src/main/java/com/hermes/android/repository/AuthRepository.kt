// Hermes Android Client - Authentication Repository
// This file is part of the Hermes Android project

package com.hermes.android.repository

import android.util.Log
import com.hermes.android.model.*
import com.hermes.android.network.HermesApi
import com.hermes.android.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository for managing authentication
 */
class AuthRepository(private val api: HermesApi) {
    private const val TAG = "AuthRepository"

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentSessionInfo: SessionInfo? = null
    private var serverConfig: ServerConfig? = null

    /**
     * Check server health
     */
    suspend fun checkHealth(baseUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            _connectionState.value = ConnectionState.Connecting

            val response = api.healthCheck()

            if (response.isSuccessful) {
                val healthData = response.body()
                val isHealthy = healthData?.get("status") == "ok"

                if (isHealthy) {
                    serverConfig = ServerConfig(
                        baseUrl = baseUrl,
                        isConnected = true,
                        connectionStatus = ConnectionStatus.CONNECTED
                    )
                    _connectionState.value = ConnectionState.Connected(baseUrl)
                } else {
                    serverConfig = ServerConfig(
                        baseUrl = baseUrl,
                        isConnected = false,
                        connectionStatus = ConnectionStatus.ERROR,
                        lastError = "Server not healthy"
                    )
                    _connectionState.value = ConnectionState.Error("Server not healthy")
                }

                Result.success(isHealthy)
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                serverConfig = ServerConfig(
                    baseUrl = baseUrl,
                    isConnected = false,
                    connectionStatus = ConnectionStatus.ERROR,
                    lastError = error
                )
                _connectionState.value = ConnectionState.Error(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking health", e)
            serverConfig = ServerConfig(
                baseUrl = baseUrl,
                isConnected = false,
                connectionStatus = ConnectionStatus.ERROR,
                lastError = e.message
            )
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Check authentication status
     */
    suspend fun checkAuthStatus(): Result<AuthStatusResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            _authState.value = AuthState.Checking

            val response = api.getAuthStatus()

            if (response.isSuccessful) {
                val authStatus = response.body()
                if (authStatus != null) {
                    if (authStatus.authEnabled && authStatus.loggedIn) {
                        currentSessionInfo = SessionInfo(
                            sessionToken = ApiClient.getSessionCookie(serverConfig?.baseUrl ?: "") ?: "",
                            authType = authStatus.authType,
                            username = authStatus.user,
                            boundProfile = authStatus.boundProfile
                        )
                        _authState.value = AuthState.Authenticated(authStatus)
                    } else if (authStatus.authEnabled) {
                        _authState.value = AuthState.RequiresLogin(authStatus)
                    } else {
                        _authState.value = AuthState.NoAuthRequired(authStatus)
                    }

                    Result.success(authStatus)
                } else {
                    _authState.value = AuthState.Error("Empty response")
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                _authState.value = AuthState.Error(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking auth status", e)
            _authState.value = AuthState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Login with password
     */
    suspend fun login(password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            _authState.value = AuthState.LoggingIn

            val response = api.login(password)

            if (response.isSuccessful) {
                val loginResponse = response.body()
                if (loginResponse != null) {
                    val sessionCookie = ApiClient.getSessionCookie(serverConfig?.baseUrl ?: "")
                    if (sessionCookie != null) {
                        currentSessionInfo = SessionInfo(
                            sessionToken = sessionCookie,
                            authType = "password",
                            username = null,
                            boundProfile = null
                        )
                        _authState.value = AuthState.Authenticated(
                            AuthStatusResponse(
                                authEnabled = true,
                                loggedIn = true,
                                authType = "password"
                            )
                        )
                    }
                    Result.success(loginResponse)
                } else {
                    _authState.value = AuthState.Error("Empty response")
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                _authState.value = AuthState.Error(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error logging in", e)
            _authState.value = AuthState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Logout
     */
    suspend fun logout(): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            _authState.value = AuthState.LoggingOut

            val response = api.logout()

            if (response.isSuccessful) {
                serverConfig?.baseUrl?.let { ApiClient.clearSessionCookie(it) }
                currentSessionInfo = null
                _authState.value = AuthState.LoggedOut
                Result.success(true)
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                _authState.value = AuthState.Error(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error logging out", e)
            _authState.value = AuthState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Set server configuration
     */
    fun setServerConfig(config: ServerConfig) {
        serverConfig = config
        if (config.isConnected) {
            _connectionState.value = ConnectionState.Connected(config.baseUrl)
        } else {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * Get current server config
     */
    fun getServerConfig(): ServerConfig? {
        return serverConfig
    }

    /**
     * Get current session info
     */
    fun getSessionInfo(): SessionInfo? {
        return currentSessionInfo
    }

    /**
     * Clear session
     */
    fun clearSession() {
        currentSessionInfo = null
        serverConfig?.baseUrl?.let { ApiClient.clearAllCookies(it) }
        _authState.value = AuthState.Idle
        _connectionState.value = ConnectionState.Disconnected
    }
}

/**
 * Authentication state
 */
sealed class AuthState {
    object Idle : AuthState()
    object Checking : AuthState()
    object LoggingIn : AuthState()
    object LoggingOut : AuthState()
    data class Authenticated(val authStatus: AuthStatusResponse) : AuthState()
    data class RequiresLogin(val authStatus: AuthStatusResponse) : AuthState()
    data class NoAuthRequired(val authStatus: AuthStatusResponse) : AuthState()
    data class Error(val message: String) : AuthState()
    object LoggedOut : AuthState()
}

/**
 * Connection state
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val baseUrl: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
