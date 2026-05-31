package com.example.humsafar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.humsafar.data.ActiveSiteManager
import com.example.humsafar.data.GamificationRepository
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors

private data class TierVisual(
    val tier: String,
    val title: String,
    val price: Int,
    val discountText: String,
    val gradient: List<Color>,
    val textColor: Color,
    val emoji: String
)

private val TIER_VISUALS = listOf(
    TierVisual("ultimate", "Ultimate Special", 200, "20% – 30% OFF",
        listOf(Color(0xFF1A1026), Color(0xFF6D28D9)), Color(0xFFF3E8FF), "👑"),
    TierVisual("special", "Special", 120, "12% – 19% OFF",
        listOf(Color(0xFFB8860B), Color(0xFFFFD54A)), Color(0xFF3A2A00), "✨"),
    TierVisual("normal", "Normal", 70, "7% – 11% OFF",
        listOf(Color(0xFFC62828), Color(0xFFEF5350)), Color(0xFFFFF1F1), "🎟️")
)

@Composable
fun StoreScreen(
    onBack: () -> Unit,
    onSpin: (tier: String, kind: String, siteId: Int) -> Unit,
    onOpenCoupons: () -> Unit
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val gems by GamificationRepository.gems.collectAsState()
    var kind by remember { mutableStateOf("hotel") }
    val siteId = ActiveSiteManager.activeSiteId ?: -1

    LaunchedEffect(Unit) { GamificationRepository.refresh() }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)))
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(56.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tokens.surface)
                        .border(0.8.dp, tokens.border, RoundedCornerShape(14.dp)).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tokens.textPrimary, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Coupon Store", color = tokens.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("Spin your gems into real discounts", color = tokens.textSecondary, fontSize = 13.sp)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(accent.tint).padding(horizontal = 12.dp, vertical = 8.dp)
                ) { Text("💎 $gems", color = accent.dark, fontSize = 14.sp, fontWeight = FontWeight.Black) }
            }

            Spacer(Modifier.height(20.dp))

            // Accommodation kind toggle (which kind of partner the wheel picks from)
            SectionLabel("Where do you want to redeem?")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                KindChip("🏨 Hotels", kind == "hotel", Modifier.weight(1f)) { kind = "hotel" }
                KindChip("🍽️ Restaurants", kind == "restaurant", Modifier.weight(1f)) { kind = "restaurant" }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel("Pick a coupon to spin for")
            Spacer(Modifier.height(8.dp))

            TIER_VISUALS.forEach { tv ->
                val affordable = gems >= tv.price
                TierCard(tv, affordable) { if (affordable) onSpin(tv.tier, kind, siteId) }
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(tokens.surface)
                    .border(0.8.dp, tokens.border, RoundedCornerShape(16.dp)).clickable { onOpenCoupons() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🎫  My Coupons", color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.tint else tokens.surface)
            .border(1.dp, if (selected) accent.primary else tokens.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) accent.dark else tokens.textSecondary,
            fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun TierCard(tv: TierVisual, affordable: Boolean, onClick: () -> Unit) {
    val tokens = LocalAppColors.current
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(tv.gradient))
            .then(if (affordable) Modifier else Modifier.background(Color(0x66000000)))
            .clickable(enabled = affordable, onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tv.emoji, fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tv.title, color = tv.textColor, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(tv.discountText, color = tv.textColor.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("💎 ${tv.price}", color = tv.textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(if (affordable) "Tap to spin" else "Need more gems",
                        color = tv.textColor.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }
    }
}
