// app/src/main/java/com/example/humsafar/data/HeritageRepository.kt
// FIXED: IDs now match the seeded PostgreSQL database exactly.
//
// Previous bug: IIIT Sonepat was "2" here but id=1 in DB.
//               Qutub Minar was "6" here but id=2 in DB.
//               Taj Mahal was "4" here but id=3 in DB.
// This caused ChatbotActivity to send site_id=2 (Qutub Minar) when the
// user was actually at IIIT Sonepat (site_id=1), fetching the wrong prompt.

package com.example.humsafar.data

import com.example.humsafar.models.HeritageSite

object HeritageRepository {

    /**
     * IDs MUST match your seeded PostgreSQL heritage_sites table.
     * Check your DB with: SELECT id, name FROM heritage_sites ORDER BY id;
     * and update this list accordingly.
     *
     * Current seeded sites (from your DB screenshot):
     *   id=1  — IIIT Sonepat
     *   id=2  — Qutub Minar Complex
     *   id=3  — Taj Mahal
     */
    val sites: List<HeritageSite> = listOf(
        HeritageSite("1",  "IIIT Sonepat",         28.9896,   77.1511,  500.0),
        HeritageSite("2",  "Qutub Minar Complex",  28.5245,   77.1855,  600.0),
        HeritageSite("3",  "Taj Mahal",            27.1751,   78.0421,  900.0),

        // ── Add more sites below as you seed them in your DB ──────────────
        // HeritageSite("4",  "Red Fort",           28.6562,   77.2410,   400.0),
        // HeritageSite("5",  "India Gate",         28.6129,   77.2295,   350.0),
    )
}