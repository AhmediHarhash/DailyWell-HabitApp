package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

// ============== XP & LEVELS ==============

@Serializable
data class GamificationData(
    val userId: String = "",
    val totalXp: Long = 0,
    val currentLevel: Int = 1,
    val xpToNextLevel: Long = 100,
    val lifetimeXp: Long = 0,
    val dailyXp: Long = 0,
    val weeklyXp: Long = 0,
    val monthlyXp: Long = 0,
    val lastXpResetDate: String = "",
    val lastWeeklyResetDate: String = "",
    val lastMonthlyResetDate: String = "",

    // Streak shields
    val streakShields: Int = 1, // Start with 1 free
    val streakShieldsUsed: Int = 0,
    val lastShieldUsedDate: String? = null,

    // Daily rewards
    val lastDailyRewardDate: String? = null,
    val dailyRewardStreak: Int = 0,
    val lastSpinDate: String? = null,
    val totalSpins: Int = 0,

    // Unlocked content
    val unlockedThemes: Set<String> = setOf("default"),
    val unlockedAvatarFrames: Set<String> = setOf("default"),
    val unlockedHabitIcons: Set<String> = emptySet(),
    val selectedTheme: String = "default",
    val selectedAvatarFrame: String = "default",

    // Stats
    val totalHabitsCompleted: Long = 0,
    val perfectDays: Int = 0,
    val perfectWeeks: Int = 0,
    val longestStreak: Int = 0,
    val currentStreak: Int = 0,
    val duelsWon: Int = 0,
    val challengesCompleted: Int = 0,
    val friendsHelped: Int = 0
)

@Serializable
data class XpTransaction(
    val id: String,
    val amount: Long,
    val reason: XpReason,
    val habitId: String? = null,
    val timestamp: String,
    val bonusMultiplier: Float = 1f
)

@Serializable
enum class XpReason {
    HABIT_COMPLETED,
    ALL_HABITS_COMPLETED,
    STREAK_BONUS,
    PERFECT_WEEK,
    PERFECT_MONTH,
    EARLY_BIRD,        // Before 9 AM
    MORNING_CHAMPION,  // Before noon
    CHALLENGE_WIN,
    DUEL_WIN,
    DAILY_LOGIN,
    DAILY_SPIN,
    FRIEND_HELPED,
    LEVEL_UP_BONUS,
    ACHIEVEMENT_UNLOCKED,
    REFERRAL_BONUS,
    COMEBACK_BONUS,
    FIRST_HABIT_OF_DAY
}

object XpValues {
    const val HABIT_COMPLETED = 10L
    const val ALL_HABITS_BONUS = 20L
    const val STREAK_MULTIPLIER = 5L // per day of streak
    const val PERFECT_WEEK = 200L
    const val PERFECT_WEEK_BONUS = 100L
    const val PERFECT_MONTH = 1000L
    const val EARLY_BIRD_BONUS = 15L
    const val MORNING_CHAMPION_BONUS = 10L
    const val FIRST_HABIT_OF_DAY = 5L
    const val CHALLENGE_WIN = 500L
    const val DUEL_WIN = 250L
    const val DAILY_LOGIN = 10L
    const val DAILY_SPIN_BASE = 5L
    const val FRIEND_HELPED = 25L
    const val LEVEL_UP_BONUS = 50L
    const val ACHIEVEMENT_BONUS = 100L
    const val REFERRAL_BONUS = 500L
    const val COMEBACK_BONUS = 50L
}

object LevelSystem {
    // XP required for each level (exponential curve)
    fun xpForLevel(level: Int): Long {
        return when {
            level <= 1 -> 0
            level <= 10 -> (level - 1) * 100L
            level <= 25 -> 900 + (level - 10) * 150L
            level <= 50 -> 3150 + (level - 25) * 250L
            level <= 75 -> 9400 + (level - 50) * 400L
            level <= 100 -> 19400 + (level - 75) * 600L
            else -> 34400 + (level - 100) * 1000L
        }
    }

    fun levelForXp(xp: Long): Int {
        var level = 1
        while (xpForLevel(level + 1) <= xp) {
            level++
        }
        return level
    }

    fun xpToNextLevel(currentXp: Long): Long {
        val currentLevel = levelForXp(currentXp)
        return xpForLevel(currentLevel + 1) - currentXp
    }

    fun progressToNextLevel(currentXp: Long): Float {
        val currentLevel = levelForXp(currentXp)
        val currentLevelXp = xpForLevel(currentLevel)
        val nextLevelXp = xpForLevel(currentLevel + 1)
        val xpInCurrentLevel = currentXp - currentLevelXp
        val xpNeededForLevel = nextLevelXp - currentLevelXp
        return (xpInCurrentLevel.toFloat() / xpNeededForLevel).coerceIn(0f, 1f)
    }

    fun getLevelTitle(level: Int): LevelTitle {
        return when {
            level <= 10 -> LevelTitle("Seedling", "🌱", "Just starting your wellness journey")
            level <= 25 -> LevelTitle("Sprout", "🌿", "Growing stronger every day")
            level <= 50 -> LevelTitle("Growing", "🌳", "Building solid foundations")
            level <= 75 -> LevelTitle("Thriving", "🌲", "Your habits are flourishing")
            level <= 99 -> LevelTitle("Master", "⭐", "A true habit master")
            else -> LevelTitle("Legend", "👑", "An inspiration to all")
        }
    }
}

@Serializable
data class LevelTitle(
    val name: String,
    val emoji: String,
    val description: String
)

// ============== ACHIEVEMENTS/BADGES ==============

@Serializable
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val category: BadgeCategory,
    val tier: BadgeTier,
    val requirement: BadgeRequirement,
    val xpReward: Long = 100,
    val isSecret: Boolean = false
)

@Serializable
enum class BadgeCategory {
    STREAKS,
    COMPLETION,
    SOCIAL,
    CHALLENGES,
    MILESTONES,
    SPECIAL,
    HABITS,
    TIME_BASED
}

