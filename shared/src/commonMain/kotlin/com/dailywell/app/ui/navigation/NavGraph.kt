package com.dailywell.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.core.theme.PremiumMotionTokens
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import com.dailywell.app.data.repository.AchievementRepository
import com.dailywell.app.data.repository.RecoveryRepository
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.ui.components.GlassNavBar
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.screens.today.TodayScreen
import com.dailywell.app.ui.screens.insights.InsightsTabScreen
import com.dailywell.app.ui.screens.track.TrackTabScreen
import com.dailywell.app.ui.screens.you.YouScreen
import com.dailywell.app.ui.screens.you.AIUsageDetailsScreen
import com.dailywell.app.ui.screens.you.AIObservabilityScreen
import com.dailywell.app.ui.screens.insights.InsightsScreen
import com.dailywell.app.ui.screens.settings.SettingsScreen
import com.dailywell.app.ui.screens.onboarding.OnboardingScreen
import com.dailywell.app.ui.screens.achievements.AchievementsScreen
import com.dailywell.app.ui.screens.stacking.HabitStackingScreen
import com.dailywell.app.ui.screens.intentions.IntentionsScreen
import com.dailywell.app.ui.screens.reminders.SmartRemindersScreen
import com.dailywell.app.ui.screens.recovery.RecoveryScreen
import com.dailywell.app.ui.screens.insights.AIInsightsScreen
import com.dailywell.app.ui.screens.audio.AudioCoachingScreen
import com.dailywell.app.ui.screens.biometric.BiometricScreen
import com.dailywell.app.ui.screens.coaching.AICoachingScreen
import com.dailywell.app.ui.screens.customhabit.CustomHabitScreen
import com.dailywell.app.ui.screens.gamification.GamificationScreen
import com.dailywell.app.ui.screens.biometric.BiometricViewModel
import com.dailywell.app.ui.screens.coaching.AICoachingViewModel
import com.dailywell.app.ui.screens.notifications.ProactiveNotificationSettingsScreen
import com.dailywell.app.ui.screens.calendar.CalendarIntegrationScreen
import com.dailywell.app.ui.screens.calendar.CalendarViewModel
import com.dailywell.app.ui.screens.atrisk.AtRiskScreen
import com.dailywell.app.ui.screens.atrisk.AtRiskViewModel
import com.dailywell.app.ui.screens.healthconnect.HealthConnectScreen
import com.dailywell.app.ui.screens.healthconnect.HealthConnectViewModel
import com.dailywell.app.ui.screens.reflections.ReflectionsScreen
import com.dailywell.app.ui.screens.reflections.ReflectionsViewModel
import com.dailywell.app.ui.screens.water.WaterTrackingScreen
import com.dailywell.app.ui.screens.water.WaterTrackingViewModel
import com.dailywell.app.ui.screens.nutrition.FoodScannerScreen
import com.dailywell.app.ui.screens.nutrition.NutritionScreen
import com.dailywell.app.ui.screens.scan.FoodScanViewModel
import com.dailywell.app.ui.screens.workout.WorkoutLogScreen
import com.dailywell.app.ui.screens.workout.WorkoutHistoryScreen
import com.dailywell.app.ui.screens.body.BodyMetricsScreen
import com.dailywell.app.ui.screens.body.MeasurementsScreen
import com.dailywell.app.ui.screens.body.ProgressPhotosScreen
import com.dailywell.app.ui.screens.calendar.TrackerExportPayload
import com.dailywell.app.data.model.CalendarOAuthCallback
import com.dailywell.app.data.model.PhotoAngle
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.NutritionRepository
import com.dailywell.app.data.repository.WorkoutRepository
import com.dailywell.app.data.repository.BodyMetricsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

/**
 * Navigation state - single sealed class for all destinations
 */
sealed interface NavigationDestination {
    // Bottom nav tabs (new 5-tab structure)
    data object Today : NavigationDestination
    data object Insights : NavigationDestination
    data object Track : NavigationDestination
    data object Coach : NavigationDestination
    data object You : NavigationDestination

    // Feature screens (navigated from any tab)
    data class Feature(val screen: Screen) : NavigationDestination
}

