package com.mithaq.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class MithaqIllustrationType {
    ARCH,
    CRESCENT_STAR,
    SHIELD_LOCK,
    SPEECH_BUBBLES,
    CHECK_GEOMETRIC,
    ALERT_GEOMETRIC
}

@Composable
fun MithaqStateIllustration(
    type: MithaqIllustrationType,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val defaultGold = Color(0xFFF2CA50)
    val defaultEmerald = Color(0xFF8BD6B6)
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)

    Canvas(
        modifier = modifier
            .size(140.dp)
            .aspectRatio(1f)
            .padding(8.dp)
    ) {
        val width = size.width
        val height = size.height
        val cx = width / 2
        val cy = height / 2
        val strokeWidth = 2.dp.toPx()

        // 1. Draw subtle background geometric grid/circles for a premium technical/sacred look
        drawCircle(
            color = outlineColor,
            radius = cx * 0.95f,
            style = Stroke(width = 0.5.dp.toPx())
        )
        drawCircle(
            color = outlineColor,
            radius = cx * 0.75f,
            style = Stroke(width = 0.5.dp.toPx())
        )

        // Draw diagonal grid lines
        drawLine(
            color = outlineColor,
            start = Offset(cx - cx * 0.8f, cy - cy * 0.8f),
            end = Offset(cx + cx * 0.8f, cy + cy * 0.8f),
            strokeWidth = 0.5.dp.toPx()
        )
        drawLine(
            color = outlineColor,
            start = Offset(cx - cx * 0.8f, cy + cy * 0.8f),
            end = Offset(cx + cx * 0.8f, cy - cy * 0.8f),
            strokeWidth = 0.5.dp.toPx()
        )

        when (type) {
            MithaqIllustrationType.ARCH -> {
                // Pointed Archway (Islamic geometry)
                val path = Path().apply {
                    val startX = width * 0.25f
                    val endX = width * 0.75f
                    val baseHeight = height * 0.85f
                    val archShoulder = height * 0.45f
                    val archPeak = height * 0.18f

                    moveTo(startX, baseHeight)
                    lineTo(startX, archShoulder)
                    // Left curve of the arch
                    cubicTo(
                        startX, archShoulder - (archShoulder - archPeak) * 0.4f,
                        cx - (cx - startX) * 0.3f, archPeak + (archShoulder - archPeak) * 0.1f,
                        cx, archPeak
                    )
                    // Right curve of the arch
                    cubicTo(
                        cx + (cx - startX) * 0.3f, archPeak + (archShoulder - archPeak) * 0.1f,
                        endX, archShoulder - (archShoulder - archPeak) * 0.4f,
                        endX, archShoulder
                    )
                    lineTo(endX, baseHeight)
                }

                // Nested inner arch
                val innerPath = Path().apply {
                    val startX = width * 0.35f
                    val endX = width * 0.65f
                    val baseHeight = height * 0.85f
                    val archShoulder = height * 0.5f
                    val archPeak = height * 0.28f

                    moveTo(startX, baseHeight)
                    lineTo(startX, archShoulder)
                    cubicTo(
                        startX, archShoulder - (archShoulder - archPeak) * 0.4f,
                        cx - (cx - startX) * 0.3f, archPeak + (archShoulder - archPeak) * 0.1f,
                        cx, archPeak
                    )
                    cubicTo(
                        cx + (cx - startX) * 0.3f, archPeak + (archShoulder - archPeak) * 0.1f,
                        endX, archShoulder - (archShoulder - archPeak) * 0.4f,
                        endX, archShoulder
                    )
                    lineTo(endX, baseHeight)
                }

                drawPath(path, color = tint, style = Stroke(width = strokeWidth))
                drawPath(innerPath, color = tint.copy(alpha = 0.4f), style = Stroke(width = strokeWidth * 0.75f))
                
                // Base line
                drawLine(
                    color = tint,
                    start = Offset(width * 0.15f, height * 0.85f),
                    end = Offset(width * 0.85f, height * 0.85f),
                    strokeWidth = strokeWidth
                )
            }

            MithaqIllustrationType.CRESCENT_STAR -> {
                // Crescent Moon & 8-pointed star (Khatam star)
                // Draw 8-pointed star in the center
                val starPath = Path()
                val points = 8
                val outerRadius = width * 0.12f
                val innerRadius = outerRadius * 0.707f
                val starCx = cx + width * 0.08f
                val starCy = cy - height * 0.08f

                for (i in 0 until points * 2) {
                    val angle = i * Math.PI / points
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val x = starCx + radius * cos(angle).toFloat()
                    val y = starCy + radius * sin(angle).toFloat()
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()
                drawPath(starPath, color = defaultGold, style = Stroke(width = strokeWidth))

                // Crescent Moon
                val moonPath = Path().apply {
                    // Outer arc path
                    addArc(
                        oval = Rect(cx - width * 0.3f, cy - height * 0.3f, cx + width * 0.3f, cy + height * 0.3f),
                        startAngleDegrees = -110f,
                        sweepAngleDegrees = 220f
                    )
                    // Inner arc to create crescent
                    val startX = cx + width * 0.3f * cos(-110f * Math.PI / 180f).toFloat()
                    val startY = cy + height * 0.3f * sin(-110f * Math.PI / 180f).toFloat()
                    val endX = cx + width * 0.3f * cos(110f * Math.PI / 180f).toFloat()
                    val endY = cy + height * 0.3f * sin(110f * Math.PI / 180f).toFloat()

                    cubicTo(
                        cx - width * 0.05f, cy - height * 0.22f,
                        cx - width * 0.05f, cy + height * 0.22f,
                        endX, endY
                    )
                }
                drawPath(moonPath, color = tint, style = Stroke(width = strokeWidth))
            }

            MithaqIllustrationType.SHIELD_LOCK -> {
                // Shield Outline
                val shieldPath = Path().apply {
                    moveTo(cx, height * 0.2f)
                    cubicTo(width * 0.72f, height * 0.2f, width * 0.82f, height * 0.25f, width * 0.82f, height * 0.38f)
                    cubicTo(width * 0.82f, height * 0.62f, cx, height * 0.82f, cx, height * 0.86f)
                    cubicTo(cx, height * 0.82f, width * 0.18f, height * 0.62f, width * 0.18f, height * 0.38f)
                    cubicTo(width * 0.18f, height * 0.25f, width * 0.28f, height * 0.2f, cx, height * 0.2f)
                    close()
                }
                drawPath(shieldPath, color = tint, style = Stroke(width = strokeWidth))

                // Inner Lock
                val lockBody = Rect(cx - width * 0.12f, cy - height * 0.02f, cx + width * 0.12f, cy + height * 0.2f)
                drawRoundRect(
                    color = defaultGold,
                    topLeft = Offset(lockBody.left, lockBody.top),
                    size = Size(lockBody.width, lockBody.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                // Lock shackle
                val shacklePath = Path().apply {
                    addArc(
                        oval = Rect(cx - width * 0.08f, cy - height * 0.12f, cx + width * 0.08f, cy + height * 0.04f),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f
                    )
                    // straight lines down to the lock body
                    moveTo(cx - width * 0.08f, cy - height * 0.04f)
                    lineTo(cx - width * 0.08f, cy - height * 0.01f)
                    moveTo(cx + width * 0.08f, cy - height * 0.04f)
                    lineTo(cx + width * 0.08f, cy - height * 0.01f)
                }
                drawPath(shacklePath, color = defaultGold, style = Stroke(width = strokeWidth))
            }

            MithaqIllustrationType.SPEECH_BUBBLES -> {
                // Two intersecting line-art conversation bubbles
                // Bubble 1 (Back)
                val bubble1 = Rect(cx - width * 0.32f, cy - height * 0.28f, cx + width * 0.08f, cy + height * 0.08f)
                drawRoundRect(
                    color = tint.copy(alpha = 0.4f),
                    topLeft = Offset(bubble1.left, bubble1.top),
                    size = Size(bubble1.width, bubble1.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                // tail
                val tail1 = Path().apply {
                    moveTo(cx - width * 0.2f, cy + height * 0.08f)
                    lineTo(cx - width * 0.26f, cy + height * 0.16f)
                    lineTo(cx - width * 0.14f, cy + height * 0.08f)
                }
                drawPath(tail1, color = tint.copy(alpha = 0.4f), style = Stroke(width = strokeWidth))

                // Bubble 2 (Front)
                val bubble2 = Rect(cx - width * 0.08f, cy - height * 0.12f, cx + width * 0.32f, cy + height * 0.24f)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(bubble2.left, bubble2.top),
                    size = Size(bubble2.width, bubble2.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
                // tail
                val tail2 = Path().apply {
                    moveTo(cx + width * 0.14f, cy + height * 0.24f)
                    lineTo(cx + width * 0.2f, cy + height * 0.32f)
                    lineTo(cx + width * 0.2f, cy + height * 0.24f)
                }
                drawPath(tail2, color = tint, style = Stroke(width = strokeWidth))
            }

            MithaqIllustrationType.CHECK_GEOMETRIC -> {
                // Octagon/Star background with Emerald checkmark
                val starPath = Path()
                val points = 8
                val outerRadius = width * 0.35f
                val innerRadius = outerRadius * 0.85f

                for (i in 0 until points * 2) {
                    val angle = i * Math.PI / points
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val x = cx + radius * cos(angle).toFloat()
                    val y = cy + radius * sin(angle).toFloat()
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()
                drawPath(starPath, color = tint.copy(alpha = 0.24f), style = Stroke(width = strokeWidth))

                // Checkmark
                val checkPath = Path().apply {
                    moveTo(cx - width * 0.16f, cy)
                    lineTo(cx - width * 0.04f, cy + height * 0.12f)
                    lineTo(cx + width * 0.18f, cy - height * 0.12f)
                }
                drawPath(checkPath, color = defaultEmerald, style = Stroke(width = strokeWidth * 1.5f))
            }

            MithaqIllustrationType.ALERT_GEOMETRIC -> {
                // Star background with Warning Exclamation Mark (Gold/Red)
                val starPath = Path()
                val points = 8
                val outerRadius = width * 0.35f
                val innerRadius = outerRadius * 0.85f

                for (i in 0 until points * 2) {
                    val angle = i * Math.PI / points
                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                    val x = cx + radius * cos(angle).toFloat()
                    val y = cy + radius * sin(angle).toFloat()
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()
                drawPath(starPath, color = tint.copy(alpha = 0.24f), style = Stroke(width = strokeWidth))

                // Exclamation mark
                drawLine(
                    color = tint,
                    start = Offset(cx, cy - height * 0.15f),
                    end = Offset(cx, cy + height * 0.03f),
                    strokeWidth = strokeWidth * 1.5f
                )
                drawCircle(
                    color = tint,
                    radius = strokeWidth * 1.2f,
                    center = Offset(cx, cy + height * 0.14f)
                )
            }
        }
    }
}

// Simple Rect definition helper to keep standard signatures compatible
private class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

private fun Path.addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float) {
    addArc(
        oval = androidx.compose.ui.geometry.Rect(oval.left, oval.top, oval.right, oval.bottom),
        startAngleDegrees = startAngleDegrees,
        sweepAngleDegrees = sweepAngleDegrees
    )
}


