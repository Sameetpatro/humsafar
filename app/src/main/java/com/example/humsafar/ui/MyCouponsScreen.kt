package com.example.humsafar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.models.Coupon
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

private fun parseInstant(s: String): Instant? =
    runCatching { OffsetDateTime.parse(s).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(s) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(s).toInstant(ZoneOffset.UTC) }.getOrNull()

private fun formatRemaining(millis: Long): String {
    if (millis <= 0) return "Expired"
    val totalSec = millis / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (h > 0) "%dh %02dm %02ds".format(h, m, sec) else "%dm %02ds".format(m, sec)
}

@Composable
fun MyCouponsScreen(
    onBack: () -> Unit,
    vm: StoreViewModel = viewModel()
) {
    val tokens = LocalAppColors.current
    val coupons by vm.coupons.collectAsState()

    LaunchedEffect(Unit) { vm.loadCoupons() }

    // 1Hz tick driving all countdowns.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { nowMs = System.currentTimeMillis(); delay(1000) }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)))
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tokens.surface)
                        .border(0.8.dp, tokens.border, RoundedCornerShape(14.dp)).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tokens.textPrimary, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(14.dp))
                Text("My Coupons", color = tokens.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }

            if (coupons.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎫", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No coupons yet", color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Spin the wheel in the store to win discounts at nearby hotels and restaurants.",
                        color = tokens.textSecondary, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 40.dp)
                ) {
                    items(coupons) { c -> CouponCard(c, nowMs) }
                }
            }
        }
    }
}

@Composable
private fun CouponCard(c: Coupon, nowMs: Long) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current

    val expiresMs = remember(c.expiresAt) { parseInstant(c.expiresAt)?.toEpochMilli() }
    val remaining = expiresMs?.let { it - nowMs } ?: 0L
    val isExpired = c.status == "expired" || c.status == "redeemed" || remaining <= 0
    val isRedeemed = c.status == "redeemed"

    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (c.partnerType == "hotel") "🏨" else "🍽️", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.partnerName, color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${Tiers.label[c.tier] ?: c.tier} coupon", color = tokens.textTertiary, fontSize = 11.sp)
                }
                Text("${c.discountPct}%", color = accent.dark, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(tokens.surfaceMuted)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) { Text(c.code, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black) }
                Spacer(Modifier.weight(1f))
                val (label, color) = when {
                    isRedeemed -> "Redeemed" to tokens.textTertiary
                    isExpired -> "Expired" to Color(0xFFE05555)
                    remaining <= 5 * 60_000 -> "⏱ ${formatRemaining(remaining)}" to Color(0xFFE05555)
                    else -> "⏱ ${formatRemaining(remaining)}" to accent.dark
                }
                Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
