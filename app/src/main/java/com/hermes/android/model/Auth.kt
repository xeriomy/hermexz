// Hermes Android Client - Authentication Model
// This file is part of the Hermes Android project

package com.hermes.android.model

import com.google.gson.annotations.SerializedName

/**
 * Authentication status response
 */
data class AuthStatusResponse(
    @SerializedName("auth_enabled")
    val authEnabled: Boolean,
    @SerializedName("logged_in")
    val loggedIn: Boolean,
    @SerializedName("oidc_enabled")
    val oidcEnabled: Boolean = false,
    @SerializedName("password_auth_enabled")
    val passwordAuthEnabled: Boolean = false,
    @SerializedName("passwordless_enabled")
    val passwordlessEnabled: Boolean = false,
    @SerializedName("passkeys_enabled")
    val passkeysEnabled: Boolean = false,
    @SerializedName("passkeys_count")
    val passkeysCount: Int = 0,
    @SerializedName("passkey_feature_flag")
    val passkeyFeatureFlag: Boolean = false,
    @SerializedName("auth_disabled_acknowledged")
    val authDisabledAcknowledged: Boolean = false,
    @SerializedName("trusted_auth_enabled")
    val trustedAuthEnabled: Boolean = false,
    @SerializedName("auth_type")
    val authType: String? = null,
    @SerializedName("user")
    val user: String? = null,
    @SerializedName("bound_profile")
    val boundProfile: String? = null
)

/**
 * Login request
 */
data class LoginRequest(
    @SerializedName("password")
    val password: String
)

/**
 * Login response - sets cookie on success
 */
data class LoginResponse(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("error")
    val error: String? = null
)

/**
 * Session info for authenticated user
 */
data class SessionInfo(
    val sessionToken: String,
    val authType: String? = null,
    val username: String? = null,
    val boundProfile: String? = null,
    val expiry: Long? = null
)

/**
 * Connection status enum
 */
enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

/**
 * Server configuration
 */
data class ServerConfig(
    val baseUrl: String,
    val isConnected: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val lastError: String? = null
)
