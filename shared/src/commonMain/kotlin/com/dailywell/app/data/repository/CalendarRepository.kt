package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Calendar Integration
 * Supports Google Calendar and Outlook Calendar
 */
interface CalendarRepository {

    // ==================== CONNECTION STATUS ====================

    /**
     * Get all connected calendar accounts
     */
    fun getConnectedAccounts(): Flow<List<CalendarAccount>>

    /**
     * Check if any calendar is connected
     */
    fun isCalendarConnected(): Flow<Boolean>

    /**
     * Get calendar sync state
     */
    fun getSyncState(): Flow<CalendarSyncState>

    // ==================== OAUTH AUTHENTICATION ====================

    /**
     * Start Google Calendar OAuth flow
     * Returns the OAuth authorization URL to open in browser
     */
    suspend fun startGoogleOAuth(): String

    /**
     * Complete Google OAuth with authorization code
     */
    suspend fun completeGoogleOAuth(authCode: String): Result<CalendarAccount>

    /**
     * Start Outlook Calendar OAuth flow
     */
    suspend fun startOutlookOAuth(): String

    /**
     * Complete Outlook OAuth with authorization code
     */
    suspend fun completeOutlookOAuth(authCode: String): Result<CalendarAccount>

    /**
     * Disconnect a calendar account
     */
    suspend fun disconnectAccount(accountId: String)

    /**
     * Refresh OAuth token if expired
     */
    suspend fun refreshToken(accountId: String): Boolean

    // ==================== CALENDAR EVENTS ====================

    /**
     * Sync calendar events from all connected accounts
     */
    suspend fun syncEvents(): Result<Int>

    /**
     * Get events for a specific date
     */
    fun getEventsForDate(date: String): Flow<List<CalendarEvent>>

    /**
     * Get events for a date range
     */
    suspend fun getEventsForRange(startDate: String, endDate: String): List<CalendarEvent>

    /**
     * Get today's calendar schedule
     */
    fun getTodaySchedule(): Flow<CalendarDaySchedule>

    /**
     * Get schedule for a specific date
     */
    fun getScheduleForDate(date: String): Flow<CalendarDaySchedule>

    // ==================== FREE TIME SLOTS ====================

    /**
     * Calculate free time slots for a date
     */
    suspend fun calculateFreeSlots(date: String, minDurationMinutes: Int = 15): List<FreeTimeSlot>

    /**
     * Get free slots for today
     */
    fun getTodayFreeSlots(): Flow<List<FreeTimeSlot>>

    // ==================== HABIT TIME SUGGESTIONS ====================

    /**
     * Get best time suggestions for all habits today
     */
    fun getHabitTimeSuggestions(): Flow<List<HabitTimeSuggestion>>

    /**
     * Get best time for a specific habit on a date
     */
    suspend fun getBestTimeForHabit(habitId: String, date: String): BestTimeAnalysis

    /**
     * Analyze calendar and suggest optimal times for a habit
     */
    suspend fun analyzeOptimalTimes(habitId: String, durationMinutes: Int): List<FreeTimeSlot>

    // ==================== AUTO-BLOCK HABITS ====================

    /**
     * Get habit calendar settings
     */
    fun getHabitCalendarSettings(habitId: String): Flow<HabitCalendarSettings?>

    /**
     * Update habit calendar settings
     */
    suspend fun updateHabitCalendarSettings(settings: HabitCalendarSettings)

    /**
     * Auto-block time for a habit on a specific date
     */
    suspend fun autoBlockHabit(habitId: String, date: String): AutoBlockResult

    /**
     * Remove a habit block from calendar
     */
    suspend fun removeHabitBlock(habitId: String, eventId: String): Boolean

    /**
     * Create a calendar event for a habit
     */
    suspend fun createHabitEvent(
        habitId: String,
        title: String,
        startTime: Long,
        durationMinutes: Int,
        accountId: String
    ): Result<CalendarEvent>

    // ==================== RESCHEDULE SUGGESTIONS ====================

    /**
     * Check for conflicts and suggest reschedules
     */
    suspend fun checkForConflicts(habitId: String): List<RescheduleSuggestion>

    /**
     * Get all reschedule suggestions for today
     */
    fun getRescheduleSuggestions(): Flow<List<RescheduleSuggestion>>

    // ==================== NOTIFICATIONS ====================

    /**
     * Get pending calendar notifications
     */
    fun getCalendarNotifications(): Flow<List<CalendarNotification>>

    /**
     * Schedule a free slot opportunity notification
     */
    suspend fun scheduleFreeSlotNotification(
        habitId: String,
        freeSlot: FreeTimeSlot,
        message: String
    )

    /**
     * Dismiss a notification
     */
    suspend fun dismissNotification(notificationId: String)

    // ==================== CALENDAR VIEW DATA ====================

    /**
     * Get weekly calendar view data with habits and events
     */
    fun getWeeklyCalendarView(startDate: String): Flow<List<CalendarDaySchedule>>

    /**
     * Get monthly calendar view summary
     */
    suspend fun getMonthlyCalendarSummary(yearMonth: String): List<CalendarDaySchedule>
}
