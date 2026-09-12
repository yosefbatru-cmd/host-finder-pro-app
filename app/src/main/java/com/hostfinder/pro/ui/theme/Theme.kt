package com.hostfinder.pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3B9EFF),
    onPrimary = Color.White,
    secondary = Color(0xFF6366F1),
    background = Color(0xFF070B10),
    surface = Color(0xFF111820),
    onBackground = Color(0xFFEEF3F9),
    onSurface = Color(0xFFEEF3F9),
    surfaceVariant = Color(0xFF18212C),
    onSurfaceVariant = Color(0xFF7A8A9E),
    error = Color(0xFFEF4444),
    outline = Color(0xFF243041),
)

@Composable
fun HostFinderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
