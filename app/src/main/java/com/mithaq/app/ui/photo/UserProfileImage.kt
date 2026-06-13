package com.mithaq.app.ui.photo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.mithaq.app.model.Gender
import com.mithaq.app.security.modestyBlur

// Preset modesty avatar options for Mithaq users
val BrotherhoodAvatars = listOf(
    "avatar_brother_green" to Color(0xFF1E5631), // Deep Islamic Green
    "avatar_brother_blue" to Color(0xFF1F4E5B),  // Teal Slate Blue
    "avatar_brother_brown" to Color(0xFF5C4033)  // Warm Sandalwood Brown
)

val SisterhoodAvatars = listOf(
    "avatar_sister_teal" to Color(0xFF1E6B65),  // Elegant Teal Hijab
    "avatar_sister_rose" to Color(0xFF8A5A5C),  // Dusty Rose Hijab
    "avatar_sister_purple" to Color(0xFF4A2F48) // Eggplant Modesty Hijab
)

/**
 * A beautiful, premium profile image loader.
 * Renders high-quality Compose Canvas vector drawings for privacy/modesty preset avatars
 * and uses Coil to fetch custom profile URLs. Automatically integrates modesty blur.
 */
@Composable
fun UserProfileImage(
    imageUrl: String,
    gender: Gender,
    isBlurred: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 25.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape
) {
    val context = LocalContext.current
    val defaultAvatarId = if (gender == Gender.MALE) "avatar_brother_green" else "avatar_sister_teal"
    val isPresetAvatar = imageUrl.startsWith("avatar_")
    val canLoadCustomPhoto = imageUrl.isNotBlank() && !isPresetAvatar && !isBlurred

    Box(
        modifier = modifier
            .clip(shape)
            .modestyBlur(isBlurred, blurRadius)
    ) {
        if (isPresetAvatar) {
            // Render custom vector modesty avatar
            AvatarVector(avatarId = imageUrl, gender = gender, modifier = Modifier.fillMaxSize())
        } else if (canLoadCustomPhoto) {
            // Never fetch a private photo while it is still blurred/locked. This legacy URL
            // path is used only after approval, and cache is disabled to avoid stale copies.
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .diskCachePolicy(CachePolicy.DISABLED)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .build(),
                contentDescription = "Profile Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = coil.compose.rememberAsyncImagePainter(
                    model = null,
                    error = null
                )
            )
        } else {
            // Default placeholder avatar based on gender
            AvatarVector(avatarId = defaultAvatarId, gender = gender, modifier = Modifier.fillMaxSize())
        }

        if (!isBlurred) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    alpha = 60 // semi-transparent (out of 255)
                    textSize = size.width * 0.08f
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val text = "MITHAQ SECURE"
                
                // Draw text at 45 degrees
                drawContext.canvas.nativeCanvas.save()
                drawContext.canvas.nativeCanvas.rotate(-30f, size.width / 2f, size.height / 2f)
                
                // Draw repeated watermark lines
                drawContext.canvas.nativeCanvas.drawText(text, size.width / 2f, size.height * 0.35f, paint)
                drawContext.canvas.nativeCanvas.drawText(text, size.width / 2f, size.height * 0.5f, paint)
                drawContext.canvas.nativeCanvas.drawText(text, size.width / 2f, size.height * 0.65f, paint)
                
                drawContext.canvas.nativeCanvas.restore()
            }
        }
    }
}

/**
 * Draws a gorgeous, clean, modern modesty avatar vector on Compose Canvas.
 */
