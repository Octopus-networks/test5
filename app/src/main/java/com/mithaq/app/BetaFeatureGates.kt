package com.mithaq.app

/**
 * Temporary beta gates for surfaces that are visible in the codebase but not
 * production-ready yet. Keep these disabled until the related backend, privacy,
 * and Play policy flows are complete.
 */
object BetaFeatureGates {
    const val PREMIUM_BILLING = false
    const val GEMINI_AI = false
    const val VOICE_INTRO = false
    const val VOICE_CALL = false
    const val LEGACY_WALI_DASHBOARD = false
}
