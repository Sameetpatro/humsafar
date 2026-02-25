// app/src/main/java/com/example/humsafar/ui/NodeDetailScreen.kt
// REWRITTEN — Fixed to work with FastAPI backend models (Int IDs, SiteNode)

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.ChatbotActivity
import com.example.humsafar.data.TripManager
import com.example.humsafar.network.Node
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*

@Composable
fun NodeDetailScreen(
    nodeId:            Int,
    siteId:            Int,
    isKing:            Boolean,
    onBack:            () -> Unit,
    onNavigateToQr:    (Long) -> Unit,
    onNavigateToVoice: (String, String) -> Unit,
    viewModel:         NodeDetailViewModel = viewModel()
) {
    val context   = LocalContext.current
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val tripState by TripManager.state.collectAsStateWithLifecycle()

    LaunchedEffect(nodeId, siteId) {
        viewModel.loadNode(nodeId, siteId)
    }

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
                val node     = s.node
                val site     = s.site
                val allNodes = s.allNodes

                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {

                        HeroSection(
                            nodeName = node.name,
                            siteId   = siteId,
                            onBack   = onBack
                        )

                        Column(Modifier.padding(horizontal = 20.dp)) {

                            Spacer(Modifier.height(16.dp))

                            NodeActionRow(
                                node              = node,
                                siteId            = siteId,
                                context           = context,
                                onNavigateToVoice = onNavigateToVoice,
                                onNavigateToQr    = onNavigateToQr
                            )

                            Spacer(Modifier.height(24.dp))

                            // Site info
                            if (site.summary?.isNotBlank() == true) {
                                NodeSection("📖 About ${site.name}", site.summary!!)
                                Spacer(Modifier.height(16.dp))
                            }

                            if (site.history?.isNotBlank() == true) {
                                NodeSection("📜 History", site.history!!)
                                Spacer(Modifier.height(16.dp))
                            }

                            if (node.description?.isNotBlank() == true) {
                                NodeSection("🗺️ About This Spot", node.description!!)
                                Spacer(Modifier.height(16.dp))
                            }

                            if (tripState.isTripActive && allNodes.isNotEmpty()) {
                                AllNodesSection(
                                    nodes      = allNodes,
                                    visitedIds = tripState.visitedNodeIds,
                                    currentId  = tripState.currentNodeId
                                )
                                Spacer(Modifier.height(16.dp))
                            }

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

                // Sticky chatbot + voice bubbles
                Row(
                    Modifier.align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF2D1A00), Color(0xFF1A0E00))))
                            .border(1.dp, Color(0x55FFD54F), CircleShape)
                            .clickable { onNavigateToVoice(node.name, node.id.toString()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, null, tint = AccentYellow, modifier = Modifier.size(24.dp))
                    }
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF1A3A6B), Color(0xFF0D2040))))
                            .border(1.dp, GlassBorderBright, CircleShape)
                            .clickable {
                                context.startActivity(
                                    Intent(context, ChatbotActivity::class.java).apply {
                                        putExtra("SITE_NAME", node.name)
                                        putExtra("SITE_ID", siteId.toString())    // ← siteId is the correct DB FK
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Chat, null, tint = TextPrimary, modifier = Modifier.size(22.dp))
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
    }
}

// ── Hero section ──────────────────────────────────────────────────────────────

@Composable
private fun HeroSection(
    nodeName: String,
    siteId:   Int,
    onBack:   () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(240.dp)) {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF1A0A00), Color(0xFF0A1628)))
        ))
        Text("🏛️", fontSize = 80.sp, modifier = Modifier.align(Alignment.Center))

        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xBB050D1A), Color.Transparent, Color(0xFF050D1A)))
        ))

        Box(
            Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp)
                .size(44.dp).clip(CircleShape)
                .background(Color(0xBB000000)).border(0.5.dp, GlassBorder, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            Text(nodeName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ── Action row ────────────────────────────────────────────────────────────────

@Composable
private fun NodeActionRow(
    node:              Node,
    siteId:            Int,
    context:           android.content.Context,
    onNavigateToVoice: (String, String) -> Unit,
    onNavigateToQr:    (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            NodeActionChip("🎙️", "Hear It") {
                onNavigateToVoice(node.name, node.id.toString())
            }
        }
        item {
            NodeActionChip("📷", "Scan Next") {
                onNavigateToQr(siteId.toLong())
            }
        }
        item {
            NodeActionChip("💬", "Ask AI") {
                context.startActivity(
                    Intent(context, ChatbotActivity::class.java).apply {
                        putExtra("SITE_NAME", node.name)
                        putExtra("SITE_ID", siteId.toString())
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
private fun AllNodesSection(
    nodes:      List<Node>,
    visitedIds: List<Int>,
    currentId:  Int
) {
    Column {
        Text("🗺️ Trip Progress", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        nodes.sortedBy { it.sequenceOrder }.forEach { node ->
            val visited   = node.id in visitedIds
            val isCurrent = node.id == currentId
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(CircleShape)
                        .background(when {
                            isCurrent -> AccentYellow
                            visited -> Color(0xFF4ADE80)
                            else -> GlassWhite15
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (visited || isCurrent) "✓" else "${node.sequenceOrder}",
                        color = if (visited || isCurrent) Color.Black else TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    node.name,
                    color = when {
                        isCurrent -> AccentYellow
                        visited -> TextTertiary
                        else -> TextPrimary
                    },
                    fontSize = 14.sp,
                    fontWeight = if (node.sequenceOrder == 0) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}