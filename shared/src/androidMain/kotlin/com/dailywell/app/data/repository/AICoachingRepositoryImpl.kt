package com.dailywell.app.data.repository

import com.dailywell.app.api.ClaudeApiClient
import com.dailywell.app.api.CoachPersonality
import com.dailywell.app.api.UserContext
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Implementation of AICoachingRepository with REAL Claude API integration
 * Gets real user data from HabitRepository and EntryRepository
 */
class AICoachingRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val claudeApiClient: ClaudeApiClient,
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository
) : AICoachingRepository {

    private val _selectedCoach = MutableStateFlow(CoachPersonas.supportiveSam)
    private val _dailyInsight = MutableStateFlow<DailyCoachingInsight?>(null)
    private val _sessions = MutableStateFlow<List<AICoachingSession>>(emptyList())
    private val _actionItems = MutableStateFlow<List<CoachingActionItem>>(emptyList())
    private val _weeklySummary = MutableStateFlow<WeeklyCoachingSummary?>(null)

    private fun getTimeOfDay(): String {
        val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            else -> "evening"
        }
    }

    private suspend fun buildUserContext(): UserContext {
        // Get real data from repositories
        val streakInfo = entryRepository.getStreakInfo().first()
        val enabledHabits = habitRepository.getEnabledHabits().first()
        val todayEntry = entryRepository.getTodayEntry().first()

        val completedToday = todayEntry?.completedCount() ?: 0

        // Calculate strongest and weakest habits based on completion rates
        val habitRates = enabledHabits.map { habit ->
            habit.id to (entryRepository.getCompletionRateForHabit(habit.id, 14))
        }.sortedByDescending { it.second }

        val strongestHabit = habitRates.firstOrNull()?.first ?: "sleep"
        val weakestHabit = habitRates.lastOrNull()?.first ?: "water"

        return UserContext(
            currentStreak = streakInfo.currentStreak,
            totalHabits = enabledHabits.size,
            todayCompleted = completedToday,
            strongestHabit = strongestHabit,
            weakestHabit = weakestHabit,
            timeOfDay = getTimeOfDay()
        )
    }

    private fun buildCoachPersonality(): CoachPersonality {
        val coach = _selectedCoach.value
        return CoachPersonality(
            name = coach.name,
            style = coach.style.name.lowercase().replaceFirstChar { it.uppercase() },
            description = coach.description
        )
    }

    override fun getSelectedCoach(): Flow<CoachPersona> = _selectedCoach

    override suspend fun selectCoach(coachId: String) {
        val coach = CoachPersonas.allCoaches.find { it.id == coachId }
        if (coach != null) {
            _selectedCoach.value = coach
        }
    }

    override fun getAvailableCoaches(): List<CoachPersona> = CoachPersonas.allCoaches

    override fun getDailyInsight(): Flow<DailyCoachingInsight?> = _dailyInsight

    override suspend fun generateDailyInsight() {
        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()

        // Use real Claude API to generate insight
        val result = claudeApiClient.generateDailyInsight(userContext, coachPersonality)

        result.onSuccess { aiInsight ->
            val now = Clock.System.now()
            val dateStr = now.toString()

            _dailyInsight.value = DailyCoachingInsight(
                id = "insight_${System.currentTimeMillis()}",
                date = dateStr.substringBefore("T"),
                greeting = aiInsight.greeting,
                mainMessage = aiInsight.mainMessage,
                focusHabit = aiInsight.focusHabit,
                focusReason = aiInsight.focusReason,
                motivationalQuote = aiInsight.motivationalQuote,
                suggestedActions = listOf(
                    SuggestedAction(
                        id = "action_1",
                        emoji = "✅",
                        title = "Complete a Habit",
                        description = "Start with any habit right now!",
                        actionType = SuggestedActionType.COMPLETE_HABIT
                    ),
                    SuggestedAction(
                        id = "action_2",
                        emoji = "🎯",
                        title = "Set Intention",
                        description = "Plan your day for success",
                        actionType = SuggestedActionType.SET_INTENTION
                    )
                ),
                celebrationNote = aiInsight.celebrationNote
            )
        }.onFailure {
            // Fallback to template-based insight
            val now = Clock.System.now()
            val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val hour = localDateTime.hour
            val dateStr = now.toString()

            _dailyInsight.value = DailyCoachingInsight(
                id = "insight_${System.currentTimeMillis()}",
                date = dateStr.substringBefore("T"),
                greeting = CoachingTemplates.getDailyGreeting(hour, "there", userContext.currentStreak),
                mainMessage = CoachingTemplates.morningCheckins.random(),
                suggestedActions = emptyList()
            )
        }
    }

    override suspend fun markSuggestedActionDone(actionId: String) {
        val current = _dailyInsight.value ?: return
        _dailyInsight.value = current.copy(
            suggestedActions = current.suggestedActions.filter { it.id != actionId }
        )
    }

    override fun getActiveSessions(): Flow<List<AICoachingSession>> {
        return _sessions.map { sessions ->
            sessions.filter { it.status == SessionStatus.IN_PROGRESS }
        }
    }

    override fun getSessionHistory(): Flow<List<AICoachingSession>> {
        return _sessions.map { sessions ->
            sessions.filter { it.status != SessionStatus.IN_PROGRESS }
        }
    }

    override suspend fun startSession(type: CoachingSessionType): AICoachingSession {
        val now = Clock.System.now().toString()
        val coach = _selectedCoach.value

        // Get AI-generated opening message
        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()

        val openingPrompt = when (type) {
            CoachingSessionType.DAILY_CHECKIN -> "Start a brief daily check-in with the user. Ask how they're feeling today."
            CoachingSessionType.WEEKLY_REVIEW -> "Begin a weekly review. Summarize their progress and celebrate wins."
            CoachingSessionType.HABIT_COACHING -> "Start a coaching session about optimizing habits. Ask which habit they want to focus on."
            CoachingSessionType.MOTIVATION_BOOST -> "Give the user an energizing motivation boost. Be inspiring!"
            CoachingSessionType.OBSTACLE_SOLVING -> "Help the user identify and overcome obstacles. Ask what's been challenging."
            CoachingSessionType.CELEBRATION -> "Celebrate the user's achievements! Be enthusiastic and acknowledge their hard work."
            CoachingSessionType.RECOVERY_SUPPORT -> "Support the user in recovering from a missed streak. Be compassionate and helpful."
        }

        val openingMessage = claudeApiClient.getCoachingResponse(
            openingPrompt,
            coachPersonality,
            userContext
        ).getOrElse {
            // Fallback messages
            when (type) {
                CoachingSessionType.DAILY_CHECKIN -> "Hey! Ready for your daily check-in? How are you feeling today?"
                CoachingSessionType.WEEKLY_REVIEW -> "Time for our weekly review! Let's look at what you've accomplished."
                CoachingSessionType.HABIT_COACHING -> "Let's work on optimizing your habits! Which one would you like to focus on?"
                CoachingSessionType.MOTIVATION_BOOST -> CoachingTemplates.motivationBoosts.random()
                CoachingSessionType.OBSTACLE_SOLVING -> "I'm here to help! What's been getting in the way of your habits?"
                CoachingSessionType.CELEBRATION -> "🎉 Time to celebrate! You've been doing amazing work!"
                CoachingSessionType.RECOVERY_SUPPORT -> CoachingTemplates.recoveryMessages.random()
            }
        }

        val session = AICoachingSession(
            id = "session_${System.currentTimeMillis()}",
            type = type,
            title = type.displayName,
            description = "Session with ${coach.name}",
            messages = listOf(
                CoachingMessage(
                    id = "msg_${System.currentTimeMillis()}",
                    role = MessageRole.COACH,
                    content = openingMessage,
                    timestamp = now,
                    suggestions = getSuggestionsForType(type),
                    actionButtons = getActionButtonsForType(type)
                )
            ),
            startedAt = now
        )

        _sessions.value = listOf(session) + _sessions.value
        return session
    }

    private fun getSuggestionsForType(type: CoachingSessionType): List<String> {
        return when (type) {
            CoachingSessionType.DAILY_CHECKIN -> listOf(
                "Feeling great! 💪",
                "A bit tired today 😴",
                "Ready to crush it! 🔥",
                "Need some motivation 🌱"
            )
            CoachingSessionType.HABIT_COACHING -> listOf(
                "Sleep habits",
                "Hydration",
                "Movement",
                "Mindfulness"
            )
            CoachingSessionType.OBSTACLE_SOLVING -> listOf(
                "Too busy lately",
                "Forgetting to do them",
                "Lost motivation",
                "Something else"
            )
            else -> listOf("Let's go! 🚀", "Tell me more", "I have a question")
        }
    }

    private fun getActionButtonsForType(type: CoachingSessionType): List<CoachingAction> {
        return when (type) {
            CoachingSessionType.DAILY_CHECKIN -> listOf(
                CoachingAction("action_log", "Log Habit", "✅", CoachingActionType.LOG_HABIT),
                CoachingAction("action_view", "View Stats", "📊", CoachingActionType.VIEW_INSIGHTS)
            )
            CoachingSessionType.HABIT_COACHING -> listOf(
                CoachingAction("action_intention", "Set Intention", "🎯", CoachingActionType.SET_INTENTION),
                CoachingAction("action_adjust", "Adjust Goal", "⚙️", CoachingActionType.ADJUST_GOAL)
            )
            else -> emptyList()
        }
    }

    override suspend fun sendMessage(sessionId: String, message: String): CoachingMessage {
        val now = Clock.System.now().toString()

        // Add user message
        val userMessage = CoachingMessage(
            id = "msg_user_${System.currentTimeMillis()}",
            role = MessageRole.USER,
            content = message,
            timestamp = now
        )

        // Get REAL AI response from Claude
        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()

        val aiResponse = claudeApiClient.getCoachingResponse(
            message,
            coachPersonality,
            userContext
        ).getOrElse {
            // Fallback response
            generateFallbackResponse(message)
        }

        val coachResponse = CoachingMessage(
            id = "msg_coach_${System.currentTimeMillis()}",
            role = MessageRole.COACH,
            content = aiResponse,
            timestamp = now,
            suggestions = listOf("Thanks!", "Tell me more", "What should I focus on?")
        )

        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    messages = session.messages + userMessage + coachResponse
                )
            } else session
        }

        return coachResponse
    }

    private fun generateFallbackResponse(userMessage: String): String {
        val coach = _selectedCoach.value
        return when (coach.style) {
            CoachingStyle.ENCOURAGING -> {
                when {
                    userMessage.contains("tired", ignoreCase = true) ->
                        "I hear you! It's okay to feel tired. Remember, even small steps count. What's one tiny thing you could do today? 💚"
                    userMessage.contains("great", ignoreCase = true) ->
                        "That's wonderful to hear! 🌟 Your positive energy will fuel your habits today. Let's make it count!"
                    else ->
                        "Thanks for sharing! I'm here to support you. What would make today a win for you? ✨"
                }
            }
            CoachingStyle.DIRECT -> {
                when {
                    userMessage.contains("tired", ignoreCase = true) ->
                        "Noted. Tired days are part of the process. Pick your easiest habit and just get it done. Action creates energy."
                    userMessage.contains("great", ignoreCase = true) ->
                        "Good. Channel that energy into your habits now while you have it. What's first on your list?"
                    else ->
                        "Let's focus on action. What specific habit are you working on today?"
                }
            }
            else -> "Thanks for sharing! How can I help you with your habits today?"
        }
    }

    override suspend fun selectQuickReply(sessionId: String, reply: String): CoachingMessage {
        return sendMessage(sessionId, reply)
    }

    override suspend fun completeSession(sessionId: String) {
        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    status = SessionStatus.COMPLETED,
                    completedAt = Clock.System.now().toString()
                )
            } else session
        }
    }

    override suspend fun abandonSession(sessionId: String) {
        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    status = SessionStatus.ABANDONED,
                    completedAt = Clock.System.now().toString()
                )
            } else session
        }
    }

    override fun getActionItems(): Flow<List<CoachingActionItem>> = _actionItems

    override suspend fun completeActionItem(itemId: String) {
        _actionItems.value = _actionItems.value.map { item ->
            if (item.id == itemId) item.copy(isCompleted = true) else item
        }
    }

    override suspend fun dismissActionItem(itemId: String) {
        _actionItems.value = _actionItems.value.filter { it.id != itemId }
    }

    override fun getWeeklySummary(): Flow<WeeklyCoachingSummary?> = _weeklySummary

    override suspend fun generateWeeklySummary() {
        // Get real data from repositories
        val streakInfo = entryRepository.getStreakInfo().first()
        val enabledHabits = habitRepository.getEnabledHabits().first()
        val weekData = entryRepository.getWeekData(0).first()

        // Calculate completion rate from week data
        val totalPossible = weekData.days.sumOf { it.totalCount.toLong() }.toInt()
        val totalCompleted = weekData.days.sumOf { day -> day.completedCount.toLong() }.toInt()
        val completionRate = if (totalPossible > 0) totalCompleted.toFloat() / totalPossible else 0f

        // Calculate overall score (0-100)
        val overallScore = (completionRate * 100).toInt()

        // Get strongest/weakest habits
        val habitRates = enabledHabits.map { habit ->
            habit to entryRepository.getCompletionRateForHabit(habit.id, 7)
        }.sortedByDescending { it.second }

        val strongestHabit = habitRates.firstOrNull()
        val weakestHabit = habitRates.lastOrNull()

        // Calculate streak trend
        val previousStreakInfo = entryRepository.getStreakInfo().first()
        val streakTrend = when {
            streakInfo.currentStreak > previousStreakInfo.longestStreak -> TrendDirection.UP
            streakInfo.currentStreak < 3 -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }

        // Get dates
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val weekEndDate = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
        val weekStartInstant = Clock.System.now().minus(kotlin.time.Duration.parse("7d"))
        val weekStartDate = weekStartInstant.toLocalDateTime(TimeZone.currentSystemDefault())
            .let { "${it.year}-${it.monthNumber.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}" }

        // Use Claude API for personalized insights
        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()

        val aiSummary = claudeApiClient.getCoachingResponse(
            "Generate a brief weekly summary for a habit tracker user. " +
            "Their completion rate was ${(completionRate * 100).toInt()}%. " +
            "Strongest habit: ${strongestHabit?.first?.name ?: "None"} (${((strongestHabit?.second ?: 0f) * 100).toInt()}%). " +
            "Weakest habit: ${weakestHabit?.first?.name ?: "None"} (${((weakestHabit?.second ?: 0f) * 100).toInt()}%). " +
            "Provide: 1) top win, 2) growth area, 3) pattern observation, 4) focus for next week. Keep each point to 1 sentence.",
            coachPersonality,
            userContext
        ).getOrNull()

        _weeklySummary.value = WeeklyCoachingSummary(
            id = "summary_${System.currentTimeMillis()}",
            weekStartDate = weekStartDate,
            weekEndDate = weekEndDate,
            overallScore = overallScore,
            completionRate = completionRate,
            streakStatus = StreakSummary(
                currentStreak = streakInfo.currentStreak,
                longestStreak = streakInfo.longestStreak,
                streakTrend = streakTrend
            ),
            topWin = strongestHabit?.let { "Great consistency with ${it.first.name}!" } ?: "You're building momentum!",
            growthArea = weakestHabit?.let { "${it.first.name} needs more attention" } ?: "Keep exploring what works",
            patternDiscovered = aiSummary ?: "You're most consistent in the mornings",
            nextWeekFocus = weakestHabit?.let { "Try focusing on ${it.first.name} this week" } ?: "Maintain your current habits",
            personalizedAdvice = listOf(
                strongestHabit?.let { "Your ${it.first.name} habit is your anchor - keep it strong!" } ?: "Build one strong habit first",
                "Consistency matters more than perfection",
                if (completionRate > 0.7f) "You're doing great! Small improvements compound over time" else "Focus on completing just one habit each day to build momentum"
            )
        )
    }

    override suspend fun getMotivationBoost(): String {
        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()

        return claudeApiClient.getCoachingResponse(
            "Give me a quick, energizing motivation boost in 1-2 sentences!",
            coachPersonality,
            userContext
        ).getOrElse {
            CoachingTemplates.motivationBoosts.random()
        }
    }

    override suspend fun getRecoveryMessage(habitId: String): String {
        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()

        return claudeApiClient.getCoachingResponse(
            "Help me recover from breaking my streak on the '$habitId' habit. Be supportive and give practical advice.",
            coachPersonality,
            userContext
        ).getOrElse {
            CoachingTemplates.getRecoveryAdvice(2, habitId)
        }
    }

    override suspend fun getCelebrationMessage(habitId: String, completionCount: Int): String {
        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()

        return claudeApiClient.getCoachingResponse(
            "Celebrate my achievement! I've completed the '$habitId' habit $completionCount times!",
            coachPersonality,
            userContext
        ).getOrElse {
            CoachingTemplates.getCompletionCelebration(habitId, completionCount)
        }
    }

    /**
     * Generate action items based on real user data
     */
    suspend fun generateActionItems() {
        val todayEntry = entryRepository.getTodayEntry().first()
        val enabledHabits = habitRepository.getEnabledHabits().first()
        val streakInfo = entryRepository.getStreakInfo().first()

        val incompleteHabits = enabledHabits.filter { habit ->
            todayEntry?.completions?.get(habit.id) != true
        }

        val actionItems = mutableListOf<CoachingActionItem>()

        // Add action items for incomplete habits
        incompleteHabits.take(2).forEach { habit ->
            actionItems.add(
                CoachingActionItem(
                    id = "item_${habit.id}",
                    title = "Complete ${habit.name}",
                    description = "Keep your streak going!",
                    habitId = habit.id,
                    priority = if (streakInfo.currentStreak > 3) ActionPriority.HIGH else ActionPriority.MEDIUM
                )
            )
        }

        // Add review action if they have completed habits
        val completedCount = todayEntry?.completions?.count { it.value } ?: 0
        if (completedCount > 0) {
            actionItems.add(
                CoachingActionItem(
                    id = "item_review",
                    title = "Review your progress",
                    description = "You've completed $completedCount habits today!",
                    priority = ActionPriority.LOW
                )
            )
        }

        _actionItems.value = actionItems
    }
}
