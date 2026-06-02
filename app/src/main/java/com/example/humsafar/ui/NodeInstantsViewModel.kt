package com.example.humsafar.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.data.InstantStorageUploader
import com.example.humsafar.data.UserRepository
import com.example.humsafar.models.NodeInstantCreateRequest
import com.example.humsafar.models.NodeInstantResponse
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InstantsUiState(
    val isLoading: Boolean = true,
    val instants: List<NodeInstantResponse> = emptyList(),
    val error: String? = null,
    val posting: Boolean = false,
    val postError: String? = null
)

class NodeInstantsViewModel : ViewModel() {

    private val _state = MutableStateFlow(InstantsUiState())
    val state: StateFlow<InstantsUiState> = _state.asStateFlow()

    private var nodeId = 0
    private var siteId = 0

    fun init(nodeId: Int, siteId: Int) {
        if (this.nodeId == nodeId && this.siteId == siteId && _state.value.instants.isNotEmpty()) return
        this.nodeId = nodeId
        this.siteId = siteId
        load()
    }

    fun load() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val uid = AuthManager.currentUser.value?.uid
                val resp = HumsafarClient.api.getNodeInstants(nodeId, limit = 50, firebaseUid = uid)
                if (resp.isSuccessful && resp.body() != null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        instants = resp.body()!!,
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Couldn't load instants (${resp.code()})"
                    )
                }
            }.onFailure {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = it.message ?: "Network error"
                )
            }
        }
    }

    fun toggleLike(instantId: Int) {
        val uid = AuthManager.currentUser.value?.uid ?: return
        viewModelScope.launch {
            runCatching {
                val resp = HumsafarClient.api.toggleInstantLike(instantId, uid)
                if (resp.isSuccessful && resp.body() != null) {
                    val r = resp.body()!!
                    _state.value = _state.value.copy(
                        instants = _state.value.instants.map { inst ->
                            if (inst.id == instantId) {
                                inst.copy(likedByMe = r.liked, likeCount = r.likeCount)
                            } else inst
                        }
                    )
                }
            }
        }
    }

    fun postInstant(context: Context, uri: Uri, onDone: (Boolean) -> Unit) {
        val uid = AuthManager.currentUser.value?.uid
        if (uid.isNullOrBlank()) {
            onDone(false)
            return
        }
        _state.value = _state.value.copy(posting = true, postError = null)
        viewModelScope.launch {
            runCatching {
                // Ensure Firebase profile (name) is synced before we snapshot photographer.
                AuthManager.currentUser.value?.let { UserRepository.syncFirebaseUserSuspend(it) }

                val mediaUrl = InstantStorageUploader.uploadPhoto(
                    context.applicationContext,
                    siteId,
                    nodeId,
                    uri
                )
                val resp = HumsafarClient.api.postNodeInstant(
                    NodeInstantCreateRequest(
                        firebaseUid = uid,
                        siteId = siteId,
                        nodeId = nodeId,
                        mediaUrl = mediaUrl,
                        mediaType = "image"
                    )
                )
                if (resp.isSuccessful && resp.body() != null) {
                    load()
                    _state.value = _state.value.copy(posting = false)
                    onDone(true)
                } else {
                    _state.value = _state.value.copy(
                        posting = false,
                        postError = "Upload failed (${resp.code()})"
                    )
                    onDone(false)
                }
            }.onFailure {
                _state.value = _state.value.copy(
                    posting = false,
                    postError = it.message ?: "Upload failed"
                )
                onDone(false)
            }
        }
    }
}
