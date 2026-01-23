package com.dailywell.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.domain.model.HabitType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 7, // Added First Win page
    val selectedHabitIds: Set<String> = emptySet(),
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val isCompleting: Boolean = false,
    val allHabits: List<HabitType> = HabitType.entries.toList(),
    val maxFreeHabits: Int = 3,
    // First Win state
    val firstWinCompleted: Boolean = false,
    val firstWinCelebrating: Boolean = false
)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextPage() {
        val current = _uiState.value.currentPage
        if (current < _uiState.value.totalPages - 1) {
            _uiState.update { it.copy(currentPage = current + 1) }
        }
    }

    fun previousPage() {
        val current = _uiState.value.currentPage
        if (current > 0) {
            _uiState.update { it.copy(currentPage = current - 1) }
        }
    }

    fun goToPage(page: Int) {
        if (page in 0 until _uiState.value.totalPages) {
            _uiState.update { it.copy(currentPage = page) }
        }
    }

    fun toggleHabit(habitId: String) {
        val current = _uiState.value.selectedHabitIds
        val newSelection = if (current.contains(habitId)) {
            current - habitId
        } else {
            // Only allow up to maxFreeHabits
            if (current.size < _uiState.value.maxFreeHabits) {
                current + habitId
            } else {
                current
            }
        }
        _uiState.update { it.copy(selectedHabitIds = newSelection) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
    }

    fun canProceedFromHabitSelection(): Boolean {
        return _uiState.value.selectedHabitIds.isNotEmpty()
    }

    /**
     * First Win - Complete a quick habit during onboarding
     * This creates immediate dopamine hit and psychological commitment
     */
    fun completeFirstWin() {
        viewModelScope.launch {
            _uiState.update { it.copy(firstWinCelebrating = true) }
            // Simulate celebration animation
            kotlinx.coroutines.delay(1500)
            _uiState.update { it.copy(firstWinCompleted = true, firstWinCelebrating = false) }
        }
    }

    fun canProceedFromFirstWin(): Boolean {
        return _uiState.value.firstWinCompleted
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCompleting = true) }

            try {
                // Initialize habits
                habitRepository.initializeDefaultHabits()

                // Enable selected habits
                _uiState.value.selectedHabitIds.forEach { habitId ->
                    settingsRepository.enableHabit(habitId)
                }

                // Set reminder time
                settingsRepository.setReminderTime(
                    _uiState.value.reminderHour,
                    _uiState.value.reminderMinute
                )

                /**
                 * AUTO-START 14-DAY FREE TRIAL
                 * Give new users FULL premium access immediately.
                 * No restrictions, no locked features - everything unlocked.
                 * This maximizes engagement and conversion.
                 */
                settingsRepository.startFreeTrial()

                // Mark onboarding complete
                settingsRepository.setOnboardingComplete()

                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(isCompleting = false) }
            }
        }
    }
}
