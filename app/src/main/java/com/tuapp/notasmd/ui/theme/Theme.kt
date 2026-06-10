package com.tuapp.notasmd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    background         = HudDayBackground,
    surface            = HudDaySurface,
    surfaceVariant     = HudDaySurfaceHigh,
    onBackground       = HudDayOnBg,
    onSurface          = HudDayOnSurface,
    onSurfaceVariant   = HudDayOnVariant,
    primary            = HudDayPrimary,
    onPrimary          = HudDayOnPrimary,
    primaryContainer   = HudDayContainer,
    onPrimaryContainer = HudDayOnContainer,
    error              = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    background         = JarvisBackground,
    surface            = JarvisSurface,
    surfaceVariant     = JarvisSurfaceHigh,
    onBackground       = JarvisText,
    onSurface          = JarvisText,
    onSurfaceVariant   = JarvisTextSec,
    primary            = JarvisCyan,
    onPrimary          = JarvisBackground,
    primaryContainer   = JarvisCyanDim,
    onPrimaryContainer = JarvisCyan,
    error              = ErrorRed,
)

@Composable
fun NotasMdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}