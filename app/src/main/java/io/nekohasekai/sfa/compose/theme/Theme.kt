package io.nekohasekai.sfa.compose.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ModexCharcoalDarkScheme =
    darkColorScheme(
        primary = AccentEmerald,
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF0D281E),
        onPrimaryContainer = Color(0xFFA7F3D0),
        inversePrimary = Color(0xFF059669),
        secondary = AccentCyan,
        onSecondary = Color.Black,
        secondaryContainer = CharcoalSurfaceVariant,
        onSecondaryContainer = ModexTextPrimary,
        tertiary = Color(0xFF94A3B8),
        onTertiary = Color.Black,
        background = CharcoalBackground,
        onBackground = ModexTextPrimary,
        surface = CharcoalSurface,
        onSurface = ModexTextPrimary,
        surfaceVariant = CharcoalSurfaceVariant,
        onSurfaceVariant = ModexTextSecondary,
        surfaceContainer = CharcoalSurface,
        surfaceContainerLow = CharcoalBackground,
        surfaceContainerHigh = CharcoalSurfaceVariant,
        surfaceContainerHighest = Color(0xFF1E2330),
        surfaceDim = CharcoalBackground,
        surfaceBright = CharcoalSurfaceVariant,
        outline = CharcoalBorder,
        outlineVariant = Color(0xFF171A24),
        error = ErrorRed,
        onError = Color.White,
        errorContainer = Color(0xFF450A0A),
        onErrorContainer = Color(0xFFFEE2E2),
    )

private val ModexFlatLightScheme =
    lightColorScheme(
        primary = AccentEmerald,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1FAE5),
        onPrimaryContainer = Color(0xFF065F46),
        secondary = Color(0xFF0284C7),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1F5F9),
        onSecondaryContainer = Color(0xFF0F172A),
        tertiary = Color(0xFF475569),
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
