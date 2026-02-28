package com.dailywell.app.data.content

import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.AchievementCategory

/**
 * AchievementsDatabase - Production-Ready Achievement System
 *
 * 75 Unique, Creative Achievements organized by category:
 * - STREAK: Time-based consistency milestones (15 achievements)
 * - HABIT: Per-habit mastery with tiers (21 achievements)
 * - CONSISTENCY: Perfection-based achievements (12 achievements)
 * - SPECIAL: Unique behavioral achievements (15 achievements)
 * - COMMUNITY: Social/sharing achievements (6 achievements)
 * - MILESTONE: Cumulative progress achievements (6 achievements)
 *
 * Each achievement has:
 * - Unique ID for database persistence
 * - Creative name and description
 * - Emoji for visual representation
 * - Category for organization
 * - Target value for progress tracking
 * - Tier level for multi-stage achievements
 */
object AchievementsDatabase {

    // ==================== STREAK ACHIEVEMENTS (15) ====================
    // Progressive streak milestones based on habit formation science

    private val streakAchievements = listOf(
        Achievement(
            id = "streak_1",
            name = "First Step",
            description = "Complete your first day - every journey starts here",
            emoji = "🌱",
            category = AchievementCategory.STREAK,
            target = 1
        ),
        Achievement(
            id = "streak_3",
            name = "Triple Threat",
            description = "3 days in a row - momentum is building!",
            emoji = "🔥",
            category = AchievementCategory.STREAK,
            target = 3
        ),
        Achievement(
            id = "streak_7",
            name = "Week Warrior",
            description = "7 day streak - you made it through a full week!",
            emoji = "⚡",
            category = AchievementCategory.STREAK,
            target = 7
        ),
        Achievement(
            id = "streak_14",
            name = "Fortnight Force",
            description = "14 days strong - habits are taking root",
            emoji = "💪",
            category = AchievementCategory.STREAK,
            target = 14
        ),
        Achievement(
            id = "streak_21",
            name = "Neural Pathway",
            description = "21 days - science says habits are forming in your brain!",
            emoji = "🧠",
            category = AchievementCategory.STREAK,
            target = 21
        ),
        Achievement(
            id = "streak_30",
            name = "Monthly Master",
            description = "30 day streak - you're officially consistent",
            emoji = "🏆",
            category = AchievementCategory.STREAK,
            target = 30
        ),
        Achievement(
            id = "streak_45",
            name = "Momentum Machine",
            description = "45 days - you're unstoppable now",
            emoji = "🚀",
            category = AchievementCategory.STREAK,
            target = 45
        ),
        Achievement(
            id = "streak_66",
            name = "Lifestyle Locked",
            description = "66 days - this is who you are now",
            emoji = "💫",
            category = AchievementCategory.STREAK,
            target = 66
        ),
        Achievement(
            id = "streak_90",
            name = "Quarter Champion",
            description = "90 day streak - a full quarter of consistency",
            emoji = "👑",
            category = AchievementCategory.STREAK,
            target = 90
        ),
        Achievement(
            id = "streak_100",
            name = "Century Club",
            description = "100 days - you're in the elite 1%",
            emoji = "💯",
            category = AchievementCategory.STREAK,
            target = 100
        ),
        Achievement(
            id = "streak_150",
            name = "Steadfast",
            description = "150 days - half a year of dedication incoming",
            emoji = "🔒",
            category = AchievementCategory.STREAK,
            target = 150
        ),
        Achievement(
            id = "streak_180",
            name = "Half-Year Hero",
            description = "180 days - 6 months of pure commitment",
            emoji = "🌟",
            category = AchievementCategory.STREAK,
            target = 180
        ),
        Achievement(
            id = "streak_270",
            name = "Three Quarter King",
            description = "270 days - legendary status approaching",
            emoji = "⚜️",
            category = AchievementCategory.STREAK,
            target = 270
        ),
        Achievement(
            id = "streak_365",
            name = "Year Legend",
            description = "365 days - an entire year! You're a legend.",
            emoji = "🏅",
            category = AchievementCategory.STREAK,
            target = 365
        ),
        Achievement(
            id = "streak_500",
            name = "Immortal",
            description = "500 days - habits are now your superpower",
            emoji = "♾️",
            category = AchievementCategory.STREAK,
            target = 500
        )
    )

