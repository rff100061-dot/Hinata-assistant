package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HinataColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = DeepViolet,
    onPrimaryContainer = SoftLilac,
    secondary = LavenderPurple,
    onSecondary = Color.Black,
    secondaryContainer = GlassSurfaceLight,
    onSecondaryContainer = Color.White,
    tertiary = BlushingPink,
    background = MidnightNavy,
    onBackground = TextWhite,
    surface = DeepViolet,
    onSurface = TextWhite,
    error = CallRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HinataColorScheme,
        typography = Typography,
        content = content
    )
}
