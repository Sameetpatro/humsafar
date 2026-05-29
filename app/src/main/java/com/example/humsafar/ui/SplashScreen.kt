package com.example.humsafar.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MS = 2000L

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current

    val logoScale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.4f) }
    val ringAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(500))
        logoScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        )
    }

    LaunchedEffect(Unit) {
        delay(180)
        ringAlpha.animateTo(1f, tween(400))
        ringScale.animateTo(1.6f, tween(1200, easing = EaseOutCubic))
        ringAlpha.animateTo(0f, tween(400))
    }

    LaunchedEffect(Unit) {
        delay(450)
        titleAlpha.animateTo(1f, tween(700))
    }

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.46f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.tint.copy(alpha = 0.9f),
                        accent.tint.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension * 0.7f
                ),
                radius = size.minDimension * 0.7f,
                center = center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(ringScale.value)
                        .clip(RoundedCornerShape(60.dp))
                        .border(
                            width = 1.5.dp,
                            color = accent.primary.copy(alpha = ringAlpha.value),
                            shape = RoundedCornerShape(60.dp)
                        )
                )

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(logoScale.value)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(accent.primary, accent.dark)
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(Color(0x66FFFFFF), Color(0x11000000))
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DS",
                        color = accent.onAccent,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            val appTitle = "Dharohar Setu"
            Row(
                modifier = Modifier.scale(0.9f + 0.1f * titleAlpha.value),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                appTitle.forEachIndexed { index, ch ->
                    key(index) {
                        val letterAlpha = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(420 + index * 72L)
                            letterAlpha.animateTo(1f, tween(340, easing = EaseOutCubic))
                        }
                        Text(
                            text = ch.toString(),
                            color = tokens.textPrimary
                                .copy(alpha = titleAlpha.value * letterAlpha.value),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val tagline = "Your Heritage Companion"
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tagline.forEachIndexed { index, ch ->
                    key("tagline_$index") {
                        val a = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(650 + index * 28L)
                            a.animateTo(1f, tween(280))
                        }
                        Text(
                            text = if (ch == ' ') "\u00A0" else ch.toString(),
                            modifier = Modifier.graphicsLayer { alpha = titleAlpha.value * a.value },
                            color = tokens.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
        ) {
            Text(
                text = "Crafted for travellers",
                color = tokens.textTertiary.copy(alpha = titleAlpha.value * 0.8f),
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
