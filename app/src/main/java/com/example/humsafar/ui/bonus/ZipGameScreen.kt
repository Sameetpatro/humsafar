package com.example.humsafar.ui.bonus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors

/**
 * Zip-style ordered-path minigame: tap the numbers 1 → 9 in order as fast as
 * possible. A wrong tap resets your progress. Completing the chain wins.
 */
@Composable
fun ZipGameScreen(onSolved: () -> Unit) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current

    val numbers = remember { (1..9).shuffled() }
    var next by remember { mutableIntStateOf(1) }
    var wrongAt by remember { mutableIntStateOf(-1) }

    LaunchedEffect(next) {
        if (next > 9) onSolved()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("⚡ Zip", color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text("Tap 1 → 9 in order. A wrong tap resets you.",
            color = tokens.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Next: ${if (next > 9) "✓" else next}", color = accent.dark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (col in 0 until 3) {
                    val value = numbers[row * 3 + col]
                    val done = value < next
                    val isWrong = wrongAt == value
                    val target by animateColorAsState(
                        when {
                            done -> accent.primary
                            isWrong -> Color(0xFFE05555)
                            else -> tokens.surface
                        }, tween(180), label = "cell"
                    )
                    Box(
                        Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).background(target)
                            .border(1.dp, tokens.border, RoundedCornerShape(16.dp))
                            .clickable(enabled = !done) {
                                if (value == next) { next++; wrongAt = -1 }
                                else { next = 1; wrongAt = value }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$value",
                            color = if (done) accent.onAccent else tokens.textPrimary,
                            fontSize = 26.sp, fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
