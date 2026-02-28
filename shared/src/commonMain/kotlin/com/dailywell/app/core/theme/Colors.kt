package com.dailywell.app.core.theme

import androidx.compose.ui.graphics.Color

// ============================================
// DAILYWELL 2026 COLOR SYSTEM
// Inspired by: Oura, Calm, Rise, Apple Health
// Direction: Warm wellness, organic, sophisticated
// ============================================

// Primary - Sage Green (Growth, Balance, Renewal)
// Shifted from stock Material green to a refined, muted sage
val Primary = Color(0xFF5B8C6B)
val PrimaryLight = Color(0xFF8BB89A)
val PrimaryDark = Color(0xFF3D6B4F)
val OnPrimary = Color.White

// Secondary - Eucalyptus Teal (Calm, Trust, Serenity)
val Secondary = Color(0xFF4A9E8F)
val SecondaryLight = Color(0xFF7EC4B8)
val SecondaryDark = Color(0xFF2E7A6D)
val OnSecondary = Color.White

// Background - Warm Cream/Off-white (not cold gray-white)
val BackgroundLight = Color(0xFFFAF8F5)
val BackgroundDark = Color(0xFF141413)
val SurfaceLight = Color(0xFFFFFEFC)
val SurfaceDark = Color(0xFF1E1D1B)

// Text - Warm charcoal (not pure black for gentler reading)
val TextPrimaryLight = Color(0xFF2D2B29)
val TextSecondaryLight = Color(0xFF6B6966)
val TextPrimaryDark = Color(0xFFF5F3F0)
val TextSecondaryDark = Color(0xFFADABA8)

// Accent Colors - Softer, more contemporary
val Success = Color(0xFF5AAF6E)
val SuccessLight = Color(0xFFEDF7F0)
val Warning = Color(0xFFE8A838)
val WarningLight = Color(0xFFFFF6E8)
val Error = Color(0xFFD9534F)
val ErrorLight = Color(0xFFFDECEC)

// Habit-specific colors - Refined, less saturated
val SleepColor = Color(0xFF7B89C9)      // Soft Indigo - night/rest
val WaterColor = Color(0xFF5CB8E0)      // Ocean Blue - hydration
val MoveColor = Color(0xFFE8825A)       // Warm Coral - energy
val VegetablesColor = Color(0xFF7BB87D) // Leaf Green - nutrition
val CalmColor = Color(0xFFAB6DBF)       // Lavender - meditation
val ConnectColor = Color(0xFFE8C44A)    // Warm Gold - warmth
val UnplugColor = Color(0xFF8E9EAB)     // Dusty Blue - digital detox

// New habit colors (onboarding redesign)
val FocusColor = Color(0xFFD4785C)      // Terracotta - focus/discipline
val LearnColor = Color(0xFF5A8FA8)      // Ocean Teal - learning
val GratitudeColor = Color(0xFFE0A84C)  // Warm Amber - gratitude
val NatureColor = Color(0xFF6DAF7B)     // Forest Green - nature
val BreatheColor = Color(0xFF8BA4C9)    // Sky Blue - breathwork

// Streak colors - Warm fire tones
val StreakFire = Color(0xFFE86B35)
val StreakGold = Color(0xFFE8C44A)

// Card backgrounds - Warm tones
val CardBackgroundLight = Color(0xFFFFFEFC)
val CardBackgroundDark = Color(0xFF272624)

// Dividers - Softer, warmer
val DividerLight = Color(0xFFE8E5E0)
val DividerDark = Color(0xFF3A3836)

// ============================================
// 2026 MODERN UX COLORS
// ============================================

// WCAG Compliant High Contrast Text (4.5:1+ ratio)
val TextOnPrimaryLight = Color(0xFF1C3D26)  // Deep sage on light green backgrounds
val TextOnSuccessLight = Color(0xFF2A4D30)  // For SuccessLight backgrounds
val HighContrastGreen = Color(0xFF1C3D26)   // For any light green surface

// Glassmorphism Colors - Warmer glass
val GlassLightBackground = Color(0xFFFFFEFC).copy(alpha = 0.78f)
val GlassDarkBackground = Color(0xFF1E1D1B).copy(alpha = 0.82f)
val GlassBorderLight = Color(0xFFFFFFFF).copy(alpha = 0.25f)
val GlassBorderDark = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val GlassHighlight = Color(0xFFFFFFFF).copy(alpha = 0.35f)