@Serializable
enum class BadgeTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND
}

@Serializable
sealed class BadgeRequirement {
    @Serializable
    data class StreakDays(val days: Int) : BadgeRequirement()
    @Serializable
    data class TotalHabits(val count: Long) : BadgeRequirement()
    @Serializable
    data class PerfectDays(val count: Int) : BadgeRequirement()
    @Serializable
    data class PerfectWeeks(val count: Int) : BadgeRequirement()
    @Serializable
    data class Level(val level: Int) : BadgeRequirement()
    @Serializable
    data class XpEarned(val xp: Long) : BadgeRequirement()
    @Serializable
    data class ChallengesWon(val count: Int) : BadgeRequirement()
    @Serializable
    data class DuelsWon(val count: Int) : BadgeRequirement()
    @Serializable
    data class FriendsHelped(val count: Int) : BadgeRequirement()
    @Serializable
    data class EarlyBirdDays(val count: Int) : BadgeRequirement()
    @Serializable
    data class HabitSpecific(val habitId: String, val completions: Int) : BadgeRequirement()
    @Serializable
    data class DailyLoginStreak(val days: Int) : BadgeRequirement()
    @Serializable
    object FirstHabit : BadgeRequirement()
    @Serializable
    object FirstPerfectDay : BadgeRequirement()
    @Serializable
    object FirstChallenge : BadgeRequirement()
    @Serializable
    object FirstDuel : BadgeRequirement()
    @Serializable
    object JoinedCommunity : BadgeRequirement()
}

@Serializable
data class UnlockedBadge(
    val badgeId: String,
    val unlockedAt: String,
    val xpAwarded: Long
)

