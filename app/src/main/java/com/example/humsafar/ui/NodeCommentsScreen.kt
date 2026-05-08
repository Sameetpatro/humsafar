// app/src/main/java/com/example/humsafar/ui/NodeCommentsScreen.kt
//
// Threaded comments per node. Backend: /community/comments/* (FastAPI).
//
// UX:
//   • Root comments listed newest-first.
//   • Tap "View N replies" to lazy-load + expand a thread (kept flat — replies-of-replies
//     are normalised to the root parent server-side, so the UI never gets deeper than 2).
//   • Composer at the bottom, sticky above the keyboard via imePadding().
//   • "Replying to <name>" chip appears when a reply target is set; tap × to cancel.
//   • Author of a comment sees a Delete affordance; everyone sees a Reply affordance.

package com.example.humsafar.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.humsafar.models.NodeCommentResponse
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.theme.*

@Composable
fun NodeCommentsScreen(
    nodeId:    Int,
    siteId:    Int,
    nodeName:  String,
    onBack:    () -> Unit,
    viewModel: NodeCommentsViewModel = viewModel()
) {
    val context = LocalContext.current
    val state   by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(nodeId, siteId) { viewModel.init(nodeId, siteId) }

    var draft by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize().imePadding()) {
            CommentsHeader(
                nodeName    = nodeName,
                rootCount   = state.threads.size,
                onBack      = onBack,
                isLoading   = state.isLoading
            )

            Box(Modifier.weight(1f)) {
                when {
                    state.isLoading -> LoadingPanel()
                    state.error != null && state.threads.isEmpty() ->
                        ErrorPanel(state.error!!) { viewModel.refresh() }
                    state.threads.isEmpty() -> EmptyCommentsPanel()
                    else -> CommentList(
                        state          = state,
                        onToggleReplies = viewModel::toggleReplies,
                        onReply         = viewModel::startReply,
                        onDelete        = { c ->
                            viewModel.deleteComment(c) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Composer(
                draft       = draft,
                onDraft     = { draft = it.take(2000) },
                replyTo     = state.replyTo,
                isPosting   = state.isPosting,
                onCancelReply = { viewModel.cancelReply() },
                onSend = {
                    if (draft.isBlank()) return@Composer
                    val text = draft
                    viewModel.postComment(text) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    draft = ""
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CommentsHeader(
    nodeName:  String,
    rootCount: Int,
    onBack:    () -> Unit,
    isLoading: Boolean
) {
    val tokens = LocalAppColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(tokens.surface.copy(alpha = 0.95f), tokens.surface.copy(alpha = 0.7f))
                )
            )
            .drawBehind {
                drawLine(
                    tokens.border,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    0.5.dp.toPx()
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 12.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp).clip(CircleShape)
                    .background(GlassWhite15)
                    .border(0.5.dp, GlassBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint     = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Comments",
                    color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    if (isLoading) "Loading…"
                    else if (rootCount == 0) "Be the first to comment on $nodeName"
                    else "$rootCount on $nodeName",
                    color    = TextTertiary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Comment list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CommentList(
    state:           NodeCommentsUiState,
    onToggleReplies: (Int) -> Unit,
    onReply:         (NodeCommentResponse) -> Unit,
    onDelete:        (NodeCommentResponse) -> Unit
) {
    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(state.threads, key = { it.root.id }) { thread ->
            CommentThreadCard(
                thread          = thread,
                onToggleReplies = { onToggleReplies(thread.root.id) },
                onReply         = onReply,
                onDelete        = onDelete
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun CommentThreadCard(
    thread:          CommentThread,
    onToggleReplies: () -> Unit,
    onReply:         (NodeCommentResponse) -> Unit,
    onDelete:        (NodeCommentResponse) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, tint = GlassWhite10) {
        Column(Modifier.padding(14.dp)) {
            CommentRow(
                comment = thread.root,
                onReply = onReply,
                onDelete = onDelete
            )

            if (thread.root.replyCount > 0 || thread.replies.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                RepliesToggleRow(
                    expanded = thread.isExpanded,
                    loading  = thread.repliesLoading,
                    count    = if (thread.repliesLoaded) thread.replies.size else thread.root.replyCount,
                    onClick  = onToggleReplies
                )

                AnimatedVisibility(
                    visible = thread.isExpanded,
                    enter   = expandVertically(tween(220)) + fadeIn(tween(220)),
                    exit    = shrinkVertically(tween(180)) + fadeOut(tween(180))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, start = 12.dp)
                    ) {
                        thread.replies.forEach { reply ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                                    .drawBehind {
                                        drawLine(
                                            color = GlassBorder,
                                            start = Offset(0f, 0f),
                                            end   = Offset(0f, size.height),
                                            strokeWidth = 1.5.dp.toPx()
                                        )
                                    }
                                    .padding(start = 14.dp)
                            ) {
                                CommentRow(
                                    comment  = reply,
                                    onReply  = onReply,
                                    onDelete = onDelete
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// One comment row (used for both root and reply)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CommentRow(
    comment:  NodeCommentResponse,
    onReply:  (NodeCommentResponse) -> Unit,
    onDelete: (NodeCommentResponse) -> Unit
) {
    Row(verticalAlignment = Alignment.Top) {
        Avatar(name = comment.displayName, url = comment.avatarUrl)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.displayName?.takeIf { it.isNotBlank() } ?: "Heritage Explorer",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    relativeTime(comment.createdAt),
                    color    = TextTertiary,
                    fontSize = 11.sp
                )
                if (comment.isOwn) {
                    val ownAccent = LocalAccent.current
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ownAccent.tint)
                            .border(0.5.dp, ownAccent.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("you", color = ownAccent.dark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                comment.content,
                color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionPill(
                    icon = { Icon(Icons.AutoMirrored.Filled.Reply, null, tint = Color(0xFF8AC7FF), modifier = Modifier.size(14.dp)) },
                    label = "Reply",
                    color = Color(0xFF8AC7FF),
                    onClick = { onReply(comment) }
                )
                if (comment.isOwn) {
                    Spacer(Modifier.width(8.dp))
                    ActionPill(
                        icon = { Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFFF8A8A), modifier = Modifier.size(14.dp)) },
                        label = "Delete",
                        color = Color(0xFFFF8A8A),
                        onClick = { onDelete(comment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon:    @Composable () -> Unit,
    label:   String,
    color:   Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.10f))
            .border(0.5.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RepliesToggleRow(
    expanded: Boolean,
    loading:  Boolean,
    count:    Int,
    onClick:  () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = !loading) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color(0xFF8AC7FF),
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(13.dp)
            )
        } else {
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint     = Color(0xFF8AC7FF),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            if (expanded) "Hide replies"
            else if (count == 1) "View 1 reply"
            else "View $count replies",
            color      = Color(0xFF8AC7FF),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Avatar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Avatar(name: String?, url: String?) {
    val initial = (name?.trim()?.firstOrNull()?.uppercaseChar() ?: '?')
    val seed    = (name ?: "?").hashCode()
    val accent  = LocalAccent.current
    val palette = listOf(
        accent.primary, Color(0xFF8AC7FF), Color(0xFF80CBC4),
        Color(0xFFFFAB91), Color(0xFFB39DDB), Color(0xFFAED581)
    )
    val color = palette[((seed % palette.size) + palette.size) % palette.size]

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.20f))
            .border(0.7.dp, color.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model              = url,
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Text(
                initial.toString(),
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Composer
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Composer(
    draft:         String,
    onDraft:       (String) -> Unit,
    replyTo:       NodeCommentResponse?,
    isPosting:     Boolean,
    onCancelReply: () -> Unit,
    onSend:        () -> Unit
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(tokens.surface.copy(alpha = 0.85f), tokens.surface)
                )
            )
            .drawBehind {
                drawLine(tokens.border, Offset(0f, 0f), Offset(size.width, 0f), 0.5.dp.toPx())
            }
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (replyTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x222196F3))
                    .border(0.5.dp, Color(0x552196F3), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply, null,
                    tint     = Color(0xFF8AC7FF),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Replying to ${replyTo.displayName?.takeIf { it.isNotBlank() } ?: "this comment"}",
                    color = Color(0xFF8AC7FF),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(22.dp).clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onCancelReply() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close, null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(GlassWhite10)
                    .border(0.7.dp, GlassBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value         = draft,
                    onValueChange = onDraft,
                    textStyle     = androidx.compose.ui.text.TextStyle(
                        color    = TextPrimary,
                        fontSize = 14.sp
                    ),
                    cursorBrush   = SolidColor(accent.primary),
                    modifier      = Modifier.fillMaxWidth().heightIn(min = 24.dp, max = 120.dp),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text(
                                if (replyTo != null) "Write a reply…" else "Share your thoughts…",
                                color    = TextTertiary,
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                )
            }
            Spacer(Modifier.width(10.dp))
            val canSend = draft.isNotBlank() && !isPosting
            Box(
                modifier = Modifier
                    .size(44.dp).clip(CircleShape)
                    .background(
                        if (canSend)
                            Brush.linearGradient(listOf(accent.primary, accent.dark))
                        else
                            Brush.linearGradient(listOf(GlassWhite15, GlassWhite10))
                    )
                    .border(
                        0.7.dp,
                        if (canSend) Color(0x33000000) else GlassBorder,
                        CircleShape
                    )
                    .clickable(enabled = canSend) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        color       = accent.onAccent,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send, null,
                        tint     = if (canSend) accent.onAccent else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State panels
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingPanel() {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = accent.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(14.dp))
            Text("Loading comments…", color = TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyCommentsPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val inf   = rememberInfiniteTransition(label = "empty_c")
            val pulse by inf.animateFloat(
                0.94f, 1.06f,
                infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
                label = "p"
            )
            Text("💬", fontSize = 56.sp, modifier = Modifier.scale(pulse))
            Spacer(Modifier.height(14.dp))
            Text(
                "No comments yet",
                color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Be the first to share what you noticed at this spot — questions, observations, or anything you'd love other travellers to know.",
                color    = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠️", fontSize = 44.sp)
            Spacer(Modifier.height(10.dp))
            Text("Couldn't load comments", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(message, color = Color(0xFFFF6B6B), fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GlassWhite15)
                    .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
                    .clickable { onRetry() }
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text("Try again", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers — relative time formatter (ISO-8601 → "5m", "2h", "Mar 12")
// ─────────────────────────────────────────────────────────────────────────────

private fun relativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        val sanitized = iso.substringBefore('+').substringBefore('Z').substringBefore('.')
        val parser    = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US
        ).apply {
            isLenient = true
            timeZone  = java.util.TimeZone.getTimeZone("UTC")
        }
        val date  = parser.parse(sanitized) ?: return ""
        val diff  = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
        val mins  = diff / 60_000
        val hours = diff / 3_600_000
        val days  = diff / 86_400_000

        when {
            mins < 1   -> "just now"
            mins < 60  -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7   -> "${days}d ago"
            else -> {
                val display = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                display.format(date)
            }
        }
    } catch (_: Exception) {
        ""
    }
}
