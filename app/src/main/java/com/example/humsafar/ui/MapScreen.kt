package com.example.humsafar.ui

import android.Manifest
import android.os.Bundle
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.humsafar.data.MonumentRepository
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(userLocation: LatLng?) {

    AndroidView(factory = { context ->

        MapLibre.getInstance(context)

        val mapView = MapView(context)
        mapView.onCreate(Bundle())

        mapView.getMapAsync { mapboxMap ->

            mapboxMap.setStyle(
                Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")
            ) {

                val center = userLocation ?: LatLng(28.5917, 77.0888)

                mapboxMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(center, 12.0)
                )

                // Add all monuments
                MonumentRepository.monuments.forEach { monument ->
                    mapboxMap.addMarker(
                        MarkerOptions()
                            .position(
                                LatLng(monument.latitude, monument.longitude)
                            )
                            .title(monument.name)
                    )
                }

                // Add user marker
                userLocation?.let {
                    mapboxMap.addMarker(
                        MarkerOptions()
                            .position(it)
                            .title("You are here")
                    )
                }
            }
        }

        mapView
    })
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
