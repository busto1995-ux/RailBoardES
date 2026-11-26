package com.railboard.es.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BoardBackground = Color(0xFF0B1117)
val BoardSurface = Color(0xFF131C26)
val BoardSurfaceVariant = Color(0xFF1D2A38)
val PlatformAccent = Color(0xFFFFC107)
val OnTimeGreen = Color(0xFF2ED573)
val DelayedAmber = Color(0xFFFFA502)
val CancelledRed = Color(0xFFFF4757)
val BoardingCyan = Color(0xFF1E90FF)

private val RailDarkColors = darkColorScheme(
    primary = PlatformAccent,
    onPrimary = Color(0xFF111111),
    secondary = BoardingCyan,
    onSecondary = Color.White,
    background = BoardBackground,
    onBackground = Color(0xFFF3F6FA),
    surface = BoardSurface,
    onSurface = Color(0xFFF3F6FA),
    surfaceVariant = BoardSurfaceVariant,
    onSurfaceVariant = Color(0xFFB4C2D0),
    error = CancelledRed,
    onError = Color.White
)

@Composable
fun RailBoardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RailDarkColors,
        content = content
    )
}
