package com.saha.kidstest

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Blue-ink and pencil-yellow, like an OMR answer sheet.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2747C9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF0B1A5E),
    secondary = Color(0xFF7A5D00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF2A2000),
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF181B24),
    surface = Color.White,
    onSurface = Color(0xFF181B24),
    surfaceVariant = Color(0xFFE5E8F2),
    onSurfaceVariant = Color(0xFF4A4F5E),
    outline = Color(0xFFB7BCCB),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C2FF),
    onPrimary = Color(0xFF0B1A5E),
    primaryContainer = Color(0xFF263A8F),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFFFFD45C),
    onSecondary = Color(0xFF3D2F00),
    secondaryContainer = Color(0xFF5A4500),
    onSecondaryContainer = Color(0xFFFFE08A),
    background = Color(0xFF11141C),
    onBackground = Color(0xFFE3E5EE),
    surface = Color(0xFF1A1E29),
    onSurface = Color(0xFFE3E5EE),
    surfaceVariant = Color(0xFF2A2F3D),
    onSurfaceVariant = Color(0xFFC3C7D4),
    outline = Color(0xFF5B6172),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun SahaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}

@Composable
fun goodColor(): Color = if (isSystemInDarkTheme()) Color(0xFF7FD9A6) else Color(0xFF1B7F4B)

@Composable
fun goodBg(): Color = if (isSystemInDarkTheme()) Color(0xFF173A28) else Color(0xFFDDF4E6)

@Composable
fun badColor(): Color = if (isSystemInDarkTheme()) Color(0xFFFFB4AB) else Color(0xFFB3261E)

@Composable
fun badBg(): Color = if (isSystemInDarkTheme()) Color(0xFF4A1C1A) else Color(0xFFFDE3E0)
