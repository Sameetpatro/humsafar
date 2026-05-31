package com.example.humsafar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.audio.BgmPlayer
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

@Composable
fun QuizScreen(
    tripId: Int,
    onFinish: () -> Unit,
    vm: QuizViewModel = viewModel()
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var showQuitDialog by remember { mutableStateOf(false) }

    // Background music for the whole quiz session.
    val bgm = remember { BgmPlayer(context) }
    DisposableEffect(Unit) {
        bgm.start()
        onDispose { bgm.stop() }
    }

    LaunchedEffect(tripId) { vm.load(tripId) }

    // Only Playing state forfeits gems on exit; warn before leaving.
    val isPlaying = state is QuizUiState.Playing
    BackHandler(enabled = isPlaying) { showQuitDialog = true }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(tokens.bgWarm, tokens.surface, tokens.bgWarmDeep)))
    ) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Text("Heritage Quiz", color = tokens.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Answer fast — quicker means more gems!", color = tokens.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            when (val s = state) {
                is QuizUiState.Loading -> CenterLoader(accent.primary, "Building your quiz...")
                is QuizUiState.Error -> ResultCard("Quiz unavailable", s.message, "💡", onFinish)
                is QuizUiState.AlreadyPlayed -> ResultCard(
                    title = if (s.status == "abandoned") "Quiz forfeited" else "Already played",
                    body = if (s.status == "abandoned")
                        "You left this quiz earlier, so no gems were earned for this trip."
                    else "You already earned ${s.gems} gems from this trip's quiz.",
                    emoji = if (s.status == "abandoned") "🚪" else "✅",
                    onContinue = onFinish
                )
                is QuizUiState.Finished -> ResultCard(
                    title = "Quiz complete!",
                    body = "You earned ${s.gemsEarned} gems.\nWallet balance: ${s.newBalance} gems.",
                    emoji = "🏆",
                    onContinue = onFinish
                )
                is QuizUiState.Playing -> PlayingContent(s, vm)
            }
        }
    }

    if (showQuitDialog) {
        QuitWarningDialog(
            onConfirm = {
                showQuitDialog = false
                vm.abandon()
                onFinish()
            },
            onDismiss = { showQuitDialog = false }
        )
    }
}

@Composable
private fun PlayingContent(s: QuizUiState.Playing, vm: QuizViewModel) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current
    val q = s.questions[s.index]

    val startMs = remember(s.index) { System.currentTimeMillis() }
    var remaining by remember(s.index) { mutableFloatStateOf(s.secondsPerQuestion.toFloat()) }

    // Countdown — auto-submit a timeout when it hits zero.
    LaunchedEffect(s.index) {
        while (true) {
            val cur = vm.state.value
            if (cur !is QuizUiState.Playing || cur.index != s.index || cur.feedback != null) break
            val elapsed = (System.currentTimeMillis() - startMs) / 1000f
            remaining = (s.secondsPerQuestion - elapsed).coerceAtLeast(0f)
            if (remaining <= 0f) {
                vm.submitAnswer(q.questionId, -1, s.secondsPerQuestion.toDouble())
                break
            }
            delay(50)
        }
    }

    // After showing feedback, move on.
    LaunchedEffect(s.index, s.feedback != null) {
        if (s.feedback != null) {
            delay(1300)
            vm.advance()
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Question ${s.index + 1}/${s.questions.size}", color = tokens.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("💎 ${s.runningGems}", color = accent.dark, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
    Spacer(Modifier.height(10.dp))

    // Timer bar
    val frac by animateFloatAsState(remaining / s.secondsPerQuestion, tween(120), label = "timer")
    val barColor = if (remaining <= 3f) Color(0xFFE05555) else accent.primary
    Box(
        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(tokens.surfaceMuted)
    ) {
        Box(Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(4.dp)).background(barColor))
    }
    Spacer(Modifier.height(6.dp))
    Text("${remaining.toInt()}s", color = if (remaining <= 3f) Color(0xFFE05555) else tokens.textTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

    Spacer(Modifier.height(16.dp))

    GlassCard(Modifier.fillMaxWidth()) {
        Text(
            q.question,
            color = tokens.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 25.sp,
            modifier = Modifier.fillMaxWidth().padding(20.dp)
        )
    }

    Spacer(Modifier.height(16.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        q.options.forEachIndexed { i, opt ->
            OptionButton(
                label = opt,
                index = i,
                feedback = s.feedback,
                enabled = s.feedback == null && !s.submitting,
                onClick = {
                    val secs = (System.currentTimeMillis() - startMs) / 1000.0
                    vm.submitAnswer(q.questionId, i, secs)
                }
            )
        }
    }

    if (s.feedback != null) {
        Spacer(Modifier.height(14.dp))
        val fb = s.feedback
        Text(
            if (fb.correct) "Correct! +${fb.awarded} 💎" else "Time's up / wrong — no gems",
            color = if (fb.correct) accent.dark else tokens.textTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OptionButton(
    label: String,
    index: Int,
    feedback: AnswerFeedback?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalAppColors.current
    val accent = LocalAccent.current

    val correctColor = Color(0xFF35B07A)
    val wrongColor = Color(0xFFE05555)

    val target = when {
        feedback == null -> tokens.surface
        index == feedback.correctIndex -> correctColor.copy(alpha = 0.22f)
        index == feedback.selectedIndex -> wrongColor.copy(alpha = 0.22f)
        else -> tokens.surface
    }
    val bg by animateColorAsState(target, tween(200), label = "optbg")
    val borderC = when {
        feedback == null -> tokens.border
        index == feedback.correctIndex -> correctColor
        index == feedback.selectedIndex -> wrongColor
        else -> tokens.border
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderC, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(accent.tint),
                contentAlignment = Alignment.Center
            ) {
                Text(('A' + index).toString(), color = accent.dark, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(14.dp))
            Text(label, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CenterLoader(color: Color, msg: String) {
    val tokens = LocalAppColors.current
    Column(
        Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = color)
        Spacer(Modifier.height(16.dp))
        Text(msg, color = tokens.textSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun ResultCard(title: String, body: String, emoji: String, onContinue: () -> Unit) {
    val tokens = LocalAppColors.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(emoji, fontSize = 44.sp)
                Spacer(Modifier.height(12.dp))
                Text(title, color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text(body, color = tokens.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        GlassPrimaryButton(text = "Continue", onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun QuitWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val tokens = LocalAppColors.current
    Box(
        Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        GlassCard(Modifier.fillMaxWidth().padding(28.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️", fontSize = 36.sp)
                Spacer(Modifier.height(10.dp))
                Text("Quit the quiz?", color = tokens.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(
                    "If you leave now, all gems you've earned in this quiz will be lost and you can't retake it for this trip.",
                    color = tokens.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp
                )
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(tokens.surfaceMuted).clickable { onDismiss() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Keep playing", color = tokens.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(Color(0x22E05555)).border(1.dp, Color(0x55E05555), RoundedCornerShape(14.dp))
                            .clickable { onConfirm() }.padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("Quit & lose gems", color = Color(0xFFE05555), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
