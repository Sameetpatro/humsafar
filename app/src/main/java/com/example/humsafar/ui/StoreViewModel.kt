package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.data.GamificationRepository
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.Coupon
import com.example.humsafar.models.CouponPurchaseRequest
import com.example.humsafar.models.StorePartner
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** tier -> (gem price, discount range). Mirrors the backend TIERS map. */
object Tiers {
    val price = mapOf("ultimate" to 200, "special" to 120, "normal" to 70)
    val range = mapOf("ultimate" to (20..30), "special" to (12..19), "normal" to (7..11))
    val label = mapOf("ultimate" to "Ultimate Special", "special" to "Special", "normal" to "Normal")
}

data class SpinWheelData(
    val partnerLabels: List<String>,
    val partnerIndex: Int,
    val discountOptions: List<Int>,
    val discountIndex: Int,
    val coupon: Coupon
)

sealed interface SpinState {
    /** Wheels show preview labels; user picks hotel/restaurant then spins each wheel. */
    data class Ready(
        val tier: String,
        val kind: String,
        val siteId: Int?,
        val partnerLabels: List<String>,
        val discountOptions: List<Int>,
        val gemsPrice: Int
    ) : SpinState

    data object PartnerSpinning : SpinState
    data class PartnerDone(val pending: SpinWheelData) : SpinState
    data object DiscountSpinning : SpinState
    data class Complete(val pending: SpinWheelData) : SpinState
    data class Error(val message: String) : SpinState
}

class StoreViewModel : ViewModel() {

    private val _spin = MutableStateFlow<SpinState>(SpinState.Ready("", "hotel", null, emptyList(), emptyList(), 0))
    val spin: StateFlow<SpinState> = _spin.asStateFlow()

    private val _coupons = MutableStateFlow<List<Coupon>>(emptyList())
    val coupons: StateFlow<List<Coupon>> = _coupons.asStateFlow()

    private var partners: List<StorePartner> = emptyList()
    private var currentTier = "normal"
    private var currentKind = "hotel"
    private var currentSiteId: Int? = null

    fun loadCoupons() {
        val uid = AuthManager.currentUser.value?.uid ?: return
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.getMyCoupons(uid)
                if (resp.isSuccessful) _coupons.value = resp.body() ?: emptyList()
            }
        }
    }

    /** Load partner names onto the partner wheel before spinning. */
    fun prepare(tier: String, kind: String, siteId: Int?) {
        currentTier = tier
        currentKind = kind
        currentSiteId = siteId
        val range = Tiers.range[tier] ?: (7..11)
        val discounts = range.toList()
        _spin.value = SpinState.Ready(
            tier = tier,
            kind = kind,
            siteId = siteId,
            partnerLabels = emptyList(),
            discountOptions = discounts,
            gemsPrice = Tiers.price[tier] ?: 70
        )
        reloadPartnerPreview(kind, siteId)
    }

    fun setPartnerKind(kind: String) {
        if (currentKind == kind) return
        currentKind = kind
        val cur = _spin.value
        if (cur is SpinState.Ready) {
            _spin.value = cur.copy(kind = kind)
            reloadPartnerPreview(kind, currentSiteId)
        }
    }

    private fun reloadPartnerPreview(kind: String, siteId: Int?) {
        val trip = TripManager.current()
        val lat = trip.lastLat.takeIf { it != 0.0 }
        val lng = trip.lastLng.takeIf { it != 0.0 }
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.getStorePartners(siteId, kind, lat, lng)
                if (resp.isSuccessful) {
                    partners = resp.body() ?: emptyList()
                    val labels = buildPreviewLabels(partners.map { it.name })
                    val cur = _spin.value
                    if (cur is SpinState.Ready) {
                        _spin.value = cur.copy(partnerLabels = labels, kind = kind)
                    }
                }
            }
        }
    }

    /** Step 1: purchase coupon + spin partner wheel to the server result. */
    fun spinPartnerWheel() {
        val ready = _spin.value as? SpinState.Ready ?: return
        val uid = AuthManager.currentUser.value?.uid
        if (uid.isNullOrBlank()) {
            _spin.value = SpinState.Error("Sign in to buy coupons.")
            return
        }
        _spin.value = SpinState.PartnerSpinning
        val trip = TripManager.current()
        val lat = trip.lastLat.takeIf { it != 0.0 }
        val lng = trip.lastLng.takeIf { it != 0.0 }

        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.purchaseCoupon(
                    CouponPurchaseRequest(
                        firebaseUid = uid,
                        tier = ready.tier,
                        partnerKind = ready.kind,
                        siteId = ready.siteId,
                        userLat = lat,
                        userLng = lng
                    )
                )
                if (!resp.isSuccessful || resp.body() == null) {
                    _spin.value = SpinState.Error(
                        when (resp.code()) {
                            402 -> "Not enough gems for this coupon."
                            404 -> "No ${ready.kind} partners available near here."
                            else -> "Purchase failed (HTTP ${resp.code()})."
                        }
                    )
                    return@launch
                }
                val body = resp.body()!!
                GamificationRepository.setBalance(body.newBalance)
                val coupon = body.coupon

                val labels = buildPreviewLabels(
                    partners.map { it.name }.ifEmpty { listOf(coupon.partnerName) },
                    winner = coupon.partnerName
                )
                val pIdx = labels.indexOf(coupon.partnerName).coerceAtLeast(0)
                val discounts = ready.discountOptions.ifEmpty {
                    (Tiers.range[ready.tier] ?: (7..11)).toList()
                }
                val dIdx = discounts.indexOf(coupon.discountPct).coerceAtLeast(0)

                _spin.value = SpinState.PartnerDone(
                    SpinWheelData(labels, pIdx, discounts, dIdx, coupon)
                )
            }.onFailure {
                _spin.value = SpinState.Error(it.message ?: "Network error")
            }
        }
    }

    /** Step 2: animate discount wheel (result already decided on purchase). */
    fun spinDiscountWheel() {
        val done = _spin.value as? SpinState.PartnerDone ?: return
        _spin.value = SpinState.DiscountSpinning
        viewModelScope.launch {
            kotlinx.coroutines.delay(2600)
            _spin.value = SpinState.Complete(done.pending)
        }
    }

    private fun buildPreviewLabels(names: List<String>, winner: String? = null): List<String> {
        val base = names.filter { it.isNotBlank() }.distinct().take(8).toMutableList()
        if (base.isEmpty()) base.add(winner ?: "Partner")
        if (winner != null && winner !in base) {
            if (base.size >= 8) base[0] = winner else base.add(0, winner)
        }
        return base.map { shortenLabel(it) }
    }

    private fun shortenLabel(name: String): String {
        val words = name.split(" ").filter { it.isNotBlank() }
        return when {
            name.length <= 14 -> name
            words.size >= 2 -> words.take(2).joinToString("\n")
            else -> name.take(12) + "…"
        }
    }

    fun reset() {
        _spin.value = SpinState.Ready("", "hotel", null, emptyList(), emptyList(), 0)
    }
}
