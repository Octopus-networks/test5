package com.mithaq.app

import com.google.firebase.firestore.FirebaseFirestore

/**
 * Global Configuration for the Mithaq Application.
 * Toggle [IS_PRODUCTION] to true to completely lock down the application,
 * disabling all simulated mock modes, developer bypasses, and fake data helpers.
 */
object Config {
    /**
     * PRODUCTION FLAG — Set to true before any public release.
     * When true: disables all mock/demo bypasses, developer menus, and fake data helpers.
     * When false: allows role-switching and admin access via version tap — DANGEROUS in production!
     */
    const val IS_PRODUCTION = true

    /**
     * WARNING: Do NOT commit real API keys to source control.
     * Store the real key in local.properties: GEMINI_API_KEY=your_key_here
     * and read it via BuildConfig (see build.gradle.kts).
     */
    const val GEMINI_API_KEY = "AIzaSyCz-A23MzxTRXPTHwWdQSz9_twGfjSyEWs"

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
