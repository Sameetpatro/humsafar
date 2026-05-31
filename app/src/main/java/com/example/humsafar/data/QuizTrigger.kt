// app/src/main/java/com/example/humsafar/data/QuizTrigger.kt
//
// Bridges "the user left the site geofence while on a trip" to "show the final
// quiz". When that happens we end the trip on the server and stash a PendingQuiz;
// AppNavigation observes it and routes to the quiz (immediately when foreground,
// or on the next resume if the exit happened in the background).
//
// handledTripId dedupes so a trip can only ever queue its quiz once (and so the
// manual end-trip path can claim it via markHandled()).

package com.example.humsafar.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingQuiz(
    val tripId: Int,
    val siteId: Int,
    val siteName: String,
    val visitedCount: Int,
    val totalCount: Int
)

object QuizTrigger {

    private val _pending = MutableStateFlow<PendingQuiz?>(null)
    val pending: StateFlow<PendingQuiz?> = _pending.asStateFlow()

    @Volatile private var handledTripId = -1

    /** Called when the user exits a site geofence. No-op unless a matching trip is active. */
    fun onSiteExit(exitedSiteId: Int) {
        val trip = TripManager.current()
        if (!trip.isTripActive || trip.tripId == 0) return
        if (trip.siteId != exitedSiteId) return
        if (handledTripId == trip.tripId) return

        handledTripId = trip.tripId
        TripManager.endTripOnServer()
        val visited = trip.visitedNodeIds.size
        _pending.value = PendingQuiz(
            tripId = trip.tripId,
            siteId = trip.siteId,
            siteName = trip.siteName,
            visitedCount = visited,
            totalCount = visited.coerceAtLeast(1)
        )
    }

    /** Mark a trip's quiz as already routed (e.g. via the manual End Trip button). */
    fun markHandled(tripId: Int) {
        handledTripId = tripId
    }

    fun consume() {
        _pending.value = null
    }
}
