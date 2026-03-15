package com.example.humsafar.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
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
    // IMPORTANT: Replace 'dharoharsetu' with YOUR Firebase Project ID
    suspend fun sendSignInLinkToEmail(email: String, context: Context): Result<Unit> = runCatching {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            // ⚠️ CHANGE 'dharoharsetu' to YOUR Firebase Project ID
            .setUrl("https://dharoharsetu.firebaseapp.com/__/auth/action")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                context.packageName,
                true,  // Install app if not available
                null   // Minimum version
            )
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings).await()

        // Save email locally for later retrieval
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
    fun signOut() {
        auth.signOut()
        Log.i(TAG, "Signed out")
    }

    // ── Google Sign-In Client ─────────────────────────────────────────────────
    fun getGoogleSignInClient(context: Context) = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("865150467468-p0in0ue0agavb8196s27sdnq5sq9rhdi.apps.googleusercontent.com")
            .requestEmail()
            .build()
    )
}