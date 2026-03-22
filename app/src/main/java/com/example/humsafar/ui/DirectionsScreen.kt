// app/src/main/java/com/example/humsafar/ui/DirectionsScreen.kt
// UPDATED — amenity markers (🚻 washrooms, 🛍️ shops) added to the trip map.

package com.example.humsafar.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
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
import com.example.humsafar.models.AmenityResponse
import com.example.humsafar.models.NodePositionResponse
import com.example.humsafar.network.HumsafarClient
import com.example.humsafar.ui.components.AnimatedOrbBackground
import com.example.humsafar.ui.components.GlassCard
import com.example.humsafar.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private val MAP_STYLE
    get() = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_KEY}"

private val KingNodeColor      = Color(0xFFFF4444)
private val VisitedNodeColor   = Color(0xFF4ADE80)
private val UnvisitedNodeColor = Color(0xFFFF9800)
private val UserLocationColor  = Color(0xFF2196F3)

// ─────────────────────────────────────────────────────────────────────────────
// Emoji bitmap icon — renders an emoji centred on a white circle
// ─────────────────────────────────────────────────────────────────────────────

private fun createEmojiMarkerIcon(
    context: android.content.Context,
    emoji: String,
    sizeDp: Int = 40
): org.maplibre.android.annotations.Icon {
    val density = context.resources.displayMetrics.density
    val sizePx  = (sizeDp * density).toInt()
    val bitmap  = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas  = Canvas(bitmap)

    // White circle background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2, bgPaint)

    // Subtle border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = android.graphics.Color.argb(80, 0, 0, 0)
        style       = Paint.Style.STROKE
        strokeWidth = 2f
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2, borderPaint)

    // Emoji centred
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = sizePx * 0.52f
        typeface  = Typeface.DEFAULT
        textAlign = Paint.Align.CENTER
    }
    val metrics  = textPaint.fontMetrics
    val baseline = sizePx / 2f - (metrics.ascent + metrics.descent) / 2f
    canvas.drawText(emoji, sizePx / 2f, baseline, textPaint)

    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

