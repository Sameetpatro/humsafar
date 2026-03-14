package com.example.humsafar.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.*

@Composable
fun ReviewScreen(
    tripId: Int,
    siteId: Int,
    siteName: String,
    visitedCount: Int,
    totalCount: Int,
    onNavigateToTripCompletion: () -> Unit,
    onSkip: () -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    var starRating by remember { mutableStateOf(0) }
    var q1 by remember { mutableStateOf(0) }
    var q2 by remember { mutableStateOf(0) }
    var q3 by remember { mutableStateOf(0) }
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()

    LaunchedEffect(submitState) {
        when (val s = submitState) {
            is ReviewSubmitState.Success -> {
                viewModel.resetState()
                onNavigateToTripCompletion()
            }
            else -> {}
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xBB000000))
                        .border(0.5.dp, GlassBorder, CircleShape)
                        .clickable { onSkip() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Rate Your Experience",
                    color = AccentYellow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                cornerRadius = 16.dp,
                tint = Color(0x22FFD54F)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(siteName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "$visitedCount/$totalCount nodes visited",
                        color = TextTertiary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "Overall rating",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..5).forEach { stars ->
                    val selected = starRating >= stars
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) AccentYellow.copy(alpha = 0.5f)
                                else GlassWhite10
                            )
                            .border(
                                0.7.dp,
                                if (selected) AccentYellow else GlassBorder,
                                CircleShape
                            )
                            .clickable { starRating = stars },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = if (selected) AccentYellow else TextTertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            QuestionRow(
                question = "How was your overall experience?",
                labels = listOf("Very Poor", "Poor", "Okay", "Good", "Excellent"),
                selected = q1,
                onSelect = { q1 = it }
            )
            QuestionRow(
                question = "How helpful was the AI heritage guide?",
                labels = listOf("Not helpful", "Slightly", "Somewhat", "Helpful", "Extremely"),
                selected = q2,
                onSelect = { q2 = it }
            )
            QuestionRow(
                question = "Would you recommend this site to others?",
                labels = listOf("Definitely not", "Probably not", "Maybe", "Probably yes", "Definitely yes"),
                selected = q3,
                onSelect = { q3 = it }
            )

            Spacer(Modifier.height(24.dp))

            when (val s = submitState) {
                is ReviewSubmitState.Error -> {
                    Text(
                        s.message,
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                else -> {}
            }

            GlassPrimaryButton(
                text = if (submitState is ReviewSubmitState.Submitting) "Submitting…" else "Submit",
                onClick = {
                    if (starRating in 1..5 && q1 in 1..5 && q2 in 1..5 && q3 in 1..5) {
                        viewModel.submitReview(tripId, siteId, starRating, q1, q2, q3, { }, { })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                enabled = submitState !is ReviewSubmitState.Submitting
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GlassWhite15)
                    .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
                    .clickable { onSkip() }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Skip", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun QuestionRow(
    question: String,
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(question, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..5).forEach { i ->
                val sel = selected == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) AccentYellow.copy(alpha = 0.3f) else GlassWhite10)
                        .border(0.7.dp, if (sel) AccentYellow else GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        labels[i - 1],
                        color = if (sel) AccentYellow else TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
