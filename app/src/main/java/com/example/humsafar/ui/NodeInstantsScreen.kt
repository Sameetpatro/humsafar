package com.example.humsafar.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.humsafar.models.NodeInstantResponse
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassPrimaryButton
import com.example.humsafar.ui.theme.LocalAccent
import com.example.humsafar.ui.theme.LocalAppColors
import java.io.File
import kotlin.math.absoluteValue

/** ~3 inch squircle on standard-density screens (288dp ≈ 3"). */
private val InstantCardSize = 288.dp
private val SquircleRadius = 52.dp

/**
 * Horizontal swipe gallery of top-loved instants at a node.
 * Pick photo → auto-upload to Firebase → auto-save URL to database.
 */
@Composable
fun NodeInstantsScreen(
    nodeId: Int,
    siteId: Int,
    nodeName: String,
    onBack: () -> Unit,
    viewModel: NodeInstantsViewModel = viewModel()
) {
    val context = LocalContext.current
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(nodeId, siteId) { viewModel.init(nodeId, siteId) }

    var showWarning by remember { mutableStateOf(false) }
    var autoShareTriggered by remember { mutableStateOf(false) }

    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = pendingCaptureUri
        pendingCaptureUri = null
        if (ok && uri != null) {
            viewModel.postInstant(context.applicationContext, uri) { posted ->
                Toast.makeText(
                    context,
                    if (posted) "Instant shared!" else (state.postError ?: "Upload failed"),
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(context, "Camera cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchShareFlow() { showWarning = true }

    fun openPickerAfterWarning() {
        showWarning = false
        val uri = createInstantCaptureUri(context)
        if (uri == null) {
            Toast.makeText(context, "Couldn't open camera", Toast.LENGTH_LONG).show()
            return
        }
        pendingCaptureUri = uri
        cameraLauncher.launch(uri)
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = tokens.textPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Instants", color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(nodeName, color = tokens.textSecondary, fontSize = 12.sp)
                }
            }

            when {
                state.isLoading -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent.primary)
                    }
                }
                state.error != null -> {
                    Column(
                        Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📸", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(state.error!!, color = tokens.textSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        GlassPrimaryButton(text = "Retry", onClick = { viewModel.load() })
                    }
                }
                state.instants.isEmpty() -> {
                    Column(
                        Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptySquirclePlaceholder(accent.primary)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "No instants yet",
                            color = tokens.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Be the first to share a moment at this spot.",
                            color = tokens.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    val instants = state.instants
                    val pagerState = rememberPagerState(pageCount = { instants.size })

                    LaunchedEffect(pagerState.currentPage, instants.size) {
                        if (!autoShareTriggered &&
                            instants.isNotEmpty() &&
                            pagerState.currentPage == instants.lastIndex
                        ) {
                            autoShareTriggered = true
                            kotlinx.coroutines.delay(900)
                            launchShareFlow()
                        }
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 36.dp),
                            pageSpacing = 20.dp
                        ) { page ->
                            val pageOffset = (
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).absoluteValue.coerceIn(0f, 1f)
                            val scale = lerp(0.88f, 1f, 1f - pageOffset)
                            val alpha = lerp(0.55f, 1f, 1f - pageOffset)

                            InstantSquircleCard(
                                instant = instants[page],
                                rank = page + 1,
                                scale = scale,
                                alpha = alpha,
                                onLike = { viewModel.toggleLike(instants[page].id) }
                            )
                        }
                    }

                    // Page dots
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(instants.size.coerceAtMost(50)) { i ->
                            val selected = pagerState.currentPage == i
                            Box(
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (selected) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) accent.primary
                                        else tokens.textTertiary.copy(0.35f)
                                    )
                            )
                        }
                    }

                    Text(
                        "${pagerState.currentPage + 1} of ${instants.size} · swipe to explore",
                        color = tokens.textTertiary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
            }

            // Share + posting overlay
            Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
                if (state.posting) {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accent.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Uploading your instant…", color = tokens.textSecondary, fontSize = 13.sp)
                    }
                }
                Box(Modifier.fillMaxWidth()) {
                    PopOutShareInstantButton(
                        enabled = !state.posting,
                        onClick = { launchShareFlow() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        if (showWarning) {
            PostWarningDialog(
                onConfirm = { openPickerAfterWarning() },
                onDismiss = { showWarning = false }
            )
        }
    }
}

@Composable
private fun InstantSquircleCard(
    instant: NodeInstantResponse,
    rank: Int,
    scale: Float,
    alpha: Float,
    onLike: () -> Unit
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val cardSize = minOf(InstantCardSize, screenW - 72.dp)

    val likeScale by animateFloatAsState(
        targetValue = if (instant.likedByMe) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium),
        label = "likePop"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    ) {
        Box(
            Modifier
                .size(cardSize)
                .shadow(24.dp, RoundedCornerShape(SquircleRadius), ambientColor = accent.primary.copy(0.3f))
                .clip(RoundedCornerShape(SquircleRadius))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(accent.primary, accent.dark)),
                    shape = RoundedCornerShape(SquircleRadius)
                )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(instant.mediaUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Photographer name overlay on the image
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xE6000000))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                PhotographerBadge(
                    name = instant.displayName,
                    avatarUrl = instant.avatarUrl,
                    accent = accent,
                    onImage = true
                )
            }

            if (rank <= 3) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF8C00))))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("#$rank", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        PhotographerBadge(
            name = instant.displayName,
            avatarUrl = instant.avatarUrl,
            accent = accent,
            onImage = false
        )

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(tokens.surfaceMuted)
                .clickable { onLike() }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Icon(
                if (instant.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                null,
                tint = if (instant.likedByMe) Color(0xFFFF4081) else tokens.textSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { scaleX = likeScale; scaleY = likeScale }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "${instant.likeCount} likes",
                color = tokens.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PhotographerBadge(
    name: String?,
    avatarUrl: String?,
    accent: com.example.humsafar.ui.theme.Accent,
    onImage: Boolean
) {
    val tokens = LocalAppColors.current
    val label = name?.takeIf { it.isNotBlank() } ?: "Traveller"
    val textColor = if (onImage) Color.White else tokens.textPrimary
    val subColor = if (onImage) Color.White.copy(0.75f) else tokens.textSecondary

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(if (onImage) 28.dp else 32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Color.White.copy(if (onImage) 0.6f else 0.2f), CircleShape)
            )
        } else {
            Box(
                Modifier
                    .size(if (onImage) 28.dp else 32.dp)
                    .clip(CircleShape)
                    .background(if (onImage) accent.primary.copy(0.85f) else accent.tint),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label.firstOrNull()?.uppercase() ?: "?",
                    color = if (onImage) Color.White else accent.dark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "Photo by",
                color = subColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                label,
                color = textColor,
                fontSize = if (onImage) 13.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptySquirclePlaceholder(accentColor: Color) {
    Box(
        Modifier
            .size(InstantCardSize)
            .clip(RoundedCornerShape(SquircleRadius))
            .background(
                Brush.linearGradient(
                    listOf(accentColor.copy(0.15f), accentColor.copy(0.05f))
                )
            )
            .border(2.dp, accentColor.copy(0.25f), RoundedCornerShape(SquircleRadius)),
        contentAlignment = Alignment.Center
    ) {
        Text("📸", fontSize = 56.sp)
    }
}

@Composable
private fun ShareInstantButton(enabled: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(accent.primary, accent.dark)))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Share Your Instant", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PopOutShareInstantButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    val inf = rememberInfiniteTransition(label = "instantPop")
    val pulse by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(modifier = modifier) {
        // soft glow behind
        Box(
            Modifier
                .offset(x = 22.dp, y = (-2).dp)
                .size(78.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(accent.primary.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )

        // main "pop-out" bubble, slightly off-screen to the right
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(x = 22.dp, y = (-2).dp)
        ) {
            Box(
                Modifier
                    .size(66.dp)
                    .shadow(18.dp, CircleShape, ambientColor = accent.primary.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent.primary, accent.dark)))
                    .border(2.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .clickable(enabled = enabled) { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Instant",
                color = tokens.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PostWarningDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val tokens = LocalAppColors.current
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(tokens.surface)
                .padding(24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB300), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Before you post", color = tokens.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Your photo will be visible to all visitors at this heritage spot. " +
                        "Only share content you have permission to post. " +
                        "Inappropriate images may be removed.",
                    color = tokens.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(Modifier.height(20.dp))
                GlassPrimaryButton(text = "I understand — open camera", onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel", color = tokens.textTertiary)
                }
            }
        }
    }
}

private fun createInstantCaptureUri(context: Context): Uri? {
    return runCatching {
        val dir = File(context.cacheDir, "instants").apply { mkdirs() }
        val file = File.createTempFile("instant_", ".jpg", dir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }.getOrNull()
}
