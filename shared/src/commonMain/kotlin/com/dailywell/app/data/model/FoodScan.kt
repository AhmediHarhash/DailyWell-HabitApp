package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

// ============== HEALTH GRADES (Yuka-style) ==============

enum class HealthGrade(val score: IntRange, val label: String, val emoji: String) {
    A(75..100, "Excellent", "A"),
    B(60..74, "Good", "B"),
    C(45..59, "Fair", "C"),
    D(25..44, "Moderate", "D"),
    E(0..24, "Consider alternatives", "E");

    companion object {
        fun fromScore(score: Int): HealthGrade = entries.first { score in it.score }
    }
}

// ============== NOVA PROCESSING LEVEL (1-4 scale) ==============

enum class NovaGroup(val level: Int, val displayName: String, val description: String) {
    UNPROCESSED(1, "Unprocessed", "Fresh foods like fruits, vegetables, meat, eggs"),
    PROCESSED_INGREDIENTS(2, "Processed ingredients", "Salt, sugar, oils, butter"),
    PROCESSED(3, "Processed foods", "Canned vegetables, cheese, bread"),
    ULTRA_PROCESSED(4, "Ultra-processed", "Soft drinks, chips, ready meals");

    companion object {
        fun fromLevel(level: Int): NovaGroup = entries.firstOrNull { it.level == level } ?: ULTRA_PROCESSED
    }
}

// ============== NUTRIENT INFO ==============

@Serializable
data class NutrientInfo(
    val calories: Int = 0,           // kcal per 100g
    val fat: Float = 0f,             // grams per 100g
    val saturatedFat: Float = 0f,    // grams per 100g
    val carbohydrates: Float = 0f,   // grams per 100g
    val sugars: Float = 0f,          // grams per 100g
    val fiber: Float = 0f,           // grams per 100g
    val protein: Float = 0f,         // grams per 100g
    val sodium: Float = 0f,          // mg per 100g
    val salt: Float = 0f,            // grams per 100g
    val servingSize: String = "100g"
) {
    // Percentage of daily recommended values
    val fatPercent: Int get() = ((fat / 65f) * 100).toInt().coerceIn(0, 999)
    val saturatedFatPercent: Int get() = ((saturatedFat / 20f) * 100).toInt().coerceIn(0, 999)
    val sugarsPercent: Int get() = ((sugars / 50f) * 100).toInt().coerceIn(0, 999)
    val sodiumPercent: Int get() = ((sodium / 2300f) * 100).toInt().coerceIn(0, 999)
    val fiberPercent: Int get() = ((fiber / 25f) * 100).toInt().coerceIn(0, 999)
    val proteinPercent: Int get() = ((protein / 50f) * 100).toInt().coerceIn(0, 999)
}

// ============== ADDITIVES ==============

enum class AdditiveRisk(val label: String, val color: Long) {
    SAFE(label = "No concern", color = 0xFF4CAF50),      // Green
    LIMITED(label = "Limited risk", color = 0xFFFFA726), // Orange
    AVOID(label = "Best avoided", color = 0xFFFF7043)    // Deep Orange (NOT red)
}

@Serializable
data class Additive(
    val code: String,               // E.g., "E150d"
    val name: String,               // E.g., "Caramel color"
    val function: String = "",      // E.g., "Coloring agent"
    val riskLevel: String = "SAFE", // SAFE, LIMITED, AVOID
    val explanation: String = ""    // Why it's concerning
) {
    val risk: AdditiveRisk get() = AdditiveRisk.entries.firstOrNull { it.name == riskLevel } ?: AdditiveRisk.SAFE
}

// ============== FOOD ALTERNATIVES ==============

@Serializable
data class FoodAlternative(
    val name: String,
    val brand: String = "",
    val healthScore: Int,
    val imageUrl: String = "",
    val barcode: String = "",
    val reason: String = ""         // "Lower in sugar" or "More protein"
)

// ============== MAIN SCANNED FOOD MODEL ==============

@Serializable
data class ScannedFood(
    val id: String = "",
    val barcode: String = "",
    val name: String,
    val brand: String = "",
    val imageUrl: String = "",
    val quantity: String = "",      // E.g., "330ml" or "500g"

    // Health scoring (Yuka-style)
    val healthScore: Int,           // 0-100
    val healthGradeStr: String = "",// A, B, C, D, E

    // NOVA processing (1-4)
    val novaGroup: Int = 3,

    // Nutrients
    val nutrients: NutrientInfo = NutrientInfo(),

    // Ingredients & additives
    val ingredients: String = "",
    val additives: List<Additive> = emptyList(),

    // Better alternatives
    val alternatives: List<FoodAlternative> = emptyList(),

    // Friendly message (NEVER scary!)
    val friendlyMessage: String = "",
    val tips: List<String> = emptyList(),

    // Eco-score (for future)
    val ecoScore: String = "",      // A, B, C, D, E

    // Scan metadata
    val scannedAt: Long = 0,
    val scanSource: String = "barcode" // "barcode" or "photo"
) {
    val healthGrade: HealthGrade get() = HealthGrade.fromScore(healthScore)
    val novaLevel: NovaGroup get() = NovaGroup.fromLevel(novaGroup)

    // Additive summary
    val hasRiskyAdditives: Boolean get() = additives.any { it.risk != AdditiveRisk.SAFE }
    val riskyAdditiveCount: Int get() = additives.count { it.risk != AdditiveRisk.SAFE }
}

