package com.example.humsafar.ui

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.humsafar.models.MonumentNode
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScanScreen(
    monumentId: Long,
    currentLat: Double,
    currentLng: Double,
    onNodeReady: (Long, String, Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: QrScanViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val camPerm = rememberPermissionState(Manifest.permission.CAMERA)
    val lifecycleOwner = LocalLifecycleOwner.current
    var flashEnabled by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(Unit) { camPerm.launchPermissionRequest() }

    LaunchedEffect(uiState) {
        if (uiState is QrUiState.Success) {
            val data = (uiState as QrUiState.Success).data
            onNodeReady(data.node.id, data.node.name, data.node.nodeType == "KING")
        }
    }

    // Root: full screen Box so camera and UI layers stack properly
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        // ── Layer 1: Camera preview (full screen) ─────────────────────────
        val showCamera = camPerm.status.isGranted &&
                uiState !is QrUiState.ShowingNodes &&
                uiState !is QrUiState.FetchingLocation

        if (showCamera) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        // IMPORTANT: Keep scaleType as FILL_CENTER so camera fills screen symmetrically
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val scanner = BarcodeScanning.getClient()
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { ia ->
                                ia.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                    val img = proxy.image
                                    if (img != null && uiState is QrUiState.Scanning) {
                                        scanner.process(
                                            InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)
                                        ).addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                                ?.rawValue?.let { viewModel.onQrDetected(it) }
                                        }.addOnCompleteListener { proxy.close() }
                                    } else {
                                        proxy.close()
                                    }
                                }
                            }
                        val cam = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                        cameraControl = cam.cameraControl
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (uiState is QrUiState.ShowingNodes || uiState is QrUiState.FetchingLocation) {
            AnimatedOrbBackground(Modifier.fillMaxSize())
        }

        // ── Layer 2: All UI on top (fills screen, Column from top to bottom) ─
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Back button — pinned left
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xBB000000))
                        .border(0.7.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title — absolutely centered in the Bar Box
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Scan QR Code",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Point at any node marker",
                        color = Color(0x99FFFFFF),
                        fontSize = 12.sp
                    )
                }
            }

            // ── State-based content ───────────────────────────────────────
            when (uiState) {
                is QrUiState.Scanning, is QrUiState.Validating ->
                    ScanningViewContent(
                        isValidating = uiState is QrUiState.Validating,
                        flashEnabled = flashEnabled,
                        onFlashToggle = {
                            flashEnabled = !flashEnabled
                            cameraControl?.enableTorch(flashEnabled)
                        },
                        onTrouble = { viewModel.fetchNearbyNodes(currentLat, currentLng, monumentId) }
                    )

                is QrUiState.FetchingLocation -> FetchingLocationContent()

                is QrUiState.ShowingNodes ->
                    ShowingNodesContent(
                        nodes = (uiState as QrUiState.ShowingNodes).nodes,
                        currentLat = currentLat,
                        currentLng = currentLng,
                        onSelect = { viewModel.selectNodeManually(it) }
                    )

                is QrUiState.Error ->
                    ErrorContent(
                        message = (uiState as QrUiState.Error).message,
                        onRetry = { viewModel.resetScanner() }
                    )

                is QrUiState.Success -> SuccessContent()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCANNING VIEW — fully centered frame using BoxWithConstraints
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.ScanningViewContent(
    isValidating: Boolean,
    flashEnabled: Boolean,
    onFlashToggle: () -> Unit,
    onTrouble: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "sl"
    )
    val cornerGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "cg"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ps"
    )

    val frameSize    = 270.dp
    val cornerLength = 38.dp
    var strokeWidth  = 3.5.dp
    val cornerRadius = 18.dp

    val frameColor = if (isValidating) Color(0xFF4ADE80) else Color(0xFF38BDF8)

    // ── This Box takes all remaining vertical space and centers the frame ─
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),   // takes all space between top bar and bottom controls
        contentAlignment = Alignment.Center
    ) {
        // Full-area dark overlay with transparent cutout — drawn on a Canvas
        // that fills the entire Box so coordinates are accurate
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fpx = frameSize.toPx()
            val cx  = size.width  / 2f
            val cy  = size.height / 2f
            val l   = cx - fpx / 2f
            val t   = cy - fpx / 2f
            val cr  = cornerRadius.toPx()

            // 1. Dark overlay everywhere
            drawRect(Color(0xAA000000))

            // 2. Punch out the frame window using BlendMode.Clear
            drawRoundRect(
                color       = Color.Transparent,
                topLeft     = Offset(l, t),
                size        = Size(fpx, fpx),
                cornerRadius = CornerRadius(cr),
                blendMode   = BlendMode.Clear
            )
        }

        // The actual frame box — size(frameSize) is CENTERED inside the Box above
        Box(
            modifier = Modifier
                .size(frameSize)
                .scale(if (isValidating) pulse else 1f),
            contentAlignment = Alignment.Center
        ) {
            // Scan line sweep (only while scanning)
            if (!isValidating) {
                val offsetY = ((scanLineY - 0.5f) * frameSize.value).dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = offsetY)
                        .height(2.dp)
                        .padding(horizontal = 10.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF38BDF8).copy(alpha = 0.85f),
                                    Color(0xFF7DD3FC),
                                    Color(0xFF38BDF8).copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Green fill + checkmark when validating
            if (isValidating) {
                Box(
                    modifier = Modifier
                        .size(frameSize)
                        .clip(RoundedCornerShape(cornerRadius))
                        .background(Color(0x334ADE80))
                )
                Text("✓", color = Color(0xFF4ADE80), fontSize = 52.sp, fontWeight = FontWeight.Bold)
            }

            // Corner bracket lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w  = size.width
                val h  = size.height
                val cl = cornerLength.toPx()
                val sw = strokeWidth.toPx()
                val r  = cornerRadius.toPx()

                val paint = Paint().apply {
                    color       = frameColor.copy(alpha = cornerGlow)
                    style       = PaintingStyle.Stroke
                    strokeCap   = StrokeCap.Round
                    isAntiAlias = true
                }

                drawContext.canvas.apply {
                    // Top-left
                    drawLine(Offset(r, 0f),  Offset(cl, 0f), paint)
                    drawLine(Offset(0f, r),  Offset(0f, cl), paint)
                    // Top-right
                    drawLine(Offset(w - cl, 0f), Offset(w - r, 0f), paint)
                    drawLine(Offset(w, r),        Offset(w, cl),      paint)
                    // Bottom-left
                    drawLine(Offset(0f, h - cl), Offset(0f, h - r), paint)
                    drawLine(Offset(r, h),        Offset(cl, h),      paint)
                    // Bottom-right
                    drawLine(Offset(w, h - cl),   Offset(w, h - r),   paint)
                    drawLine(Offset(w - cl, h),   Offset(w - r, h),   paint)
                }
            }
        }

        // Status pill — anchored at the bottom of the centered Box
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xCC000000))
                .border(0.7.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            if (isValidating) {
                val spin by rememberInfiniteTransition(label = "spin").animateFloat(
                    0f, 360f,
                    infiniteRepeatable(tween(800, easing = LinearEasing)),
                    label = "sp"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "◌",
                        color = Color(0xFF4ADE80),
                        fontSize = 16.sp,
                        modifier = Modifier.rotate(spin)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Verifying node…", color = Color.White, fontSize = 14.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Align QR code within the frame",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // ── Bottom controls bar — always at the very bottom ───────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000)))
            )
            .navigationBarsPadding()
            .padding(vertical = 20.dp, horizontal = 28.dp)
    ) {
        // Flash toggle — left
        Column(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (flashEnabled) Color(0x33FFD54F) else Color(0x33FFFFFF)
                    )
                    .border(
                        1.dp,
                        if (flashEnabled) Color(0x88FFD54F) else Color(0x44FFFFFF),
                        CircleShape
                    )
                    .clickable { onFlashToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (flashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    contentDescription = "Flash",
                    tint = if (flashEnabled) Color(0xFFFFD54F) else Color(0xCCFFFFFF),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (flashEnabled) "Flash On" else "Flash",
                color = Color(0x88FFFFFF),
                fontSize = 11.sp
            )
        }

        // "Can't Scan?" — centered
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x44FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .clickable { onTrouble() }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 22.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Can't Scan?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Browse nearby nodes",
                        color = Color(0x88FFFFFF),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Right side spacer to balance flash button
        Spacer(modifier = Modifier.align(Alignment.CenterEnd).size(54.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fetching location
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.FetchingLocationContent() {
    val pulse by rememberInfiniteTransition(label = "loc").animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "lp"
    )

    Spacer(Modifier.weight(1f))
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0x3338BDF8), Color(0x110EA5E9))))
            .border(1.dp, Color(0x5538BDF8), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("📍", fontSize = 40.sp)
    }
    Spacer(Modifier.height(24.dp))
    Text("Finding nearby nodes", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Using your GPS location", color = TextTertiary, fontSize = 14.sp)
    Spacer(Modifier.weight(1f))
}

