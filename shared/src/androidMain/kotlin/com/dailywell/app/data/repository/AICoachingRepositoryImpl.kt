package com.dailywell.app.data.repository

import com.dailywell.app.ai.AIRoutingEngine
import com.dailywell.app.ai.ModelDownloadManager
import com.dailywell.app.ai.ModelDownloadState
import com.dailywell.app.ai.SLMService
import com.dailywell.app.ai.ResponseCategory
import com.dailywell.app.api.ClaudeApiClient
import com.dailywell.app.api.CoachPersonality
import com.dailywell.app.api.UserContext as ApiUserContext
import com.dailywell.app.data.local.AIFeaturePersistence
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Task complexity levels for intelligent AI routing
 *
 * Determines which AI model to use based on message complexity:
 * - SIMPLE: Quick affirmations, yes/no, greetings -> Qwen2.5 0.5B (FREE)
 * - MODERATE: Coaching questions, habit tips -> Claude Haiku ($1/$5 MTok)
 * - COMPLEX: Detailed analysis, personalized plans -> Claude Sonnet ($3/$15 MTok)
 * - HEAVY: Weekly/monthly reports, comprehensive insights -> Claude Opus ($15/$75 MTok)
 */
enum class TaskComplexity {
    SIMPLE,    // SLM can handle
    MODERATE,  // Haiku
    COMPLEX,   // Sonnet
    HEAVY      // Opus (reports)
}

/** Helper for routing result with 4 values */
private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Implementation of AICoachingRepository with REAL Claude API integration
 * Includes COST CONTROL with hybrid Decision Tree + Claude AI + SLM system
 *
 * AI Model Hierarchy (cost savings strategy):
 * 1. Decision Tree (FREE) - Simple routing, ~70% of messages
 * 2. Qwen2.5 0.5B (FREE, on-device) - Complex reasoning when cloud cap reached
 * 3. Claude Haiku 4.5 (PAID) - High quality cloud responses
 * 4. Claude Sonnet 4.5 (PAID) - Vision/complex tasks only
 *
 * Monthly USD Caps:
 * - MONTHLY/LIFETIME: $5.00 soft, $5.50 hard -> SLM-only after
 * - ANNUAL: $3.60 soft, $4.00 hard -> SLM-only after
 * - FAMILY_OWNER (60%/3x): $2.50 soft, $2.75 hard -> SLM-only after
 * - FAMILY_MEMBER (20%): $0.80 soft, $0.92 hard -> SLM-only after
 */
class AICoachingRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val claudeApiClient: ClaudeApiClient,
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository,
    private val aiFeaturePersistence: AIFeaturePersistence? = null,  // Persistence for all 5 AI features
    private val slmService: SLMService? = null,  // On-device Qwen 0.5B via llama.cpp
    private val familyUsageManager: FamilyUsageManager? = null,  // Optional family quota
    private val userBehaviorRepository: UserBehaviorRepository? = null,  // User learning system
    private val settingsRepository: SettingsRepository? = null,
    private val modelDownloadManager: ModelDownloadManager? = null  // Cloud rate limiter when no SLM
) : AICoachingRepository {

    private val defaultCoach = CoachPersonas.supportiveSam
    private val _dailyInsight = MutableStateFlow<DailyCoachingInsight?>(null)
    private val _sessions = MutableStateFlow<List<AICoachingSession>>(emptyList())
    private val _actionItems = MutableStateFlow<List<CoachingActionItem>>(emptyList())
    private val _weeklySummary = MutableStateFlow<WeeklyCoachingSummary?>(null)

    // AI Usage Tracking
    private val _aiUsage = MutableStateFlow(createDefaultUsage())
    private val _dailyMessageCount = MutableStateFlow(0)
    private val _interactions = MutableStateFlow<List<AIInteraction>>(emptyList())

    private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val persistJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private companion object {
        const val AI_USAGE_KEY = "ai_usage"
        const val AI_SESSIONS_KEY = "ai_sessions"
        const val AI_MEMORIES_KEY = "ai_conversation_memories"
        const val AI_DAILY_MSG_COUNT_KEY = "ai_daily_msg_count"
        const val AI_PENDING_SCAN_HANDOFF_KEY = "ai_pending_scan_handoff"
        const val SESSION_RETENTION_DAYS = 60L
        const val MAX_SAVED_SESSIONS = 50
    }

    init {
        // Load persisted state on startup
        persistScope.launch {
            try {
                // Load real userId
                val realUserId = settingsRepository?.getSettingsSnapshot()?.firebaseUid
                if (!realUserId.isNullOrBlank()) {
                    _aiUsage.value = _aiUsage.value.copy(userId = realUserId)
                }
            } catch (_: Exception) {}

            try {
                val usageJson = dataStoreManager.getString(AI_USAGE_KEY).first()
                if (usageJson != null) {
                    _aiUsage.value = persistJson.decodeFromString<UserAIUsage>(usageJson)
                }
            } catch (_: Exception) {}

            try {
                val sessionsJson = dataStoreManager.getString(AI_SESSIONS_KEY).first()
                if (sessionsJson != null) {
                    val restored = persistJson.decodeFromString<List<AICoachingSession>>(sessionsJson)
                    val pruned = pruneOldSessions(restored).take(MAX_SAVED_SESSIONS)
                    _sessions.value = pruned
                    if (pruned.size != restored.size) {
                        dataStoreManager.putString(AI_SESSIONS_KEY, persistJson.encodeToString(pruned))
                    }
                }
            } catch (_: Exception) {}

            try {
                val memoriesJson = dataStoreManager.getString(AI_MEMORIES_KEY).first()
                if (memoriesJson != null) {
                    _conversationMemories.value = persistJson.decodeFromString<Map<String, List<ConversationMemory>>>(memoriesJson)
                }
            } catch (_: Exception) {}

            try {
                val msgCountJson = dataStoreManager.getString(AI_DAILY_MSG_COUNT_KEY).first()
                if (msgCountJson != null) {
                    _dailyMessageCount.value = msgCountJson.toIntOrNull() ?: 0
                }
            } catch (_: Exception) {}

            // Keep premium users out of FREE gating if legacy state had no explicit plan set.
            try {
                val settings = settingsRepository?.getSettingsSnapshot()
                if (settings?.isPremium == true && _aiUsage.value.planType == AIPlanType.FREE) {
                    _aiUsage.value = _aiUsage.value.copy(
                        planType = AIPlanType.MONTHLY,
                        monthlyTokenLimit = AIPlanType.MONTHLY.monthlyTokenLimit
                    )
                    dataStoreManager.putString(AI_USAGE_KEY, persistJson.encodeToString(_aiUsage.value))
                }
            } catch (_: Exception) {}
        }
    }

    private fun persistAiUsage() {
        persistScope.launch {
            try {
                dataStoreManager.putString(AI_USAGE_KEY, persistJson.encodeToString(_aiUsage.value))
            } catch (_: Exception) {}
        }
    }

    private fun persistSessions() {
        persistScope.launch {
            try {
                // Keep recent sessions only and auto-expire old history.
                val toSave = pruneOldSessions(_sessions.value).take(MAX_SAVED_SESSIONS)
                _sessions.value = toSave
                dataStoreManager.putString(AI_SESSIONS_KEY, persistJson.encodeToString(toSave))
            } catch (_: Exception) {}
        }
    }

    private fun pruneOldSessions(sessions: List<AICoachingSession>): List<AICoachingSession> {
        val cutoffMs = Clock.System.now().toEpochMilliseconds() - SESSION_RETENTION_DAYS * 24L * 60L * 60L * 1000L
        return sessions.filter { session ->
            runCatching { Instant.parse(session.startedAt).toEpochMilliseconds() >= cutoffMs }
                .getOrDefault(true)
        }
    }

    private fun persistConversationMemories() {
        persistScope.launch {
            try {
                dataStoreManager.putString(AI_MEMORIES_KEY, persistJson.encodeToString(_conversationMemories.value))
            } catch (_: Exception) {}
        }
    }

    private fun persistDailyMessageCount() {
        persistScope.launch {
            try {
                dataStoreManager.putString(AI_DAILY_MSG_COUNT_KEY, _dailyMessageCount.value.toString())
            } catch (_: Exception) {}
        }
    }

    private fun createDefaultUsage(): UserAIUsage {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val nextMonth = LocalDate(
            year = if (today.monthNumber == 12) today.year + 1 else today.year,
            monthNumber = if (today.monthNumber == 12) 1 else today.monthNumber + 1,
            dayOfMonth = 1
        )
        return UserAIUsage(
            userId = "default_user",
            planType = AIPlanType.FREE,
            monthlyTokenLimit = AIPlanType.FREE.monthlyTokenLimit,
            tokensUsed = 0,
            messagesCount = 0,
            freeMessagesCount = 0,
            aiMessagesCount = 0,
            resetDate = nextMonth.toString(),
            lastUpdated = today.toString()
        )
    }

    private fun getTimeOfDay(): String {
        val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            else -> "evening"
        }
    }

    private suspend fun buildUserContext(): ApiUserContext {
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

        // ========================================
        // FEATURE: Use Context Cache for token efficiency
        // This reduces context tokens by ~30% using cached summaries
        // ========================================
        val userId = _aiUsage.value.userId
        val cachedContextSummary = try {
            val cached = AIRoutingEngine.ContextCache.getCachedContext(userId)
            cached?.toCondensedPrompt()
        } catch (e: Exception) { null }

        // Get behavior profile for personalized AI coaching
        // Combine cached context with live behavior profile
        val behaviorContext = try {
            val liveContext = userBehaviorRepository?.getPromptContext()
            when {
                cachedContextSummary != null && liveContext != null ->
                    "$cachedContextSummary\n$liveContext"
                cachedContextSummary != null -> cachedContextSummary
                else -> liveContext
            }
        } catch (e: Exception) {
            cachedContextSummary
        }

        return ApiUserContext(
            currentStreak = streakInfo.currentStreak,
            totalHabits = enabledHabits.size,
            todayCompleted = completedToday,
            strongestHabit = strongestHabit,
            weakestHabit = weakestHabit,
            timeOfDay = getTimeOfDay(),
            behaviorProfileContext = behaviorContext
        )
    }

    private fun buildCoachPersonality(): CoachPersonality {
        val coach = defaultCoach
        return CoachPersonality(
            name = coach.name,
            style = coach.style.name.lowercase().replaceFirstChar { it.uppercase() },
            description = coach.description
        )
    }

    override fun getDailyInsight(): Flow<DailyCoachingInsight?> = _dailyInsight

    override suspend fun generateDailyInsight() {
        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()
        val isPremium = hasCloudEntitlement()

        // TIER CHECK: Only premium users get Claude API (rate-limited when no SLM)
        val result = if (isPremium) {
            guardedCloudCall {
                claudeApiClient.generateDailyInsight(userContext, coachPersonality)
            } ?: Result.failure(Exception("Rate limited - no SLM"))
        } else {
            Result.failure(Exception("Free tier - using template"))
        }

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
                suggestedActions = buildList {
                    // First action: contextual based on focusHabit
                    val focus = aiInsight.focusHabit
                    if (focus != null) {
                        add(SuggestedAction(
                            id = "action_1",
                            emoji = "Check",
                            title = "Focus on $focus",
                            description = aiInsight.focusReason ?: "Give this habit some extra attention today",
                            actionType = SuggestedActionType.COMPLETE_HABIT
                        ))
                    } else {
                        add(SuggestedAction(
                            id = "action_1",
                            emoji = "Check",
                            title = "Complete a Habit",
                            description = "Start with any habit right now!",
                            actionType = SuggestedActionType.COMPLETE_HABIT
                        ))
                    }
                    // Second action: reflect if there's a quote, celebrate if there's a celebration, else set intention
                    if (aiInsight.motivationalQuote != null) {
                        add(SuggestedAction(
                            id = "action_2",
                            emoji = "Reflect",
                            title = "Reflect on Today",
                            description = "Take a moment to reflect: ${aiInsight.motivationalQuote}",
                            actionType = SuggestedActionType.REFLECT
                        ))
                    } else if (aiInsight.celebrationNote != null) {
                        add(SuggestedAction(
                            id = "action_2",
                            emoji = "Celebrate",
                            title = "Celebrate Your Wins",
                            description = aiInsight.celebrationNote,
                            actionType = SuggestedActionType.CELEBRATE
                        ))
                    } else {
                        add(SuggestedAction(
                            id = "action_2",
                            emoji = "Plan",
                            title = "Set Intention",
                            description = "Plan your day for success",
                            actionType = SuggestedActionType.SET_INTENTION
                        ))
                    }
                },
                celebrationNote = aiInsight.celebrationNote
            )
        }.onFailure {
            // SLM-based daily insight for free/trial users, or download status if SLM not ready
            val slmGreeting = generateSLMFallbackResponse(
                "Give a short, warm daily greeting and one motivating sentence about habits."
            )
            val now = Clock.System.now()
            val dateStr = now.toString()

            _dailyInsight.value = DailyCoachingInsight(
                id = "insight_${System.currentTimeMillis()}",
                date = dateStr.substringBefore("T"),
                greeting = if (slmGreeting.length > 20) slmGreeting.substringBefore(".").plus(".") else getDownloadStatusMessage(),
                mainMessage = if (slmGreeting.length > 20) slmGreeting else getDownloadStatusMessage(),
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
        val coach = defaultCoach
        val openingMessage = buildInstantOpeningMessage(type, coach)

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
                    modelUsed = AIModelUsed.DECISION_TREE,
                    suggestions = getSuggestionsForType(type),
                    actionButtons = getActionButtonsForType(type)
                )
            ),
            startedAt = now
        )

        // Keep only one active session at a time.
        val closed = _sessions.value.map { existing ->
            if (existing.status == SessionStatus.IN_PROGRESS) {
                existing.copy(status = SessionStatus.COMPLETED, completedAt = now)
            } else {
                existing
            }
        }
        _sessions.value = listOf(session) + closed
        persistSessions()
        return session
    }

    private fun buildInstantOpeningMessage(
        type: CoachingSessionType,
        coach: CoachPersona
    ): String {
        val coachTone = when (coach.style) {
            CoachingStyle.ENCOURAGING -> "Great to see you. Let's build momentum today."
            CoachingStyle.DIRECT -> "Let's keep this simple and action-first."
            CoachingStyle.ANALYTICAL -> "We will focus on a practical step with clear impact."
            CoachingStyle.GENTLE -> "Take a calm step forward. No pressure."
            CoachingStyle.MOTIVATIONAL -> "Let's move now and use this energy well."
        }

        val prompt = when (type) {
            CoachingSessionType.DAILY_CHECKIN ->
                "How are you feeling today, and what is one habit you want to complete first?"
            CoachingSessionType.WEEKLY_REVIEW ->
                "What felt strongest this week, and what should we improve next week?"
            CoachingSessionType.HABIT_COACHING ->
                "Tell me one habit you want to improve, and I will give you a clear next step."
            CoachingSessionType.MOTIVATION_BOOST ->
                "Name one task you are avoiding, and we will break it into a tiny first move."
            CoachingSessionType.OBSTACLE_SOLVING ->
                "What is blocking you right now: time, energy, focus, or consistency?"
            CoachingSessionType.CELEBRATION ->
                "Share one win from today so we can lock in that momentum."
            CoachingSessionType.RECOVERY_SUPPORT ->
                "No reset needed. Which habit slipped, and what is the easiest restart version?"
        }

        return "$coachTone\n\n$prompt"
    }

    override suspend fun resumeSession(sessionId: String): AICoachingSession? {
        val now = Clock.System.now().toString()
        val target = _sessions.value.find { it.id == sessionId } ?: return null

        _sessions.value = _sessions.value.map { session ->
            when {
                session.id == sessionId ->
                    session.copy(status = SessionStatus.IN_PROGRESS, completedAt = null)
                session.status == SessionStatus.IN_PROGRESS ->
                    session.copy(status = SessionStatus.COMPLETED, completedAt = now)
                else -> session
            }
        }
        persistSessions()
        return target.copy(status = SessionStatus.IN_PROGRESS, completedAt = null)
    }

    private fun getSuggestionsForType(type: CoachingSessionType): List<String> {
        return when (type) {
            CoachingSessionType.DAILY_CHECKIN -> listOf(
                "Feeling great!",
                "A bit tired today",
                "Ready to crush it!",
                "Need some motivation"
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
            else -> listOf("Let's go!", "Tell me more", "I have a question")
        }
    }

    private fun getActionButtonsForType(type: CoachingSessionType): List<CoachingAction> {
        return when (type) {
            CoachingSessionType.DAILY_CHECKIN -> listOf(
                CoachingAction("action_log", "Log Habit", "Log", CoachingActionType.LOG_HABIT),
                CoachingAction("action_view", "View Stats", "Stats", CoachingActionType.VIEW_INSIGHTS)
            )
            CoachingSessionType.HABIT_COACHING -> listOf(
                CoachingAction("action_intention", "Set Intention", "Plan", CoachingActionType.SET_INTENTION),
                CoachingAction("action_adjust", "Adjust Goal", "Adjust", CoachingActionType.ADJUST_GOAL)
            )
            else -> emptyList()
        }
    }

    override suspend fun sendMessage(sessionId: String, message: String): CoachingMessage {
        val now = Clock.System.now().toString()

        val userMessage = CoachingMessage(
            id = "msg_user_${System.currentTimeMillis()}",
            role = MessageRole.USER,
            content = message,
            timestamp = now
        )

        val availability = checkAIAvailability()
        val result: ResponseResult = if (!availability.canUseCloudAI) {
            val unavailableMessage = when (availability.reason) {
                AIUsageBlockReason.NOT_PREMIUM ->
                    "Cloud coach replies use Haiku and need Premium. Upgrade to unlock live coaching."
                AIUsageBlockReason.SOFT_CAP_REACHED,
                AIUsageBlockReason.HARD_CAP_REACHED,
                AIUsageBlockReason.CREDITS_DEPLETED ->
                    "Cloud coach credits are exhausted for now. They reset on ${_aiUsage.value.resetDate}."
                AIUsageBlockReason.DAILY_LIMIT_REACHED ->
                    "Daily coach message limit reached. Try again tomorrow."
                else ->
                    "Cloud coach is not available right now."
            }
            ResponseResult(unavailableMessage, AIModelUsed.DECISION_TREE)
        } else {
            val userContext = buildUserContext()
            val coachPersonality = buildCoachPersonality()
            val contextualMessage = appendCoachReplyTemplate(
                buildCoachConversationContext(sessionId, message)
            )

            val cloudReply = guardedCloudCall {
                claudeApiClient.getCoachingResponse(
                    userMessage = contextualMessage,
                    coachPersonality = coachPersonality,
                    userContext = userContext,
                    model = AIModelUsed.CLAUDE_HAIKU
                ).getOrElse {
                    throw Exception("Claude API failed: ${it.message}")
                }
            }

            if (cloudReply == null) {
                ResponseResult(
                    "Cloud coach is temporarily unavailable. Try again in a moment.",
                    AIModelUsed.DECISION_TREE
                )
            } else {
                val inputTokens = (contextualMessage.length / 4).coerceAtLeast(120)
                val outputTokens = (cloudReply.length / 4).coerceIn(90, 320)
                trackTokenUsageWithModel(
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    model = AIModelUsed.CLAUDE_HAIKU,
                    category = "CHAT_HAIKU"
                )
                ResponseResult(cloudReply, AIModelUsed.CLAUDE_HAIKU)
            }
        }

        trackMessageSentByModel(result.model, ResponseCategory.NEEDS_AI.name)

        val templatedCoachReply = normalizeCoachReply(result.response, message)
        val coachResponse = CoachingMessage(
            id = "msg_coach_${System.currentTimeMillis()}",
            role = MessageRole.COACH,
            content = templatedCoachReply,
            timestamp = now,
            modelUsed = result.model,
            suggestions = listOf("Give me one step", "Make it easier", "Plan the next one")
        )

        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(messages = session.messages + userMessage + coachResponse)
            } else session
        }
        persistSessions()

        return coachResponse
    }

    private fun buildCoachConversationContext(sessionId: String, latestMessage: String): String {
        val session = _sessions.value.firstOrNull { it.id == sessionId }
        val recentTurns = session?.messages
            ?.takeLast(6)
            .orEmpty()
            .joinToString(separator = "\n") { turn ->
                val role = if (turn.role == MessageRole.USER) "User" else "Coach"
                "$role: ${turn.content.trim()}"
            }

        return buildString {
            if (recentTurns.isNotBlank()) {
                appendLine("Recent conversation:")
                appendLine(recentTurns)
                appendLine()
            }
            appendLine("Latest user message:")
            appendLine(latestMessage.trim())
            appendLine()
            append("Respond to the latest message directly and avoid repeating earlier phrasing.")
        }
    }

    private fun appendCoachReplyTemplate(message: String): String {
        return buildString {
            append(message.trim())
            appendLine()
            appendLine()
            appendLine("Coach reply format:")
            appendLine("- Keep it to 2-3 short sentences.")
            appendLine("- Include exactly one sentence starting with 'Next step:'.")
            appendLine("- End with one brief check-in question when useful.")
            appendLine("- Be friendly, practical, and non-judgmental.")
        }
    }

    private fun normalizeCoachReply(raw: String, userMessage: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return "You're not behind. Next step: ${buildDynamicNextStep(userMessage)} Want me to tailor it to your day?"
        }
        if (isSystemStatusMessage(trimmed)) return trimmed

        val flattened = trimmed
            .lineSequence()
            .map { line ->
                line.trim()
                    .removePrefix("- ")
                    .removePrefix("* ")
                    .removePrefix("• ")
                    .trim()
            }
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val concise = flattened
            .split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" ")
            .trim()

        val withAction = if (Regex("next\\s*step\\s*:", RegexOption.IGNORE_CASE).containsMatchIn(concise)) {
            concise
        } else {
            val suffix = if (concise.endsWith(".") || concise.endsWith("!") || concise.endsWith("?")) "" else "."
            "$concise$suffix Next step: ${buildDynamicNextStep(userMessage)}"
        }

        return if (withAction.contains("?")) {
            withAction
        } else {
            "$withAction Want me to tailor that to your day?"
        }
    }

    private fun buildDynamicNextStep(userMessage: String): String {
        val normalized = userMessage.lowercase()
        return when {
            normalized.contains("sleep") -> "set a wind-down alarm 45 minutes before bed tonight."
            normalized.contains("water") || normalized.contains("hydrate") -> "drink one glass of water right now."
            normalized.contains("workout") || normalized.contains("exercise") || normalized.contains("move") ->
                "do 5 minutes of movement before your next break."
            normalized.contains("stress") || normalized.contains("anxious") || normalized.contains("overwhelmed") ->
                "take 3 slow breaths and write one simple priority."
            else -> "pick one action you can finish in 5 minutes."
        }
    }

    private fun isSystemStatusMessage(text: String): Boolean {
        val lower = text.lowercase()
        return listOf(
            "on-device ai coach",
            "ready to install",
            "connect to wifi",
            "needs a bit more storage",
            "setting up your on-device ai coach",
            "installing your on-device ai coach",
            "please wait a moment",
            "rate limited"
        ).any { token -> lower.contains(token) }
    }

    private fun trackMessageSent(usedDecisionTree: Boolean, category: String) {
        trackMessageSentByModel(
            if (usedDecisionTree) AIModelUsed.DECISION_TREE else AIModelUsed.CLAUDE_HAIKU,
            category
        )
    }

    /**
     * Track message sent by specific AI model
     */
    private fun trackMessageSentByModel(model: AIModelUsed, category: String) {
        val current = _aiUsage.value
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        _aiUsage.value = current.copy(
            messagesCount = current.messagesCount + 1,
            freeMessagesCount = if (model == AIModelUsed.DECISION_TREE)
                current.freeMessagesCount + 1 else current.freeMessagesCount,
            slmMessagesCount = if (model == AIModelUsed.QWEN_0_5B)
                current.slmMessagesCount + 1 else current.slmMessagesCount,
            aiMessagesCount = if (!model.isFree)
                current.aiMessagesCount + 1 else current.aiMessagesCount,
            cloudChatCalls = if (!model.isFree)
                current.cloudChatCalls + 1 else current.cloudChatCalls,
            lastUpdated = today
        )

        // Only count cloud API messages against daily limit
        if (!model.isFree) {
            _dailyMessageCount.value = _dailyMessageCount.value + 1
            persistDailyMessageCount()
        }
        persistAiUsage()
    }

    private fun generateFallbackResponse(userMessage: String): String {
        val coach = defaultCoach
        return when (coach.style) {
            CoachingStyle.ENCOURAGING -> {
                when {
                    userMessage.contains("tired", ignoreCase = true) ->
                        "I hear you! It's okay to feel tired. Remember, even small steps count. What's one tiny thing you could do today?"
                    userMessage.contains("great", ignoreCase = true) ->
                        "That's wonderful to hear! Your positive energy will fuel your habits today. Let's make it count!"
                    else ->
                        "Thanks for sharing! I'm here to support you. What would make today a win for you?"
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

    /**
     * Generate a response using on-device Qwen 0.5B (FREE, offline).
     * Falls back to pattern-based responses if model not loaded.
     */
    private suspend fun generateSLMFallbackResponse(userMessage: String): String {
        val coach = defaultCoach

        // Try Qwen 0.5B - generateResponse() handles lazy init internally
        // (loads model into memory on first call after download completes)
        if (slmService != null) {
            val systemPrompt = """
                You are ${coach.name}, a ${coach.style.name.lowercase()} habit coach.
                ${coach.description}
                Keep responses concise (2-3 sentences) and friendly.
                Include one sentence that starts with "Next step:" and one short check-in question.
            """.trimIndent()

            val result = slmService.generateResponse(
                prompt = userMessage,
                systemPrompt = systemPrompt,
                maxTokens = 150
            )

            if (result.isSuccess) {
                return result.getOrThrow()
            }
            // SLM failed (not ARM64, model not downloaded, generation error)
        }

        // SLM not available - show real download status
        return getDownloadStatusMessage()
    }

    /**
     * Transparent message when SLM is not yet available.
     * Shows real download progress instead of generic canned templates.
     */
    private fun getDownloadStatusMessage(): String {
        modelDownloadManager?.maybeRecoverStalledDownload()
        val state = modelDownloadManager?.downloadState?.value
        return when (state) {
            is ModelDownloadState.Downloading -> {
                val pct = (state.progress * 100).toInt()
                if (pct <= 0) {
                    "Setting up your on-device AI coach from Google Play..."
                } else {
                    "Installing your on-device AI coach... $pct% complete."
                }
            }
            is ModelDownloadState.WaitingForWifi ->
                "Your AI coach is ready to install. Connect to WiFi to continue."
            is ModelDownloadState.NeedsStorage ->
                "Your AI coach needs a bit more storage space. Free up some space and I'll be ready to help!"
            is ModelDownloadState.Failed ->
                state.error
            is ModelDownloadState.NotStarted ->
                "Your AI coach is setting up. It'll be ready shortly!"
            is ModelDownloadState.Completed ->
                "Your AI coach is ready! Try sending another message."
            null ->
                "Your AI coach is setting up. It'll be ready shortly!"
        }
    }

    // =========================================================================
    // EXECUTE WITH FALLBACK CHAIN (Enterprise-Grade Reliability)
    // =========================================================================

    /**
     * Execute AI request with full fallback chain
     *
     * Fallback order: Primary Model -> Haiku -> SLM (Qwen2.5) -> Pattern-based
     * Includes circuit breaker, timeout, and cost tracking
     */
    private data class ResponseResult(val response: String, val model: AIModelUsed)

    private suspend fun executeWithFallback(
        message: String,
        routingDecision: AIRoutingEngine.RoutingDecision
    ): ResponseResult {
        val model = routingDecision.model
        val fallbackChain = routingDecision.fallbackChain
        val budgetMode = routingDecision.budgetMode
        val intent = routingDecision.intent

        // Check circuit breaker
        if (!AIRoutingEngine.isModelAllowed(model)) {
            // Circuit breaker tripped - use first allowed fallback
            val allowedModel = fallbackChain.firstOrNull { AIRoutingEngine.isModelAllowed(it) }
                ?: AIModelUsed.DECISION_TREE
            return executeModel(message, allowedModel, routingDecision.maxOutputTokens, budgetMode, intent)
        }

        // Try primary model
        return try {
            val result = executeModel(message, model, routingDecision.maxOutputTokens, budgetMode, intent)
            AIRoutingEngine.recordSuccess()
            result
        } catch (e: Exception) {
            // Record failure and try fallback
            AIRoutingEngine.recordFailure(model, e.message ?: "Unknown error")

            // Walk the fallback chain
            for (fallbackModel in fallbackChain) {
                if (AIRoutingEngine.isModelAllowed(fallbackModel)) {
                    try {
                        return executeModel(message, fallbackModel, routingDecision.maxOutputTokens, budgetMode, intent)
                    } catch (fallbackError: Exception) {
                        AIRoutingEngine.recordFailure(fallbackModel, fallbackError.message ?: "Fallback failed")
                        continue
                    }
                }
            }

            // Ultimate fallback - show download status
            ResponseResult(getDownloadStatusMessage(), AIModelUsed.DECISION_TREE)
        }
    }

    /**
     * Check if user is on active 14-day trial (planType can still be FREE in local AI usage state).
     */
    private suspend fun isTrialUser(): Boolean {
        val settings = try {
            settingsRepository?.getSettingsSnapshot() ?: return false
        } catch (_: Exception) { return false }
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        return settings.isTrialActive(today)
    }

    private data class EntitlementState(
        val isTrialActive: Boolean,
        val effectivePlanType: AIPlanType,
        val softCapUsd: Float,
        val hardCapUsd: Float
    )

    /**
     * Trial users can keep `planType=FREE` in local state while still having premium cloud entitlement.
     * Resolve one effective plan/cap bundle so routing + wallet checks stay consistent.
     */
    private suspend fun resolveEntitlementState(): EntitlementState {
        val usagePlanType = _aiUsage.value.planType
        val trialActive = usagePlanType == AIPlanType.FREE && isTrialUser()
        val effectivePlanType = if (trialActive) AIPlanType.MONTHLY else usagePlanType
        val softCap = if (trialActive) effectivePlanType.softCapUsd else _aiUsage.value.softCapUsd
        val hardCap = if (trialActive) effectivePlanType.hardCapUsd else _aiUsage.value.hardCapUsd
        return EntitlementState(
            isTrialActive = trialActive,
            effectivePlanType = effectivePlanType,
            softCapUsd = softCap,
            hardCapUsd = hardCap
        )
    }

    private suspend fun hasCloudEntitlement(): Boolean {
        return resolveEntitlementState().effectivePlanType != AIPlanType.FREE
    }

    /**
     * Guard cloud API calls with rate limiter.
     * When no SLM is present, users are limited to 10 cloud calls/day to protect margins.
     * Returns null if rate-limited (caller should use SLM fallback).
     */
    private suspend fun <T> guardedCloudCall(block: suspend () -> T): T? {
        val mgr = modelDownloadManager
        if (mgr != null && !mgr.canMakeCloudCall()) {
            return null // Rate-limited - no SLM and daily quota exceeded
        }
        val result = block()
        mgr?.recordCloudCall()
        return result
    }

    /**
     * Execute a specific model with appropriate handling
     */
    private suspend fun executeModel(
        message: String,
        model: AIModelUsed,
        maxOutputTokens: Int,
        budgetMode: AIRoutingEngine.BudgetMode,
        intent: AIRoutingEngine.RequestIntent
    ): ResponseResult {
        return when (model) {
            AIModelUsed.DECISION_TREE -> {
                ResponseResult(getDownloadStatusMessage(), AIModelUsed.DECISION_TREE)
            }

            AIModelUsed.QWEN_0_5B -> {
                val response = generateSLMFallbackResponse(message)
                ResponseResult(response, AIModelUsed.QWEN_0_5B)
            }

            AIModelUsed.CLAUDE_HAIKU,
            AIModelUsed.CLAUDE_SONNET,
            AIModelUsed.CLAUDE_OPUS -> {
                // Rate-limit cloud when no SLM is available
                val guardedResponse = guardedCloudCall {
                    val userContext = buildUserContext()
                    val coachPersonality = buildCoachPersonality()

                    val promptContract = AIRoutingEngine.getPromptContract(model, budgetMode)
                    val coachTemplatedMessage = appendCoachReplyTemplate(message)
                    val enhancedMessage = if (promptContract.isNotEmpty()) {
                        "$coachTemplatedMessage\n\n$promptContract"
                    } else coachTemplatedMessage

                    claudeApiClient.getCoachingResponse(
                        enhancedMessage,
                        coachPersonality,
                        userContext,
                        model
                    ).getOrElse {
                        throw Exception("Claude API failed: ${it.message}")
                    }
                }

                // If rate-limited, fall back to SLM or download status
                val response = guardedResponse
                    ?: generateSLMFallbackResponse(message).takeIf { it.length > 20 }
                    ?: getDownloadStatusMessage()

                // Guarded call may fall back locally; only bill cloud on real cloud response.
                if (guardedResponse == null) {
                    return ResponseResult(response, AIModelUsed.QWEN_0_5B)
                }

                // Estimate tokens (will be replaced with actual counting)
                val (inputTokens, outputTokens) = when (model) {
                    AIModelUsed.CLAUDE_HAIKU -> Pair(150, minOf(200, maxOutputTokens))
                    AIModelUsed.CLAUDE_SONNET -> Pair(300, minOf(400, maxOutputTokens))
                    AIModelUsed.CLAUDE_OPUS -> Pair(500, minOf(800, maxOutputTokens))
                    else -> Pair(100, 150)
                }

                // Track usage
                val cost = trackTokenUsageWithModel(
                    inputTokens = inputTokens,
                    outputTokens = outputTokens,
                    model = model,
                    category = intent.name
                )

                // Record intent usage and telemetry
                AIRoutingEngine.recordIntentUsage(intent, inputTokens + outputTokens, cost)
                AIRoutingEngine.recordTelemetry(intent, model, cost, _aiUsage.value.userId)

                // Track family usage if applicable
                familyUsageManager?.trackUsage(cost, inputTokens, outputTokens, model)

                ResponseResult(response, model)
            }
        }
    }

    /**
     * Determine task complexity for intelligent model routing
     *
     * Analyzes the user message to determine appropriate AI model:
     * - SIMPLE: Greetings, yes/no, quick acknowledgments -> SLM
     * - MODERATE: Coaching questions, habit advice -> Haiku
     * - COMPLEX: Detailed analysis, personal plans -> Sonnet
     * - HEAVY: Weekly/monthly reports, comprehensive insights -> Opus
     */
    private fun determineTaskComplexity(message: String, sessionType: CoachingSessionType? = null): TaskComplexity {
        val lowerMessage = message.lowercase().trim()
        val wordCount = lowerMessage.split("\\s+".toRegex()).size

        // HEAVY: Report generation, weekly summaries, comprehensive analysis
        val heavyPatterns = listOf(
            "weekly", "monthly", "report", "summary", "comprehensive",
            "analyze my", "full analysis", "deep dive", "in-depth",
            "over the past", "last month", "last week", "progress report"
        )
        if (heavyPatterns.any { lowerMessage.contains(it) } ||
            sessionType == CoachingSessionType.WEEKLY_REVIEW) {
            return TaskComplexity.HEAVY
        }

        // SIMPLE: Very short messages, greetings, yes/no, acknowledgments
        val simplePatterns = listOf(
            "hi", "hello", "hey", "thanks", "thank you", "ok", "okay",
            "yes", "no", "sure", "got it", "sounds good", "great",
                "good morning", "good night", "bye", "later"
        )
        if (wordCount <= 3 || simplePatterns.any { lowerMessage == it || lowerMessage.startsWith("$it ") }) {
            return TaskComplexity.SIMPLE
        }

        // COMPLEX: Detailed personal questions, planning, multiple topics
        val complexPatterns = listOf(
            "why do i", "how should i", "what's the best way",
            "help me understand", "explain how", "detailed",
            "personalized", "custom plan", "specifically for me",
            "analyze", "struggling with", "pattern", "trend"
        )
        if (complexPatterns.any { lowerMessage.contains(it) } || wordCount > 30) {
            return TaskComplexity.COMPLEX
        }

        // MODERATE: Standard coaching questions (default for most messages)
        return TaskComplexity.MODERATE
    }

    /**
     * Get recommended AI model based on complexity and plan type
     */
    private fun getRecommendedModel(complexity: TaskComplexity, isPremium: Boolean): AIModelUsed {
        // FREE tier: Always use free models
        if (!isPremium) {
            return AIModelUsed.QWEN_0_5B  // SLM for all non-decision-tree cases
        }

        // PREMIUM tier: Route based on complexity
        return when (complexity) {
            TaskComplexity.SIMPLE -> AIModelUsed.QWEN_0_5B      // Free, fast
            TaskComplexity.MODERATE -> AIModelUsed.CLAUDE_HAIKU  // $1/$5 MTok
            TaskComplexity.COMPLEX -> AIModelUsed.CLAUDE_SONNET  // $3/$15 MTok
            TaskComplexity.HEAVY -> AIModelUsed.CLAUDE_OPUS      // $15/$75 MTok
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
        persistSessions()
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
        persistSessions()
    }

    override suspend fun storePendingScanHandoff(handoff: ScanToCoachHandoff) {
        dataStoreManager.putString(
            AI_PENDING_SCAN_HANDOFF_KEY,
            persistJson.encodeToString(handoff)
        )
    }

    override suspend fun consumePendingScanHandoff(): ScanToCoachHandoff? {
        val raw = dataStoreManager.getString(AI_PENDING_SCAN_HANDOFF_KEY).first() ?: return null
        val parsed = runCatching {
            persistJson.decodeFromString<ScanToCoachHandoff>(raw)
        }.getOrNull()
        dataStoreManager.remove(AI_PENDING_SCAN_HANDOFF_KEY)
        return parsed
    }

    override suspend fun addScanContinuationMessage(
        sessionId: String,
        handoff: ScanToCoachHandoff
    ): CoachingMessage {
        val now = Clock.System.now().toString()
        val continuation = CoachingMessage(
            id = "msg_scan_link_${System.currentTimeMillis()}",
            role = MessageRole.COACH,
            content = buildScanContinuationMessage(handoff),
            timestamp = now,
            modelUsed = AIModelUsed.DECISION_TREE,
            suggestions = listOf(
                "Optimize next meal",
                "Balance today",
                "Create plan"
            )
        )

        _sessions.value = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(messages = session.messages + continuation)
            } else session
        }
        persistSessions()
        return continuation
    }

    private fun buildScanContinuationMessage(handoff: ScanToCoachHandoff): String {
        val qualityHint = when {
            handoff.confidencePercent >= 85 -> "Strong scan confidence."
            handoff.confidencePercent >= 65 -> "Good scan confidence."
            else -> "Lower scan confidence, so keep portions adjustable."
        }

        val nextMove = when {
            handoff.protein < 20 ->
                "Next move: add a lean protein source in your next meal."
            handoff.calories > 850 ->
                "Next move: keep the next meal lighter and high in fiber."
            handoff.carbs > handoff.protein * 3 ->
                "Next move: pair your next meal with protein to stabilize energy."
            else ->
                "Next move: repeat this balance and keep your streak alive."
        }

        return buildString {
            append("Nice scan. I logged your context: ${handoff.mealName} (${handoff.calories} cal, P${handoff.protein}/C${handoff.carbs}/F${handoff.fat}) from ${handoff.source}.")
            append("\n\n")
            append(qualityHint)
            append(" ")
            append(nextMove)
            append("\n\n")
            append("Want me to build a one-step plan for the rest of today?")
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

        // Weekly report AI generation is premium-only.
        val isPremium = hasCloudEntitlement()

        val aiSummary: String? = if (isPremium) {
            val userContext = buildUserContext()
            val coachPersonality = buildCoachPersonality()

            guardedCloudCall {
                claudeApiClient.getCoachingResponse(
                    "Generate a brief weekly summary for a habit tracker user. " +
                    "Their completion rate was ${(completionRate * 100).toInt()}%. " +
                    "Strongest habit: ${strongestHabit?.first?.name ?: "None"} (${((strongestHabit?.second ?: 0f) * 100).toInt()}%). " +
                    "Weakest habit: ${weakestHabit?.first?.name ?: "None"} (${((weakestHabit?.second ?: 0f) * 100).toInt()}%). " +
                    "Provide: 1) top win, 2) growth area, 3) pattern observation, 4) focus for next week. Keep each point to 1 sentence.",
                    coachPersonality,
                    userContext,
                    AIModelUsed.CLAUDE_OPUS
                ).getOrNull()
            }
        } else {
            null
        }

        if (aiSummary != null) {
            val estimatedInputTokens = 420
            val estimatedOutputTokens = (aiSummary.length / 4).coerceAtLeast(180)
            trackTokenUsageWithModel(
                inputTokens = estimatedInputTokens,
                outputTokens = estimatedOutputTokens,
                model = AIModelUsed.CLAUDE_OPUS,
                category = "WEEKLY_SUMMARY"
            )
        }

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
            patternDiscovered = aiSummary ?: run {
                val strongName = strongestHabit?.first?.name
                val rate = (completionRate * 100).toInt()
                when {
                    strongName != null && rate > 70 -> "Your $strongName habit is your strongest anchor at ${rate}% completion"
                    rate > 50 -> "You complete ${rate}% of your habits on average"
                    else -> "Building consistency \u2014 focus on one habit at a time"
                }
            },
            nextWeekFocus = weakestHabit?.let { "Try focusing on ${it.first.name} this week" } ?: "Maintain your current habits",
            personalizedAdvice = listOf(
                strongestHabit?.let { "Your ${it.first.name} habit is your anchor - keep it strong!" } ?: "Build one strong habit first",
                "Consistency matters more than perfection",
                if (completionRate > 0.7f) "You're doing great! Small improvements compound over time" else "Focus on completing just one habit each day to build momentum"
            )
        )
    }

    override suspend fun getMotivationBoost(): String {
        if (!checkAIAvailability().canUseCloudAI) {
            return "Cloud coach is unavailable right now."
        }

        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()
        return guardedCloudCall {
            claudeApiClient.getCoachingResponse(
                "Give me a quick, energizing motivation boost in 1-2 sentences.",
                coachPersonality,
                userContext,
                AIModelUsed.CLAUDE_HAIKU
            ).getOrNull()
        } ?: "Cloud coach is temporarily unavailable. Try again in a moment."
    }

    override suspend fun getRecoveryMessage(habitId: String): String {
        if (!checkAIAvailability().canUseCloudAI) {
            return "Cloud coach is unavailable right now."
        }

        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()
        return guardedCloudCall {
            claudeApiClient.getCoachingResponse(
                "Help me recover from breaking my streak on the '$habitId' habit. Be supportive and give practical advice.",
                coachPersonality,
                userContext,
                AIModelUsed.CLAUDE_HAIKU
            ).getOrNull()
        } ?: "Cloud coach is temporarily unavailable. Try again in a moment."
    }

    override suspend fun getCelebrationMessage(habitId: String, completionCount: Int): String {
        if (!checkAIAvailability().canUseCloudAI) {
            return "Cloud coach is unavailable right now."
        }

        val userContext = buildUserContext()
        val coachPersonality = buildCoachPersonality()
        return guardedCloudCall {
            claudeApiClient.getCoachingResponse(
                "Celebrate my achievement! I've completed the '$habitId' habit $completionCount times!",
                coachPersonality,
                userContext,
                AIModelUsed.CLAUDE_HAIKU
            ).getOrNull()
        } ?: "Cloud coach is temporarily unavailable. Try again in a moment."
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

    // ==========================================
    // AI COST CONTROL IMPLEMENTATION
    // ==========================================

    override fun getAIUsage(): Flow<UserAIUsage> = _aiUsage

    override suspend fun checkAIAvailability(): AIUsageCheckResult {
        val usage = _aiUsage.value
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        // Check if we need to reset (new month)
        if (today >= usage.resetDate) {
            resetMonthlyUsage()
        }

        val entitlementState = resolveEntitlementState()
        val effectiveCostRemainingUsd = (entitlementState.hardCapUsd - usage.currentMonthCostUsd).coerceAtLeast(0f)
        val effectivePercentRemaining = if (entitlementState.hardCapUsd > 0f) {
            (100f - ((usage.currentMonthCostUsd / entitlementState.hardCapUsd) * 100f).coerceIn(0f, 100f))
                .coerceAtLeast(0f)
        } else {
            0f
        }

        // =====================================================
        // PRIMARY CHECK: Plan entitlement (FREE tier = SLM only)
        // FREE users NEVER get Claude API - always SLM
        // =====================================================
        if (entitlementState.effectivePlanType == AIPlanType.FREE) {
            return AIUsageCheckResult(
                canUseAI = true,  // Can use AI (SLM)
                canUseCloudAI = false,  // NEVER Claude API for free tier
                recommendedModel = AIModelUsed.QWEN_0_5B,  // Always SLM
                reason = AIUsageBlockReason.NOT_PREMIUM,
                tokensRemaining = usage.tokensRemaining,
                costRemainingUsd = effectiveCostRemainingUsd,
                percentRemaining = 100f,  // SLM is unlimited
                isAtSoftCap = false,
                isAtHardCap = false,
                upgradeMessage = "Upgrade to Premium for personalized AI coaching!",
                slmFallbackMessage = "Using on-device AI. Upgrade for cloud AI coaching!"
            )
        }

        // =====================================================
        // PREMIUM USERS: Check USD-based caps
        // =====================================================

        // Hard cap check (SLM fallback for premium)
        val isAtHardCap = usage.currentMonthCostUsd >= entitlementState.hardCapUsd
        if (isAtHardCap) {
            return AIUsageCheckResult(
                canUseAI = true,  // Can still use AI (just SLM)
                canUseCloudAI = false,  // Cannot use Claude API
                recommendedModel = AIModelUsed.QWEN_0_5B,  // Use on-device SLM
                reason = AIUsageBlockReason.HARD_CAP_REACHED,
                tokensRemaining = usage.tokensRemaining,
                costRemainingUsd = 0f,
                percentRemaining = 0f,
                isAtSoftCap = true,
                isAtHardCap = true,
                upgradeMessage = null,
                slmFallbackMessage = "Using on-device AI until your credits reset on ${usage.resetDate}"
            )
        }

        // Soft cap check (warning for premium)
        val isAtSoftCap = usage.currentMonthCostUsd >= entitlementState.softCapUsd
        if (isAtSoftCap) {
            return AIUsageCheckResult(
                canUseAI = true,
                canUseCloudAI = true,  // Still can use Claude, but warn user
                recommendedModel = AIModelUsed.CLAUDE_HAIKU,
                reason = AIUsageBlockReason.SOFT_CAP_REACHED,
                tokensRemaining = usage.tokensRemaining,
                costRemainingUsd = effectiveCostRemainingUsd,
                percentRemaining = effectivePercentRemaining,
                isAtSoftCap = true,
                isAtHardCap = false,
                upgradeMessage = "Approaching limit - ${String.format("$%.2f", effectiveCostRemainingUsd)} remaining"
            )
        }

        // Legacy token limit check (backwards compatibility)
        if (usage.isAtLimit) {
            return AIUsageCheckResult(
                canUseAI = true,
                canUseCloudAI = false,
                recommendedModel = AIModelUsed.QWEN_0_5B,
                reason = AIUsageBlockReason.CREDITS_DEPLETED,
                tokensRemaining = 0,
                costRemainingUsd = effectiveCostRemainingUsd,
                percentRemaining = 0f,
                isAtHardCap = true,
                upgradeMessage = "Your AI credits will reset on ${usage.resetDate}"
            )
        }

        // Daily limit check
        val maxDaily = if (entitlementState.isTrialActive) {
            AIGovernancePolicy.intentQuotasFor(
                planType = entitlementState.effectivePlanType,
                isTrial = true
            ).coachMessagesPerDay
        } else {
            entitlementState.effectivePlanType.maxMessagesPerDay
        }
        if (_dailyMessageCount.value >= maxDaily) {
            return AIUsageCheckResult(
                canUseAI = false,
                canUseCloudAI = false,
                recommendedModel = AIModelUsed.DECISION_TREE,
                reason = AIUsageBlockReason.DAILY_LIMIT_REACHED,
                tokensRemaining = usage.tokensRemaining,
                costRemainingUsd = effectiveCostRemainingUsd,
                percentRemaining = effectivePercentRemaining,
                upgradeMessage = "Daily limit reached. Come back tomorrow!"
            )
        }

        // All good - premium user can use full cloud AI with intelligent routing
        return AIUsageCheckResult(
            canUseAI = true,
            canUseCloudAI = true,
            recommendedModel = AIModelUsed.CLAUDE_HAIKU,  // Default, actual routing in sendMessage
            reason = null,
            tokensRemaining = usage.tokensRemaining,
            costRemainingUsd = effectiveCostRemainingUsd,
            percentRemaining = effectivePercentRemaining,
            upgradeMessage = null
        )
    }

    override suspend fun trackTokenUsage(inputTokens: Int, outputTokens: Int, usedDecisionTree: Boolean) {
        if (usedDecisionTree) return  // No cost for decision tree

        trackTokenUsageWithModel(inputTokens, outputTokens, AIModelUsed.CLAUDE_HAIKU)
    }

    override suspend fun canSpendCloudCost(rawCostUsd: Float): Boolean {
        val entitlementState = resolveEntitlementState()
        if (entitlementState.effectivePlanType == AIPlanType.FREE) return false

        val usage = _aiUsage.value
        val charged = rawCostUsd * AIGovernancePolicy.INTERNAL_COST_MULTIPLIER
        return usage.currentMonthCostUsd + charged <= entitlementState.hardCapUsd
    }

    override suspend fun trackExternalCloudUsage(
        inputTokens: Int,
        outputTokens: Int,
        model: AIModelUsed,
        category: String
    ) {
        trackTokenUsageWithModel(inputTokens, outputTokens, model, category)
    }

    private enum class CloudUsageBucket {
        CHAT,
        SCAN,
        REPORT
    }

    private fun cloudUsageBucketFor(category: String): CloudUsageBucket {
        val normalized = category.trim().uppercase()
        return when {
            normalized.startsWith("FOOD_SCAN") -> CloudUsageBucket.SCAN
            normalized == "WEEKLY_SUMMARY" -> CloudUsageBucket.REPORT
            normalized.startsWith("SCHEDULED_") && normalized.contains("REPORT") -> CloudUsageBucket.REPORT
            else -> CloudUsageBucket.CHAT
        }
    }

    /**
     * Track token usage with specific model for accurate cost calculation
     */
    private fun trackTokenUsageWithModel(
        inputTokens: Int,
        outputTokens: Int,
        model: AIModelUsed,
        category: String = "AI_RESPONSE"
    ): Float {
        if (model.isFree) return 0f // No cost for decision tree or SLM

        val totalTokens = inputTokens + outputTokens
        val current = _aiUsage.value
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val usageBucket = cloudUsageBucketFor(category)

        val cost = AIGovernancePolicy.chargedCloudCostUsd(model, inputTokens, outputTokens)

        _aiUsage.value = current.copy(
            tokensUsed = current.tokensUsed + totalTokens,
            aiMessagesCount = current.aiMessagesCount + if (usageBucket == CloudUsageBucket.CHAT) 0 else 1,
            cloudScanCalls = current.cloudScanCalls + if (usageBucket == CloudUsageBucket.SCAN) 1 else 0,
            cloudReportCalls = current.cloudReportCalls + if (usageBucket == CloudUsageBucket.REPORT) 1 else 0,
            currentMonthCostUsd = current.currentMonthCostUsd + cost,
            lastUpdated = today
        )
        persistAiUsage()

        // Record interaction for analytics
        val interaction = AIInteraction(
            id = "int_${System.currentTimeMillis()}",
            userId = current.userId,
            timestamp = Clock.System.now().toString(),
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            totalTokens = totalTokens,
            modelUsed = model,
            responseCategory = category
        )
        _interactions.value = _interactions.value + interaction
        return cost
    }

    override suspend fun updatePlanType(planType: AIPlanType) {
        val current = _aiUsage.value
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        _aiUsage.value = current.copy(
            planType = planType,
            monthlyTokenLimit = planType.monthlyTokenLimit,
            lastUpdated = today
        )
        persistAiUsage()
    }

    override suspend fun getMonthlyUsageReport(): MonthlyAIUsageReport? {
        val usage = _aiUsage.value
        val interactions = _interactions.value
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val month = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}"
        val entitlementState = resolveEntitlementState()
        val totalOperations = usage.localMessagesCount + usage.cloudTotalCalls

        if (totalOperations == 0) return null

        // Calculate estimated cost
        val estimatedCost = interactions
            .filter { !it.usedDecisionTree }
            .sumOf { it.estimatedCostUsd.toDouble() }
            .toFloat()

        // Calculate local handling efficiency against all wallet operations.
        val efficiency = if (totalOperations > 0) {
            (usage.localMessagesCount.toFloat() / totalOperations * 100)
        } else 0f

        return MonthlyAIUsageReport(
            userId = usage.userId,
            month = month,
            planType = entitlementState.effectivePlanType,
            totalMessages = totalOperations,
            freeMessages = usage.localMessagesCount,
            paidMessages = usage.cloudTotalCalls,
            totalTokens = usage.tokensUsed,
            tokenLimit = usage.monthlyTokenLimit,
            estimatedCostUsd = estimatedCost,
            efficiencyPercent = efficiency,
            peakUsageDay = null,
            mostUsedCategory = null,
            dailyBreakdown = emptyList()
        )
    }

    override suspend fun getRecentAIInteractions(limit: Int): List<AIInteraction> {
        return _interactions.value
            .sortedByDescending { it.timestamp }
            .take(limit.coerceAtLeast(1))
    }

    override suspend fun getRoutingIntentStats(limit: Int): List<AIRoutingIntentStat> {
        val analytics = aiFeaturePersistence?.getIntentAnalytics()
            ?.takeIf { it.isNotEmpty() }
            ?: AIRoutingEngine.ABTestHook.analyzeIntentPerformance()

        return analytics
            .sortedByDescending { it.callCount }
            .take(limit.coerceAtLeast(1))
            .map { stat ->
                AIRoutingIntentStat(
                    intent = stat.intent.name,
                    callCount = stat.callCount,
                    avgResponseTimeMs = stat.avgResponseTimeMs,
                    avgCostUsd = stat.avgCost
                )
            }
    }

    override suspend fun resetMonthlyUsage() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val nextMonth = LocalDate(
            year = if (today.monthNumber == 12) today.year + 1 else today.year,
            monthNumber = if (today.monthNumber == 12) 1 else today.monthNumber + 1,
            dayOfMonth = 1
        )

        val current = _aiUsage.value
        _aiUsage.value = current.copy(
            tokensUsed = 0,
            messagesCount = 0,
            freeMessagesCount = 0,
            slmMessagesCount = 0,
            aiMessagesCount = 0,
            cloudChatCalls = 0,
            cloudScanCalls = 0,
            cloudReportCalls = 0,
            resetDate = nextMonth.toString(),
            lastUpdated = today.toString()
        )

        _dailyMessageCount.value = 0
        _interactions.value = emptyList()
        persistAiUsage()
        persistDailyMessageCount()
    }

    // ==========================================
    // AI COACH MEMORY - Feature #8 Implementation
    // ==========================================

    private val _conversationMemories = MutableStateFlow<Map<String, List<ConversationMemory>>>(emptyMap())
    private val _userPreferences = MutableStateFlow<Map<String, UserPreferences>>(emptyMap())
    private val _insights = MutableStateFlow<List<ConversationInsight>>(emptyList())

    override suspend fun storeConversationMemory(memory: ConversationMemory) {
        val current = _conversationMemories.value
        val userMemories = current[memory.userId] ?: emptyList()
        _conversationMemories.value = current + (memory.userId to (userMemories + memory))
        persistConversationMemories()
    }

    override suspend fun getConversationMemories(userId: String, limit: Int): List<ConversationMemory> {
        return _conversationMemories.value[userId]
            ?.sortedByDescending { it.timestamp }
            ?.take(limit)
            ?: emptyList()
    }

    override suspend fun getUserPreferences(userId: String): UserPreferences {
        return _userPreferences.value[userId] ?: UserPreferences(
            userId = userId,
            preferredCoachId = null,
            communicationStyle = CommunicationStyle.BALANCED,
            topicsOfInterest = emptyList(),
            avoidTopics = emptyList(),
            preferredTime = null,
            motivationalApproach = MotivationalApproach.SUPPORTIVE,
            lastUpdated = Clock.System.now()
        )
    }

    override suspend fun updateUserPreferences(preferences: UserPreferences) {
        val current = _userPreferences.value
        _userPreferences.value = current + (preferences.userId to preferences)
    }

    override suspend fun getUserContext(userId: String): UserContext {
        val streakInfo = entryRepository.getStreakInfo().first()
        val enabledHabits = habitRepository.getEnabledHabits().first()

        // Get recent moods from last 7 days (simplified - collect from available entries)
        val recentMoods = mutableListOf<MoodLevel>()
        try {
            // Get today's entry for mood
            val todayEntry = entryRepository.getTodayEntry().first()
            todayEntry?.mood?.let { recentMoods.add(it) }
        } catch (e: Exception) {
            // Mood tracking not available
        }

        val memories = getConversationMemories(userId, limit = 10)

        // Calculate total completions from streak data
        val totalCompletions = streakInfo.currentStreak * enabledHabits.size

        // Extract recent challenges from conversation memories
        val recentChallenges = memories
            .flatMap { it.userChallenges }
            .distinct()
            .take(3)

        // Get life situation from onboarding goal
        val userSettings = settingsRepository?.getSettingsSnapshot()
        val lifeSituation = userSettings?.onboardingGoal

        return UserContext(
            userId = userId,
            recentHabits = enabledHabits.map { it.name },
            currentStreak = streakInfo.currentStreak,
            totalCompletions = totalCompletions,
            recentChallenges = recentChallenges,
            recentMoods = recentMoods,
            lifeSituation = lifeSituation,
            lastConversationDate = memories.firstOrNull()?.timestamp,
            conversationCount = memories.size
        )
    }

    override suspend fun storeInsight(insight: ConversationInsight) {
        _insights.value = _insights.value + insight
    }

    override suspend fun getRelevantInsights(userId: String, topic: String, limit: Int): List<ConversationInsight> {
        return _insights.value
            .filter { it.userId == userId }
            .filter { insight ->
                // Simple keyword matching for relevance
                insight.insight.contains(topic, ignoreCase = true) ||
                insight.category.name.contains(topic, ignoreCase = true)
            }
            .sortedByDescending { it.confidence }
            .take(limit)
    }

    override suspend fun buildMemoryContext(query: MemoryQuery): MemoryContext {
        val memories = getConversationMemories(query.userId, query.maxMemories)
        val preferences = getUserPreferences(query.userId)
        val context = getUserContext(query.userId)
        val insights = getRelevantInsights(query.userId, query.currentTopic, 3)

        return MemoryContext(
            recentConversations = memories,
            userPreferences = preferences,
            userContext = context,
            relevantInsights = insights
        )
    }

    // =========================================================================
    // FEATURE INTEGRATIONS: Advanced AI Cost Control & Retention
    // =========================================================================

    /**
     * Schedule insights for a new user (call after onboarding)
     * Feature: Insight Scheduler - NOW PERSISTED TO ROOM DATABASE
     */
    override suspend fun scheduleNewUserInsights(userId: String) {
        val signupTimestamp = Clock.System.now().toEpochMilliseconds()
        // Use persistence layer (Room DB) instead of in-memory storage
        aiFeaturePersistence?.scheduleInsightsForNewUser(userId, signupTimestamp)
            ?: AIRoutingEngine.InsightScheduler.scheduleForNewUser(userId, signupTimestamp)
    }

    /**
     * Check for and deliver any due proactive insights
     * Call this on app launch or in a background check
     * Feature: Insight Scheduler - NOW PERSISTED TO ROOM DATABASE
     */
    suspend fun checkAndDeliverDueInsights(userId: String): AIRoutingEngine.InsightScheduler.ScheduledInsight? {
        val entitlementState = resolveEntitlementState()

        // Use persistence layer (Room DB) instead of in-memory storage
        val dueInsights = aiFeaturePersistence?.getDueInsights(userId)
            ?: AIRoutingEngine.InsightScheduler.getDueInsights(userId)

        // Return first eligible insight
        for (insight in dueInsights) {
            if (!AIRoutingEngine.InsightScheduler.isEligibleForInsight(insight.milestone, entitlementState.effectivePlanType)) {
                continue
            }

            // Build context for the insight
            val contextSummary = buildCachedContextSummary(userId)
            val settings = settingsRepository?.getSettingsSnapshot()
            val userName = settings?.displayName ?: settings?.userName ?: "there"

            val prompt = AIRoutingEngine.InsightScheduler.getInsightPrompt(
                milestone = insight.milestone,
                userName = userName,
                contextSummary = contextSummary
            )

            // Generate the insight using appropriate model (premium-only cloud).
            val model = insight.milestone.model
            val isPremium = hasCloudEntitlement()

            val content = if (isPremium && !model.isFree) {
                val userContext = buildUserContext()
                val coachPersonality = buildCoachPersonality()
                guardedCloudCall {
                    claudeApiClient.getCoachingResponse(prompt, coachPersonality, userContext, model).getOrNull()
                }
                } else {
                // Use SLM or template for free users
                generateSLMFallbackResponse(prompt)
            }

            if (content != null) {
                // Persist to Room DB
                aiFeaturePersistence?.markInsightGenerated(userId, insight.milestone, content)
                    ?: AIRoutingEngine.InsightScheduler.markGenerated(userId, insight.milestone, content)
                return insight.copy(content = content)
            }
        }

        return null
    }

    /**
     * Build a cached context summary for token-efficient prompts
     * Feature: Cached Context Summaries - NOW PERSISTED TO ROOM DATABASE
     */
    private suspend fun buildCachedContextSummary(userId: String): String {
        // Check cache first - use persistence layer (Room DB)
        val cached = aiFeaturePersistence?.getCachedContext(userId)
            ?: AIRoutingEngine.ContextCache.getCachedContext(userId)
        if (cached != null) {
            return cached.toCondensedPrompt()
        }

        // Build new cache from real data
        val dailySummaries = mutableListOf<AIRoutingEngine.ContextCache.DailySummary>()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // Collect last 14 days of data using real per-habit completion
        val enabledHabits = habitRepository.getEnabledHabits().first()
        for (i in 0 until 14) {
            try {
                val weekData = entryRepository.getWeekData(i / 7).first()
                val dayIndex = i % 7
                if (dayIndex < weekData.days.size) {
                    val dayData = weekData.days[dayIndex]

                    // Get actual per-habit completion from DailyEntry
                    val dayEntry = try {
                        entryRepository.getEntryForDate(dayData.date).first()
                    } catch (_: Exception) { null }

                    val completedHabits = if (dayEntry != null) {
                        enabledHabits.filter { habit ->
                            dayEntry.completions[habit.id] == true
                        }.map { it.name }
                    } else emptyList()

                    val missedHabits = if (dayEntry != null) {
                        enabledHabits.filter { habit ->
                            dayEntry.completions.containsKey(habit.id) && dayEntry.completions[habit.id] != true
                        }.map { it.name }
                    } else emptyList()

                    val completionRate = if (dayData.totalCount > 0) {
                        dayData.completedCount.toFloat() / dayData.totalCount
                    } else 0f

                    dailySummaries.add(
                        AIRoutingEngine.ContextCache.DailySummary(
                            date = dayData.date,
                            userId = userId,
                            habitCompletionRate = completionRate,
                            completedHabits = completedHabits.take(3),
                            missedHabits = missedHabits.take(2),
                            mood = dayEntry?.mood?.name,
                            sleepHours = null,
                            sleepQuality = null,
                            nutritionScore = null,
                            workoutMinutes = null,
                            energyLevel = null,
                            notes = null
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip days with errors
            }
        }

        // Build and cache the context - persist to Room DB
        val newContext = AIRoutingEngine.ContextCache.buildCachedContext(userId, dailySummaries)
        aiFeaturePersistence?.cacheContext(newContext)
            ?: AIRoutingEngine.ContextCache.cacheContext(newContext)

        return newContext.toCondensedPrompt()
    }

    /**
     * Check for ready Opus-generated weekly report
     * Feature: Opus Scheduled Generation - NOW PERSISTED TO ROOM DATABASE
     */
    suspend fun getReadyWeeklyReport(userId: String): AIRoutingEngine.OpusScheduler.ScheduledReport? {
        return aiFeaturePersistence?.getReadyReportForUser(userId)
            ?: AIRoutingEngine.OpusScheduler.getReadyReportForUser(userId)
    }

    /**
     * Schedule weekly report generation (call this from WorkManager)
     * Feature: Opus Scheduled Generation - NOW PERSISTED TO ROOM DATABASE
     */
    suspend fun scheduleWeeklyReportGeneration(userId: String): AIRoutingEngine.OpusScheduler.ScheduledReport? {
        val entitlementState = resolveEntitlementState()
        return aiFeaturePersistence?.scheduleWeeklyReport(userId, entitlementState.effectivePlanType)
            ?: AIRoutingEngine.OpusScheduler.scheduleWeeklyReport(userId, entitlementState.effectivePlanType)
    }

    /**
     * Generate scheduled reports (call from background worker at 2 AM Sunday)
     * Feature: Opus Scheduled Generation - NOW PERSISTED TO ROOM DATABASE
     */
    suspend fun generateDueReports() {
        val dueReports = aiFeaturePersistence?.getDueReports()
            ?: AIRoutingEngine.OpusScheduler.getDueReports()

        for (report in dueReports) {
            aiFeaturePersistence?.markReportGenerating(report.reportId)
                ?: AIRoutingEngine.OpusScheduler.markGenerating(report.reportId)

            try {
                // Get user context for the report
                val contextSummary = buildCachedContextSummary(report.userId)
                val reportSettings = settingsRepository?.getSettingsSnapshot()
                val userName = reportSettings?.displayName ?: reportSettings?.userName ?: "there"

                val prompt = AIRoutingEngine.OpusScheduler.getWeeklyReportPrompt(userName, contextSummary)

                val promptContract = AIRoutingEngine.getPromptContract(
                    AIModelUsed.CLAUDE_OPUS,
                    AIRoutingEngine.BudgetMode.NORMAL
                )
                val reportPrompt = if (promptContract.isNotBlank()) {
                    "$prompt\n\n$promptContract"
                } else {
                    prompt
                }

                // Guard weekly report generation by wallet before making cloud call.
                val estimatedInputTokens = (prompt.length / 4).coerceAtLeast(400)
                val estimatedOutputTokens = 1200
                val estimatedRawCost = AIGovernancePolicy.rawCloudCostUsd(
                    model = AIModelUsed.CLAUDE_OPUS,
                    inputTokens = estimatedInputTokens,
                    outputTokens = estimatedOutputTokens
                )
                if (!canSpendCloudCost(estimatedRawCost)) {
                    aiFeaturePersistence?.markReportFailed(report.reportId)
                    continue
                }

                // Generate with Opus (rate-limited when no SLM)
                val userContext = buildUserContext()
                val coachPersonality = buildCoachPersonality()
                val content = guardedCloudCall {
                    claudeApiClient.getCoachingResponse(
                        reportPrompt,
                        coachPersonality,
                        userContext,
                        AIModelUsed.CLAUDE_OPUS
                    ).getOrNull()
                }

                if (content != null) {
                    // Estimate tokens and cost
                    val inputTokens = prompt.length / 4  // Rough estimate
                    val outputTokens = content.length / 4
                    val cost = trackTokenUsageWithModel(
                        inputTokens = inputTokens,
                        outputTokens = outputTokens,
                        model = AIModelUsed.CLAUDE_OPUS,
                        category = "SCHEDULED_WEEKLY_REPORT"
                    )

                    // Store to Room DB
                    aiFeaturePersistence?.storeGeneratedReport(
                        reportId = report.reportId,
                        content = content,
                        tokensCost = inputTokens + outputTokens,
                        costUsd = cost
                    ) ?: AIRoutingEngine.OpusScheduler.storeGeneratedReport(
                        reportId = report.reportId,
                        content = content,
                        tokensCost = inputTokens + outputTokens,
                        costUsd = cost
                    )

                }
            } catch (e: Exception) {
                // Mark as failed in Room DB
                aiFeaturePersistence?.markReportFailed(report.reportId)
            }
        }
    }

    /**
     * Mark weekly report as delivered to user
     * Feature: Opus Scheduled Generation - NOW PERSISTED TO ROOM DATABASE
     */
    suspend fun markWeeklyReportDelivered(reportId: String) {
        aiFeaturePersistence?.markReportDelivered(reportId)
            ?: AIRoutingEngine.OpusScheduler.markDelivered(reportId)
    }

    /**
     * Get localized response for common interactions
     * Feature: Regional SLM
     */
    fun getLocalizedGreeting(): String {
        val language = AIRoutingEngine.RegionalSLM.getCurrentLanguage()
        return AIRoutingEngine.RegionalSLM.LocalizedTemplates.getGreeting(language)
    }

    /**
     * Get localized streak celebration
     * Feature: Regional SLM
     */
    fun getLocalizedStreakCelebration(streak: Int): String {
        val language = AIRoutingEngine.RegionalSLM.getCurrentLanguage()
        return AIRoutingEngine.RegionalSLM.LocalizedTemplates.getStreakCelebration(streak, language)
    }

    /**
     * Get localized encouragement
     * Feature: Regional SLM
     */
    fun getLocalizedEncouragement(): String {
        val language = AIRoutingEngine.RegionalSLM.getCurrentLanguage()
        return AIRoutingEngine.RegionalSLM.LocalizedTemplates.getEncouragement(language)
    }

    /**
     * Get A/B test analytics and recommendations
     * Feature: A/B Test Hook
     */
    suspend fun getABTestAnalytics(): List<AIRoutingEngine.ABTestHook.IntentPerformance> {
        return AIRoutingEngine.ABTestHook.analyzeIntentPerformance()
    }

    /**
     * Get routing optimization recommendations based on A/B test data
     * Feature: A/B Test Hook
     */
    override suspend fun getRoutingRecommendations(): List<String> {
        return AIRoutingEngine.ABTestHook.getRoutingRecommendations()
    }

    /**
     * Invalidate context cache after significant user data changes
     * Feature: Cached Context Summaries - NOW PERSISTED TO ROOM DATABASE
     */
    override suspend fun invalidateContextCache(userId: String) {
        aiFeaturePersistence?.invalidateContextCache(userId)
            ?: AIRoutingEngine.ContextCache.invalidate(userId)
    }

    /**
     * Prune old data from the persistence layer
     * Call this periodically (e.g., weekly) to keep database size manageable
     */
    suspend fun pruneOldData() {
        aiFeaturePersistence?.pruneOldData()
    }
}





