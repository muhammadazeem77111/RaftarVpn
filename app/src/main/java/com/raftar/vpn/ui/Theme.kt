package com.raftar.vpn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Raftar VPN premium DARK theme — Surfshark-style deep navy + mint teal.
 */
private val RaftarColors = darkColorScheme(
    primary = Color(0xFF00D6A0),
    onPrimary = Color(0xFF052A20),
    primaryContainer = Color(0xFF0B3D31),
    onPrimaryContainer = Color(0xFFA9F5DD),
    secondary = Color(0xFF4FC3F7),
    onSecondary = Color(0xFF062A3A),
    secondaryContainer = Color(0xFF12314A),
    onSecondaryContainer = Color(0xFFC9E6F7),
    background = Color(0xFF081420),
    onBackground = Color(0xFFEAF2F8),
    surface = Color(0xFF0C1F33),
    onSurface = Color(0xFFEAF2F8),
    surfaceVariant = Color(0xFF132A42),
    onSurfaceVariant = Color(0xFF8FA3B8),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3A0A0A),
    outline = Color(0xFF1E3A55)
)

@Composable
fun RaftarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RaftarColors, content = content)
}
