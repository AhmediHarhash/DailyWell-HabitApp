package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * User Behavior Profile
 * Tracks user patterns and preferences for AI personalization
 *
 * The AI coach learns from user behavior to provide personalized guidance:
 * - Chronotype: Morning person vs night owl
 * - Motivation style: Supportive vs direct vs analytical
 * - Streak recovery rate: How often they bounce back after missing
 * - Habit correlations: Which habits influence each other
 * - Attitude score: Current engagement level
 *
 * This data is injected into Claude prompts for personalized coaching.
 */
@Serializable
data class UserBehaviorProfile(
    val userId: String,
    val chronotype: Chronotype = Chronotype.FLEXIBLE,
    val motivationStyle: MotivationStyle = MotivationStyle.SUPPORTIVE,
    val streakRecoveryRate: Float = 0.5f,  // 0-1, how often they recover after miss
    val averageCheckInHour: Int = 12,  // 0-23, average hour of day for check-ins
    val preferredNotificationHours: List<Int> = listOf(8, 20),  // Morning and evening
    val habitCorrelations: List<HabitCorrelationData> = emptyList(),
    val attitudeScore: Float = 0f,  // -1 to 1, negative = frustrated, positive = engaged
    val engagementLevel: EngagementLevel = EngagementLevel.MODERATE,
    val weekdayVsWeekendRatio: Float = 1.0f,  // >1 = better on weekdays
    val mostProductiveDay: Int = 1,  // 1-7, Monday-Sunday
    val leastProductiveDay: Int = 7,
    val checkInConsistency: Float = 0.5f,  // 0-1, how consistent is check-in timing
    val featureUsage: FeatureUsageProfile = FeatureUsageProfile(),
    val totalCompletions: Int = 0,
    val totalMissedDays: Int = 0,
    val currentMissedStreak: Int = 0,  // Days since last completion
    val lastUpdated: String = "",
    val profileVersion: Int = 1
) {
    /**
     * Get personalized prompt context for AI coaching
     */
    fun toPromptContext(): String {
        val chronotypeDesc = when (chronotype) {
            Chronotype.MORNING_PERSON -> "Morning person (most active before 10 AM)"
            Chronotype.NIGHT_OWL -> "Night owl (most active after 6 PM)"
            Chronotype.FLEXIBLE -> "Flexible schedule (no strong preference)"
        }

        val motivationDesc = when (motivationStyle) {
            MotivationStyle.SUPPORTIVE -> "Prefers supportive, encouraging guidance"
            MotivationStyle.DIRECT -> "Prefers direct, action-focused guidance"
            MotivationStyle.ANALYTICAL -> "Prefers data-driven, analytical insights"
        }

        val attitudeDesc = when {
            attitudeScore < -0.5f -> "Currently frustrated (may need extra support)"
            attitudeScore < 0f -> "Slightly disengaged (gentle encouragement helps)"
            attitudeScore < 0.5f -> "Moderately engaged (on track)"
            else -> "Highly engaged and motivated"
        }

        val recoveryDesc = when {
            streakRecoveryRate > 0.8f -> "Excellent recovery (${(streakRecoveryRate * 100).toInt()}% bounce back rate)"
            streakRecoveryRate > 0.5f -> "Good recovery (${(streakRecoveryRate * 100).toInt()}% bounce back rate)"
            else -> "Struggles with recovery after misses (${(streakRecoveryRate * 100).toInt()}%)"
        }

        val topCorrelation = habitCorrelations.maxByOrNull { kotlin.math.abs(it.correlation) }
        val correlationDesc = topCorrelation?.let {
            val direction = if (it.correlation > 0) "improves" else "decreases"
            "${it.habit1} $direction ${it.habit2} (${(kotlin.math.abs(it.correlation) * 100).toInt()}% correlation)"
        } ?: "Still learning habit correlations"

        return """
User Profile:
- Chronotype: $chronotypeDesc
- Motivation style: $motivationDesc
- Streak recovery: $recoveryDesc
- Current attitude: $attitudeDesc
- Strong correlation: $correlationDesc
- Best day: ${getDayName(mostProductiveDay)}, Challenging day: ${getDayName(leastProductiveDay)}
        """.trimIndent()
    }

    private fun getDayName(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Unknown"
    }
}