object BadgeLibrary {
    val allBadges: List<Badge> = listOf(
        // STREAK BADGES
        Badge("streak_3", "Hat Trick", "3-day streak", "🎩", BadgeCategory.STREAKS, BadgeTier.BRONZE, BadgeRequirement.StreakDays(3), 50),
        Badge("streak_7", "Week Warrior", "7-day streak", "⚔️", BadgeCategory.STREAKS, BadgeTier.BRONZE, BadgeRequirement.StreakDays(7), 100),
        Badge("streak_14", "Fortnight Force", "14-day streak", "🛡️", BadgeCategory.STREAKS, BadgeTier.SILVER, BadgeRequirement.StreakDays(14), 200),
        Badge("streak_21", "Habit Forming", "21-day streak - habits are forming!", "🧠", BadgeCategory.STREAKS, BadgeTier.SILVER, BadgeRequirement.StreakDays(21), 300),
        Badge("streak_30", "Month Master", "30-day streak", "📅", BadgeCategory.STREAKS, BadgeTier.GOLD, BadgeRequirement.StreakDays(30), 500),
        Badge("streak_60", "Dedicated", "60-day streak", "💪", BadgeCategory.STREAKS, BadgeTier.GOLD, BadgeRequirement.StreakDays(60), 750),
        Badge("streak_90", "Quarter Champion", "90-day streak", "🏆", BadgeCategory.STREAKS, BadgeTier.PLATINUM, BadgeRequirement.StreakDays(90), 1000),
        Badge("streak_180", "Half Year Hero", "180-day streak", "🦸", BadgeCategory.STREAKS, BadgeTier.PLATINUM, BadgeRequirement.StreakDays(180), 2000),
        Badge("streak_365", "Year Legend", "365-day streak", "👑", BadgeCategory.STREAKS, BadgeTier.DIAMOND, BadgeRequirement.StreakDays(365), 5000),

        // COMPLETION BADGES
        Badge("habits_10", "Getting Started", "Complete 10 habits", "🚀", BadgeCategory.COMPLETION, BadgeTier.BRONZE, BadgeRequirement.TotalHabits(10), 50),
        Badge("habits_50", "Building Momentum", "Complete 50 habits", "🔥", BadgeCategory.COMPLETION, BadgeTier.BRONZE, BadgeRequirement.TotalHabits(50), 100),
        Badge("habits_100", "Century Club", "Complete 100 habits", "💯", BadgeCategory.COMPLETION, BadgeTier.SILVER, BadgeRequirement.TotalHabits(100), 200),
        Badge("habits_500", "High Achiever", "Complete 500 habits", "🌟", BadgeCategory.COMPLETION, BadgeTier.GOLD, BadgeRequirement.TotalHabits(500), 500),
        Badge("habits_1000", "Habit Legend", "Complete 1000 habits", "🏅", BadgeCategory.COMPLETION, BadgeTier.PLATINUM, BadgeRequirement.TotalHabits(1000), 1000),
        Badge("habits_5000", "Unstoppable", "Complete 5000 habits", "⚡", BadgeCategory.COMPLETION, BadgeTier.DIAMOND, BadgeRequirement.TotalHabits(5000), 2500),

        // PERFECT DAY BADGES
        Badge("perfect_1", "First Step", "First perfect day", "👣", BadgeCategory.MILESTONES, BadgeTier.BRONZE, BadgeRequirement.FirstPerfectDay, 50),
        Badge("perfect_7", "Perfect Week", "7 perfect days", "📆", BadgeCategory.MILESTONES, BadgeTier.SILVER, BadgeRequirement.PerfectDays(7), 200),
        Badge("perfect_30", "Perfect Month", "30 perfect days", "🗓️", BadgeCategory.MILESTONES, BadgeTier.GOLD, BadgeRequirement.PerfectDays(30), 500),
        Badge("perfect_100", "Perfectionist", "100 perfect days", "✨", BadgeCategory.MILESTONES, BadgeTier.PLATINUM, BadgeRequirement.PerfectDays(100), 1000),

        // LEVEL BADGES
        Badge("level_10", "Rising Star", "Reach level 10", "⭐", BadgeCategory.MILESTONES, BadgeTier.BRONZE, BadgeRequirement.Level(10), 100),
        Badge("level_25", "Established", "Reach level 25", "🌟", BadgeCategory.MILESTONES, BadgeTier.SILVER, BadgeRequirement.Level(25), 250),
        Badge("level_50", "Veteran", "Reach level 50", "💫", BadgeCategory.MILESTONES, BadgeTier.GOLD, BadgeRequirement.Level(50), 500),
        Badge("level_75", "Elite", "Reach level 75", "🔱", BadgeCategory.MILESTONES, BadgeTier.PLATINUM, BadgeRequirement.Level(75), 1000),
        Badge("level_100", "Legendary", "Reach level 100", "👑", BadgeCategory.MILESTONES, BadgeTier.DIAMOND, BadgeRequirement.Level(100), 2500),

        // SOCIAL BADGES
        Badge("friend_1", "Social Butterfly", "Help 1 friend", "🦋", BadgeCategory.SOCIAL, BadgeTier.BRONZE, BadgeRequirement.FriendsHelped(1), 50),
        Badge("friend_10", "Accountability Pro", "Help 10 friends", "🤝", BadgeCategory.SOCIAL, BadgeTier.SILVER, BadgeRequirement.FriendsHelped(10), 200),
        Badge("friend_50", "Community Leader", "Help 50 friends", "👥", BadgeCategory.SOCIAL, BadgeTier.GOLD, BadgeRequirement.FriendsHelped(50), 500),
        Badge("community", "Community Member", "Join the community", "🌍", BadgeCategory.SOCIAL, BadgeTier.BRONZE, BadgeRequirement.JoinedCommunity, 50),

        // CHALLENGE BADGES
        Badge("challenge_1", "Challenger", "Complete first challenge", "🎯", BadgeCategory.CHALLENGES, BadgeTier.BRONZE, BadgeRequirement.FirstChallenge, 100),
        Badge("challenge_5", "Challenge Seeker", "Win 5 challenges", "🏹", BadgeCategory.CHALLENGES, BadgeTier.SILVER, BadgeRequirement.ChallengesWon(5), 250),
        Badge("challenge_25", "Challenge Master", "Win 25 challenges", "🎖️", BadgeCategory.CHALLENGES, BadgeTier.GOLD, BadgeRequirement.ChallengesWon(25), 750),
        Badge("duel_1", "Duelist", "Win first duel", "⚔️", BadgeCategory.CHALLENGES, BadgeTier.BRONZE, BadgeRequirement.FirstDuel, 100),
        Badge("duel_10", "Duel Champion", "Win 10 duels", "🗡️", BadgeCategory.CHALLENGES, BadgeTier.SILVER, BadgeRequirement.DuelsWon(10), 300),
        Badge("duel_50", "Duel Legend", "Win 50 duels", "👊", BadgeCategory.CHALLENGES, BadgeTier.GOLD, BadgeRequirement.DuelsWon(50), 750),

        // TIME-BASED BADGES
        Badge("early_bird_7", "Early Bird", "Complete habits before 9 AM for 7 days", "🐦", BadgeCategory.TIME_BASED, BadgeTier.BRONZE, BadgeRequirement.EarlyBirdDays(7), 100),
        Badge("early_bird_30", "Dawn Warrior", "Complete habits before 9 AM for 30 days", "🌅", BadgeCategory.TIME_BASED, BadgeTier.SILVER, BadgeRequirement.EarlyBirdDays(30), 300),
        Badge("login_7", "Committed", "Log in 7 days in a row", "📱", BadgeCategory.TIME_BASED, BadgeTier.BRONZE, BadgeRequirement.DailyLoginStreak(7), 100),
        Badge("login_30", "Dedicated User", "Log in 30 days in a row", "🔒", BadgeCategory.TIME_BASED, BadgeTier.SILVER, BadgeRequirement.DailyLoginStreak(30), 300),

        // HABIT-SPECIFIC BADGES
        Badge("sleep_master", "Sleep Master", "Complete Sleep habit 100 times", "😴", BadgeCategory.HABITS, BadgeTier.GOLD, BadgeRequirement.HabitSpecific("sleep", 100), 300),
        Badge("hydration_hero", "Hydration Hero", "Complete Water habit 100 times", "💧", BadgeCategory.HABITS, BadgeTier.GOLD, BadgeRequirement.HabitSpecific("water", 100), 300),
        Badge("movement_master", "Movement Master", "Complete Move habit 100 times", "🏃", BadgeCategory.HABITS, BadgeTier.GOLD, BadgeRequirement.HabitSpecific("move", 100), 300),

        // SPECIAL/SECRET BADGES
        Badge("first_habit", "Welcome!", "Complete your first habit", "🎉", BadgeCategory.SPECIAL, BadgeTier.BRONZE, BadgeRequirement.FirstHabit, 25),
        Badge("comeback", "Comeback Kid", "Return after 7+ days away", "🔄", BadgeCategory.SPECIAL, BadgeTier.BRONZE, BadgeRequirement.DailyLoginStreak(1), 100, isSecret = true),
        Badge("night_owl", "Night Owl", "Complete habits after midnight", "🦉", BadgeCategory.SPECIAL, BadgeTier.BRONZE, BadgeRequirement.TotalHabits(1), 50, isSecret = true),
        Badge("weekend_warrior", "Weekend Warrior", "Perfect Saturday and Sunday", "🎊", BadgeCategory.SPECIAL, BadgeTier.SILVER, BadgeRequirement.PerfectDays(2), 150, isSecret = true)
    )

    fun getBadgeById(id: String): Badge? = allBadges.find { it.id == id }

    fun getBadgesByCategory(category: BadgeCategory): List<Badge> =
        allBadges.filter { it.category == category }

    fun getBadgesByTier(tier: BadgeTier): List<Badge> =
        allBadges.filter { it.tier == tier }
}

// ============== DAILY REWARDS ==============

@Serializable
data class DailyReward(
    val day: Int, // 1-7 for the week cycle
    val xpReward: Long,
    val bonusReward: DailyBonusReward? = null
)

