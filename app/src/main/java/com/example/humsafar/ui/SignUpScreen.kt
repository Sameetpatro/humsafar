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
    onSignUpSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                        .onSuccess { onSignUpSuccess() }
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
                            .background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .clickable { onLoginClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(40.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Create\nAccount",
                        color = TextPrimary, fontSize = 40.sp,
                        fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp, lineHeight = 44.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Join Dharohar Setu to explore India's heritage",
                        color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Light
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
                                color = Color(0xFFFF6B6B), fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(28.dp))

                        if (isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentYellow, modifier = Modifier.size(32.dp))
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
                                                ).onSuccess { onSignUpSuccess() }
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
                                .background(GlassWhite10)
                                .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .clickable {
                                    val client = AuthManager.getGoogleSignInClient(context)
                                    googleLauncher.launch(client.signInIntent)
                                }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Continue with Google", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Row {
                    Text("Already have an account? ", color = TextTertiary, fontSize = 14.sp)
                    Text(
                        "Sign in", color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onLoginClick() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Continue as Guest",
                    color = TextTertiary, fontSize = 14.sp,
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