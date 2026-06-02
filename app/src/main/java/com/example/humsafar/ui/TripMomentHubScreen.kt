package com.example.humsafar.ui

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.data.QuizPrepareManager
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * Heart of the post-trip flow — celebrate the journey, tease memory sharing,
 * and let the quiz generate in the background before Continue.
 */
@Composable
fun TripMomentHubScreen(
    tripId: Int,
    siteName: String,
    visitedCount: Int,
    totalCount: Int,
    onContinue: () -> Unit,
    onSkipQuiz: () -> Unit
) {
    val context = LocalContext.current
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current

    LaunchedEffect(tripId) { QuizPrepareManager.preloadStart(tripId) }

    var quizReady by remember { mutableStateOf(QuizPrepareManager.isReady(tripId)) }
    LaunchedEffect(tripId) {
        while (!quizReady) {
            quizReady = QuizPrepareManager.isReady(tripId)
            delay(400)
        }
    }

    val shimmer = rememberInfiniteTransition(label = "hubShimmer")
    val shimmerOffset by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerOff"
    )
    val pulse by shimmer.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        // Soft radial glow behind hero
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.primary.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.18f),
                    radius = size.width * 0.75f
                ),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.5f, size.height * 0.18f)
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
            ) {
                Spacer(Modifier.height(28.dp))

                // ── Hero ──────────────────────────────────────────────────
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(96.dp)
                                .scale(pulse)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(accent.primary.copy(0.35f), Color.Transparent)
                                    )
                                )
                        )
                        Text("✨", fontSize = 52.sp, modifier = Modifier.scale(pulse))
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Journey Complete",
                        color = accent.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        siteName,
                        color = tokens.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "You explored $visitedCount of $totalCount spots — " +
                            "your story at this heritage site is worth remembering.",
                        color = tokens.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Progress ribbon
                HubStatsRibbon(visitedCount, totalCount, accent.primary)

                Spacer(Modifier.height(28.dp))

                Text(
                    "What's next?",
                    color = tokens.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Take a moment before the quiz — your memories matter here.",
                    color = tokens.textTertiary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(16.dp))

                // ── Share your moments ────────────────────────────────────
                HubActionCard(
                    title = "Share Your Moments",
                    subtitle = "Photo · video · message",
                    description = "After completing your journey, share your experience and " +
                        "be featured in the official memory countdown of this site's history.",
                    icon = { Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(26.dp)) },
                    gradient = listOf(Color(0xFF6B2D8B), Color(0xFFE91E8C), Color(0xFFFF6B9D)),
                    badge = "COMING SOON",
                    shimmerOffset = shimmerOffset,
                    onClick = {
                        Toast.makeText(
                            context,
                            "Share Your Moments is coming soon — stay tuned!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )

                Spacer(Modifier.height(16.dp))

                // ── Play quiz ─────────────────────────────────────────────
                HubActionCard(
                    title = "Play Quiz to Earn Gems",
                    subtitle = "Heritage knowledge challenge",
                    description = "Answer questions about the spots you visited. Faster correct " +
                        "answers earn more gems — use them in the coupon store for real discounts.",
                    icon = { Icon(Icons.Default.Diamond, null, tint = Color(0xFFFFF8E1), modifier = Modifier.size(26.dp)) },
                    gradient = listOf(Color(0xFF1A237E), Color(0xFF3949AB), Color(0xFF7C4DFF)),
                    badge = null,
                    shimmerOffset = shimmerOffset,
                    trailingContent = {
                        if (!quizReady) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = Color.White.copy(0.9f),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Preparing quiz…",
                                    color = Color.White.copy(0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ready!", color = Color(0xFFFFD54F), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    onClick = onContinue
                )

                Spacer(Modifier.height(24.dp))
            }

            // ── Bottom CTA ────────────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 20.dp)
            ) {
                GlassPrimaryButton(
                    text = if (quizReady) "Continue to Quiz" else "Continue to Quiz  ·  preparing…",
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Quiz is built as you explore — usually ready instantly.",
                    color = tokens.textTertiary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, tokens.textTertiary.copy(0.25f), RoundedCornerShape(14.dp))
                        .clickable { onSkipQuiz() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Skip the quiz and continue",
                        color = tokens.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HubStatsRibbon(visited: Int, total: Int, accentColor: Color) {
    val pct = if (total > 0) visited.toFloat() / total else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accentColor.copy(0.15f), Color(0x22FFFFFF), accentColor.copy(0.1f))
                )
            )
            .border(1.dp, accentColor.copy(0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Spots visited", color = LocalAppColors.current.textSecondary, fontSize = 12.sp)
                Text(
                    "$visited / $total",
                    color = LocalAppColors.current.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(0.08f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(pct.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(0.6f))))
                )
            }
        }
    }
}

@Composable
private fun HubActionCard(
    title: String,
    subtitle: String,
    description: String,
    icon: @Composable () -> Unit,
    gradient: List<Color>,
    badge: String?,
    shimmerOffset: Float,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.linearGradient(gradient))
        )
        // Shimmer sweep
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val x = (shimmerOffset * (w + 200f)) - 100f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(0.12f), Color.Transparent),
                    start = Offset(x, 0f),
                    end = Offset(x + 120f, size.height)
                )
            )
        }
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.18f))
                        .border(1.dp, Color.White.copy(0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { icon() }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    if (badge != null) {
                        Text(
                            badge,
                            color = Color.White.copy(0.75f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = Color.White.copy(0.8f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                description,
                color = Color.White.copy(0.88f),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            if (trailingContent != null) {
                Spacer(Modifier.height(12.dp))
                trailingContent()
            }
        }
    }
}
