package com.dailywell.app.ai

import android.util.Log
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.repository.EntryRepository
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Builds a condensed user profile for SLM system prompt injection.
 *
 * Aggregates data from:
 * - Habit completion patterns (EntryRepository)
 * - User settings (SettingsRepository) — goal, assessment, coach persona
 * - Per-habit completion rates over 14 days
 *
 * Output: ~200 token text block cached in DataStore, rebuilt daily.
 */
class UserProfileBuilder(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository,
    private val dataStoreManager: DataStoreManager
) {
    companion object {
        private const val TAG = "UserProfileBuilder"
        private const val PROFILE_KEY = "slm_user_profile"
        private const val PROFILE_DATE_KEY = "slm_user_profile_date"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Get the cached user profile, rebuilding if stale (>24h old or missing).
     */
    suspend fun getProfile(): String {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val cachedDate = dataStoreManager.getString(PROFILE_DATE_KEY).first()
        val cachedProfile = dataStoreManager.getString(PROFILE_KEY).first()

        if (cachedDate == today && !cachedProfile.isNullOrBlank()) {
            return cachedProfile
        }

        // Rebuild profile
        val profile = buildProfile()
        // Persist asynchronously
        scope.launch {
            try {
                dataStoreManager.putString(PROFILE_KEY, profile)
                dataStoreManager.putString(PROFILE_DATE_KEY, today)
            } catch (_: Exception) {}
        }
        return profile
    }

    /**
     * Build a condensed user profile text (~200 tokens).
     */
    private suspend fun buildProfile(): String {
        return try {
            val settings = settingsRepository.getSettingsSnapshot()
            val habits = habitRepository.getAllHabits().first()
            val streakInfo = entryRepository.getStreakInfo().first()

            // Per-habit completion rates (14-day window)
            val habitStats = mutableMapOf<String, Float>()
            for (habit in habits) {
                try {
                    val rate = entryRepository.getCompletionRateForHabit(habit.id, 14)
                    habitStats[habit.name] = rate
                } catch (_: Exception) {
                    habitStats[habit.name] = 0f
                }
            }

            // Sort by rate for top/struggling
            val sorted = habitStats.entries.sortedByDescending { it.value }
            val strongHabits = sorted.take(3).filter { it.value >= 0.5f }
            val strugglingHabits = sorted.takeLast(3).filter { it.value < 0.5f }

            // Determine active time from settings
            val reminderHour = settings.reminderHour
            val timePreference = when {
                reminderHour < 10 -> "early morning"
                reminderHour < 13 -> "morning"
                reminderHour < 17 -> "afternoon"
                reminderHour < 21 -> "evening"
                else -> "late evening"
            }

            buildString {
                appendLine("About this user:")

                // Goal
                settings.onboardingGoal?.let {
                    appendLine("- Goal: $it")
                }

                // Assessment
                settings.assessmentScore?.let { score ->
                    val level = when (score) {
                        1 -> "beginner (just starting)"
                        2 -> "developing (some habits)"
                        3 -> "intermediate (building consistency)"
                        4 -> "strong (mostly consistent)"
                        5 -> "advanced (well-established routines)"
                        else -> "unknown"
                    }
                    appendLine("- Self-assessment: $level")
                }

                // Strong habits
                if (strongHabits.isNotEmpty()) {
                    val list = strongHabits.joinToString(", ") {
                        "${it.key} (${(it.value * 100).toInt()}%)"
                    }
                    appendLine("- Strong habits: $list")
                }

                // Struggling habits
                if (strugglingHabits.isNotEmpty()) {
                    val list = strugglingHabits.joinToString(", ") {
                        "${it.key} (${(it.value * 100).toInt()}%)"
                    }
                    appendLine("- Needs support: $list")
                }

                // Streak
                appendLine("- Current streak: ${streakInfo.currentStreak} days (best: ${streakInfo.longestStreak})")

                // Active habits count
                appendLine("- Tracking ${habits.size} habits")

                // Time preference
                appendLine("- Most active: $timePreference")

                // Coach persona
                settings.selectedCoachPersona?.let {
                    appendLine("- Preferred coach style: $it")
                }

                // Start date for journey duration
                settings.startDate?.let { start ->
                    try {
                        val startDate = kotlinx.datetime.LocalDate.parse(start)
                        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                        val daysSinceStart = today.toEpochDays() - startDate.toEpochDays()
                        if (daysSinceStart > 0) {
                            appendLine("- On journey for $daysSinceStart days")
                        }
                    } catch (_: Exception) {}
                }
            }.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build user profile", e)
            "About this user: Profile unavailable."
        }
    }
}
