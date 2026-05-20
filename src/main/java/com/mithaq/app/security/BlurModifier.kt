package com.mithaq.app.security

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modifiers representing Feature 4's Modesty Control.
 * Applies a visual blur to profile images unless modesty locks are released (e.g. premium access, match approval, or user toggle).
 */
fun Modifier.modestyBlur(
    isBlurred: Boolean,
    blurRadius: Dp = 25.dp
): Modifier {
    if (!isBlurred) return this

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // High-fidelity hardware-accelerated blur on Android 12+ (API 31+)
        this.graphicsLayer {
            val blurPx = blurRadius.toPx()
            if (blurPx > 0) {
                val androidRenderEffect = android.graphics.RenderEffect.createBlurEffect(
                    blurPx,
                    blurPx,
                    android.graphics.Shader.TileMode.CLAMP
                )
                this.renderEffect = androidRenderEffect.asComposeRenderEffect()
            }
        }
    } else {
        // Standard Compose blur modifier fallback on older Android versions (API 21-30)
        this.blur(radius = blurRadius)
    }
}
