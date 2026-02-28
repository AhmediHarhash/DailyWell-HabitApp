package com.dailywell.app.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Feature #10: Nutrition Tracking & AI Food Scanner
 * Complete nutrition tracking system for fitness enthusiasts
 */

@Serializable
data class DailyNutrition(
    val userId: String,
    val date: String, // ISO date
    val calorieGoal: Int = 2000,
    val caloriesConsumed: Int = 0,
    val macros: MacroNutrients,
    val macroGoals: MacroNutrients,
    val meals: List<MealEntry> = emptyList(),
    val waterIntake: Int = 0, // ml
    val waterGoal: Int = 2000 // ml
) {
    val caloriesRemaining: Int get() = calorieGoal - caloriesConsumed
    val proteinRemaining: Int get() = macroGoals.protein - macros.protein
    val carbsRemaining: Int get() = macroGoals.carbs - macros.carbs
    val fatRemaining: Int get() = macroGoals.fat - macros.fat
}

@Serializable
data class MacroNutrients(
    val protein: Int = 0,  // grams
    val carbs: Int = 0,    // grams
    val fat: Int = 0       // grams
) {
    val totalCalories: Int get() = (protein * 4) + (carbs * 4) + (fat * 9)
}

@Serializable
data class MealEntry(
    val id: String,
    val userId: String,
    val date: String,
    val mealType: MealType,
    val mealName: String,
    val foods: List<FoodItem>,
    val totalCalories: Int,
    val totalMacros: MacroNutrients,
    val photo: String? = null, // Photo URL/path
    val notes: String? = null,
    val timestamp: Instant,
    val scanMethod: ScanMethod = ScanMethod.MANUAL,
    // Noom-inspired emotion tracking
    val emotionBefore: String? = null,  // EatingEmotion enum from PsychologyModels
    val hungerLevel: Int? = null,       // 1-10 scale
    val fullnessAfter: Int? = null,     // 1-10 scale
    val satisfactionLevel: Int? = null  // 1-10 scale
)

@Serializable
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    PRE_WORKOUT,
    POST_WORKOUT
}

@Serializable
enum class ScanMethod {
    AI_PHOTO_SCAN,   // Scanned with camera + AI
    VOICE_LOG,       // Logged with voice
    MANUAL,          // Typed manually
    BARCODE          // Scanned barcode
}

/**
 * Noom-inspired color-coded food system based on calorie density
 * Psychology: Focus on volume, not just calories
 */
@Serializable
enum class CalorieDensityType {
    GREEN,    // < 0.8 cal/g - Eat freely (vegetables, fruits, broth soups)
    YELLOW,   // 0.8-2.5 cal/g - Moderate portions (lean proteins, whole grains)
    RED       // > 2.5 cal/g - Small portions (oils, nuts, processed foods)
}

@Serializable
data class FoodItem(
    val id: String,
    val name: String,
    val serving: String,      // e.g., "2 large eggs", "1 cup", "100g"
    val quantity: Float = 1f,
    val calories: Int,
    val macros: MacroNutrients,
    val micronutrients: MicroNutrients? = null,
    val weightGrams: Float? = null,  // Weight in grams for density calculation
    val calorieType: CalorieDensityType? = null  // Green/Yellow/Red
) {
    /**
     * Calculate calorie density (calories per gram)
     * Used for Noom-style color coding
     */
    val calorieDensity: Float? get() = weightGrams?.let { calories / it }

    /**
     * Automatically determine color type based on calorie density
     */
    val autoCalorieType: CalorieDensityType get() {
        return calorieType ?: run {
            val density = calorieDensity ?: return CalorieDensityType.YELLOW
            when {
                density < 0.8f -> CalorieDensityType.GREEN
                density <= 2.5f -> CalorieDensityType.YELLOW
                else -> CalorieDensityType.RED
            }
        }
    }
}

