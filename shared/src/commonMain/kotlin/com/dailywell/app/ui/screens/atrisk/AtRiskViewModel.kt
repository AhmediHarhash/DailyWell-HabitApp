package com.dailywell.app.ui.screens.atrisk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.AtRiskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AtRiskUiState(
    val isLoading: Boolean = false,
    val dailySummary: DailyRiskSummary? = null,
    val riskAssessments: List<HabitRiskAssessment> = emptyList(),
    val habitHealthList: List<HabitHealth> = emptyList(),
    val activeAlerts: List<AtRiskAlert> = emptyList(),
    val currentWeather: WeatherCondition? = null,
    val notificationSettings: AtRiskNotificationSettings = AtRiskNotificationSettings(),
    val selectedHabitHealth: HabitHealth? = null,
    val selectedHabitPattern: HabitPattern? = null,
    val error: String? = null
)

class AtRiskViewModel(
    private val atRiskRepository: AtRiskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AtRiskUiState())
    val uiState: StateFlow<AtRiskUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Refresh risk assessments
                atRiskRepository.refreshRiskAssessments()

                // Refresh habit health
                atRiskRepository.refreshHabitHealth()

                // Generate daily summary
                val summary = atRiskRepository.generateDailyRiskSummary()

                // Generate alerts
                atRiskRepository.generateAlerts()

                // Get weather
                val weather = atRiskRepository.getCurrentWeather()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dailySummary = summary,
                        currentWeather = weather,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load risk data"
                    )
                }
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            atRiskRepository.getAllRiskAssessments().collect { assessments ->
                _uiState.update { it.copy(riskAssessments = assessments) }
            }
        }

        viewModelScope.launch {
            atRiskRepository.getAllHabitHealth().collect { healthList ->
                _uiState.update { it.copy(habitHealthList = healthList) }
            }
        }

        viewModelScope.launch {
            atRiskRepository.getActiveAlerts().collect { alerts ->
                _uiState.update { it.copy(activeAlerts = alerts.filter { !it.dismissed }) }
            }
        }

        viewModelScope.launch {
            atRiskRepository.getNotificationSettings().collect { settings ->
                _uiState.update { it.copy(notificationSettings = settings) }
            }
        }
    }

    fun refreshRiskData() {
        loadData()
    }

    fun dismissAlert(alertId: String) {
        viewModelScope.launch {
            atRiskRepository.dismissAlert(alertId)
        }
    }

    fun selectHabitForDetails(habitId: String) {
        viewModelScope.launch {
            val health = _uiState.value.habitHealthList.find { it.habitId == habitId }
            val pattern = atRiskRepository.getHabitPattern(habitId)

            _uiState.update {
                it.copy(
                    selectedHabitHealth = health,
                    selectedHabitPattern = pattern
                )
            }
        }
    }

    fun clearSelectedHabit() {
        _uiState.update {
            it.copy(
                selectedHabitHealth = null,
                selectedHabitPattern = null
            )
        }
    }

    fun updateNotificationSettings(settings: AtRiskNotificationSettings) {
        viewModelScope.launch {
            atRiskRepository.updateNotificationSettings(settings)
        }
    }

    fun getHighRiskHabits(): List<HabitRiskAssessment> {
        return _uiState.value.riskAssessments.filter {
            it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL
        }
    }

    fun getMediumRiskHabits(): List<HabitRiskAssessment> {
        return _uiState.value.riskAssessments.filter {
            it.riskLevel == RiskLevel.MEDIUM
        }
    }

    fun getLowRiskHabits(): List<HabitRiskAssessment> {
        return _uiState.value.riskAssessments.filter {
            it.riskLevel == RiskLevel.LOW
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
