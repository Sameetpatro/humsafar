// app/src/main/java/com/example/humsafar/ui/components/VideoComponents.kt

package com.example.humsafar.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import com.example.humsafar.models.VideoType
import com.example.humsafar.models.VideoUiState
import com.example.humsafar.ui.VideoViewModel
import com.example.humsafar.ui.theme.*

// ── Watch Video Button ────────────────────────────────────────────────────────

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

// ── Cinematic Loader Overlay ──────────────────────────────────────────────────

@Composable
fun CinematicLoaderOverlay(
    uiState:  VideoUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = uiState !is VideoUiState.Hidden && uiState !is VideoUiState.ReadyToPlay

    AnimatedVisibility(visible, enter = fadeIn(tween(300)), exit = fadeOut(tween(300)), modifier = modifier) {
        Box(Modifier.fillMaxSize().background(Color(0xEE050D1A))) {

            // Cancel button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd).statusBarsPadding().padding(20.dp)
                    .size(40.dp).clip(CircleShape).background(GlassWhite15)
                    .border(0.5.dp, GlassBorder, CircleShape).clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }

            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is VideoUiState.Loading     -> LoadingContent()
                    is VideoUiState.Generating  -> GeneratingContent(uiState.progress, uiState.message)
                    is VideoUiState.Error       -> ErrorContent(uiState.message, onCancel)
                    else                        -> Unit
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    val pulse by rememberInfiniteTransition(label = "l").animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse), label = "lp"
    )
    Text("🎬", fontSize = 72.sp, modifier = Modifier.scale(pulse))
    Spacer(Modifier.height(24.dp))
    Text("Preparing your video…", color = TextPrimary, fontSize = 20.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
private fun GeneratingContent(progress: Int, message: String) {
    val pulse by rememberInfiniteTransition(label = "g").animateFloat(
        0.88f, 1.12f,
        infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "gp"
    )
    Text("🎬", fontSize = 64.sp, modifier = Modifier.scale(pulse))
    Spacer(Modifier.height(28.dp))
    Text("Creating Your Video", color = TextPrimary, fontSize = 22.sp,
        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(message, color = TextTertiary, fontSize = 14.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(GlassWhite15)) {
        Box(
            Modifier.fillMaxWidth((progress / 100f).coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))))
        )
    }
    Spacer(Modifier.height(8.dp))
    Text("$progress%", color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(16.dp))
    Text("First generation takes 1–2 min", color = TextTertiary, fontSize = 12.sp, textAlign = TextAlign.Center)
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
        modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassWhite15)
            .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
            .clickable { onDismiss() }.padding(horizontal = 28.dp, vertical = 12.dp)
    ) {
        Text("Dismiss", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Video Player Overlay ──────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerOverlay(videoUrl: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val html = """<!DOCTYPE html><html>
        <head><meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>* {margin:0;padding:0;box-sizing:border-box;}
        body{background:#050D1A;display:flex;align-items:center;justify-content:center;height:100vh;}
        video{width:100%;max-height:100vh;object-fit:contain;}</style></head>
        <body><video controls autoplay playsinline>
        <source src="$videoUrl" type="video/mp4"></video></body></html>""".trimIndent()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF050D1A))) {
        AndroidView(factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled               = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                webViewClient   = WebViewClient()
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }, modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)
                .size(44.dp).clip(CircleShape).background(Color(0xBB050D1A))
                .border(0.5.dp, GlassBorder, CircleShape).clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}