@Serializable
data class MicroNutrients(
    val fiber: Int = 0,      // grams
    val sugar: Int = 0,      // grams
    val sodium: Int = 0,     // mg
    val cholesterol: Int = 0, // mg
    val vitaminC: Int = 0,   // mg
    val calcium: Int = 0,    // mg
    val iron: Int = 0        // mg
)

@Serializable
data class FoodScanResult(
    val id: String,
    val photoPath: String,
    val recognizedFoods: List<FoodItem>,
    val totalCalories: Int,
    val totalMacros: MacroNutrients,
    val confidence: Float, // 0.0 to 1.0
    val mealSuggestion: String, // AI-generated meal name
    val timestamp: Instant
)

@Serializable
data class FoodScanSummary(
    val id: String = "",
    val userId: String = "",
    val mealSuggestion: String = "",
    val totalCalories: Int = 0,
    val confidence: Float = 0f,
    val foodCount: Int = 0,
    val photoPath: String = "",
    val timestamp: String = "",
    val source: String = "photo"
)

@Serializable
data class NutritionGoals(
    val userId: String,
    val dailyCalories: Int,
    val macroGoals: MacroNutrients,
    val waterGoalMl: Int = 2000,
    val goalType: NutritionGoalType,
    val activityLevel: ActivityLevel,
    val calculatedAt: Instant
)

@Serializable
enum class NutritionGoalType {
    LOSE_WEIGHT,       // Calorie deficit
    MAINTAIN_WEIGHT,   // Maintenance calories
    GAIN_MUSCLE,       // Calorie surplus + high protein
    CUTTING,           // Body fat loss while maintaining muscle
    BULKING            // Muscle gain with controlled surplus
}

@Serializable
enum class ActivityLevel {
    SEDENTARY,         // Little to no exercise (BMR × 1.2)
    LIGHTLY_ACTIVE,    // 1-3 days/week (BMR × 1.375)
    MODERATELY_ACTIVE, // 3-5 days/week (BMR × 1.55)
    VERY_ACTIVE,       // 6-7 days/week (BMR × 1.725)
    EXTREMELY_ACTIVE   // Athlete/physical job (BMR × 1.9)
}

// Macro Calculator using Mifflin-St Jeor Equation
object MacroCalculator {

    /**
     * Calculate TDEE (Total Daily Energy Expenditure)
     */
    fun calculateTDEE(
        weight: Float,      // kg
        height: Float,      // cm
        age: Int,
        isMale: Boolean,
        activityLevel: ActivityLevel
    ): Int {
        // Mifflin-St Jeor Equation for BMR
        val bmr = if (isMale) {
            (10 * weight) + (6.25 * height) - (5 * age) + 5
        } else {
            (10 * weight) + (6.25 * height) - (5 * age) - 161
        }

        // Multiply by activity multiplier
        val tdee = bmr * activityLevel.multiplier()
        return tdee.toInt()
    }

    /**
     * Calculate macro distribution based on goal
     */
    fun calculateMacros(
        tdee: Int,
        weight: Float, // kg
        goalType: NutritionGoalType
    ): MacroNutrients {
        val targetCalories = when (goalType) {
            NutritionGoalType.LOSE_WEIGHT -> (tdee * 0.8).toInt()  // 20% deficit
            NutritionGoalType.MAINTAIN_WEIGHT -> tdee
            NutritionGoalType.GAIN_MUSCLE -> (tdee * 1.1).toInt()  // 10% surplus
            NutritionGoalType.CUTTING -> (tdee * 0.85).toInt()     // 15% deficit
            NutritionGoalType.BULKING -> (tdee * 1.15).toInt()     // 15% surplus
        }

        // Protein: 1.8-2.2g per kg for muscle building/retention
        val protein = when (goalType) {
            NutritionGoalType.GAIN_MUSCLE, NutritionGoalType.BULKING -> (weight * 2.2).toInt()
            NutritionGoalType.CUTTING -> (weight * 2.0).toInt()
            NutritionGoalType.LOSE_WEIGHT -> (weight * 1.8).toInt()
            NutritionGoalType.MAINTAIN_WEIGHT -> (weight * 1.6).toInt()
        }

        // Fat: 20-30% of calories
        val fatCalories = (targetCalories * 0.25).toInt()
        val fat = fatCalories / 9

        // Carbs: Remaining calories
        val proteinCalories = protein * 4
        val carbCalories = targetCalories - proteinCalories - fatCalories
        val carbs = carbCalories / 4

        return MacroNutrients(
            protein = protein,
            carbs = carbs,
            fat = fat
        )
    }

