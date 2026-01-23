package com.dailywell.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.ThemeMode
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val allHabits: List<Habit> = emptyList(),
    val customHabits: List<Habit> = emptyList(),
    val customHabitCount: Int = 0,
    val isLoading: Boolean = true
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.getSettings(),
                habitRepository.getAllHabits(),
                habitRepository.getCustomHabits()
            ) { settings, allHabits, customHabits ->
                SettingsUiState(
                    settings = settings,
                    allHabits = allHabits,
                    customHabits = customHabits,
                    customHabitCount = customHabits.size,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                _uiState.value.settings.copy(reminderEnabled = enabled)
            )
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                _uiState.value.settings.copy(
                    reminderHour = hour,
                    reminderMinute = minute
                )
            )
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                _uiState.value.settings.copy(themeMode = mode)
            )
        }
    }

    fun setIncludeWeekends(include: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                _uiState.value.settings.copy(includeWeekends = include)
            )
        }
    }

    fun setHabitEnabled(habitId: String, enabled: Boolean) {
        viewModelScope.launch {
            habitRepository.setHabitEnabled(habitId, enabled)
        }
    }

    fun createCustomHabit(
        name: String,
        emoji: String,
        threshold: String,
        question: String
    ) {
        viewModelScope.launch {
            habitRepository.createCustomHabit(name, emoji, threshold, question)
        }
    }

    fun deleteCustomHabit(habitId: String) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
        }
    }
}