@Composable
fun MainNavigation(
    hasCompletedOnboarding: Boolean,
    isPremium: Boolean,
    onOnboardingComplete: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onOpenHealthConnectSettings: () -> Unit = {},
    onInstallHealthConnect: () -> Unit = {},
    onTakeProgressPhoto: (PhotoAngle, (String) -> Unit) -> Unit = { _, _ -> },
    onShareStreak: (String, String) -> Unit = { _, _ -> },
    onShareCalendarTrackerImage: (TrackerExportPayload) -> Unit = {},
    onExportCalendarTrackerPdf: (TrackerExportPayload) -> Unit = {},
    hasCompletedAuth: Boolean = true,
    authSkipped: Boolean = false,
    onAuthComplete: () -> Unit = {},
    onSignIn: (suspend (String, String) -> Result<Unit>)? = null,
    onSignUp: (suspend (String, String, String) -> Result<Unit>)? = null,
    onGoogleSignIn: (suspend () -> Result<Unit>)? = null,
    onForgotPassword: (suspend (String) -> Result<Unit>)? = null,
    onSignOut: (() -> Unit)? = null,
    onDeleteAccount: (suspend () -> Result<Unit>)? = null,
    onChangePassword: (suspend (String, String) -> Result<Unit>)? = null,
    onUpdateDisplayName: (suspend (String) -> Result<Unit>)? = null,
    isSignedIn: Boolean = false,
    accountEmail: String? = null,
    accountDisplayName: String? = null,
    isEmailVerified: Boolean = false,
    authProvider: String = "none",
    pendingDeepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
    pendingCalendarOAuth: CalendarOAuthCallback? = null,
    onCalendarOAuthConsumed: () -> Unit = {}
) {
    var currentDestination by remember { mutableStateOf<NavigationDestination>(NavigationDestination.Today) }
    var currentTab by remember { mutableStateOf<Screen>(Screen.Today) }

    val achievementRepository: AchievementRepository = koinInject()
    val recoveryRepository: RecoveryRepository = koinInject()
    val navSettingsRepository: SettingsRepository = koinInject()
    val currentUserId by navSettingsRepository.getSettings()
        .map { it.firebaseUid ?: "anonymous" }
        .collectAsState(initial = "anonymous")
    val unlockedAchievementIds by achievementRepository.getUnlockedAchievementIds()
        .collectAsState(initial = emptySet())
    val recoveryState by recoveryRepository.getRecoveryState()
        .collectAsState(initial = null)

    val navigateToFeature: (Screen) -> Unit = { screen ->
        if (screen == Screen.Premium) {
            onNavigateToPaywall()
        } else {
            currentDestination = NavigationDestination.Feature(screen)
        }
    }

    val navigateBack: () -> Unit = {
        currentDestination = when (currentTab) {
            Screen.Today -> NavigationDestination.Today
            Screen.Insights -> NavigationDestination.Insights
            Screen.Track -> NavigationDestination.Track
            Screen.Coach -> NavigationDestination.Coach
            Screen.You -> NavigationDestination.You
            else -> NavigationDestination.Today
        }
    }

    val navigateToTab: (Screen) -> Unit = { screen ->
        currentTab = screen
        currentDestination = when (screen) {
            Screen.Today -> NavigationDestination.Today
            Screen.Insights -> NavigationDestination.Insights
            Screen.Track -> NavigationDestination.Track
            Screen.Coach -> NavigationDestination.Coach
            Screen.You -> NavigationDestination.You
            else -> NavigationDestination.Today
        }
    }

    val handleDeepLink: (String) -> Unit = { deepLink ->
        when (parseNotificationDeepLink(deepLink)) {
            NotificationDeepLinkTarget.TODAY -> navigateToTab(Screen.Today)
            NotificationDeepLinkTarget.INSIGHTS -> navigateToTab(Screen.Insights)
            NotificationDeepLinkTarget.COACH -> navigateToTab(Screen.Coach)
            NotificationDeepLinkTarget.TRACK -> navigateToTab(Screen.Track)
            NotificationDeepLinkTarget.YOU -> navigateToTab(Screen.You)
            NotificationDeepLinkTarget.ACHIEVEMENTS -> {
                currentTab = Screen.You
                currentDestination = NavigationDestination.Feature(Screen.Milestones)
            }
            NotificationDeepLinkTarget.UNKNOWN -> Unit
        }
    }

    LaunchedEffect(pendingDeepLink, hasCompletedOnboarding) {
        if (!hasCompletedOnboarding) return@LaunchedEffect
        val link = pendingDeepLink?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        handleDeepLink(link)
        onDeepLinkConsumed()
    }

    // Show onboarding if not completed
    if (!hasCompletedOnboarding) {
        OnboardingScreen(onComplete = onOnboardingComplete)
        return
    }

    // Auth is optional after onboarding; do not block dashboard.

    // Show recovery flow if user is in recovery mode
    if (recoveryState?.isInRecovery == true && currentDestination !is NavigationDestination.Feature) {
        RecoveryScreen(
            onComplete = { /* Stay on current screen */ },
            onDismiss = { /* Continue */ }
        )
        return
    }

    // Track whether we're in a feature screen for transitions
    val isInFeature = currentDestination is NavigationDestination.Feature

    // Animated feature overlay - slides up over tab content
    AnimatedContent(
        targetState = currentDestination,
        transitionSpec = {
            val entering = targetState
            val leaving = initialState

            if (entering is NavigationDestination.Feature && leaving !is NavigationDestination.Feature) {
                // Tab -> Feature: slide up + fade in
                (slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight / 5 },
                    animationSpec = tween(
                        PremiumMotionTokens.featureTransitionEnterMs,
                        easing = EaseOutCubic
                    )
                ) + fadeIn(
                    tween(
                        PremiumMotionTokens.featureTransitionCrossfadeInMs,
                        easing = EaseOutCubic
                    )
                )) togetherWith
                    fadeOut(
                        tween(
                            PremiumMotionTokens.featureTransitionExitMs,
                            easing = EaseInCubic
                        )
                    )
            } else if (entering !is NavigationDestination.Feature && leaving is NavigationDestination.Feature) {
                // Feature -> Tab: fade in tab + slide down feature
                fadeIn(
                    tween(
                        PremiumMotionTokens.featureTransitionCrossfadeInMs,
                        PremiumMotionTokens.featureTransitionDelayMs,
                        easing = EaseOutCubic
                    )
                ) togetherWith
                    (slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight / 5 },
                        animationSpec = tween(
                            PremiumMotionTokens.tabTransitionEnterMs,
                            easing = EaseInCubic
                        )
                    ) + fadeOut(
                        tween(
                            PremiumMotionTokens.featureTransitionExitMs,
                            easing = EaseInCubic
                        )
                    ))
            } else if (entering is NavigationDestination.Feature && leaving is NavigationDestination.Feature) {
                // Feature -> Feature: crossfade
                fadeIn(
                    tween(
                        PremiumMotionTokens.featureTransitionCrossfadeInMs,
                        easing = EaseOutCubic
                    )
                ) togetherWith
                    fadeOut(
                        tween(
                            PremiumMotionTokens.featureTransitionCrossfadeExitMs,
                            easing = EaseInCubic
                        )
                    )
            } else {
                // Tab -> Tab: smooth crossfade
                fadeIn(
                    tween(
                        PremiumMotionTokens.tabTransitionEnterMs,
                        easing = EaseOutCubic
                    )
                ) togetherWith
                    fadeOut(
                        tween(
                            PremiumMotionTokens.tabTransitionExitMs,
                            easing = EaseInCubic
                        )
                    )
            }
        },
        label = "mainNavTransition"
    ) { destination ->
        when (destination) {
            is NavigationDestination.Feature -> {
                RenderFeatureScreen(
                    screen = destination.screen,
                    isPremium = isPremium,
                    unlockedAchievementIds = unlockedAchievementIds,
                    onBack = navigateBack,
                    onNavigateToPaywall = onNavigateToPaywall,
                    onOpenHealthConnectSettings = onOpenHealthConnectSettings,
                    onInstallHealthConnect = onInstallHealthConnect,
                    onTakeProgressPhoto = onTakeProgressPhoto,
                    onNavigateToFeature = navigateToFeature,
                    onShareStreak = onShareStreak,
                    onShareCalendarTrackerImage = onShareCalendarTrackerImage,
                    onExportCalendarTrackerPdf = onExportCalendarTrackerPdf,
                    currentUserId = currentUserId,
                    onSignIn = onSignIn,
                    onSignUp = onSignUp,
                    onGoogleSignIn = onGoogleSignIn,
                    onForgotPassword = onForgotPassword,
                    onSignOut = onSignOut,
                    onDeleteAccount = onDeleteAccount,
                    onChangePassword = onChangePassword,
                    onUpdateDisplayName = onUpdateDisplayName,
                    onAuthComplete = onAuthComplete,
                    isSignedIn = isSignedIn,
                    accountEmail = accountEmail,
                    accountDisplayName = accountDisplayName,
                    isEmailVerified = isEmailVerified,
                    authProvider = authProvider,
                    pendingCalendarOAuth = pendingCalendarOAuth,
                    onCalendarOAuthConsumed = onCalendarOAuthConsumed
                )
            }
            else -> {
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            currentTab = currentTab,
                            onTabSelected = navigateToTab,
                            onCenterFabClick = { navigateToFeature(Screen.FoodScanning) }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Tab content - uses crossfade within tabs
                        AnimatedContent(
                            targetState = destination,
                            transitionSpec = {
                                fadeIn(
                                    tween(
                                        PremiumMotionTokens.tabTransitionEnterMs,
                                        easing = EaseOutCubic
                                    )
                                ) togetherWith
                                    fadeOut(
                                        tween(
                                            PremiumMotionTokens.tabTransitionExitMs,
                                            easing = EaseInCubic
                                        )
                                    )
                            },
                            label = "tabTransition"
                        ) { dest ->
                            when (dest) {
                                is NavigationDestination.Today -> TodayScreen(
                                    onSettingsClick = { navigateToFeature(Screen.Settings) },
                                    onUpgradeClick = onNavigateToPaywall,
                                    onShareStreak = onShareStreak,
                                    onNavigateToWater = { navigateToFeature(Screen.WaterTracking) },
                                    onNavigateToScan = { navigateToFeature(Screen.FoodScanning) },
                                    onNavigateToInsights = { navigateToTab(Screen.Insights) },
                                    onNavigateToAICoach = { navigateToFeature(Screen.AICoaching) },
                                    onNavigateToWorkout = { navigateToFeature(Screen.WorkoutLog) },
                                    onNavigateToCustomHabit = { navigateToFeature(Screen.CustomHabit) }
                                )
                                is NavigationDestination.Insights -> InsightsTabScreen(
                                    isPremium = isPremium,
                                    onNavigateToFeature = navigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                                is NavigationDestination.Track -> TrackTabScreen(
                                    isPremium = isPremium,
                                    onNavigateToFeature = navigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                                is NavigationDestination.Coach -> {
                                    val aiCoachingViewModel: AICoachingViewModel = koinInject()
                                    AICoachingScreen(
                                        viewModel = aiCoachingViewModel,
                                        onBack = { navigateToTab(Screen.Today) }
                                    )
                                }
                                is NavigationDestination.You -> YouScreen(
                                    isPremium = isPremium,
                                    onNavigateToSettings = { navigateToFeature(Screen.Settings) },
                                    onNavigateToMilestones = { navigateToFeature(Screen.Milestones) },
                                    onNavigateToPaywall = onNavigateToPaywall,
                                    onNavigateToUsageDetails = { navigateToFeature(Screen.AIUsageDetails) },
                                    onNavigateToCustomHabit = { navigateToFeature(Screen.CustomHabit) }
                                )
                                else -> TodayScreen(
                                    onSettingsClick = { navigateToFeature(Screen.Settings) },
                                    onUpgradeClick = onNavigateToPaywall,
                                    onShareStreak = onShareStreak
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderFeatureScreen(
    screen: Screen,
    isPremium: Boolean,
    unlockedAchievementIds: Set<String>,
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onOpenHealthConnectSettings: () -> Unit = {},
    onInstallHealthConnect: () -> Unit = {},
    onTakeProgressPhoto: (PhotoAngle, (String) -> Unit) -> Unit = { _, _ -> },
    onNavigateToFeature: (Screen) -> Unit,
    onShareStreak: (String, String) -> Unit = { _, _ -> },
    onShareCalendarTrackerImage: (TrackerExportPayload) -> Unit = {},
    onExportCalendarTrackerPdf: (TrackerExportPayload) -> Unit = {},
    currentUserId: String = "anonymous",
    onSignIn: (suspend (String, String) -> Result<Unit>)? = null,
    onSignUp: (suspend (String, String, String) -> Result<Unit>)? = null,
    onGoogleSignIn: (suspend () -> Result<Unit>)? = null,
    onForgotPassword: (suspend (String) -> Result<Unit>)? = null,
    onSignOut: (() -> Unit)? = null,
    onDeleteAccount: (suspend () -> Result<Unit>)? = null,
    onChangePassword: (suspend (String, String) -> Result<Unit>)? = null,
    onUpdateDisplayName: (suspend (String) -> Result<Unit>)? = null,
    onAuthComplete: () -> Unit = {},
    isSignedIn: Boolean = false,
    accountEmail: String? = null,
    accountDisplayName: String? = null,
    isEmailVerified: Boolean = false,
    authProvider: String = "none",
    pendingCalendarOAuth: CalendarOAuthCallback? = null,
    onCalendarOAuthConsumed: () -> Unit = {}
) {
    when (screen) {
        Screen.Settings -> SettingsScreen(
            onBack = onBack,
            onNavigateToPaywall = onNavigateToPaywall,
            onNavigateToHabitStacking = if (isPremium) {{ onNavigateToFeature(Screen.HabitStacking) }} else null,
            onNavigateToIntentions = if (isPremium) {{ onNavigateToFeature(Screen.Intentions) }} else null,
            onNavigateToSmartReminders = if (isPremium) {{ onNavigateToFeature(Screen.SmartReminders) }} else null,
            onNavigateToAIUsageDetails = if (isPremium) {{ onNavigateToFeature(Screen.AIUsageDetails) }} else null,
            onNavigateToAIInsights = if (isPremium) {{ onNavigateToFeature(Screen.AIInsights) }} else null,
            onNavigateToAudioCoaching = if (isPremium) {{ onNavigateToFeature(Screen.AudioCoaching) }} else null,
            onNavigateToBiometric = if (isPremium) {{ onNavigateToFeature(Screen.Biometric) }} else null,
            onNavigateToAICoaching = if (isPremium) {{ onNavigateToFeature(Screen.AICoaching) }} else null,
            onNavigateToGamification = if (isPremium) {{ onNavigateToFeature(Screen.Gamification) }} else null,
            onNavigateToSmartNotifications = { onNavigateToFeature(Screen.SmartNotifications) },
            onNavigateToCalendarIntegration = if (isPremium) {{ onNavigateToFeature(Screen.Calendar) }} else null,
            onNavigateToAtRiskAnalysis = if (isPremium) {{ onNavigateToFeature(Screen.AtRisk) }} else null,
            // Account management
            onNavigateToAuth = { onNavigateToFeature(Screen.Auth) },
            onSignOut = onSignOut,
            onDeleteAccount = onDeleteAccount,
            onChangePassword = onChangePassword,
            onUpdateDisplayName = onUpdateDisplayName,
            isSignedIn = isSignedIn,
            accountEmail = accountEmail,
            accountDisplayName = accountDisplayName,
            isEmailVerified = isEmailVerified,
            authProvider = authProvider,
            isPremiumOverride = isPremium
        )

        // Auth screen (from Settings)
        Screen.Auth -> {
            if (onSignIn != null && onSignUp != null) {
                com.dailywell.app.ui.screens.auth.AuthScreen(
                    onSignIn = onSignIn,
                    onSignUp = onSignUp,
                    onGoogleSignIn = onGoogleSignIn,
                    onForgotPassword = onForgotPassword,
                    onComplete = {
                        onAuthComplete()
                        onBack()
                    },
                    isFromSettings = true,
                    onBack = onBack
                )
            }
        }

        Screen.Milestones -> AchievementsScreen(
            onBack = onBack,
            onShareAchievement = onShareStreak
        )

        // Backward-compatible alias to the habit settings section
        Screen.MyHabits -> {
            LaunchedEffect(Unit) { onNavigateToFeature(Screen.Settings) }
        }

        // Build Better Habits
        Screen.Onboarding -> OnboardingScreen(onComplete = onBack)
        Screen.CustomHabit -> {
            val habitRepository: HabitRepository = koinInject()
            val customHabits by habitRepository.getCustomHabits().collectAsState(initial = emptyList())
            val scope = rememberCoroutineScope()

            CustomHabitScreen(
                onDismiss = onBack,
                onSave = { name, emoji, threshold, question ->
                    scope.launch {
                        habitRepository.createCustomHabit(name, emoji, threshold, question)
                        onBack()
                    }
                },
                existingCustomCount = customHabits.size,
                isPremium = isPremium
            )
        }
        Screen.HabitStacking -> HabitStackingScreen(onBack = onBack)
        Screen.Intentions -> IntentionsScreen(onBack = onBack)
        Screen.SmartReminders -> SmartRemindersScreen(onBack = onBack)
        Screen.Recovery -> RecoveryScreen(onComplete = onBack, onDismiss = onBack)

        // Analytics
        Screen.Patterns -> InsightsScreen(
            onUpgradeClick = onNavigateToPaywall,
            onNavigateToAchievements = { onNavigateToFeature(Screen.Milestones) }
        )
        Screen.AIInsights -> AIInsightsScreen(onBack = onBack)
        Screen.AIUsageDetails -> AIUsageDetailsScreen(
            onBack = onBack,
            onOpenObservability = { onNavigateToFeature(Screen.AIObservability) }
        )
        Screen.AIObservability -> AIObservabilityScreen(onBack = onBack)
        Screen.Calendar -> {
            val calendarViewModel: CalendarViewModel = koinInject()
            CalendarIntegrationScreen(
                viewModel = calendarViewModel,
                onNavigateBack = onBack,
                pendingOAuthCallback = pendingCalendarOAuth,
                onOAuthCallbackConsumed = onCalendarOAuthConsumed,
                onShareTrackerImage = onShareCalendarTrackerImage,
                onExportTrackerPdf = onExportCalendarTrackerPdf
            )
        }
        Screen.AtRisk -> {
            val atRiskViewModel: AtRiskViewModel = koinInject()
            AtRiskScreen(
                viewModel = atRiskViewModel,
                onNavigateBack = onBack
            )
        }
        Screen.Reflections -> {
            val reflectionsViewModel: ReflectionsViewModel = koinInject()
            ReflectionsScreen(
                viewModel = reflectionsViewModel,
                onNavigateBack = onBack
            )
        }

        // AI & Coaching
        Screen.AICoaching -> {
            val aiCoachingViewModel: AICoachingViewModel = koinInject()
            AICoachingScreen(
                viewModel = aiCoachingViewModel,
                onBack = onBack
            )
        }
        Screen.AudioCoaching -> AudioCoachingScreen(
            onBack = onBack,
            isPremium = isPremium
        )
        Screen.SmartNotifications -> ProactiveNotificationSettingsScreen(
            onNavigateBack = onBack
        )

        // Rewards
        Screen.Gamification -> GamificationScreen(onBack = onBack)
        Screen.Biometric -> {
            val biometricViewModel: BiometricViewModel = koinInject()
            BiometricScreen(
                viewModel = biometricViewModel,
                onBack = onBack
            )
        }

        // Health Tracking
        Screen.WaterTracking -> {
            val waterViewModel: WaterTrackingViewModel = koinInject()
            WaterTrackingScreen(
                viewModel = waterViewModel,
                onNavigateBack = onBack
            )
        }
        Screen.HealthConnect -> {
            val healthConnectViewModel: HealthConnectViewModel = koinInject()
            val connectionState by healthConnectViewModel.uiState.collectAsState()

            HealthConnectScreen(
                connectionState = connectionState,
                onRequestPermissions = { healthConnectViewModel.requestPermissions() },
                onOpenHealthConnect = onOpenHealthConnectSettings,
                onInstallHealthConnect = onInstallHealthConnect,
                onSyncNow = { healthConnectViewModel.syncNow() },
                onBack = onBack
            )
        }

        // Nutrition & Fitness
        Screen.FoodScanning -> {
            val nutritionRepository: NutritionRepository = koinInject()
            val aiCoachingRepository: com.dailywell.app.data.repository.AICoachingRepository = koinInject()
            val foodScanViewModel: FoodScanViewModel = koinInject()
            FoodScannerScreen(
                userId = currentUserId,
                nutritionRepository = nutritionRepository,
                aiCoachingRepository = aiCoachingRepository,
                foodScanViewModel = foodScanViewModel,
                onBack = onBack,
                onMealLogged = onBack
            )
        }
        Screen.Nutrition -> {
            val nutritionRepository: NutritionRepository = koinInject()
            NutritionScreen(
                userId = currentUserId,
                nutritionRepository = nutritionRepository,
                onScanFood = { onNavigateToFeature(Screen.FoodScanning) }
            )
        }
        Screen.WorkoutLog -> {
            val workoutRepository: WorkoutRepository = koinInject()
            WorkoutLogScreen(
                userId = currentUserId,
                workoutRepository = workoutRepository,
                onBack = onBack,
                onViewHistory = { onNavigateToFeature(Screen.WorkoutHistory) }
            )
        }
        Screen.WorkoutHistory -> {
            val workoutRepository: WorkoutRepository = koinInject()
            WorkoutHistoryScreen(
                userId = currentUserId,
                workoutRepository = workoutRepository,
                onBack = onBack,
                onViewWorkout = { /* Workout details screen not implemented yet */ }
            )
        }
        Screen.BodyMetrics -> {
            val bodyMetricsRepository: BodyMetricsRepository = koinInject()
            BodyMetricsScreen(
                userId = currentUserId,
                bodyMetricsRepository = bodyMetricsRepository,
                onNavigateToMeasurements = { onNavigateToFeature(Screen.Measurements) },
                onNavigateToPhotos = { onNavigateToFeature(Screen.ProgressPhotos) },
                onBack = onBack
            )
        }
        Screen.Measurements -> {
            val bodyMetricsRepository: BodyMetricsRepository = koinInject()
            MeasurementsScreen(
                userId = currentUserId,
                bodyMetricsRepository = bodyMetricsRepository,
                onBack = onBack
            )
        }
        Screen.ProgressPhotos -> {
            val bodyMetricsRepository: BodyMetricsRepository = koinInject()
            ProgressPhotosScreen(
                userId = currentUserId,
                bodyMetricsRepository = bodyMetricsRepository,
                onBack = onBack,
                onTakePhoto = onTakeProgressPhoto
            )
        }

        Screen.Premium -> { /* Handled by onNavigateToPaywall */ }

        // Fallback
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Screen not found")
            }
        }
    }
}

/**
 * 2026 Frosted Glass Bottom Navigation
 *
 * Design: Floating pill with frosted glass effect,
 * Material Icons, animated selection indicator.
 * Inspired by: iOS 18, Arc Browser, modern wellness apps
 */
@Composable
private fun BottomNavigationBar(
    currentTab: Screen,
    onTabSelected: (Screen) -> Unit,
    onCenterFabClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassNavBar {
            bottomNavItems.forEachIndexed { index, item ->
                if (index == 2) {
                    // Center item - raised Scan FAB
                    CenterTrackFAB(
                        onClick = onCenterFabClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    val isSelected = currentTab == item.screen
                    FloatingNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onTabSelected(item.screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterTrackFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabScale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .offset(y = (-10).dp)
                .size(56.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DailyWellIcons.Health.FoodScan,
                contentDescription = "Scan",
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
        }
        Text(
            text = "Scan",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun FloatingNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated scale on selection
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navScale"
    )

    // Animated indicator background
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(
            PremiumMotionTokens.navIndicatorDurationMs,
            easing = EaseOutCubic
        ),
        label = "indicatorAlpha"
    )

    // Animated tint color
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        },
        animationSpec = tween(PremiumMotionTokens.navTintDurationMs),
        label = "tintColor"
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with animated indicator pill behind it
        Box(contentAlignment = Alignment.Center) {
            // Selection indicator pill
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 28.dp)
                    .graphicsLayer { alpha = indicatorAlpha }
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    )
            )

            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                modifier = Modifier
                    .size(22.dp)
                    .padding(vertical = 2.dp),
                tint = tintColor
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Label
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = tintColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

