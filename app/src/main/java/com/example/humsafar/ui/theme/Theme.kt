package com.example.humsafar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NavyBlue,
    secondary = AccentYellow,
    background = LightYellow,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black
)

@Composable
fun HumsafarTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
