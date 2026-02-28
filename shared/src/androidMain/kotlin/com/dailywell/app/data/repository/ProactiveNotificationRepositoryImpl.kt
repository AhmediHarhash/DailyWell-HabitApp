package com.dailywell.app.data.repository

import com.dailywell.app.api.ClaudeApiClient
import com.dailywell.app.api.CoachPersonality
import com.dailywell.app.api.UserContext
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ProactiveNotificationRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val claudeApiClient: ClaudeApiClient,
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository,
    private val settingsRepository: SettingsRepository
) : ProactiveNotificationRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val KEY_PREFERENCES = "proactive_notification_prefs"
        private const val KEY_HISTORY = "proactive_notification_history"
        private const val KEY_DAILY_STATE = "proactive_notification_daily_state"
        private const val KEY_WEEKLY_STATE = "proactive_notification_weekly_state"
        private const val KEY_SMART_TIMING = "proactive_notification_smart_timing"

        // Milestones to celebrate
        val MILESTONES = listOf(7, 14, 21, 30, 60, 90, 100, 180, 365)
    }

    // ============================================================
    // PREFERENCES
    // ============================================================

    override fun getPreferences(): Flow<ProactiveNotificationPreferences> {
        return dataStoreManager.getString(KEY_PREFERENCES).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<ProactiveNotificationPreferences>(it)
                } catch (e: Exception) {
                    ProactiveNotificationPreferences()
                }
            } ?: ProactiveNotificationPreferences()
        }
    }

    override suspend fun updatePreferences(preferences: ProactiveNotificationPreferences) {
        dataStoreManager.putString(KEY_PREFERENCES, json.encodeToString(preferences))
    }

    override suspend fun toggleNotificationType(type: ProactiveNotificationType, enabled: Boolean) {
        val current = getPreferences().first()
        val updatedTypes = current.enabledTypes.toMutableMap().apply {
            this[type] = enabled
        }
        updatePreferences(current.copy(enabledTypes = updatedTypes))
    }

    override suspend fun setQuietHours(start: Int, end: Int) {
        val current = getPreferences().first()
        updatePreferences(current.copy(dndStart = start, dndEnd = end))
    }

    // ============================================================
    // NOTIFICATION GENERATION
    // ============================================================

    override suspend fun generateNotificationContent(
        type: ProactiveNotificationType,
        userContext: NotificationUserContext
    ): ProactiveNotification {
        val preferences = getPreferences().first()

        // Try AI generation first, fall back to template
        var message = try {
            generateAIMessage(type, userContext, preferences)
        } catch (e: Exception) {
            generateTemplateMessage(type, userContext)
        }

        // Guardrail: sanitize banned phrases from AI-generated or template messages
        message = NotificationGuardrails.sanitize(message)

        val title = ProactiveNotificationTemplates.getTitle(type, userContext.coachPersona)

        return ProactiveNotification(
            id = UUID.randomUUID().toString(),
            type = type,
            title = title,
            message = message,
            aiGenerated = true,
            scheduledAt = System.currentTimeMillis(),
            priority = getNotificationPriority(type, userContext),
            deepLink = getDeepLink(type),
            metadata = buildMetadata(type, userContext)
        )
    }

    private suspend fun generateAIMessage(
        type: ProactiveNotificationType,
        context: NotificationUserContext,
        preferences: ProactiveNotificationPreferences
    ): String {
        // Check if user has exceeded their monthly AI cost cap — use template fallback if so
        val settings = settingsRepository.getSettingsSnapshot()
        if (!settings.isPremium) {
            // Free users: always use templates for notifications to conserve API budget
            return generateTemplateMessage(type, context)
        }

        val prompt = buildAIPrompt(type, context, preferences)

        val coachPersonality = CoachPersonality(
            name = context.coachPersona,
            style = preferences.tone.displayName,
            description = "You are sending a push notification to the user"
        )

        val apiContext = UserContext(
            currentStreak = context.currentStreak,
            totalHabits = context.totalHabits,
            todayCompleted = context.todayCompleted,
            strongestHabit = context.strongestHabit,
            weakestHabit = context.weakestHabit,
            timeOfDay = when {
                context.currentHour < 12 -> "morning"
                context.currentHour < 17 -> "afternoon"
                else -> "evening"
            }
        )

        val result = claudeApiClient.getCoachingResponse(prompt, coachPersonality, apiContext)

        return result.getOrNull()?.take(200) ?: generateTemplateMessage(type, context)
    }

    private fun buildAIPrompt(
        type: ProactiveNotificationType,
        context: NotificationUserContext,
        preferences: ProactiveNotificationPreferences
    ): String {
        val basePrompt = """
            Generate a short, engaging push notification message (max 150 characters).
            Tone: ${preferences.tone.displayName} - ${preferences.tone.description}

            Context:
            - Notification type: ${type.displayName}
            - User's streak: ${context.currentStreak} days
            - Today's progress: ${context.todayCompleted}/${context.totalHabits} habits
            - Habits remaining: ${context.habitsRemaining}
            - Time: ${if (context.currentHour < 12) "Morning" else if (context.currentHour < 17) "Afternoon" else "Evening"}
        """.trimIndent()

        val typeSpecificPrompt = when (type) {
            ProactiveNotificationType.STREAK_AT_RISK -> """
                $basePrompt
                URGENT: User's streak is at risk! Create urgency without causing anxiety.
                They have ${context.habitsRemaining} habits left to complete today.
            """.trimIndent()

            ProactiveNotificationType.COMEBACK_NUDGE -> """
                $basePrompt
                User has been away for ${context.missedDays} days.
                Be welcoming, not guilt-tripping. Emphasize fresh starts.
            """.trimIndent()

            ProactiveNotificationType.MILESTONE_APPROACHING -> """
                $basePrompt
                User is close to a milestone! Build excitement.
                Current streak: ${context.currentStreak} days.
            """.trimIndent()

            ProactiveNotificationType.MORNING_MOTIVATION -> """
                $basePrompt
                Start their day positively. Reference their streak if > 0.
                Be energizing but not overwhelming.
            """.trimIndent()

            ProactiveNotificationType.COACH_OUTREACH -> """
                $basePrompt
                This is proactive outreach from their AI coach.
                Make it feel personal, like you're thinking about them.
                Reference something specific about their habits.
            """.trimIndent()

            else -> basePrompt
        }

        val guardrailPrompt = """

            RULES (MUST follow):
            - Max 150 characters
            - NEVER use guilt language: "don't forget", "remember to", "you haven't", "you need to", "hurry", "last chance", "falling behind"
            - Frame everything as opportunity, not obligation
            - Be warm and empowering, never nagging
            - One clear action, not multiple asks

            Respond with ONLY the notification message, no quotes or explanation.
        """.trimIndent()

        return "$typeSpecificPrompt\n\n$guardrailPrompt"
    }

    private fun generateTemplateMessage(
        type: ProactiveNotificationType,
        context: NotificationUserContext
    ): String {
        var template = ProactiveNotificationTemplates.getTemplate(type)

        // Replace placeholders
        template = template
            .replace("{streak}", context.currentStreak.toString())
            .replace("{coach_name}", context.coachPersona)
            .replace("{completed}", context.todayCompleted.toString())
            .replace("{total}", context.totalHabits.toString())
            .replace("{remaining}", context.habitsRemaining.toString())
            .replace("{habits_remaining}", context.habitsRemaining.toString())
            .replace("{weakest_habit}", context.weakestHabit ?: "your habits")
            .replace("{days}", context.missedDays.toString())
            .replace("{hours_left}", calculateHoursUntilMidnight().toString())

        // Milestone specific
        if (type == ProactiveNotificationType.MILESTONE_APPROACHING) {
            val nextMilestone = getNextMilestone(context.currentStreak)
            val daysRemaining = nextMilestone - context.currentStreak
            template = template
                .replace("{milestone}", nextMilestone.toString())
                .replace("{days_remaining}", daysRemaining.toString())
        }

        return template
    }

    private fun getNotificationPriority(
        type: ProactiveNotificationType,
        context: NotificationUserContext
    ): NotificationPriority {
        return when (type) {
            ProactiveNotificationType.STREAK_AT_RISK -> {
                val hoursLeft = calculateHoursUntilMidnight()
                when {
                    hoursLeft < 1 -> NotificationPriority.URGENT
                    hoursLeft < 3 -> NotificationPriority.HIGH
                    else -> NotificationPriority.MEDIUM
                }
            }
            ProactiveNotificationType.ACHIEVEMENT_UNLOCKED -> NotificationPriority.HIGH
            ProactiveNotificationType.SOCIAL_ACTIVITY -> NotificationPriority.LOW
            ProactiveNotificationType.WEEKLY_SUMMARY -> NotificationPriority.LOW
            else -> NotificationPriority.MEDIUM
        }
    }

    /**
     * Deep links per behavioral category:
     * - Celebration → Achievements/XP page
     * - Curiosity Hook → Insights card
     * - Streak Shield → Today checklist
     * - Social Whisper → Coaching chat or social
     */
    private fun getDeepLink(type: ProactiveNotificationType): String {
        return when (NotificationBehaviorCategory.fromType(type)) {
            NotificationBehaviorCategory.CELEBRATION -> "dailywell://achievements"
            NotificationBehaviorCategory.CURIOSITY_HOOK -> when (type) {
                ProactiveNotificationType.WEEKLY_SUMMARY -> "dailywell://insights"
                ProactiveNotificationType.AI_INSIGHT -> "dailywell://insights"
                else -> "dailywell://today"
            }
            NotificationBehaviorCategory.STREAK_SHIELD -> "dailywell://today"
            NotificationBehaviorCategory.SOCIAL_WHISPER -> when (type) {
                ProactiveNotificationType.COACH_OUTREACH -> "dailywell://coaching"
                else -> "dailywell://social"
            }
            NotificationBehaviorCategory.SILENT_DAY -> "dailywell://today"
        }
    }

    private fun buildMetadata(
        type: ProactiveNotificationType,
        context: NotificationUserContext
    ): NotificationMetadata {
        return NotificationMetadata(
            currentStreak = context.currentStreak,
            missedDays = if (context.missedDays > 0) context.missedDays else null,
            milestoneDays = if (type == ProactiveNotificationType.MILESTONE_APPROACHING) {
                getNextMilestone(context.currentStreak)
            } else null,
            coachPersona = context.coachPersona,
            completionRate = if (context.totalHabits > 0) {
                context.todayCompleted.toFloat() / context.totalHabits
            } else 0f
        )
    }

    // ============================================================
    // BEHAVIORAL NOTIFICATION ENGINE
    // ============================================================

    /**
     * Core gate: Should we send this notification?
     *
     * Decision cascade:
     * 1. Basic checks (enabled, quiet hours)
     * 2. Weekly cap (4/week, with at-risk escalation)
     * 3. Daily cap (2/day hard ceiling)
     * 4. Type rotation (never same category two consecutive days)
     * 5. Time gap (2 hour minimum)
     * 6. Value Score gate (must score 65+)
     */
    override suspend fun shouldSendNotification(type: ProactiveNotificationType): Boolean {
        val preferences = getPreferences().first()

        // 1. Basic checks
        if (!preferences.enabled) return false
        if (preferences.enabledTypes[type] != true) return false
        if (isInQuietHours(preferences)) return false

        // 2. Weekly frequency cap (primary limiter)
        val weeklyState = getWeeklyNotificationState()
        val isAtRisk = type == ProactiveNotificationType.STREAK_AT_RISK
        if (!isAtRisk && weeklyState.notificationsSent >= preferences.maxNotificationsPerWeek) return false

        // 3. Daily hard ceiling (safety net even for at-risk)
        val dailyState = getTodayNotificationState()
        if (dailyState.notificationsSent >= preferences.maxNotificationsPerDay) return false

        // 4. Time gap between notifications
        val lastNotificationTime = dailyState.lastNotificationAt
        if (lastNotificationTime != null) {
            val minutesSince = (System.currentTimeMillis() - lastNotificationTime) / 60000
            if (minutesSince < preferences.minMinutesBetweenNotifications) return false
        }

        // 5. Type rotation — never same behavioral category two consecutive days
        val todayCategory = NotificationBehaviorCategory.fromType(type)
        val lastSentDate = weeklyState.lastSentDate
        val lastSentType = weeklyState.lastSentType
        if (lastSentDate != null && lastSentType != null) {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
            val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE)
            if (lastSentDate == today || lastSentDate == yesterday) {
                val lastCategory = NotificationBehaviorCategory.fromType(lastSentType)
                if (todayCategory == lastCategory && !isAtRisk) return false
            }
        }

        // 6. Value Score gate — the behavioral core
        val valueScore = calculateValueScore(type)
        return valueScore.passes
    }

    /**
     * Calculate the notification value score (0-100).
     *
     * Components:
     * - Risk (0-30):    Streak danger level
     * - Readiness (0-25): User likely to act right now
     * - Novelty (0-20):  Hasn't seen this type recently
     * - Impact (0-15):   Drives meaningful behavior change
     * - Trust (0-10):    User's relationship health with notifications
     */
    override suspend fun calculateValueScore(type: ProactiveNotificationType): NotificationValueScore {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val todayEntries = entryRepository.getEntriesForDate(today).first()
        val enabledHabits = habitRepository.getEnabledHabits().first()
        val streakInfo = entryRepository.getStreakInfo().first()
        val history = getNotificationHistoryList()
        val weeklyState = getWeeklyNotificationState()
        val currentHour = LocalTime.now().hour

        val completed = todayEntries.count { it.completed }
        val remaining = enabledHabits.size - completed
        val hoursLeft = calculateHoursUntilMidnight()

        // ── RISK (0-30) ──
        val risk = when {
            type == ProactiveNotificationType.STREAK_AT_RISK && streakInfo.currentStreak > 0 -> when {
                hoursLeft < 1 && remaining > 0 -> 30  // Critical
                hoursLeft < 3 && remaining > 0 -> 25
                hoursLeft < 6 && remaining > 0 -> 15
                else -> 5
            }
            type == ProactiveNotificationType.COMEBACK_NUDGE -> {
                val lastEntry = entryRepository.getLastEntryDate()
                val daysSince = lastEntry?.let {
                    java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDate.parse(it), LocalDate.now()
                    ).toInt()
                } ?: 0
                when {
                    daysSince >= 7 -> 25   // Week+ gone, high risk of churn
                    daysSince >= 3 -> 20
                    daysSince >= 2 -> 10
                    else -> 0
                }
            }
            else -> 0
        }

        // ── READINESS (0-25) ──
        // Is the user likely to engage right now?
        val smartTiming = getSmartTiming().first()
        val isOptimalHour = smartTiming?.mostResponsiveHours?.contains(currentHour) == true
        val readiness = when {
            // User historically engages at this hour
            isOptimalHour -> 20
            // Good general times (morning 7-9, lunch 12-13, evening 18-20)
            currentHour in 7..9 || currentHour in 12..13 || currentHour in 18..20 -> 15
            // Decent times
            currentHour in 10..11 || currentHour in 14..17 -> 10
            // Off hours
            else -> 3
        } + if (completed > 0 && remaining > 0) 5 else 0 // Active today = more ready

        // ── NOVELTY (0-20) ──
        // Has user seen this behavioral category recently?
        val category = NotificationBehaviorCategory.fromType(type)
        val recentTypes = weeklyState.typesSentThisWeek
        val sameCategory = recentTypes.count { NotificationBehaviorCategory.fromType(it) == category }
        val novelty = when {
            sameCategory == 0 -> 20  // Fresh category
            sameCategory == 1 -> 12  // Seen once
            sameCategory == 2 -> 5   // Getting stale
            else -> 0                // Over-used
        }

        // ── IMPACT (0-15) ──
        // Will this drive meaningful behavior?
        val impact = when (category) {
            NotificationBehaviorCategory.STREAK_SHIELD -> 15  // Directly prevents loss
            NotificationBehaviorCategory.CELEBRATION -> 12    // Reinforces positive behavior
            NotificationBehaviorCategory.CURIOSITY_HOOK -> 10 // Drives engagement
            NotificationBehaviorCategory.SOCIAL_WHISPER -> 8  // Social motivation
            NotificationBehaviorCategory.SILENT_DAY -> 0      // Never sent
        }

        // ── TRUST (0-10) ──
        // User's relationship health with our notifications
        val recentHistory = history.filter {
            it.sentAt > System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L) // Last 14 days
        }
        val openRate = if (recentHistory.isNotEmpty()) {
            recentHistory.count { it.opened }.toFloat() / recentHistory.size
        } else 0.5f // Default trust for new users
        val trust = when {
            openRate >= 0.6f -> 10   // High engagement
            openRate >= 0.4f -> 7    // Good engagement
            openRate >= 0.2f -> 4    // Low engagement — be more selective
            recentHistory.isEmpty() -> 7  // New user — give benefit of doubt
            else -> 1                // User ignoring us — barely send anything
        }

        val reason = buildString {
            append("risk=$risk(streak=${streakInfo.currentStreak},h=${hoursLeft}) ")
            append("ready=$readiness(h=$currentHour,opt=$isOptimalHour) ")
            append("novel=$novelty(cat=$category,seen=$sameCategory) ")
            append("impact=$impact trust=$trust(rate=${(openRate*100).toInt()}%)")
        }

        return NotificationValueScore(
            risk = risk.coerceIn(0, 30),
            readiness = readiness.coerceIn(0, 25),
            novelty = novelty.coerceIn(0, 20),
            impact = impact.coerceIn(0, 15),
            trust = trust.coerceIn(0, 10),
            reason = reason
        )
    }

    // ============================================================
    // WEEKLY STATE TRACKING
    // ============================================================

    override suspend fun getWeeklyNotificationState(): WeeklyNotificationState {
        val today = LocalDate.now()
        // Calculate Monday of current week
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekStart = monday.format(DateTimeFormatter.ISO_DATE)

        return dataStoreManager.getString(KEY_WEEKLY_STATE).first()?.let {
            try {
                val state = json.decodeFromString<WeeklyNotificationState>(it)
                if (state.weekStart == weekStart) state else WeeklyNotificationState(weekStart = weekStart)
            } catch (e: Exception) {
                WeeklyNotificationState(weekStart = weekStart)
            }
        } ?: WeeklyNotificationState(weekStart = weekStart)
    }

    private suspend fun updateWeeklyState(notification: ProactiveNotification) {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekStart = monday.format(DateTimeFormatter.ISO_DATE)
        val todayStr = today.format(DateTimeFormatter.ISO_DATE)

        val current = getWeeklyNotificationState()
        val updated = if (current.weekStart == weekStart) {
            current.copy(
                notificationsSent = current.notificationsSent + 1,
                typesSentThisWeek = current.typesSentThisWeek + notification.type,
                lastSentType = notification.type,
                lastSentDate = todayStr
            )
        } else {
            WeeklyNotificationState(
                weekStart = weekStart,
                notificationsSent = 1,
                typesSentThisWeek = listOf(notification.type),
                lastSentType = notification.type,
                lastSentDate = todayStr
            )
        }
        dataStoreManager.putString(KEY_WEEKLY_STATE, json.encodeToString(updated))
    }

    private fun isInQuietHours(preferences: ProactiveNotificationPreferences): Boolean {
        val now = LocalTime.now()
        val currentHour = now.hour
        val isWeekend = LocalDate.now().dayOfWeek.value >= 6

        val dndStart = if (isWeekend) preferences.weekendDndStart else preferences.dndStart
        val dndEnd = if (isWeekend) preferences.weekendDndEnd else preferences.dndEnd

        return if (dndStart < dndEnd) {
            // Same day DND (e.g., 1am to 7am)
            currentHour >= dndStart && currentHour < dndEnd
        } else {
            // Overnight DND (e.g., 10pm to 7am)
            currentHour >= dndStart || currentHour < dndEnd
        }
    }

    override suspend fun getNextScheduledNotification(): ProactiveNotification? {
        // Determine what notification should be sent next based on triggers and value score
        val triggers = checkAllTriggers()
        if (triggers.isEmpty()) return null

        // Score each trigger and pick the highest value one
        val scoredTriggers = triggers.map { trigger ->
            val score = calculateValueScore(trigger.type)
            trigger to score
        }.filter { it.second.passes } // Only consider triggers that pass the 65+ gate
            .sortedByDescending { it.second.total }

        val topTrigger = scoredTriggers.firstOrNull()?.first ?: return null

        val context = buildUserContext()
        return generateNotificationContent(topTrigger.type, context)
    }

    // ============================================================
    // NOTIFICATION HISTORY
    // ============================================================

    override suspend fun recordNotificationSent(notification: ProactiveNotification) {
        // Update daily state
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val currentState = getTodayNotificationState()

        val updatedState = if (currentState.date == today) {
            currentState.copy(
                notificationsSent = currentState.notificationsSent + 1,
                lastNotificationAt = System.currentTimeMillis(),
                typesSentToday = currentState.typesSentToday + notification.type
            )
        } else {
            DailyNotificationState(
                date = today,
                notificationsSent = 1,
                lastNotificationAt = System.currentTimeMillis(),
                typesSentToday = listOf(notification.type)
            )
        }
        dataStoreManager.putString(KEY_DAILY_STATE, json.encodeToString(updatedState))

        // Update weekly state (behavioral engine's primary tracker)
        updateWeeklyState(notification)

        // Add to history
        val history = getNotificationHistoryList()
        val newEntry = NotificationHistory(
            notificationId = notification.id,
            type = notification.type,
            sentAt = System.currentTimeMillis()
        )
        val updatedHistory = (listOf(newEntry) + history).take(100)  // Keep last 100
        dataStoreManager.putString(KEY_HISTORY, json.encodeToString(updatedHistory))
    }

    override suspend fun recordNotificationOpened(notificationId: String) {
        val history = getNotificationHistoryList().toMutableList()
        val index = history.indexOfFirst { it.notificationId == notificationId }
        if (index >= 0) {
            history[index] = history[index].copy(
                opened = true,
                openedAt = System.currentTimeMillis()
            )
            dataStoreManager.putString(KEY_HISTORY, json.encodeToString(history))
        }

        // Update smart timing based on when user opens notifications
        analyzeAndUpdateOptimalTiming()
    }

    override suspend fun recordNotificationDismissed(notificationId: String) {
        val history = getNotificationHistoryList().toMutableList()
        val index = history.indexOfFirst { it.notificationId == notificationId }
        if (index >= 0) {
            history[index] = history[index].copy(dismissed = true)
            dataStoreManager.putString(KEY_HISTORY, json.encodeToString(history))
        }
    }

    override fun getNotificationHistory(days: Int): Flow<List<NotificationHistory>> {
        return dataStoreManager.getString(KEY_HISTORY).map { jsonString ->
            val allHistory = jsonString?.let {
                try {
                    json.decodeFromString<List<NotificationHistory>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()

            val cutoff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
            allHistory.filter { it.sentAt >= cutoff }
        }
    }

    private suspend fun getNotificationHistoryList(): List<NotificationHistory> {
        return dataStoreManager.getString(KEY_HISTORY).first()?.let {
            try {
                json.decodeFromString<List<NotificationHistory>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    override suspend fun getTodayNotificationState(): DailyNotificationState {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return dataStoreManager.getString(KEY_DAILY_STATE).first()?.let {
            try {
                val state = json.decodeFromString<DailyNotificationState>(it)
                if (state.date == today) state else DailyNotificationState(date = today)
            } catch (e: Exception) {
                DailyNotificationState(date = today)
            }
        } ?: DailyNotificationState(date = today)
    }

    // ============================================================
    // SMART TIMING
    // ============================================================

    override fun getSmartTiming(): Flow<SmartNotificationTiming?> {
        return dataStoreManager.getString(KEY_SMART_TIMING).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<SmartNotificationTiming>(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun updateSmartTiming(timing: SmartNotificationTiming) {
        dataStoreManager.putString(KEY_SMART_TIMING, json.encodeToString(timing))
    }

    override suspend fun analyzeAndUpdateOptimalTiming() {
        val history = getNotificationHistoryList().filter { it.opened }
        if (history.size < 10) return  // Need enough data

        // Calculate which hours have best open rates
        val hourlyOpenCounts = mutableMapOf<Int, Int>()
        history.forEach { notification ->
            notification.openedAt?.let { openTime ->
                val hour = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(openTime),
                    java.time.ZoneId.systemDefault()
                ).hour
                hourlyOpenCounts[hour] = (hourlyOpenCounts[hour] ?: 0) + 1
            }
        }

        val sortedHours = hourlyOpenCounts.entries.sortedByDescending { it.value }.map { it.key }
        val optimalMorning = sortedHours.find { it in 6..11 } ?: 8
        val optimalMidday = sortedHours.find { it in 11..15 } ?: 13
        val optimalEvening = sortedHours.find { it in 17..22 } ?: 19

        val currentTiming = getSmartTiming().first() ?: SmartNotificationTiming(userId = "user")
        updateSmartTiming(
            currentTiming.copy(
                optimalMorningHour = optimalMorning,
                optimalMiddayHour = optimalMidday,
                optimalEveningHour = optimalEvening,
                mostResponsiveHours = sortedHours.take(7),
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    // ============================================================
    // TRIGGER CHECKS
    // ============================================================

    override suspend fun checkStreakAtRisk(): StreakRiskStatus? {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val todayEntries = entryRepository.getEntriesForDate(today).first()
        val enabledHabits = habitRepository.getEnabledHabits().first()

        val completed = todayEntries.count { it.completed }
        val remaining = enabledHabits.size - completed

        if (remaining <= 0) return null  // All done!

        val streakInfo = entryRepository.getStreakInfo().first()
        if (streakInfo.currentStreak == 0) return null  // No streak to protect

        val hoursLeft = calculateHoursUntilMidnight()

        // Only alert if there's a streak worth protecting and time is running out
        if (hoursLeft > 6 || streakInfo.currentStreak < 2) return null

        val riskLevel = when {
            hoursLeft < 1 -> RiskLevel.CRITICAL
            hoursLeft < 3 -> RiskLevel.HIGH
            hoursLeft < 6 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        return StreakRiskStatus(
            currentStreak = streakInfo.currentStreak,
            habitsRemaining = remaining,
            hoursUntilMidnight = hoursLeft,
            riskLevel = riskLevel
        )
    }

    override suspend fun checkComebackNeeded(): ComebackStatus? {
        val lastEntry = entryRepository.getLastEntryDate()
        if (lastEntry == null) return null

        val daysSince = java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.parse(lastEntry),
            LocalDate.now()
        ).toInt()

        // Trigger comeback after 2+ days
        if (daysSince < 2) return null

        val streakInfo = entryRepository.getStreakInfo().first()

        return ComebackStatus(
            daysSinceLastCheckIn = daysSince,
            previousStreak = streakInfo.currentStreak,
            previousBestStreak = streakInfo.longestStreak
        )
    }

    override suspend fun checkMilestoneApproaching(): MilestoneStatus? {
        val streakInfo = entryRepository.getStreakInfo().first()
        val currentStreak = streakInfo.currentStreak

        val nextMilestone = MILESTONES.find { it > currentStreak } ?: return null
        val daysRemaining = nextMilestone - currentStreak

        // Only notify when 1-3 days away
        if (daysRemaining > 3 || daysRemaining < 1) return null

        return MilestoneStatus(
            currentStreak = currentStreak,
            nextMilestone = nextMilestone,
            daysRemaining = daysRemaining
        )
    }

    override suspend fun checkAllTriggers(): List<NotificationTrigger> {
        val triggers = mutableListOf<NotificationTrigger>()
        val currentHour = LocalTime.now().hour
        val preferences = getPreferences().first()

        // Check streak at risk (highest priority)
        checkStreakAtRisk()?.let { risk ->
            if (shouldSendNotification(ProactiveNotificationType.STREAK_AT_RISK)) {
                triggers.add(
                    NotificationTrigger(
                        type = ProactiveNotificationType.STREAK_AT_RISK,
                        priority = if (risk.riskLevel == RiskLevel.CRITICAL) NotificationPriority.URGENT else NotificationPriority.HIGH,
                        reason = "Streak of ${risk.currentStreak} days at risk with ${risk.habitsRemaining} habits remaining",
                        metadata = NotificationMetadata(
                            currentStreak = risk.currentStreak
                        )
                    )
                )
            }
        }

        // Check comeback needed
        checkComebackNeeded()?.let { comeback ->
            if (shouldSendNotification(ProactiveNotificationType.COMEBACK_NUDGE)) {
                triggers.add(
                    NotificationTrigger(
                        type = ProactiveNotificationType.COMEBACK_NUDGE,
                        priority = NotificationPriority.MEDIUM,
                        reason = "User inactive for ${comeback.daysSinceLastCheckIn} days",
                        metadata = NotificationMetadata(
                            missedDays = comeback.daysSinceLastCheckIn
                        )
                    )
                )
            }
        }

        // Check milestone approaching
        checkMilestoneApproaching()?.let { milestone ->
            if (shouldSendNotification(ProactiveNotificationType.MILESTONE_APPROACHING)) {
                triggers.add(
                    NotificationTrigger(
                        type = ProactiveNotificationType.MILESTONE_APPROACHING,
                        priority = NotificationPriority.MEDIUM,
                        reason = "${milestone.daysRemaining} days until ${milestone.nextMilestone}-day milestone",
                        metadata = NotificationMetadata(
                            currentStreak = milestone.currentStreak,
                            milestoneDays = milestone.nextMilestone
                        )
                    )
                )
            }
        }

        // Time-based notifications
        val smartTiming = getSmartTiming().first()

        // Morning motivation
        val morningHour = smartTiming?.optimalMorningHour ?: preferences.morningWindowStart
        if (currentHour in (morningHour - 1)..(morningHour + 1)) {
            if (shouldSendNotification(ProactiveNotificationType.MORNING_MOTIVATION)) {
                triggers.add(
                    NotificationTrigger(
                        type = ProactiveNotificationType.MORNING_MOTIVATION,
                        priority = NotificationPriority.MEDIUM,
                        reason = "Morning motivation time",
                        metadata = NotificationMetadata()
                    )
                )
            }
        }

        // Midday check-in
        val middayHour = smartTiming?.optimalMiddayHour ?: 13
        if (currentHour in (middayHour - 1)..(middayHour + 1)) {
            if (shouldSendNotification(ProactiveNotificationType.MIDDAY_CHECKIN)) {
                triggers.add(
                    NotificationTrigger(
                        type = ProactiveNotificationType.MIDDAY_CHECKIN,
                        priority = NotificationPriority.MEDIUM,
                        reason = "Midday check-in time",
                        metadata = NotificationMetadata()
                    )
                )
            }
        }

        // Evening reminder
        val eveningHour = smartTiming?.optimalEveningHour ?: preferences.eveningWindowStart
        if (currentHour in (eveningHour - 1)..(eveningHour + 1)) {
            if (shouldSendNotification(ProactiveNotificationType.EVENING_REMINDER)) {
                triggers.add(
                    NotificationTrigger(
                        type = ProactiveNotificationType.EVENING_REMINDER,
                        priority = NotificationPriority.MEDIUM,
                        reason = "Evening reminder time",
                        metadata = NotificationMetadata()
                    )
                )
            }
        }

        return triggers
    }

    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    private suspend fun buildUserContext(): NotificationUserContext {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val todayEntries = entryRepository.getEntriesForDate(today).first()
        val enabledHabits = habitRepository.getEnabledHabits().first()
        val streakInfo = entryRepository.getStreakInfo().first()
        val preferences = getPreferences().first()
        val settings = settingsRepository.getSettings().first()

        val completed = todayEntries.count { it.completed }
        val lastEntry = entryRepository.getLastEntryDate()
        val missedDays = lastEntry?.let {
            java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.parse(it),
                LocalDate.now()
            ).toInt()
        } ?: 0

        val now = LocalDateTime.now()

        return NotificationUserContext(
            userName = settings.userName,
            currentStreak = streakInfo.currentStreak,
            longestStreak = streakInfo.longestStreak,
            todayCompleted = completed,
            totalHabits = enabledHabits.size,
            habitsRemaining = enabledHabits.size - completed,
            weakestHabit = calculateWeakestHabit(),
            strongestHabit = calculateStrongestHabit(),
            missedDays = missedDays,
            lastCheckInAt = lastEntry?.let { LocalDate.parse(it).atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 },
            coachPersona = settings.selectedCoachPersona ?: "Sam",
            preferredTone = preferences.tone,
            currentHour = now.hour,
            dayOfWeek = now.dayOfWeek.value,
            isWeekend = now.dayOfWeek.value >= 6
        )
    }

    private suspend fun calculateWeakestHabit(): String? {
        // Get last 14 days of data
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(14)
        val entries = entryRepository.getEntriesInRange(
            startDate.format(DateTimeFormatter.ISO_DATE),
            endDate.format(DateTimeFormatter.ISO_DATE)
        ).first()

        val habits = habitRepository.getEnabledHabits().first()

        val completionRates = habits.map { habit ->
            val habitEntries = entries.filter { it.habitId == habit.id }
            val completed = habitEntries.count { it.completed }
            habit.name to (completed.toFloat() / 14)
        }

        return completionRates.minByOrNull { it.second }?.first
    }

    private suspend fun calculateStrongestHabit(): String? {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(14)
        val entries = entryRepository.getEntriesInRange(
            startDate.format(DateTimeFormatter.ISO_DATE),
            endDate.format(DateTimeFormatter.ISO_DATE)
        ).first()

        val habits = habitRepository.getEnabledHabits().first()

        val completionRates = habits.map { habit ->
            val habitEntries = entries.filter { it.habitId == habit.id }
            val completed = habitEntries.count { it.completed }
            habit.name to (completed.toFloat() / 14)
        }

        return completionRates.maxByOrNull { it.second }?.first
    }

    private fun calculateHoursUntilMidnight(): Int {
        val now = LocalTime.now()
        val midnight = LocalTime.MIDNIGHT
        val minutesUntilMidnight = (24 * 60) - (now.hour * 60 + now.minute)
        return minutesUntilMidnight / 60
    }

    private fun getNextMilestone(currentStreak: Int): Int {
        return MILESTONES.find { it > currentStreak } ?: (currentStreak + 30)
    }
}
