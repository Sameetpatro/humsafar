package com.example.humsafar.ui.bonus

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.sqrt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.data.BonusGameManager
import com.example.humsafar.data.BonusOffer
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class BonusDisplayPhase { Banner, Bubble, Expanded }

/**
 * Bonus challenge UI: full top banner (2 s) → shrinks to draggable bubble with live
 * timer → tap bubble to expand details (site, node, prize, minigame).
 */
@Composable
fun BonusGameHost() {
    val offer by BonusGameManager.offer.collectAsState()
    val playing by BonusGameManager.playing.collectAsState()
    val result by BonusGameManager.result.collectAsState()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(250)
        }
    }

    var phase by remember { mutableStateOf(BonusDisplayPhase.Banner) }
    var trackedChallengeId by remember { mutableIntStateOf(-1) }

    // New offer → show banner, then auto-shrink to bubble after 2 seconds.
    LaunchedEffect(offer?.challengeId) {
        val id = offer?.challengeId ?: return@LaunchedEffect
        if (id != trackedChallengeId) {
            trackedChallengeId = id
            phase = BonusDisplayPhase.Banner
            delay(2_000)
            if (BonusGameManager.offer.value?.challengeId == id && !BonusGameManager.playing.value) {
                phase = BonusDisplayPhase.Bubble
            }
        }
    }

    LaunchedEffect(offer, nowMs) {
        offer?.let { if (nowMs > it.expiresAtMs && !playing) BonusGameManager.dismissOffer() }
    }

    if (offer == null) {
        trackedChallengeId = -1
        phase = BonusDisplayPhase.Banner
    }

    Box(Modifier.fillMaxSize()) {
        val o = offer
        if (o != null && !playing && result == null) {
            AnimatedContent(
                targetState = phase,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (targetState == BonusDisplayPhase.Banner) {
                        fadeIn(tween(300)) + slideInVertically(tween(350)) { -it } togetherWith
                            fadeOut(tween(200))
                    } else if (initialState == BonusDisplayPhase.Banner) {
                        fadeIn(tween(350)) + scaleIn(
                            initialScale = 0.35f,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium)
                        ) togetherWith shrinkOut(
                            shrinkTowards = Alignment.TopEnd,
                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                        ) + fadeOut(tween(300))
                    } else {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    }
                },
                label = "bonusPhase"
            ) { currentPhase ->
                when (currentPhase) {
                    BonusDisplayPhase.Banner -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            BonusTopBanner(
                                offer = o,
                                nowMs = nowMs,
                                onDismiss = { BonusGameManager.dismissOffer() }
                            )
                        }
                    }
                    BonusDisplayPhase.Bubble, BonusDisplayPhase.Expanded -> {
                        DraggableBonusBubble(
                            offer = o,
                            nowMs = nowMs,
                            highlighted = currentPhase == BonusDisplayPhase.Expanded,
                            onTap = { phase = BonusDisplayPhase.Expanded }
                        )
                    }
                }
            }

            if (phase == BonusDisplayPhase.Expanded) {
                BonusExpandedPanel(
                    offer = o,
                    nowMs = nowMs,
                    onCollapse = { phase = BonusDisplayPhase.Bubble },
                    onDismiss = { BonusGameManager.dismissOffer() }
                )
            }
        }

        if (playing && offer != null) {
            MinigameOverlay(
                minigame = offer!!.minigame,
                onSolved = { BonusGameManager.submitSolved() },
                onClose = { BonusGameManager.cancelPlaying() }
            )
        }

        if (result != null) {
            ResultOverlay(
                rewardGems = result!!.rewardGems,
                message = result!!.message,
                onDismiss = { BonusGameManager.dismissResult() }
            )
        }
    }
}

