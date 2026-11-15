package io.github.iamoutofmana.doomscroll.ui.theme

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

// "Ink & Signal" brand palette — see docs/branding/README.md.
private val Ink = Color(0xFF10151A)
private val Paper = Color(0xFFF4F1EA)
private val Signal = Color(0xFFFF6A3D)

private val LightScheme =
    lightColorScheme(
        primary = Signal,
        onPrimary = Color.White,
        background = Paper,
        onBackground = Ink,
        surface = Paper,
        onSurface = Ink,
    )

private val DarkScheme =
    darkColorScheme(
        primary = Signal,
        onPrimary = Color.White,
        background = Ink,
        onBackground = Paper,
        surface = Ink,
        onSurface = Paper,
    )

@Composable
fun DoomscrollTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You dynamic color on Android 12+ — picks up One UI wallpaper theming.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkScheme
            else -> LightScheme
        }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