@Serializable
sealed class DailyBonusReward {
    @Serializable
    data class StreakShield(val count: Int = 1) : DailyBonusReward()
    @Serializable
    data class ThemeUnlock(val themeId: String) : DailyBonusReward()
    @Serializable
    data class XpMultiplier(val multiplier: Float, val durationHours: Int) : DailyBonusReward()
    @Serializable
    data class BadgeProgress(val badgeId: String, val progress: Int) : DailyBonusReward()
}

object DailyRewardSchedule {
    val weeklyRewards = listOf(
        DailyReward(1, 10),
        DailyReward(2, 15),
        DailyReward(3, 20),
        DailyReward(4, 25),
        DailyReward(5, 30, DailyBonusReward.StreakShield(1)),
        DailyReward(6, 40),
        DailyReward(7, 100, DailyBonusReward.XpMultiplier(1.5f, 24)) // Big reward + 1.5x XP for 24h
    )

    fun getRewardForDay(day: Int): DailyReward {
        val index = ((day - 1) % 7)
        return weeklyRewards[index]
    }
}

// ============== SPIN WHEEL ==============

@Serializable
data class SpinWheelResult(
    val reward: SpinReward,
    val timestamp: String
)

@Serializable
sealed class SpinReward {
    @Serializable
    data class Xp(val amount: Long) : SpinReward()
    @Serializable
    data class StreakShields(val count: Int) : SpinReward()
    @Serializable
    data class XpBoost(val multiplier: Float, val hours: Int) : SpinReward()
    @Serializable
    object ThemeTicket : SpinReward() // Can unlock a random theme
    @Serializable
    object TryAgain : SpinReward()
}

object SpinWheel {
    // Weighted probabilities (should sum to 100)
    val segments = listOf(
        SpinSegment(SpinReward.Xp(10), 25, "10 XP", "💫"),
        SpinSegment(SpinReward.Xp(25), 20, "25 XP", "⭐"),
        SpinSegment(SpinReward.Xp(50), 15, "50 XP", "🌟"),
        SpinSegment(SpinReward.Xp(100), 8, "100 XP", "💥"),
        SpinSegment(SpinReward.StreakShields(1), 10, "Streak Shield", "🛡️"),
        SpinSegment(SpinReward.XpBoost(1.5f, 2), 10, "1.5x XP (2h)", "🚀"),
        SpinSegment(SpinReward.XpBoost(2f, 1), 5, "2x XP (1h)", "⚡"),
        SpinSegment(SpinReward.ThemeTicket, 2, "Theme Ticket!", "🎨"),
        SpinSegment(SpinReward.TryAgain, 5, "Try Again", "🔄")
    )

    fun spin(): SpinReward {
        val random = (1..100).random()
        var cumulative = 0
        for (segment in segments) {
            cumulative += segment.probability
            if (random <= cumulative) {
                return segment.reward
            }
        }
        return SpinReward.Xp(10) // Fallback
    }
}

@Serializable
data class SpinSegment(
    val reward: SpinReward,
    val probability: Int,
    val displayText: String,
    val emoji: String
)

// ============== THEMES ==============

@Serializable
data class AppTheme(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val primaryColor: Long,
    val secondaryColor: Long,
    val backgroundColor: Long,
    val surfaceColor: Long,
    val isUnlockedByDefault: Boolean = false,
    val unlockRequirement: ThemeUnlockRequirement? = null
)

@Serializable
sealed class ThemeUnlockRequirement {
    @Serializable
    data class Level(val level: Int) : ThemeUnlockRequirement()
    @Serializable
    data class Badge(val badgeId: String) : ThemeUnlockRequirement()
    @Serializable
    data class Xp(val totalXp: Long) : ThemeUnlockRequirement()
    @Serializable
    object SpinWheelTicket : ThemeUnlockRequirement()
    @Serializable
    object Premium : ThemeUnlockRequirement()
}

object ThemeLibrary {
    val allThemes = listOf(
        AppTheme(
            id = "default",
            name = "DailyWell Green",
            emoji = "🌿",
            description = "The classic DailyWell look",
            primaryColor = 0xFF4CAF50,
            secondaryColor = 0xFF81C784,
            backgroundColor = 0xFFF5F5F5,
            surfaceColor = 0xFFFFFFFF,
            isUnlockedByDefault = true
        ),
        AppTheme(
            id = "dark",
            name = "Dark Mode",
            emoji = "🌙",
            description = "Easy on the eyes",
            primaryColor = 0xFF66BB6A,
            secondaryColor = 0xFF4CAF50,
            backgroundColor = 0xFF121212,
            surfaceColor = 0xFF1E1E1E,
            unlockRequirement = ThemeUnlockRequirement.Level(5)
        ),
        AppTheme(
            id = "ocean",
            name = "Ocean Calm",
            emoji = "🌊",
            description = "Peaceful blue waters",
            primaryColor = 0xFF0288D1,
            secondaryColor = 0xFF4FC3F7,
            backgroundColor = 0xFFE3F2FD,
            surfaceColor = 0xFFFFFFFF,
            unlockRequirement = ThemeUnlockRequirement.Level(10)
        ),
        AppTheme(
            id = "sunset",
            name = "Sunset Glow",
            emoji = "🌅",
            description = "Warm and inviting",
            primaryColor = 0xFFFF7043,
            secondaryColor = 0xFFFFAB91,
            backgroundColor = 0xFFFFF3E0,
            surfaceColor = 0xFFFFFFFF,
            unlockRequirement = ThemeUnlockRequirement.Level(15)
        ),
        AppTheme(
            id = "nature",
            name = "Forest",
            emoji = "🌲",
            description = "Deep in the woods",
            primaryColor = 0xFF2E7D32,
            secondaryColor = 0xFF66BB6A,
            backgroundColor = 0xFFE8F5E9,
            surfaceColor = 0xFFFFFFFF,
            unlockRequirement = ThemeUnlockRequirement.Level(20)
        ),
        AppTheme(
            id = "minimal",
            name = "Minimal",
            emoji = "⬜",
            description = "Clean and simple",
            primaryColor = 0xFF424242,
            secondaryColor = 0xFF757575,
            backgroundColor = 0xFFFAFAFA,
            surfaceColor = 0xFFFFFFFF,
            unlockRequirement = ThemeUnlockRequirement.Level(25)
        ),
        AppTheme(
            id = "lavender",
            name = "Lavender Dreams",
            emoji = "💜",
            description = "Soft and calming",
            primaryColor = 0xFF7E57C2,
            secondaryColor = 0xFFB39DDB,
            backgroundColor = 0xFFF3E5F5,
            surfaceColor = 0xFFFFFFFF,
            unlockRequirement = ThemeUnlockRequirement.Badge("streak_30")
        ),
        AppTheme(
            id = "gold",
            name = "Golden Hour",
            emoji = "✨",
            description = "For the elite achievers",
            primaryColor = 0xFFFFB300,
            secondaryColor = 0xFFFFD54F,
            backgroundColor = 0xFFFFFDE7,
            surfaceColor = 0xFFFFFFFF,
            unlockRequirement = ThemeUnlockRequirement.Level(50)
        ),
        AppTheme(
            id = "champion",
            name = "Champion",
            emoji = "🏆",
            description = "Reserved for legends",
            primaryColor = 0xFFE65100,
            secondaryColor = 0xFFFF9800,
            backgroundColor = 0xFFFFF8E1,
            surfaceColor = 0xFFFFFFFF,
            unlockRequirement = ThemeUnlockRequirement.Level(100)
        )
    )

