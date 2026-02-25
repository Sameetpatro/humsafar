// app/src/main/java/com/example/humsafar/ui/HeritageDetailScreen.kt
// UPDATED: Real image gallery from site_images + intro_video_url watch button
// Images sorted by display_order, horizontal scroll, thumbnail strip below hero

package com.example.humsafar.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.humsafar.network.SiteImage
import com.example.humsafar.network.NodeImage
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.*

@Composable
fun HeritageDetailScreen(
    siteName:           String,
    siteId:             String,
    onBack:             () -> Unit,
    onNavigateToVoice:  (String, String) -> Unit,
    onNavigateToQrScan: (Int) -> Unit,
    viewModel:          HeritageDetailViewModel = viewModel()
) {
    val context  = LocalContext.current
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(siteId) { viewModel.loadSite(siteId.toIntOrNull() ?: 0) }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        when (val s = uiState) {

            is HeritageDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏯", fontSize = 52.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading site…", color = TextPrimary, fontSize = 16.sp)
                    }
                }
            }

            is HeritageDetailUiState.Ready -> {
                val site = s.site

                // Images sorted by display_order
                val siteImages = remember(site.images) {
                    site.images.sortedBy { it.displayOrder }
                }

                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {

                            // ── Image gallery hero ──────────────────────────
                            SiteImageGallery(
                                siteName = site.name,
                                images   = siteImages,
                                rating   = site.rating,
                                onBack   = onBack
                            )

                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Spacer(Modifier.height(16.dp))

                                // Action chips
                                SiteChipsRow(
                                    siteName  = site.name,
                                    siteId    = siteId,
                                    context   = context,
                                    onVoice   = onNavigateToVoice,
                                    onQr      = { onNavigateToQrScan(site.id) }
                                )

                                Spacer(Modifier.height(20.dp))

                                if (!site.summary.isNullOrBlank()) {
                                    SiteSection("📖 Overview", site.summary!!)
                                    Spacer(Modifier.height(14.dp))
                                }
                                if (!site.history.isNullOrBlank()) {
                                    SiteSection("📜 History", site.history!!)
                                    Spacer(Modifier.height(14.dp))
                                }
                                if (!site.funFacts.isNullOrBlank()) {
                                    SiteSection("✨ Fun Facts", site.funFacts!!)
                                    Spacer(Modifier.height(14.dp))
                                }

                                // ── Nodes section with Start Trip button ──────
                                if (site.nodes.isNotEmpty()) {
                                    NodesSection(
                                        nodes    = site.nodes.sortedBy { it.sequenceOrder },
                                        siteId   = site.id,
                                        onStartTrip = onNavigateToQrScan
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                if (!site.helplineNumber.isNullOrBlank()) {
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x1500C853))
                                            .border(0.7.dp, Color(0x3300C853), RoundedCornerShape(12.dp))
                                            .clickable {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${site.helplineNumber}"))
                                                )
                                            }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Phone, null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text("Helpline: ${site.helplineNumber}", color = Color(0xFF00C853), fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(14.dp))
                                }

                                // Bottom padding for video bar
                                Spacer(Modifier.height(if (!site.introVideoUrl.isNullOrBlank()) 96.dp else 24.dp))
                            }
                        }
                    }

                    // ── Sticky "Watch Video" bar ─────────────────────────────
                    if (!site.introVideoUrl.isNullOrBlank()) {
                        WatchVideoBar(
                            videoUrl = site.introVideoUrl!!,
                            label    = "Watch ${site.name} Video",
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            is HeritageDetailUiState.Error -> {
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

// ──────────────────────────────────────────────────────────────────────────────
// Site image gallery (site_images table)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SiteImageGallery(
    siteName: String,
    images:   List<SiteImage>,
    rating:   Double,
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
            .height(320.dp)
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
                            is AsyncImagePainter.State.Loading -> SiteShimmer()
                            is AsyncImagePainter.State.Error   -> SiteEmptyImage()
                            else                               -> SubcomposeAsyncImageContent()
                        }
                    }
                }
            }

            // Counter badge
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

            // Dots
            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 52.dp),
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
            SiteEmptyImage()
        }

        // Gradient overlays
        Box(
            Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color(0xCC050D1A), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(120.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF050D1A))))
        )

        // Back button
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

        // Rating badge
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 48.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xCC000000))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⭐", fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Text(String.format("%.1f", rating), color = AccentYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Site name overlay
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(siteName, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }

    // Thumbnail filmstrip
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
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SiteSection(title: String, body: String) {
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

@Composable
private fun SiteChipsRow(
    siteName: String,
    siteId:   String,
    context:  android.content.Context,
    onVoice:  (String, String) -> Unit,
    onQr:     () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SiteChip("💬 Ask Shree") {
                context.startActivity(
                    Intent(context, ChatbotActivity::class.java).apply {
                        putExtra("SITE_NAME", siteName)
                        putExtra("SITE_ID", siteId)
                    }
                )
            }
        }
        item { SiteChip("🎙️ Voice Guide") { onVoice(siteName, siteId) } }
        item { SiteChip("📷 Scan Next QR") { onQr() } }

    }
}

