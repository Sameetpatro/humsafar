// app/src/main/java/com/example/humsafar/data/HeritageRepository.kt
// UPDATED — removed dead SiteClient.api.getAllMonuments() call.
// MapScreen uses the hardcoded list. When you want live data, call
// HumsafarClient.api.getNearbySites(lat, lng) from the ViewModel directly.

package com.example.humsafar.data

import com.example.humsafar.models.HeritageSite

object HeritageRepository {

    /**
     * Hardcoded site list used by MapScreen markers + geofencing.
     * IDs must match your seeded PostgreSQL site IDs so that QR scans
     * can resolve correctly. Update these after seeding new sites.
     *
     * Current seeded sites (from your session):
     *   id=1  — placeholder (if any)
     *   id=2  — IIIT SONEPAT
     */
    val sites: List<HeritageSite> = listOf(
        HeritageSite("2",  "IIIT Sonepat",      28.989596, 77.151120, 285.0),
        HeritageSite("3",  "Red Fort",           28.6562,   77.2410,   400.0),
        HeritageSite("4",  "Taj Mahal",          27.1751,   78.0421,   500.0),
        HeritageSite("5",  "India Gate",         28.6129,   77.2295,   350.0),
        HeritageSite("6",  "Qutub Minar",        28.5244,   77.1855,   350.0),
        HeritageSite("7",  "Konark Sun Temple",  19.8876,   86.0945,   450.0),
        HeritageSite("8",  "Gateway of India",   18.9218,   72.8347,   300.0),
        HeritageSite("9",  "Hampi",              15.3350,   76.4600,   800.0),
        HeritageSite("10", "Golden Temple",      31.6200,   74.8765,   400.0),
        HeritageSite("11", "Mysore Palace",      12.3052,   76.6552,   400.0)
    )
}