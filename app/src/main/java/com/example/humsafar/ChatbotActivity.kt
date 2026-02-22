// app/src/main/java/com/example/humsafar/ChatbotActivity.kt
// UPDATED: Integrated video feature — WatchVideoButton, CinematicLoaderOverlay, VideoPlayerOverlay
// ChatMessage model updated with video flags.
// VideoViewModel injected via viewModel().

package com.example.humsafar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.models.ChatMessage
import com.example.humsafar.models.VideoType
import com.example.humsafar.models.VideoUiState
import com.example.humsafar.ui.VideoViewModel
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.CinematicLoaderOverlay
import com.example.humsafar.ui.components.VideoPlayerOverlay
import com.example.humsafar.ui.components.WatchVideoButton
import com.example.humsafar.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────
class ChatbotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val siteName = intent.getStringExtra("SITE_NAME") ?: "Heritage Site"
        val siteId   = intent.getStringExtra("SITE_ID")   ?: ""
        setContent {
            com.example.humsafar.ui.theme.HumsafarTheme {
                ChatbotScreen(siteName = siteName, siteId = siteId, onBack = { finish() })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Network helper (unchanged)
// ─────────────────────────────────────────────────────────────────────────────
suspend fun callBackend(
    message: String,
    siteName: String,
    siteId: String,
    history: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    try {
        val baseUrl = "https://humsafar-backend-59ic.onrender.com"
        try {
            (URL("$baseUrl/").openConnection() as HttpURLConnection).apply {
                connectTimeout = 60_000; readTimeout = 60_000; requestMethod = "GET"; responseCode; disconnect()
            }
        } catch (_: Exception) {}

        val historyArray = JSONArray()
        history.filter { !it.isLoading }.forEach { msg ->
            historyArray.put(JSONObject().apply {
                put("role",    if (msg.isUser) "user" else "assistant")
                put("content", msg.text)
            })
        }

        val conn = (URL("$baseUrl/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true; connectTimeout = 30_000; readTimeout = 120_000
        }
        val body = JSONObject().apply {
            put("message", message); put("site_name", siteName); put("site_id", siteId); put("history", historyArray)
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        if (conn.responseCode == HttpURLConnection.HTTP_OK)
            JSONObject(conn.inputStream.bufferedReader().readText()).getString("reply")
        else "Server error ${conn.responseCode}"
    } catch (e: Exception) { "Connection failed: ${e.message}" }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick chip model
// ─────────────────────────────────────────────────────────────────────────────
private data class QuickChip(val label: String, val query: String)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ChatbotScreen(
    siteName: String,
    siteId: String,
    onBack: () -> Unit,
    videoViewModel: VideoViewModel = viewModel()
) {
    val scope     = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    val videoUiState by videoViewModel.uiState.collectAsStateWithLifecycle()

    val chips = remember(siteName) {
        listOf(
            QuickChip("📜 History",      "Tell me the history of $siteName"),
            QuickChip("🏗️ Who built it", "Who built $siteName?"),
            QuickChip("🌅 Best time",    "What is the best time to visit $siteName?"),
            QuickChip("✨ Fun facts",    "Tell me some fun facts about $siteName"),
            QuickChip("🎭 Legends",      "What are the legends and myths around $siteName?")
        )
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                text   = "👋 Welcome to $siteName!\n\nI'm your AI heritage guide. Ask me anything — history, architecture, legends, or visiting tips!",
                isUser = false,
                videoAvailable = true,
                videoType = VideoType.OVERVIEW,
                videoId = siteId
            )
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        messages.add(ChatMessage(text = userText, isUser = true))
        val historySnapshot = messages.toList()
        val loadingMsg = ChatMessage(text = "", isUser = false, isLoading = true)
        messages.add(loadingMsg)

        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val reply = callBackend(message = userText, siteName = siteName, siteId = siteId, history = historySnapshot)
            val idx = messages.indexOf(loadingMsg)
            val botMsg = ChatMessage(
                text = reply,
                isUser = false,
                videoAvailable = true,
                videoType = VideoType.PROMPT,
                videoId = "",
                userPrompt = userText
            )
            if (idx != -1) messages[idx] = botMsg else messages.add(botMsg)
            listState.animateScrollToItem(messages.size - 1)
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
                    .drawBehind {
                        drawLine(GlassBorder, Offset(0f, size.height), Offset(size.width, size.height), 0.5.dp.toPx())
                    }
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(GlassWhite15)
                            .border(0.5.dp, GlassBorder, CircleShape).clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))),
                        contentAlignment = Alignment.Center
                    ) { Text("🏛️", fontSize = 20.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(siteName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4ADE80)))
                            Spacer(Modifier.width(5.dp))
                            Text("Heritage Guide · Online", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ── Messages ──────────────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    GlassChatBubble(
                        message = msg,
                        siteName = siteName,
                        siteId = siteId,
                        videoViewModel = videoViewModel
                    )
                }
            }

            // ── Quick chips ───────────────────────────────────────────────
            val lastMsg = messages.lastOrNull()
            if (lastMsg != null && !lastMsg.isUser && !lastMsg.isLoading) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(chips) { chip ->
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassWhite15)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(50))
                                .clickable { sendMessage(chip.query) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(chip.label, color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────
            val canSend = inputText.isNotBlank()
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0x99050D1A), Color(0xF0050D1A))))
                    .drawBehind { drawLine(GlassBorder, Offset(0f, 0f), Offset(size.width, 0f), 0.5.dp.toPx()) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = inputText, onValueChange = { inputText = it },
                            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
                            modifier = Modifier.fillMaxWidth(), maxLines = 4,
                            decorationBox = { inner ->
                                if (inputText.isEmpty()) Text("Ask about $siteName…", color = TextTertiary, fontSize = 15.sp)
                                inner()
                            }
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.size(50.dp).clip(CircleShape)
                            .background(if (canSend) Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))) else Brush.linearGradient(listOf(GlassWhite15, GlassWhite15)))
                            .border(0.5.dp, if (canSend) Color(0x44FFFFFF) else GlassBorder, CircleShape)
                            .clickable(enabled = canSend) { val t = inputText.trim(); inputText = ""; sendMessage(t) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (canSend) DeepNavy else TextTertiary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // ── Cinematic Loader (shown over everything) ──────────────────────
        CinematicLoaderOverlay(
            uiState = videoUiState,
            onCancel = { videoViewModel.dismiss() },
            modifier = Modifier.fillMaxSize()
        )

        // ── Video Player (shown when video is ready) ──────────────────────
        if (videoUiState is VideoUiState.ReadyToPlay) {
            VideoPlayerOverlay(
                videoUrl = (videoUiState as VideoUiState.ReadyToPlay).videoUrl,
                onDismiss = { videoViewModel.dismiss() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat bubble — updated to show WatchVideoButton for bot messages
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlassChatBubble(
    message: ChatMessage,
    siteName: String = "",
    siteId: String = "",
    videoViewModel: VideoViewModel? = null
) {
    val isUser    = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val bubbleShape = RoundedCornerShape(
        topStart    = 20.dp, topEnd = 20.dp,
        bottomStart = if (isUser) 20.dp else 5.dp,
        bottomEnd   = if (isUser) 5.dp  else 20.dp
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(
                    if (isUser) Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                    else Brush.linearGradient(listOf(GlassWhite20, GlassWhite15))
                )
                .border(
                    width = 0.5.dp,
                    brush = if (isUser) Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color(0x11FFFFFF)))
                    else Brush.verticalGradient(listOf(GlassBorderBright, GlassBorder)),
                    shape = bubbleShape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (message.isLoading) TypingIndicator()
            else Text(message.text, color = if (isUser) DeepNavy else TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
        }

        // Show "Watch Video" button for bot messages that have video available
        if (!isUser && !message.isLoading && message.videoAvailable && videoViewModel != null) {
            Spacer(Modifier.height(6.dp))
            WatchVideoButton(
                videoType = message.videoType,
                videoId = message.videoId,
                prompt = message.userPrompt,
                botText = message.text,
                siteName = siteName,
                siteId = siteId,
                viewModel = videoViewModel,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Typing indicator (unchanged)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")

    @Composable
    fun Dot(offsetMs: Int) {
        val scale by transition.animateFloat(
            0.7f, 1.35f,
            infiniteRepeatable(tween(450, easing = EaseInOutSine), RepeatMode.Reverse, StartOffset(offsetMs)),
            label = "dot_$offsetMs"
        )
        Box(Modifier.size(7.dp).scale(scale).clip(CircleShape).background(TextTertiary))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) { Dot(0); Dot(150); Dot(300) }
}