package com.dailywell.app.data.repository

import com.dailywell.app.data.content.ReflectionPromptsDatabase
import com.dailywell.app.data.content.ReflectionPromptsDatabase.ReflectionPrompt
import com.dailywell.app.data.content.ReflectionPromptsDatabase.ReflectionTheme
import com.dailywell.app.data.content.ReflectionPromptsDatabase.JourneyStage
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ReflectionPromptsRepositoryImpl - PRODUCTION-READY Firebase Implementation
 *
 * All reflection data persisted to Firebase Firestore with real-time sync.
 * NO MOCK DATA - Full production implementation.
 *
 * Firestore Collections:
 * - users/{userId}/reflections/{weekId} - Weekly reflection entries
 * - users/{userId}/reflection_responses - Individual response records
 * - users/{userId}/reflection_stats - Aggregated statistics
 */
@OptIn(ExperimentalUuidApi::class)
class ReflectionPromptsRepositoryImpl(
    private val claudeApiClient: com.dailywell.app.api.ClaudeApiClient? = null
) : ReflectionPromptsRepository {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    // Cached data for efficient access
    private val _reflectionHistory = MutableStateFlow<List<WeeklyReflectionEntry>>(emptyList())
    private val _currentWeekPrompts = MutableStateFlow<WeeklyPromptSet?>(null)
    private val _responseCache = MutableStateFlow<Map<String, ReflectionResponse>>(emptyMap())

    private val reflectionsCollection
        get() = firestore.collection("users").document(userId).collection("reflections")

    private val responsesCollection
        get() = firestore.collection("users").document(userId).collection("reflection_responses")

    private val statsDocument
        get() = firestore.collection("users").document(userId).collection("reflection_stats").document("current")

    init {
        loadReflectionHistory()
        loadCurrentWeekPrompts()
    }

    private fun loadReflectionHistory() {
        repositoryScope.launch {
            try {
                reflectionsCollection.snapshots.collect { querySnapshot ->
                    val entries = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            val responsesMap = mutableMapOf<String, ReflectionResponse>()
                            val responsesData = doc.get<Map<String, Map<String, Any>>?>("responses")
                            responsesData?.forEach { (theme, data) ->
                                responsesMap[theme] = ReflectionResponse(
                                    id = data["id"] as? String ?: "",
                                    promptId = data["promptId"] as? String ?: "",
                                    theme = theme,
                                    question = data["question"] as? String ?: "",
                                    response = data["response"] as? String ?: "",
                                    weekNumber = (data["weekNumber"] as? Long)?.toInt() ?: 0,
                                    year = (data["year"] as? Long)?.toInt() ?: 0,
                                    createdAt = Instant.parse(data["createdAt"] as? String ?: Clock.System.now().toString()),
                                    wordCount = (data["wordCount"] as? Long)?.toInt() ?: 0,
                                    sentimentScore = (data["sentimentScore"] as? Double)?.toFloat(),
                                    keyInsights = (data["keyInsights"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                )
                            }

                            WeeklyReflectionEntry(
                                id = doc.id,
                                weekNumber = doc.get<Long>("weekNumber").toInt(),
                                year = doc.get<Long>("year").toInt(),
                                responses = responsesMap,
                                overallMood = doc.get<String?>("overallMood"),
                                createdAt = Instant.parse(doc.get<String>("createdAt")),
                                completedAt = doc.get<String?>("completedAt")?.let { Instant.parse(it) },
                                isComplete = doc.get<Boolean>("isComplete"),
                                completionPercent = doc.get<Double>("completionPercent").toFloat()
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }.sortedByDescending { it.year * 100 + it.weekNumber }

                    _reflectionHistory.value = entries
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadCurrentWeekPrompts() {
        repositoryScope.launch {
            try {
                val now = Clock.System.now()
                val today = now.toLocalDateTime(TimeZone.currentSystemDefault())
                val weekNumber = getWeekOfYear(today)
                val year = today.year

                // Get prompts for current week
                val prompts = getPromptsForWeek(weekNumber)

                // Check for existing responses
                val weekId = "${year}_week_$weekNumber"
                val existingDoc = reflectionsCollection.document(weekId).get()

                val existingResponses = if (existingDoc.exists) {
                    val responsesData = existingDoc.get<Map<String, Map<String, Any>>?>("responses")
                    responsesData?.mapValues { (_, data) ->
                        data["response"] as? String ?: ""
                    } ?: emptyMap()
                } else {
                    emptyMap()
                }

                _currentWeekPrompts.value = WeeklyPromptSet(
                    weekNumber = weekNumber,
                    year = year,
                    prompts = prompts,
                    isCompleted = existingDoc.exists && existingDoc.get<Boolean>("isComplete"),
                    existingResponses = existingResponses
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getPromptsForWeek(weekNumber: Int): List<ReflectionPrompt> {
        // Get one prompt from each theme based on week number for deterministic selection
        return ReflectionTheme.entries.map { theme ->
            ReflectionPromptsDatabase.getPromptForTheme(theme, weekNumber)
        }
    }

    private fun getWeekOfYear(date: LocalDateTime): Int {
        val startOfYear = LocalDate(date.year, 1, 1)
        val dayOfYear = date.date.toEpochDays() - startOfYear.toEpochDays()
        return (dayOfYear / 7) + 1
    }

    // ==================== PROMPT RETRIEVAL ====================

    override fun getCurrentWeekPromptSet(): Flow<WeeklyPromptSet> {
        return _currentWeekPrompts.map { it ?: createEmptyWeekSet() }
    }

    private fun createEmptyWeekSet(): WeeklyPromptSet {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val weekNumber = getWeekOfYear(today)
        return WeeklyPromptSet(
            weekNumber = weekNumber,
            year = today.year,
            prompts = getPromptsForWeek(weekNumber),
            isCompleted = false,
            existingResponses = emptyMap()
        )
    }

    override suspend fun getPromptSetForWeek(weekNumber: Int, year: Int): WeeklyPromptSet {
        val prompts = getPromptsForWeek(weekNumber)
        val weekId = "${year}_week_$weekNumber"

        return try {
            val doc = reflectionsCollection.document(weekId).get()
            if (doc.exists) {
                val responsesData = doc.get<Map<String, Map<String, Any>>?>("responses")
                val existingResponses = responsesData?.mapValues { (_, data) ->
                    data["response"] as? String ?: ""
                } ?: emptyMap()

                WeeklyPromptSet(
                    weekNumber = weekNumber,
                    year = year,
                    prompts = prompts,
                    isCompleted = doc.get<Boolean>("isComplete"),
                    existingResponses = existingResponses
                )
            } else {
                WeeklyPromptSet(
                    weekNumber = weekNumber,
                    year = year,
                    prompts = prompts,
                    isCompleted = false,
                    existingResponses = emptyMap()
                )
            }
        } catch (e: Exception) {
            WeeklyPromptSet(
                weekNumber = weekNumber,
                year = year,
                prompts = prompts,
                isCompleted = false,
                existingResponses = emptyMap()
            )
        }
    }

    override suspend fun getContextualPrompt(
        journeyStage: JourneyStage,
        currentStreak: Int,
        recentBreak: Boolean,
        moodLow: Boolean
    ): ContextualPrompt {
        val prompt = ReflectionPromptsDatabase.getContextualPrompt(
            hasRecentBreak = recentBreak,
            currentStreak = currentStreak,
            moodLow = moodLow
        )

        val reason = when {
            recentBreak -> "Selected to help you process and learn from recent challenges"
            moodLow -> "Gratitude prompts can help shift perspective when feeling low"
            currentStreak < 7 -> "Perfect for someone just starting their journey"
            currentStreak > 30 -> "A deeper prompt for your established practice"
            else -> "Selected based on your current journey stage"
        }

        return ContextualPrompt(
            prompt = prompt,
            relevanceScore = 1.0f,
            reason = reason
        )
    }

    override fun getPromptsByTheme(theme: ReflectionTheme): List<ReflectionPrompt> {
        return ReflectionPromptsDatabase.getPromptsByTheme(theme)
    }

    override fun searchPrompts(query: String): List<ReflectionPrompt> {
        return ReflectionPromptsDatabase.searchPrompts(query)
    }

    // ==================== RESPONSE MANAGEMENT ====================

    override suspend fun saveResponse(
        promptId: String,
        response: String,
        weekNumber: Int,
        year: Int
    ): Result<ReflectionResponse> {
        return try {
            val prompt = ReflectionPromptsDatabase.allPrompts.find { it.id == promptId }
                ?: return Result.failure(Exception("Prompt not found"))

            val now = Clock.System.now()
            val responseId = Uuid.random().toString()
            val wordCount = response.split("\\s+".toRegex()).size

            val reflectionResponse = ReflectionResponse(
                id = responseId,
                promptId = promptId,
                theme = prompt.theme.name,
                question = prompt.question,
                response = response,
                weekNumber = weekNumber,
                year = year,
                createdAt = now,
                wordCount = wordCount
            )

            // Save to responses collection
            val responseData = hashMapOf(
                "id" to responseId,
                "promptId" to promptId,
                "theme" to prompt.theme.name,
                "question" to prompt.question,
                "response" to response,
                "weekNumber" to weekNumber,
                "year" to year,
                "createdAt" to now.toString(),
                "wordCount" to wordCount
            )
            responsesCollection.document(responseId).set(responseData)

            // Update weekly reflection entry
            val weekId = "${year}_week_$weekNumber"
            val weekDoc = reflectionsCollection.document(weekId).get()

            if (weekDoc.exists) {
                // Update existing entry
                val existingResponses = weekDoc.get<Map<String, Any>?>("responses")?.toMutableMap() ?: mutableMapOf()
                existingResponses[prompt.theme.name] = responseData

                val completionPercent = existingResponses.size.toFloat() / 6f
                reflectionsCollection.document(weekId).update(
                    mapOf(
                        "responses" to existingResponses,
                        "completionPercent" to completionPercent,
                        "isComplete" to (existingResponses.size == 6)
                    )
                )
            } else {
                // Create new entry
                val weekData = hashMapOf(
                    "weekNumber" to weekNumber,
                    "year" to year,
                    "responses" to mapOf(prompt.theme.name to responseData),
                    "createdAt" to now.toString(),
                    "completionPercent" to (1f / 6f),
                    "isComplete" to false
                )
                reflectionsCollection.document(weekId).set(weekData)
            }

            // Update stats
            updateStats()

            // Reload current week prompts
            loadCurrentWeekPrompts()

            Result.success(reflectionResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateResponse(
        responseId: String,
        newResponse: String
    ): Result<ReflectionResponse> {
        return try {
            val now = Clock.System.now()
            val wordCount = newResponse.split("\\s+".toRegex()).size

            responsesCollection.document(responseId).update(
                mapOf(
                    "response" to newResponse,
                    "wordCount" to wordCount,
                    "updatedAt" to now.toString()
                )
            )

            val doc = responsesCollection.document(responseId).get()
            val response = ReflectionResponse(
                id = responseId,
                promptId = doc.get<String>("promptId"),
                theme = doc.get<String>("theme"),
                question = doc.get<String>("question"),
                response = newResponse,
                weekNumber = doc.get<Long>("weekNumber").toInt(),
                year = doc.get<Long>("year").toInt(),
                createdAt = Instant.parse(doc.get<String>("createdAt")),
                wordCount = wordCount
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getResponse(promptId: String, weekNumber: Int, year: Int): ReflectionResponse? {
        return try {
            val weekId = "${year}_week_$weekNumber"
            val weekDoc = reflectionsCollection.document(weekId).get()

            if (!weekDoc.exists) return null

            val prompt = ReflectionPromptsDatabase.allPrompts.find { it.id == promptId } ?: return null
            val responsesData = weekDoc.get<Map<String, Map<String, Any>>?>("responses")
            val responseData = responsesData?.get(prompt.theme.name) ?: return null

            ReflectionResponse(
                id = responseData["id"] as? String ?: "",
                promptId = promptId,
                theme = prompt.theme.name,
                question = responseData["question"] as? String ?: "",
                response = responseData["response"] as? String ?: "",
                weekNumber = weekNumber,
                year = year,
                createdAt = Instant.parse(responseData["createdAt"] as? String ?: Clock.System.now().toString()),
                wordCount = (responseData["wordCount"] as? Long)?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getWeeklyResponses(weekNumber: Int, year: Int): List<ReflectionResponse> {
        return try {
            val weekId = "${year}_week_$weekNumber"
            val weekDoc = reflectionsCollection.document(weekId).get()

            if (!weekDoc.exists) return emptyList()

            val responsesData = weekDoc.get<Map<String, Map<String, Any>>?>("responses") ?: return emptyList()

            responsesData.map { (theme, data) ->
                ReflectionResponse(
                    id = data["id"] as? String ?: "",
                    promptId = data["promptId"] as? String ?: "",
                    theme = theme,
                    question = data["question"] as? String ?: "",
                    response = data["response"] as? String ?: "",
                    weekNumber = weekNumber,
                    year = year,
                    createdAt = Instant.parse(data["createdAt"] as? String ?: Clock.System.now().toString()),
                    wordCount = (data["wordCount"] as? Long)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun completeWeeklyReflection(weekNumber: Int, year: Int): Result<Unit> {
        return try {
            val weekId = "${year}_week_$weekNumber"
            val now = Clock.System.now()

            reflectionsCollection.document(weekId).update(
                mapOf(
                    "isComplete" to true,
                    "completedAt" to now.toString(),
                    "completionPercent" to 1.0f
                )
            )

            updateStats()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== HISTORY & ANALYTICS ====================

    override fun getReflectionHistory(): Flow<List<WeeklyReflectionEntry>> = _reflectionHistory

    override suspend fun getReflectionStats(): ReflectionStats {
        val history = _reflectionHistory.value
        val allResponses = history.flatMap { it.responses.values }

        val totalReflections = history.count { it.isComplete }
        val totalResponses = allResponses.size
        val totalWords = allResponses.sumOf { it.wordCount }
        val avgWords = if (totalResponses > 0) totalWords.toFloat() / totalResponses else 0f

        // Calculate streak
        val streak = calculateReflectionStreak(history)
        val longestStreak = calculateLongestStreak(history)

        // Find favorite theme
        val themeCounts = allResponses.groupBy { it.theme }.mapValues { it.value.size }
        val favoriteTheme = themeCounts.maxByOrNull { it.value }?.key

        // Weekly trend (last 4 weeks)
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val currentWeek = getWeekOfYear(today)
        val weeklyTrend = (0..3).map { offset ->
            val week = currentWeek - offset
            val entry = history.find { it.weekNumber == week && it.year == today.year }
            ((entry?.completionPercent ?: 0f) * 100).toInt()
        }.reversed()

        return ReflectionStats(
            totalReflections = totalReflections,
            totalResponses = totalResponses,
            totalWords = totalWords,
            averageWordsPerResponse = avgWords,
            reflectionStreak = streak,
            longestStreak = longestStreak,
            favoriteTheme = favoriteTheme,
            themeCounts = themeCounts,
            weeklyTrend = weeklyTrend,
            lastReflectionDate = history.firstOrNull()?.completedAt
        )
    }

    private fun calculateReflectionStreak(history: List<WeeklyReflectionEntry>): Int {
        if (history.isEmpty()) return 0

        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val currentWeek = getWeekOfYear(today)
        val currentYear = today.year

        var streak = 0
        var checkWeek = currentWeek
        var checkYear = currentYear

        while (true) {
            val entry = history.find { it.weekNumber == checkWeek && it.year == checkYear && it.isComplete }
            if (entry != null) {
                streak++
                checkWeek--
                if (checkWeek < 1) {
                    checkWeek = 52
                    checkYear--
                }
            } else {
                // Allow current week to be incomplete
                if (checkWeek == currentWeek && checkYear == currentYear) {
                    checkWeek--
                    if (checkWeek < 1) {
                        checkWeek = 52
                        checkYear--
                    }
                } else {
                    break
                }
            }
        }

        return streak
    }

    private fun calculateLongestStreak(history: List<WeeklyReflectionEntry>): Int {
        if (history.isEmpty()) return 0

        val sortedHistory = history
            .filter { it.isComplete }
            .sortedWith(compareBy({ it.year }, { it.weekNumber }))

        var maxStreak = 0
        var currentStreak = 0
        var prevWeek = -1
        var prevYear = -1

        for (entry in sortedHistory) {
            if (prevYear == -1 || isConsecutiveWeek(prevYear, prevWeek, entry.year, entry.weekNumber)) {
                currentStreak++
            } else {
                maxStreak = maxOf(maxStreak, currentStreak)
                currentStreak = 1
            }
            prevWeek = entry.weekNumber
            prevYear = entry.year
        }

        return maxOf(maxStreak, currentStreak)
    }

    private fun isConsecutiveWeek(year1: Int, week1: Int, year2: Int, week2: Int): Boolean {
        return when {
            year1 == year2 -> week2 == week1 + 1
            year2 == year1 + 1 && week1 == 52 && week2 == 1 -> true
            else -> false
        }
    }

    override suspend fun getReflectionStreak(): Int {
        return calculateReflectionStreak(_reflectionHistory.value)
    }

    override suspend fun isCurrentWeekComplete(): Boolean {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val currentWeek = getWeekOfYear(today)

        return _reflectionHistory.value.any {
            it.weekNumber == currentWeek && it.year == today.year && it.isComplete
        }
    }

    override suspend fun getResponsesByTheme(theme: ReflectionTheme): List<ReflectionResponse> {
        return _reflectionHistory.value
            .flatMap { it.responses.values }
            .filter { it.theme == theme.name }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun exportReflections(): String {
        val history = _reflectionHistory.value
        val exportData = history.map { entry ->
            mapOf(
                "weekNumber" to entry.weekNumber,
                "year" to entry.year,
                "responses" to entry.responses.map { (theme, response) ->
                    mapOf(
                        "theme" to theme,
                        "question" to response.question,
                        "response" to response.response,
                        "createdAt" to response.createdAt.toString()
                    )
                },
                "isComplete" to entry.isComplete,
                "completedAt" to entry.completedAt?.toString()
            )
        }
        return Json.encodeToString(exportData)
    }

    // ==================== AI FEATURES ====================

    override suspend fun analyzeResponseSentiment(response: String): Float {
        // Try Claude Haiku for sentiment analysis, fall back to keyword-based
        if (claudeApiClient != null) {
            try {
                val result = claudeApiClient.simpleCompletion(
                    systemPrompt = "You are a sentiment analyzer. Respond with ONLY a single decimal number between -1.0 (very negative) and 1.0 (very positive). Nothing else.",
                    userMessage = "Analyze the sentiment of this journal reflection:\n\n$response",
                    maxTokens = 10
                )
                result.getOrNull()?.trim()?.toFloatOrNull()?.coerceIn(-1f, 1f)?.let { return it }
            } catch (_: Exception) { /* fall through to keyword analysis */ }
        }

        // Keyword-based fallback
        val positiveWords = listOf("grateful", "happy", "proud", "accomplished", "joy", "love", "amazing", "wonderful", "great", "fantastic")
        val negativeWords = listOf("struggled", "failed", "difficult", "frustrated", "angry", "sad", "disappointed", "hard", "challenge", "problem")

        val words = response.lowercase().split("\\s+".toRegex())
        val positiveCount = words.count { positiveWords.any { pw -> it.contains(pw) } }
        val negativeCount = words.count { negativeWords.any { nw -> it.contains(nw) } }

        return when {
            positiveCount == 0 && negativeCount == 0 -> 0f
            else -> ((positiveCount - negativeCount).toFloat() / (positiveCount + negativeCount)).coerceIn(-1f, 1f)
        }
    }

    override suspend fun extractInsights(response: String): List<String> {
        // Try Claude Haiku for insight extraction, fall back to keyword-based
        if (claudeApiClient != null) {
            try {
                val result = claudeApiClient.simpleCompletion(
                    systemPrompt = "You are a wellness insight extractor. Given a journal reflection, identify 1-3 key themes or insights. Respond with ONLY a newline-separated list of short insight phrases (max 6 words each). No numbering, no bullets.",
                    userMessage = response,
                    maxTokens = 100
                )
                result.getOrNull()?.let { text ->
                    val aiInsights = text.trim().lines().filter { it.isNotBlank() }.take(3)
                    if (aiInsights.isNotEmpty()) return aiInsights
                }
            } catch (_: Exception) { /* fall through to keyword analysis */ }
        }

        // Keyword-based fallback
        val insights = mutableListOf<String>()

        if (response.contains("learned", ignoreCase = true)) {
            insights.add("Learning from experiences")
        }
        if (response.contains("grateful", ignoreCase = true) || response.contains("thankful", ignoreCase = true)) {
            insights.add("Practicing gratitude")
        }
        if (response.contains("goal", ignoreCase = true) || response.contains("intention", ignoreCase = true)) {
            insights.add("Setting clear intentions")
        }
        if (response.contains("challenge", ignoreCase = true) || response.contains("difficult", ignoreCase = true)) {
            insights.add("Acknowledging challenges")
        }
        if (response.contains("progress", ignoreCase = true) || response.contains("improve", ignoreCase = true)) {
            insights.add("Recognizing progress")
        }

        return insights.take(3)
    }

    override suspend fun getRecommendedPrompt(): ContextualPrompt {
        val stats = getReflectionStats()
        val leastUsedTheme = stats.themeCounts.minByOrNull { it.value }?.key
            ?.let { name -> ReflectionTheme.entries.find { it.name == name } }
            ?: ReflectionTheme.entries.random()

        val prompt = ReflectionPromptsDatabase.getPromptsByTheme(leastUsedTheme).random()

        return ContextualPrompt(
            prompt = prompt,
            relevanceScore = 0.9f,
            reason = "You haven't explored ${leastUsedTheme.name.lowercase().replace("_", " ")} prompts as much"
        )
    }

    // ==================== UTILITY ====================

    override fun getTotalPromptCount(): Int = ReflectionPromptsDatabase.getTotalCount()

    override fun getThemeInfo(theme: ReflectionTheme): ThemeInfo {
        val completedCount = _reflectionHistory.value
            .flatMap { it.responses.values }
            .count { it.theme == theme.name }

        return theme.toInfo(completedCount)
    }

    private suspend fun updateStats() {
        try {
            val stats = getReflectionStats()
            val statsData = hashMapOf(
                "totalReflections" to stats.totalReflections,
                "totalResponses" to stats.totalResponses,
                "totalWords" to stats.totalWords,
                "reflectionStreak" to stats.reflectionStreak,
                "longestStreak" to stats.longestStreak,
                "favoriteTheme" to (stats.favoriteTheme ?: ""),
                "lastUpdated" to Clock.System.now().toString()
            )
            statsDocument.set(statsData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
