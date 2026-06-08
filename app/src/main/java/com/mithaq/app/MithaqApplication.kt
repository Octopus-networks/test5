package com.mithaq.app

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Application entry point.
 *
 * Initializes a small, non-PII Crashlytics custom key so field crashes are easier to
 * triage by build type. Crash collection itself is enabled automatically by the
 * Crashlytics SDK/Gradle plugin — no manual init required. Never record PII here
 * (no email, name, phone, or free-text profile data).
 */
class MithaqApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().setCustomKey("build_type", BuildConfig.BUILD_TYPE)
    }
}
