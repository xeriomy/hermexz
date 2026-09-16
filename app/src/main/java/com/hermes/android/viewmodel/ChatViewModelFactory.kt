// Hermes Android Client - Chat ViewModel Factory
// This file is part of the Hermes Android project

package com.hermes.android.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hermes.android.network.HermesApi
import com.hermes.android.network.SseClient

/**
 * Factory for creating ChatViewModel
 */
class ChatViewModelFactory(
    private val application: Application,
    private val api: HermesApi,
    private val sseClient: SseClient
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(application, api, sseClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
