package com.dailywell.app.ui.screens.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.RecoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecoveryUiState(
    val isInRecovery: Boolean = false,
    val currentPhase: RecoveryPhase = RecoveryPhase.ACKNOWLEDGE,
    val previousStreak: Int = 0,
    val selectedReason: StreakBreakReason? = null,
    val reflectionAnswer: String = "",
    val commitmentLevel: CommitmentLevel = CommitmentLevel.SAME,
    val isLoading: Boolean = true
)

/**
 * ViewModel for Recovery Protocol flow
 */
class RecoveryViewModel(
    private val recoveryRepository: RecoveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecoveryUiState())
    val uiState: StateFlow<RecoveryUiState> = _uiState.asStateFlow()

    init {
        loadRecoveryState()
    }

    private fun loadRecoveryState() {
        viewModelScope.launch {
            recoveryRepository.getRecoveryState().collect { state ->
                if (state != null) {
                    _uiState.value = RecoveryUiState(
                        isInRecovery = state.isInRecovery,
                        currentPhase = state.recoveryPhase,
                        previousStreak = state.previousStreak,
                        selectedReason = state.selectedReason,
                        reflectionAnswer = state.reflectionAnswer ?: "",
                        commitmentLevel = state.commitmentLevel,
                        isLoading = false
                    )
                } else {
                    _uiState.value = RecoveryUiState(isLoading = false)
                }
            }
        }
    }

    fun startRecovery(previousStreak: Int) {
        viewModelScope.launch {
            recoveryRepository.startRecovery(previousStreak)
        }
    }

    fun selectReason(reason: StreakBreakReason) {
        viewModelScope.launch {
            recoveryRepository.setStreakBreakReason(reason)
            _uiState.value = _uiState.value.copy(selectedReason = reason)
        }
    }

    fun setReflection(answer: String) {
        _uiState.value = _uiState.value.copy(reflectionAnswer = answer)
        viewModelScope.launch {
            recoveryRepository.setReflectionAnswer(answer)
        }
    }

    fun setCommitmentLevel(level: CommitmentLevel) {
        viewModelScope.launch {
            recoveryRepository.setCommitmentLevel(level)
            _uiState.value = _uiState.value.copy(commitmentLevel = level)
        }
    }

    fun canAdvance(): Boolean {
        return when (_uiState.value.currentPhase) {
            RecoveryPhase.ACKNOWLEDGE -> _uiState.value.selectedReason != null
            RecoveryPhase.REFLECT -> true // Reflection is optional
            RecoveryPhase.RECOMMIT -> true
            RecoveryPhase.CELEBRATE -> true
            RecoveryPhase.NONE -> false
        }
    }

    fun advancePhase() {
        val nextPhase = when (_uiState.value.currentPhase) {
            RecoveryPhase.ACKNOWLEDGE -> RecoveryPhase.REFLECT
            RecoveryPhase.REFLECT -> RecoveryPhase.RECOMMIT
            RecoveryPhase.RECOMMIT -> RecoveryPhase.CELEBRATE
            RecoveryPhase.CELEBRATE -> RecoveryPhase.NONE
            RecoveryPhase.NONE -> RecoveryPhase.NONE
        }

        viewModelScope.launch {
            recoveryRepository.advanceToPhase(nextPhase)
        }
    }

    fun goBack() {
        val prevPhase = when (_uiState.value.currentPhase) {
            RecoveryPhase.REFLECT -> RecoveryPhase.ACKNOWLEDGE
            RecoveryPhase.RECOMMIT -> RecoveryPhase.REFLECT
            RecoveryPhase.CELEBRATE -> RecoveryPhase.RECOMMIT
            else -> _uiState.value.currentPhase
        }

        viewModelScope.launch {
            recoveryRepository.advanceToPhase(prevPhase)
        }
    }

    fun completeRecovery() {
        viewModelScope.launch {
            recoveryRepository.completeRecovery()
        }
    }

    fun cancelRecovery() {
        viewModelScope.launch {
            recoveryRepository.cancelRecovery()
        }
    }
}
