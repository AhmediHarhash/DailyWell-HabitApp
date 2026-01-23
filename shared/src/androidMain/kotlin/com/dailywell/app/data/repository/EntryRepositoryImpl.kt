package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.local.PreferencesKeys
import com.dailywell.app.data.local.db.EntryDao
import com.dailywell.app.data.local.db.EntryEntity
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EntryRepositoryImpl(
    private val entryDao: EntryDao,
    private val dataStoreManager: DataStoreManager,
    private val settingsRepository: SettingsRepository
) : EntryRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun today(): String =
        Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

    override fun getTodayEntry(): Flow<DailyEntry?> {
        return getEntryForDate(today())
    }

    override fun getEntryForDate(date: String): Flow<DailyEntry?> {
        return entryDao.getEntriesForDate(date).map { entities ->
            if (entities.isEmpty()) return@map null
            entitiesToDailyEntry(date, entities)
        }
    }

    override fun getEntriesForDateRange(startDate: String, endDate: String): Flow<List<DailyEntry>> {
        return entryDao.getEntriesInRange(startDate, endDate).map { entities ->
            val grouped = entities.groupBy { it.date }
            grouped.map { (date, dayEntities) ->
                entitiesToDailyEntry(date, dayEntities)
            }.sortedBy { it.date }
        }
    }

    private fun entitiesToDailyEntry(date: String, entities: List<EntryEntity>): DailyEntry {
        val completions = entities.associate { it.habitId to it.completed }
        return DailyEntry(
            date = date,
            completions = completions,
            createdAt = entities.minOfOrNull { it.createdAt } ?: System.currentTimeMillis(),
            updatedAt = entities.maxOfOrNull { it.updatedAt } ?: System.currentTimeMillis()
        )
    }

    override fun getStreakInfo(): Flow<StreakInfo> {
        return dataStoreManager.getString(PreferencesKeys.STREAK_INFO).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<StreakInfo>(it)
                } catch (e: Exception) {
                    StreakInfo()
                }
            } ?: StreakInfo()
        }
    }

    override fun getWeekData(weekOffset: Int): Flow<WeekData> {
        val todayDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dayOfWeek = todayDate.dayOfWeek.ordinal // Monday = 0
        val mondayOffset = dayOfWeek
        val weekStart = todayDate.minus(mondayOffset + (weekOffset * 7), DateTimeUnit.DAY)
        val weekEnd = weekStart.plus(6, DateTimeUnit.DAY)

        return combine(
            getEntriesForDateRange(weekStart.toString(), weekEnd.toString()),
            settingsRepository.getSettings()
        ) { entries, settings ->
            val days = (0..6).map { dayIndex ->
                val date = weekStart.plus(dayIndex, DateTimeUnit.DAY)
                val dateStr = date.toString()
                val entry = entries.find { it.date == dateStr }
                val isToday = dateStr == today()
                val isFuture = date > todayDate

                val status = when {
                    isFuture -> CompletionStatus.FUTURE
                    entry == null -> CompletionStatus.NO_DATA
                    entry.completedCount() == entry.totalCount() && entry.totalCount() > 0 -> CompletionStatus.COMPLETE
                    entry.completedCount() > 0 -> CompletionStatus.PARTIAL
                    else -> CompletionStatus.NONE
                }

                DayStatus(
                    date = dateStr,
                    dayOfWeek = dayIndex,
                    dayLabel = listOf("M", "T", "W", "T", "F", "S", "S")[dayIndex],
                    status = status,
                    completedCount = entry?.completedCount() ?: 0,
                    totalCount = entry?.totalCount() ?: settings.enabledHabitIds.size,
                    isToday = isToday,
                    isFuture = isFuture
                )
            }

            val totalCompleted = days.sumOf { it.completedCount }
            val totalPossible = days.filter { !it.isFuture }.sumOf { it.totalCount }
            val completionRate = if (totalPossible > 0) {
                totalCompleted.toFloat() / totalPossible
            } else 0f

            WeekData(
                days = days,
                weekStartDate = weekStart.toString(),
                weekEndDate = weekEnd.toString(),
                completionRate = completionRate,
                totalCompleted = totalCompleted,
                totalPossible = totalPossible
            )
        }
    }

    override suspend fun setHabitCompletion(date: String, habitId: String, completed: Boolean) {
        val existingEntry = entryDao.getEntry(date, habitId)

        if (existingEntry != null) {
            entryDao.updateEntry(
                existingEntry.copy(
                    completed = completed,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            entryDao.insertEntry(
                EntryEntity(
                    date = date,
                    habitId = habitId,
                    completed = completed
                )
            )
        }

        updateStreak()
    }

    override suspend fun createEntryIfNeeded(date: String, habitIds: List<String>) {
        val existingEntries = entryDao.getEntriesForDateSync(date)
        val existingHabitIds = existingEntries.map { it.habitId }.toSet()

        val newEntries = habitIds
            .filter { it !in existingHabitIds }
            .map { habitId ->
                EntryEntity(
                    date = date,
                    habitId = habitId,
                    completed = false
                )
            }

        if (newEntries.isNotEmpty()) {
            entryDao.insertEntries(newEntries)
        }
    }

    override suspend fun updateStreak() {
        val todayDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startDate = todayDate.minus(365, DateTimeUnit.DAY) // Check last year
        val entries = entryDao.getEntriesInRangeSync(startDate.toString(), todayDate.toString())

        val entriesByDate = entries.groupBy { it.date }

        var currentStreak = 0
        var checkDate = todayDate
        var lastCompletedDate: String? = null
        var streakStartDate: String? = null

        // Check today and go backwards
        while (true) {
            val dateStr = checkDate.toString()
            val dayEntries = entriesByDate[dateStr] ?: emptyList()

            // A day counts as completed if user completed at least 80% of habits
            val total = dayEntries.size
            val completed = dayEntries.count { it.completed }
            val dayCompleted = total > 0 && (completed.toFloat() / total >= 0.8f)

            if (dayCompleted) {
                if (currentStreak == 0) {
                    lastCompletedDate = dateStr
                }
                streakStartDate = dateStr
                currentStreak++
                checkDate = checkDate.minus(1, DateTimeUnit.DAY)
            } else if (checkDate == todayDate) {
                // Today not completed yet, check yesterday
                checkDate = checkDate.minus(1, DateTimeUnit.DAY)
            } else {
                // Streak broken
                break
            }

            // Safety limit
            if (currentStreak > 1000) break
        }

        val currentStreakInfo = getStreakInfo().first()
        val longestStreak = maxOf(currentStreakInfo.longestStreak, currentStreak)

        val newStreakInfo = StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastCompletedDate = lastCompletedDate,
            streakStartDate = streakStartDate
        )

        dataStoreManager.putString(
            PreferencesKeys.STREAK_INFO,
            json.encodeToString(newStreakInfo)
        )
    }

    override suspend fun getCompletionRateForHabit(habitId: String, days: Int): Float {
        val todayDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startDate = todayDate.minus(days - 1, DateTimeUnit.DAY)

        val completed = entryDao.getCompletedCountForHabit(
            habitId,
            startDate.toString(),
            todayDate.toString()
        )

        return completed.toFloat() / days
    }

    /**
     * Mood storage key prefix
     */
    private fun moodKey(date: String) = "daily_mood_$date"

    override suspend fun setMood(date: String, mood: MoodLevel) {
        dataStoreManager.putString(moodKey(date), mood.name)
    }

    /**
     * Get mood for a specific date
     */
    suspend fun getMood(date: String): MoodLevel? {
        return dataStoreManager.getString(moodKey(date)).first()?.let { moodName ->
            try {
                MoodLevel.valueOf(moodName)
            } catch (e: Exception) {
                null
            }
        }
    }
}
