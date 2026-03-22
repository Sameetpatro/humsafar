package com.example.humsafar.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.humsafar.models.AmenityResponse
import com.example.humsafar.network.HumsafarClient
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AmenityDetailScreen(
    amenityId: Int,
    onBack:    () -> Unit
) {
    val context = LocalContext.current
    var amenity  by remember { mutableStateOf<AmenityResponse?>(null) }
    var loading  by remember { mutableStateOf(true) }
    var error    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(amenityId) {
        loading = true
        error   = null
        try {
            val resp = withContext(Dispatchers.IO) {
                HumsafarClient.api.getAmenityDetail(amenityId)
            }
            if (resp.isSuccessful && resp.body() != null) {
                amenity = resp.body()
            } else {
                error = "Could not load details (${resp.code()})"
            }
        } catch (e: Exception) {
            error = e.message ?: "Connection error"
        }
        loading = false
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {

            // ── Top bar ──────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color(0xF0050D1A), Color(0xBB050D1A))))
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
                    Text(
                        text = if (amenity != null) amenity!!.name else "Loading…",
                        color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (amenity?.type == "shop") "🛍️" else "🚻",
                                fontSize = 48.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Loading…", color = TextPrimary, fontSize = 16.sp)
                        }
                    }
                }

                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            error!!, color = Color(0xFFFF6B6B),
                            fontSize = 14.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }

                amenity != null -> {
                    val a = amenity!!
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                    ) {
                        Spacer(Modifier.height(24.dp))

                        // ── Hero icon + type badge ────────────────────────────
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val inf = rememberInfiniteTransition(label = "amenity")
                                val glow by inf.animateFloat(
                                    0.4f, 0.8f,
                                    infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
                                    "g"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(96.dp).clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    if (a.type == "shop") Color(0xFFFFD54F).copy(glow * 0.3f)
                                                    else Color(0xFF2196F3).copy(glow * 0.3f),
                                                    Color.Transparent
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp).clip(CircleShape)
                                            .background(
                                                if (a.type == "shop")
                                                    Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                                                else
                                                    Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF1565C0)))
                                            )
                                            .border(1.5.dp, GlassBorderBright, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (a.type == "shop") "🛍️" else "🚻",
                                            fontSize = 36.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(a.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (a.type == "shop") Color(0x22FFD54F) else Color(0x222196F3)
                                        )
                                        .border(
                                            0.7.dp,
                                            if (a.type == "shop") Color(0x55FFD54F) else Color(0x552196F3),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (a.type == "shop") "🛍️  Shop" else "🚻  Washroom",
                                        color = if (a.type == "shop") AccentYellow else Color(0xFF64B5F6),
                                        fontSize = 13.sp, fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // ── Distance chip ─────────────────────────────────────
                        a.distanceMeters?.let { dist ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlassWhite10)
                                    .border(0.7.dp, GlassBorder, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn, null,
                                        tint = AccentYellow, modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (dist >= 1000)
                                            "${"%.1f".format(dist / 1000)} km from current node"
                                        else
                                            "${dist.toInt()} m from current node",
                                        color = TextPrimary, fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        // ── Info cards grid ───────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Price info
                            AmenityInfoCard(
                                icon  = if (a.isPaid) "💰" else "🆓",
                                label = "Entry",
                                value = a.priceInfo ?: if (a.isPaid) "Paid" else "Free",
                                modifier = Modifier.weight(1f)
                            )
                            // Timing
                            AmenityInfoCard(
                                icon  = "🕐",
                                label = "Timing",
                                value = a.timing ?: "Check on-site",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Description / what's famous ───────────────────────
                        if (!a.description.isNullOrBlank()) {
                            GlassCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        text = if (a.type == "shop") "✨ What's special here" else "ℹ️ About this washroom",
                                        color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        a.description!!,
                                        color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        // ── Directions CTA ────────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0D2845), Color(0xFF071428))))
                                .border(1.dp, Color(0xFF4A90D9).copy(0.5f), RoundedCornerShape(16.dp))
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("google.navigation:q=${a.latitude},${a.longitude}")
                                        ).apply { setPackage("com.google.android.apps.maps") }
                                    )
                                }
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp).clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF1565C0)))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Navigation, null,
                                        tint = Color.White, modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Get Directions", color = TextPrimary,
                                        fontSize = 15.sp, fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Open in Google Maps",
                                        color = TextTertiary, fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight, null,
                                    tint = Color(0xFF4A90D9), modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AmenityInfoCard(
    icon:     String,
    label:    String,
    value:    String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 14.dp) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(label, color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                value, color = TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 2
            )
        }
    }
}