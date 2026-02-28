package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Risk level for a habit
 */
enum class RiskLevel {
    LOW,      // High confidence habit will be completed
    MEDIUM,   // Some factors may prevent completion
    HIGH,     // Multiple risk factors detected
    CRITICAL  // Streak at immediate risk
}

/**
 * Types of risk factors that can affect habit completion
 */
enum class RiskFactorType {
    DAY_OF_WEEK,         // Historically poor performance on this day
    TIME_PRESSURE,       // Busy schedule detected from calendar
    WEATHER,             // Weather conditions unfavorable for habit
    STREAK_FATIGUE,      // Long streak may lead to burnout
    RECENT_MISSES,       // Recent pattern of missed completions
    CALENDAR_CONFLICT,   // Direct conflict with scheduled event
    LATE_IN_DAY,         // Running out of time to complete
    WEEKEND_PATTERN,     // Different weekend behavior
    RECOVERY_MODE        // User in recovery from broken streak
}

/**
 * A specific risk factor affecting a habit
 */
@Serializable
data class AtRiskFactor(
    val type: RiskFactorType,
    val severity: Float,        // 0.0 to 1.0
    val description: String,
    val suggestion: String?     // Actionable suggestion to mitigate
)

/**
 * Complete at-risk assessment for a habit
 */
@Serializable
data class HabitRiskAssessment(
    val habitId: String,
    val habitName: String,
    val habitEmoji: String,
    val riskLevel: RiskLevel,
    val riskScore: Float,       // 0.0 (no risk) to 1.0 (maximum risk)
    val riskFactors: List<AtRiskFactor>,
    val preemptiveSuggestion: String?,
    val optimalTimeToday: Long?, // Best time to complete today
    val assessedAt: Long = System.currentTimeMillis()
)

/**
 * Habit health metrics tracked over time
 */
@Serializable
data class HabitHealth(
    val habitId: String,
    val healthScore: Int,               // 0-100
    val trend: HealthTrend,
    val currentStreak: Int,
    val longestStreak: Int,
    val completionRateLast7Days: Float,
    val completionRateLast30Days: Float,
    val bestDay: String?,               // Best performing day of week
    val worstDay: String?,              // Worst performing day of week
    val bestTimeOfDay: String?,         // Morning, Afternoon, Evening
    val averageCompletionTime: Long?,   // Average time of day completed
    val lastCompletedAt: Long?,
    val missedInLastWeek: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class HealthTrend {
    IMPROVING,
    STABLE,
    DECLINING,
    NEW          // Not enough data
}

/**
 * Day of week statistics for pattern analysis
 */
@Serializable
data class DayOfWeekStats(
    val dayOfWeek: Int,         // 1 = Monday, 7 = Sunday
    val dayName: String,
    val completions: Int,
    val attempts: Int,
    val completionRate: Float,
    val averageCompletionTime: Long?    // Average time of day
)

/**
 * Historical pattern analysis for a habit
 */
@Serializable
data class HabitPattern(
    val habitId: String,
    val dayOfWeekStats: List<DayOfWeekStats>,
    val morningCompletionRate: Float,   // Before 12 PM
    val afternoonCompletionRate: Float, // 12 PM - 6 PM
    val eveningCompletionRate: Float,   // After 6 PM
    val weekdayCompletionRate: Float,
    val weekendCompletionRate: Float,
    val averageStreakLength: Float,
    val streakBreakPatterns: List<StreakBreakPattern>,
    val analyzedAt: Long = System.currentTimeMillis()
)

/**
 * Pattern of when streaks tend to break
 */
@Serializable
data class StreakBreakPattern(
    val dayOfWeek: Int,
    val streakLengthAtBreak: Int,
    val timeOfLastCompletion: Long?,
    val occurredAt: Long
)

/**
 * Weather conditions that may affect habits
 */
@Serializable
data class WeatherCondition(
    val temperature: Float,         // Celsius
    val condition: WeatherType,
    val description: String,
    val humidity: Int,
    val windSpeed: Float,
    val isOutdoorFriendly: Boolean,
    val fetchedAt: Long = System.currentTimeMillis()
)

enum class WeatherType {
    SUNNY,
    CLOUDY,
    RAINY,
    STORMY,
    SNOWY,
    WINDY,
    FOGGY,
    EXTREME_HEAT,
    EXTREME_COLD,
    UNKNOWN
}

/**
 * User's at-risk notification preferences
 */
@Serializable
data class AtRiskNotificationSettings(
    val enabled: Boolean = true,
    val notifyOnHighRisk: Boolean = true,
    val notifyOnMediumRisk: Boolean = false,
    val includeWeatherAlerts: Boolean = true,
    val includeCalendarAlerts: Boolean = true,
    val notifyHoursBeforeEndOfDay: Int = 4,  // Hours before midnight to send "last chance" alert
    val preferredReminderTime: String? = null // e.g., "09:00" for morning risk briefing
)

/**
 * Aggregated risk summary for the day
 */
@Serializable
data class DailyRiskSummary(
    val date: Long,
    val overallRiskLevel: RiskLevel,
    val habitsAtRisk: List<HabitRiskAssessment>,
    val totalHabits: Int,
    val highRiskCount: Int,
    val mediumRiskCount: Int,
    val weatherImpact: WeatherCondition?,
    val busyScheduleDetected: Boolean,
    val calendarConflicts: Int,
    val recommendations: List<String>,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * At-risk alert to show to user
 */
@Serializable
data class AtRiskAlert(
    val id: String,
    val habitId: String,
    val habitName: String,
    val habitEmoji: String,
    val riskLevel: RiskLevel,
    val title: String,
    val message: String,
    val actionSuggestion: String?,
    val suggestedTime: Long?,
    val expiresAt: Long,
    val dismissed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
