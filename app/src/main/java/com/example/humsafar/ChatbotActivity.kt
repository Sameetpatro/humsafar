// app/src/main/java/com/example/humsafar/ChatbotActivity.kt
// FIXED:
//   1. Added Log.e to expose the actual site_id received — makes debugging trivial
//   2. Default fallback changed from "1" to "-1" so silent mis-launches are obvious
//   3. siteId/nodeId passed explicitly through the call chain so nothing is lost

package com.example.humsafar

import android.os.Bundle
import android.util.Log
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
import com.example.humsafar.models.ChatHistoryItem
import com.example.humsafar.models.ChatMessage
import com.example.humsafar.models.ChatRequest
import com.example.humsafar.network.HumsafarClient
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Constants — use these same keys when calling startActivity() elsewhere
// ─────────────────────────────────────────────────────────────────────────────

object ChatbotExtras {
    const val SITE_ID   = "SITE_ID"
    const val SITE_NAME = "SITE_NAME"
    const val NODE_ID   = "NODE_ID"
}

// ─────────────────────────────────────────────────────────────────────────────
// Activity
// ─────────────────────────────────────────────────────────────────────────────

class ChatbotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val siteName = intent.getStringExtra(ChatbotExtras.SITE_NAME) ?: "Heritage Site"
        // FIXED: default is "-1" not "1" — so a missing extra is immediately obvious in logs
        val siteId   = intent.getStringExtra(ChatbotExtras.SITE_ID)   ?: "-1"
        val nodeId   = intent.getStringExtra(ChatbotExtras.NODE_ID)

        // ✅ This log will tell you exactly what ID arrived — check Logcat for "ChatbotActivity"
        Log.e("ChatbotActivity", "▶ LAUNCHED — siteName='$siteName'  siteId='$siteId'  nodeId='$nodeId'")

        if (siteId == "-1") {
            Log.e("ChatbotActivity", "⚠️ SITE_ID was not passed in the Intent! " +
                    "Use intent.putExtra(ChatbotExtras.SITE_ID, site.id.toString()) when launching.")
        }

        setContent {
            HumsafarTheme {
                ChatbotScreen(
                    siteName = siteName,
                    siteId   = siteId,
                    nodeId   = nodeId,
                    onBack   = { finish() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Network helper
// ─────────────────────────────────────────────────────────────────────────────

suspend fun callChatApi(
    message : String,
    siteId  : String,
    nodeId  : String?,
    history : List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    try {
        val siteIdInt = siteId.toIntOrNull()
        if (siteIdInt == null || siteIdInt == -1) {
            Log.e("ChatbotActivity", "⚠️ callChatApi: invalid siteId='$siteId' — request will fail or use wrong site")
        }

        val nodeIdInt = nodeId?.toIntOrNull()

        // ✅ Log the actual values going to the server — check these in Logcat
        Log.d("ChatbotActivity", "→ POST /chat/  site_id=$siteIdInt  node_id=$nodeIdInt  msg='${message.take(60)}'")

        val historyItems = history
            .filter { !it.isLoading }
            .map { msg ->
                ChatHistoryItem(
                    role    = if (msg.isUser) "user" else "assistant",
                    content = msg.text
                )
            }

        val request = ChatRequest(
            siteId  = siteIdInt ?: -1,
            nodeId  = nodeIdInt,
            message = message,
            history = historyItems
        )

        val resp = HumsafarClient.api.sendChat(request)
        Log.d("ChatbotActivity", "← /chat/ responded ${resp.code()}")

        if (resp.isSuccessful && resp.body() != null) {
            resp.body()!!.reply
        } else {
            "Server error ${resp.code()} — please try again"
        }
    } catch (e: Exception) {
        Log.e("ChatbotActivity", "callChatApi exception: ${e.message}", e)
        "Connection failed: ${e.message}"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

private data class QuickChip(val label: String, val query: String)

@Composable
fun ChatbotScreen(
    siteName : String,
    siteId   : String,
    nodeId   : String?,
    onBack   : () -> Unit
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

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        messages.add(ChatMessage(text = userText, isUser = true))
        val historySnapshot = messages.toList()
        val loadingMsg      = ChatMessage(text = "", isUser = false, isLoading = true)
        messages.add(loadingMsg)

        scope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val reply = callChatApi(
                message = userText,
                siteId  = siteId,
                nodeId  = nodeId,
                history = historySnapshot
            )
            val botMsg = ChatMessage(text = reply, isUser = false)
            val idx = messages.indexOf(loadingMsg)
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
                    .background(
                        Brush.verticalGradient(listOf(Color(0xF0050D1A), Color(0xBB050D1A)))
                    )
                    .drawBehind {
                        drawLine(GlassBorder, Offset(0f, size.height), Offset(size.width, size.height), 0.5.dp.toPx())
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
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint     = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
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
                                Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4ADE80))
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

            // ── Messages ──────────────────────────────────────────────────
            LazyColumn(
                state               = listState,
                modifier            = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg: ChatMessage ->
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
                    items(chips) { chip: QuickChip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(GlassWhite15)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0x99050D1A), Color(0xF0050D1A)))
                    )
                    .drawBehind {
                        drawLine(GlassBorder, Offset(0f, 0f), Offset(size.width, 0f), 0.5.dp.toPx())
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
                            decorationBox = { inner ->
                                if (inputText.isEmpty()) {
                                    Text(
                                        text     = "Ask about $siteName…",
                                        color    = TextTertiary,
                                        fontSize = 15.sp
                                    )
                                }
                                inner()
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
                                    Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                                else
                                    Brush.linearGradient(listOf(GlassWhite15, GlassWhite15))
                            )
                            .border(
                                0.5.dp,
                                if (canSend) Color(0x44FFFFFF) else GlassBorder,
                                CircleShape
                            )
                            .clickable(enabled = canSend) {
                                val t = inputText.trim()
                                inputText = ""
                                sendMessage(t)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint     = if (canSend) DeepNavy else TextTertiary,
                            modifier = Modifier.size(20.dp)
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
    val isUser      = message.isUser
    val alignment   = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = RoundedCornerShape(
        topStart    = 20.dp,
        topEnd      = 20.dp,
        bottomStart = if (isUser) 20.dp else 5.dp,
        bottomEnd   = if (isUser) 5.dp  else 20.dp
    )

    Column(
        modifier             = Modifier.fillMaxWidth(),
        horizontalAlignment  = alignment
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
                    0.5.dp,
                    if (isUser)
                        Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color(0x11FFFFFF)))
                    else
                        Brush.verticalGradient(listOf(GlassBorderBright, GlassBorder)),
                    bubbleShape
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
// Typing indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")

    @Composable
    fun Dot(offsetMs: Int) {
        val scale by transition.animateFloat(
            initialValue   = 0.7f,
            targetValue    = 1.35f,
            animationSpec  = infiniteRepeatable(
                animation  = tween(450, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse,
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