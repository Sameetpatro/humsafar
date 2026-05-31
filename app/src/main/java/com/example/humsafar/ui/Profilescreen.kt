// app/src/main/java/com/example/humsafar/ui/ProfileScreen.kt

package com.example.humsafar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.components.SectionLabel
import com.example.humsafar.ui.theme.*

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onSignOut:      () -> Unit = {},
    onOpenHistory:  () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onOpenMyInsights: () -> Unit = {},
    onOpenStore: () -> Unit = {},
    onOpenCoupons: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onAccentChange: (Accent) -> Unit = {}
) {
    var visible   by remember { mutableStateOf(false) }
    val scroll    = rememberScrollState()
    var showAbout by remember { mutableStateOf(false) }

    val currentUser  by AuthManager.currentUser.collectAsState()
    val displayName  = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer"
    val displayEmail = currentUser?.email?.takeIf { it.isNotBlank() } ?: "guest@dharoharsetu.app"
    val isAnonymous  = currentUser?.isAnonymous ?: true

    LaunchedEffect(Unit) { visible = true }

    // ── Main content ──────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(500)) + slideInVertically(tween(600, easing = EaseOutCubic)) { it / 6 }
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {

                ProfileHeader(onBack = onBack)

                ProfileAvatar(
                    displayName = displayName,
                    isAnonymous = isAnonymous
                )

                Spacer(Modifier.height(8.dp))

                Column(Modifier.padding(horizontal = 20.dp)) {

                    SectionLabel("Account Details")
                    Spacer(Modifier.height(12.dp))
                    AccountDetailsCard(
                        displayName  = displayName,
                        displayEmail = displayEmail,
                        isAnonymous  = isAnonymous
                    )

                    Spacer(Modifier.height(24.dp))

                    SectionLabel("Your Journey")
                    Spacer(Modifier.height(12.dp))
                    JourneyCard(
                        onOpenHistory   = onOpenHistory,
                        onOpenFeedback  = onOpenFeedback,
                        onOpenMyInsights = onOpenMyInsights
                    )

                    Spacer(Modifier.height(24.dp))

                    SectionLabel("Rewards")
                    Spacer(Modifier.height(12.dp))
                    RewardsCard(
                        onOpenStore   = onOpenStore,
                        onOpenCoupons = onOpenCoupons
                    )

                    Spacer(Modifier.height(24.dp))

                    SectionLabel("Language & Region")
                    Spacer(Modifier.height(12.dp))
                    LanguageCard()

                    Spacer(Modifier.height(24.dp))

                    SectionLabel("Personalize")
                    Spacer(Modifier.height(12.dp))
                    PersonalizeCard(
                        onReplayOnboarding = onReplayOnboarding,
                        onAccentChange     = onAccentChange
                    )

                    Spacer(Modifier.height(24.dp))

                    SectionLabel("App Settings")
                    Spacer(Modifier.height(12.dp))
                    AppSettingsCard()

                    Spacer(Modifier.height(24.dp))

                    SectionLabel("About")
                    Spacer(Modifier.height(12.dp))
                    AboutCard(
                        expanded     = showAbout,
                        onToggle     = { showAbout = !showAbout },
                        onReportBug  = onOpenFeedback
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Sign Out ──────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x15FF5252))
                            .border(0.7.dp, Color(0x44FF5252), RoundedCornerShape(16.dp))
                            .clickable { onSignOut() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚪", fontSize = 16.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Sign Out",
                                color      = Color(0xFFFF6B6B),
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text      = "Dharohar Setu v1.0  •  Prototype Build",
                        color     = TextTertiary,
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth().padding(bottom = 40.dp)
                    )
                }
            }
        }
    }

    // ── About overlay — outside Box to avoid scope conflict ───────────────────
    if (showAbout) {
        AboutOverlay(onClose = { showAbout = false })
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileHeader(onBack: () -> Unit) {
    val tokens = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(tokens.surface.copy(alpha = 0.97f), tokens.surfaceMuted.copy(alpha = 0.92f))
                )
            )
            .drawBehind {
                drawLine(tokens.divider, Offset(0f, size.height), Offset(size.width, size.height), 0.5.dp.toPx())
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
            Text(
                "Profile & Settings",
                color      = TextPrimary,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Avatar ────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileAvatar(
    displayName: String,
    isAnonymous: Boolean
) {
    val inf  = rememberInfiniteTransition(label = "avatar")
    val glow by inf.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "g"
    )

    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(104.dp).clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(LocalAccent.current.primary.copy(alpha = glow * 0.35f), Color.Transparent)
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(90.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(LocalAccent.current.primary, LocalAccent.current.dark)))
                    .border(2.dp, GlassBorderBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.matchParentSize().clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color.Transparent)))
                )
                Text("👤", fontSize = 38.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            displayName,
            color      = TextPrimary,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (isAnonymous) Color(0x22FFFFFF) else LocalAccent.current.primary.copy(alpha = 0.13f)
                )
                .border(
                    0.7.dp,
                    if (isAnonymous) Color(0x44FFFFFF) else LocalAccent.current.primary.copy(alpha = 0.27f),
                    RoundedCornerShape(50)
                )
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                if (isAnonymous) "🗺️  Guest Explorer" else "🗺️  Heritage Enthusiast",
                color      = if (isAnonymous) TextSecondary else LocalAccent.current.primary,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Account Details ───────────────────────────────────────────────────────────

