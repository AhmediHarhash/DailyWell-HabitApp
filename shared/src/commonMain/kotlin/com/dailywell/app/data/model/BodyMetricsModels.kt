package com.dailywell.app.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Feature #12: Body Metrics & Progress Tracking
 * Weight, measurements, body composition, and progress photos
 *
 * Note: `id: String = ""` and similar empty-string defaults are intentional.
 * Kotlin serialization requires default values for fields that may be absent in JSON.
 * Using "" instead of null avoids nullable types throughout the codebase while still
 * allowing @Serializable deserialization to succeed when the field is missing.
 */

@Serializable
data class BodyMetrics(
    val id: String = "",
    val userId: String,
    val date: String,
    val weight: Float = 0f,         // Primary weight in user's preferred unit
    val weightKg: Float = 0f,       // Weight in kg
    val weightLbs: Float = 0f,      // Weight in lbs
    val bmi: Float = 0f,
    val bmiCategory: BMICategory = BMICategory.NORMAL,
    val bodyFatPercentage: Float? = null,
    val muscleMass: Float? = null,  // kg or lbs
    val muscleMassKg: Float? = null,
    val measurements: BodyMeasurements? = null,
    val notes: String? = null,
    val note: String = "",          // Alternative field name for compatibility
    val timestamp: Instant? = null
)

@Serializable
data class BodyMeasurements(
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val neck: Float? = null,        // cm or inches
    val chest: Float? = null,
    val waist: Float? = null,
    val hips: Float? = null,
    val leftBicep: Float? = null,
    val rightBicep: Float? = null,
    val leftThigh: Float? = null,
    val rightThigh: Float? = null,
    val leftCalf: Float? = null,
    val rightCalf: Float? = null,
    val unit: MeasurementUnit = MeasurementUnit.METRIC
)

@Serializable
enum class MeasurementUnit {
    METRIC,    // cm, kg
    IMPERIAL   // inches, lbs
}

@Serializable
data class ProgressPhoto(
    val id: String,
    val userId: String,
    val date: String,
    val photoPath: String = "",
    val photoUrl: String = "",      // Alternative field for URL storage
    val angle: PhotoAngle = PhotoAngle.FRONT,
    val photoType: PhotoAngle = PhotoAngle.FRONT, // Alias for compatibility
    val weight: Float? = null,
    val bodyFat: Float? = null,
    val notes: String? = null,
    val note: String = "",          // Alternative field name
    val timestamp: Instant? = null
)

@Serializable
enum class PhotoAngle {
    FRONT,
    SIDE,
    BACK,
    FLEX_FRONT,
    FLEX_BACK,
    OTHER
}

@Serializable
data class ProgressComparison(
    val startPhoto: ProgressPhoto,
    val currentPhoto: ProgressPhoto,
    val daysBetween: Int,
    val weightChange: Float,
    val bodyFatChange: Float?,
    val summary: ComparisonSummary
)

@Serializable
data class ComparisonSummary(
    val weightLost: Float = 0f,      // Positive = lost, negative = gained
    val fatLost: Float = 0f,
    val muscleGained: Float = 0f,
    val totalInchesLost: Float = 0f,
    val achievement: String
)

@Serializable
data class WeightHistory(
    val userId: String,
    val entries: List<BodyMetrics>,
    val startingWeight: Float,
    val currentWeight: Float,
    val goalWeight: Float? = null,
    val trend: WeightTrend
) {
    val totalChange: Float get() = currentWeight - startingWeight
    val progressToGoal: Float? get() = goalWeight?.let {
        val totalNeeded = kotlin.math.abs(it - startingWeight)
        val achieved = kotlin.math.abs(startingWeight - currentWeight)
        (achieved / totalNeeded * 100)
    }
}

@Serializable
enum class WeightTrend {
    LOSING,     // Trending down
    GAINING,    // Trending up
    STABLE,     // Maintaining
    FLUCTUATING // Inconsistent
}

