package com.example.humsafar.ui

import android.os.Bundle
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.humsafar.BuildConfig
import com.example.humsafar.data.TripManager
import com.example.humsafar.location.HumsafarLocationManager
import com.example.humsafar.models.NodePositionResponse
import com.example.humsafar.network.HumsafarClient
import com.example.humsafar.ui.components.AnimatedOrbBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.theme.*
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

private val MAP_STYLE
    get() = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_KEY}"

private val KingNodeColor = Color(0xFFFF4444)
private val VisitedNodeColor = Color(0xFF4ADE80)
private val UnvisitedNodeColor = Color(0xFFFF9800)
private val UserLocationColor = Color(0xFF2196F3)

@Composable
fun DirectionsScreen(
    siteId: Int,
    siteName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tripState by TripManager.state.collectAsStateWithLifecycle()

    // Nodes from dedicated endpoint: exact lat/lng from backend nodes table
    var nodes by remember { mutableStateOf<List<NodePositionResponse>>(emptyList()) }
    var nodesLoading by remember { mutableStateOf(true) }
    var nodesError by remember { mutableStateOf<String?>(null) }

    // User location: prefer live updates, fallback to TripManager's last known
    var userLat by remember { mutableStateOf<Double?>(tripState.lastLat.takeIf { it != 0.0 }) }
    var userLng by remember { mutableStateOf<Double?>(tripState.lastLng.takeIf { it != 0.0 }) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleLoaded by remember(siteId) { mutableStateOf(false) }
    var nodeMarkersAdded by remember(siteId) { mutableStateOf(false) }
    var userMarkerRef by remember { mutableStateOf<Marker?>(null) }
    var mapInitRequested by remember(siteId) { mutableStateOf(false) }

    val mapView = remember(siteId) { MapLibre.getInstance(context); MapView(context) }
    val locationManager = remember { HumsafarLocationManager(context) }

    // Reset user marker ref when map changes (new site = new MapView)
    LaunchedEffect(mapLibreMap) {
        userMarkerRef = null
    }

    LaunchedEffect(siteId) {
        nodeMarkersAdded = false
        styleLoaded = false
        userMarkerRef = null
        mapLibreMap = null
        nodesLoading = true
        nodesError = null
        try {
            val resp = withContext(Dispatchers.IO) {
                HumsafarClient.api.getSiteNodes(siteId)
            }
            if (resp.isSuccessful && resp.body() != null) {
                nodes = resp.body()!!.sortedBy { it.sequenceOrder }
            } else {
                nodesError = "Could not load nodes"
            }
        } catch (e: Exception) {
            nodesError = e.message ?: "Connection error"
        }
        nodesLoading = false
    }

    // Start location updates; getLastLocation() gives an immediate fix when available
    LaunchedEffect(Unit) {
        locationManager.startUpdates { lat, lng ->
            userLat = lat
            userLng = lng
            TripManager.updateLocation(lat, lng)
        }
    }

    // Add or update user marker when we have map and location
    LaunchedEffect(mapLibreMap, userLat, userLng) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val lat = userLat ?: return@LaunchedEffect
        val lng = userLng ?: return@LaunchedEffect
        val userPos = LatLng(lat, lng)
        val current = userMarkerRef
        if (current != null) {
            current.setPosition(userPos)
        } else {
            userMarkerRef = map.addMarker(
                MarkerOptions().position(userPos).title("You are here")
            )
        }
    }

    // Add node markers ONLY when style is loaded + we have map + nodes (uses reactive state, not stale closure)
    LaunchedEffect(styleLoaded, mapLibreMap, nodes, siteId) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!styleLoaded || nodes.isEmpty() || nodeMarkersAdded) return@LaunchedEffect
        val nodeList = nodes
        val visitedIds = tripState.visitedNodeIds
        val boundsBuilder = LatLngBounds.Builder()
        nodeList.forEach { node ->
            val position = LatLng(node.latitude, node.longitude)
            boundsBuilder.include(position)
            val isVisited = node.id in visitedIds
            val markerColor = when {
                node.isKing -> KingNodeColor
                isVisited -> VisitedNodeColor
                else -> UnvisitedNodeColor
            }
            val icon = createColoredMarkerIcon(context, markerColor)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(icon)
                    .title(node.name)
                    .snippet(
                        when {
                            node.isKing -> "King Node (Start/End)"
                            isVisited -> "Visited"
                            else -> "Not visited yet"
                        }
                    )
            )
        }
        userLat?.let { lat -> userLng?.let { lng -> boundsBuilder.include(LatLng(lat, lng)) } }
        try {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80))
        } catch (e: Exception) {
            nodeList.firstOrNull()?.let { n ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(n.latitude, n.longitude), 16.0))
            }
        }
        nodeMarkersAdded = true
    }

    DisposableEffect(lifecycleOwner, siteId) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    locationManager.stopUpdates()
                    mapView.onDestroy()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        mapView.onCreate(Bundle())
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            locationManager.stopUpdates()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())

        when {
            nodesLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️", fontSize = 52.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading directions…", color = TextPrimary, fontSize = 16.sp)
                    }
                }
            }
            nodesError != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            nodesError!!,
                            color = Color(0xFFFF6B6B),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }
            else -> {
                Column(Modifier.fillMaxSize()) {
                    DirectionsTopBar(
                        siteName = siteName,
                        onBack = onBack
                    )

                    Box(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    ) {
                        key(siteId) {
                            AndroidView(
                                factory = { mapView },
                                modifier = Modifier.fillMaxSize()
                            ) { mv ->
                                if (!mapInitRequested) {
                                    mapInitRequested = true
                                    mv.getMapAsync { map ->
                                        mapLibreMap = map
                                        map.setStyle(MAP_STYLE) {
                                            styleLoaded = true
                                        }
                                    }
                                }
                            }
                        }

                        MapLegend(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    NodesProgressList(
                        nodes = nodes,
                        visitedIds = tripState.visitedNodeIds,
                        currentNodeId = tripState.currentNodeId,
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DirectionsTopBar(
    siteName: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xF0050D1A), Color(0xBB050D1A))))
            .drawBehind {
                drawLine(GlassBorder, Offset(0f, size.height), Offset(size.width, size.height), 0.5.dp.toPx())
            }
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassWhite15)
                    .border(0.5.dp, GlassBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF1976D2)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Current Scenario",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = siteName,
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier,
        cornerRadius = 12.dp,
        tint = Color(0xDD050D1A)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LegendItem(color = KingNodeColor, label = "Entry Gate")
            LegendItem(color = VisitedNodeColor, label = "Visited")
            LegendItem(color = UnvisitedNodeColor, label = "Not Visited")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun NodesProgressList(
    nodes: List<NodePositionResponse>,
    visitedIds: List<Int>,
    currentNodeId: Int,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🗺️ Trip Progress",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GlassWhite15)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${visitedIds.size}/${nodes.size} visited",
                        color = AccentYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(nodes) { node ->
                    val isKing = node.isKing
                    val isVisited = node.id in visitedIds
                    val isCurrent = node.id == currentNodeId

                    NodeProgressItem(
                        node = node,
                        isKing = isKing,
                        isVisited = isVisited,
                        isCurrent = isCurrent
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeProgressItem(
    node: NodePositionResponse,
    isKing: Boolean,
    isVisited: Boolean,
    isCurrent: Boolean
) {
    val backgroundColor = when {
        isCurrent -> Color(0x33FFD54F)
        isVisited -> Color(0x224ADE80)
        else -> GlassWhite10
    }

    val borderColor = when {
        isCurrent -> AccentYellow.copy(alpha = 0.5f)
        isVisited -> VisitedNodeColor.copy(alpha = 0.3f)
        else -> GlassBorder
    }

    val nodeColor = when {
        isKing -> KingNodeColor
        isVisited -> VisitedNodeColor
        else -> UnvisitedNodeColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(0.7.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(nodeColor.copy(alpha = 0.2f))
                .border(1.5.dp, nodeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                isKing -> Text("👑", fontSize = 14.sp)
                isVisited -> Icon(
                    Icons.Default.Check,
                    null,
                    tint = VisitedNodeColor,
                    modifier = Modifier.size(16.dp)
                )
                else -> Text(
                    "${node.sequenceOrder}",
                    color = UnvisitedNodeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    node.name,
                    color = if (isCurrent) AccentYellow else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                )
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AccentYellow)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("NOW", color = DeepNavy, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Text(
                when {
                    isKing -> "Start/End Point"
                    isVisited -> "Completed"
                    else -> "Pending"
                },
                color = TextTertiary,
                fontSize = 11.sp
            )
        }

        if (isVisited) {
            Icon(
                Icons.Default.CheckCircle,
                null,
                tint = VisitedNodeColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun createColoredMarkerIcon(context: android.content.Context, composeColor: Color): org.maplibre.android.annotations.Icon {
    val sizePx = (48 * context.resources.displayMetrics.density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val androidColor = android.graphics.Color.argb(
        (composeColor.alpha * 255).toInt(),
        (composeColor.red * 255).toInt(),
        (composeColor.green * 255).toInt(),
        (composeColor.blue * 255).toInt()
    )
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = androidColor
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2, paint)
    val borderPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
        this.color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 3, borderPaint)
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}
