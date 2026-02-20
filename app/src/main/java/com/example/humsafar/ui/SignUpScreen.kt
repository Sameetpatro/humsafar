package com.example.humsafar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*

@Composable
fun SignUpScreen(
    onLoginClick: () -> Unit,
    onBypassClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

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

                // ── Back + Header ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(14.dp))
                            .clickable { onLoginClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Create\nAccount",
                        color = TextPrimary,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp,
                        lineHeight = 44.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Join Humsafar to explore India's heritage",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                Spacer(Modifier.height(40.dp))

                // ── Form card ──
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {

                        SectionLabel("Personal Info")
                        Spacer(Modifier.height(16.dp))

                        GlassTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Full name",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        GlassTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = "Phone number",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(24.dp))
                        SectionLabel("Account")
                        Spacer(Modifier.height(16.dp))

                        GlassTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Email address",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        GlassTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Password",
                            isPassword = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(28.dp))

                        GlassPrimaryButton(
                            text = "Create Account",
                            onClick = { },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(16.dp))

                        // Google sign in
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(GlassWhite10)
                                .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .clickable { }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Continue with Google",
                                color = TextSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                Row {
                    Text("Already have an account? ", color = TextTertiary, fontSize = 14.sp)
                    Text(
                        "Sign in",
                        color = AccentYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onLoginClick() }
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "Continue as Guest",
                    color = TextTertiary,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onBypassClick() }
                )

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}