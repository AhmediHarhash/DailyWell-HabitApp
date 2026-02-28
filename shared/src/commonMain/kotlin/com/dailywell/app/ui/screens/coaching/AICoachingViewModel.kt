package com.dailywell.app.ui.screens.coaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.ai.SLMDownloadInfo
import com.dailywell.app.ai.SLMDownloadProgress
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.AICoachingRepository
import com.dailywell.app.speech.SpeechRecognitionService
import com.dailywell.app.speech.SpeechRecognitionState
import com.dailywell.app.speech.SpeechSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class AICoachingUiState(
    val selectedCoach: CoachPersona = CoachPersonas.supportiveSam,
    val dailyInsight: DailyCoachingInsight? = null,
    val activeSession: AICoachingSession? = null,
    val sessionHistory: List<AICoachingSession> = emptyList(),
    val actionItems: List<CoachingActionItem> = emptyList(),
    val weeklySummary: WeeklyCoachingSummary? = null,
    val isLoading: Boolean = true,
    val showSessionTypeSelector: Boolean = false,
    val currentMessage: String = "",
    val pendingUserMessage: CoachingMessage? = null,
    val isGeneratingReply: Boolean = false,
    val awaitingSessionId: String? = null,
    val awaitingBaseMessageCount: Int? = null,
    val error: String? = null,
    // Voice input state
    val voiceInputEnabled: Boolean = true,
    val needsMicrophonePermission: Boolean = false,
    // AI Usage/Credits state (for cost control)
    val aiUsage: UserAIUsage? = null,
    val aiCreditsPercent: Float = 100f,
    val canUseAI: Boolean = true,
    val aiLimitMessage: String? = null,
    val showUpgradePrompt: Boolean = false,
    val slmDownloadProgress: SLMDownloadProgress = SLMDownloadProgress.Dismissed
)

/**
 * ViewModel for Advanced AI Coaching features
 */
