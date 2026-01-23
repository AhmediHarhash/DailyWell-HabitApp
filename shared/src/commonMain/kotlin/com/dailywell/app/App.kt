package com.dailywell.app

import androidx.compose.runtime.*
import com.dailywell.app.core.theme.DailyWellTheme
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.ui.navigation.MainNavigation
import org.koin.compose.koinInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun App() {
    val settingsRepository: SettingsRepository = koinInject()
    var settings by remember { mutableStateOf(UserSettings()) }
    var showPaywall by remember { mutableStateOf(false) }

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
    val isOnTrial = settings.isTrialActive(currentDate)
    val trialDaysLeft = settings.trialDaysRemaining(currentDate)

    DailyWellTheme {
        if (showPaywall) {
            PaywallScreen(
                onDismiss = { showPaywall = false },
                onPurchaseSuccess = { showPaywall = false }
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
    }
}

@Composable
private fun PaywallScreen(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit
) {
    com.dailywell.app.ui.screens.paywall.PaywallScreen(
        onDismiss = onDismiss,
        onPurchaseSuccess = onPurchaseSuccess,
        onPurchaseProduct = { productId ->
            // Platform-specific purchase handling will be done by the Android app
            // This is a placeholder - the actual billing flow is handled externally
        },
        onRestorePurchases = {
            // Platform-specific restore handling will be done by the Android app
        }
    )
}
