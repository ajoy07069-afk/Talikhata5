package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val TalikhataLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueSoftLight,
    onPrimaryContainer = DeepNavy,
    secondary = BrandPurple,
    onSecondary = Color.White,
    secondaryContainer = BrandPurpleSoftLight,
    onSecondaryContainer = Color(0xFF3B0764),
    tertiary = ReportTeal,
    onTertiary = Color.White,
    tertiaryContainer = ReportTealSoftLight,
    onTertiaryContainer = Color(0xFF042F2E),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = WarningRed,
    onError = Color.White,
    errorContainer = WarningRedSoftLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val TalikhataDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = PrimaryBlueSoftDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = BrandPurpleSoftDark,
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = ReportTealSoftDark,
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF0F172A),
    errorContainer = WarningRedSoftDark,
    onErrorContainer = Color(0xFFFEE2E2)
)

val TalikhataShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun TalikhataTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) TalikhataDarkColorScheme else TalikhataLightColorScheme
    val talikhataColors = if (useDarkTheme) DarkTalikhataColors else LightTalikhataColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDarkTheme
                controller.isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    CompositionLocalProvider(LocalTalikhataColors provides talikhataColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TalikhataTypography,
            shapes = TalikhataShapes,
            content = content
        )
    }
}
