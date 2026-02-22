// app/src/main/java/com/example/humsafar/ui/VideoPlayerScreen.kt
// Shows generation progress, then plays video in a WebView (works with Supabase public URLs)

package com.example.humsafar.ui

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.theme.*

@Composable
fun VideoPlayerScreen(
    botText:  String,
    siteName: String,
    siteId:   String,
    onBack:   () -> Unit,
    viewModel: VideoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Auto-start generation when screen opens
    LaunchedEffect(botText, siteName) {
        if (state is VideoUiState.Idle) {
            viewModel.requestVideo(botText, siteName, siteId)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xF0050D1A), Color(0xBB050D1A))))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp).clip(CircleShape)
                            .background(GlassWhite15)
                            .border(0.5.dp, GlassBorder, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Heritage Video", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(siteName, color = TextTertiary, fontSize = 12.sp)
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────
            AnimatedContent(targetState = state, label = "videoState") { s ->
                when (s) {
                    is VideoUiState.Idle,
                    is VideoUiState.Generating -> {
                        val progress = (s as? VideoUiState.Generating)?.progress ?: 0
                        val message  = (s as? VideoUiState.Generating)?.message ?: "Starting…"
                        GeneratingView(progress = progress, message = message)
                    }
                    is VideoUiState.Ready -> {
                        VideoView(url = s.videoUrl)
                    }
                    is VideoUiState.Error -> {
                        ErrorView(message = s.message) {
                            viewModel.reset()
                            viewModel.requestVideo(botText, siteName, siteId)
                        }
                    }
                }
            }
        }
    }
}

// ── Generating progress view ──────────────────────────────────────────────────

@Composable
private fun GeneratingView(progress: Int, message: String) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "p"
    )

    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎬", fontSize = 64.sp, modifier = Modifier.scale(pulse))
        Spacer(Modifier.height(32.dp))

        Text("Creating Your Video", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = TextTertiary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(GlassWhite15)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("$progress%", color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(24.dp))
        Text(
            "This takes 1–2 minutes on first generation.\nSubsequent requests are instant.",
            color = TextTertiary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 18.sp
        )
    }
}

// ── Video playback view ────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VideoView(url: String) {
    // Embed the MP4 URL in a simple HTML5 video player
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { background: #050D1A; display: flex; align-items: center; justify-content: center; height: 100vh; }
            video { width: 100%; max-height: 100vh; object-fit: contain; }
          </style>
        </head>
        <body>
          <video controls autoplay playsinline>
            <source src="$url" type="video/mp4">
            Your browser does not support video.
          </video>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                webViewClient   = WebViewClient()
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ── Error view ────────────────────────────────────────────────────────────────

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 52.sp)
        Spacer(Modifier.height(24.dp))
        Text("Video Generation Failed", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))))
                .clickable { onRetry() }
                .padding(horizontal = 32.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Refresh, null, tint = Color(0xFF050D1A), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Try Again", color = Color(0xFF050D1A), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}