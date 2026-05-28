package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun SkeletalLoader(
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shimmerColors = if (!isDark) {
        listOf(
            Color(0xFFF1F5F9),
            Color(0xFFE2E8F0),
            Color(0xFFCBD5E1),
            Color(0xFFE2E8F0),
            Color(0xFFF1F5F9)
        )
    } else {
        listOf(
            Color(0xFF1E293B),
            Color(0xFF334155),
            Color(0xFF475569),
            Color(0xFF334155),
            Color(0xFF1E293B)
        )
    }

    val transition = rememberInfiniteTransition(label = "SkeletonTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SkeletonTranslate"
    )

    // Linear gradient brush to simulate the sweep light shimmer
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("skeletal_loader"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mock Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(brush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x33000000))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0x22FFFFFF) else Color(0x22000000))
                )
            }
        }

        // Subtitle Text Helper
        Text(
            text = "SYNCING PORTAL CHANNELS...",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )

        // Mock Cards Grid (2 rows of 2 columns) - Matches typical matches odds list
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Box(modifier = Modifier.width(60.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Box(modifier = Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Box(modifier = Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                }
            }

            // Card 2
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Box(modifier = Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Box(modifier = Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                    Box(modifier = Modifier.weight(1f).height(24.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                }
            }
        }

        // Mock Category Tabs Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(brush)
                )
            }
        }

        // Detailed ListItem rows
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circle icon leading
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                // Middle text lines
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(3.dp)).background(brush))
                    Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(brush))
                }
                // Trailing button capsule
                Box(
                    modifier = Modifier
                        .size(48.dp, 20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}
