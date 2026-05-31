// app/src/main/java/com/example/humsafar/data/BonusGameManager.kt
//
// Drives the surprise "Bingo" bonus challenge. While the user is inside a site,
// we occasionally ask the backend for an offer (a target node + deadline + a
// random minigame). An in-app banner tells the user to reach + scan that node.
// When the QR flow reports the target node was scanned in time, we launch the
// minigame; solving it asks the backend to award the pre-rolled bonus gems.

package com.example.humsafar.data

import android.util.Log
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.models.BonusCompleteRequest
import com.example.humsafar.models.BonusOfferRequest
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val TAG = "BonusGameManager"
private const val OFFER_COOLDOWN_MS = 10 * 60_000L   // at most one offer / 10 min
private const val OFFER_CHANCE = 0.6                 // probability when eligible

data class BonusOffer(
    val challengeId: Int,
    val siteId: Int,
    val targetNodeId: Int,
    val targetNodeName: String,
    val minigame: String,        // zip | sudoku
    val deadlineMinutes: Int,
    val expiresAtMs: Long
)

data class BonusResult(val rewardGems: Int, val message: String)

object BonusGameManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _offer = MutableStateFlow<BonusOffer?>(null)
    val offer: StateFlow<BonusOffer?> = _offer.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _result = MutableStateFlow<BonusResult?>(null)
    val result: StateFlow<BonusResult?> = _result.asStateFlow()

    @Volatile private var lastOfferMs = 0L
    @Volatile private var requesting = false

    /** Called periodically while the user is inside a site. Self-throttles. */
    fun considerOffer(siteId: Int, currentNodeId: Int?) {
        if (_offer.value != null || _playing.value || requesting) return
        val now = System.currentTimeMillis()
        if (now - lastOfferMs < OFFER_COOLDOWN_MS) return
        if (Random.nextDouble() > OFFER_CHANCE) {
            lastOfferMs = now   // back off this window even when we skip
            return
        }
        val uid = AuthManager.currentUser.value?.uid ?: return

        requesting = true
        scope.launch {
            runCatching {
                val resp = HumsafarClient.api.offerBonus(
                    BonusOfferRequest(firebaseUid = uid, siteId = siteId, excludeNodeId = currentNodeId)
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val b = resp.body()!!
                    lastOfferMs = System.currentTimeMillis()
                    _offer.value = BonusOffer(
                        challengeId = b.challengeId,
                        siteId = siteId,
                        targetNodeId = b.targetNodeId,
                        targetNodeName = b.targetNodeName,
                        minigame = b.minigame,
                        deadlineMinutes = b.deadlineMinutes,
                        expiresAtMs = System.currentTimeMillis() + b.deadlineMinutes * 60_000L
                    )
                }
            }.onFailure { Log.w(TAG, "offerBonus failed: ${it.message}") }
            requesting = false
        }
    }

    /** Called from the QR flow whenever a node is scanned. */
    fun onNodeScanned(nodeId: Int) {
        val o = _offer.value ?: return
        if (System.currentTimeMillis() > o.expiresAtMs) {
            _offer.value = null
            return
        }
        if (nodeId == o.targetNodeId) {
            _playing.value = true
        }
    }

    fun cancelPlaying() {
        _playing.value = false
    }

    /** Minigame solved — claim the bonus gems. */
    fun submitSolved() {
        val o = _offer.value ?: return
        val uid = AuthManager.currentUser.value?.uid ?: return
        scope.launch {
            runCatching {
                val resp = HumsafarClient.api.completeBonus(
                    BonusCompleteRequest(
                        firebaseUid = uid,
                        challengeId = o.challengeId,
                        scannedNodeId = o.targetNodeId,
                        solved = true
                    )
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val b = resp.body()!!
                    if (b.status == "completed") {
                        GamificationRepository.setBalance(b.newBalance)
                        _result.value = BonusResult(b.rewardGems, "You earned ${b.rewardGems} bonus gems!")
                    } else {
                        _result.value = BonusResult(0, "Challenge expired before it could be claimed.")
                    }
                } else {
                    _result.value = BonusResult(0, "Couldn't claim the bonus right now.")
                }
            }.onFailure {
                _result.value = BonusResult(0, "Couldn't claim the bonus right now.")
            }
            _playing.value = false
            _offer.value = null
        }
    }

    fun dismissOffer() { _offer.value = null }
    fun dismissResult() { _result.value = null }
}
