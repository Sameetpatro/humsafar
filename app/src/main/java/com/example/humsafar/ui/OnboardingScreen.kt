package com.example.humsafar.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.Accent
import com.example.humsafar.ui.theme.Accents
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import kotlin.math.sin

/**
 * First-run onboarding. The user scrolls vertically through 4 panels with
 * parallax + spring animations, lands on an 8-swatch accent picker that lives
 * in the last panel, picks a colour (live previewed against the CTA), and
 * taps "Get Started" to finish.
 */
@Composable
fun OnboardingScreen(
    onAccentPicked: (Accent) -> Unit,
    onFinish: () -> Unit
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val panelHeight = configuration.screenHeightDp.dp

    val scrollState = rememberScrollState()
    val scrollPx by remember { derivedStateOf { scrollState.value.toFloat() } }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientParticles(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            OnboardPanel(
                index = 0,
                scrollPx = scrollPx,
                screenHeightPx = screenHeightPx,
                height = panelHeight
            ) { progress ->
                Panel1(progress)
            }

            OnboardPanel(
                index = 1,
                scrollPx = scrollPx,
                screenHeightPx = screenHeightPx,
                height = panelHeight
            ) { progress ->
                Panel2(progress)
            }

            OnboardPanel(
                index = 2,
                scrollPx = scrollPx,
                screenHeightPx = screenHeightPx,
                height = panelHeight
            ) { progress ->
                Panel3(progress)
            }

            OnboardPanel(
                index = 3,
                scrollPx = scrollPx,
                screenHeightPx = screenHeightPx,
                height = panelHeight
            ) { progress ->
                Panel4Accent(
                    progress = progress,
                    onAccentPicked = onAccentPicked,
                    onFinish = onFinish
                )
            }
        }

        // Top progress dots — independent of scroll, morph based on which
        // panel is most-visible
        val activePanel by remember {
            derivedStateOf { (scrollPx / screenHeightPx).coerceIn(0f, 3f) }
        }
        ProgressDots(
            activePanel = activePanel,
            totalPanels = 4,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        )

        // Hint to scroll down (fades after the user starts moving)
        val hintAlpha = (1f - (scrollPx / (screenHeightPx * 0.4f))).coerceIn(0f, 1f)
        if (hintAlpha > 0.01f) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .graphicsLayer { alpha = hintAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BouncingChevron(accent.primary)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Scroll to begin",
                    color = tokens.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Per-panel scaffolding
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnboardPanel(
    index: Int,
    scrollPx: Float,
    screenHeightPx: Float,
    height: Dp,
    content: @Composable BoxScope.(progress: Float) -> Unit
) {
    val center = index * screenHeightPx
    // -1 (just above), 0 (centred), 1 (just below)
    val rawProgress = (scrollPx - center) / screenHeightPx
    val progress = rawProgress.coerceIn(-1f, 1f)
    // Visibility 0..1 — peaks when this panel is centred
    val visibility = (1f - kotlin.math.abs(progress)).coerceIn(0f, 1f)

    val tokens = LocalAppColors.current
    val backgroundBrush = Brush.verticalGradient(
        colors = when (index) {
            0 -> listOf(tokens.bgWarm, tokens.surface)
            1 -> listOf(tokens.surface, tokens.bgWarm)
            2 -> listOf(tokens.bgWarm, tokens.bgWarmDeep)
            else -> listOf(tokens.bgWarmDeep, tokens.surface)
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundBrush)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Each panel slides in from below with a parallax factor
                    translationY = progress * 120f
                    alpha = visibility.coerceAtLeast(0.05f)
                }
        ) {
            content(visibility)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Panels
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoxScope.Panel1(progress: Float) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "p1pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(pulse * (0.7f + 0.3f * progress)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.primary.copy(alpha = 0.35f),
                            accent.tint.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = r
                    ),
                    radius = r,
                    center = center
                )
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(accent.primary, accent.dark))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDDFA", fontSize = 44.sp)
            }
        }

        Spacer(Modifier.height(36.dp))

        StaggeredText(
            title = "Discover heritage\nlike never before",
            subtitle = "A curated companion for every monument, fort and temple along your route.",
            progress = progress,
            tokens = tokens
        )
    }
}

@Composable
private fun BoxScope.Panel2(progress: Float) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current

    val infinite = rememberInfiniteTransition(label = "wave")
    val phase by infinite.animateFloat(
        0f,
        2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "wavePhase"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barCount = 18
                val gap = size.width / (barCount + 1)
                val midY = size.height / 2f
                for (i in 0 until barCount) {
                    val x = gap * (i + 1)
                    val ampScale = 0.5f + 0.5f * sin(phase + i * 0.4f)
                    val h = (size.height * 0.18f) + (size.height * 0.55f * ampScale * progress)
                    drawRoundRect(
                        color = if (i % 2 == 0) accent.primary else accent.dark,
                        topLeft = Offset(x - 6f, midY - h / 2f),
                        size = androidx.compose.ui.geometry.Size(12f, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        StaggeredText(
            title = "Voice guides in\nyour language",
            subtitle = "Tap once and let the storyteller walk you through. English, हिंदी or Hinglish — your call.",
            progress = progress,
            tokens = tokens
        )
    }
}

@Composable
private fun BoxScope.Panel3(progress: Float) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current

    val infinite = rememberInfiniteTransition(label = "scan")
    val ring1 by infinite.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = EaseOutCubic)),
        label = "ring1"
    )
    val ring2 by infinite.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(1800, delayMillis = 600, easing = EaseOutCubic)),
        label = "ring2"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(0.7f + 0.3f * progress),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                listOf(ring1, ring2).forEach { t ->
                    drawCircle(
                        color = accent.primary.copy(alpha = (1f - t) * 0.5f),
                        radius = r * (0.4f + 0.6f * t),
                        center = center,
                        style = Stroke(width = 4f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(accent.tint),
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDCF7", fontSize = 56.sp)
            }
        }

        Spacer(Modifier.height(36.dp))

        StaggeredText(
            title = "Scan, learn,\nsave every visit",
            subtitle = "Point your camera at a heritage QR — the spot, its history and your photo memories all unlock at once.",
            progress = progress,
            tokens = tokens
        )
    }
}

