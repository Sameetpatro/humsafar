package com.example.humsafar.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSignupClick: () -> Unit,
    onBypassClick: () -> Unit,
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var forgotSent by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // ── Google Sign-In launcher ───────────────────────────────────────────────
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken ?: return@rememberLauncherForActivityResult
                scope.launch {
                    isLoading = true
                    errorMessage = ""
                    AuthManager.signInWithGoogle(idToken)
                        .onSuccess { onLoginSuccess() }
                        .onFailure { errorMessage = it.message ?: "Google sign-in failed" }
                    isLoading = false
                }
            } catch (e: ApiException) {
                errorMessage = "Google sign-in failed: ${e.message}"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedOrbBackground(modifier = Modifier.fillMaxSize())

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color(0x06FFFFFF)
            val step = 60.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                y += step
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(800)) + slideInVertically(tween(800, easing = EaseOutCubic)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(100.dp))

                // ── Logo ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))))
                        .border(1.dp, Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF))), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                        .background(Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color.Transparent))))
                    Text("🏛️", fontSize = 36.sp)
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "धरोहरसेतु",
                    color = TextPrimary, fontSize = 38.sp,
                    fontWeight = FontWeight.Black, letterSpacing = (-1).sp
                )
                Text(
                    text = "Your Heritage Companion",
                    color = TextSecondary, fontSize = 14.sp,
                    fontWeight = FontWeight.Light, letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(52.dp))

                // ── Form card ─────────────────────────────────────────────
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        SectionLabel("Sign In")
                        Spacer(Modifier.height(16.dp))

                        GlassTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = "" },
                            placeholder = "Email address",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        GlassTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = "" },
                            placeholder = "Password",
                            isPassword = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        // Forgot Password
                        Text(
                            text = if (forgotSent) "✓ Reset email sent!" else "Forgot Password?",
                            color = if (forgotSent) Color(0xFF4ADE80) else AccentYellow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable {
                                    if (email.isNotBlank() && !forgotSent) {
                                        scope.launch {
                                            AuthManager.sendPasswordReset(email)
                                                .onSuccess { forgotSent = true }
                                                .onFailure { errorMessage = "Enter a valid email first" }
                                        }
                                    } else if (email.isBlank()) {
                                        errorMessage = "Enter your email above first"
                                    }
                                }
                        )

                        // Error message
                        if (errorMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                errorMessage,
                                color = Color(0xFFFF6B6B),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Sign In button
                        if (isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentYellow, modifier = Modifier.size(32.dp))
                            }
                        } else {
                            GlassPrimaryButton(
                                text = "Sign In",
                                onClick = {
                                    if (email.isBlank() || password.isBlank()) {
                                        errorMessage = "Please enter email and password"
                                        return@GlassPrimaryButton
                                    }
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = ""
                                        AuthManager.signIn(email.trim(), password)
                                            .onSuccess { onLoginSuccess() }
                                            .onFailure { errorMessage = friendlyError(it.message) }
                                        isLoading = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Divider ───────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f).height(0.5.dp).background(GlassBorder))
                    Text("  or  ", color = TextTertiary, fontSize = 13.sp)
                    Box(Modifier.weight(1f).height(0.5.dp).background(GlassBorder))
                }

                Spacer(Modifier.height(20.dp))

                // ── Google + Guest row ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google Sign-In
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                val client = AuthManager.getGoogleSignInClient(context)
                                googleLauncher.launch(client.signInIntent)
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G  Google", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // Guest (anonymous)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                scope.launch {
                                    isLoading = true
                                    AuthManager.signInAnonymously()
                                        .onSuccess { onBypassClick() }
                                        .onFailure { errorMessage = "Guest login failed" }
                                    isLoading = false
                                }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Guest", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(36.dp))

                Row {
                    Text("No account? ", color = TextTertiary, fontSize = 14.sp)
                    Text(
                        "Create one",
                        color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onSignupClick() }
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

private fun friendlyError(msg: String?): String = when {
    msg == null -> "Something went wrong"
    "no user" in msg.lowercase() || "not found" in msg.lowercase() -> "No account found with this email"
    "password" in msg.lowercase() -> "Incorrect password"
    "badly formatted" in msg.lowercase() -> "Invalid email address"
    "network" in msg.lowercase() -> "Connection error — check your internet"
    else -> msg
}