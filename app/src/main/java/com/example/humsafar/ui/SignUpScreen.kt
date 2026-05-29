package com.example.humsafar.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onLoginClick: () -> Unit,
    onBypassClick: () -> Unit,
    onSignUpSuccess: () -> Unit = {},
    onNeedPhoneNumber: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // ── Google Sign-In launcher ───────────────────────────────────────────────
    // Mirrors LoginScreen: handle cancellation, map common ApiException codes,
    // and route to the phone-collection step when no number is on record.
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
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
                val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken == null) {
                    errorMessage = "Google sign-in failed: no ID token received.\n" +
                            "Check that the SHA-1 fingerprint is added in the Firebase console."
                    isLoading = false
                    return@launch
                }
                AuthManager.signInWithGoogle(idToken)
                    .onSuccess { user ->
                        if (com.example.humsafar.data.UserRepository.needsPhoneNumber(user))
                            onNeedPhoneNumber()
                        else
                            onSignUpSuccess()
                    }
                    .onFailure { errorMessage = "Google sign-in failed: ${it.message}" }
            } catch (e: ApiException) {
                errorMessage = when (e.statusCode) {
                    10    -> "Developer error: check the SHA-1 fingerprint in Firebase console and that the OAuth client ID matches."
                    12501 -> "Sign-in cancelled."
                    7     -> "Network error. Check your connection."
                    else  -> "Google sign-in error (code ${e.statusCode}): ${e.message}"
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

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(700)) + slideInVertically(tween(700, easing = EaseOutCubic)) { it / 5 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(64.dp))

                // ── Back button ───────────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp).clip(RoundedCornerShape(14.dp))
                            .background(tokens.surface)
                            .border(0.8.dp, tokens.border, RoundedCornerShape(14.dp))
                            .clickable { onLoginClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tokens.textPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(40.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Create\nAccount",
                        color = tokens.textPrimary, fontSize = 40.sp,
                        fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp, lineHeight = 44.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Join Dharohar Setu to explore India's heritage",
                        color = tokens.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Light
                    )
                }

                Spacer(Modifier.height(40.dp))

                // ── Form card ─────────────────────────────────────────────
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                    Column(modifier = Modifier.padding(24.dp)) {

                        SectionLabel("Personal Info")
                        Spacer(Modifier.height(16.dp))

                        GlassTextField(
                            value = name,
                            onValueChange = { name = it; errorMessage = "" },
                            placeholder = "Full name",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        GlassTextField(
                            value = phone,
                            onValueChange = { phone = it; errorMessage = "" },
                            placeholder = "Phone number (e.g. +91 98765 43210)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(24.dp))
                        SectionLabel("Account")
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
                            placeholder = "Password (min 6 characters)",
                            isPassword = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Error
                        if (errorMessage.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                errorMessage,
                                color = Color(0xFFC23B3B), fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(28.dp))

                        if (isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accent.primary, modifier = Modifier.size(32.dp))
                            }
                        } else {
                            GlassPrimaryButton(
                                text = "Create Account",
                                onClick = {
                                    when {
                                        name.isBlank() -> errorMessage = "Please enter your name"
                                        phone.isBlank() -> errorMessage = "Please enter your phone number"
                                        email.isBlank() -> errorMessage = "Please enter your email"
                                        password.length < 6 -> errorMessage = "Password must be at least 6 characters"
                                        else -> {
                                            scope.launch {
                                                isLoading = true
                                                errorMessage = ""
                                            AuthManager.signUp(
                                                name = name.trim(),
                                                email = email.trim(),
                                                phone = phone.trim(),
                                                password = password
                                            ).onSuccess { user ->
                                                // Email signup already collects the phone — persist it
                                                // to the backend so it's stored against the user.
                                                com.example.humsafar.data.UserRepository
                                                    .syncFirebaseUserSuspend(user, phone.trim())
                                                onSignUpSuccess()
                                            }
                                                .onFailure { errorMessage = signUpError(it.message) }
                                                isLoading = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Google Sign-Up
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(tokens.surface)
                                .border(0.8.dp, tokens.border, RoundedCornerShape(16.dp))
                                .clickable(enabled = !isLoading) {
                                    errorMessage = ""
                                    isLoading = true
                                    try {
                                        googleLauncher.launch(AuthManager.getGoogleSignInIntent(context))
                                    } catch (e: Exception) {
                                        errorMessage = "Could not start Google Sign-In: ${e.message}"
                                        isLoading = false
                                    }
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Continue with Google", color = tokens.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Row {
                    Text("Already have an account? ", color = tokens.textTertiary, fontSize = 14.sp)
                    Text(
                        "Sign in", color = accent.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onLoginClick() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Continue as Guest",
                    color = tokens.textTertiary, fontSize = 14.sp,
                    modifier = Modifier.clickable { onBypassClick() }
                )

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

private fun signUpError(msg: String?): String = when {
    msg == null -> "Sign up failed"
    "email already" in msg.lowercase() -> "An account already exists with this email"
    "badly formatted" in msg.lowercase() -> "Invalid email address"
    "weak password" in msg.lowercase() -> "Password is too weak — use at least 6 characters"
    "network" in msg.lowercase() -> "Connection error — check your internet"
    else -> msg
}