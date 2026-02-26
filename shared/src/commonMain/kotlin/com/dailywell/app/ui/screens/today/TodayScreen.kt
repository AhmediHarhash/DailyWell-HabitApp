package com.dailywell.app.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.ai.SLMDownloadProgress
import com.dailywell.app.data.model.ImplementationIntention
import com.dailywell.app.data.model.MoodLevel
import com.dailywell.app.data.model.ThemeMode
import com.dailywell.app.data.model.TodayViewMode
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.domain.model.TimeOfDay
import com.dailywell.app.ui.components.*
import kotlinx.datetime.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit = {},
    onShareStreak: (String, String) -> Unit = { _, _ -> },  // shareText, imageDescription
    onNavigateToWater: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToAICoach: () -> Unit = {},
    onNavigateToWorkout: () -> Unit = {},
    onNavigateToCustomHabit: () -> Unit = {},
    viewModel: TodayViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.getSettings().collectAsState(initial = UserSettings())
    val systemDarkTheme = isSystemInDarkTheme()
    val isDarkThemeEnabled = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDarkTheme
    }
    val uiScope = rememberCoroutineScope()

    // State for showing share dialog
    var showShareDialog by remember { mutableStateOf(false) }

    // State for confetti celebration
    var showConfetti by remember { mutableStateOf(false) }

    // Track when all habits are completed for confetti
    val allHabitsComplete = uiState.completedCount == uiState.totalCount && uiState.totalCount > 0
    LaunchedEffect(allHabitsComplete) {
        if (allHabitsComplete && !showConfetti) {
            showConfetti = true
        }
    }

    // Share streak dialog (user-initiated, always available regardless of overlay queue)
    if (showShareDialog && uiState.streakInfo.currentStreak > 0) {
        ShareStreakDialog(
            streak = uiState.streakInfo.currentStreak,
            completedToday = uiState.completedCount,
            onDismiss = { showShareDialog = false },
            onShare = { text, imageDesc ->
                onShareStreak(text, imageDesc)
            }
        )
    }

    // Single overlay dispatch â€” max 1 per session, priority-ordered by ViewModel
    when (uiState.activeOverlay) {
        TodayOverlay.TUTORIAL -> {
            FirstDayTutorialOverlay(
                onDismiss = {
                    viewModel.dismissOverlay()
                    viewModel.dismissTutorial()
                }
            )
        }
        TodayOverlay.MILESTONE -> if (uiState.currentMilestone != null) {
            StreakMilestoneDialog(
                milestone = uiState.currentMilestone!!,
                streak = uiState.streakInfo.currentStreak,
                onDismiss = {
                    viewModel.dismissOverlay()
                    viewModel.dismissMilestone()
                }
            )
        }
        TodayOverlay.CELEBRATION -> uiState.celebrationMessage?.let { message ->
            CelebrationDialog(
                message = message,
                completedCount = uiState.completedCount,
                totalCount = uiState.totalCount,
                onDismiss = {
                    viewModel.dismissOverlay()
                    viewModel.dismissCelebration()
                }
            )
        }
        TodayOverlay.HABIT_STACK_NUDGE -> if (uiState.suggestedNextHabit != null) {
            HabitStackNudgeDialog(
                triggerHabit = uiState.habits.find {
                    it.id == uiState.suggestedNextHabitStack?.triggerHabitId
                },
                targetHabit = uiState.suggestedNextHabit!!,
                stack = uiState.suggestedNextHabitStack,
                onComplete = { viewModel.completeStackedHabit() },
                onSkip = {
                    viewModel.dismissOverlay()
                    viewModel.dismissHabitStackNudge()
                }
            )
        }
        null -> {} // No overlay active
    }

    // Recovery prompt â€” shown independently of overlay queue (user must acknowledge streak break)
    if (uiState.showRecoveryPrompt && uiState.brokenStreakCount > 0) {
        RecoveryPromptDialog(
            brokenStreakCount = uiState.brokenStreakCount,
            onStartRecovery = { viewModel.startRecoveryProtocol() },
            onDismiss = { viewModel.dismissRecoveryPrompt() }
        )
    }

    // Get time-of-day colors for animated gradient
    val dailyWellColors = LocalDailyWellColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        PremiumMotionBackdrop()

        Scaffold { paddingValues ->
            if (uiState.isLoading) {
                ShimmerLoadingScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else if (uiState.habits.isEmpty()) {
                EmptyHabitsView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    userName = uiState.preferredName,
                    onboardingGoal = uiState.onboardingGoal,
                    onOpenSettings = onSettingsClick,
                    onAddCustomHabit = onNavigateToCustomHabit,
                    onRetry = viewModel::refresh
                )
            } else {
            val mode = uiState.layoutMode
            val coreTrackerOnly = settings.todayViewMode == TodayViewMode.SIMPLE

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                            )
                        )
                    ),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ======================================================
                // DONE_FOR_TODAY: Closure card replaces upper motivational stack
                // "You're done. Rest is part of the plan."
                // ======================================================
                if (mode == TodayLayoutMode.DONE_FOR_TODAY) {
                    item(key = "done_closure") {
                        DoneForTodayCard(
                            completedCount = uiState.completedCount,
                            streakCount = uiState.streakInfo.currentStreak
                        )
                    }
                }

                // ======================================================
                // Trial banner: ESTABLISHED only, never on first days
                // ======================================================
                if (!coreTrackerOnly &&
                    mode == TodayLayoutMode.ESTABLISHED &&
                    uiState.isOnTrial && uiState.trialDaysRemaining <= 7
                ) {
                    item(key = "trial_banner") {
                        TrialBanner(
                            daysRemaining = uiState.trialDaysRemaining,
                            onUpgradeClick = onUpgradeClick
                        )
                    }
                }

                // ======================================================
                // Greeting + Progress: all modes except DONE_FOR_TODAY
                // ======================================================
                if (mode != TodayLayoutMode.DONE_FOR_TODAY) {
                    item(key = "greeting") {
                        PersonalizedGreetingHeader(
                            userName = uiState.preferredName,
                            primaryGoal = uiState.onboardingGoal,
                            currentStreak = uiState.streakInfo.currentStreak,
                            completionRate = if (uiState.totalCount > 0) {
                                uiState.completedCount.toFloat() / uiState.totalCount
                            } else 0f,
                            isDarkMode = isDarkThemeEnabled,
                            onThemeToggle = { enableDarkMode ->
                                uiScope.launch {
                                    val currentSettings = settingsRepository.getSettingsSnapshot()
                                    val targetMode = if (enableDarkMode) ThemeMode.DARK else ThemeMode.LIGHT
                                    if (currentSettings.themeMode != targetMode) {
                                        settingsRepository.updateSettings(
                                            currentSettings.copy(themeMode = targetMode)
                                        )
                                    }
                                }
                            },
                            onSettingsClick = onSettingsClick
                        )
                    }

                    item(key = "progress_dashboard") {
                        DailyProgressDashboard(
                            completedCount = uiState.completedCount,
                            totalCount = uiState.totalCount,
                            streakCount = uiState.streakInfo.currentStreak
                        )
                    }

                    item(key = "today_view_mode_toggle") {
                        TodayViewModeToggle(
                            mode = settings.todayViewMode,
                            onModeChange = { selectedMode ->
                                uiScope.launch {
                                    val currentSettings = settingsRepository.getSettingsSnapshot()
                                    if (currentSettings.todayViewMode != selectedMode) {
                                        settingsRepository.updateSettings(
                                            currentSettings.copy(todayViewMode = selectedMode)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // ======================================================
                // Quick actions: BUILDING and ESTABLISHED only
                // (Not on FIRST_SESSION â€” reduce noise. Not on DONE â€” earned rest.)
                // ======================================================
                if (!coreTrackerOnly && mode != TodayLayoutMode.DONE_FOR_TODAY) {
                    item(key = "quick_actions") {
                        AlwaysVisibleQuickActionsRow(
                            onWaterClick = { viewModel.quickLogWater() },
                            onLogClick = onNavigateToWorkout,
                            onAICoachClick = onNavigateToAICoach,
                            onAddHabitClick = onNavigateToCustomHabit,
                            waterCount = uiState.todayWaterCount,
                            waterGoal = uiState.dailyWaterGoal
                        )
                    }
                }

                if (mode != TodayLayoutMode.DONE_FOR_TODAY) {
                    item(key = "extras_nav") {
                        FocusedExtrasNavCard(
                            isSimpleMode = coreTrackerOnly,
                            onOpenInsights = onNavigateToInsights,
                            onOpenCoach = onNavigateToAICoach
                        )
                    }
                }

                // ======================================================
                // Habits: time-of-day grouped with staggered entrance
                // ======================================================
                run {
                    val groupedHabits = uiState.habits.groupBy { habit ->
                        habit.habitType?.preferredTime ?: TimeOfDay.ANYTIME
                    }.toSortedMap(compareBy { it.order })

                    item(key = "today_plan_header") {
                        SectionEyebrow(
                            title = "Today's Plan",
                            subtitle = "Check off completed habits",
                            accentIcon = DailyWellIcons.Habits.HabitStacking,
                            accentColor = Color(0xFF4A9E8F)
                        )
                    }

                    groupedHabits.forEach { (timeOfDay, habitsInGroup) ->
                        val totalInGroup = habitsInGroup.size
                        val completedInGroup = habitsInGroup.count { habit ->
                            uiState.completions[habit.id] == true
                        }
                        item(key = "header_${timeOfDay.name}") {
                            TimeOfDaySectionHeader(
                                timeOfDay = timeOfDay,
                                completedCount = completedInGroup,
                                totalCount = totalInGroup
                            )
                        }

                        itemsIndexed(
                            items = habitsInGroup,
                            key = { _, habit -> habit.id }
                        ) { index, habit ->
                            StaggeredItem(
                                index = index,
                                delayPerItem = 60L,
                                baseDelay = 100L
                            ) {
                                HabitCheckItem(
                                    habit = habit,
                                    timeOfDay = timeOfDay,
                                    isCompleted = uiState.completions[habit.id] == true,
                                    onToggle = { completed ->
                                        viewModel.toggleHabit(habit.id, completed)
                                    }
                                )
                            }
                        }
                    }
                }

                // ======================================================
                // Week grid + Streak (hidden only on FIRST_SESSION)
                // ======================================================
                if (mode != TodayLayoutMode.FIRST_SESSION) {
                    if (!coreTrackerOnly) {
                        item(key = "week_section") {
                            Spacer(modifier = Modifier.height(8.dp))
                            WeekSection(weekData = uiState.weekData)
                        }
                    }

                    item(key = "streak_section") {
                        Spacer(modifier = Modifier.height(8.dp))
                        StreakSection(
                            streakInfo = uiState.streakInfo,
                            completedToday = uiState.completedCount,
                            onShareStreak = { showShareDialog = true }
                        )
                    }
                }

                // Bottom spacing for nav bar
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            }
        } // end Scaffold

        // Confetti celebration overlay â€” visual effect, not a dialog overlay
        ConfettiCelebrationOverlay(
            isActive = showConfetti,
            onComplete = { showConfetti = false }
        )

        // Water snackbar feedback
        uiState.waterSnackbarMessage?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(text = message)
            }
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(2000)
                viewModel.dismissWaterSnackbar()
            }
        }
    }
}

