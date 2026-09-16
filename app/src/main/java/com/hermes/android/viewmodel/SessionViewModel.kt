// Hermes Android Client - Session ViewModel
// This file is part of the Hermes Android project

package com.hermes.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.model.Session
import com.hermes.android.model.CreateSessionResponse
import com.hermes.android.network.HermesApi
import com.hermes.android.repository.SessionRepository
import com.hermes.android.repository.SessionState
import com.hermes.android.repository.SessionCreationState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing sessions
 */
class SessionViewModel(
    application: Application,
    private val api: HermesApi
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SessionViewModel"
    }

    private val sessionRepository: SessionRepository = SessionRepository(api)

    private val _sessionsState = MutableStateFlow<SessionListState>(SessionListState.Idle)
    val sessionsState: StateFlow<SessionListState> = _sessionsState.asStateFlow()

    private val _selectedSessionState = MutableStateFlow<Session?>(null)
    val selectedSessionState: StateFlow<Session?> = _selectedSessionState.asStateFlow()

    private val _sessionCreationState = MutableStateFlow<SessionCreationState>(SessionCreationState.Idle)
    val sessionCreationState: StateFlow<SessionCreationState> = _sessionCreationState.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionsState.collect { state ->
                when (state) {
                    is SessionState.Idle -> {
                        _sessionsState.value = SessionListState.Idle
                    }
                    is SessionState.Loading -> {
                        _sessionsState.value = SessionListState.Loading
                    }
                    is SessionState.Success -> {
                        _sessionsState.value = SessionListState.Success(state.sessions)
                    }
                    is SessionState.Error -> {
                        _sessionsState.value = SessionListState.Error(state.message)
                        _errorState.value = state.message
                    }
                }
            }
        }

        viewModelScope.launch {
            sessionRepository.sessionCreationState.collect { state ->
                when (state) {
                    is SessionCreationState.Idle -> {
                        _sessionCreationState.value = SessionCreationState.Idle
                    }
                    is SessionCreationState.Loading -> {
                        _sessionCreationState.value = SessionCreationState.Loading
                    }
                    is SessionCreationState.Success -> {
                        _sessionCreationState.value = SessionCreationState.Success(state.response)
                    }
                    is SessionCreationState.Error -> {
                        _sessionCreationState.value = SessionCreationState.Error(state.message)
                        _errorState.value = state.message
                    }
                }
            }
        }
    }

    /**
     * Load sessions
     */
    fun loadSessions() {
        viewModelScope.launch {
            _errorState.value = null
            sessionRepository.loadSessions()
        }
    }

    /**
     * Create a new session
     */
    fun createSession(title: String, profile: String? = null) {
        viewModelScope.launch {
            _errorState.value = null
            sessionRepository.createSession(title, profile)
        }
    }

    /**
     * Select a session
     */
    fun selectSession(session: Session) {
        _selectedSessionState.value = session
        _errorState.value = null
    }

    /**
     * Clear selected session
     */
    fun clearSelectedSession() {
        _selectedSessionState.value = null
    }

    /**
     * Refresh sessions
     */
    fun refreshSessions() {
        viewModelScope.launch {
            _errorState.value = null
            sessionRepository.refreshSessions()
        }
    }

    /**
     * Get cached sessions
     */
    fun getCachedSessions(): List<Session> {
        return sessionRepository.getCachedSessions()
    }

    /**
     * Get repository for more advanced operations
     */
    fun getRepository(): SessionRepository {
        return sessionRepository
    }
}

/**
 * Session list state for UI
 */
sealed class SessionListState {
    object Idle : SessionListState()
    object Loading : SessionListState()
    data class Success(val sessions: List<Session>) : SessionListState()
    data class Error(val message: String) : SessionListState()
}
