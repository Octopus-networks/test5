package com.mithaq.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Font Families for the Mithaq App.
 *
 * NOTE: For optimum visual quality, download the following Google Fonts (Cairo, Amiri, Outfit)
 * and place their .ttf files in your `res/font/` directory. Then, uncomment the resources below.
 * Standard system fallbacks are configured to compile out-of-the-box.
 */

// Cairo Font Family: Clean, readable font recommended for Arabic body texts and UI elements.
val CairoFontFamily = FontFamily(
    // Font(resId = R.font.cairo_regular, weight = FontWeight.Normal),
    // Font(resId = R.font.cairo_medium, weight = FontWeight.Medium),
    // Font(resId = R.font.cairo_bold, weight = FontWeight.Bold),
    androidx.compose.ui.text.font.FontFamily.SansSerif // System fallback
)

// Amiri Font Family: Classically-styled Naskh font for Islamic headings, Quran verses, and titles.
val AmiriFontFamily = FontFamily(
    // Font(resId = R.font.amiri_regular, weight = FontWeight.Normal),
    // Font(resId = R.font.amiri_bold, weight = FontWeight.Bold),
    androidx.compose.ui.text.font.FontFamily.Serif // System fallback
)

// Outfit Font Family: Modern, geometric geometric-sans font for Latin texts.
val OutfitFontFamily = FontFamily(
    // Font(resId = R.font.outfit_regular, weight = FontWeight.Normal),
    // Font(resId = R.font.outfit_bold, weight = FontWeight.Bold),
    androidx.compose.ui.text.font.FontFamily.SansSerif // System fallback
)

// Material 3 Typography settings for Mithaq
val Typography = Typography(
    // Title Large: Amiri for classical titles
    titleLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // Title Medium: Amiri for secondary headings
    titleMedium = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // Headline Small: Amiri for big subheadings
    headlineSmall = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    // Body Large: Cairo / Outfit for general message lists and readable text
    bodyLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Body Medium: General paragraph reading text
    bodyMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // Label Medium: Button text and micro actions
    labelMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    // Label Small: Dynamic tags, statuses, compatibility numbers
    labelSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
