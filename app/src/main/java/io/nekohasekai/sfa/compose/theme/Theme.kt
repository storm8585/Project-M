package io.nekohasekai.sfa.compose.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// UI/UX Pro Max - Flat Design Mobile & Dark Charcoal Theme
private val ModexCharcoalDarkScheme =
    darkColorScheme(
        primary = ModexBlueLight,
        onPrimary = Color.White,
        primaryContainer = ModexBlueContainer,
        onPrimaryContainer = Color(0xFFDBEAFE),
        inversePrimary = ModexBlueDark,
        secondary = ModexBlueLight,
        onSecondary = Color.White,
        secondaryContainer = CharcoalSurfaceVariant,
        onSecondaryContainer = ModexTextPrimary,
        tertiary = Color(0xFF60A5FA),
        onTertiary = Color(0xFF0F172A),
        background = CharcoalBackground,
        onBackground = ModexTextPrimary,
        surface = CharcoalSurface,
        onSurface = ModexTextPrimary,
        surfaceVariant = CharcoalSurfaceVariant,
        onSurfaceVariant = ModexTextSecondary,
        surfaceContainer = CharcoalSurface,
        surfaceContainerLow = CharcoalBackground,
        surfaceContainerHigh = CharcoalSurfaceVariant,
        surfaceContainerHighest = Color(0xFF262C3A),
        surfaceDim = CharcoalBackground,
        surfaceBright = CharcoalSurfaceVariant,
        outline = CharcoalBorder,
        outlineVariant = Color(0xFF1F2430),
        error = ErrorRed,
        onError = Color.White,
        errorContainer = Color(0xFF7F1D1D),
        onErrorContainer = Color(0xFFFEE2E2),
    )

private val ModexFlatLightScheme =
    lightColorScheme(
        primary = ModexBlue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = ModexBlue,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1F5F9),
        onSecondaryContainer = Color(0xFF0F172A),
        tertiary = Color(0xFF0284C7),
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF475569),
        surfaceContainer = Color(0xFFF1F5F9),
        surfaceContainerLow = Color(0xFFFFFFFF),
        surfaceContainerHigh = Color(0xFFE2E8F0),
        outline = Color(0xFFE2E8F0),
        outlineVariant = Color(0xFFCBD5E1),
        error = ErrorRed,
        onError = Color.White,
    )

@Composable
fun Theme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ModexCharcoalDarkScheme else ModexFlatLightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
