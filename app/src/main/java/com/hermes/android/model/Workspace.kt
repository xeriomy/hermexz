// Hermes Android Client - Workspace Model
// This file is part of the Hermes Android project

package com.hermes.android.model

import com.google.gson.annotations.SerializedName

/**
 * Workspace information
 */
data class Workspace(
    @SerializedName("path")
    val path: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("last")
    val last: Boolean = false
)

/**
 * Workspace list response
 */
data class WorkspaceListResponse(
    @SerializedName("workspaces")
    val workspaces: List<Workspace>? = null,
    @SerializedName("last")
    val last: String? = null,
    @SerializedName("terminal_remote_backend")
    val terminalRemoteBackend: Boolean = false
)

/**
 * Workspace suggestion request
 */
data class WorkspaceSuggestionRequest(
    @SerializedName("prefix")
    val prefix: String
)

/**
 * Workspace suggestion response
 */
data class WorkspaceSuggestionResponse(
    @SerializedName("suggestions")
    val suggestions: List<String>? = null,
    @SerializedName("prefix")
    val prefix: String? = null
)
