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

// ─────────────────────────────────────────────────────────────────────────────
// Phase C: LoginScreen refreshed for light theme + dynamic accent.
// All Firebase / Google / email-link logic is unchanged from the previous
// version — only colours and a few entrance animations were updated.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onSignupClick: () -> Unit,
    onBypassClick: () -> Unit,
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var forgotSent by remember { mutableStateOf(false) }
    var emailLinkSent by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // ── Google Sign-In launcher ───────────────────────────────────────────────
    // FIX: Use getGoogleSignInIntent (which signs out stale session first) instead
    // of getGoogleSignInClient().signInIntent — avoids silent reuse of cached account.
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // FIX: Check resultCode FIRST and surface cancellation clearly
        if (result.resultCode != Activity.RESULT_OK) {
            // User cancelled or back-pressed — don't show an error, just stop loading
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        val data = result.data
        if (data == null) {
            errorMessage = "Google sign-in returned no data. Please try again."
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            isLoading = true
            errorMessage = ""
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                // FIX: getResult throws ApiException — catch it properly
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken

                if (idToken == null) {
                    errorMessage = "Google sign-in failed: no ID token received.\n" +
                            "Check that SHA-1 fingerprint is added in Firebase console."
                    isLoading = false
                    return@launch
                }

                AuthManager.signInWithGoogle(idToken)
                    .onSuccess { onLoginSuccess() }
                    .onFailure { e ->
                        errorMessage = "Google sign-in failed: ${e.message}"
                    }
            } catch (e: ApiException) {
                // FIX: Map common ApiException status codes to human-readable messages
                errorMessage = when (e.statusCode) {
                    10   -> "Developer error: check SHA-1 fingerprint in Firebase console and that the OAuth client ID matches."
                    12501 -> "Sign-in cancelled."
                    12502 -> "Sign-in is currently in progress. Please wait."
                    7    -> "Network error. Check your connection."
                    else -> "Google sign-in error (code ${e.statusCode}): ${e.message}"
                }
            } catch (e: Exception) {
                errorMessage = "Unexpected error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedOrbBackground(modifier = Modifier.fillMaxSize())

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color(0x0A0E1014)
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
                        .background(Brush.linearGradient(listOf(accent.primary, accent.dark)))
                        .border(1.dp, Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x11000000))), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
                        .background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color.Transparent))))
                    Text("🏛️", fontSize = 36.sp)
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "धरोहरसेतु",
                    color = tokens.textPrimary, fontSize = 38.sp,
                    fontWeight = FontWeight.Black, letterSpacing = (-1).sp
                )
                Text(
                    text = "Your Heritage Companion",
                    color = tokens.textSecondary, fontSize = 14.sp,
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
                            onValueChange = { email = it; errorMessage = ""; emailLinkSent = false },
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (forgotSent) "✓ Reset email sent!" else "Forgot Password?",
                                color = if (forgotSent) Color(0xFF15803D) else accent.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
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

                            Text(
                                text = if (emailLinkSent) "✓ Link sent!" else "Email Link",
                                color = if (emailLinkSent) Color(0xFF15803D) else accent.dark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    if (email.isNotBlank() && !emailLinkSent) {
                                        scope.launch {
                                            isLoading = true
                                            AuthManager.sendSignInLinkToEmail(email.trim(), context)
                                                .onSuccess {
                                                    emailLinkSent = true
                                                    errorMessage = ""
                                                }
                                                .onFailure { errorMessage = it.message ?: "Failed to send link" }
                                            isLoading = false
                                        }
                                    } else if (email.isBlank()) {
                                        errorMessage = "Enter your email above first"
                                    }
                                }
                            )
                        }

                        if (emailLinkSent) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Check your email and click the sign-in link",
                                color = Color(0xFF15803D),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (errorMessage.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                errorMessage,
                                color = Color(0xFFC23B3B),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        if (isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accent.primary, modifier = Modifier.size(32.dp))
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

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f).height(0.7.dp).background(tokens.border))
                    Text("  or  ", color = tokens.textTertiary, fontSize = 13.sp)
                    Box(Modifier.weight(1f).height(0.7.dp).background(tokens.border))
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.surface)
                            .border(0.8.dp, tokens.border, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isLoading) {
                                errorMessage = ""
                                isLoading = true
                                try {
                                    val intent = AuthManager.getGoogleSignInIntent(context)
                                    googleLauncher.launch(intent)
                                } catch (e: Exception) {
                                    errorMessage = "Could not start Google Sign-In: ${e.message}"
                                    isLoading = false
                                }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G  Google", color = tokens.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.surface)
                            .border(0.8.dp, tokens.border, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isLoading) {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = ""
                                    AuthManager.signInAnonymously()
                                        .onSuccess { onBypassClick() }
                                        .onFailure { errorMessage = "Guest login failed: ${it.message}" }
                                    isLoading = false
                                }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Guest", color = tokens.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(36.dp))

                Row {
                    Text("No account? ", color = tokens.textTertiary, fontSize = 14.sp)
                    Text(
                        "Create one",
                        color = accent.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
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