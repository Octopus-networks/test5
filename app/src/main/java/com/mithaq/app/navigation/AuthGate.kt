package com.mithaq.app.navigation

import com.mithaq.app.ui.auth.AuthState

object AuthGate {
    fun routeFor(authState: AuthState): String {
        return when (authState) {
            is AuthState.Authenticated -> Routes.Home
            is AuthState.EmailVerificationRequired -> Routes.VerifyEmail
            is AuthState.NeedsProfileCompletion -> Routes.ProfileCompletion
            AuthState.Idle,
            is AuthState.Error,
            AuthState.Loading -> Routes.Entry
        }
    }

    fun canAccessProtectedRoute(authState: AuthState): Boolean {
        return authState is AuthState.Authenticated || authState is AuthState.NeedsProfileCompletion
    }
}
