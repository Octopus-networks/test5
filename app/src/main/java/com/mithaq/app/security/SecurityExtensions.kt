package com.mithaq.app.security

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Extension function to securely prevent screenshotting and screen recording on any Activity.
 */
fun Activity.preventScreenshots(enable: Boolean) {
    if (enable) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

/**
 * A lifecycle-aware Compose wrapper that ensures screen privacy by disabling screenshots
 * and screen recordings while the screen is visible. Automatically restores window permissions on disposal.
 */
@Composable
fun SecureScreen(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(activity) {
        activity?.preventScreenshots(true)
        onDispose {
            activity?.preventScreenshots(false)
        }
    }

    content()
}
