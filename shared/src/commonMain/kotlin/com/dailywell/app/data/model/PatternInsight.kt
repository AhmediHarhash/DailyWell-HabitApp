package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * AI Pattern Recognition & Insights
 * Discovers hidden correlations between habits, timing, and success rates
 *
 * Key insight: 58% of 2025 apps have AI - it's table stakes
 * Users love data-driven personalization
 */
@Serializable
data class PatternInsightsData(
    val insights: List<PatternInsight> = emptyList(),
    val correlations: List<HabitCorrelation> = emptyList(),
    val predictions: List<SuccessPrediction> = emptyList(),
    val weeklyReport: WeeklyInsightReport? = null,
    val lastAnalyzedAt: String? = null
)

@Serializable
data class PatternInsight(
    val id: String,
    val type: InsightType,
    val title: String,
    val description: String,
    val emoji: String,
    val significance: Float,          // 0-1, how significant this insight is
    val actionable: Boolean = true,   // Can user act on it?
    val recommendation: String? = null,
    val relatedHabits: List<String> = emptyList(),
    val createdAt: String,
    val expiresAt: String? = null,    // Some insights are time-sensitive
    val isDismissed: Boolean = false
)

@Serializable
enum class InsightType(val label: String) {
    CORRELATION("Correlation"),           // Two things affect each other
    TIMING_PATTERN("Timing Pattern"),     // Best/worst times
    STREAK_RISK("Streak Risk"),           // Danger of losing streak
    SUCCESS_FACTOR("Success Factor"),     // What helps completion
    RECOVERY_OPPORTUNITY("Recovery"),     // Chance to bounce back
    CELEBRATION("Celebration"),           // Positive milestone
    WEEKLY_TREND("Weekly Trend"),         // This week vs last
    IMPROVEMENT("Improvement")            // Getting better at something
}

/**
 * Correlation between two habits or habit and external factor
 * Example: "When you sleep <6.5 hours, workout completion drops 67%"
 */
@Serializable
data class HabitCorrelation(
    val id: String,
    val factorA: CorrelationFactor,
    val factorB: CorrelationFactor,
    val correlationType: CorrelationType,
    val strength: Float,              // -1 to 1 (negative = inverse correlation)
    val confidence: Float,            // 0-1, how confident we are
    val sampleSize: Int,              // Number of data points
    val description: String,
    val implication: String           // What this means for the user
)

@Serializable
data class CorrelationFactor(
    val type: FactorType,
    val id: String,                   // habitId or "sleep", "weather", etc.
    val label: String,
    val threshold: Float? = null,     // e.g., "<6.5 hours", ">30 minutes"
    val thresholdDirection: ThresholdDirection? = null
)

@Serializable
enum class FactorType {
    HABIT,
    SLEEP_DURATION,
    SLEEP_QUALITY,
    TIME_OF_DAY,
    DAY_OF_WEEK,
    WEEKEND_VS_WEEKDAY,
    STREAK_LENGTH,
    MOOD,
    PREVIOUS_HABIT
}

@Serializable
enum class ThresholdDirection {
    ABOVE,
    BELOW,
    EQUALS
}

@Serializable
enum class CorrelationType {
    POSITIVE,      // When A is high, B is high
    NEGATIVE,      // When A is high, B is low
    PREREQUISITE,  // A must happen before B succeeds
    COMPLEMENTARY  // A and B work well together
}

/**
 * Prediction of future success or failure
 */
@Serializable
data class SuccessPrediction(
    val id: String,
    val habitId: String,
    val predictedDate: String,
    val successProbability: Float,    // 0-1
    val riskFactors: List<RiskFactor> = emptyList(),
    val positiveFactors: List<String> = emptyList(),
    val recommendation: String
)

@Serializable
data class RiskFactor(
    val factor: String,
    val impact: Float,                // How much it affects probability
    val suggestion: String
)

/**
 * Weekly insight report for users
 */
@Serializable
data class WeeklyInsightReport(
    val weekStartDate: String,
    val weekEndDate: String,
    val overallScore: Float,          // 0-100
    val comparedToLastWeek: Float,    // +/- percentage
    val topAchievement: String,
    val topChallenge: String,
    val keyInsights: List<String>,
    val nextWeekFocus: String,
    val habitScores: Map<String, Float>
)

/**
 * Pre-computed insight templates based on common patterns
 */
object InsightTemplates {

