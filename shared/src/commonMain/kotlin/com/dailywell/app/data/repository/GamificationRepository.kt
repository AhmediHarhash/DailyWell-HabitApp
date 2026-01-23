package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

interface GamificationRepository {
    // XP & Levels
    fun getGamificationData(): Flow<GamificationData>
    suspend fun addXp(amount: Long, reason: XpReason, habitId: String? = null): XpTransaction
    suspend fun getCurrentLevel(): Int
    suspend fun getXpProgress(): Float

    // Badges
    fun getUnlockedBadges(): Flow<List<UnlockedBadge>>
    fun getAllBadgesWithStatus(): Flow<List<Pair<Badge, Boolean>>>
    suspend fun checkAndUnlockBadges(): List<Badge>
    suspend fun unlockBadge(badgeId: String): UnlockedBadge?

    // Daily Rewards
    suspend fun claimDailyReward(): DailyReward?
    suspend fun canClaimDailyReward(): Boolean
    fun getDailyRewardStreak(): Flow<Int>

    // Spin Wheel
    suspend fun spin(): SpinWheelResult?
    suspend fun canSpin(): Boolean
    fun getLastSpinResult(): Flow<SpinWheelResult?>

    // Streak Shields
    suspend fun useStreakShield(): Boolean
    fun getAvailableShields(): Flow<Int>
    suspend fun addStreakShield(count: Int = 1)

    // Themes
    fun getUnlockedThemes(): Flow<List<AppTheme>>
    fun getSelectedTheme(): Flow<AppTheme>
    suspend fun selectTheme(themeId: String): Boolean
    suspend fun unlockTheme(themeId: String): Boolean

    // Stats tracking
    suspend fun recordHabitCompletion(habitId: String, isAllCompleted: Boolean, isEarlyBird: Boolean, isMorning: Boolean)
    suspend fun recordPerfectDay()
    suspend fun recordPerfectWeek()
    suspend fun recordLogin()
    suspend fun updateStreak(currentStreak: Int, longestStreak: Int)
    suspend fun recordChallengeWin()
    suspend fun recordDuelWin()
    suspend fun recordFriendHelped()

    // Leaderboard data
    suspend fun getLeaderboardStats(): LeaderboardStats

    // Reset
    suspend fun resetDailyXp()
    suspend fun resetWeeklyXp()
    suspend fun resetMonthlyXp()
}

data class LeaderboardStats(
    val totalXp: Long,
    val weeklyXp: Long,
    val monthlyXp: Long,
    val level: Int,
    val currentStreak: Int,
    val perfectDays: Int,
    val habitsCompleted: Long
)
