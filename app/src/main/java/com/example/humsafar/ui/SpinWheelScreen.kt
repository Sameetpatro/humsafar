package com.example.humsafar.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.nativeCanvas
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
import kotlin.math.cos
import kotlin.math.sin

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
    LaunchedEffect(tier, kind, siteId) {
        vm.prepare(tier, kind, siteId.takeIf { it > 0 })
    }

    val ready = state as? SpinState.Ready
    val partnerDone = state as? SpinState.PartnerDone
    val complete = state as? SpinState.Complete
    val pending = partnerDone?.pending ?: complete?.pending

    val partnerLabels = ready?.partnerLabels?.takeIf { it.isNotEmpty() }
        ?: pending?.partnerLabels?.takeIf { it.isNotEmpty() }
        ?: listOf("Hotel A", "Hotel B", "Hotel C", "Place D")
    val discountOptions = ready?.discountOptions?.takeIf { it.isNotEmpty() }
        ?: pending?.discountOptions?.takeIf { it.isNotEmpty() }
        ?: (10..15).toList()

    val partnerIdx = when (state) {
        is SpinState.PartnerSpinning -> -1
        is SpinState.PartnerDone -> partnerDone!!.pending.partnerIndex
        is SpinState.DiscountSpinning -> pending?.partnerIndex ?: -1
        is SpinState.Complete -> complete!!.pending.partnerIndex
        else -> -1
    }
    val discountIdx = when (state) {
        is SpinState.DiscountSpinning -> pending?.discountIndex ?: -1
        is SpinState.Complete -> complete!!.pending.discountIndex
        else -> -1
    }

    val selectedKind = ready?.kind ?: pending?.coupon?.partnerType ?: kind
    val gemsPrice = ready?.gemsPrice ?: Tiers.price[tier] ?: 70

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)))
    ) {
        AnimatedOrbBackground(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Text(Tiers.label[tier] ?: "Coupon", color = tokens.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Pick hotel or restaurant, then spin each wheel",
                color = tokens.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))

            when (val s = state) {
                is SpinState.Error -> {
                    ResultCard("Couldn't spin", s.message, "😕", "Back", onBack)
                }
                else -> {
                    // ── Partner kind toggle ─────────────────────────────────
                    SectionCaption("Choose partner type")
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpinKindButton(
                            label = "🏨 Hotel",
                            selected = selectedKind == "hotel",
                            enabled = s is SpinState.Ready,
                            modifier = Modifier.weight(1f)
                        ) { vm.setPartnerKind("hotel") }
                        SpinKindButton(
                            label = "🍽️ Restaurant",
                            selected = selectedKind == "restaurant",
                            enabled = s is SpinState.Ready,
                            modifier = Modifier.weight(1f)
                        ) { vm.setPartnerKind("restaurant") }
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        LabeledWheelColumn(
                            caption = if (selectedKind == "hotel") "🏨 Partner" else "🍽️ Partner",
                            labels = partnerLabels,
                            labelSuffix = "",
                            targetIndex = partnerIdx,
                            spinning = s is SpinState.PartnerSpinning,
                            modifier = Modifier.weight(1f)
                        )
                        LabeledWheelColumn(
                            caption = "💰 Discount",
                            labels = discountOptions.map { "$it%" },
                            labelSuffix = "",
                            targetIndex = discountIdx,
                            spinning = s is SpinState.DiscountSpinning,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    when (s) {
                        is SpinState.Ready -> {
                            GlassPrimaryButton(
                                text = "🎡 Spin ${if (selectedKind == "hotel") "Hotel" else "Restaurant"}  •  💎 $gemsPrice",
                                onClick = { vm.spinPartnerWheel() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = partnerLabels.isNotEmpty()
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(tokens.surfaceMuted).padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Spin discount after partner wheel",
                                    color = tokens.textTertiary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is SpinState.PartnerSpinning -> {
                            Text("Spinning partner…", color = accent.dark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        is SpinState.PartnerDone -> {
                            Text(
                                "You got: ${s.pending.coupon.partnerName}!",
                                color = accent.dark, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            GlassPrimaryButton(
                                text = "🎡 Spin Discount %",
                                onClick = { vm.spinDiscountWheel() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is SpinState.DiscountSpinning -> {
                            Text("Spinning discount…", color = accent.dark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        is SpinState.Complete -> {
                            CouponRevealCard(s.pending, onViewCoupons, onBack)
                        }
                        else -> {}
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    Text(
        text.uppercase(),
        color = LocalAppColors.current.textTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun SpinKindButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.tint else tokens.surface)
            .border(1.5.dp, if (selected) accent.primary else tokens.border, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 14.dp),
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
private fun LabeledWheelColumn(
    caption: String,
    labels: List<String>,
    labelSuffix: String,
    targetIndex: Int,
    spinning: Boolean,
    modifier: Modifier = Modifier
) {
    val tokens = LocalAppColors.current
    val count = labels.size.coerceAtLeast(2)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LabeledSpinningWheel(labels = labels, targetIndex = targetIndex, animate = targetIndex >= 0)
        Spacer(Modifier.height(10.dp))
        Text(caption, color = tokens.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (spinning) {
            Text("…", color = tokens.textTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LabeledSpinningWheel(labels: List<String>, targetIndex: Int, animate: Boolean) {
    val accent = LocalAccent.current
    val segmentCount = labels.size.coerceAtLeast(2)
    val seg = 360f / segmentCount

    val targetRotation = if (animate && targetIndex >= 0) {
        360f * 5 - (targetIndex * seg + seg / 2f)
    } else 0f
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = if (animate) 2500 else 0),
        label = "wheel"
    )

    Box(contentAlignment = Alignment.TopCenter) {
        Canvas(
            Modifier.size(168.dp).rotate(rotation)
        ) {
            val d = size.minDimension
            val cx = size.width / 2f
            val cy = size.height / 2f
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

            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                textSize = when {
                    segmentCount > 6 -> 22f
                    segmentCount > 4 -> 26f
                    else -> 30f
                }
            }

            for (i in 0 until segmentCount) {
                val midAngle = Math.toRadians((-90 + i * seg + seg / 2).toDouble())
                val r = d * 0.32f
                val tx = cx + (cos(midAngle) * r).toFloat()
                val ty = cy + (sin(midAngle) * r).toFloat()
                val label = labels.getOrElse(i) { "?" }
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    rotate((-90 + i * seg + seg / 2).toFloat(), tx, ty)
                    label.split("\n").forEachIndexed { lineIdx, line ->
                        drawText(line, tx, ty + lineIdx * paint.textSize * 0.85f, paint)
                    }
                    restore()
                }
            }
        }
        Canvas(Modifier.size(width = 24.dp, height = 18.dp)) {
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
private fun CouponRevealCard(data: SpinWheelData, onViewCoupons: () -> Unit, onBack: () -> Unit) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val c = data.coupon
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
