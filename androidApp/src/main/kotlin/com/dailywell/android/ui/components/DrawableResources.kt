package com.dailywell.android.ui.components

import com.dailywell.android.R

/**
 * Centralized drawable resource mapping for DailyWell
 * Maps model IDs to their AI-generated image resources
 */
object DrawableResources {

    // ==================== AI COACH PORTRAITS ====================

    /**
     * Get drawable resource ID for a coach by their ID
     */
    fun getCoachDrawable(coachId: String): Int {
        return when (coachId) {
            "coach_sam" -> R.drawable.coach_sam
            "coach_alex" -> R.drawable.coach_alex
            "coach_dana" -> R.drawable.coach_dana
            "coach_grace" -> R.drawable.coach_grace
            // Legacy ID kept for backward compatibility after asset cleanup.
            "coach_mike" -> R.drawable.coach_sam
            else -> R.drawable.coach_sam // Default to Sam
        }
    }

    // ==================== HABIT ICONS ====================

    /**
     * Get drawable resource ID for a habit by its ID
     */
    fun getHabitDrawable(habitId: String): Int {
        return when (habitId.lowercase()) {
            "sleep", "rest" -> R.drawable.habit_rest
            "water", "hydrate" -> R.drawable.habit_hydrate
            "move", "exercise" -> R.drawable.habit_move
            "vegetables", "nourish" -> R.drawable.habit_nourish
            "calm", "meditate", "mindfulness" -> R.drawable.habit_calm
            "connect", "social" -> R.drawable.habit_connect
            "unplug", "digital_detox" -> R.drawable.habit_unplug
            else -> R.drawable.habit_calm // Default
        }
    }

    // ==================== ACHIEVEMENT BADGES ====================

    /**
     * Get badge drawable based on streak count
     */
    fun getStreakBadgeDrawable(streakDays: Int): Int {
        return when {
            streakDays >= 100 -> R.drawable.badge_streak_100
            streakDays >= 30 -> R.drawable.badge_streak_30
            streakDays >= 7 -> R.drawable.badge_streak_7
            else -> R.drawable.badge_first_habit
        }
    }

    /**
     * Get specific badge by name
     */
    fun getBadgeDrawable(badgeName: String): Int {
        return when (badgeName.lowercase()) {
            "streak_7", "week_streak" -> R.drawable.badge_streak_7
            "streak_30", "month_streak" -> R.drawable.badge_streak_30
            "streak_100", "century_streak" -> R.drawable.badge_streak_100
            "first_habit", "first_completion" -> R.drawable.badge_first_habit
            "perfect_week" -> R.drawable.badge_perfect_week
            "early_bird" -> R.drawable.badge_early_bird
            "night_owl" -> R.drawable.badge_night_owl
            "comeback", "comeback_champion" -> R.drawable.badge_comeback
            else -> R.drawable.badge_first_habit
        }
    }

    // ==================== SECTION BACKGROUNDS ====================

    /**
     * Get background drawable for app sections
     */
    fun getSectionBackground(section: AppSection): Int {
        return when (section) {
            AppSection.DASHBOARD -> R.drawable.bg_dashboard
            AppSection.INSIGHTS -> R.drawable.bg_insights
            AppSection.SETTINGS -> R.drawable.bg_settings
            AppSection.PROFILE -> R.drawable.bg_profile
        }
    }

    enum class AppSection {
        DASHBOARD,
        INSIGHTS,
        SETTINGS,
        PROFILE
    }

    // ==================== ALL RESOURCES LIST ====================

    val allCoachDrawables: List<Int> = listOf(
        R.drawable.coach_sam,
        R.drawable.coach_alex,
        R.drawable.coach_dana,
        R.drawable.coach_grace
    )

    val allHabitDrawables: List<Int> = listOf(
        R.drawable.habit_rest,
        R.drawable.habit_hydrate,
        R.drawable.habit_move,
        R.drawable.habit_nourish,
        R.drawable.habit_calm,
        R.drawable.habit_connect,
        R.drawable.habit_unplug
    )

    val allBadgeDrawables: List<Int> = listOf(
        R.drawable.badge_streak_7,
        R.drawable.badge_streak_30,
        R.drawable.badge_streak_100,
        R.drawable.badge_first_habit,
        R.drawable.badge_perfect_week,
        R.drawable.badge_early_bird,
        R.drawable.badge_night_owl,
        R.drawable.badge_comeback
    )
}
