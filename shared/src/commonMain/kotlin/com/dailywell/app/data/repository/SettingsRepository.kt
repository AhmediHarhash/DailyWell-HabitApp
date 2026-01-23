package com.dailywell.app.data.repository

import com.dailywell.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<UserSettings>
    suspend fun updateSettings(settings: UserSettings)
    suspend fun setOnboardingComplete()
    suspend fun enableHabit(habitId: String)
    suspend fun disableHabit(habitId: String)
    suspend fun setReminderTime(hour: Int, minute: Int)
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setPremium(isPremium: Boolean)

    /**
     * Start the 14-day free trial with FULL premium access.
     * Users get access to ALL features during trial - no restrictions.
     */
    suspend fun startFreeTrial()

    /**
     * Check if user has premium access (paid OR trial active)
     */
    suspend fun hasPremiumAccess(): Boolean

    /**
     * Get remaining trial days (0 if not on trial or trial expired)
     */
    suspend fun getTrialDaysRemaining(): Int
}