// ─────────────────────────────────────────────────────────────────────────────
// Showing nodes list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.ShowingNodesContent(
    nodes: List<MonumentNode>,
    currentLat: Double,
    currentLng: Double,
    onSelect: (Long) -> Unit
) {
    Spacer(Modifier.height(8.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text("Nearby Nodes", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Select the node you're standing at", color = TextTertiary, fontSize = 13.sp)
    }

    Spacer(Modifier.height(16.dp))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(nodes) { node ->
            NodeSelectCard(node = node, currentLat = currentLat, currentLng = currentLng,
                onClick = { onSelect(node.id) })
        }
    }

    Spacer(Modifier.height(16.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
// Error state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.ErrorContent(message: String, onRetry: () -> Unit) {
    Spacer(Modifier.weight(1f))
    Text("⚠️", fontSize = 52.sp)
    Spacer(Modifier.height(16.dp))
    Text("Something went wrong", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        message,
        color = Color(0xFFFF6B6B),
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    )
    Spacer(Modifier.height(28.dp))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9))))
            .clickable { onRetry() }
            .padding(horizontal = 32.dp, vertical = 14.dp)
    ) {
        Text("Try Again", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.weight(1f))
}

// ─────────────────────────────────────────────────────────────────────────────
// Success state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.SuccessContent() {
    Spacer(Modifier.weight(1f))
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0x334ADE80), Color(0x1122C55E))))
            .border(1.5.dp, Color(0xFF4ADE80), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", color = Color(0xFF4ADE80), fontSize = 44.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(20.dp))
    Text("Node Verified!", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Loading content…", color = TextTertiary, fontSize = 14.sp)
    Spacer(Modifier.weight(1f))
}

// ─────────────────────────────────────────────────────────────────────────────
// Node selection card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodeSelectCard(
    node: MonumentNode,
    currentLat: Double,
    currentLng: Double,
    onClick: () -> Unit
) {
    val dist     = com.example.humsafar.utils.haversineDistance(currentLat, currentLng, node.latitude, node.longitude)
    val distText = if (dist >= 1000) "${"%.1f".format(dist / 1000)} km" else "${dist.toInt()} m"
    val isKing   = node.nodeType == "KING"

    val borderBrush = if (isKing)
        Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
    else
        Brush.linearGradient(listOf(Color(0x3338BDF8), Color(0x1A0EA5E9)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isKing) Color(0x1AFFD54F) else Color(0x1438BDF8))
            .border(1.dp, borderBrush, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isKing) Color(0x33FFD54F) else Color(0x2238BDF8)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isKing) "👑" else "📍", fontSize = 20.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        node.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isKing) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isKing) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x33FFD54F))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ENTRY", color = AccentYellow, fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                    if (node.recommended && !isKing) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x334ADE80))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("NEXT", color = Color(0xFF4ADE80), fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text("📏 $distText away", color = TextTertiary, fontSize = 12.sp)
            }

            // Chevron
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text("›", color = TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}