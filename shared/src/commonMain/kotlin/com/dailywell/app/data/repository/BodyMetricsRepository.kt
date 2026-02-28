package com.dailywell.app.data.repository

import com.dailywell.app.data.model.BodyMetrics
import com.dailywell.app.data.model.BodyMeasurements
import com.dailywell.app.data.model.ProgressPhoto
import com.dailywell.app.data.model.PhotoAngle
import com.dailywell.app.data.model.BMICategory
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlin.math.pow
import kotlinx.serialization.Serializable

/**
 * Body Metrics Repository - Complete Backend Layer
 *
 * PERFECTION MODE: Full body tracking system
 * - Weight logging with trends
 * - Body measurements tracking
 * - Progress photos management
 * - BMI & body composition calculations
 * - Real-time Flow updates
 * - Weekly change analytics
 * - Goal tracking
 *
 * Quality Standard: Better than MyFitnessPal's body tracking
 */
class BodyMetricsRepository {
    private val firestore = Firebase.firestore
    private val bodyMetricsCollection = firestore.collection("body_metrics")
    private val measurementsCollection = firestore.collection("body_measurements")
    private val progressPhotosCollection = firestore.collection("progress_photos")
    private val goalsCollection = firestore.collection("body_goals")

    /**
     * Log daily weight
     */
    suspend fun logWeight(
        userId: String,
        weight: Float,
        unit: WeightUnit = WeightUnit.LBS,
        date: String = Clock.System.now().toString(),
        note: String = ""
    ): Result<BodyMetrics> {
        return try {
            val weightInKg = if (unit == WeightUnit.LBS) weight * 0.453592f else weight
            val weightInLbs = if (unit == WeightUnit.KG) weight * 2.20462f else weight

            // Get user's height for BMI calculation
            val userGoal = getUserGoal(userId).getOrNull()
            val heightCm = userGoal?.heightCm ?: 170f // Default if not set

            // Calculate BMI
            val bmi = calculateBMI(weightInKg, heightCm)
            val bmiCategory = getBMICategory(bmi)

            val metrics = BodyMetrics(
                id = "${userId}_${date.take(10)}",
                userId = userId,
                date = date.take(10),
                weight = if (unit == WeightUnit.LBS) weight else weightInLbs,
                weightKg = weightInKg,
                weightLbs = weightInLbs,
                bmi = bmi,
                bmiCategory = bmiCategory,
                bodyFatPercentage = null, // Can be updated later
                muscleMassKg = null,
                note = note
            )

            bodyMetricsCollection.document(metrics.id).set(metrics)
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to log weight: ${e.message}"))
        }
    }

    /**
     * Log body measurements
     */
    suspend fun logMeasurements(
        userId: String,
        measurements: BodyMeasurements,
        date: String = Clock.System.now().toString()
    ): Result<BodyMeasurements> {
        return try {
            val fullMeasurements = measurements.copy(
                id = "${userId}_${date.take(10)}",
                userId = userId,
                date = date
            )

            measurementsCollection.document(fullMeasurements.id).set(fullMeasurements)
            Result.success(fullMeasurements)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to log measurements: ${e.message}"))
        }
    }

    /**
     * Upload progress photo
     */
    suspend fun saveProgressPhoto(
        userId: String,
        photoUrl: String,
        photoAngle: PhotoAngle,
        date: String = Clock.System.now().toString(),
        note: String = ""
    ): Result<ProgressPhoto> {
        return try {
            val photo = ProgressPhoto(
                id = "${userId}_${Clock.System.now().toEpochMilliseconds()}",
                userId = userId,
                photoUrl = photoUrl,
                photoPath = photoUrl,
                angle = photoAngle,
                photoType = photoAngle,
                date = date.take(10),
                note = note
            )

            progressPhotosCollection.document(photo.id).set(photo)
            Result.success(photo)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save photo: ${e.message}"))
        }
    }

    /**
     * Get weight history for date range
     */
    suspend fun getWeightHistory(
        userId: String,
        days: Int = 30
    ): Result<List<BodyMetrics>> {
        return try {
            val now = Clock.System.now()
            val startDate = now.minus(DateTimePeriod(days = days), TimeZone.currentSystemDefault())
            val startDateString = startDate.toString().take(10)

            val snapshot = bodyMetricsCollection
                .where { "userId" equalTo userId }
                .where { "date" greaterThanOrEqualTo startDateString }
                .orderBy("date")
                .get()

            val metrics = snapshot.documents.mapNotNull { doc ->
                doc.data<BodyMetrics>()
            }

            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get weight history: ${e.message}"))
        }
    }

    /**
     * Get latest weight entry
     */
    suspend fun getLatestWeight(userId: String): Result<BodyMetrics?> {
        return try {
            val snapshot = bodyMetricsCollection
                .where { "userId" equalTo userId }
                .orderBy("date", Direction.DESCENDING)
                .get()

            val metrics = snapshot.documents.firstOrNull()?.data<BodyMetrics>()
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get latest weight: ${e.message}"))
        }
    }

