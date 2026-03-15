package com.example.humsafar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.data.TripManager
import com.example.humsafar.navigation.AppNavigation
import com.example.humsafar.ui.theme.HumsafarTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TripManager.init(applicationContext)

        // Handle email link if present
        handleEmailLinkIntent(intent)

        setContent {
            HumsafarTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEmailLinkIntent(intent)
    }

    private fun handleEmailLinkIntent(intent: Intent?) {
        val link = intent?.data?.toString() ?: return

        Log.d("MainActivity", "Received deep link: $link")

        if (AuthManager.isSignInWithEmailLink(link)) {
            Log.d("MainActivity", "Valid email link detected")

            // Get the email from SharedPreferences
            val email = AuthManager.getPendingEmailLinkEmail(this)

            if (email != null) {
                // Sign in with the link
                lifecycleScope.launch {
                    AuthManager.signInWithEmailLink(email, link)
                        .onSuccess { user ->
                            Log.i("MainActivity", "Email link sign-in success: ${user.uid}")
                            AuthManager.clearPendingEmailLinkEmail(this@MainActivity)
                            // Navigation will automatically update via AuthManager.currentUser flow
                        }
                        .onFailure { error ->
                            Log.e("MainActivity", "Email link sign-in failed", error)
                            // Show error to user (you can use a Toast or Snackbar)
                            android.widget.Toast.makeText(
                                this@MainActivity,
                                "Sign-in failed: ${error.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                }
            } else {
                // Email not found - ask user to enter it
                Log.w("MainActivity", "Email not found in storage, user needs to re-enter")
                // You can show a dialog or navigate to a screen to collect email
                // For now, show a toast
                android.widget.Toast.makeText(
                    this,
                    "Please enter your email to complete sign-in",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}