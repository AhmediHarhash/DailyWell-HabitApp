package com.dailywell.app.data.model

import com.dailywell.app.data.content.AchievementsDatabase
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

/**
 * Achievements object - Backward compatibility wrapper for AchievementsDatabase
 *
 * The actual achievements are defined in AchievementsDatabase (75 achievements).
 * This object maintains compatibility with existing code that uses Achievements.all, etc.
 */
object Achievements {
    // Delegate to AchievementsDatabase for all data
    val all: List<Achievement>
        get() = AchievementsDatabase.all

    val streakMilestones: Map<Int, Achievement>
        get() = AchievementsDatabase.streakMilestones

    fun getById(id: String): Achievement? = AchievementsDatabase.getById(id)

    fun getByCategory(category: AchievementCategory): List<Achievement> =
        AchievementsDatabase.getByCategory(category)

    // Legacy named achievements - map to new IDs for compatibility
    val FIRST_DAY: Achievement
        get() = AchievementsDatabase.getById("streak_1")!!

    val WEEK_WARRIOR: Achievement
        get() = AchievementsDatabase.getById("streak_7")!!

    val FORTNIGHT_FORCE: Achievement
        get() = AchievementsDatabase.getById("streak_14")!!

    val MONTHLY_MASTER: Achievement
        get() = AchievementsDatabase.getById("streak_30")!!

    val QUARTER_CHAMPION: Achievement
        get() = AchievementsDatabase.getById("streak_90")!!

    val YEAR_LEGEND: Achievement
        get() = AchievementsDatabase.getById("streak_365")!!

    val PERFECT_WEEK: Achievement
        get() = AchievementsDatabase.getById("perfect_week")!!

    val PERFECT_MONTH: Achievement
        get() = AchievementsDatabase.getById("perfect_month")!!

    val SLEEP_CHAMPION: Achievement
        get() = AchievementsDatabase.getById("sleep_30")!!

    val HYDRATION_HERO: Achievement
        get() = AchievementsDatabase.getById("water_30")!!

    val MOVEMENT_MASTER: Achievement
        get() = AchievementsDatabase.getById("move_30")!!

    val VEGGIE_VICTOR: Achievement
        get() = AchievementsDatabase.getById("veggies_30")!!

    val ZEN_MASTER: Achievement
        get() = AchievementsDatabase.getById("calm_30")!!

    val SOCIAL_BUTTERFLY: Achievement
        get() = AchievementsDatabase.getById("connect_30")!!

    val DIGITAL_DETOXER: Achievement
        get() = AchievementsDatabase.getById("unplug_30")!!

    val EARLY_BIRD: Achievement
        get() = AchievementsDatabase.getById("early_bird_7")!!

    val NIGHT_OWL: Achievement
        get() = AchievementsDatabase.getById("night_owl_7")!!

    val COMEBACK_KID: Achievement
        get() = AchievementsDatabase.getById("comeback_3")!!

    val ALL_STAR: Achievement
        get() = AchievementsDatabase.getById("all_habits_active")!!

    val HABIT_FORMING: Achievement
        get() = AchievementsDatabase.getById("streak_21")!!

    val LIFESTYLE: Achievement
        get() = AchievementsDatabase.getById("streak_66")!!

    val CENTURY: Achievement
        get() = AchievementsDatabase.getById("streak_100")!!
}
