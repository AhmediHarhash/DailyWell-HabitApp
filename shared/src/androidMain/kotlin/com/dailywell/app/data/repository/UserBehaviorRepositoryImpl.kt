package com.dailywell.app.data.repository

import com.dailywell.app.api.FirebaseService
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

/**
 * User Behavior Repository Implementation
 *
 * Tracks user patterns and preferences for AI personalization.
 * Uses DataStore for local caching and Firebase for cloud sync.
 *
 * Key features:
 * - Chronotype detection (morning person vs night owl)
 * - Motivation style inference (supportive vs direct vs analytical)
 * - Streak recovery rate calculation
 * - Habit correlation analysis
 * - Attitude/engagement scoring
 */
class UserBehaviorRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val firebaseService: FirebaseService,
    private val entryRepository: EntryRepository
) : UserBehaviorRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val PROFILE_KEY = "user_behavior_profile"
        private const val EVENTS_KEY = "behavior_events"
        private const val DAILY_SUMMARIES_KEY = "daily_behavior_summaries"

        // Analysis thresholds
        private const val MIN_SAMPLES_FOR_CHRONOTYPE = 7  // 7 days of data
        private const val MIN_SAMPLES_FOR_CORRELATION = 14  // 2 weeks
        private const val MORNING_HOUR_THRESHOLD = 10  // Before 10 AM
        private const val EVENING_HOUR_THRESHOLD = 18  // After 6 PM

        // Event retention
        private const val MAX_EVENTS_STORED = 500
        private const val MAX_DAILY_SUMMARIES = 90  // 3 months
    }

    private val _profile = MutableStateFlow<UserBehaviorProfile?>(null)
    private val _events = MutableStateFlow<List<BehaviorEvent>>(emptyList())

    // ============ Profile Access ============

    override fun getUserProfile(): Flow<UserBehaviorProfile?> {
        return dataStoreManager.getString(PROFILE_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<UserBehaviorProfile>(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun getPromptContext(): String {
        val profile = getUserProfile().first()
        return profile?.toPromptContext() ?: "New user - no behavior patterns yet"
    }

    // ============ Event Tracking ============

    override suspend fun trackEvent(event: BehaviorEvent) {
        val events = loadEvents().toMutableList()
        events.add(event)

        // Trim old events if too many
        if (events.size > MAX_EVENTS_STORED) {
            events.removeAt(0)
        }

        saveEvents(events)

        // Update daily summary
        updateDailySummary(event)
    }

    override suspend fun trackHabitCompletion(habitId: String, completedAt: String) {
        val event = BehaviorEvent(
            id = generateEventId(),
            userId = getCurrentUserId(),
            eventType = BehaviorEventType.HABIT_COMPLETED,
            habitId = habitId,
            value = extractHour(completedAt).toFloat(),
            metadata = mapOf("completedAt" to completedAt),
            timestamp = Clock.System.now().toString()
        )
        trackEvent(event)
    }

    override suspend fun trackHabitSkipped(habitId: String, reason: String?) {
        val event = BehaviorEvent(
            id = generateEventId(),
            userId = getCurrentUserId(),
            eventType = BehaviorEventType.HABIT_SKIPPED,
            habitId = habitId,
            metadata = reason?.let { mapOf("reason" to it) } ?: emptyMap(),
            timestamp = Clock.System.now().toString()
        )
        trackEvent(event)
    }

    override suspend fun trackStreakBroken(habitId: String, streakLength: Int) {
        val event = BehaviorEvent(
            id = generateEventId(),
            userId = getCurrentUserId(),
            eventType = BehaviorEventType.STREAK_BROKEN,
            habitId = habitId,
            value = streakLength.toFloat(),
            timestamp = Clock.System.now().toString()
        )
        trackEvent(event)

        // Update profile with missed streak info
        updateProfile { profile ->
            profile.copy(
                currentMissedStreak = 1,
                totalMissedDays = profile.totalMissedDays + 1
            )
        }
    }

    override suspend fun trackStreakRecovery(habitId: String, daysAfterMiss: Int) {
        val event = BehaviorEvent(
            id = generateEventId(),
            userId = getCurrentUserId(),
            eventType = BehaviorEventType.STREAK_RECOVERED,
            habitId = habitId,
            value = daysAfterMiss.toFloat(),
            timestamp = Clock.System.now().toString()
        )
        trackEvent(event)

        // Update recovery rate
        updateProfile { profile ->
            val newRecoveryRate = calculateStreakRecoveryRateInternal(profile)
            profile.copy(
                streakRecoveryRate = newRecoveryRate,
                currentMissedStreak = 0
            )
        }
    }

    override suspend fun trackAIInteraction(
        messageType: String,
        sentiment: Float?,
        wasHelpful: Boolean?
    ) {
        val eventType = when {
            sentiment != null && sentiment < -0.3f -> BehaviorEventType.EXPRESSED_FRUSTRATION
            sentiment != null && sentiment > 0.3f -> BehaviorEventType.EXPRESSED_MOTIVATION
            else -> BehaviorEventType.AI_MESSAGE_SENT
        }

        val event = BehaviorEvent(
            id = generateEventId(),
            userId = getCurrentUserId(),
            eventType = eventType,
            value = sentiment ?: 0f,
            metadata = buildMap {
                put("messageType", messageType)
                wasHelpful?.let { put("wasHelpful", it.toString()) }
            },
            timestamp = Clock.System.now().toString()
        )
        trackEvent(event)

        // Update attitude score based on sentiment
        if (sentiment != null) {
            updateProfile { profile ->
                // Weighted moving average: new sentiment has 20% weight
                val newAttitude = profile.attitudeScore * 0.8f + sentiment * 0.2f
                profile.copy(attitudeScore = newAttitude.coerceIn(-1f, 1f))
            }
        }

        // Update feature usage
        updateProfile { profile ->
            val usage = profile.featureUsage
            profile.copy(
                featureUsage = usage.copy(
                    usesAICoaching = true,
                    aiCoachingFrequency = usage.aiCoachingFrequency + 0.1f  // Increment
                )
            )
        }
    }

    override suspend fun trackFeatureUsage(feature: String, durationSeconds: Int?) {
        val eventType = when (feature) {
            "tts" -> BehaviorEventType.TTS_USED
            "voice_input" -> BehaviorEventType.VOICE_INPUT_USED
            "insight" -> BehaviorEventType.INSIGHT_VIEWED
            else -> BehaviorEventType.SCREEN_VIEWED
        }

        val event = BehaviorEvent(
            id = generateEventId(),
            userId = getCurrentUserId(),
            eventType = eventType,
            value = (durationSeconds ?: 0).toFloat(),
            metadata = mapOf("feature" to feature),
            timestamp = Clock.System.now().toString()
        )
        trackEvent(event)

        // Update feature usage profile
        updateProfile { profile ->
            val usage = profile.featureUsage
            val newUsage = when (feature) {
                "tts" -> usage.copy(usesTTS = true)
                "voice_input" -> usage.copy(usesVoiceInput = true)
                "insight" -> usage.copy(
                    usesPatternInsights = true,
                    insightsViewCount = usage.insightsViewCount + 1
                )
                "calendar" -> usage.copy(usesCalendarIntegration = true)
                "social" -> usage.copy(usesSocialFeatures = true)
                "widget" -> usage.copy(usesWidgets = true)
                else -> usage.copy(lastFeatureUsed = feature)
            }
            profile.copy(featureUsage = newUsage)
        }
    }

    override suspend fun trackNotificationResponse(responded: Boolean, delaySeconds: Int?) {
        val eventType = if (responded) {
            BehaviorEventType.NOTIFICATION_TAPPED
        } else {
            BehaviorEventType.NOTIFICATION_DISMISSED
        }

        val event = BehaviorEvent(
            id = generateEventId(),
            userId = getCurrentUserId(),
            eventType = eventType,
            value = (delaySeconds ?: 0).toFloat(),
            timestamp = Clock.System.now().toString()
        )
        trackEvent(event)

        // Update notification response rate
        updateProfile { profile ->
            val usage = profile.featureUsage
            val currentRate = usage.notificationResponseRate
            // Weighted average with new response
            val newRate = if (responded) {
                currentRate * 0.9f + 0.1f
            } else {
                currentRate * 0.9f
            }
            profile.copy(
                featureUsage = usage.copy(notificationResponseRate = newRate)
            )
        }
    }

    override suspend fun trackSession(durationSeconds: Int, screensViewed: List<String>) {
        val event = BehaviorEvent(
            id = generateEventId(),
            userId = getCurrentUserId(),
            eventType = BehaviorEventType.SESSION_ENDED,
            value = durationSeconds.toFloat(),
            metadata = mapOf("screens" to screensViewed.joinToString(",")),
            timestamp = Clock.System.now().toString()
        )
        trackEvent(event)

        // Update session stats
        updateProfile { profile ->
            val usage = profile.featureUsage
            val totalSessions = (usage.sessionsPerDay * 7 + 1) / 7  // Rough weekly average
            val avgDuration = (usage.averageSessionDurationSeconds * 0.9f + durationSeconds * 0.1f).toInt()
            profile.copy(
                featureUsage = usage.copy(
                    averageSessionDurationSeconds = avgDuration,
                    sessionsPerDay = totalSessions
                )
            )
        }
    }

    // ============ Profile Analysis ============

    override suspend fun detectChronotype(): Chronotype {
        val events = loadEvents()
        val completionEvents = events.filter { it.eventType == BehaviorEventType.HABIT_COMPLETED }

        if (completionEvents.size < MIN_SAMPLES_FOR_CHRONOTYPE) {
            return Chronotype.FLEXIBLE  // Not enough data
        }

        val hours = completionEvents.mapNotNull { event ->
            event.metadata["completedAt"]?.let { extractHour(it) }
        }

        if (hours.isEmpty()) return Chronotype.FLEXIBLE

        val averageHour = hours.average()
        val morningCompletions = hours.count { it < MORNING_HOUR_THRESHOLD }
        val eveningCompletions = hours.count { it >= EVENING_HOUR_THRESHOLD }

        return when {
            morningCompletions > eveningCompletions * 2 -> Chronotype.MORNING_PERSON
            eveningCompletions > morningCompletions * 2 -> Chronotype.NIGHT_OWL
            averageHour < 12 -> Chronotype.MORNING_PERSON
            averageHour > 16 -> Chronotype.NIGHT_OWL
            else -> Chronotype.FLEXIBLE
        }
    }

    override suspend fun detectMotivationStyle(): MotivationStyle {
        val events = loadEvents()

        // Check AI interaction patterns
        val aiEvents = events.filter {
            it.eventType in listOf(
                BehaviorEventType.AI_MESSAGE_SENT,
                BehaviorEventType.AI_RESPONSE_RATED,
                BehaviorEventType.EXPRESSED_FRUSTRATION,
                BehaviorEventType.EXPRESSED_MOTIVATION
            )
        }

        // Check insight usage (analytical users view insights more)
        val insightViews = events.count { it.eventType == BehaviorEventType.INSIGHT_VIEWED }

        // Check response to frustration events
        val frustrationEvents = events.filter { it.eventType == BehaviorEventType.EXPRESSED_FRUSTRATION }
        val motivationEvents = events.filter { it.eventType == BehaviorEventType.EXPRESSED_MOTIVATION }

        // Analytical: views insights frequently
        if (insightViews > 10) {
            return MotivationStyle.ANALYTICAL
        }

        // Direct: fewer AI messages, more action-oriented
        if (aiEvents.size < 5 && events.size > 20) {
            return MotivationStyle.DIRECT
        }

        // Supportive: default for engaged users with moderate AI usage
        return MotivationStyle.SUPPORTIVE
    }

    override suspend fun calculateStreakRecoveryRate(): Float {
        val events = loadEvents()

        val streakBroken = events.count { it.eventType == BehaviorEventType.STREAK_BROKEN }
        val streakRecovered = events.count { it.eventType == BehaviorEventType.STREAK_RECOVERED }

        if (streakBroken == 0) return 0.5f  // Default

        return (streakRecovered.toFloat() / streakBroken).coerceIn(0f, 1f)
    }

    override suspend fun calculateHabitCorrelations(): List<HabitCorrelationData> {
        val correlations = mutableListOf<HabitCorrelationData>()

        try {
            // Get completion history for correlation analysis
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val startDate = today.minus(DatePeriod(days = 30))

            // Analyze habit pairs
            val habitIds = listOf("sleep", "water", "move", "vegetables", "calm", "connect", "unplug")

            for (i in habitIds.indices) {
                for (j in i + 1 until habitIds.size) {
                    val habit1 = habitIds[i]
                    val habit2 = habitIds[j]

                    val correlation = calculatePairCorrelation(habit1, habit2, startDate, today)
                    if (abs(correlation) > 0.3f) {  // Only strong correlations
                        correlations.add(
                            HabitCorrelationData(
                                habit1 = habit1,
                                habit2 = habit2,
                                correlation = correlation,
                                sampleSize = 30,
                                confidence = if (abs(correlation) > 0.5f) 0.8f else 0.5f
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Return empty if analysis fails
        }

        return correlations.sortedByDescending { abs(it.correlation) }.take(5)
    }

    private suspend fun calculatePairCorrelation(
        habit1: String,
        habit2: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Float {
        // Simplified correlation calculation
        // In production, would use actual completion data from entryRepository
        val events = loadEvents()

        val habit1Completions = events.filter {
            it.eventType == BehaviorEventType.HABIT_COMPLETED &&
                    it.habitId == habit1
        }
        val habit2Completions = events.filter {
            it.eventType == BehaviorEventType.HABIT_COMPLETED &&
                    it.habitId == habit2
        }

        // Check if habits are often completed on same days
        val habit1Dates = habit1Completions.map { it.timestamp.take(10) }.toSet()
        val habit2Dates = habit2Completions.map { it.timestamp.take(10) }.toSet()

        val overlap = habit1Dates.intersect(habit2Dates).size
        val total = (habit1Dates + habit2Dates).size

        if (total == 0) return 0f

        return (overlap.toFloat() / total * 2 - 1).coerceIn(-1f, 1f)
    }

    override suspend fun calculateAttitudeScore(): Float {
        val events = loadEvents()

        // Recent events weighted more heavily
        val recentEvents = events.takeLast(50)

        var score = 0f
        var count = 0

        for (event in recentEvents) {
            when (event.eventType) {
                BehaviorEventType.HABIT_COMPLETED -> {
                    score += 0.1f
                    count++
                }
                BehaviorEventType.STREAK_BROKEN -> {
                    score -= 0.3f
                    count++
                }
                BehaviorEventType.STREAK_RECOVERED -> {
                    score += 0.2f
                    count++
                }
                BehaviorEventType.EXPRESSED_FRUSTRATION -> {
                    score -= 0.5f
                    count++
                }
                BehaviorEventType.EXPRESSED_MOTIVATION -> {
                    score += 0.5f
                    count++
                }
                BehaviorEventType.CELEBRATION_TRIGGERED -> {
                    score += 0.3f
                    count++
                }
                else -> { /* No impact */ }
            }
        }

        return if (count > 0) (score / count).coerceIn(-1f, 1f) else 0f
    }

    override suspend fun getDailySummary(date: String): DailyBehaviorSummary? {
        val summaries = loadDailySummaries()
        return summaries.find { it.date == date }
    }

    override suspend fun getWeeklyAnalysis(weekStartDate: String): WeeklyPatternAnalysis? {
        val summaries = loadDailySummaries()

        // Get summaries for the week
        val weekSummaries = summaries.filter { summary ->
            try {
                val summaryDate = LocalDate.parse(summary.date)
                val weekStart = LocalDate.parse(weekStartDate)
                val weekEnd = weekStart.plus(DatePeriod(days = 6))
                summaryDate >= weekStart && summaryDate <= weekEnd
            } catch (e: Exception) {
                false
            }
        }

        if (weekSummaries.isEmpty()) return null

        // Calculate day patterns (1-7 = Monday-Sunday)
        val dayPatterns = mutableMapOf<Int, Float>()
        for (summary in weekSummaries) {
            try {
                val date = LocalDate.parse(summary.date)
                val dayOfWeek = date.dayOfWeek.ordinal + 1  // 1-7
                dayPatterns[dayOfWeek] = summary.completionRate
            } catch (e: Exception) { }
        }

        // Calculate time patterns
        val timePatterns = mutableMapOf<Int, Float>()
        for (summary in weekSummaries) {
            summary.firstCheckInHour?.let { hour ->
                timePatterns[hour] = (timePatterns[hour] ?: 0f) + 1f
            }
        }

        val strongestDay = dayPatterns.maxByOrNull { it.value }?.key ?: 1
        val weakestDay = dayPatterns.minByOrNull { it.value }?.key ?: 7
        val peakHour = timePatterns.maxByOrNull { it.value }?.key ?: 9

        val avgCompletionRate = weekSummaries.map { it.completionRate }.average().toFloat()
        val consistencyScore = 1f - (weekSummaries.map { it.completionRate }
            .let { rates ->
                if (rates.size > 1) {
                    val mean = rates.average()
                    val variance = rates.map { (it - mean) * (it - mean) }.average()
                    kotlin.math.sqrt(variance).toFloat()
                } else 0f
            })

        return WeeklyPatternAnalysis(
            weekStartDate = weekStartDate,
            userId = getCurrentUserId(),
            dayPatterns = dayPatterns,
            timePatterns = timePatterns,
            strongestDay = strongestDay,
            weakestDay = weakestDay,
            peakHour = peakHour,
            consistencyScore = consistencyScore.coerceIn(0f, 1f),
            weekOverWeekTrend = 0f  // Would need previous week data
        )
    }

    // ============ Profile Updates ============

    override suspend fun refreshProfile() {
        val currentProfile = getUserProfile().first() ?: createDefaultProfile()

        val updatedProfile = currentProfile.copy(
            chronotype = detectChronotype(),
            motivationStyle = detectMotivationStyle(),
            streakRecoveryRate = calculateStreakRecoveryRate(),
            habitCorrelations = calculateHabitCorrelations(),
            attitudeScore = calculateAttitudeScore(),
            engagementLevel = calculateEngagementLevel(calculateAttitudeScore()),
            lastUpdated = Clock.System.now().toString(),
            profileVersion = currentProfile.profileVersion + 1
        )

        saveProfile(updatedProfile)
    }

    override suspend fun updateProfile(update: (UserBehaviorProfile) -> UserBehaviorProfile) {
        val current = getUserProfile().first() ?: createDefaultProfile()
        val updated = update(current).copy(
            lastUpdated = Clock.System.now().toString()
        )
        saveProfile(updated)
    }

    override suspend fun syncToFirebase() {
        val profile = getUserProfile().first() ?: return

        try {
            firebaseService.updateUserBehaviorProfile(profile)
        } catch (e: Exception) {
            // Silently fail - local is source of truth
        }
    }

    override suspend fun clearAllData() {
        dataStoreManager.remove(PROFILE_KEY)
        dataStoreManager.remove(EVENTS_KEY)
        dataStoreManager.remove(DAILY_SUMMARIES_KEY)
        _profile.value = null
        _events.value = emptyList()
    }

    // ============ Private Helpers ============

    private suspend fun loadEvents(): List<BehaviorEvent> {
        return dataStoreManager.getString(EVENTS_KEY).first()?.let { jsonString ->
            try {
                json.decodeFromString<List<BehaviorEvent>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    private suspend fun saveEvents(events: List<BehaviorEvent>) {
        dataStoreManager.putString(EVENTS_KEY, json.encodeToString(events))
        _events.value = events
    }

    private suspend fun loadDailySummaries(): List<DailyBehaviorSummary> {
        return dataStoreManager.getString(DAILY_SUMMARIES_KEY).first()?.let { jsonString ->
            try {
                json.decodeFromString<List<DailyBehaviorSummary>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    private suspend fun saveDailySummaries(summaries: List<DailyBehaviorSummary>) {
        // Keep only recent summaries
        val trimmed = summaries.takeLast(MAX_DAILY_SUMMARIES)
        dataStoreManager.putString(DAILY_SUMMARIES_KEY, json.encodeToString(trimmed))
    }

    private suspend fun saveProfile(profile: UserBehaviorProfile) {
        dataStoreManager.putString(PROFILE_KEY, json.encodeToString(profile))
        _profile.value = profile
    }

    private suspend fun updateDailySummary(event: BehaviorEvent) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val summaries = loadDailySummaries().toMutableList()

        val existingIndex = summaries.indexOfFirst { it.date == today }
        val existing = if (existingIndex >= 0) summaries[existingIndex] else null

        val updated = when (event.eventType) {
            BehaviorEventType.HABIT_COMPLETED -> {
                val hour = event.value.toInt()
                (existing ?: createEmptySummary(today)).copy(
                    habitsCompleted = (existing?.habitsCompleted ?: 0) + 1,
                    firstCheckInHour = existing?.firstCheckInHour ?: hour,
                    lastCheckInHour = hour,
                    completionRate = calculateTodayCompletionRate(
                        (existing?.habitsCompleted ?: 0) + 1,
                        existing?.habitsMissed ?: 0
                    )
                )
            }
            BehaviorEventType.HABIT_SKIPPED, BehaviorEventType.HABIT_DISMISSED -> {
                (existing ?: createEmptySummary(today)).copy(
                    habitsMissed = (existing?.habitsMissed ?: 0) + 1,
                    completionRate = calculateTodayCompletionRate(
                        existing?.habitsCompleted ?: 0,
                        (existing?.habitsMissed ?: 0) + 1
                    )
                )
            }
            BehaviorEventType.AI_MESSAGE_SENT -> {
                (existing ?: createEmptySummary(today)).copy(
                    aiMessagesCount = (existing?.aiMessagesCount ?: 0) + 1
                )
            }
            BehaviorEventType.SESSION_ENDED -> {
                (existing ?: createEmptySummary(today)).copy(
                    sessionCount = (existing?.sessionCount ?: 0) + 1,
                    totalSessionSeconds = (existing?.totalSessionSeconds ?: 0) + event.value.toInt()
                )
            }
            else -> existing
        } ?: return

        if (existingIndex >= 0) {
            summaries[existingIndex] = updated
        } else {
            summaries.add(updated)
        }

        saveDailySummaries(summaries)
    }

    private fun createEmptySummary(date: String): DailyBehaviorSummary {
        return DailyBehaviorSummary(
            date = date,
            userId = getCurrentUserId(),
            habitsCompleted = 0,
            habitsMissed = 0,
            completionRate = 0f,
            firstCheckInHour = null,
            lastCheckInHour = null,
            aiMessagesCount = 0,
            attitudeIndicator = 0f,
            sessionCount = 0,
            totalSessionSeconds = 0
        )
    }

    private fun createDefaultProfile(): UserBehaviorProfile {
        return UserBehaviorProfile(
            userId = getCurrentUserId(),
            lastUpdated = Clock.System.now().toString()
        )
    }

    private fun getCurrentUserId(): String {
        return firebaseService.getCurrentUserId() ?: "unknown"
    }

    private fun generateEventId(): String {
        return "event_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
    }

    private fun extractHour(timestamp: String): Int {
        return try {
            // Try parsing as ISO instant
            val instant = Instant.parse(timestamp)
            val localTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localTime.hour
        } catch (e: Exception) {
            try {
                // Try parsing as local time string like "HH:mm"
                timestamp.split(":").firstOrNull()?.toIntOrNull() ?: 12
            } catch (e2: Exception) {
                12  // Default to noon
            }
        }
    }

    private fun calculateTodayCompletionRate(completed: Int, missed: Int): Float {
        val total = completed + missed
        return if (total > 0) completed.toFloat() / total else 0f
    }

    private fun calculateEngagementLevel(attitudeScore: Float): EngagementLevel {
        return when {
            attitudeScore >= 0.5f -> EngagementLevel.HIGH
            attitudeScore >= 0f -> EngagementLevel.MODERATE
            attitudeScore >= -0.5f -> EngagementLevel.LOW
            else -> EngagementLevel.DISENGAGED
        }
    }

    private fun calculateStreakRecoveryRateInternal(profile: UserBehaviorProfile): Float {
        val totalMissed = profile.totalMissedDays
        val totalCompletions = profile.totalCompletions

        if (totalMissed == 0) return 0.5f

        // Estimate recoveries based on completions after misses
        val estimatedRecoveries = (totalCompletions * 0.3f).toInt()  // Rough estimate
        return (estimatedRecoveries.toFloat() / totalMissed).coerceIn(0f, 1f)
    }
}
