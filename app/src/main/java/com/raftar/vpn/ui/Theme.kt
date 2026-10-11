package com.raftar.vpn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Raftar VPN premium light theme — saaf suthra, teal accents.
 */
private val RaftarColors = lightColorScheme(
    primary = Color(0xFF00BFA5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF7F0),
    onPrimaryContainer = Color(0xFF062A5E),
    secondary = Color(0xFF0D47A1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE9FB),
    onSecondaryContainer = Color(0xFF062A5E),
    background = Color(0xFFF1F8F6),
    onBackground = Color(0xFF10233B),
    surface = Color.White,
    onSurface = Color(0xFF10233B),
    surfaceVariant = Color(0xFFE4EFEc),
    onSurfaceVariant = Color(0xFF4A6076),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    outline = Color(0xFFC4D6D2)
)

@Composable
fun RaftarTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RaftarColors, content = content)
}
