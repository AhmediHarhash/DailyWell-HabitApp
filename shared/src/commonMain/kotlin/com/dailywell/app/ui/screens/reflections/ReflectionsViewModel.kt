package com.dailywell.app.ui.screens.reflections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.content.ReflectionPromptsDatabase.ReflectionPrompt
import com.dailywell.app.data.repository.ReflectionPromptsRepository
import com.dailywell.app.data.repository.ReflectionStats
import com.dailywell.app.data.repository.ThemeInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PromptCardState(
    val prompt: ReflectionPrompt,
    val themeInfo: ThemeInfo,
    val response: String = "",
    val isAnswered: Boolean = false
)

data class ReflectionsUiState(
    val weekNumber: Int = 0,
    val year: Int = 0,
    val promptCards: List<PromptCardState> = emptyList(),
    val expandedPromptId: String? = null,
    val reflectionStats: ReflectionStats? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isWeekComplete: Boolean = false,
    val error: String? = null
)

class ReflectionsViewModel(
    private val reflectionRepository: ReflectionPromptsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReflectionsUiState())
    val uiState: StateFlow<ReflectionsUiState> = _uiState.asStateFlow()

    init {
        loadPrompts()
        loadStats()
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            try {
                reflectionRepository.getCurrentWeekPromptSet().collect { promptSet ->
                    val cards = promptSet.prompts.map { prompt ->
                        val existingResponse = promptSet.existingResponses[prompt.id] ?: ""
                        val themeInfo = reflectionRepository.getThemeInfo(prompt.theme)
                        PromptCardState(
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
                            promptCards = cards,
                            isWeekComplete = promptSet.isCompleted,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = reflectionRepository.getReflectionStats()
                _uiState.update { it.copy(reflectionStats = stats) }
            } catch (_: Exception) {
                // Stats are non-critical
            }
        }
    }

    fun toggleExpand(promptId: String) {
        val currentExpanded = _uiState.value.expandedPromptId
        if (currentExpanded != null && currentExpanded != promptId) {
            // Auto-save the previously expanded prompt before switching
            autoSave(currentExpanded)
        }
        _uiState.update {
            it.copy(expandedPromptId = if (currentExpanded == promptId) null else promptId)
        }
        // If collapsing, auto-save
        if (currentExpanded == promptId) {
            autoSave(promptId)
        }
    }

    fun updateResponse(promptId: String, text: String) {
        _uiState.update { state ->
            val updatedCards = state.promptCards.map { card ->
                if (card.prompt.id == promptId) {
                    card.copy(response = text, isAnswered = text.isNotBlank())
                } else card
            }
            state.copy(promptCards = updatedCards)
        }
    }

    private fun autoSave(promptId: String) {
        val state = _uiState.value
        val card = state.promptCards.find { it.prompt.id == promptId } ?: return
        if (card.response.isBlank()) return

        viewModelScope.launch {
            reflectionRepository.saveResponse(
                promptId = promptId,
                response = card.response,
                weekNumber = state.weekNumber,
                year = state.year
            )
        }
    }

    fun saveAll() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            state.promptCards.filter { it.response.isNotBlank() }.forEach { card ->
                reflectionRepository.saveResponse(
                    promptId = card.prompt.id,
                    response = card.response,
                    weekNumber = state.weekNumber,
                    year = state.year
                )
            }
            if (state.promptCards.all { it.isAnswered }) {
                reflectionRepository.completeWeeklyReflection(state.weekNumber, state.year)
                _uiState.update { it.copy(isWeekComplete = true) }
            }
            _uiState.update { it.copy(isSaving = false) }
            loadStats()
        }
    }
}