@Serializable
enum class Chronotype(val displayName: String, val description: String) {
    MORNING_PERSON("Morning Person", "Most productive before 10 AM"),
    NIGHT_OWL("Night Owl", "Most productive after 6 PM"),
    FLEXIBLE("Flexible", "Adaptable to any schedule")
}

@Serializable
enum class MotivationStyle(val displayName: String, val description: String) {
    SUPPORTIVE("Supportive", "Prefers gentle encouragement and celebration of small wins"),
    DIRECT("Direct", "Prefers clear action items and accountability"),
    ANALYTICAL("Analytical", "Prefers data-driven insights and progress metrics")
}

@Serializable
enum class EngagementLevel(val displayName: String, val scoreRange: String) {
    HIGH("Highly Engaged", "0.5 to 1.0"),
    MODERATE("Moderately Engaged", "0.0 to 0.5"),
    LOW("Low Engagement", "-0.5 to 0.0"),
    DISENGAGED("Disengaged", "-1.0 to -0.5")
}

/**
 * Habit correlation data - how one habit affects another
 */
@Serializable
data class HabitCorrelationData(
    val habit1: String,  // e.g., "exercise"
    val habit2: String,  // e.g., "sleep"
    val correlation: Float,  // -1 to 1, positive = completing habit1 helps habit2
    val sampleSize: Int = 0,  // Number of days analyzed
    val confidence: Float = 0f  // 0-1, statistical confidence
)

/**
 * Track which features the user engages with
 */
@Serializable
data class FeatureUsageProfile(
    val usesAICoaching: Boolean = false,
    val aiCoachingFrequency: Float = 0f,  // Messages per day
    val usesPatternInsights: Boolean = false,
    val insightsViewCount: Int = 0,
    val usesVoiceInput: Boolean = false,
    val usesTTS: Boolean = false,
    val preferredCoachId: String? = null,
    val usesCalendarIntegration: Boolean = false,
    val usesSocialFeatures: Boolean = false,
    val usesWidgets: Boolean = false,
    val notificationResponseRate: Float = 0f,  // 0-1, how often they act on notifications
    val averageSessionDurationSeconds: Int = 0,
    val sessionsPerDay: Float = 0f,
    val lastFeatureUsed: String = ""
)

/**
 * Behavior event for tracking
 */
@Serializable
data class BehaviorEvent(
    val id: String,
    val userId: String,
    val eventType: BehaviorEventType,
    val habitId: String? = null,
    val value: Float = 0f,  // Event-specific value
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: String
)

@Serializable
enum class BehaviorEventType {
    // Habit events
    HABIT_COMPLETED,
    HABIT_SKIPPED,
    HABIT_DISMISSED,
    STREAK_BROKEN,
    STREAK_RECOVERED,

    // AI interaction events
    AI_MESSAGE_SENT,
    AI_RESPONSE_RATED,
    AI_COACH_CHANGED,

    // Feature usage events
    INSIGHT_VIEWED,
    INSIGHT_DISMISSED,
    NOTIFICATION_TAPPED,
    NOTIFICATION_DISMISSED,
    TTS_USED,
    VOICE_INPUT_USED,

    // Session events
    APP_OPENED,
    SESSION_ENDED,
    SCREEN_VIEWED,

    // Attitude indicators
    EXPRESSED_FRUSTRATION,  // Detected in AI chat
    EXPRESSED_MOTIVATION,
    CELEBRATION_TRIGGERED
}

/**
 * Daily behavior summary for analytics
 */
@Serializable
data class DailyBehaviorSummary(
    val date: String,
    val userId: String,
    val habitsCompleted: Int,
    val habitsMissed: Int,
    val completionRate: Float,
    val firstCheckInHour: Int?,
    val lastCheckInHour: Int?,
    val aiMessagesCount: Int,
    val attitudeIndicator: Float,  // -1 to 1
    val sessionCount: Int,
    val totalSessionSeconds: Int
)

/**
 * Weekly pattern analysis
 */
@Serializable
data class WeeklyPatternAnalysis(
    val weekStartDate: String,
    val userId: String,
    val dayPatterns: Map<Int, Float>,  // Day of week (1-7) -> completion rate
    val timePatterns: Map<Int, Float>,  // Hour (0-23) -> activity level
    val strongestDay: Int,
    val weakestDay: Int,
    val peakHour: Int,
    val consistencyScore: Float,  // 0-1, how consistent across the week
    val weekOverWeekTrend: Float  // -1 to 1, improvement vs previous week
)
