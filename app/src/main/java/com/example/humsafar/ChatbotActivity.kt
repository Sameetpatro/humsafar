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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(
    siteName: String,
    siteId: String,
    onBack: () -> Unit
) {
    val NavyBlue    = Color(0xFF0A1F44)
    val AccentYellow = Color(0xFFFFD54F)

    val scope         = rememberCoroutineScope()
    val listState     = rememberLazyListState()
    var inputText     by remember { mutableStateOf("") }
    val messages      = remember {
        mutableStateListOf(
            ChatMessage(
                text = "👋 Welcome to **$siteName**! I'm your AI heritage guide.\n\nAsk me anything — history, architecture, legends, visiting tips, or fun facts!",
                isUser = false
            )
        )
    }

    val quickQuestions = listOf(
        "Tell me the history",
        "Who built it?",
        "Best time to visit",
        "Fun facts"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = siteName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Heritage Guide",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Chat messages ──
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg, accentYellow = AccentYellow, navyBlue = NavyBlue)
                }
            }

            // ── Quick suggestion chips (only show if last message is from bot & no loading) ──
            if (messages.lastOrNull()?.isUser == false && messages.lastOrNull()?.isLoading == false) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickQuestions.take(4).forEach { suggestion ->
                        SuggestionChip(
                            onClick = {
                                sendMessage(
                                    userText  = suggestion,
                                    siteName  = siteName,
                                    messages  = messages,
                                    scope     = scope,
                                    listState = listState
                                )
                            },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                            sendMessage(
                                userText  = text,
                                siteName  = siteName,
                                messages  = messages,
                                scope     = scope,
                                listState = listState
                            )
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(NavyBlue)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = AccentYellow
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    accentYellow: Color,
    navyBlue: Color
) {
    val alignment  = if (message.isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isUser) navyBlue else Color.White
    val textColor   = if (message.isUser) Color.White else Color(0xFF1A1A1A)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd   = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (message.isLoading) {
                // Animated typing indicator
                var dots by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    while (true) {
                        dots = "."; delay(400)
                        dots = ".."; delay(400)
                        dots = "..."; delay(400)
                    }
                }
                Text("Thinking$dots", color = Color.Gray, fontSize = 14.sp)
            } else {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun sendMessage(
    userText: String,
    siteName: String,
    messages: MutableList<ChatMessage>,
    scope: kotlinx.coroutines.CoroutineScope,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    // Add user message
    messages.add(ChatMessage(text = userText, isUser = true))

    // Add loading bubble
    val loadingMsg = ChatMessage(text = "", isUser = false, isLoading = true)
    messages.add(loadingMsg)

    scope.launch {
        listState.animateScrollToItem(messages.size - 1)

        // Simulate network delay (replace this with real API call)
        delay(1200)

        // Remove loading bubble
        messages.remove(loadingMsg)

        // Generate mock response (swap this with your actual AI call)
        val reply = generateMockReply(userText, siteName)
        messages.add(ChatMessage(text = reply, isUser = false))

        listState.animateScrollToItem(messages.size - 1)
    }
}

private fun generateMockReply(question: String, siteName: String): String {
    val q = question.lowercase()
    return when {
        q.contains("history") || q.contains("tell me") ->
            "$siteName has a rich history spanning several centuries. It has witnessed empires rise and fall, and remains one of India's most treasured cultural landmarks.\n\n(Connect your AI backend to get the full story!)"

        q.contains("built") || q.contains("who") ->
            "$siteName was built by rulers who wanted to leave a lasting legacy. The construction involved thousands of artisans and took many years to complete.\n\n(Plug in your AI for the real details!)"

        q.contains("visit") || q.contains("time") || q.contains("best") ->
            "The best time to visit $siteName is from October to March when the weather is pleasant. Early mornings offer great light and fewer crowds."

        q.contains("fact") || q.contains("fun") ->
            "Fun fact: $siteName attracts millions of visitors every year and has appeared on numerous UNESCO heritage lists. Its architectural details hide many secrets waiting to be discovered!"

        q.contains("ticket") || q.contains("entry") || q.contains("fee") ->
            "️Entry fees and timings for $siteName vary. It's best to check the ASI (Archaeological Survey of India) website or local tourism boards for the latest information."

        else ->
            "That's a great question about $siteName! I'd love to give you a detailed answer — connect me to your AI backend and I'll have all the answers ready for you. 😊"
    }
}