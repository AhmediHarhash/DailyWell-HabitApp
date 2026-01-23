package com.dailywell.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.repository.AchievementRepository
import com.dailywell.app.data.repository.RecoveryRepository
import com.dailywell.app.ui.screens.today.TodayScreen
import com.dailywell.app.ui.screens.week.WeekScreen
import com.dailywell.app.ui.screens.insights.InsightsScreen
import com.dailywell.app.ui.screens.settings.SettingsScreen
import com.dailywell.app.ui.screens.onboarding.OnboardingScreen
import com.dailywell.app.ui.screens.achievements.AchievementsScreen
import com.dailywell.app.ui.screens.stacking.HabitStackingScreen
import com.dailywell.app.ui.screens.intentions.IntentionsScreen
import com.dailywell.app.ui.screens.reminders.SmartRemindersScreen
import com.dailywell.app.ui.screens.recovery.RecoveryScreen
import com.dailywell.app.ui.screens.insights.AIInsightsScreen
import com.dailywell.app.ui.screens.social.SocialScreen
import com.dailywell.app.ui.screens.audio.AudioCoachingScreen
import com.dailywell.app.ui.screens.biometric.BiometricScreen
import com.dailywell.app.ui.screens.family.FamilyScreen
import com.dailywell.app.ui.screens.coaching.AICoachingScreen
import com.dailywell.app.ui.screens.gamification.GamificationScreen
import com.dailywell.app.ui.screens.challenges.ChallengeScreen
import com.dailywell.app.ui.screens.leaderboard.LeaderboardScreen
import com.dailywell.app.ui.screens.biometric.BiometricViewModel
import com.dailywell.app.ui.screens.family.FamilyViewModel
import com.dailywell.app.ui.screens.coaching.AICoachingViewModel
import com.dailywell.app.ui.screens.gamification.GamificationViewModel
import org.koin.compose.koinInject

