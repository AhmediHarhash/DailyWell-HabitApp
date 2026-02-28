package com.dailywell.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Health Connect Repository
 * Integrates with Android Health Connect API for reading health data
 *
 * PRODUCTION-READY: Real Health Connect integration for 2026 standards
 */
interface HealthConnectRepository {

    // ==================== AVAILABILITY ====================

    /**
     * Check if Health Connect is available on this device
     */
    suspend fun isAvailable(): Boolean

    /**
     * Check if Health Connect is installed
     */
    suspend fun isInstalled(): Boolean

    /**
     * Get Health Connect status
     */
    suspend fun getStatus(): HealthConnectStatus

    // ==================== PERMISSIONS ====================

    /**
     * Check which permissions are currently granted
     */
    suspend fun getGrantedPermissions(): Set<HealthPermission>

    /**
     * Check if all required permissions are granted
     */
    suspend fun hasAllPermissions(): Boolean

    /**
     * Request health connect permissions
     * Returns true if all permissions were granted
     */
    suspend fun requestPermissions(): Boolean

    // ==================== READ DATA ====================

    /**
     * Read step count for date range
     */
    suspend fun getSteps(startTime: Instant, endTime: Instant): Result<List<StepRecord>>

    /**
     * Read today's total steps
     */
    suspend fun getTodaySteps(): Result<Int>

    /**
     * Read weight records for date range
     */
    suspend fun getWeightRecords(startTime: Instant, endTime: Instant): Result<List<WeightRecord>>

    /**
     * Read latest weight
     */
    suspend fun getLatestWeight(): Result<WeightRecord?>

    /**
     * Read sleep sessions for date range
     */
    suspend fun getSleepSessions(startTime: Instant, endTime: Instant): Result<List<SleepRecord>>

    /**
     * Read last night's sleep
     */
    suspend fun getLastNightSleep(): Result<SleepRecord?>

    /**
     * Read heart rate samples for date range
     */
    suspend fun getHeartRate(startTime: Instant, endTime: Instant): Result<List<HeartRateRecord>>

    /**
     * Read resting heart rate
     */
    suspend fun getRestingHeartRate(): Result<Int?>

    /**
     * Read active calories burned for date range
     */
    suspend fun getActiveCalories(startTime: Instant, endTime: Instant): Result<List<ActiveCaloriesRecord>>

    /**
     * Read today's active calories
     */
    suspend fun getTodayActiveCalories(): Result<Double>

    /**
     * Read workout sessions (exercises) for date range
     */
    suspend fun getWorkoutSessions(startTime: Instant, endTime: Instant): Result<List<ExerciseSessionRecord>>

    // ==================== WRITE DATA ====================

    /**
     * Write a weight record
     */
    suspend fun writeWeight(weightKg: Double, time: Instant): Result<Unit>

    /**
     * Write a workout session
     */
    suspend fun writeWorkout(
        title: String,
        startTime: Instant,
        endTime: Instant,
        exerciseType: ExerciseType,
        caloriesBurned: Double?
    ): Result<Unit>

    /**
     * Write nutrition record (meal)
     */
    suspend fun writeNutrition(
        mealType: NutritionMealType,
        startTime: Instant,
        endTime: Instant,
        calories: Double,
        protein: Double?,
        carbs: Double?,
        fat: Double?
    ): Result<Unit>

    /**
     * Write step count
     */
    suspend fun writeSteps(
        count: Long,
        startTime: Instant,
        endTime: Instant
    ): Result<Unit>

    // ==================== SYNC ====================

    /**
     * Observe step count updates in real-time
     */
    fun observeSteps(): Flow<Int>

    /**
     * Sync all available health data
     */
    suspend fun syncAllData(): Result<HealthSyncSummary>

    /**
     * Get last sync timestamp
     */
    suspend fun getLastSyncTime(): Instant?
}

// ==================== DATA MODELS ====================

enum class HealthConnectStatus {
    AVAILABLE,
    NOT_INSTALLED,
    NOT_SUPPORTED,
    API_UNAVAILABLE
}

enum class HealthPermission {
    READ_STEPS,
    WRITE_STEPS,
    READ_WEIGHT,
    WRITE_WEIGHT,
    READ_SLEEP,
    WRITE_SLEEP,
    READ_HEART_RATE,
    READ_ACTIVE_CALORIES,
    WRITE_ACTIVE_CALORIES,
    READ_EXERCISE,
    WRITE_EXERCISE,
    READ_NUTRITION,
    WRITE_NUTRITION
}

data class StepRecord(
    val count: Long,
    val startTime: Instant,
    val endTime: Instant,
    val source: String? = null
)

data class WeightRecord(
    val weightKg: Double,
    val time: Instant,
    val source: String? = null
)

data class SleepRecord(
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int,
    val stages: List<SleepStage> = emptyList(),
    val source: String? = null
)

data class SleepStage(
    val stage: SleepStageType,
    val startTime: Instant,
    val endTime: Instant
)

enum class SleepStageType {
    AWAKE,
    LIGHT,
    DEEP,
    REM,
    SLEEPING,  // Generic sleeping (no stage info)
    OUT_OF_BED,
    UNKNOWN
}

data class HeartRateRecord(
    val beatsPerMinute: Int,
    val time: Instant,
    val source: String? = null
)

data class ActiveCaloriesRecord(
    val calories: Double,
    val startTime: Instant,
    val endTime: Instant,
    val source: String? = null
)

data class ExerciseSessionRecord(
    val title: String?,
    val exerciseType: ExerciseType,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int,
    val caloriesBurned: Double?,
    val source: String? = null
)

enum class ExerciseType {
    RUNNING,
    WALKING,
    CYCLING,
    SWIMMING,
    HIKING,
    YOGA,
    STRENGTH_TRAINING,
    HIIT,
    DANCING,
    STRETCHING,
    PILATES,
    ELLIPTICAL,
    ROWING,
    STAIR_CLIMBING,
    MARTIAL_ARTS,
    BOXING,
    TENNIS,
    BASKETBALL,
    SOCCER,
    GOLF,
    BADMINTON,
    SKIING,
    SNOWBOARDING,
    SKATING,
    ROCK_CLIMBING,
    OTHER,
    UNKNOWN
}

enum class NutritionMealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK,
    UNKNOWN
}

data class HealthSyncSummary(
    val stepsImported: Int,
    val weightRecordsImported: Int,
    val sleepSessionsImported: Int,
    val heartRateSamplesImported: Int,
    val caloriesRecordsImported: Int,
    val workoutsImported: Int,
    val syncTime: Instant,
    val errors: List<String> = emptyList()
)