    // ==================== HABIT MASTERY ACHIEVEMENTS (21) ====================
    // Per-habit achievements with 3 tiers each (7 habits × 3 tiers)

    private val habitAchievements = listOf(
        // SLEEP HABIT - 3 tiers
        Achievement(
            id = "sleep_7",
            name = "Sleep Starter",
            description = "7 days of quality sleep tracked",
            emoji = "😴",
            category = AchievementCategory.HABIT,
            target = 7
        ),
        Achievement(
            id = "sleep_30",
            name = "Sleep Champion",
            description = "30 days of prioritizing rest",
            emoji = "🛏️",
            category = AchievementCategory.HABIT,
            target = 30
        ),
        Achievement(
            id = "sleep_90",
            name = "Dream Master",
            description = "90 days - sleep is your secret weapon",
            emoji = "🌙",
            category = AchievementCategory.HABIT,
            target = 90
        ),

        // HYDRATION HABIT - 3 tiers
        Achievement(
            id = "water_7",
            name = "Hydration Starter",
            description = "7 days of proper hydration",
            emoji = "💧",
            category = AchievementCategory.HABIT,
            target = 7
        ),
        Achievement(
            id = "water_30",
            name = "Hydration Hero",
            description = "30 days of staying hydrated",
            emoji = "🌊",
            category = AchievementCategory.HABIT,
            target = 30
        ),
        Achievement(
            id = "water_90",
            name = "Water Warrior",
            description = "90 days - hydration is second nature",
            emoji = "🏊",
            category = AchievementCategory.HABIT,
            target = 90
        ),

        // MOVEMENT HABIT - 3 tiers
        Achievement(
            id = "move_7",
            name = "Movement Starter",
            description = "7 days of getting your body moving",
            emoji = "🚶",
            category = AchievementCategory.HABIT,
            target = 7
        ),
        Achievement(
            id = "move_30",
            name = "Movement Master",
            description = "30 days of consistent activity",
            emoji = "🏃",
            category = AchievementCategory.HABIT,
            target = 30
        ),
        Achievement(
            id = "move_90",
            name = "Fitness Force",
            description = "90 days - your body thanks you",
            emoji = "💪",
            category = AchievementCategory.HABIT,
            target = 90
        ),

        // VEGETABLES HABIT - 3 tiers
        Achievement(
            id = "veggies_7",
            name = "Veggie Beginner",
            description = "7 days of eating your greens",
            emoji = "🥬",
            category = AchievementCategory.HABIT,
            target = 7
        ),
        Achievement(
            id = "veggies_30",
            name = "Veggie Victor",
            description = "30 days of plant-powered nutrition",
            emoji = "🥗",
            category = AchievementCategory.HABIT,
            target = 30
        ),
        Achievement(
            id = "veggies_90",
            name = "Garden Guardian",
            description = "90 days of vegetable excellence",
            emoji = "🌱",
            category = AchievementCategory.HABIT,
            target = 90
        ),

        // CALM/STRESS HABIT - 3 tiers
        Achievement(
            id = "calm_7",
            name = "Calm Starter",
            description = "7 days of stress management",
            emoji = "🧘",
            category = AchievementCategory.HABIT,
            target = 7
        ),
        Achievement(
            id = "calm_30",
            name = "Zen Master",
            description = "30 days of inner peace",
            emoji = "☮️",
            category = AchievementCategory.HABIT,
            target = 30
        ),
        Achievement(
            id = "calm_90",
            name = "Serenity Sage",
            description = "90 days - stress doesn't stand a chance",
            emoji = "🕊️",
            category = AchievementCategory.HABIT,
            target = 90
        ),

        // CONNECTION HABIT - 3 tiers
        Achievement(
            id = "connect_7",
            name = "Social Starter",
            description = "7 days of meaningful connections",
            emoji = "💬",
            category = AchievementCategory.HABIT,
            target = 7
        ),
        Achievement(
            id = "connect_30",
            name = "Social Butterfly",
            description = "30 days of nurturing relationships",
            emoji = "🦋",
            category = AchievementCategory.HABIT,
            target = 30
        ),
        Achievement(
            id = "connect_90",
            name = "Heart Connector",
            description = "90 days of deep human connection",
            emoji = "❤️",
            category = AchievementCategory.HABIT,
            target = 90
        ),

        // DIGITAL DETOX HABIT - 3 tiers
        Achievement(
            id = "unplug_7",
            name = "Unplug Starter",
            description = "7 days of digital balance",
            emoji = "📵",
            category = AchievementCategory.HABIT,
            target = 7
        ),
        Achievement(
            id = "unplug_30",
            name = "Digital Detoxer",
            description = "30 days of mindful technology use",
            emoji = "🔌",
            category = AchievementCategory.HABIT,
            target = 30
        ),
        Achievement(
            id = "unplug_90",
            name = "Tech-Life Balancer",
            description = "90 days - you control tech, not vice versa",
            emoji = "⚖️",
            category = AchievementCategory.HABIT,
            target = 90
        )
    )

