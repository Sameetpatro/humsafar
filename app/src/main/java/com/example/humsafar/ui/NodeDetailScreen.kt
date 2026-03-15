// app/src/main/java/com/example/humsafar/ui/NodeDetailScreen.kt
// UPDATED: Real image gallery from node_images table (sorted by display_order)
//          + "Watch Video" sticky bar when node.video_url is present
//          + TTS narration for node description and "Hear This Node" button

package com.example.humsafar.ui

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.humsafar.ChatbotActivity
import com.example.humsafar.data.TripManager
import com.example.humsafar.network.Node
import com.example.humsafar.network.NodeImage
import com.example.humsafar.network.SiteDetail
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.TripInfoButton
import com.example.humsafar.ui.theme.*
import java.util.Locale

@Composable
fun NodeDetailScreen(
    nodeId:            Int,
    siteId:            Int,
    isKing:            Boolean,
    onBack:            () -> Unit,
    onNavigateToQr:    (Long) -> Unit,
    onNavigateToVoice: (String, String) -> Unit,
    onNavigateToDirections: ((Int, String) -> Unit)? = null,
    onNavigateToReview: ((Int, Int, String, Int, Int) -> Unit)? = null,
    viewModel:         NodeDetailViewModel = viewModel()
) {
    val context   = LocalContext.current
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val tripState by TripManager.state.collectAsStateWithLifecycle()

    // ── TTS Setup ─────────────────────────────────────────────────────────────
    val tts = remember {
        TextToSpeech(context) { /* status handled silently */ }
    }

    DisposableEffect(Unit) {
        tts.language = Locale.US
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // ── TTS Helper Functions ───────────────────────────────────────────────────
    fun speakNodeSection(nodeName: String, siteName: String, description: String?) {
        if (description.isNullOrBlank()) return
        tts.stop()
        tts.speak(
            "Now you are hearing about $nodeName at $siteName",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "hdr_node"
        )
        tts.speak(description, TextToSpeech.QUEUE_ADD, null, "body_node")
    }

    fun speakEntireNodePage(node: Node, site: SiteDetail) {
        tts.stop()
        var firstQueued = false

        val aboutSite = site.summary
        val history   = site.history
        val nodeDesc  = node.description

        if (!aboutSite.isNullOrBlank()) {
            tts.speak(
                "Now you are hearing about ${site.name}",
                TextToSpeech.QUEUE_FLUSH, null, "hdr_about"
            )
            tts.speak(aboutSite, TextToSpeech.QUEUE_ADD, null, "body_about")
            firstQueued = true
        }
        if (!history.isNullOrBlank()) {
            val mode = if (!firstQueued) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak("Now you are hearing the history of ${site.name}", mode, null, "hdr_hist")
            tts.speak(history, TextToSpeech.QUEUE_ADD, null, "body_hist")
            firstQueued = true
        }
        if (!nodeDesc.isNullOrBlank()) {
            val mode = if (!firstQueued) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak("Now you are hearing about ${node.name} at ${site.name}", mode, null, "hdr_node")
            tts.speak(nodeDesc, TextToSpeech.QUEUE_ADD, null, "body_node")
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

    LaunchedEffect(nodeId, siteId) { viewModel.loadNode(nodeId, siteId) }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        when (val s = uiState) {
            is NodeDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏛️", fontSize = 52.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading…", color = TextPrimary, fontSize = 16.sp)
                    }
                }
            }

            is NodeDetailUiState.Ready -> {
                val node     = s.node
                val site     = s.site
                val allNodes = s.allNodes

                val nodeImages = remember(node.images, node.imageUrl) {
                    if (node.images.isNotEmpty()) {
                        node.images.sortedBy { it.displayOrder }
                    } else if (!node.imageUrl.isNullOrBlank()) {
                        listOf(NodeImage(id = 0, imageUrl = node.imageUrl!!, displayOrder = 0))
                    } else {
                        emptyList()
                    }
                }

                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            NodeImageGallery(
                                nodeName   = node.name,
                                images     = nodeImages,
                                onBack     = onBack
                            )

                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Spacer(Modifier.height(16.dp))

                                NodeActionsGrid(
                                    node = node,
                                    siteId = siteId,
                                    context = context,
                                    onQr = onNavigateToQr,
                                    onHearThisNode = { speakEntireNodePage(node, site) }
                                )

                                Spacer(Modifier.height(20.dp))

                                if (!site.summary.isNullOrBlank()) {
                                    InfoCard("📖 About ${site.name}", site.summary!!)
                                    Spacer(Modifier.height(14.dp))
                                }
                                if (!site.history.isNullOrBlank()) {
                                    InfoCard("📜 History", site.history!!)
                                    Spacer(Modifier.height(14.dp))
                                }
                                if (!node.description.isNullOrBlank()) {
                                    // ── Node description WITH speaker icon ──
                                    NodeDescriptionCard(
                                        nodeName    = node.name,
                                        siteName    = site.name,
                                        description = node.description!!,
                                        onSpeak     = { speakNodeSection(node.name, site.name, node.description) }
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                if (tripState.isTripActive && allNodes.isNotEmpty()) {
                                    TripProgressSection(
                                        nodes      = allNodes,
                                        visitedIds = tripState.visitedNodeIds,
                                        currentId  = tripState.currentNodeId
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                Spacer(Modifier.height(16.dp))
                                NodeActionCard(
                                    icon = "📷",
                                    title = "Scan Next QR",
                                    subtitle = "Continue your journey to the next node",
                                    gradientColors = listOf(Color(0xFF0D2825), Color(0xFF091F1E)),
                                    borderColor = Color(0xFF2DD4BF),
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onNavigateToQr(siteId.toLong()) }
                                )

                                Spacer(Modifier.height(100.dp))
                            }
                        }
                    }

                    FloatingAskShreeButton(
                        node = node,
                        siteId = siteId,
                        context = context,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 16.dp, bottom = 16.dp)
                    )

                    if (tripState.isTripActive) {
                        TripInfoButton(
                            siteName = tripState.siteName,
                            visitedCount = tripState.visitedNodeIds.size,
                            totalCount = allNodes.size,
                            onDirectionsClick = {
                                onNavigateToDirections?.invoke(siteId, tripState.siteName)
                            },
                            onEndTripClick = {
                                val tripId = tripState.tripId
                                val tripSiteId = tripState.siteId
                                val visitedCount = tripState.visitedNodeIds.size
                                val totalCount = allNodes.size
                                val siteName = tripState.siteName
                                viewModel.endTrip(tripState.visitedNodeIds, tripState.lastLat, tripState.lastLng)
                                onNavigateToReview?.invoke(tripId, tripSiteId, siteName, visitedCount, totalCount)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .navigationBarsPadding()
                                .padding(start = 16.dp, bottom = 16.dp)
                        )
                    }
                }
            }

            is NodeDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = Color(0xFFFF6B6B), fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp))
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Shared gallery composable (used in both screens)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun NodeImageGallery(
    nodeName: String,
    images:   List<NodeImage>,
    onBack:   () -> Unit
) {
    val listState    = rememberLazyListState()
    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        currentIndex = listState.firstVisibleItemIndex
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        if (images.isNotEmpty()) {
            LazyRow(
                state    = listState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) {
                itemsIndexed(images) { _, img ->
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(img.imageUrl)
                            .crossfade(400)
                            .build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .fillParentMaxWidth()
                            .fillMaxHeight()
                    ) {
                        when (painter.state) {
                            is AsyncImagePainter.State.Loading -> ShimmerBox()
                            is AsyncImagePainter.State.Error   -> EmptyImageBox()
                            else                               -> SubcomposeAsyncImageContent()
                        }
                    }
                }
            }

            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xBB000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("${currentIndex + 1} / ${images.size}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 46.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    images.forEachIndexed { i, _ ->
                        Box(
                            Modifier
                                .size(if (i == currentIndex) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (i == currentIndex) AccentYellow else Color(0x66FFFFFF))
                        )
                    }
                }
            }
        } else {
            EmptyImageBox()
        }

        Box(
            Modifier.fillMaxWidth().height(100.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xCC050D1A), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF050D1A))))
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xBB000000))
                .border(0.5.dp, GlassBorder, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(nodeName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }

    if (images.size > 1) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF050D1A))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(images) { i, img ->
                val selected = i == currentIndex
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(img.imageUrl).crossfade(300).build(),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp, 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            if (selected) 2.dp else 0.dp,
                            if (selected) AccentYellow else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .alpha(if (selected) 1f else 0.5f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Shared "Watch Video" sticky bar (used in both screens)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
fun WatchVideoBar(
    videoUrl: String,
    label:    String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inf = rememberInfiniteTransition(label = "vb")
    val pulse by inf.animateFloat(
        0.94f, 1.06f,
        infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "vp"
    )
    val glow by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "vg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF5050D1A))))
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF120038), Color(0xFF06001E), Color(0xFF1A0050)))
                )
                .border(
                    1.5.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF9B30FF).copy(alpha = glow),
                            Color(0xFF5B5FFF).copy(alpha = glow * 0.6f),
                            Color(0xFF9B30FF).copy(alpha = 0.2f)
                        )
                    ),
                    RoundedCornerShape(18.dp)
                )
                .clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .scale(pulse)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF9B30FF), Color(0xFF5B5FFF)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Tap to open video", color = Color(0xFF9B87CC), fontSize = 11.sp)
                }
                Text("▶", color = Color(0xFF9B30FF).copy(alpha = glow), fontSize = 18.sp)
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Small helpers
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoCard(title: String, body: String) {
    Column {
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassWhite10)
                .border(0.7.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

// ── Node Description Card WITH speaker icon ───────────────────────────────────
@Composable
private fun NodeDescriptionCard(
    nodeName:    String,
    siteName:    String,
    description: String,
    onSpeak:     () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "🗺️ About This Spot",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x221A0050))
                    .border(0.7.dp, Color(0xFF9B30FF).copy(alpha = 0.4f), CircleShape)
                    .clickable { onSpeak() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Listen to description of $nodeName",
                    tint = Color(0xFF9B30FF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(GlassWhite10)
                .border(0.7.dp, GlassBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(description, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun NodeActionsGrid(
    node:           Node,
    siteId:         Int,
    context:        android.content.Context,
    onQr:           (Long) -> Unit,
    onHearThisNode: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Hear This Node button — NOW FUNCTIONAL ──────────────────────
            NodeActionCard(
                icon = "🎧",
                title = "Hear This Node",
                subtitle = "Listen to node narration",
                gradientColors = listOf(Color(0xFF1A0050), Color(0xFF0D0030)),
                borderColor = Color(0xFF9B30FF),
                modifier = Modifier.weight(1f),
                onClick = { onHearThisNode() }
            )

            NodeActionCard(
                icon = "🎬",
                title = "Watch Video",
                subtitle = "Visual tour",
                gradientColors = listOf(Color(0xFF002A4D), Color(0xFF001830)),
                borderColor = Color(0xFF2196F3),
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!node.videoUrl.isNullOrBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(node.videoUrl)))
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "No video available for this node",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        NodeActionCard(
            icon = "📷",
            title = "Scan Next QR",
            subtitle = "Continue your journey to the next node",
            gradientColors = listOf(Color(0xFF0D2825), Color(0xFF091F1E)),
            borderColor = Color(0xFF2DD4BF),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onQr(siteId.toLong()) }
        )
    }
}

@Composable
private fun NodeActionCard(
    icon: String,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "action_card")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(
                1.dp,
                borderColor.copy(alpha = glowAlpha),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = borderColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FloatingAskShreeButton(
    node: Node,
    siteId: Int,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ask_shree")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .scale(pulseScale)
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            AccentYellow.copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                )
                .border(
                    2.dp,
                    Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x22FFFFFF))),
                    CircleShape
                )
                .clickable {
                    context.startActivity(
                        Intent(context, ChatbotActivity::class.java).apply {
                            putExtra("SITE_NAME", node.name)
                            putExtra("SITE_ID", siteId.toString())
                            putExtra("NODE_ID", node.id.toString())
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    null,
                    tint = DeepNavy,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "Ask",
                    color = DeepNavy,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TripProgressSection(nodes: List<Node>, visitedIds: List<Int>, currentId: Int) {
    Column {
        Text("🗺️ Trip Progress", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        nodes.sortedBy { it.sequenceOrder }.forEach { n ->
            val visited   = n.id in visitedIds
            val isCurrent = n.id == currentId
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(
                        when { isCurrent -> AccentYellow; visited -> Color(0xFF4ADE80); else -> GlassWhite15 }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (visited || isCurrent) "✓" else "${n.sequenceOrder}",
                        color = if (visited || isCurrent) Color.Black else TextTertiary,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    n.name,
                    color = when { isCurrent -> AccentYellow; visited -> TextTertiary; else -> TextPrimary },
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ShimmerBox() {
    val inf = rememberInfiniteTransition(label = "sh")
    val x by inf.animateFloat(
        -1f, 2f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "sx"
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color(0xFF0D1F3C), Color(0xFF1E3050), Color(0xFF0D1F3C)),
                start = androidx.compose.ui.geometry.Offset(x * 800f, 0f),
                end   = androidx.compose.ui.geometry.Offset((x + 1f) * 800f, 0f)
            )
        )
    )
}

@Composable
private fun EmptyImageBox() {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF050D1A)))),
        contentAlignment = Alignment.Center
    ) {
        Text("🏛️", fontSize = 64.sp, modifier = Modifier.alpha(0.2f))
    }
}