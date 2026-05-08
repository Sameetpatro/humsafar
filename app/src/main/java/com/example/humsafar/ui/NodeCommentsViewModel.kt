// app/src/main/java/com/example/humsafar/ui/NodeCommentsViewModel.kt
//
// State + actions for NodeCommentsScreen.
// Threading: flat 2-level (root + replies). Replies for a root post are
// fetched lazily (only when the user expands them) to keep the initial
// payload small.

package com.example.humsafar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.humsafar.data.TripManager
import com.example.humsafar.models.NodeCommentCreateRequest
import com.example.humsafar.models.NodeCommentResponse
import com.example.humsafar.network.HumsafarClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommentThread(
    val root:           NodeCommentResponse,
    val replies:        List<NodeCommentResponse> = emptyList(),
    val repliesLoaded:  Boolean = false,
    val repliesLoading: Boolean = false,
    val isExpanded:     Boolean = false
)

data class NodeCommentsUiState(
    val isLoading:    Boolean = true,
    val error:        String? = null,
    val threads:      List<CommentThread> = emptyList(),
    val isPosting:    Boolean = false,
    val replyTo:      NodeCommentResponse? = null   // null = new top-level comment
)

class NodeCommentsViewModel : ViewModel() {

    private val _state = MutableStateFlow(NodeCommentsUiState())
    val state: StateFlow<NodeCommentsUiState> = _state.asStateFlow()

    private var nodeId: Int = 0
    private var siteId: Int = 0

    fun init(nodeId: Int, siteId: Int) {
        this.nodeId = nodeId
        this.siteId = siteId
        loadComments()
    }

    fun refresh() = loadComments()

    fun loadComments() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        try {
            val resp = HumsafarClient.api.getNodeComments(
                nodeId      = nodeId,
                page        = 1,
                pageSize    = 50,
                firebaseUid = TripManager.USER_ID.takeIf { it.isNotBlank() }
            )
            if (resp.isSuccessful) {
                val roots = resp.body() ?: emptyList()
                _state.update {
                    it.copy(
                        isLoading = false,
                        threads   = roots.map { r -> CommentThread(root = r) }
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error     = "Failed to load comments (${resp.code()})"
                    )
                }
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    error     = e.message ?: "Network error"
                )
            }
        }
    }

    fun toggleReplies(rootId: Int) {
        val current = _state.value.threads.firstOrNull { it.root.id == rootId } ?: return
        // Already loaded → just flip expanded state
        if (current.repliesLoaded) {
            _state.update { s ->
                s.copy(threads = s.threads.map {
                    if (it.root.id == rootId) it.copy(isExpanded = !it.isExpanded) else it
                })
            }
            return
        }
        // First-time load
        viewModelScope.launch {
            _state.update { s ->
                s.copy(threads = s.threads.map {
                    if (it.root.id == rootId) it.copy(repliesLoading = true, isExpanded = true) else it
                })
            }
            try {
                val resp = HumsafarClient.api.getCommentReplies(
                    commentId   = rootId,
                    firebaseUid = TripManager.USER_ID.takeIf { it.isNotBlank() }
                )
                val replies = if (resp.isSuccessful) resp.body() ?: emptyList() else emptyList()
                _state.update { s ->
                    s.copy(threads = s.threads.map {
                        if (it.root.id == rootId)
                            it.copy(
                                replies        = replies,
                                repliesLoaded  = true,
                                repliesLoading = false,
                                isExpanded     = true
                            )
                        else it
                    })
                }
            } catch (_: Exception) {
                _state.update { s ->
                    s.copy(threads = s.threads.map {
                        if (it.root.id == rootId)
                            it.copy(repliesLoading = false, isExpanded = false)
                        else it
                    })
                }
            }
        }
    }

    fun startReply(parent: NodeCommentResponse) {
        // If user clicked reply on a reply, the backend will normalize to the
        // root parent — but we still capture the user's intent visually so the
        // composer shows "Replying to <name>".
        _state.update { it.copy(replyTo = parent) }
    }

    fun cancelReply() {
        _state.update { it.copy(replyTo = null) }
    }

    fun postComment(content: String, onError: (String) -> Unit) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        val firebaseUid = TripManager.USER_ID
        if (firebaseUid.isBlank()) {
            onError("Please sign in to post a comment")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isPosting = true) }
            val replyTo = _state.value.replyTo
            try {
                val resp = HumsafarClient.api.postNodeComment(
                    NodeCommentCreateRequest(
                        firebaseUid     = firebaseUid,
                        siteId          = siteId,
                        nodeId          = nodeId,
                        content         = trimmed,
                        parentCommentId = replyTo?.id
                    )
                )
                if (!resp.isSuccessful || resp.body() == null) {
                    val msg = resp.errorBody()?.string() ?: "Failed to post (${resp.code()})"
                    _state.update { it.copy(isPosting = false) }
                    onError(msg)
                    return@launch
                }
                val saved = resp.body()!!
                _state.update { s ->
                    if (saved.parentCommentId == null) {
                        // New root comment goes to the top
                        s.copy(
                            isPosting = false,
                            replyTo   = null,
                            threads   = listOf(CommentThread(root = saved)) + s.threads
                        )
                    } else {
                        // Reply: append to the matching root and bump replyCount.
                        val rootId = saved.parentCommentId
                        s.copy(
                            isPosting = false,
                            replyTo   = null,
                            threads   = s.threads.map { t ->
                                if (t.root.id == rootId) {
                                    t.copy(
                                        root          = t.root.copy(replyCount = t.root.replyCount + 1),
                                        replies       = t.replies + saved,
                                        repliesLoaded = true,
                                        isExpanded    = true
                                    )
                                } else t
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isPosting = false) }
                onError(e.message ?: "Network error")
            }
        }
    }

    fun deleteComment(comment: NodeCommentResponse, onError: (String) -> Unit) {
        val firebaseUid = TripManager.USER_ID
        if (firebaseUid.isBlank()) {
            onError("Please sign in")
            return
        }
        viewModelScope.launch {
            try {
                val resp = HumsafarClient.api.deleteNodeComment(comment.id, firebaseUid)
                if (!resp.isSuccessful) {
                    onError(resp.errorBody()?.string() ?: "Delete failed (${resp.code()})")
                    return@launch
                }
                _state.update { s ->
                    if (comment.parentCommentId == null) {
                        s.copy(threads = s.threads.filterNot { it.root.id == comment.id })
                    } else {
                        s.copy(threads = s.threads.map { t ->
                            if (t.root.id == comment.parentCommentId) {
                                t.copy(
                                    root    = t.root.copy(
                                        replyCount = (t.root.replyCount - 1).coerceAtLeast(0)
                                    ),
                                    replies = t.replies.filterNot { it.id == comment.id }
                                )
                            } else t
                        })
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Network error")
            }
        }
    }
}