@Composable
private fun AccountDetailsCard(
    displayName:  String,
    displayEmail: String,
    isAnonymous:  Boolean
) {
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(Modifier.padding(4.dp)) {
            ProfileRow(icon = "👤", label = "Name",  value = displayName)
            ProfileDivider()
            ProfileRow(
                icon  = "📧",
                label = "Email",
                value = if (isAnonymous) "Signed in as Guest" else displayEmail
            )
            ProfileDivider()
            ProfileRow(
                icon  = "🔑",
                label = "Account",
                value = if (isAnonymous) "Guest Mode  •  Limited features" else "Verified Account"
            )
        }
    }
}

@Composable
private fun ProfileRow(icon: String, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                color        = TextTertiary,
                fontSize     = 11.sp,
                fontWeight   = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(GlassBorder)
    )
}

// ── Journey Card (history + feedback) ────────────────────────────────────────

@Composable
private fun JourneyCard(
    onOpenHistory:  () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenMyInsights: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenHistory() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📜",
                    fontSize = 18.sp,
                    modifier = Modifier.size(24.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Visit History",
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Every site you've explored",
                        color = TextTertiary, fontSize = 12.sp
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = LocalAccent.current.primary, modifier = Modifier.size(18.dp))
            }
            ProfileDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenMyInsights() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📊",
                    fontSize = 18.sp,
                    modifier = Modifier.size(24.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "My Insights",
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Your personal heritage footprint",
                        color = TextTertiary, fontSize = 12.sp
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = LocalAccent.current.primary, modifier = Modifier.size(18.dp))
            }
            ProfileDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFeedback() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "💬",
                    fontSize = 18.sp,
                    modifier = Modifier.size(24.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Send Feedback",
                        color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Found a bug or have a suggestion?",
                        color = TextTertiary, fontSize = 12.sp
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Rewards Card (gems + store + coupons) ────────────────────────────────────

@Composable
private fun RewardsCard(
    onOpenStore: () -> Unit,
    onOpenCoupons: () -> Unit
) {
    val gems by com.example.humsafar.data.GamificationRepository.gems.collectAsState()
    LaunchedEffect(Unit) { com.example.humsafar.data.GamificationRepository.refresh() }

    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(Modifier.padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💎", fontSize = 18.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Gem Wallet", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Earned by playing the heritage quiz", color = TextTertiary, fontSize = 12.sp)
                }
                Text("$gems", color = LocalAccent.current.dark, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            ProfileDivider()
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenStore() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🛍️", fontSize = 18.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Coupon Store", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Spin gems into hotel & food discounts", color = TextTertiary, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = LocalAccent.current.primary, modifier = Modifier.size(18.dp))
            }
            ProfileDivider()
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenCoupons() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🎫", fontSize = 18.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("My Coupons", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("View and redeem before they expire", color = TextTertiary, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = LocalAccent.current.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Language Card ─────────────────────────────────────────────────────────────

@Composable
private fun LanguageCard() {
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(Modifier.padding(4.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🇬🇧", fontSize = 20.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("English", color = LocalAccent.current.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("en-IN  •  Active", color = TextTertiary, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(24.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(LocalAccent.current.primary, LocalAccent.current.dark))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, null, tint = LocalAccent.current.onAccent, modifier = Modifier.size(14.dp))
                }
            }
            ProfileDivider()
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🇮🇳", fontSize = 20.sp, modifier = Modifier.alpha(0.4f))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hindi / Hinglish", color = TextTertiary, fontSize = 15.sp)
                    Text("Coming soon", color = TextTertiary.copy(alpha = 0.5f), fontSize = 12.sp)
                }
                Text(
                    "Soon",
                    color    = TextTertiary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GlassWhite10)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ── Personalize (replay intro + change theme colour) ──────────────────────────

@Composable
private fun PersonalizeCard(
    onReplayOnboarding: () -> Unit,
    onAccentChange: (Accent) -> Unit
) {
    val accent = LocalAccent.current
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(Modifier.padding(4.dp)) {

            // ── Inline accent colour picker ───────────────────────────────────
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎨", fontSize = 16.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Accent Colour", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(accent.name, color = TextTertiary, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Accents.forEach { a ->
                        AccentDot(
                            accent   = a,
                            selected = a.name == accent.name,
                            onClick  = { onAccentChange(a) }
                        )
                    }
                }
            }

            ProfileDivider()

            // ── Replay tutorial ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReplayOnboarding() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧭", fontSize = 16.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Replay Intro Tour", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Watch the welcome walkthrough again", color = TextTertiary, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = accent.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AccentDot(
    accent: Accent,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "accentDot"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent.primary, accent.dark)))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent.onAccent.copy(alpha = 0.9f) else GlassBorder,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text("✓", color = accent.onAccent, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ── App Settings ──────────────────────────────────────────────────────────────

@Composable
private fun AppSettingsCard() {
    val settings = listOf(
        Triple("🔔", "Push Notifications", "Heritage site alerts"),
        Triple("📍", "Background Location", "Auto-detect sites"),
        Triple("🎵", "Voice Audio", "Text-to-speech responses"),
        Triple("🌙", "Dark Mode", "Always on  •  Optimised for heritage")
    )

    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(Modifier.padding(4.dp)) {
            settings.forEachIndexed { i, (icon, title, sub) ->
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(icon, fontSize = 18.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(sub, color = TextTertiary, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .width(44.dp).height(26.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(listOf(LocalAccent.current.primary, LocalAccent.current.dark))
                            ),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 3.dp)
                                .size(20.dp).clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                if (i < settings.size - 1) ProfileDivider()
            }
        }
    }
}

// ── About Card ────────────────────────────────────────────────────────────────

@Composable
private fun AboutCard(
    expanded:    Boolean,
    onToggle:    () -> Unit,
    onReportBug: () -> Unit = {}
) {
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(Modifier.padding(4.dp)) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️", fontSize = 18.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("About Dharohar Setu", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("Meet the team & tech stack", color = TextTertiary, fontSize = 12.sp)
                }
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint     = LocalAccent.current.primary,
                    modifier = Modifier.size(18.dp).rotate(if (expanded) 90f else 0f)
                )
            }

            ProfileDivider()

            data class AboutItem(
                val icon:    String,
                val label:   String,
                val onClick: () -> Unit
            )

            val items = listOf(
                AboutItem("⭐", "Rate the App", { /* future: launch Play Store */ }),
                AboutItem("📤", "Share App",    { /* future: share intent */ }),
                AboutItem("🐛", "Report a Bug", onReportBug)
            )

            items.forEachIndexed { i, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.icon, fontSize = 18.sp, modifier = Modifier.size(24.dp), textAlign = TextAlign.Center)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        item.label, color = TextPrimary, fontSize = 14.sp,
                        fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                }
                if (i < items.size - 1) ProfileDivider()
            }
        }
    }
}

