package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Implementation of SmartReminderRepository using DataStore
 */
class SmartReminderRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : SmartReminderRepository {

    override fun getSmartReminderData(): Flow<SmartReminderData?> {
        return dataStoreManager.smartReminders
    }

    override fun getReminderSettingsForHabit(habitId: String): Flow<HabitReminderSettings?> {
        return dataStoreManager.smartReminders.map { data ->
            data?.habitReminders?.get(habitId)
        }
    }

    override suspend fun updateHabitReminderSettings(habitId: String, settings: HabitReminderSettings) {
        val currentData = dataStoreManager.smartReminders.first() ?: SmartReminderData()
        val updatedReminders = currentData.habitReminders.toMutableMap()
        updatedReminders[habitId] = settings

        val updatedData = currentData.copy(
            habitReminders = updatedReminders,
            lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        dataStoreManager.updateSmartReminders(updatedData)
    }

    override suspend fun toggleGlobalReminders(enabled: Boolean) {
        val currentData = dataStoreManager.smartReminders.first() ?: SmartReminderData()
        val updatedData = currentData.copy(
            isEnabled = enabled,
            lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        dataStoreManager.updateSmartReminders(updatedData)
    }

    override suspend fun toggleHabitReminder(habitId: String, enabled: Boolean) {
        val currentData = dataStoreManager.smartReminders.first() ?: SmartReminderData()
        val currentSettings = currentData.habitReminders[habitId] ?: HabitReminderSettings(habitId = habitId)
        val updatedSettings = currentSettings.copy(isEnabled = enabled)

        val updatedReminders = currentData.habitReminders.toMutableMap()
        updatedReminders[habitId] = updatedSettings

        val updatedData = currentData.copy(
            habitReminders = updatedReminders,
            lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        dataStoreManager.updateSmartReminders(updatedData)
    }

    override suspend fun setPreferredTime(habitId: String, time: String?) {
        val currentData = dataStoreManager.smartReminders.first() ?: SmartReminderData()
        val currentSettings = currentData.habitReminders[habitId] ?: HabitReminderSettings(habitId = habitId)
        val updatedSettings = currentSettings.copy(preferredTime = time)

        val updatedReminders = currentData.habitReminders.toMutableMap()
        updatedReminders[habitId] = updatedSettings

        val updatedData = currentData.copy(
            habitReminders = updatedReminders,
            lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        dataStoreManager.updateSmartReminders(updatedData)
    }

    override suspend fun setReminderTone(habitId: String, tone: ReminderTone) {
        val currentData = dataStoreManager.smartReminders.first() ?: SmartReminderData()
        val currentSettings = currentData.habitReminders[habitId] ?: HabitReminderSettings(habitId = habitId)
        val updatedSettings = currentSettings.copy(tone = tone)

        val updatedReminders = currentData.habitReminders.toMutableMap()
        updatedReminders[habitId] = updatedSettings

        val updatedData = currentData.copy(
            habitReminders = updatedReminders,
            lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        dataStoreManager.updateSmartReminders(updatedData)
    }

    override suspend fun setReminderFrequency(habitId: String, frequency: ReminderFrequency) {
        val currentData = dataStoreManager.smartReminders.first() ?: SmartReminderData()
        val currentSettings = currentData.habitReminders[habitId] ?: HabitReminderSettings(habitId = habitId)
        val updatedSettings = currentSettings.copy(frequency = frequency)

        val updatedReminders = currentData.habitReminders.toMutableMap()
        updatedReminders[habitId] = updatedSettings

        val updatedData = currentData.copy(
            habitReminders = updatedReminders,
            lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        dataStoreManager.updateSmartReminders(updatedData)
    }

    override suspend fun recordReminderResponse(habitId: String, responseTimeMinutes: Int) {
        val currentData = dataStoreManager.smartReminders.first() ?: SmartReminderData()
        val currentSettings = currentData.habitReminders[habitId] ?: HabitReminderSettings(habitId = habitId)

        // Calculate new average response time
        val currentAvg = currentSettings.averageResponseTimeMinutes ?: responseTimeMinutes
        val newAvg = (currentAvg + responseTimeMinutes) / 2

        // Update response rate (simplified - just track that they responded)
        val newResponseRate = minOf(1.0f, currentSettings.responseRate + 0.1f)

        val updatedSettings = currentSettings.copy(
            responseRate = newResponseRate,
            averageResponseTimeMinutes = newAvg,
            lastReminderAt = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )

        val updatedReminders = currentData.habitReminders.toMutableMap()
        updatedReminders[habitId] = updatedSettings

        val updatedData = currentData.copy(
            habitReminders = updatedReminders,
            lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        dataStoreManager.updateSmartReminders(updatedData)
    }

    override suspend fun recordHabitCompletion(habitId: String, hour: Int, minute: Int) {
        val currentData = dataStoreManager.smartReminders.first() ?: SmartReminderData()
        val currentPatterns = currentData.learnedPatterns
        val habitPatterns = currentPatterns.habitSpecificPatterns.toMutableMap()

        val existingPattern = habitPatterns[habitId]
        val completionTime = String.format("%02d:%02d", hour, minute)

        if (existingPattern != null) {
            // Update pattern with new completion
            val newHistory = (existingPattern.completionHistory + completionTime).takeLast(30)
            val avgHour = calculateAverageHour(newHistory)

            habitPatterns[habitId] = existingPattern.copy(
                optimalHour = avgHour,
                completionHistory = newHistory,
                successRateAtOptimal = calculateSuccessRate(newHistory, avgHour)
            )
        } else {
            // Create new pattern
            habitPatterns[habitId] = HabitActivityPattern(
                habitId = habitId,
                optimalHour = hour,
                optimalMinute = minute,
                completionHistory = listOf(completionTime)
            )
        }

        val updatedPatterns = currentPatterns.copy(habitSpecificPatterns = habitPatterns)
        val updatedData = currentData.copy(
            learnedPatterns = updatedPatterns,
            lastUpdated = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        )
        dataStoreManager.updateSmartReminders(updatedData)
    }

    override suspend fun clearAllReminderData() {
        dataStoreManager.updateSmartReminders(SmartReminderData())
    }

    private fun calculateAverageHour(history: List<String>): Int {
        if (history.isEmpty()) return 8
        val hours = history.mapNotNull { it.split(":").firstOrNull()?.toIntOrNull() }
        return if (hours.isNotEmpty()) hours.sum() / hours.size else 8
    }

    private fun calculateSuccessRate(history: List<String>, targetHour: Int): Float {
        if (history.isEmpty()) return 0f
        val matchingHours = history.count { time ->
            val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: -1
            kotlin.math.abs(hour - targetHour) <= 1
        }
        return matchingHours.toFloat() / history.size
    }
}
