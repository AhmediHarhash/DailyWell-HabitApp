package com.dailywell.app.ui.screens.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.content.ReflectionPromptsDatabase.ReflectionPrompt
import com.dailywell.app.data.content.ReflectionPromptsDatabase.ReflectionTheme
import com.dailywell.app.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class WeeklyReflectionUiState(
    val weekNumber: Int = 0,
    val year: Int = 0,
    val currentPromptIndex: Int = 0,
    val prompts: List<ReflectionPromptWithResponse> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isComplete: Boolean = false,
    val reflectionStreak: Int = 0,
    val stats: ReflectionStats? = null,
    val error: String? = null,
    val showCompletionDialog: Boolean = false
)

data class ReflectionPromptWithResponse(
    val prompt: ReflectionPrompt,
    val themeInfo: ThemeInfo,
    val response: String = "",
    val isAnswered: Boolean = false
)

class WeeklyReflectionViewModel(
    private val reflectionRepository: ReflectionPromptsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyReflectionUiState())
    val uiState: StateFlow<WeeklyReflectionUiState> = _uiState.asStateFlow()

    init {
        loadCurrentWeekPrompts()
        loadStats()
    }

    private fun loadCurrentWeekPrompts() {
        viewModelScope.launch {
            try {
                reflectionRepository.getCurrentWeekPromptSet()
                    .collect { promptSet ->
                        val promptsWithResponses = promptSet.prompts.map { prompt ->
                            val existingResponse = promptSet.existingResponses[prompt.id] ?: ""
                            val themeInfo = reflectionRepository.getThemeInfo(prompt.theme)

                            ReflectionPromptWithResponse(
                                prompt = prompt,
                                themeInfo = themeInfo,
                                response = existingResponse,
                                isAnswered = existingResponse.isNotBlank()
                            )
                        }

                        _uiState.update {
                            it.copy(
                                weekNumber = promptSet.weekNumber,
                                year = promptSet.year,
                                prompts = promptsWithResponses,
                                isComplete = promptSet.isCompleted,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = reflectionRepository.getReflectionStats()
                val streak = reflectionRepository.getReflectionStreak()

                _uiState.update {
                    it.copy(
                        stats = stats,
                        reflectionStreak = streak
                    )
                }
            } catch (e: Exception) {
                // Stats loading is non-critical
            }
        }
    }

    fun navigateToPrompt(index: Int) {
        if (index in 0 until _uiState.value.prompts.size) {
            _uiState.update { it.copy(currentPromptIndex = index) }
        }
    }

    fun nextPrompt() {
        val current = _uiState.value.currentPromptIndex
        val max = _uiState.value.prompts.size - 1
        if (current < max) {
            _uiState.update { it.copy(currentPromptIndex = current + 1) }
        }
    }

    fun previousPrompt() {
        val current = _uiState.value.currentPromptIndex
        if (current > 0) {
            _uiState.update { it.copy(currentPromptIndex = current - 1) }
        }
    }

    fun updateResponse(response: String) {
        val state = _uiState.value
        val currentIndex = state.currentPromptIndex

        if (currentIndex < state.prompts.size) {
            val updatedPrompts = state.prompts.toMutableList()
            updatedPrompts[currentIndex] = updatedPrompts[currentIndex].copy(
                response = response,
                isAnswered = response.isNotBlank()
            )

            _uiState.update { it.copy(prompts = updatedPrompts) }
        }
    }

    fun saveCurrentResponse() {
        val state = _uiState.value
        val currentPromptWithResponse = state.prompts.getOrNull(state.currentPromptIndex) ?: return

        if (currentPromptWithResponse.response.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val result = reflectionRepository.saveResponse(
                promptId = currentPromptWithResponse.prompt.id,
                response = currentPromptWithResponse.response,
                weekNumber = state.weekNumber,
                year = state.year
            )

            result.onSuccess {
                // Update local state
                val updatedPrompts = state.prompts.toMutableList()
                updatedPrompts[state.currentPromptIndex] = currentPromptWithResponse.copy(
                    isAnswered = true
                )

                _uiState.update {
                    it.copy(
                        prompts = updatedPrompts,
                        isSaving = false
                    )
                }

                // Check if all are answered
                if (updatedPrompts.all { it.isAnswered }) {
                    _uiState.update { it.copy(showCompletionDialog = true) }
                }
            }

            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = error.message
                    )
                }
            }
        }
    }

    fun completeReflection() {
        viewModelScope.launch {
            val state = _uiState.value

            // Save all unsaved responses first
            state.prompts.forEachIndexed { index, promptWithResponse ->
                if (promptWithResponse.response.isNotBlank()) {
                    reflectionRepository.saveResponse(
                        promptId = promptWithResponse.prompt.id,
                        response = promptWithResponse.response,
                        weekNumber = state.weekNumber,
                        year = state.year
                    )
                }
            }

            // Mark as complete
            val result = reflectionRepository.completeWeeklyReflection(
                weekNumber = state.weekNumber,
                year = state.year
            )

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isComplete = true,
                        showCompletionDialog = false
                    )
                }
                loadStats() // Refresh stats after completion
            }

            result.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }

    fun dismissCompletionDialog() {
        _uiState.update { it.copy(showCompletionDialog = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getCompletionProgress(): Float {
        val prompts = _uiState.value.prompts
        if (prompts.isEmpty()) return 0f
        return prompts.count { it.isAnswered }.toFloat() / prompts.size
    }

    fun getCurrentThemeEmoji(): String {
        val currentIndex = _uiState.value.currentPromptIndex
        return _uiState.value.prompts.getOrNull(currentIndex)?.themeInfo?.emoji ?: "💭"
    }

    fun getCurrentThemeName(): String {
        val currentIndex = _uiState.value.currentPromptIndex
        return _uiState.value.prompts.getOrNull(currentIndex)?.themeInfo?.displayName ?: "Reflection"
    }
}
