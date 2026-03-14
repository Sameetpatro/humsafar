package com.example.humsafar.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.RecommendationResponse
import com.example.humsafar.network.HumsafarClient
import com.example.humsafar.utils.haversineDistance
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TripCompletionScreen(
    siteId: Int,
    siteName: String,
    visitedNodesCount: Int,
    totalNodesCount: Int,
    onExploreRecommendations: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val tripState by TripManager.state.collectAsStateWithLifecycle()
    var showFarewellMessage by remember { mutableStateOf(false) }
    var recommendations by remember { mutableStateOf<List<RecommendationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(siteId) {
        isLoading = true
        errorMessage = null
        try {
            val response = withContext(Dispatchers.IO) {
                HumsafarClient.api.getRecommendations(siteId)
            }
            if (response.isSuccessful && response.body() != null) {
                recommendations = response.body()!!
                Log.d("TripCompletion", "Loaded ${recommendations.size} recommendations for site $siteId")
            } else {
                errorMessage = "Failed to load recommendations"
                Log.e("TripCompletion", "API error: ${response.code()}")
            }
        } catch (e: Exception) {
            errorMessage = "Connection error: ${e.message}"
            Log.e("TripCompletion", "Exception: ${e.message}", e)
        }
        isLoading = false
    }

    val userLat = tripState.lastLat.takeIf { it != 0.0 }
    val userLng = tripState.lastLng.takeIf { it != 0.0 }
    val withCoords = recommendations.filter { it.latitude != null && it.longitude != null }
    val sortedByDist = if (userLat != null && userLng != null) {
        withCoords.sortedBy { haversineDistance(userLat, userLng, it.latitude!!, it.longitude!!) }
    } else withCoords
    val nearbyVisits = sortedByDist.filter { it.type == "monument" }.take(5)
    val nearbyStays = sortedByDist.filter { it.type == "hotel" }.take(5)
    val nearbyRestaurants = sortedByDist.filter { it.type == "restaurant" }.take(5)

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        AnimatedVisibility(
            visible = showFarewellMessage,
            enter = fadeIn(tween(500)) + scaleIn(tween(500)),
            exit = fadeOut(tween(300))
        ) {
            FarewellMessage(
                onDismiss = onSkip
            )
        }

        AnimatedVisibility(
            visible = !showFarewellMessage,
            enter = fadeIn(),
            exit = fadeOut(tween(300))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(32.dp))

                    CompletionHeader(
                        siteName = siteName,
                        visitedNodesCount = visitedNodesCount,
                        totalNodesCount = totalNodesCount
                    )

                    Spacer(Modifier.height(32.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentYellow)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Loading recommendations...",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                errorMessage!!,
                                color = Color(0xFFFF6B6B),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        if (nearbyVisits.isNotEmpty()) {
                            RecommendationSection(
                                title = "🏛️ Spots to Visit",
                                subtitle = "Top 5 nearest heritage sites",
                                recommendations = nearbyVisits,
                                context = context
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        if (nearbyStays.isNotEmpty()) {
                            RecommendationSection(
                                title = "🏨 Stays",
                                subtitle = "Top 5 nearest hotels & guest houses",
                                recommendations = nearbyStays,
                                context = context
                            )
                            Spacer(Modifier.height(24.dp))
                        }

                        if (nearbyRestaurants.isNotEmpty()) {
                            RecommendationSection(
                                title = "🍽️ Restaurants",
                                subtitle = "Top 5 nearest places to eat",
                                recommendations = nearbyRestaurants,
                                context = context
                            )
                        }

                        if (recommendations.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📍", fontSize = 48.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "No recommendations available for this site yet",
                                        color = TextSecondary,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                BottomButtonsSection(
                    onExploreRecommendations = onExploreRecommendations,
                    onSkip = {
                        showFarewellMessage = true
                    }
                )
            }
        }
    }
}

@Composable
private fun CompletionHeader(
    siteName: String,
    visitedNodesCount: Int,
    totalNodesCount: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🙏",
            fontSize = 64.sp,
            modifier = Modifier.scale(scale)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Namaskar",
            color = AccentYellow,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Hope you enjoyed exploring!",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            tint = Color(0x22FFD54F)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = siteName,
                    color = AccentYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(
                        icon = Icons.Default.Place,
                        value = "$visitedNodesCount/$totalNodesCount",
                        label = "Nodes Visited"
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(GlassBorder)
                    )
                    StatItem(
                        icon = Icons.Default.CheckCircle,
                        value = "✓",
                        label = "Trip Complete"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AccentYellow, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = TextTertiary, fontSize = 11.sp)
    }
}

@Composable
private fun RecommendationSection(
    title: String,
    subtitle: String,
    recommendations: List<RecommendationResponse>,
    context: android.content.Context
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(recommendations) { recommendation ->
                RecommendationCard(recommendation = recommendation, context = context)
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: RecommendationResponse,
    context: android.content.Context
) {
    val icon = when (recommendation.type) {
        "monument" -> "🏛️"
        "hotel" -> "🏨"
        "restaurant" -> "🍽️"
        else -> "📍"
    }
    val lat = recommendation.latitude ?: return
    val lng = recommendation.longitude ?: return

    GlassCard(
        modifier = Modifier
            .width(160.dp)
            .clickable {
                val uri = Uri.parse("google.navigation:q=$lat,$lng")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")))
                }
            },
        cornerRadius = 16.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0D1F3C), Color(0xFF1E3050))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 36.sp)
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = recommendation.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = recommendation.description ?: recommendation.type.replaceFirstChar { it.uppercase() },
                    color = TextTertiary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun BottomButtonsSection(
    onExploreRecommendations: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0xF0050D1A))
                )
            )
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp, top = 8.dp)
    ) {
        GlassPrimaryButton(
            text = "Explore Recommendations",
            onClick = onExploreRecommendations,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(GlassWhite15)
                .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
                .clickable { onSkip() }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Skip",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FarewellMessage(
    onDismiss: () -> Unit
) {
    val scale by rememberInfiniteTransition(label = "farewell").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🙏",
                fontSize = 80.sp,
                modifier = Modifier.scale(scale)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Visit Again",
                color = AccentYellow,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Namaskar",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    val delay = it * 200
                    val alpha by rememberInfiniteTransition(label = "dot$it").animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = delay, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentYellow.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}