    // ==================== CONSISTENCY ACHIEVEMENTS (12) ====================
    // Perfect completion achievements

    private val consistencyAchievements = listOf(
        Achievement(
            id = "perfect_day_1",
            name = "Perfect Day",
            description = "Complete all habits in a single day",
            emoji = "✨",
            category = AchievementCategory.CONSISTENCY,
            target = 1
        ),
        Achievement(
            id = "perfect_day_3",
            name = "Trifecta",
            description = "3 perfect days in a row",
            emoji = "🌟",
            category = AchievementCategory.CONSISTENCY,
            target = 3
        ),
        Achievement(
            id = "perfect_week",
            name = "Perfect Week",
            description = "Complete all habits every day for 7 days",
            emoji = "💎",
            category = AchievementCategory.CONSISTENCY,
            target = 7
        ),
        Achievement(
            id = "perfect_fortnight",
            name = "Flawless Fortnight",
            description = "14 days of 100% completion",
            emoji = "💠",
            category = AchievementCategory.CONSISTENCY,
            target = 14
        ),
        Achievement(
            id = "perfect_month",
            name = "Perfect Month",
            description = "30 days of absolute consistency",
            emoji = "🏆",
            category = AchievementCategory.CONSISTENCY,
            target = 30
        ),
        Achievement(
            id = "consistency_80_week",
            name = "Reliable",
            description = "80%+ completion rate for a week",
            emoji = "📊",
            category = AchievementCategory.CONSISTENCY,
            target = 7
        ),
        Achievement(
            id = "consistency_80_month",
            name = "Dependable",
            description = "80%+ completion rate for a month",
            emoji = "📈",
            category = AchievementCategory.CONSISTENCY,
            target = 30
        ),
        Achievement(
            id = "all_habits_active",
            name = "All-Star",
            description = "Track all 7 core habits",
            emoji = "⭐",
            category = AchievementCategory.CONSISTENCY,
            target = 7
        ),
        Achievement(
            id = "multi_habit_5",
            name = "Multi-Tasker",
            description = "Complete 5+ habits in a single day",
            emoji = "🎯",
            category = AchievementCategory.CONSISTENCY,
            target = 5
        ),
        Achievement(
            id = "weekend_warrior",
            name = "Weekend Warrior",
            description = "Complete all habits on Saturday AND Sunday",
            emoji = "🌅",
            category = AchievementCategory.CONSISTENCY,
            target = 2
        ),
        Achievement(
            id = "monday_momentum",
            name = "Monday Momentum",
            description = "Complete all habits on 4 Mondays in a row",
            emoji = "📅",
            category = AchievementCategory.CONSISTENCY,
            target = 4
        ),
        Achievement(
            id = "daily_doubles",
            name = "Daily Doubles",
            description = "Complete the same habit twice in one day 10 times",
            emoji = "✌️",
            category = AchievementCategory.CONSISTENCY,
            target = 10
        )
    )