    /**
     * Observe weight in real-time
     */
    fun observeWeight(userId: String): Flow<BodyMetrics?> {
        return bodyMetricsCollection
            .where { "userId" equalTo userId }
            .orderBy("date", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.data<BodyMetrics>()
            }
            .catch { emit(null) }
    }

    /**
     * Calculate weekly weight change
     */
    suspend fun getWeeklyChange(userId: String): Result<WeightChange> {
        return try {
            val now = Clock.System.now()
            val oneWeekAgo = now.minus(DateTimePeriod(days = 7), TimeZone.currentSystemDefault())

            // Get today's weight
            val todayWeight = getLatestWeight(userId).getOrNull()

            // Get weight from 7 days ago
            val oneWeekAgoDateString = oneWeekAgo.toString().take(10)
            val snapshot = bodyMetricsCollection
                .where { "userId" equalTo userId }
                .where { "date" lessThanOrEqualTo oneWeekAgoDateString }
                .orderBy("date", Direction.DESCENDING)
                .get()

            val oneWeekAgoWeight = snapshot.documents.firstOrNull()?.data<BodyMetrics>()

            val change = if (todayWeight != null && oneWeekAgoWeight != null) {
                WeightChange(
                    changeLbs = todayWeight.weightLbs - oneWeekAgoWeight.weightLbs,
                    changeKg = todayWeight.weightKg - oneWeekAgoWeight.weightKg,
                    changePercentage = ((todayWeight.weightKg - oneWeekAgoWeight.weightKg) / oneWeekAgoWeight.weightKg) * 100,
                    days = 7
                )
            } else {
                WeightChange(changeLbs = 0f, changeKg = 0f, changePercentage = 0f, days = 7)
            }

            Result.success(change)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to calculate weekly change: ${e.message}"))
        }
    }

    /**
     * Get measurement history
     */
    suspend fun getMeasurementHistory(
        userId: String,
        days: Int = 90
    ): Result<List<BodyMeasurements>> {
        return try {
            val now = Clock.System.now()
            val startDate = now.minus(DateTimePeriod(days = days), TimeZone.currentSystemDefault())
            val startDateString = startDate.toString().take(10)

            val snapshot = measurementsCollection
                .where { "userId" equalTo userId }
                .where { "date" greaterThanOrEqualTo startDateString }
                .orderBy("date")
                .get()

            val measurements = snapshot.documents.mapNotNull { doc ->
                doc.data<BodyMeasurements>()
            }

            Result.success(measurements)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get measurement history: ${e.message}"))
        }
    }

    /**
     * Get latest measurements
     */
    suspend fun getLatestMeasurements(userId: String): Result<BodyMeasurements?> {
        return try {
            val snapshot = measurementsCollection
                .where { "userId" equalTo userId }
                .orderBy("date", Direction.DESCENDING)
                .get()

            val measurements = snapshot.documents.firstOrNull()?.data<BodyMeasurements>()
            Result.success(measurements)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get latest measurements: ${e.message}"))
        }
    }

    /**
     * Get progress photos
     */
    suspend fun getProgressPhotos(
        userId: String,
        photoAngle: PhotoAngle? = null
    ): Result<List<ProgressPhoto>> {
        return try {
            val query = if (photoAngle != null) {
                progressPhotosCollection
                    .where { "userId" equalTo userId }
                    .where { "angle" equalTo photoAngle.name }
            } else {
                progressPhotosCollection.where { "userId" equalTo userId }
            }

            val snapshot = query.orderBy("date", Direction.DESCENDING).get()

            val photos = snapshot.documents.mapNotNull { doc ->
                doc.data<ProgressPhoto>()
            }

            Result.success(photos)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get progress photos: ${e.message}"))
        }
    }

    /**
     * Set body goal
     */
    suspend fun setGoal(
        userId: String,
        targetWeightKg: Float,
        targetDate: String,
        heightCm: Float,
        currentWeightKg: Float
    ): Result<BodyGoal> {
        return try {
            val goal = BodyGoal(
                id = userId,
                userId = userId,
                targetWeightKg = targetWeightKg,
                targetWeightLbs = targetWeightKg * 2.20462f,
                targetDate = targetDate,
                heightCm = heightCm,
                startWeightKg = currentWeightKg,
                startWeightLbs = currentWeightKg * 2.20462f,
                createdAt = Clock.System.now().toString()
            )

            goalsCollection.document(goal.id).set(goal)
            Result.success(goal)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to set goal: ${e.message}"))
        }
    }

    /**
     * Get user's goal
     */
    suspend fun getUserGoal(userId: String): Result<BodyGoal?> {
        return try {
            val snapshot = goalsCollection.document(userId).get()
            val goal = if (snapshot.exists) snapshot.data<BodyGoal>() else null
            Result.success(goal)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get goal: ${e.message}"))
        }
    }

