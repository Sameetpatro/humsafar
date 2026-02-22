// app/src/main/java/com/example/humsafar/ui/components/VideoComponents.kt
//
// Premium cinematic video UI — liquid glass aesthetic, funded-startup feel.
// Covers:
//   • WatchVideoButton      — glassmorphism chip shown below bot messages
//   • CinematicLoaderOverlay— fullscreen blurred loader with animated stages
//   • VideoPlayerOverlay    — fullscreen ExoPlayer with fade-in & minimalist controls

package com.example.humsafar.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.humsafar.models.LOADER_STAGES
import com.example.humsafar.models.VideoType
import com.example.humsafar.models.VideoUiState
import com.example.humsafar.ui.VideoViewModel
import com.example.humsafar.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// Watch Video Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WatchVideoButton(
    videoType: VideoType,
    videoId: String,
    prompt: String = "",
    botText: String = "",
    siteName: String = "",
    siteId: String = "",
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    // Delayed fade-in after message appears
    LaunchedEffect(Unit) {
        delay(400)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600)) + slideInVertically(tween(500, easing = EaseOutCubic)) { it / 2 },
        modifier = modifier
    ) {
        var pressed by remember { mutableStateOf(false) }
        val elevation by animateFloatAsState(
            targetValue = if (pressed) 0f else 6f,
            animationSpec = spring(stiffness = Spring.StiffnessHigh),
            label = "elev"
        )
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessHigh),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .scale(scale)
                .shadow(elevation.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0x33FFD54F), Color(0x1AFFC107), Color(0x22FFFFFF))
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0x66FFD54F), Color(0x33FFFFFF), Color(0x22FFD54F))
                    ),
                    shape = RoundedCornerShape(50)
                )
                .clickable {
                    pressed = true
                    viewModel.requestVideo(
                        videoType = videoType,
                        videoId = videoId,
                        prompt = prompt,
                        botText = botText,
                        siteName = siteName,
                        siteId = siteId
                    )
                }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Glowing film reel icon
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(Color(0x44FFD54F), Color.Transparent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎬", fontSize = 14.sp)
                }
                Text(
                    text = "Watch Video Instead",
                    color = Color(0xFFFFE082),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }
        }

        // Reset press state
        LaunchedEffect(pressed) {
            if (pressed) { delay(150); pressed = false }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cinematic Loader Overlay — fullscreen with animated text stages
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CinematicLoaderOverlay(
    uiState: VideoUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = uiState !is VideoUiState.Hidden && uiState !is VideoUiState.ReadyToPlay

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(400)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE040C1A))  // near-opaque blur substitute
        ) {
            // Animated orb background inside the overlay
            CinematicOrbBackground(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── AI brain glow animation ─────────────────────────────
                GlowingOrb()

                Spacer(Modifier.height(40.dp))

                // ── Animated stage text ─────────────────────────────────
                AnimatedStageText(uiState = uiState)

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Generating AI Summary Video For You",
                    color = Color(0x88FFFFFF),
                    fontSize = 13.sp,
                    letterSpacing = 0.3.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(36.dp))

                // ── Progress indicator ──────────────────────────────────
                when (uiState) {
                    is VideoUiState.Generating -> {
                        LinearGlowProgress(progress = uiState.progress / 100f)
                    }
                    else -> {
                        IndeterminateGlowLine()
                    }
                }

                Spacer(Modifier.height(52.dp))

                // ── Cancel button ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x22FFFFFF))
                        .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(50))
                        .clickable { onCancel() }
                        .padding(horizontal = 28.dp, vertical = 12.dp)
                ) {
                    Text("Cancel", color = Color(0x99FFFFFF), fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Glowing orb ───────────────────────────────────────────────────────────────

@Composable
private fun GlowingOrb() {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val scale by infiniteTransition.animateFloat(
        0.9f, 1.1f,
        infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "s"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        0.4f, 0.9f,
        infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "g"
    )
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "r"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp)
    ) {
        // Outer glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFFFFD54F).copy(alpha = glowAlpha * 0.3f),
                        Color(0xFFFFC107).copy(alpha = 0f)
                    )
                ),
                radius = size.minDimension * 0.6f * scale
            )
        }

        // Rotating ring
        Canvas(modifier = Modifier.size(90.dp).rotate(rotation)) {
            val stroke = 1.5.dp.toPx()
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, Color(0x88FFD54F), Color.Transparent)
                ),
                radius = size.minDimension / 2f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
            )
        }

        // Core
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFFFFC107), Color(0xFFE65100))
                    )
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0x22FFFFFF))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("✨", fontSize = 26.sp)
        }
    }
}

