package com.example.cropdrone.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenPrimaryDark,
    secondary = AmberAccent,
    onSecondary = OnSurfaceLight,
    secondaryContainer = AmberDark,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = GreenMuted,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OnSurfaceVariantLight,
    error = StatusError,
    onError = SurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = BackgroundDark,
    primaryContainer = GreenPrimaryDark,
    onPrimaryContainer = GreenPale,
    secondary = AmberAccent,
    onSecondary = BackgroundDark,
    secondaryContainer = AmberDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OnSurfaceVariantDark,
    error = StatusError,
    onError = BackgroundDark,
)

@Composable
fun CropDroneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CropDroneTypography,
        content = content,
    )
}
