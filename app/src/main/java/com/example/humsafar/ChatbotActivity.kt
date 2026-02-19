package com.example.humsafar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.ui.theme.HumsafarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

class ChatbotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val siteName = intent.getStringExtra("SITE_NAME") ?: "Heritage Site"
        val siteId   = intent.getStringExtra("SITE_ID")   ?: ""

        setContent {
            HumsafarTheme {
                ChatbotScreen(
                    siteName = siteName,
                    siteId   = siteId,
                    onBack   = { finish() }
                )
            }
        }
    }
}

// ── Real API call to your Render backend ──
suspend fun callBackend(
    message: String,
    siteName: String,
    siteId: String,
    history: List<ChatMessage>
): String = withContext(Dispatchers.IO) {
    try {
        val baseUrl = "https://humsafar-backend-59ic.onrender.com"

        // Step 1: Wake up the server (Render free tier spins down)
        try {
            val wakeUrl = URL("$baseUrl/")
            val wakeConn = wakeUrl.openConnection() as HttpURLConnection
            wakeConn.connectTimeout = 60000  // wait up to 60s for wake-up
            wakeConn.readTimeout = 60000
            wakeConn.requestMethod = "GET"
            wakeConn.responseCode  // actually triggers the connection
            wakeConn.disconnect()
        } catch (e: Exception) {
            // ignore wake-up errors, still try the main request
        }

        // Step 2: Send the actual chat request
        val url = URL("$baseUrl/chat")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 120000

        val historyArray = JSONArray()
        history.filter { !it.isLoading }.forEach { msg ->
            val obj = JSONObject()
            obj.put("role", if (msg.isUser) "user" else "assistant")
            obj.put("content", msg.text)
            historyArray.put(obj)
        }

        val body = JSONObject()
        body.put("message", message)
        body.put("site_name", siteName)
        body.put("site_id", siteId)
        body.put("history", historyArray)

        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(body.toString())
        writer.flush()
        writer.close()

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            json.getString("reply")
        } else {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            "Server error $responseCode: $error"
        }

    } catch (e: Exception) {
        "Connection failed: ${e.message}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    siteName: String,
    siteId: String,
    onBack: () -> Unit
) {
    val NavyBlue     = Color(0xFF0A1F44)
    val AccentYellow = Color(0xFFFFD54F)

    val scope     = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val messages  = remember {
        mutableStateListOf(
            ChatMessage(
                text = "👋 Welcome to $siteName! I'm your AI heritage guide.\n\nAsk me anything — history, architecture, legends, visiting tips, or fun facts!",
                isUser = false
            )
        )
    }

    val quickQuestions = listOf("Tell me the history", "Who built it?", "Best time to visit", "Fun facts")

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        messages.add(ChatMessage(text = userText, isUser = true))
        val loadingMsg = ChatMessage(text = "", isUser = false, isLoading = true)
        messages.add(loadingMsg)

        scope.launch {
            listState.animateScrollToItem(messages.size - 1)

            // Snapshot history BEFORE adding loading bubble (exclude the loading msg itself)
            val historySnapshot = messages.filter { it != loadingMsg && !it.isLoading }

            val reply = callBackend(
                message  = userText,
                siteName = siteName,
                siteId   = siteId,
                history  = historySnapshot.dropLast(1) // drop the user msg we just added, backend adds it
            )

            messages.remove(loadingMsg)
            messages.add(ChatMessage(text = reply, isUser = false))
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = siteName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text(text = "Heritage Guide", fontSize = 12.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg, accentYellow = AccentYellow, navyBlue = NavyBlue)
                }
            }

            // Quick suggestion chips
            if (messages.lastOrNull()?.isUser == false && messages.lastOrNull()?.isLoading == false) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickQuestions.take(4).forEach { suggestion ->
                        SuggestionChip(
                            onClick = { sendMessage(suggestion) },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about $siteName…", fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            inputText = ""
                            sendMessage(text)
                        }
                    },
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(50)).background(NavyBlue)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = AccentYellow)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, accentYellow: Color, navyBlue: Color) {
    val alignment   = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isUser) navyBlue else Color.White
    val textColor   = if (message.isUser) Color.White else Color(0xFF1A1A1A)

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (message.isUser) 16.dp else 4.dp,
                    bottomEnd   = if (message.isUser) 4.dp else 16.dp
                ))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (message.isLoading) {
                var dots by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    while (true) { dots = "."; delay(400); dots = ".."; delay(400); dots = "..."; delay(400) }
                }
                Text("Thinking$dots", color = Color.Gray, fontSize = 14.sp)
            } else {
                Text(text = message.text, color = textColor, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}