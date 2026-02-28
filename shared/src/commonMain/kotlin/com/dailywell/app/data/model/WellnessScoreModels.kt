package com.dailywell.app.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Feature #9: Weekly Wellness Score
 * Calculates a 0-100 wellness score based on habit performance
 */

@Serializable
data class WeeklyWellnessScore(
    val userId: String,
    val weekStart: String, // ISO date (e.g., "2026-02-03")
    val weekEnd: String,   // ISO date (e.g., "2026-02-09")
    val overallScore: Int, // 0-100
    val scoreBreakdown: ScoreBreakdown,
    val weekSummary: WeekSummary,
    val insights: List<String>, // AI-generated insights
    val recommendations: List<String>, // Suggestions for improvement
    val previousWeekScore: Int? = null,
    val scoreChange: Int? = null, // +/- points from previous week
    val rank: ScoreRank,
    val calculatedAt: Instant
)

@Serializable
data class ScoreBreakdown(
    val completionScore: Int,    // 0-40 points (habit completion rate)
    val streakScore: Int,         // 0-20 points (maintaining streaks)
    val consistencyScore: Int,    // 0-20 points (daily consistency)
    val improvementScore: Int     // 0-20 points (improvement over previous week)
) {
    val total: Int get() = completionScore + streakScore + consistencyScore + improvementScore
}

@Serializable
data class WeekSummary(
    val daysActive: Int,         // Out of 7
    val totalHabitsCompleted: Int,
    val totalHabitsPossible: Int,
    val completionRate: Float,   // 0.0 to 1.0
    val perfectDays: Int,        // Days where all habits completed
    val streakMaintained: Boolean,
    val currentStreak: Int,
    val topHabit: String?,       // Most completed habit
    val strugglingHabit: String?, // Least completed habit
    val bestDay: String?         // Day with most completions
)

@Serializable
enum class ScoreRank {
    STRUGGLING,    // 0-40
    BUILDING,      // 41-60
    THRIVING,      // 61-80
    EXCELLENT,     // 81-95
    CHAMPION       // 96-100
}

@Serializable
data class WellnessScoreHistory(
    val userId: String,
    val scores: List<WeeklyWellnessScore>
) {
    val averageScore: Float get() = scores.map { it.overallScore }.average().toFloat()
    val trend: ScoreTrend get() = calculateTrend()

    private fun calculateTrend(): ScoreTrend {
        if (scores.size < 2) return ScoreTrend.STABLE

        val recent = scores.takeLast(4).map { it.overallScore }
        val older = scores.dropLast(4).takeLast(4).map { it.overallScore }

        if (older.isEmpty()) return ScoreTrend.STABLE

        val recentAvg = recent.average()
        val olderAvg = older.average()

        return when {
            recentAvg > olderAvg + 5 -> ScoreTrend.IMPROVING
            recentAvg < olderAvg - 5 -> ScoreTrend.DECLINING
            else -> ScoreTrend.STABLE
        }
    }
}

@Serializable
enum class ScoreTrend {
    IMPROVING,
    STABLE,
    DECLINING
}

@Serializable
data class ShareableScoreCard(
    val score: WeeklyWellnessScore,
    val shareText: String,      // Text for social sharing
    val imageDescription: String // Description for image generation
) {
    companion object {
        fun from(score: WeeklyWellnessScore): ShareableScoreCard {
            val emoji = when (score.rank) {
                ScoreRank.CHAMPION -> "🏆"
                ScoreRank.EXCELLENT -> "⭐"
                ScoreRank.THRIVING -> "🌟"
                ScoreRank.BUILDING -> "💪"
                ScoreRank.STRUGGLING -> "🌱"
            }

            val shareText = buildString {
                appendLine("$emoji My Weekly Wellness Score: ${score.overallScore}/100")
                appendLine()
                appendLine("📊 ${score.weekSummary.totalHabitsCompleted} habits completed")
                appendLine("🔥 ${score.weekSummary.currentStreak} day streak")
                if (score.scoreChange != null && score.scoreChange > 0) {
                    appendLine("📈 +${score.scoreChange} points from last week!")
                }
                appendLine()
                appendLine("#DailyWell #HabitTracking #WellnessJourney")
            }

            val imageDesc = """
                A clean wellness score card showing:
                - Large score number: ${score.overallScore}/100 in bold
                - Rank badge: ${score.rank.displayName()}
                - Week range: ${score.weekStart} to ${score.weekEnd}
                - Score breakdown bar chart
                - Key stats: ${score.weekSummary.totalHabitsCompleted} habits, ${score.weekSummary.currentStreak} day streak
                - Gradient background: ${score.rank.colorScheme()}
                - DailyWell logo in corner
            """.trimIndent()

            return ShareableScoreCard(
                score = score,
                shareText = shareText,
                imageDescription = imageDesc
            )
        }
    }
}

