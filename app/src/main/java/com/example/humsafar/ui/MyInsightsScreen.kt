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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.models.UserInsights
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors

@Composable
fun MyInsightsScreen(
    onBack: () -> Unit,
    vm: InsightsViewModel = viewModel()
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val state by vm.myState.collectAsState()

    LaunchedEffect(Unit) { vm.loadMyInsights() }

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
                    Text("My Insights", color = tokens.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Text("Your personal heritage footprint", color = tokens.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Light)
                }
            }

            Spacer(Modifier.height(20.dp))

            when (val s = state) {
                is MyInsightsUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent.primary)
                    }
                }
                is MyInsightsUiState.Error -> {
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
                is MyInsightsUiState.Ready -> MyInsightsContent(s.me)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MyInsightsContent(me: UserInsights) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current

    // ── Level + engagement hero ─────────────────────────────────────────────
    GlassCard(modifier = Modifier.fillMaxWidth(), tint = accent.tint.copy(alpha = 0.5f)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EngagementRing(me.engagementScore.toFloat())
                Spacer(Modifier.width(18.dp))
                Column {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(accent.tint).padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(me.explorerLevel, color = accent.dark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Engagement", color = tokens.textSecondary, fontSize = 12.sp)
                    Text("${me.engagementScore.toInt()}/100", color = tokens.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(me.insightText, color = tokens.textSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionLabel("Your numbers")
    Spacer(Modifier.height(8.dp))

    val metrics = listOf(
        Triple("Visits", me.totalVisits.toString(), "👣"),
        Triple("Sites explored", me.sitesExplored.toString(), "🧭"),
        Triple("Time spent", "${me.totalDurationMins} min", "⏱️"),
        Triple("Avg. per visit", "${me.avgDurationMins.toInt()} min", "📈"),
        Triple("Spots seen", me.totalNodesCompleted.toString(), "📍"),
        Triple("Completion", "${me.avgCompletionRate.toInt()}%", "✅"),
        Triple("Chats", me.totalInteractions.toString(), "💬"),
        Triple("Next visit", "~${me.predictedNextDurationMins.toInt()} min", "🔮"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { (label, value, icon) ->
                    MyMetricCard(label, value, icon, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }

    if (!me.favoriteSiteName.isNullOrBlank()) {
        Spacer(Modifier.height(20.dp))
        SectionLabel("Your favourite")
        Spacer(Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⭐", fontSize = 26.sp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Most-visited site", color = tokens.textTertiary, fontSize = 11.sp)
                    Text(me.favoriteSiteName!!, color = tokens.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))
    Text(
        "Computed live from your visit history · linear_regression",
        color = tokens.textTertiary, fontSize = 10.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EngagementRing(score: Float) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(82.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            drawArc(
                color = tokens.border,
                startAngle = 130f, sweepAngle = 280f, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent.primary,
                startAngle = 130f, sweepAngle = 280f * (score / 100f), useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Text("${score.toInt()}", color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MyMetricCard(label: String, value: String, icon: String, modifier: Modifier = Modifier) {
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