@Composable
fun AvatarVector(
    avatarId: String,
    gender: Gender,
    modifier: Modifier = Modifier
) {
    // Single source of truth: look up the background color from the canonical avatar lists.
    val avatarColorMap = remember {
        (BrotherhoodAvatars + SisterhoodAvatars).toMap()
    }
    val backgroundColor = avatarColorMap[avatarId]
        ?: if (gender == Gender.MALE) BrotherhoodAvatars.first().second else SisterhoodAvatars.first().second

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        
        // 1. Draw circular background
        drawCircle(color = backgroundColor)

        if (gender == Gender.MALE) {
            // BROTHER AVATAR DRAWING
            // Neck
            drawRect(
                color = Color(0xFFFFD1A9), // Light skin tone
                topLeft = Offset(cx - w * 0.1f, cy + h * 0.1f),
                size = Size(w * 0.2f, h * 0.2f)
            )
            
            // Shoulder/Shirt (Chest)
            val chestPath = Path().apply {
                moveTo(w * 0.15f, h)
                lineTo(w * 0.85f, h)
                quadraticTo(w * 0.8f, cy + h * 0.2f, cx + w * 0.2f, cy + h * 0.25f)
                lineTo(cx - w * 0.2f, cy + h * 0.25f)
                quadraticTo(w * 0.2f, cy + h * 0.2f, w * 0.15f, h)
                close()
            }
            drawPath(path = chestPath, color = Color(0xFFECEFF1)) // White Thobe / Shirt
            
            // Collar V-Neck
            val collarPath = Path().apply {
                moveTo(cx - w * 0.08f, cy + h * 0.25f)
                lineTo(cx + w * 0.08f, cy + h * 0.25f)
                lineTo(cx, cy + h * 0.32f)
                close()
            }
            drawPath(path = collarPath, color = Color(0xFFFFD1A9))

            // Face Circle
            drawCircle(
                color = Color(0xFFFFE0BD), // Face skin tone
                radius = w * 0.25f,
                center = Offset(cx, cy - h * 0.05f)
            )

            // Kufi / Cap
            val capPath = Path().apply {
                moveTo(cx - w * 0.25f, cy - h * 0.12f)
                quadraticTo(cx, cy - h * 0.38f, cx + w * 0.25f, cy - h * 0.12f)
                quadraticTo(cx, cy - h * 0.15f, cx - w * 0.25f, cy - h * 0.12f)
                close()
            }
            drawPath(path = capPath, color = Color.White) // White Kufi

            // Beard / Modesty Facial Hair
            val beardPath = Path().apply {
                moveTo(cx - w * 0.25f, cy - h * 0.08f)
                quadraticTo(cx - w * 0.22f, cy + h * 0.18f, cx, cy + h * 0.22f)
                quadraticTo(cx + w * 0.22f, cy + h * 0.18f, cx + w * 0.25f, cy - h * 0.08f)
                quadraticTo(cx + w * 0.2f, cy + h * 0.08f, cx, cy + h * 0.1f)
                quadraticTo(cx - w * 0.2f, cy + h * 0.08f, cx - w * 0.25f, cy - h * 0.08f)
                close()
            }
            drawPath(path = beardPath, color = Color(0xFF2C2C2C)) // Dark Beard

            // Mustache
            val mustachePath = Path().apply {
                moveTo(cx - w * 0.12f, cy + h * 0.05f)
                quadraticTo(cx, cy + h * 0.02f, cx + w * 0.12f, cy + h * 0.05f)
                quadraticTo(cx, cy + h * 0.09f, cx - w * 0.12f, cy + h * 0.05f)
                close()
            }
            drawPath(path = mustachePath, color = Color(0xFF2C2C2C))

        } else {
            // SISTER AVATAR DRAWING (Elegant Hijab/Abaya)
            // Hijab Outer Drape
            val outerHijabPath = Path().apply {
                moveTo(w * 0.15f, h)
                quadraticTo(w * 0.2f, cy - h * 0.25f, cx - w * 0.32f, cy - h * 0.15f)
                quadraticTo(cx, cy - h * 0.42f, cx + w * 0.32f, cy - h * 0.15f)
                quadraticTo(w * 0.8f, cy - h * 0.25f, w * 0.85f, h)
                close()
            }
            drawPath(path = outerHijabPath, color = backgroundColor.copy(alpha = 0.9f))
            
            // Abaya/Gown Chest Overlay
            val chestPath = Path().apply {
                moveTo(w * 0.22f, h)
                lineTo(w * 0.78f, h)
                quadraticTo(cx, cy + h * 0.3f, w * 0.22f, h)
                close()
            }
            drawPath(path = chestPath, color = Color(0xFF1C1C1C)) // Dark elegant Abaya chest area

            // Hijab Inner Frame (Wraps around the face)
            val innerHijabPath = Path().apply {
                moveTo(cx - w * 0.22f, cy - h * 0.05f)
                quadraticTo(cx - w * 0.24f, cy - h * 0.32f, cx, cy - h * 0.34f)
                quadraticTo(cx + w * 0.24f, cy - h * 0.32f, cx + w * 0.22f, cy - h * 0.05f)
                quadraticTo(cx + w * 0.18f, cy + h * 0.18f, cx, cy + h * 0.24f)
                quadraticTo(cx - w * 0.18f, cy + h * 0.18f, cx - w * 0.22f, cy - h * 0.05f)
                close()
            }
            drawPath(path = innerHijabPath, color = Color.White) // Inner white cap/scarf liner

            // Inner Face Area
            val facePath = Path().apply {
                moveTo(cx - w * 0.16f, cy - h * 0.05f)
                quadraticTo(cx - w * 0.17f, cy - h * 0.24f, cx, cy - h * 0.25f)
                quadraticTo(cx + w * 0.17f, cy - h * 0.24f, cx + w * 0.16f, cy - h * 0.05f)
                quadraticTo(cx + w * 0.12f, cy + h * 0.12f, cx, cy + h * 0.14f)
                quadraticTo(cx - w * 0.12f, cy + h * 0.12f, cx - w * 0.16f, cy - h * 0.05f)
                close()
            }
            
            // Check if niqab is selected (niqab hides lower face)
            val isNiqab = avatarId == "avatar_sister_purple" // We treat purple as the Niqab option for test
            
            drawPath(path = facePath, color = Color(0xFFFFE0BD)) // Face skin tone
            
            if (isNiqab) {
                // Draw Niqab veil covering face from nose down
                val niqabPath = Path().apply {
                    moveTo(cx - w * 0.18f, cy - h * 0.02f)
                    lineTo(cx + w * 0.18f, cy - h * 0.02f)
                    quadraticTo(cx + w * 0.15f, cy + h * 0.25f, cx, cy + h * 0.26f)
                    quadraticTo(cx - w * 0.15f, cy + h * 0.25f, cx - w * 0.18f, cy - h * 0.02f)
                    close()
                }
                drawPath(path = niqabPath, color = Color(0xFF2C2C2C)) // Dark niqab veil
                
                // Forehead band
                val bandPath = Path().apply {
                    moveTo(cx - w * 0.18f, cy - h * 0.22f)
                    quadraticTo(cx, cy - h * 0.24f, cx + w * 0.18f, cy - h * 0.22f)
                    lineTo(cx + w * 0.17f, cy - h * 0.18f)
                    quadraticTo(cx, cy - h * 0.2f, cx - w * 0.17f, cy - h * 0.18f)
                    close()
                }
                drawPath(path = bandPath, color = Color(0xFF2C2C2C))
            }
        }
    }
}
