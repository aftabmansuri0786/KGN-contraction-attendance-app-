package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ConstructionOrangeLight,
    onPrimary = CharcoalDark,
    primaryContainer = ConstructionOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFCFD8DC),
    onSecondary = CharcoalDark,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceCardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalMedium,
    onSurfaceVariant = TextSecondaryDark,
    outline = SurfaceCardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = ConstructionOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = ConstructionOrangeDark,
    secondary = CharcoalDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECEFF1),
    onSecondaryContainer = CharcoalDark,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = TextSecondaryLight,
    outline = SurfaceCardBorder
)

@Composable
fun KgnConstructionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
