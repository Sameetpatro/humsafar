package com.example.humsafar.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.humsafar.data.TripManager

/**
 * Wraps back navigation during an active trip with a warning that the user will
 * return to the home page while their trip remains active.
 */
@Composable
fun rememberTripSafeBack(onBack: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val tripState by TripManager.state.collectAsStateWithLifecycle()
    val enabled = tripState.isTripActive

    fun handleBack() {
        if (!enabled) {
            onBack()
            return
        }

        val firstNodeId = tripState.visitedNodeIds.firstOrNull()
        val isFirstScannedNode = firstNodeId != null && tripState.currentNodeId == firstNodeId
        if (isFirstScannedNode) {
            Toast.makeText(context, "You can't go back until you end the trip", Toast.LENGTH_SHORT).show()
            return
        }
        onBack()
    }

    BackHandler(enabled = enabled) { handleBack() }

    return { handleBack() }
}
