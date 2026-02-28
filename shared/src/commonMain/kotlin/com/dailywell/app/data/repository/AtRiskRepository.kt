package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing at-risk predictions and habit health analysis
 */
interface AtRiskRepository {

    // ==================== Risk Assessment ====================

    /**
     * Get risk assessment for a specific habit
     */
    suspend fun getHabitRiskAssessment(habitId: String): HabitRiskAssessment

    /**
     * Get risk assessments for all active habits
     */
    fun getAllRiskAssessments(): Flow<List<HabitRiskAssessment>>

    /**
     * Get habits currently at high or critical risk
     */
    fun getHighRiskHabits(): Flow<List<HabitRiskAssessment>>

    /**
     * Calculate and refresh risk assessments for all habits
     */
    suspend fun refreshRiskAssessments(): Result<Unit>

    // ==================== Habit Health ====================

    /**
     * Get health metrics for a specific habit
     */
    fun getHabitHealth(habitId: String): Flow<HabitHealth?>

    /**
     * Get health metrics for all habits
     */
    fun getAllHabitHealth(): Flow<List<HabitHealth>>

    /**
     * Recalculate health metrics for all habits
     */
    suspend fun refreshHabitHealth(): Result<Unit>

    // ==================== Pattern Analysis ====================

    /**
     * Get historical patterns for a habit
     */
    suspend fun getHabitPattern(habitId: String): HabitPattern?

    /**
     * Analyze completion patterns by day of week
     */
    suspend fun getDayOfWeekStats(habitId: String): List<DayOfWeekStats>

    /**
     * Get streak break patterns to predict future breaks
     */
    suspend fun getStreakBreakPatterns(habitId: String): List<StreakBreakPattern>

    // ==================== Weather Integration ====================

    /**
     * Get current weather conditions
     */
    suspend fun getCurrentWeather(): WeatherCondition?

    /**
     * Check if weather affects any habits
     */
    suspend fun getWeatherImpactedHabits(): List<Pair<String, AtRiskFactor>>

    // ==================== Daily Summary ====================

    /**
     * Get the daily risk summary
     */
    fun getDailyRiskSummary(): Flow<DailyRiskSummary?>

    /**
     * Generate fresh daily risk summary
     */
    suspend fun generateDailyRiskSummary(): DailyRiskSummary

    // ==================== Alerts ====================

    /**
     * Get active at-risk alerts
     */
    fun getActiveAlerts(): Flow<List<AtRiskAlert>>

    /**
     * Dismiss an alert
     */
    suspend fun dismissAlert(alertId: String)

    /**
     * Generate alerts based on current risk assessments
     */
    suspend fun generateAlerts(): List<AtRiskAlert>

    /**
     * Clear expired alerts
     */
    suspend fun clearExpiredAlerts()

    // ==================== Settings ====================

    /**
     * Get notification settings
     */
    fun getNotificationSettings(): Flow<AtRiskNotificationSettings>

    /**
     * Update notification settings
     */
    suspend fun updateNotificationSettings(settings: AtRiskNotificationSettings)

    // ==================== Preemptive Suggestions ====================

    /**
     * Get optimal time to complete a habit today
     */
    suspend fun getOptimalTimeForHabit(habitId: String): Long?

    /**
     * Get preemptive suggestions for all at-risk habits
     */
    suspend fun getPreemptiveSuggestions(): List<Pair<String, String>>
}