@Composable
private fun TodayViewModeToggle(
    mode: TodayViewMode,
    onModeChange: (TodayViewMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "View mode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = mode == TodayViewMode.SIMPLE,
                    onClick = { onModeChange(TodayViewMode.SIMPLE) },
                    label = { Text("Simple") }
                )
                FilterChip(
                    selected = mode == TodayViewMode.FULL,
                    onClick = { onModeChange(TodayViewMode.FULL) },
                    label = { Text("Full") }
                )
            }
        }
    }
}

@Composable
private fun FocusedExtrasNavCard(
    isSimpleMode: Boolean,
    onOpenInsights: () -> Unit,
    onOpenCoach: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isSimpleMode) "Extras are in tabs" else "More tools in tabs",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onOpenInsights,
                    label = { Text("Open Insights") }
                )
                AssistChip(
                    onClick = onOpenCoach,
                    label = { Text("Open Coach") }
                )
            }
        }
    }
}

@Composable
private fun EnterpriseTodayHeader(
    onSettingsClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "headerFx")
    val glowPulse by transition.animateFloat(
        initialValue = 0.48f,
        targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    val sheenShift by transition.animateFloat(
        initialValue = -240f,
        targetValue = 240f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheenShift"
    )
    val orbShift by transition.animateFloat(
        initialValue = -16f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbShift"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF2C4F8C),
                            Color(0xFF345E96),
                            Color(0xFF1E4B73),
                            Color(0xFF3D618C)
                        )
                    )
                )
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(alpha = 0.24f)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB2E3FF).copy(alpha = 0.34f),
                            Color.Transparent
                        ),
                        center = Offset(x = size.width * 0.22f + orbShift * 2.8f, y = size.height * 0.07f),
                        radius = size.minDimension * 0.85f
                    ),
                    radius = size.minDimension * 0.85f,
                    center = Offset(x = size.width * 0.22f + orbShift * 2.8f, y = size.height * 0.07f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF78E0D1).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(x = size.width * 0.82f - orbShift * 1.8f, y = size.height * 0.62f),
                        radius = size.minDimension * 0.42f
                    ),
                    radius = size.minDimension * 0.42f,
                    center = Offset(x = size.width * 0.82f - orbShift * 1.8f, y = size.height * 0.62f)
                )
                val y = size.height * 0.80f
                drawLine(
                    color = Color(0xFFBDEBFF).copy(alpha = 0.26f),
                    start = Offset(-220f + sheenShift, y),
                    end = Offset(size.width * 0.46f + sheenShift, y),
                    strokeWidth = 3f
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DailyWell",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.2).sp,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFD9F0FF),
                                Color(0xFFA8DFFF)
                            )
                        ),
                        shadow = Shadow(
                            color = Color(0xFFAED8FF).copy(alpha = glowPulse),
                            offset = Offset(0f, 0f),
                            blurRadius = 30f
                        )
                    )
                )

                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onSettingsClick)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.30f),
                                    Color(0xFFC4E7FF).copy(alpha = 0.12f)
                                )
                            )
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f)),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = DailyWellIcons.Nav.Settings,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun PremiumMotionBackdrop() {
    val infiniteTransition = rememberInfiniteTransition(label = "premiumBackdrop")
    val orbitShift by infiniteTransition.animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbitShift"
    )
    val orbitLift by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbitLift"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF0F4FA),
                        Color(0xFFEAF0F9),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-74).dp + orbitShift.dp, y = (-118).dp + orbitLift.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF89C2F3).copy(alpha = 0.34f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = (34).dp + (orbitShift * -0.6f).dp, y = (14).dp + orbitLift.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9EDAC8).copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 22.dp + (orbitShift * -0.4f).dp, y = (18).dp + orbitLift.dp)
                .graphicsLayer {
                    alpha = glowPulse
                }
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF2F7FF).copy(alpha = 0.48f),
                            Color(0xFFDCE8F8).copy(alpha = 0.24f)
                        )
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.35f)
        ) {
            drawLine(
                color = Color(0xFFB3C6DC).copy(alpha = 0.16f),
                start = Offset(-70f + orbitShift * 4f, size.height * 0.64f),
                end = Offset(size.width + 120f, size.height * 0.58f),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0xFFB6D4EE).copy(alpha = 0.14f),
                start = Offset(80f, size.height * 0.22f + orbitLift * 2f),
                end = Offset(size.width * 0.9f, size.height * 0.16f + orbitShift * 0.5f),
                strokeWidth = 1.6f
            )
            drawLine(
                color = Color(0xFFB6D4EE).copy(alpha = 0.14f),
                start = Offset(size.width * 0.2f, size.height * 0.94f),
                end = Offset(size.width * 0.8f, size.height * 0.84f),
                strokeWidth = 1.7f
            )
            drawCircle(
                color = Color(0xFFBEDBF1).copy(alpha = 0.17f),
                radius = size.minDimension * 0.17f,
                center = Offset(size.width * 0.72f, size.height * 0.31f)
            )
            drawCircle(
                color = Color(0xFFBEDBF1).copy(alpha = 0.12f),
                radius = size.minDimension * 0.11f,
                center = Offset(size.width * 0.22f, size.height * 0.74f)
            )
            if (glowPulse > 0.95f) {
                drawCircle(
                    color = Color(0xFF9BD0F2).copy(alpha = 0.10f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.7f, size.height * 0.74f)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-36).dp, y = (-106).dp)
                .graphicsLayer { alpha = 0.22f; scaleX = glowPulse; scaleY = glowPulse }
                .border(1.6.dp, Color(0xFF7F96B7), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(118.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-34).dp, y = 20.dp)
                .graphicsLayer { alpha = 0.18f; scaleX = glowPulse; scaleY = glowPulse }
                .border(1.4.dp, Color(0xFF70A999), CircleShape)
        )
    }
}

