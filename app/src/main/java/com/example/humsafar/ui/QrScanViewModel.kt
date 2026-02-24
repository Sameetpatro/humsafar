package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.NodeScanResponse
import com.example.humsafar.network.SiteClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class QrScanViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<QrUiState>(QrUiState.Scanning)
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()

    private val isProcessing = AtomicBoolean(false)

    // Called when ML Kit detects a QR — nodeId is encoded in QR as just the number
    fun onQrDetected(rawQr: String) {
        if (!isProcessing.compareAndSet(false, true)) return
        val nodeId = rawQr.trim().toLongOrNull() ?: run {
            _uiState.value = QrUiState.Error("Invalid QR code format")
            isProcessing.set(false)
            return
        }
        viewModelScope.launch {
            _uiState.value = QrUiState.Validating
            try {
                val resp = SiteClient.api.scanNode(nodeId)
                if (resp.isSuccessful) {
                    val data = resp.body()!!
                    // If KING node → start session
                    if (data.node.nodeType == "KING" && !TripManager.isActive()) {
                        val sessionResp = SiteClient.api.startSession(
                            firebaseUid = TripManager.USER_ID,
                            monumentId  = data.node.monumentId
                        )
                        if (sessionResp.isSuccessful) {
                            TripManager.activateTrip(
                                monumentId   = data.node.monumentId,
                                monumentName = data.node.name,
                                nodeId       = data.node.id,
                                nodeName     = data.node.name
                            )
                        }
                    } else if (TripManager.isActive()) {
                        TripManager.updateCurrentNode(data.node)
                    }
                    _uiState.value = QrUiState.Success(data)
                } else {
                    _uiState.value = QrUiState.Error("Node not found: ${resp.code()}")
                    isProcessing.set(false)
                }
            } catch (e: Exception) {
                _uiState.value = QrUiState.Error("Network error: ${e.message}")
                isProcessing.set(false)
            }
        }
    }

    // Trouble scanning — fetch nodes by location
    fun fetchNearbyNodes(lat: Double, lng: Double, monumentId: Long) {
        viewModelScope.launch {
            _uiState.value = QrUiState.FetchingLocation
            try {
                val resp = SiteClient.api.getNodesForMonument(monumentId)
                if (resp.isSuccessful) {
                    val nodes = resp.body()!!
                    TripManager.cachedNodes = nodes
                    _uiState.value = QrUiState.ShowingNodes(nodes)
                } else {
                    _uiState.value = QrUiState.Error("Could not fetch nodes")
                }
            } catch (e: Exception) {
                _uiState.value = QrUiState.Error("Network error: ${e.message}")
            }
        }
    }

    fun selectNodeManually(nodeId: Long) {
        if (!isProcessing.compareAndSet(false, true)) return
        viewModelScope.launch {
            _uiState.value = QrUiState.Validating
            try {
                val resp = SiteClient.api.scanNode(nodeId)
                if (resp.isSuccessful) {
                    val data = resp.body()!!
                    if (data.node.nodeType == "KING" && !TripManager.isActive()) {
                        SiteClient.api.startSession(TripManager.USER_ID, data.node.monumentId)
                        TripManager.activateTrip(data.node.monumentId, data.node.name, data.node.id, data.node.name)
                    } else if (TripManager.isActive()) {
                        TripManager.updateCurrentNode(data.node)
                    }
                    _uiState.value = QrUiState.Success(data)
                } else {
                    _uiState.value = QrUiState.Error("Failed: ${resp.code()}")
                    isProcessing.set(false)
                }
            } catch (e: Exception) {
                _uiState.value = QrUiState.Error(e.message ?: "Error")
                isProcessing.set(false)
            }
        }
    }

    fun resetScanner() {
        _uiState.value = QrUiState.Scanning
        isProcessing.set(false)
    }
}

sealed class QrUiState {
    data object Scanning        : QrUiState()
    data object Validating      : QrUiState()
    data object FetchingLocation : QrUiState()
    data class  ShowingNodes(val nodes: List<com.example.humsafar.models.MonumentNode>) : QrUiState()
    data class  Success(val data: NodeScanResponse) : QrUiState()
    data class  Error(val message: String) : QrUiState()
}