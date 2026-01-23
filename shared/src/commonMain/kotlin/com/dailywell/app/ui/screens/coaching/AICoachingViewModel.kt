package com.dailywell.app.ui.screens.coaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.AICoachingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AICoachingUiState(
    val selectedCoach: CoachPersona = CoachPersonas.supportiveSam,
    val availableCoaches: List<CoachPersona> = CoachPersonas.allCoaches,
    val dailyInsight: DailyCoachingInsight? = null,
    val activeSession: AICoachingSession? = null,
    val sessionHistory: List<AICoachingSession> = emptyList(),
    val actionItems: List<CoachingActionItem> = emptyList(),
    val weeklySummary: WeeklyCoachingSummary? = null,
    val isLoading: Boolean = true,
    val showCoachSelector: Boolean = false,
    val showSessionTypeSelector: Boolean = false,
    val currentMessage: String = "",
    val error: String? = null
)

/**
 * ViewModel for Advanced AI Coaching features
 */
class AICoachingViewModel(
    private val coachingRepository: AICoachingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AICoachingUiState())
    val uiState: StateFlow<AICoachingUiState> = _uiState.asStateFlow()

    init {
        loadCoachingData()
    }

    private fun loadCoachingData() {
        viewModelScope.launch {
            coachingRepository.getSelectedCoach().collect { coach ->
                _uiState.value = _uiState.value.copy(
                    selectedCoach = coach,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            coachingRepository.getDailyInsight().collect { insight ->
                _uiState.value = _uiState.value.copy(dailyInsight = insight)
            }
        }

        viewModelScope.launch {
            coachingRepository.getActiveSessions().collect { sessions ->
                _uiState.value = _uiState.value.copy(
                    activeSession = sessions.firstOrNull()
                )
            }
        }

        viewModelScope.launch {
            coachingRepository.getSessionHistory().collect { history ->
                _uiState.value = _uiState.value.copy(sessionHistory = history)
            }
        }

        viewModelScope.launch {
            coachingRepository.getActionItems().collect { items ->
                _uiState.value = _uiState.value.copy(actionItems = items.filter { !it.isCompleted })
            }
        }

        viewModelScope.launch {
            coachingRepository.getWeeklySummary().collect { summary ->
                _uiState.value = _uiState.value.copy(weeklySummary = summary)
            }
        }
    }

    // Coach selection
    fun showCoachSelector(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCoachSelector = show)
    }

    fun selectCoach(coachId: String) {
        viewModelScope.launch {
            coachingRepository.selectCoach(coachId)
            _uiState.value = _uiState.value.copy(showCoachSelector = false)
        }
    }

    // Sessions
    fun showSessionTypeSelector(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSessionTypeSelector = show)
    }

    fun startSession(type: CoachingSessionType) {
        viewModelScope.launch {
            try {
                val session = coachingRepository.startSession(type)
                _uiState.value = _uiState.value.copy(
                    activeSession = session,
                    showSessionTypeSelector = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun updateCurrentMessage(message: String) {
        _uiState.value = _uiState.value.copy(currentMessage = message)
    }

    fun sendMessage() {
        val message = _uiState.value.currentMessage.trim()
        val sessionId = _uiState.value.activeSession?.id ?: return

        if (message.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(currentMessage = "")
                coachingRepository.sendMessage(sessionId, message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun selectQuickReply(reply: String) {
        val sessionId = _uiState.value.activeSession?.id ?: return

        viewModelScope.launch {
            try {
                coachingRepository.selectQuickReply(sessionId, reply)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun endSession() {
        val sessionId = _uiState.value.activeSession?.id ?: return

        viewModelScope.launch {
            coachingRepository.completeSession(sessionId)
            _uiState.value = _uiState.value.copy(activeSession = null)
        }
    }

    // Action items
    fun completeActionItem(itemId: String) {
        viewModelScope.launch {
            coachingRepository.completeActionItem(itemId)
        }
    }

    fun dismissActionItem(itemId: String) {
        viewModelScope.launch {
            coachingRepository.dismissActionItem(itemId)
        }
    }

    // Suggested actions
    fun completeSuggestedAction(actionId: String) {
        viewModelScope.launch {
            coachingRepository.markSuggestedActionDone(actionId)
        }
    }

    // Quick actions
    fun getMotivationBoost() {
        viewModelScope.launch {
            startSession(CoachingSessionType.MOTIVATION_BOOST)
        }
    }

    fun startDailyCheckin() {
        startSession(CoachingSessionType.DAILY_CHECKIN)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
