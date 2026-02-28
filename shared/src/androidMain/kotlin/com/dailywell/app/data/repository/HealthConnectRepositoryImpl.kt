package com.dailywell.app.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission as HCPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord as HCWeightRecord
import androidx.health.connect.client.records.HeartRateRecord as HCHeartRateRecord
import androidx.health.connect.client.records.ExerciseSessionRecord as HCExerciseSessionRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.*
import java.time.ZoneId

/**
 * Android implementation of HealthConnectRepository
 * Uses Health Connect API for reading/writing health data
 *
 * PRODUCTION-READY: Full Health Connect integration
 */
class HealthConnectRepositoryImpl(
    private val context: Context
) : HealthConnectRepository {

    private var healthConnectClient: HealthConnectClient? = null
    private val _stepsFlow = MutableStateFlow(0)
    private var lastSyncTime: Instant? = null

    // Required permissions for full functionality
    private val requiredPermissions = setOf(
        HCPermission.getReadPermission(StepsRecord::class),
        HCPermission.getWritePermission(StepsRecord::class),
        HCPermission.getReadPermission(HCWeightRecord::class),
        HCPermission.getWritePermission(HCWeightRecord::class),
        HCPermission.getReadPermission(SleepSessionRecord::class),
        HCPermission.getReadPermission(HCHeartRateRecord::class),
        HCPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HCPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
        HCPermission.getReadPermission(HCExerciseSessionRecord::class),
        HCPermission.getWritePermission(HCExerciseSessionRecord::class),
        HCPermission.getReadPermission(NutritionRecord::class),
        HCPermission.getWritePermission(NutritionRecord::class)
    )

    private fun getClient(): HealthConnectClient? {
        if (healthConnectClient == null) {
            val status = HealthConnectClient.getSdkStatus(context)
            if (status == HealthConnectClient.SDK_AVAILABLE) {
                healthConnectClient = HealthConnectClient.getOrCreate(context)
            }
        }
        return healthConnectClient
    }

    // ==================== AVAILABILITY ====================

    override suspend fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    override suspend fun isInstalled(): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        return status == HealthConnectClient.SDK_AVAILABLE
    }

    override suspend fun getStatus(): HealthConnectStatus {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectStatus.NOT_SUPPORTED
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.NOT_INSTALLED
            else -> HealthConnectStatus.API_UNAVAILABLE
        }
    }

    // ==================== PERMISSIONS ====================

    override suspend fun getGrantedPermissions(): Set<HealthPermission> {
        val client = getClient() ?: return emptySet()
        val granted = client.permissionController.getGrantedPermissions()

        return buildSet {
            if (HCPermission.getReadPermission(StepsRecord::class) in granted) add(HealthPermission.READ_STEPS)
            if (HCPermission.getWritePermission(StepsRecord::class) in granted) add(HealthPermission.WRITE_STEPS)
            if (HCPermission.getReadPermission(HCWeightRecord::class) in granted) add(HealthPermission.READ_WEIGHT)
            if (HCPermission.getWritePermission(HCWeightRecord::class) in granted) add(HealthPermission.WRITE_WEIGHT)
            if (HCPermission.getReadPermission(SleepSessionRecord::class) in granted) add(HealthPermission.READ_SLEEP)
            if (HCPermission.getReadPermission(HCHeartRateRecord::class) in granted) add(HealthPermission.READ_HEART_RATE)
            if (HCPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) add(HealthPermission.READ_ACTIVE_CALORIES)
            if (HCPermission.getWritePermission(ActiveCaloriesBurnedRecord::class) in granted) add(HealthPermission.WRITE_ACTIVE_CALORIES)
            if (HCPermission.getReadPermission(HCExerciseSessionRecord::class) in granted) add(HealthPermission.READ_EXERCISE)
            if (HCPermission.getWritePermission(HCExerciseSessionRecord::class) in granted) add(HealthPermission.WRITE_EXERCISE)
            if (HCPermission.getReadPermission(NutritionRecord::class) in granted) add(HealthPermission.READ_NUTRITION)
            if (HCPermission.getWritePermission(NutritionRecord::class) in granted) add(HealthPermission.WRITE_NUTRITION)
        }
    }

    override suspend fun hasAllPermissions(): Boolean {
        val client = getClient() ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    override suspend fun requestPermissions(): Boolean {
        // Note: Permission request must be handled via Activity result
        // This method returns current permission state
        return hasAllPermissions()
    }

    /**
     * Get permission request contract for use with Activity
     */
    fun getPermissionContract() = PermissionController.createRequestPermissionResultContract()

    /**
     * Get required permissions set
     */
    fun getRequiredPermissions() = requiredPermissions

    // ==================== READ DATA ====================

    override suspend fun getSteps(startTime: Instant, endTime: Instant): Result<List<StepRecord>> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime.toJavaInstant(),
                    endTime.toJavaInstant()
                )
            )

            val response = client.readRecords(request)
            val records = response.records.map { record ->
                StepRecord(
                    count = record.count,
                    startTime = record.startTime.toKotlinInstant(),
                    endTime = record.endTime.toKotlinInstant(),
                    source = record.metadata.dataOrigin.packageName
                )
            }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTodaySteps(): Result<Int> {
        return try {
            val now = Clock.System.now()
            val startOfDay = now.toLocalDateTime(TimeZone.currentSystemDefault())
                .date.atStartOfDayIn(TimeZone.currentSystemDefault())

            val result = getSteps(startOfDay, now)
            result.map { records -> records.sumOf { it.count.toInt() } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWeightRecords(startTime: Instant, endTime: Instant): Result<List<WeightRecord>> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val request = ReadRecordsRequest(
                recordType = HCWeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime.toJavaInstant(),
                    endTime.toJavaInstant()
                )
            )

            val response = client.readRecords(request)
            val records = response.records.map { record ->
                WeightRecord(
                    weightKg = record.weight.inKilograms,
                    time = record.time.toKotlinInstant(),
                    source = record.metadata.dataOrigin.packageName
                )
            }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLatestWeight(): Result<WeightRecord?> {
        return try {
            val now = Clock.System.now()
            val thirtyDaysAgo = now.minus(30, DateTimeUnit.DAY, TimeZone.currentSystemDefault())

            val result = getWeightRecords(thirtyDaysAgo, now)
            result.map { records -> records.maxByOrNull { it.time } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSleepSessions(startTime: Instant, endTime: Instant): Result<List<SleepRecord>> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime.toJavaInstant(),
                    endTime.toJavaInstant()
                )
            )

            val response = client.readRecords(request)
            val records = response.records.map { record ->
                val stages = record.stages.map { stage ->
                    SleepStage(
                        stage = mapSleepStageType(stage.stage),
                        startTime = stage.startTime.toKotlinInstant(),
                        endTime = stage.endTime.toKotlinInstant()
                    )
                }

                val durationMs = java.time.Duration.between(record.startTime, record.endTime).toMillis()
                val durationMinutes = (durationMs / 60000).toInt()

                SleepRecord(
                    startTime = record.startTime.toKotlinInstant(),
                    endTime = record.endTime.toKotlinInstant(),
                    durationMinutes = durationMinutes,
                    stages = stages,
                    source = record.metadata.dataOrigin.packageName
                )
            }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapSleepStageType(stage: Int): SleepStageType {
        return when (stage) {
            SleepSessionRecord.STAGE_TYPE_AWAKE -> SleepStageType.AWAKE
            SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStageType.LIGHT
            SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStageType.DEEP
            SleepSessionRecord.STAGE_TYPE_REM -> SleepStageType.REM
            SleepSessionRecord.STAGE_TYPE_SLEEPING -> SleepStageType.SLEEPING
            SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> SleepStageType.OUT_OF_BED
            else -> SleepStageType.UNKNOWN
        }
    }

    override suspend fun getLastNightSleep(): Result<SleepRecord?> {
        return try {
            val now = Clock.System.now()
            val yesterday = now.minus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())

            val result = getSleepSessions(yesterday, now)
            result.map { records -> records.maxByOrNull { it.endTime } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHeartRate(startTime: Instant, endTime: Instant): Result<List<HeartRateRecord>> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val request = ReadRecordsRequest(
                recordType = HCHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime.toJavaInstant(),
                    endTime.toJavaInstant()
                )
            )

            val response = client.readRecords(request)
            val records = response.records.flatMap { record ->
                record.samples.map { sample ->
                    HeartRateRecord(
                        beatsPerMinute = sample.beatsPerMinute.toInt(),
                        time = sample.time.toKotlinInstant(),
                        source = record.metadata.dataOrigin.packageName
                    )
                }
            }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRestingHeartRate(): Result<Int?> {
        return try {
            val now = Clock.System.now()
            val sevenDaysAgo = now.minus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault())

            val result = getHeartRate(sevenDaysAgo, now)
            result.map { records ->
                if (records.isEmpty()) null
                else {
                    // Calculate resting heart rate as average of lowest 10% of readings
                    val sorted = records.sortedBy { it.beatsPerMinute }
                    val lowestCount = maxOf(1, sorted.size / 10)
                    sorted.take(lowestCount).map { it.beatsPerMinute }.average().toInt()
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveCalories(startTime: Instant, endTime: Instant): Result<List<ActiveCaloriesRecord>> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val request = ReadRecordsRequest(
                recordType = ActiveCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime.toJavaInstant(),
                    endTime.toJavaInstant()
                )
            )

            val response = client.readRecords(request)
            val records = response.records.map { record ->
                ActiveCaloriesRecord(
                    calories = record.energy.inKilocalories,
                    startTime = record.startTime.toKotlinInstant(),
                    endTime = record.endTime.toKotlinInstant(),
                    source = record.metadata.dataOrigin.packageName
                )
            }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTodayActiveCalories(): Result<Double> {
        return try {
            val now = Clock.System.now()
            val startOfDay = now.toLocalDateTime(TimeZone.currentSystemDefault())
                .date.atStartOfDayIn(TimeZone.currentSystemDefault())

            val result = getActiveCalories(startOfDay, now)
            result.map { records -> records.sumOf { it.calories } }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorkoutSessions(startTime: Instant, endTime: Instant): Result<List<ExerciseSessionRecord>> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val request = ReadRecordsRequest(
                recordType = HCExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startTime.toJavaInstant(),
                    endTime.toJavaInstant()
                )
            )

            val response = client.readRecords(request)
            val records = response.records.map { record ->
                val durationMs = java.time.Duration.between(record.startTime, record.endTime).toMillis()
                val durationMinutes = (durationMs / 60000).toInt()

                ExerciseSessionRecord(
                    title = record.title,
                    exerciseType = mapExerciseType(record.exerciseType),
                    startTime = record.startTime.toKotlinInstant(),
                    endTime = record.endTime.toKotlinInstant(),
                    durationMinutes = durationMinutes,
                    caloriesBurned = null, // Would need to read associated ActiveCaloriesBurnedRecord
                    source = record.metadata.dataOrigin.packageName
                )
            }

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapExerciseType(type: Int): ExerciseType {
        return when (type) {
            HCExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> ExerciseType.RUNNING
            HCExerciseSessionRecord.EXERCISE_TYPE_WALKING -> ExerciseType.WALKING
            HCExerciseSessionRecord.EXERCISE_TYPE_BIKING -> ExerciseType.CYCLING
            HCExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            HCExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> ExerciseType.SWIMMING
            HCExerciseSessionRecord.EXERCISE_TYPE_HIKING -> ExerciseType.HIKING
            HCExerciseSessionRecord.EXERCISE_TYPE_YOGA -> ExerciseType.YOGA
            HCExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
            HCExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> ExerciseType.STRENGTH_TRAINING
            HCExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> ExerciseType.HIIT
            HCExerciseSessionRecord.EXERCISE_TYPE_DANCING -> ExerciseType.DANCING
            HCExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> ExerciseType.STRETCHING
            HCExerciseSessionRecord.EXERCISE_TYPE_PILATES -> ExerciseType.PILATES
            HCExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> ExerciseType.ELLIPTICAL
            HCExerciseSessionRecord.EXERCISE_TYPE_ROWING,
            HCExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> ExerciseType.ROWING
            HCExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
            HCExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> ExerciseType.STAIR_CLIMBING
            HCExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS -> ExerciseType.MARTIAL_ARTS
            HCExerciseSessionRecord.EXERCISE_TYPE_BOXING -> ExerciseType.BOXING
            HCExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> ExerciseType.TENNIS
            HCExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> ExerciseType.BASKETBALL
            HCExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> ExerciseType.SOCCER
            HCExerciseSessionRecord.EXERCISE_TYPE_GOLF -> ExerciseType.GOLF
            HCExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> ExerciseType.BADMINTON
            HCExerciseSessionRecord.EXERCISE_TYPE_SKIING -> ExerciseType.SKIING
            HCExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> ExerciseType.SNOWBOARDING
            HCExerciseSessionRecord.EXERCISE_TYPE_SKATING -> ExerciseType.SKATING
            HCExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING -> ExerciseType.ROCK_CLIMBING
            else -> ExerciseType.OTHER
        }
    }

    // ==================== WRITE DATA ====================

    override suspend fun writeWeight(weightKg: Double, time: Instant): Result<Unit> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val record = HCWeightRecord(
                weight = Mass.kilograms(weightKg),
                time = time.toJavaInstant(),
                zoneOffset = ZoneId.systemDefault().rules.getOffset(time.toJavaInstant())
            )

            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeWorkout(
        title: String,
        startTime: Instant,
        endTime: Instant,
        exerciseType: ExerciseType,
        caloriesBurned: Double?
    ): Result<Unit> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val record = HCExerciseSessionRecord(
                startTime = startTime.toJavaInstant(),
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime.toJavaInstant()),
                endTime = endTime.toJavaInstant(),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime.toJavaInstant()),
                exerciseType = mapExerciseTypeToHealth(exerciseType),
                title = title
            )

            client.insertRecords(listOf(record))

            // Also write calories if provided
            if (caloriesBurned != null && caloriesBurned > 0) {
                val caloriesRecord = ActiveCaloriesBurnedRecord(
                    startTime = startTime.toJavaInstant(),
                    startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime.toJavaInstant()),
                    endTime = endTime.toJavaInstant(),
                    endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime.toJavaInstant()),
                    energy = Energy.kilocalories(caloriesBurned)
                )
                client.insertRecords(listOf(caloriesRecord))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapExerciseTypeToHealth(type: ExerciseType): Int {
        return when (type) {
            ExerciseType.RUNNING -> HCExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            ExerciseType.WALKING -> HCExerciseSessionRecord.EXERCISE_TYPE_WALKING
            ExerciseType.CYCLING -> HCExerciseSessionRecord.EXERCISE_TYPE_BIKING
            ExerciseType.SWIMMING -> HCExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            ExerciseType.HIKING -> HCExerciseSessionRecord.EXERCISE_TYPE_HIKING
            ExerciseType.YOGA -> HCExerciseSessionRecord.EXERCISE_TYPE_YOGA
            ExerciseType.STRENGTH_TRAINING -> HCExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            ExerciseType.HIIT -> HCExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            ExerciseType.DANCING -> HCExerciseSessionRecord.EXERCISE_TYPE_DANCING
            ExerciseType.STRETCHING -> HCExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            ExerciseType.PILATES -> HCExerciseSessionRecord.EXERCISE_TYPE_PILATES
            ExerciseType.ELLIPTICAL -> HCExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
            ExerciseType.ROWING -> HCExerciseSessionRecord.EXERCISE_TYPE_ROWING
            ExerciseType.STAIR_CLIMBING -> HCExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
            ExerciseType.MARTIAL_ARTS -> HCExerciseSessionRecord.EXERCISE_TYPE_MARTIAL_ARTS
            ExerciseType.BOXING -> HCExerciseSessionRecord.EXERCISE_TYPE_BOXING
            ExerciseType.TENNIS -> HCExerciseSessionRecord.EXERCISE_TYPE_TENNIS
            ExerciseType.BASKETBALL -> HCExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL
            ExerciseType.SOCCER -> HCExerciseSessionRecord.EXERCISE_TYPE_SOCCER
            ExerciseType.GOLF -> HCExerciseSessionRecord.EXERCISE_TYPE_GOLF
            ExerciseType.BADMINTON -> HCExerciseSessionRecord.EXERCISE_TYPE_BADMINTON
            ExerciseType.SKIING -> HCExerciseSessionRecord.EXERCISE_TYPE_SKIING
            ExerciseType.SNOWBOARDING -> HCExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
            ExerciseType.SKATING -> HCExerciseSessionRecord.EXERCISE_TYPE_SKATING
            ExerciseType.ROCK_CLIMBING -> HCExerciseSessionRecord.EXERCISE_TYPE_ROCK_CLIMBING
            ExerciseType.OTHER, ExerciseType.UNKNOWN -> HCExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
        }
    }

    override suspend fun writeNutrition(
        mealType: NutritionMealType,
        startTime: Instant,
        endTime: Instant,
        calories: Double,
        protein: Double?,
        carbs: Double?,
        fat: Double?
    ): Result<Unit> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val record = NutritionRecord(
                startTime = startTime.toJavaInstant(),
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime.toJavaInstant()),
                endTime = endTime.toJavaInstant(),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime.toJavaInstant()),
                energy = Energy.kilocalories(calories),
                protein = protein?.let { Mass.grams(it) },
                totalCarbohydrate = carbs?.let { Mass.grams(it) },
                totalFat = fat?.let { Mass.grams(it) },
                mealType = when (mealType) {
                    NutritionMealType.BREAKFAST -> MealType.MEAL_TYPE_BREAKFAST
                    NutritionMealType.LUNCH -> MealType.MEAL_TYPE_LUNCH
                    NutritionMealType.DINNER -> MealType.MEAL_TYPE_DINNER
                    NutritionMealType.SNACK -> MealType.MEAL_TYPE_SNACK
                    NutritionMealType.UNKNOWN -> MealType.MEAL_TYPE_UNKNOWN
                }
            )

            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun writeSteps(count: Long, startTime: Instant, endTime: Instant): Result<Unit> {
        return try {
            val client = getClient() ?: return Result.failure(Exception("Health Connect not available"))

            val record = StepsRecord(
                count = count,
                startTime = startTime.toJavaInstant(),
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(startTime.toJavaInstant()),
                endTime = endTime.toJavaInstant(),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(endTime.toJavaInstant())
            )

            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== SYNC ====================

    override fun observeSteps(): Flow<Int> = _stepsFlow.asStateFlow()

    override suspend fun syncAllData(): Result<HealthSyncSummary> {
        return try {
            val now = Clock.System.now()
            val sevenDaysAgo = now.minus(7, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            val errors = mutableListOf<String>()

            // Sync steps
            val stepsResult = getSteps(sevenDaysAgo, now)
            val stepsCount = stepsResult.getOrNull()?.size ?: 0
            if (stepsResult.isFailure) errors.add("Steps: ${stepsResult.exceptionOrNull()?.message}")

            // Update today's steps for Flow
            getTodaySteps().onSuccess { _stepsFlow.value = it }

            // Sync weight
            val weightResult = getWeightRecords(sevenDaysAgo, now)
            val weightCount = weightResult.getOrNull()?.size ?: 0
            if (weightResult.isFailure) errors.add("Weight: ${weightResult.exceptionOrNull()?.message}")

            // Sync sleep
            val sleepResult = getSleepSessions(sevenDaysAgo, now)
            val sleepCount = sleepResult.getOrNull()?.size ?: 0
            if (sleepResult.isFailure) errors.add("Sleep: ${sleepResult.exceptionOrNull()?.message}")

            // Sync heart rate
            val heartRateResult = getHeartRate(sevenDaysAgo, now)
            val heartRateCount = heartRateResult.getOrNull()?.size ?: 0
            if (heartRateResult.isFailure) errors.add("Heart Rate: ${heartRateResult.exceptionOrNull()?.message}")

            // Sync calories
            val caloriesResult = getActiveCalories(sevenDaysAgo, now)
            val caloriesCount = caloriesResult.getOrNull()?.size ?: 0
            if (caloriesResult.isFailure) errors.add("Calories: ${caloriesResult.exceptionOrNull()?.message}")

            // Sync workouts
            val workoutsResult = getWorkoutSessions(sevenDaysAgo, now)
            val workoutsCount = workoutsResult.getOrNull()?.size ?: 0
            if (workoutsResult.isFailure) errors.add("Workouts: ${workoutsResult.exceptionOrNull()?.message}")

            lastSyncTime = now

            Result.success(
                HealthSyncSummary(
                    stepsImported = stepsCount,
                    weightRecordsImported = weightCount,
                    sleepSessionsImported = sleepCount,
                    heartRateSamplesImported = heartRateCount,
                    caloriesRecordsImported = caloriesCount,
                    workoutsImported = workoutsCount,
                    syncTime = now,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLastSyncTime(): Instant? = lastSyncTime

    // ==================== EXTENSION HELPERS ====================

    private fun Instant.toJavaInstant(): java.time.Instant =
        java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())

    private fun java.time.Instant.toKotlinInstant(): Instant =
        Instant.fromEpochMilliseconds(this.toEpochMilli())
}