@Composable
private fun DateHeader(date: String) {
    val displayDate = try {
        val localDate = LocalDate.parse(date)
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        "${months[localDate.monthNumber - 1]} ${localDate.dayOfMonth}"
    } catch (e: Exception) {
        date
    }

    // A9: Fade-in entrance animation
    Column(modifier = Modifier.padding(bottom = 8.dp).fadeInOnAppear(durationMs = 400, delay = 100)) {
        Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = displayDate,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun SectionEyebrow(
    title: String,
    subtitle: String,
    accentIcon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val highlight = Brush.linearGradient(
        listOf(
            accentColor.copy(alpha = 0.11f),
            accentColor.copy(alpha = 0.03f)
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlight, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                accentColor.copy(alpha = 0.18f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (accentIcon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.16f))
                        .border(
                            1.dp,
                            accentColor.copy(alpha = 0.35f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = accentIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = accentColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeekSection(weekData: com.dailywell.app.data.model.WeekData?) {
    if (weekData == null) return

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Prominent,
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF2F6FF).copy(alpha = 0.98f),
                            Color(0xFFF8F2FF).copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Surface(
                color = Color(0xFF6D63C4).copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF6D63C4).copy(alpha = 0.20f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Analytics.Calendar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF4A4E77)
                    )
                    Text(
                        text = "THIS WEEK",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A4E77)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Momentum",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B6F9C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.55f))
                    .padding(10.dp)
            ) {
                WeekCalendar(weekData = weekData)
            }
            Spacer(modifier = Modifier.height(10.dp))
            WeekSummaryText(
                weekData = weekData,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF6D63C4).copy(alpha = 0.06f)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun StreakSection(
    streakInfo: com.dailywell.app.data.model.StreakInfo,
    completedToday: Int = 0,
    onShareStreak: () -> Unit = {}
) {
    if (streakInfo.currentStreak > 0) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = ElevationLevel.Prominent,
            cornerRadius = 22.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFFF6EE).copy(alpha = 0.95f),
                                Color(0xFFFFEDF8).copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFFFF6B35).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(0xFFFF6B35).copy(alpha = 0.40f)),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "KEEP GOING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD65A2A),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Text(
                        text = "${streakInfo.currentStreak}-day streak",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.68f))
                        .padding(top = 6.dp, bottom = 6.dp)
                ) {
                    StreakBadge(streakInfo = streakInfo)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFA64D))
                    )
                    Text(
                        text = "Current run: $completedToday completed today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = onShareStreak,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Primary
                    )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share My Streak",
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = getStreakMotivation(streakInfo.currentStreak),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Returns motivational text based on streak count
 */