    /**
     * Calculate water goal based on weight
     * Rule: 30-40ml per kg of body weight
     */
    fun calculateWaterGoal(weight: Float): Int {
        return (weight * 35).toInt() // ml
    }
}

// Extension function for activity level multiplier
fun ActivityLevel.multiplier(): Double = when (this) {
    ActivityLevel.SEDENTARY -> 1.2
    ActivityLevel.LIGHTLY_ACTIVE -> 1.375
    ActivityLevel.MODERATELY_ACTIVE -> 1.55
    ActivityLevel.VERY_ACTIVE -> 1.725
    ActivityLevel.EXTREMELY_ACTIVE -> 1.9
}

/**
 * Calorie Density Calculator (Noom-inspired)
 * Helps users understand food volume vs calories
 */
object CalorieDensityHelper {

    /**
     * Calculate calorie density for any food
     * @param calories Total calories
     * @param grams Weight in grams
     * @return Calories per gram
     */
    fun calculateDensity(calories: Int, grams: Float): Float {
        return calories / grams
    }

    /**
     * Get color type based on density
     */
    fun getColorType(density: Float): CalorieDensityType {
        return when {
            density < 0.8f -> CalorieDensityType.GREEN
            density <= 2.5f -> CalorieDensityType.YELLOW
            else -> CalorieDensityType.RED
        }
    }

    /**
     * Get emoji representation of color type
     */
    fun getEmoji(type: CalorieDensityType): String = when (type) {
        CalorieDensityType.GREEN -> "🟢"
        CalorieDensityType.YELLOW -> "🟡"
        CalorieDensityType.RED -> "🔴"
    }

    /**
     * Get user-friendly description
     */
    fun getDescription(type: CalorieDensityType): String = when (type) {
        CalorieDensityType.GREEN -> "Eat freely - High volume, low calories"
        CalorieDensityType.YELLOW -> "Eat in moderation - Balanced nutrition"
        CalorieDensityType.RED -> "Eat in small portions - High calorie density"
    }

    /**
     * Analyze a meal's color distribution
     */
    fun analyzeMeal(foods: List<FoodItem>): MealColorAnalysis {
        val greenFoods = foods.filter { it.autoCalorieType == CalorieDensityType.GREEN }
        val yellowFoods = foods.filter { it.autoCalorieType == CalorieDensityType.YELLOW }
        val redFoods = foods.filter { it.autoCalorieType == CalorieDensityType.RED }

        val totalCalories = foods.sumOf { it.calories }
        val greenCalories = greenFoods.sumOf { it.calories }
        val yellowCalories = yellowFoods.sumOf { it.calories }
        val redCalories = redFoods.sumOf { it.calories }

        return MealColorAnalysis(
            greenCount = greenFoods.size,
            yellowCount = yellowFoods.size,
            redCount = redFoods.size,
            greenCaloriesPercent = if (totalCalories > 0) (greenCalories.toFloat() / totalCalories * 100).toInt() else 0,
            yellowCaloriesPercent = if (totalCalories > 0) (yellowCalories.toFloat() / totalCalories * 100).toInt() else 0,
            redCaloriesPercent = if (totalCalories > 0) (redCalories.toFloat() / totalCalories * 100).toInt() else 0,
            feedback = generateMealFeedback(greenCalories, yellowCalories, redCalories, totalCalories)
        )
    }

