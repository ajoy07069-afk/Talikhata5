package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Brand Palette
val DeepNavy = Color(0xFF172554)
val DeepNavyLight = Color(0xFF1E3A8A)
val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueSoftLight = Color(0xFFDBEAFE)
val PrimaryBlueSoftDark = Color(0xFF1E3A8A)

val BrandPurple = Color(0xFF7C3AED)
val BrandPurpleSoftLight = Color(0xFFEDE9FE)
val BrandPurpleSoftDark = Color(0xFF4C1D95)

val EmeraldGreen = Color(0xFF059669)
val EmeraldSoftLight = Color(0xFFD1FAE5)
val EmeraldSoftDark = Color(0xFF064E3B)

val DueOrange = Color(0xFFEA580C)
val DueOrangeSoftLight = Color(0xFFFFEDD5)
val DueOrangeSoftDark = Color(0xFF7C2D12)

val WarningRed = Color(0xFFDC2626)
val WarningRedSoftLight = Color(0xFFFEE2E2)
val WarningRedSoftDark = Color(0xFF7F1D1D)

val ReportTeal = Color(0xFF0F766E)
val ReportTealSoftLight = Color(0xFFCCFBF1)
val ReportTealSoftDark = Color(0xFF134E4A)

// Surfaces & Backgrounds
val LightBackground = Color(0xFFF4F7FC)
val LightCard = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EEF8)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightOutline = Color(0xFFCBD5E1)

val DarkBackground = Color(0xFF0F172A)
val DarkCard = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF273549)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)

@Immutable
data class TalikhataColors(
    val isDark: Boolean,
    val deepNavy: Color,
    val customerBlue: Color,
    val customerContainer: Color,
    val supplierPurple: Color,
    val supplierContainer: Color,
    val paymentGreen: Color,
    val paymentContainer: Color,
    val dueOrange: Color,
    val dueContainer: Color,
    val warningRed: Color,
    val warningContainer: Color,
    val reportTeal: Color,
    val reportContainer: Color,
    val cardBackground: Color,
    val subtleDivider: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LightTalikhataColors = TalikhataColors(
    isDark = false,
    deepNavy = DeepNavy,
    customerBlue = PrimaryBlue,
    customerContainer = PrimaryBlueSoftLight,
    supplierPurple = BrandPurple,
    supplierContainer = BrandPurpleSoftLight,
    paymentGreen = EmeraldGreen,
    paymentContainer = EmeraldSoftLight,
    dueOrange = DueOrange,
    dueContainer = DueOrangeSoftLight,
    warningRed = WarningRed,
    warningContainer = WarningRedSoftLight,
    reportTeal = ReportTeal,
    reportContainer = ReportTealSoftLight,
    cardBackground = LightCard,
    subtleDivider = Color(0xFFE2E8F0),
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary
)

val DarkTalikhataColors = TalikhataColors(
    isDark = true,
    deepNavy = Color(0xFF1E3A8A),
    customerBlue = Color(0xFF60A5FA),
    customerContainer = PrimaryBlueSoftDark,
    supplierPurple = Color(0xFFA78BFA),
    supplierContainer = BrandPurpleSoftDark,
    paymentGreen = Color(0xFF34D399),
    paymentContainer = EmeraldSoftDark,
    dueOrange = Color(0xFFFB923C),
    dueContainer = DueOrangeSoftDark,
    warningRed = Color(0xFFF87171),
    warningContainer = WarningRedSoftDark,
    reportTeal = Color(0xFF2DD4BF),
    reportContainer = ReportTealSoftDark,
    cardBackground = DarkCard,
    subtleDivider = Color(0xFF334155),
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary
)

val LocalTalikhataColors = staticCompositionLocalOf { LightTalikhataColors }
