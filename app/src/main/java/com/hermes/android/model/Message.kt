// Hermes Android Client - Message Model
// This file is part of the Hermes Android project

package com.hermes.android.model

import com.google.gson.annotations.SerializedName

/**
 * Represents a chat message
 */
data class Message(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("timestamp")
    val timestamp: Double? = null,
    @SerializedName("token_count")
    val tokenCount: Int = 0,
    @SerializedName("cost")
    val cost: Double = 0.0,
    @SerializedName("message_id")
    val messageId: String? = null,
    @SerializedName("stream_id")
    val streamId: String? = null,
    @SerializedName("seq")
    val seq: Int = 0,
    @SerializedName("event_id")
    val eventId: String? = null,
    @SerializedName("hidden")
    val hidden: Boolean = false
) {
    /**
     * Check if message is from user
     */
    fun isUserMessage(): Boolean {
        return role == "user"
    }

    /**
     * Check if message is from assistant
     */
    fun isAssistantMessage(): Boolean {
        return role == "assistant"
    }

    /**
     * Check if message is a system message
     */
    fun isSystemMessage(): Boolean {
        return role == "system"
    }
}

/**
 * Message role enum
 */
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

/**
 * Streaming message event
 */
data class StreamMessageEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("text")
    val text: String? = null,
    @SerializedName("stream_id")
    val streamId: String? = null,
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName("message_id")
    val messageId: String? = null,
    @SerializedName("seq")
    val seq: Int = 0,
    @SerializedName("event_id")
    val eventId: String? = null,
    @SerializedName("done")
    val done: Boolean = false
)

/**
 * Tool call event
 */
data class ToolCallEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("tool_name")
    val toolName: String? = null,
    @SerializedName("arguments")
    val arguments: Map<String, Any>? = null,
    @SerializedName("call_id")
    val callId: String? = null,
    @SerializedName("stream_id")
    val streamId: String? = null
)

/**
 * Tool result event
 */
data class ToolResultEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("result")
    val result: String? = null,
    @SerializedName("call_id")
    val callId: String? = null,
    @SerializedName("stream_id")
    val streamId: String? = null
)

/**
 * Approval event
 */
data class ApprovalEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("prompt")
    val prompt: String? = null,
    @SerializedName("call_id")
    val callId: String? = null,
    @SerializedName("action")
    val action: String? = null
)

/**
 * Done event
 */
data class DoneEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("stream_id")
    val streamId: String? = null,
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName("message_id")
    val messageId: String? = null
)

/**
 * Stream end event
 */
data class StreamEndEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("stream_id")
    val streamId: String? = null,
    @SerializedName("session_id")
    val sessionId: String? = null
)

/**
 * Error event
 */
data class ErrorEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("stream_id")
    val streamId: String? = null
)

/**
 * Generic SSE event wrapper
 */
data class SseEvent(
    @SerializedName("type")
    val type: String,
    @SerializedName("data")
    val data: String? = null,
    @SerializedName("id")
    val id: String? = null
)