    fun getThemeById(id: String): AppTheme? = allThemes.find { it.id == id }

    fun getUnlockedThemes(level: Int, unlockedBadges: Set<String>, purchasedThemes: Set<String>): List<AppTheme> {
        return allThemes.filter { theme ->
            theme.isUnlockedByDefault ||
            purchasedThemes.contains(theme.id) ||
            when (val req = theme.unlockRequirement) {
                is ThemeUnlockRequirement.Level -> level >= req.level
                is ThemeUnlockRequirement.Badge -> unlockedBadges.contains(req.badgeId)
                else -> false
            }
        }
    }
}

// ============== PHASE 5B: CHALLENGES ==============

@Serializable
data class Challenge(
    val id: String,
    val type: ChallengeType,
    val title: String,
    val description: String,
    val emoji: String,
    val goal: ChallengeGoal,
    val duration: ChallengeDuration,
    val rewards: ChallengeRewards,
    val difficulty: ChallengeDifficulty,
    val startDate: String? = null, // null for solo challenges (start when joined)
    val endDate: String? = null,
    val creatorId: String? = null, // for user-created challenges
    val participantLimit: Int? = null, // null for unlimited
    val isPublic: Boolean = true,
    val tags: List<String> = emptyList()
)

@Serializable
enum class ChallengeType {
    SOLO,           // Personal challenge
    DUEL,           // 1v1 with a friend
    GROUP,          // Small group (3-10 people)
    COMMUNITY,      // Large scale (unlimited)
    SEASONAL        // Time-limited event
}

@Serializable
enum class ChallengeDifficulty {
    EASY,
    MEDIUM,
    HARD,
    EXTREME
}

@Serializable
sealed class ChallengeGoal {
    @Serializable
    data class TotalHabits(val count: Int) : ChallengeGoal()
    @Serializable
    data class StreakDays(val days: Int) : ChallengeGoal()
    @Serializable
    data class PerfectDays(val days: Int) : ChallengeGoal()
    @Serializable
    data class SpecificHabit(val habitType: String, val count: Int) : ChallengeGoal()
    @Serializable
    data class EarlyBird(val beforeHour: Int, val days: Int) : ChallengeGoal()
    @Serializable
    data class TotalXp(val xp: Long) : ChallengeGoal()
    @Serializable
    data class ConsecutiveDays(val days: Int) : ChallengeGoal()
    @Serializable
    data class MultiGoal(val goals: List<ChallengeGoal>) : ChallengeGoal()
}

@Serializable
data class ChallengeDuration(
    val days: Int,
    val type: DurationType = DurationType.FIXED
)

@Serializable
enum class DurationType {
    FIXED,      // Specific number of days
    WEEKLY,     // Resets weekly
    MONTHLY,    // Resets monthly
    ONGOING     // No end date
}

@Serializable
data class ChallengeRewards(
    val xp: Long,
    val badge: String? = null,
    val themeUnlock: String? = null,
    val streakShields: Int = 0,
    val title: String? = null // Special title to display
)

@Serializable
data class ChallengeParticipant(
    val odId: String,
    val challengeId: String,
    val userId: String,
    val displayName: String,
    val avatarEmoji: String = "😊",
    val joinedAt: String,
    val progress: Float = 0f,
    val currentValue: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val rank: Int? = null
)

@Serializable
data class UserChallengeProgress(
    val odId: String,
    val challengeId: String,
    val currentValue: Int = 0,
    val targetValue: Int,
    val progressPercent: Float = 0f,
    val lastUpdated: String,
    val dailyProgress: Map<String, Int> = emptyMap(), // date -> value
    val isCompleted: Boolean = false,
    val completedAt: String? = null
)

@Serializable
data class ActiveChallenge(
    val challenge: Challenge,
    val userProgress: UserChallengeProgress,
    val participants: List<ChallengeParticipant> = emptyList(),
    val userRank: Int? = null,
    val totalParticipants: Int = 1
)

// ============== FRIEND DUELS ==============

