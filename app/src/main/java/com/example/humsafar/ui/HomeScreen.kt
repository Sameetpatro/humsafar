package com.example.humsafar.ui

import android.Manifest
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import com.example.humsafar.data.MonumentRepository
import com.example.humsafar.utils.calculateDistance

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen() {

    val permissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var currentMonument by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
    }

    if (permissionState.status.isGranted) {

        LaunchedEffect(Unit) {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                location?.let {
                    val lat = it.latitude
                    val lng = it.longitude
                    userLocation = LatLng(lat, lng)

                    // Check monuments
                    MonumentRepository.monuments.forEach { monument ->
                        val distance = calculateDistance(
                            lat, lng,
                            monument.latitude,
                            monument.longitude
                        )
                        if (distance < monument.radius) {
                            currentMonument = monument.name
                        }
                    }
                }
            }
        }

        if (currentMonument != null) {

            InsideMonumentScreen(currentMonument!!)

        } else {

            Column(Modifier.fillMaxSize()) {

                MapScreen(userLocation)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Searching for nearby monument...")
                }
            }
        }

    } else {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Location permission required")
        }
    }
}
