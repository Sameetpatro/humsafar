package com.example.humsafar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humsafar.ui.theme.*

@Composable
fun SignUpScreen(
    onLoginClick: () -> Unit,
    onBypassClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBlue),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {

            Text(
                text = "Create Account",
                color = AccentYellow,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentYellow,
                    contentColor = NavyBlue
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Up")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { /* Google Sign In placeholder */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onLoginClick) {
                Text("Already have an account? Login", color = AccentYellow)
            }

            TextButton(onClick = onBypassClick) {
                Text("Continue as Guest", color = AccentYellow)
            }
        }
    }
}
