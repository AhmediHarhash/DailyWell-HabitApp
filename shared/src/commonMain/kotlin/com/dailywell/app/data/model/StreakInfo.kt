package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StreakInfo(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletedDate: String? = null, // ISO date
    val streakStartDate: String? = null // ISO date
) {
    val isActive: Boolean
        get() = currentStreak > 0

    fun getMilestone(): StreakMilestone? {
        return when {
            currentStreak >= 100 -> StreakMilestone.CENTURY
            currentStreak >= 66 -> StreakMilestone.LIFESTYLE
            currentStreak >= 30 -> StreakMilestone.MONTH
            currentStreak >= 21 -> StreakMilestone.HABIT_FORMED
            currentStreak >= 7 -> StreakMilestone.WEEK
            currentStreak >= 1 -> StreakMilestone.FIRST_DAY
            else -> null
        }
    }

    fun isNewMilestone(previousStreak: Int): Boolean {
        val previousMilestone = StreakInfo(currentStreak = previousStreak).getMilestone()
        val currentMilestone = getMilestone()
        return currentMilestone != previousMilestone && currentMilestone != null
    }
}

enum class StreakMilestone(
    val days: Int,
    val title: String,
    val message: String,
    val emoji: String
) {
    FIRST_DAY(1, "First Day", "You started! That's the hardest part.", "🌱"),
    WEEK(7, "One Week", "A full week of showing up!", "🔥"),
    HABIT_FORMED(21, "21 Days", "Habit forming in progress...", "💪"),
    MONTH(30, "30 Days", "This is becoming who you are.", "⭐"),
    LIFESTYLE(66, "66 Days", "Scientists say it's official now.", "🏆"),
    CENTURY(100, "100 Days", "You've built something lasting.", "👑")
}