    // ==================== SPECIAL ACHIEVEMENTS (15) ====================
    // Unique behavioral and timing-based achievements

    private val specialAchievements = listOf(
        Achievement(
            id = "early_bird_7",
            name = "Early Bird",
            description = "Check in before 7 AM for 7 days",
            emoji = "🐦",
            category = AchievementCategory.SPECIAL,
            target = 7
        ),
        Achievement(
            id = "early_bird_30",
            name = "Dawn Warrior",
            description = "30 days of early morning check-ins",
            emoji = "🌅",
            category = AchievementCategory.SPECIAL,
            target = 30
        ),
        Achievement(
            id = "night_owl_7",
            name = "Night Owl",
            description = "Check in after 10 PM for 7 days",
            emoji = "🦉",
            category = AchievementCategory.SPECIAL,
            target = 7
        ),
        Achievement(
            id = "night_owl_30",
            name = "Midnight Master",
            description = "30 days of evening dedication",
            emoji = "🌙",
            category = AchievementCategory.SPECIAL,
            target = 30
        ),
        Achievement(
            id = "comeback_3",
            name = "Comeback Kid",
            description = "Return after missing 3+ days",
            emoji = "🔄",
            category = AchievementCategory.SPECIAL,
            target = 1
        ),
        Achievement(
            id = "comeback_7",
            name = "Resilient",
            description = "Return after missing 7+ days",
            emoji = "💪",
            category = AchievementCategory.SPECIAL,
            target = 1
        ),
        Achievement(
            id = "comeback_30",
            name = "Phoenix Rising",
            description = "Return after missing 30+ days - welcome back!",
            emoji = "🔥",
            category = AchievementCategory.SPECIAL,
            target = 1
        ),
        Achievement(
            id = "speed_demon",
            name = "Speed Demon",
            description = "Complete all habits within 2 hours of waking",
            emoji = "⚡",
            category = AchievementCategory.SPECIAL,
            target = 7
        ),
        Achievement(
            id = "habit_explorer",
            name = "Habit Explorer",
            description = "Try every habit category at least once",
            emoji = "🧭",
            category = AchievementCategory.SPECIAL,
            target = 7
        ),
        Achievement(
            id = "reflection_regular",
            name = "Thoughtful",
            description = "Complete weekly reflection 4 times",
            emoji = "💭",
            category = AchievementCategory.SPECIAL,
            target = 4
        ),
        Achievement(
            id = "reflection_master",
            name = "Deep Thinker",
            description = "Complete weekly reflection 12 times",
            emoji = "🧠",
            category = AchievementCategory.SPECIAL,
            target = 12
        ),
        Achievement(
            id = "intention_setter",
            name = "Intentional",
            description = "Set daily intentions 10 times",
            emoji = "🎯",
            category = AchievementCategory.SPECIAL,
            target = 10
        ),
        Achievement(
            id = "holiday_hero",
            name = "Holiday Hero",
            description = "Complete habits on 5 major holidays",
            emoji = "🎉",
            category = AchievementCategory.SPECIAL,
            target = 5
        ),
        Achievement(
            id = "season_survivor",
            name = "Season Survivor",
            description = "Maintain streak through a season change",
            emoji = "🍂",
            category = AchievementCategory.SPECIAL,
            target = 1
        ),
        Achievement(
            id = "new_year_new_you",
            name = "Fresh Start",
            description = "Complete all habits on January 1st",
            emoji = "🎊",
            category = AchievementCategory.SPECIAL,
            target = 1
        )
    )

    // ==================== COMMUNITY ACHIEVEMENTS (6) ====================
    // Social and sharing-based achievements

