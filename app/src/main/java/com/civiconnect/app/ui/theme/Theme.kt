package com.civiconnect.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaCivi = lightColorScheme(
    primary = Color(0xFF0E3A56),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6F2),
    onPrimaryContainer = Color(0xFF0E3A56),
    secondary = Color(0xFFF28C1B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE6C7),
    onSecondaryContainer = Color(0xFF5A3000),
    background = Color(0xFFF4F7FA),
    onBackground = Color(0xFF1B1F23),
    surface = Color.White,
    onSurface = Color(0xFF1B1F23),
    surfaceVariant = Color(0xFFE6ECF1),
    onSurfaceVariant = Color(0xFF55606A),
    error = Color(0xFFC0392B)
)

@Composable
fun CiviTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EsquemaCivi, content = content)
}
