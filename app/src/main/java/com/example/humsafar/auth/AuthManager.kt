package com.example.humsafar.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
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

    // Firebase UID — "guest_anonymous" if somehow null
    val uid: String
        get() = auth.currentUser?.uid ?: "guest_anonymous"

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    init {
        // Keep StateFlow in sync with Firebase auth state changes
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

        // Save display name to Firebase profile
        val profileUpdate = userProfileChangeRequest {
            displayName = name
        }
        user.updateProfile(profileUpdate).await()

        // NOTE: phone is saved to Firebase profile as photoUrl workaround
        // (Firebase free tier doesn't store custom fields — use your own backend for phone)
        // You can store phone in your PostgreSQL users table via a POST /users endpoint later.
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

    // ── Google Sign In (pass the ID token from GoogleSignInAccount) ───────────
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

    // ── Sign Out ──────────────────────────────────────────────────────────────
    fun signOut() {
        auth.signOut()
        Log.i(TAG, "Signed out")
    }

    // ── Google Sign-In Client helper ──────────────────────────────────────────
    // Call this in your Activity/Composable to get the GoogleSignInClient
    fun getGoogleSignInClient(context: Context) = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("865150467468-p0in0ue0agavb8196s27sdnq5sq9rhdi.apps.googleusercontent.com") // ← replace with your Web Client ID from Firebase Console
            .requestEmail()
            .build()
    )
}