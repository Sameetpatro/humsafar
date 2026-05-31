// app/src/main/java/com/example/humsafar/data/GamificationRepository.kt
//
// Holds the user's live gem balance and refreshes it from the backend. Quiz,
// store, and bonus flows call refresh() (or setBalance with a known value from
// an API response) so the gems chip across the app stays current.

package com.example.humsafar.data

import android.util.Log
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "GamificationRepo"

object GamificationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _gems = MutableStateFlow(0)
    val gems: StateFlow<Int> = _gems.asStateFlow()

    /** Refresh from the backend for the currently signed-in user. */
    fun refresh() {
        val uid = AuthManager.currentUser.value?.uid ?: return
        scope.launch {
            runCatching {
                val resp = HumsafarClient.api.getGems(uid)
                if (resp.isSuccessful) resp.body()?.let { _gems.value = it.gems }
            }.onFailure { Log.w(TAG, "gems refresh failed: ${it.message}") }
        }
    }

    /** Update the cached balance immediately from a fresh API response. */
    fun setBalance(value: Int) {
        _gems.value = value
    }
}