    private val communityAchievements = listOf(
        Achievement(
            id = "first_share",
            name = "Sharer",
            description = "Share your first progress update",
            emoji = "📤",
            category = AchievementCategory.SPECIAL,
            target = 1
        ),
        Achievement(
            id = "streak_share_7",
            name = "Proud Week",
            description = "Share a 7+ day streak",
            emoji = "📣",
            category = AchievementCategory.SPECIAL,
            target = 1
        ),
        Achievement(
            id = "achievement_share_5",
            name = "Trophy Shower",
            description = "Share 5 achievement unlocks",
            emoji = "🏆",
            category = AchievementCategory.SPECIAL,
            target = 5
        ),
        Achievement(
            id = "invite_friend",
            name = "Wellness Ambassador",
            description = "Invite a friend to join DailyWell",
            emoji = "👋",
            category = AchievementCategory.SPECIAL,
            target = 1
        ),
        Achievement(
            id = "family_plan",
            name = "Family Wellness",
            description = "Add a family member to your plan",
            emoji = "👨‍👩‍👧‍👦",
            category = AchievementCategory.SPECIAL,
            target = 1
        ),
        Achievement(
            id = "challenge_complete",
            name = "Challenger",
            description = "Complete your first community challenge",
            emoji = "🎖️",
            category = AchievementCategory.SPECIAL,
            target = 1
        )
    )

    // ==================== MILESTONE ACHIEVEMENTS (6) ====================
    // Cumulative progress achievements

    private val milestoneAchievements = listOf(
        Achievement(
            id = "entries_100",
            name = "Century of Habits",
            description = "Log 100 total habit completions",
            emoji = "💯",
            category = AchievementCategory.SPECIAL,
            target = 100
        ),
        Achievement(
            id = "entries_500",
            name = "Habit Hunter",
            description = "Log 500 total habit completions",
            emoji = "🎯",
            category = AchievementCategory.SPECIAL,
            target = 500
        ),
        Achievement(
            id = "entries_1000",
            name = "Thousand Club",
            description = "Log 1,000 total habit completions",
            emoji = "🏅",
            category = AchievementCategory.SPECIAL,
            target = 1000
        ),
        Achievement(
            id = "entries_5000",
            name = "Habit Legend",
            description = "Log 5,000 total habit completions",
            emoji = "👑",
            category = AchievementCategory.SPECIAL,
            target = 5000
        ),
        Achievement(
            id = "days_active_100",
            name = "100 Days Active",
            description = "Be active on 100 different days",
            emoji = "📆",
            category = AchievementCategory.SPECIAL,
            target = 100
        ),
        Achievement(
            id = "days_active_365",
            name = "Year of Wellness",
            description = "Be active on 365 different days",
            emoji = "🗓️",
            category = AchievementCategory.SPECIAL,
            target = 365
        )
    )

    // ==================== ALL ACHIEVEMENTS ====================

    val all: List<Achievement> = streakAchievements +
                                  habitAchievements +
                                  consistencyAchievements +
                                  specialAchievements +
                                  communityAchievements +
                                  milestoneAchievements

    val totalCount: Int = all.size // 75 achievements

    // ==================== LOOKUP METHODS ====================

    fun getById(id: String): Achievement? = all.find { it.id == id }

    fun getByCategory(category: AchievementCategory): List<Achievement> =
        all.filter { it.category == category }

    fun getStreakAchievements(): List<Achievement> = streakAchievements

    fun getHabitAchievements(): List<Achievement> = habitAchievements

    fun getConsistencyAchievements(): List<Achievement> = consistencyAchievements

    fun getSpecialAchievements(): List<Achievement> = specialAchievements

    // ==================== STREAK MILESTONE MAP ====================

    val streakMilestones: Map<Int, Achievement> = mapOf(
        1 to all.first { it.id == "streak_1" },
        3 to all.first { it.id == "streak_3" },
        7 to all.first { it.id == "streak_7" },
        14 to all.first { it.id == "streak_14" },
        21 to all.first { it.id == "streak_21" },
        30 to all.first { it.id == "streak_30" },
        45 to all.first { it.id == "streak_45" },
        66 to all.first { it.id == "streak_66" },
        90 to all.first { it.id == "streak_90" },
        100 to all.first { it.id == "streak_100" },
        150 to all.first { it.id == "streak_150" },
        180 to all.first { it.id == "streak_180" },
        270 to all.first { it.id == "streak_270" },
        365 to all.first { it.id == "streak_365" },
        500 to all.first { it.id == "streak_500" }
    )