private fun getStreakMotivation(streak: Int): String = when {
    streak >= 100 -> "You're a legend! Share your incredible journey!"
    streak >= 30 -> "30+ days of dedication! Inspire others!"
    streak >= 14 -> "Two weeks strong! Your consistency is contagious!"
    streak >= 7 -> "One week milestone! Time to celebrate!"
    streak >= 3 -> "Building momentum! Keep it going!"
    else -> "Every day counts. You've got this!"
}

@Composable
private fun EmptyHabitsView(
    modifier: Modifier = Modifier,
    userName: String = "",
    onboardingGoal: String? = null,
    onOpenSettings: () -> Unit = {},
    onAddCustomHabit: () -> Unit = {},
    onRetry: () -> Unit = {}
) {
    val goalLabel = remember(onboardingGoal) {
        when (onboardingGoal) {
            "sleep_better" -> "sleep better"
            "more_energy" -> "more energy"
            "less_stress" -> "feel less stress"
            "get_healthier" -> "get healthier"
            "build_discipline" -> "build discipline"
            "feel_happier" -> "feel happier"
            else -> null
        }
    }
    val firstName = remember(userName) {
        userName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
    }
    val title = if (firstName.isBlank()) "Your journey starts here" else "$firstName, your journey starts here"
    val subtitle = if (goalLabel == null) {
        "Pick your first habit and start building momentum today."
    } else {
        "Let us set up your first habits to help you $goalLabel."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = ElevationLevel.Medium,
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Habits.Add,
                        contentDescription = "No habits",
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (goalLabel != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Goal focus: $goalLabel",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Settings")
                    }
                    Button(
                        onClick = onAddCustomHabit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Add Habit")
                    }
                }

                TextButton(onClick = onRetry) {
                    Text("Refresh")
                }
            }
        }
    }
}

