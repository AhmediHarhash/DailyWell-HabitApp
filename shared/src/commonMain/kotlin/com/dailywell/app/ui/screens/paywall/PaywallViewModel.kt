package com.dailywell.app.ui.screens.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.AIPlanType
import com.dailywell.app.data.repository.AICoachingRepository
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PaywallUiState(
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val selectedProductId: String = PRODUCT_ANNUAL, // Default to annual (best conversion)
    val monthlyPrice: String = "$9.99",
    val annualPrice: String = "$79.99",
    val errorMessage: String? = null,
    val purchaseSuccess: Boolean = false,
    val isTrialActive: Boolean = false,
    val trialDaysRemaining: Int = 0
) {
    // Monthly equivalents for display
    val annualMonthlyEquivalent: String get() = "$6.67"   // $79.99 / 12

    // Savings percentages (vs monthly)
    val annualSavingsPercent: Int get() = 33  // $9.99 * 12 = $119.88, save ~$40 (33%)
}

// Product IDs for Google Play Billing
const val PRODUCT_MONTHLY = "dailywell_premium_monthly"
const val PRODUCT_ANNUAL = "dailywell_premium_annual"

class PaywallViewModel(
    private val settingsRepository: SettingsRepository,
    private val aiCoachingRepository: AICoachingRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    fun selectProduct(productId: String) {
        _uiState.update { it.copy(selectedProductId = productId) }
    }

    fun getSelectedProductId(): String = _uiState.value.selectedProductId

    fun setPurchasing(isPurchasing: Boolean) {
        _uiState.update { it.copy(isPurchasing = isPurchasing, errorMessage = null) }
    }

    fun onPurchaseSuccess() {
        viewModelScope.launch {
            settingsRepository.setPremium(true)
            syncPlanTypeWithSelectedProduct()
            _uiState.update { it.copy(isPurchasing = false, purchaseSuccess = true) }
        }
    }

    fun onPurchaseError(message: String) {
        _uiState.update { it.copy(isPurchasing = false, errorMessage = message) }
    }

    fun onPurchaseCancelled() {
        _uiState.update { it.copy(isPurchasing = false) }
    }

    fun setRestoring(isRestoring: Boolean) {
        _uiState.update { it.copy(isLoading = isRestoring, errorMessage = null) }
    }

    fun onRestoreSuccess() {
        viewModelScope.launch {
            settingsRepository.setPremium(true)
            _uiState.update { it.copy(isLoading = false, purchaseSuccess = true) }
        }
    }

    fun onRestoreError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    fun onRestoreNothingToRestore() {
        _uiState.update { it.copy(isLoading = false, errorMessage = "No purchases to restore") }
    }

    fun updatePrices(
        monthlyPrice: String,
        annualPrice: String
    ) {
        _uiState.update {
            it.copy(
                monthlyPrice = monthlyPrice,
                annualPrice = annualPrice
            )
        }
    }

    fun setTrialStatus(isActive: Boolean, daysRemaining: Int) {
        _uiState.update {
            it.copy(
                isTrialActive = isActive,
                trialDaysRemaining = daysRemaining
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun syncPlanTypeWithSelectedProduct() {
        val selected = _uiState.value.selectedProductId
        val planType = when (selected) {
            PRODUCT_ANNUAL -> AIPlanType.ANNUAL
            PRODUCT_MONTHLY -> AIPlanType.MONTHLY
            else -> AIPlanType.MONTHLY
        }
        aiCoachingRepository?.updatePlanType(planType)
    }
}