@Composable
fun MainNavigation(
    hasCompletedOnboarding: Boolean,
    isPremium: Boolean,
    onOnboardingComplete: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Today) }
    var showMyHabits by remember { mutableStateOf(false) }
    var showMilestones by remember { mutableStateOf(false) }
    var showHabitStacking by remember { mutableStateOf(false) }
    var showIntentions by remember { mutableStateOf(false) }
    var showSmartReminders by remember { mutableStateOf(false) }
    var showRecovery by remember { mutableStateOf(false) }
    // Phase 3
    var showAIInsights by remember { mutableStateOf(false) }
    var showSocial by remember { mutableStateOf(false) }
    var showAudioCoaching by remember { mutableStateOf(false) }
    // Phase 4
    var showBiometric by remember { mutableStateOf(false) }
    var showFamily by remember { mutableStateOf(false) }
    var showAICoaching by remember { mutableStateOf(false) }
    // Phase 5
    var showGamification by remember { mutableStateOf(false) }
    var showChallenges by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }

    val achievementRepository: AchievementRepository = koinInject()
    val recoveryRepository: RecoveryRepository = koinInject()
    val unlockedAchievementIds by achievementRepository.getUnlockedAchievementIds()
        .collectAsState(initial = emptySet())
    val recoveryState by recoveryRepository.getRecoveryState()
        .collectAsState(initial = null)

    if (!hasCompletedOnboarding) {
        OnboardingScreen(
            onComplete = onOnboardingComplete
        )
        return
    }

    if (showMyHabits) {
        SettingsScreen(
            onBack = { showMyHabits = false },
            onNavigateToPaywall = onNavigateToPaywall,
            onNavigateToHabitStacking = if (isPremium) {{ showMyHabits = false; showHabitStacking = true }} else null,
            onNavigateToIntentions = if (isPremium) {{ showMyHabits = false; showIntentions = true }} else null,
            onNavigateToSmartReminders = if (isPremium) {{ showMyHabits = false; showSmartReminders = true }} else null,
            onNavigateToAIInsights = if (isPremium) {{ showMyHabits = false; showAIInsights = true }} else null,
            onNavigateToSocial = if (isPremium) {{ showMyHabits = false; showSocial = true }} else null,
            onNavigateToAudioCoaching = if (isPremium) {{ showMyHabits = false; showAudioCoaching = true }} else null,
            onNavigateToBiometric = if (isPremium) {{ showMyHabits = false; showBiometric = true }} else null,
            onNavigateToFamily = if (isPremium) {{ showMyHabits = false; showFamily = true }} else null,
            onNavigateToAICoaching = if (isPremium) {{ showMyHabits = false; showAICoaching = true }} else null,
            onNavigateToGamification = if (isPremium) {{ showMyHabits = false; showGamification = true }} else null,
            onNavigateToChallenges = if (isPremium) {{ showMyHabits = false; showChallenges = true }} else null,
            onNavigateToLeaderboard = if (isPremium) {{ showMyHabits = false; showLeaderboard = true }} else null
        )
        return
    }

    if (showMilestones) {
        AchievementsScreen(
            unlockedAchievementIds = unlockedAchievementIds,
            onBack = { showMilestones = false }
        )
        return
    }

    if (showHabitStacking) {
        HabitStackingScreen(
            onBack = { showHabitStacking = false }
        )
        return
    }

    if (showIntentions) {
        IntentionsScreen(
            onBack = { showIntentions = false }
        )
        return
    }

    if (showSmartReminders) {
        SmartRemindersScreen(
            onBack = { showSmartReminders = false }
        )
        return
    }

    // Phase 3 screens
    if (showAIInsights) {
        AIInsightsScreen(
            onBack = { showAIInsights = false }
        )
        return
    }

    if (showSocial) {
        SocialScreen(
            onBack = { showSocial = false }
        )
        return
    }

    if (showAudioCoaching) {
        AudioCoachingScreen(
            onBack = { showAudioCoaching = false },
            isPremium = isPremium
        )
        return
    }

    // Phase 4 screens
    if (showBiometric) {
        val biometricViewModel: BiometricViewModel = koinInject()
        BiometricScreen(
            viewModel = biometricViewModel,
            onBack = { showBiometric = false }
        )
        return
    }

    if (showFamily) {
        val familyViewModel: FamilyViewModel = koinInject()
        FamilyScreen(
            viewModel = familyViewModel,
            onBack = { showFamily = false }
        )
        return
    }

    if (showAICoaching) {
        val aiCoachingViewModel: AICoachingViewModel = koinInject()
        AICoachingScreen(
            viewModel = aiCoachingViewModel,
            onBack = { showAICoaching = false }
        )
        return
    }

    // Phase 5 screens
    if (showGamification) {
        GamificationScreen(
            onBack = { showGamification = false }
        )
        return
    }

    if (showChallenges) {
        ChallengeScreen(
            onBack = { showChallenges = false }
        )
        return
    }

    if (showLeaderboard) {
        LeaderboardScreen(
            onBack = { showLeaderboard = false }
        )
        return
    }

    // Show recovery flow if user is in recovery
    if (showRecovery || recoveryState?.isInRecovery == true) {
        RecoveryScreen(
            onComplete = { showRecovery = false },
            onDismiss = { showRecovery = false }
        )
        return
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentScreen,
                isPremium = isPremium,
                onScreenSelected = { screen ->
                    if (screen == Screen.Patterns && !isPremium) {
                        onNavigateToPaywall()
                    } else {
                        currentScreen = screen
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                }
            ) { screen ->
                when (screen) {
                    Screen.Today -> TodayScreen(
                        onSettingsClick = { showMyHabits = true }
                    )
                    Screen.Week -> WeekScreen()
                    Screen.Patterns -> InsightsScreen(
                        onUpgradeClick = onNavigateToPaywall,
                        onNavigateToAchievements = { showMilestones = true }
                    )
                    else -> TodayScreen(
                        onSettingsClick = { showMyHabits = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentScreen: Screen,
    isPremium: Boolean,
    onScreenSelected: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentScreen == item.screen
            val isLocked = item.isPremium && !isPremium

            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.icon,
                            fontSize = 20.sp
                        )
                        if (isLocked) {
                            Text(
                                text = "🔒",
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
