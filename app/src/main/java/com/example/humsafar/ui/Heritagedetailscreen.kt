// HeritageDetailScreen.kt — DISTINCT layout from NodeDetailScreen
// Hero gallery → Weather → Forecast → Video → Hear This Page → Overview/History/Fun Facts → Nodes → Helpline → Scan QR → Chatbot FAB

package com.example.humsafar.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.humsafar.ChatbotActivity
import com.example.humsafar.network.Node
import com.example.humsafar.network.SiteDetail
import com.example.humsafar.network.SiteImage
import com.example.humsafar.network.WeatherService
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.theme.AccentYellow
import com.example.humsafar.ui.theme.GlassBorder
import com.example.humsafar.ui.theme.GlassWhite10
import com.example.humsafar.ui.theme.TextPrimary
import com.example.humsafar.ui.theme.TextSecondary
import com.example.humsafar.ui.theme.TextTertiary

@Composable
fun HeritageDetailScreen(
    @Suppress("UNUSED_PARAMETER") siteName: String,
    siteId: String,
    onBack: () -> Unit,
    onNavigateToVoice: (String, String) -> Unit,
    onNavigateToQrScan: (String) -> Unit,
    viewModel: HeritageDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val siteIdInt = siteId.toIntOrNull() ?: 0

    var weather by remember { mutableStateOf<WeatherService.WeatherResult?>(null) }
    var forecast by remember { mutableStateOf<List<WeatherService.ForecastDay>>(emptyList()) }

    LaunchedEffect(siteIdInt) { viewModel.loadSite(siteIdInt) }

    LaunchedEffect(uiState) {
        val site = (uiState as? HeritageDetailUiState.Ready)?.site ?: return@LaunchedEffect
        weather = WeatherService.fetchWeather(site.latitude, site.longitude)
        forecast = WeatherService.fetchForecast(site.latitude, site.longitude)
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        when (val s = uiState) {
            is HeritageDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏛️", fontSize = 52.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading…", color = TextPrimary, fontSize = 16.sp)
                    }
                }
            }

            is HeritageDetailUiState.Ready -> {
                val site = s.site
                val siteImages = remember(site.images) {
                    site.images.sortedBy { it.displayOrder }
                }

                Column(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // ── Hero image gallery (distinct from NodeDetailScreen) ──
                        HeritageHeroGallery(
                            siteName = site.name,
                            images = siteImages,
                            onBack = onBack
                        )

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Spacer(Modifier.height(20.dp))

                            // ── Weather card (current) ──
                            weather?.let { w ->
                                HeritageWeatherCard(weather = w)
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Forecast horizontal scroll ──
                            if (forecast.isNotEmpty()) {
                                HeritageForecastSection(forecast = forecast)
                                Spacer(Modifier.height(20.dp))
                            }

                            // ── Scan Node to Start Trip ──
                            HeritageActionCard(
                                icon = "📷",
                                title = "Scan Node to Start Trip",
                                subtitle = "Scan QR & explore nodes",
                                gradientColors = listOf(Color(0xFF0D2825), Color(0xFF091F1E)),
                                borderColor = Color(0xFF2DD4BF)
                            ) {
                                onNavigateToQrScan(siteId)
                            }
                            Spacer(Modifier.height(16.dp))

                            // ── Video card ──
                            site.introVideoUrl?.takeIf { it.isNotBlank() }?.let { url ->
                                HeritageVideoCard(
                                    label = "Watch Intro Video"
                                ) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Hear This Page ──
                            HeritageActionCard(
                                icon = "🎧",
                                title = "Hear This Page",
                                subtitle = "Feature coming soon",
                                gradientColors = listOf(Color(0xFF1A0050), Color(0xFF0D0030)),
                                borderColor = Color(0xFF9B30FF)
                            ) {
                                android.widget.Toast.makeText(context, "Feature coming soon", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            Spacer(Modifier.height(16.dp))

                            // ── Content: Overview, History, Fun Facts ──
                            site.summary?.takeIf { it.isNotBlank() }?.let { text ->
                                HeritageContentCard(title = "Overview", body = text)
                                Spacer(Modifier.height(12.dp))
                            }
                            site.history?.takeIf { it.isNotBlank() }?.let { text ->
                                HeritageContentCard(title = "History", body = text)
                                Spacer(Modifier.height(12.dp))
                            }
                            site.funFacts?.takeIf { it.isNotBlank() }?.let { text ->
                                HeritageContentCard(title = "Fun Facts", body = text)
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Nodes ──
                            if (site.nodes.isNotEmpty()) {
                                HeritageNodesSection(
                                    nodes = site.nodes.sortedBy { it.sequenceOrder }
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Helpline ──
                            site.helplineNumber?.takeIf { it.isNotBlank() }?.let { num ->
                                HeritageHelplineCard(
                                    number = num,
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                                        context.startActivity(intent)
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Feedback ──
                            HeritageActionCard(
                                icon = "💬",
                                title = "Feedback",
                                subtitle = "Share your experience",
                                gradientColors = listOf(Color(0xFF0D2825), Color(0xFF091F1E)),
                                borderColor = Color(0xFF2DD4BF)
                            ) {
                                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                            }

                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }

                // ── Chatbot FAB ──
                HeritageChatbotFab(
                    siteName = site.name,
                    siteId = site.id,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }

            is HeritageDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        s.message,
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}

// ── Heritage Hero Gallery (distinct from NodeDetailScreen) ────────────────────
@Composable
private fun HeritageHeroGallery(
    siteName: String,
    images: List<SiteImage>,
    onBack: () -> Unit
) {
    val listState = rememberLazyListState()
    var currentIndex by remember { mutableStateOf(0) }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        currentIndex = listState.firstVisibleItemIndex
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        if (images.isNotEmpty()) {
            LazyRow(
                state = listState,
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
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .fillParentMaxHeight()
                    )
                }
            }

            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(14.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC1A1A2E))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "${currentIndex + 1} / ${images.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (images.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 52.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    images.forEachIndexed { i, _ ->
                        Box(
                            Modifier
                                .size(if (i == currentIndex) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == currentIndex) AccentYellow
                                    else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A1A2E), Color(0xFF0D0D1A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🏛️", fontSize = 80.sp, modifier = Modifier.alpha(0.3f))
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xE61A1A2E), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF050D1A))
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xBB1A1A2E))
                .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                siteName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// ── Weather & Forecast ────────────────────────────────────────────────────────
@Composable
private fun HeritageWeatherCard(weather: WeatherService.WeatherResult) {
    val suggestions = remember(weather) {
        WeatherService.weatherSuggestions(weather.tempC, weather.weatherCode)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E3A5F), Color(0xFF0D2137))
                )
            )
            .border(1.dp, Color(0xFF2E5A8F).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "%.0f°C".format(weather.tempC),
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        weather.description,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
                Text("🌡️", fontSize = 40.sp)
            }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("Suggestions:", color = TextTertiary, fontSize = 12.sp)
                suggestions.forEach { sugg ->
                    Text(
                        "• $sugg",
                        color = AccentYellow,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeritageForecastSection(forecast: List<WeatherService.ForecastDay>) {
    Column {
        Text(
            "7-Day Forecast",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            forecast.forEach { day ->
                val shortDate = day.date.takeLast(5)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassWhite10)
                        .border(0.7.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            shortDate,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "%.0f°".format(day.tempMax),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "%.0f°".format(day.tempMin),
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Video card ─────────────────────────────────────────────────────────────────
@Composable
private fun HeritageVideoCard(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF120038), Color(0xFF06001E))
                )
            )
            .border(1.dp, Color(0xFF9B30FF).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF9B30FF), Color(0xFF5B5FFF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Tap to watch", color = TextTertiary, fontSize = 12.sp)
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color(0xFF9B30FF).copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Content cards ──────────────────────────────────────────────────────────────
@Composable
private fun HeritageContentCard(title: String, body: String) {
    Column {
        Text(
            title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite10)
                .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                body,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

// ── Nodes section ────────────────────────────────────────────────────────────
@Composable
private fun HeritageNodesSection(nodes: List<Node>) {
    Column {
        Text(
            "📍 Nodes at this site",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            nodes.forEach { node ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlassWhite10)
                        .border(0.7.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E5A8F).copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${node.sequenceOrder}",
                                color = AccentYellow,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Text(
                            node.name,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Helpline card ──────────────────────────────────────────────────────────────
@Composable
private fun HeritageHelplineCard(number: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0D2825), Color(0xFF051F1E))
                )
            )
            .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📞", fontSize = 28.sp)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Helpline", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(number, color = TextSecondary, fontSize = 13.sp)
            }
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = Color(0xFF2DD4BF).copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Action card (reusable) ─────────────────────────────────────────────────────
@Composable
private fun HeritageActionCard(
    icon: String,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    borderColor: Color,
    onClick: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "action")
    val glow by inf.animateFloat(
        0.35f, 0.65f,
        infiniteRepeatable(tween(1500, easing = EaseInOutSine), RepeatMode.Reverse),
        "glow"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(1.dp, borderColor.copy(alpha = glow), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextTertiary, fontSize = 11.sp)
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

// ── Chatbot FAB ────────────────────────────────────────────────────────────────
@Composable
private fun HeritageChatbotFab(
    siteName: String,
    siteId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFD54F), Color(0xFFFFC107))
                    )
                )
                .border(
                    2.dp,
                    Brush.linearGradient(
                        listOf(Color(0x66FFFFFF), Color(0x22FFFFFF))
                    ),
                    CircleShape
                )
                .clickable {
                    context.startActivity(
                        Intent(context, ChatbotActivity::class.java).apply {
                            putExtra("SITE_NAME", siteName)
                            putExtra("SITE_ID", siteId.toString())
                            putExtra("NODE_ID", "")
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    null,
                    tint = Color(0xFF050D1A),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "Ask",
                    color = Color(0xFF050D1A),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
