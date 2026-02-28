package com.dailywell.app.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.RewardItem
import com.dailywell.app.data.repository.RewardRepository
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RewardStoreUiState(
    val availableRewards: List<RewardItem> = emptyList(),
    val coinBalance: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val redemptionSuccess: Boolean = false
)

class RewardStoreViewModel(
    private val rewardRepository: RewardRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RewardStoreUiState())
    val uiState: StateFlow<RewardStoreUiState> = _uiState.asStateFlow()

    private suspend fun getUserId(): String =
        settingsRepository.getSettingsSnapshot().firebaseUid ?: "anonymous"

    init {
        loadRewards()
        loadCoinBalance()
    }

    private fun loadRewards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            rewardRepository.getAvailableRewards()
                .onSuccess { rewards ->
                    _uiState.update {
                        it.copy(
                            availableRewards = rewards,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load rewards: ${error.message}"
                        )
                    }
                }
        }
    }

    private fun loadCoinBalance() {
        viewModelScope.launch {
            val userId = getUserId()

            // Observe coin balance
            rewardRepository.observeCoinBalance(userId)
                .collect { balance ->
                    _uiState.update { it.copy(coinBalance = balance.totalCoins) }
                }
        }
    }

    fun redeemItem(itemId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val userId = getUserId()

            rewardRepository.redeemReward(userId, itemId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            redemptionSuccess = true,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to redeem reward"
                        )
                    }
                }
        }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(redemptionSuccess = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
