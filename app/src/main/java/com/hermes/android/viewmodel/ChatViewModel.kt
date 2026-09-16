// Hermes Android Client - Chat ViewModel
// This file is part of the Hermes Android project

package com.hermes.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.model.Message
import com.hermes.android.model.FullSession
import com.hermes.android.model.ChatState
import com.hermes.android.model.SseEvent
import com.hermes.android.model.StreamMessageEvent
import com.hermes.android.model.DoneEvent
import com.hermes.android.model.StreamEndEvent
import com.hermes.android.model.ErrorEvent
import com.hermes.android.network.HermesApi
import com.hermes.android.network.SseClient
import com.hermes.android.network.SseEventType
import com.hermes.android.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing chat operations
 */
class ChatViewModel(
    application: Application,
    private val api: HermesApi,
    private val sseClient: SseClient
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val chatRepository: ChatRepository = ChatRepository(api, sseClient)

    private val _messagesState = MutableStateFlow<List<Message>>(emptyList())
    val messagesState: StateFlow<List<Message>> = _messagesState.asStateFlow()

    private val _streamingMessageState = MutableStateFlow<String?>(null)
    val streamingMessageState: StateFlow<String?> = _streamingMessageState.asStateFlow()

    private val _chatState = MutableStateFlow<ChatState>(ChatState.IDLE)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val _chatStartState = MutableStateFlow<ChatStartState>(ChatStartState.Idle)
    val chatStartState: StateFlow<ChatStartState> = _chatStartState.asStateFlow()

    private val _currentSessionState = MutableStateFlow<FullSession?>(null)
    val currentSessionState: StateFlow<FullSession?> = _currentSessionState.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private val _streamEvents = MutableStateFlow<SseEvent?>(null)
    val streamEvents: StateFlow<SseEvent?> = _streamEvents.asStateFlow()

    private var currentSessionId: String? = null

    init {
        viewModelScope.launch {
            chatRepository.chatState.collect { state ->
                _chatState.value = state
            }
        }

        viewModelScope.launch {
            chatRepository.streamingMessageState.collect { message ->
                _streamingMessageState.value = message
            }
        }

        viewModelScope.launch {
            chatRepository.chatStartState.collect { state ->
                when (state) {
                    ChatStartState.Idle -> {}
                    ChatStartState.Loading -> {}
                    is ChatStartState.Success -> {
                        _chatStartState.value = state
                    }
                    is ChatStartState.Error -> {
                        _chatStartState.value = state
                        _errorState.value = state.message
                    }
                }
            }
        }

        viewModelScope.launch {
            chatRepository.currentSessionState.collect { session ->
                _currentSessionState.value = session
                if (session != null) {
                    _messagesState.value = session.messages
                }
            }
        }
    }

    /**
     * Load session with messages
     */
    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _errorState.value = null
            currentSessionId = sessionId

            try {
                val result = withContext(Dispatchers.IO) {
                    chatRepository.loadSession(sessionId)
                }

                result.onSuccess { session ->
                    _currentSessionState.value = session
                    _messagesState.value = session.messages
                }.onFailure { e ->
                    _errorState.value = e.message ?: "Failed to load session"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading session", e)
                _errorState.value = e.message ?: "Failed to load session"
            }
        }
    }

    /**
     * Send a message
     */
    fun sendMessage(message: String, profile: String? = null, model: String? = null) {
        viewModelScope.launch {
            _errorState.value = null

            if (message.isBlank()) {
                _errorState.value = "Message cannot be empty"
                return@launch
            }

            val sessionId = currentSessionId ?: run {
                _errorState.value = "No session selected"
                return@launch
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    chatRepository.startChat(sessionId, message, profile, model)
                }

                result.onSuccess { response ->
                    _chatState.value = ChatState.STREAMING
                    startStreaming()
                }.onFailure { e ->
                    _errorState.value = e.message ?: "Failed to send message"
                    _chatState.value = ChatState.ERROR
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                _errorState.value = e.message ?: "Failed to send message"
                _chatState.value = ChatState.ERROR
            }
        }
    }

    /**
     * Start streaming
     */
    private fun startStreaming() {
        viewModelScope.launch {
            try {
                chatRepository.startStreaming()
                    .catch { e ->
                        Log.e(TAG, "Error in stream", e)
                        _errorState.value = e.message ?: "Stream error"
                    }
                    .collect { event ->
                        _streamEvents.value = event
                        handleStreamEvent(event)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting stream", e)
                _errorState.value = e.message ?: "Failed to start stream"
            }
        }
    }

    /**
     * Stop current stream
     */
    fun stopStream() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    chatRepository.stopStream()
                }

                result.onSuccess {
                    _chatState.value = ChatState.IDLE
                    _streamingMessageState.value = null
                }.onFailure { e ->
                    _errorState.value = e.message ?: "Failed to stop stream"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping stream", e)
                _errorState.value = e.message ?: "Failed to stop stream"
            }
        }
    }

    /**
     * Clear current session
     */
    fun clearCurrentSession() {
        chatRepository.clearCurrentSession()
        currentSessionId = null
        _currentSessionState.value = null
        _messagesState.value = emptyList()
        _streamingMessageState.value = null
        _chatState.value = ChatState.IDLE
        _errorState.value = null
    }

    /**
     * Set current session
     */
    fun setCurrentSession(session: FullSession) {
        currentSessionId = session.sessionId
        _currentSessionState.value = session
        _messagesState.value = session.messages
    }

    /**
     * Add message to list
     */
    fun addMessage(message: Message) {
        val currentMessages = _messagesState.value.toMutableList()
        currentMessages.add(message)
        _messagesState.value = currentMessages
    }

    /**
     * Handle stream events
     */
    private fun handleStreamEvent(event: SseEvent) {
        when (event.type) {
            SseEventType.MESSAGE -> {
                val messageEvent = Gson().fromJson(event.data, StreamMessageEvent::class.java)
                val text = messageEvent?.text ?: ""
                if (messageEvent?.done == true) {
                    _streamingMessageState.value = null
                    _chatState.value = ChatState.IDLE
                } else {
                    _streamingMessageState.value = text
                    _chatState.value = ChatState.STREAMING
                }
            }
            SseEventType.DONE -> {
                _streamingMessageState.value = null
                _chatState.value = ChatState.IDLE
            }
            SseEventType.STREAM_END -> {
                _streamingMessageState.value = null
                _chatState.value = ChatState.IDLE
            }
            SseEventType.ERROR -> {
                val errorEvent = Gson().fromJson(event.data, ErrorEvent::class.java)
                _streamingMessageState.value = null
                _chatState.value = ChatState.ERROR
                _errorState.value = errorEvent?.error ?: "Stream error"
            }
            SseEventType.TOOL_CALL -> {}
            SseEventType.TOOL_RESULT -> {}
            SseEventType.APPROVAL -> {}
            SseEventType.INITIAL -> {}
            SseEventType.SERVER_TURN_STARTED -> {}
            else -> {
                Log.d(TAG, "Unhandled event type: ${event.type}")
            }
        }
    }

    /**
     * Get repository for more advanced operations
     */
    fun getRepository(): ChatRepository {
        return chatRepository
    }
}

/**
 * Chat start state for UI
 */
sealed class ChatStartState {
    object Idle : ChatStartState()
    object Loading : ChatStartState()
    data class Success(val response: com.hermes.android.model.ChatStartResponse) : ChatStartState()
    data class Error(val message: String) : ChatStartState()
}
