package com.nikosdays.metaldetector.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonGreen = Color(0xFF00E676)
val NeonCyan = Color(0xFF00E5FF)
val NeonYellow = Color(0xFFFFEA00)
val NeonOrange = Color(0xFFFF9100)
val NeonRed = Color(0xFFFF1744)

val DarkBackground = Color(0xFF0A0E17)
val DarkSurface = Color(0xFF131A29)
val DarkCard = Color(0xFF1B2438)
val DarkBorder = Color(0xFF2B3A5A)

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = NeonCyan,
    tertiary = NeonYellow,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MetalDetectorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
