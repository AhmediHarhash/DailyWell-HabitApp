package com.dailywell.app.ui.screens.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.CelebrationMessages
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.EntryRepository
import com.dailywell.app.data.repository.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WeekUiState(
    val currentWeekData: WeekData? = null,
    val previousWeekData: WeekData? = null,
    val habits: List<Habit> = emptyList(),
    val selectedWeekOffset: Int = 0,
    val weeklySummaryMessage: String = "",
    val isLoading: Boolean = true
)

class WeekViewModel(
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeekUiState())
    val uiState: StateFlow<WeekUiState> = _uiState.asStateFlow()

    private val _weekOffset = MutableStateFlow(0)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                habitRepository.getEnabledHabits(),
                _weekOffset
            ) { habits, offset ->
                habits to offset
            }.collectLatest { (habits, offset) ->
                // Load current week
                entryRepository.getWeekData(offset).collect { weekData ->
                    val completedDays = weekData.days.count {
                        it.status == CompletionStatus.COMPLETE
                    }
                    val summaryMessage = CelebrationMessages.getWeeklySummaryMessage(completedDays)

                    _uiState.update {
                        it.copy(
                            currentWeekData = weekData,
                            habits = habits,
                            selectedWeekOffset = offset,
                            weeklySummaryMessage = summaryMessage,
                            isLoading = false
                        )
                    }
                }
            }
        }

        // Also load previous week for comparison
        viewModelScope.launch {
            entryRepository.getWeekData(-1).collect { weekData ->
                _uiState.update { it.copy(previousWeekData = weekData) }
            }
        }
    }

    fun goToPreviousWeek() {
        _weekOffset.value -= 1
    }

    fun goToNextWeek() {
        if (_weekOffset.value < 0) {
            _weekOffset.value += 1
        }
    }

    fun goToCurrentWeek() {
        _weekOffset.value = 0
    }
}
