package com.dailywell.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.dailywell.app.data.model.AtRiskAlert
import com.dailywell.app.data.model.AtRiskNotificationSettings
import com.dailywell.app.data.model.CalendarEvent
import com.dailywell.app.data.model.DailyRiskSummary
import com.dailywell.app.data.model.DayOfWeekStats
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.HabitHealth
import com.dailywell.app.data.model.HabitPattern
import com.dailywell.app.data.model.HabitRiskAssessment
import com.dailywell.app.data.model.HealthTrend
import com.dailywell.app.data.model.AtRiskFactor
import com.dailywell.app.data.model.RiskFactorType
import com.dailywell.app.data.model.RiskLevel
import com.dailywell.app.data.model.StreakBreakPattern
import com.dailywell.app.data.model.WeatherCondition
import com.dailywell.app.data.model.WeatherType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.math.max
import kotlin.math.min

private val Context.atRiskDataStore: DataStore<Preferences> by preferencesDataStore(name = "at_risk_settings")

class AtRiskRepositoryImpl(
    private val context: Context,
    private val habitRepository: HabitRepository,
    private val calendarRepository: CalendarRepository,
    private val entryRepository: EntryRepository,
    private val locationService: com.dailywell.app.api.LocationService? = null
) : AtRiskRepository {

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val _riskAssessments = MutableStateFlow<List<HabitRiskAssessment>>(emptyList())
    private val _habitHealthMap = MutableStateFlow<Map<String, HabitHealth>>(emptyMap())
    private val _dailySummary = MutableStateFlow<DailyRiskSummary?>(null)
    private val _activeAlerts = MutableStateFlow<List<AtRiskAlert>>(emptyList())
    private val _currentWeather = MutableStateFlow<WeatherCondition?>(null)

    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Load cached risk data from DataStore
        persistScope.launch {
            try {
                val cached = context.atRiskDataStore.data.first()
                cached[HEALTH_CACHE_KEY]?.let { healthJson ->
                    try {
                        _habitHealthMap.value = json.decodeFromString<Map<String, HabitHealth>>(healthJson)
                    } catch (_: Exception) {}
                }
                cached[ALERTS_KEY]?.let { alertsJson ->
                    try {
                        _activeAlerts.value = json.decodeFromString<List<AtRiskAlert>>(alertsJson)
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    private fun persistHabitHealth() {
        persistScope.launch {
            try {
                context.atRiskDataStore.edit { prefs ->
                    prefs[HEALTH_CACHE_KEY] = json.encodeToString(_habitHealthMap.value)
                }
            } catch (_: Exception) {}
        }
    }

    private fun persistAlerts() {
        persistScope.launch {
            try {
                context.atRiskDataStore.edit { prefs ->
                    prefs[ALERTS_KEY] = json.encodeToString(_activeAlerts.value)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Fetches real completion data from EntryRepository and converts to timestamps
     */
    private suspend fun getCompletionDatesForHabit(habitId: String): List<Long> {
        val endDate = LocalDate.now().format(dateFormatter)
        val startDate = LocalDate.now().minusDays(90).format(dateFormatter)

        return try {
            val entries = entryRepository.getEntriesInRange(startDate, endDate).first()
            entries
                .filter { it.habitId == habitId && it.completed }
                .mapNotNull { entry ->
                    try {
                        // Convert date string to epoch millis (assume completion at noon for time-based analysis)
                        LocalDate.parse(entry.date, dateFormatter)
                            .atTime(12, 0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    } catch (e: Exception) {
                        null
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private val SETTINGS_KEY = stringPreferencesKey("at_risk_notification_settings")
        private val ALERTS_KEY = stringPreferencesKey("at_risk_alerts")
        private val HEALTH_CACHE_KEY = stringPreferencesKey("habit_health_cache")

        // Weather API - using Open-Meteo (free, no API key required)
        private const val WEATHER_API_BASE = "https://api.open-meteo.com/v1/forecast"

        // Risk score thresholds
        private const val HIGH_RISK_THRESHOLD = 0.7f
        private const val MEDIUM_RISK_THRESHOLD = 0.4f
        private const val CRITICAL_RISK_THRESHOLD = 0.85f

        private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ==================== Risk Assessment ====================

    override suspend fun getHabitRiskAssessment(habitId: String): HabitRiskAssessment {
        val habits = habitRepository.getAllHabits().first()
        val habit = habits.find { it.id == habitId } ?: throw IllegalArgumentException("Habit not found")
        val completionDates = getCompletionDatesForHabit(habitId)

        return calculateRiskAssessment(habit, completionDates)
    }

    override fun getAllRiskAssessments(): Flow<List<HabitRiskAssessment>> = _riskAssessments.asStateFlow()

    override fun getHighRiskHabits(): Flow<List<HabitRiskAssessment>> = _riskAssessments.map { assessments ->
        assessments.filter { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
    }

    override suspend fun refreshRiskAssessments(): Result<Unit> = runCatching {
        val habits = habitRepository.getAllHabits().first()
        val assessments = mutableListOf<HabitRiskAssessment>()

        // Fetch weather for weather-affected habits
        fetchWeather()

        // Get today's calendar events for conflict detection
        val todayDateString = LocalDate.now().format(dateFormatter)
        val todayEvents = try {
            calendarRepository.getEventsForDate(todayDateString).first()
        } catch (e: Exception) {
            emptyList()
        }

        for (habit in habits) {
            if (!habit.isEnabled) continue

            val completionDates = getCompletionDatesForHabit(habit.id)
            val assessment = calculateRiskAssessment(habit, completionDates, todayEvents)
            assessments.add(assessment)
        }

        _riskAssessments.value = assessments.sortedByDescending { it.riskScore }
    }  // Risk assessments are transient (recomputed), no persistence needed

    private suspend fun calculateRiskAssessment(
        habit: Habit,
        completionDates: List<Long>,
        calendarEvents: List<CalendarEvent> = emptyList()
    ): HabitRiskAssessment {
        val riskFactors = mutableListOf<AtRiskFactor>()
        var totalRiskScore = 0f

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val dayOfWeek = today.dayOfWeek.value // 1 = Monday

        // Factor 1: Day of week analysis
        val dayStats = analyzeDayOfWeek(habit.id, completionDates, dayOfWeek)
        if (dayStats.completionRate < 0.5f && dayStats.attempts >= 3) {
            val severity = 1f - dayStats.completionRate
            riskFactors.add(
                AtRiskFactor(
                    type = RiskFactorType.DAY_OF_WEEK,
                    severity = severity,
                    description = "You complete ${habit.name} only ${(dayStats.completionRate * 100).toInt()}% of the time on ${dayStats.dayName}s",
                    suggestion = "Try scheduling ${habit.name} earlier on ${dayStats.dayName}s"
                )
            )
            totalRiskScore += severity * 0.25f
        }

        // Factor 2: Time pressure from calendar
        val busyHours = calendarEvents.sumOf { event ->
            val duration = event.endTime - event.startTime
            duration / (1000 * 60 * 60) // Convert to hours
        }
        if (busyHours > 6) {
            val severity = min(1f, busyHours / 10f)
            riskFactors.add(
                AtRiskFactor(
                    type = RiskFactorType.TIME_PRESSURE,
                    severity = severity,
                    description = "Busy day detected with ${busyHours.toInt()} hours of meetings",
                    suggestion = "Consider completing ${habit.name} before your first meeting"
                )
            )
            totalRiskScore += severity * 0.2f
        }

        // Factor 3: Weather impact (for outdoor habits)
        if (isOutdoorHabit(habit)) {
            _currentWeather.value?.let { weather ->
                if (!weather.isOutdoorFriendly) {
                    riskFactors.add(
                        AtRiskFactor(
                            type = RiskFactorType.WEATHER,
                            severity = 0.6f,
                            description = "${weather.description} - not ideal for outdoor activities",
                            suggestion = "Consider an indoor alternative for ${habit.name} today"
                        )
                    )
                    totalRiskScore += 0.15f
                }
            }
        }

        // Factor 4: Streak fatigue (estimate current streak from completions)
        val currentStreak = estimateCurrentStreak(completionDates)
        if (currentStreak > 21) {
            val fatigueSeverity = min(1f, (currentStreak - 21) / 30f) * 0.3f
            riskFactors.add(
                AtRiskFactor(
                    type = RiskFactorType.STREAK_FATIGUE,
                    severity = fatigueSeverity,
                    description = "You're on a ${currentStreak}-day streak - that's impressive but watch for burnout",
                    suggestion = "Remember it's okay to take a rest day if needed"
                )
            )
            totalRiskScore += fatigueSeverity * 0.1f
        }

        // Factor 5: Recent misses pattern
        val nowMillis = System.currentTimeMillis()
        val sevenDaysAgo = nowMillis - (7 * 24 * 60 * 60 * 1000)
        val recentCompletions = completionDates.filter { it > sevenDaysAgo }
        val missedDays = 7 - recentCompletions.size
        if (missedDays >= 3) {
            val severity = missedDays / 7f
            riskFactors.add(
                AtRiskFactor(
                    type = RiskFactorType.RECENT_MISSES,
                    severity = severity,
                    description = "Missed $missedDays days in the past week",
                    suggestion = "Small consistent steps are better than perfection"
                )
            )
            totalRiskScore += severity * 0.2f
        }

        // Factor 6: Late in day without completion
        val completedToday = completionDates.any {
            val completionDate = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            completionDate == today
        }

        if (!completedToday && now.hour >= 18) { // After 6 PM
            val hoursUntilMidnight = 24 - now.hour
            val severity = 1f - (hoursUntilMidnight / 6f)
            riskFactors.add(
                AtRiskFactor(
                    type = RiskFactorType.LATE_IN_DAY,
                    severity = severity,
                    description = "Only $hoursUntilMidnight hours left to complete ${habit.name}",
                    suggestion = "Now would be a good time for ${habit.name}!"
                )
            )
            totalRiskScore += severity * 0.3f
        }

        // Factor 7: Weekend pattern
        if (dayOfWeek >= 6) { // Saturday or Sunday
            val weekendRate = getWeekendCompletionRate(habit.id, completionDates)
            if (weekendRate < 0.5f) {
                riskFactors.add(
                    AtRiskFactor(
                        type = RiskFactorType.WEEKEND_PATTERN,
                        severity = 0.4f,
                        description = "Weekend completion rate is only ${(weekendRate * 100).toInt()}%",
                        suggestion = "Weekends can disrupt routines - try maintaining consistency"
                    )
                )
                totalRiskScore += 0.1f
            }
        }

        // Determine risk level based on total score
        val riskLevel = when {
            totalRiskScore >= CRITICAL_RISK_THRESHOLD -> RiskLevel.CRITICAL
            totalRiskScore >= HIGH_RISK_THRESHOLD -> RiskLevel.HIGH
            totalRiskScore >= MEDIUM_RISK_THRESHOLD -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // Calculate optimal time
        val optimalTime = calculateOptimalTime(habit.id, completionDates, calendarEvents)

        // Generate preemptive suggestion
        val suggestion = generatePreemptiveSuggestion(habit, riskFactors, optimalTime)

        return HabitRiskAssessment(
            habitId = habit.id,
            habitName = habit.name,
            habitEmoji = habit.emoji,
            riskLevel = riskLevel,
            riskScore = min(1f, totalRiskScore),
            riskFactors = riskFactors.sortedByDescending { it.severity },
            preemptiveSuggestion = suggestion,
            optimalTimeToday = optimalTime
        )
    }

    private fun estimateCurrentStreak(completionDates: List<Long>): Int {
        if (completionDates.isEmpty()) return 0

        val sortedDates = completionDates.sortedDescending()
        val today = LocalDate.now()
        var streak = 0
        var checkDate = today

        for (i in 0..30) { // Check up to 30 days
            val hasCompletion = sortedDates.any { millis ->
                Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate() == checkDate
            }

            if (hasCompletion) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else if (checkDate == today) {
                // Today not completed yet is okay
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        return streak
    }

    private fun analyzeDayOfWeek(
        habitId: String,
        completionDates: List<Long>,
        dayOfWeek: Int
    ): DayOfWeekStats {
        val completionsOnDay = completionDates.filter {
            val date = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            date.dayOfWeek.value == dayOfWeek
        }

        val dayName = DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale.getDefault())

        // Estimate attempts (assume 30 days of data, ~4 of each day)
        val estimatedAttempts = max(1, 30 / 7)

        return DayOfWeekStats(
            dayOfWeek = dayOfWeek,
            dayName = dayName,
            completions = completionsOnDay.size,
            attempts = estimatedAttempts,
            completionRate = completionsOnDay.size.toFloat() / estimatedAttempts,
            averageCompletionTime = completionsOnDay.average().toLong().takeIf { completionsOnDay.isNotEmpty() }
        )
    }

    private fun getWeekendCompletionRate(habitId: String, completionDates: List<Long>): Float {
        val weekendCompletions = completionDates.filter {
            val date = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            date.dayOfWeek.value >= 6
        }

        // Estimate weekend days in 30-day period
        val estimatedWeekendDays = 8 // Roughly 4 weekends = 8 days
        return if (estimatedWeekendDays > 0) {
            weekendCompletions.size.toFloat() / estimatedWeekendDays
        } else 0f
    }

    private fun isOutdoorHabit(habit: Habit): Boolean {
        val outdoorKeywords = listOf("walk", "run", "jog", "hike", "bike", "cycle", "outdoor", "outside", "exercise", "move", "workout")
        return outdoorKeywords.any {
            habit.name.lowercase().contains(it) || habit.displayQuestion.lowercase().contains(it)
        }
    }

    private fun calculateOptimalTime(
        habitId: String,
        completionDates: List<Long>,
        calendarEvents: List<CalendarEvent>
    ): Long? {
        // Find historically best time
        val completionTimes = completionDates.map {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }

        if (completionTimes.isEmpty()) return null

        // Average completion hour
        val avgHour = completionTimes.map { it.hour }.average().toInt()

        // Find free slot around that time
        val today = LocalDate.now()
        val targetTime = LocalDateTime.of(today, LocalTime.of(avgHour, 0))

        // Check if there's a calendar conflict
        val targetStart = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val targetEnd = targetStart + (60 * 60 * 1000) // 1 hour window

        val hasConflict = calendarEvents.any { event ->
            event.startTime < targetEnd && event.endTime > targetStart
        }

        return if (hasConflict) {
            // Find nearest free slot
            findNearestFreeSlot(targetStart, calendarEvents)
        } else {
            targetStart
        }
    }

    private fun findNearestFreeSlot(preferredTime: Long, events: List<CalendarEvent>): Long {
        val sortedEvents = events.sortedBy { it.startTime }
        var checkTime = preferredTime

        for (event in sortedEvents) {
            if (checkTime + (60 * 60 * 1000) <= event.startTime) {
                // Found a free slot before this event
                return checkTime
            }
            // Move to after this event
            checkTime = event.endTime
        }

        return checkTime // After all events
    }

    private fun generatePreemptiveSuggestion(
        habit: Habit,
        riskFactors: List<AtRiskFactor>,
        optimalTime: Long?
    ): String? {
        if (riskFactors.isEmpty()) return null

        val primaryFactor = riskFactors.maxByOrNull { it.severity } ?: return null

        return when (primaryFactor.type) {
            RiskFactorType.TIME_PRESSURE -> {
                optimalTime?.let {
                    val time = Instant.ofEpochMilli(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                    "Consider completing ${habit.name} at ${time.hour}:${String.format("%02d", time.minute)} before your busy schedule"
                } ?: "Complete ${habit.name} early today - your schedule looks packed"
            }
            RiskFactorType.DAY_OF_WEEK -> "Today is historically challenging - try ${habit.name} at a different time"
            RiskFactorType.WEATHER -> "Weather isn't ideal - have a backup plan for ${habit.name}"
            RiskFactorType.LATE_IN_DAY -> "Don't forget ${habit.name} - there's still time!"
            RiskFactorType.RECENT_MISSES -> "Getting back on track with ${habit.name} today will feel great"
            else -> primaryFactor.suggestion
        }
    }

    // ==================== Habit Health ====================

    override fun getHabitHealth(habitId: String): Flow<HabitHealth?> =
        _habitHealthMap.map { it[habitId] }

    override fun getAllHabitHealth(): Flow<List<HabitHealth>> =
        _habitHealthMap.map { it.values.toList() }

    override suspend fun refreshHabitHealth(): Result<Unit> = runCatching {
        val habits = habitRepository.getAllHabits().first()
        val healthMap = mutableMapOf<String, HabitHealth>()

        for (habit in habits) {
            if (!habit.isEnabled) continue

            val completionDates = getCompletionDatesForHabit(habit.id)
            val health = calculateHabitHealth(habit, completionDates)
            healthMap[habit.id] = health
        }

        _habitHealthMap.value = healthMap
        persistHabitHealth()
    }

    private fun calculateHabitHealth(habit: Habit, completionDates: List<Long>): HabitHealth {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000)
        val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000)

        val last7Days = completionDates.filter { it >= sevenDaysAgo }
        val last30Days = completionDates.filter { it >= thirtyDaysAgo }

        // Completion rates
        val rate7Days = last7Days.size / 7f
        val rate30Days = last30Days.size / 30f

        // Day of week analysis
        val dayStats = (1..7).map { day ->
            val dayCompletions = completionDates.filter {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .dayOfWeek.value == day
            }
            day to dayCompletions.size
        }

        val bestDay = dayStats.maxByOrNull { it.second }?.let {
            DayOfWeek.of(it.first).getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
        val worstDay = dayStats.minByOrNull { it.second }?.let {
            DayOfWeek.of(it.first).getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        // Time of day analysis
        val morningCount = completionDates.count {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime().hour < 12
        }
        val afternoonCount = completionDates.count {
            val hour = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime().hour
            hour in 12..17
        }
        val eveningCount = completionDates.count {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime().hour >= 18
        }

        val bestTimeOfDay = when {
            morningCount >= afternoonCount && morningCount >= eveningCount -> "Morning"
            afternoonCount >= morningCount && afternoonCount >= eveningCount -> "Afternoon"
            else -> "Evening"
        }

        // Current streak
        val currentStreak = estimateCurrentStreak(completionDates)
        val longestStreak = estimateLongestStreak(completionDates)

        // Health score calculation
        val streakBonus = min(20, currentStreak)
        val consistencyBonus = (rate7Days * 30).toInt()
        val trendBonus = if (rate7Days > rate30Days) 10 else 0
        val healthScore = min(100, 40 + streakBonus + consistencyBonus + trendBonus)

        // Trend determination
        val trend = when {
            completionDates.size < 7 -> HealthTrend.NEW
            rate7Days > rate30Days * 1.1 -> HealthTrend.IMPROVING
            rate7Days < rate30Days * 0.9 -> HealthTrend.DECLINING
            else -> HealthTrend.STABLE
        }

        return HabitHealth(
            habitId = habit.id,
            healthScore = healthScore,
            trend = trend,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completionRateLast7Days = rate7Days,
            completionRateLast30Days = rate30Days,
            bestDay = bestDay,
            worstDay = worstDay,
            bestTimeOfDay = bestTimeOfDay,
            averageCompletionTime = completionDates.average().toLong().takeIf { completionDates.isNotEmpty() },
            lastCompletedAt = completionDates.maxOrNull(),
            missedInLastWeek = 7 - last7Days.size
        )
    }

    private fun estimateLongestStreak(completionDates: List<Long>): Int {
        if (completionDates.isEmpty()) return 0

        val sortedDates = completionDates
            .map { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()

        var longest = 0
        var current = 0
        var prevDate: LocalDate? = null

        for (date in sortedDates) {
            if (prevDate == null || date == prevDate.plusDays(1)) {
                current++
            } else {
                current = 1
            }
            longest = max(longest, current)
            prevDate = date
        }

        return longest
    }

    // ==================== Pattern Analysis ====================

    override suspend fun getHabitPattern(habitId: String): HabitPattern? {
        val completionDates = getCompletionDatesForHabit(habitId)
        if (completionDates.isEmpty()) return null

        val dayStats = (1..7).map { day ->
            val dayName = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.getDefault())
            val dayCompletions = completionDates.filter {
                Instant.ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .dayOfWeek.value == day
            }
            DayOfWeekStats(
                dayOfWeek = day,
                dayName = dayName,
                completions = dayCompletions.size,
                attempts = 13, // ~90 days / 7
                completionRate = dayCompletions.size / 13f,
                averageCompletionTime = dayCompletions.average().toLong().takeIf { dayCompletions.isNotEmpty() }
            )
        }

        val morningCompletions = completionDates.count {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime().hour < 12
        }
        val afternoonCompletions = completionDates.count {
            val hour = Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime().hour
            hour in 12..17
        }
        val eveningCompletions = completionDates.size - morningCompletions - afternoonCompletions

        val weekdayCompletions = completionDates.count {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .dayOfWeek.value < 6
        }
        val weekendCompletions = completionDates.size - weekdayCompletions

        val total = completionDates.size.coerceAtLeast(1).toFloat()

        return HabitPattern(
            habitId = habitId,
            dayOfWeekStats = dayStats,
            morningCompletionRate = morningCompletions / total,
            afternoonCompletionRate = afternoonCompletions / total,
            eveningCompletionRate = eveningCompletions / total,
            weekdayCompletionRate = if (weekdayCompletions > 0) weekdayCompletions / (total * 5f / 7f) else 0f,
            weekendCompletionRate = if (weekendCompletions > 0) weekendCompletions / (total * 2f / 7f) else 0f,
            averageStreakLength = 7f, // Simplified
            streakBreakPatterns = emptyList()
        )
    }

    override suspend fun getDayOfWeekStats(habitId: String): List<DayOfWeekStats> {
        return getHabitPattern(habitId)?.dayOfWeekStats ?: emptyList()
    }

    override suspend fun getStreakBreakPatterns(habitId: String): List<StreakBreakPattern> {
        return getHabitPattern(habitId)?.streakBreakPatterns ?: emptyList()
    }

    // ==================== Weather Integration ====================

    override suspend fun getCurrentWeather(): WeatherCondition? {
        return _currentWeather.value ?: fetchWeather()
    }

    private suspend fun fetchWeather(): WeatherCondition? {
        return try {
            // Use real GPS coordinates, fall back to Dubai
            val location = locationService?.getLastLocation()
            val latitude = location?.latitude ?: 25.2048
            val longitude = location?.longitude ?: 55.2708

            val response = httpClient.get(WEATHER_API_BASE) {
                parameter("latitude", latitude)
                parameter("longitude", longitude)
                parameter("current", "temperature_2m,weather_code,wind_speed_10m,relative_humidity_2m")
            }

            val data: OpenMeteoResponse = response.body()
            val weather = parseWeatherResponse(data)
            _currentWeather.value = weather
            weather
        } catch (e: Exception) {
            null
        }
    }

    @Serializable
    private data class OpenMeteoResponse(
        val current: CurrentWeather? = null
    )

    @Serializable
    private data class CurrentWeather(
        val temperature_2m: Float = 0f,
        val weather_code: Int = 0,
        val wind_speed_10m: Float = 0f,
        val relative_humidity_2m: Int = 0
    )

    private fun parseWeatherResponse(response: OpenMeteoResponse): WeatherCondition {
        val current = response.current ?: return WeatherCondition(
            temperature = 25f,
            condition = WeatherType.UNKNOWN,
            description = "Weather unavailable",
            humidity = 50,
            windSpeed = 0f,
            isOutdoorFriendly = true
        )

        val weatherType = when (current.weather_code) {
            0 -> WeatherType.SUNNY
            in 1..3 -> WeatherType.CLOUDY
            in 45..48 -> WeatherType.FOGGY
            in 51..67 -> WeatherType.RAINY
            in 71..77 -> WeatherType.SNOWY
            in 80..82 -> WeatherType.RAINY
            in 95..99 -> WeatherType.STORMY
            else -> WeatherType.UNKNOWN
        }

        val description = when (weatherType) {
            WeatherType.SUNNY -> "Clear and sunny"
            WeatherType.CLOUDY -> "Partly cloudy"
            WeatherType.RAINY -> "Rainy conditions"
            WeatherType.STORMY -> "Stormy weather"
            WeatherType.SNOWY -> "Snow expected"
            WeatherType.FOGGY -> "Foggy conditions"
            WeatherType.WINDY -> "Windy"
            WeatherType.EXTREME_HEAT -> "Extreme heat"
            WeatherType.EXTREME_COLD -> "Extreme cold"
            WeatherType.UNKNOWN -> "Weather conditions unknown"
        }

        val isOutdoorFriendly = weatherType in listOf(WeatherType.SUNNY, WeatherType.CLOUDY) &&
                current.temperature_2m in 10f..35f &&
                current.wind_speed_10m < 40f

        return WeatherCondition(
            temperature = current.temperature_2m,
            condition = weatherType,
            description = description,
            humidity = current.relative_humidity_2m,
            windSpeed = current.wind_speed_10m,
            isOutdoorFriendly = isOutdoorFriendly
        )
    }

    override suspend fun getWeatherImpactedHabits(): List<Pair<String, AtRiskFactor>> {
        val weather = getCurrentWeather() ?: return emptyList()
        if (weather.isOutdoorFriendly) return emptyList()

        val habits = habitRepository.getAllHabits().first()
        return habits
            .filter { isOutdoorHabit(it) }
            .map { habit ->
                habit.id to AtRiskFactor(
                    type = RiskFactorType.WEATHER,
                    severity = 0.6f,
                    description = "${weather.description} may affect ${habit.name}",
                    suggestion = "Consider an indoor alternative"
                )
            }
    }

    // ==================== Daily Summary ====================

    override fun getDailyRiskSummary(): Flow<DailyRiskSummary?> = _dailySummary.asStateFlow()

    override suspend fun generateDailyRiskSummary(): DailyRiskSummary {
        refreshRiskAssessments()

        val assessments = _riskAssessments.value
        val weather = getCurrentWeather()

        val highRiskCount = assessments.count { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
        val mediumRiskCount = assessments.count { it.riskLevel == RiskLevel.MEDIUM }

        val overallRisk = when {
            highRiskCount >= 2 -> RiskLevel.HIGH
            highRiskCount >= 1 || mediumRiskCount >= 3 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val recommendations = mutableListOf<String>()
        if (highRiskCount > 0) {
            recommendations.add("Focus on your ${highRiskCount} high-risk habit${if (highRiskCount > 1) "s" else ""} first")
        }
        if (weather?.isOutdoorFriendly == false) {
            recommendations.add("Weather may affect outdoor activities - have backup plans")
        }

        val summary = DailyRiskSummary(
            date = System.currentTimeMillis(),
            overallRiskLevel = overallRisk,
            habitsAtRisk = assessments.filter { it.riskLevel != RiskLevel.LOW },
            totalHabits = assessments.size,
            highRiskCount = highRiskCount,
            mediumRiskCount = mediumRiskCount,
            weatherImpact = weather,
            busyScheduleDetected = assessments.any {
                it.riskFactors.any { f -> f.type == RiskFactorType.TIME_PRESSURE }
            },
            calendarConflicts = assessments.sumOf {
                it.riskFactors.count { f -> f.type == RiskFactorType.CALENDAR_CONFLICT }
            },
            recommendations = recommendations
        )

        _dailySummary.value = summary
        return summary
    }

    // ==================== Alerts ====================

    override fun getActiveAlerts(): Flow<List<AtRiskAlert>> = _activeAlerts.asStateFlow()

    override suspend fun dismissAlert(alertId: String) {
        _activeAlerts.value = _activeAlerts.value.map {
            if (it.id == alertId) it.copy(dismissed = true) else it
        }
        persistAlerts()
    }

    override suspend fun generateAlerts(): List<AtRiskAlert> {
        val assessments = _riskAssessments.value
        val settings = getNotificationSettings().first()
        val now = System.currentTimeMillis()

        val alerts = mutableListOf<AtRiskAlert>()

        for (assessment in assessments) {
            val shouldAlert = when (assessment.riskLevel) {
                RiskLevel.CRITICAL, RiskLevel.HIGH -> settings.notifyOnHighRisk
                RiskLevel.MEDIUM -> settings.notifyOnMediumRisk
                RiskLevel.LOW -> false
            }

            if (!shouldAlert) continue

            val title = when (assessment.riskLevel) {
                RiskLevel.CRITICAL -> "${assessment.habitEmoji} Streak at risk!"
                RiskLevel.HIGH -> "${assessment.habitEmoji} ${assessment.habitName} needs attention"
                RiskLevel.MEDIUM -> "${assessment.habitEmoji} Heads up about ${assessment.habitName}"
                RiskLevel.LOW -> ""
            }

            val primaryFactor = assessment.riskFactors.firstOrNull()
            val message = primaryFactor?.description ?: "Today might be challenging for this habit"

            alerts.add(
                AtRiskAlert(
                    id = "${assessment.habitId}_${now}",
                    habitId = assessment.habitId,
                    habitName = assessment.habitName,
                    habitEmoji = assessment.habitEmoji,
                    riskLevel = assessment.riskLevel,
                    title = title,
                    message = message,
                    actionSuggestion = assessment.preemptiveSuggestion,
                    suggestedTime = assessment.optimalTimeToday,
                    expiresAt = now + (12 * 60 * 60 * 1000) // 12 hours
                )
            )
        }

        _activeAlerts.value = alerts
        persistAlerts()
        return alerts
    }

    override suspend fun clearExpiredAlerts() {
        val now = System.currentTimeMillis()
        _activeAlerts.value = _activeAlerts.value.filter { it.expiresAt > now && !it.dismissed }
        persistAlerts()
    }

    // ==================== Settings ====================

    override fun getNotificationSettings(): Flow<AtRiskNotificationSettings> =
        context.atRiskDataStore.data.map { prefs ->
            prefs[SETTINGS_KEY]?.let {
                try {
                    json.decodeFromString<AtRiskNotificationSettings>(it)
                } catch (e: Exception) {
                    AtRiskNotificationSettings()
                }
            } ?: AtRiskNotificationSettings()
        }

    override suspend fun updateNotificationSettings(settings: AtRiskNotificationSettings) {
        context.atRiskDataStore.edit { prefs ->
            prefs[SETTINGS_KEY] = json.encodeToString(AtRiskNotificationSettings.serializer(), settings)
        }
    }

    // ==================== Preemptive Suggestions ====================

    override suspend fun getOptimalTimeForHabit(habitId: String): Long? {
        val assessment = _riskAssessments.value.find { it.habitId == habitId }
        return assessment?.optimalTimeToday
    }

    override suspend fun getPreemptiveSuggestions(): List<Pair<String, String>> {
        return _riskAssessments.value
            .filter { it.preemptiveSuggestion != null }
            .map { it.habitId to it.preemptiveSuggestion!! }
    }
}
