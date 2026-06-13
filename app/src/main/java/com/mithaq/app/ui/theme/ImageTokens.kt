package com.mithaq.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Standardized image corner radii for photos, avatars, and image cards.
 * Do NOT use these for non-image elements (buttons, text fields, chips, banners).
 */
object ImageCorners {
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
}

/**
 * Standardized avatar sizing tokens. These map 1:1 to the existing pixel values
 * used across the app — this is a single-source-of-truth consolidation, NOT a redesign.
 */
object AvatarSize {
    val Small = 40.dp   // Chat avatars, AiMatchmaker list items
    val Medium = 52.dp  // Tabs list items
    val Large = 64.dp   // Account Hub, Search Top Match, RegisterScreen avatar picker
    val XLarge = 72.dp  // ProfileSettings additional photos
    val Hero = 96.dp    // ProfileSettings main photo
}
