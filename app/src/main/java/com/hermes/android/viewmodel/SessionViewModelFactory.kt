// Hermes Android Client - Session ViewModel Factory
// This file is part of the Hermes Android project

package com.hermes.android.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hermes.android.network.HermesApi

/**
 * Factory for creating SessionViewModel
 */
class SessionViewModelFactory(
    private val application: Application,
    private val api: HermesApi
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            return SessionViewModel(application, api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
