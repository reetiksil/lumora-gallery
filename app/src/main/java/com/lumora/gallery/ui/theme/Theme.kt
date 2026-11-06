package com.lumora.gallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lumora.gallery.data.model.ThemeMode

// AMOLED True-Black ColorScheme (Default)
private val DarkColorScheme = darkColorScheme(
    primary = SoothingBlue,
    onPrimary = PureBlack,
    primaryContainer = DarkSurfaceHighlight,
    onPrimaryContainer = SoothingBlue,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = PureBlack,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

// HyperOS Clean Light ColorScheme
private val LightColorScheme = lightColorScheme(
    primary = SoothingBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = SoothingBlue,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFFE0E0E0)
)

val LumoraShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun LumoraTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED_DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.AMOLED_DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = LumoraShapes,
        content = content
    )
}
