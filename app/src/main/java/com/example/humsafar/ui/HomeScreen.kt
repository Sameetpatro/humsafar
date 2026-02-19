package com.example.humsafar.ui

import android.Manifest
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.geometry.LatLng
import com.example.humsafar.data.HeritageRepository
import com.example.humsafar.utils.haversineDistance
import com.example.humsafar.models.HeritageSite

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
    var currentSite by remember { mutableStateOf<HeritageSite?>(null) }

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

                    // Check heritage sites
                    HeritageRepository.sites.forEach { site ->
                        val distance = haversineDistance(
                            lat, lng,
                            site.latitude,
                            site.longitude
                        )
                        if (distance < site.radius) {
                            currentSite = site
                        }
                    }
                }
            }
        }

        if (currentSite != null) {
            InsideMonumentScreen(currentSite!!.name)
        } else {
            Column(Modifier.fillMaxSize()) {
                MapScreen()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Searching for nearby heritage site...")
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

@Composable
fun InsideMonumentScreen(monumentName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Welcome to $monumentName",
            fontSize = 22.sp
        )
    }
}