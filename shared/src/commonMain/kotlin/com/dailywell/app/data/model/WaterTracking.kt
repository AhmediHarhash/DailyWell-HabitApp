package com.dailywell.app.data.model

import kotlinx.datetime.*

/**
 * Water Tracking Data Models
 * Tracks daily hydration goals and intake
 */

/**
 * Represents a single water intake entry
 */
data class WaterEntry(
    val id: String = "",
    val amountMl: Int,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val source: WaterSource = WaterSource.WATER
)

/**
 * Source of hydration
 */
enum class WaterSource(val displayName: String, val icon: String, val hydrationFactor: Float) {
    WATER("Water", "💧", 1.0f),
    TEA("Tea", "🍵", 0.95f),
    COFFEE("Coffee", "☕", 0.85f),
    JUICE("Juice", "🧃", 0.9f),
    SPARKLING("Sparkling Water", "🫧", 1.0f),
    MILK("Milk", "🥛", 0.87f),
    SMOOTHIE("Smoothie", "🥤", 0.85f),
    SPORTS_DRINK("Sports Drink", "🏃", 1.0f),
    COCONUT_WATER("Coconut Water", "🥥", 1.05f)
}

/**
 * Preset glass sizes for quick logging
 */
enum class GlassSize(val displayName: String, val amountMl: Int, val icon: String) {
    SMALL("Small", 150, "🥃"),
    MEDIUM("Glass", 250, "🥛"),
    LARGE("Large", 350, "🍺"),
    BOTTLE("Bottle", 500, "🍾"),
    SPORTS_BOTTLE("Sports Bottle", 750, "💪"),
    LITER("Liter", 1000, "🫗")
}

/**
 * Daily water tracking summary
 */
data class DailyWaterSummary(
    val date: String, // YYYY-MM-DD format
    val entries: List<WaterEntry> = emptyList(),
    val goalMl: Int = 2500, // Default 2.5L daily goal
    val totalMl: Int = 0,
    val effectiveHydrationMl: Int = 0 // Accounts for hydration factors
) {
    val progressPercent: Float
        get() = if (goalMl > 0) (totalMl.toFloat() / goalMl * 100).coerceAtMost(150f) else 0f

    val isGoalReached: Boolean
        get() = totalMl >= goalMl

    val remainingMl: Int
        get() = (goalMl - totalMl).coerceAtLeast(0)

    val glassesLogged: Int
        get() = entries.size

    val hydrationStatus: HydrationStatus
        get() = when {
            progressPercent >= 100 -> HydrationStatus.EXCELLENT
            progressPercent >= 75 -> HydrationStatus.GOOD
            progressPercent >= 50 -> HydrationStatus.MODERATE
            progressPercent >= 25 -> HydrationStatus.LOW
            else -> HydrationStatus.DEHYDRATED
        }
}

/**
 * Hydration status with friendly messages
 */
enum class HydrationStatus(
    val displayName: String,
    val icon: String,
    val message: String,
    val tip: String
) {
    EXCELLENT(
        "Excellent",
        "🌊",
        "You're fully hydrated!",
        "Great job keeping up with your water intake today!"
    ),
    GOOD(
        "Good",
        "💧",
        "Almost there!",
        "Just a few more glasses to reach your goal."
    ),
    MODERATE(
        "Moderate",
        "💦",
        "Keep going!",
        "You're halfway there. Try to drink more in the next few hours."
    ),
    LOW(
        "Low",
        "🫗",
        "Time to hydrate!",
        "Your body needs more water. Take a drink break."
    ),
    DEHYDRATED(
        "Needs Attention",
        "⚠️",
        "Let's catch up!",
        "Start with a full glass now and set reminders."
    )
}

/**
 * Water tracking settings
 */
data class WaterSettings(
    val dailyGoalMl: Int = 2500,
    val reminderEnabled: Boolean = true,
    val reminderIntervalMinutes: Int = 60,
    val reminderStartHour: Int = 8,
    val reminderEndHour: Int = 22,
    val preferredGlassSize: GlassSize = GlassSize.MEDIUM,
    val showNotifications: Boolean = true,
    val trackCaffeine: Boolean = true
)

/**
 * Weekly hydration statistics
 */
data class WeeklyHydrationStats(
    val weekStart: String,
    val dailySummaries: List<DailyWaterSummary>,
    val averageDailyMl: Int,
    val goalReachedDays: Int,
    val totalMl: Int,
    val bestDay: DailyWaterSummary?,
    val streakDays: Int
)

/**
 * Hydration insight generated from patterns
 */
data class HydrationInsight(
    val type: HydrationInsightType,
    val title: String,
    val message: String,
    val icon: String
)

enum class HydrationInsightType {
    STREAK,
    IMPROVEMENT,
    PATTERN,
    TIP,
    CELEBRATION
}

/**
 * UI State for Water Tracking Screen
 */
data class WaterTrackingUiState(
    val todaySummary: DailyWaterSummary = DailyWaterSummary(date = ""),
    val settings: WaterSettings = WaterSettings(),
    val weeklyStats: WeeklyHydrationStats? = null,
    val recentEntries: List<WaterEntry> = emptyList(),
    val insights: List<HydrationInsight> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val selectedSource: WaterSource = WaterSource.WATER,
    val customAmountMl: Int? = null
)
