package com.example.bauhausroute.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BauhausColorScheme = lightColorScheme(
    primary = BauhausRed,
    secondary = BauhausBlue,
    tertiary = BauhausYellow,
    background = BauhausWarmWhite,
    surface = BauhausWarmWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = BauhausCarbonBlack,
    onBackground = BauhausCarbonBlack,
    onSurface = BauhausCarbonBlack
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
