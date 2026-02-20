// app/src/main/java/com/example/humsafar/ui/VoiceChatScreen.kt
// NEW FILE

package com.example.humsafar.ui

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.models.VoiceChatResponse
import com.example.humsafar.models.VoiceUiState
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceChatScreen(
    siteName:            String,
    siteId:              String,
    onBack:              () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel:           VoiceChatViewModel = viewModel()
) {
    LaunchedEffect(siteName, siteId) {
        viewModel.currentSiteName = siteName
        viewModel.currentSiteId   = siteId
    }

    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val micPerm   = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────
            VoiceTopBar(
                siteName             = siteName,
                onBack               = onBack,
                onSettings           = onNavigateToSettings
            )

            // ── Message list ──────────────────────────────────────────────
            LazyColumn(
                state               = listState,
                modifier            = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item { EmptyHint(siteName) }
                }
                items(messages, key = { it.userText + it.botText }) { msg ->
                    VoiceMessageCard(msg)
                }
            }

            // ── Status banner ─────────────────────────────────────────────
            StatusBanner(uiState = uiState, modifier = Modifier.padding(horizontal = 24.dp))

            // ── Mic button ────────────────────────────────────────────────
            MicSection(
                uiState           = uiState,
                hasPermission     = micPerm.status.isGranted,
                onRequestPerm     = { micPerm.launchPermissionRequest() },
                onStartRecording  = { viewModel.startRecording() },
                onStopRecording   = { viewModel.stopAndSend() },
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 52.dp, top = 12.dp)
            )
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun VoiceTopBar(
    siteName:  String,
    onBack:    () -> Unit,
    onSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xF0050D1A), Color(0xBB050D1A))))
            .drawBehind {
                drawLine(GlassBorder, Offset(0f, size.height), Offset(size.width, size.height), 0.5.dp.toPx())
            }
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {

            // Back
            IconCircle(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.width(14.dp))

            // Mic badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, null, tint = DeepNavy, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(siteName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Voice Heritage Guide", color = TextTertiary, fontSize = 12.sp)
            }

            // Settings
            IconCircle(onClick = onSettings) {
                Icon(Icons.Default.Settings, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun IconCircle(onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(GlassWhite15)
            .border(0.5.dp, GlassBorder, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyHint(siteName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp)
    ) {
        Text("🎙️", fontSize = 52.sp)
        Spacer(Modifier.height(20.dp))
        Text("Hold the mic to ask about", color = TextTertiary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(siteName, color = AccentYellow, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("in English, Hindi, or Hinglish", color = TextTertiary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// ── Message card ──────────────────────────────────────────────────────────────

@Composable
private fun VoiceMessageCard(response: VoiceChatResponse) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // User utterance (right-aligned, gold tint)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GlassCard(
                modifier     = Modifier.widthIn(max = 280.dp),
                cornerRadius = 20.dp,
                tint         = Color(0x2DFFD54F)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    SectionLabel("You said")
                    Spacer(Modifier.height(4.dp))
                    Text(response.userText, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }

        // Bot response (left-aligned, default glass)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            GlassCard(modifier = Modifier.widthIn(max = 280.dp), cornerRadius = 20.dp) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏛️", fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        SectionLabel("Heritage Guide")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(response.botText, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
        }
    }
}

// ── Status banner ─────────────────────────────────────────────────────────────

@Composable
private fun StatusBanner(uiState: VoiceUiState, modifier: Modifier = Modifier) {
    val text = when (uiState) {
        is VoiceUiState.Recording  -> "🔴  Listening…"
        is VoiceUiState.Processing -> "⏳  Processing…"
        is VoiceUiState.Error      -> "⚠️  ${uiState.message}"
        else -> ""
    }
    val color = if (uiState is VoiceUiState.Error) Color(0xFFFF6B6B)
    else if (uiState is VoiceUiState.Recording) Color(0xFFFF6B6B)
    else TextTertiary

    AnimatedVisibility(text.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
        Text(text, color = color, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth().padding(vertical = 4.dp))
    }
}

// ── Mic button ────────────────────────────────────────────────────────────────

@Composable
private fun MicSection(
    uiState:          VoiceUiState,
    hasPermission:    Boolean,
    onRequestPerm:    () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording:  () -> Unit,
    modifier:         Modifier = Modifier
) {
    val isRecording  = uiState is VoiceUiState.Recording
    val isProcessing = uiState is VoiceUiState.Processing

    val glowAlpha by rememberInfiniteTransition(label = "glow").animateFloat(
        0.25f, 0.85f,
        infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ga"
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, 1.14f,
        infiniteRepeatable(tween(700, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ps"
    )

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {

        // Hint text
        Text(
            text = when {
                !hasPermission -> "Tap to grant microphone access"
                isRecording    -> "Release to send"
                isProcessing   -> "Please wait…"
                else           -> "Hold to speak"
            },
            color = TextTertiary, fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        Box(contentAlignment = Alignment.Center) {

            // Animated glow ring — only while recording
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFFFF4444).copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Core button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(if (isRecording) pulse * 0.93f else 1f)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRecording  -> Brush.linearGradient(listOf(Color(0xFFFF4444), Color(0xFFCC0000)))
                            isProcessing -> Brush.linearGradient(listOf(GlassWhite20, GlassWhite15))
                            else         -> Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                        }
                    )
                    .border(1.dp, if (isRecording) Color(0x55FFFFFF) else GlassBorder, CircleShape)
                    .clickable(enabled = !isProcessing) {
                        when {
                            !hasPermission -> onRequestPerm()
                            isRecording    -> onStopRecording()
                            else           -> onStartRecording()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Specular highlight
                Box(
                    Modifier.matchParentSize().clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color.Transparent)))
                )
                Icon(
                    imageVector        = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop" else "Record",
                    tint               = if (isProcessing) TextTertiary else DeepNavy,
                    modifier           = Modifier.size(32.dp)
                )
            }
        }
    }
}