// ─────────────────────────────────────────────────────────────────────────────
// DirectionsScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DirectionsScreen(
    siteId: Int,
    siteName: String,
    onBack: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tripState      by TripManager.state.collectAsStateWithLifecycle()

    // ── Node data ──────────────────────────────────────────────────────────
    var nodes        by remember { mutableStateOf<List<NodePositionResponse>>(emptyList()) }
    var nodesLoading by remember { mutableStateOf(true) }
    var nodesError   by remember { mutableStateOf<String?>(null) }

    // ── Amenity data ───────────────────────────────────────────────────────
    var amenities    by remember { mutableStateOf<List<AmenityResponse>>(emptyList()) }

    // ── User location ──────────────────────────────────────────────────────
    var userLat by remember { mutableStateOf<Double?>(tripState.lastLat.takeIf { it != 0.0 }) }
    var userLng by remember { mutableStateOf<Double?>(tripState.lastLng.takeIf { it != 0.0 }) }

    // ── Map state ──────────────────────────────────────────────────────────
    var mapLibreMap       by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleLoaded       by remember(siteId) { mutableStateOf(false) }
    var nodeMarkersAdded  by remember(siteId) { mutableStateOf(false) }
    var userMarkerRef     by remember { mutableStateOf<Marker?>(null) }
    var mapInitRequested  by remember(siteId) { mutableStateOf(false) }

    val mapView         = remember(siteId) { MapLibre.getInstance(context); MapView(context) }
    val locationManager = remember { HumsafarLocationManager(context) }

    LaunchedEffect(mapLibreMap) { userMarkerRef = null }

    // ── Fetch nodes + amenities in parallel ────────────────────────────────
    LaunchedEffect(siteId) {
        nodeMarkersAdded = false
        styleLoaded      = false
        userMarkerRef    = null
        mapLibreMap      = null
        nodesLoading     = true
        nodesError       = null

        try {
            // Nodes
            val nodeResp = withContext(Dispatchers.IO) {
                HumsafarClient.api.getSiteNodes(siteId)
            }
            if (nodeResp.isSuccessful && nodeResp.body() != null) {
                nodes = nodeResp.body()!!.sortedBy { it.sequenceOrder }
            } else {
                nodesError = "Could not load nodes"
            }

            // Amenities — fail silently
            try {
                val amenityResp = withContext(Dispatchers.IO) {
                    HumsafarClient.api.getSiteAmenities(siteId)
                }
                if (amenityResp.isSuccessful) {
                    amenities = amenityResp.body() ?: emptyList()
                }
            } catch (_: Exception) { }

        } catch (e: Exception) {
            nodesError = e.message ?: "Connection error"
        }
        nodesLoading = false
    }

    // ── Live location updates ──────────────────────────────────────────────
    LaunchedEffect(Unit) {
        locationManager.startUpdates { lat, lng ->
            userLat = lat
            userLng = lng
            TripManager.updateLocation(lat, lng)
        }
    }

    // ── User marker: add/update whenever map or location changes ───────────
    LaunchedEffect(mapLibreMap, userLat, userLng) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val lat = userLat    ?: return@LaunchedEffect
        val lng = userLng    ?: return@LaunchedEffect
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

    // ── Node + amenity markers: added once style is ready ──────────────────
    LaunchedEffect(styleLoaded, mapLibreMap, nodes, amenities, siteId) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (!styleLoaded || nodes.isEmpty() || nodeMarkersAdded) return@LaunchedEffect

        val visitedIds     = tripState.visitedNodeIds
        val boundsBuilder  = LatLngBounds.Builder()

        // ── Node markers ───────────────────────────────────────────────────
        nodes.forEach { node ->
            val position  = LatLng(node.latitude, node.longitude)
            boundsBuilder.include(position)
            val isVisited = node.id in visitedIds
            val markerColor = when {
                node.isKing -> KingNodeColor
                isVisited   -> VisitedNodeColor
                else        -> UnvisitedNodeColor
            }
            val icon = createColoredMarkerIcon(context, markerColor)
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(icon)
                    .title(node.name)
                    .snippet(when {
                        node.isKing -> "Entry Gate (Start/End)"
                        isVisited   -> "✓ Visited"
                        else        -> "Not visited yet"
                    })
            )
        }

        // ── Amenity markers ────────────────────────────────────────────────
        amenities.forEach { amenity ->
            val position = LatLng(amenity.latitude, amenity.longitude)
            boundsBuilder.include(position)
            val emoji    = if (amenity.type == "washroom") "🚻" else "🛍️"
            val icon     = createEmojiMarkerIcon(context, emoji, sizeDp = 38)
            val snippet  = buildList {
                amenity.priceInfo?.let { add(it) }
                amenity.timing?.let   { add(it) }
            }.joinToString(" · ").ifBlank {
                amenity.type.replaceFirstChar { it.uppercase() }
            }
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(icon)
                    .title(amenity.name)
                    .snippet(snippet)
            )
        }

        // ── Camera: fit all markers ────────────────────────────────────────
        userLat?.let { lat -> userLng?.let { lng -> boundsBuilder.include(LatLng(lat, lng)) } }
        try {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80))
        } catch (e: Exception) {
            nodes.firstOrNull()?.let { n ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(n.latitude, n.longitude), 16.0))
            }
        }

        nodeMarkersAdded = true
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────
    DisposableEffect(lifecycleOwner, siteId) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
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

    // ── UI ─────────────────────────────────────────────────────────────────
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
                            color = Color(0xFFFF6B6B), fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            }

            else -> {
                Column(Modifier.fillMaxSize()) {
                    DirectionsTopBar(siteName = siteName, onBack = onBack)

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
                                factory  = { mapView },
                                modifier = Modifier.fillMaxSize()
                            ) { mv ->
                                if (!mapInitRequested) {
                                    mapInitRequested = true
                                    mv.getMapAsync { map ->
                                        mapLibreMap = map
                                        map.setStyle(MAP_STYLE) { styleLoaded = true }
                                    }
                                }
                            }
                        }

                        // Legend overlay — bottom-left of map
                        MapLegend(
                            showAmenities = amenities.isNotEmpty(),
                            modifier      = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    NodesProgressList(
                        nodes         = nodes,
                        visitedIds    = tripState.visitedNodeIds,
                        currentNodeId = tripState.currentNodeId,
                        amenities     = amenities,
                        modifier      = Modifier
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

// ─────────────────────────────────────────────────────────────────────────────
// Top bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DirectionsTopBar(siteName: String, onBack: () -> Unit) {
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
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(GlassWhite15).border(0.5.dp, GlassBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, null,
                    tint = TextPrimary, modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF1976D2)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Current Scenario", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(siteName, color = TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Map legend
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapLegend(
    showAmenities: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, cornerRadius = 12.dp, tint = Color(0xDD050D1A)) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            LegendItem(color = KingNodeColor,      label = "Entry Gate")
            LegendItem(color = VisitedNodeColor,   label = "Visited")
            LegendItem(color = UnvisitedNodeColor, label = "Not Visited")
            if (showAmenities) {
                // Divider
                Box(
                    Modifier.fillMaxWidth().height(0.5.dp).background(GlassBorder)
                        .padding(vertical = 2.dp)
                )
                EmojiLegendItem(emoji = "🚻", label = "Washroom")
                EmojiLegendItem(emoji = "🛍️", label = "Shop")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(10.dp).clip(CircleShape).background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun EmojiLegendItem(emoji: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 12.sp)
        Spacer(Modifier.width(5.dp))
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Nodes progress list — now also shows amenity summary
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodesProgressList(
    nodes:         List<NodePositionResponse>,
    visitedIds:    List<Int>,
    currentNodeId: Int,
    amenities:     List<AmenityResponse>,
    modifier:      Modifier = Modifier
) {
    val washrooms = amenities.count { it.type == "washroom" }
    val shops     = amenities.count { it.type == "shop" }

    GlassCard(modifier = modifier, cornerRadius = 20.dp) {
        Column(Modifier.padding(16.dp)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗺️ Trip Progress", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassWhite15)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${visitedIds.size}/${nodes.size} visited",
                        color = AccentYellow, fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }
            }

            // Amenity summary chips
            if (amenities.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (washrooms > 0) {
                        AmenityChip(emoji = "🚻", count = washrooms, label = "Washroom")
                    }
                    if (shops > 0) {
                        AmenityChip(emoji = "🛍️", count = shops, label = "Shop")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(nodes) { node ->
                    NodeProgressItem(
                        node      = node,
                        isKing    = node.isKing,
                        isVisited = node.id in visitedIds,
                        isCurrent = node.id == currentNodeId
                    )
                }
            }
        }
    }
}

@Composable
private fun AmenityChip(emoji: String, count: Int, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(GlassWhite10)
            .border(0.5.dp, GlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                "$count $label${if (count > 1) "s" else ""}",
                color = TextSecondary, fontSize = 11.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Node progress item (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NodeProgressItem(
    node:      NodePositionResponse,
    isKing:    Boolean,
    isVisited: Boolean,
    isCurrent: Boolean
) {
    val backgroundColor = when {
        isCurrent -> Color(0x33FFD54F)
        isVisited -> Color(0x224ADE80)
        else      -> GlassWhite10
    }
    val borderColor = when {
        isCurrent -> AccentYellow.copy(alpha = 0.5f)
        isVisited -> VisitedNodeColor.copy(alpha = 0.3f)
        else      -> GlassBorder
    }
    val nodeColor = when {
        isKing    -> KingNodeColor
        isVisited -> VisitedNodeColor
        else      -> UnvisitedNodeColor
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
            modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(nodeColor.copy(alpha = 0.2f))
                .border(1.5.dp, nodeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                isKing    -> Text("👑", fontSize = 14.sp)
                isVisited -> Icon(Icons.Default.Check, null, tint = VisitedNodeColor, modifier = Modifier.size(16.dp))
                else      -> Text("${node.sequenceOrder}", color = UnvisitedNodeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    node.name,
                    color      = if (isCurrent) AccentYellow else TextPrimary,
                    fontSize   = 14.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                )
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(AccentYellow).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("NOW", color = DeepNavy, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold) }
                }
            }
            Text(
                when { isKing -> "Start/End Point"; isVisited -> "Completed"; else -> "Pending" },
                color = TextTertiary, fontSize = 11.sp
            )
        }

        if (isVisited) {
            Icon(Icons.Default.CheckCircle, null, tint = VisitedNodeColor, modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Coloured circle marker (for nodes — same as before)
// ─────────────────────────────────────────────────────────────────────────────

private fun createColoredMarkerIcon(
    context:      android.content.Context,
    composeColor: Color
): org.maplibre.android.annotations.Icon {
    val sizePx = (48 * context.resources.displayMetrics.density).toInt()
    val bitmap = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val androidColor = android.graphics.Color.argb(
        (composeColor.alpha * 255).toInt(),
        (composeColor.red   * 255).toInt(),
        (composeColor.green * 255).toInt(),
        (composeColor.blue  * 255).toInt()
    )
    val paint = android.graphics.Paint().apply { isAntiAlias = true; color = androidColor }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2, paint)
    val borderPaint = android.graphics.Paint().apply {
        isAntiAlias = true; style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f; color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 3, borderPaint)
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}