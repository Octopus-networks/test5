package com.mithaq.app

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Global Configuration for the Mithaq Application.
 * Toggle [IS_PRODUCTION] to true to completely lock down the application,
 * disabling all simulated mock modes, developer bypasses, and fake data helpers.
 */
object Config {
    const val IS_PRODUCTION = false
    const val GEMINI_API_KEY = "YOUR_GEMINI_API_KEY" // Add your Gemini API key here

    /**
     * Centralized utility to check if the app is running in simulated mock/demo mode.
     */
    fun isMock(): Boolean {
        if (IS_PRODUCTION) return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val apiKey = db.app.options.apiKey ?: ""
            apiKey == "mock-api-key-for-testing" || apiKey.contains("mock")
        } catch (e: Exception) {
            true // Default to mock mode if Firebase is not initialized or fails
        }
    }
}
