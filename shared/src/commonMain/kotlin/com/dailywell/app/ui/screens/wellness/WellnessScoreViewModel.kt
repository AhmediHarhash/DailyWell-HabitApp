package com.dailywell.app.ui.screens.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.StreakInfo
import com.dailywell.app.data.model.WeekData
import com.dailywell.app.data.model.WeeklyWellnessScore
import com.dailywell.app.data.model.WellnessScoreCalculator
import com.dailywell.app.data.repository.EntryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WellnessScoreUiState(
    val score: WeeklyWellnessScore? = null,
    val isLoading: Boolean = true
)

class WellnessScoreViewModel(
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WellnessScoreUiState())
    val uiState: StateFlow<WellnessScoreUiState> = _uiState.asStateFlow()

    init {
        loadScore()
    }

    private fun loadScore() {
        viewModelScope.launch {
            combine(
                entryRepository.getWeekData(0),
                entryRepository.getStreakInfo()
            ) { weekData, streakInfo ->
                WellnessScoreCalculator.calculate(weekData, streakInfo, previousWeekScore = null)
            }.collect { score ->
                _uiState.update { it.copy(score = score, isLoading = false) }
            }
        }
    }
}
