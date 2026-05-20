package com.mithaq.app.ui.match

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A beautiful, highly-polished circular compatibility badge with dynamic color grading
 * and a smooth sweep entrance animation.
 */
@Composable
fun MatchScoreBadge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    showLabel: Boolean = true
) {
    // Dynamic color determination based on score
    val targetColor = when {
        score >= 80 -> Color(0xFF00A86B) // Premium Emerald/Green for high compatibility
        score >= 50 -> Color(0xFFFFB300) // Deep Amber for moderate compatibility
        else -> Color(0xFFE53935)        // Ruby Red for low compatibility
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 800),
        label = "BadgeColorAnimation"
    )

    // Sweep animation for the circular indicator
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = score) {
        animationTriggered = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) score / 100f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "BadgeSweepAnimation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = size.toPx()
                val strokeWidthPx = strokeWidth.toPx()
                val radius = (diameter - strokeWidthPx) / 2f
                val centerOffset = center

                // 1. Draw Background Track Circle
                drawCircle(
                    color = animatedColor.copy(alpha = 0.15f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidthPx)
                )

                // 2. Draw Progress Arc (Starting from top: -90 degrees)
                drawArc(
                    color = animatedColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = centerOffset - androidx.compose.ui.geometry.Offset(radius, radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // 3. Score text overlay
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = (size.value * 0.22f).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (showLabel) {
            val labelText = when {
                score >= 80 -> "Highly Compatible"
                score >= 50 -> "Good Match"
                else -> "Moderate Match"
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = animatedColor
            )
        }
    }
}
