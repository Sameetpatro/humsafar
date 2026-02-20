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
import com.example.humsafar.ui.components.AnimatedOrbBackground
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
// Data model
// ─────────────────────────────────────────────────────────────────────────────
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

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
// Network
// ─────────────────────────────────────────────────────────────────────────────
suspend fun callBackend(
    message: String,
    siteName: String,
    siteId: String,
    history: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    try {
        val baseUrl = "https://humsafar-backend-59ic.onrender.com"

        // Wake Render free-tier (may be sleeping)
        try {
            (URL("$baseUrl/").openConnection() as HttpURLConnection).apply {
                connectTimeout = 60_000
                readTimeout    = 60_000
                requestMethod  = "GET"
                responseCode   // triggers the connection
                disconnect()
            }
        } catch (_: Exception) { /* ignore — still try real request */ }

        // Build history JSON
        val historyArray = JSONArray()
        history.filter { !it.isLoading }.forEach { msg ->
            historyArray.put(
                JSONObject().apply {
                    put("role",    if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                }
            )
        }

        // POST /chat
        val conn = (URL("$baseUrl/chat").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput       = true
            connectTimeout = 30_000
            readTimeout    = 120_000
        }

        val body = JSONObject().apply {
            put("message",   message)
            put("site_name", siteName)
            put("site_id",   siteId)
            put("history",   historyArray)
        }

        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

        return@withContext if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val raw = conn.inputStream.bufferedReader().readText()
            JSONObject(raw).getString("reply")
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: "no body"
            "Server error ${conn.responseCode}: $err"
        }

    } catch (e: Exception) {
        "Connection failed: ${e.message}"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick-reply chip model
// ─────────────────────────────────────────────────────────────────────────────
private data class QuickChip(val label: String, val query: String)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ChatbotScreen(
    siteName: String,
    siteId: String,
    onBack: () -> Unit
) {
    val scope     = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

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
                isUser = false
            )
        )
    }

    // ─── Send logic ───────────────────────────────────────────────────────
    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // 1. Add user message
        messages.add(ChatMessage(text = userText, isUser = true))

        // 2. Snapshot history BEFORE adding loading bubble
        //    This is exactly what the backend will use as context
        val historySnapshot = messages.toList()

        // 3. Add loading indicator
        val loadingMsg = ChatMessage(text = "", isUser = false, isLoading = true)
        messages.add(loadingMsg)

        scope.launch {
            listState.animateScrollToItem(messages.size - 1)

            val reply = callBackend(
                message  = userText,
                siteName = siteName,
                siteId   = siteId,
                history  = historySnapshot   // full context including user's message
            )

            // 4. Swap loading bubble with the real reply
            val idx = messages.indexOf(loadingMsg)
            if (idx != -1) {
                messages[idx] = ChatMessage(text = reply, isUser = false)
            } else {
                messages.add(ChatMessage(text = reply, isUser = false))
            }

            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // ─── UI ───────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {

        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {

            // ── Top bar ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF0050D1A), Color(0xBB050D1A))
                        )
                    )
                    // ✅ Use drawBehind instead of a custom border() extension
                    //    — avoids ANY conflict with Compose's own Modifier.border()
                    .drawBehind {
                        drawLine(
                            color       = GlassBorder,
                            start       = Offset(0f, size.height),
                            end         = Offset(size.width, size.height),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GlassWhite15)
                            .border(0.5.dp, GlassBorder, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = TextPrimary,
                            modifier           = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFD54F), Color(0xFFFFC107))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏛️", fontSize = 20.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text       = siteName,
                            color      = TextPrimary,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4ADE80))
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text     = "Heritage Guide · Online",
                                color    = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // ── Messages ─────────────────────────────────────────────────
            LazyColumn(
                state               = listState,
                modifier            = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    GlassChatBubble(message = msg)
                }
            }

            // ── Quick chips ───────────────────────────────────────────────
            val lastMsg = messages.lastOrNull()
            if (lastMsg != null && !lastMsg.isUser && !lastMsg.isLoading) {
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.padding(bottom = 8.dp)
                ) {
                    items(chips) { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(GlassWhite15)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(50))
                                // ✅ chip.query is a clean, pre-built string — no regex needed
                                .clickable { sendMessage(chip.query) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text     = chip.label,
                                color    = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ── Input bar ────────────────────────────────────────────────
            val canSend = inputText.isNotBlank()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x99050D1A), Color(0xF0050D1A))
                        )
                    )
                    // ✅ drawBehind for top divider — no extension conflict
                    .drawBehind {
                        drawLine(
                            color       = GlassBorder,
                            start       = Offset(0f, 0f),
                            end         = Offset(size.width, 0f),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value         = inputText,
                            onValueChange = { inputText = it },
                            textStyle     = TextStyle(color = TextPrimary, fontSize = 15.sp),
                            modifier      = Modifier.fillMaxWidth(),
                            maxLines      = 4,
                            decorationBox = { innerTextField ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        text     = "Ask about $siteName…",
                                        color    = TextTertiary,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend)
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFFD54F), Color(0xFFFFC107))
                                    )
                                else
                                    Brush.linearGradient(
                                        listOf(GlassWhite15, GlassWhite15)
                                    )
                            )
                            .border(
                                width = 0.5.dp,
                                color = if (canSend) Color(0x44FFFFFF) else GlassBorder,
                                shape = CircleShape
                            )
                            .clickable(enabled = canSend) {
                                val text = inputText.trim()
                                inputText = ""
                                sendMessage(text)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint               = if (canSend) DeepNavy else TextTertiary,
                            modifier           = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Chat bubble
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun GlassChatBubble(message: ChatMessage) {
    val isUser    = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start

    val bubbleShape = RoundedCornerShape(
        topStart    = 20.dp,
        topEnd      = 20.dp,
        bottomStart = if (isUser) 20.dp else 5.dp,
        bottomEnd   = if (isUser) 5.dp  else 20.dp
    )

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(bubbleShape)
                .background(
                    if (isUser)
                        Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                    else
                        Brush.linearGradient(listOf(GlassWhite20, GlassWhite15))
                )
                .border(
                    width = 0.5.dp,
                    brush = if (isUser)
                        Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color(0x11FFFFFF)))
                    else
                        Brush.verticalGradient(listOf(GlassBorderBright, GlassBorder)),
                    shape = bubbleShape
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (message.isLoading) {
                TypingIndicator()
            } else {
                Text(
                    text       = message.text,
                    color      = if (isUser) DeepNavy else TextPrimary,
                    fontSize   = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Typing indicator — proper InfiniteTransition dots, no coroutine loop needed
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")

    @Composable
    fun Dot(offsetMs: Int) {
        val scale by transition.animateFloat(
            initialValue  = 0.7f,
            targetValue   = 1.35f,
            animationSpec = infiniteRepeatable(
                animation          = tween(450, easing = EaseInOutSine),
                repeatMode         = RepeatMode.Reverse,
                initialStartOffset = StartOffset(offsetMs)
            ),
            label = "dot_$offsetMs"
        )
        Box(
            Modifier
                .size(7.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(TextTertiary)
        )
    }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier              = Modifier.padding(vertical = 2.dp)
    ) {
        Dot(0)
        Dot(150)
        Dot(300)
    }
}