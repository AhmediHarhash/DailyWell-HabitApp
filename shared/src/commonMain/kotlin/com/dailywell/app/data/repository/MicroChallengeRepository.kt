package com.dailywell.app.data.repository

import com.dailywell.app.data.content.MicroChallengesDatabase
import com.dailywell.app.data.content.MicroChallengesDatabase.MicroChallenge
import com.dailywell.app.data.content.MicroChallengesDatabase.ChallengeCategory
import com.dailywell.app.data.content.MicroChallengesDatabase.ChallengeDifficulty
import kotlinx.coroutines.flow.Flow

/**
 * MicroChallengeRepository - 365 Unique Daily Micro-Challenges
 *
 * PRODUCTION-READY: Full end-to-end pipeline with:
 * - Daily challenge selection based on day of year (365 unique)
 * - Completion tracking with Firebase persistence
 * - Streak tracking for consecutive challenge completions
 * - Category and difficulty-based filtering
 * - Contextual challenge suggestions
 */

data class DailyMicroChallengeWithMeta(
    val challenge: MicroChallenge,
    val dayOfYear: Int,
    val isCompleted: Boolean = false,
    val completedAt: String? = null,
    val challengeStreak: Int = 0,
    val categoryProgress: Map<ChallengeCategory, Int> = emptyMap()
)

data class MicroChallengeStats(
    val totalCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val categoryBreakdown: Map<ChallengeCategory, Int> = emptyMap(),
    val difficultyBreakdown: Map<ChallengeDifficulty, Int> = emptyMap(),
    val averageCompletionRate: Float = 0f,
    val favoriteCategory: ChallengeCategory? = null
)

interface MicroChallengeRepository {

    // ==================== DAILY CHALLENGE ====================

    /**
     * Get today's unique micro-challenge (based on day of year)
     * Returns the same challenge for all users on the same day
     */
    fun getTodayChallenge(): Flow<DailyMicroChallengeWithMeta>

    /**
     * Get challenge for a specific day of year (1-365)
     */
    suspend fun getChallengeForDay(dayOfYear: Int): DailyMicroChallengeWithMeta

    /**
     * Mark today's challenge as completed
     */
    suspend fun completeTodayChallenge(): DailyMicroChallengeWithMeta

    /**
     * Skip today's challenge (breaks streak)
     */
    suspend fun skipTodayChallenge()

    // ==================== CONTEXTUAL CHALLENGES ====================

    /**
     * Get a contextual challenge based on user preferences
     */
    suspend fun getContextualChallenge(
        preferIndoor: Boolean = false,
        preferredDifficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM,
        isWeekend: Boolean = false
    ): MicroChallenge

    /**
     * Get challenges by category
     */
    fun getChallengesByCategory(category: ChallengeCategory): List<MicroChallenge>

    /**
     * Get quick challenges (under 15 minutes)
     */
    fun getQuickChallenges(): List<MicroChallenge>

    /**
     * Search challenges by keyword
     */
    fun searchChallenges(query: String): List<MicroChallenge>

    // ==================== HISTORY & STATS ====================

    /**
     * Get completion history for the last N days
     */
    fun getCompletionHistory(days: Int = 30): Flow<List<DailyMicroChallengeWithMeta>>

    /**
     * Get micro-challenge statistics
     */
    fun getStats(): Flow<MicroChallengeStats>

    /**
     * Check if today's challenge is completed
     */
    suspend fun isTodayChallengeCompleted(): Boolean

    /**
     * Get current challenge streak
     */
    suspend fun getChallengeStreak(): Int

    // ==================== SYNC ====================

    /**
     * Sync challenge progress with Firebase
     */
    suspend fun syncWithCloud()
}
