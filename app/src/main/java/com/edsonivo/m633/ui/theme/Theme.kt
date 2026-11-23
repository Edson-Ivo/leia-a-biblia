package com.edsonivo.m633.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.edsonivo.m633.core.ThemeMode

data class Extra(val muted: Color, val line: Color, val dark: Boolean)

val LocalExtra = staticCompositionLocalOf { Extra(Color.Gray, Color.LightGray, false) }

private val Light = lightColorScheme(
    primary = Color(0xFF245C45),
    onPrimary = Color(0xFFFFFCF7),
    primaryContainer = Color(0xFFDCE9E0),
    onPrimaryContainer = Color(0xFF123526),
    secondary = Color(0xFFC48A12),
    background = Color(0xFFF7F4EE),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFCF7),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFEDE7DC),
    onSurfaceVariant = Color(0xFF5E5A54),
    surfaceContainer = Color(0xFFF3EEE4),
    surfaceContainerHigh = Color(0xFFEDE7DC),
    surfaceContainerHighest = Color(0xFFE7E1D6),
    outline = Color(0xFFD9D2C5),
    error = Color(0xFF9A3B2F),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FBFA4),
    onPrimary = Color(0xFF0E2219),
    primaryContainer = Color(0xFF244536),
    onPrimaryContainer = Color(0xFFD5EADD),
    secondary = Color(0xFFE6C36A),
    background = Color(0xFF141210),
    onBackground = Color(0xFFF6F1E8),
    surface = Color(0xFF1C1916),
    onSurface = Color(0xFFF6F1E8),
    surfaceVariant = Color(0xFF2A2520),
    onSurfaceVariant = Color(0xFFCFC6BA),
    surfaceContainer = Color(0xFF211D19),
    surfaceContainerHigh = Color(0xFF2A2520),
    surfaceContainerHighest = Color(0xFF332D27),
    outline = Color(0xFF4A423A),
    error = Color(0xFFFFB4A8),
)

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun M633Theme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = isDark(mode)
    val extra = if (dark) Extra(Color(0xFFB7AEA2), Color(0xFF3A332C), true)
    else Extra(Color(0xFF8A867F), Color(0xFFE7E1D6), false)
    CompositionLocalProvider(LocalExtra provides extra) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
    }
}