@Composable
private fun BoxScope.Panel4Accent(
    progress: Float,
    onAccentPicked: (Accent) -> Unit,
    onFinish: () -> Unit
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 96.dp, bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StaggeredText(
            title = "Make it\nyours",
            subtitle = "Pick a colour you love. Every screen will pick it up.",
            progress = progress,
            tokens = tokens,
            centered = true
        )

        Spacer(Modifier.height(28.dp))

        AccentPickerGrid(
            current = accent,
            onPick = onAccentPicked
        )

        Spacer(Modifier.height(28.dp))

        AccentPreviewCard(accent = accent)

        Spacer(Modifier.weight(1f))

        GlassPrimaryButton(
            text = "Get Started",
            onClick = {
                onAccentPicked(accent)
                onFinish()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "You can change this later in Settings",
            color = tokens.textTertiary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Accent picker
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccentPickerGrid(
    current: Accent,
    onPick: (Accent) -> Unit
) {
    val rows = Accents.chunked(4)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { a ->
                    AccentSwatch(
                        accent = a,
                        selected = a.name == current.name,
                        onClick = { onPick(a) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: Accent,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "swatchScale"
    )

    val infinite = rememberInfiniteTransition(label = "ringRot")
    val rotation by infinite.animateFloat(
        0f, 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "ringSpin"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Canvas(Modifier.matchParentSize()) {
                val r = size.minDimension / 2f
                rotate(degrees = rotation, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                accent.primary,
                                accent.dark,
                                accent.primary
                            ),
                            center = center
                        ),
                        radius = r,
                        center = center,
                        style = Stroke(width = 6f)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(accent.primary, accent.dark))
                )
                .border(
                    width = 1.dp,
                    color = Color(0x33000000),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text("\u2713", color = accent.onAccent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun AccentPreviewCard(accent: Accent) {
    val tokens = LocalAppColors.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(accent.primary, accent.dark))
                        )
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = accent.name,
                        color = tokens.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live preview",
                        color = tokens.textTertiary,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(tokens.surfaceMuted)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(listOf(accent.primary, accent.dark))
                        )
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.tint)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Heritage", color = accent.dark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.primary)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Voice", color = accent.onAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StaggeredText(
    title: String,
    subtitle: String,
    progress: Float,
    tokens: com.example.humsafar.ui.theme.LightTokens,
    centered: Boolean = true
) {
    AnimatedVisibility(
        visible = progress > 0.05f,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 },
        exit = fadeOut(tween(150))
    ) {
        Column(
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = title,
                color = tokens.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 36.sp,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                letterSpacing = (-0.5).sp
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = subtitle,
                color = tokens.textSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier
                    .padding(horizontal = if (centered) 8.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun ProgressDots(
    activePanel: Float,
    totalPanels: Int,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalPanels) { i ->
            val distance = kotlin.math.abs(activePanel - i)
            val width = (8.dp + (16.dp - 8.dp) * (1f - distance.coerceIn(0f, 1f)))
            val color = if (distance < 0.5f) accent.primary else tokens.borderStrong
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun BouncingChevron(color: Color) {
    val infinite = rememberInfiniteTransition(label = "chev")
    val offset by infinite.animateFloat(
        0f, 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevOff"
    )
    Canvas(
        modifier = Modifier
            .size(width = 22.dp, height = 14.dp)
            .graphicsLayer { translationY = offset }
    ) {
        val w = size.width
        val h = size.height
        val stroke = 3f
        drawLine(color, Offset(2f, 2f), Offset(w / 2f, h - 2f), strokeWidth = stroke)
        drawLine(color, Offset(w - 2f, 2f), Offset(w / 2f, h - 2f), strokeWidth = stroke)
    }
}

@Composable
private fun AmbientParticles(modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val infinite = rememberInfiniteTransition(label = "particles")
    val t by infinite.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "pt"
    )
    Canvas(modifier = modifier) {
        val particleCount = 14
        for (i in 0 until particleCount) {
            val seed = i.toFloat()
            val x = (size.width * ((seed * 0.137f + t * (0.3f + 0.05f * i)) % 1f))
            val y = size.height * (0.05f + 0.9f * ((seed * 0.241f + t * 0.5f) % 1f))
            val r = 3f + 4f * (sin(t * 6.283f + seed) * 0.5f + 0.5f)
            drawCircle(
                color = accent.primary.copy(alpha = 0.10f + 0.10f * sin(t * 6.283f + seed)),
                radius = r,
                center = Offset(x, y)
            )
        }
    }
}