// ── About Full-Screen Overlay ─────────────────────────────────────────────────

@Composable
private fun AboutOverlay(onClose: () -> Unit) {
    val scroll = rememberScrollState()
    val tokens = LocalAppColors.current

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(tokens.bgWarm.copy(alpha = 0.98f)))
        AnimatedOrbBackground(Modifier.fillMaxSize().alpha(0.5f))

        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {

            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(tokens.surface.copy(alpha = 0.97f), tokens.surfaceMuted.copy(alpha = 0.92f))
                        )
                    )
                    .drawBehind {
                        drawLine(tokens.divider, Offset(0f, size.height), Offset(size.width, size.height), 0.5.dp.toPx())
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
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "About Dharohar Setu",
                        color      = TextPrimary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(Modifier.padding(horizontal = 20.dp)) {

                // Logo + title
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp).clip(RoundedCornerShape(28.dp))
                                .background(Brush.linearGradient(listOf(LocalAccent.current.primary, LocalAccent.current.dark)))
                                .border(1.dp, GlassBorderBright, RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.matchParentSize().clip(RoundedCornerShape(28.dp))
                                    .background(Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color.Transparent)))
                            )
                            Text("🏛️", fontSize = 36.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Dharohar Setu",
                            color        = TextPrimary,
                            fontSize     = 30.sp,
                            fontWeight   = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        Text("Your Heritage Companion", color = TextTertiary, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(LocalAccent.current.primary.copy(alpha = 0.13f))
                                .border(0.7.dp, LocalAccent.current.primary.copy(alpha = 0.27f), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text("v1.0  •  Prototype", color = LocalAccent.current.primary, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                AboutSection(
                    title = "🎯 The Mission",
                    body  = "Dharohar Setu was built to bridge the gap between India's rich heritage and the modern traveller. Using AI, voice technology, and location awareness, it transforms every heritage site visit into an immersive, personalised learning experience."
                )

                Spacer(Modifier.height(16.dp))

                AboutSection(
                    title = "👥 The Team",
                    body  = "This app is developed by Sameet Patro and Harsh Kaldoke — undergraduate students at IIIT Sonepat — under the expert mentorship of Dr. Mukesh Mann, whose guidance shaped the vision and technical direction of this project."
                )

                Spacer(Modifier.height(16.dp))

                GlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(Modifier.padding(20.dp)) {
                        Text("⚙️ Tech Stack", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        val tech = listOf(
                            "📱 Android (Kotlin + Jetpack Compose)" to "Native UI with glassmorphism design",
                            "🗺️ MapLibre GL"                        to "Open-source interactive map rendering",
                            "📍 Google Play Services"                to "Location + Geofencing + Activity Recognition",
                            "🔐 Firebase Auth"                       to "Email, Google & Guest authentication",
                            "🤖 Claude AI (Anthropic)"               to "Conversational heritage guide",
                            "🎙️ Sarvam AI"                          to "Indian language STT & TTS",
                            "🎬 ExoPlayer (Media3)"                  to "Cinematic video playback",
                            "🔗 Retrofit + OkHttp"                   to "REST API networking layer",
                            "☁️ Render.com"                         to "Backend hosting (Python / FastAPI)"
                        )
                        tech.forEachIndexed { i, (name, desc) ->
                            if (i > 0) Box(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                    .height(0.5.dp).background(GlassBorder)
                            )
                            Column {
                                Text(name, color = LocalAccent.current.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(desc, color = TextTertiary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x22FF6B6B))
                        .border(0.7.dp, Color(0x44FF6B6B), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row {
                        Text("⚠️", fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Prototype Notice",
                                color      = Color(0xFFFF8080),
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "This is a research prototype developed for academic purposes. Features may be incomplete, and some services use free-tier backends that may have latency. Not intended for production use.",
                                color      = TextTertiary,
                                fontSize   = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text      = "Dharohar Setu - 2026",
                    color     = TextTertiary,
                    fontSize  = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(bottom = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun AboutSection(title: String, body: String) {
    Column {
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite10)
                .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(body, color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}