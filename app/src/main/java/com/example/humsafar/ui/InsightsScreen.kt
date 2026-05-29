package com.example.humsafar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.data.StatsRepository
import com.example.humsafar.models.DailyVisit
import com.example.humsafar.models.NodePopularity
import com.example.humsafar.models.SiteInsights
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors

@Composable
fun InsightsScreen(
    siteId: Int,
    siteName: String,
    onBack: () -> Unit,
    vm: InsightsViewModel = viewModel()
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val state by vm.uiState.collectAsState()
    val live by StatsRepository.stats.collectAsState()

    LaunchedEffect(siteId) {
        vm.loadSite(siteId)
        StatsRepository.refresh()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(56.dp))

            // ── Header ────────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.surface)
                        .border(0.8.dp, tokens.border, RoundedCornerShape(14.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Insights", color = tokens.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text(siteName, color = tokens.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Light)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Live community stats ───────────────────────────────────────────
            LiveStatsCard(active = live.activeUsers, lifetime = live.lifetimeVisits, total = live.totalUsers)

            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is InsightsUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent.primary)
                    }
                }
                is InsightsUiState.Error -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            s.message,
                            color = tokens.textSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                }
                is InsightsUiState.Ready -> SiteInsightsContent(s.site)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SiteInsightsContent(site: SiteInsights) {
    MlHighlightCard(site)
    Spacer(Modifier.height(16.dp))

    SectionLabel("Visitors · last 14 days")
    Spacer(Modifier.height(8.dp))
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            VisitsBarChart(site.dailyVisits)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(site.dailyVisits.firstOrNull()?.date ?: "", color = LocalAppColors.current.textTertiary, fontSize = 11.sp)
                Text(site.dailyVisits.lastOrNull()?.date ?: "", color = LocalAppColors.current.textTertiary, fontSize = 11.sp)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionLabel("Key metrics")
    Spacer(Modifier.height(8.dp))

    val metrics = listOf(
        Triple("Total visits", site.totalVisits.toString(), "👣"),
        Triple("Unique visitors", site.uniqueVisitors.toString(), "🧭"),
        Triple("Avg. time", "${site.avgDurationMins.toInt()} min", "⏱️"),
        Triple("Avg. spots", "${site.avgNodesCompleted}", "📍"),
        Triple("Completion", "${site.completionRate.toInt()}%", "✅"),
        Triple("Interactions", site.totalInteractions.toString(), "💬"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (label, value, icon) ->
                    MetricCard(label, value, icon, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    SectionLabel("Per-spot insights")
    Spacer(Modifier.height(8.dp))

    val maxVisits = (site.nodePopularity.maxOfOrNull { it.visits } ?: 0).coerceAtLeast(1)
    if (site.nodePopularity.isEmpty()) {
        Text("No spots recorded yet.", color = LocalAppColors.current.textTertiary, fontSize = 13.sp)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            site.nodePopularity.forEach { node -> NodePopularityRow(node, maxVisits) }
        }
    }
}

@Composable
private fun LiveStatsCard(active: Int, lifetime: Int, total: Int) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = accent.tint.copy(alpha = 0.5f)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatPill("$active", "online now", accent.dark)
            StatDivider()
            StatPill("$lifetime", "total visits", tokens.textPrimary)
            StatDivider()
            StatPill("$total", "explorers", tokens.textPrimary)
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(label, color = LocalAppColors.current.textTertiary, fontSize = 11.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.height(34.dp).width(0.8.dp).background(LocalAppColors.current.border))
}

@Composable
private fun MlHighlightCard(site: SiteInsights) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val ml = site.ml
    val trendIcon = when (ml.visitsTrend) {
        "rising" -> "▲"
        "falling" -> "▼"
        else -> "▬"
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("AI insight")
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(accent.tint).padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("$trendIcon ${ml.visitsTrend}", color = accent.dark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EngagementDial(ml.engagementScore.toFloat())
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Engagement", color = tokens.textSecondary, fontSize = 12.sp)
                    Text("${ml.engagementScore.toInt()}/100", color = tokens.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(ml.insightText, color = tokens.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("Tomorrow", "~${ml.predictedVisitsNextDay}", "visits", Modifier.weight(1f))
                MiniStat("Per extra spot", "+${ml.minsPerExtraNode}", "min", Modifier.weight(1f))
                MiniStat("Full tour", "${ml.predictedFullDurationMins.toInt()}", "min", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Trained on ${ml.trainedOn} visit${if (ml.trainedOn == 1) "" else "s"} · ${ml.model}",
                color = tokens.textTertiary, fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EngagementDial(score: Float) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(
                color = tokens.border,
                startAngle = 130f, sweepAngle = 280f, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = accent.primary,
                startAngle = 130f, sweepAngle = 280f * (score / 100f), useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Text("${score.toInt()}", color = tokens.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MiniStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    val tokens = LocalAppColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.surfaceMuted)
            .padding(vertical = 12.dp, horizontal = 10.dp)
    ) {
        Text(label, color = tokens.textTertiary, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(3.dp))
            Text(unit, color = tokens.textTertiary, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
    val tokens = LocalAppColors.current
    GlassCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(value, color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, color = tokens.textTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NodePopularityRow(node: NodePopularity, maxVisits: Int) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(node.name, color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${node.visits} visits", color = tokens.textSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(tokens.surfaceMuted)
            ) {
                val frac = (node.visits.toFloat() / maxVisits).coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(accent.primary, accent.dark)))
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("⭐ ${node.avgRating} (${node.ratingCount})", color = tokens.textTertiary, fontSize = 11.sp)
                Text("⚡ ${node.engagementScore.toInt()}/100", color = tokens.textTertiary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun VisitsBarChart(data: List<DailyVisit>) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    val maxCount = (data.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val n = data.size
        if (n == 0) return@Canvas
        val gap = 5.dp.toPx()
        val barW = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        val usableH = size.height - 4.dp.toPx()
        data.forEachIndexed { i, d ->
            val x = i * (barW + gap)
            // track
            drawRoundRect(
                color = tokens.surfaceMuted,
                topLeft = Offset(x, 0f),
                size = Size(barW, size.height),
                cornerRadius = CornerRadius(4f, 4f)
            )
            val h = (d.count / maxCount.toFloat()) * usableH
            if (h > 0f) {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(accent.primary, accent.dark)),
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}
