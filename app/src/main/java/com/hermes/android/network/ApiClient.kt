// Hermes Android Client - API Client
// This file is part of the Hermes Android project

package com.hermes.android.network

import android.content.Context
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object for managing API client instances
 */
object ApiClient {
    private const val TAG = "ApiClient"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 60L
    private const val WRITE_TIMEOUT_SECONDS = 60L

    // Cookie storage
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    // Session cookie name
    const val SESSION_COOKIE_NAME = "hermes_session"

    /**
     * Create and configure OkHttpClient
     */
    fun createOkHttpClient(context: Context, baseUrl: String): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            // Never log sensitive information
            if (message.contains("password", ignoreCase = true) ||
                message.contains("token", ignoreCase = true) ||
                message.contains("cookie", ignoreCase = true) ||
                message.contains("authorization", ignoreCase = true)) {
                Log.d(TAG, "[REDACTED] Sensitive data in log")
            } else {
                Log.d(TAG, message)
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val cookieJar = object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val urlKey = url.host
                cookieStore[urlKey] = cookies.toMutableList()
                Log.d(TAG, "Saved cookies for ${url.host}: ${cookies.map { it.name }}")
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                val urlKey = url.host
                return cookieStore[urlKey] ?: emptyList()
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "HermesAndroid/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    /**
     * Create Retrofit instance for Hermes API
     */
    fun createHermesApi(context: Context, baseUrl: String): HermesApi {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) {
            baseUrl.dropLast(1)
        } else {
            baseUrl
        }

        val okHttpClient = createOkHttpClient(context, normalizedBaseUrl)

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HermesApi::class.java)
    }

    /**
     * Get current session cookie value
     */
    fun getSessionCookie(baseUrl: String): String? {
        val host = HttpUrl.parse(baseUrl)?.host ?: return null
        val cookies = cookieStore[host] ?: return null
        return cookies.firstOrNull { it.name == SESSION_COOKIE_NAME }?.value
    }

    /**
     * Set session cookie manually
     */
    fun setSessionCookie(baseUrl: String, cookieValue: String) {
        val host = HttpUrl.parse(baseUrl)?.host ?: return
        val cookie = Cookie.Builder()
            .name(SESSION_COOKIE_NAME)
            .value(cookieValue)
            .domain(host)
            .path("/")
            .httpOnly(true)
            .secure(false)
            .build()

        cookieStore.getOrPut(host) { mutableListOf() }.apply {
            removeAll { it.name == SESSION_COOKIE_NAME }
            add(cookie)
        }
    }

    /**
     * Clear session cookie
     */
    fun clearSessionCookie(baseUrl: String) {
        val host = HttpUrl.parse(baseUrl)?.host ?: return
        cookieStore[host]?.removeAll { it.name == SESSION_COOKIE_NAME }
    }

    /**
     * Clear all cookies for a host
     */
    fun clearAllCookies(baseUrl: String) {
        val host = HttpUrl.parse(baseUrl)?.host ?: return
        cookieStore.remove(host)
    }

    /**
     * Clear all cookies
     */
    fun clearAllCookies() {
        cookieStore.clear()
    }
}
