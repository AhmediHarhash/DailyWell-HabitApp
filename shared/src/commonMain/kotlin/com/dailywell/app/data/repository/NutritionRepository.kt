package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import com.dailywell.app.api.ClaudeFoodVisionApi
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*

/**
 * Repository for Nutrition & Food Scanning
 * Handles all nutrition data persistence and Claude Vision API calls
 *
 * PERFECTION MODE: Complete backend integration
 */
class NutritionRepository(
    private val claudeApi: ClaudeFoodVisionApi,
    private val settingsRepository: SettingsRepository? = null,
    private val aiCoachingRepository: AICoachingRepository? = null
) {
    private val firestore = Firebase.firestore
    private val nutritionCollection = firestore.collection("nutrition")
    private val mealsCollection = firestore.collection("meals")
    private val scansCollection = firestore.collection("food_scans")
    private val scanUsageCollection = firestore.collection("food_scan_usage")

    companion object {
        private const val FREE_SONNET_SCANS_PER_MONTH = AIGovernancePolicy.FREE_SCAN_LIMIT_PER_MONTH
        private const val TRIAL_SONNET_SCANS_TOTAL = AIGovernancePolicy.TRIAL_SCAN_LIMIT_TOTAL
    }

    private val fallbackTrialQuota = mutableMapOf<String, Int>()

    /**
     * Scan food photo with Claude Vision AI
     * Returns results ready to be logged
     */
    suspend fun scanFoodPhoto(
        userId: String,
        imageBytes: ByteArray,
        mealType: MealType? = null,
        photoPath: String? = null
    ): Result<FoodScanResult> {
        val access = resolveScanAccess()
        val reservation = reserveScanQuotaIfNeeded(userId, access)
        if (reservation.isFailure) {
            return Result.failure(reservation.exceptionOrNull() ?: Exception("Food scan limit reached"))
        }

        val estimatedRawScanCost = AIGovernancePolicy.rawCloudCostUsd(
            model = AIModelUsed.CLAUDE_SONNET,
            inputTokens = AIGovernancePolicy.FOOD_SCAN_ESTIMATED_INPUT_TOKENS,
            outputTokens = AIGovernancePolicy.FOOD_SCAN_ESTIMATED_OUTPUT_TOKENS
        )

        if (access.isPaidPremium) {
            val canSpend = aiCoachingRepository?.canSpendCloudCost(estimatedRawScanCost) ?: true
            if (!canSpend) {
                releaseReservedScanQuotaIfNeeded(reservation.getOrNull())
                return Result.failure(
                    Exception("You've reached this month's AI budget for scans. It resets next billing cycle.")
                )
            }
        }

        return try {
            val scanResult = claudeApi.analyzeFoodImage(imageBytes, mealType).getOrElse { throw it }

            // Update photo path
            val finalResult = scanResult.copy(photoPath = photoPath ?: "")

            // Save scan result for history
            saveScanResult(userId, finalResult)

            if (access.isPaidPremium || access.isTrial) {
                aiCoachingRepository?.trackExternalCloudUsage(
                    inputTokens = AIGovernancePolicy.FOOD_SCAN_ESTIMATED_INPUT_TOKENS,
                    outputTokens = AIGovernancePolicy.FOOD_SCAN_ESTIMATED_OUTPUT_TOKENS,
                    model = AIModelUsed.CLAUDE_SONNET,
                    category = "FOOD_SCAN"
                )
            }

            Result.success(finalResult)
        } catch (e: Exception) {
            // Best effort rollback so failed requests do not consume a monthly scan slot.
            releaseReservedScanQuotaIfNeeded(reservation.getOrNull())
            Result.failure(e)
        }
    }

    private suspend fun resolveScanAccess(): ScanAccess {
        return try {
            val settings = settingsRepository?.getSettingsSnapshot()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            val isTrial = settings?.isTrialActive(today) == true && settings.isPremium != true
            ScanAccess(
                isPaidPremium = settings?.isPremium == true,
                isTrial = isTrial
            )
        } catch (_: Exception) {
            ScanAccess()
        }
    }

    private suspend fun reserveScanQuotaIfNeeded(
        userId: String,
        access: ScanAccess
    ): Result<ScanQuotaReservation> {
        if (access.isPaidPremium) {
            return Result.success(ScanQuotaReservation(mode = QuotaMode.NONE))
        }

        if (access.isTrial) {
            return reserveTrialScanQuota(userId)
        }

        return reserveFreeScanQuota(userId)
    }

    private suspend fun reserveFreeScanQuota(userId: String): Result<ScanQuotaReservation> {
        return try {
            val monthKey = currentMonthKey()
            val docId = "${userId}_$monthKey"
            val usageDoc = scanUsageCollection.document(docId)
            val snapshot = usageDoc.get()

            val currentUsed = if (snapshot.exists) {
                runCatching { snapshot.data<FoodScanUsage>().sonnetScansUsed }
                    .getOrElse { 0 }
            } else 0

            if (currentUsed >= FREE_SONNET_SCANS_PER_MONTH) {
                return Result.failure(
                    Exception(
                        "You used your $FREE_SONNET_SCANS_PER_MONTH free AI food scans this month. More scans unlock with Premium."
                    )
                )
            }

            usageDoc.set(
                FoodScanUsage(
                    userId = userId,
                    month = monthKey,
                    sonnetScansUsed = currentUsed + 1,
                    monthlyLimit = FREE_SONNET_SCANS_PER_MONTH,
                    updatedAt = Clock.System.now().toString()
                )
            )

            Result.success(ScanQuotaReservation(mode = QuotaMode.FREE_MONTHLY, docId = docId))
        } catch (_: Exception) {
            Result.failure(Exception("Unable to verify food scan quota. Please try again."))
        }
    }

    private suspend fun reserveTrialScanQuota(userId: String): Result<ScanQuotaReservation> {
        return try {
            val docId = "${userId}_trial_total"
            val usageDoc = scanUsageCollection.document(docId)
            val snapshot = usageDoc.get()

            val currentUsed = if (snapshot.exists) {
                runCatching { snapshot.data<TrialFoodScanUsage>().totalScansUsed }
                    .getOrElse { 0 }
            } else 0

            if (currentUsed >= TRIAL_SONNET_SCANS_TOTAL) {
                return Result.failure(
                    Exception(
                        "Trial scan limit reached ($TRIAL_SONNET_SCANS_TOTAL total). Upgrade to continue scanning."
                    )
                )
            }

            usageDoc.set(
                TrialFoodScanUsage(
                    userId = userId,
                    totalScansUsed = currentUsed + 1,
                    totalLimit = TRIAL_SONNET_SCANS_TOTAL,
                    updatedAt = Clock.System.now().toString()
                )
            )

            Result.success(ScanQuotaReservation(mode = QuotaMode.TRIAL_TOTAL, docId = docId))
        } catch (_: Exception) {
            val fallbackKey = "${userId}_trial_total"
            val currentFallback = fallbackTrialQuota[fallbackKey] ?: 0
            if (currentFallback >= TRIAL_SONNET_SCANS_TOTAL) {
                return Result.failure(
                    Exception(
                        "Trial scan limit reached ($TRIAL_SONNET_SCANS_TOTAL total). Upgrade to continue scanning."
                    )
                )
            }

            fallbackTrialQuota[fallbackKey] = currentFallback + 1
            Result.success(ScanQuotaReservation(mode = QuotaMode.TRIAL_FALLBACK, docId = fallbackKey))
        }
    }

    private suspend fun releaseReservedScanQuotaIfNeeded(reservation: ScanQuotaReservation?) {
        val effectiveReservation = reservation ?: return
        if (effectiveReservation.mode == QuotaMode.NONE) return
        if (effectiveReservation.mode == QuotaMode.TRIAL_FALLBACK) {
            val fallbackKey = effectiveReservation.docId
            val currentFallback = fallbackTrialQuota[fallbackKey] ?: 0
            fallbackTrialQuota[fallbackKey] = (currentFallback - 1).coerceAtLeast(0)
            if (fallbackTrialQuota[fallbackKey] == 0) {
                fallbackTrialQuota.remove(fallbackKey)
            }
            return
        }

        try {
            val usageDoc = scanUsageCollection.document(effectiveReservation.docId)
            val snapshot = usageDoc.get()
            if (!snapshot.exists) return

            when (effectiveReservation.mode) {
                QuotaMode.FREE_MONTHLY -> {
                    val current = snapshot.data<FoodScanUsage>()
                    val correctedCount = (current.sonnetScansUsed - 1).coerceAtLeast(0)
                    usageDoc.set(
                        current.copy(
                            sonnetScansUsed = correctedCount,
                            updatedAt = Clock.System.now().toString()
                        )
                    )
                }
                QuotaMode.TRIAL_TOTAL -> {
                    val current = snapshot.data<TrialFoodScanUsage>()
                    val correctedCount = (current.totalScansUsed - 1).coerceAtLeast(0)
                    usageDoc.set(
                        current.copy(
                            totalScansUsed = correctedCount,
                            updatedAt = Clock.System.now().toString()
                        )
                    )
                }
                QuotaMode.TRIAL_FALLBACK -> Unit
                QuotaMode.NONE -> Unit
            }
        } catch (_: Exception) {
            // Ignore rollback failures; quota self-corrects on next successful update.
        }
    }

    private fun currentMonthKey(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val month = now.monthNumber.toString().padStart(2, '0')
        return "${now.year}-$month"
    }

    private data class ScanAccess(
        val isPaidPremium: Boolean = false,
        val isTrial: Boolean = false
    )

    private enum class QuotaMode {
        NONE,
        FREE_MONTHLY,
        TRIAL_TOTAL,
        TRIAL_FALLBACK
    }

    private data class ScanQuotaReservation(
        val mode: QuotaMode,
        val docId: String = ""
    )

    /**
     * Log meal with emotion tracking (Noom-inspired)
     */
    suspend fun logMeal(
        userId: String,
        scanResult: FoodScanResult,
        mealType: MealType,
        emotion: String? = null,
        hungerLevel: Int? = null,
        notes: String? = null
    ): Result<MealEntry> {
        return try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

            // Create meal entry
            val meal = MealEntry(
                id = generateId(),
                userId = userId,
                date = today,
                mealType = mealType,
                mealName = scanResult.mealSuggestion,
                foods = scanResult.recognizedFoods,
                totalCalories = scanResult.totalCalories,
                totalMacros = scanResult.totalMacros,
                photo = scanResult.photoPath.takeIf { it.isNotEmpty() },
                notes = notes,
                timestamp = scanResult.timestamp,
                scanMethod = ScanMethod.AI_PHOTO_SCAN,
                emotionBefore = emotion,
                hungerLevel = hungerLevel
            )

            // Save to Firestore
            mealsCollection.document(meal.id).set(meal)

            // Update daily nutrition totals
            updateDailyNutrition(userId, today, meal)

            Result.success(meal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update daily nutrition totals
     */
    private suspend fun updateDailyNutrition(
        userId: String,
        date: String,
        newMeal: MealEntry
    ) {
        val docId = "${userId}_$date"
        val doc = nutritionCollection.document(docId)

        try {
            val snapshot = doc.get()

            if (snapshot.exists) {
                // Update existing
                val current = snapshot.data<DailyNutrition>()
                val updated = current.copy(
                    caloriesConsumed = current.caloriesConsumed + newMeal.totalCalories,
                    macros = MacroNutrients(
                        protein = current.macros.protein + newMeal.totalMacros.protein,
                        carbs = current.macros.carbs + newMeal.totalMacros.carbs,
                        fat = current.macros.fat + newMeal.totalMacros.fat
                    ),
                    meals = current.meals + newMeal
                )
                doc.set(updated)
            } else {
                // Create new daily nutrition entry
                val goals = getUserNutritionGoals(userId)
                val dailyNutrition = DailyNutrition(
                    userId = userId,
                    date = date,
                    calorieGoal = goals?.dailyCalories ?: 2000,
                    caloriesConsumed = newMeal.totalCalories,
                    macros = newMeal.totalMacros,
                    macroGoals = goals?.macroGoals ?: MacroNutrients(protein = 150, carbs = 200, fat = 60),
                    meals = listOf(newMeal)
                )
                doc.set(dailyNutrition)
            }
        } catch (e: Exception) {
            // Log error but don't fail the meal logging
            println("Failed to update daily nutrition: ${e.message}")
        }
    }

    /**
     * Get user's nutrition goals
     */
    suspend fun getUserNutritionGoals(userId: String): NutritionGoals? {
        return try {
            val doc = firestore.collection("nutrition_goals").document(userId).get()
            if (doc.exists) {
                doc.data()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save nutrition goals for user
     */
    suspend fun saveNutritionGoals(goals: NutritionGoals): Result<Unit> {
        return try {
            firestore.collection("nutrition_goals")
                .document(goals.userId)
                .set(goals)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate and save nutrition goals based on user profile
     */
    suspend fun calculateAndSaveGoals(
        userId: String,
        weight: Float,
        height: Float,
        age: Int,
        isMale: Boolean,
        activityLevel: ActivityLevel,
        goalType: NutritionGoalType
    ): Result<NutritionGoals> {
        return try {
            val tdee = MacroCalculator.calculateTDEE(weight, height, age, isMale, activityLevel)
            val macros = MacroCalculator.calculateMacros(tdee, weight, goalType)
            val waterGoal = MacroCalculator.calculateWaterGoal(weight)

            val targetCalories = when (goalType) {
                NutritionGoalType.LOSE_WEIGHT -> (tdee * 0.8).toInt()
                NutritionGoalType.MAINTAIN_WEIGHT -> tdee
                NutritionGoalType.GAIN_MUSCLE -> (tdee * 1.1).toInt()
                NutritionGoalType.CUTTING -> (tdee * 0.85).toInt()
                NutritionGoalType.BULKING -> (tdee * 1.15).toInt()
            }

            val goals = NutritionGoals(
                userId = userId,
                dailyCalories = targetCalories,
                macroGoals = macros,
                waterGoalMl = waterGoal,
                goalType = goalType,
                activityLevel = activityLevel,
                calculatedAt = Clock.System.now()
            )

            saveNutritionGoals(goals)
            Result.success(goals)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get today's nutrition data
     */
    suspend fun getTodayNutrition(userId: String): DailyNutrition? {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        return getDailyNutrition(userId, today)
    }

    /**
     * Get nutrition data for specific date
     */
    suspend fun getDailyNutrition(userId: String, date: String): DailyNutrition? {
        return try {
            val docId = "${userId}_$date"
            val doc = nutritionCollection.document(docId).get()
            if (doc.exists) {
                doc.data()
            } else {
                // Return empty daily nutrition with goals
                val goals = getUserNutritionGoals(userId)
                DailyNutrition(
                    userId = userId,
                    date = date,
                    calorieGoal = goals?.dailyCalories ?: 2000,
                    caloriesConsumed = 0,
                    macros = MacroNutrients(),
                    macroGoals = goals?.macroGoals ?: MacroNutrients(protein = 150, carbs = 200, fat = 60),
                    meals = emptyList()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Observe today's nutrition data (real-time updates)
     */
    fun observeTodayNutrition(userId: String): Flow<DailyNutrition?> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        val docId = "${userId}_$today"

        return nutritionCollection.document(docId).snapshots.map { snapshot ->
            if (snapshot.exists) {
                snapshot.data()
            } else {
                null
            }
        }
    }

    /**
     * Get recent meals
     */
    suspend fun getRecentMeals(userId: String, limit: Int = 10): List<MealEntry> {
        return try {
            mealsCollection
                .where { "userId" equalTo userId }
                .orderBy("timestamp", dev.gitlive.firebase.firestore.Direction.DESCENDING)
                .limit(limit)
                .get()
                .documents
                .mapNotNull { it.data<MealEntry>() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get meals for date range (for weekly/monthly analysis)
     */
    suspend fun getMealsInRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<MealEntry> {
        return try {
            mealsCollection
                .where { "userId" equalTo userId }
                .where { "date" greaterThanOrEqualTo startDate }
                .where { "date" lessThanOrEqualTo endDate }
                .orderBy("timestamp", dev.gitlive.firebase.firestore.Direction.DESCENDING)
                .get()
                .documents
                .mapNotNull { it.data<MealEntry>() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Delete meal
     */
    suspend fun deleteMeal(userId: String, mealId: String): Result<Unit> {
        return try {
            // Get meal first
            val mealDoc = mealsCollection.document(mealId).get()
            if (!mealDoc.exists) {
                return Result.failure(Exception("Meal not found"))
            }

            val meal = mealDoc.data<MealEntry>()

            // Delete meal
            mealsCollection.document(mealId).delete()

            // Update daily totals
            val docId = "${userId}_${meal.date}"
            val dailyDoc = nutritionCollection.document(docId)
            val dailySnapshot = dailyDoc.get()

            if (dailySnapshot.exists) {
                val current = dailySnapshot.data<DailyNutrition>()
                val updated = current.copy(
                    caloriesConsumed = (current.caloriesConsumed - meal.totalCalories).coerceAtLeast(0),
                    macros = MacroNutrients(
                        protein = (current.macros.protein - meal.totalMacros.protein).coerceAtLeast(0),
                        carbs = (current.macros.carbs - meal.totalMacros.carbs).coerceAtLeast(0),
                        fat = (current.macros.fat - meal.totalMacros.fat).coerceAtLeast(0)
                    ),
                    meals = current.meals.filter { it.id != mealId }
                )
                dailyDoc.set(updated)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update water intake
     */
    suspend fun updateWaterIntake(userId: String, amountMl: Int): Result<Unit> {
        return try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            val docId = "${userId}_$today"
            val doc = nutritionCollection.document(docId)
            val snapshot = doc.get()

            if (snapshot.exists) {
                val current = snapshot.data<DailyNutrition>()
                val updated = current.copy(
                    waterIntake = current.waterIntake + amountMl
                )
                doc.set(updated)
            } else {
                // Create new entry with water only
                val goals = getUserNutritionGoals(userId)
                val dailyNutrition = DailyNutrition(
                    userId = userId,
                    date = today,
                    calorieGoal = goals?.dailyCalories ?: 2000,
                    macros = MacroNutrients(protein = 0, carbs = 0, fat = 0), // Starting at 0
                    macroGoals = goals?.macroGoals ?: MacroNutrients(protein = 150, carbs = 200, fat = 60),
                    waterIntake = amountMl,
                    waterGoal = goals?.waterGoalMl ?: 2000
                )
                doc.set(dailyNutrition)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save scan result for history
     */
    private suspend fun saveScanResult(userId: String, result: FoodScanResult) {
        try {
            scansCollection.document(result.id).set(
                FoodScanSummary(
                    id = result.id,
                    userId = userId,
                    mealSuggestion = result.mealSuggestion,
                    totalCalories = result.totalCalories,
                    confidence = result.confidence,
                    foodCount = result.recognizedFoods.size,
                    photoPath = result.photoPath,
                    timestamp = result.timestamp.toString(),
                    source = "photo"
                )
            )
        } catch (e: Exception) {
            // Don't fail the scan if saving history fails
            println("Failed to save scan result: ${e.message}")
        }
    }

    suspend fun getRecentScanSummaries(userId: String, limit: Int = 8): List<FoodScanSummary> {
        return try {
            scansCollection
                .where { "userId" equalTo userId }
                .orderBy("timestamp", dev.gitlive.firebase.firestore.Direction.DESCENDING)
                .limit(limit)
                .get()
                .documents
                .mapNotNull { it.data<FoodScanSummary>() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveExternalScanSummary(summary: FoodScanSummary): Result<Unit> {
        return try {
            scansCollection.document(summary.id).set(summary)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analyze eating patterns (emotion tracking insights)
     * Noom-inspired: Find correlations between emotions and eating
     */
    suspend fun analyzeEatingPatterns(userId: String, daysBack: Int = 30): EatingPatternInsights? {
        return try {
            val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val startDate = endDate.minus(daysBack, DateTimeUnit.DAY)

            val meals = getMealsInRange(
                userId,
                startDate.toString(),
                endDate.toString()
            )

            // Analyze emotions
            val emotionMeals = meals.filter { it.emotionBefore != null }
            if (emotionMeals.isEmpty()) return null

            val emotionGroups = emotionMeals.groupBy { it.emotionBefore }
            val avgCaloriesByEmotion = emotionGroups.mapValues { (_, meals) ->
                meals.map { it.totalCalories }.average().toInt()
            }

            val mostCommonEmotion = emotionGroups.maxByOrNull { it.value.size }?.key
            val highestCalorieEmotion = avgCaloriesByEmotion.maxByOrNull { it.value }?.key

            EatingPatternInsights(
                daysAnalyzed = daysBack,
                totalMealsLogged = meals.size,
                mealsWithEmotions = emotionMeals.size,
                mostCommonEmotion = mostCommonEmotion,
                highestCalorieEmotion = highestCalorieEmotion,
                avgCaloriesByEmotion = avgCaloriesByEmotion,
                insight = generatePatternInsight(mostCommonEmotion, highestCalorieEmotion, avgCaloriesByEmotion)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun generatePatternInsight(
        mostCommon: String?,
        highestCalorie: String?,
        avgByEmotion: Map<String?, Int>
    ): String {
        return when {
            highestCalorie != null && mostCommon != highestCalorie ->
                "You eat ${"%.0f".format(avgByEmotion[highestCalorie])} calories when $highestCalorie, compared to ${"%.0f".format(avgByEmotion.values.average())} on average. This might be emotional eating."

            mostCommon != null ->
                "You most often eat when $mostCommon. Understanding your triggers is the first step to better habits."

            else ->
                "Keep logging your emotions to discover your eating patterns!"
        }
    }

    private fun generateId(): String {
        return "meal_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}

/**
 * Eating pattern insights from emotion tracking
 */
data class EatingPatternInsights(
    val daysAnalyzed: Int,
    val totalMealsLogged: Int,
    val mealsWithEmotions: Int,
    val mostCommonEmotion: String?,
    val highestCalorieEmotion: String?,
    val avgCaloriesByEmotion: Map<String?, Int>,
    val insight: String
)

@kotlinx.serialization.Serializable
private data class FoodScanUsage(
    val userId: String = "",
    val month: String = "",
    val sonnetScansUsed: Int = 0,
    val monthlyLimit: Int = AIGovernancePolicy.FREE_SCAN_LIMIT_PER_MONTH,
    val updatedAt: String = ""
)

@kotlinx.serialization.Serializable
private data class TrialFoodScanUsage(
    val userId: String = "",
    val totalScansUsed: Int = 0,
    val totalLimit: Int = AIGovernancePolicy.TRIAL_SCAN_LIMIT_TOTAL,
    val updatedAt: String = ""
)
