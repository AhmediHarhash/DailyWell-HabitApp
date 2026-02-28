package com.dailywell.app.api

import com.dailywell.app.data.model.CompletionRecord
import com.dailywell.app.data.model.AIModelUsed
import com.dailywell.shared.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Real Claude API client for AI coaching
 * Uses claude-sonnet-4-20250514 (latest 2026 model)
 *
 * SECURITY HARDENED:
 * - CVE-DW-005 FIX: Rate limiting (max 10 requests/minute)
 * - CVE-DW-006 FIX: No body logging in release builds
 */
class ClaudeApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        classDiscriminator = "_kind"
    }

    // SECURITY: Rate limiting - max 10 requests per minute
    private val rateLimitMutex = Mutex()
    private val requestTimestamps = mutableListOf<Long>()
    private val maxRequestsPerMinute = 10

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            // CVE-DW-006 FIX: Only log headers in debug, nothing in release
            level = if (BuildConfig.DEBUG) LogLevel.HEADERS else LogLevel.NONE
        }
    }

    /**
     * SECURITY: Rate limiter to prevent API cost attacks
     * Returns true if request is allowed, false if rate limited
     */
    private suspend fun checkRateLimit(): Boolean {
        return rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val oneMinuteAgo = now - 60_000

            // Remove old timestamps
            requestTimestamps.removeAll { it < oneMinuteAgo }

            // Check if under limit
            if (requestTimestamps.size >= maxRequestsPerMinute) {
                false
            } else {
                requestTimestamps.add(now)
                true
            }
        }
    }

    /**
     * Send a message to Claude and get a coaching response
     * SECURITY: Rate limited to prevent API cost attacks
     */
    suspend fun getCoachingResponse(
        userMessage: String,
        coachPersonality: CoachPersonality,
        userContext: UserContext,
        model: AIModelUsed = AIModelUsed.CLAUDE_SONNET
    ): Result<String> {
        // SECURITY: Check rate limit before making request
        if (!checkRateLimit()) {
            return Result.failure(RateLimitException("Too many requests. Please wait a moment."))
        }

        return try {
            val systemPrompt = buildSystemPrompt(coachPersonality, userContext)
            val modelId = when (model) {
                AIModelUsed.CLAUDE_HAIKU -> ApiConfig.CLAUDE_HAIKU_MODEL
                AIModelUsed.CLAUDE_OPUS -> ApiConfig.CLAUDE_OPUS_MODEL
                else -> ApiConfig.CLAUDE_MODEL
            }

            val request = ClaudeRequest(
                model = modelId,
                maxTokens = 500,
                system = systemPrompt,
                messages = listOf(
                    ClaudeMessage(role = "user", content = userMessage)
                )
            )

            val response = client.post(ApiConfig.CLAUDE_API_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", ApiConfig.CLAUDE_API_KEY)
                header("anthropic-version", ApiConfig.CLAUDE_API_VERSION)
                setBody(request)
            }

            val claudeResponse = response.body<ClaudeResponse>()
            val text = claudeResponse.content.firstOrNull()?.text ?: "I'm here to help!"

            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Rate limit exception for UI handling
     */
    class RateLimitException(message: String) : Exception(message)

    /**
     * Generate daily insight using real AI
     */
    suspend fun generateDailyInsight(
        userContext: UserContext,
        coachPersonality: CoachPersonality
    ): Result<DailyInsightResponse> {
        return try {
            val prompt = """
                Generate a personalized daily coaching insight for a habit tracking app user.

                User Context:
                - Current streak: ${userContext.currentStreak} days
                - Total habits tracked: ${userContext.totalHabits}
                - Today's completion: ${userContext.todayCompleted}/${userContext.totalHabits}
                - Weakest habit: ${userContext.weakestHabit ?: "none identified"}
                - Strongest habit: ${userContext.strongestHabit ?: "none identified"}
                - Time of day: ${userContext.timeOfDay}

                Respond with a JSON object containing:
                {
                    "greeting": "personalized greeting",
                    "mainMessage": "1-2 sentence motivational insight",
                    "focusHabit": "habit to focus on today (or null)",
                    "focusReason": "why this habit needs attention",
                    "motivationalQuote": "relevant quote with attribution",
                    "celebrationNote": "celebration for recent wins (or null)"
                }
            """.trimIndent()

            val systemPrompt = """
                You are ${coachPersonality.name}, a ${coachPersonality.style} habit coach.
                ${coachPersonality.description}
                Always respond with valid JSON only, no markdown.
            """.trimIndent()

            val request = ClaudeRequest(
                model = ApiConfig.CLAUDE_MODEL,
                maxTokens = 600,
                system = systemPrompt,
                messages = listOf(
                    ClaudeMessage(role = "user", content = prompt)
                )
            )

            val response = client.post(ApiConfig.CLAUDE_API_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", ApiConfig.CLAUDE_API_KEY)
                header("anthropic-version", ApiConfig.CLAUDE_API_VERSION)
                setBody(request)
            }

            val claudeResponse = response.body<ClaudeResponse>()
            val text = claudeResponse.content.firstOrNull()?.text ?: "{}"

            val insight = json.decodeFromString<DailyInsightResponse>(text)
            Result.success(insight)
        } catch (e: Exception) {
            // Return a fallback insight
            Result.success(
                DailyInsightResponse(
                    greeting = "Good ${userContext.timeOfDay}!",
                    mainMessage = "Every small step counts toward your goals. Let's make today meaningful.",
                    focusHabit = userContext.weakestHabit,
                    focusReason = "This habit could use some extra attention today",
                    motivationalQuote = "\"The secret of getting ahead is getting started.\" - Mark Twain",
                    celebrationNote = if (userContext.currentStreak > 0) "You're on a ${userContext.currentStreak}-day streak!" else null
                )
            )
        }
    }

    /**
     * Analyze a food photo using Claude Haiku 4.5 Vision
     * Cost: ~$0.006 per image (very affordable)
     *
     * Returns structured food analysis with:
     * - Food name and description
     * - Estimated calories and nutrients
     * - Health score (0-100)
     * - Friendly tips
     */
    suspend fun analyzeFoodPhoto(imageBase64: String): Result<FoodPhotoAnalysis> {
        return try {
            val prompt = """
                Analyze this food photo and provide a detailed nutritional assessment.

                Respond with a JSON object containing:
                {
                    "foodName": "Main food item name",
                    "description": "Brief description of what's in the photo",
                    "estimatedCalories": number (per serving),
                    "portionSize": "estimated portion description",
                    "nutrients": {
                        "protein": number (grams),
                        "carbs": number (grams),
                        "fat": number (grams),
                        "fiber": number (grams),
                        "sugar": number (grams),
                        "sodium": number (mg)
                    },
                    "healthScore": number (0-100, based on nutritional value),
                    "healthGrade": "A/B/C/D/E",
                    "novaGroup": number (1-4, processing level),
                    "positiveAspects": ["list", "of", "good", "things"],
                    "considerations": ["list", "of", "things", "to", "note"],
                    "friendlyTip": "One helpful tip for enjoying this food",
                    "alternativeSuggestion": "A healthier alternative if applicable, or null"
                }

                Guidelines:
                - Be accurate but friendly (never scary or judgmental)
                - Focus on balance, not restriction
                - Celebrate nutritious choices
                - For less healthy options, suggest pairing with vegetables
                - Use encouraging language
            """.trimIndent()

            val systemPrompt = """
                You are a friendly nutritionist AI helping users understand their food choices.
                Always respond with valid JSON only, no markdown code blocks.
                Be supportive and educational, never judgmental.
                Use Yuka-style scoring (0-100):
                - 75-100: Excellent
                - 60-74: Good
                - 45-59: Fair
                - 25-44: Moderate
                - 0-24: Consider alternatives
            """.trimIndent()

            val contentArray = buildJsonArray {
                // Image content
                add(buildJsonObject {
                    put("type", "image")
                    put("source", buildJsonObject {
                        put("type", "base64")
                        put("media_type", "image/jpeg")
                        put("data", imageBase64)
                    })
                })
                // Text content
                add(buildJsonObject {
                    put("type", "text")
                    put("text", prompt)
                })
            }

            val request = ClaudeVisionRequest(
                model = ApiConfig.CLAUDE_HAIKU_MODEL,
                maxTokens = 800,
                system = systemPrompt,
                messages = listOf(
                    ClaudeVisionMessage(
                        role = "user",
                        content = contentArray
                    )
                )
            )

            val response = client.post(ApiConfig.CLAUDE_API_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", ApiConfig.CLAUDE_API_KEY)
                header("anthropic-version", ApiConfig.CLAUDE_API_VERSION)
                setBody(request)
            }

            val claudeResponse = response.body<ClaudeResponse>()
            val text = claudeResponse.content.firstOrNull()?.text ?: "{}"

            val analysis = json.decodeFromString<FoodPhotoAnalysis>(text)
            Result.success(analysis)
        } catch (e: Exception) {
            // Return a fallback response
            Result.failure(e)
        }
    }

    /**
     * Generate AI-powered pattern insights from habit completion history
     * Uses Claude Haiku 4.5 for cost-effective pattern analysis
     *
     * @param completionHistory Map of habitId to completion records
     * @return List of AI-generated insights
     */
    suspend fun generatePatternInsights(
        completionHistory: Map<String, List<CompletionRecord>>
    ): Result<List<PatternInsightAI>> {
        return try {
            // Build summary of habit data for Claude
            val habitSummaries = completionHistory.map { (habitId, records) ->
                val totalRecords = records.size
                val completedCount = records.count { it.completed }
                val completionRate = if (totalRecords > 0) (completedCount.toFloat() / totalRecords * 100).toInt() else 0

                // Analyze timing patterns
                val morningCompletions = records.count { record ->
                    record.completedAt?.let { time ->
                        val hour = time.substring(11, 13).toIntOrNull() ?: 12
                        hour in 5..11
                    } == true
                }
                val eveningCompletions = records.count { record ->
                    record.completedAt?.let { time ->
                        val hour = time.substring(11, 13).toIntOrNull() ?: 12
                        hour in 17..23
                    } == true
                }

                // Analyze weekday patterns
                val weekendCompletions = records.count { it.isWeekend }
                val weekdayCompletions = totalRecords - weekendCompletions

                """
                Habit: $habitId
                - Total days tracked: $totalRecords
                - Completion rate: $completionRate%
                - Morning completions: $morningCompletions
                - Evening completions: $eveningCompletions
                - Weekday completions: $weekdayCompletions
                - Weekend completions: $weekendCompletions
                """.trimIndent()
            }.joinToString("\n\n")

            val prompt = """
                Analyze the following habit tracking data and generate personalized insights.

                $habitSummaries

                Generate 3-5 actionable insights in JSON format:
                [
                    {
                        "type": "CORRELATION|TIMING_PATTERN|SUCCESS_FACTOR|IMPROVEMENT|STREAK_RISK",
                        "title": "Short insight title",
                        "description": "1-2 sentence description of the pattern",
                        "emoji": "relevant emoji",
                        "significance": 0.0-1.0,
                        "recommendation": "Actionable advice",
                        "relatedHabits": ["habitId1", "habitId2"]
                    }
                ]

                Focus on:
                - Correlations between habits (e.g., sleep affects exercise)
                - Optimal timing patterns (morning vs evening)
                - Weekend vs weekday differences
                - Recent improvements or concerns
                - Success factors the user should maintain

                Be encouraging and specific. Only respond with valid JSON array.
            """.trimIndent()

            val systemPrompt = """
                You are a data analyst for a habit tracking app.
                Analyze habit patterns and generate actionable insights.
                Focus on practical, encouraging advice.
                Always respond with valid JSON only, no markdown.
            """.trimIndent()

            val request = ClaudeRequest(
                model = ApiConfig.CLAUDE_HAIKU_MODEL,  // Use Haiku for cost efficiency
                maxTokens = 800,
                system = systemPrompt,
                messages = listOf(
                    ClaudeMessage(role = "user", content = prompt)
                )
            )

            val response = client.post(ApiConfig.CLAUDE_API_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", ApiConfig.CLAUDE_API_KEY)
                header("anthropic-version", ApiConfig.CLAUDE_API_VERSION)
                setBody(request)
            }

            val claudeResponse = response.body<ClaudeResponse>()
            val text = claudeResponse.content.firstOrNull()?.text ?: "[]"

            val insights = json.decodeFromString<List<PatternInsightAI>>(text)
            Result.success(insights)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildSystemPrompt(personality: CoachPersonality, context: UserContext): String {
        // Include behavior profile for hyper-personalized coaching
        val behaviorSection = context.behaviorProfileContext?.let {
            """

            ${it}
            """
        } ?: ""

        return """
            You are ${personality.name}, a ${personality.style} habit coach in a mobile app called DailyWell.

            Your personality: ${personality.description}

            User's current state:
            - Streak: ${context.currentStreak} days
            - Today's progress: ${context.todayCompleted}/${context.totalHabits} habits completed
            - Strongest habit: ${context.strongestHabit ?: "Still learning"}
            - Area to improve: ${context.weakestHabit ?: "Still learning"}
            $behaviorSection

            Guidelines:
            - Keep responses concise (2-4 sentences max)
            - Be ${personality.style.lowercase()} in tone, adapting to the user's motivation style if known
            - Focus on encouragement and practical advice
            - Use emojis sparingly but appropriately
            - Never be preachy or judgmental
            - Celebrate small wins
            - If the user seems frustrated, be extra supportive
            - Reference habit correlations when relevant
        """.trimIndent()
    }

    /**
     * Lightweight completion for analysis tasks (sentiment, insight extraction)
     * Uses Haiku for cost efficiency
     */
    suspend fun simpleCompletion(systemPrompt: String, userMessage: String, maxTokens: Int = 200): Result<String> {
        if (!checkRateLimit()) {
            return Result.failure(RateLimitException("Too many requests. Please wait a moment."))
        }
        return try {
            val request = ClaudeRequest(
                model = "claude-haiku-4-5-20251001",
                maxTokens = maxTokens,
                system = systemPrompt,
                messages = listOf(ClaudeMessage(role = "user", content = userMessage))
            )
            val response = client.post(ApiConfig.CLAUDE_API_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", ApiConfig.CLAUDE_API_KEY)
                header("anthropic-version", ApiConfig.CLAUDE_API_VERSION)
                setBody(request)
            }
            val claudeResponse = response.body<ClaudeResponse>()
            val text = claudeResponse.content.firstOrNull()?.text ?: ""
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}

// Request/Response models
@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String? = null,
    val messages: List<ClaudeMessage>
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: String
)

@Serializable
data class ClaudeResponse(
    val id: String = "",
    val type: String = "",
    val role: String = "",
    val content: List<ClaudeContentBlock> = emptyList(),
    @SerialName("stop_reason") val stopReason: String? = null
)

@Serializable
data class ClaudeContentBlock(
    val type: String = "",
    val text: String = ""
)

@Serializable
data class DailyInsightResponse(
    val greeting: String,
    val mainMessage: String,
    val focusHabit: String? = null,
    val focusReason: String? = null,
    val motivationalQuote: String? = null,
    val celebrationNote: String? = null
)

// Context models
data class UserContext(
    val currentStreak: Int,
    val totalHabits: Int,
    val todayCompleted: Int,
    val strongestHabit: String?,
    val weakestHabit: String?,
    val timeOfDay: String, // "morning", "afternoon", "evening"
    val behaviorProfileContext: String? = null  // User learning system data for AI personalization
)

data class CoachPersonality(
    val name: String,
    val style: String,
    val description: String
)

// Vision API request models for food photo analysis
@Serializable
data class ClaudeVisionRequest(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val system: String? = null,
    val messages: List<ClaudeVisionMessage>
)

@Serializable
data class ClaudeVisionMessage(
    val role: String,
    val content: kotlinx.serialization.json.JsonArray
)

@Serializable
data class ImageSource(
    val type: String,
    @SerialName("media_type") val mediaType: String,
    val data: String
)

// Food photo analysis response
@Serializable
data class FoodPhotoAnalysis(
    val foodName: String,
    val description: String = "",
    val estimatedCalories: Int = 0,
    val portionSize: String = "",
    val nutrients: PhotoNutrients = PhotoNutrients(),
    val healthScore: Int = 50,
    val healthGrade: String = "C",
    val novaGroup: Int = 3,
    val positiveAspects: List<String> = emptyList(),
    val considerations: List<String> = emptyList(),
    val friendlyTip: String = "",
    val alternativeSuggestion: String? = null
)

@Serializable
data class PhotoNutrients(
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f
)

/**
 * AI-generated pattern insight response model
 */
@Serializable
data class PatternInsightAI(
    val type: String,              // CORRELATION, TIMING_PATTERN, etc.
    val title: String,
    val description: String,
    val emoji: String = "💡",
    val significance: Float = 0.5f,
    val recommendation: String? = null,
    val relatedHabits: List<String> = emptyList()
)