/**
 * "Done for today" closure card â€” replaces upper motivational stack when all habits complete.
 * Psychology: Zeigarnik effect in reverse â€” resolved tension creates satisfaction and
 * a clear reason to come back tomorrow.
 */
@Composable
private fun DoneForTodayCard(
    completedCount: Int,
    streakCount: Int
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .fadeInOnAppear(durationMs = 600, delay = 0),
        elevation = ElevationLevel.Medium,
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkmark icon with gentle breathing
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF4CD964).copy(alpha = 0.12f))
                    .breathingAnimation(minScale = 1f, maxScale = 1.04f, durationMs = 2500),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.Actions.Check,
                    contentDescription = "All done",
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF4CD964)
                )
            }

            Text(
                text = "You're done for today.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Rest is part of the plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtle stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedCount habits completed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (streakCount > 0) {
                    Text(
                        text = "$streakCount-day streak",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Generate AI-style insight based on user patterns
 * Psychology-driven: Uses positive reinforcement and social proof
 */
private fun getAIInsight(streak: Int, completed: Int, total: Int): String {
    val progressPercent = if (total > 0) (completed * 100 / total) else 0

    return when {
        // Progress-based insights
        progressPercent >= 80 -> "You're crushing it today! Just ${total - completed} more to go."
        progressPercent >= 50 -> "Halfway there - your future self will thank you!"
        progressPercent > 0 -> "Great start! Small wins lead to big changes."

        // Streak-based motivation
        streak >= 21 -> "21+ days = a real habit! You've rewired your brain."
        streak >= 14 -> "Two weeks of consistency - that's rare. Keep it up!"
        streak >= 7 -> "One week strong! Research shows habits stick after 7 days."
        streak >= 3 -> "3-day momentum building. You're on the right track!"

        // Default encouragement
        else -> "Every expert was once a beginner. Start your journey today!"
    }
}

/**
 * Time of day section header - groups habits by Morning/Afternoon/Evening
 * 2026 UX standard from competitors like Habitify, Streaks
 */
@Composable
private fun TimeOfDaySectionHeader(
    timeOfDay: TimeOfDay,
    completedCount: Int,
    totalCount: Int
) {
    val sectionColor = when (timeOfDay) {
        TimeOfDay.MORNING -> Color(0xFF59B8E8)
        TimeOfDay.AFTERNOON -> Color(0xFF3CC6A3)
        TimeOfDay.EVENING -> Color(0xFFA68BFF)
        TimeOfDay.ANYTIME -> Color(0xFF7DB0D7)
    }
    val progressPercent = if (totalCount > 0) {
        ((completedCount.toFloat() / totalCount) * 100).toInt()
    } else 0
    val progressLabel = "$completedCount/$totalCount"
    val sectionRange = when (timeOfDay) {
        TimeOfDay.MORNING -> "06:00-12:00"
        TimeOfDay.AFTERNOON -> "12:00-18:00"
        TimeOfDay.EVENING -> "18:00-24:00"
        TimeOfDay.ANYTIME -> "All Day"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(sectionColor.copy(alpha = 0.14f))
                    .border(
                        1.dp,
                        sectionColor.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.getTimeOfDayIcon(timeOfDay.name),
                    contentDescription = timeOfDay.displayName,
                    modifier = Modifier
                        .size(16.dp)
                        .breathingAnimation(minScale = 1f, maxScale = 1.08f, durationMs = 2000),
                    tint = sectionColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeOfDay.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = sectionColor
                )
                Text(
                    text = sectionRange,
                    style = MaterialTheme.typography.labelSmall,
                    color = sectionColor.copy(alpha = 0.85f)
                )
            }

            Surface(
                color = sectionColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, sectionColor.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "$progressLabel",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = sectionColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$completedCount habits done",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${progressPercent}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = sectionColor.copy(alpha = 0.95f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    color = sectionColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

// A11: Glass effect WellCoinBadge with press feedback
@Composable
private fun WellCoinBadge(coins: Int) {
    val dailyWellColors = LocalDailyWellColors.current
    GlassChip(
        modifier = Modifier
            .padding(end = 4.dp)
            .pressScale()
    ) {
        Icon(
            imageVector = DailyWellIcons.Gamification.Coin,
            contentDescription = "Coins",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = coins.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

/**
 * 2026 Apple Watch-Style Progress Dashboard
 * Shows daily habit completion with animated rings
 */
@Composable
private fun DailyProgressDashboard(
    completedCount: Int,
    totalCount: Int,
    streakCount: Int
) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val trend = remember(progress) {
        listOf(
            (progress * 0.45f + 0.18f).coerceIn(0.1f, 1f),
            (progress * 0.55f + 0.14f).coerceIn(0.1f, 1f),
            (progress * 0.72f + 0.1f).coerceIn(0.1f, 1f),
            (progress * 0.35f + 0.22f).coerceIn(0.1f, 1f),
            (progress * 0.85f + 0.05f).coerceIn(0.1f, 1f),
            (progress * 0.62f + 0.15f).coerceIn(0.1f, 1f),
            (progress * 0.9f + 0.04f).coerceIn(0.1f, 1f)
        )
    }

    val ringPulse by rememberInfiniteTransition(label = "dashboardPulse").animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dashboardPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF4FAFF),
                        Color(0xFFF4F8FF),
                        Color(0xFFF9F4FF)
                    )
                )
            )
            .border(1.dp, Color(0xFFD7EAE2), RoundedCornerShape(28.dp))
            .padding(20.dp)
            .fadeInOnAppear(durationMs = 460, delay = 40)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(alpha = 0.28f + (ringPulse * 0.04f))
                .padding(2.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val first = Color(0xFFCDE8FD).copy(alpha = 0.30f)
                val second = Color(0xFFDCE9FF).copy(alpha = 0.16f)
                drawCircle(
                    color = first,
                    center = Offset(size.width * 0.16f, size.height * 0.18f),
                    radius = size.width * 0.18f
                )
                drawCircle(
                    color = second,
                    center = Offset(size.width * 0.82f, size.height * 0.80f),
                    radius = size.width * 0.24f
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Performance Hub",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF507167)
                    )
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF203A35)
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3F6E62),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedProgressRing(
                        progress = progress,
                        ringColor = if (progress >= 1f) Color(0xFF48BE8E) else Color(0xFF63AFD6),
                        backgroundColor = Color(0xFFD7E9E6),
                        strokeWidth = 10.dp,
                        modifier = Modifier.size(92.dp),
                        glowEnabled = true
                    ) {
                        Text(
                            text = "$completedCount/$totalCount",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF234641)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardMetricRow(
                        label = "Completed",
                        value = "$completedCount of $totalCount habits",
                        accentColor = Color(0xFF76BADF),
                        labelColor = Color(0xFF5A776F),
                        valueColor = Color(0xFF2A4640)
                    )
                    DashboardMetricRow(
                        label = "Streak",
                        value = if (streakCount > 0) "$streakCount day run" else "Start your first streak",
                        accentColor = Color(0xFFE0A45F),
                        labelColor = Color(0xFF5A776F),
                        valueColor = Color(0xFF2A4640)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = Color(0xFF67C9A3),
                trackColor = Color(0xFFDDEDE7)
            )

            Text(
                text = "Weekly rhythm",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3F645B)
            )
            MiniWeekBars(values = trend)

            Text(
                text = when {
                    progress >= 1f -> "Elite consistency today. Everything is complete."
                    progress >= 0.6f -> "Strong pace. Finish the final habits."
                    progress > 0f -> "Momentum started. Keep stacking wins."
                    else -> "No check-ins yet. Start with one easy habit."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4A6B63)
            )
        }
    }
}

