package com.dailywell.app.data.repository

import com.dailywell.app.api.HealthConnectService
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of BiometricRepository with REAL Health Connect integration
 * Persists biometric data to DataStore so it survives app restarts
 */
class BiometricRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val healthConnectService: HealthConnectService
) : BiometricRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private companion object {
        const val BIOMETRIC_DATA_KEY = "biometric_data"
        const val RECOVERY_SCORE_KEY = "biometric_recovery_score"
    }

    private val _biometricData = MutableStateFlow(BiometricData())
    private val _recoveryScore = MutableStateFlow(50)
    private val _connectionStatus = MutableStateFlow(HealthConnectService.ConnectionStatus.NOT_CHECKED)

    init {
        scope.launch {
            try {
                val dataJson = dataStoreManager.getString(BIOMETRIC_DATA_KEY).first()
                if (dataJson != null) {
                    _biometricData.value = json.decodeFromString<BiometricData>(dataJson)
                }
            } catch (_: Exception) {}

            try {
                val scoreStr = dataStoreManager.getString(RECOVERY_SCORE_KEY).first()
                if (scoreStr != null) {
                    _recoveryScore.value = scoreStr.toIntOrNull() ?: 50
                }
            } catch (_: Exception) {}
        }
    }

    private fun persistBiometricData() {
        scope.launch {
            try {
                dataStoreManager.putString(BIOMETRIC_DATA_KEY, json.encodeToString(_biometricData.value))
            } catch (_: Exception) {}
        }
    }

    private fun persistRecoveryScore() {
        scope.launch {
            try {
                dataStoreManager.putString(RECOVERY_SCORE_KEY, _recoveryScore.value.toString())
            } catch (_: Exception) {}
        }
    }

    override fun getBiometricData(): Flow<BiometricData> = _biometricData

    override fun getDashboardSummary(): Flow<BiometricDashboardSummary> {
        return _biometricData.map { data ->
            val todaySleep = data.sleepRecords.firstOrNull()
            val todayHrv = data.hrvRecords.firstOrNull()

            BiometricDashboardSummary(
                todaySleepScore = calculateSleepScore(todaySleep),
                todayRecoveryScore = _recoveryScore.value,
                weeklyHrvTrend = calculateHrvTrend(data.hrvRecords),
                avgSleepDuration = data.sleepRecords.takeIf { it.isNotEmpty() }
                    ?.map { it.durationMinutes / 60f }?.average()?.toFloat() ?: 0f,
                avgDeepSleep = data.sleepRecords.takeIf { it.isNotEmpty() }
                    ?.map { it.deepSleepMinutes / 60f }?.average()?.toFloat() ?: 0f,
                habitImpactScore = calculateHabitImpactScore(data.correlations),
                topCorrelation = data.correlations.maxByOrNull { kotlin.math.abs(it.correlationStrength) },
                latestInsight = data.insights.firstOrNull()
            )
        }
    }

    override fun getCorrelations(): Flow<List<BiometricCorrelation>> {
        return _biometricData.map { it.correlations }
    }

    override fun getInsights(): Flow<List<BiometricInsight>> {
        return _biometricData.map { it.insights }
    }

    override fun getConnectedDevices(): Flow<List<ConnectedDevice>> {
        return _biometricData.map { it.connectedDevices }
    }

    override fun getSleepRecords(days: Int): Flow<List<SleepBiometricRecord>> {
        return _biometricData.map { it.sleepRecords.take(days) }
    }

    override suspend fun addSleepRecord(record: SleepBiometricRecord) {
        val currentData = _biometricData.value
        _biometricData.value = currentData.copy(
            sleepRecords = listOf(record) + currentData.sleepRecords
        )
        persistBiometricData()
    }

    override fun getHrvRecords(days: Int): Flow<List<HrvRecord>> {
        return _biometricData.map { it.hrvRecords.take(days) }
    }

    override suspend fun addHrvRecord(record: HrvRecord) {
        val currentData = _biometricData.value
        _biometricData.value = currentData.copy(
            hrvRecords = listOf(record) + currentData.hrvRecords
        )
        persistBiometricData()
    }

    override fun getActivityRecords(days: Int): Flow<List<ActivityRecord>> {
        return _biometricData.map { it.activityRecords.take(days) }
    }

    override suspend fun addActivityRecord(record: ActivityRecord) {
        val currentData = _biometricData.value
        _biometricData.value = currentData.copy(
            activityRecords = listOf(record) + currentData.activityRecords
        )
        persistBiometricData()
    }

    override suspend fun connectDevice(source: BiometricSource, deviceName: String) {
        // For Health Connect, check availability first
        if (source == BiometricSource.HEALTH_CONNECT) {
            val status = healthConnectService.checkAvailability()
            _connectionStatus.value = status

            if (status == HealthConnectService.ConnectionStatus.PERMISSIONS_GRANTED ||
                status == HealthConnectService.ConnectionStatus.AVAILABLE) {

                val now = Clock.System.now().toString()
                val device = ConnectedDevice(
                    source = source,
                    deviceName = "Health Connect",
                    connectedAt = now,
                    lastSyncedAt = now,
                    isActive = true,
                    syncPermissions = listOf(
                        SyncPermission.SLEEP,
                        SyncPermission.HRV,
                        SyncPermission.HEART_RATE,
                        SyncPermission.STEPS,
                        SyncPermission.WORKOUTS
                    )
                )

                val currentData = _biometricData.value
                _biometricData.value = currentData.copy(
                    connectedDevices = currentData.connectedDevices
                        .filter { it.source != source } + device
                )

                // Immediately sync data
                syncDevice(source)
            }
        } else {
            // Other sources (Oura, Fitbit, Apple Watch) require their own SDK integrations
            // Currently only Health Connect is fully supported
            // When connected, the device entry is created but data comes through Health Connect
            // if the device syncs to Health Connect (which most wearables do on Android)
            val now = Clock.System.now().toString()
            val device = ConnectedDevice(
                source = source,
                deviceName = deviceName,
                connectedAt = now,
                lastSyncedAt = now,
                isActive = true,
                // These devices typically sync to Health Connect, so we can read their data from there
                syncPermissions = listOf(
                    SyncPermission.SLEEP,
                    SyncPermission.HRV,
                    SyncPermission.HEART_RATE,
                    SyncPermission.STEPS,
                    SyncPermission.WORKOUTS
                )
            )

            val currentData = _biometricData.value
            _biometricData.value = currentData.copy(
                connectedDevices = currentData.connectedDevices
                    .filter { it.source != source } + device
            )

            // Try to sync from Health Connect since most wearables push data there
            val status = healthConnectService.checkAvailability()
            if (status == HealthConnectService.ConnectionStatus.PERMISSIONS_GRANTED || status == HealthConnectService.ConnectionStatus.AVAILABLE) {
                syncHealthConnectData()
            }
        }
    }

    override suspend fun disconnectDevice(source: BiometricSource) {
        val currentData = _biometricData.value
        _biometricData.value = currentData.copy(
            connectedDevices = currentData.connectedDevices
                .filter { it.source != source }
        )
    }

    override suspend fun syncDevice(source: BiometricSource) {
        val now = Clock.System.now().toString()

        if (source == BiometricSource.HEALTH_CONNECT) {
            // Fetch REAL data from Health Connect
            syncHealthConnectData()
        }

        // Update last synced time
        val currentData = _biometricData.value
        _biometricData.value = currentData.copy(
            connectedDevices = currentData.connectedDevices.map { device ->
                if (device.source == source) {
                    device.copy(lastSyncedAt = now)
                } else device
            },
            lastSyncedAt = now
        )
    }

    /**
     * Sync REAL data from Health Connect
     */
    private suspend fun syncHealthConnectData() {
        val currentData = _biometricData.value

        // Fetch real sleep data
        val sleepData = healthConnectService.getSleepRecords(7)
        val sleepRecords = sleepData.map { sleep ->
            SleepBiometricRecord(
                date = sleep.date,
                durationMinutes = sleep.durationMinutes,
                deepSleepMinutes = sleep.deepSleepMinutes,
                remSleepMinutes = sleep.remSleepMinutes,
                lightSleepMinutes = sleep.lightSleepMinutes,
                awakeMinutes = sleep.awakeMinutes,
                efficiency = sleep.efficiency,
                latencyMinutes = 10 // Health Connect doesn't track this directly
            )
        }

        // Fetch real HRV data
        val hrvData = healthConnectService.getHrvRecords(7)
        val restingHrData = healthConnectService.getRestingHeartRate(7)

        val hrvRecords = hrvData.map { hrv ->
            val restingHr = restingHrData.find { it.date == hrv.date }?.bpm ?: 60
            HrvRecord(
                date = hrv.date,
                timestamp = Clock.System.now().toString(),
                avgHrv = hrv.avgHrv,
                minHrv = hrv.minHrv,
                maxHrv = hrv.maxHrv,
                restingHeartRate = restingHr
            )
        }

        // Fetch real activity data
        val stepsData = healthConnectService.getStepRecords(7)
        val exerciseData = healthConnectService.getExerciseSessions(7)

        val activityRecords = stepsData.map { steps ->
            val exercisesForDay = exerciseData.filter { it.date == steps.date }
            val activeMinutes = exercisesForDay.sumOf { it.durationMinutes }

            ActivityRecord(
                date = steps.date,
                steps = steps.totalSteps,
                activeMinutes = activeMinutes,
                caloriesBurned = (steps.totalSteps * 0.04f).toInt(), // Rough estimate
                workouts = exercisesForDay.map { exercise ->
                    WorkoutRecord(
                        type = exercise.type,
                        durationMinutes = exercise.durationMinutes,
                        startTime = exercise.date,
                        endTime = exercise.date // Using same date as both start and end for simplicity
                    )
                }
            )
        }

        // Calculate real recovery score
        val recoveryScore = healthConnectService.calculateRecoveryScore()
        _recoveryScore.value = recoveryScore
        persistRecoveryScore()

        // Update biometric data with real values
        _biometricData.value = currentData.copy(
            sleepRecords = sleepRecords.ifEmpty { currentData.sleepRecords },
            hrvRecords = hrvRecords.ifEmpty { currentData.hrvRecords },
            activityRecords = activityRecords.ifEmpty { currentData.activityRecords },
            lastSyncedAt = Clock.System.now().toString()
        )
        persistBiometricData()

        // Generate insights based on real data
        generateInsights()
    }

    override suspend fun analyzeCorrelations(habitCompletions: Map<String, List<Boolean>>) {
        val currentData = _biometricData.value
        val newCorrelations = BiometricAnalyzer.findCorrelations(
            habitCompletions = habitCompletions,
            sleepRecords = currentData.sleepRecords,
            hrvRecords = currentData.hrvRecords
        )

        _biometricData.value = currentData.copy(
            correlations = newCorrelations.ifEmpty { currentData.correlations }
        )
        persistBiometricData()
    }

    override suspend fun generateInsights() {
        val currentData = _biometricData.value
        val newInsights = mutableListOf<BiometricInsight>()

        // Generate insights based on real data
        val avgSleep = currentData.sleepRecords.takeIf { it.isNotEmpty() }
            ?.map { it.durationMinutes / 60f }?.average()?.toFloat() ?: 7f

        if (avgSleep < 6.5f) {
            newInsights.add(
                BiometricInsight(
                    id = "insight_low_sleep_${System.currentTimeMillis()}",
                    type = BiometricInsightType.SLEEP_IMPACT,
                    title = "Sleep needs attention",
                    description = "Your average sleep (${String.format("%.1f", avgSleep)}hrs) is below optimal. This may affect your habit completion.",
                    emoji = "😴",
                    recommendation = "Aim for 7-8 hours of sleep to boost your habit success rate.",
                    biometricType = BiometricType.SLEEP_DURATION,
                    severity = InsightSeverity.WARNING,
                    createdAt = Clock.System.now().toString()
                )
            )
        }

        val avgDeepSleep = currentData.sleepRecords.takeIf { it.isNotEmpty() }
            ?.map { it.deepSleepMinutes / 60f }?.average()?.toFloat() ?: 1.5f

        if (avgDeepSleep >= 1.5f) {
            newInsights.add(
                BiometricInsightTemplates.deepSleepCelebration(avgDeepSleep)
            )
        }

        // HRV trend insight
        if (currentData.hrvRecords.size >= 2) {
            val recentHrv = currentData.hrvRecords.take(3).map { it.avgHrv }.average()
            val previousHrv = currentData.hrvRecords.drop(3).take(3).map { it.avgHrv }.average()

            if (recentHrv > previousHrv * 1.1) {
                newInsights.add(
                    BiometricInsight(
                        id = "insight_hrv_up_${System.currentTimeMillis()}",
                        type = BiometricInsightType.HRV_TREND,
                        title = "HRV trending upward!",
                        description = "Your HRV has improved by ${((recentHrv / previousHrv - 1) * 100).toInt()}% recently. Great recovery!",
                        emoji = "📈",
                        biometricType = BiometricType.HRV,
                        severity = InsightSeverity.SUCCESS,
                        createdAt = Clock.System.now().toString()
                    )
                )
            }
        }

        // Activity insight
        val todaySteps = healthConnectService.getTodaySteps()
        if (todaySteps > 0) {
            newInsights.add(
                BiometricInsight(
                    id = "insight_steps_${System.currentTimeMillis()}",
                    type = if (todaySteps >= 10000) BiometricInsightType.CELEBRATION else BiometricInsightType.HABIT_BENEFIT,
                    title = "Today's activity",
                    description = "You've taken $todaySteps steps today${if (todaySteps >= 10000) " - excellent!" else ". Keep moving!"}",
                    emoji = if (todaySteps >= 10000) "🏆" else "🚶",
                    biometricType = BiometricType.STEPS,
                    severity = if (todaySteps >= 10000) InsightSeverity.SUCCESS else InsightSeverity.INFO,
                    createdAt = Clock.System.now().toString()
                )
            )
        }

        if (newInsights.isNotEmpty()) {
            _biometricData.value = currentData.copy(
                insights = newInsights + currentData.insights.take(10)
            )
            persistBiometricData()
        }
    }

    override fun getRecoveryScore(): Flow<Int> = _recoveryScore

    override suspend fun dismissInsight(insightId: String) {
        val currentData = _biometricData.value
        _biometricData.value = currentData.copy(
            insights = currentData.insights.filter { it.id != insightId }
        )
        persistBiometricData()
    }

    // ==================== NEW HEALTH CONNECT SPECIFIC METHODS ====================

    /**
     * Check Health Connect availability
     */
    suspend fun checkHealthConnectAvailability(): HealthConnectService.ConnectionStatus {
        val status = healthConnectService.checkAvailability()
        _connectionStatus.value = status
        return status
    }

    /**
     * Get Health Connect connection status
     */
    fun getConnectionStatus(): Flow<HealthConnectService.ConnectionStatus> {
        return healthConnectService.connectionStatus
    }

    /**
     * Get required Health Connect permissions
     */
    fun getRequiredPermissions() = healthConnectService.requiredPermissions

    /**
     * Get permission request contract
     */
    fun getPermissionRequestContract() = healthConnectService.getPermissionRequestContract()

    /**
     * Check if all permissions are granted
     */
    suspend fun hasAllPermissions(): Boolean = healthConnectService.hasAllPermissions()

    /**
     * Get today's real step count
     */
    suspend fun getTodaySteps(): Int = healthConnectService.getTodaySteps()

    /**
     * Get last night's real sleep data
     */
    suspend fun getLastNightSleep() = healthConnectService.getLastNightSleep()

    /**
     * Get today's real heart rate
     */
    suspend fun getTodayHeartRate() = healthConnectService.getTodayHeartRate()

    /**
     * Calculate real recovery score
     */
    suspend fun calculateRecoveryScore(): Int {
        val score = healthConnectService.calculateRecoveryScore()
        _recoveryScore.value = score
        return score
    }

    private fun calculateSleepScore(sleep: SleepBiometricRecord?): Int {
        if (sleep == null) return 0
        var score = 0

        // Duration (40 points max)
        val durationHours = sleep.durationMinutes / 60f
        score += when {
            durationHours >= 8 -> 40
            durationHours >= 7 -> 35
            durationHours >= 6.5 -> 25
            durationHours >= 6 -> 15
            else -> 5
        }

        // Efficiency (30 points max)
        score += (sleep.efficiency * 0.3f).toInt()

        // Deep sleep ratio (30 points max)
        val deepSleepRatio = sleep.deepSleepMinutes.toFloat() / sleep.durationMinutes
        score += when {
            deepSleepRatio >= 0.20 -> 30
            deepSleepRatio >= 0.15 -> 25
            deepSleepRatio >= 0.10 -> 15
            else -> 5
        }

        return score.coerceIn(0, 100)
    }

    private fun calculateHrvTrend(hrvRecords: List<HrvRecord>): Float {
        if (hrvRecords.size < 2) return 0f

        val recent = hrvRecords.take(7).map { it.avgHrv }.average()
        val previous = hrvRecords.drop(7).take(7).map { it.avgHrv }.average()

        if (previous == 0.0) return 0f
        return ((recent - previous) / previous * 100).toFloat()
    }

    private fun calculateHabitImpactScore(correlations: List<BiometricCorrelation>): Int {
        if (correlations.isEmpty()) return 0

        val avgStrength = correlations.map { kotlin.math.abs(it.correlationStrength) }.average()
        return (avgStrength * 100).toInt().coerceIn(0, 100)
    }
}
