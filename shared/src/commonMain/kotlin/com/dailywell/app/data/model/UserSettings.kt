package com.dailywell.app.data.model

import kotlinx.datetime.LocalDate
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
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val customThresholds: Map<String, String> = emptyMap(), // habitId -> custom threshold

    // User Profile
    val userName: String? = null,
    val selectedCoachPersona: String? = "Sam", // Default coach

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
    val lastHealthSyncDate: String? = null,

    // Proactive Notifications
    val proactiveNotificationsEnabled: Boolean = true,

    // Today screen density mode (anti-spam default)
    val todayViewMode: TodayViewMode = TodayViewMode.SIMPLE,

    // Onboarding personalization
    val onboardingGoal: String? = null,       // Selected goal during onboarding
    val assessmentScore: Int? = null,         // 1-5 scale from quick assessment

    // Auth state
    val hasCompletedAuth: Boolean = false,
    val authSkipped: Boolean = false,
    val userEmail: String? = null,
    val authProvider: String = "none",
    val isEmailVerified: Boolean = false,
    val firebaseUid: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
) {
    fun withHabitEnabled(habitId: String): UserSettings {
        if (enabledHabitIds.contains(habitId)) return this
        return copy(enabledHabitIds = enabledHabitIds + habitId)
    }

    fun withHabitDisabled(habitId: String): UserSettings {
        return copy(enabledHabitIds = enabledHabitIds - habitId)
    }

    fun canAddMoreHabits(): Boolean {
        // UX/design pass: allow full habit visibility/editing for all users during dashboard redesign.
        val maxHabits = if (isPremium) 12 else 12
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
        val endDateStr = trialEndDate ?: return 0
        if (isPremium) return 0
        return try {
            val today = LocalDate.parse(currentDate)
            val end = LocalDate.parse(endDateStr)
            (end.toEpochDays() - today.toEpochDays()).coerceAtLeast(0)
        } catch (_: Exception) {
            0
        }
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

@Serializable
enum class TodayViewMode {
    SIMPLE,
    FULL
}
