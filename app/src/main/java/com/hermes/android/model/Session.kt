// Hermes Android Client - Session Model
// This file is part of the Hermes Android project

package com.hermes.android.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Represents a Hermes session from the API
 */
data class Session(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("profile")
    val profile: String? = null,
    @SerializedName("created_at")
    val createdAt: Double? = null,
    @SerializedName("updated_at")
    val updatedAt: Double? = null,
    @SerializedName("message_count")
    val messageCount: Int = 0,
    @SerializedName("input_tokens")
    val inputTokens: Long = 0,
    @SerializedName("output_tokens")
    val outputTokens: Long = 0,
    @SerializedName("estimated_cost")
    val estimatedCost: Double = 0.0,
    @SerializedName("source")
    val source: String? = null,
    @SerializedName("source_tag")
    val sourceTag: String? = null,
    @SerializedName("source_label")
    val sourceLabel: String? = null,
    @SerializedName("pinned")
    val pinned: Boolean = false,
    @SerializedName("archived")
    val archived: Boolean = false,
    @SerializedName("hidden")
    val hidden: Boolean = false,
    @SerializedName("active_stream_id")
    val activeStreamId: String? = null,
    @SerializedName("pending_user_message")
    val pendingUserMessage: String? = null,
    @SerializedName("llm_title_generated")
    val llmTitleGenerated: Boolean = false,
    @SerializedName("manual_title")
    val manualTitle: Boolean = false
) {
    /**
     * Convert timestamp to Date
     */
    fun getCreatedAtDate(): Date? {
        return createdAt?.let { Date(it.toLong() * 1000) }
    }

    fun getUpdatedAtDate(): Date? {
        return updatedAt?.let { Date(it.toLong() * 1000) }
    }

    /**
     * Check if session is active (has an active stream)
     */
    fun isActive(): Boolean {
        return activeStreamId != null
    }
}

/**
 * Response for listing sessions
 */
data class SessionListResponse(
    @SerializedName("sessions")
    val sessions: List<Session>? = null,
    @SerializedName("projects")
    val projects: List<Project>? = null,
    @SerializedName("active_profile")
    val activeProfile: String? = null,
    @SerializedName("all_profiles")
    val allProfiles: Boolean = false,
    @SerializedName("other_profile_count")
    val otherProfileCount: Int = 0
)

/**
 * Request to create a new session
 */
data class CreateSessionRequest(
    @SerializedName("title")
    val title: String,
    @SerializedName("profile")
    val profile: String? = null
)

/**
 * Response for creating a new session
 */
data class CreateSessionResponse(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("profile")
    val profile: String? = null,
    @SerializedName("created_at")
    val createdAt: Double? = null
)

/**
 * Full session with messages
 */
data class FullSession(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("model")
    val model: String? = null,
    @SerializedName("profile")
    val profile: String? = null,
    @SerializedName("messages")
    val messages: List<Message>? = null,
    @SerializedName("context_messages")
    val contextMessages: List<Message>? = null,
    @SerializedName("active_stream_id")
    val activeStreamId: String? = null,
    @SerializedName("pending_user_message")
    val pendingUserMessage: String? = null,
    @SerializedName("created_at")
    val createdAt: Double? = null,
    @SerializedName("updated_at")
    val updatedAt: Double? = null,
    @SerializedName("input_tokens")
    val inputTokens: Long = 0,
    @SerializedName("output_tokens")
    val outputTokens: Long = 0,
    @SerializedName("estimated_cost")
    val estimatedCost: Double = 0.0,
    @SerializedName("cache_read_tokens")
    val cacheReadTokens: Long = 0,
    @SerializedName("cache_write_tokens")
    val cacheWriteTokens: Long = 0
)

/**
 * Session status response
 */
data class SessionStatusResponse(
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("active_stream_id")
    val activeStreamId: String? = null,
    @SerializedName("pending_user_message")
    val pendingUserMessage: String? = null,
    @SerializedName("state")
    val state: String? = null
)

/**
 * Project model
 */
data class Project(
    @SerializedName("name")
    val name: String,
    @SerializedName("color")
    val color: String? = null,
    @SerializedName("profile")
    val profile: String? = null,
    @SerializedName("session_count")
    val sessionCount: Int = 0
)
