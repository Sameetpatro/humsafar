// app/src/main/java/com/example/humsafar/ui/HistoryScreen.kt
//
// Visit history — every completed trip the user has finished.
// Source: GET /reviews/users/{firebase_uid}/history (backend table user_visit_history)
//
// Each row shows:
//   • Site name + visit date
//   • Duration (minutes)
//   • Nodes-completed badge (X / Y)
//   • Review status — green tick if submitted, "Add Review" CTA otherwise
// Tap a row to expand details (raw stats + jump-back-to-site / submit-review actions).
// Pull-to-refresh + empty/error/not-logged-in states.

package com.example.humsafar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.models.VisitHistoryItem
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.*

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onWriteReview: (tripId: Int, siteId: Int, siteName: String, visited: Int, total: Int) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val state      by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.load() }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {

            HistoryHeader(
                onBack       = onBack,
                onRefresh    = { viewModel.refresh() },
                refreshing   = refreshing,
                count        = (state as? HistoryUiState.Ready)?.items?.size ?: 0
            )

            when (val s = state) {
                is HistoryUiState.Loading     -> LoadingPanel()
                is HistoryUiState.NotLoggedIn -> NotLoggedInPanel()
                is HistoryUiState.Empty       -> EmptyHistoryPanel()
                is HistoryUiState.Error       -> ErrorPanel(message = s.message, onRetry = viewModel::refresh)
                is HistoryUiState.Ready       -> HistoryList(
                    items = s.items,
                    onWriteReview = onWriteReview
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryHeader(
    onBack:     () -> Unit,
    onRefresh:  () -> Unit,
    refreshing: Boolean,
    count:      Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xF0050D1A), Color(0xBB050D1A))))
            .drawBehind {
                drawLine(GlassBorder, Offset(0f, size.height), Offset(size.width, size.height), 0.5.dp.toPx())
            }
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
                    tint     = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("My Heritage Journey", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (count > 0) {
                    Text(
                        "$count site${if (count == 1) "" else "s"} explored",
                        color = TextTertiary, fontSize = 12.sp
                    )
                }
            }

            val spinAngle by rememberInfiniteTransition(label = "rf").animateFloat(
                0f, 360f,
                infiniteRepeatable(tween(800, easing = LinearEasing)),
                label = "spin"
            )
            Box(
                modifier = Modifier
                    .size(40.dp).clip(CircleShape)
                    .background(GlassWhite15)
                    .border(0.5.dp, GlassBorder, CircleShape)
                    .clickable(enabled = !refreshing) { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Refresh, null,
                    tint = if (refreshing) AccentYellow else TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .then(if (refreshing) Modifier.rotate(spinAngle) else Modifier)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// List & list row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryList(
    items: List<VisitHistoryItem>,
    onWriteReview: (Int, Int, String, Int, Int) -> Unit
) {
    var expanded by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionLabel("Recent Visits")
            Spacer(Modifier.height(8.dp))
        }
        items(items, key = { it.id }) { item ->
            HistoryRow(
                item     = item,
                expanded = expanded == item.id,
                onToggle = { expanded = if (expanded == item.id) null else item.id },
                onWriteReview = {
                    val tripId = item.tripId ?: 0
                    if (tripId != 0) {
                        onWriteReview(tripId, item.siteId, item.siteName, item.nodesCompleted, item.totalNodes)
                    }
                }
            )
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun HistoryRow(
    item:           VisitHistoryItem,
    expanded:       Boolean,
    onToggle:       () -> Unit,
    onWriteReview:  () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, tint = GlassWhite10) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Site icon
                Box(
                    modifier = Modifier
                        .size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(
                            if (item.reviewSubmitted)
                                Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                            else
                                Brush.linearGradient(listOf(Color(0xFF1A2D52), Color(0xFF0E1C36)))
                        )
                        .border(
                            0.7.dp,
                            if (item.reviewSubmitted) Color(0x77FFFFFF) else GlassBorder,
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏛️", fontSize = 22.sp)
                }
                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        item.siteName,
                        color = TextPrimary, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text  = formatDate(item.visitedAt),
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                        item.durationMins?.takeIf { it > 0 }?.let { mins ->
                            Text(
                                "  •  ${formatDuration(mins)}",
                                color    = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                NodesBadge(
                    visited = item.nodesCompleted,
                    total   = item.totalNodes,
                    reviewed = item.reviewSubmitted
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint     = TextTertiary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (expanded) 90f else 0f)
                )
            }

            // Expanded details
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(tween(220)) + fadeIn(tween(220)),
                exit    = shrinkVertically(tween(180)) + fadeOut(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(0.5.dp).background(GlassBorder)
                            .padding(bottom = 12.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    DetailStat(label = "Trip ID",      value = (item.tripId ?: 0).toString())
                    Spacer(Modifier.height(8.dp))
                    DetailStat(label = "Started",      value = formatDateTime(item.visitedAt))
                    Spacer(Modifier.height(8.dp))
                    DetailStat(label = "Ended",        value = formatDateTime(item.endedAt))
                    Spacer(Modifier.height(8.dp))
                    DetailStat(
                        label = "Nodes visited",
                        value = "${item.nodesCompleted} of ${item.totalNodes}"
                    )

                    Spacer(Modifier.height(16.dp))

                    if (item.reviewSubmitted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x224ADE80))
                                .border(0.7.dp, Color(0x554ADE80), RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp, horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, null,
                                tint     = Color(0xFF4ADE80),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "You've reviewed this site — thanks!",
                                color    = Color(0xFFB6F5C9),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if ((item.tripId ?: 0) != 0) {
                        GlassPrimaryButton(
                            text     = "Write a review",
                            onClick  = onWriteReview,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Old/incomplete trip — no server trip_id, can't review
                        Text(
                            "Reviews can only be added for trips started by scanning the entry QR.",
                            color    = TextTertiary,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodesBadge(visited: Int, total: Int, reviewed: Boolean) {
    val (bg, fg, border) = when {
        total <= 0                -> Triple(GlassWhite10, TextTertiary, GlassBorder)
        visited >= total          -> Triple(Color(0x224ADE80), Color(0xFFB6F5C9), Color(0x554ADE80))
        else                      -> Triple(Color(0x222196F3), Color(0xFF8AC7FF), Color(0x552196F3))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(0.7.dp, border, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (reviewed) {
                Icon(
                    Icons.Default.Star, null,
                    tint     = AccentYellow,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = if (total <= 0) "—" else "$visited/$total",
                color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextTertiary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State panels
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentYellow, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(14.dp))
            Text("Loading your journey…", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun NotLoggedInPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔒", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text("Sign in required", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in with Google or email to keep a record of every heritage site you visit.",
                color    = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun EmptyHistoryPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val inf = rememberInfiniteTransition(label = "empty")
            val pulse by inf.animateFloat(
                0.92f, 1.05f,
                infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "p"
            )
            Text("🗺️", fontSize = 56.sp, modifier = Modifier.scale(pulse))
            Spacer(Modifier.height(16.dp))
            Text("No visits yet", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Scan a heritage site's entry QR to start your first guided tour. Your journey will appear here.",
                color    = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Could not load your history",
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color    = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            GlassPrimaryButton(text = "Try again", onClick = onRetry)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Date formatting (ISO-8601 → short label)
// ─────────────────────────────────────────────────────────────────────────────

private fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val parser  = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US
        ).apply { isLenient = true }
        // Strip fractional seconds + timezone if present so the parser doesn't choke
        val sanitized = iso.substringBefore('+').substringBefore('.').substringBefore('Z')
        val date    = parser.parse(sanitized) ?: return iso
        val display = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        display.format(date)
    } catch (_: Exception) {
        iso
    }
}

private fun formatDateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val parser  = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US
        ).apply { isLenient = true }
        val sanitized = iso.substringBefore('+').substringBefore('.').substringBefore('Z')
        val date    = parser.parse(sanitized) ?: return iso
        val display = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
        display.format(date)
    } catch (_: Exception) {
        iso
    }
}

private fun formatDuration(mins: Int): String = when {
    mins < 1   -> "<1 min"
    mins < 60  -> "$mins min"
    else       -> {
        val h = mins / 60
        val m = mins % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    }
}