@Serializable
data class Duel(
    val id: String,
    val challengerId: String,
    val challengerName: String,
    val challengerEmoji: String,
    val opponentId: String,
    val opponentName: String,
    val opponentEmoji: String,
    val goal: ChallengeGoal,
    val duration: ChallengeDuration,
    val stake: DuelStake,
    val status: DuelStatus,
    val createdAt: String,
    val startedAt: String? = null,
    val endsAt: String? = null,
    val challengerProgress: Int = 0,
    val opponentProgress: Int = 0,
    val winnerId: String? = null,
    val rewards: ChallengeRewards
)

@Serializable
enum class DuelStatus {
    PENDING,        // Waiting for opponent to accept
    ACTIVE,         // In progress
    COMPLETED,      // Finished
    DECLINED,       // Opponent declined
    CANCELLED,      // Challenger cancelled
    EXPIRED         // Invitation expired
}

@Serializable
sealed class DuelStake {
    @Serializable
    object Friendly : DuelStake() // Just for fun
    @Serializable
    data class XpWager(val amount: Long) : DuelStake() // Winner takes XP
    @Serializable
    data class BraggingRights(val title: String) : DuelStake() // Winner gets title
}

@Serializable
data class DuelInvitation(
    val duelId: String,
    val challengerName: String,
    val challengerEmoji: String,
    val goal: ChallengeGoal,
    val duration: ChallengeDuration,
    val stake: DuelStake,
    val expiresAt: String
)

// ============== COMMUNITY CHALLENGES ==============

@Serializable
data class CommunityChallenge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val goal: CommunityChallengeGoal,
    val startDate: String,
    val endDate: String,
    val totalParticipants: Int = 0,
    val globalProgress: Long = 0,
    val globalTarget: Long,
    val rewards: CommunityRewards,
    val tiers: List<CommunityTier> = emptyList(),
    val isActive: Boolean = true
)

@Serializable
data class CommunityChallengeGoal(
    val type: CommunityGoalType,
    val target: Long,
    val description: String
)

@Serializable
enum class CommunityGoalType {
    TOTAL_HABITS,           // Community completes X habits total
    TOTAL_PERFECT_DAYS,     // Community achieves X perfect days
    TOTAL_STREAK_DAYS,      // Community maintains streaks
    TOTAL_XP,               // Community earns X XP
    SPECIFIC_HABIT          // Community completes specific habit type
}

@Serializable
data class CommunityRewards(
    val baseXp: Long,
    val completionBadge: String? = null,
    val themeUnlock: String? = null,
    val specialTitle: String? = null
)

@Serializable
data class CommunityTier(
    val tier: Int,
    val threshold: Float, // Percentage of goal (0.25, 0.5, 0.75, 1.0)
    val xpBonus: Long,
    val description: String
)

@Serializable
data class UserCommunityProgress(
    val challengeId: String,
    val contribution: Long = 0,
    val rank: Int? = null,
    val percentile: Float? = null,
    val tiersUnlocked: Int = 0
)

// ============== SEASONAL EVENTS ==============

