package com.example.humsafar.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthManager"

object AuthManager {

    private val auth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    val uid: String
        get() = auth.currentUser?.uid ?: "guest_anonymous"

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    // Cached Google Sign-In client — recreated only when context changes
    private var googleSignInClient: GoogleSignInClient? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            Log.d(TAG, "Auth state changed: uid=${firebaseAuth.currentUser?.uid}")
        }
    }

    // ── Email Sign Up ─────────────────────────────────────────────────────────
    suspend fun signUp(
        name: String,
        email: String,
        phone: String,
        password: String
    ): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user!!

        val profileUpdate = userProfileChangeRequest {
            displayName = name
        }
        user.updateProfile(profileUpdate).await()

        Log.i(TAG, "Sign up success: uid=${user.uid} name=$name email=$email phone=$phone")
        user
    }

    // ── Email Sign In ─────────────────────────────────────────────────────────
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user!!
        Log.i(TAG, "Sign in success: uid=${user.uid}")
        user
    }

    // ── Google Sign In ────────────────────────────────────────────────────────
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user!!
        Log.i(TAG, "Google sign in success: uid=${user.uid}")
        user
    }

    // ── Anonymous / Guest ─────────────────────────────────────────────────────
    suspend fun signInAnonymously(): Result<FirebaseUser> = runCatching {
        val result = auth.signInAnonymously().await()
        val user = result.user!!
        Log.i(TAG, "Anonymous sign in: uid=${user.uid}")
        user
    }

    // ── Password Reset ────────────────────────────────────────────────────────
    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Log.i(TAG, "Password reset email sent to $email")
    }

    // ── Email Link (Passwordless) Login ───────────────────────────────────────
    suspend fun sendSignInLinkToEmail(email: String, context: Context): Result<Unit> = runCatching {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://dharoharsetu.firebaseapp.com/__/auth/action")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                context.packageName,
                true,
                null
            )
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings).await()

        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("pending_email_link_email", email)
            .apply()

        Log.i(TAG, "Sign-in link sent to $email")
    }

    suspend fun signInWithEmailLink(email: String, emailLink: String): Result<FirebaseUser> = runCatching {
        if (!auth.isSignInWithEmailLink(emailLink)) {
            throw IllegalArgumentException("Invalid email link")
        }

        val result = auth.signInWithEmailLink(email, emailLink).await()
        val user = result.user!!
        Log.i(TAG, "Email link sign in success: uid=${user.uid}")
        user
    }

    fun isSignInWithEmailLink(link: String): Boolean {
        return auth.isSignInWithEmailLink(link)
    }

    fun getPendingEmailLinkEmail(context: Context): String? {
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("pending_email_link_email", null)
    }

    fun clearPendingEmailLinkEmail(context: Context) {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("pending_email_link_email")
            .apply()
    }

    // ── Sign Out ──────────────────────────────────────────────────────────────
    fun signOut(context: Context? = null) {
        auth.signOut()
        // Also sign out from Google so next login shows account picker
        context?.let {
            googleSignInClient?.signOut()
            googleSignInClient = null
        }
        Log.i(TAG, "Signed out")
    }

    // Overload for callers that don't have context
    fun signOut() {
        auth.signOut()
        googleSignInClient = null
        Log.i(TAG, "Signed out (no Google client reset)")
    }

    // ── Google Sign-In Client ─────────────────────────────────────────────────
    // FIX: Sign out the previous Google session before returning the intent,
    // so the account chooser always appears (avoids silent stale-token reuse).
    fun getGoogleSignInIntent(context: Context) =
        buildGoogleClient(context).also { client ->
            // Sign out silently so the picker always shows fresh
            client.signOut()
        }.signInIntent

    // Helper kept for backward compat — returns the client
    fun getGoogleSignInClient(context: Context): GoogleSignInClient =
        buildGoogleClient(context)

    private fun buildGoogleClient(context: Context): GoogleSignInClient {
        // Reuse cached client for the same context to avoid multiple GSI initializations
        return googleSignInClient ?: GoogleSignIn.getClient(
            context.applicationContext,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                // ⚠️ Replace this with your actual Web client ID from Firebase console
                // Firebase Console → Project Settings → Your Apps → Web API key
                // OR Google Cloud Console → APIs → Credentials → OAuth 2.0 Client IDs → Web client
                .requestIdToken("865150467468-p0in0ue0agavb8196s27sdnq5sq9rhdi.apps.googleusercontent.com")
                .requestEmail()
                .build()
        ).also { googleSignInClient = it }
    }
}