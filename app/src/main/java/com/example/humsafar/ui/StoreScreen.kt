package com.example.humsafar.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.data.ActiveSiteManager
import com.example.humsafar.data.GamificationRepository
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors

private data class TierVisual(
    val tier: String,
    val title: String,
    val subtitle: String,
    val price: Int,
    val discountText: String,
    val gradient: List<Color>,
    val glow: Color,
    val textColor: Color,
    val badge: String,
    val emoji: String
)

private val TIER_VISUALS = listOf(
    TierVisual(
        tier = "ultimate",
        title = "Ultimate Special",
        subtitle = "Royal treatment · max savings",
        price = 200,
        discountText = "20% – 30% OFF",
        gradient = listOf(Color(0xFF0F0A18), Color(0xFF4C1D95), Color(0xFF7C3AED)),
        glow = Color(0xFF9333EA),
        textColor = Color(0xFFF5E6FF),
        badge = "LEGENDARY",
        emoji = "👑"
    ),
    TierVisual(
        tier = "special",
        title = "Special Coupon",
        subtitle = "Golden pick · great value",
        price = 120,
        discountText = "12% – 19% OFF",
        gradient = listOf(Color(0xFF3D2E00), Color(0xFFB8860B), Color(0xFFFFD54F)),
        glow = Color(0xFFFFB300),
        textColor = Color(0xFFFFF8E1),
        badge = "PREMIUM",
        emoji = "✨"
    ),
    TierVisual(
        tier = "normal",
        title = "Normal Coupon",
        subtitle = "Quick win · easy spin",
        price = 70,
        discountText = "7% – 11% OFF",
        gradient = listOf(Color(0xFF4A0E0E), Color(0xFFC62828), Color(0xFFEF5350)),
        glow = Color(0xFFE53935),
        textColor = Color(0xFFFFF5F5),
        badge = "CLASSIC",
        emoji = "🎟️"
    )
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

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)))
        )
        AnimatedOrbBackground(Modifier.fillMaxSize())

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
                    Text("Coupon Store", color = tokens.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("Spin gems into real discounts ✨", color = tokens.textSecondary, fontSize = 13.sp)
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(Brush.linearGradient(listOf(accent.tint, accent.primary.copy(alpha = 0.35f))))
                        .border(1.dp, accent.primary.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) { Text("💎 $gems", color = accent.dark, fontSize = 14.sp, fontWeight = FontWeight.Black) }
            }

            Spacer(Modifier.height(24.dp))

            SectionLabel("Where do you want to redeem?")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                KindChip("🏨 Hotels", kind == "hotel", Modifier.weight(1f)) { kind = "hotel" }
                KindChip("🍽️ Restaurants", kind == "restaurant", Modifier.weight(1f)) { kind = "restaurant" }
            }

            Spacer(Modifier.height(26.dp))
            SectionLabel("Pick your spin tier")
            Spacer(Modifier.height(12.dp))

            TIER_VISUALS.forEach { tv ->
                val affordable = gems >= tv.price
                FunTierCard(tv, affordable) { if (affordable) onSpin(tv.tier, kind, siteId) }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(listOf(tokens.surface, accent.tint.copy(alpha = 0.25f)))
                    )
                    .border(1.dp, accent.primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                    .clickable { onOpenCoupons() }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎫", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("My Coupons", color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
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
            .shadow(if (selected) 6.dp else 0.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Brush.linearGradient(listOf(accent.tint, accent.primary.copy(alpha = 0.2f))) else Brush.linearGradient(listOf(tokens.surface, tokens.surface)))
            .border(1.5.dp, if (selected) accent.primary else tokens.border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) accent.dark else tokens.textSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun FunTierCard(tv: TierVisual, affordable: Boolean, onClick: () -> Unit) {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "sh"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .scale(if (affordable) shimmer else 1f)
            .shadow(if (affordable) 16.dp else 4.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(tv.gradient))
            .then(
                if (affordable) Modifier.border(1.5.dp, tv.glow.copy(alpha = 0.55f), RoundedCornerShape(26.dp))
                else Modifier
            )
            .clickable(enabled = affordable, onClick = onClick)
            .padding(22.dp)
    ) {
        if (!affordable) {
            Box(
                Modifier.matchParentSize().background(Color(0x77000000))
            )
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text(tv.emoji, fontSize = 28.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(tv.glow.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(tv.badge, color = tv.textColor, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(tv.title, color = tv.textColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(tv.subtitle, color = tv.textColor.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(tv.discountText, color = tv.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (affordable) "Tap to spin the wheel →" else "Need more gems",
                        color = tv.textColor.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("💎 ${tv.price}", color = tv.textColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