@Composable
private fun SiteChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(GlassWhite15)
            .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SiteShimmer() {
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
                start = Offset(x * 800f, 0f),
                end   = Offset((x + 1f) * 800f, 0f)
            )
        )
    )
}

@Composable
private fun SiteEmptyImage() {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1628), Color(0xFF050D1A)))),
        contentAlignment = Alignment.Center
    ) {
        Text("🏯", fontSize = 72.sp, modifier = Modifier.alpha(0.2f))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Nodes section — beautiful cards + Start Trip button
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodesSection(
    nodes:       List<com.example.humsafar.network.Node>,
    siteId:      Int,
    onStartTrip: (Int) -> Unit
) {
    Column {
        // Header row: title + Start Trip button
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("🗺️ Explore Points", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("${nodes.size} stops in this site", color = TextTertiary, fontSize = 11.sp)
            }

            // ── Start Trip button ──
            val inf   = rememberInfiniteTransition(label = "st")
            val pulse by inf.animateFloat(
                0.94f, 1.06f,
                infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "sp"
            )
            val glow by inf.animateFloat(
                0.5f, 1f,
                infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "sg"
            )

            Box(
                modifier = Modifier
                    .scale(pulse)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF004D00), Color(0xFF001A00)))
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF00C853).copy(alpha = glow),
                                Color(0xFF69F0AE).copy(alpha = glow * 0.5f)
                            )
                        ),
                        RoundedCornerShape(50)
                    )
                    .clickable { onStartTrip(siteId) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint     = Color(0xFF00C853),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Start Trip",
                        color      = Color(0xFF00C853),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Node cards
        nodes.forEachIndexed { index, node ->
            NodeCard(node = node, index = index, totalNodes = nodes.size)
            if (index < nodes.size - 1) {
                // Connector line between cards
                Box(
                    Modifier
                        .padding(start = 21.dp)
                        .width(1.5.dp)
                        .height(10.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (node.isKing) AccentYellow.copy(alpha = 0.5f) else Color(0xFF2A4A7F),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun NodeCard(
    node:       com.example.humsafar.network.Node,
    index:      Int,
    totalNodes: Int
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "nc_scale"
    )

    val isKing = node.isKing

    // Animated entrance
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 60L)
        visible.value = true
    }
    AnimatedVisibility(
        visible  = visible.value,
        enter    = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isKing)
                        Brush.linearGradient(listOf(Color(0xFF2A1A00), Color(0xFF1A0E00)))
                    else
                        Brush.linearGradient(listOf(Color(0xFF0D1E3A), Color(0xFF091428)))
                )
                .border(
                    width = if (isKing) 1.5.dp else 0.7.dp,
                    brush = if (isKing)
                        Brush.linearGradient(listOf(AccentYellow.copy(alpha = 0.7f), Color(0xFFFFD54F).copy(alpha = 0.2f)))
                    else
                        Brush.linearGradient(listOf(Color(0xFF1E3A6A), Color(0xFF0D1F3C))),
                    shape = RoundedCornerShape(16.dp)
                )
                .indication(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication        = null
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        }
                    )
                }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Sequence badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isKing)
                                Brush.linearGradient(listOf(AccentYellow, Color(0xFFFFC107)))
                            else
                                Brush.linearGradient(listOf(Color(0xFF1A3A6B), Color(0xFF0D2040)))
                        )
                        .border(
                            1.dp,
                            if (isKing) Color(0x55FFFFFF) else Color(0xFF1E3A6A),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isKing) {
                        Text("⭐", fontSize = 18.sp)
                    } else {
                        Text(
                            "${node.sequenceOrder}",
                            color      = TextPrimary,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            node.name,
                            color      = if (isKing) AccentYellow else TextPrimary,
                            fontSize   = 14.sp,
                            fontWeight = if (isKing) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                        if (isKing) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(AccentYellow.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("MAIN", color = AccentYellow, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    if (!node.description.isNullOrBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            node.description!!.take(60) + if (node.description!!.length > 60) "…" else "",
                            color    = TextTertiary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                // QR scan hint icon
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        tint     = if (isKing) AccentYellow.copy(alpha = 0.6f) else TextTertiary.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "scan",
                        color    = TextTertiary.copy(alpha = 0.4f),
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}