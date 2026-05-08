package com.example.humsafar.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Legacy colour names — preserved verbatim so every screen still compiles, but
// repointed to light-theme equivalents. New code should prefer reading from
// `LocalAccent.current` and `LocalAppColors.current`; these constants remain
// for screens that have not yet been migrated.
// ─────────────────────────────────────────────────────────────────────────────

// "Navy / dark base" tokens are now reused as DARK FOREGROUND (text/headlines)
// because in a light theme there's no dark background — anything that used
// these as a background should be migrated to LightTokens.bgWarm explicitly.
val DeepNavy        = Color(0xFF0E1014)   // near-black, primary text
val MidnightBlue    = Color(0xFF161A22)
val RichNavy        = Color(0xFF1F2533)
val NavyBlue        = Color(0xFF24304D)   // strong slate (titles)
val DarkNavy        = Color(0xFF0E1014)

// Legacy gold accent — kept as static fallback so screens not yet migrated to
// `LocalAccent` still render. The default user accent ("Heritage Gold") in
// AccentPalette.kt mirrors these values intentionally.
val AccentYellow    = Color(0xFFE6A609)
val GoldGlow        = Color(0xFFC9890A)
val WarmGold        = Color(0xFFB37F00)
val LightYellow     = Color(0xFFFFF3C4)

// "Glass" tints — historically translucent white over a dark background.
// In a light theme we flip them to translucent dark over a cream surface,
// which gives the equivalent "subtle layered glass" feel on light surfaces.
val GlassWhite10    = Color(0x0A0E1014)   // 4% slate
val GlassWhite15    = Color(0x140E1014)   // 8%
val GlassWhite20    = Color(0x1F0E1014)   // 12%
val GlassWhite30    = Color(0x290E1014)   // 16%
val GlassWhite60    = Color(0xCCFFFFFF)   // soft frosted white panel
val GlassBorder     = Color(0x1F0E1014)   // hairline slate border
val GlassBorderBright = Color(0x330E1014)

// Text — flipped from white-on-dark to dark-on-light.
val TextPrimary     = Color(0xFF0E1014)
val TextSecondary   = Color(0xFF4A4A52)
val TextTertiary    = Color(0xFF7C7C85)
val TextDark        = Color(0xFF0A1628)

// Specular highlights are now soft warm washes rather than bright white, so
// they blend on cream backgrounds without looking blown out.
val Specular        = Color(0x14E6A609)
val SpecularBright  = Color(0x33E6A609)
