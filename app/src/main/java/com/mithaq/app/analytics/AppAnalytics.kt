package com.mithaq.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin wrapper over Firebase Analytics so screens/ViewModels log typed funnel events without
 * referencing the SDK directly. Initialize once from [com.mithaq.app.MithaqApplication].
 *
 * NEVER pass PII here (no name/email/phone/free-text profile data) — only ids and enums.
 * Calls made before [init] are safe no-ops.
 */
object AppAnalytics {
    private var analytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context.applicationContext)
    }

    private fun log(event: String, params: Bundle? = null) {
        analytics?.logEvent(event, params)
    }

    /** A match profile was opened (top of the discovery → conversation funnel). */
    fun matchViewed() = log("match_viewed")

    /** The user sent a new chat request. */
    fun chatRequestSent() = log("chat_request_sent")

    /** The user opened a chat conversation. */
    fun chatOpened() = log("chat_opened")
}
