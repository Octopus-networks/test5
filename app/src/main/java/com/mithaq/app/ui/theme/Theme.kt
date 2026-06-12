package com.mithaq.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// Stitch "Sacred Covenant" dark scheme: Soft Gold primary, Emerald trust secondary,
// Deep Charcoal surfaces, warm outlines.
private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = BackgroundDark,
    primaryContainer = AccentGoldDeep,
    onPrimaryContainer = Color(0xFF554300),
    secondary = PrimaryEmeraldLight,
    onSecondary = BackgroundDark,
    secondaryContainer = PrimaryEmeraldDark,
    onSecondaryContainer = PrimaryEmeraldLight,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineWarm,
    outlineVariant = OutlineWarmVariant,
    error = ErrorRed,
    onError = Color.White
)

// Curated Light Color Scheme for Mithaq Premium (Eggshell/Ivory Base)
private val LightColorScheme = lightColorScheme(
    primary = AccentGoldDeep,
    onPrimary = Color.White,
    primaryContainer = SurfaceVariantLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = PrimaryEmerald,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = AccentGold,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorRed,
    onError = Color.White
)

/**
 * Main application theme wrapper for Mithaq.
 * Enforces beautiful light/dark palettes and coordinates status bar colors.
 */
@Composable
fun MithaqTheme(
    darkTheme: Boolean = true,
    isArabic: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Set system status bar color dynamically
                window.statusBarColor = colorScheme.surface.toArgb()
                window.navigationBarColor = colorScheme.surface.toArgb()
                
                // Adjust status bar icon colors based on theme
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.isAppearanceLightStatusBars = !darkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val strings = if (isArabic) ArabicStrings else EnglishStrings
    val layoutDirection = if (isArabic) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr

    androidx.compose.runtime.CompositionLocalProvider(
        LocalMithaqStrings provides strings,
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
