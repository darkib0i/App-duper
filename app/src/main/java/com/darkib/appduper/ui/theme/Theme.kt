package com.darkib.appduper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Neon-on-deep-space palette
val SpaceBlack = Color(0xFF0B0620)
val SpaceDeep = Color(0xFF120B33)
val ElectricViolet = Color(0xFF7C4DFF)
val NeonCyan = Color(0xFF00E5FF)
val HotMagenta = Color(0xFFFF2E93)
val MintGlow = Color(0xFF69F0AE)
val Starlight = Color(0xFFEDEAFF)
val Dimmed = Color(0xFF9C93C9)
val CardSurface = Color(0xFF1A1240)
val CardSurfaceHi = Color(0xFF251A57)

private val DuperColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    secondary = NeonCyan,
    onSecondary = SpaceBlack,
    tertiary = HotMagenta,
    background = SpaceBlack,
    onBackground = Starlight,
    surface = CardSurface,
    onSurface = Starlight,
    surfaceVariant = CardSurfaceHi,
    onSurfaceVariant = Dimmed,
    error = Color(0xFFFF6E6E),
)

@Composable
fun AppDuperTheme(content: @Composable () -> Unit) {
    // The app is deliberately dark & neon in both system themes.
    MaterialTheme(
        colorScheme = DuperColorScheme,
        content = content,
    )
}
