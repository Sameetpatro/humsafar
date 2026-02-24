package com.example.humsafar.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.humsafar.ChatbotActivity
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.*
import com.example.humsafar.navigation.qrScanRoute
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*

@Composable
fun NodeDetailScreen(
    nodeId: Long,
    isKing: Boolean,
    onBack: () -> Unit,
    onNavigateToQr: (Long) -> Unit,      // monumentId
    onNavigateToVoice: (String, String) -> Unit,
    viewModel: NodeDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tripState by TripManager.state.collectAsStateWithLifecycle()
    val videoUiState by viewModel.videoViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(nodeId) { viewModel.loadNode(nodeId) }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        when (val s = uiState) {
            is NodeDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏛️", fontSize = 52.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading node…", color = TextPrimary, fontSize = 16.sp)
                    }
                }
            }

            is NodeDetailUiState.Ready -> {
                val node    = s.node
                val nearby  = s.nearbyPlaces
                val allNodes = s.allNodes

                Column(Modifier.fillMaxSize()) {

                    // ── Scrollable body ───────────────────────────────────
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState())
                    ) {

                        // ── Hero image carousel ───────────────────────────
                        HeroCarousel(
                            images   = node.photoUrls,
                            nodeName = node.name,
                            nodeType = node.nodeType,
                            onBack   = onBack
                        )

                        Column(Modifier.padding(horizontal = 20.dp)) {

                            Spacer(Modifier.height(16.dp))

                            // ── Watch video button ────────────────────────
                            if (node.videoUrl.isNotBlank()) {
                                Box(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF1A002D), Color(0xFF0E001A))))
                                        .border(1.dp, Color(0x554A90D9), RoundedCornerShape(16.dp))
                                        .clickable {
                                            viewModel.videoViewModel.playDirectUrl(node.videoUrl)
                                        }
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayCircle, null, tint = Color(0xFF4A90D9), modifier = Modifier.size(28.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("Watch Video Instead", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("Full cinematic tour", color = TextTertiary, fontSize = 12.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            // ── Action row ────────────────────────────────
                            NodeActionRow(
                                node              = node,
                                tripState         = tripState,
                                context           = context,
                                onNavigateToVoice = onNavigateToVoice,
                                onScanNext        = { onNavigateToQr(node.monumentId) },
                                onDirection       = { viewModel.requestDirection() }
                            )

                            Spacer(Modifier.height(24.dp))

                            // ── History ───────────────────────────────────
                            if (node.history.isNotBlank()) {
                                NodeSection("📜 History", node.history)
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Navigation info ───────────────────────────
                            if (node.navigationInfo.isNotBlank()) {
                                NodeSection("🗺️ How to Reach", node.navigationInfo)
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Nearby places ─────────────────────────────
                            if (nearby.isNotEmpty()) {
                                NearbySection(nearby)
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── All nodes (if trip active) ────────────────
                            if (tripState.isTripActive && allNodes.isNotEmpty()) {
                                AllNodesSection(
                                    nodes       = allNodes,
                                    visitedIds  = tripState.visitedNodeIds,
                                    currentId   = tripState.currentNodeId
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Non-king: offer to start trip ─────────────
                            if (!isKing && !tripState.isTripActive) {
                                StartTripFromNormalCard(
                                    node    = node,
                                    allNodes = allNodes,
                                    viewModel = viewModel
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── End trip ──────────────────────────────────
                            if (tripState.isTripActive) {
                                Box(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0x22FF4444))
                                        .border(1.dp, Color(0x55FF4444), RoundedCornerShape(16.dp))
                                        .clickable { viewModel.endTrip() }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🚪 End Trip", color = Color(0xFFFF6B6B), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(80.dp))
                            }
                        }
                    }
                }

                // ── Direction sheet overlay ───────────────────────────────
                val dirState = viewModel.directionState.collectAsStateWithLifecycle().value
                AnimatedVisibility(
                    visible  = dirState != null,
                    enter    = slideInVertically { it },
                    exit     = slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    dirState?.let {
                        DirectionSheet(
                            direction  = it,
                            allNodes   = allNodes,
                            nearbyPlaces = nearby,
                            onDismiss  = { viewModel.dismissDirection() }
                        )
                    }
                }
            }

            is NodeDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(s.message, color = Color(0xFFFF6B6B), fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // ── Sticky chatbot + voice bubbles ────────────────────────────────
        val tripSnap = TripManager.current()
        Row(
            Modifier.align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Voice
            Box(
                Modifier.size(52.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2D1A00), Color(0xFF1A0E00))))
                    .border(1.dp, Color(0x55FFD54F), CircleShape)
                    .clickable {
                        onNavigateToVoice(tripSnap.currentNodeName, tripSnap.currentNodeId.toString())
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, null, tint = AccentYellow, modifier = Modifier.size(24.dp))
            }
            // Chat
            Box(
                Modifier.size(52.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF1A3A6B), Color(0xFF0D2040))))
                    .border(1.dp, GlassBorderBright, CircleShape)
                    .clickable {
                        context.startActivity(
                            Intent(context, ChatbotActivity::class.java).apply {
                                putExtra("SITE_NAME", tripSnap.currentNodeName)
                                putExtra("SITE_ID", tripSnap.currentNodeId.toString())
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Chat, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
        }

        // Video overlays
        CinematicLoaderOverlay(
            uiState  = videoUiState,
            onCancel = { viewModel.videoViewModel.dismiss() },
            modifier = Modifier.fillMaxSize()
        )
        if (videoUiState is com.example.humsafar.models.VideoUiState.ReadyToPlay) {
            VideoPlayerOverlay(
                videoUrl  = (videoUiState as com.example.humsafar.models.VideoUiState.ReadyToPlay).videoUrl,
                onDismiss = { viewModel.videoViewModel.dismiss() },
                modifier  = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Hero image carousel ────────────────────────────────────────────────────────

@Composable
private fun HeroCarousel(
    images: List<String>,
    nodeName: String,
    nodeType: String,
    onBack: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(280.dp)) {
        if (images.isNotEmpty()) {
            var current by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(3500)
                    current = (current + 1) % images.size
                }
            }
            AsyncImage(
                model            = images[current],
                contentDescription = nodeName,
                contentScale     = ContentScale.Crop,
                modifier         = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1A0A00), Color(0xFF0A1628)))))
            Text("🏛️", fontSize = 80.sp, modifier = Modifier.align(Alignment.Center))
        }
        // Scrim
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xBB050D1A), Color.Transparent, Color(0xFF050D1A)))
        ))
        // Back
        Box(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)
                .size(44.dp).clip(CircleShape)
                .background(Color(0xBB000000)).border(0.5.dp, GlassBorder, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        // Title
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            if (nodeType == "KING") {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Color(0x44FFD54F))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("👑 ENTRY NODE", color = AccentYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(nodeName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ── Action row ────────────────────────────────────────────────────────────────

@Composable
private fun NodeActionRow(
    node: MonumentNode,
    tripState: com.example.humsafar.models.TripSnapshot,
    context: android.content.Context,
    onNavigateToVoice: (String, String) -> Unit,
    onScanNext: () -> Unit,
    onDirection: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            NodeActionChip("🎙️", "Hear It") {
                onNavigateToVoice(node.name, node.id.toString())
            }
        }
        item {
            NodeActionChip("🗺️", "Direction") { onDirection() }
        }
        item {
            NodeActionChip("📷", "Scan Next") { onScanNext() }
        }
        item {
            NodeActionChip("💬", "Ask AI") {
                context.startActivity(
                    Intent(context, ChatbotActivity::class.java).apply {
                        putExtra("SITE_NAME", node.name)
                        putExtra("SITE_ID", node.id.toString())
                    }
                )
            }
        }
    }
}

@Composable
private fun NodeActionChip(emoji: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50))
            .background(GlassWhite15)
            .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Sections ──────────────────────────────────────────────────────────────────

