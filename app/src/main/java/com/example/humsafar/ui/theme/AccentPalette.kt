package com.example.humsafar.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * A user-selectable accent. Every accent ships with:
 *  - [primary]  the brand colour used for CTAs, highlights, focused states.
 *  - [dark]     a deeper shade used for gradients, pressed states, headlines.
 *  - [tint]     a very pale wash used for backgrounds, chips, hovered surfaces.
 *  - [onAccent] the foreground colour to use when text/icons sit on top of [primary].
 */
data class Accent(
    val name: String,
    val primary: Color,
    val dark: Color,
    val tint: Color,
    val onAccent: Color
)

val Accents: List<Accent> = listOf(
    Accent(
        name = "Heritage Gold",
        primary = Color(0xFFE6A609),
        dark = Color(0xFFB37F00),
        tint = Color(0xFFFFF3C4),
        onAccent = Color(0xFF1A1300)
    ),
    Accent(
        name = "Saffron",
        primary = Color(0xFFFF7A1A),
        dark = Color(0xFFC25500),
        tint = Color(0xFFFFE4D0),
        onAccent = Color(0xFF1A0A00)
    ),
    Accent(
        name = "Crimson",
        primary = Color(0xFFE53935),
        dark = Color(0xFFAA1F1C),
        tint = Color(0xFFFFE0DE),
        onAccent = Color.White
    ),
    Accent(
        name = "Rose",
        primary = Color(0xFFEC4899),
        dark = Color(0xFFB02E73),
        tint = Color(0xFFFCE4EF),
        onAccent = Color.White
    ),
    Accent(
        name = "Royal Purple",
        primary = Color(0xFF7C3AED),
        dark = Color(0xFF5B21B6),
        tint = Color(0xFFEDE4FE),
        onAccent = Color.White
    ),
    Accent(
        name = "Cobalt",
        primary = Color(0xFF2563EB),
        dark = Color(0xFF1640A8),
        tint = Color(0xFFDDE7FB),
        onAccent = Color.White
    ),
    Accent(
        name = "Teal",
        primary = Color(0xFF0EA5A4),
        dark = Color(0xFF086B6A),
        tint = Color(0xFFD3F0EF),
        onAccent = Color.White
    ),
    Accent(
        name = "Forest",
        primary = Color(0xFF16A34A),
        dark = Color(0xFF0E6B31),
        tint = Color(0xFFD7F1E0),
        onAccent = Color.White
    )
)

val DefaultAccent: Accent = Accents.first()

fun accentByName(name: String?): Accent =
    Accents.firstOrNull { it.name == name } ?: DefaultAccent
