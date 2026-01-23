package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Biometric Correlation Dashboard
 * Connects Oura Ring / WHOOP / Apple Health data for habit impact analysis
 *
 * Shows the "WHY" behind habits with quantified health data:
 * - Sleep quality vs. habits
 * - HRV trends vs. habit streaks
 * - Recovery score recommendations
 */
@Serializable
data class BiometricData(
    val sleepRecords: List<SleepBiometricRecord> = emptyList(),
    val hrvRecords: List<HrvRecord> = emptyList(),
    val activityRecords: List<ActivityRecord> = emptyList(),
    val correlations: List<BiometricCorrelation> = emptyList(),
    val insights: List<BiometricInsight> = emptyList(),
    val connectedDevices: List<ConnectedDevice> = emptyList(),
    val lastSyncedAt: String? = null
)

@Serializable
data class SleepBiometricRecord(
    val date: String,
    val durationMinutes: Int,
    val deepSleepMinutes: Int = 0,
    val remSleepMinutes: Int = 0,
    val lightSleepMinutes: Int = 0,
    val awakeMinutes: Int = 0,
    val efficiency: Float = 0f,  // 0-100
    val latencyMinutes: Int = 0, // Time to fall asleep
    val source: BiometricSource = BiometricSource.MANUAL
)

@Serializable
data class HrvRecord(
    val date: String,
    val timestamp: String,
    val avgHrv: Float,           // Average HRV (ms)
    val minHrv: Float = 0f,
    val maxHrv: Float = 0f,
    val restingHeartRate: Int = 0,
    val source: BiometricSource = BiometricSource.MANUAL
)

@Serializable
data class ActivityRecord(
    val date: String,
    val steps: Int = 0,
    val activeMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val workouts: List<WorkoutRecord> = emptyList(),
    val source: BiometricSource = BiometricSource.MANUAL
)

@Serializable
data class WorkoutRecord(
    val type: String,        // "running", "strength", "yoga", etc.
    val durationMinutes: Int,
    val caloriesBurned: Int = 0,
    val avgHeartRate: Int = 0,
    val startTime: String,
    val endTime: String
)

@Serializable
enum class BiometricSource(val displayName: String) {
    MANUAL("Manual Entry"),
    APPLE_HEALTH("Apple Health"),
    GOOGLE_FIT("Google Fit"),
    HEALTH_CONNECT("Health Connect"),
    OURA("Oura Ring"),
    WHOOP("WHOOP"),
    FITBIT("Fitbit"),
    GARMIN("Garmin")
}

@Serializable
data class ConnectedDevice(
    val source: BiometricSource,
    val deviceName: String,
    val connectedAt: String,
    val lastSyncedAt: String,
    val isActive: Boolean = true,
    val syncPermissions: List<SyncPermission> = emptyList()
)

@Serializable
enum class SyncPermission {
    SLEEP,
    HRV,
    HEART_RATE,
    STEPS,
    WORKOUTS,
    CALORIES
}

/**
 * Correlation between biometric data and habit performance
 */
@Serializable
data class BiometricCorrelation(
    val id: String,
    val habitId: String,
    val habitName: String,
    val biometricType: BiometricType,
    val correlationStrength: Float,      // -1 to 1
    val direction: CorrelationDirection,
    val description: String,
    val dataPoints: Int,
    val confidence: Float,               // 0-1
    val recommendation: String,
    val visualData: CorrelationVisualData? = null
)

@Serializable
enum class BiometricType(val displayName: String, val emoji: String) {
    SLEEP_DURATION("Sleep Duration", "😴"),
    SLEEP_QUALITY("Sleep Quality", "💤"),
    DEEP_SLEEP("Deep Sleep", "🌙"),
    HRV("Heart Rate Variability", "💗"),
    RESTING_HR("Resting Heart Rate", "❤️"),
    STEPS("Daily Steps", "👟"),
    ACTIVE_MINUTES("Active Minutes", "🏃"),
    RECOVERY_SCORE("Recovery Score", "🔋")
}

@Serializable
enum class CorrelationDirection {
    POSITIVE,     // Higher biometric = higher habit success
    NEGATIVE,     // Higher biometric = lower habit success
    NEUTRAL       // No significant correlation
}

@Serializable
data class CorrelationVisualData(
    val xAxisLabel: String,
    val yAxisLabel: String,
    val dataPoints: List<CorrelationDataPoint>,
    val trendLine: TrendLine? = null
)

@Serializable
data class CorrelationDataPoint(
    val x: Float,
    val y: Float,
    val label: String? = null
)

@Serializable
data class TrendLine(
    val slope: Float,
    val intercept: Float,
    val rSquared: Float
)

/**
 * Biometric-driven insight
 */
