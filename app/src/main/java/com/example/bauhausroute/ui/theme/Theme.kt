package com.example.bauhausroute.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BauhausLightColorScheme = lightColorScheme(
    primary = ExpressiveCoral,
    secondary = BauhausBlue,
    tertiary = ExpressiveAmber,
    background = ExpressiveWarmBackground,
    surface = ExpressiveSurface,
    surfaceVariant = ExpressivePeach,
    onPrimary = ExpressiveInk,
    onSecondary = Color.White,
    onTertiary = ExpressiveInk,
    onBackground = ExpressiveInk,
    onSurface = ExpressiveInk,
    onSurfaceVariant = ExpressiveInk
)

private val BauhausDarkColorScheme = darkColorScheme(
    primary = ExpressiveCoral,
    secondary = Color(0xFF6ED6B0),
    tertiary = ExpressiveAmber,
    background = ExpressiveDarkBackground,
    surface = ExpressiveDarkSurface,
    surfaceVariant = ExpressiveDarkSurfaceVariant,
    onPrimary = ExpressiveInk,
    onSecondary = ExpressiveDarkBackground,
    onTertiary = ExpressiveInk,
    onBackground = ExpressiveDarkInk,
    onSurface = ExpressiveDarkInk,
    onSurfaceVariant = ExpressiveDarkMuted,
    outline = Color(0xFF4B6355),
    outlineVariant = Color(0xFF2E4437)
)

@Composable
fun BauhausTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BauhausDarkColorScheme else BauhausLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun BauhausRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BauhausTheme(darkTheme = darkTheme, content = content)
}
