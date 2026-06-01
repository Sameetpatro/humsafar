package com.example.humsafar.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.humsafar.data.TripManager
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors

/**
 * Wraps back navigation during an active trip with a warning that the user will
 * return to the home page while their trip remains active.
 */
@Composable
fun rememberTripSafeBack(onBack: () -> Unit): () -> Unit {
    var showDialog by remember { mutableStateOf(false) }
    val tripActive by TripManager.state.collectAsStateWithLifecycle()
    val enabled = tripActive.isTripActive

    BackHandler(enabled = enabled) { showDialog = true }

    if (showDialog) {
        TripBackWarningDialog(
            onConfirm = {
                showDialog = false
                onBack()
            },
            onDismiss = { showDialog = false }
        )
    }

    return {
        if (enabled) showDialog = true else onBack()
    }
}

@Composable
private fun TripBackWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    Box(
        Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        GlassCard(Modifier.fillMaxWidth().padding(28.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚠️", fontSize = 36.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Return to home?",
                    color = tokens.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You have an active trip in progress. Going back will take you to the home page. " +
                        "Your visited nodes are saved — you can continue scanning later.",
                    color = tokens.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(tokens.surfaceMuted).clickable { onDismiss() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Stay here", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(accent.tint).border(1.dp, accent.primary, RoundedCornerShape(14.dp))
                            .clickable { onConfirm() }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Go to home", color = accent.dark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
