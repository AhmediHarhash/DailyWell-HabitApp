package com.dailywell.app.ui.screens.stacking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.HabitStackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class HabitStackingUiState(
    val habits: List<Habit> = emptyList(),
    val activeStacks: List<HabitStack> = emptyList(),
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null
)

class HabitStackingViewModel(
    private val habitRepository: HabitRepository,
    private val stackRepository: HabitStackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitStackingUiState())
    val uiState: StateFlow<HabitStackingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                habitRepository.getEnabledHabits(),
                stackRepository.getAllStacks()
            ) { habits, stacks ->
                HabitStackingUiState(
                    habits = habits,
                    activeStacks = stacks,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun createCustomStack(
        triggerHabitId: String,
        targetHabitId: String,
        triggerType: StackTriggerType
    ) {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            val stack = HabitStack(
                id = "stack_${System.currentTimeMillis()}",
                triggerHabitId = triggerHabitId,
                targetHabitId = targetHabitId,
                triggerType = triggerType,
                isEnabled = true,
                createdAt = today
            )
            stackRepository.addStack(stack)
            _uiState.update { it.copy(message = "Habit chain created!") }
        }
    }

    fun toggleStack(stackId: String) {
        viewModelScope.launch {
            stackRepository.toggleStack(stackId)
        }
    }

    fun deleteStack(stackId: String) {
        viewModelScope.launch {
            stackRepository.deleteStack(stackId)
        }
    }

    fun applyMorningRoutine() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            val enabledHabitIds = _uiState.value.habits.map { it.id }.toSet()

            HabitStackTemplates.morningRoutineStacks
                .filter { template ->
                    enabledHabitIds.contains(template.triggerHabitId) &&
                            enabledHabitIds.contains(template.targetHabitId)
                }
                .forEach { template ->
                    val stack = HabitStack(
                        id = "stack_morning_${template.triggerHabitId}_${template.targetHabitId}",
                        triggerHabitId = template.triggerHabitId,
                        targetHabitId = template.targetHabitId,
                        triggerType = template.triggerType,
                        isEnabled = true,
                        createdAt = today
                    )
                    stackRepository.addStack(stack)
                }

            _uiState.update { it.copy(message = "Morning routine applied!") }
        }
    }

    fun applyEveningRoutine() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            val enabledHabitIds = _uiState.value.habits.map { it.id }.toSet()

            HabitStackTemplates.eveningRoutineStacks
                .filter { template ->
                    enabledHabitIds.contains(template.triggerHabitId) &&
                            enabledHabitIds.contains(template.targetHabitId)
                }
                .forEach { template ->
                    val stack = HabitStack(
                        id = "stack_evening_${template.triggerHabitId}_${template.targetHabitId}",
                        triggerHabitId = template.triggerHabitId,
                        targetHabitId = template.targetHabitId,
                        triggerType = template.triggerType,
                        isEnabled = true,
                        createdAt = today
                    )
                    stackRepository.addStack(stack)
                }

            _uiState.update { it.copy(message = "Evening routine applied!") }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