    /**
     * Calculate goal progress
     */
    suspend fun getGoalProgress(userId: String): Result<GoalProgress?> {
        return try {
            val goal = getUserGoal(userId).getOrNull() ?: return Result.success(null)
            val currentWeight = getLatestWeight(userId).getOrNull() ?: return Result.success(null)

            val totalToLose = goal.startWeightKg - goal.targetWeightKg
            val lostSoFar = goal.startWeightKg - currentWeight.weightKg
            val percentComplete = if (totalToLose > 0) (lostSoFar / totalToLose) * 100 else 0f
            val remainingKg = currentWeight.weightKg - goal.targetWeightKg

            // Calculate days remaining
            val targetDate = Instant.parse(goal.targetDate)
            val now = Clock.System.now()
            val daysRemaining = ((targetDate.toEpochMilliseconds() - now.toEpochMilliseconds()) / (1000 * 60 * 60 * 24)).toInt()

            val progress = GoalProgress(
                percentComplete = percentComplete.coerceIn(0f, 100f),
                remainingKg = remainingKg,
                remainingLbs = remainingKg * 2.20462f,
                daysRemaining = daysRemaining,
                isOnTrack = lostSoFar > 0 && daysRemaining > 0
            )

            Result.success(progress)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to calculate goal progress: ${e.message}"))
        }
    }

    /**
     * Update body composition (body fat %, muscle mass)
     */
    suspend fun updateBodyComposition(
        userId: String,
        date: String,
        bodyFatPercentage: Float?,
        muscleMassKg: Float?
    ): Result<BodyMetrics> {
        return try {
            val docId = "${userId}_${date.take(10)}"
            val snapshot = bodyMetricsCollection.document(docId).get()
            if (!snapshot.exists) {
                Result.failure(Exception("No weight entry found for this date"))
            } else {
                val existing = snapshot.data<BodyMetrics>()
                val updated = existing.copy(
                    bodyFatPercentage = bodyFatPercentage,
                    muscleMassKg = muscleMassKg
                )
                bodyMetricsCollection.document(docId).set(updated)
                Result.success(updated)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update body composition: ${e.message}"))
        }
    }

    /**
     * Delete weight entry
     */
    suspend fun deleteWeight(userId: String, date: String): Result<Unit> {
        return try {
            val docId = "${userId}_${date.take(10)}"
            bodyMetricsCollection.document(docId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete weight: ${e.message}"))
        }
    }

    /**
     * Delete progress photo
     */
    suspend fun deleteProgressPhoto(photoId: String): Result<Unit> {
        return try {
            progressPhotosCollection.document(photoId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete photo: ${e.message}"))
        }
    }

    // ========== HELPER FUNCTIONS ==========

    /**
     * Calculate BMI from weight (kg) and height (cm)
     */
    private fun calculateBMI(weightKg: Float, heightCm: Float): Float {
        val heightM = heightCm / 100f
        return weightKg / (heightM * heightM)
    }

    /**
     * Get BMI category
     */
    private fun getBMICategory(bmi: Float): BMICategory {
        return when {
            bmi < 18.5f -> BMICategory.UNDERWEIGHT
            bmi < 25.0f -> BMICategory.NORMAL
            bmi < 30.0f -> BMICategory.OVERWEIGHT
            else -> BMICategory.OBESE
        }
    }

    /**
     * Estimate body fat percentage (Navy Method)
     * Requires waist, neck, height (and hip for females)
     */
    fun estimateBodyFat(
        isMale: Boolean,
        heightCm: Float,
        waistCm: Float,
        neckCm: Float,
        hipCm: Float? = null
    ): Float {
        return if (isMale) {
            // Male formula: 495 / (1.0324 - 0.19077 * log10(waist - neck) + 0.15456 * log10(height)) - 450
            val waistNeckDiff = waistCm - neckCm
            val logWaistNeck = kotlin.math.log10(waistNeckDiff.toDouble())
            val logHeight = kotlin.math.log10(heightCm.toDouble())
            val bodyDensity = 1.0324 - 0.19077 * logWaistNeck + 0.15456 * logHeight
            (495 / bodyDensity - 450).toFloat()
        } else {
            // Female formula requires hip measurement
            if (hipCm == null) return 0f
            val waistHipNeckSum = waistCm + hipCm - neckCm
            val logWaistHipNeck = kotlin.math.log10(waistHipNeckSum.toDouble())
            val logHeight = kotlin.math.log10(heightCm.toDouble())
            val bodyDensity = 1.29579 - 0.35004 * logWaistHipNeck + 0.22100 * logHeight
            (495 / bodyDensity - 450).toFloat()
        }
    }
}

// ========== DATA MODELS (Unique to Repository) ==========

@Serializable
data class WeightChange(
    val changeLbs: Float,
    val changeKg: Float,
    val changePercentage: Float,
    val days: Int
)

@Serializable
data class GoalProgress(
    val percentComplete: Float,
    val remainingKg: Float,
    val remainingLbs: Float,
    val daysRemaining: Int,
    val isOnTrack: Boolean
)

enum class WeightUnit {
    KG, LBS
}

// Note: BMICategory, ProgressPhoto, PhotoAngle are imported from BodyMetricsModels.kt

@Serializable
data class BodyGoal(
    val id: String,
    val userId: String,
    val targetWeightKg: Float,
    val targetWeightLbs: Float,
    val targetDate: String,
    val heightCm: Float,
    val startWeightKg: Float,
    val startWeightLbs: Float,
    val createdAt: String
)
