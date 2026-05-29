// app/src/main/java/com/example/humsafar/data/StatsRepository.kt
//
// Tracks app-wide live stats from the backend:
//   - active_users    : explorers active in the last few minutes
//   - lifetime_visits : cumulative app opens (all time)
//   - total_users     : distinct registered users
//
// On cold start we POST /stats/visit once (bumps the lifetime counter), then a
// lightweight heartbeat loop keeps the current user counted as "active now" and
// refreshes the figures every HEARTBEAT_INTERVAL_MS for any UI observing them.

package com.example.humsafar.data

import android.util.Log
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.models.LiveStats
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "StatsRepository"
private const val HEARTBEAT_INTERVAL_MS = 60_000L

object StatsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _stats = MutableStateFlow(LiveStats())
    val stats: StateFlow<LiveStats> = _stats.asStateFlow()

    @Volatile private var started = false

    /** Call once from MainActivity. Records the cold-start visit + starts heartbeat. */
    fun start() {
        if (started) return
        started = true

        scope.launch {
            val uid = AuthManager.currentUser.value?.uid
            runCatching {
                val resp = HumsafarClient.api.recordVisit(uid)
                if (resp.isSuccessful) resp.body()?.let { _stats.value = it }
            }.onFailure { Log.w(TAG, "recordVisit failed: ${it.message}") }
        }

        scope.launch {
            while (isActive) {
                val uid = AuthManager.currentUser.value?.uid
                runCatching {
                    val resp = HumsafarClient.api.heartbeat(uid)
                    if (resp.isSuccessful) resp.body()?.let { _stats.value = it }
                }.onFailure { Log.w(TAG, "heartbeat failed: ${it.message}") }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    /** One-off refresh, e.g. when a stats-bearing screen opens. */
    fun refresh() {
        scope.launch {
            runCatching {
                val resp = HumsafarClient.api.getLiveStats()
                if (resp.isSuccessful) resp.body()?.let { _stats.value = it }
            }.onFailure { Log.w(TAG, "refresh failed: ${it.message}") }
        }
    }
}
