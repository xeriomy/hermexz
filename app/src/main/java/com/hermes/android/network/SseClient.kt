// Hermes Android Client - SSE Client
// This file is part of the Hermes Android project

package com.hermes.android.network

import android.util.Log
import com.hermes.android.model.SseEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SSE Client for handling Server-Sent Events from Hermes WebUI
 */
class SseClient(
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String
) {
    companion object {
        private const val TAG = "SseClient"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val activeStreams = mutableMapOf<String, StreamHandler>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Start listening to chat stream
     */
    fun chatStream(
        streamId: String,
        afterEventId: String? = null,
        afterSeq: Int? = null
    ): Flow<SseEvent> {
        val handler = StreamHandler(streamId)
        activeStreams[streamId] = handler

        val url = buildChatStreamUrl(streamId, afterEventId, afterSeq)
        startSseConnection(url, handler, streamId)

        return handler.eventFlow
    }

    /**
     * Start listening to session stream
     */
    fun sessionStream(
        sessionId: String,
        knownCount: Int? = null
    ): Flow<SseEvent> {
        val handler = StreamHandler(sessionId)
        activeStreams[sessionId] = handler

        val url = buildSessionStreamUrl(sessionId, knownCount)
        startSseConnection(url, handler, sessionId)

        return handler.eventFlow
    }

    /**
     * Stop listening to a specific stream
     */
    fun stopStream(streamId: String) {
        activeStreams.remove(streamId)?.let { handler ->
            handler.close()
        }
    }

    /**
     * Stop all active streams
     */
    fun stopAllStreams() {
        activeStreams.values.forEach { it.close() }
        activeStreams.clear()
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopAllStreams()
        scope.cancel()
    }

    private fun buildChatStreamUrl(
        streamId: String,
        afterEventId: String? = null,
        afterSeq: Int? = null
    ): String {
        val builder = StringBuilder("$baseUrl/api/chat/stream?stream_id=$streamId")

        afterEventId?.let { builder.append("&after_event_id=$it") }
        afterSeq?.let { builder.append("&after_seq=$it") }

        return builder.toString()
    }

    private fun buildSessionStreamUrl(
        sessionId: String,
        knownCount: Int? = null
    ): String {
        val builder = StringBuilder("$baseUrl/api/session/stream?session_id=$sessionId")

        knownCount?.let { builder.append("&known_count=$it") }

        return builder.toString()
    }

    private fun startSseConnection(
        url: String,
        handler: StreamHandler,
        streamId: String
    ) {
        scope.launch {
            var lastEventId: String? = null
            var reconnectCount = 0

            while (isActive && activeStreams.containsKey(streamId)) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .header("Accept", "text/event-stream")
                        .header("Cache-Control", "no-cache")
                        .header("X-Accel-Buffering", "no")
                        .apply {
                            lastEventId?.let { header("Last-Event-ID", it) }
                        }
                        .build()

                    Log.d(TAG, "Connecting to SSE: $url")

                    val eventSource = EventSources.createFactory(okHttpClient).newEventSource(request, object : EventSourceListener() {
                        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                            try {
                                lastEventId = id
                                val sseEvent = SseEvent(
                                    type = type ?: "",
                                    data = data,
                                    id = id
                                )
                                handler.emit(sseEvent)
                                reconnectCount = 0
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing SSE event", e)
                            }
                        }

                        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                            Log.e(TAG, "SSE connection error: ${t?.message}")
                            eventSource.cancel()
                        }

                        override fun onOpen(eventSource: EventSource, response: Response) {
                            Log.d(TAG, "SSE connection opened")
                        }

                        override fun onClosed(eventSource: EventSource) {
                            Log.d(TAG, "SSE connection closed")
                        }
                    })

                    // Keep the connection alive while active
                    while (isActive && activeStreams.containsKey(streamId)) {
                        kotlinx.coroutines.delay(1000)
                    }
                    eventSource.cancel()

                } catch (e: Exception) {
                    Log.e(TAG, "SSE connection error (attempt ${reconnectCount + 1}): ${e.message}")

                    if (!activeStreams.containsKey(streamId)) {
                        break
                    }

                    reconnectCount++
                    val delay = minOf(RECONNECT_DELAY_MS * reconnectCount, 30000L)

                    try {
                        kotlinx.coroutines.delay(delay)
                    } catch (ce: Exception) {
                        break
                    }
                }
            }

            Log.d(TAG, "SSE connection stopped for: $streamId")
        }
    }

    /**
     * Handler for managing a single SSE stream
     */
    private inner class StreamHandler(private val streamId: String) {
        private val _eventChannel = Channel<SseEvent>(Channel.UNLIMITED)
        val eventFlow = _eventChannel.receiveAsFlow()

        private val isClosed = AtomicBoolean(false)

        fun emit(event: SseEvent) {
            if (!isClosed.get() && _eventChannel.isClosedForSend.not()) {
                try {
                    scope.launch {
                        _eventChannel.send(event)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error emitting event to channel", e)
                }
            }
        }

        fun close() {
            if (isClosed.compareAndSet(false, true)) {
                try {
                    _eventChannel.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing channel", e)
                }
            }
        }
    }
}

/**
 * SSE Event types
 */
object SseEventType {
    const val MESSAGE = "message"
    const val TOOL_CALL = "tool_call"
    const val TOOL_RESULT = "tool_result"
    const val APPROVAL = "approval"
    const val DONE = "done"
    const val STREAM_END = "stream_end"
    const val ERROR = "error"
    const val INITIAL = "initial"
    const val SERVER_TURN_STARTED = "server_turn_started"
    const val BG_TASK_COMPLETE = "bg_task_complete"
    const val SESSION_SNAPSHOT = "session_snapshot"
    const val SESSION_UPDATED = "session_updated"
    const val HEARTBEAT = "heartbeat"
}
