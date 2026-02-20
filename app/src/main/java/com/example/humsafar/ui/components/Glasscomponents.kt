package com.example.humsafar.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.ui.theme.*

// ── Liquid Glass Card ────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tint: Color = GlassWhite15,
    borderColor: Color = GlassBorder,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint)
            .border(
                width = 0.7.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(GlassBorderBright, Color.Transparent, borderColor)
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

// ── Specular Shine overlay ───────────────────────────────────────────────────
@Composable
fun SpecularShine(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x33FFFFFF),
                        Color(0x08FFFFFF),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    )
}

// ── Animated Orb Background ──────────────────────────────────────────────────
@Composable
fun AnimatedOrbBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    val orb1X by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(8000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "o1x"
    )
    val orb1Y by infiniteTransition.animateFloat(
        initialValue = 0.1f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(7000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "o1y"
    )
    val orb2X by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(9000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "o2x"
    )
    val orb2Y by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(11000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "o2y"
    )

    Canvas(modifier = modifier) {
        // Deep base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF040C1A), Color(0xFF071428), Color(0xFF050F22))
            )
        )

        // Orb 1 — warm gold
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x55C8860A), Color(0x22805200), Color.Transparent),
                radius = size.minDimension * 0.55f
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * orb1X, size.height * orb1Y)
        )

        // Orb 2 — cool blue
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x44103080), Color(0x22081840), Color.Transparent),
                radius = size.minDimension * 0.6f
            ),
            radius = size.minDimension * 0.6f,
            center = Offset(size.width * orb2X, size.height * orb2Y)
        )

        // Fine noise shimmer overlay (approximated with subtle gradient)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x08FFFFFF), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.3f),
                radius = size.maxDimension * 0.6f
            )
        )
    }
}

// ── Glass Button ─────────────────────────────────────────────────────────────
@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.97f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD54F), Color(0xFFFFC107))
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0x22FFFFFF))),
                shape = RoundedCornerShape(50)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner specular on button
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x44FFFFFF), Color.Transparent)
                    )
                )
        )
        Text(
            text = text,
            color = Color(0xFF0A1628),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 0.3.sp
        )
    }
}

// ── Glass Text Field ─────────────────────────────────────────────────────────
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GlassWhite10)
            .border(
                width = 0.7.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0x44FFFFFF), Color(0x11FFFFFF))
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (isPassword)
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            else
                androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 16.sp,
                letterSpacing = 0.2.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = TextTertiary, fontSize = 16.sp)
                }
                inner()
            }
        )
    }
}

// ── Floating Label ───────────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = modifier
    )
}