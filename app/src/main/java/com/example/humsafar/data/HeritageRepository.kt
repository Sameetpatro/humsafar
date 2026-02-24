package com.example.humsafar.data

import com.example.humsafar.models.HeritageSite
import com.example.humsafar.models.Monument
import com.example.humsafar.network.SiteClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HeritageRepository {

    // ── Live data from Spring Boot ────────────────────────────────────────────
    private val _monuments = MutableStateFlow<List<Monument>>(emptyList())
    val monuments: StateFlow<List<Monument>> = _monuments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Fetch all monuments from Spring Boot. Call this once from MapScreen.
     * Results flow through [monuments] StateFlow.
     */
    suspend fun loadMonuments() {
        _isLoading.value = true
        _error.value = null
        try {
            val resp = SiteClient.api.getAllMonuments()
            if (resp.isSuccessful) {
                val list = resp.body() ?: emptyList()
                _monuments.value = list
                // Keep geofence sites in sync
                _sites = list.map { m ->
                    HeritageSite(
                        id        = m.id.toString(),
                        name      = m.name,
                        latitude  = m.latitude,
                        longitude = m.longitude,
                        radius    = 300.0   // default geofence radius
                    )
                }
            } else {
                _error.value = "Failed to load sites (${resp.code()})"
            }
        } catch (e: Exception) {
            _error.value = "Network error: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    // ── HeritageSite list for geofencing / MapScreen ──────────────────────────
    // Seeded with hardcoded fallback; gets replaced when loadMonuments() succeeds.
    private var _sites: List<HeritageSite> = listOf(
        HeritageSite("1",  "IIIT Sonepat",      28.989545, 77.151057, 300.0),
        HeritageSite("2",  "Red Fort",           28.6562,   77.2410,   400.0),
        HeritageSite("3",  "Taj Mahal",          27.1751,   78.0421,   500.0),
        HeritageSite("4",  "India Gate",         28.6129,   77.2295,   350.0),
        HeritageSite("5",  "Qutub Minar",        28.5244,   77.1855,   350.0),
        HeritageSite("6",  "Konark Sun Temple",  19.8876,   86.0945,   450.0),
        HeritageSite("7",  "Gateway of India",   18.9218,   72.8347,   300.0),
        HeritageSite("8",  "Hampi",              15.3350,   76.4600,   800.0),
        HeritageSite("9",  "Golden Temple",      31.6200,   74.8765,   400.0),
        HeritageSite("10", "Mysore Palace",      12.3052,   76.6552,   400.0)
    )

    /** Used by MapScreen, geofencing, and location tracking */
    val sites: List<HeritageSite> get() = _sites
}