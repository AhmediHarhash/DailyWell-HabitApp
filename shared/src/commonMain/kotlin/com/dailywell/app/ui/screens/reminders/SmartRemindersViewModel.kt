package com.dailywell.app.ui.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SmartReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SmartRemindersUiState(
    val isGlobalEnabled: Boolean = true,
    val habitSettings: List<HabitReminderSettings> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for Smart Reminders configuration
 */
class SmartRemindersViewModel(
    private val smartReminderRepository: SmartReminderRepository,
    private val habitRepository: HabitRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartRemindersUiState())
    val uiState: StateFlow<SmartRemindersUiState> = _uiState.asStateFlow()

    // Fallback habits if repository unavailable
    private val defaultHabits = listOf("sleep", "water", "move", "vegetables", "calm", "connect", "unplug")

    init {
        loadReminderSettings()
    }

    private fun loadReminderSettings() {
        viewModelScope.launch {
            // Load user's actual enabled habits, fall back to defaults
            val enabledHabits = try {
                habitRepository?.getEnabledHabits()?.first()?.map { it.id } ?: defaultHabits
            } catch (e: Exception) {
                defaultHabits
            }
            val habitIds = enabledHabits.ifEmpty { defaultHabits }

            smartReminderRepository.getSmartReminderData().collect { data ->
                val settings = if (data != null) {
                    habitIds.map { habitId ->
                        data.habitReminders[habitId] ?: HabitReminderSettings(habitId = habitId)
                    }
                } else {
                    habitIds.map { habitId -> HabitReminderSettings(habitId = habitId) }
                }

                _uiState.value = SmartRemindersUiState(
                    isGlobalEnabled = data?.isEnabled ?: true,
                    habitSettings = settings,
                    isLoading = false
                )
            }
        }
    }

    fun toggleGlobalReminders() {
        viewModelScope.launch {
            smartReminderRepository.toggleGlobalReminders(!_uiState.value.isGlobalEnabled)
        }
    }

    fun toggleHabitReminder(habitId: String) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.habitSettings.find { it.habitId == habitId }
            smartReminderRepository.toggleHabitReminder(habitId, !(currentSettings?.isEnabled ?: true))
        }
    }

    fun setPreferredTime(habitId: String, time: String?) {
        viewModelScope.launch {
            smartReminderRepository.setPreferredTime(habitId, time)
        }
    }

    fun setReminderTone(habitId: String, tone: ReminderTone) {
        viewModelScope.launch {
            smartReminderRepository.setReminderTone(habitId, tone)
        }
    }

    fun setReminderFrequency(habitId: String, frequency: ReminderFrequency) {
        viewModelScope.launch {
            smartReminderRepository.setReminderFrequency(habitId, frequency)
        }
    }
}
