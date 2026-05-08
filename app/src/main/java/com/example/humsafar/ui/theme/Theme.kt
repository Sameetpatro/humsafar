package com.example.humsafar.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun HumsafarTheme(
    accent: Accent = DefaultAccent,
    tokens: LightTokens = DefaultLightTokens,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary           = accent.primary,
        onPrimary         = accent.onAccent,
        primaryContainer  = accent.tint,
        onPrimaryContainer = accent.dark,
        secondary         = accent.dark,
        onSecondary       = Color.White,
        secondaryContainer = accent.tint,
        onSecondaryContainer = accent.dark,
        background        = tokens.bgWarm,
        onBackground      = tokens.textPrimary,
        surface           = tokens.surface,
        onSurface         = tokens.textPrimary,
        surfaceVariant    = tokens.surfaceMuted,
        onSurfaceVariant  = tokens.textSecondary,
        outline           = tokens.border,
        outlineVariant    = tokens.divider
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    CompositionLocalProvider(
        LocalAccent provides accent,
        LocalAppColors provides tokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
