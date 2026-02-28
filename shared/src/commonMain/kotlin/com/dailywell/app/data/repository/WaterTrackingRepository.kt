package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Water Tracking feature
 */
interface WaterTrackingRepository {
    /**
     * Get today's water summary as a flow
     */
    fun getTodaySummary(): Flow<DailyWaterSummary>

    /**
     * Get summary for a specific date
     */
    suspend fun getSummaryForDate(date: String): DailyWaterSummary

    /**
     * Log a water entry
     */
    suspend fun logWater(amountMl: Int, source: WaterSource = WaterSource.WATER): Result<WaterEntry>

    /**
     * Log water using preset glass size
     */
    suspend fun logGlass(size: GlassSize, source: WaterSource = WaterSource.WATER): Result<WaterEntry>

    /**
     * Remove a water entry
     */
    suspend fun removeEntry(entryId: String): Result<Unit>

    /**
     * Get weekly hydration statistics
     */
    suspend fun getWeeklyStats(): WeeklyHydrationStats

    /**
     * Get water tracking settings
     */
    fun getSettings(): Flow<WaterSettings>

    /**
     * Update water tracking settings
     */
    suspend fun updateSettings(settings: WaterSettings)

    /**
     * Update daily goal
     */
    suspend fun updateDailyGoal(goalMl: Int)

    /**
     * Generate personalized hydration insights
     */
    suspend fun generateInsights(): List<HydrationInsight>

    /**
     * Get recent water entries (last 10)
     */
    fun getRecentEntries(): Flow<List<WaterEntry>>

    /**
     * Check if reminder is due
     */
    suspend fun isReminderDue(): Boolean

    /**
     * Get hydration streak (consecutive days meeting goal)
     */
    suspend fun getHydrationStreak(): Int
}