@Serializable
data class BiometricInsight(
    val id: String,
    val type: BiometricInsightType,
    val title: String,
    val description: String,
    val emoji: String,
    val actionable: Boolean = true,
    val recommendation: String? = null,
    val relatedHabits: List<String> = emptyList(),
    val biometricType: BiometricType,
    val severity: InsightSeverity = InsightSeverity.INFO,
    val createdAt: String,
    val expiresAt: String? = null
)

@Serializable
enum class BiometricInsightType {
    SLEEP_IMPACT,          // Sleep affecting habits
    HRV_TREND,             // HRV changes over time
    RECOVERY_WARNING,      // Low recovery detected
    HABIT_BENEFIT,         // Habit improving biometrics
    OVERTRAINING_RISK,     // Too much activity
    BURNOUT_WARNING,       // Combined stress signals
    CELEBRATION            // Positive milestone
}

@Serializable
enum class InsightSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ALERT
}

/**
 * Dashboard summary for quick viewing
 */
@Serializable
data class BiometricDashboardSummary(
    val todaySleepScore: Int = 0,        // 0-100
    val todayRecoveryScore: Int = 0,     // 0-100
    val weeklyHrvTrend: Float = 0f,      // % change
    val avgSleepDuration: Float = 0f,    // hours
    val avgDeepSleep: Float = 0f,        // hours
    val habitImpactScore: Int = 0,       // 0-100, how habits affect biometrics
    val topCorrelation: BiometricCorrelation? = null,
    val latestInsight: BiometricInsight? = null
)

/**
 * Pre-built insight templates
 */
object BiometricInsightTemplates {

