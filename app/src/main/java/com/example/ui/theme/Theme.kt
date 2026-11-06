package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AnonCyan,
    onPrimary = Color.Black,
    primaryContainer = AnonDarkSurfaceVariant,
    onPrimaryContainer = AnonCyan,
    secondary = AnonViolet,
    onSecondary = Color.White,
    secondaryContainer = AnonDarkSurfaceVariant,
    onSecondaryContainer = AnonViolet,
    tertiary = AnonIndigo,
    onTertiary = Color.White,
    background = AnonDarkBackground,
    onBackground = AnonTextPrimaryDark,
    surface = AnonDarkSurface,
    onSurface = AnonTextPrimaryDark,
    surfaceVariant = AnonDarkSurfaceVariant,
    onSurfaceVariant = AnonTextSecondaryDark,
    outline = AnonDarkBorder,
    outlineVariant = AnonDarkBorder.copy(alpha = 0.5f),
    error = AnonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = AnonCyanVariant,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    secondary = AnonViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF311B92),
    tertiary = AnonIndigo,
    onTertiary = Color.White,
    background = AnonLightBackground,
    onBackground = AnonTextPrimaryLight,
    surface = AnonLightSurface,
    onSurface = AnonTextPrimaryLight,
    surfaceVariant = AnonLightSurfaceVariant,
    onSurfaceVariant = AnonTextSecondaryLight,
    outline = AnonLightBorder,
    outlineVariant = AnonLightBorder.copy(alpha = 0.7f),
    error = AnonError,
    onError = Color.White
)

@Composable
fun AnonbotTheme(
    darkTheme: Boolean = true, // Default to dark theme as requested
    dynamicColor: Boolean = false, // Use our branded gradient palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
