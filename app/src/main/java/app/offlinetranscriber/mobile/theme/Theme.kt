package app.offlinetranscriber.mobile.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.offlinetranscriber.mobile.ui.design.AppShapes

private val RecordedPaperLight = lightColorScheme(
    primary = ActionCobalt,
    onPrimary = SheetWhite,
    primaryContainer = ActionCobaltSoft,
    onPrimaryContainer = Ink950,

    secondary = LocalTeal,
    onSecondary = SheetWhite,
    secondaryContainer = LocalTealSoft,
    onSecondaryContainer = Ink950,

    tertiary = ProcessAmber,
    onTertiary = SheetWhite,
    tertiaryContainer = ProcessAmberSoft,
    onTertiaryContainer = Ink950,

    error = DestructiveBrick,
    onError = SheetWhite,
    errorContainer = DestructiveBrickSoft,
    onErrorContainer = Ink950,

    background = CanvasMist,
    onBackground = Ink950,

    surface = SheetWhite,
    onSurface = Ink950,
    surfaceVariant = UtilityFill,
    onSurfaceVariant = Ink700,

    outline = Line300,
    outlineVariant = Line200,

    inverseSurface = Ink950,
    inverseOnSurface = SheetWhite,
    inversePrimary = DarkActionCobalt
)

private val RecordedPaperDark = darkColorScheme(
    primary = DarkActionCobalt,
    onPrimary = DarkCanvas,
    primaryContainer = DarkActionCobaltSoft,
    onPrimaryContainer = DarkTextPrimary,

    secondary = DarkLocalTeal,
    onSecondary = DarkCanvas,
    secondaryContainer = DarkLocalTealSoft,
    onSecondaryContainer = DarkTextPrimary,

    tertiary = DarkProcessAmber,
    onTertiary = DarkCanvas,
    tertiaryContainer = DarkProcessAmberSoft,
    onTertiaryContainer = DarkTextPrimary,

    error = DarkDestructive,
    onError = DarkCanvas,
    errorContainer = DarkDestructiveSoft,
    onErrorContainer = DarkTextPrimary,

    background = DarkCanvas,
    onBackground = DarkTextPrimary,

    surface = DarkSheet,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkUtility,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkLine,
    outlineVariant = DarkLine.copy(alpha = 0.65f),

    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkCanvas,
    inversePrimary = ActionCobalt
)

@Composable
fun TranscriberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /*
     * Dynamic color remains supported for the existing setting/feature,
     * but the deliberate Recorded Paper palette is the default identity.
     */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> RecordedPaperDark
        else -> RecordedPaperLight
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Edge-to-edge activity stays controlled by MainActivity.
            // System-bar icon contrast follows the active theme.
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()

            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes.material,
        content = content
    )
}
