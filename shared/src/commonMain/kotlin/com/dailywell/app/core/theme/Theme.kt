package com.dailywell.app.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Time-of-day enum for circadian-aware theming
 * Psychology: Aligning UI with user's natural rhythm increases engagement
 */
enum class TimeOfDay {
    MORNING,    // 5am - 11am: Energizing, warm tones
    AFTERNOON,  // 12pm - 5pm: Productive, bright tones
    EVENING,    // 6pm - 9pm: Calming, relaxing tones
    NIGHT       // 10pm - 4am: Restful, sleep-inducing tones
}

/**
 * Extended color palette for 2026 modern UX
 * Provides glassmorphism, time-aware gradients, mesh backgrounds,
 * surface layers, and accessibility colors
 */
@Immutable
data class DailyWellColors(
    // Glassmorphism
    val glassBackground: Color,
    val glassBorder: Color,
    val glassHighlight: Color,

    // Glass panel variants (iOS 18 / Arc Browser style)
    val glassPrimary: Color,
    val glassSecondary: Color,
    val glassAccent: Color,
    val glassOverlay: Color,
    val glassSheet: Color,
    val glassNavBar: Color,

    // Time-of-day gradients
    val timeGradientStart: Color,
    val timeGradientEnd: Color,
    val timeAccent: Color,

    // Shadows (warm-toned)
    val shadowLight: Color,
    val shadowMedium: Color,
    val shadowStrong: Color,

    // High contrast text (WCAG 4.5:1+)
    val highContrastOnPrimary: Color,
    val highContrastOnSuccess: Color,

    // Interactive states
    val pressedOverlay: Color,
    val focusRing: Color,

    // Current time of day
    val timeOfDay: TimeOfDay,

    // Mesh gradient anchors (for aurora/organic backgrounds)
    val meshColor1: Color,
    val meshColor2: Color,

    // Layered surfaces (for depth without heavy shadows)
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val surfaceMuted: Color,

    // Warm divider
    val divider: Color
)

/**
 * CompositionLocal for accessing extended colors throughout the app
 */
val LocalDailyWellColors = staticCompositionLocalOf {
    DailyWellColors(
        glassBackground = GlassLightBackground,
        glassBorder = GlassBorderLight,
        glassHighlight = GlassHighlight,
        glassPrimary = GlassPrimaryLight,
        glassSecondary = GlassSecondaryLight,
        glassAccent = GlassAccentLight,
        glassOverlay = GlassOverlayLight,
        glassSheet = GlassSheetLight,
        glassNavBar = GlassNavBarLight,
        timeGradientStart = MorningGradientStart,
        timeGradientEnd = MorningGradientEnd,
        timeAccent = MorningAccent,
        shadowLight = ShadowLight,
        shadowMedium = ShadowMedium,
        shadowStrong = ShadowStrong,
        highContrastOnPrimary = TextOnPrimaryLight,
        highContrastOnSuccess = TextOnSuccessLight,
        pressedOverlay = PressedOverlay,
        focusRing = FocusRing,
        timeOfDay = TimeOfDay.MORNING,
        meshColor1 = MeshWarm1,
        meshColor2 = MeshWarm2,
        surfaceElevated = SurfaceElevated,
        surfaceSubtle = SurfaceSubtle,
        surfaceMuted = SurfaceMuted,
        divider = DividerLight
    )
}

/**
 * Get current time of day based on system clock
 */
private fun getCurrentTimeOfDay(): TimeOfDay {
    val currentHour = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .hour

    return when (currentHour) {
        in 5..11 -> TimeOfDay.MORNING
        in 12..17 -> TimeOfDay.AFTERNOON
        in 18..21 -> TimeOfDay.EVENING
        else -> TimeOfDay.NIGHT
    }
}

/**
 * Create DailyWellColors based on dark mode and time of day
 */