// Scoring algorithm
object WellnessScoreCalculator {

    /**
     * Calculate weekly wellness score (0-100)
     *
     * Components:
     * 1. Completion Score (40 points) - Based on completion rate
     * 2. Streak Score (20 points) - Based on streak maintenance
     * 3. Consistency Score (20 points) - Based on daily activity
     * 4. Improvement Score (20 points) - Based on week-over-week progress
     */
    fun calculate(
        weekData: WeekData,
        streakInfo: StreakInfo,
        previousWeekScore: Int?,
        userId: String = "user",
        habitStats: Map<String, Float> = emptyMap() // habitName -> completionRate
    ): WeeklyWellnessScore {
        val breakdown = ScoreBreakdown(
            completionScore = calculateCompletionScore(weekData),
            streakScore = calculateStreakScore(streakInfo, weekData),
            consistencyScore = calculateConsistencyScore(weekData),
            improvementScore = calculateImprovementScore(weekData, previousWeekScore)
        )

        val overallScore = breakdown.total
        val rank = determineRank(overallScore)

        // Calculate top and struggling habits from stats
        val topHabit = habitStats.maxByOrNull { it.value }?.key
        val strugglingHabit = habitStats.entries
            .filter { it.value < (habitStats.values.maxOrNull() ?: 0f) }
            .minByOrNull { it.value }?.key

        val summary = WeekSummary(
            daysActive = weekData.days.count { it.completedCount > 0 },
            totalHabitsCompleted = weekData.days.sumOf { it.completedCount },
            totalHabitsPossible = weekData.days.sumOf { it.totalCount },
            completionRate = calculateCompletionRate(weekData),
            perfectDays = weekData.days.count { it.status == CompletionStatus.COMPLETE },
            streakMaintained = streakInfo.currentStreak >= 7,
            currentStreak = streakInfo.currentStreak,
            topHabit = topHabit,
            strugglingHabit = strugglingHabit,
            bestDay = weekData.days.maxByOrNull { it.completedCount }?.date
        )

        val insights = generateInsights(breakdown, summary, rank)
        val recommendations = generateRecommendations(breakdown, summary)

        return WeeklyWellnessScore(
            userId = userId,
            weekStart = weekData.days.firstOrNull()?.date ?: "",
            weekEnd = weekData.days.lastOrNull()?.date ?: "",
            overallScore = overallScore,
            scoreBreakdown = breakdown,
            weekSummary = summary,
            insights = insights,
            recommendations = recommendations,
            previousWeekScore = previousWeekScore,
            scoreChange = if (previousWeekScore != null) overallScore - previousWeekScore else null,
            rank = rank,
            calculatedAt = kotlinx.datetime.Clock.System.now()
        )
    }

    private fun calculateCompletionScore(weekData: WeekData): Int {
        val rate = calculateCompletionRate(weekData)
        return (rate * 40).toInt()
    }

    private fun calculateCompletionRate(weekData: WeekData): Float {
        val completed = weekData.days.sumOf { it.completedCount }
        val total = weekData.days.sumOf { it.totalCount }
        return if (total > 0) completed.toFloat() / total else 0f
    }

    private fun calculateStreakScore(streakInfo: StreakInfo, weekData: WeekData): Int {
        val points = when {
            streakInfo.currentStreak >= 30 -> 20 // Full points for 30+ days
            streakInfo.currentStreak >= 14 -> 17 // Great
            streakInfo.currentStreak >= 7 -> 14  // Good
            streakInfo.currentStreak >= 3 -> 10  // Building
            streakInfo.currentStreak > 0 -> 5    // Starting
            else -> 0                            // No streak
        }

        // Bonus for not missing any days this week
        val allDaysActive = weekData.days.all { it.completedCount > 0 }
        return if (allDaysActive) points + 3 else points
    }

