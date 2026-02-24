package com.example.humsafar.ui

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    onNodeReady: (Long, String, Boolean) -> Unit,  // nodeId, nodeName, isKing
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

    Box(Modifier.fillMaxSize()) {

        // Camera preview
        if (camPerm.status.isGranted &&
            uiState !is QrUiState.ShowingNodes &&
            uiState !is QrUiState.FetchingLocation) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        val preview = Preview.Builder().build()
                            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val scanner = BarcodeScanning.getClient()
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().also { ia ->
                                ia.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                                    val img = proxy.image
                                    if (img != null && uiState is QrUiState.Scanning) {
                                        scanner.process(
                                            InputImage.fromMediaImage(img, proxy.imageInfo.rotationDegrees)
                                        ).addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull {
                                                it.format == Barcode.FORMAT_QR_CODE
                                            }?.rawValue?.let { viewModel.onQrDetected(it) }
                                        }.addOnCompleteListener { proxy.close() }
                                    } else proxy.close()
                                }
                            }
                        val cam = provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                        )
                        cameraControl = cam.cameraControl
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            // Dark overlay at top/bottom
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xBB000000), Color.Transparent, Color.Transparent, Color(0xBB000000)))
                )
            )
        } else if (uiState is QrUiState.ShowingNodes || uiState is QrUiState.FetchingLocation) {
            AnimatedOrbBackground(Modifier.fillMaxSize())
        }

        Column(Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape)
                            .background(Color(0xBB000000))
                            .border(0.5.dp, GlassBorder, CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Scan Node QR", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ── Main content area ─────────────────────────────────────────
            when (uiState) {

                is QrUiState.Scanning, is QrUiState.Validating -> {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        // Scan frame
                        Box(
                            Modifier.size(260.dp)
                                .border(2.dp, AccentYellow, RoundedCornerShape(20.dp))
                        ) {
                            // Corner accents
                            listOf(
                                Alignment.TopStart, Alignment.TopEnd,
                                Alignment.BottomStart, Alignment.BottomEnd
                            ).forEach { align ->
                                Box(
                                    Modifier.size(24.dp).align(align)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentYellow)
                                )
                            }
                        }
                    }
                    // Status text
                    Box(
                        Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(Color(0xCC000000))
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                if (uiState is QrUiState.Validating) "⏳ Verifying node…"
                                else "Point camera at node QR code",
                                color = Color.White, fontSize = 14.sp
                            )
                        }
                    }
                }

                is QrUiState.FetchingLocation -> {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📍", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("Fetching your location…", color = TextPrimary, fontSize = 16.sp)
                            Text("Finding nearby nodes", color = TextTertiary, fontSize = 13.sp)
                        }
                    }
                }

                is QrUiState.ShowingNodes -> {
                    val nodes = (uiState as QrUiState.ShowingNodes).nodes
                    Column(
                        Modifier.weight(1f).padding(horizontal = 20.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Text("Nearby Nodes", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Tap a node to select it", color = TextTertiary, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(nodes) { node ->
                                NodeSelectCard(
                                    node = node,
                                    currentLat = currentLat,
                                    currentLng = currentLng,
                                    onClick = { viewModel.selectNodeManually(node.id) }
                                )
                            }
                        }
                    }
                }

                is QrUiState.Error -> {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Text("⚠️", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                (uiState as QrUiState.Error).message,
                                color = Color(0xFFFF6B6B), fontSize = 14.sp, textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(20.dp))
                            Box(
                                Modifier.clip(RoundedCornerShape(50))
                                    .background(GlassWhite15)
                                    .border(0.5.dp, GlassBorder, RoundedCornerShape(50))
                                    .clickable { viewModel.resetScanner() }
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text("Try Again", color = AccentYellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                is QrUiState.Success -> {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Node Verified!", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Bottom controls ───────────────────────────────────────────
            if (uiState is QrUiState.Scanning || uiState is QrUiState.Validating) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(Color(0xCC000000))
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flashlight
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(GlassWhite15)
                            .border(0.5.dp, GlassBorder, CircleShape)
                            .clickable {
                                flashEnabled = !flashEnabled
                                cameraControl?.enableTorch(flashEnabled)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (flashEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            null, tint = if (flashEnabled) AccentYellow else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Trouble scanning
                    Box(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(GlassWhite15)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(50))
                            .clickable {
                                viewModel.fetchNearbyNodes(currentLat, currentLng, monumentId)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("Trouble scanning?", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeSelectCard(
    node: MonumentNode,
    currentLat: Double,
    currentLng: Double,
    onClick: () -> Unit
) {
    val dist = com.example.humsafar.utils.haversineDistance(
        currentLat, currentLng, node.latitude, node.longitude
    )
    val distText = if (dist >= 1000) "${"%.1f".format(dist / 1000)} km" else "${dist.toInt()} m"

    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (node.nodeType == "KING") Color(0x33FFD54F) else GlassWhite15
            )
            .border(
                width = if (node.nodeType == "KING") 1.5.dp else 0.7.dp,
                color = if (node.nodeType == "KING") AccentYellow else GlassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (node.nodeType == "KING") "👑" else "📍", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        node.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (node.nodeType == "KING") FontWeight.Bold else FontWeight.Medium
                    )
                    if (node.nodeType == "KING") {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(Color(0x44FFD54F))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ENTRY", color = AccentYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (node.recommended) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(Color(0x4400FF88))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("RECOMMENDED", color = Color(0xFF00FF88), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(distText, color = TextTertiary, fontSize = 12.sp)
            }
        }
    }
}