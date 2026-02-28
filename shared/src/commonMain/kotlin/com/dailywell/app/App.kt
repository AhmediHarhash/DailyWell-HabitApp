package com.dailywell.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import com.dailywell.app.core.theme.DailyWellTheme
import com.dailywell.app.data.model.ThemeMode
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.ui.navigation.MainNavigation
import org.koin.compose.koinInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun App(
    onPurchaseProduct: ((String) -> Unit)? = null,
    onRestorePurchases: (() -> Unit)? = null
) {
    val settingsRepository: SettingsRepository = koinInject()
    var settings by remember { mutableStateOf(UserSettings()) }
    var showPaywall by remember { mutableStateOf(false) }
    var billingUnavailableMessage by remember { mutableStateOf<String?>(null) }

    // Get current date for trial check
    val currentDate = remember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
    }

    LaunchedEffect(Unit) {
        settingsRepository.getSettings().collect {
            settings = it
        }
    }

    /**
     * CRITICAL: Use hasPremiumAccess() NOT isPremium
     * This ensures 14-day trial users get FULL access to ALL features:
     * - All 7 habits + custom habits
     * - AI coaching with all 5 personas
     * - Full audio library (15+ tracks)
     * - Habit stacking & implementation intentions
     * - Social features, family plan, leaderboards
     * - Biometric integration (Oura, WHOOP, etc.)
     * - Smart reminders, pattern insights
     * - Streak protection
     * - EVERYTHING unlocked. No restrictions.
     */
    val hasFullAccess = settings.hasPremiumAccess(currentDate)
    val systemDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDarkTheme
    }
    DailyWellTheme(darkTheme = useDarkTheme) {
        if (showPaywall) {
            PaywallScreen(
                onDismiss = { showPaywall = false },
                onPurchaseSuccess = { showPaywall = false },
                onPurchaseProduct = onPurchaseProduct,
                onRestorePurchases = onRestorePurchases,
                onBillingUnavailable = { message ->
                    billingUnavailableMessage = message
                }
            )
        } else {
            MainNavigation(
                hasCompletedOnboarding = settings.hasCompletedOnboarding,
                isPremium = hasFullAccess, // Trial users get FULL premium access
                onOnboardingComplete = {
                    // Settings will update automatically via Flow
                },
                onNavigateToSettings = { },
                onNavigateToPaywall = { showPaywall = true }
            )
        }

        if (billingUnavailableMessage != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { billingUnavailableMessage = null },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { billingUnavailableMessage = null }
                    ) {
                        androidx.compose.material3.Text("OK")
                    }
                },
                title = { androidx.compose.material3.Text("Purchases Unavailable") },
                text = { androidx.compose.material3.Text(billingUnavailableMessage ?: "") }
            )
        }
    }
}

@Composable
private fun PaywallScreen(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    onPurchaseProduct: ((String) -> Unit)?,
    onRestorePurchases: (() -> Unit)?,
    onBillingUnavailable: (String) -> Unit
) {
    com.dailywell.app.ui.screens.paywall.PaywallScreen(
        onDismiss = onDismiss,
        onPurchaseSuccess = onPurchaseSuccess,
        onPurchaseProduct = { productId ->
            val purchaseHandler = onPurchaseProduct
            if (purchaseHandler != null) {
                purchaseHandler(productId)
            } else {
                onBillingUnavailable("Purchases are not available in this build.")
            }
        },
        onRestorePurchases = {
            val restoreHandler = onRestorePurchases
            if (restoreHandler != null) {
                restoreHandler()
            } else {
                onBillingUnavailable("Restore is not available in this build.")
            }
        }
    )
}
