package com.example.humsafar.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.data.UserRepository
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*
import kotlinx.coroutines.launch

// Shown right after a Google sign-in, where no phone number is available.
// Lets the user add (or skip) their mobile number before entering the app.
@Composable
fun CollectPhoneScreen(
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current

    var phone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "One last step",
                    color = tokens.textPrimary, fontSize = 34.sp,
                    fontWeight = FontWeight.Black, letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Add your mobile number so we can reach you about your heritage trips.",
                    color = tokens.textSecondary, fontSize = 15.sp,
                    fontWeight = FontWeight.Light, textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(36.dp))

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        SectionLabel("Mobile Number")
                        Spacer(Modifier.height(16.dp))

                        GlassTextField(
                            value = phone,
                            onValueChange = { phone = it; errorMessage = "" },
                            placeholder = "e.g. +91 98765 43210",
                            keyboardType = KeyboardType.Phone,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                errorMessage,
                                color = Color(0xFFC23B3B), fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        if (isLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = accent.primary, modifier = Modifier.size(32.dp))
                            }
                        } else {
                            GlassPrimaryButton(
                                text = "Save & Continue",
                                onClick = {
                                    val digits = phone.filter { it.isDigit() }
                                    if (digits.length < 7) {
                                        errorMessage = "Please enter a valid mobile number"
                                        return@GlassPrimaryButton
                                    }
                                    val uid = AuthManager.currentUser.value?.uid
                                    if (uid == null) { onDone(); return@GlassPrimaryButton }
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = ""
                                        val ok = UserRepository.updatePhone(uid, phone.trim())
                                        isLoading = false
                                        if (ok) onDone()
                                        else errorMessage = "Couldn't save your number — please try again."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Skip for now",
                    color = tokens.textTertiary, fontSize = 14.sp,
                    modifier = Modifier
                        .clickable(enabled = !isLoading) { onDone() }
                        .padding(8.dp)
                )
            }
        }
    }
}
