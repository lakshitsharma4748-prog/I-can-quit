package com.safeshield.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ShieldGreen80,
    secondary = ShieldGreenGrey80,
    tertiary = AlertAmber80,
    background = NeutralBackgroundDark,
    surface = NeutralBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = ShieldGreen40,
    secondary = ShieldGreenGrey40,
    tertiary = AlertAmber40,
    background = NeutralBackgroundLight,
    surface = NeutralBackgroundLight
)

/**
 * SafeShield's Material 3 theme.
 *
 * Dynamic color (Android 12+) is disabled by default so that protection
 * status colors (active/inactive/blocked) stay consistent across devices
 * regardless of the user's wallpaper-derived palette.
 */
@Composable
fun SafeShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
