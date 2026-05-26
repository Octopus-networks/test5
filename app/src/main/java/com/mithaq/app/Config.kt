package com.mithaq.app

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Global configuration for Mithaq.
 * Release builds are locked down by Gradle BuildConfig.
 */
object Config {
    /**
     * True in release builds. Debug builds may enable local demo helpers.
     */
    val IS_PRODUCTION: Boolean = BuildConfig.IS_PRODUCTION

    /**
     * Debug builds may read GEMINI_API_KEY from local.properties or the environment.
     * Release builds intentionally ship this empty; use a backend proxy for production AI.
     */
    val GEMINI_API_KEY: String = BuildConfig.GEMINI_API_KEY

    /**
     * Centralized utility to check if the app is running in simulated mock/demo mode.
     */
    fun isMock(): Boolean {
        if (BuildConfig.IS_PRODUCTION) return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val apiKey = db.app.options.apiKey ?: ""
            apiKey == "mock-api-key-for-testing" || apiKey.contains("mock")
        } catch (e: Exception) {
            true
        }
    }
}
