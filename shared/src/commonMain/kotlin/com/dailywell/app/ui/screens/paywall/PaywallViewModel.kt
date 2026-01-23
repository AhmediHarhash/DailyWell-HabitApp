package com.dailywell.app.ui.screens.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PaywallUiState(
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val selectedProductId: String = PRODUCT_ANNUAL, // Default to annual (best conversion)
    val monthlyPrice: String = "$4.99",
    val annualPrice: String = "$29.99",
    val lifetimePrice: String = "$79.99",
    val errorMessage: String? = null,
    val purchaseSuccess: Boolean = false,
    val isTrialActive: Boolean = false,
    val trialDaysRemaining: Int = 0
) {
    // Monthly equivalent for annual plan (for display)
    val annualMonthlyEquivalent: String get() = "$2.50"

    // Savings percentages
    val annualSavingsPercent: Int get() = 50 // $4.99 * 12 = $59.88, save ~$30
    val lifetimeSavingsPercent: Int get() = 87 // Based on 2-year comparison
}

// Product IDs for Google Play Billing
const val PRODUCT_MONTHLY = "dailywell_premium_monthly"
const val PRODUCT_ANNUAL = "dailywell_premium_annual"
const val PRODUCT_LIFETIME = "dailywell_premium_lifetime"

class PaywallViewModel(
    private val settingsRepository: SettingsRepository
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

    fun updatePrices(monthlyPrice: String, annualPrice: String, lifetimePrice: String) {
        _uiState.update {
            it.copy(
                monthlyPrice = monthlyPrice,
                annualPrice = annualPrice,
                lifetimePrice = lifetimePrice
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
}
