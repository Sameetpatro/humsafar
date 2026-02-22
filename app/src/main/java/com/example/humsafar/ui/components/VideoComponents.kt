// app/src/main/java/com/example/humsafar/ui/components/VideoComponents.kt

package com.example.humsafar.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.humsafar.models.VideoType
import com.example.humsafar.models.VideoUiState
import com.example.humsafar.ui.VideoViewModel
import com.example.humsafar.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Watch Video Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WatchVideoButton(
    videoType: VideoType,
    videoId:   String,
    prompt:    String,
    botText:   String,
    siteName:  String,
    siteId:    String,
    viewModel: VideoViewModel,
    modifier:  Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Color(0xFF1A3A6B), Color(0xFF0D2040))))
            .border(0.7.dp, GlassBorderBright, RoundedCornerShape(50))
            .clickable {
                viewModel.requestVideo(
                    botText  = botText.ifBlank { prompt },
                    siteName = siteName,
                    siteId   = siteId
                )
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, null, tint = AccentYellow, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Watch as Video", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cinematic Loader Overlay
// Visible while state is Loading, Generating, or Error.
// Hidden for Hidden/Idle and ReadyToPlay.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CinematicLoaderOverlay(
    uiState:  VideoUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = uiState is VideoUiState.Loading ||
            uiState is VideoUiState.Generating ||
            uiState is VideoUiState.Error

    AnimatedVisibility(
        visible  = visible,
        enter    = fadeIn(tween(300)),
        exit     = fadeOut(tween(300)),
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xEE050D1A))) {

            // Cancel button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(20.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassWhite15)
                    .border(0.5.dp, GlassBorder, CircleShape)
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }

            Column(
                modifier            = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is VideoUiState.Loading    -> LoadingContent()
                    is VideoUiState.Generating -> GeneratingContent(uiState.progress, uiState.message)
                    is VideoUiState.Error      -> ErrorContent(uiState.message, onCancel)
                    else                       -> Unit
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    val pulse by rememberInfiniteTransition(label = "lp").animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ls"
    )
    Text("🎬", fontSize = 72.sp, modifier = Modifier.scale(pulse))
    Spacer(Modifier.height(24.dp))
    Text(
        "Preparing your video…",
        color      = TextPrimary,
        fontSize   = 20.sp,
        fontWeight = FontWeight.Bold,
        textAlign  = TextAlign.Center
    )
}

@Composable
private fun GeneratingContent(progress: Int, message: String) {
    val pulse by rememberInfiniteTransition(label = "gp").animateFloat(
        0.88f, 1.12f,
        infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "gs"
    )
    Text("🎬", fontSize = 64.sp, modifier = Modifier.scale(pulse))
    Spacer(Modifier.height(28.dp))
    Text(
        "Creating Your Video",
        color      = TextPrimary,
        fontSize   = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign  = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(message, color = TextTertiary, fontSize = 14.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))

    // Progress bar
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(GlassWhite15)
    ) {
        Box(
            Modifier
                .fillMaxWidth((progress / 100f).coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                )
        )
    }
    Spacer(Modifier.height(8.dp))
    Text("$progress%", color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(16.dp))
    Text(
        "First generation takes 1–2 min",
        color     = TextTertiary,
        fontSize  = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ErrorContent(message: String, onDismiss: () -> Unit) {
    Text("⚠️", fontSize = 52.sp)
    Spacer(Modifier.height(16.dp))
    Text("Generation Failed", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(message, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(24.dp))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(GlassWhite15)
            .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
            .clickable { onDismiss() }
            .padding(horizontal = 28.dp, vertical = 12.dp)
    ) {
        Text("Dismiss", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Video Player Overlay — ExoPlayer
//
// WHY THIS REPLACES WEBVIEW:
//   The old implementation used WebView.loadDataWithBaseURL() with an inline
//   HTML <video> tag. Android WebView blocks external MP4 URLs when loaded via
//   a data: URI due to cross-origin restrictions — it renders a black frame
//   with no error. ExoPlayer has no such restriction and streams directly.
//
// ORIENTATION:
//   RESIZE_MODE_FIT preserves the video's native aspect ratio and letterboxes
//   it (black bars) to fit whatever screen orientation the device is in.
//   The built-in PlayerView controller includes a fullscreen toggle button
//   that rotates the display automatically on tap.
// ─────────────────────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerOverlay(
    videoUrl:  String,
    onDismiss: () -> Unit,
    modifier:  Modifier = Modifier
) {
    val context = LocalContext.current

    // Build and prepare once per distinct URL; release on leave
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
            repeatMode    = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {

        // ── ExoPlayer surface (fills entire screen) ───────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player        = exoPlayer
                    resizeMode    = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = true   // play/pause/seek/fullscreen button
                    keepScreenOn  = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Back button (dismisses player, stops playback) ────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xBB000000))
                .border(0.5.dp, Color(0x44FFFFFF), CircleShape)
                .clickable {
                    exoPlayer.stop()
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close video",
                tint     = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}