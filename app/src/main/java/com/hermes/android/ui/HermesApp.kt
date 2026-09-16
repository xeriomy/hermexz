// Hermes Android Client - Application Class
// This file is part of the Hermes Android project

package com.hermes.android.ui

import android.app.Application
import android.util.Log

/**
 * Application class for Hermes Android client
 */
class HermesApp : Application() {
    companion object {
        private const val TAG = "HermesApp"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Hermes Android app created")

        initializeApp()
    }

    private fun initializeApp() {
    }
}
