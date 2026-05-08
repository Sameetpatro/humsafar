package com.example.humsafar.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens that don't depend on the user's accent choice. These describe
 * the base "light parchment" surface set we paint everything else on top of.
 *
 * These are intentionally warm rather than pure white to keep the heritage feel
 * of the original UI alive while staying firmly in light-theme territory.
 */
data class LightTokens(
    val bgWarm: Color,
    val bgWarmDeep: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val shadow: Color,
    val scrim: Color
)

val DefaultLightTokens = LightTokens(
    bgWarm           = Color(0xFFFAF7EF),
    bgWarmDeep       = Color(0xFFF2ECDD),
    surface          = Color(0xFFFFFFFF),
    surfaceMuted     = Color(0xFFF6F3EC),
    surfaceElevated  = Color(0xFFFFFFFF),
    border           = Color(0xFFE5E1D8),
    borderStrong     = Color(0xFFCFCABF),
    divider          = Color(0xFFEDE9DF),
    textPrimary      = Color(0xFF0E1014),
    textSecondary    = Color(0xFF4A4A52),
    textTertiary     = Color(0xFF7C7C85),
    shadow           = Color(0x14000000),
    scrim            = Color(0x66000000)
)