@Serializable
data class SeasonalEvent(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val theme: String, // "winter", "spring", "summer", "fall", "halloween", "holiday", etc.
    val startDate: String,
    val endDate: String,
    val challenges: List<Challenge>,
    val exclusiveRewards: List<SeasonalReward>,
    val specialBadge: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class SeasonalReward(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val type: SeasonalRewardType,
    val requirement: SeasonalRequirement
)

@Serializable
enum class SeasonalRewardType {
    THEME,
    BADGE,
    AVATAR_FRAME,
    TITLE,
    XP_MULTIPLIER
}

@Serializable
sealed class SeasonalRequirement {
    @Serializable
    data class CompleteChallenges(val count: Int) : SeasonalRequirement()
    @Serializable
    data class EarnXp(val amount: Long) : SeasonalRequirement()
    @Serializable
    data class PerfectDays(val days: Int) : SeasonalRequirement()
    @Serializable
    object CompleteAll : SeasonalRequirement()
}

@Serializable
data class UserSeasonalProgress(
    val eventId: String,
    val challengesCompleted: Set<String> = emptySet(),
    val rewardsEarned: Set<String> = emptySet(),
    val totalXpEarned: Long = 0,
    val perfectDays: Int = 0
)

// ============== CHALLENGE CREATOR ==============

@Serializable
data class CustomChallengeTemplate(
    val id: String,
    val creatorId: String,
    val creatorName: String,
    val title: String,
    val description: String,
    val emoji: String,
    val goalType: ChallengeGoal,
    val duration: ChallengeDuration,
    val difficulty: ChallengeDifficulty,
    val isPublic: Boolean = false,
    val timesUsed: Int = 0,
    val averageCompletionRate: Float = 0f,
    val createdAt: String
)

// ============== CHALLENGE LIBRARY ==============

object ChallengeLibrary {

    val soloChallenges: List<Challenge> = listOf(
        // Beginner challenges
        Challenge(
            id = "solo_first_week",
            type = ChallengeType.SOLO,
            title = "First Week Champion",
            description = "Complete all habits for 7 days straight",
            emoji = "🌟",
            goal = ChallengeGoal.PerfectDays(7),
            duration = ChallengeDuration(7),
            rewards = ChallengeRewards(xp = 200, badge = "challenge_1"),
            difficulty = ChallengeDifficulty.EASY,
            tags = listOf("beginner", "streak")
        ),
        Challenge(
            id = "solo_early_riser",
            type = ChallengeType.SOLO,
            title = "Early Riser",
            description = "Complete habits before 9 AM for 5 days",
            emoji = "🌅",
            goal = ChallengeGoal.EarlyBird(beforeHour = 9, days = 5),
            duration = ChallengeDuration(7),
            rewards = ChallengeRewards(xp = 150),
            difficulty = ChallengeDifficulty.EASY,
            tags = listOf("morning", "time")
        ),
        Challenge(
            id = "solo_hydration_hero",
            type = ChallengeType.SOLO,
            title = "Hydration Hero",
            description = "Complete the Water habit 14 times",
            emoji = "💧",
            goal = ChallengeGoal.SpecificHabit(habitType = "water", count = 14),
            duration = ChallengeDuration(14),
            rewards = ChallengeRewards(xp = 250),
            difficulty = ChallengeDifficulty.MEDIUM,
            tags = listOf("water", "health")
        ),
        Challenge(
            id = "solo_sleep_master",
            type = ChallengeType.SOLO,
            title = "Sleep Master",
            description = "Get good sleep for 21 nights",
            emoji = "😴",
            goal = ChallengeGoal.SpecificHabit(habitType = "sleep", count = 21),
            duration = ChallengeDuration(30),
            rewards = ChallengeRewards(xp = 400),
            difficulty = ChallengeDifficulty.MEDIUM,
            tags = listOf("sleep", "health")
        ),
        Challenge(
            id = "solo_movement_marathon",
            type = ChallengeType.SOLO,
            title = "Movement Marathon",
            description = "Complete the Move habit 30 times",
            emoji = "🏃",
            goal = ChallengeGoal.SpecificHabit(habitType = "move", count = 30),
            duration = ChallengeDuration(30),
            rewards = ChallengeRewards(xp = 500, streakShields = 1),
            difficulty = ChallengeDifficulty.HARD,
            tags = listOf("movement", "fitness")
        ),
        Challenge(
            id = "solo_perfect_month",
            type = ChallengeType.SOLO,
            title = "Perfect Month",
            description = "Achieve 30 perfect days",
            emoji = "🏆",
            goal = ChallengeGoal.PerfectDays(30),
            duration = ChallengeDuration(45),
            rewards = ChallengeRewards(xp = 1000, badge = "challenge_5", streakShields = 2),
            difficulty = ChallengeDifficulty.EXTREME,
            tags = listOf("advanced", "perfectionist")
        ),
        Challenge(
            id = "solo_xp_hunter",
            type = ChallengeType.SOLO,
            title = "XP Hunter",
            description = "Earn 500 XP in one week",
            emoji = "⚡",
            goal = ChallengeGoal.TotalXp(500),
            duration = ChallengeDuration(7),
            rewards = ChallengeRewards(xp = 100),
            difficulty = ChallengeDifficulty.MEDIUM,
            tags = listOf("xp", "grind")
        ),
        Challenge(
            id = "solo_habit_spree",
            type = ChallengeType.SOLO,
            title = "Habit Spree",
            description = "Complete 50 habits total",
            emoji = "🔥",
            goal = ChallengeGoal.TotalHabits(50),
            duration = ChallengeDuration(14),
            rewards = ChallengeRewards(xp = 300),
            difficulty = ChallengeDifficulty.MEDIUM,
            tags = listOf("completion", "volume")
        ),
        Challenge(
            id = "solo_consistency_king",
            type = ChallengeType.SOLO,
            title = "Consistency King",
            description = "Maintain a 14-day streak",
            emoji = "👑",
            goal = ChallengeGoal.StreakDays(14),
            duration = ChallengeDuration(21),
            rewards = ChallengeRewards(xp = 350, streakShields = 1),
            difficulty = ChallengeDifficulty.HARD,
            tags = listOf("streak", "consistency")
        ),
        Challenge(
            id = "solo_triple_threat",
            type = ChallengeType.SOLO,
            title = "Triple Threat",
            description = "Complete Sleep, Water, and Move 10 times each",
            emoji = "💪",
            goal = ChallengeGoal.MultiGoal(listOf(
                ChallengeGoal.SpecificHabit("sleep", 10),
                ChallengeGoal.SpecificHabit("water", 10),
                ChallengeGoal.SpecificHabit("move", 10)
            )),
            duration = ChallengeDuration(14),
            rewards = ChallengeRewards(xp = 400),
            difficulty = ChallengeDifficulty.MEDIUM,
            tags = listOf("balanced", "all-habits")
        )
    )

    val duelTemplates: List<Challenge> = listOf(
        Challenge(
            id = "duel_weekly_warrior",
            type = ChallengeType.DUEL,
            title = "Weekly Warrior",
            description = "Who can complete more habits in a week?",
            emoji = "⚔️",
            goal = ChallengeGoal.TotalHabits(0), // Dynamic based on performance
            duration = ChallengeDuration(7),
            rewards = ChallengeRewards(xp = 200),
            difficulty = ChallengeDifficulty.MEDIUM
        ),
        Challenge(
            id = "duel_perfect_streak",
            type = ChallengeType.DUEL,
            title = "Perfect Streak Duel",
            description = "First to achieve 5 perfect days wins!",
            emoji = "🎯",
            goal = ChallengeGoal.PerfectDays(5),
            duration = ChallengeDuration(10),
            rewards = ChallengeRewards(xp = 300),
            difficulty = ChallengeDifficulty.HARD
        ),
        Challenge(
            id = "duel_xp_race",
            type = ChallengeType.DUEL,
            title = "XP Race",
            description = "First to earn 300 XP wins!",
            emoji = "🏁",
            goal = ChallengeGoal.TotalXp(300),
            duration = ChallengeDuration(5),
            rewards = ChallengeRewards(xp = 150),
            difficulty = ChallengeDifficulty.MEDIUM
        )
    )

    fun getSoloChallengeById(id: String): Challenge? = soloChallenges.find { it.id == id }

    fun getDuelTemplateById(id: String): Challenge? = duelTemplates.find { it.id == id }

    fun getChallengesByDifficulty(difficulty: ChallengeDifficulty): List<Challenge> =
        soloChallenges.filter { it.difficulty == difficulty }

    fun getChallengesByTag(tag: String): List<Challenge> =
        soloChallenges.filter { tag in it.tags }
}

// ============== PHASE 5C: LEADERBOARDS ==============

@Serializable
data class LeaderboardEntry(
    val odId: String,
    val userId: String,
    val displayName: String,
    val avatarEmoji: String = "😊",
    val level: Int,
    val rank: Int,
    val previousRank: Int? = null,
    val score: Long, // XP or other metric
    val streak: Int = 0,
    val perfectDays: Int = 0,
    val isFriend: Boolean = false,
    val isCurrentUser: Boolean = false,
    val updatedAt: String
)

@Serializable
enum class LeaderboardType {
    FRIENDS,        // Friends only
    GLOBAL,         // Everyone
    HABIT_SLEEP,    // Per-habit leaderboards
    HABIT_WATER,
    HABIT_MOVE,
    WEEKLY,         // Time-based
    MONTHLY,
    ALL_TIME
}

@Serializable
enum class LeaderboardMetric {
    TOTAL_XP,
    WEEKLY_XP,
    MONTHLY_XP,
    CURRENT_STREAK,
    LONGEST_STREAK,
    PERFECT_DAYS,
    HABITS_COMPLETED,
    CHALLENGES_WON,
    DUELS_WON
}

@Serializable
data class LeaderboardConfig(
    val type: LeaderboardType,
    val metric: LeaderboardMetric,
    val title: String,
    val emoji: String,
    val refreshIntervalMinutes: Int = 15
)

// ============== PHASE 5C: ACTIVITY FEED ==============

@Serializable
data class ActivityFeedItem(
    val id: String,
    val userId: String,
    val userName: String,
    val userEmoji: String,
    val type: ActivityType,
    val content: ActivityContent,
    val timestamp: String,
    val reactions: List<Reaction> = emptyList(),
    val isCurrentUser: Boolean = false
)

@Serializable
enum class ActivityType {
    HABIT_COMPLETED,
    PERFECT_DAY,
    STREAK_MILESTONE,
    BADGE_EARNED,
    LEVEL_UP,
    CHALLENGE_COMPLETED,
    CHALLENGE_JOINED,
    DUEL_WON,
    DUEL_STARTED,
    FRIEND_JOINED,
    COMEBACK
}

@Serializable
sealed class ActivityContent {
    @Serializable
    data class HabitCompleted(val habitName: String, val habitEmoji: String, val count: Int = 1) : ActivityContent()
    @Serializable
    data class PerfectDay(val dayNumber: Int) : ActivityContent()
    @Serializable
    data class StreakMilestone(val days: Int) : ActivityContent()
    @Serializable
    data class BadgeEarned(val badgeId: String, val badgeName: String, val badgeEmoji: String) : ActivityContent()
    @Serializable
    data class LevelUp(val newLevel: Int, val levelTitle: String) : ActivityContent()
    @Serializable
    data class ChallengeCompleted(val challengeName: String, val challengeEmoji: String) : ActivityContent()
    @Serializable
    data class ChallengeJoined(val challengeName: String, val challengeEmoji: String) : ActivityContent()
    @Serializable
    data class DuelWon(val opponentName: String, val score: String) : ActivityContent()
    @Serializable
    data class DuelStarted(val opponentName: String) : ActivityContent()
    @Serializable
    data class FriendJoined(val friendName: String) : ActivityContent()
    @Serializable
    data class Comeback(val daysAway: Int) : ActivityContent()
}

// ============== PHASE 5D: REACTIONS & CHEERS ==============

@Serializable
data class Reaction(
    val id: String,
    val userId: String,
    val userName: String,
    val type: ReactionType,
    val timestamp: String
)

@Serializable
enum class ReactionType(val emoji: String, val label: String) {
    LIKE("👍", "Like"),
    LOVE("❤️", "Love"),
    FIRE("🔥", "Fire"),
    CLAP("👏", "Clap"),
    STAR("⭐", "Star"),
    MUSCLE("💪", "Strong"),
    PARTY("🎉", "Party"),
    ROCKET("🚀", "Rocket")
}

@Serializable
data class Cheer(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserEmoji: String,
    val toUserId: String,
    val message: String,
    val cheerType: CheerType,
    val timestamp: String,
    val isRead: Boolean = false
)

@Serializable
enum class CheerType(val emoji: String, val defaultMessage: String) {
    ENCOURAGEMENT("💪", "You got this!"),
    CELEBRATION("🎉", "Amazing job!"),
    SUPPORT("🤗", "I believe in you!"),
    MOTIVATION("🚀", "Keep pushing!"),
    CONGRATULATIONS("🏆", "Congratulations!")
}

// ============== PHASE 5D: REFERRAL SYSTEM ==============

@Serializable
data class ReferralCode(
    val code: String,
    val userId: String,
    val createdAt: String,
    val usageCount: Int = 0,
    val maxUses: Int? = null, // null = unlimited
    val expiresAt: String? = null
)

@Serializable
data class ReferralReward(
    val forReferrer: ReferralBenefit,
    val forReferred: ReferralBenefit
)

@Serializable
sealed class ReferralBenefit {
    @Serializable
    data class Xp(val amount: Long) : ReferralBenefit()
    @Serializable
    data class StreakShields(val count: Int) : ReferralBenefit()
    @Serializable
    data class PremiumDays(val days: Int) : ReferralBenefit()
    @Serializable
    data class ThemeUnlock(val themeId: String) : ReferralBenefit()
}

@Serializable
enum class ReferralTier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND
}

@Serializable
data class ReferralStats(
    val totalReferrals: Int = 0,
    val successfulReferrals: Int = 0,
    val pendingReferrals: Int = 0,
    val totalXpEarned: Long = 0,
    val totalShieldsEarned: Int = 0,
    val currentTier: ReferralTier = ReferralTier.BRONZE
)

object ReferralConfig {
    val defaultReward = ReferralReward(
        forReferrer = ReferralBenefit.Xp(500),
        forReferred = ReferralBenefit.Xp(250)
    )

    val milestoneRewards = mapOf(
        5 to ReferralBenefit.StreakShields(2),
        10 to ReferralBenefit.ThemeUnlock("referral_gold"),
        25 to ReferralBenefit.Xp(2500),
        50 to ReferralBenefit.PremiumDays(7)
    )

    fun generateCode(userId: String): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = (1..6).map { chars.random() }.joinToString("")
        return "DW$random"
    }
}
