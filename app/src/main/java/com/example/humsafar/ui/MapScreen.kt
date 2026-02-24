// app/src/main/java/com/example/humsafar/ui/MapScreen.kt
// UPDATED — onNavigateToQrScan now passes Int siteId (not Long).
// Removed dead HeritageRepository.loadMonuments() call.
// Everything else (map, geofence, UI) unchanged.

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.humsafar.BuildConfig
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

private val QrTealBright = Color(0xFF2DD4BF)
private val QrTealMid    = Color(0xFF14B8A6)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToVoice:   (String, String) -> Unit = { _, _ -> },
    onNavigateToDetail:  (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {},
    onNavigateToQrScan:  (Int) -> Unit = {}          // ← Int, not Long
) {
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { locationPermission.launchPermissionRequest() }

    if (locationPermission.status.isGranted) {
        MapContent(onNavigateToVoice, onNavigateToDetail, onNavigateToProfile, onNavigateToQrScan)
    } else {
        PermissionGate(locationPermission)
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
                color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            GlassPrimaryButton("Allow Location", onClick = { state.launchPermissionRequest() })
        }
    }
}

@Composable
fun MapContent(
    onNavigateToVoice:   (String, String) -> Unit = { _, _ -> },
    onNavigateToDetail:  (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {},
    onNavigateToQrScan:  (Int) -> Unit = {}
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var userLat         by remember { mutableStateOf<Double?>(null) }
    var userLng         by remember { mutableStateOf<Double?>(null) }
    var insideSite      by remember { mutableStateOf<HeritageSite?>(null) }
    var sortedSites     by remember { mutableStateOf<List<Pair<HeritageSite, Double>>>(emptyList()) }
    var mapLibreMap     by remember { mutableStateOf<MapLibreMap?>(null) }
    var userMarkerAdded by remember { mutableStateOf(false) }

    val mapView         = remember { MapLibre.getInstance(context); MapView(context) }
    val locationManager = remember { HumsafarLocationManager(context) }

    GeofencePermissionHandler()

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
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(receiver, IntentFilter(GeofenceTransitionReceiver.ACTION_GEOFENCE_UI_UPDATE))
        onDispose { LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver) }
    }

    LaunchedEffect(Unit) {
        locationManager.startUpdates { lat, lng ->
            userLat = lat; userLng = lng
            var found: HeritageSite? = null
            val distances = HeritageRepository.sites.map { site ->
                val d = haversineDistance(lat, lng, site.latitude, site.longitude)
                if (d < site.radius && found == null) found = site
                site to d
            }.sortedBy { it.second }
            insideSite  = found
            sortedSites = distances
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

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter).statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp).fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                        .background(Brush.linearGradient(listOf(Color(0xCC050D1A), Color(0xBB0A1628))))
                        .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotPulse by rememberInfiniteTransition(label = "dp").animateFloat(
                            0.4f, 1f,
                            infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "d"
                        )
                        Box(Modifier.size(8.dp).scale(dotPulse).clip(CircleShape)
                            .background(if (insideSite != null) Color(0xFF4ADE80) else AccentYellow))
                        Spacer(Modifier.width(9.dp))
                        Text(
                            if (insideSite != null) "Inside ${insideSite!!.name}" else "Scanning for heritage sites…",
                            color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xCC050D1A), Color(0xBB0A1628))))
                        .border(0.7.dp, GlassBorderBright, CircleShape)
                        .clickable { onNavigateToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = AccentYellow, modifier = Modifier.size(22.dp))
                }
            }

            AnimatedVisibility(
                visible = insideSite != null,
                enter   = fadeIn(tween(280)) + slideInVertically(tween(280, easing = EaseOutCubic)) { -it },
                exit    = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it }
            ) {
                Spacer(Modifier.height(10.dp))
                QrScanButton(onClick = {
                    insideSite?.id?.toIntOrNull()?.let { onNavigateToQrScan(it) }
                })
            }
        }

        BottomGlassPanel(
            insideSite         = insideSite,
            sortedSites        = sortedSites,
            context            = context,
            onNavigateToDetail = onNavigateToDetail,
            modifier           = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun QrScanButton(onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "qb")
    val shimmer by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "sh")
    val edgeGlow by inf.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse), label = "eg")

    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF091F1E), Color(0xFF0D2825))))
            .border(1.dp, Brush.linearGradient(
                colorStops = arrayOf(
                    (shimmer + 0.0f).rem(1f) to QrTealBright.copy(alpha = edgeGlow),
                    (shimmer + 0.3f).rem(1f) to QrTealMid.copy(alpha = edgeGlow * 0.5f),
                    (shimmer + 0.6f).rem(1f) to QrTealBright.copy(alpha = edgeGlow)
                )
            ), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(QrTealBright.copy(0.22f), QrTealMid.copy(0.08f))))
                    .border(0.8.dp, QrTealBright.copy(0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.QrCodeScanner, null, tint = QrTealBright, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Scan Node QR", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Unlock the AI guide for this spot", color = QrTealBright.copy(alpha = 0.65f), fontSize = 11.sp)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(QrTealBright.copy(alpha = 0.14f))
                    .border(0.5.dp, QrTealBright.copy(0.3f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) { Text("›", color = QrTealBright, fontSize = 18.sp, fontWeight = FontWeight.Light) }
        }
    }
}

