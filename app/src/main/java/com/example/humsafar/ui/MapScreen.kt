// app/src/main/java/com/example/humsafar/ui/MapScreen.kt
// UPDATED — single "Explore" CTA, profile button in top status pill area

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
    onNavigateToVoice:   (siteName: String, siteId: String) -> Unit = { _, _ -> },
    onNavigateToDetail:  (siteName: String, siteId: String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {},
    onNavigateToQrScan:  (Long) -> Unit = {}
) {
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { locationPermission.launchPermissionRequest() }

    when {
        locationPermission.status.isGranted -> MapContent(
            onNavigateToVoice   = onNavigateToVoice,
            onNavigateToDetail  = onNavigateToDetail,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToQrScan  = onNavigateToQrScan
        )
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
    onNavigateToVoice:   (String, String) -> Unit = { _, _ -> },
    onNavigateToDetail:  (String, String) -> Unit = { _, _ -> },
    onNavigateToProfile: () -> Unit = {},
    onNavigateToQrScan:  (Long) -> Unit = {}
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

        // ── Top bar row: status pill + profile button ─────────────────────
        Row(
            modifier              = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status pill (shrunk weight)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(Color(0xCC050D1A), Color(0xBB0A1628))))
                    .border(0.7.dp, GlassBorder, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                        0.4f, 1f,
                        infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse), label = "p"
                    )
                    Box(Modifier.size(8.dp).scale(pulse).clip(CircleShape)
                        .background(if (insideSite != null) Color(0xFF4ADE80) else AccentYellow))
                    Spacer(Modifier.height(10.dp))

// Scan QR bubble
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x22FFD54F))
                            .border(1.dp, Color(0x55FFD54F), RoundedCornerShape(16.dp))
                            .clickable {
                                insideSite?.id?.toLong()?.let { onNavigateToQrScan(it) }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📷", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text("Scan Node QR", color = AccentYellow, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (insideSite != null) "Inside ${insideSite!!.name}" else "Scanning for heritage sites…",
                        color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Profile button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xCC050D1A), Color(0xBB0A1628))))
                    .border(0.7.dp, GlassBorderBright, CircleShape)
                    .clickable { onNavigateToProfile() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = AccentYellow, modifier = Modifier.size(22.dp))
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
private fun BottomGlassPanel(
    insideSite:        HeritageSite?,
    sortedSites:       List<Pair<HeritageSite, Double>>,
    context:           android.content.Context,
    onNavigateToDetail: (String, String) -> Unit,
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

                            // ── Single primary CTA: Explore This Site ─────
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFC107))))
                                    .border(0.7.dp, Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x11FFFFFF))), RoundedCornerShape(20.dp))
                                    .clickable { onNavigateToDetail(insideSite.name, insideSite.id) }
                                    .padding(vertical = 22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.matchParentSize().clip(RoundedCornerShape(20.dp))
                                    .background(Brush.verticalGradient(listOf(Color(0x44FFFFFF), Color.Transparent))))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🏛️", fontSize = 24.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Explore ${insideSite.name}",
                                            color      = DeepNavy,
                                            fontSize   = 17.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            "History • AI Guide • Voice • Video",
                                            color    = Color(0x99050D1A),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // ── Secondary: Open in Maps ───────────────────
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
                        // Nearby sites list
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
                // No site — sorted list
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