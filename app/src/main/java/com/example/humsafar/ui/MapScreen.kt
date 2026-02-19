package com.example.humsafar.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.humsafar.BuildConfig
import com.example.humsafar.data.HeritageRepository
import com.example.humsafar.location.HumsafarLocationManager
import com.example.humsafar.models.HeritageSite
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
fun MapScreen() {
    val locationPermission = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(Unit) {
        locationPermission.launchPermissionRequest()
    }

    when {
        locationPermission.status.isGranted -> {
            MapContent()
        }
        locationPermission.status.shouldShowRationale -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Location permission is needed to detect nearby heritage sites.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { locationPermission.launchPermissionRequest() }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
        else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Location permission denied. Please enable it in Settings.")
            }
        }
    }
}

@Composable
fun MapContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    var insideSite by remember { mutableStateOf<HeritageSite?>(null) }
    var sortedSites by remember { mutableStateOf<List<Pair<HeritageSite, Double>>>(emptyList()) }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var userMarkerAdded by remember { mutableStateOf(false) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    val locationManager = remember { HumsafarLocationManager(context) }

    LaunchedEffect(Unit) {
        locationManager.startUpdates { lat, lng ->
            userLat = lat
            userLng = lng

            var found: HeritageSite? = null
            val distances = HeritageRepository.sites.map { site ->
                val dist = haversineDistance(lat, lng, site.latitude, site.longitude)
                if (dist < site.radius && found == null) {
                    found = site
                }
                site to dist
            }.sortedBy { it.second }

            insideSite = found
            sortedSites = distances

            val userLatLng = LatLng(lat, lng)
            mapLibreMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(userLatLng, 14.0)
            )

            if (!userMarkerAdded) {
                mapLibreMap?.addMarker(
                    MarkerOptions()
                        .position(userLatLng)
                        .title("You are here")
                )
                userMarkerAdded = true
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
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
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onCreate(Bundle())

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            locationManager.stopUpdates()
        }
    }

    Column(Modifier.fillMaxSize()) {

        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
        ) { mv ->
            mv.getMapAsync { map ->
                mapLibreMap = map
                map.setStyle(MAP_STYLE) {
                    HeritageRepository.sites.forEach { site ->
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(site.latitude, site.longitude))
                                .title(site.name)
                                .snippet("Radius: ${site.radius.toInt()}m")
                        )
                    }

                    userLat?.let { lat ->
                        userLng?.let { lng ->
                            map.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 14.0)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .padding(12.dp)
        ) {
            if (insideSite != null) {
                InsideSitePanel(
                    site = insideSite!!,
                    sortedSites = sortedSites,
                    context = context
                )
            } else {
                NearbyPanel(sortedSites = sortedSites, context = context)
            }
        }
    }
}

@Composable
fun InsideSitePanel(
    site: HeritageSite,
    sortedSites: List<Pair<HeritageSite, Double>>,
    context: android.content.Context
) {

    var showNearby by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {

        // Header row with site name + toggle button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📍 ${site.name}",
                fontSize = 18.sp,
                color = Color(0xFF0A1F44),
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { showNearby = !showNearby }) {
                Text(
                    text = if (showNearby) "◀ Back" else "Nearby ▶",
                    fontSize = 13.sp,
                    color = Color(0xFF0A1F44)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (!showNearby) {
            // ── Current site info ──
            Text(
                text = "You are inside this heritage zone.",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    val uri = Uri.parse(
                        "geo:${site.latitude},${site.longitude}?q=${site.latitude},${site.longitude}(${site.name})"
                    )
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                }
            ) {
                Text("Open in Google Maps")
            }
        } else {
            val nearby = sortedSites.filter { (s, _) -> s.id != site.id }

            if (nearby.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No other sites found nearby.", color = Color.Gray)
                }
            } else {
                Text(
                    text = "Other Heritage Sites",
                    fontSize = 14.sp,
                    color = Color(0xFF0A1F44)
                )
                Spacer(Modifier.height(6.dp))
                LazyColumn {
                    items(nearby) { (nearbySite, distance) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(nearbySite.name, fontSize = 14.sp)
                                Text(
                                    text = formatDistance(distance),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            TextButton(onClick = {
                                val uri = Uri.parse(
                                    "geo:${nearbySite.latitude},${nearbySite.longitude}?q=${nearbySite.latitude},${nearbySite.longitude}(${nearbySite.name})"
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }) {
                                Text("Maps", fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun NearbyPanel(
    sortedSites: List<Pair<HeritageSite, Double>>,
    context: android.content.Context
) {
    if (sortedSites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Fetching your location...", color = Color.Gray)
        }
    } else {
        Text(
            text = "Nearby Heritage Sites",
            fontSize = 16.sp,
            color = Color(0xFF0A1F44)
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(sortedSites) { (site, distance) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(site.name, fontSize = 14.sp)
                        Text(
                            text = formatDistance(distance),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    TextButton(onClick = {
                        val uri = Uri.parse(
                            "geo:${site.latitude},${site.longitude}?q=${site.latitude},${site.longitude}(${site.name})"
                        )
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }) {
                        Text("Maps", fontSize = 12.sp)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        "${"%.1f".format(meters / 1000)} km away"
    } else {
        "${meters.toInt()} m away"
    }
}