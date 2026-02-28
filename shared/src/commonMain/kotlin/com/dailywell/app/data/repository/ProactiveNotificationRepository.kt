package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing proactive AI notifications
 */
interface ProactiveNotificationRepository {

    // ============================================================
    // PREFERENCES
    // ============================================================

    /**
     * Get user's notification preferences
     */
    fun getPreferences(): Flow<ProactiveNotificationPreferences>

    /**
     * Update notification preferences
     */
    suspend fun updatePreferences(preferences: ProactiveNotificationPreferences)

    /**
     * Toggle a specific notification type
     */
    suspend fun toggleNotificationType(type: ProactiveNotificationType, enabled: Boolean)

    /**
     * Set quiet hours / DND
     */
    suspend fun setQuietHours(start: Int, end: Int)

    // ============================================================
    // NOTIFICATION GENERATION
    // ============================================================

    /**
     * Generate AI-powered notification content
     */
    suspend fun generateNotificationContent(
        type: ProactiveNotificationType,
        userContext: NotificationUserContext
    ): ProactiveNotification

    /**
     * Check if a notification should be sent now.
     * Uses the behavioral value score gate (65+ required).
     */
    suspend fun shouldSendNotification(type: ProactiveNotificationType): Boolean

    /**
     * Calculate the value score for a potential notification.
     * Returns a breakdown of risk/readiness/novelty/impact/trust.
     */
    suspend fun calculateValueScore(type: ProactiveNotificationType): NotificationValueScore

    /**
     * Get the weekly notification state (tracks 4/week cap).
     */
    suspend fun getWeeklyNotificationState(): WeeklyNotificationState

    /**
     * Get the next scheduled notification
     */
    suspend fun getNextScheduledNotification(): ProactiveNotification?

    // ============================================================
    // NOTIFICATION HISTORY
    // ============================================================

    /**
     * Record that a notification was sent
     */
    suspend fun recordNotificationSent(notification: ProactiveNotification)

    /**
     * Record that a notification was opened
     */
    suspend fun recordNotificationOpened(notificationId: String)

    /**
     * Record that a notification was dismissed
     */
    suspend fun recordNotificationDismissed(notificationId: String)

    /**
     * Get notification history for analytics
     */
    fun getNotificationHistory(days: Int = 30): Flow<List<NotificationHistory>>

    /**
     * Get today's notification state
     */
    suspend fun getTodayNotificationState(): DailyNotificationState

    // ============================================================
    // SMART TIMING
    // ============================================================

    /**
     * Get smart timing data
     */
    fun getSmartTiming(): Flow<SmartNotificationTiming?>

    /**
     * Update smart timing based on user behavior
     */
    suspend fun updateSmartTiming(timing: SmartNotificationTiming)

    /**
     * Learn optimal timing from user engagement patterns
     */
    suspend fun analyzeAndUpdateOptimalTiming()

    // ============================================================
    // TRIGGER CHECKS
    // ============================================================

    /**
     * Check if streak is at risk (needs notification)
     */
    suspend fun checkStreakAtRisk(): StreakRiskStatus?

    /**
     * Check if user needs comeback notification
     */
    suspend fun checkComebackNeeded(): ComebackStatus?

    /**
     * Check if milestone is approaching
     */
    suspend fun checkMilestoneApproaching(): MilestoneStatus?

    /**
     * Check all triggers and return which notifications should be sent
     */
    suspend fun checkAllTriggers(): List<NotificationTrigger>
}

/**
 * Context passed to AI for personalized notifications
 */
data class NotificationUserContext(
    val userName: String?,
    val currentStreak: Int,
    val longestStreak: Int,
    val todayCompleted: Int,
    val totalHabits: Int,
    val habitsRemaining: Int,
    val weakestHabit: String?,
    val strongestHabit: String?,
    val missedDays: Int,
    val lastCheckInAt: Long?,
    val coachPersona: String,
    val preferredTone: NotificationTone,
    val currentHour: Int,
    val dayOfWeek: Int,
    val isWeekend: Boolean
)

/**
 * Streak risk status
 */
data class StreakRiskStatus(
    val currentStreak: Int,
    val habitsRemaining: Int,
    val hoursUntilMidnight: Int,
    val riskLevel: RiskLevel
)

enum class RiskLevel {
    LOW,      // > 6 hours left
    MEDIUM,   // 3-6 hours left
    HIGH,     // 1-3 hours left
    CRITICAL  // < 1 hour left
}

/**
 * Comeback status for inactive users
 */
data class ComebackStatus(
    val daysSinceLastCheckIn: Int,
    val previousStreak: Int,
    val previousBestStreak: Int
)

/**
 * Milestone approaching status
 */
data class MilestoneStatus(
    val currentStreak: Int,
    val nextMilestone: Int,
    val daysRemaining: Int
)

/**
 * Trigger for a specific notification
 */
data class NotificationTrigger(
    val type: ProactiveNotificationType,
    val priority: NotificationPriority,
    val reason: String,
    val metadata: NotificationMetadata
)
