package com.raftar.vpn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Raftar VPN premium DARK theme — deep navy + glowing teal (app icon jaisa).
 */
private val RaftarColors = darkColorScheme(
    primary = Color(0xFF00BFA5),
    onPrimary = Color(0xFF052E28),
    primaryContainer = Color(0xFF0E3B34),
    onPrimaryContainer = Color(0xFFB9F5EA),
    secondary = Color(0xFF4FC3F7),
    onSecondary = Color(0xFF062A3A),
    secondaryContainer = Color(0xFF14324A),
    onSecondaryContainer = Color(0xFFC9E6F7),
    background = Color(0xFF0A1A2E),
    onBackground = Color(0xFFEAF2F8),
    surface = Color(0xFF10263F),
    onSurface = Color(0xFFEAF2F8),
    surfaceVariant = Color(0xFF1A3350),
    onSurfaceVariant = Color(0xFF93A9BE),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3A0A0A),
    outline = Color(0xFF2A4560)
)

@Composable
fun RaftarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RaftarColors, content = content)
}
