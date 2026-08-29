package com.autopilot.driver.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF78E6D0),
    secondary = Color(0xFFFFC86B),
    tertiary = Color(0xFFFFA9B2),
    background = Color(0xFF081013),
    surface = Color(0xFF102126),
    onPrimary = Color(0xFF0B161A),
    onSecondary = Color(0xFF0B161A),
    onTertiary = Color(0xFF0B161A),
    onBackground = Color(0xFFE0EEF0),
    onSurface = Color(0xFFE0EEF0),
)

@Composable
fun AalamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