@Composable
private fun DashboardMetricRow(
    label: String,
    value: String,
    accentColor: Color,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun MiniWeekBars(values: List<Float>) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { index, value ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height((18 + (value * 36f)).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF93C8E5),
                                    Color(0xFF86D8BC)
                                )
                            )
                        )
                )
                Text(
                    text = days[index.coerceIn(0, days.lastIndex)],
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF58716A)
                )
            }
        }
    }
}

@Composable
private fun FirstDayTutorialOverlay(
    onDismiss: () -> Unit
) {
    var currentTip by remember { mutableIntStateOf(0) }

    val tips = listOf(
        TutorialTip(
            title = "Complete a Habit",
            description = "Tap any habit card to mark it done. Build your streak day by day!",
            icon = DailyWellIcons.Status.Success,
            alignment = Alignment.Center
        ),
        TutorialTip(
            title = "Quick Actions",
            description = "Use the action bar to log water, workouts, or chat with your AI coach â€” all in one tap.",
            icon = DailyWellIcons.Actions.Add,
            alignment = Alignment.Center
        ),
        TutorialTip(
            title = "Daily Insights",
            description = "Every day you'll get a fresh, research-backed insight to fuel your motivation.",
            icon = DailyWellIcons.Coaching.AICoach,
            alignment = Alignment.Center
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (currentTip < tips.size - 1) {
                    currentTip++
                } else {
                    onDismiss()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val tip = tips[currentTip]

        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(initialScale = 0.9f)
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                elevation = ElevationLevel.Prominent,
                cornerRadius = 24.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tip.icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = tip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = tip.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tips.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == currentTip) 8.dp else 6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (index == currentTip) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (currentTip < tips.size - 1) "Tap to continue" else "Tap to start",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class TutorialTip(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val alignment: Alignment
)

/**
 * SLM Model Download Progress Card
 * Shows on TodayScreen when no model is downloaded yet.
 * Non-blocking, dismissible â€” app is fully usable during download.
 */
@Composable
private fun SLMDownloadCard(
    progress: SLMDownloadProgress,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    // Storage warning uses a distinct amber/orange tint to stand out
    val isStorageWarning = progress is SLMDownloadProgress.NeedsStorage

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (progress) {
                        is SLMDownloadProgress.NotStarted -> "Offline AI Coach"
                        is SLMDownloadProgress.Downloading -> "Setting up AI coach..."
                        is SLMDownloadProgress.Failed -> "Download failed"
                        is SLMDownloadProgress.NeedsStorage -> "Storage needed"
                        is SLMDownloadProgress.WaitingForWifi -> "Waiting for WiFi"
                        else -> "AI Coach"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isStorageWarning) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                )
                // Only allow dismiss for non-critical states â€” NEVER for storage warning
                if (progress is SLMDownloadProgress.NotStarted ||
                    progress is SLMDownloadProgress.Failed ||
                    progress is SLMDownloadProgress.WaitingForWifi
                ) {
                    Text(
                        text = "Later",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (progress) {
                is SLMDownloadProgress.NotStarted -> {
                    Text(
                        text = "Download your AI coach for free, offline coaching (~380 MB over WiFi).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        )
                    ) {
                        Text("Download AI Coach (WiFi)")
                    }
                }
                is SLMDownloadProgress.Downloading -> {
                    val percent = (progress.progress * 100).toInt()
                    Text(
                        text = "Setting up your personal AI coach... $percent%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                is SLMDownloadProgress.Failed -> {
                    Text(
                        text = "Something went wrong. Tap to retry on WiFi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Retry Download")
                    }
                }
                is SLMDownloadProgress.NeedsStorage -> {
                    // This card CANNOT be dismissed â€” user must free space
                    val needMB = progress.needBytes / (1024 * 1024)
                    Text(
                        text = "Free up ${needMB}MB to unlock your personal AI coach. " +
                            "Without it, coaching features are limited to 10 messages per day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE65100)
                        )
                    ) {
                        Text("Check Storage Again", color = Color.White)
                    }
                }
                is SLMDownloadProgress.WaitingForWifi -> {
                    Text(
                        text = "Your AI coach is ready to download (~380 MB). Connect to WiFi to start - we'll never use your mobile data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onStartDownload,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Check Connection")
                    }
                }
                else -> { /* Ready or Dismissed â€” not shown */ }
            }
        }
    }
}

