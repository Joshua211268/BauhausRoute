package com.example.bauhausroute.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BauhausColorScheme = lightColorScheme(
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

@Composable
fun BauhausTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BauhausColorScheme,
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
    BauhausTheme(content = content)
}
