package com.dailywell.app.ui.screens.intentions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.ImplementationIntention
import com.dailywell.app.data.model.IntentionSituation
import com.dailywell.app.data.model.IntentionTemplates
import com.dailywell.app.data.repository.IntentionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class IntentionsUiState(
    val intentions: List<ImplementationIntention> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * ViewModel for Implementation Intentions feature
 * Based on Gollwitzer's "if-then" planning research
 */
class IntentionsViewModel(
    private val intentionRepository: IntentionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntentionsUiState())
    val uiState: StateFlow<IntentionsUiState> = _uiState.asStateFlow()

    init {
        loadIntentions()
    }

    private fun loadIntentions() {
        viewModelScope.launch {
            intentionRepository.getAllIntentions().collect { intentions ->
                _uiState.value = _uiState.value.copy(
                    intentions = intentions,
                    isLoading = false
                )
            }
        }
    }

    fun getIntentionsForHabit(habitId: String): List<ImplementationIntention> {
        return _uiState.value.intentions.filter { it.habitId == habitId }
    }

    fun addIntention(intention: ImplementationIntention) {
        viewModelScope.launch {
            intentionRepository.addIntention(intention)
        }
    }

    fun createIntention(
        habitId: String,
        situation: IntentionSituation,
        action: String,
        location: String? = null,
        time: String? = null,
        obstacle: String? = null,
        obstacleResponse: String? = null
    ) {
        viewModelScope.launch {
            val intention = ImplementationIntention(
                id = generateId(),
                habitId = habitId,
                situation = situation,
                action = action,
                location = location,
                time = time,
                obstacle = obstacle,
                obstacleResponse = obstacleResponse,
                isEnabled = true,
                createdAt = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            )
            intentionRepository.addIntention(intention)
        }
    }

    fun applyTemplate(template: IntentionTemplates.IntentionTemplate) {
        viewModelScope.launch {
            val intention = ImplementationIntention(
                id = generateId(),
                habitId = template.habitId,
                situation = template.situation,
                action = template.suggestedAction,
                location = template.suggestedLocation,
                time = template.suggestedTime,
                obstacle = template.commonObstacle,
                obstacleResponse = template.obstacleResponse,
                isEnabled = true,
                createdAt = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            )
            intentionRepository.addIntention(intention)
        }
    }

    fun toggleIntention(intentionId: String) {
        viewModelScope.launch {
            intentionRepository.toggleIntention(intentionId)
        }
    }

    fun deleteIntention(intentionId: String) {
        viewModelScope.launch {
            intentionRepository.deleteIntention(intentionId)
        }
    }

    fun recordIntentionTriggered(intentionId: String) {
        viewModelScope.launch {
            intentionRepository.recordIntentionTriggered(intentionId)
        }
    }

    private fun generateId(): String {
        return "intention_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}
