// app/src/main/java/com/example/humsafar/data/UserRepository.kt
//
// Bridges Firebase Auth (client-side identity) to the backend (server-side
// identity). Pattern:
//   1. User signs in via Firebase (email / Google / anonymous)
//   2. As soon as AuthManager.currentUser changes, we call POST /users/register
//      with the firebase_uid + profile fields
//   3. Backend upserts the row and returns the canonical UserResponse
//   4. Without this, every backend call that uses firebase_uid (trips/start,
//      reviews/submit, chat history persistence) will 404 with "User not found"
//
// The registration is idempotent on the backend, but we still cache the last
// successfully-registered uid in SharedPreferences to avoid hammering the
// endpoint on every app foreground.

package com.example.humsafar.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.humsafar.models.UserRegisterRequest
import com.example.humsafar.network.HumsafarClient
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG          = "UserRepository"
private const val PREFS_NAME   = "humsafar_user_repo"
private const val KEY_LAST_UID = "last_registered_uid"

object UserRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private lateinit var prefs: SharedPreferences

    /**
     * State flow that screens can observe to know whether the current Firebase
     * user has been successfully synced to the backend yet.
     */
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Fire-and-forget. Registers/updates the Firebase user with the backend.
     * Safe to call multiple times — guarded by a mutex and a SharedPreferences
     * cache of the last successfully-synced uid.
     */
    fun syncFirebaseUser(user: FirebaseUser?) {
        if (user == null) {
            _isRegistered.value = false
            return
        }
        scope.launch { syncFirebaseUserSuspend(user) }
    }

    /**
     * Suspend version for callers (e.g. login flow) that want to wait for the
     * sync to complete before proceeding.
     */
    suspend fun syncFirebaseUserSuspend(user: FirebaseUser): Boolean = mutex.withLock {
        val uid = user.uid
        val cached = prefs.getString(KEY_LAST_UID, null)
        if (cached == uid && _isRegistered.value) {
            Log.d(TAG, "User $uid already registered (cached), skipping")
            return@withLock true
        }

        return@withLock try {
            val request = UserRegisterRequest(
                firebaseUid   = uid,
                displayName   = user.displayName?.takeIf { it.isNotBlank() },
                email         = user.email?.takeIf { it.isNotBlank() },
                phone         = user.phoneNumber?.takeIf { it.isNotBlank() },
                avatarUrl     = user.photoUrl?.toString(),
                preferredLang = "en-IN",
                isAnonymous   = user.isAnonymous
            )
            val resp = HumsafarClient.api.registerUser(request)
            if (resp.isSuccessful && resp.body() != null) {
                prefs.edit().putString(KEY_LAST_UID, uid).apply()
                _isRegistered.value = true
                Log.i(TAG, "Registered backend user uid=$uid id=${resp.body()!!.id}")
                true
            } else {
                Log.w(TAG, "registerUser failed: HTTP ${resp.code()} ${resp.message()}")
                _isRegistered.value = false
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerUser exception: ${e.message}", e)
            _isRegistered.value = false
            false
        }
    }

    fun clear() {
        if (::prefs.isInitialized) {
            prefs.edit().remove(KEY_LAST_UID).apply()
        }
        _isRegistered.value = false
    }
}
