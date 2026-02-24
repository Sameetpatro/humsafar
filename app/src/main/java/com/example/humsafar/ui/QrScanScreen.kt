// app/src/main/java/com/example/humsafar/ui/QrScanScreen.kt
// REWRITTEN — wired to new QrScanViewModel, adds AskStartTrip popup.

package com.example.humsafar.ui

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.example.humsafar.models.QrScanResult
import com.example.humsafar.models.SiteDetail
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
    monumentId:  Long,        // kept in signature for nav compat — unused now
    currentLat:  Double,
    currentLng:  Double,
    onNodeReady: (nodeId: Int, nodeName: String, isKing: Boolean, siteId: Int) -> Unit,
    onBack:      () -> Unit,
    viewModel:   QrScanViewModel = viewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val camPerm        = rememberPermissionState(Manifest.permission.CAMERA)
    val lifecycleOwner = LocalLifecycleOwner.current
    var flashEnabled   by remember { mutableStateOf(false) }
    var cameraControl  by remember { mutableStateOf<CameraControl?>(null) }

    LaunchedEffect(Unit) { camPerm.launchPermissionRequest() }

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is QrUiState.Success) {
            val s = uiState as QrUiState.Success
            val r = s.scanResult
            if (r.isValid && r.nodeId != null && r.siteId != null) {
                onNodeReady(r.nodeId, r.nodeName ?: "", r.isKingNode, r.siteId)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        // ── Camera layer ──────────────────────────────────────────────────
        val showCamera = camPerm.status.isGranted && uiState is QrUiState.Scanning || uiState is QrUiState.Validating

        if (showCamera) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        val preview  = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val scanner  = BarcodeScanning.getClient()
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
                            preview, analysis
                        )
                        cameraControl = cam.cameraControl
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AnimatedOrbBackground(Modifier.fillMaxSize())
        }

        // ── UI layers ─────────────────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(44.dp).clip(CircleShape)
                        .background(Color(0xBB000000))
                        .border(0.7.dp, Color(0x44FFFFFF), CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column(
                    modifier            = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Scan QR Code", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Point at any node marker", color = Color(0x99FFFFFF), fontSize = 12.sp)
                }
            }

            // State content
            when (val s = uiState) {
                is QrUiState.Scanning, is QrUiState.Validating ->
                    ScanningViewContent(
                        isValidating  = s is QrUiState.Validating,
                        flashEnabled  = flashEnabled,
                        onFlashToggle = {
                            flashEnabled = !flashEnabled
                            cameraControl?.enableTorch(flashEnabled)
                        },
                        onTrouble = { /* could show manual list if you add endpoint */ }
                    )

                is QrUiState.Error ->
                    ErrorContent(message = s.message, onRetry = { viewModel.resetScanner() })

                is QrUiState.Success -> SuccessContent()

                // AskStartTrip and FetchingLocation handled as overlays below
                else -> Spacer(Modifier.weight(1f))
            }
        }

        // ── AskStartTrip popup overlay ────────────────────────────────────
        if (uiState is QrUiState.AskStartTrip) {
            val askState = uiState as QrUiState.AskStartTrip
            StartTripPopup(
                scanResult  = askState.scanResult,
                site        = askState.site,
                onStartTrip = { viewModel.confirmStartTripFromNormalNode(askState.scanResult, askState.site) },
                onDismiss   = { viewModel.dismissStartTrip(askState.scanResult, askState.site) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Start Trip Popup
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StartTripPopup(
    scanResult:  QrScanResult,
    site:        SiteDetail?,
    onStartTrip: () -> Unit,
    onDismiss:   () -> Unit
) {
    // Dark scrim
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .clickable { /* consume touches */ },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0D1F3C), Color(0xFF071428)))
                )
                .border(1.dp, GlassBorderBright, RoundedCornerShape(28.dp))
                .padding(28.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Icon
                Box(
                    modifier = Modifier
                        .size(72.dp).clip(CircleShape)
                        .background(Color(0x22FFD54F))
                        .border(1.dp, Color(0x55FFD54F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🗺️", fontSize = 32.sp)
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "Start Heritage Tour?",
                    color      = TextPrimary,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "You scanned ${scanResult.nodeName ?: "a node"}" +
                            (site?.name?.let { " at $it" } ?: "") +
                            ".\n\nWould you like to start a guided tour to track your progress and get navigation help?",
                    color     = TextSecondary,
                    fontSize  = 14.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // Start trip button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107)))
                        )
                        .clickable { onStartTrip() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🚀", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Yes, Start Tour",
                            color      = Color(0xFF050D1A),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Skip button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GlassWhite15)
                        .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Just Show Node Info",
                        color     = TextSecondary,
                        fontSize  = 14.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scanning frame (reused from original — unchanged visually)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.ScanningViewContent(
    isValidating:  Boolean,
    flashEnabled:  Boolean,
    onFlashToggle: () -> Unit,
    onTrouble:     () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLineY by infiniteTransition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "sl"
    )
    val cornerGlow by infiniteTransition.animateFloat(
        0.45f, 1f,
        infiniteRepeatable(tween(1100, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "cg"
    )
    val pulse by infiniteTransition.animateFloat(
        0.94f, 1.06f,
        infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ps"
    )

    val frameSize    = 270.dp
    val cornerLength = 38.dp
    val strokeWidth  = 3.5.dp
    val cornerRadius = 18.dp
    val frameColor   = if (isValidating) Color(0xFF4ADE80) else Color(0xFF38BDF8)

    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val fpx = frameSize.toPx()
            val cx  = size.width  / 2f
            val cy  = size.height / 2f
            drawRect(Color(0xAA000000))
            drawRoundRect(
                color        = Color.Transparent,
                topLeft      = Offset(cx - fpx / 2f, cy - fpx / 2f),
                size         = Size(fpx, fpx),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                blendMode    = BlendMode.Clear
            )
        }

        Box(modifier = Modifier.size(frameSize).scale(if (isValidating) pulse else 1f), contentAlignment = Alignment.Center) {
            if (!isValidating) {
                val offsetY = ((scanLineY - 0.5f) * frameSize.value).dp
                Box(
                    modifier = Modifier.fillMaxWidth().offset(y = offsetY).height(2.dp).padding(horizontal = 10.dp)
                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF38BDF8).copy(0.85f), Color(0xFF7DD3FC), Color(0xFF38BDF8).copy(0.85f), Color.Transparent)))
                )
            }
            if (isValidating) {
                Box(modifier = Modifier.size(frameSize).clip(RoundedCornerShape(cornerRadius)).background(Color(0x334ADE80)))
                Text("✓", color = Color(0xFF4ADE80), fontSize = 52.sp, fontWeight = FontWeight.Bold)
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w  = size.width; val h = size.height
                val cl = cornerLength.toPx()
                val r  = cornerRadius.toPx()
                val paint = Paint().apply {
                    color = frameColor.copy(alpha = cornerGlow); style = PaintingStyle.Stroke
                    strokeCap = StrokeCap.Round; isAntiAlias = true
                }
                drawContext.canvas.apply {
                    drawLine(Offset(r, 0f), Offset(cl, 0f), paint); drawLine(Offset(0f, r), Offset(0f, cl), paint)
                    drawLine(Offset(w - cl, 0f), Offset(w - r, 0f), paint); drawLine(Offset(w, r), Offset(w, cl), paint)
                    drawLine(Offset(0f, h - cl), Offset(0f, h - r), paint); drawLine(Offset(r, h), Offset(cl, h), paint)
                    drawLine(Offset(w, h - cl), Offset(w, h - r), paint); drawLine(Offset(w - cl, h), Offset(w - r, h), paint)
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                .clip(RoundedCornerShape(50)).background(Color(0xCC000000))
                .border(0.7.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            if (isValidating) {
                val spin by rememberInfiniteTransition(label = "spin").animateFloat(
                    0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "sp"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("◌", color = Color(0xFF4ADE80), fontSize = 16.sp, modifier = Modifier.rotate(spin))
                    Spacer(Modifier.width(8.dp))
                    Text("Verifying node…", color = Color.White, fontSize = 14.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                    Spacer(Modifier.width(8.dp))
                    Text("Align QR code within the frame", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    // Bottom controls
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000))))
            .navigationBarsPadding().padding(vertical = 20.dp, horizontal = 28.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(54.dp).clip(CircleShape)
                    .background(if (flashEnabled) Color(0x33FFD54F) else Color(0x33FFFFFF))
                    .border(1.dp, if (flashEnabled) Color(0x88FFD54F) else Color(0x44FFFFFF), CircleShape)
                    .clickable { onFlashToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (flashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff, null,
                    tint = if (flashEnabled) Color(0xFFFFD54F) else Color(0xCCFFFFFF), modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(if (flashEnabled) "Flash On" else "Flash", color = Color(0x88FFFFFF), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.align(Alignment.CenterEnd).size(54.dp))
    }
}

@Composable
private fun ColumnScope.ErrorContent(message: String, onRetry: () -> Unit) {
    Spacer(Modifier.weight(1f))
    Text("⚠️", fontSize = 52.sp)
    Spacer(Modifier.height(16.dp))
    Text("Something went wrong", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(message, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
    Spacer(Modifier.height(28.dp))
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9))))
            .clickable { onRetry() }.padding(horizontal = 32.dp, vertical = 14.dp)
    ) { Text("Try Again", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    Spacer(Modifier.weight(1f))
}

@Composable
private fun ColumnScope.SuccessContent() {
    Spacer(Modifier.weight(1f))
    Box(
        modifier = Modifier.size(100.dp).clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0x334ADE80), Color(0x1122C55E))))
            .border(1.5.dp, Color(0xFF4ADE80), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text("✓", color = Color(0xFF4ADE80), fontSize = 44.sp, fontWeight = FontWeight.Bold) }
    Spacer(Modifier.height(20.dp))
    Text("Node Verified!", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Loading content…", color = TextTertiary, fontSize = 14.sp)
    Spacer(Modifier.weight(1f))
}