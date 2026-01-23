package com.dailywell.app.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Real Claude API client for AI coaching
 * Uses claude-sonnet-4-20250514 (latest 2026 model)
 */
class ClaudeApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    /**
     * Send a message to Claude and get a coaching response
     */
    suspend fun getCoachingResponse(
        userMessage: String,
        coachPersonality: CoachPersonality,
        userContext: UserContext
    ): Result<String> {
        return try {
            val systemPrompt = buildSystemPrompt(coachPersonality, userContext)

            val request = ClaudeRequest(
                model = ApiConfig.CLAUDE_MODEL,
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

    private fun buildSystemPrompt(personality: CoachPersonality, context: UserContext): String {
        return """
            You are ${personality.name}, a ${personality.style} habit coach in a mobile app called DailyWell.

            Your personality: ${personality.description}

            User's current state:
            - Streak: ${context.currentStreak} days
            - Today's progress: ${context.todayCompleted}/${context.totalHabits} habits completed
            - Strongest habit: ${context.strongestHabit ?: "Still learning"}
            - Area to improve: ${context.weakestHabit ?: "Still learning"}

            Guidelines:
            - Keep responses concise (2-4 sentences max)
            - Be ${personality.style.lowercase()} in tone
            - Focus on encouragement and practical advice
            - Use emojis sparingly but appropriately
            - Never be preachy or judgmental
            - Celebrate small wins
        """.trimIndent()
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
    val timeOfDay: String // "morning", "afternoon", "evening"
)

data class CoachPersonality(
    val name: String,
    val style: String,
    val description: String
)
