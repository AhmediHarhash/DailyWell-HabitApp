package com.dailywell.app.core.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import habithealth.shared.generated.resources.Res
import habithealth.shared.generated.resources.inter_bold
import habithealth.shared.generated.resources.inter_medium
import habithealth.shared.generated.resources.inter_regular
import habithealth.shared.generated.resources.inter_semibold
import habithealth.shared.generated.resources.inter_display_bold
import habithealth.shared.generated.resources.inter_display_medium
import habithealth.shared.generated.resources.inter_display_semibold
import org.jetbrains.compose.resources.Font

/**
 * DailyWell 2026 Typography System — Inter
 *
 * Font: Inter by Rasmus Andersson (same font Strava uses for health/fitness UI)
 * - High x-height (73%) for exceptional mobile readability
 * - Optical sizing: InterDisplay for headlines (24sp+), Inter for body (<24sp)
 * - Variable tracking: tight on headlines, open on small text
 * - Weight hierarchy: Bold/SemiBold display → Medium titles → Regular body
 *
 * 2026 principles applied:
 * - Negative letter-spacing on display/headline (tight, premium)
 * - Generous line-height on body (1.5x+, calm wellness feel)
 * - Wide tracking on ALL-CAPS labels (+4-8%, prestige signal)
 * - Single font family, 4 weights (no font mixing)
 * - 16sp body baseline, 1.25x modular scale
 */

/** Inter font family — body text (optimized for <24sp) */
val InterFontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )

/** InterDisplay font family — headlines & display (optimized for 24sp+) */
val InterDisplayFontFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.inter_display_medium, FontWeight.Medium),
        Font(Res.font.inter_display_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_display_bold, FontWeight.Bold),
    )

/**
 * Create the full Material3 Typography using Inter.
 * Must be called from a @Composable context to load fonts.
 */
@Composable
fun createAppTypography(): Typography {
    val displayFamily = InterDisplayFontFamily
    val bodyFamily = InterFontFamily

    return Typography(
        // ── Display: Hero moments, onboarding, philosophy messages ──
        // InterDisplay at large sizes, tight tracking for premium feel
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 40.sp,       // ~1.18x — tight for display
            letterSpacing = (-0.5).sp  // Tight: premium 2026 signal
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,       // ~1.21x
            letterSpacing = (-0.4).sp
        ),
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,       // 1.25x
            letterSpacing = (-0.3).sp
        ),

        // ── Headlines: Section titles, screen headers ──
        // InterDisplay for visual weight, negative tracking
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 30.sp,       // 1.25x
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,       // 1.3x
            letterSpacing = (-0.15).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp,       // 1.33x
            letterSpacing = (-0.1).sp
        ),

        // ── Titles: Card headers, habit names, navigation items ──
        // Transition zone: Inter (body variant) at medium sizes
        titleLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp,       // 1.44x
            letterSpacing = (-0.05).sp
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,       // 1.5x
            letterSpacing = 0.sp      // Neutral at crossover
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,       // 1.43x
            letterSpacing = 0.05.sp
        ),

        // ── Body: Main content, descriptions, insights ──
        // Inter body, generous line height for calm wellness reading
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 26.sp,       // 1.625x — generous, calm
            letterSpacing = 0.1.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,       // 1.57x
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,       // 1.5x
            letterSpacing = 0.2.sp    // Open for small text readability
        ),

        // ── Labels: Buttons, chips, badges, nav items, ALL-CAPS overlines ──
        // Inter body, wider tracking for small sizes (2026 prestige pattern)
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,       // 1.43x
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,       // 1.33x
            letterSpacing = 0.5.sp    // Wide tracking for ALL-CAPS labels
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp,       // 1.4x
            letterSpacing = 0.6.sp    // Extra-wide for tiny caps
        )
    )
}

/**
 * Fallback typography for non-composable contexts.
 * Uses system default font. The real Inter-based typography is
 * created at theme composition time via createAppTypography().
 */
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.15).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.05).sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.05.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
)
