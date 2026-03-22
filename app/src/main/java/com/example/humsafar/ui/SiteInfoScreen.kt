// app/src/main/java/com/example/humsafar/ui/SiteInfoScreen.kt

package com.example.humsafar.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.humsafar.network.HumsafarClient
import com.example.humsafar.network.SiteDetail
import com.example.humsafar.network.WeatherService
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.theme.*

@Composable
fun SiteInfoScreen(
    siteId:     Int,
    siteName:   String,
    latitude:   Double,
    longitude:  Double,
    onBack:     () -> Unit,
    onExplore:  (siteName: String, siteId: String) -> Unit = { _, _ -> }  // ← NEW
) {
    var site    by remember { mutableStateOf<SiteDetail?>(null) }
    var weather by remember { mutableStateOf<WeatherService.WeatherResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error   by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(siteId, latitude, longitude) {
        loading = true
        error   = null
        try {
            val siteResp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                HumsafarClient.api.getSiteDetail(siteId)
            }
            if (siteResp.isSuccessful && siteResp.body() != null) {
                site = siteResp.body()
            } else {
                error = "Could not load site"
            }
            weather = WeatherService.fetchWeather(latitude, longitude)
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xF0050D1A), Color(0xBB050D1A))))
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
                            .size(40.dp).clip(CircleShape)
                            .background(GlassWhite15)
                            .border(0.5.dp, GlassBorder, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = TextPrimary, modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            siteName,
                            color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Site Info",
                            color = TextTertiary, fontSize = 12.sp
                        )
                    }
                }
            }

            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏛️", fontSize = 52.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("Loading…", color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }

                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, color = Color(0xFFFF6B6B))
                    }
                }

                else -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                    ) {
                        Spacer(Modifier.height(20.dp))

                        site?.let { s ->

                            // ── Explore CTA ───────────────────────────────
                            ExploreButton(
                                siteName  = s.name,
                                siteId    = s.id,
                                onExplore = onExplore
                            )
                            Spacer(Modifier.height(16.dp))

                            // ── Weather ───────────────────────────────────
                            weather?.let { w ->
                                SiteInfoWeatherCard(weather = w)
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Images ────────────────────────────────────
                            if (s.images.isNotEmpty()) {
                                val sortedImages = s.images.sortedBy { it.displayOrder }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding        = PaddingValues(vertical = 4.dp)
                                ) {
                                    itemsIndexed(sortedImages) { _, img ->
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(img.imageUrl).crossfade(300).build(),
                                            contentDescription = null,
                                            contentScale       = ContentScale.Crop,
                                            modifier           = Modifier
                                                .size(140.dp, 90.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Summary ───────────────────────────────────
                            s.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                                InfoSection(title = "About", body = summary)
                                Spacer(Modifier.height(16.dp))
                            }

                            // ── Rating ────────────────────────────────────
                            RatingRow(rating = s.rating.takeIf { it > 0 } ?: 4.5)
                            Spacer(Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Explore CTA button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExploreButton(
    siteName:  String,
    siteId:    Int,
    onExplore: (String, String) -> Unit
) {
    val inf     = rememberInfiniteTransition(label = "explore")
    val shimmer by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        "sh"
    )
    val glow by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A1000), Color(0xFF2A1F00), Color(0xFF1A1400))))
            .border(
                1.5.dp,
                Brush.linearGradient(
                    colorStops = arrayOf(
                        (shimmer + 0.0f).rem(1f) to AccentYellow.copy(alpha = glow),
                        (shimmer + 0.4f).rem(1f) to Color(0xFFFFC107).copy(alpha = glow * 0.6f),
                        (shimmer + 0.8f).rem(1f) to AccentYellow.copy(alpha = glow)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { onExplore(siteName, siteId.toString()) }
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp).clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(AccentYellow.copy(0.3f), Color(0xFFFFC107).copy(0.1f)))
                    )
                    .border(1.dp, AccentYellow.copy(0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🏛️", fontSize = 22.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Explore This Place",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    "History • AI Guide • Voice • Gallery",
                    color = AccentYellow.copy(alpha = 0.75f), fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(AccentYellow.copy(alpha = 0.18f))
                    .border(0.5.dp, AccentYellow.copy(0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = AccentYellow, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Info section card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoSection(title: String, body: String) {
    Column {
        Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        GlassCard(Modifier.fillMaxWidth(), cornerRadius = 14.dp) {
            Text(
                body,
                color      = TextSecondary,
                fontSize   = 14.sp,
                lineHeight = 22.sp,
                modifier   = Modifier.padding(14.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Weather card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SiteInfoWeatherCard(weather: WeatherService.WeatherResult) {
    val suggestions = remember(weather) {
        WeatherService.weatherSuggestions(weather.tempC, weather.weatherCode)
    }
    GlassCard(
        modifier      = Modifier.fillMaxWidth(),
        cornerRadius  = 16.dp,
        tint          = Color(0x1A2196F3)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "%.0f°C".format(weather.tempC),
                    color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold
                )
                Text(weather.description, color = TextSecondary, fontSize = 13.sp)
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        suggestions.joinToString(" · "),
                        color = AccentYellow, fontSize = 12.sp
                    )
                }
            }
            Text("🌡️", fontSize = 36.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rating row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RatingRow(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("⭐", fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            "%.1f".format(rating),
            color = AccentYellow, fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text("Community rating", color = TextTertiary, fontSize = 12.sp)
    }
}