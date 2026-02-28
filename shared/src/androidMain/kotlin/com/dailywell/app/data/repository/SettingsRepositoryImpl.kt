package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.local.PreferencesKeys
import com.dailywell.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : SettingsRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun getSettings(): Flow<UserSettings> {
        return dataStoreManager.getString(PreferencesKeys.USER_SETTINGS).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<UserSettings>(it)
                } catch (e: Exception) {
                    UserSettings()
                }
            } ?: UserSettings()
        }
    }

    override suspend fun getSettingsSnapshot(): UserSettings {
        return getSettings().first()
    }

    override suspend fun updateSettings(settings: UserSettings) {
        dataStoreManager.putString(
            PreferencesKeys.USER_SETTINGS,
            json.encodeToString(settings)
        )
    }

    override suspend fun setOnboardingComplete() {
        val current = getSettings().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        updateSettings(current.copy(
            hasCompletedOnboarding = true,
            startDate = current.startDate ?: today
        ))
    }

    override suspend fun enableHabit(habitId: String) {
        val current = getSettings().first()
        if (!current.enabledHabitIds.contains(habitId)) {
            updateSettings(current.copy(
                enabledHabitIds = current.enabledHabitIds + habitId
            ))
        }
    }

    override suspend fun disableHabit(habitId: String) {
        val current = getSettings().first()
        updateSettings(current.copy(
            enabledHabitIds = current.enabledHabitIds - habitId
        ))
    }

    override suspend fun setReminderTime(hour: Int, minute: Int) {
        val current = getSettings().first()
        updateSettings(current.copy(
            reminderHour = hour,
            reminderMinute = minute
        ))
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        val current = getSettings().first()
        updateSettings(current.copy(reminderEnabled = enabled))
    }

    override suspend fun setPremium(isPremium: Boolean) {
        val current = getSettings().first()
        updateSettings(current.copy(isPremium = isPremium))
    }

    /**
     * Start the 14-day free trial with FULL premium access.
     * All features unlocked - no restrictions during trial.
     */
    override suspend fun startFreeTrial() {
        val current = getSettings().first()
        if (current.hasUsedTrial || current.isPremium) return

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val updatedSettings = current.withTrialStarted(today)
        updateSettings(updatedSettings)
    }

    override suspend fun hasPremiumAccess(): Boolean {
        val current = getSettings().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        return current.hasPremiumAccess(today)
    }

    override suspend fun getTrialDaysRemaining(): Int {
        val current = getSettings().first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        return current.trialDaysRemaining(today)
    }

    // Quick water tracking — lightweight daily glass count for the Today screen.
    // This is a convenience accessor using ad-hoc DataStore keys, NOT a replacement
    // for WaterTrackingRepository which handles full history, goals, and analytics.
    override suspend fun getWaterCount(date: String): Int {
        return dataStoreManager.getString("water_count_$date").first()?.toIntOrNull() ?: 0
    }

    override suspend fun setWaterCount(date: String, count: Int) {
        dataStoreManager.putString("water_count_$date", count.toString())
    }

    // First-day tutorial overlay
    override suspend fun getHasSeenTutorial(): Boolean {
        return dataStoreManager.getString("has_seen_tutorial").first()?.toBooleanStrictOrNull() ?: false
    }

    override suspend fun setHasSeenTutorial(seen: Boolean) {
        dataStoreManager.putString("has_seen_tutorial", seen.toString())
    }
}
