package com.example.humsafar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*

@Composable
fun LoginScreen(
    onSignupClick: () -> Unit,
    onBypassClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Entry animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Animated orb background ──
        AnimatedOrbBackground(modifier = Modifier.fillMaxSize())

        // ── Subtle grid lines (iOS-style depth) ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineColor = Color(0x06FFFFFF)
            val step = 60.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
                y += step
            }
        }

        // ── Content ──
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

                // ── Logo area ──
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD54F), Color(0xFFFFC107))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF))),
                            RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Inner specular
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color.Transparent))
                            )
                    )
                    Text("🏛️", fontSize = 36.sp)
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Humsafar",
                    color = TextPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Your Heritage Companion",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(52.dp))

                // ── Glass card form ──
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 28.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {

                        SectionLabel("Sign In")
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

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Forgot Password?",
                            color = AccentYellow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.End)
                        )

                        Spacer(Modifier.height(24.dp))

                        GlassPrimaryButton(
                            text = "Sign In",
                            onClick = { /* TODO */ },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Divider ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.weight(1f).height(0.5.dp).background(GlassBorder))
                    Text("  or  ", color = TextTertiary, fontSize = 13.sp)
                    Box(Modifier.weight(1f).height(0.5.dp).background(GlassBorder))
                }

                Spacer(Modifier.height(20.dp))

                // ── Google / Guest row ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Google button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .clickable { }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G  Google", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }

                    // Guest button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(GlassWhite15)
                            .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                            .clickable { onBypassClick() }
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
                        color = AccentYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onSignupClick() }
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}