@Serializable
data class BodyComposition(
    val weight: Float,
    val bodyFat: Float,              // percentage
    val leanMass: Float,             // kg
    val fatMass: Float,              // kg
    val bmr: Int,                    // Basal Metabolic Rate (calories/day)
    val bmi: Float,
    val category: BMICategory
) {
    companion object {
        fun calculate(
            weight: Float,
            height: Float,        // cm
            bodyFat: Float,       // percentage
            age: Int,
            isMale: Boolean
        ): BodyComposition {
            val fatMass = weight * (bodyFat / 100)
            val leanMass = weight - fatMass

            // BMI = weight(kg) / height(m)²
            val heightM = height / 100
            val bmi = weight / (heightM * heightM)

            // Katch-McArdle BMR formula (uses lean mass)
            val bmr = (370 + (21.6 * leanMass)).toInt()

            val category = when {
                bmi < 18.5 -> BMICategory.UNDERWEIGHT
                bmi < 25 -> BMICategory.NORMAL
                bmi < 30 -> BMICategory.OVERWEIGHT
                else -> BMICategory.OBESE
            }

            return BodyComposition(
                weight = weight,
                bodyFat = bodyFat,
                leanMass = leanMass,
                fatMass = fatMass,
                bmr = bmr,
                bmi = bmi,
                category = category
            )
        }
    }
}

@Serializable
enum class BMICategory {
    UNDERWEIGHT,
    NORMAL,
    OVERWEIGHT,
    OBESE
}

@Serializable
data class MeasurementProgress(
    val measurementType: String,    // e.g., "waist", "bicep"
    val history: List<MeasurementEntry>,
    val change: Float,              // Total change
    val trend: MeasurementTrend
)

@Serializable
data class MeasurementEntry(
    val date: String,
    val value: Float
)

@Serializable
enum class MeasurementTrend {
    INCREASING,
    DECREASING,
    STABLE
}

// Progress insights generator
object ProgressInsights {

    fun generateWeightInsight(history: WeightHistory): String {
        val change = history.totalChange
        val weeks = history.entries.size / 7

        return when {
            change < -5 && weeks >= 4 ->
                "Amazing! You've lost ${kotlin.math.abs(change).format(1)} kg in $weeks weeks. Keep it up!"
            change > 5 && weeks >= 4 ->
                "Great progress! You've gained ${change.format(1)} kg of mass in $weeks weeks."
            kotlin.math.abs(change) < 1 ->
                "Your weight is stable. Perfect for body recomposition!"
            else ->
                "You're making steady progress. Stay consistent!"
        }
    }

    fun generateBodyFatInsight(startBF: Float, currentBF: Float): String {
        val change = startBF - currentBF
        return when {
            change >= 5 -> "Incredible! You've lost ${change.format(1)}% body fat!"
            change >= 2 -> "Great work! ${change.format(1)}% body fat reduction."
            change >= 0.5 -> "Solid progress! Down ${change.format(1)}% body fat."
            change <= -2 -> "Body fat has increased by ${kotlin.math.abs(change).format(1)}%."
            else -> "Body fat is stable. Focus on muscle building!"
        }
    }

    fun generateMeasurementInsight(measurements: List<BodyMeasurements>): String {
        if (measurements.size < 2) return "Track more measurements to see trends!"

        val first = measurements.first()
        val latest = measurements.last()

        val waistChange = (first.waist ?: 0f) - (latest.waist ?: 0f)

        return when {
            waistChange > 5 -> "You've lost ${waistChange.format(1)} cm from your waist!"
            waistChange < -3 -> "Waist measurement increased. Check your diet!"
            else -> "Measurements are stable. Keep training!"
        }
    }
}

// Extension function for number formatting
fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)

// Weekly summary for body metrics
@Serializable
data class WeeklyBodySummary(
    val weekStart: String,
    val weekEnd: String,
    val startWeight: Float,
    val endWeight: Float,
    val weightChange: Float,
    val avgCalories: Int,
    val workoutsCompleted: Int,
    val stepsAverage: Int,
    val trend: WeightTrend
) {
    val summary: String get() = when {
        weightChange < -0.5 -> "Lost ${kotlin.math.abs(weightChange).format(1)} kg this week!"
        weightChange > 0.5 -> "Gained ${weightChange.format(1)} kg this week!"
        else -> "Weight stable this week"
    }
}