@Composable
private fun BottomGlassPanel(
    insideSite:         HeritageSite?,
    sortedSites:        List<Pair<HeritageSite, Double>>,
    context:            android.content.Context,
    onNavigateToDetail: (String, String) -> Unit,
    modifier:           Modifier = Modifier
) {
    var showNearby by remember { mutableStateOf(false) }
    LaunchedEffect(insideSite) { showNearby = false }

    Column(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Brush.verticalGradient(listOf(Color(0xDD050D1A), Color(0xF5050D1A))))
            .border(0.7.dp, Brush.verticalGradient(listOf(GlassBorderBright, Color.Transparent)), RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp, bottom = 8.dp)
            .size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(GlassWhite30))

        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            if (insideSite != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("You are here")
                        Spacer(Modifier.height(4.dp))
                        Text(insideSite.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(GlassWhite15).border(0.5.dp, GlassBorder, RoundedCornerShape(50))) {
                        Row {
                            listOf("Site" to false, "Nearby" to true).forEach { (label, target) ->
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(if (showNearby == target) GlassWhite30 else Color.Transparent)
                                        .clickable { showNearby = target }.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) { Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                AnimatedContent(targetState = showNearby, label = "panel") { nearby ->
                    if (!nearby) {
                        Column {
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))))
                                    .clickable { onNavigateToDetail(insideSite.name, insideSite.id) }
                                    .padding(vertical = 22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏛️", fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Explore ${insideSite.name}", color = DeepNavy, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                        Text("History • AI Guide • Voice • Video", color = Color(0x99050D1A), fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(GlassWhite15).border(0.7.dp, GlassBorder, RoundedCornerShape(16.dp))
                                    .clickable {
                                        context.startActivity(Intent(Intent.ACTION_VIEW,
                                            Uri.parse("geo:${insideSite.latitude},${insideSite.longitude}?q=${insideSite.latitude},${insideSite.longitude}(${insideSite.name})")))
                                    }.padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Open in Google Maps", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sortedSites.filter { (s, _) -> s.id != insideSite.id }) { (site, dist) ->
                                SiteDistanceRow(site, dist, context)
                            }
                        }
                    }
                }
            } else {
                Text("Nearby Heritage Sites", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(if (sortedSites.isEmpty()) "Acquiring GPS signal…" else "Sorted by distance", color = TextTertiary, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                if (sortedSites.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val spin by rememberInfiniteTransition(label = "sp").animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "r")
                        Text("◌", color = AccentYellow, fontSize = 20.sp, modifier = Modifier.rotate(spin))
                        Spacer(Modifier.width(12.dp))
                        Text("Searching for your location…", color = TextSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sortedSites) { (site, dist) -> SiteDistanceRow(site, dist, context) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteDistanceRow(site: HeritageSite, dist: Double, context: android.content.Context) {
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, tint = GlassWhite10) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(site.name, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(if (dist >= 1000) "${"%.1f".format(dist / 1000)} km away" else "${dist.toInt()} m away", color = TextTertiary, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(GlassWhite15)
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${site.latitude},${site.longitude}"))) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) { Text("Maps", color = AccentYellow, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        }
    }
}