    private fun calculateConsistencyScore(weekData: WeekData): Int {
        val daysActive = weekData.days.count { it.completedCount > 0 }
        val perfectDays = weekData.days.count { it.status == CompletionStatus.COMPLETE }

        val baseScore = when (daysActive) {
            7 -> 12      // All 7 days
            6 -> 10      // 6 days
            5 -> 8       // 5 days
            4 -> 6       // 4 days
            3 -> 4       // 3 days
            2 -> 2       // 2 days
            1 -> 1       // 1 day
            else -> 0    // No activity
        }

        // Bonus for perfect days
        val perfectBonus = (perfectDays * 2).coerceAtMost(8)

        return (baseScore + perfectBonus).coerceAtMost(20)
    }

    private fun calculateImprovementScore(weekData: WeekData, previousWeekScore: Int?): Int {
        if (previousWeekScore == null) return 10 // Default middle score for first week

        // Simple calculation based on overall performance
        val completed = weekData.days.sumOf { it.completedCount }
        val total = weekData.days.sumOf { it.totalCount }
        val rate = if (total > 0) completed.toFloat() / total else 0f

        return when {
            rate >= 0.9f -> 20  // Excellent improvement
            rate >= 0.75f -> 16 // Good improvement
            rate >= 0.6f -> 12  // Moderate improvement
            rate >= 0.5f -> 8   // Some improvement
            else -> 4           // Needs work
        }
    }

    private fun determineRank(score: Int): ScoreRank = when (score) {
        in 96..100 -> ScoreRank.CHAMPION
        in 81..95 -> ScoreRank.EXCELLENT
        in 61..80 -> ScoreRank.THRIVING
        in 41..60 -> ScoreRank.BUILDING
        else -> ScoreRank.STRUGGLING
    }

    private fun generateInsights(breakdown: ScoreBreakdown, summary: WeekSummary, rank: ScoreRank): List<String> {
        val insights = mutableListOf<String>()

        insights.add("You completed ${summary.totalHabitsCompleted} habits this week - ${rank.encouragement()}!")

        if (summary.perfectDays > 0) {
            insights.add("You had ${ summary.perfectDays} perfect day${if (summary.perfectDays > 1) "s" else ""} this week!")
        }

        if (summary.streakMaintained) {
            insights.add("Impressive ${summary.currentStreak}-day streak! You're building lasting habits.")
        }

        if (breakdown.consistencyScore >= 16) {
            insights.add("Your consistency is outstanding. This is the key to long-term success.")
        }

        return insights
    }

    private fun generateRecommendations(breakdown: ScoreBreakdown, summary: WeekSummary): List<String> {
        val recommendations = mutableListOf<String>()

        if (breakdown.completionScore < 20) {
            recommendations.add("Focus on completing more habits each day. Start with just one.")
        }

        if (breakdown.streakScore < 10) {
            recommendations.add("Build a streak by completing at least one habit daily.")
        }

        if (breakdown.consistencyScore < 12) {
            recommendations.add("Try to be active ${7 - summary.daysActive} more day${if (7 - summary.daysActive > 1) "s" else ""} per week.")
        }

        if (summary.perfectDays == 0) {
            recommendations.add("Aim for one perfect day this week - complete all your habits!")
        }

        return recommendations
    }
}

// Extension functions for display
fun ScoreRank.displayName(): String = when (this) {
    ScoreRank.CHAMPION -> "Champion"
    ScoreRank.EXCELLENT -> "Excellent"
    ScoreRank.THRIVING -> "Thriving"
    ScoreRank.BUILDING -> "Building"
    ScoreRank.STRUGGLING -> "Struggling"
}

fun ScoreRank.encouragement(): String = when (this) {
    ScoreRank.CHAMPION -> "Absolutely phenomenal"
    ScoreRank.EXCELLENT -> "Outstanding work"
    ScoreRank.THRIVING -> "Great job"
    ScoreRank.BUILDING -> "Keep going"
    ScoreRank.STRUGGLING -> "You've got this"
}

fun ScoreRank.colorScheme(): String = when (this) {
    ScoreRank.CHAMPION -> "Gold gradient"
    ScoreRank.EXCELLENT -> "Purple to blue gradient"
    ScoreRank.THRIVING -> "Green to teal gradient"
    ScoreRank.BUILDING -> "Blue to indigo gradient"
    ScoreRank.STRUGGLING -> "Soft gray to light blue gradient"
}
