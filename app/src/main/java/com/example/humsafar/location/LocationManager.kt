package com.example.humsafar.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*

class HumsafarLocationManager(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        3000L // update every 3 seconds
    ).apply {
        setMinUpdateIntervalMillis(1500L)
        setWaitForAccurateLocation(false)
    }.build()

    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startUpdates(onLocation: (lat: Double, lng: Double) -> Unit) {
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    onLocation(loc.latitude, loc.longitude)
                }
            }
        }
        fusedClient.requestLocationUpdates(
            locationRequest,
            callback!!,
            Looper.getMainLooper()
        )
    }

    fun stopUpdates() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        callback = null
    }
}