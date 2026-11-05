package com.lumora.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// AMOLED True-Black ColorScheme
private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = PureBlack,
    primaryContainer = AccentGoldSubtle,
    onPrimaryContainer = AccentGold,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = PureBlack,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

val LumoraShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun LumoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Defaulting to true-black AMOLED theme
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = LumoraShapes,
        content = content
    )
}
