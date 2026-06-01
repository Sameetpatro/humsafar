// app/src/main/java/com/example/humsafar/data/BonusGameManager.kt
//
// Drives the surprise "Bingo" bonus challenge. After the user has spent
// [BONUS_OFFER_AFTER_MINUTES] of foreground time in the app (once ever), we ask
// the backend for an offer (target node + deadline + minigame). An in-app banner
// tells them to reach + scan that node; solving the minigame awards bonus gems.

package com.example.humsafar.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.models.BonusCompleteRequest
import com.example.humsafar.models.BonusOfferRequest
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "BonusGameManager"
private const val PREFS_NAME = "bonus_game_prefs"
private const val KEY_ACCUMULATED_MS = "accumulated_foreground_ms"
private const val KEY_TIME_OFFER_USED = "time_based_offer_used"

object BonusGameManager {

    // ── Change this one value to adjust when the bonus appears ───────────────
    /** Minutes of total foreground app time before the one-time bonus offer. */
    const val BONUS_OFFER_AFTER_MINUTES = 30

    private val BONUS_OFFER_AFTER_MS = BONUS_OFFER_AFTER_MINUTES * 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context

    private val _offer = MutableStateFlow<BonusOffer?>(null)
    val offer: StateFlow<BonusOffer?> = _offer.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _result = MutableStateFlow<BonusResult?>(null)
    val result: StateFlow<BonusResult?> = _result.asStateFlow()

    @Volatile private var requesting = false
    @Volatile private var foregroundStartMs: Long? = null
    @Volatile private var tickJob: Job? = null
    @Volatile private var thresholdReached = false

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        thresholdReached = accumulatedForegroundMs() >= BONUS_OFFER_AFTER_MS
        com.example.humsafar.notifications.BonusNotificationHelper.ensureChannel(appContext)
    }

    /** Call from MainActivity when the app comes to the foreground. */
    fun onAppForeground() {
        if (timeBasedOfferUsed()) return
        foregroundStartMs = System.currentTimeMillis()
        startForegroundTicker()
        maybeRequestOffer(ActiveSiteManager.activeSiteId)
    }

    /** Call from MainActivity when the app goes to the background. */
    fun onAppBackground() {
        flushForegroundSession()
        tickJob?.cancel()
        tickJob = null
    }

    /** Called when the user enters a site — covers the case where 5 min passed off-site. */
    fun onSiteEntered(siteId: Int) {
        if (thresholdReached && !timeBasedOfferUsed()) {
            maybeRequestOffer(siteId)
        }
    }

    private fun startForegroundTicker() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (isActive) {
                delay(1_000)
                if (timeBasedOfferUsed()) break
                val total = currentForegroundMs()
                if (total >= BONUS_OFFER_AFTER_MS) {
                    thresholdReached = true
                    maybeRequestOffer(ActiveSiteManager.activeSiteId)
                    break
                }
            }
        }
    }

    private fun flushForegroundSession() {
        val start = foregroundStartMs ?: return
        val elapsed = System.currentTimeMillis() - start
        foregroundStartMs = null
        if (elapsed <= 0) return
        val newTotal = accumulatedForegroundMs() + elapsed
        prefs.edit { putLong(KEY_ACCUMULATED_MS, newTotal) }
        if (newTotal >= BONUS_OFFER_AFTER_MS) thresholdReached = true
    }

    private fun currentForegroundMs(): Long {
        val start = foregroundStartMs
        val session = if (start != null) System.currentTimeMillis() - start else 0L
        return accumulatedForegroundMs() + session
    }

    private fun accumulatedForegroundMs(): Long =
        if (::prefs.isInitialized) prefs.getLong(KEY_ACCUMULATED_MS, 0L) else 0L

    private fun timeBasedOfferUsed(): Boolean =
        if (::prefs.isInitialized) prefs.getBoolean(KEY_TIME_OFFER_USED, false) else false

    private fun markTimeBasedOfferUsed() {
        prefs.edit { putBoolean(KEY_TIME_OFFER_USED, true) }
        tickJob?.cancel()
        tickJob = null
    }

    /** Request the one-time bonus offer from the backend (requires an active site). */
    private fun maybeRequestOffer(siteId: Int?) {
        if (siteId == null) return
        if (timeBasedOfferUsed()) return
        if (!thresholdReached && currentForegroundMs() < BONUS_OFFER_AFTER_MS) return
        if (_offer.value != null || _playing.value || requesting) return

        val uid = AuthManager.currentUser.value?.uid ?: return
        requesting = true
        val excludeNode = ActiveSiteManager.activeNodeId.value

        scope.launch {
            runCatching {
                val resp = HumsafarClient.api.offerBonus(
                    BonusOfferRequest(
                        firebaseUid = uid,
                        siteId = siteId,
                        excludeNodeId = excludeNode
                    )
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val b = resp.body()!!
                    markTimeBasedOfferUsed()
                    flushForegroundSession()
                    _offer.value = BonusOffer(
                        challengeId = b.challengeId,
                        siteId = siteId,
                        siteName = b.siteName.ifBlank { ActiveSiteManager.activeSiteName },
                        targetNodeId = b.targetNodeId,
                        targetNodeName = b.targetNodeName,
                        minigame = b.minigame,
                        rewardGems = b.rewardGems,
                        deadlineMinutes = b.deadlineMinutes,
                        expiresAtMs = System.currentTimeMillis() + b.deadlineMinutes * 60_000L
                    )
                    if (::appContext.isInitialized) {
                        com.example.humsafar.notifications.BonusNotificationHelper.showBonusOffer(
                            appContext, b.targetNodeName, b.deadlineMinutes
                        )
                    }
                    Log.i(TAG, "Bonus offer shown after ${BONUS_OFFER_AFTER_MINUTES} min in app")
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

    fun dismissOffer() {
        _offer.value = null
        if (::appContext.isInitialized) {
            com.example.humsafar.notifications.BonusNotificationHelper.cancel(appContext)
        }
    }
    fun dismissResult() { _result.value = null }

    /** DEBUG: clears persisted timer + one-time flag so you can re-test the offer. */
    fun resetTimeBasedOfferForTesting() {
        if (::prefs.isInitialized) {
            prefs.edit {
                remove(KEY_ACCUMULATED_MS)
                remove(KEY_TIME_OFFER_USED)
            }
        }
        thresholdReached = false
        foregroundStartMs = null
        tickJob?.cancel()
        tickJob = null
    }
}

data class BonusOffer(
    val challengeId: Int,
    val siteId: Int,
    val siteName: String,
    val targetNodeId: Int,
    val targetNodeName: String,
    val minigame: String,
    val rewardGems: Int,
    val deadlineMinutes: Int,
    val expiresAtMs: Long
)

data class BonusResult(val rewardGems: Int, val message: String)