/** Original-style full-width banner at the top. */
@Composable
private fun BonusTopBanner(offer: BonusOffer, nowMs: Long, onDismiss: () -> Unit) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val timeText = formatRemaining(offer.expiresAtMs, nowMs)

    Box(Modifier.statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
        GlassCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎁", fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Surprise bonus game!",
                        color = tokens.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Reach & scan \"${offer.targetNodeName}\" within $timeText to play.",
                        color = tokens.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(accent.tint)
                        .clickable { onDismiss() }.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("✕", color = accent.dark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Expanded detail card when the bubble is tapped. */
@Composable
private fun BonusExpandedPanel(
    offer: BonusOffer,
    nowMs: Long,
    onCollapse: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val timeText = formatRemaining(offer.expiresAtMs, nowMs)
    val gameLabel = offer.minigame.replaceFirstChar { it.uppercase() }
    val prizeText = if (offer.rewardGems > 0) "${offer.rewardGems} gems" else "100–200 gems"

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x44000000))
            .clickable { onCollapse() },
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(220)) + expandVertically(tween(320)) { it / 2 },
            exit = fadeOut(tween(180)) + shrinkVertically()
        ) {
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 72.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎁", fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Bonus Challenge",
                                color = tokens.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Time left: $timeText",
                                color = accent.dark,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            Modifier.clip(RoundedCornerShape(50)).background(accent.tint)
                                .clickable { onCollapse() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Minimize", color = accent.dark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    DetailRow("📍 Location", offer.siteName.ifBlank { "Current heritage site" })
                    DetailRow("🎯 Scan this node", offer.targetNodeName)
                    DetailRow("💎 Bonus prize", prizeText)
                    DetailRow("🎮 Mini-game", gameLabel)
                    DetailRow("⏱ Deadline", "$timeText remaining")

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan the QR at the target node before time runs out to unlock the mini-game. " +
                            "Solve it to claim your bonus gems!",
                        color = tokens.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(16.dp))
                    GlassPrimaryButton(text = "Got it!", onClick = onCollapse, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Dismiss challenge", color = tokens.textTertiary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val tokens = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = tokens.textTertiary, fontSize = 13.sp, modifier = Modifier.weight(0.45f))
        Text(
            value,
            color = tokens.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun DraggableBonusBubble(
    offer: BonusOffer,
    nowMs: Long,
    highlighted: Boolean,
    onTap: () -> Unit
) {
    val accent = LocalAccent.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val maxX = with(density) { (config.screenWidthDp.dp - 72.dp).toPx() }
    val maxY = with(density) { (config.screenHeightDp.dp - 120.dp).toPx() }

    var offsetX by remember(offer.challengeId) { mutableFloatStateOf(maxX - 16f) }
    var offsetY by remember(offer.challengeId) { mutableFloatStateOf(with(density) { 100.dp.toPx() }) }

    val timeText = formatRemaining(offer.expiresAtMs, nowMs)

    val pulse by rememberInfiniteTransition(label = "bonusPulse").animateFloat(
        initialValue = 1f,
        targetValue = if (highlighted) 1.12f else 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    val enterScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "bubbleEnter"
    )

    val dragSlopPx = with(density) { 12.dp.toPx() }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .scale(enterScale * pulse)
                .pointerInput(offer.challengeId) {
                    var dragAccum = Offset.Zero
                    detectDragGestures(
                        onDragStart = { dragAccum = Offset.Zero },
                        onDragEnd = {
                            if (sqrt(dragAccum.x * dragAccum.x + dragAccum.y * dragAccum.y) < dragSlopPx) {
                                onTap()
                            }
                            dragAccum = Offset.Zero
                        },
                        onDragCancel = { dragAccum = Offset.Zero },
                        onDrag = { change, drag ->
                            change.consume()
                            dragAccum += drag
                            offsetX = (offsetX + drag.x).coerceIn(0f, maxX)
                            offsetY = (offsetY + drag.y).coerceIn(0f, maxY)
                        }
                    )
                }
                .size(68.dp)
                .shadow(if (highlighted) 16.dp else 10.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent.primary, accent.dark)))
                .border(
                    width = if (highlighted) 2.5.dp else 2.dp,
                    color = Color.White.copy(alpha = if (highlighted) 0.85f else 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎁", fontSize = 20.sp)
                Text(
                    timeText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun formatRemaining(expiresAtMs: Long, nowMs: Long): String {
    val remaining = ((expiresAtMs - nowMs) / 1000).coerceAtLeast(0)
    val mm = remaining / 60
    val ss = remaining % 60
    return "%d:%02d".format(mm, ss)
}

@Composable
private fun MinigameOverlay(minigame: String, onSolved: () -> Unit, onClose: () -> Unit) {
    val tokens = LocalAppColors.current
    Box(
        Modifier.fillMaxSize().background(Color(0xE6000000)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Text("Bonus Challenge", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Solve it to win bonus gems!", color = Color(0xFFCFCFCF), fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Box(Modifier.padding(20.dp)) {
                    when (minigame) {
                        "sudoku" -> SudokuGameScreen(onSolved = onSolved)
                        else -> ZipGameScreen(onSolved = onSolved)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.clip(RoundedCornerShape(14.dp)).background(tokens.surfaceMuted)
                    .clickable { onClose() }.padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text("Give up", color = tokens.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ResultOverlay(rewardGems: Int, message: String, onDismiss: () -> Unit) {
    val tokens = LocalAppColors.current
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        GlassCard(Modifier.fillMaxWidth().padding(28.dp)) {
            Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (rewardGems > 0) "🎉" else "⌛", fontSize = 44.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    if (rewardGems > 0) "Bonus won!" else "Challenge over",
                    color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Text(message, color = tokens.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                GlassPrimaryButton(text = "Awesome", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
