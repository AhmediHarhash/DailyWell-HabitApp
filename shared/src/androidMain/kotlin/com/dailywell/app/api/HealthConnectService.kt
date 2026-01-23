package com.dailywell.app.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.*
import java.time.ZoneId

/**
 * Real Health Connect integration for biometric data
 * Fetches actual sleep, HRV, heart rate, and activity data from Android Health Connect
 */
class HealthConnectService(private val context: Context) {

    private var healthConnectClient: HealthConnectClient? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.NOT_CHECKED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    // Required permissions for the app
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    )

    enum class ConnectionStatus {
        NOT_CHECKED,
        NOT_AVAILABLE,
        NOT_INSTALLED,
        AVAILABLE,
        PERMISSIONS_GRANTED
    }

    /**
     * Check if Health Connect is available
     */
    suspend fun checkAvailability(): ConnectionStatus {
        val status = when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_UNAVAILABLE -> ConnectionStatus.NOT_AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> ConnectionStatus.NOT_INSTALLED
            HealthConnectClient.SDK_AVAILABLE -> {
                healthConnectClient = HealthConnectClient.getOrCreate(context)
                if (hasAllPermissions()) {
                    ConnectionStatus.PERMISSIONS_GRANTED
                } else {
                    ConnectionStatus.AVAILABLE
                }
            }
            else -> ConnectionStatus.NOT_AVAILABLE
        }

        _connectionStatus.value = status
        return status
    }

    /**
     * Check if all required permissions are granted
     */
    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    /**
     * Get the intent to request permissions
     */
    fun getPermissionRequestContract() = PermissionController.createRequestPermissionResultContract()

    /**
     * Get intent to open Health Connect app (for installing or updating)
     */
    fun getHealthConnectIntent(): Intent {
        val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata")
        return Intent(Intent.ACTION_VIEW, uri)
    }

    // ==================== SLEEP DATA ====================

    /**
     * Get sleep records for the past N days
     */
    suspend fun getSleepRecords(days: Int = 7): List<SleepData> {
        val client = healthConnectClient ?: return emptyList()

        val endTime = Clock.System.now().toJavaInstant()
        val startTime = Clock.System.now()
            .minus(days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            .toJavaInstant()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            response.records.map { record ->
                val startInstant = record.startTime.atZone(ZoneId.systemDefault()).toInstant()
                val endInstant = record.endTime.atZone(ZoneId.systemDefault()).toInstant()
                val durationMinutes = java.time.Duration.between(startInstant, endInstant).toMinutes().toInt()

                // Calculate sleep stages
                var deepSleepMinutes = 0
                var remSleepMinutes = 0
                var lightSleepMinutes = 0
                var awakeMinutes = 0

                record.stages.forEach { stage ->
                    val stageDuration = java.time.Duration.between(
                        stage.startTime,
                        stage.endTime
                    ).toMinutes().toInt()

                    when (stage.stage) {
                        SleepSessionRecord.STAGE_TYPE_DEEP -> deepSleepMinutes += stageDuration
                        SleepSessionRecord.STAGE_TYPE_REM -> remSleepMinutes += stageDuration
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> lightSleepMinutes += stageDuration
                        SleepSessionRecord.STAGE_TYPE_AWAKE,
                        SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> awakeMinutes += stageDuration
                    }
                }

                // If no stage data, estimate based on typical sleep patterns
                if (record.stages.isEmpty() && durationMinutes > 0) {
                    deepSleepMinutes = (durationMinutes * 0.20).toInt()  // ~20% deep
                    remSleepMinutes = (durationMinutes * 0.25).toInt()   // ~25% REM
                    lightSleepMinutes = (durationMinutes * 0.50).toInt() // ~50% light
                    awakeMinutes = (durationMinutes * 0.05).toInt()      // ~5% awake
                }

                val sleepTime = durationMinutes - awakeMinutes
                val efficiency = if (durationMinutes > 0) {
                    (sleepTime.toFloat() / durationMinutes * 100)
                } else 0f

                SleepData(
                    date = record.startTime.atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(),
                    durationMinutes = durationMinutes,
                    deepSleepMinutes = deepSleepMinutes,
                    remSleepMinutes = remSleepMinutes,
                    lightSleepMinutes = lightSleepMinutes,
                    awakeMinutes = awakeMinutes,
                    efficiency = efficiency,
                    startTime = record.startTime.toString(),
                    endTime = record.endTime.toString()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get last night's sleep
     */
    suspend fun getLastNightSleep(): SleepData? {
        return getSleepRecords(2).firstOrNull()
    }

    // ==================== HEART RATE / HRV DATA ====================

    /**
     * Get HRV records for the past N days
     */
    suspend fun getHrvRecords(days: Int = 7): List<HrvData> {
        val client = healthConnectClient ?: return emptyList()

        val endTime = Clock.System.now().toJavaInstant()
        val startTime = Clock.System.now()
            .minus(days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            .toJavaInstant()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Group by day and calculate daily averages
            response.records
                .groupBy { record ->
                    record.time.atZone(ZoneId.systemDefault())
                        .toLocalDate().toString()
                }
                .map { (date, records) ->
                    val hrvValues = records.map { it.heartRateVariabilityMillis.toFloat() }
                    HrvData(
                        date = date,
                        avgHrv = hrvValues.average().toFloat(),
                        minHrv = hrvValues.minOrNull() ?: 0f,
                        maxHrv = hrvValues.maxOrNull() ?: 0f,
                        readings = records.size
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get heart rate records for today
     */
    suspend fun getTodayHeartRate(): HeartRateData? {
        val client = healthConnectClient ?: return null

        val today = Clock.System.now()
        val startOfDay = today.toLocalDateTime(TimeZone.currentSystemDefault())
            .date.atStartOfDayIn(TimeZone.currentSystemDefault())

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        startOfDay.toJavaInstant(),
                        today.toJavaInstant()
                    )
                )
            )

            if (response.records.isEmpty()) return null

            val allSamples = response.records.flatMap { it.samples }
            val bpmValues = allSamples.map { it.beatsPerMinute.toInt() }

            HeartRateData(
                date = today.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
                avgBpm = bpmValues.average().toInt(),
                minBpm = bpmValues.minOrNull() ?: 0,
                maxBpm = bpmValues.maxOrNull() ?: 0,
                readings = allSamples.size
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get resting heart rate
     */
    suspend fun getRestingHeartRate(days: Int = 7): List<RestingHeartRateData> {
        val client = healthConnectClient ?: return emptyList()

        val endTime = Clock.System.now().toJavaInstant()
        val startTime = Clock.System.now()
            .minus(days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            .toJavaInstant()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = RestingHeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            response.records.map { record ->
                RestingHeartRateData(
                    date = record.time.atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(),
                    bpm = record.beatsPerMinute.toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== STEPS / ACTIVITY DATA ====================

    /**
     * Get step counts for the past N days
     */
    suspend fun getStepRecords(days: Int = 7): List<StepsData> {
        val client = healthConnectClient ?: return emptyList()

        val endTime = Clock.System.now().toJavaInstant()
        val startTime = Clock.System.now()
            .minus(days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            .toJavaInstant()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            // Group by day
            response.records
                .groupBy { record ->
                    record.startTime.atZone(ZoneId.systemDefault())
                        .toLocalDate().toString()
                }
                .map { (date, records) ->
                    StepsData(
                        date = date,
                        totalSteps = records.sumOf { it.count }.toInt(),
                        sources = records.mapNotNull { it.metadata.dataOrigin.packageName }.distinct()
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get today's step count
     */
    suspend fun getTodaySteps(): Int {
        val today = getStepRecords(1).firstOrNull()
        return today?.totalSteps ?: 0
    }

    /**
     * Get exercise sessions for the past N days
     */
    suspend fun getExerciseSessions(days: Int = 7): List<ExerciseData> {
        val client = healthConnectClient ?: return emptyList()

        val endTime = Clock.System.now().toJavaInstant()
        val startTime = Clock.System.now()
            .minus(days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            .toJavaInstant()

        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            response.records.map { record ->
                val startInstant = record.startTime.atZone(ZoneId.systemDefault()).toInstant()
                val endInstant = record.endTime.atZone(ZoneId.systemDefault()).toInstant()
                val durationMinutes = java.time.Duration.between(startInstant, endInstant).toMinutes().toInt()

                ExerciseData(
                    date = record.startTime.atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(),
                    type = getExerciseTypeName(record.exerciseType),
                    durationMinutes = durationMinutes,
                    title = record.title ?: getExerciseTypeName(record.exerciseType)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getExerciseTypeName(type: Int): String {
        return when (type) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Cycling"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swimming"
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength Training"
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
            ExerciseSessionRecord.EXERCISE_TYPE_DANCING -> "Dancing"
            else -> "Workout"
        }
    }

    // ==================== UTILITY ====================

    /**
     * Calculate recovery score based on biometric data
     */
    suspend fun calculateRecoveryScore(): Int {
        val sleepData = getLastNightSleep()
        val hrvData = getHrvRecords(1).firstOrNull()
        val restingHr = getRestingHeartRate(1).firstOrNull()

        var score = 50 // Base score

        // Sleep quality impact (max +30)
        sleepData?.let { sleep ->
            val durationHours = sleep.durationMinutes / 60f
            score += when {
                durationHours >= 8 -> 30
                durationHours >= 7 -> 25
                durationHours >= 6 -> 15
                durationHours >= 5 -> 5
                else -> -10
            }

            // Deep sleep bonus
            val deepSleepRatio = sleep.deepSleepMinutes.toFloat() / sleep.durationMinutes
            score += when {
                deepSleepRatio >= 0.20 -> 10
                deepSleepRatio >= 0.15 -> 5
                else -> 0
            }
        }

        // HRV impact (max +15)
        hrvData?.let { hrv ->
            score += when {
                hrv.avgHrv >= 60 -> 15
                hrv.avgHrv >= 50 -> 10
                hrv.avgHrv >= 40 -> 5
                hrv.avgHrv >= 30 -> 0
                else -> -5
            }
        }

        // Resting heart rate impact (max +5)
        restingHr?.let { rhr ->
            score += when {
                rhr.bpm < 55 -> 5
                rhr.bpm < 65 -> 3
                rhr.bpm < 75 -> 0
                else -> -5
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun kotlinx.datetime.Instant.toJavaInstant(): java.time.Instant {
        return java.time.Instant.ofEpochMilli(this.toEpochMilliseconds())
    }
}

// Data classes for Health Connect data
data class SleepData(
    val date: String,
    val durationMinutes: Int,
    val deepSleepMinutes: Int,
    val remSleepMinutes: Int,
    val lightSleepMinutes: Int,
    val awakeMinutes: Int,
    val efficiency: Float,
    val startTime: String,
    val endTime: String
)

data class HrvData(
    val date: String,
    val avgHrv: Float,
    val minHrv: Float,
    val maxHrv: Float,
    val readings: Int
)

data class HeartRateData(
    val date: String,
    val avgBpm: Int,
    val minBpm: Int,
    val maxBpm: Int,
    val readings: Int
)

data class RestingHeartRateData(
    val date: String,
    val bpm: Int
)

data class StepsData(
    val date: String,
    val totalSteps: Int,
    val sources: List<String>
)

data class ExerciseData(
    val date: String,
    val type: String,
    val durationMinutes: Int,
    val title: String
)
