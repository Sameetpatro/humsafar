package com.example.humsafar.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.humsafar.ui.theme.*

@Composable
fun TripInfoButton(
    siteName: String,
    visitedCount: Int,
    totalCount: Int,
    onDirectionsClick: () -> Unit,
    onEndTripClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "trip_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .scale(pulseScale)
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            AccentYellow.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFFFFC107))
                    )
                )
                .border(
                    2.dp,
                    Brush.linearGradient(
                        listOf(Color(0x66FFFFFF), Color(0x22FFFFFF))
                    ),
                    CircleShape
                )
                .clickable { showBottomSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = "Trip Info",
                tint = DeepNavy,
                modifier = Modifier.size(26.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF4ADE80))
                .border(2.dp, DeepNavy, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$visitedCount",
                color = DeepNavy,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    if (showBottomSheet) {
        TripInfoBottomSheet(
            siteName = siteName,
            visitedCount = visitedCount,
            totalCount = totalCount,
            onDismiss = { showBottomSheet = false },
            onDirectionsClick = {
                showBottomSheet = false
                onDirectionsClick()
            },
            onEndTripClick = {
                showBottomSheet = false
                onEndTripClick()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripInfoBottomSheet(
    siteName: String,
    visitedCount: Int,
    totalCount: Int,
    onDismiss: () -> Unit,
    onDirectionsClick: () -> Unit,
    onEndTripClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0A1628),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GlassWhite30)
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AccentYellow, GoldGlow))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🚶", fontSize = 22.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Active Trip",
                        color = AccentYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        siteName,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x224ADE80))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "$visitedCount/$totalCount",
                        color = Color(0xFF4ADE80),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            TripActionButton(
                icon = Icons.Default.Map,
                title = "Directions / Current Scenario",
                subtitle = "View map with all nodes and your progress",
                iconBackground = Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF1976D2))),
                onClick = onDirectionsClick
            )

            Spacer(Modifier.height(12.dp))

            TripActionButton(
                icon = Icons.Default.Close,
                title = "End Trip",
                subtitle = "Complete your trip and see recommendations",
                iconBackground = Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFD32F2F))),
                onClick = onEndTripClick,
                isDestructive = true
            )
        }
    }
}

@Composable
private fun TripActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconBackground: Brush,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val borderColor = if (isDestructive) {
        Color(0x33FF5252)
    } else {
        GlassBorder
    }

    val backgroundColor = if (isDestructive) {
        Color(0x15FF5252)
    } else {
        GlassWhite10
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(0.7.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (isDestructive) Color(0xFFFF6B6B) else TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = TextTertiary,
                fontSize = 12.sp
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
