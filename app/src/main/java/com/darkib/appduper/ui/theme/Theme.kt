package com.darkib.appduper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Strict black / grey / white palette.
val SpaceBlack = Color(0xFF060607)   // near-pure black background
val SpaceDeep = Color(0xFF101013)    // slightly lifted black for depth
val ElectricViolet = Color(0xFFFFFFFF) // primary accent -> white
val NeonCyan = Color(0xFFC7C7CD)     // light grey
val HotMagenta = Color(0xFF8A8A90)   // mid grey
val MintGlow = Color(0xFFFFFFFF)     // success -> white
val Starlight = Color(0xFFF3F3F5)    // primary text
val Dimmed = Color(0xFF7C7C84)       // secondary text
val CardSurface = Color(0xFF161619)  // card fill
val CardSurfaceHi = Color(0xFF212126) // raised card fill

private val DuperColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF060607),
    secondary = NeonCyan,
    onSecondary = SpaceBlack,
    tertiary = HotMagenta,
    background = SpaceBlack,
    onBackground = Starlight,
    surface = CardSurface,
    onSurface = Starlight,
    surfaceVariant = CardSurfaceHi,
    onSurfaceVariant = Dimmed,
    error = Color(0xFFE6E6EA),
)

@Composable
fun AppDuperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DuperColorScheme,
        content = content,
    )
}
