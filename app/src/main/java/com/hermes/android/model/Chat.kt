// Hermes Android Client - Chat Model
// This file is part of the Hermes Android project

package com.hermes.android.model

import com.google.gson.annotations.SerializedName

/**
 * Request to start a chat
 */
data class ChatStartRequest(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("profile")
    val profile: String? = null,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("attachments")
    val attachments: List<Any>? = null,
    @SerializedName("regenerate")
    val regenerate: Boolean = false,
    @SerializedName("regeneration_revision")
    val regenerationRevision: String? = null
)

/**
 * Response from starting a chat
 */
data class ChatStartResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("stream_id")
    val streamId: String? = null,
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName("message_id")
    val messageId: String? = null,
    @SerializedName("seq")
    val seq: Int = 0
)

/**
 * Stream status response
 */
data class StreamStatusResponse(
    @SerializedName("status")
    val status: String,
    @SerializedName("stream_id")
    val streamId: String? = null,
    @SerializedName("session_id")
    val sessionId: String? = null,
    @SerializedName("cancelled")
    val cancelled: Boolean = false
)

/**
 * Request to cancel a stream
 */
data class CancelStreamRequest(
    @SerializedName("stream_id")
    val streamId: String
)

/**
 * Response from cancelling a stream
 */
data class CancelStreamResponse(
    @SerializedName("ok")
    val ok: Boolean,
    @SerializedName("cancelled")
    val cancelled: Boolean,
    @SerializedName("stream_id")
    val streamId: String? = null
)

/**
 * Chat state enum
 */
enum class ChatState {
    IDLE, PROCESSING, WAITING, STREAMING, ERROR
}