    fun sleepHabitCorrelation(habitId: String, habitName: String, sleepHours: Float, completionDrop: Int): PatternInsight {
        return PatternInsight(
            id = "insight_sleep_${habitId}_${System.currentTimeMillis()}",
            type = InsightType.CORRELATION,
            title = "Sleep affects your $habitName",
            description = "When you get less than $sleepHours hours of sleep, your $habitName completion drops by $completionDrop%",
            emoji = "😴",
            significance = 0.8f,
            recommendation = "Prioritize sleep before important habit days",
            relatedHabits = listOf(habitId, "sleep"),
            createdAt = kotlinx.datetime.Clock.System.now().toString(),
            expiresAt = null
        )
    }

    fun weekendPattern(habitId: String, habitName: String, weekendMultiplier: Float): PatternInsight {
        val isBetter = weekendMultiplier > 1.0f
        return PatternInsight(
            id = "insight_weekend_${habitId}_${System.currentTimeMillis()}",
            type = InsightType.TIMING_PATTERN,
            title = if (isBetter) "Weekends are your $habitName time!" else "Weekdays work better for $habitName",
            description = if (isBetter)
                "You complete $habitName ${String.format("%.1f", weekendMultiplier)}x more often on weekends"
            else
                "Weekday completion is ${String.format("%.1f", 1/weekendMultiplier)}x better than weekends",
            emoji = if (isBetter) "🌅" else "💼",
            significance = 0.6f,
            recommendation = if (isBetter)
                "Consider building on weekend momentum"
            else
                "Protect your weekday routine",
            relatedHabits = listOf(habitId),
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }

    fun streakRiskWarning(habitId: String, habitName: String, daysMissed: Int): PatternInsight {
        return PatternInsight(
            id = "insight_risk_${habitId}_${System.currentTimeMillis()}",
            type = InsightType.STREAK_RISK,
            title = "Your $habitName streak needs attention",
            description = "You've missed $habitName for $daysMissed days. Your patterns suggest today is critical.",
            emoji = "⚠️",
            significance = 0.9f,
            recommendation = "Even a minimal completion today can rebuild momentum",
            relatedHabits = listOf(habitId),
            createdAt = kotlinx.datetime.Clock.System.now().toString(),
            expiresAt = kotlinx.datetime.Clock.System.now().toString() // Expires same day
        )
    }

    fun celebrateStreak(habitId: String, habitName: String, streakDays: Int): PatternInsight {
        val emoji = when {
            streakDays >= 66 -> "🏆"
            streakDays >= 30 -> "🔥"
            streakDays >= 14 -> "⭐"
            streakDays >= 7 -> "✨"
            else -> "🎉"
        }
        val milestone = when {
            streakDays >= 66 -> "habit is automatic now!"
            streakDays >= 30 -> "one month strong!"
            streakDays >= 14 -> "two weeks of consistency!"
            streakDays >= 7 -> "one week complete!"
            else -> "great start!"
        }
        return PatternInsight(
            id = "insight_celebrate_${habitId}_${streakDays}",
            type = InsightType.CELEBRATION,
            title = "$streakDays day $habitName streak - $milestone",
            description = "Your $habitName streak is building real neural pathways. Research shows ${if (streakDays >= 66) "habits become automatic after 66 days" else "${66 - streakDays} more days until it's automatic"}.",
            emoji = emoji,
            significance = 0.7f,
            actionable = false,
            relatedHabits = listOf(habitId),
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }

    fun habitStackingSuccess(anchorHabit: String, stackedHabit: String, successRate: Float): PatternInsight {
        return PatternInsight(
            id = "insight_stacking_${anchorHabit}_${stackedHabit}",
            type = InsightType.SUCCESS_FACTOR,
            title = "Habit stacking is working!",
            description = "When you complete $anchorHabit first, you complete $stackedHabit ${String.format("%.0f", successRate * 100)}% of the time.",
            emoji = "🔗",
            significance = 0.75f,
            recommendation = "Keep this chain going",
            relatedHabits = listOf(anchorHabit, stackedHabit),
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }

    fun morningVsEveningPattern(habitId: String, habitName: String, morningSuccessRate: Float, eveningSuccessRate: Float): PatternInsight {
        val isMorningBetter = morningSuccessRate > eveningSuccessRate
        return PatternInsight(
            id = "insight_timing_${habitId}",
            type = InsightType.TIMING_PATTERN,
            title = "${if (isMorningBetter) "Morning" else "Evening"} is your $habitName time",
            description = "Your ${if (isMorningBetter) "morning" else "evening"} completion rate for $habitName is ${String.format("%.0f", (if (isMorningBetter) morningSuccessRate else eveningSuccessRate) * 100)}% vs ${String.format("%.0f", (if (isMorningBetter) eveningSuccessRate else morningSuccessRate) * 100)}% in the ${if (isMorningBetter) "evening" else "morning"}.",
            emoji = if (isMorningBetter) "🌅" else "🌙",
            significance = 0.65f,
            recommendation = "Schedule $habitName reminders for the ${if (isMorningBetter) "morning" else "evening"}",
            relatedHabits = listOf(habitId),
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }

    fun weeklyImprovement(habitId: String, habitName: String, improvementPercent: Float): PatternInsight {
        return PatternInsight(
            id = "insight_improvement_${habitId}_weekly",
            type = InsightType.IMPROVEMENT,
            title = "$habitName is ${String.format("%.0f", improvementPercent)}% better this week!",
            description = "Your $habitName consistency improved compared to last week. Small progress compounds into big results.",
            emoji = "📈",
            significance = 0.6f,
            actionable = false,
            relatedHabits = listOf(habitId),
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }
}

/**
 * AI Pattern Analyzer - computes insights from user data
 */
object PatternAnalyzer {

    /**
     * Analyze habit completion data and generate insights
     */
    fun analyzePatterns(
        completionHistory: Map<String, List<CompletionRecord>>,
        sleepData: List<SleepRecord>? = null
    ): List<PatternInsight> {
        val insights = mutableListOf<PatternInsight>()

        // Analyze each habit
        completionHistory.forEach { (habitId, records) ->
            if (records.size >= 7) {
                // Weekend vs weekday analysis
                val weekendCompletions = records.count { it.isWeekend && it.completed }
                val weekendTotal = records.count { it.isWeekend }
                val weekdayCompletions = records.count { !it.isWeekend && it.completed }
                val weekdayTotal = records.count { !it.isWeekend }

                if (weekendTotal > 0 && weekdayTotal > 0) {
                    val weekendRate = weekendCompletions.toFloat() / weekendTotal
                    val weekdayRate = weekdayCompletions.toFloat() / weekdayTotal

                    if (kotlin.math.abs(weekendRate - weekdayRate) > 0.2f) {
                        insights.add(
                            InsightTemplates.weekendPattern(
                                habitId,
                                getHabitName(habitId),
                                weekendRate / weekdayRate.coerceAtLeast(0.1f)
                            )
                        )
                    }
                }

                // Streak analysis
                val currentStreak = calculateCurrentStreak(records)
                if (currentStreak in listOf(7, 14, 21, 30, 66)) {
                    insights.add(
                        InsightTemplates.celebrateStreak(habitId, getHabitName(habitId), currentStreak)
                    )
                }

                // Weekly improvement
                val thisWeekRate = records.takeLast(7).count { it.completed } / 7f
                val lastWeekRate = if (records.size >= 14) {
                    records.dropLast(7).takeLast(7).count { it.completed } / 7f
                } else null

                if (lastWeekRate != null && thisWeekRate > lastWeekRate + 0.1f) {
                    val improvement = ((thisWeekRate - lastWeekRate) / lastWeekRate.coerceAtLeast(0.1f)) * 100
                    insights.add(
                        InsightTemplates.weeklyImprovement(habitId, getHabitName(habitId), improvement)
                    )
                }
            }
        }

        // Sort by significance
        return insights.sortedByDescending { it.significance }
    }

    private fun calculateCurrentStreak(records: List<CompletionRecord>): Int {
        var streak = 0
        for (record in records.sortedByDescending { it.date }) {
            if (record.completed) streak++ else break
        }
        return streak
    }

    private fun getHabitName(habitId: String): String {
        return when (habitId) {
            "sleep" -> "Rest"
            "water" -> "Hydration"
            "move" -> "Movement"
            "vegetables" -> "Nutrition"
            "calm" -> "Mindfulness"
            "connect" -> "Connection"
            "unplug" -> "Digital Detox"
            else -> habitId.replaceFirstChar { it.uppercase() }
        }
    }
}

/**
 * Simple completion record for analysis
 */
@Serializable
data class CompletionRecord(
    val habitId: String,
    val date: String,
    val completed: Boolean,
    val completedAt: String? = null,
    val isWeekend: Boolean = false
)

/**
 * Sleep record for correlation analysis
 */
@Serializable
data class SleepRecord(
    val date: String,
    val durationMinutes: Int,
    val quality: Float? = null  // 0-1
)
