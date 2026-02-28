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
        Color(0xFF486C66),
        Color(0xFF537A72),
        Color(0xFF3F6E8A)
    )
    val headerBorder = Color(0x22FFFFFF)
    val headerIconBackdrop = Color(0xFFFFFFFF).copy(alpha = 0.22f)
    val headerTitleText = Color(0xFFF5F8F7)

    val heroCardGradient = listOf(
        Color(0xFFDCEEE8),
        Color(0xFFE0F0F6),
        Color(0xFFDFE8FA)
    )
    val heroCardOverlay = listOf(
        Color(0xFF7CD1B8).copy(alpha = 0.2f),
        Color(0xFFB5D8E7).copy(alpha = 0.12f)
    )

    val sectionChipGradient = listOf(
        Color(0xFFE8F0EC),
        Color(0xFFDFECE7),
        Color(0xFFD8E7F3)
    )
    val sectionChipBorder = Color(0x336A746F)
    val sectionChipText = Color(0xFF2D3F3A)

    // Shared gradients for action tiles and CTAs.
    val actionTileGradients = listOf(
        listOf(Color(0xFF4F7E74), Color(0xFF6C9D90)),
        listOf(Color(0xFF4E6F8D), Color(0xFF6E8FAF)),
        listOf(Color(0xFF6C5F8E), Color(0xFF8C7AAF)),
        listOf(Color(0xFF7A6E58), Color(0xFF9D8E75))
    )

    val ctaGradient = listOf(
        Color(0xFF4E7B72),
        Color(0xFF5D8A80),
        Color(0xFF3F6E86)
    )

    val progressGradient = listOf(
        Color(0xFF5A8F82),
        Color(0xFF6EA89A)
    )

    val insightsFeaturePalettes = listOf(
        PremiumPalette(
            background = listOf(AccentIndigo, AccentViolet),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, AccentEmerald),
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
            background = listOf(AccentEmerald, AccentSky),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, PrimaryLight),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentAmber, Primary),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(PrimaryLight, Success),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, Secondary),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentViolet, AccentRose),
            iconColor = Color(0xFFFFFFFF)
        )
    )

    val trackQuickActionPalettes = listOf(
        PremiumPalette(
            background = listOf(AccentEmerald, AccentSky),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(AccentSky, Secondary),
            iconColor = Color(0xFFFFFFFF)
        ),
        PremiumPalette(
            background = listOf(Secondary, AccentAmber),
            iconColor = Color(0xFFFFFFFF)
        )
    )
}
