package com.dailywell.app.data.repository

import com.dailywell.app.data.model.DailyEntry
import com.dailywell.app.data.model.MoodLevel
import com.dailywell.app.data.model.StreakInfo
import com.dailywell.app.data.model.WeekData
import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    fun getTodayEntry(): Flow<DailyEntry?>
    fun getEntryForDate(date: String): Flow<DailyEntry?>
    fun getEntriesForDateRange(startDate: String, endDate: String): Flow<List<DailyEntry>>
    fun getStreakInfo(): Flow<StreakInfo>
    fun getWeekData(weekOffset: Int = 0): Flow<WeekData>

    suspend fun setHabitCompletion(date: String, habitId: String, completed: Boolean)
    suspend fun setMood(date: String, mood: MoodLevel)
    suspend fun createEntryIfNeeded(date: String, habitIds: List<String>)
    suspend fun updateStreak()
    suspend fun getCompletionRateForHabit(habitId: String, days: Int): Float

    // For proactive notifications
    fun getEntriesForDate(date: String): Flow<List<HabitEntryStatus>>
    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<HabitEntryStatus>>
    suspend fun getLastEntryDate(): String?
}

/**
 * Simple data class for habit entry status used by notification system
 */
data class HabitEntryStatus(
    val habitId: String,
    val date: String,
    val completed: Boolean
)
