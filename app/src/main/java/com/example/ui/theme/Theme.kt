package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = GreenBright,
    onPrimary = Color.Black,
    primaryContainer = GreenPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = GreenHighlight,
    onSecondary = Color.Black,
    secondaryContainer = GreenSurfaceTint,
    onSecondaryContainer = GreenHighlight,
    tertiary = AmberPro,
    onTertiary = Color.Black,
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceTint = GreenHighlight,
    outline = DarkSurfaceBorder,
    outlineVariant = GreenBorder,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedDark,
    onErrorContainer = Color(0xFFFCA5A5)
  )

@Composable
fun OrkiTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
