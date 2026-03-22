// app/src/main/java/com/example/humsafar/ui/NodeDetailScreen.kt

package com.example.humsafar.ui

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import com.example.humsafar.models.AmenityResponse
import com.example.humsafar.network.Node
import com.example.humsafar.network.NodeImage
import com.example.humsafar.network.SiteDetail
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.TripInfoButton
import com.example.humsafar.ui.theme.*
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// TTS state
// ─────────────────────────────────────────────────────────────────────────────

private enum class NodeTtsStatus { IDLE, SPEAKING, PAUSED }

// ─────────────────────────────────────────────────────────────────────────────
// NodeDetailScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NodeDetailScreen(
    nodeId:                 Int,
    siteId:                 Int,
    isKing:                 Boolean,
    onBack:                 () -> Unit,
    onNavigateToQr:         (Long) -> Unit,
    onNavigateToVoice:      (String, String) -> Unit,
    onNavigateToDirections: ((Int, String) -> Unit)? = null,
    onNavigateToReview:     ((Int, Int, String, Int, Int) -> Unit)? = null,
    onNavigateToAmenity:    (Int) -> Unit = {},          // ← NEW
    viewModel:              NodeDetailViewModel = viewModel()
) {
    val context   = LocalContext.current
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val tripState by TripManager.state.collectAsStateWithLifecycle()

    // ── TTS State ─────────────────────────────────────────────────────────────
    var ttsStatus          by remember { mutableStateOf(NodeTtsStatus.IDLE) }
    var activeSectionLabel by remember { mutableStateOf("") }

    val tts = remember { TextToSpeech(context) { } }

    DisposableEffect(Unit) {
        tts.language = Locale.US
        onDispose { tts.stop(); tts.shutdown() }
    }

    // ── TTS Helpers ───────────────────────────────────────────────────────────

    fun stopTts() {
        tts.stop()
        ttsStatus = NodeTtsStatus.IDLE
        activeSectionLabel = ""
    }

    fun togglePause() {
        when (ttsStatus) {
            NodeTtsStatus.SPEAKING -> {
                tts.stop()
                ttsStatus = NodeTtsStatus.PAUSED
            }
            NodeTtsStatus.PAUSED -> {
                ttsStatus = NodeTtsStatus.IDLE
                activeSectionLabel = ""
            }
            NodeTtsStatus.IDLE -> {}
        }
    }

    fun speakSequence(label: String, texts: List<Pair<String, String>>) {
        tts.stop()
        ttsStatus = NodeTtsStatus.SPEAKING
        activeSectionLabel = label

        val listener = object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == "final") {
                    ttsStatus = NodeTtsStatus.IDLE
                    activeSectionLabel = ""
                }
            }
            override fun onError(id: String?) {
                ttsStatus = NodeTtsStatus.IDLE
                activeSectionLabel = ""
            }
        }
        tts.setOnUtteranceProgressListener(listener)

        texts.forEachIndexed { i, (header, body) ->
            val isLast = i == texts.size - 1
            tts.speak(header, TextToSpeech.QUEUE_ADD, null, "hdr_$i")
            tts.speak(body,   TextToSpeech.QUEUE_ADD, null, if (isLast) "final" else "body_$i")
        }
    }

    fun speakSection(label: String, siteName: String, text: String?) {
        if (text.isNullOrBlank()) return
        speakSequence(label, listOf("Now hearing $label at $siteName" to text))
    }

    fun speakEntirePage(node: Node, site: SiteDetail) {
        val texts = mutableListOf<Pair<String, String>>()
        site.summary?.takeIf { it.isNotBlank() }?.let {
            texts += "Now hearing about ${site.name}" to it
        }
        site.history?.takeIf { it.isNotBlank() }?.let {
            texts += "Now hearing the history of ${site.name}" to it
        }
        node.description?.takeIf { it.isNotBlank() }?.let {
            texts += "Now hearing about ${node.name} at ${site.name}" to it
        }
        if (texts.isNotEmpty()) speakSequence("Full Page", texts)
    }

    // ── Load ──────────────────────────────────────────────────────────────────
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

                    // ── Scrollable content ────────────────────────────────────
                    Column(Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            NodeImageGallery(
                                nodeName = node.name,
                                images   = nodeImages,
                                onBack   = onBack
                            )

                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Spacer(Modifier.height(16.dp))

                                // ── "Hear This Node" top card ─────────────────
                                NodeHearPageCard(
                                    ttsStatus = ttsStatus,
                                    onSpeak   = { speakEntirePage(node, site) },
                                    onStop    = { stopTts() }
                                )
                                Spacer(Modifier.height(16.dp))

                                // ── Action grid (Watch Video, Scan QR) ────────
                                NodeActionsGrid(
                                    node    = node,
                                    siteId  = siteId,
                                    context = context,
                                    onQr    = onNavigateToQr
                                )
                                Spacer(Modifier.height(20.dp))

                                // ── About site ────────────────────────────────
                                if (!site.summary.isNullOrBlank()) {
                                    NodeInfoCard(
                                        title              = "📖 About ${site.name}",
                                        body               = site.summary!!,
                                        sectionLabel       = "About Site",
                                        ttsStatus          = ttsStatus,
                                        activeSectionLabel = activeSectionLabel,
                                        onSpeak            = { speakSection("About Site", site.name, site.summary) },
                                        onStop             = { stopTts() },
                                        onTogglePause      = { togglePause() }
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                // ── History ───────────────────────────────────
                                if (!site.history.isNullOrBlank()) {
                                    NodeInfoCard(
                                        title              = "📜 History",
                                        body               = site.history!!,
                                        sectionLabel       = "History",
                                        ttsStatus          = ttsStatus,
                                        activeSectionLabel = activeSectionLabel,
                                        onSpeak            = { speakSection("History", site.name, site.history) },
                                        onStop             = { stopTts() },
                                        onTogglePause      = { togglePause() }
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                // ── Node Description ──────────────────────────
                                if (!node.description.isNullOrBlank()) {
                                    NodeInfoCard(
                                        title              = "🗺️ About This Spot",
                                        body               = node.description!!,
                                        sectionLabel       = "About This Spot",
                                        ttsStatus          = ttsStatus,
                                        activeSectionLabel = activeSectionLabel,
                                        onSpeak            = { speakSection("About This Spot", node.name, node.description) },
                                        onStop             = { stopTts() },
                                        onTogglePause      = { togglePause() }
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                // ── Nearby Amenities ──────────────────────────
                                if (s.amenities.isNotEmpty()) {
                                    AmenitiesSection(
                                        amenities = s.amenities,
                                        onNavigateToAmenity = onNavigateToAmenity
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                // ── Trip Progress ─────────────────────────────
                                if (tripState.isTripActive && allNodes.isNotEmpty()) {
                                    TripProgressSection(
                                        nodes      = allNodes,
                                        visitedIds = tripState.visitedNodeIds,
                                        currentId  = tripState.currentNodeId
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                // Extra bottom padding so floating bar / FABs don't overlap
                                Spacer(Modifier.height(if (ttsStatus != NodeTtsStatus.IDLE) 150.dp else 100.dp))
                            }
                        }
                    }

                    // ── Floating TTS Control Bar ──────────────────────────────
                    if (ttsStatus != NodeTtsStatus.IDLE) {
                        NodeTtsControlBar(
                            ttsStatus          = ttsStatus,
                            activeSectionLabel = activeSectionLabel,
                            onTogglePause      = { togglePause() },
                            onStop             = { stopTts() },
                            modifier           = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }

                    // ── Ask FAB ───────────────────────────────────────────────
                    FloatingAskShreeButton(
                        node     = node,
                        siteId   = siteId,
                        context  = context,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(
                                end    = 16.dp,
                                bottom = if (ttsStatus != NodeTtsStatus.IDLE) 110.dp else 16.dp
                            )
                    )

                    // ── Trip Info Button ──────────────────────────────────────
                    if (tripState.isTripActive) {
                        TripInfoButton(
                            siteName          = tripState.siteName,
                            visitedCount      = tripState.visitedNodeIds.size,
                            totalCount        = allNodes.size,
                            onDirectionsClick = {
                                onNavigateToDirections?.invoke(siteId, tripState.siteName)
                            },
                            onEndTripClick    = {
                                val tripId       = tripState.tripId
                                val tripSiteId   = tripState.siteId
                                val visitedCount = tripState.visitedNodeIds.size
                                val totalCount   = allNodes.size
                                val sName        = tripState.siteName
                                viewModel.endTrip(tripState.visitedNodeIds, tripState.lastLat, tripState.lastLng)
                                onNavigateToReview?.invoke(tripId, tripSiteId, sName, visitedCount, totalCount)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .navigationBarsPadding()
                                .padding(
                                    start  = 16.dp,
                                    bottom = if (ttsStatus != NodeTtsStatus.IDLE) 110.dp else 16.dp
                                )
                        )
                    }
                }
            }

            is NodeDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        s.message,
                        color     = Color(0xFFFF6B6B),
                        fontSize  = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nearby Amenities section — shown inside NodeDetailScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmenitiesSection(
    amenities:           List<AmenityResponse>,
    onNavigateToAmenity: (Int) -> Unit
) {
    val washrooms = amenities.filter { it.type == "washroom" }
    val shops     = amenities.filter { it.type == "shop" }

    Column {
        Text(
            "🗺️ Nearby Amenities",
            color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))

        if (washrooms.isNotEmpty()) {
            Text(
                "🚻 Washrooms",
                color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            washrooms.forEach { amenity ->
                AmenityRowCard(
                    amenity = amenity,
                    onClick = { onNavigateToAmenity(amenity.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        if (shops.isNotEmpty()) {
            if (washrooms.isNotEmpty()) Spacer(Modifier.height(4.dp))
            Text(
                "🛍️ Shops",
                color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            shops.forEach { amenity ->
                AmenityRowCard(
                    amenity = amenity,
                    onClick = { onNavigateToAmenity(amenity.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AmenityRowCard(
    amenity: AmenityResponse,
    onClick: () -> Unit
) {
    val isShop    = amenity.type == "shop"
    val accent    = if (isShop) AccentYellow else Color(0xFF64B5F6)
    val bgGrad    = if (isShop)
        listOf(Color(0xFF1A1200), Color(0xFF100C00))
    else
        listOf(Color(0xFF001428), Color(0xFF000E1E))
    val borderClr = if (isShop) Color(0x44FFD54F) else Color(0x442196F3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(bgGrad))
            .border(0.7.dp, borderClr, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Type icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
                    .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isShop) "🛍️" else "🚻", fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    amenity.name,
                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
                // Distance if available, else price, else timing
                val subtitle = amenity.distanceMeters?.let { d ->
                    if (d >= 1000) "${"%.1f".format(d / 1000)} km away"
                    else "${d.toInt()} m away"
                } ?: amenity.priceInfo ?: amenity.timing ?: ""

                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = TextTertiary, fontSize = 12.sp)
                }
            }

            // Price badge
            amenity.priceInfo?.takeIf { it.isNotBlank() }?.let { price ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .border(0.5.dp, accent.copy(0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(price, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint = TextTertiary, modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// "Hear This Node" top card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodeHearPageCard(
    ttsStatus: NodeTtsStatus,
    onSpeak:   () -> Unit,
    onStop:    () -> Unit
) {
    val inf  = rememberInfiniteTransition(label = "hearNode")
    val glow by inf.animateFloat(
        0.35f, 0.65f,
        infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        "g"
    )
    val isActive = ttsStatus != NodeTtsStatus.IDLE

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A0050), Color(0xFF0D0030))))
            .border(
                1.dp,
                Color(0xFF9B30FF).copy(alpha = if (isActive) glow else 0.4f),
                RoundedCornerShape(16.dp)
            )
            .clickable { if (isActive) onStop() else onSpeak() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF9B30FF).copy(alpha = if (isActive) glow * 0.5f else 0.2f))
                    .border(1.dp, Color(0xFF9B30FF).copy(if (isActive) glow else 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isActive) "⏹️" else "🎧", fontSize = 18.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isActive) "Stop Narration" else "Hear This Node",
                    color      = TextPrimary,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isActive) "Tap to stop listening"
                    else "Listen to full node narration",
                    color    = Color(0xFF9B30FF).copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
            Icon(
                if (isActive) Icons.Default.Stop else Icons.Default.VolumeUp,
                contentDescription = null,
                tint     = Color(0xFF9B30FF).copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Floating TTS Control Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodeTtsControlBar(
    ttsStatus:          NodeTtsStatus,
    activeSectionLabel: String,
    onTogglePause:      () -> Unit,
    onStop:             () -> Unit,
    modifier:           Modifier = Modifier
) {
    val inf   = rememberInfiniteTransition(label = "nodeTts")
    val pulse by inf.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        "p"
    )

    val isSpeaking = ttsStatus == NodeTtsStatus.SPEAKING
    val isPaused   = ttsStatus == NodeTtsStatus.PAUSED

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Color(0xEE1A0050), Color(0xEE0D0030))))
            .border(
                1.dp,
                Color(0xFF9B30FF).copy(alpha = if (isSpeaking) pulse else 0.4f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Color(0xFF9B30FF).copy(
                            alpha = if (isSpeaking) pulse * 0.4f else 0.15f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint     = Color(0xFF9B30FF),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = when (ttsStatus) {
                        NodeTtsStatus.SPEAKING -> "Narrating…"
                        NodeTtsStatus.PAUSED   -> "Paused"
                        NodeTtsStatus.IDLE     -> ""
                    },
                    color      = Color(0xCCFFFFFF),
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (activeSectionLabel.isNotBlank()) {
                    Text(
                        activeSectionLabel,
                        color    = Color(0xFF9B30FF).copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x339B30FF))
                    .border(0.7.dp, Color(0xFF9B30FF).copy(0.5f), CircleShape)
                    .clickable { onTogglePause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Resume" else "Pause",
                    tint     = Color(0xFF9B30FF),
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF5252))
                    .border(0.7.dp, Color(0xFFFF5252).copy(0.5f), CircleShape)
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint     = Color(0xFFFF6B6B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info Card with inline voice controls
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodeInfoCard(
    title:              String,
    body:               String,
    sectionLabel:       String,
    ttsStatus:          NodeTtsStatus,
    activeSectionLabel: String,
    onSpeak:            () -> Unit,
    onStop:             () -> Unit,
    onTogglePause:      () -> Unit
) {
    val isThisActive = activeSectionLabel == sectionLabel && ttsStatus != NodeTtsStatus.IDLE
    val isSpeaking   = isThisActive && ttsStatus == NodeTtsStatus.SPEAKING
    val isPaused     = isThisActive && ttsStatus == NodeTtsStatus.PAUSED

    val inf  = rememberInfiniteTransition(label = "nodeCard_$sectionLabel")
    val glow by inf.animateFloat(
        0.4f, 0.9f,
        infiniteRepeatable(tween(800, easing = EaseInOutSine), RepeatMode.Reverse),
        "g"
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {
            Text(
                title,
                color      = if (isThisActive) AccentYellow else TextPrimary,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (isThisActive) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0x339B30FF))
                            .border(0.7.dp, Color(0xFF9B30FF).copy(0.5f), CircleShape)
                            .clickable { onTogglePause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume" else "Pause",
                            tint     = Color(0xFF9B30FF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FF5252))
                            .border(0.7.dp, Color(0xFFFF5252).copy(0.5f), CircleShape)
                            .clickable { onStop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint     = Color(0xFFFF6B6B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
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
                            contentDescription = "Listen to $title",
                            tint     = Color(0xFF9B30FF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isThisActive)
                        Brush.linearGradient(listOf(Color(0x221A0050), Color(0x110D0030)))
                    else
                        Brush.linearGradient(listOf(GlassWhite10, GlassWhite10))
                )
                .border(
                    if (isThisActive) 1.dp else 0.7.dp,
                    if (isThisActive) Color(0xFF9B30FF).copy(alpha = if (isSpeaking) glow else 0.4f)
                    else GlassBorder,
                    RoundedCornerShape(14.dp)
                )
                .padding(14.dp)
        ) {
            Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action grid — Watch Video + Scan QR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodeActionsGrid(
    node:    Node,
    siteId:  Int,
    context: android.content.Context,
    onQr:    (Long) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NodeActionCard(
            icon           = "🎬",
            title          = "Watch Video",
            subtitle       = "Visual tour",
            gradientColors = listOf(Color(0xFF002A4D), Color(0xFF001830)),
            borderColor    = Color(0xFF2196F3),
            modifier       = Modifier.weight(1f),
            onClick        = {
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
        NodeActionCard(
            icon           = "📷",
            title          = "Scan Next QR",
            subtitle       = "Continue journey",
            gradientColors = listOf(Color(0xFF0D2825), Color(0xFF091F1E)),
            borderColor    = Color(0xFF2DD4BF),
            modifier       = Modifier.weight(1f),
            onClick        = { onQr(siteId.toLong()) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Node image gallery
// ─────────────────────────────────────────────────────────────────────────────

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

    Box(Modifier.fillMaxWidth().height(300.dp)) {
        if (images.isNotEmpty()) {
            LazyRow(
                state             = listState,
                modifier          = Modifier.fillMaxSize(),
                userScrollEnabled = true
            ) {
                itemsIndexed(images) { _, img ->
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(img.imageUrl).crossfade(400).build(),
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillParentMaxWidth().fillMaxHeight()
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
                Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xBB000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${currentIndex + 1} / ${images.size}",
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }

            if (images.size > 1) {
                Row(
                    modifier              = Modifier.align(Alignment.BottomCenter).padding(bottom = 46.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    images.forEachIndexed { i, _ ->
                        Box(
                            Modifier
                                .size(if (i == currentIndex) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == currentIndex) AccentYellow else Color(0x66FFFFFF)
                                )
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
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)
                .size(42.dp).clip(CircleShape).background(Color(0xBB000000))
                .border(0.5.dp, GlassBorder, CircleShape).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, null,
                tint = Color.White, modifier = Modifier.size(20.dp)
            )
        }

        Column(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(nodeName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }

    // Thumbnail strip
    if (images.size > 1) {
        LazyRow(
            modifier              = Modifier
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
                    modifier           = Modifier
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

// ─────────────────────────────────────────────────────────────────────────────
// WatchVideoBar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WatchVideoBar(
    videoUrl: String,
    label:    String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inf     = rememberInfiniteTransition(label = "vb")
    val pulse   by inf.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), "vp")
    val glow    by inf.animateFloat(0.5f,  1f,    infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse), "vg")

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
                .background(Brush.linearGradient(listOf(Color(0xFF120038), Color(0xFF06001E), Color(0xFF1A0050))))
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(
                        Color(0xFF9B30FF).copy(alpha = glow),
                        Color(0xFF5B5FFF).copy(alpha = glow * 0.6f),
                        Color(0xFF9B30FF).copy(alpha = 0.2f)
                    )),
                    RoundedCornerShape(18.dp)
                )
                .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))) }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).scale(pulse).clip(CircleShape)
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

// ─────────────────────────────────────────────────────────────────────────────
// Small reusable cards / helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodeActionCard(
    icon:           String,
    title:          String,
    subtitle:       String,
    gradientColors: List<Color>,
    borderColor:    Color,
    modifier:       Modifier = Modifier,
    onClick:        () -> Unit
) {
    val inf      = rememberInfiniteTransition(label = "ac")
    val glowAlpha by inf.animateFloat(
        0.3f, 0.6f,
        infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        "glow"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, borderColor.copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(icon,     fontSize = 26.sp)
            Text(title,    color = TextPrimary,  fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FloatingAskShreeButton(
    node:     Node,
    siteId:   Int,
    context:  android.content.Context,
    modifier: Modifier = Modifier
) {
    val inf       = rememberInfiniteTransition(label = "ask")
    val pulse     by inf.animateFloat(1f,   1.05f, infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), "ps")
    val glowAlpha by inf.animateFloat(0.4f, 0.8f,  infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse), "ga")

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .scale(pulse).size(68.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(AccentYellow.copy(alpha = glowAlpha * 0.4f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .size(60.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))))
                .border(2.dp, Brush.linearGradient(listOf(Color(0x66FFFFFF), Color(0x22FFFFFF))), CircleShape)
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
                Icon(Icons.AutoMirrored.Filled.Chat, null, tint = DeepNavy, modifier = Modifier.size(22.dp))
                Text("Ask", color = DeepNavy, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TripProgressSection(
    nodes:      List<Node>,
    visitedIds: List<Int>,
    currentId:  Int
) {
    Column {
        Text("🗺️ Trip Progress", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        nodes.sortedBy { it.sequenceOrder }.forEach { n ->
            val visited   = n.id in visitedIds
            val isCurrent = n.id == currentId
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(
                        when {
                            isCurrent -> AccentYellow
                            visited   -> Color(0xFF4ADE80)
                            else      -> GlassWhite15
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (visited || isCurrent) "✓" else "${n.sequenceOrder}",
                        color      = if (visited || isCurrent) Color.Black else TextTertiary,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    n.name,
                    color    = when { isCurrent -> AccentYellow; visited -> TextTertiary; else -> TextPrimary },
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ShimmerBox() {
    val inf = rememberInfiniteTransition(label = "sh")
    val x   by inf.animateFloat(-1f, 2f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), "sx")
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
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF050D1A)))),
        contentAlignment = Alignment.Center
    ) {
        Text("🏛️", fontSize = 64.sp, modifier = Modifier.alpha(0.2f))
    }
}