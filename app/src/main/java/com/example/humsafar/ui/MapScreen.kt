// app/src/main/java/com/example/humsafar/ui/MapScreen.kt
// FIXED:
//  1. Removed amenity markers (washrooms/shops) from map
//  2. Fixed location detection — LocationBasedSiteDetector now wired with api in MapContent
//  3. HeritageRepository.fetchAll() called early so geofence data is ready
//  4. LocationBasedSiteDetector.forceRefresh() on first location fix so site detection is immediate

package com.example.humsafar.ui

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.humsafar.BuildConfig
import com.example.humsafar.auth.AuthManager
import com.example.humsafar.data.HeritageRepository
import com.example.humsafar.data.LocationBasedSiteDetector
import com.example.humsafar.geofence.GeofencePermissionHandler
import com.example.humsafar.geofence.GeofenceTransitionReceiver
import com.example.humsafar.location.HumsafarLocationManager
import com.example.humsafar.models.HeritageSite
import com.example.humsafar.network.HumsafarClient
import com.example.humsafar.network.WeatherService
import com.example.humsafar.ui.components.*
import com.example.humsafar.ui.theme.*
import com.example.humsafar.utils.haversineDistance
import com.google.accompanist.permissions.*
import kotlinx.coroutines.*
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private val MAP_STYLE
    get() = "https://api.maptiler.com/maps/streets/style.json?key=${BuildConfig.MAPTILER_KEY}"

