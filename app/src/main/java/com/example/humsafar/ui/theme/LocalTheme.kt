package com.example.humsafar.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The user-selected accent, propagated to every composable via [HumsafarTheme].
 *
 * Reads inside composables: `LocalAccent.current.primary` etc. We use
 * [compositionLocalOf] (not static) because the accent legitimately changes at
 * runtime when the user picks a different swatch, and we want the whole tree to
 * recompose when that happens.
 */
val LocalAccent = compositionLocalOf { DefaultAccent }

/**
 * The base light-theme tokens (cream, white, slate text, etc.). These don't
 * change per user today, so a [staticCompositionLocalOf] is fine — but using a
 * regular [compositionLocalOf] keeps the door open for a future light/dark
 * toggle without breaking call sites.
 */
val LocalAppColors = staticCompositionLocalOf { DefaultLightTokens }