class AICoachingViewModel(
    private val coachingRepository: AICoachingRepository,
    private val speechService: SpeechRecognitionService? = null,
    private val slmDownloadInfo: SLMDownloadInfo? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(AICoachingUiState())
    val uiState: StateFlow<AICoachingUiState> = _uiState.asStateFlow()

    // Speech recognition state
    val speechState: StateFlow<SpeechRecognitionState> = speechService?.state
        ?: MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)

    val speechSettings: StateFlow<SpeechSettings> = speechService?.settings
        ?: MutableStateFlow(SpeechSettings())

    val isSpeaking: StateFlow<Boolean> = speechService?.isSpeaking
        ?: MutableStateFlow(false)

    val isVoiceAvailable: Boolean = speechService?.isAvailable ?: false

    init {
        loadCoachingData()
        observeSpeechResults()
        observeSLMDownload()
    }

    private fun observeSLMDownload() {
        val downloadInfo = slmDownloadInfo ?: return
        viewModelScope.launch {
            downloadInfo.downloadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(slmDownloadProgress = progress)
            }
        }
    }

    private fun observeSpeechResults() {
        if (speechService == null) return

        viewModelScope.launch {
            speechService.state.collect { state ->
                when (state) {
                    is SpeechRecognitionState.Result -> {
                        // Auto-populate the message field with recognized text
                        _uiState.value = _uiState.value.copy(currentMessage = state.text)

                        // Auto-send if settings allow
                        if (speechSettings.value.autoSendAfterSpeech) {
                            sendMessage()
                        }
                    }
                    is SpeechRecognitionState.PartialResult -> {
                        // Show partial result in input field
                        _uiState.value = _uiState.value.copy(currentMessage = state.text)
                    }
                    else -> { /* Other states handled by UI */ }
                }
            }
        }
    }

    private fun loadCoachingData() {
        _uiState.value = _uiState.value.copy(
            selectedCoach = CoachPersonas.supportiveSam,
            isLoading = false
        )

        viewModelScope.launch {
            coachingRepository.getDailyInsight().collect { insight ->
                _uiState.value = _uiState.value.copy(dailyInsight = insight)
            }
        }

        viewModelScope.launch {
            coachingRepository.getActiveSessions().collect { sessions ->
                val active = sessions.firstOrNull()
                val current = _uiState.value
                val replyPersisted = current.isGeneratingReply &&
                    current.awaitingSessionId != null &&
                    current.awaitingBaseMessageCount != null &&
                    active != null &&
                    active.id == current.awaitingSessionId &&
                    active.messages.size >= current.awaitingBaseMessageCount + 2

                _uiState.value = current.copy(
                    activeSession = active,
                    pendingUserMessage = if (replyPersisted) null else current.pendingUserMessage,
                    isGeneratingReply = if (replyPersisted) false else current.isGeneratingReply,
                    awaitingSessionId = if (replyPersisted) null else current.awaitingSessionId,
                    awaitingBaseMessageCount = if (replyPersisted) null else current.awaitingBaseMessageCount
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

        // Observe AI usage for credits display
        viewModelScope.launch {
            coachingRepository.getAIUsage().collect { usage ->
                _uiState.value = _uiState.value.copy(
                    aiUsage = usage,
                    aiCreditsPercent = usage.percentRemaining,
                    canUseAI = usage.hasCreditsRemaining
                )
            }
        }
    }

    // Check AI availability before sending message
    private suspend fun checkAILimits(): Boolean {
        val result = coachingRepository.checkAIAvailability()
        if (!result.canUseAI) {
            _uiState.value = _uiState.value.copy(
                canUseAI = false,
                aiLimitMessage = result.reason?.userMessage,
                showUpgradePrompt = result.upgradeMessage != null
            )
            return false
        }
        return true
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

    fun startNewChat() {
        viewModelScope.launch {
            try {
                _uiState.value.activeSession?.id?.let { coachingRepository.completeSession(it) }
                val session = coachingRepository.startSession(CoachingSessionType.HABIT_COACHING)
                _uiState.value = _uiState.value.copy(
                    activeSession = session,
                    currentMessage = "",
                    pendingUserMessage = null,
                    isGeneratingReply = false,
                    awaitingSessionId = null,
                    awaitingBaseMessageCount = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun onCoachScreenOpened() {
        viewModelScope.launch {
            // Give repository flows a brief moment to hydrate persisted sessions.
            delay(250)

            val state = _uiState.value
            if (state.activeSession != null) {
                attachPendingScanHandoff(state.activeSession.id)
                return@launch
            }

            val recentSession = state.sessionHistory.firstOrNull()
            if (recentSession != null) {
                val resumed = coachingRepository.resumeSession(recentSession.id)
                if (resumed != null) {
                    _uiState.value = _uiState.value.copy(
                        activeSession = resumed,
                        currentMessage = "",
                        pendingUserMessage = null,
                        isGeneratingReply = false,
                        awaitingSessionId = null,
                        awaitingBaseMessageCount = null
                    )
                    attachPendingScanHandoff(resumed.id)
                    return@launch
                }
            }

            try {
                val session = coachingRepository.startSession(CoachingSessionType.HABIT_COACHING)
                _uiState.value = _uiState.value.copy(
                    activeSession = session,
                    currentMessage = "",
                    pendingUserMessage = null,
                    isGeneratingReply = false,
                    awaitingSessionId = null,
                    awaitingBaseMessageCount = null
                )
                attachPendingScanHandoff(session.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private suspend fun attachPendingScanHandoff(sessionId: String) {
        val handoff = coachingRepository.consumePendingScanHandoff() ?: return
        coachingRepository.addScanContinuationMessage(sessionId, handoff)
    }

    fun resumeSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val session = coachingRepository.resumeSession(sessionId) ?: return@launch
                _uiState.value = _uiState.value.copy(
                    activeSession = session,
                    currentMessage = "",
                    pendingUserMessage = null,
                    isGeneratingReply = false,
                    awaitingSessionId = null,
                    awaitingBaseMessageCount = null
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
        val state = _uiState.value
        val message = state.currentMessage.trim()
        val activeSession = state.activeSession ?: return
        val sessionId = activeSession.id

        if (message.isBlank()) return

        viewModelScope.launch {
            try {
                val now = Clock.System.now().toString()
                val pendingUser = CoachingMessage(
                    id = "pending_user_${Clock.System.now().toEpochMilliseconds()}",
                    role = MessageRole.USER,
                    content = message,
                    timestamp = now
                )

                _uiState.value = _uiState.value.copy(
                    currentMessage = "",
                    pendingUserMessage = pendingUser,
                    isGeneratingReply = true,
                    awaitingSessionId = sessionId,
                    awaitingBaseMessageCount = activeSession.messages.size
                )

                coachingRepository.sendMessage(sessionId, message)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    currentMessage = message,
                    pendingUserMessage = null,
                    isGeneratingReply = false,
                    awaitingSessionId = null,
                    awaitingBaseMessageCount = null,
                    error = e.message
                )
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
            _uiState.value = _uiState.value.copy(
                activeSession = null,
                pendingUserMessage = null,
                isGeneratingReply = false,
                awaitingSessionId = null,
                awaitingBaseMessageCount = null
            )
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

    // Voice input methods
    fun startVoiceInput() {
        viewModelScope.launch {
            if (speechService == null) return@launch

            val hasPermission = speechService.checkPermission()
            if (!hasPermission) {
                _uiState.value = _uiState.value.copy(needsMicrophonePermission = true)
                return@launch
            }

            speechService.startListening()
        }
    }

    fun stopVoiceInput() {
        speechService?.stopListening()
    }

    fun cancelVoiceInput() {
        speechService?.cancelListening()
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(needsMicrophonePermission = false)
        if (granted) {
            startVoiceInput()
        }
    }

    fun speakResponse(text: String) {
        viewModelScope.launch {
            speechService?.speak(text)
        }
    }

    fun stopSpeaking() {
        speechService?.stopSpeaking()
    }

    fun updateVoiceSettings(settings: SpeechSettings) {
        viewModelScope.launch {
            speechService?.updateSettings(settings)
        }
    }

    // AI Credits/Usage methods
    fun dismissUpgradePrompt() {
        _uiState.value = _uiState.value.copy(showUpgradePrompt = false)
    }

    fun clearAILimitMessage() {
        _uiState.value = _uiState.value.copy(aiLimitMessage = null)
    }

    fun refreshAIUsage() {
        viewModelScope.launch {
            val result = coachingRepository.checkAIAvailability()
            _uiState.value = _uiState.value.copy(
                canUseAI = result.canUseAI,
                aiCreditsPercent = result.percentRemaining,
                aiLimitMessage = if (!result.canUseAI) result.reason?.userMessage else null
            )
        }
    }

    fun startSLMDownload() {
        slmDownloadInfo?.startDownload()
    }

    fun dismissSLMDownloadCard() {
        slmDownloadInfo?.dismissDownloadCard()
    }

    /**
     * Update plan type when user upgrades
     */
    fun onPlanUpgraded(planType: AIPlanType) {
        viewModelScope.launch {
            coachingRepository.updatePlanType(planType)
            _uiState.value = _uiState.value.copy(
                showUpgradePrompt = false,
                canUseAI = true,
                aiLimitMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechService?.release()
    }
}