    fun sleepHabitImpact(habitName: String, sleepHours: Float, completionDrop: Int): BiometricInsight {
        return BiometricInsight(
            id = "bio_sleep_${habitName}_${System.currentTimeMillis()}",
            type = BiometricInsightType.SLEEP_IMPACT,
            title = "Sleep affects your $habitName",
            description = "When you get less than ${"%.1f".format(sleepHours)} hours of sleep, your $habitName completion drops by $completionDrop%.",
            emoji = "😴",
            recommendation = "Prioritize 7+ hours of sleep for better $habitName consistency.",
            relatedHabits = listOf(habitName.lowercase()),
            biometricType = BiometricType.SLEEP_DURATION,
            severity = InsightSeverity.WARNING,
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }

    fun hrvImprovement(habitName: String, streakDays: Int, hrvImprovement: Float): BiometricInsight {
        return BiometricInsight(
            id = "bio_hrv_${habitName}_${System.currentTimeMillis()}",
            type = BiometricInsightType.HABIT_BENEFIT,
            title = "Your $habitName streak is improving HRV!",
            description = "After $streakDays days of consistent $habitName, your HRV has improved by ${"%.1f".format(hrvImprovement)}%.",
            emoji = "💗",
            actionable = false,
            relatedHabits = listOf(habitName.lowercase()),
            biometricType = BiometricType.HRV,
            severity = InsightSeverity.SUCCESS,
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }

    fun recoveryWarning(recoveryScore: Int, missedHabits: List<String>): BiometricInsight {
        return BiometricInsight(
            id = "bio_recovery_${System.currentTimeMillis()}",
            type = BiometricInsightType.RECOVERY_WARNING,
            title = "Low recovery detected",
            description = "Your recovery score is $recoveryScore%. Consider taking it easier today.",
            emoji = "⚠️",
            recommendation = "Focus on rest and low-intensity activities. ${if (missedHabits.isNotEmpty()) "Skip intense workouts." else ""}",
            relatedHabits = missedHabits,
            biometricType = BiometricType.RECOVERY_SCORE,
            severity = InsightSeverity.ALERT,
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }

    fun burnoutWarning(elevatedRhr: Int, daysElevated: Int): BiometricInsight {
        return BiometricInsight(
            id = "bio_burnout_${System.currentTimeMillis()}",
            type = BiometricInsightType.BURNOUT_WARNING,
            title = "Burnout risk detected",
            description = "Your resting heart rate has been elevated ($elevatedRhr+ bpm) for $daysElevated days. Combined with missed habits, this suggests stress accumulation.",
            emoji = "🔥",
            recommendation = "Take a recovery day. Prioritize sleep, hydration, and relaxation.",
            biometricType = BiometricType.RESTING_HR,
            severity = InsightSeverity.ALERT,
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }

    fun deepSleepCelebration(avgDeepSleep: Float, targetDeepSleep: Float = 1.5f): BiometricInsight {
        return BiometricInsight(
            id = "bio_deep_sleep_${System.currentTimeMillis()}",
            type = BiometricInsightType.CELEBRATION,
            title = "Excellent deep sleep!",
            description = "You're averaging ${"%.1f".format(avgDeepSleep)} hours of deep sleep, above the ${"%.1f".format(targetDeepSleep)}hr target.",
            emoji = "🌙",
            actionable = false,
            biometricType = BiometricType.DEEP_SLEEP,
            severity = InsightSeverity.SUCCESS,
            createdAt = kotlinx.datetime.Clock.System.now().toString()
        )
    }
}

/**
 * Analyzer for biometric-habit correlations
 */
object BiometricAnalyzer {

    /**
     * Calculate recovery score based on available biometrics
     */
    fun calculateRecoveryScore(
        hrvRecord: HrvRecord?,
        sleepRecord: SleepBiometricRecord?,
        previousDayActivity: ActivityRecord?
    ): Int {
        var score = 50 // Base score

        // HRV contribution (40%)
        if (hrvRecord != null) {
            val hrvBaseline = 50f // Assumed baseline
            val hrvDiff = (hrvRecord.avgHrv - hrvBaseline) / hrvBaseline
            score += (hrvDiff * 40).toInt().coerceIn(-20, 20)
        }

        // Sleep contribution (40%)
        if (sleepRecord != null) {
            val sleepHours = sleepRecord.durationMinutes / 60f
            val sleepScore = when {
                sleepHours >= 8 -> 20
                sleepHours >= 7 -> 15
                sleepHours >= 6 -> 5
                else -> -10
            }
            val efficiencyBonus = ((sleepRecord.efficiency - 80) / 20 * 10).toInt().coerceIn(-5, 10)
            score += sleepScore + efficiencyBonus
        }

        // Activity load from previous day (20%)
        if (previousDayActivity != null) {
            val activeMinutes = previousDayActivity.activeMinutes
            val activityPenalty = when {
                activeMinutes > 120 -> -15  // Very high load
                activeMinutes > 90 -> -5    // High load
                activeMinutes in 30..60 -> 5 // Optimal
                else -> 0
            }
            score += activityPenalty
        }

        return score.coerceIn(0, 100)
    }

    /**
     * Find correlations between habits and biometric data
     */
    fun findCorrelations(
        habitCompletions: Map<String, List<Boolean>>, // habitId -> daily completions
        sleepRecords: List<SleepBiometricRecord>,
        hrvRecords: List<HrvRecord>
    ): List<BiometricCorrelation> {
        val correlations = mutableListOf<BiometricCorrelation>()

        // Only analyze if we have enough data
        if (sleepRecords.size < 7 || habitCompletions.values.all { it.size < 7 }) {
            return correlations
        }

        habitCompletions.forEach { (habitId, completions) ->
            // Sleep duration correlation
            if (completions.size >= sleepRecords.size) {
                val sleepHoursOnCompleteDays = mutableListOf<Float>()
                val sleepHoursOnMissedDays = mutableListOf<Float>()

                completions.take(sleepRecords.size).forEachIndexed { index, completed ->
                    val sleepHours = sleepRecords[index].durationMinutes / 60f
                    if (completed) {
                        sleepHoursOnCompleteDays.add(sleepHours)
                    } else {
                        sleepHoursOnMissedDays.add(sleepHours)
                    }
                }

                if (sleepHoursOnCompleteDays.isNotEmpty() && sleepHoursOnMissedDays.isNotEmpty()) {
                    val avgSleepComplete = sleepHoursOnCompleteDays.average().toFloat()
                    val avgSleepMissed = sleepHoursOnMissedDays.average().toFloat()
                    val diff = avgSleepComplete - avgSleepMissed

                    if (kotlin.math.abs(diff) > 0.5f) {
                        correlations.add(
                            BiometricCorrelation(
                                id = "corr_sleep_$habitId",
                                habitId = habitId,
                                habitName = getHabitDisplayName(habitId),
                                biometricType = BiometricType.SLEEP_DURATION,
                                correlationStrength = (diff / 2f).coerceIn(-1f, 1f),
                                direction = if (diff > 0) CorrelationDirection.POSITIVE else CorrelationDirection.NEGATIVE,
                                description = "On days you complete ${getHabitDisplayName(habitId)}, you average ${"%.1f".format(avgSleepComplete)}hrs of sleep vs ${"%.1f".format(avgSleepMissed)}hrs when missed.",
                                dataPoints = sleepRecords.size,
                                confidence = minOf(sleepRecords.size / 30f, 1f),
                                recommendation = if (diff > 0) "More sleep = better ${getHabitDisplayName(habitId)} success" else "Sleep may not be the limiting factor for ${getHabitDisplayName(habitId)}"
                            )
                        )
                    }
                }
            }
        }

        return correlations
    }

    private fun getHabitDisplayName(habitId: String): String {
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
