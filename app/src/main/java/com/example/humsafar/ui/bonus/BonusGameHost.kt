package com.example.humsafar.ui.bonus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.data.BonusGameManager
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * App-wide overlay for the surprise bonus challenge: shows the offer banner,
 * the minigame when the target node is scanned in time, and the reward result.
 * Mounted once over the whole NavHost.
 */
@Composable
fun BonusGameHost() {
    val offer by BonusGameManager.offer.collectAsState()
    val playing by BonusGameManager.playing.collectAsState()
    val result by BonusGameManager.result.collectAsState()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { nowMs = System.currentTimeMillis(); delay(1000) }
    }
    // Auto-expire the banner.
    LaunchedEffect(offer, nowMs) {
        offer?.let { if (nowMs > it.expiresAtMs && !playing) BonusGameManager.dismissOffer() }
    }

    Box(Modifier.fillMaxSize()) {
        // ── Offer banner ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = offer != null && !playing && result == null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            offer?.let { o ->
                val remaining = ((o.expiresAtMs - nowMs) / 1000).coerceAtLeast(0)
                val mm = remaining / 60
                val ss = remaining % 60
                Box(Modifier.statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    BonusBanner(
                        targetName = o.targetNodeName,
                        timeText = "%d:%02d".format(mm, ss),
                        onDismiss = { BonusGameManager.dismissOffer() }
                    )
                }
            }
        }

        // ── Minigame overlay ──────────────────────────────────────────────
        if (playing && offer != null) {
            MinigameOverlay(
                minigame = offer!!.minigame,
                onSolved = { BonusGameManager.submitSolved() },
                onClose = { BonusGameManager.cancelPlaying() }
            )
        }

        // ── Reward result ─────────────────────────────────────────────────
        if (result != null) {
            ResultOverlay(
                rewardGems = result!!.rewardGems,
                message = result!!.message,
                onDismiss = { BonusGameManager.dismissResult() }
            )
        }
    }
}

@Composable
private fun BonusBanner(targetName: String, timeText: String, onDismiss: () -> Unit) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎁", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Surprise bonus game!", color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text("Reach & scan \"$targetName\" within $timeText to play.",
                    color = tokens.textSecondary, fontSize = 12.sp)
            }
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(accent.tint)
                    .clickable { onDismiss() }.padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("✕", color = accent.dark, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
    }
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
            ) { Text("Give up", color = tokens.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
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
                Text(if (rewardGems > 0) "Bonus won!" else "Challenge over",
                    color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(message, color = tokens.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                GlassPrimaryButton(text = "Awesome", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
