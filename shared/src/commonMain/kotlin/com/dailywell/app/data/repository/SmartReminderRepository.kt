package com.dailywell.app.data.repository

import com.dailywell.app.data.model.HabitReminderSettings
import com.dailywell.app.data.model.SmartReminderData
import com.dailywell.app.data.model.ReminderTone
import com.dailywell.app.data.model.ReminderFrequency
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Smart Adaptive Reminders
 */
interface SmartReminderRepository {

    /**
     * Get all smart reminder data
     */
    fun getSmartReminderData(): Flow<SmartReminderData?>

    /**
     * Get reminder settings for a specific habit
     */
    fun getReminderSettingsForHabit(habitId: String): Flow<HabitReminderSettings?>

    /**
     * Update reminder settings for a habit
     */
    suspend fun updateHabitReminderSettings(habitId: String, settings: HabitReminderSettings)

    /**
     * Toggle reminders on/off globally
     */
    suspend fun toggleGlobalReminders(enabled: Boolean)

    /**
     * Toggle reminders for a specific habit
     */
    suspend fun toggleHabitReminder(habitId: String, enabled: Boolean)

    /**
     * Set preferred time for a habit's reminder
     */
    suspend fun setPreferredTime(habitId: String, time: String?)

    /**
     * Set reminder tone for a habit
     */
    suspend fun setReminderTone(habitId: String, tone: ReminderTone)

    /**
     * Set reminder frequency for a habit
     */
    suspend fun setReminderFrequency(habitId: String, frequency: ReminderFrequency)

    /**
     * Record that a reminder was sent and user responded
     */
    suspend fun recordReminderResponse(habitId: String, responseTimeMinutes: Int)

    /**
     * Record a habit completion to learn patterns
     */
    suspend fun recordHabitCompletion(habitId: String, hour: Int, minute: Int)

    /**
     * Clear all reminder data (for testing or reset)
     */
    suspend fun clearAllReminderData()
}
