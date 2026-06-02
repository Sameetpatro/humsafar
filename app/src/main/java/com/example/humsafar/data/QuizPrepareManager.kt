package com.example.humsafar.data

import com.example.humsafar.auth.AuthManager
import com.example.humsafar.models.QuizPrepareRequest
import com.example.humsafar.models.QuizStartResponse
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Builds the quiz incrementally while the user explores (7 questions per scanned node).
 * Also caches the /quiz/start response so the quiz screen opens instantly at trip end.
 */
object QuizPrepareManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var tripId: Int? = null
    @Volatile private var body: QuizStartResponse? = null
    @Volatile private var error: String? = null
    @Volatile private var inFlight = false
    @Volatile private var lastPreparedNodes: List<Int> = emptyList()

    /** Call after every node scan — fire-and-forget incremental generation on server. */
    fun prepareOnNodeScan(targetTripId: Int, visitedNodeIds: List<Int>) {
        if (targetTripId == 0 || visitedNodeIds.isEmpty()) return
        if (visitedNodeIds == lastPreparedNodes && tripId == targetTripId) return
        lastPreparedNodes = visitedNodeIds
        tripId = targetTripId
        scope.launch {
            val uid = AuthManager.currentUser.value?.uid ?: return@launch
            runCatching {
                HumsafarClient.api.prepareQuiz(uid, targetTripId, QuizPrepareRequest(visitedNodeIds))
            }
        }
    }

    /** Warm the start-quiz cache (hub screen + final activation). */
    fun preloadStart(targetTripId: Int) {
        if (tripId == targetTripId && (body != null || error != null)) return
        if (inFlight && tripId == targetTripId) return
        tripId = targetTripId
        body = null
        error = null
        inFlight = true
        scope.launch {
            val uid = AuthManager.currentUser.value?.uid
            if (uid.isNullOrBlank()) {
                error = "Sign in to play the quiz."
                inFlight = false
                return@launch
            }
            runCatching {
                val resp = HumsafarClient.api.startQuiz(uid, targetTripId)
                if (resp.isSuccessful && resp.body() != null) {
                    body = resp.body()!!
                } else {
                    error = "Couldn't start the quiz (HTTP ${resp.code()})."
                }
            }.onFailure {
                error = it.message ?: "Network error"
            }
            inFlight = false
        }
    }

    fun consume(targetTripId: Int): QuizStartResponse? {
        if (tripId != targetTripId) return null
        return body.also { body = null }
    }

    fun consumeError(targetTripId: Int): String? {
        if (tripId != targetTripId) return null
        return error.also { error = null }
    }

    fun isReady(targetTripId: Int): Boolean =
        tripId == targetTripId && body != null

    fun isLoading(targetTripId: Int): Boolean =
        tripId == targetTripId && inFlight && body == null && error == null

    fun resetForNewTrip() {
        tripId = null
        body = null
        error = null
        inFlight = false
        lastPreparedNodes = emptyList()
    }
}