// ============== SCAN HISTORY ==============

@Serializable
data class ScanHistory(
    val scans: List<ScannedFood> = emptyList(),
    val totalScans: Int = 0,
    val averageScore: Int = 0
)

// ============== FOOD CATEGORY (Noom-style) ==============

enum class FoodCategory(val label: String, val color: Long) {
    GREEN("Eat freely", 0xFF4CAF50),         // Low calorie, nutrient-dense
    YELLOW("Moderate portions", 0xFFFFA726), // Balanced
    ORANGE("Smaller portions", 0xFFFF7043);  // High calorie, less nutritious (NOT "red")

    companion object {
        fun fromScore(score: Int): FoodCategory = when {
            score >= 70 -> GREEN
            score >= 40 -> YELLOW
            else -> ORANGE
        }
    }
}

// ============== FRIENDLY MESSAGES (Never scary!) ==============

object FriendlyMessages {

    fun getMainMessage(score: Int): String = when {
        score >= 75 -> "Excellent choice!"
        score >= 60 -> "Good option for you"
        score >= 45 -> "Okay in moderation"
        score >= 25 -> "Here's what to know"
        else -> "Let's find something even better"
    }

    fun getNutrientTip(nutrients: NutrientInfo): String? {
        return when {
            nutrients.protein >= 20 -> "Great source of protein!"
            nutrients.fiber >= 6 -> "High in fiber - good for digestion"
            nutrients.sugarsPercent > 50 -> "High in sugar - balance with protein or fiber"
            nutrients.sodiumPercent > 50 -> "Higher in sodium - drink extra water"
            nutrients.saturatedFatPercent > 50 -> "Higher in saturated fat - pair with vegetables"
            else -> null
        }
    }

    fun getAlternativeSuggestion(hasAlternatives: Boolean): String? {
        return if (hasAlternatives) "If you love this, try one of our suggestions!" else null
    }

    fun getAdditiveTip(additives: List<Additive>): String? {
        val riskyCount = additives.count { it.risk != AdditiveRisk.SAFE }
        return when {
            riskyCount == 0 -> null
            riskyCount == 1 -> "Contains 1 additive worth knowing about"
            else -> "Contains $riskyCount additives worth knowing about"
        }
    }

    fun getNovaMessage(novaGroup: Int): String = when (novaGroup) {
        1 -> "Minimally processed - great choice!"
        2 -> "Basic processed ingredient"
        3 -> "Processed food - okay in moderation"
        4 -> "Ultra-processed - enjoy occasionally"
        else -> ""
    }

    // Combined tips for a food item
    fun generateTips(food: ScannedFood): List<String> {
        val tips = mutableListOf<String>()

        // Nutrient-based tips
        getNutrientTip(food.nutrients)?.let { tips.add(it) }

        // Additive tip
        getAdditiveTip(food.additives)?.let { tips.add(it) }

        // NOVA tip (only if ultra-processed)
        if (food.novaGroup == 4) {
            tips.add("Pro tip: pair with fresh fruits or vegetables")
        }

        // Alternative suggestion
        if (food.alternatives.isNotEmpty()) {
            tips.add("Tap 'See alternatives' for similar but healthier options")
        }

        return tips
    }
}

// ============== OPEN FOOD FACTS API RESPONSE ==============

@Serializable
data class OpenFoodFactsProduct(
    val code: String = "",
    val product_name: String = "",
    val brands: String = "",
    val image_url: String = "",
    val image_front_url: String = "",
    val quantity: String = "",
    val nutriscore_grade: String = "",      // a, b, c, d, e
    val nova_group: Int = 0,                // 1-4
    val ecoscore_grade: String = "",        // a, b, c, d, e
    val ingredients_text: String = "",
    val additives_tags: List<String> = emptyList(),
    val categories_tags: List<String> = emptyList(),  // For finding alternatives
    val nutriments: OpenFoodFactsNutriments = OpenFoodFactsNutriments()
)

@Serializable
data class OpenFoodFactsNutriments(
    val energy_kcal_100g: Float = 0f,
    val fat_100g: Float = 0f,
    val saturated_fat_100g: Float = 0f,
    val carbohydrates_100g: Float = 0f,
    val sugars_100g: Float = 0f,
    val fiber_100g: Float = 0f,
    val proteins_100g: Float = 0f,
    val sodium_100g: Float = 0f,
    val salt_100g: Float = 0f
)

@Serializable
data class OpenFoodFactsResponse(
    val code: String = "",
    val status: Int = 0,
    val status_verbose: String = "",
    val product: OpenFoodFactsProduct? = null
)

// ============== SEARCH API RESPONSE ==============

@Serializable
data class OpenFoodFactsSearchResponse(
    val count: Int = 0,
    val page: Int = 1,
    val page_count: Int = 0,
    val page_size: Int = 20,
    val products: List<OpenFoodFactsProduct> = emptyList()
)
