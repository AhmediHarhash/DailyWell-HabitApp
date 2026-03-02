package com.dailywell.app.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared premium design tokens for the Tab-level redesign (phase-1 baseline).
 *
 * Goal: stabilize one neutral, premium system so all core tabs consume the same
 * gradients, accents, and motion timings.
 */

@Immutable
data class PremiumPalette(
    val background: List<Color>,
    val iconColor: Color
)

/**
 * Motion timing references used by shared components.
 */
object PremiumMotionTokens {
    const val listEnterDurationMs: Int = 320
    const val listItemStaggerMs: Long = 58L
    const val shimmerDurationMs: Int = 1500
    const val tabTransitionEnterMs: Int = 280
    const val tabTransitionExitMs: Int = 180
    const val tabTransitionDelayMs: Int = 0
    const val featureTransitionEnterMs: Int = 350
    const val featureTransitionCrossfadeInMs: Int = 220
    const val featureTransitionCrossfadeExitMs: Int = 180
    const val featureTransitionExitMs: Int = 200
    const val featureTransitionDelayMs: Int = 50
    const val navIndicatorDurationMs: Int = 250
    const val navTintDurationMs: Int = 200
    const val navTapScaleDurationMs: Int = 150
    const val fastFadeMs: Int = 150
    const val microFadeMs: Int = 200
    const val pressScaleMs: Int = 150
    const val breathingDurationMs: Int = 2000
    const val pulseDurationMs: Int = 800
    const val shakeDurationMs: Int = 500
    const val fadeInDurationMs: Int = 300
    const val gradientShiftDurationMs: Int = 8000
}

/**
 * Frozen premium color families for top-level surfaces.
 */
object PremiumColorTokens {
    val sectionChipGradient = PremiumDesignTokens.sectionChipGradient
    val sectionChipBorder = PremiumDesignTokens.sectionChipBorder
    val sectionChipText = PremiumDesignTokens.sectionChipText
    val headerGradient = PremiumDesignTokens.headerGradient
    val heroCardGradient = PremiumDesignTokens.heroCardGradient
    val heroCardOverlay = PremiumDesignTokens.heroCardOverlay
    val actionTileGradients = PremiumDesignTokens.actionTileGradients
    val ctaGradient = PremiumDesignTokens.ctaGradient
    val progressGradient = PremiumDesignTokens.progressGradient
}

/**
 * Shared layout spacing and sizing tokens for premium components.
 */
object PremiumLayoutTokens {
    val headerCornerRadius = 22.dp
    val headerOuterHorizontalPadding = 16.dp
    val headerOuterVerticalPadding = 10.dp
    val headerInnerPadding = 16.dp
    val headerIconContainer = 40.dp
    val headerIconSize = 20.dp

    val sectionChipHorizontalPadding = 12.dp
    val sectionChipVerticalPadding = 6.dp
    val sectionChipIconSize = 14.dp

    val actionTileCornerRadius = 20.dp
    val actionTilePadding = 16.dp
    val actionTileSpacing = 14.dp
    val actionTileIconContainer = 40.dp
    val actionTileIconSize = 20.dp
}

/**
 * Color and surface presets for shared premium UI surfaces.
 */
object PremiumDesignTokens {
    // Frozen luxury-neutral palette.
    val neutralSurface = Color(0xFFF4F7F5)
    val neutralSurfaceAlt = Color(0xFFEAF1EE)
    val neutralInk = Color(0xFF1F2F2A)
    val neutralMuted = Color(0xFF4F635C)

    val headerGradient = listOf(
        Color(0xFF3F6E86),
        Color(0xFF4A83A8),
        Color(0xFF5F74B1)
    )
    val headerBorder = Color(0x22FFFFFF)
    val headerIconBackdrop = Color(0xFFFFFFFF).copy(alpha = 0.22f)
    val headerTitleText = Color(0xFFF5F8F7)

    val heroCardGradient = listOf(
        Color(0xFFE3F0FA),
        Color(0xFFE8EEFC),
        Color(0xFFEDEAFE)
    )
    val heroCardOverlay = listOf(
        AccentSky.copy(alpha = 0.18f),
        AccentIndigo.copy(alpha = 0.12f)
    )

    val sectionChipGradient = listOf(
        Color(0xFFE7F1FB),
        Color(0xFFE8EEFB),
        Color(0xFFEDE9FB)
    )
    val sectionChipBorder = Color(0x33516A86)
    val sectionChipText = Color(0xFF2C3C53)

    // Shared gradients for action tiles and CTAs.
    val actionTileGradients = listOf(
        listOf(AccentSky, AccentIndigo),
        listOf(AccentIndigo, AccentViolet),
        listOf(AccentSky, AccentViolet),
        listOf(AccentAmber, AccentRose)
    )

    val ctaGradient = listOf(
        AccentSky,
        AccentIndigo,
        AccentViolet
    )

    val progressGradient = listOf(
        AccentSky,
        AccentIndigo
    )

    val insightsFeaturePalettes = listOf(
        PremiumPalette(
            background = listOf(AccentIndigo, AccentViolet),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, AccentIndigo),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentAmber, AccentRose),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, AccentViolet),
            iconColor = Color(0xFFFFFFFF)
        )
    )

    val trackFeaturePalettes = listOf(
        PremiumPalette(
            background = listOf(AccentSky, AccentIndigo),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, AccentViolet),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentAmber, AccentIndigo),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentIndigo, AccentViolet),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, AccentViolet),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentViolet, AccentRose),
            iconColor = Color(0xFFFFFFFF)
        )
    )

    val trackQuickActionPalettes = listOf(
        PremiumPalette(
            background = listOf(AccentSky, AccentIndigo),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, AccentViolet),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentIndigo, AccentAmber),
            iconColor = Color(0xFFFFFFFF)
        )
    )
}
