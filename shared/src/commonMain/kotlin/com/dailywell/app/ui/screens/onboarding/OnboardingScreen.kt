package com.dailywell.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.PremiumDesignTokens
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.Success
import com.dailywell.app.domain.model.HabitRecommendation
import com.dailywell.app.domain.model.OnboardingGoal
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.PremiumSectionChip
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import org.koin.compose.viewmodel.koinViewModel

/**
 * 3-Step Onboarding Flow (fast path)
 *
 * 0. Welcome + Philosophy
 * 1. Goal Selection + Assessment
 * 2. Habit Recommendations -> go directly to dashboard
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (uiState.currentPage > 0) {
                    PremiumTopBar(
                        title = "Your Habit Plan",
                        subtitle = "Step ${uiState.currentPage + 1} of ${uiState.totalPages}",
                        onNavigationClick = { viewModel.previousPage() }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = uiState.currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        }
                    }
                ) { page ->
                    when (page) {
                        0 -> WelcomePhilosophyPage(onNext = { viewModel.nextPage() })
                        1 -> GoalAssessmentPage(
                            selectedGoal = uiState.selectedGoal,
                            score = uiState.assessmentScore,
                            onSelectGoal = { viewModel.selectGoal(it) },
                            onScoreChange = { viewModel.setAssessmentScore(it) },
                            onNext = { viewModel.nextPage() }
                        )
                        2 -> HabitRecommendationPage(
                            recommendations = uiState.recommendations,
                            selectedHabitIds = uiState.selectedHabitIds,
                            maxHabits = uiState.maxFreeHabits,
                            onToggleHabit = { viewModel.toggleHabit(it) },
                            isLoading = uiState.isCompleting,
                            onNext = { viewModel.completeOnboarding(onComplete) },
                            canProceed = viewModel.canProceedFromHabitSelection()
                        )
                    }
                }

                // Animated progress bar
                if (uiState.currentPage > 0 && uiState.currentPage < uiState.totalPages - 1) {
                    OnboardingProgressBar(
                        currentPage = uiState.currentPage,
                        totalPages = uiState.totalPages,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                    )
                }
            }
        }
    }
}

// ==================== ANIMATED PROGRESS BAR ====================

@Composable
private fun OnboardingProgressBar(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (currentPage + 1).toFloat() / totalPages,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "progressBar"
    )

    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Step ${currentPage + 1} of $totalPages",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Primary, Success)
                        )
                    )
            )
        }
    }
}

// ==================== PAGE BACKGROUND WRAPPER ====================

@Composable
private fun OnboardingPageBackground(
    gradient: List<Color>,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(gradient)
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

// ==================== PAGE 0: WELCOME + PHILOSOPHY (MERGED) ====================

@Composable
private fun WelcomePhilosophyPage(onNext: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val useDarkHeroPalette = colorScheme.surface.luminance() < 0.45f
    val bgGradient = if (useDarkHeroPalette) {
        listOf(
            colorScheme.surfaceVariant.copy(alpha = 0.92f),
            colorScheme.primary.copy(alpha = 0.22f),
            colorScheme.background
        )
    } else {
        listOf(
            PremiumDesignTokens.heroCardGradient[0].copy(alpha = 0.20f),
            PremiumDesignTokens.heroCardGradient[1].copy(alpha = 0.14f),
            MaterialTheme.colorScheme.background
        )
    }

    OnboardingPageBackground(gradient = bgGradient) {
        PremiumSectionChip(
            text = "Welcome",
            icon = DailyWellIcons.Onboarding.Welcome
        )

        Spacer(modifier = Modifier.height(18.dp))

        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Primary.copy(alpha = 0.14f),
                            Success.copy(alpha = 0.10f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DailyWellIcons.Onboarding.Welcome,
                contentDescription = "Welcome",
                modifier = Modifier.size(100.dp).scale(scale),
                tint = Primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Health doesn't have\nto be complicated.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Philosophy punch - one line, not a page
        Text(
            text = "Enough, not perfect.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = Primary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Start small, win daily.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Let's Go",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// ==================== PAGE 1: GOAL SELECTION + ASSESSMENT (MERGED) ====================

@Composable
private fun GoalAssessmentPage(
    selectedGoal: OnboardingGoal?,
    score: Int,
    onSelectGoal: (OnboardingGoal) -> Unit,
    onScoreChange: (Int) -> Unit,
    onNext: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val useDarkPalette = colorScheme.surface.luminance() < 0.45f
    val bgGradient = if (useDarkPalette) {
        listOf(
            colorScheme.surfaceVariant.copy(alpha = 0.90f),
            colorScheme.primary.copy(alpha = 0.14f),
            colorScheme.background
        )
    } else {
        listOf(
            PremiumDesignTokens.heroCardGradient[0].copy(alpha = 0.14f),
            PremiumDesignTokens.heroCardGradient[2].copy(alpha = 0.08f),
            colorScheme.background
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = ElevationLevel.Subtle,
            cornerRadius = 22.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = if (useDarkPalette) {
                                listOf(
                                    colorScheme.surfaceVariant.copy(alpha = 0.84f),
                                    colorScheme.secondary.copy(alpha = 0.22f)
                                )
                            } else {
                                listOf(
                                    PremiumDesignTokens.heroCardGradient[0].copy(alpha = 0.26f),
                                    PremiumDesignTokens.heroCardGradient[1].copy(alpha = 0.18f)
                                )
                            }
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PremiumSectionChip(
                    text = "Step 2: Goal",
                    icon = DailyWellIcons.Onboarding.SelectHabits
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "What brought you here?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pick the one that resonates most",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Goal cards
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(OnboardingGoal.entries.toList()) { goal ->
                GoalCard(
                    goal = goal,
                    isSelected = selectedGoal == goal,
                    onClick = { onSelectGoal(goal) }
                )
            }

            // Assessment slider appears below goals when one is selected
            if (selectedGoal != null) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AssessmentInline(
                        goal = selectedGoal,
                        score = score,
                        onScoreChange = onScoreChange
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = selectedGoal != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "That's Me",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun AssessmentInline(
    goal: OnboardingGoal,
    score: Int,
    onScoreChange: (Int) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = goal.assessmentQuestion,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1-5 scale buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..5).forEach { value ->
                    val isSelected = score == value
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .scale(buttonScale)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .clickable { onScoreChange(value) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$value",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = goal.lowLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = goal.highLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    score <= 2 -> "That's exactly why you're here. We'll start with small, easy wins."
                    score == 3 -> "A solid foundation to build on. Let's level it up."
                    else -> "You're already doing well! Let's make it consistent."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: OnboardingGoal,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surface
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Primary else Color.Transparent
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(20.dp))
                else Modifier
            ),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp,
        enablePressScale = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = goal.emoji,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = goal.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = DailyWellIcons.Actions.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.size(24.dp),
                    tint = Primary
                )
            }
        }
    }
}

// ==================== PAGE 2: HABIT RECOMMENDATIONS ====================

@Composable
private fun HabitRecommendationPage(
    recommendations: List<HabitRecommendation>,
    selectedHabitIds: Set<String>,
    maxHabits: Int,
    onToggleHabit: (String) -> Unit,
    isLoading: Boolean,
    onNext: () -> Unit,
    canProceed: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val useDarkPalette = colorScheme.surface.luminance() < 0.45f
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (useDarkPalette) {
                        listOf(
                            colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            colorScheme.primary.copy(alpha = 0.16f),
                            colorScheme.background
                        )
                    } else {
                        listOf(
                            PremiumDesignTokens.heroCardGradient[1].copy(alpha = 0.16f),
                            PremiumDesignTokens.heroCardGradient[2].copy(alpha = 0.08f),
                            colorScheme.background
                        )
                    }
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = ElevationLevel.Subtle,
            cornerRadius = 22.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = if (useDarkPalette) {
                                listOf(
                                    colorScheme.surfaceVariant.copy(alpha = 0.84f),
                                    colorScheme.primary.copy(alpha = 0.20f)
                                )
                            } else {
                                listOf(
                                    PremiumDesignTokens.heroCardGradient[0].copy(alpha = 0.22f),
                                    PremiumDesignTokens.heroCardGradient[1].copy(alpha = 0.16f)
                                )
                            }
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PremiumSectionChip(
                    text = "Step 3: Plan",
                    icon = DailyWellIcons.Onboarding.Ready
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your Personalized Plan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We picked these based on your goal. Tap to adjust.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${selectedHabitIds.size} / $maxHabits selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedHabitIds.size == maxHabits) Success else Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(recommendations) { recommendation ->
                RecommendationCard(
                    recommendation = recommendation,
                    isSelected = selectedHabitIds.contains(recommendation.habitType.id),
                    isDisabled = !selectedHabitIds.contains(recommendation.habitType.id) &&
                            selectedHabitIds.size >= maxHabits,
                    onToggle = { onToggleHabit(recommendation.habitType.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            enabled = canProceed && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Start My Dashboard",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: HabitRecommendation,
    isSelected: Boolean,
    isDisabled: Boolean,
    onToggle: () -> Unit
) {
    val habit = recommendation.habitType
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> habit.color.copy(alpha = 0.12f)
            isDisabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        }
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) habit.color else Color.Transparent
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(20.dp))
                else Modifier
            ),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp,
        enablePressScale = !isDisabled,
        onClick = if (!isDisabled) onToggle else ({})
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(habit.color.copy(alpha = if (isSelected) 0.2f else 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.getHabitIcon(habit.id),
                    contentDescription = habit.displayName,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) habit.color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isDisabled) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (recommendation.isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recommended",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isSelected) {
                Icon(
                    imageVector = DailyWellIcons.Actions.CheckCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.size(24.dp),
                    tint = habit.color
                )
            }
        }
    }
}


