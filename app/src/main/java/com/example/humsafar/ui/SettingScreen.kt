// app/src/main/java/com/example/humsafar/ui/SettingsScreen.kt
// NEW FILE

package com.example.humsafar.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.humsafar.prefs.LanguagePreferences
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.*

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context   = LocalContext.current
    val langPrefs = remember { LanguagePreferences(context) }
    var selected  by remember { mutableStateOf(langPrefs.selectedLanguage) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 5 }
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {

                // ── Header ────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xF0050D1A), Color(0xBB050D1A))))
                        .drawWithContent {
                            drawContent()
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("Settings", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(28.dp))

                Column(Modifier.padding(horizontal = 20.dp)) {

                    // ── Language section ──────────────────────────────────
                    SectionLabel("Voice Language")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Select the language for voice input and spoken responses",
                        color = TextTertiary, fontSize = 13.sp, lineHeight = 19.sp
                    )
                    Spacer(Modifier.height(18.dp))

                    LanguagePreferences.Language.entries.forEach { lang ->
                        LanguageOptionCard(
                            language   = lang,
                            isSelected = selected == lang,
                            onClick    = {
                                selected               = lang
                                langPrefs.selectedLanguage = lang
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── Info card ─────────────────────────────────────────
                    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                        Column(Modifier.padding(16.dp)) {
                            Text("ℹ️  Language modes", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "English — Full English speech & text\n" +
                                        "हिंदी — Full Hindi Devanagari\n" +
                                        "Hinglish — Casual Roman-script mix",
                                color = TextTertiary, fontSize = 13.sp, lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    language:   LanguagePreferences.Language,
    isSelected: Boolean,
    onClick:    () -> Unit
) {
    val borderBrush = if (isSelected)
        Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
    else
        Brush.linearGradient(listOf(GlassBorder, GlassBorder))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color(0x22FFD54F) else GlassWhite10)
            .border(if (isSelected) 1.5.dp else 0.7.dp, borderBrush, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    language.displayName,
                    color      = if (isSelected) AccentYellow else TextPrimary,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(language.bcp47Code, color = TextTertiary, fontSize = 12.sp)
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = DeepNavy, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}