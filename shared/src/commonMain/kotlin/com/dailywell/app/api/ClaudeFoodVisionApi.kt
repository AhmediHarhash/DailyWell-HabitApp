package com.dailywell.app.api

import com.dailywell.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Claude Vision API Integration for Food Scanning
 *
 * PERFORMANCE TARGET: < 3 seconds from photo to results
 * ACCURACY TARGET: 95%+ correct food identification
 *
 * Cost Management:
 * - Claude Haiku Vision: ~$0.0004 per image
 * - Budget: $5.50/month per user = ~13,750 scans/month
 * - Reality: Users scan 2-3x/day = 90 scans/month = $0.036/month
 */
class ClaudeFoodVisionApi(
    private val apiKey: String,
    private val baseUrl: String = "https://api.anthropic.com/v1",
    private val model: String = "claude-haiku-4-5-20250514"
) {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
                classDiscriminator = "_kind"
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000 // 30 second timeout
            connectTimeoutMillis = 10000 // 10 second connect
        }
    }

    /**
     * Analyze food photo and return nutrition data
     * Includes automatic retry logic with exponential backoff
     *
     * @param imageBytes Raw image bytes (JPEG/PNG)
     * @param mealType Optional meal type hint (breakfast, lunch, etc)
     * @return FoodScanResult with identified foods and nutrition
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun analyzeFoodImage(
        imageBytes: ByteArray,
        mealType: MealType? = null,
        userContext: String? = null
    ): Result<FoodScanResult> {
        return try {
            // Retry with exponential backoff for network failures
            val result = retryWithExponentialBackoff(
                maxRetries = 3,
                initialDelayMs = 1000
            ) {
                analyzeFoodImageInternal(imageBytes, mealType, userContext)
            }

            Result.success(result)

        } catch (e: Exception) {
            // Provide user-friendly error messages
            val userMessage = when {
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Request timed out. Please check your internet connection."
                e.message?.contains("401", ignoreCase = true) == true ->
                    "API authentication failed. Please check configuration."
                e.message?.contains("429", ignoreCase = true) == true ->
                    "Rate limit exceeded. Please try again in a moment."
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                else ->
                    "Failed to analyze image: ${e.message ?: "Unknown error"}"
            }

            Result.failure(Exception(userMessage, e))
        }
    }

    /**
     * Internal implementation with retry logic
     */
    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun analyzeFoodImageInternal(
        imageBytes: ByteArray,
        mealType: MealType?,
        userContext: String?
    ): FoodScanResult {
        // Convert image to base64
        val base64Image = Base64.encode(imageBytes)

        // Build prompt for Claude
        val prompt = buildFoodAnalysisPrompt(mealType, userContext)

        // Call Claude Vision API
        val response = httpClient.post("$baseUrl/messages") {
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            contentType(ContentType.Application.Json)

            setBody(FoodVisionRequest(
                model = model,
                max_tokens = 2048,
                messages = listOf(
                    FoodVisionMessage(
                        role = "user",
                        content = listOf(
                            ClaudeContent(
                                type = "image",
                                source = FoodImageSource(
                                    type = "base64",
                                    media_type = "image/jpeg",
                                    data = base64Image
                                )
                            ),
                            ClaudeContent(
                                type = "text",
                                text = prompt
                            )
                        )
                    )
                )
            ))
        }

        // Parse response
        val claudeResponse: ClaudeVisionResponse = response.body()

        // Extract nutrition data from Claude's response
        val nutritionData = parseClaudeResponse(claudeResponse)

        // Build FoodScanResult
        return FoodScanResult(
            id = generateId(),
            photoPath = "", // Will be set by caller
            recognizedFoods = nutritionData.foods,
            totalCalories = nutritionData.totalCalories,
            totalMacros = nutritionData.totalMacros,
            confidence = nutritionData.confidence,
            mealSuggestion = nutritionData.mealName,
            timestamp = kotlinx.datetime.Clock.System.now()
        )
    }

    /**
     * Retry with exponential backoff for transient failures
     */
    private suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        maxDelayMs: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e

                // Don't retry on auth errors or client errors
                if (e.message?.contains("401") == true ||
                    e.message?.contains("400") == true ||
                    e.message?.contains("403") == true) {
                    throw e
                }

                // If this was the last attempt, throw
                if (attempt == maxRetries - 1) {
                    throw e
                }

                // Wait before retrying
                kotlinx.coroutines.delay(currentDelay)

                // Exponential backoff with max cap
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }

        throw lastException ?: Exception("Max retries exceeded")
    }

    /**
     * Build intelligent prompt for Claude based on context
     */
    private fun buildFoodAnalysisPrompt(
        mealType: MealType?,
        userContext: String?
    ): String {
        val mealHint = when (mealType) {
            MealType.BREAKFAST -> " This is a breakfast meal."
            MealType.LUNCH -> " This is a lunch meal."
            MealType.DINNER -> " This is a dinner meal."
            MealType.SNACK -> " This is a snack."
            MealType.PRE_WORKOUT -> " This is a pre-workout meal."
            MealType.POST_WORKOUT -> " This is a post-workout meal."
            null -> ""
        }

        return """
            Analyze this food photo and provide detailed nutrition information.

            ${mealHint}${if (userContext != null) " Context: $userContext" else ""}

            Identify ALL foods visible in the image with PRECISE portions.

            Return ONLY valid JSON in this EXACT format (no markdown, no extra text):
            {
                "mealName": "Brief descriptive name (e.g., 'Grilled Chicken Salad')",
                "confidence": 0.95,
                "foods": [
                    {
                        "name": "Grilled Chicken Breast",
                        "serving": "4 oz (113g)",
                        "weightGrams": 113,
                        "calories": 165,
                        "protein": 31,
                        "carbs": 0,
                        "fat": 4
                    },
                    {
                        "name": "Mixed Green Salad",
                        "serving": "2 cups",
                        "weightGrams": 150,
                        "calories": 25,
                        "protein": 2,
                        "carbs": 5,
                        "fat": 0
                    }
                ]
            }

            CRITICAL REQUIREMENTS:
            1. Be SPECIFIC about portions (use grams/oz whenever possible)
            2. Calculate calories using: (protein × 4) + (carbs × 4) + (fat × 9)
            3. Include weightGrams for EVERY food (estimate if needed)
            4. Confidence should be 0.0-1.0 (how sure you are)
            5. If you can't identify something, list it as "Unknown food item" with estimated calories
            6. For packaged foods, use standard nutrition facts
            7. For restaurant food, use typical portions

            Examples of good serving descriptions:
            - "4 oz grilled chicken breast (113g)"
            - "1 cup cooked brown rice (195g)"
            - "2 large eggs, scrambled (100g)"
            - "1 medium banana (118g)"
            - "2 slices whole wheat bread (60g)"

            Return ONLY the JSON object, nothing else.
        """.trimIndent()
    }

    /**
     * Parse Claude's JSON response into nutrition data
     */
    private fun parseClaudeResponse(response: ClaudeVisionResponse): NutritionData {
        // Extract JSON from Claude's text response
        val jsonText = response.content.firstOrNull()?.text ?: ""

        // Parse JSON
        val json = Json {
            ignoreUnknownKeys = true
            classDiscriminator = "_kind"
        }
        val parsed = try {
            json.decodeFromString<ClaudeFoodResponse>(jsonText.trim())
        } catch (e: Exception) {
            // Fallback: manual parsing if JSON is malformed
            parseManually(jsonText)
        }

        // Convert to our data models and sanitize malformed or partial responses.
        val foods = parsed.foods.map { food ->
            val protein = food.protein.coerceAtLeast(0)
            val carbs = food.carbs.coerceAtLeast(0)
            val fat = food.fat.coerceAtLeast(0)
            val macroCalories = (protein * 4) + (carbs * 4) + (fat * 9)
            val calories = when {
                food.calories > 0 -> food.calories
                macroCalories > 0 -> macroCalories
                else -> 120
            }
            val name = food.name.takeIf { it.isNotBlank() } ?: "Unidentified food item"
            val serving = food.serving.takeIf { it.isNotBlank() } ?: "1 serving (est.)"

            FoodItem(
                id = generateId(),
                name = name,
                serving = serving,
                quantity = 1f,
                calories = calories,
                macros = MacroNutrients(
                    protein = protein,
                    carbs = carbs,
                    fat = fat
                ),
                weightGrams = food.weightGrams?.takeIf { it > 0f && it <= 2000f },
                calorieType = null // Will be auto-calculated
            )
        }.ifEmpty {
            listOf(buildFallbackFoodItem(parsed.mealName, jsonText))
        }

        val totalCalories = foods.sumOf { it.calories }
        val totalMacros = MacroNutrients(
            protein = foods.sumOf { it.macros.protein },
            carbs = foods.sumOf { it.macros.carbs },
            fat = foods.sumOf { it.macros.fat }
        )
        val hasFallback = parsed.foods.isEmpty()
        val safeMealName = parsed.mealName.takeIf { it.isNotBlank() }
            ?: foods.firstOrNull()?.name
            ?: "Unidentified meal"
        val safeConfidence = if (hasFallback) {
            parsed.confidence.coerceIn(0.32f, 0.58f)
        } else {
            parsed.confidence.coerceIn(0.45f, 0.98f)
        }

        return NutritionData(
            mealName = safeMealName,
            confidence = safeConfidence,
            foods = foods,
            totalCalories = totalCalories,
            totalMacros = totalMacros
        )
    }

    /**
     * Fallback manual parser if JSON is slightly malformed
     */
    private fun parseManually(text: String): ClaudeFoodResponse {
        // Extract meal name
        val mealName = Regex("\"mealName\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
            ?: "Unidentified Meal"

        // Extract confidence
        val confidence = Regex("\"confidence\"\\s*:\\s*([0-9.]+)").find(text)?.groupValues?.get(1)?.toFloatOrNull()
            ?: 0.5f

        val foodBlocks = Regex("\\{[^{}]*\"name\"\\s*:\\s*\"[^\"]+\"[^{}]*\\}")
            .findAll(text)
            .map { it.value }
            .toList()

        val foods = foodBlocks.map { block ->
            ClaudeFoodItem(
                name = Regex("\"name\"\\s*:\\s*\"([^\"]+)\"")
                    .find(block)?.groupValues?.getOrNull(1).orEmpty(),
                serving = Regex("\"serving\"\\s*:\\s*\"([^\"]+)\"")
                    .find(block)?.groupValues?.getOrNull(1).orEmpty(),
                weightGrams = Regex("\"weightGrams\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)")
                    .find(block)?.groupValues?.getOrNull(1)?.toFloatOrNull(),
                calories = Regex("\"calories\"\\s*:\\s*([0-9]+)")
                    .find(block)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
                protein = Regex("\"protein\"\\s*:\\s*([0-9]+)")
                    .find(block)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
                carbs = Regex("\"carbs\"\\s*:\\s*([0-9]+)")
                    .find(block)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
                fat = Regex("\"fat\"\\s*:\\s*([0-9]+)")
                    .find(block)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            )
        }

        return ClaudeFoodResponse(
            mealName = mealName,
            confidence = confidence,
            foods = foods
        )
    }

    private fun buildFallbackFoodItem(
        mealName: String,
        rawText: String
    ): FoodItem {
        val caloriesHint = Regex("\"(?:totalCalories|calories)\"\\s*:\\s*([0-9]{2,4})")
            .find(rawText)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(80, 1200)

        val calories = caloriesHint ?: 260
        val protein = max(8, (calories * 20 / 100) / 4)
        val fat = max(6, (calories * 30 / 100) / 9)
        val carbs = max(10, (calories - (protein * 4) - (fat * 9)) / 4)
        val fallbackName = mealName.takeIf { it.isNotBlank() } ?: "Unidentified meal item"

        return FoodItem(
            id = generateId(),
            name = fallbackName,
            serving = "1 serving (est.)",
            quantity = 1f,
            calories = calories,
            macros = MacroNutrients(
                protein = protein,
                carbs = carbs,
                fat = fat
            ),
            weightGrams = null,
            calorieType = null
        )
    }

    private fun generateId(): String {
        return "food_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}

// Data classes for API communication

@Serializable
data class FoodVisionRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<FoodVisionMessage>
)

@Serializable
data class FoodVisionMessage(
    val role: String,
    val content: List<ClaudeContent>
)

@Serializable
data class ClaudeContent(
    val type: String,
    val text: String? = null,
    val source: FoodImageSource? = null
)

@Serializable
data class FoodImageSource(
    val type: String,
    val media_type: String,
    val data: String
)

@Serializable
data class ClaudeVisionResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    val stop_reason: String? = null,
    val usage: Usage? = null
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String
)

@Serializable
data class Usage(
    val input_tokens: Int,
    val output_tokens: Int
)

// Claude's food response format

@Serializable
data class ClaudeFoodResponse(
    val mealName: String,
    val confidence: Float,
    val foods: List<ClaudeFoodItem>
)

@Serializable
data class ClaudeFoodItem(
    val name: String = "",
    val serving: String = "",
    val weightGrams: Float? = null,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
)

// Internal data structure

data class NutritionData(
    val mealName: String,
    val confidence: Float,
    val foods: List<FoodItem>,
    val totalCalories: Int,
    val totalMacros: MacroNutrients
)
