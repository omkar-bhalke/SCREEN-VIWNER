package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AhiDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = GlassSurface,
    onPrimaryContainer = NeonCyan,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    tertiary = TertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

@Composable
fun AhiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AhiDarkColorScheme,
        typography = Typography,
        content = content
    )
}
