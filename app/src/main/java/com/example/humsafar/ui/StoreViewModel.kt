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

sealed interface SpinState {
    data object Idle : SpinState
    data object Spinning : SpinState
    data class Result(
        val coupon: Coupon,
        val partnerLabels: List<String>,
        val partnerIndex: Int,
        val discountOptions: List<Int>,
        val discountIndex: Int
    ) : SpinState
    data class Error(val message: String) : SpinState
}

class StoreViewModel : ViewModel() {

    private val _spin = MutableStateFlow<SpinState>(SpinState.Idle)
    val spin: StateFlow<SpinState> = _spin.asStateFlow()

    private val _coupons = MutableStateFlow<List<Coupon>>(emptyList())
    val coupons: StateFlow<List<Coupon>> = _coupons.asStateFlow()

    private var partners: List<StorePartner> = emptyList()

    fun loadCoupons() {
        val uid = AuthManager.currentUser.value?.uid ?: return
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.getMyCoupons(uid)
                if (resp.isSuccessful) _coupons.value = resp.body() ?: emptyList()
            }
        }
    }

    fun spin(tier: String, kind: String, siteId: Int?) {
        val uid = AuthManager.currentUser.value?.uid
        if (uid.isNullOrBlank()) {
            _spin.value = SpinState.Error("Sign in to buy coupons.")
            return
        }
        _spin.value = SpinState.Spinning
        val trip = TripManager.current()
        val lat = trip.lastLat.takeIf { it != 0.0 }
        val lng = trip.lastLng.takeIf { it != 0.0 }

        viewModelScope.launch {
            // Wheel segment labels (cosmetic) — fetched best-effort.
            runCatching {
                val pResp = HumsafarClient.api.getStorePartners(siteId, kind, lat, lng)
                if (pResp.isSuccessful) partners = pResp.body() ?: emptyList()
            }

            runCatching {
                val resp = HumsafarClient.api.purchaseCoupon(
                    CouponPurchaseRequest(
                        firebaseUid = uid,
                        tier = tier,
                        partnerKind = kind,
                        siteId = siteId,
                        userLat = lat,
                        userLng = lng
                    )
                )
                if (!resp.isSuccessful || resp.body() == null) {
                    _spin.value = SpinState.Error(
                        if (resp.code() == 402) "Not enough gems for this coupon."
                        else if (resp.code() == 404) "No $kind partners available near here."
                        else "Purchase failed (HTTP ${resp.code()})."
                    )
                    return@launch
                }
                val body = resp.body()!!
                GamificationRepository.setBalance(body.newBalance)

                val coupon = body.coupon
                // Build the partner wheel labels, guaranteeing the winner is on it.
                val labels = partners.map { it.name }.toMutableList()
                if (labels.isEmpty()) labels.add(coupon.partnerName)
                var pIdx = labels.indexOf(coupon.partnerName)
                if (pIdx < 0) {
                    labels[0] = coupon.partnerName
                    pIdx = 0
                }
                val cappedLabels = labels.take(8).toMutableList()
                if (pIdx >= cappedLabels.size) {
                    cappedLabels[cappedLabels.size - 1] = coupon.partnerName
                    pIdx = cappedLabels.size - 1
                }

                val range = Tiers.range[tier] ?: (5..30)
                val discountOptions = range.toList()
                val dIdx = discountOptions.indexOf(coupon.discountPct).coerceAtLeast(0)

                _spin.value = SpinState.Result(coupon, cappedLabels, pIdx, discountOptions, dIdx)
            }.onFailure {
                _spin.value = SpinState.Error(it.message ?: "Network error")
            }
        }
    }

    fun reset() {
        _spin.value = SpinState.Idle
    }
}