private fun createDailyWellColors(
    darkTheme: Boolean,
    timeOfDay: TimeOfDay
): DailyWellColors {
    val (gradientStart, gradientEnd, accent) = when (timeOfDay) {
        TimeOfDay.MORNING -> Triple(MorningGradientStart, MorningGradientEnd, MorningAccent)
        TimeOfDay.AFTERNOON -> Triple(AfternoonGradientStart, AfternoonGradientEnd, AfternoonAccent)
        TimeOfDay.EVENING -> Triple(EveningGradientStart, EveningGradientEnd, EveningAccent)
        TimeOfDay.NIGHT -> Triple(NightGradientStart, NightGradientEnd, NightAccent)
    }

    // Mesh gradient colors shift with time of day for organic feel
    val (mesh1, mesh2) = when (timeOfDay) {
        TimeOfDay.MORNING -> Pair(MeshWarm1, MeshWarm2)
        TimeOfDay.AFTERNOON -> Pair(MeshCool1, MeshWarm1)
        TimeOfDay.EVENING -> Pair(MeshWarm2, MeshCool2)
        TimeOfDay.NIGHT -> Pair(MeshCool2, MeshCool1)
    }

    return if (darkTheme) {
        DailyWellColors(
            glassBackground = GlassDarkBackground,
            glassBorder = GlassBorderDark,
            glassHighlight = GlassHighlight.copy(alpha = 0.12f),
            glassPrimary = GlassPrimaryDark,
            glassSecondary = GlassSecondaryDark,
            glassAccent = GlassAccentDark,
            glassOverlay = GlassOverlayDark,
            glassSheet = GlassSheetDark,
            glassNavBar = GlassNavBarDark,
            timeGradientStart = gradientStart.copy(alpha = 0.12f),
            timeGradientEnd = gradientEnd.copy(alpha = 0.12f),
            timeAccent = accent,
            shadowLight = ShadowDarkMode.copy(alpha = 0.12f),
            shadowMedium = ShadowDarkMode.copy(alpha = 0.22f),
            shadowStrong = ShadowDarkMode,
            highContrastOnPrimary = TextPrimaryDark,
            highContrastOnSuccess = TextPrimaryDark,
            pressedOverlay = Color.White.copy(alpha = 0.06f),
            focusRing = PrimaryLight,
            timeOfDay = timeOfDay,
            meshColor1 = mesh1.copy(alpha = 0.08f),
            meshColor2 = mesh2.copy(alpha = 0.08f),
            surfaceElevated = Color(0xFF242321),
            surfaceSubtle = Color(0xFF1A1918),
            surfaceMuted = Color(0xFF2A2826),
            divider = DividerDark
        )
    } else {
        DailyWellColors(
            glassBackground = GlassLightBackground,
            glassBorder = GlassBorderLight,
            glassHighlight = GlassHighlight,
            glassPrimary = GlassPrimaryLight,
            glassSecondary = GlassSecondaryLight,
            glassAccent = GlassAccentLight,
            glassOverlay = GlassOverlayLight,
            glassSheet = GlassSheetLight,
            glassNavBar = GlassNavBarLight,
            timeGradientStart = gradientStart,
            timeGradientEnd = gradientEnd,
            timeAccent = accent,
            shadowLight = ShadowLight,
            shadowMedium = ShadowMedium,
            shadowStrong = ShadowStrong,
            highContrastOnPrimary = TextOnPrimaryLight,
            highContrastOnSuccess = TextOnSuccessLight,
            pressedOverlay = PressedOverlay,
            focusRing = FocusRing,
            timeOfDay = timeOfDay,
            meshColor1 = mesh1,
            meshColor2 = mesh2,
            surfaceElevated = SurfaceElevated,
            surfaceSubtle = SurfaceSubtle,
            surfaceMuted = SurfaceMuted,
            divider = DividerLight
        )
    }
}

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = SecondaryDark,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardBackgroundLight,
    onSurfaceVariant = TextSecondaryLight,
    error = Error,
    onError = OnPrimary,
    errorContainer = ErrorLight,
    outline = DividerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = PrimaryDark,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryLight,
    onSecondary = SecondaryDark,
    secondaryContainer = SecondaryDark,
    onSecondaryContainer = SecondaryLight,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardBackgroundDark,
    onSurfaceVariant = TextSecondaryDark,
    error = Error,
    onError = OnPrimary,
    errorContainer = ErrorLight,
    outline = DividerDark
)

@Composable
fun DailyWellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Create extended colors with time-of-day awareness
    val timeOfDay = getCurrentTimeOfDay()
    val dailyWellColors = createDailyWellColors(darkTheme, timeOfDay)

    // 2026 Typography: Inter font with optical sizing (Display vs Body)
    val typography = createAppTypography()

    CompositionLocalProvider(
        LocalDailyWellColors provides dailyWellColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

/**
 * Accessor for extended DailyWell colors
 * Usage: DailyWellTheme.colors.glassBackground
 */
object DailyWellTheme {
    val colors: DailyWellColors
        @Composable
        get() = LocalDailyWellColors.current
}
