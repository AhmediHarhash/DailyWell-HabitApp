package com.dailywell.app.health

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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.*
import java.time.ZoneId
import kotlin.time.Duration.Companion.hours

/**
 * Manages Health Connect integration for auto-tracking habits
 * Supports: Sleep, Steps/Exercise, Water intake
 */
class HealthConnectManager(private val context: Context) {

    private val _connectionState = MutableStateFlow(HealthConnectionState.NOT_CHECKED)
    val connectionState: StateFlow<HealthConnectionState> = _connectionState.asStateFlow()

    private val _healthData = MutableStateFlow(HealthData())
    val healthData: StateFlow<HealthData> = _healthData.asStateFlow()

    // Permissions we need for auto-tracking
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
    )

    private var healthConnectClient: HealthConnectClient? = null

    /**
     * Check if Health Connect is available and initialized
     */
    suspend fun checkAvailability(): HealthConnectionState {
        val availabilityStatus = HealthConnectClient.getSdkStatus(context)

        val state = when (availabilityStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> HealthConnectionState.NOT_SUPPORTED
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectionState.NOT_INSTALLED
            HealthConnectClient.SDK_AVAILABLE -> {
                healthConnectClient = HealthConnectClient.getOrCreate(context)
                checkPermissions()
            }
            else -> HealthConnectionState.NOT_SUPPORTED
        }

        _connectionState.value = state
        return state
    }

    /**
     * Check if we have all required permissions
     */
    private suspend fun checkPermissions(): HealthConnectionState {
        val client = healthConnectClient ?: return HealthConnectionState.NOT_SUPPORTED

        val granted = client.permissionController.getGrantedPermissions()
        return if (granted.containsAll(requiredPermissions)) {
            HealthConnectionState.CONNECTED
        } else {
            HealthConnectionState.PERMISSIONS_REQUIRED
        }
    }

    /**
     * Create permission request contract
     */
    fun createPermissionRequestContract() = PermissionController.createRequestPermissionResultContract()

    /**
     * Open Health Connect app for manual permission management
     */
    fun openHealthConnectSettings() {
        val intent = Intent().apply {
            action = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS"
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open Play Store to install Health Connect
     */
    fun openPlayStoreForHealthConnect() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Read today's health data for auto-tracking
     */
    suspend fun readTodayHealthData(): HealthData {
        val client = healthConnectClient ?: return HealthData()

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startOfDay = today.atStartOfDayIn(TimeZone.currentSystemDefault())
        val endOfDay = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault())

        val startInstant = java.time.Instant.ofEpochMilli(startOfDay.toEpochMilliseconds())
        val endInstant = java.time.Instant.ofEpochMilli(endOfDay.toEpochMilliseconds())

        val timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)

        val healthData = HealthData(
            sleepMinutes = readSleepData(client, today),
            steps = readStepsData(client, timeRangeFilter),
            exerciseMinutes = readExerciseData(client, timeRangeFilter),
            waterMl = readHydrationData(client, timeRangeFilter),
            averageHeartRate = readHeartRateData(client, timeRangeFilter)
        )

        _healthData.value = healthData
        return healthData
    }

    /**
     * Read last night's sleep data (sleep ending today)
     */
    private suspend fun readSleepData(client: HealthConnectClient, today: LocalDate): Int {
        try {
            // Look for sleep sessions that ended today (last night's sleep)
            val yesterday = today.minus(1, DateTimeUnit.DAY)
            val startOfYesterday = yesterday.atStartOfDayIn(TimeZone.currentSystemDefault())
            val endOfToday = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(TimeZone.currentSystemDefault())

            val startInstant = java.time.Instant.ofEpochMilli(startOfYesterday.toEpochMilliseconds())
            val endInstant = java.time.Instant.ofEpochMilli(endOfToday.toEpochMilliseconds())

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                )
            )

            // Sum up all sleep sessions
            var totalSleepMinutes = 0
            for (session in response.records) {
                val durationMinutes = java.time.Duration.between(session.startTime, session.endTime).toMinutes()
                totalSleepMinutes += durationMinutes.toInt()
            }

            return totalSleepMinutes
        } catch (e: Exception) {
            return 0
        }
    }

    /**
     * Read today's steps
     */
    private suspend fun readStepsData(client: HealthConnectClient, timeRangeFilter: TimeRangeFilter): Long {
        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeRangeFilter
                )
            )

            return response.records.sumOf { it.count }
        } catch (e: Exception) {
            return 0
        }
    }

    /**
     * Read today's exercise duration
     */
    private suspend fun readExerciseData(client: HealthConnectClient, timeRangeFilter: TimeRangeFilter): Int {
        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeRangeFilter
                )
            )

            var totalMinutes = 0
            for (session in response.records) {
                val durationMinutes = java.time.Duration.between(session.startTime, session.endTime).toMinutes()
                totalMinutes += durationMinutes.toInt()
            }

            return totalMinutes
        } catch (e: Exception) {
            return 0
        }
    }

    /**
     * Read today's water intake
     */
    private suspend fun readHydrationData(client: HealthConnectClient, timeRangeFilter: TimeRangeFilter): Double {
        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HydrationRecord::class,
                    timeRangeFilter = timeRangeFilter
                )
            )

            return response.records.sumOf { it.volume.inMilliliters }
        } catch (e: Exception) {
            return 0.0
        }
    }

    /**
     * Read today's average heart rate
     */
    private suspend fun readHeartRateData(client: HealthConnectClient, timeRangeFilter: TimeRangeFilter): Double {
        try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = timeRangeFilter
                )
            )

            val allSamples = response.records.flatMap { it.samples }
            return if (allSamples.isNotEmpty()) {
                allSamples.map { it.beatsPerMinute }.average()
            } else {
                0.0
            }
        } catch (e: Exception) {
            return 0.0
        }
    }

    /**
     * Check if habits should be auto-completed based on health data
     */
    fun getAutoCompletionSuggestions(healthData: HealthData): Map<String, Boolean> {
        val suggestions = mutableMapOf<String, Boolean>()

        // Sleep: 7+ hours = 420 minutes
        if (healthData.sleepMinutes >= 420) {
            suggestions["sleep"] = true
        }

        // Move: 30+ minutes of exercise OR 10,000+ steps
        if (healthData.exerciseMinutes >= 30 || healthData.steps >= 10000) {
            suggestions["move"] = true
        }

        // Hydrate: 8 glasses = ~2000ml (250ml per glass)
        if (healthData.waterMl >= 2000) {
            suggestions["water"] = true
        }

        return suggestions
    }
}

/**
 * Current state of Health Connect connection
 */
enum class HealthConnectionState {
    NOT_CHECKED,
    NOT_SUPPORTED,      // Device doesn't support Health Connect
    NOT_INSTALLED,      // Health Connect app not installed
    PERMISSIONS_REQUIRED, // Need to request permissions
    CONNECTED           // Ready to use
}

/**
 * Health data read from Health Connect
 */
data class HealthData(
    val sleepMinutes: Int = 0,
    val steps: Long = 0,
    val exerciseMinutes: Int = 0,
    val waterMl: Double = 0.0,
    val averageHeartRate: Double = 0.0
) {
    val sleepHours: Double get() = sleepMinutes / 60.0
    val waterGlasses: Int get() = (waterMl / 250).toInt() // 250ml per glass

    fun getSleepDisplay(): String {
        val hours = sleepMinutes / 60
        val mins = sleepMinutes % 60
        return if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    }

    fun getExerciseDisplay(): String {
        return if (exerciseMinutes >= 60) {
            val hours = exerciseMinutes / 60
            val mins = exerciseMinutes % 60
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        } else {
            "${exerciseMinutes}m"
        }
    }
}