    private fun generateMealFeedback(green: Int, yellow: Int, red: Int, total: Int): String {
        val greenPercent = if (total > 0) green.toFloat() / total * 100 else 0f
        val redPercent = if (total > 0) red.toFloat() / total * 100 else 0f

        return when {
            greenPercent >= 50 -> "Excellent! Your meal is mostly green foods - filling and nutritious!"
            greenPercent >= 30 -> "Great balance! Good mix of green and other foods."
            redPercent >= 50 -> "Heads up: This meal is calorie-dense. Consider adding more green foods next time."
            else -> "Decent meal! Try adding more vegetables or fruits (green foods) to feel fuller."
        }
    }
}

@Serializable
data class MealColorAnalysis(
    val greenCount: Int,
    val yellowCount: Int,
    val redCount: Int,
    val greenCaloriesPercent: Int,
    val yellowCaloriesPercent: Int,
    val redCaloriesPercent: Int,
    val feedback: String
)

// Common foods database with calorie density (Noom-style color coding)
object CommonFoods {
    val POPULAR_FOODS = listOf(
        // 🟢 GREEN FOODS (< 0.8 cal/g)
        FoodItem(
            id = "broccoli",
            name = "Broccoli",
            serving = "1 cup steamed",
            calories = 55,
            macros = MacroNutrients(protein = 4, carbs = 11, fat = 1),
            weightGrams = 156f,
            calorieType = CalorieDensityType.GREEN  // 0.35 cal/g
        ),
        FoodItem(
            id = "watermelon",
            name = "Watermelon",
            serving = "1 cup cubed",
            calories = 46,
            macros = MacroNutrients(protein = 1, carbs = 12, fat = 0),
            weightGrams = 152f,
            calorieType = CalorieDensityType.GREEN  // 0.30 cal/g
        ),
        FoodItem(
            id = "strawberries",
            name = "Strawberries",
            serving = "1 cup",
            calories = 49,
            macros = MacroNutrients(protein = 1, carbs = 12, fat = 0),
            weightGrams = 150f,
            calorieType = CalorieDensityType.GREEN  // 0.33 cal/g
        ),
        FoodItem(
            id = "spinach",
            name = "Spinach",
            serving = "2 cups raw",
            calories = 14,
            macros = MacroNutrients(protein = 2, carbs = 2, fat = 0),
            weightGrams = 60f,
            calorieType = CalorieDensityType.GREEN  // 0.23 cal/g
        ),

        // 🟡 YELLOW FOODS (0.8-2.5 cal/g)
        FoodItem(
            id = "eggs_scrambled",
            name = "Scrambled Eggs",
            serving = "2 large eggs",
            calories = 180,
            macros = MacroNutrients(protein = 13, carbs = 2, fat = 14),
            weightGrams = 122f,
            calorieType = CalorieDensityType.YELLOW  // 1.47 cal/g
        ),
        FoodItem(
            id = "chicken_breast",
            name = "Chicken Breast",
            serving = "100g grilled",
            calories = 165,
            macros = MacroNutrients(protein = 31, carbs = 0, fat = 4),
            weightGrams = 100f,
            calorieType = CalorieDensityType.YELLOW  // 1.65 cal/g
        ),
        FoodItem(
            id = "brown_rice",
            name = "Brown Rice",
            serving = "1 cup cooked",
            calories = 216,
            macros = MacroNutrients(protein = 5, carbs = 45, fat = 2),
            weightGrams = 195f,
            calorieType = CalorieDensityType.YELLOW  // 1.11 cal/g
        ),
        FoodItem(
            id = "oatmeal",
            name = "Oatmeal",
            serving = "1 cup cooked",
            calories = 154,
            macros = MacroNutrients(protein = 6, carbs = 27, fat = 3),
            weightGrams = 234f,
            calorieType = CalorieDensityType.GREEN  // 0.66 cal/g
        ),
        FoodItem(
            id = "banana",
            name = "Banana",
            serving = "1 medium",
            calories = 105,
            macros = MacroNutrients(protein = 1, carbs = 27, fat = 0),
            weightGrams = 118f,
            calorieType = CalorieDensityType.YELLOW  // 0.89 cal/g
        ),
        FoodItem(
            id = "sweet_potato",
            name = "Sweet Potato",
            serving = "1 medium baked",
            calories = 103,
            macros = MacroNutrients(protein = 2, carbs = 24, fat = 0),
            weightGrams = 114f,
            calorieType = CalorieDensityType.YELLOW  // 0.90 cal/g
        ),
        FoodItem(
            id = "greek_yogurt",
            name = "Greek Yogurt",
            serving = "1 cup plain",
            calories = 100,
            macros = MacroNutrients(protein = 17, carbs = 6, fat = 0),
            weightGrams = 170f,
            calorieType = CalorieDensityType.GREEN  // 0.59 cal/g
        ),
        FoodItem(
            id = "salmon",
            name = "Salmon",
            serving = "100g grilled",
            calories = 206,
            macros = MacroNutrients(protein = 22, carbs = 0, fat = 13),
            weightGrams = 100f,
            calorieType = CalorieDensityType.YELLOW  // 2.06 cal/g
        ),

        // 🔴 RED FOODS (> 2.5 cal/g)
        FoodItem(
            id = "peanut_butter",
            name = "Peanut Butter",
            serving = "2 tbsp",
            calories = 188,
            macros = MacroNutrients(protein = 8, carbs = 6, fat = 16),
            weightGrams = 32f,
            calorieType = CalorieDensityType.RED  // 5.88 cal/g
        ),
        FoodItem(
            id = "almonds",
            name = "Almonds",
            serving = "1/4 cup",
            calories = 207,
            macros = MacroNutrients(protein = 8, carbs = 7, fat = 18),
            weightGrams = 36f,
            calorieType = CalorieDensityType.RED  // 5.75 cal/g
        ),
        FoodItem(
            id = "dark_chocolate",
            name = "Dark Chocolate",
            serving = "1 oz",
            calories = 155,
            macros = MacroNutrients(protein = 2, carbs = 13, fat = 12),
            weightGrams = 28f,
            calorieType = CalorieDensityType.RED  // 5.54 cal/g
        ),
        FoodItem(
            id = "protein_shake",
            name = "Whey Protein Shake",
            serving = "1 scoop",
            calories = 120,
            macros = MacroNutrients(protein = 24, carbs = 3, fat = 1),
            weightGrams = 30f,
            calorieType = CalorieDensityType.RED  // 4.0 cal/g
        ),
        FoodItem(
            id = "olive_oil",
            name = "Olive Oil",
            serving = "1 tbsp",
            calories = 119,
            macros = MacroNutrients(protein = 0, carbs = 0, fat = 14),
            weightGrams = 14f,
            calorieType = CalorieDensityType.RED  // 8.5 cal/g
        ),
        FoodItem(
            id = "pizza",
            name = "Pepperoni Pizza",
            serving = "1 slice",
            calories = 298,
            macros = MacroNutrients(protein = 13, carbs = 34, fat = 12),
            weightGrams = 112f,
            calorieType = CalorieDensityType.RED  // 2.66 cal/g
        )
    )

    /**
     * Get foods by color type
     */
    fun getByColorType(type: CalorieDensityType): List<FoodItem> {
        return POPULAR_FOODS.filter { it.calorieType == type }
    }

    /**
     * Get green foods (eat freely)
     */
    val greenFoods: List<FoodItem> get() = getByColorType(CalorieDensityType.GREEN)

    /**
     * Get yellow foods (moderate)
     */
    val yellowFoods: List<FoodItem> get() = getByColorType(CalorieDensityType.YELLOW)

    /**
     * Get red foods (small portions)
     */
    val redFoods: List<FoodItem> get() = getByColorType(CalorieDensityType.RED)
}
