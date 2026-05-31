package com.example.humsafar.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

private val WHEEL_COLORS = listOf(
    Color(0xFF6D28D9), Color(0xFFEC4899), Color(0xFFF59E0B),
    Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFEF4444),
    Color(0xFF14B8A6), Color(0xFF8B5CF6)
)

@Composable
fun SpinWheelScreen(
    tier: String,
    kind: String,
    siteId: Int,
    onBack: () -> Unit,
    onViewCoupons: () -> Unit,
    vm: StoreViewModel = viewModel()
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val state by vm.spin.collectAsState()

    DisposableEffect(Unit) { onDispose { vm.reset() } }

    var revealCoupon by remember { mutableStateOf(false) }
    val result = state as? SpinState.Result
    LaunchedEffect(result) {
        if (result != null) {
            revealCoupon = false
            delay(2700)   // let both wheels finish spinning before revealing
            revealCoupon = true
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)))
    ) {
        AnimatedOrbBackground(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text(Tiers.label[tier] ?: "Coupon", color = tokens.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Spin to reveal your ${if (kind == "hotel") "hotel" else "restaurant"} & discount",
                color = tokens.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))

            when (val s = state) {
                is SpinState.Error -> ResultCard("Couldn't spin", s.message, "😕", "Back", onBack)
                else -> {
                    val partnerLabels = result?.partnerLabels ?: List(6) { "" }
                    val partnerIdx = result?.partnerIndex ?: -1
                    val discountOptions = result?.discountOptions ?: (1..8).toList()
                    val discountIdx = result?.discountIndex ?: -1

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        WheelColumn(
                            caption = if (kind == "hotel") "🏨 Hotel" else "🍽️ Place",
                            segmentCount = partnerLabels.size.coerceAtLeast(2),
                            targetIndex = partnerIdx,
                            modifier = Modifier.weight(1f)
                        )
                        WheelColumn(
                            caption = "% Discount",
                            segmentCount = discountOptions.size.coerceAtLeast(2),
                            targetIndex = discountIdx,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    if (s is SpinState.Idle) {
                        GlassPrimaryButton(
                            text = "SPIN  •  💎 ${Tiers.price[tier] ?: 0}",
                            onClick = { vm.spin(tier, kind, siteId.takeIf { it > 0 }) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (s is SpinState.Spinning || (result != null && !revealCoupon)) {
                        Text("Spinning…", color = accent.dark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    if (result != null && revealCoupon) {
                        Spacer(Modifier.height(4.dp))
                        CouponRevealCard(result, onViewCoupons, onBack)
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelColumn(caption: String, segmentCount: Int, targetIndex: Int, modifier: Modifier = Modifier) {
    val tokens = LocalAppColors.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SpinningWheel(segmentCount = segmentCount, targetIndex = targetIndex)
        Spacer(Modifier.height(10.dp))
        Text(caption, color = tokens.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SpinningWheel(segmentCount: Int, targetIndex: Int) {
    val accent = LocalAccent.current
    val seg = 360f / segmentCount

    // Land the target segment's center under the top pointer; 0 until we have a target.
    val targetRotation = if (targetIndex >= 0) 360f * 5 - (targetIndex * seg + seg / 2f) else 0f
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = if (targetIndex >= 0) 2500 else 0),
        label = "wheel"
    )

    Box(contentAlignment = Alignment.TopCenter) {
        Canvas(
            Modifier
                .size(150.dp)
                .rotate(rotation)
        ) {
            val d = size.minDimension
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val arcSize = Size(d, d)
            for (i in 0 until segmentCount) {
                drawArc(
                    color = WHEEL_COLORS[i % WHEEL_COLORS.size],
                    startAngle = -90f + i * seg,
                    sweepAngle = seg,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
            }
        }
        // Pointer at the top.
        Canvas(Modifier.size(width = 22.dp, height = 16.dp)) {
            val p = Path().apply {
                moveTo(size.width / 2f, size.height)
                lineTo(0f, 0f)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(p, color = accent.dark)
        }
    }
}

@Composable
private fun CouponRevealCard(result: SpinState.Result, onViewCoupons: () -> Unit, onBack: () -> Unit) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val c = result.coupon
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉 You won!", color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("${c.discountPct}% OFF", color = accent.dark, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("at ${c.partnerName}", color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(accent.tint).padding(horizontal = 16.dp, vertical = 8.dp)
            ) { Text("Code: ${c.code}", color = accent.dark, fontSize = 15.sp, fontWeight = FontWeight.Black) }
            Spacer(Modifier.height(8.dp))
            Text("Redeem before the timer runs out — see My Coupons.",
                color = tokens.textSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            GlassPrimaryButton(text = "View My Coupons", onClick = onViewCoupons, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tokens.surfaceMuted)
                    .clickable { onBack() }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("Back to store", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun ResultCard(title: String, body: String, emoji: String, cta: String, onCta: () -> Unit) {
    val tokens = LocalAppColors.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emoji, fontSize = 40.sp)
                Spacer(Modifier.height(10.dp))
                Text(title, color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(body, color = tokens.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(20.dp))
        GlassPrimaryButton(text = cta, onClick = onCta, modifier = Modifier.fillMaxWidth())
    }
}
