package com.dailywell.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.dailywell.app.billing.BillingManager
import com.dailywell.app.billing.PurchaseState
import com.dailywell.app.core.theme.DailyWellTheme
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.ui.navigation.MainNavigation
import com.dailywell.app.ui.screens.paywall.PaywallScreen
import com.dailywell.app.ui.screens.paywall.PaywallViewModel
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

// Debug flag for testing premium features without billing
private const val DEBUG_ENABLE_PREMIUM = true

class MainActivity : ComponentActivity() {

    private val billingManager: BillingManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                DailyWellApp(
                    activity = this,
                    billingManager = billingManager
                )
            }
        }
    }
}

@Composable
fun DailyWellApp(
    activity: ComponentActivity,
    billingManager: BillingManager
) {
    val settingsRepository: SettingsRepository = koinInject()
    var settings by remember { mutableStateOf(UserSettings()) }
    var showPaywall by remember { mutableStateOf(false) }

    // Collect settings
    LaunchedEffect(Unit) {
        settingsRepository.getSettings().collect {
            settings = it
        }
    }

    // Debug: Enable premium for testing
    LaunchedEffect(Unit) {
        if (DEBUG_ENABLE_PREMIUM && !settings.isPremium) {
            settingsRepository.setPremium(true)
        }
    }

    // Sync premium status from billing
    LaunchedEffect(Unit) {
        billingManager.isPremium.collect { isPremium ->
            if (isPremium && !settings.isPremium) {
                settingsRepository.setPremium(true)
            }
        }
    }

    DailyWellTheme {
        if (showPaywall) {
            PaywallWithBilling(
                activity = activity,
                billingManager = billingManager,
                onDismiss = { showPaywall = false },
                onPurchaseSuccess = { showPaywall = false }
            )
        } else {
            MainNavigation(
                hasCompletedOnboarding = settings.hasCompletedOnboarding,
                isPremium = settings.isPremium,
                onOnboardingComplete = { },
                onNavigateToSettings = { },
                onNavigateToPaywall = { showPaywall = true }
            )
        }
    }
}

@Composable
private fun PaywallWithBilling(
    activity: ComponentActivity,
    billingManager: BillingManager,
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit
) {
    val viewModel: PaywallViewModel = koinViewModel()
    val purchaseState by billingManager.purchaseState.collectAsState()
    val products by billingManager.products.collectAsState()

    // Update prices from billing
    LaunchedEffect(products) {
        val monthly = products.find { it.productId == BillingManager.PRODUCT_MONTHLY }
        val annual = products.find { it.productId == BillingManager.PRODUCT_ANNUAL }
        val lifetime = products.find { it.productId == BillingManager.PRODUCT_LIFETIME }

        val monthlyPrice = monthly?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$4.99"
        val annualPrice = annual?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "$29.99"
        val lifetimePrice = lifetime?.oneTimePurchaseOfferDetails?.formattedPrice ?: "$79.99"

        viewModel.updatePrices(monthlyPrice, annualPrice, lifetimePrice)
    }

    // Handle purchase state changes
    LaunchedEffect(purchaseState) {
        when (purchaseState) {
            is PurchaseState.Success -> {
                viewModel.onPurchaseSuccess()
                billingManager.resetPurchaseState()
            }
            is PurchaseState.Error -> {
                viewModel.onPurchaseError((purchaseState as PurchaseState.Error).message)
                billingManager.resetPurchaseState()
            }
            is PurchaseState.Cancelled -> {
                viewModel.onPurchaseCancelled()
                billingManager.resetPurchaseState()
            }
            is PurchaseState.NothingToRestore -> {
                viewModel.onRestoreNothingToRestore()
                billingManager.resetPurchaseState()
            }
            is PurchaseState.Loading -> {
                // Already handled in ViewModel
            }
            is PurchaseState.Idle -> {
                // Nothing to do
            }
        }
    }

    PaywallScreen(
        onDismiss = onDismiss,
        onPurchaseSuccess = onPurchaseSuccess,
        onPurchaseProduct = { productId ->
            billingManager.launchPurchaseFlow(activity, productId)
        },
        onRestorePurchases = {
            billingManager.restorePurchases()
        },
        viewModel = viewModel
    )
}
