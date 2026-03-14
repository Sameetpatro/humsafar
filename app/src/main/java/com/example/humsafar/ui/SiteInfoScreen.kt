package com.example.humsafar.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.humsafar.network.SiteImage
import com.example.humsafar.network.WeatherService
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteInfoScreen(
    siteId: Int,
    siteName: String,
    latitude: Double,
    longitude: Double,
    onBack: () -> Unit
) {
    var site by remember { mutableStateOf<SiteDetail?>(null) }
    var weather by remember { mutableStateOf<WeatherService.WeatherResult?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(siteId, latitude, longitude) {
        loading = true
        error = null
        try {
            val siteResp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                HumsafarClient.api.getSiteDetail(siteId)
            }
            if (siteResp.isSuccessful && siteResp.body() != null) {
                site = siteResp.body()
            } else {
                error = "Could not load site"
            }
            val w = WeatherService.fetchWeather(latitude, longitude)
            weather = w
        } catch (e: Exception) {
            error = e.message ?: "Unknown error"
        }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(siteName, color = TextPrimary, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            when {
                loading -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentYellow)
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text(error!!, color = Color(0xFFFF6B6B))
                    }
                }
                else -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(padding)
                            .padding(horizontal = 20.dp)
                    ) {
                        site?.let { s ->
                            // Weather card
                            weather?.let { w ->
                                WeatherCard(weather = w)
                                Spacer(Modifier.height(16.dp))
                            }

                            // Images
                            if (s.images.isNotEmpty()) {
                                val sortedImages = s.images.sortedBy { it.displayOrder }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                                ) {
                                    itemsIndexed(sortedImages) { _, img ->
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(img.imageUrl).crossfade(300).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(120.dp, 80.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                            }

                            // Description
                            s.summary?.takeIf { it.isNotBlank() }?.let { desc ->
                                SectionTitle("About")
                                Text(
                                    desc,
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                            }

                            // Opening hours (dummy - DB field planned)
                            SectionTitle("Opening Hours")
                            Text(
                                "9:00 AM – 6:00 PM (Placeholder – will come from DB)",
                                color = TextTertiary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(Modifier.height(16.dp))

                            // Ratings (from site or dummy - DB reviews planned)
                            SectionTitle("Rating & Reviews")
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                Text("⭐", fontSize = 18.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "%.1f".format(s.rating.takeIf { it > 0 } ?: 4.5),
                                    color = AccentYellow,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("(Reviews coming from DB)", color = TextTertiary, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(weather: WeatherService.WeatherResult) {
    val suggestions = remember(weather) {
        WeatherService.weatherSuggestions(weather.tempC, weather.weatherCode)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0x22FFFFFF)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "%.0f°C".format(weather.tempC),
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(weather.description, color = TextSecondary, fontSize = 14.sp)
                }
            }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Suggestions:", color = TextTertiary, fontSize = 12.sp)
                suggestions.forEach { sugg ->
                    Text("• $sugg", color = AccentYellow, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = TextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}