    // ==================== HABIT MILESTONE MAPS ====================

    val habitMilestones: Map<String, Map<Int, Achievement>> = mapOf(
        "sleep" to mapOf(
            7 to all.first { it.id == "sleep_7" },
            30 to all.first { it.id == "sleep_30" },
            90 to all.first { it.id == "sleep_90" }
        ),
        "water" to mapOf(
            7 to all.first { it.id == "water_7" },
            30 to all.first { it.id == "water_30" },
            90 to all.first { it.id == "water_90" }
        ),
        "move" to mapOf(
            7 to all.first { it.id == "move_7" },
            30 to all.first { it.id == "move_30" },
            90 to all.first { it.id == "move_90" }
        ),
        "vegetables" to mapOf(
            7 to all.first { it.id == "veggies_7" },
            30 to all.first { it.id == "veggies_30" },
            90 to all.first { it.id == "veggies_90" }
        ),
        "calm" to mapOf(
            7 to all.first { it.id == "calm_7" },
            30 to all.first { it.id == "calm_30" },
            90 to all.first { it.id == "calm_90" }
        ),
        "connect" to mapOf(
            7 to all.first { it.id == "connect_7" },
            30 to all.first { it.id == "connect_30" },
            90 to all.first { it.id == "connect_90" }
        ),
        "unplug" to mapOf(
            7 to all.first { it.id == "unplug_7" },
            30 to all.first { it.id == "unplug_30" },
            90 to all.first { it.id == "unplug_90" }
        )
    )

    // ==================== PERFECT DAY MILESTONES ====================

    val perfectDayMilestones: Map<Int, Achievement> = mapOf(
        1 to all.first { it.id == "perfect_day_1" },
        3 to all.first { it.id == "perfect_day_3" },
        7 to all.first { it.id == "perfect_week" },
        14 to all.first { it.id == "perfect_fortnight" },
        30 to all.first { it.id == "perfect_month" }
    )

    // ==================== TOTAL ENTRIES MILESTONES ====================

    val totalEntriesMilestones: Map<Int, Achievement> = mapOf(
        100 to all.first { it.id == "entries_100" },
        500 to all.first { it.id == "entries_500" },
        1000 to all.first { it.id == "entries_1000" },
        5000 to all.first { it.id == "entries_5000" }
    )

    // ==================== UTILITY METHODS ====================

    /**
     * Check which streak achievement should be unlocked
     */
    fun getStreakAchievementForDays(days: Int): Achievement? {
        return streakMilestones.entries
            .filter { it.key <= days }
            .maxByOrNull { it.key }
            ?.value
    }

    /**
     * Get next streak milestone to aim for
     */
    fun getNextStreakMilestone(currentStreak: Int): Pair<Int, Achievement>? {
        return streakMilestones.entries
            .filter { it.key > currentStreak }
            .minByOrNull { it.key }
            ?.let { it.key to it.value }
    }

    /**
     * Get habit-specific achievement for consecutive days
     */
    fun getHabitAchievementForDays(habitId: String, days: Int): Achievement? {
        return habitMilestones[habitId]?.entries
            ?.filter { it.key <= days }
            ?.maxByOrNull { it.key }
            ?.value
    }

    /**
     * Get all achievements that could be unlocked at a given streak
     */
    fun getUnlockableStreakAchievements(currentStreak: Int): List<Achievement> {
        return streakMilestones.entries
            .filter { it.key <= currentStreak }
            .map { it.value }
    }

    /**
     * Calculate progress percentage toward next streak achievement
     */
    fun getStreakProgress(currentStreak: Int): Float {
        val next = getNextStreakMilestone(currentStreak)
        if (next == null) return 1f // All unlocked

        val previous = streakMilestones.entries
            .filter { it.key < next.first }
            .maxByOrNull { it.key }?.key ?: 0

        val range = next.first - previous
        val progress = currentStreak - previous
        return (progress.toFloat() / range).coerceIn(0f, 1f)
    }
}
