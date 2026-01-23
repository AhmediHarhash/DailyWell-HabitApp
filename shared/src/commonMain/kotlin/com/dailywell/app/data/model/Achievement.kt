package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val category: AchievementCategory = AchievementCategory.STREAK,
    val target: Int = 1,
    val progress: Int = 0
) {
    val progressPercent: Float
        get() = if (target > 0) (progress.toFloat() / target).coerceIn(0f, 1f) else 0f

    fun withProgress(newProgress: Int): Achievement = copy(progress = newProgress)

    fun unlock(timestamp: Long = System.currentTimeMillis()): Achievement = copy(
        isUnlocked = true,
        unlockedAt = timestamp,
        progress = target
    )
}

@Serializable
enum class AchievementCategory {
    STREAK,
    CONSISTENCY,
    HABIT,
    SPECIAL
}

@Serializable
data class UnlockedAchievement(
    val achievementId: String,
    val unlockedAt: Long
)

object Achievements {
    // Streak achievements
    val FIRST_DAY = Achievement(
        id = "first_day",
        name = "First Step",
        description = "Complete your first day",
        emoji = "🌱",
        category = AchievementCategory.STREAK
    )

    val WEEK_WARRIOR = Achievement(
        id = "week_warrior",
        name = "Week Warrior",
        description = "7 day streak",
        emoji = "🔥",
        category = AchievementCategory.STREAK
    )

    val FORTNIGHT_FORCE = Achievement(
        id = "fortnight_force",
        name = "Fortnight Force",
        description = "14 day streak",
        emoji = "💪",
        category = AchievementCategory.STREAK
    )

    val MONTHLY_MASTER = Achievement(
        id = "monthly_master",
        name = "Monthly Master",
        description = "30 day streak",
        emoji = "🏆",
        category = AchievementCategory.STREAK
    )

    val QUARTER_CHAMPION = Achievement(
        id = "quarter_champion",
        name = "Quarter Champion",
        description = "90 day streak",
        emoji = "👑",
        category = AchievementCategory.STREAK
    )

    val YEAR_LEGEND = Achievement(
        id = "year_legend",
        name = "Year Legend",
        description = "365 day streak",
        emoji = "🌟",
        category = AchievementCategory.STREAK
    )

    // Consistency achievements
    val PERFECT_WEEK = Achievement(
        id = "perfect_week",
        name = "Perfect Week",
        description = "Complete all habits for 7 days",
        emoji = "✨",
        category = AchievementCategory.CONSISTENCY
    )

    val PERFECT_MONTH = Achievement(
        id = "perfect_month",
        name = "Perfect Month",
        description = "Complete all habits for 30 days",
        emoji = "💎",
        category = AchievementCategory.CONSISTENCY
    )

    // Habit-specific achievements
    val SLEEP_CHAMPION = Achievement(
        id = "sleep_champion",
        name = "Sleep Champion",
        description = "30 days of good sleep",
        emoji = "😴",
        category = AchievementCategory.HABIT
    )

    val HYDRATION_HERO = Achievement(
        id = "hydration_hero",
        name = "Hydration Hero",
        description = "30 days of proper hydration",
        emoji = "💧",
        category = AchievementCategory.HABIT
    )

    val MOVEMENT_MASTER = Achievement(
        id = "movement_master",
        name = "Movement Master",
        description = "30 days of movement",
        emoji = "🏃",
        category = AchievementCategory.HABIT
    )

    val VEGGIE_VICTOR = Achievement(
        id = "veggie_victor",
        name = "Veggie Victor",
        description = "30 days of eating vegetables",
        emoji = "🥬",
        category = AchievementCategory.HABIT
    )

    val ZEN_MASTER = Achievement(
        id = "zen_master",
        name = "Zen Master",
        description = "30 days of stress relief",
        emoji = "🧘",
        category = AchievementCategory.HABIT
    )

    val SOCIAL_BUTTERFLY = Achievement(
        id = "social_butterfly",
        name = "Social Butterfly",
        description = "30 days of connection",
        emoji = "💬",
        category = AchievementCategory.HABIT
    )

    val DIGITAL_DETOXER = Achievement(
        id = "digital_detoxer",
        name = "Digital Detoxer",
        description = "30 days of unplugging",
        emoji = "📵",
        category = AchievementCategory.HABIT
    )

    // Special achievements
    val EARLY_BIRD = Achievement(
        id = "early_bird",
        name = "Early Bird",
        description = "Check in before 8 AM for 7 days",
        emoji = "🐦",
        category = AchievementCategory.SPECIAL
    )

    val NIGHT_OWL = Achievement(
        id = "night_owl",
        name = "Night Owl",
        description = "Check in after 10 PM for 7 days",
        emoji = "🦉",
        category = AchievementCategory.SPECIAL
    )

    val COMEBACK_KID = Achievement(
        id = "comeback_kid",
        name = "Comeback Kid",
        description = "Return after missing 3+ days",
        emoji = "🔄",
        category = AchievementCategory.SPECIAL
    )

    val ALL_STAR = Achievement(
        id = "all_star",
        name = "All-Star",
        description = "Track all 7 habits",
        emoji = "⭐",
        category = AchievementCategory.SPECIAL
    )

    // 21-day and 66-day milestones (habit formation science)
    val HABIT_FORMING = Achievement(
        id = "habit_forming",
        name = "Habit Forming",
        description = "21 day streak - habits are forming!",
        emoji = "🧠",
        category = AchievementCategory.STREAK,
        target = 21
    )

    val LIFESTYLE = Achievement(
        id = "lifestyle",
        name = "Lifestyle",
        description = "66 day streak - it's official now!",
        emoji = "💫",
        category = AchievementCategory.STREAK,
        target = 66
    )

    val CENTURY = Achievement(
        id = "century",
        name = "Century Club",
        description = "100 day streak",
        emoji = "💯",
        category = AchievementCategory.STREAK,
        target = 100
    )

    val all = listOf(
        FIRST_DAY,
        WEEK_WARRIOR,
        FORTNIGHT_FORCE,
        HABIT_FORMING,
        MONTHLY_MASTER,
        LIFESTYLE,
        QUARTER_CHAMPION,
        CENTURY,
        YEAR_LEGEND,
        PERFECT_WEEK,
        PERFECT_MONTH,
        SLEEP_CHAMPION,
        HYDRATION_HERO,
        MOVEMENT_MASTER,
        VEGGIE_VICTOR,
        ZEN_MASTER,
        SOCIAL_BUTTERFLY,
        DIGITAL_DETOXER,
        EARLY_BIRD,
        NIGHT_OWL,
        COMEBACK_KID,
        ALL_STAR
    )

    fun getById(id: String): Achievement? = all.find { it.id == id }

    val streakMilestones = mapOf(
        1 to FIRST_DAY,
        7 to WEEK_WARRIOR,
        14 to FORTNIGHT_FORCE,
        21 to HABIT_FORMING,
        30 to MONTHLY_MASTER,
        66 to LIFESTYLE,
        90 to QUARTER_CHAMPION,
        100 to CENTURY,
        365 to YEAR_LEGEND
    )

    fun getByCategory(category: AchievementCategory): List<Achievement> =
        all.filter { it.category == category }
}
