package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * User Behavior Repository
 * Tracks user patterns and preferences for AI personalization.
 *
 * The AI coach learns from user behavior to provide personalized guidance:
 * - When they typically complete habits (chronotype detection)
 * - What motivation style works best (supportive vs direct vs analytical)
 * - How they recover from missed streaks
 * - Which habits correlate with each other
 * - Current engagement/attitude level
 *
 * This data is injected into Claude prompts for hyper-personalized coaching.
 */
interface UserBehaviorRepository {

    /**
     * Get the current user's behavior profile
     */
    fun getUserProfile(): Flow<UserBehaviorProfile?>

    /**
     * Get the AI prompt context for personalization
     */
    suspend fun getPromptContext(): String

    // ============ Event Tracking ============

    /**
     * Track a behavior event (habit completion, AI interaction, etc.)
     */
    suspend fun trackEvent(event: BehaviorEvent)

    /**
     * Track habit completion with timing info
     */
    suspend fun trackHabitCompletion(habitId: String, completedAt: String)

    /**
     * Track habit skip/dismiss
     */
    suspend fun trackHabitSkipped(habitId: String, reason: String? = null)

    /**
     * Track streak broken
     */
    suspend fun trackStreakBroken(habitId: String, streakLength: Int)

    /**
     * Track streak recovery (user bounced back after miss)
     */
    suspend fun trackStreakRecovery(habitId: String, daysAfterMiss: Int)

    /**
     * Track AI coaching interaction
     */
    suspend fun trackAIInteraction(
        messageType: String,
        sentiment: Float? = null,  // -1 to 1, detected from message
        wasHelpful: Boolean? = null
    )

    /**
     * Track feature usage (TTS, insights, etc.)
     */
    suspend fun trackFeatureUsage(feature: String, durationSeconds: Int? = null)

    /**
     * Track notification response
     */
    suspend fun trackNotificationResponse(responded: Boolean, delaySeconds: Int? = null)

    /**
     * Track app session
     */
    suspend fun trackSession(durationSeconds: Int, screensViewed: List<String>)

    // ============ Profile Analysis ============

    /**
     * Detect user's chronotype based on check-in patterns
     */
    suspend fun detectChronotype(): Chronotype

    /**
     * Detect preferred motivation style based on AI interaction history
     */
    suspend fun detectMotivationStyle(): MotivationStyle

    /**
     * Calculate streak recovery rate (0-1)
     */
    suspend fun calculateStreakRecoveryRate(): Float

    /**
     * Calculate habit correlations (which habits help each other)
     */
    suspend fun calculateHabitCorrelations(): List<HabitCorrelationData>

    /**
     * Calculate current attitude/engagement score (-1 to 1)
     */
    suspend fun calculateAttitudeScore(): Float

    /**
     * Get daily behavior summary for a date
     */
    suspend fun getDailySummary(date: String): DailyBehaviorSummary?

    /**
     * Get weekly pattern analysis
     */
    suspend fun getWeeklyAnalysis(weekStartDate: String): WeeklyPatternAnalysis?

    // ============ Profile Updates ============

    /**
     * Refresh the user profile with latest analysis
     * Call this periodically (e.g., once per day or on app launch)
     */
    suspend fun refreshProfile()

    /**
     * Update specific profile field
     */
    suspend fun updateProfile(update: (UserBehaviorProfile) -> UserBehaviorProfile)

    /**
     * Sync profile to Firebase
     */
    suspend fun syncToFirebase()

    /**
     * Clear all behavior data (for account deletion)
     */
    suspend fun clearAllData()
}
