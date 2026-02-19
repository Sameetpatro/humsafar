package com.example.humsafar.utils

import kotlin.math.*

fun haversineDistance(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val R = 6371000.0 // Earth radius in meters
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dPhi = Math.toRadians(lat2 - lat1)
    val dLambda = Math.toRadians(lon2 - lon1)

    val a = sin(dPhi / 2).pow(2) +
            cos(phi1) * cos(phi2) *
            sin(dLambda / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}