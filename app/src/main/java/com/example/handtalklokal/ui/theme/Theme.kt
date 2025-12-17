package com.example.handtalklokal.ui.theme

import android.app.Activity
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
    primary = Blue80,
    onPrimary = Color.White,
    primaryContainer = BlueGrey80,
    onPrimaryContainer = Color.White,
    secondary = LightBlue80,
    onSecondary = Color.Black,
    secondaryContainer = BlueGrey80,
    onSecondaryContainer = Color.White,
    tertiary = LightBlue40,
    onTertiary = Color.White,
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF25282A),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF41484D),
    onSurfaceVariant = Color(0xFFC1C7CD),
    outline = Color(0xFF8B9198),
    inverseOnSurface = Color(0xFF1A1C1E),
    inverseSurface = Color(0xFFE2E2E5),
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = LightBlue40,
    onPrimaryContainer = Color.White,
    secondary = Blue80,
    onSecondary = Color.White,
    secondaryContainer = SurfaceBlue,
    onSecondaryContainer = OnSurfaceBlue,
    tertiary = LightBlue40,
    onTertiary = Color.White,
    background = BackgroundBlue,
    onBackground = OnSurfaceBlue,
    surface = Color.White,
    onSurface = OnSurfaceBlue,
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E),
    inverseOnSurface = Color(0xFFF1F0F4),
    inverseSurface = Color(0xFF2F3033),

    /* Other default colors to override */
)

@Composable
fun HandTalkLokalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Disabled dynamic color to ensure consistent blue theme
    dynamicColor: Boolean = false,
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