package com.dailywell.app.api

import com.dailywell.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Open Food Facts API Client
 * FREE barcode lookup with 4+ million products
 * No API key required!
 */
class OpenFoodFactsClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    /**
     * Lookup a product by barcode
     * Returns ScannedFood with health score, nutrients, additives
     */
    suspend fun lookupBarcode(barcode: String): Result<ScannedFood> {
        return try {
            val url = "${ApiConfig.OPEN_FOOD_FACTS_API_URL}/$barcode.json"

            val response = client.get(url) {
                header("User-Agent", ApiConfig.OPEN_FOOD_FACTS_USER_AGENT)
                accept(ContentType.Application.Json)
            }

            val offResponse = response.body<OpenFoodFactsResponse>()

            if (offResponse.status == 0 || offResponse.product == null) {
                return Result.failure(Exception("Product not found"))
            }

            val product = offResponse.product
            val nutrients = mapNutrients(product.nutriments)
            val healthScore = calculateHealthScore(product, nutrients)
            val additives = parseAdditives(product.additives_tags)

            // Fetch healthier alternatives in same category
            val alternatives = searchAlternatives(
                categories = product.categories_tags,
                currentScore = healthScore,
                excludeBarcode = barcode
            )

            val scannedFood = ScannedFood(
                id = barcode,
                barcode = barcode,
                name = product.product_name.ifEmpty { "Unknown Product" },
                brand = product.brands,
                imageUrl = product.image_front_url.ifEmpty { product.image_url },
                quantity = product.quantity,
                healthScore = healthScore,
                healthGradeStr = nutriscoreToGrade(product.nutriscore_grade),
                novaGroup = product.nova_group.coerceIn(1, 4),
                nutrients = nutrients,
                ingredients = product.ingredients_text,
                additives = additives,
                alternatives = alternatives,
                friendlyMessage = FriendlyMessages.getMainMessage(healthScore),
                tips = emptyList(), // Will be generated
                ecoScore = product.ecoscore_grade.uppercase(),
                scannedAt = System.currentTimeMillis(),
                scanSource = "barcode"
            ).let { food ->
                food.copy(tips = FriendlyMessages.generateTips(food))
            }

            Result.success(scannedFood)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate health score (0-100) using Yuka-style algorithm
     * Based on: Nutri-Score, NOVA, additives
     */
    private fun calculateHealthScore(product: OpenFoodFactsProduct, nutrients: NutrientInfo): Int {
        var score = 50 // Start neutral

        // 1. Nutri-Score contribution (40 points max)
        score += when (product.nutriscore_grade.lowercase()) {
            "a" -> 40
            "b" -> 30
            "c" -> 15
            "d" -> 5
            "e" -> -10
            else -> 0
        }

        // 2. NOVA group contribution (30 points max)
        score += when (product.nova_group) {
            1 -> 30  // Unprocessed
            2 -> 20  // Processed ingredients
            3 -> 5   // Processed foods
            4 -> -15 // Ultra-processed
            else -> 0
        }

        // 3. Positive nutrients bonus (15 points max)
        if (nutrients.protein >= 10) score += 5
        if (nutrients.fiber >= 4) score += 5
        if (nutrients.sugars <= 5) score += 5

        // 4. Negative nutrients penalty (15 points max deduction)
        if (nutrients.sugars > 20) score -= 10
        if (nutrients.saturatedFat > 10) score -= 5
        if (nutrients.sodium > 800) score -= 5

        // 5. Additives penalty
        val riskyAdditives = product.additives_tags.count { tag ->
            isRiskyAdditive(tag)
        }
        score -= (riskyAdditives * 3).coerceAtMost(15)

        return score.coerceIn(0, 100)
    }

    /**
     * Map Open Food Facts nutrients to our NutrientInfo model
     */
    private fun mapNutrients(nutriments: OpenFoodFactsNutriments): NutrientInfo {
        return NutrientInfo(
            calories = nutriments.energy_kcal_100g.toInt(),
            fat = nutriments.fat_100g,
            saturatedFat = nutriments.saturated_fat_100g,
            carbohydrates = nutriments.carbohydrates_100g,
            sugars = nutriments.sugars_100g,
            fiber = nutriments.fiber_100g,
            protein = nutriments.proteins_100g,
            sodium = nutriments.sodium_100g * 1000, // Convert g to mg
            salt = nutriments.salt_100g,
            servingSize = "100g"
        )
    }

    /**
     * Parse additives from Open Food Facts tags
     */
    private fun parseAdditives(additiveTags: List<String>): List<Additive> {
        return additiveTags.mapNotNull { tag ->
            // Format: en:e150d-caramel-color
            val code = tag.substringAfter(":").substringBefore("-").uppercase()
            if (code.startsWith("E") && code.length <= 5) {
                Additive(
                    code = code,
                    name = tag.substringAfter("-").replace("-", " ").replaceFirstChar { it.uppercase() },
                    function = getAdditiveFunction(code),
                    riskLevel = getAdditiveRisk(code),
                    explanation = getAdditiveExplanation(code)
                )
            } else null
        }
    }

    /**
     * Convert Nutri-Score letter to our HealthGrade string
     */
    private fun nutriscoreToGrade(nutriscore: String): String {
        return nutriscore.uppercase().takeIf { it in listOf("A", "B", "C", "D", "E") } ?: ""
    }

    /**
     * Check if an additive is considered risky
     */
    private fun isRiskyAdditive(tag: String): Boolean {
        val code = tag.substringAfter(":").substringBefore("-").uppercase()
        return code in RISKY_ADDITIVES
    }

    /**
     * Get additive risk level
     */
    private fun getAdditiveRisk(code: String): String {
        return when {
            code in AVOID_ADDITIVES -> "AVOID"
            code in LIMITED_ADDITIVES -> "LIMITED"
            else -> "SAFE"
        }
    }

    /**
     * Get additive function/purpose
     */
    private fun getAdditiveFunction(code: String): String {
        return ADDITIVE_FUNCTIONS[code] ?: "Food additive"
    }

    /**
     * Get additive explanation
     */
    private fun getAdditiveExplanation(code: String): String {
        return ADDITIVE_EXPLANATIONS[code] ?: "Limited research available"
    }

    /**
     * Search for healthier alternatives in the same category
     * Uses Open Food Facts search API sorted by nutriscore
     */
    private suspend fun searchAlternatives(
        categories: List<String>,
        currentScore: Int,
        excludeBarcode: String
    ): List<FoodAlternative> {
        if (categories.isEmpty()) return emptyList()

        return try {
            // Use the most specific category (usually the last one)
            val category = categories.lastOrNull { it.startsWith("en:") }
                ?: categories.lastOrNull()
                ?: return emptyList()

            // Clean up category for search
            val categoryName = category
                .removePrefix("en:")
                .replace("-", " ")

            // Search for products in same category, sorted by nutriscore (best first)
            val searchUrl = "${ApiConfig.OPEN_FOOD_FACTS_SEARCH_URL}?" +
                    "categories_tags_en=${categoryName.replace(" ", "+")}&" +
                    "sort_by=nutriscore_score&" +
                    "page_size=10&" +
                    "json=1&" +
                    "fields=code,product_name,brands,image_front_url,nutriscore_grade,nova_group,nutriments"

            val response = client.get(searchUrl) {
                header("User-Agent", ApiConfig.OPEN_FOOD_FACTS_USER_AGENT)
                accept(ContentType.Application.Json)
            }

            val searchResponse = response.body<OpenFoodFactsSearchResponse>()

            // Filter and map to alternatives
            searchResponse.products
                .filter { it.code != excludeBarcode && it.product_name.isNotBlank() }
                .mapNotNull { product ->
                    val productNutrients = mapNutrients(product.nutriments)
                    val productScore = calculateHealthScore(product, productNutrients)

                    // Only include if it's genuinely healthier
                    if (productScore > currentScore) {
                        val reason = generateBetterReason(currentScore, productScore, productNutrients)
                        FoodAlternative(
                            name = product.product_name,
                            brand = product.brands,
                            healthScore = productScore,
                            imageUrl = product.image_front_url.ifEmpty { product.image_url },
                            barcode = product.code,
                            reason = reason
                        )
                    } else null
                }
                .sortedByDescending { it.healthScore }
                .take(3) // Max 3 alternatives
        } catch (e: Exception) {
            // Silently fail - alternatives are nice-to-have
            emptyList()
        }
    }

    /**
     * Generate a friendly reason why the alternative is better
     */
    private fun generateBetterReason(
        originalScore: Int,
        alternativeScore: Int,
        nutrients: NutrientInfo
    ): String {
        val scoreDiff = alternativeScore - originalScore

        return when {
            scoreDiff >= 30 -> "Much healthier option!"
            nutrients.sugars < 5 && nutrients.protein >= 8 -> "Lower sugar, higher protein"
            nutrients.sugars < 5 -> "Lower in sugar"
            nutrients.fiber >= 4 -> "Higher in fiber"
            nutrients.protein >= 10 -> "More protein"
            nutrients.saturatedFat < 3 -> "Lower in saturated fat"
            scoreDiff >= 15 -> "Healthier choice"
            else -> "Better option"
        }
    }

    fun close() {
        client.close()
    }

    companion object {
        // Additives with limited data suggesting caution
        private val LIMITED_ADDITIVES = setOf(
            "E150A", "E150B", "E150C", "E150D", // Caramel colors
            "E211", // Sodium benzoate
            "E250", "E251", "E252", // Nitrites/Nitrates
            "E320", "E321", // BHA, BHT
            "E621", // MSG
            "E951", // Aspartame
            "E950", // Acesulfame K
        )

        // Additives best avoided (still not scary language)
        private val AVOID_ADDITIVES = setOf(
            "E102", // Tartrazine
            "E104", // Quinoline yellow
            "E110", // Sunset yellow
            "E122", // Carmoisine
            "E124", // Ponceau 4R
            "E129", // Allura red
            "E171", // Titanium dioxide
        )

        private val RISKY_ADDITIVES = LIMITED_ADDITIVES + AVOID_ADDITIVES

        // Additive functions
        private val ADDITIVE_FUNCTIONS = mapOf(
            "E150D" to "Coloring agent",
            "E211" to "Preservative",
            "E250" to "Preservative/Color fixative",
            "E621" to "Flavor enhancer",
            "E951" to "Artificial sweetener",
            "E950" to "Artificial sweetener",
            "E102" to "Artificial coloring",
            "E110" to "Artificial coloring",
            "E171" to "Whitening agent",
        )

        // Friendly explanations (never scary!)
        private val ADDITIVE_EXPLANATIONS = mapOf(
            "E150D" to "A common caramel coloring. Some prefer to limit intake.",
            "E211" to "Preservative used to extend shelf life. Best in moderation.",
            "E250" to "Used in processed meats. Fine occasionally.",
            "E621" to "Flavor enhancer (MSG). Generally safe, some prefer to avoid.",
            "E951" to "Artificial sweetener. FDA approved, but some prefer natural options.",
            "E950" to "Artificial sweetener. Often combined with others.",
            "E102" to "Artificial yellow color. Some prefer food-derived colors.",
            "E110" to "Artificial orange color. Commonly found in snacks.",
            "E171" to "Whitening agent being phased out in some regions.",
        )
    }
}
