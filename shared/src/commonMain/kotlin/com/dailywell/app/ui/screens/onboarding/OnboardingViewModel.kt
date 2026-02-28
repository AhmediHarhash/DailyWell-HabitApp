package com.dailywell.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.repository.AICoachingRepository
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.domain.model.GoalHabitMapping
import com.dailywell.app.domain.model.HabitRecommendation
import com.dailywell.app.domain.model.HabitType
import com.dailywell.app.domain.model.OnboardingGoal
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3, // 3-step fast path: Welcome, Goal, Habits -> Dashboard
    val selectedHabitIds: Set<String> = emptySet(),
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val isCompleting: Boolean = false,
    val allHabits: List<HabitType> = HabitType.entries.toList(),
    val maxFreeHabits: Int = 12,
    // Goal-based onboarding state
    val selectedGoal: OnboardingGoal? = null,
    val assessmentScore: Int = 3, // 1-5 scale, default middle
    val recommendations: List<HabitRecommendation> = emptyList()
)

class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val habitRepository: HabitRepository,
    private val aiCoachingRepository: AICoachingRepository? = null
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

    /** Select a goal and generate recommendations */
    fun selectGoal(goal: OnboardingGoal) {
        val recommendations = GoalHabitMapping.getRecommendations(goal)
        val primaryIds = GoalHabitMapping.getPrimaryHabitIds(goal)
        _uiState.update {
            it.copy(
                selectedGoal = goal,
                recommendations = recommendations,
                selectedHabitIds = primaryIds // Auto-select primary habits
            )
        }
    }

    fun setAssessmentScore(score: Int) {
        _uiState.update { it.copy(assessmentScore = score.coerceIn(1, 5)) }
    }

    fun toggleHabit(habitId: String) {
        val current = _uiState.value.selectedHabitIds
        val newSelection = if (current.contains(habitId)) {
            current - habitId
        } else {
            // Allow up to maxFreeHabits
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

                // Save onboarding personalization data
                val currentSettings = settingsRepository.getSettings().first()
                settingsRepository.updateSettings(
                    currentSettings.copy(
                        onboardingGoal = _uiState.value.selectedGoal?.id,
                        assessmentScore = _uiState.value.assessmentScore
                    )
                )

                /**
                 * AUTO-START 14-DAY FREE TRIAL
                 * Give new users FULL premium access immediately.
                 * No restrictions, no locked features - everything unlocked.
                 * This maximizes engagement and conversion.
                 */
                settingsRepository.startFreeTrial()

                /**
                 * FEATURE: Schedule proactive insights (Insight Scheduler)
                 * Schedules Day 3, 7, 14, 30, 90 milestone insights for retention
                 */
                try {
                    val uid = settingsRepository.getSettingsSnapshot().firebaseUid ?: "anonymous"
                    aiCoachingRepository?.scheduleNewUserInsights(uid)
                } catch (e: Exception) {
                    // Insight scheduling is optional - silently fail
                }

                // Mark onboarding complete
                settingsRepository.setOnboardingComplete()

                onComplete()
            } catch (e: Exception) {
                _uiState.update { it.copy(isCompleting = false) }
            }
        }
    }
}
