package com.example.humsafar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Light-theme rebuild of the original "liquid glass" components.
// Public API preserved so every existing call site keeps compiling unchanged;
// internals now produce a soft, paper-like surface on cream with the user's
// chosen accent.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tint: Color = LocalAppColors.current.surface,
    borderColor: Color = LocalAppColors.current.border,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalAppColors.current
    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = tokens.shadow,
                spotColor = tokens.shadow
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint)
            .border(
                width = 0.7.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(borderColor, tokens.divider)
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

@Composable
fun SpecularShine(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp
) {
    val accent = LocalAccent.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.primary.copy(alpha = 0.10f),
                        accent.tint.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    )
}

@Composable
fun AnimatedOrbBackground(modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
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
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.primary.copy(alpha = 0.22f),
                    accent.tint.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                radius = size.minDimension * 0.55f
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * orb1X, size.height * orb1Y)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.dark.copy(alpha = 0.14f),
                    accent.tint.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                radius = size.minDimension * 0.6f
            ),
            radius = size.minDimension * 0.6f,
            center = Offset(size.width * orb2X, size.height * orb2Y)
        )

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x14FFFFFF), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.3f),
                radius = size.maxDimension * 0.6f
            )
        )
    }
}

@Composable
fun GlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val accent = LocalAccent.current
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.97f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) 16.dp else 0.dp,
                shape = RoundedCornerShape(50),
                ambientColor = accent.primary.copy(alpha = 0.35f),
                spotColor = accent.primary.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(accent.primary, accent.dark)
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0x66FFFFFF), Color(0x11000000))
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x33FFFFFF), Color.Transparent)
                    )
                )
        )
        Text(
            text = text,
            color = accent.onAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) accent.primary else tokens.border,
        animationSpec = tween(200),
        label = "tfBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.surfaceMuted)
            .border(
                width = if (focused) 1.4.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(accent.primary),
            visualTransformation = if (isPassword)
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            else
                androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = tokens.textPrimary,
                fontSize = 16.sp,
                letterSpacing = 0.2.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = tokens.textTertiary, fontSize = 16.sp)
                }
                inner()
            }
        )
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = LocalAppColors.current.textTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = modifier
    )
}
