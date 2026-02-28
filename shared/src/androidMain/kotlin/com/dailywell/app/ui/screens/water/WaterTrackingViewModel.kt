package com.dailywell.app.ui.screens.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.WaterTrackingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel implementation for Water Tracking Screen
 * Implements the WaterTrackingViewModel interface from commonMain
 */
class WaterTrackingViewModelImpl(
    private val repository: WaterTrackingRepository
) : ViewModel(), WaterTrackingViewModel {

    private val _uiState = MutableStateFlow(WaterTrackingUiState())
    override val uiState: StateFlow<WaterTrackingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Collect today's summary
            repository.getTodaySummary()
                .collect { summary ->
                    _uiState.update { it.copy(todaySummary = summary) }
                }
        }

        viewModelScope.launch {
            // Collect settings
            repository.getSettings()
                .collect { settings ->
                    _uiState.update { it.copy(settings = settings) }
                }
        }

        viewModelScope.launch {
            // Collect recent entries
            repository.getRecentEntries()
                .collect { entries ->
                    _uiState.update { it.copy(recentEntries = entries) }
                }
        }

        // Load insights and weekly stats
        viewModelScope.launch {
            val insights = repository.generateInsights()
            val weeklyStats = repository.getWeeklyStats()
            _uiState.update {
                it.copy(
                    insights = insights,
                    weeklyStats = weeklyStats
                )
            }
        }
    }

    /**
     * Log water using a preset glass size
     */
    override fun logGlass(size: GlassSize) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val source = _uiState.value.selectedSource
            repository.logGlass(size, source).fold(
                onSuccess = {
                    refreshInsights()
                    _uiState.update { it.copy(isLoading = false, showAddDialog = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    /**
     * Log custom amount of water
     */
    override fun logCustomAmount(amountMl: Int) {
        if (amountMl <= 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val source = _uiState.value.selectedSource
            repository.logWater(amountMl, source).fold(
                onSuccess = {
                    refreshInsights()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showAddDialog = false,
                            customAmountMl = null
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    /**
     * Quick add - one tap to add preferred glass size
     */
    override fun quickAdd() {
        val preferredSize = _uiState.value.settings.preferredGlassSize
        logGlass(preferredSize)
    }

    /**
     * Remove a water entry
     */
    override fun removeEntry(entryId: String) {
        viewModelScope.launch {
            repository.removeEntry(entryId)
            refreshInsights()
        }
    }

    /**
     * Select water source
     */
    override fun selectSource(source: WaterSource) {
        _uiState.update { it.copy(selectedSource = source) }
    }

    /**
     * Update custom amount
     */
    override fun updateCustomAmount(amount: Int?) {
        _uiState.update { it.copy(customAmountMl = amount) }
    }

    /**
     * Update daily goal
     */
    override fun updateDailyGoal(goalMl: Int) {
        viewModelScope.launch {
            repository.updateDailyGoal(goalMl)
        }
    }

    /**
     * Update settings
     */
    override fun updateSettings(settings: WaterSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    /**
     * Show/hide add dialog
     */
    override fun setShowAddDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showAddDialog = show,
                selectedSource = WaterSource.WATER,
                customAmountMl = null
            )
        }
    }

    /**
     * Show/hide settings sheet
     */
    override fun setShowSettingsSheet(show: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = show) }
    }

    private fun refreshInsights() {
        viewModelScope.launch {
            val insights = repository.generateInsights()
            val weeklyStats = repository.getWeeklyStats()
            _uiState.update {
                it.copy(
                    insights = insights,
                    weeklyStats = weeklyStats
                )
            }
        }
    }
}
