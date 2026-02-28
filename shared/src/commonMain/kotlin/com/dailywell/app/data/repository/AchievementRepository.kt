package com.dailywell.app.data.repository

import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.AchievementCategory
import kotlinx.coroutines.flow.Flow

/**
 * AchievementRepository - Production-Ready Achievement System
 *
 * Features:
 * - 75 unique, creative achievements
 * - Hybrid storage: Room (offline) + Firebase (cloud sync)
 * - Real-time achievement tracking and triggers
 * - Progress tracking toward locked achievements
 * - Celebration system for newly unlocked achievements
 *
 * Firestore Collections:
 * - users/{userId}/achievements - Unlocked achievements with metadata
 * - users/{userId}/achievement_progress - Progress toward locked achievements
 * - users/{userId}/achievement_stats - Aggregated statistics
 */

/**
 * Achievement with progress information
 */
data class AchievementWithProgress(
    val achievement: Achievement,
    val isUnlocked: Boolean,
    val unlockedAt: Long? = null,
    val currentProgress: Int = 0,
    val targetProgress: Int = 1,
    val progressPercent: Float = 0f
)

/**
 * Achievement statistics
 */
data class AchievementStats(
    val totalAchievements: Int,
    val unlockedCount: Int,
    val progressPercent: Float,
    val categoryBreakdown: Map<AchievementCategory, Pair<Int, Int>>, // unlocked to total
    val recentUnlocks: List<Achievement>,
    val nextAchievements: List<AchievementWithProgress>,
    val rareAchievements: List<Achievement>
)

/**
 * Celebration data for newly unlocked achievement
 */
data class AchievementCelebration(
    val achievement: Achievement,
    val unlockedAt: Long,
    val isRare: Boolean = false,
    val shareMessage: String
)

interface AchievementRepository {

    // ==================== CORE ACHIEVEMENT DATA ====================

    /**
     * Get all achievements with their unlock status and progress
     */
    fun getAllAchievements(): Flow<List<AchievementWithProgress>>

    /**
     * Get only unlocked achievements
     */
    fun getUnlockedAchievements(): Flow<List<Achievement>>

    /**
     * Get unlocked achievement IDs for quick lookups
     */
    fun getUnlockedAchievementIds(): Flow<Set<String>>

    /**
     * Get achievements by category with progress
     */
    fun getAchievementsByCategory(category: AchievementCategory): Flow<List<AchievementWithProgress>>

    /**
     * Get a specific achievement's current status
     */
    suspend fun getAchievement(achievementId: String): AchievementWithProgress?

    // ==================== ACHIEVEMENT UNLOCKING ====================

    /**
     * Unlock an achievement
     * Returns celebration data if newly unlocked, null if already unlocked
     */
    suspend fun unlockAchievement(
        achievementId: String,
        habitId: String? = null
    ): AchievementCelebration?

    /**
     * Check if achievement is already unlocked
     */
    suspend fun hasAchievement(achievementId: String): Boolean

    // ==================== AUTOMATIC ACHIEVEMENT TRIGGERS ====================

    /**
     * Check and unlock streak-based achievements
     * Called when user's streak is updated
     */
    suspend fun checkAndUnlockStreakAchievements(currentStreak: Int): List<AchievementCelebration>

    /**
     * Check and unlock habit-specific achievements
     * Called when a habit's consecutive days updates
     */
    suspend fun checkAndUnlockHabitAchievements(
        habitId: String,
        consecutiveDays: Int
    ): List<AchievementCelebration>

    /**
     * Check perfect day achievements
     * Called when all habits for a day are completed
     */
    suspend fun checkPerfectDayAchievement(
        allCompleted: Boolean,
        consecutivePerfectDays: Int
    ): AchievementCelebration?

    /**
     * Check consistency achievements (80%+ rates)
     */
    suspend fun checkConsistencyAchievements(
        weeklyRate: Float,
        monthlyRate: Float
    ): AchievementCelebration?

    /**
     * Check total entries milestone achievements
     */
    suspend fun checkTotalEntriesAchievements(totalEntries: Int): AchievementCelebration?

    /**
     * Check time-based achievements (early bird, night owl)
     */
    suspend fun checkTimeBasedAchievements(
        checkInHour: Int,
        consecutiveEarlyDays: Int,
        consecutiveLateDays: Int
    ): AchievementCelebration?

    /**
     * Check comeback achievements
     */
    suspend fun checkComebackAchievement(daysMissed: Int): AchievementCelebration?

    /**
     * Check special date achievements (holidays, new year, etc.)
     */
    suspend fun checkSpecialDateAchievements(
        month: Int,
        dayOfMonth: Int
    ): AchievementCelebration?

    // ==================== PROGRESS TRACKING ====================

    /**
     * Update progress toward an achievement
     * Used for achievements that require incremental progress
     */
    suspend fun updateAchievementProgress(
        achievementId: String,
        progress: Int
    )

    /**
     * Increment progress by 1
     */
    suspend fun incrementAchievementProgress(achievementId: String)

    /**
     * Get progress for a specific achievement
     */
    suspend fun getAchievementProgress(achievementId: String): Int

    // ==================== CELEBRATION SYSTEM ====================

    /**
     * Get recently unlocked achievement for celebration
     * Returns null if already celebrated
     */
    fun getRecentlyUnlocked(): Flow<AchievementCelebration?>

    /**
     * Clear the recently unlocked achievement after celebration
     */
    suspend fun clearRecentlyUnlocked()

    /**
     * Get pending celebrations (multiple unlocks in one session)
     */
    fun getPendingCelebrations(): Flow<List<AchievementCelebration>>

    /**
     * Dismiss a specific celebration
     */
    suspend fun dismissCelebration(achievementId: String)

    // ==================== STATISTICS ====================

    /**
     * Get comprehensive achievement statistics
     */
    suspend fun getAchievementStats(): AchievementStats

    /**
     * Get unlocked count
     */
    suspend fun getUnlockedCount(): Int

    /**
     * Get total achievement count
     */
    fun getTotalCount(): Int

    /**
     * Get next achievements user is close to unlocking
     */
    suspend fun getNextAchievements(limit: Int = 3): List<AchievementWithProgress>

    // ==================== SYNC & BACKUP ====================

    /**
     * Sync achievements with Firebase
     * Called on app start and after major changes
     */
    suspend fun syncWithCloud()

    /**
     * Restore achievements from Firebase
     * Called when user logs in on new device
     */
    suspend fun restoreFromCloud(): Int // Returns number restored

    /**
     * Export achievements as JSON
     */
    suspend fun exportAchievements(): String
}