@Composable
private fun NodeSection(title: String, body: String) {
    Column {
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(GlassWhite10).border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun NearbySection(places: List<NearbyPlace>) {
    Column {
        Text("📌 Nearby Facilities", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        places.forEach { place ->
            val emoji = when (place.type) {
                "WASHROOM" -> "🚻"; "CANTEEN" -> "🍽️"; "SNACKS" -> "🥤"; "EXIT" -> "🚪"; else -> "📍"
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(emoji, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(place.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (place.description.isNotBlank())
                        Text(place.description, color = TextTertiary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AllNodesSection(
    nodes: List<MonumentNode>,
    visitedIds: List<Long>,
    currentId: Long
) {
    Column {
        Text("🗺️ Trip Progress", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        nodes.sortedBy { it.visitOrder }.forEach { node ->
            val visited = node.id in visitedIds
            val isCurrent = node.id == currentId
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> AccentYellow
                                visited   -> Color(0xFF4ADE80)
                                else      -> GlassWhite15
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (visited || isCurrent) "✓" else "${node.visitOrder}",
                        color = if (visited || isCurrent) Color.Black else TextTertiary,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    node.name,
                    color = when {
                        isCurrent -> AccentYellow
                        visited   -> TextTertiary
                        else      -> TextPrimary
                    },
                    fontSize = 14.sp,
                    fontWeight = if (node.nodeType == "KING") FontWeight.Bold else FontWeight.Normal
                )
                if (node.recommended && !visited) {
                    Spacer(Modifier.width(8.dp))
                    Text("→ Next", color = Color(0xFF00FF88), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StartTripFromNormalCard(
    node: MonumentNode,
    allNodes: List<MonumentNode>,
    viewModel: NodeDetailViewModel
) {
    var showVisitedPicker by remember { mutableStateOf(false) }
    val selectedVisited = remember { mutableStateListOf<Long>() }

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color(0x22FFD54F))
            .border(1.dp, Color(0x55FFD54F), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Text("🗺️ Want a guided tour?", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Start a trip from here to get proper directions and next node suggestions.",
                color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp
            )
            Spacer(Modifier.height(16.dp))

            if (!showVisitedPicker) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(AccentYellow)
                            .clickable { viewModel.startTripFromNormal(node, emptyList()) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Start Fresh", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { showVisitedPicker = true }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("I visited some", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            } else {
                Text("Which nodes have you visited?", color = TextTertiary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                allNodes.filter { it.nodeType != "KING" }.forEach { n ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (n.id in selectedVisited) selectedVisited.remove(n.id)
                            else selectedVisited.add(n.id)
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                                .background(if (n.id in selectedVisited) AccentYellow else GlassWhite15)
                                .border(0.7.dp, GlassBorder, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (n.id in selectedVisited)
                                Text("✓", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(n.name, color = TextPrimary, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(AccentYellow)
                        .clickable { viewModel.startTripFromNormal(node, selectedVisited.toList()) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Start Trip", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Direction sheet ───────────────────────────────────────────────────────────

@Composable
private fun DirectionSheet(
    direction: DirectionInfo,
    allNodes: List<MonumentNode>,
    nearbyPlaces: List<NearbyPlace>,
    onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xF2050D1A))
            .border(0.7.dp, GlassBorderBright, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(24.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("🗺️ Where to Go Next", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(GlassWhite15).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(16.dp))

            // You are here
            if (direction.nearestNodeName.isNotBlank()) {
                DirectionRow("📍", "You are near", direction.nearestNodeName, TextTertiary)
                Spacer(Modifier.height(10.dp))
            }
            // Go here next
            if (direction.recommendedNodeName.isNotBlank()) {
                DirectionRow("➡️", "Visit next", direction.recommendedNodeName, AccentYellow)
                Spacer(Modifier.height(10.dp))
            }
            // Distance
            if (direction.distanceMeters > 0) {
                DirectionRow("📏", "Distance", "${direction.distanceMeters.toInt()} m away", TextSecondary)
            }

            Spacer(Modifier.height(16.dp))

            // Nearby facilities quick list
            if (nearbyPlaces.isNotEmpty()) {
                Text("Nearby Facilities", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(nearbyPlaces) { place ->
                        val emoji = when (place.type) {
                            "WASHROOM" -> "🚻"; "CANTEEN" -> "🍽️"; "SNACKS" -> "🥤"; else -> "📍"
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(GlassWhite15)
                                .border(0.5.dp, GlassBorder, RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("$emoji ${place.name}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DirectionRow(emoji: String, label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Text("$label: ", color = TextTertiary, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

data class DirectionInfo(
    val nearestNodeName: String     = "",
    val recommendedNodeName: String = "",
    val distanceMeters: Double      = 0.0
)