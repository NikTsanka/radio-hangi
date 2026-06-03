package com.canka.dev.radiohangi.ui.theme

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

/** Manual theme selection exposed via the in-app toggle (cycles System → Light → Dark). */
enum class ThemeMode { System, Light, Dark }

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1407),
    secondary = GoldBright,
    onSecondary = Color(0xFF1A1407),
    tertiary = GoldDeep,
    background = DarkBackground,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkMuted,
    error = ErrorRed,
)

private val LightColors = lightColorScheme(
    primary = GoldDeep,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = Color(0xFF1A1407),
    tertiary = GoldBright,
    background = LightBackground,
    onBackground = OnLight,
    surface = LightSurface,
    onSurface = OnLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightMuted,
    error = ErrorRed,
)

@Composable
fun RadioHangiTheme(
    themeMode: ThemeMode = ThemeMode.System,
    // Dynamic color (Android 12+) is OFF by default so the gold brand identity shows;
    // can be toggled on later if desired.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
