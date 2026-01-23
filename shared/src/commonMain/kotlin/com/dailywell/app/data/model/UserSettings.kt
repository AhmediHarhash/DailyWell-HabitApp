package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val enabledHabitIds: List<String> = emptyList(),
    val reminderHour: Int = 20, // 8 PM default
    val reminderMinute: Int = 0,
    val reminderEnabled: Boolean = true,
    val includeWeekends: Boolean = true,
    val startDate: String? = null, // ISO date when user started
    val hasCompletedOnboarding: Boolean = false,
    val isPremium: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customThresholds: Map<String, String> = emptyMap(), // habitId -> custom threshold

    // Streak Protection (Premium Feature)
    val streakFreezesAvailable: Int = 2, // Resets monthly for premium users
    val streakFreezesUsedThisMonth: Int = 0,
    val lastStreakFreezeResetMonth: String? = null, // "2026-01" format
    val frozenDates: List<String> = emptyList(), // ISO dates that were "frozen"

    // Trial System
    val trialStartDate: String? = null, // When trial started
    val trialEndDate: String? = null, // When trial ends
    val hasUsedTrial: Boolean = false,

    // Health Connect
    val healthConnectEnabled: Boolean = false,
    val lastHealthSyncDate: String? = null
) {
    fun withHabitEnabled(habitId: String): UserSettings {
        if (enabledHabitIds.contains(habitId)) return this
        return copy(enabledHabitIds = enabledHabitIds + habitId)
    }

    fun withHabitDisabled(habitId: String): UserSettings {
        return copy(enabledHabitIds = enabledHabitIds - habitId)
    }

    fun canAddMoreHabits(): Boolean {
        val maxHabits = if (isPremium) 10 else 3
        return enabledHabitIds.size < maxHabits
    }

    fun getThreshold(habitId: String, default: String): String {
        return customThresholds[habitId] ?: default
    }

    // Streak Protection helpers
    fun canUseStreakFreeze(): Boolean {
        return isPremium && streakFreezesAvailable > streakFreezesUsedThisMonth
    }

    fun remainingStreakFreezes(): Int {
        return if (isPremium) (streakFreezesAvailable - streakFreezesUsedThisMonth).coerceAtLeast(0) else 0
    }

    fun withStreakFreezeUsed(date: String): UserSettings {
        return copy(
            streakFreezesUsedThisMonth = streakFreezesUsedThisMonth + 1,
            frozenDates = frozenDates + date
        )
    }

    fun withMonthlyFreezeReset(currentMonth: String): UserSettings {
        return if (lastStreakFreezeResetMonth != currentMonth) {
            copy(
                streakFreezesUsedThisMonth = 0,
                lastStreakFreezeResetMonth = currentMonth
            )
        } else this
    }

    // Trial helpers
    fun isTrialActive(currentDate: String): Boolean {
        val endDate = trialEndDate ?: return false
        return currentDate <= endDate && !isPremium
    }

    fun trialDaysRemaining(currentDate: String): Int {
        val endDate = trialEndDate ?: return 0
        if (isPremium) return 0
        // Parse dates and calculate difference (ISO format: YYYY-MM-DD)
        try {
            val currentParts = currentDate.split("-").map { it.toInt() }
            val endParts = endDate.split("-").map { it.toInt() }
            if (currentParts.size >= 3 && endParts.size >= 3) {
                // Simple day difference calculation
                val currentDay = currentParts[0] * 365 + currentParts[1] * 30 + currentParts[2]
                val endDay = endParts[0] * 365 + endParts[1] * 30 + endParts[2]
                return (endDay - currentDay).coerceAtLeast(0)
            }
        } catch (e: Exception) {
            // Fallback
        }
        return 0
    }

    /**
     * CRITICAL: This is the main access check.
     * During 14-day trial, users get FULL premium access to ALL features.
     * No restrictions, no locked content - everything unlocked.
     */
    fun hasPremiumAccess(currentDate: String): Boolean {
        return isPremium || isTrialActive(currentDate)
    }

    /**
     * Start a 14-day free trial with FULL premium access
     */
    fun withTrialStarted(currentDate: String): UserSettings {
        if (hasUsedTrial || isPremium) return this
        // Calculate end date (14 days from now)
        try {
            val parts = currentDate.split("-").map { it.toInt() }
            if (parts.size >= 3) {
                var year = parts[0]
                var month = parts[1]
                var day = parts[2] + 14
                // Simple overflow handling
                val daysInMonth = when (month) {
                    1, 3, 5, 7, 8, 10, 12 -> 31
                    4, 6, 9, 11 -> 30
                    2 -> if (year % 4 == 0) 29 else 28
                    else -> 30
                }
                if (day > daysInMonth) {
                    day -= daysInMonth
                    month++
                    if (month > 12) {
                        month = 1
                        year++
                    }
                }
                val endDate = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                return copy(
                    trialStartDate = currentDate,
                    trialEndDate = endDate,
                    hasUsedTrial = true
                )
            }
        } catch (e: Exception) {
            // Fallback: just set dates as strings
        }
        return this
    }
}

@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
