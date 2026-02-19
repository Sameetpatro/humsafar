package com.example.humsafar.models

data class HeritageSite(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double // in meters
)