// ── Animated stage text with crossfade ────────────────────────────────────────

@Composable
private fun AnimatedStageText(uiState: VideoUiState) {
    var stageIndex by remember { mutableStateOf(0) }

    val stageText = when (uiState) {
        is VideoUiState.Generating -> uiState.stage
        is VideoUiState.CinematicLoader -> LOADER_STAGES[stageIndex]
        is VideoUiState.Error -> "⚠️ ${uiState.message}"
        else -> LOADER_STAGES[stageIndex]
    }

    // Auto-cycle stages when in CinematicLoader mode
    if (uiState is VideoUiState.CinematicLoader) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(1_600)
                stageIndex = (stageIndex + 1) % LOADER_STAGES.size
            }
        }
    }

    Crossfade(targetState = stageText, animationSpec = tween(500), label = "stage") { text ->
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.3).sp
        )
    }
}

// ── Indeterminate glow line ───────────────────────────────────────────────────

@Composable
private fun IndeterminateGlowLine() {
    val infiniteTransition = rememberInfiniteTransition(label = "prog")
    val offset by infiniteTransition.animateFloat(
        -0.3f, 1.3f,
        infiniteRepeatable(tween(1400, easing = EaseInOutCubic), RepeatMode.Restart),
        label = "o"
    )

    Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
        // Track
        drawRoundRect(
            color = Color(0x22FFFFFF),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f)
        )
        // Glow head
        val x = size.width * offset
        val barWidth = size.width * 0.35f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color(0xFFFFD54F), Color.Transparent),
                startX = (x - barWidth).coerceAtLeast(0f),
                endX = (x + barWidth).coerceAtMost(size.width)
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f)
        )
    }
}

// ── Determinate linear progress ───────────────────────────────────────────────

@Composable
private fun LinearGlowProgress(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = EaseOutCubic),
        label = "p"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            "${(animatedProgress * 100).toInt()}%",
            color = Color(0x99FFD54F),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Canvas(modifier = Modifier.fillMaxWidth().height(3.dp)) {
            drawRoundRect(color = Color(0x22FFFFFF), cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f))
            if (animatedProgress > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFFC107), Color(0xFFFFD54F))
                    ),
                    size = androidx.compose.ui.geometry.Size(size.width * animatedProgress, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f)
                )
            }
        }
    }
}

// ── Cinematic orb background inside loader ────────────────────────────────────

@Composable
private fun CinematicOrbBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val o1x by t.animateFloat(0.2f, 0.8f, infiniteRepeatable(tween(6000, easing = EaseInOutSine), RepeatMode.Reverse), label = "x1")
    val o2x by t.animateFloat(0.7f, 0.15f, infiniteRepeatable(tween(8000, easing = EaseInOutSine), RepeatMode.Reverse), label = "x2")

    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x33B8860B), Color.Transparent),
                radius = size.minDimension * 0.5f
            ),
            radius = size.minDimension * 0.5f,
            center = Offset(size.width * o1x, size.height * 0.35f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x22103080), Color.Transparent),
                radius = size.minDimension * 0.55f
            ),
            radius = size.minDimension * 0.55f,
            center = Offset(size.width * o2x, size.height * 0.65f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video Player Overlay — ExoPlayer fullscreen with cinematic controls
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerOverlay(
    videoUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    var playerReady by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) playerReady = true
                }
            })
        }
    }

    // Auto-hide controls after 3s
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3_000)
            controlsVisible = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(500)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { controlsVisible = !controlsVisible }
        ) {
            // ── ExoPlayer surface ─────────────────────────────────────────
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Fade-in overlay when video first appears ──────────────────
            AnimatedVisibility(
                visible = !playerReady,
                exit = fadeOut(tween(800))
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }

            // ── Cinematic controls overlay ────────────────────────────────
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(300))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // Top gradient + close button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .height(120.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xCC000000), Color.Transparent)
                                )
                            )
                    )

                    // Close button
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(20.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x66000000))
                            .border(0.5.dp, Color(0x44FFFFFF), CircleShape)
                            .clickable { exoPlayer.release(); onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    // Center play/pause
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0x88000000))
                            .border(1.dp, Color(0x55FFFFFF), CircleShape)
                            .clickable {
                                if (exoPlayer.isPlaying) { exoPlayer.pause(); isPlaying = false }
                                else { exoPlayer.play(); isPlaying = true }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Bottom gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .height(100.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xCC000000))
                                )
                            )
                    )
                }
            }
        }
    }
}