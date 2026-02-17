package com.example.humsafar.data

import com.example.humsafar.models.Monument

object MonumentRepository {

    // Later this will come from backend API
    val monuments = listOf(
        Monument(
            id = "1",
            name = "IIIT Sonepat",
            latitude = 28.5917,   // replace with real
            longitude = 77.0888,  // replace with real
            radius = 300.0
        ),
        Monument(
            id = "2",
            name = "Demo Monument 2",
            latitude = 28.7000,
            longitude = 77.2000,
            radius = 300.0
        )
    )
}
