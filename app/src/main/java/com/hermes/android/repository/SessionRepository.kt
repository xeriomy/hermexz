// Hermes Android Client - Session Repository
// This file is part of the Hermes Android project

package com.hermes.android.repository

import android.util.Log
import com.hermes.android.model.*
import com.hermes.android.network.HermesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository for managing session data
 */
class SessionRepository(private val api: HermesApi) {
    private const val TAG = "SessionRepository"

    private val _sessionsState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionsState: StateFlow<SessionState> = _sessionsState.asStateFlow()

    private val _currentSessionState = MutableStateFlow<FullSession?>(null)
    val currentSessionState: StateFlow<FullSession?> = _currentSessionState.asStateFlow()

    private val _sessionCreationState = MutableStateFlow<SessionCreationState>(SessionCreationState.Idle)
    val sessionCreationState: StateFlow<SessionCreationState> = _sessionCreationState.asStateFlow()

    private var cachedSessions: List<Session> = emptyList()

    /**
     * Load sessions from API
     */
    suspend fun loadSessions(forceRefresh: Boolean = false): Result<List<Session>> = withContext(Dispatchers.IO) {
        return@withContext try {
            _sessionsState.value = SessionState.Loading

            val response = api.listSessions(
                includeArchived = false,
                excludeHidden = true
            )

            if (response.isSuccessful) {
                val sessionListResponse = response.body()
                val sessions = sessionListResponse?.sessions ?: emptyList()

                cachedSessions = sessions
                _sessionsState.value = SessionState.Success(sessions)

                Result.success(sessions)
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                _sessionsState.value = SessionState.Error(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading sessions", e)
            _sessionsState.value = SessionState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Get cached sessions
     */
    fun getCachedSessions(): List<Session> {
        return cachedSessions
    }

    /**
     * Create a new session
     */
    suspend fun createSession(title: String, profile: String? = null): Result<CreateSessionResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            _sessionCreationState.value = SessionCreationState.Loading

            val request = CreateSessionRequest(title, profile)
            val response = api.createSession(request)

            if (response.isSuccessful) {
                val createResponse = response.body()
                if (createResponse != null) {
                    _sessionCreationState.value = SessionCreationState.Success(createResponse)
                    Result.success(createResponse)
                } else {
                    _sessionCreationState.value = SessionCreationState.Error("Empty response")
                    Result.failure(Exception("Empty response"))
                }
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                _sessionCreationState.value = SessionCreationState.Error(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating session", e)
            _sessionCreationState.value = SessionCreationState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    /**
     * Load a specific session with messages
     */
    suspend fun loadSession(sessionId: String): Result<FullSession> = withContext(Dispatchers.IO) {
        return@withContext try {
            _currentSessionState.value = null

            val response = api.getSession(
                sessionId = sessionId,
                messages = 1,
                msgLimit = null,
                msgBefore = null
            )

            if (response.isSuccessful) {
                val session = response.body()
                if (session != null) {
                    _currentSessionState.value = session
                    Result.success(session)
                } else {
                    Result.failure(Exception("Session not found"))
                }
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading session", e)
            Result.failure(e)
        }
    }

    /**
     * Get session status
     */
    suspend fun getSessionStatus(sessionId: String): Result<SessionStatusResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = api.getSessionStatus(sessionId)

            if (response.isSuccessful) {
                val status = response.body()
                if (status != null) {
                    Result.success(status)
                } else {
                    Result.failure(Exception("Status not found"))
                }
            } else {
                val error = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting session status", e)
            Result.failure(e)
        }
    }

    /**
     * Refresh sessions
     */
    suspend fun refreshSessions(): Result<List<Session>> {
        return loadSessions(forceRefresh = true)
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        cachedSessions = emptyList()
    }
}

/**
 * State for session loading
 */
sealed class SessionState {
    object Idle : SessionState()
    object Loading : SessionState()
    data class Success(val sessions: List<Session>) : SessionState()
    data class Error(val message: String) : SessionState()
}

/**
 * State for session creation
 */
sealed class SessionCreationState {
    object Idle : SessionCreationState()
    object Loading : SessionCreationState()
    data class Success(val response: CreateSessionResponse) : SessionCreationState()
    data class Error(val message: String) : SessionCreationState()
}
