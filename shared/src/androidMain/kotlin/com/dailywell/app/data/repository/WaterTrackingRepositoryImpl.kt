package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Android implementation of WaterTrackingRepository
 * Uses DataStore for persistence — data survives app restart
 */
class WaterTrackingRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : WaterTrackingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private companion object {
        const val WATER_ENTRIES_KEY = "water_entries"
        const val WATER_SETTINGS_KEY = "water_settings"
    }

    // In-memory cache backed by DataStore persistence
    private val entriesCache = MutableStateFlow<Map<String, List<WaterEntry>>>(emptyMap())
    private val settingsCache = MutableStateFlow(WaterSettings())

    init {
        // Load persisted data on startup
        scope.launch {
            try {
                val entriesJson = dataStoreManager.getString(WATER_ENTRIES_KEY).first()
                if (entriesJson != null) {
                    val loaded = json.decodeFromString<Map<String, List<WaterEntry>>>(entriesJson)
                    entriesCache.value = loaded
                }
            } catch (_: Exception) {}

            try {
                val settingsJson = dataStoreManager.getString(WATER_SETTINGS_KEY).first()
                if (settingsJson != null) {
                    val loaded = json.decodeFromString<WaterSettings>(settingsJson)
                    settingsCache.value = loaded
                }
            } catch (_: Exception) {}
        }
    }

    private fun persistEntries() {
        scope.launch {
            try {
                dataStoreManager.putString(WATER_ENTRIES_KEY, json.encodeToString(entriesCache.value))
            } catch (_: Exception) {}
        }
    }

    private fun persistSettings() {
        scope.launch {
            try {
                dataStoreManager.putString(WATER_SETTINGS_KEY, json.encodeToString(settingsCache.value))
            } catch (_: Exception) {}
        }
    }

    private fun todayDate(): String {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val localDate = now.toLocalDateTime(tz).date
        return localDate.toString()
    }

    override fun getTodaySummary(): Flow<DailyWaterSummary> {
        val today = todayDate()
        return entriesCache.map { cache ->
            val entries = cache[today] ?: emptyList()
            val totalMl = entries.sumOf { it.amountMl }
            val effectiveHydration = entries.sumOf {
                (it.amountMl * it.source.hydrationFactor).toInt()
            }

            DailyWaterSummary(
                date = today,
                entries = entries,
                goalMl = settingsCache.value.dailyGoalMl,
                totalMl = totalMl,
                effectiveHydrationMl = effectiveHydration
            )
        }
    }

    override suspend fun getSummaryForDate(date: String): DailyWaterSummary {
        val entries = entriesCache.value[date] ?: emptyList()
        val totalMl = entries.sumOf { it.amountMl }
        val effectiveHydration = entries.sumOf {
            (it.amountMl * it.source.hydrationFactor).toInt()
        }

        return DailyWaterSummary(
            date = date,
            entries = entries,
            goalMl = settingsCache.value.dailyGoalMl,
            totalMl = totalMl,
            effectiveHydrationMl = effectiveHydration
        )
    }

    override suspend fun logWater(amountMl: Int, source: WaterSource): Result<WaterEntry> {
        return try {
            val entry = WaterEntry(
                id = UUID.randomUUID().toString(),
                amountMl = amountMl,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                source = source
            )

            val today = todayDate()
            entriesCache.update { cache ->
                val currentEntries = cache[today] ?: emptyList()
                cache + (today to (currentEntries + entry))
            }
            persistEntries()

            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logGlass(size: GlassSize, source: WaterSource): Result<WaterEntry> {
        return logWater(size.amountMl, source)
    }

    override suspend fun removeEntry(entryId: String): Result<Unit> {
        return try {
            entriesCache.update { cache ->
                cache.mapValues { (_, entries) ->
                    entries.filter { it.id != entryId }
                }
            }
            persistEntries()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWeeklyStats(): WeeklyHydrationStats {
        val today = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val todayLocal = today.toLocalDateTime(tz).date

        val weekStart = todayLocal.minus(6, DateTimeUnit.DAY)
        val dailySummaries = (0..6).map { daysAgo ->
            val date = todayLocal.minus(daysAgo, DateTimeUnit.DAY).toString()
            getSummaryForDate(date)
        }.reversed()

        val goalReachedDays = dailySummaries.count { it.isGoalReached }
        val totalMl = dailySummaries.sumOf { it.totalMl }
        val averageDailyMl = if (dailySummaries.isNotEmpty()) totalMl / dailySummaries.size else 0
        val bestDay = dailySummaries.maxByOrNull { it.totalMl }

        return WeeklyHydrationStats(
            weekStart = weekStart.toString(),
            dailySummaries = dailySummaries,
            averageDailyMl = averageDailyMl,
            goalReachedDays = goalReachedDays,
            totalMl = totalMl,
            bestDay = bestDay,
            streakDays = getHydrationStreak()
        )
    }

    override fun getSettings(): Flow<WaterSettings> = settingsCache

    override suspend fun updateSettings(settings: WaterSettings) {
        settingsCache.value = settings
        persistSettings()
    }

    override suspend fun updateDailyGoal(goalMl: Int) {
        settingsCache.update { it.copy(dailyGoalMl = goalMl) }
        persistSettings()
    }

    override suspend fun generateInsights(): List<HydrationInsight> {
        val insights = mutableListOf<HydrationInsight>()
        val weeklyStats = getWeeklyStats()
        val todaySummary = getSummaryForDate(todayDate())

        // Streak insight
        if (weeklyStats.streakDays >= 3) {
            insights.add(
                HydrationInsight(
                    type = HydrationInsightType.STREAK,
                    title = "${weeklyStats.streakDays} Day Streak!",
                    message = "You've met your hydration goal for ${weeklyStats.streakDays} days in a row. Keep it up!",
                    icon = "🔥"
                )
            )
        }

        // Improvement insight
        if (weeklyStats.goalReachedDays >= 5) {
            insights.add(
                HydrationInsight(
                    type = HydrationInsightType.CELEBRATION,
                    title = "Hydration Champion!",
                    message = "You reached your goal ${weeklyStats.goalReachedDays} out of 7 days this week!",
                    icon = "🏆"
                )
            )
        }

        // Pattern insight
        val morningEntries = todaySummary.entries.filter {
            val hour = Instant.fromEpochMilliseconds(it.timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault()).hour
            hour < 12
        }
        if (morningEntries.isEmpty() && todaySummary.entries.isNotEmpty()) {
            insights.add(
                HydrationInsight(
                    type = HydrationInsightType.PATTERN,
                    title = "Morning Hydration",
                    message = "Try drinking water in the morning to start your day right!",
                    icon = "🌅"
                )
            )
        }

        // Tip based on current progress
        if (todaySummary.progressPercent < 50) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            if (now.hour >= 14) {
                insights.add(
                    HydrationInsight(
                        type = HydrationInsightType.TIP,
                        title = "Time to Catch Up",
                        message = "You're at ${todaySummary.progressPercent.toInt()}% of your goal. Try drinking a full glass now!",
                        icon = "💡"
                    )
                )
            }
        }

        return insights
    }

    override fun getRecentEntries(): Flow<List<WaterEntry>> {
        return entriesCache.map { cache ->
            cache.values.flatten()
                .sortedByDescending { it.timestamp }
                .take(10)
        }
    }

    override suspend fun isReminderDue(): Boolean {
        val settings = settingsCache.value
        if (!settings.reminderEnabled) return false

        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val localTime = now.toLocalDateTime(tz)

        // Check if within reminder hours
        if (localTime.hour < settings.reminderStartHour ||
            localTime.hour >= settings.reminderEndHour) {
            return false
        }

        // Check last entry time
        val todayEntries = entriesCache.value[todayDate()] ?: emptyList()
        val lastEntry = todayEntries.maxByOrNull { it.timestamp }

        if (lastEntry == null) return true

        val lastEntryTime = Instant.fromEpochMilliseconds(lastEntry.timestamp)
        val minutesSinceLastEntry = (now - lastEntryTime).inWholeMinutes

        return minutesSinceLastEntry >= settings.reminderIntervalMinutes
    }

    override suspend fun getHydrationStreak(): Int {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        var streak = 0
        var checkDate = today.minus(1, DateTimeUnit.DAY) // Start from yesterday

        // Check today first
        val todaySummary = getSummaryForDate(today.toString())
        if (todaySummary.isGoalReached) {
            streak = 1
        }

        // Check previous days
        while (true) {
            val summary = getSummaryForDate(checkDate.toString())
            if (summary.isGoalReached) {
                streak++
                checkDate = checkDate.minus(1, DateTimeUnit.DAY)
            } else {
                break
            }

            // Limit check to 365 days
            if (streak >= 365) break
        }

        return streak
    }
}