// Glass Panel Variants - for layered glass UI (iOS 18 / Arc Browser style)
val GlassPrimaryLight = Color(0xFF5B8C6B).copy(alpha = 0.08f)       // Primary-tinted glass
val GlassPrimaryDark = Color(0xFF8BB89A).copy(alpha = 0.12f)
val GlassSecondaryLight = Color(0xFF4A9E8F).copy(alpha = 0.06f)     // Secondary-tinted glass
val GlassSecondaryDark = Color(0xFF7EC4B8).copy(alpha = 0.10f)
val GlassAccentLight = Color(0xFFE8A838).copy(alpha = 0.06f)        // Accent-tinted glass
val GlassAccentDark = Color(0xFFE8C44A).copy(alpha = 0.10f)
val GlassOverlayLight = Color(0xFFFFFFFF).copy(alpha = 0.60f)       // Heavy overlay for sheets
val GlassOverlayDark = Color(0xFF1E1D1B).copy(alpha = 0.85f)
val GlassSheetLight = Color(0xFFFFFEFC).copy(alpha = 0.92f)         // Bottom sheet glass
val GlassSheetDark = Color(0xFF1E1D1B).copy(alpha = 0.95f)
val GlassNavBarLight = Color(0xFFFFFEFC).copy(alpha = 0.85f)        // Navigation bar glass
val GlassNavBarDark = Color(0xFF1E1D1B).copy(alpha = 0.90f)

// Time-of-Day Header Gradients (Psychology: circadian rhythm awareness)
// Rich, saturated colors that carry white text — NOT pastel washes
// Brand-connected: all rooted in sage green (#5B8C6B) family

// Morning (5am-11am): Golden sage sunrise — energizing warmth
val MorningGradientStart = Color(0xFF6B9E5E)   // Fresh leaf green
val MorningGradientEnd = Color(0xFF8BAF5A)     // Golden sage
val MorningAccent = Color(0xFFE8A848)          // Golden amber

// Afternoon (12pm-5pm): Deep sage — grounded, productive
val AfternoonGradientStart = Color(0xFF5B8C6B) // Core sage green (brand)
val AfternoonGradientEnd = Color(0xFF4A8F7F)   // Sage teal
val AfternoonAccent = Color(0xFF4A9ED6)        // Gentle blue

// Evening (6pm-9pm): Dusk sage — calming, reflective
val EveningGradientStart = Color(0xFF7391A3)   // Soft steel sage
val EveningGradientEnd = Color(0xFF9483A8)     // Twilight lilac sage
val EveningAccent = Color(0xFF9B5DAB)          // Muted purple

// Night (10pm-4am): Deep midnight sage — restful, sleep-inducing
val NightGradientStart = Color(0xFF627F92)     // Calm ocean sage
val NightGradientEnd = Color(0xFF76789C)       // Soft midnight indigo
val NightAccent = Color(0xFF5B6AB0)            // Night indigo

// Elevation Shadows - Warmer tones
val ShadowLight = Color(0x14201E1A)            // Warm shadow
val ShadowMedium = Color(0x1F201E1A)           // Warm medium shadow
val ShadowStrong = Color(0x29201E1A)           // Warm strong shadow
val ShadowDarkMode = Color(0x3D000000)         // Dark mode shadow

// Interactive States
val PressedOverlay = Color(0x08201E1A)         // Subtle warm press feedback
val HoverOverlay = Color(0x05201E1A)           // Hover state
val FocusRing = Color(0xFF8BB89A)              // Sage focus indicator

// Motivation Colors (Psychology: goal achievement)
val MotivationOrange = Color(0xFFE8825A)       // Warm terracotta CTA
val AchievementGold = Color(0xFFE8C44A)        // Warm gold
val CelebrationPink = Color(0xFFE88AAB)        // Soft rose celebration

// Calm UI Colors (Psychology: reduce anxiety)
val CalmBlue = Color(0xFF8EBAD9)               // Soothing sky blue
val TrustTeal = Color(0xFF7EC4B8)              // Eucalyptus trust
val NeutralGray = Color(0xFFB8B5B0)            // Warm neutral

// ============================================
// 2026 PREMIUM ACCENT COLORS
// Used for feature categories, gradients, highlights
// ============================================

// Feature category accents (softer than pure hex)
val AccentIndigo = Color(0xFF6366F1)
val AccentViolet = Color(0xFF8B5CF6)
val AccentRose = Color(0xFFE8637A)
val AccentAmber = Color(0xFFE8A838)
val AccentEmerald = Color(0xFF3DB87A)
val AccentSky = Color(0xFF4A9ED6)

// Mesh gradient anchor colors (for aurora/mesh backgrounds)
val MeshWarm1 = Color(0xFFFFF0E6)     // Peach cream
val MeshWarm2 = Color(0xFFFDE8F0)     // Blush
val MeshCool1 = Color(0xFFE8F0FD)     // Ice blue
val MeshCool2 = Color(0xFFEDE8FD)     // Soft violet

// Surface variants for layered depth
val SurfaceElevated = Color(0xFFFFFFFF)
val SurfaceSubtle = Color(0xFFF5F3F0)
val SurfaceMuted = Color(0xFFEBE8E4)