// ─────────────────────────────────────────────────────────────────────────────
// Main screen entry
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    onNavigateToVoice:    (String, String) -> Unit = { _, _ -> },
    onNavigateToDetail:   (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile:  () -> Unit = {},
    onNavigateToQrScan:   (Int) -> Unit = {},
    onNavigateToSiteInfo: (Int, String) -> Unit = { _, _ -> }
) {
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { locationPermission.launchPermissionRequest() }

    if (locationPermission.status.isGranted) {
        MapContent(
            onNavigateToVoice    = onNavigateToVoice,
            onNavigateToDetail   = onNavigateToDetail,
            onNavigateToProfile  = onNavigateToProfile,
            onNavigateToQrScan   = onNavigateToQrScan,
            onNavigateToSiteInfo = onNavigateToSiteInfo
        )
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

// ─────────────────────────────────────────────────────────────────────────────
// MapContent — FIXED location detection, NO amenity markers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MapContent(
    onNavigateToVoice:    (String, String) -> Unit = { _, _ -> },
    onNavigateToDetail:   (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile:  () -> Unit = {},
    onNavigateToQrScan:   (Int) -> Unit = {},
    onNavigateToSiteInfo: (Int, String) -> Unit = { _, _ -> }
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val accent         = LocalAccent.current
    val tokens         = LocalAppColors.current
    val currentAuthUser by AuthManager.currentUser.collectAsStateWithLifecycle()
    var welcomeDismissed by rememberSaveable { mutableStateOf(false) }
    val showWelcome      = currentAuthUser != null && !welcomeDismissed

    var showExitDialog   by remember { mutableStateOf(false) }
    var userLat          by remember { mutableStateOf<Double?>(null) }
    var userLng          by remember { mutableStateOf<Double?>(null) }
    var mapLibreMap      by remember { mutableStateOf<MapLibreMap?>(null) }
    var userMarkerAdded  by remember { mutableStateOf(false) }
    var tappedSite       by remember { mutableStateOf<HeritageSite?>(null) }
    var firstFix         by remember { mutableStateOf(false) }

    val mapView         = remember { MapLibre.getInstance(context); MapView(context) }
    val locationManager = remember { HumsafarLocationManager(context) }

    BackHandler { showExitDialog = true }
    GeofencePermissionHandler()

    // ── Wire the API once ─────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        LocationBasedSiteDetector.api = HumsafarClient.api
    }

    // ── Observe detector output ───────────────────────────────────────────
    // nearbySites  = all sites sorted by distance  → drives the "Nearby" panel
    // currentSite  = the site user is inside right now (null = outside all)
    val nearbySites by LocationBasedSiteDetector.nearbySites.collectAsStateWithLifecycle()
    val currentSite by LocationBasedSiteDetector.currentSite.collectAsStateWithLifecycle()

    // Convert NearbySiteUi → HeritageSite for the bottom panel
    val insideSite: HeritageSite? = currentSite?.let { s ->
        HeritageSite(s.id.toString(), s.name, s.latitude, s.longitude, 300.0)
    }

    val sortedSites: List<Pair<HeritageSite, Double>> = nearbySites.map { s ->
        HeritageSite(s.id.toString(), s.name, s.latitude, s.longitude, 300.0) to s.distanceMeters
    }

    // ── GPS updates ───────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        locationManager.startUpdates { lat, lng ->
            userLat = lat
            userLng = lng

            if (!firstFix) {
                firstFix = true
                // First GPS fix: do an immediate full check
                LocationBasedSiteDetector.forceRefresh(lat, lng)
                HeritageRepository.fetchAll(lat, lng)
            } else {
                // Subsequent ticks: throttled check
                LocationBasedSiteDetector.onLocationUpdate(lat, lng)
            }

            // Move map to user location
            val ll = LatLng(lat, lng)
            mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 14.0))
            if (!userMarkerAdded) {
                mapLibreMap?.addMarker(MarkerOptions().position(ll).title("You are here"))
                userMarkerAdded = true
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            locationManager.stopUpdates()
        }
    }

    DisposableEffect(showWelcome) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (showWelcome) {
                mapView.setRenderEffect(
                    RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP)
                )
            } else {
                mapView.setRenderEffect(null)
            }
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mapView.setRenderEffect(null)
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────
    Box(Modifier.fillMaxSize()) {

        // Map
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { mv ->
            mv.getMapAsync { map ->
                mapLibreMap = map
                map.setStyle(MAP_STYLE) {
                    // Drop a pin for every known site
                    nearbySites.forEach { site ->
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(site.latitude, site.longitude))
                                .title(site.name)
                                .snippet(site.id.toString())
                        )
                    }
                    // Tap a pin → show bottom sheet
                    map.setOnMarkerClickListener { marker ->
                        val s = nearbySites.find { it.id.toString() == marker.snippet }
                        if (s != null) {
                            tappedSite = HeritageSite(s.id.toString(), s.name, s.latitude, s.longitude, 300.0)
                        }
                        s != null
                    }
                    // Jump to user location if already known
                    userLat?.let { lat -> userLng?.let { lng ->
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 14.0))
                    }}
                }
            }
        }

        if (!showWelcome) {
            // Top status bar
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Status pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(tokens.surface)
                            .border(0.7.dp, tokens.border, RoundedCornerShape(50))
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotPulse by rememberInfiniteTransition(label = "dp").animateFloat(
                                0.4f, 1f,
                                infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
                                label = "d"
                            )
                            Box(
                                Modifier.size(8.dp).scale(dotPulse).clip(CircleShape)
                                    .background(
                                        when {
                                            insideSite != null -> Color(0xFF4ADE80)   // green = inside
                                            nearbySites.isEmpty() -> accent.primary     // accent = scanning
                                            else -> Color(0xFF60A5FA)                 // blue = nearby found but outside
                                        }
                                    )
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                text = when {
                                    insideSite != null  -> "You are at ${insideSite.name}"
                                    nearbySites.isEmpty() -> if (userLat == null) "Acquiring GPS…" else "Fetching sites…"
                                    else                -> "Nearby sites found"
                                },
                                color = tokens.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // Profile button
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape)
                            .background(tokens.surfaceMuted)
                            .border(0.7.dp, tokens.borderStrong, CircleShape)
                            .clickable { onNavigateToProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = accent.primary, modifier = Modifier.size(22.dp))
                    }
                }

                // QR scan button — only when inside a site
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

            // Bottom panel:
            //   • insideSite != null → show "You are at X" with Explore + Maps buttons
            //   • insideSite == null → show sorted nearby sites list
            BottomGlassPanel(
                insideSite           = insideSite,
                sortedSites          = sortedSites,
                context              = context,
                onNavigateToDetail   = onNavigateToDetail,
                onNavigateToSiteInfo = onNavigateToSiteInfo,
                onRefresh            = {
                    userLat?.let { lat ->
                        userLng?.let { lng ->
                            LocationBasedSiteDetector.forceRefresh(lat, lng)
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Tapped marker bottom sheet
        tappedSite?.let { site ->
            SiteMarkerBottomSheet(
                site      = site,
                onDismiss = { tappedSite = null },
                onInfo    = {
                    tappedSite = null
                    onNavigateToSiteInfo(site.id.toIntOrNull() ?: 0, site.name)
                }
            )
        }

        // Exit dialog
        if (showExitDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showExitDialog = false }) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(tokens.surface)
                        .border(1.dp, tokens.border, RoundedCornerShape(24.dp))
                        .padding(28.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚪", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Exit Dharohar Setu?", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Are you sure you want to exit?", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                    .background(GlassWhite15).border(0.7.dp, GlassBorder, RoundedCornerShape(14.dp))
                                    .clickable { showExitDialog = false }.padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Stay", color = TextSecondary, fontSize = 15.sp) }
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x15FF5252)).border(0.7.dp, Color(0x55FF5252), RoundedCornerShape(14.dp))
                                    .clickable { (context as? android.app.Activity)?.finishAffinity() }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Exit", color = Color(0xFFFF6B6B), fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        if (showWelcome) {
            val first = currentAuthUser!!.displayName?.trim()?.substringBefore(" ")?.takeIf { it.isNotEmpty() }
                ?: currentAuthUser!!.email?.substringBefore("@")?.takeIf { it.isNotEmpty() }
                ?: "Explorer"
            WelcomeOverlay(
                firstName = first,
                onDismiss = { welcomeDismissed = true },
                mapBlurred = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR Scan button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrScanButton(onClick: () -> Unit) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    val inf      = rememberInfiniteTransition(label = "qb")
    val shimmer  by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "sh")
    val edgeGlow by inf.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse), label = "eg")
    val pulse    by inf.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse), label = "ps")

    Box(
        modifier = Modifier.fillMaxWidth().scale(pulse)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.dark.copy(alpha = 0.12f), accent.tint, tokens.surfaceMuted)
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(colorStops = arrayOf(
                    (shimmer + 0.0f).rem(1f) to accent.primary.copy(alpha = edgeGlow),
                    (shimmer + 0.3f).rem(1f) to accent.dark.copy(alpha = edgeGlow * 0.6f),
                    (shimmer + 0.6f).rem(1f) to accent.primary.copy(alpha = edgeGlow)
                )),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(accent.primary.copy(0.3f), accent.tint.copy(0.35f))))
                    .border(0.8.dp, accent.primary.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.QrCodeScanner, null, tint = accent.primary, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Scan Node QR", color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Unlock the AI guide for this spot", color = accent.primary.copy(alpha = 0.75f), fontSize = 11.sp)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(accent.primary.copy(alpha = 0.18f))
                    .border(0.5.dp, accent.primary.copy(0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) { Text("›", color = accent.primary, fontSize = 18.sp, fontWeight = FontWeight.Light) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom glass panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomGlassPanel(
    insideSite:           HeritageSite?,
    sortedSites:          List<Pair<HeritageSite, Double>>,
    context:              android.content.Context,
    onNavigateToDetail:   (String, String) -> Unit,
    onNavigateToSiteInfo: (Int, String) -> Unit = { _, _ -> },
    onRefresh:            () -> Unit = {},
    modifier:             Modifier = Modifier
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    var showNearby   by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope         = rememberCoroutineScope()
    LaunchedEffect(insideSite) { showNearby = false }

    Column(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(tokens.surface.copy(alpha = 0.97f), tokens.surfaceMuted.copy(alpha = 0.99f))
                )
            )
            .border(
                0.7.dp,
                Brush.verticalGradient(listOf(tokens.border, Color.Transparent)),
                RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            )
    ) {
        Box(
            Modifier.align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(50)).background(tokens.divider)
        )

        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            if (insideSite != null) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        SectionLabel("You are here")
                        Spacer(Modifier.height(4.dp))
                        Text(insideSite.name, color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(tokens.surfaceMuted)
                            .border(0.5.dp, tokens.border, RoundedCornerShape(50))
                    ) {
                        Row {
                            listOf("Site" to false, "Nearby" to true).forEach { (label, target) ->
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(if (showNearby == target) tokens.surface else Color.Transparent)
                                        .clickable { showNearby = target }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) { Text(label, color = tokens.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
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
                                    .background(Brush.linearGradient(listOf(accent.primary, accent.dark)))
                                    .clickable { onNavigateToDetail(insideSite.name, insideSite.id) }
                                    .padding(vertical = 22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏛️", fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Explore ${insideSite.name}", color = accent.onAccent, fontSize = 17.sp, fontWeight = FontWeight.Black)
                                        Text("History • AI Guide • Voice • Video", color = accent.onAccent.copy(alpha = 0.85f), fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(tokens.surfaceMuted).border(0.7.dp, tokens.border, RoundedCornerShape(16.dp))
                                    .clickable {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(
                                                "geo:${insideSite.latitude},${insideSite.longitude}?q=${insideSite.latitude},${insideSite.longitude}(${insideSite.name})"
                                            ))
                                        )
                                    }.padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Open in Google Maps", color = tokens.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sortedSites.filter { (s, _) -> s.id != insideSite.id }) { (site, dist) ->
                                SiteDistanceRow(site, dist, context, onNavigateToSiteInfo)
                            }
                        }
                    }
                }
            } else {
                // ── Header row with refresh button ────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Nearby Heritage Sites", color = tokens.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (sortedSites.isEmpty()) "Acquiring GPS signal…" else "Sorted by distance",
                            color = tokens.textTertiary, fontSize = 13.sp
                        )
                    }

                    val spinAngle by rememberInfiniteTransition(label = "refresh").animateFloat(
                        0f, 360f,
                        infiniteRepeatable(tween(800, easing = LinearEasing)),
                        label = "spin"
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(tokens.surfaceMuted)
                            .border(0.7.dp, tokens.border, CircleShape)
                            .clickable(enabled = !isRefreshing) {
                                isRefreshing = true
                                onRefresh()
                                scope.launch {
                                    kotlinx.coroutines.delay(1500)
                                    isRefreshing = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "↻",
                            color    = if (isRefreshing) accent.primary else tokens.textSecondary,
                            fontSize = 20.sp,
                            modifier = if (isRefreshing) Modifier.rotate(spinAngle) else Modifier
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (sortedSites.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val spin by rememberInfiniteTransition(label = "sp").animateFloat(
                            0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "r"
                        )
                        Text("◌", color = accent.primary, fontSize = 20.sp, modifier = Modifier.rotate(spin))
                        Spacer(Modifier.width(12.dp))
                        Text("Searching for your location…", color = tokens.textSecondary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedSites) { (site, dist) ->
                            SiteDistanceRow(site, dist, context, onNavigateToSiteInfo)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Site marker bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteMarkerBottomSheet(
    site:      HeritageSite,
    onDismiss: () -> Unit,
    onInfo:    () -> Unit
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    var weather        by remember { mutableStateOf<WeatherService.WeatherResult?>(null) }
    var weatherLoading by remember { mutableStateOf(true) }

    LaunchedEffect(site) {
        weatherLoading = true
        weather = kotlinx.coroutines.withContext(Dispatchers.IO) {
            WeatherService.fetchWeather(site.latitude, site.longitude)
        }
        weatherLoading = false
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = tokens.surface,
        contentColor     = tokens.textPrimary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)
        ) {
            Text(site.name, color = tokens.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (weatherLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent.primary, modifier = Modifier.size(24.dp))
                }
            } else if (weather != null) {
                val w = weather!!
                Text("${w.tempC.toInt()}°C • ${w.description}", color = tokens.textSecondary, fontSize = 14.sp)
                val suggestions = WeatherService.weatherSuggestions(w.tempC, w.weatherCode)
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(suggestions.joinToString(" • "), color = accent.primary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(tokens.surfaceMuted).border(0.5.dp, tokens.border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Close", color = tokens.textSecondary, fontSize = 15.sp) }
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(accent.primary, accent.dark)))
                        .clickable(onClick = onInfo).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = accent.onAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Info", color = accent.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Site distance row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SiteDistanceRow(
    site:                 HeritageSite,
    dist:                 Double,
    context:              android.content.Context,
    onNavigateToSiteInfo: (Int, String) -> Unit
) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 16.dp, tint = tokens.surfaceMuted) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(site.name, color = tokens.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (dist >= 1000) "${"%.1f".format(dist / 1000)} km away" else "${dist.toInt()} m away",
                    color = tokens.textTertiary, fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(tokens.surface).border(0.5.dp, tokens.border, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToSiteInfo(site.id.toIntOrNull() ?: 0, site.name) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = accent.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Info", color = accent.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .background(tokens.surface).border(0.5.dp, tokens.border, RoundedCornerShape(12.dp))
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("geo:${site.latitude},${site.longitude}"))
                            )
                        }.padding(horizontal = 14.dp, vertical = 8.dp)
                ) { Text("Maps", color = accent.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun WelcomeOverlay(firstName: String, onDismiss: () -> Unit, mapBlurred: Boolean) {
    val accent = LocalAccent.current
    val tokens = LocalAppColors.current
    val sweep = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        sweep.animateTo(1f, tween(1400, easing = EaseOutCubic))
    }
    LaunchedEffect(Unit) {
        delay(4000)
        onDismiss()
    }

    val backdropBrush = remember(accent, tokens, mapBlurred) {
        if (mapBlurred) {
            Brush.verticalGradient(
                colors = listOf(
                    accent.primary.copy(alpha = 0.20f),
                    accent.tint.copy(alpha = 0.28f),
                    tokens.surface.copy(alpha = 0.42f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    accent.primary.copy(alpha = 0.26f),
                    accent.tint.copy(alpha = 0.38f),
                    tokens.scrim.copy(alpha = 0.72f)
                )
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(backdropBrush)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * 0.42f)
            val maxR = size.maxDimension * 0.55f * sweep.value
            if (maxR > 1f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.primary.copy(alpha = 0.38f * sweep.value),
                            accent.tint.copy(alpha = 0.15f * sweep.value),
                            Color.Transparent
                        ),
                        center = c,
                        radius = maxR
                    ),
                    radius = maxR,
                    center = c
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val prefix = "Welcome, "
                prefix.forEachIndexed { i, ch ->
                    key("wp$i") {
                        val a = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(i * 40L)
                            a.animateTo(1f, tween(320))
                        }
                        Text(
                            ch.toString(),
                            modifier = Modifier.graphicsLayer { alpha = a.value },
                            color = tokens.textPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                firstName.forEachIndexed { i, ch ->
                    key("nm$i") {
                        val a = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(220 + i * 55L)
                            a.animateTo(1f, tween(360, easing = EaseOutCubic))
                        }
                        Text(
                            ch.toString(),
                            modifier = Modifier.graphicsLayer { alpha = a.value },
                            color = accent.primary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            val tag = "Your trip starts now"
            Row(horizontalArrangement = Arrangement.Center) {
                tag.forEachIndexed { i, ch ->
                    key("tg$i") {
                        val a = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            delay(380 + i * 28L)
                            a.animateTo(1f, tween(280))
                        }
                        Text(
                            if (ch == ' ') "\u00A0" else ch.toString(),
                            modifier = Modifier.graphicsLayer { alpha = a.value },
                            color = tokens.textSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            GlassPrimaryButton(
                text = "Begin exploring",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.88f)
            )
        }
    }
}