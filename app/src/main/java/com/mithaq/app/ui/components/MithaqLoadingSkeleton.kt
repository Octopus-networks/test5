package com.mithaq.app.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

enum class SkeletonType {
    PROFILE_CARD,
    MESSAGE_ROW,
    REQUEST_ROW
}

@Composable
fun MithaqLoadingSkeleton(
    type: SkeletonType,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    when (type) {
        SkeletonType.PROFILE_CARD -> {
            // Sweep shimmer for premium loading feel
            val shimmerOffset by transition.animateFloat(
                initialValue = -400f,
                targetValue = 1200f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Restart
                ),
                label = "skeleton_sweep"
            )
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(color, color.copy(alpha = 0.28f), color),
                start = Offset(shimmerOffset, 0f),
                end = Offset(shimmerOffset + 300f, 0f)
            )
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
                    // Photo Placeholder box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    // Name & Age placeholder row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(shimmerBrush)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Location placeholder row
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    // Chips row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(shimmerBrush)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    // Buttons placeholders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(shimmerBrush)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }

        SkeletonType.MESSAGE_ROW -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar/Initials shape
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Name placeholder
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                            // Date/Status placeholder
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // Last Message preview placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color)
                        )
                    }
                }
            }
        }

        SkeletonType.REQUEST_ROW -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Request Type Header
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Sender Name
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Location/Subtitle
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(color)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}