/**
 * Recovery prompt dialog â€” shown when user's streak was broken.
 * "Never miss twice" psychology: compassionate, not punishing.
 */
@Composable
private fun RecoveryPromptDialog(
    brokenStreakCount: Int,
    onStartRecovery: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* block propagation */ },
            elevation = ElevationLevel.Prominent,
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "We noticed you missed yesterday",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your $brokenStreakCount-day streak is gone, but your progress isn't. " +
                            "Want to start a comeback plan?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onStartRecovery,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Let's do it", fontWeight = FontWeight.SemiBold)
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        "Not now",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * AI Coach message card â€” shows personalized message from selected coach persona.
 * Style adapts to coach personality (Encouraging, Analytical, Direct, Gentle, Motivational).
 */
@Composable
private fun AICoachCard(
    message: String,
    coachName: String,
    coachAvatar: String,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .fadeInOnAppear(durationMs = 400, delay = 200),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = coachAvatar, fontSize = 20.sp)
                    Text(
                        text = coachName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "dismiss",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "New message",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRefresh() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Implementation Intention reminder card.
 * "When [situation], I will [action]" â€” context-aware nudge.
 * Subtle card that connects user's pre-commitment to the current moment.
 */
@Composable
private fun IntentionReminderCard(
    intention: ImplementationIntention,
    onDoIt: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .fadeInOnAppear(durationMs = 400, delay = 100),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = intention.situation.emoji, fontSize = 18.sp)
                Text(
                    text = "Your plan",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = intention.getIntentionStatement(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDoIt,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Do it now", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Later", fontSize = 13.sp)
                }
            }
        }
    }
}

