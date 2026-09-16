// Hermes Android Client - Hermes API Interface
// This file is part of the Hermes Android project

package com.hermes.android.network

import com.hermes.android.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Main Hermes WebUI API interface
 * Defines all REST endpoints for the Hermes WebUI backend
 */
interface HermesApi {

    // ==================== Authentication ====================

    @GET("api/auth/status")
    suspend fun getAuthStatus(): Response<AuthStatusResponse>

    @POST("api/auth/login")
    @FormUrlEncoded
    suspend fun login(@Field("password") password: String): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Void>

    // ==================== Sessions ====================

    @GET("api/sessions")
    suspend fun listSessions(
        @Query("include_archived") includeArchived: Boolean? = null,
        @Query("exclude_hidden") excludeHidden: Boolean? = null,
        @Query("archived_limit") archivedLimit: Int? = null,
        @Query("archived_offset") archivedOffset: Int? = null,
        @Query("sidebar_source") sidebarSource: String? = null
    ): Response<SessionListResponse>

    @GET("api/session")
    suspend fun getSession(
        @Query("session_id") sessionId: String,
        @Query("messages") messages: Int? = 1,
        @Query("msg_limit") msgLimit: Int? = null,
        @Query("msg_before") msgBefore: Int? = null,
        @Query("resolve_model") resolveModel: Int? = null
    ): Response<FullSession>

    @POST("api/session/new")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<CreateSessionResponse>

    @GET("api/session/status")
    suspend fun getSessionStatus(
        @Query("session_id") sessionId: String
    ): Response<SessionStatusResponse>

    @GET("api/session/usage")
    suspend fun getSessionUsage(
        @Query("session_id") sessionId: String
    ): Response<Map<String, Any>>

    // ==================== Chat ====================

    @POST("api/chat/start")
    suspend fun startChat(@Body request: ChatStartRequest): Response<ChatStartResponse>

    @GET("api/chat/stream/status")
    suspend fun getStreamStatus(
        @Query("stream_id") streamId: String
    ): Response<StreamStatusResponse>

    @POST("api/chat/stream/cancel")
    suspend fun cancelStream(@Body request: CancelStreamRequest): Response<CancelStreamResponse>

    // ==================== Health ====================

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}

/**
 * API for SSE streaming endpoints
 * Note: SSE endpoints are handled separately from REST endpoints
 */
interface StreamApi {
    companion object {
        const val CHAT_STREAM_PATH = "api/chat/stream"
        const val SESSION_STREAM_PATH = "api/session/stream"
    }
}
