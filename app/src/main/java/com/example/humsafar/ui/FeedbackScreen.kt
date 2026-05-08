// app/src/main/java/com/example/humsafar/ui/FeedbackScreen.kt
//
// Feedback / bug-report form. Endpoint: POST /community/feedback
// firebase_uid is optional — null means anonymous submission.
// site_id is required by the backend; we default to the user's currently-active
// site (from ActiveSiteManager) and fall back to the most-recently-visited site
// from history. If no site can be inferred, the screen shows a hint and
// disables submission.

package com.example.humsafar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.data.ActiveSiteManager
import com.example.humsafar.models.FeedbackCreateRequest
import com.example.humsafar.network.HumsafarClient
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

sealed class FeedbackSubmitState {
    data object Idle                                : FeedbackSubmitState()
    data object Submitting                          : FeedbackSubmitState()
    data object Success                             : FeedbackSubmitState()
    data class  Error(val message: String)          : FeedbackSubmitState()
}

class FeedbackViewModel : ViewModel() {

    private val _state = MutableStateFlow<FeedbackSubmitState>(FeedbackSubmitState.Idle)
    val state: StateFlow<FeedbackSubmitState> = _state.asStateFlow()

    fun submit(
        siteId:   Int,
        category: String,
        content:  String,
        anonymous: Boolean
    ) = viewModelScope.launch {
        if (siteId <= 0) {
            _state.value = FeedbackSubmitState.Error(
                "Could not determine which site this is about. Please open this from a heritage site you've visited."
            )
            return@launch
        }
        if (content.isBlank()) {
            _state.value = FeedbackSubmitState.Error("Please describe your feedback.")
            return@launch
        }

        _state.value = FeedbackSubmitState.Submitting
        try {
            val firebaseUid = if (anonymous) null else AuthManager.currentUser.value?.uid
            val resp = HumsafarClient.api.submitFeedback(
                FeedbackCreateRequest(
                    firebaseUid = firebaseUid,
                    siteId      = siteId,
                    category    = category,
                    content     = content.trim()
                )
            )
            _state.value = if (resp.isSuccessful) {
                FeedbackSubmitState.Success
            } else {
                FeedbackSubmitState.Error("Server error (${resp.code()})")
            }
        } catch (e: Exception) {
            _state.value = FeedbackSubmitState.Error(e.message ?: "Connection error")
        }
    }

    fun reset() { _state.value = FeedbackSubmitState.Idle }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

private val CATEGORIES = listOf(
    "general"       to "💬  General",
    "bug"           to "🐛  Bug Report",
    "content"       to "📝  Content Issue",
    "accessibility" to "♿  Accessibility"
)

@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    viewModel: FeedbackViewModel = viewModel()
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    val state         by viewModel.state.collectAsStateWithLifecycle()
    val activeSite    by ActiveSiteManager.activeSite.collectAsStateWithLifecycle()
    val firebaseUser  by AuthManager.currentUser.collectAsStateWithLifecycle()

    var category   by remember { mutableStateOf("bug") }
    var content    by remember { mutableStateOf("") }
    var anonymous  by remember { mutableStateOf(false) }

    val resolvedSiteId   = activeSite?.id ?: 0
    val resolvedSiteName = activeSite?.name ?: ""

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ── Header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(tokens.surface.copy(alpha = 0.95f), tokens.surface.copy(alpha = 0.7f))))
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
                    Text("Feedback & Bug Report", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {

                // ── Site context ─────────────────────────────────────────
                GlassCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, tint = GlassWhite10) {
                    Column(Modifier.padding(16.dp)) {
                        SectionLabel("Reporting about")
                        Spacer(Modifier.height(6.dp))
                        if (resolvedSiteId > 0) {
                            Text(
                                resolvedSiteName,
                                color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Site #$resolvedSiteId",
                                color = TextTertiary, fontSize = 12.sp
                            )
                        } else {
                            Text(
                                "No site detected",
                                color = TextSecondary, fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Open this screen while you're at a heritage site (or right after visiting one) so we know which site your feedback is about.",
                                color = TextTertiary, fontSize = 12.sp, lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Category ────────────────────────────────────────────
                SectionLabel("What's this about?")
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORIES.forEach { (id, label) ->
                        val selected = category == id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) accent.tint else GlassWhite10)
                                .border(
                                    0.8.dp,
                                    if (selected) accent.primary else GlassBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { category = id }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                label,
                                color    = if (selected) accent.dark else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Content ─────────────────────────────────────────────
                SectionLabel("Tell us more")
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassWhite10)
                        .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    BasicTextField(
                        value         = content,
                        onValueChange = { content = it.take(1000) },
                        textStyle     = TextStyle(color = TextPrimary, fontSize = 14.sp),
                        modifier      = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        decorationBox = { inner ->
                            if (content.isEmpty()) {
                                Text(
                                    when (category) {
                                        "bug"           -> "Describe what went wrong, what you expected, and (if possible) the steps to reproduce…"
                                        "content"       -> "Describe the content issue — incorrect history, missing info, image problem, etc…"
                                        "accessibility" -> "What was hard to use, see, hear, or reach? Your input helps us improve."
                                        else            -> "Anything you'd like us to know…"
                                    },
                                    color    = TextTertiary,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                            inner()
                        }
                    )
                }
                Text(
                    "${content.length}/1000",
                    color    = TextTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, end = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )

                Spacer(Modifier.height(20.dp))

                // ── Anonymous toggle ────────────────────────────────────
                if (firebaseUser != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassWhite10)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .clickable { anonymous = !anonymous }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Submit anonymously",
                                color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (anonymous) "Your account won't be linked to this report"
                                else "Linked to your account so we can follow up",
                                color = TextTertiary, fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp).clip(CircleShape)
                                .background(if (anonymous) accent.primary else GlassWhite15)
                                .border(0.7.dp, if (anonymous) accent.primary else GlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (anonymous) Icon(
                                Icons.Default.CheckCircle, null,
                                tint     = accent.onAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // ── Status / errors ─────────────────────────────────────
                when (val s = state) {
                    is FeedbackSubmitState.Error -> {
                        Text(
                            s.message,
                            color    = Color(0xFFFF6B6B),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    is FeedbackSubmitState.Success -> {
                        SuccessBanner()
                        Spacer(Modifier.height(20.dp))
                        GlassPrimaryButton(
                            text = "Done",
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        )
                        return@Column
                    }
                    else -> {}
                }

                // ── Submit button ───────────────────────────────────────
                if (state is FeedbackSubmitState.Submitting) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent.primary, modifier = Modifier.size(36.dp))
                    }
                } else {
                    GlassPrimaryButton(
                        text     = "Submit",
                        onClick  = {
                            viewModel.submit(
                                siteId    = resolvedSiteId,
                                category  = category,
                                content   = content,
                                anonymous = anonymous
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled  = content.isNotBlank() && resolvedSiteId > 0
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun SuccessBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x224ADE80))
            .border(0.7.dp, Color(0x554ADE80), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle, null,
            tint = Color(0xFF4ADE80),
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Thanks for the feedback!",
                color = Color(0xFFB6F5C9),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "We've recorded your report. We read every submission.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}
