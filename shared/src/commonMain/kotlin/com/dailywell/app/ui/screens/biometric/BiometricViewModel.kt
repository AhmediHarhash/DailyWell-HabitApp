package com.dailywell.app.ui.screens.biometric

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.BiometricRepository
import com.dailywell.app.ui.components.DailyWellIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BiometricUiState(
    val dashboardSummary: BiometricDashboardSummary = BiometricDashboardSummary(),
    val correlations: List<BiometricCorrelation> = emptyList(),
    val insights: List<BiometricInsight> = emptyList(),
    val sleepRecords: List<SleepBiometricRecord> = emptyList(),
    val hrvRecords: List<HrvRecord> = emptyList(),
    val connectedDevices: List<ConnectedDevice> = emptyList(),
    val selectedTab: BiometricTab = BiometricTab.OVERVIEW,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val showDeviceConnectDialog: Boolean = false
)

enum class BiometricTab(val title: String, val emoji: String, val icon: ImageVector) {
    OVERVIEW("Overview", "📊", DailyWellIcons.Analytics.BarChart),
    SLEEP("Sleep", "😴", DailyWellIcons.Habits.Sleep),
    HRV("HRV", "💗", DailyWellIcons.Health.Heart),
    CORRELATIONS("Correlations", "🔗", DailyWellIcons.Analytics.Correlation)
}

/**
 * ViewModel for Biometric Correlation Dashboard
 */
class BiometricViewModel(
    private val biometricRepository: BiometricRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiometricUiState())
    val uiState: StateFlow<BiometricUiState> = _uiState.asStateFlow()

    init {
        loadBiometricData()
    }

    private fun loadBiometricData() {
        // Load dashboard summary
        viewModelScope.launch {
            biometricRepository.getDashboardSummary().collect { summary ->
                _uiState.value = _uiState.value.copy(
                    dashboardSummary = summary,
                    isLoading = false
                )
            }
        }

        // Load correlations
        viewModelScope.launch {
            biometricRepository.getCorrelations().collect { correlations ->
                _uiState.value = _uiState.value.copy(correlations = correlations)
            }
        }

        // Load insights
        viewModelScope.launch {
            biometricRepository.getInsights().collect { insights ->
                _uiState.value = _uiState.value.copy(insights = insights)
            }
        }

        // Load sleep records
        viewModelScope.launch {
            biometricRepository.getSleepRecords(30).collect { records ->
                _uiState.value = _uiState.value.copy(sleepRecords = records)
            }
        }

        // Load HRV records
        viewModelScope.launch {
            biometricRepository.getHrvRecords(30).collect { records ->
                _uiState.value = _uiState.value.copy(hrvRecords = records)
            }
        }

        // Load connected devices
        viewModelScope.launch {
            biometricRepository.getConnectedDevices().collect { devices ->
                _uiState.value = _uiState.value.copy(connectedDevices = devices)
            }
        }
    }

    fun selectTab(tab: BiometricTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun showDeviceConnectDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeviceConnectDialog = show)
    }

    fun connectDevice(source: BiometricSource) {
        viewModelScope.launch {
            biometricRepository.connectDevice(source, source.displayName)
            _uiState.value = _uiState.value.copy(showDeviceConnectDialog = false)
        }
    }

    fun disconnectDevice(source: BiometricSource) {
        viewModelScope.launch {
            biometricRepository.disconnectDevice(source)
        }
    }

    fun syncAllDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            _uiState.value.connectedDevices.forEach { device ->
                biometricRepository.syncDevice(device.source)
            }
            biometricRepository.generateInsights()
            _uiState.value = _uiState.value.copy(isSyncing = false)
        }
    }

    fun dismissInsight(insightId: String) {
        viewModelScope.launch {
            biometricRepository.dismissInsight(insightId)
        }
    }

    fun analyzeCorrelations(habitCompletions: Map<String, List<Boolean>>) {
        viewModelScope.launch {
            biometricRepository.analyzeCorrelations(habitCompletions)
        }
    }

    fun getRecoveryScoreColor(score: Int): String {
        return when {
            score >= 80 -> "green"
            score >= 60 -> "yellow"
            score >= 40 -> "orange"
            else -> "red"
        }
    }

    fun formatSleepDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    }

    fun formatHrvTrend(trend: Float): String {
        val sign = if (trend >= 0) "+" else ""
        return "$sign${"%.1f".format(trend)}%"
    }
}
