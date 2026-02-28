package com.dailywell.app.ui.screens.healthconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.repository.HealthConnectRepository
import com.dailywell.app.data.repository.HealthConnectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for Health Connect setup and management screen
 * PRODUCTION-READY: Full Health Connect integration (Task #10)
 *
 * Provides:
 * - Connection status checking
 * - Permission management
 * - Health data display
 * - Manual sync functionality
 */
class HealthConnectViewModel(
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthConnectUiState())
    val uiState: StateFlow<HealthConnectUiState> = _uiState.asStateFlow()

    init {
        checkConnectionStatus()
    }

    /**
     * Check Health Connect availability and permission status
     */
    fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                val status = healthConnectRepository.getStatus()

                val connectionStatus = when (status) {
                    HealthConnectStatus.AVAILABLE -> {
                        // Check permissions
                        if (healthConnectRepository.hasAllPermissions()) {
                            ConnectionStatus.CONNECTED
                        } else {
                            ConnectionStatus.PERMISSIONS_REQUIRED
                        }
                    }
                    HealthConnectStatus.NOT_INSTALLED -> ConnectionStatus.NOT_INSTALLED
                    HealthConnectStatus.NOT_SUPPORTED -> ConnectionStatus.NOT_SUPPORTED
                    HealthConnectStatus.API_UNAVAILABLE -> ConnectionStatus.NOT_SUPPORTED
                }

                _uiState.update { it.copy(status = connectionStatus) }

                // If connected, load health data
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    loadHealthData()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(status = ConnectionStatus.NOT_SUPPORTED) }
            }
        }
    }

    /**
     * Load current health data from Health Connect
     */
    private fun loadHealthData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }

            try {
                var sleepMinutes = 0
                var steps = 0L
                var exerciseMinutes = 0
                var waterGlasses = 0
                var heartRate = 0.0

                // Get sleep data
                healthConnectRepository.getLastNightSleep().onSuccess { sleep ->
                    sleep?.let { sleepMinutes = it.durationMinutes }
                }

                // Get today's steps
                healthConnectRepository.getTodaySteps().onSuccess { stepsCount ->
                    steps = stepsCount.toLong()
                }

                // Get today's exercise
                val now = Clock.System.now()
                val startOfDay = now.toLocalDateTime(TimeZone.currentSystemDefault())
                    .date.atStartOfDayIn(TimeZone.currentSystemDefault())

                healthConnectRepository.getWorkoutSessions(startOfDay, now).onSuccess { workouts ->
                    exerciseMinutes = workouts.sumOf { it.durationMinutes }
                }

                // Get resting heart rate
                healthConnectRepository.getRestingHeartRate().onSuccess { hr ->
                    hr?.let { heartRate = it.toDouble() }
                }

                // Get last sync time
                val syncTime = healthConnectRepository.getLastSyncTime()
                val lastSyncDisplay = syncTime?.toLocalDateTime(TimeZone.currentSystemDefault())?.let {
                    "Today at ${it.hour}:${it.minute.toString().padStart(2, '0')}"
                } ?: ""

                val healthData = HealthDataUi(
                    sleepMinutes = sleepMinutes,
                    steps = steps,
                    exerciseMinutes = exerciseMinutes,
                    waterGlasses = waterGlasses,
                    heartRate = heartRate
                )

                _uiState.update {
                    it.copy(
                        healthData = healthData,
                        lastSyncTime = lastSyncDisplay,
                        isSyncing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    /**
     * Manually sync all health data
     */
    fun syncNow() {
        viewModelScope.launch {
            if (_uiState.value.status != ConnectionStatus.CONNECTED) return@launch

            _uiState.update { it.copy(isSyncing = true) }

            try {
                val result = healthConnectRepository.syncAllData()
                result.onSuccess { summary ->
                    val syncTime = summary.syncTime.toLocalDateTime(TimeZone.currentSystemDefault())
                    _uiState.update {
                        it.copy(
                            lastSyncTime = "Today at ${syncTime.hour}:${syncTime.minute.toString().padStart(2, '0')}"
                        )
                    }
                }

                // Reload health data
                loadHealthData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    /**
     * Request Health Connect permissions
     * Note: Actual permission request must be handled by Activity
     */
    fun requestPermissions() {
        // This triggers the permission flow in the Activity
        // The ViewModel just tracks the state
        viewModelScope.launch {
            // After permissions are granted (handled by Activity),
            // the status will be re-checked
            checkConnectionStatus()
        }
    }

    /**
     * Get auto-complete suggestions based on current health data
     */
    fun getAutoCompleteSuggestions(): Map<String, Boolean> {
        val healthData = _uiState.value.healthData ?: return emptyMap()

        return buildMap {
            // Rest: 7+ hours of sleep (420 minutes)
            put("rest", healthData.sleepMinutes >= 420)

            // Move: 30+ minutes of exercise or 10,000+ steps
            put("move", healthData.exerciseMinutes >= 30 || healthData.steps >= 10000)

            // Hydrate: Water tracking (would need explicit water data)
            put("hydrate", healthData.waterGlasses >= 8)
        }
    }
}
