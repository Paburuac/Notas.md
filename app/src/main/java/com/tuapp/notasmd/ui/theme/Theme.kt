package com.tuapp.notasmd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    background         = CreamBackground,
    surface            = CreamSurface,
    surfaceVariant     = CreamSurfaceHigh,
    onBackground       = InkPrimary,
    onSurface          = InkPrimary,
    onSurfaceVariant   = InkSecondary,
    primary            = AccentSepia,
    onPrimary          = CreamBackground,
    primaryContainer   = AccentSepiaLight,
    onPrimaryContainer = InkPrimary,
    error              = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    background         = DarkBackground,
    surface            = DarkSurface,
    surfaceVariant     = DarkSurfaceHigh,
    onBackground       = PaperLight,
    onSurface          = PaperLight,
    onSurfaceVariant   = PaperSecondary,
    primary            = AccentAmber,
    onPrimary          = DarkBackground,
    primaryContainer   = AccentAmberLight,
    onPrimaryContainer = DarkBackground,
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