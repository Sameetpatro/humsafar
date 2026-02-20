// app/src/main/java/com/example/humsafar/ui/MapScreen.kt
// REPLACES EXISTING FILE
//
// CHANGES vs original:
//   1. MapScreen() now accepts onNavigateToVoice: (name, id) → Unit
//   2. MapContent() threads it down to BottomGlassPanel
//   3. BottomGlassPanel gets a "Voice Guide" button beside "Explore with AI Guide"
//   All other logic is IDENTICAL to the original.

package com.example.humsafar.ui

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.humsafar.BuildConfig
import com.example.humsafar.ChatbotActivity
import com.example.humsafar.data.HeritageRepository
import com.example.humsafar.geofence.GeofencePermissionHandler
import com.example.humsafar.geofence.GeofenceTransitionReceiver
import com.example.humsafar.location.HumsafarLocationManager
import com.example.humsafar.models.HeritageSite
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*
import com.example.humsafar.utils.haversineDistance
import com.google.accompanist.permissions.*
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private val MAP_STYLE
    get() = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_KEY}"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    // NEW: navigation callback injected from AppNavigation
    onNavigateToVoice: (siteName: String, siteId: String) -> Unit = { _, _ -> }
) {
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { locationPermission.launchPermissionRequest() }

    when {
        locationPermission.status.isGranted -> MapContent(onNavigateToVoice = onNavigateToVoice)
        else -> PermissionGate(locationPermission)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionGate(state: PermissionState) {
    Box(Modifier.fillMaxSize()) {
        AnimatedOrbBackground(Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📍", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Location Access", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Humsafar needs your location to detect nearby heritage sites",
                color = TextSecondary, fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            GlassPrimaryButton("Allow Location", onClick = { state.launchPermissionRequest() })
        }
    }
}

@Composable
fun MapContent(
    onNavigateToVoice: (String, String) -> Unit = { _, _ -> }
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var userLat        by remember { mutableStateOf<Double?>(null) }
    var userLng        by remember { mutableStateOf<Double?>(null) }
    var insideSite     by remember { mutableStateOf<HeritageSite?>(null) }
    var sortedSites    by remember { mutableStateOf<List<Pair<HeritageSite, Double>>>(emptyList()) }
    var mapLibreMap    by remember { mutableStateOf<MapLibreMap?>(null) }
    var userMarkerAdded by remember { mutableStateOf(false) }

    val mapView         = remember { MapLibre.getInstance(context); MapView(context) }
    val locationManager = remember { HumsafarLocationManager(context) }

    GeofencePermissionHandler()

    // React to background geofence transitions
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: Intent) {
                val siteId     = intent.getStringExtra(GeofenceTransitionReceiver.EXTRA_SITE_ID) ?: return
                val transition = intent.getStringExtra(GeofenceTransitionReceiver.EXTRA_TRANSITION)
                when (transition) {
                    GeofenceTransitionReceiver.TRANSITION_ENTER ->
                        insideSite = HeritageRepository.sites.find { it.id == siteId }
                    GeofenceTransitionReceiver.TRANSITION_EXIT ->
                        if (insideSite?.id == siteId) insideSite = null
                }
            }
        }
        val filter = IntentFilter(GeofenceTransitionReceiver.ACTION_GEOFENCE_UI_UPDATE)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        onDispose { LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver) }
    }

    LaunchedEffect(Unit) {
        locationManager.startUpdates { lat, lng ->
            userLat = lat; userLng = lng
            var found: HeritageSite? = null
            val distances = HeritageRepository.sites.map { site ->
                val dist = haversineDistance(lat, lng, site.latitude, site.longitude)
                if (dist < site.radius && found == null) found = site
                site to dist
            }.sortedBy { it.second }
            insideSite = found; sortedSites = distances
            val ll = LatLng(lat, lng)
            mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 14.0))
            if (!userMarkerAdded) {
                mapLibreMap?.addMarker(MarkerOptions().position(ll).title("You are here"))
                userMarkerAdded = true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> { locationManager.stopUpdates(); mapView.onDestroy() }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        mapView.onCreate(Bundle())
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs); locationManager.stopUpdates() }
    }

    Box(Modifier.fillMaxSize()) {

        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { mv ->
            mv.getMapAsync { map ->
                mapLibreMap = map
                map.setStyle(MAP_STYLE) {
                    HeritageRepository.sites.forEach { site ->
                        map.addMarker(MarkerOptions().position(LatLng(site.latitude, site.longitude)).title(site.name))
                    }
                    userLat?.let { lat -> userLng?.let { lng ->
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 14.0))
                    }}
                }
            }
        }

        // Top status pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(Color(0xCC050D1A), Color(0xBB0A1628))))
                .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                    0.4f, 1f,
                    infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "p"
                )
                Box(Modifier.size(8.dp).scale(pulse).clip(CircleShape)
                    .background(if (insideSite != null) Color(0xFF4ADE80) else AccentYellow))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (insideSite != null) "Inside ${insideSite!!.name}" else "Scanning for heritage sites…",
                    color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        BottomGlassPanel(
            insideSite        = insideSite,
            sortedSites       = sortedSites,
            context           = context,
            onNavigateToVoice = onNavigateToVoice,   // NEW
            modifier          = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun BottomGlassPanel(
    insideSite:        HeritageSite?,
    sortedSites:       List<Pair<HeritageSite, Double>>,
    context:           android.content.Context,
    onNavigateToVoice: (String, String) -> Unit,          // NEW param
    modifier:          Modifier = Modifier
) {
    var showNearby by remember { mutableStateOf(false) }
    LaunchedEffect(insideSite) { showNearby = false }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(Color(0xDD050D1A), Color(0xF5050D1A))))
            .border(
                0.7.dp,
                Brush.verticalGradient(listOf(GlassBorderBright, Color.Transparent)),
                RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
    ) {
        // Drag handle
        Box(
            Modifier.align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(50)).background(GlassWhite30)
        )

        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {

            if (insideSite != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("You are here")
                        Spacer(Modifier.height(4.dp))
                        Text(insideSite.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(GlassWhite15)
                            .border(0.5.dp, GlassBorder, RoundedCornerShape(50))
                    ) {
                        Row {
                            listOf("Site" to false, "Nearby" to true).forEach { (label, target) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (showNearby == target) GlassWhite30 else Color.Transparent)
                                        .clickable { showNearby = target }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                AnimatedContent(targetState = showNearby, label = "panel") { nearby ->
                    if (!nearby) {
                        Column {

                            // ── "Explore with AI Guide" (text chat) ──────
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))))
                                    .border(0.7.dp, Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF))), RoundedCornerShape(20.dp))
                                    .clickable {
                                        context.startActivity(
                                            Intent(context, ChatbotActivity::class.java).apply {
                                                putExtra("SITE_NAME", insideSite.name)
                                                putExtra("SITE_ID",   insideSite.id)
                                            }
                                        )
                                    }
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.matchParentSize().clip(RoundedCornerShape(20.dp))
                                    .background(Brush.verticalGradient(listOf(Color(0x44FFFFFF), Color.Transparent))))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏛️", fontSize = 22.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Explore with AI Guide", color = DeepNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("History, architecture & more", color = Color(0x99050D1A), fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // ── "Voice Guide" (NEW — voice pipeline) ─────
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFF1A3A6B), Color(0xFF0D2040))))
                                    .border(0.7.dp, GlassBorderBright, RoundedCornerShape(20.dp))
                                    .clickable { onNavigateToVoice(insideSite.name, insideSite.id) }
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.matchParentSize().clip(RoundedCornerShape(20.dp))
                                    .background(Brush.verticalGradient(listOf(Color(0x22FFFFFF), Color.Transparent))))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mic, null, tint = AccentYellow, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Voice Guide", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text("Speak your questions aloud", color = TextTertiary, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // ── Google Maps ───────────────────────────────
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GlassWhite15)
                                    .border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        val uri = Uri.parse("geo:${insideSite.latitude},${insideSite.longitude}?q=${insideSite.latitude},${insideSite.longitude}(${insideSite.name})")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Open in Google Maps", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        // Nearby sites list (unchanged from original)
                        val nearby = sortedSites.filter { (s, _) -> s.id != insideSite.id }
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(nearby) { (site, dist) ->
                                GlassCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, tint = GlassWhite10) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(site.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            Text(formatDistance(dist), color = TextTertiary, fontSize = 13.sp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp)).background(GlassWhite15)
                                                .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${site.latitude},${site.longitude}")))
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text("Maps", color = AccentYellow, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // No site — show sorted list (identical to original)
                Text("Nearby Heritage Sites", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (sortedSites.isEmpty()) "Acquiring GPS signal…" else "Sorted by distance from you",
                    color = TextTertiary, fontSize = 13.sp
                )
                Spacer(Modifier.height(16.dp))

                if (sortedSites.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val spin by rememberInfiniteTransition(label = "s").animateFloat(
                            0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "rot"
                        )
                        Text("◌", color = AccentYellow, fontSize = 20.sp, modifier = Modifier.rotate(spin))
                        Spacer(Modifier.width(12.dp))
                        Text("Searching for your location…", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedSites) { (site, dist) ->
                            GlassCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, tint = GlassWhite10) {
                                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(site.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text(formatDistance(dist), color = TextTertiary, fontSize = 12.sp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp)).background(GlassWhite15)
                                            .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                                            .clickable {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${site.latitude},${site.longitude}")))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("Maps", color = AccentYellow, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000) "${"%.1f".format(meters / 1000)} km away"
    else "${meters.toInt()} m away"