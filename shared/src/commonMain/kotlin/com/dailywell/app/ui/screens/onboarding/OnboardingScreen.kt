package com.dailywell.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import habithealth.shared.generated.resources.Res
import habithealth.shared.generated.resources.onboarding_goal_build_discipline
import habithealth.shared.generated.resources.onboarding_goal_feel_happier
import habithealth.shared.generated.resources.onboarding_goal_get_healthier
import habithealth.shared.generated.resources.onboarding_goal_less_stress
import habithealth.shared.generated.resources.onboarding_goal_more_energy
import habithealth.shared.generated.resources.onboarding_goal_sleep_better
import habithealth.shared.generated.resources.onboarding_hero
import com.dailywell.app.core.theme.AccentIndigo
import com.dailywell.app.core.theme.AccentSky
import com.dailywell.app.core.theme.PremiumDesignTokens
import com.dailywell.app.domain.model.HabitRecommendation
import com.dailywell.app.domain.model.OnboardingGoal
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.PremiumSectionChip
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Suppress("unused")
private const val ONBOARDING_HERO_IMAGE_PROMPT = "Modern wellness app hero illustration, abstract human figure moving forward with calm confidence, blue-indigo gradient atmosphere, soft natural light, minimal premium style, no text, no logo, high contrast, mobile-first portrait composition."

private val OnboardingLightGradient = listOf(
    Color(0xFFF0F6FD),
    Color(0xFFE7F1FB),
    Color(0xFFF5FAFF)
)

private val OnboardingDarkGradient = listOf(
    Color(0xFF0A1522),
    Color(0xFF0F2032),
    Color(0xFF09131E)
)

private val OnboardingCtaStart = Color(0xFF4E85BF)
private val OnboardingCtaEnd = Color(0xFF2D5F8C)
private val OnboardingCtaPressedStart = Color(0xFF3E6F9F)
private val OnboardingCtaPressedEnd = Color(0xFF214969)
private val OnboardingAccentPrimary = Color(0xFF4E85BF)
private val OnboardingAccentSecondary = Color(0xFF687EE5)

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
    val isDarkPalette = MaterialTheme.colorScheme.surface.luminance() < 0.45f
    val baseGradient = if (isDarkPalette) {
        OnboardingDarkGradient
    } else {
        OnboardingLightGradient
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(baseGradient))
    ) {
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
        Spacer(modifier = Modifier.height(4.dp))
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
                            listOf(AccentSky, AccentIndigo)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(gradient)
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(width = 300.dp, height = 160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x1E69A8D6),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            content = content
        )
    }
}

// ==================== PAGE 0: WELCOME + PHILOSOPHY (MERGED) ====================

@Composable
private fun WelcomePhilosophyPage(onNext: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val useDarkHeroPalette = colorScheme.surface.luminance() < 0.45f
    val bgGradient = if (useDarkHeroPalette) {
        OnboardingDarkGradient
    } else {
        OnboardingLightGradient
    }
    val headlineColor = if (useDarkHeroPalette) Color(0xFFEAF1FC) else Color(0xFF1E2A3D)
    val taglineStart = if (useDarkHeroPalette) Color(0xFF9ECFFF) else OnboardingAccentPrimary
    val taglineEnd = if (useDarkHeroPalette) Color(0xFFC6B9FF) else OnboardingAccentSecondary
    val supportColor = if (useDarkHeroPalette) Color(0xFFAAC0D6) else Color(0xFF60758D)

    val ctaInteractionSource = remember { MutableInteractionSource() }
    val ctaPressed by ctaInteractionSource.collectIsPressedAsState()
    val ctaScale by animateFloatAsState(
        targetValue = if (ctaPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "welcomeCtaScale"
    )
    val ctaStart by animateColorAsState(
        targetValue = if (ctaPressed) OnboardingCtaPressedStart else OnboardingCtaStart,
        animationSpec = tween(durationMillis = 140),
        label = "welcomeCtaStart"
    )
    val ctaEnd by animateColorAsState(
        targetValue = if (ctaPressed) OnboardingCtaPressedEnd else OnboardingCtaEnd,
        animationSpec = tween(durationMillis = 140),
        label = "welcomeCtaEnd"
    )

    OnboardingPageBackground(gradient = bgGradient) {
        AnimatedWelcomeFlagTitle()

        Spacer(modifier = Modifier.height(14.dp))
        OnboardingHeroImage()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Health doesn't have to be complicated.",
            style = MaterialTheme.typography.displaySmall.copy(
                shadow = Shadow(
                    color = Color(0x261A241D),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f
                )
            ),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = headlineColor,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                taglineStart,
                                taglineEnd,
                                Color(0xFFC7D8FF)
                            )
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append("Enough, not perfect.")
                }
            },
            style = MaterialTheme.typography.titleLarge.copy(
                shadow = Shadow(
                    color = Color(0x332A1A0A),
                    offset = Offset(0f, 2f),
                    blurRadius = 8f
                )
            ),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Start small, win daily.",
            style = MaterialTheme.typography.titleMedium,
            color = supportColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(ctaScale),
            shape = RoundedCornerShape(16.dp),
            interactionSource = ctaInteractionSource,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFFF1F9F2)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ctaStart, ctaEnd)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x669EC7EC),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Let's Go",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF1F9F2),
                    modifier = Modifier.graphicsLayer {
                        shadowElevation = 14f
                    }
                )
            }
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
    val actionInteractionSource = remember { MutableInteractionSource() }
    val actionPressed by actionInteractionSource.collectIsPressedAsState()
    val actionScale by animateFloatAsState(
        targetValue = if (actionPressed) 0.975f else 1f,
        animationSpec = tween(durationMillis = 130),
        label = "goalActionScale"
    )
    val actionStart by animateColorAsState(
        targetValue = if (actionPressed) OnboardingCtaPressedStart else OnboardingCtaStart,
        animationSpec = tween(durationMillis = 150),
        label = "goalActionStart"
    )
    val actionEnd by animateColorAsState(
        targetValue = if (actionPressed) OnboardingCtaPressedEnd else OnboardingCtaEnd,
        animationSpec = tween(durationMillis = 150),
        label = "goalActionEnd"
    )
    val bgGradient = if (useDarkPalette) {
        OnboardingDarkGradient
    } else {
        OnboardingLightGradient
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(bgGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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
                                        Color(0xFF1A2A3B),
                                        Color(0xFF1E3550)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFF8FCFF),
                                        Color(0xFFEAF3FF)
                                    )
                                }
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "What brought you here?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = if (useDarkPalette) Color(0xFFEAF2FD) else Color(0xFF1F2A3B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pick the one that resonates most",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (useDarkPalette) Color(0xFFAFC3D9) else Color(0xFF667A90)
                    )
                }
            }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 6.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GoalSelectionGrid(
                selectedGoal = selectedGoal,
                onSelectGoal = onSelectGoal
            )

            if (selectedGoal != null) {
                AssessmentInline(
                    goal = selectedGoal,
                    score = score,
                    onScoreChange = onScoreChange
                )
            }
        }

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onNext,
                enabled = selectedGoal != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(actionScale),
                shape = RoundedCornerShape(16.dp),
                interactionSource = actionInteractionSource,
                contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFFF2F8FF)
            )
        ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(actionStart, actionEnd)
                            )
                        )
                    .border(
                        width = 1.dp,
                        color = Color(0x6695C5EC),
                        shape = RoundedCornerShape(16.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "That's Me",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF2F8FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeroImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x334A84BC),
                spotColor = Color(0x262B5F9E)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0F1E1B),
                        Color(0xFF162826),
                        Color(0xFF0C1714)
                    )
                )
            )
    ) {
        Image(
            painter = painterResource(Res.drawable.onboarding_hero),
            contentDescription = "Onboarding hero",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
        )

        // Keep contrast premium while preserving color richness.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x060F1F1A),
                            Color.Transparent,
                            Color(0x34101612)
                        )
                    )
                )
        )
    }
}

@Composable
private fun AnimatedWelcomeFlagTitle() {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 260.dp, height = 84.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x385A89D9),
                            Color.Transparent
                        )
                    )
                )
        )

        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3E7EBC),
                                Color(0xFF516CCF),
                                Color(0xFFC8D7FF)
                            )
                        ),
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append("Welcome")
                }
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 36.sp,
                letterSpacing = 0.6.sp,
                shadow = Shadow(
                    color = Color(0x4D406EA0),
                    offset = Offset(0f, 4f),
                    blurRadius = 10f
                )
            )
        )
    }
}

@Composable
private fun AssessmentInline(
    goal: OnboardingGoal,
    score: Int,
    onScoreChange: (Int) -> Unit
) {
    val accent = OnboardingAccentPrimary
    val accentSoft = goalAccent(goal).copy(alpha = 0.35f)
    val useDarkPalette = MaterialTheme.colorScheme.surface.luminance() < 0.45f
    val panelGradient = if (useDarkPalette) {
        listOf(
            Color(0xFF142334),
            Color(0xFF0F1D2B)
        )
    } else {
        listOf(
            Color(0xFFF7FBFF),
            Color(0xFFEAF3FD)
        )
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(panelGradient))
                .border(
                    width = 1.dp,
                    color = accentSoft,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pulse Check",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = goal.assessmentQuestion,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                (1..5).forEach { value ->
                    val isSelected = score == value
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            accent.copy(alpha = 0.26f)
                        } else {
                            if (useDarkPalette) Color(0xFF182A3D) else Color(0xFFEFF5FC)
                        },
                        label = "assessmentBgColor"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                        label = "assessmentTextColor"
                    )
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 480f),
                        label = "assessmentButtonScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .scale(buttonScale)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) accent.copy(alpha = 0.82f) else Color(0x3380A9CC),
                                shape = RoundedCornerShape(14.dp)
                            )
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

            Spacer(modifier = Modifier.height(7.dp))

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

            Spacer(modifier = Modifier.height(7.dp))

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
private fun GoalSelectionGrid(
    selectedGoal: OnboardingGoal?,
    onSelectGoal: (OnboardingGoal) -> Unit
) {
    val goals = OnboardingGoal.entries.toList()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        goals.chunked(2).forEach { rowGoals ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowGoals.forEach { goal ->
                    GoalGridCard(
                        goal = goal,
                        isSelected = selectedGoal == goal,
                        onClick = { onSelectGoal(goal) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowGoals.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GoalGridCard(
    goal: OnboardingGoal,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val useDarkCardPalette = MaterialTheme.colorScheme.surface.luminance() < 0.45f
    val accent = goalAccent(goal)
    val goalImage = goalImageResource(goal)
    val selectedBorderBrush = Brush.horizontalGradient(
        listOf(OnboardingAccentPrimary, OnboardingAccentSecondary)
    )
    val idleBorderBrush = Brush.horizontalGradient(
        listOf(Color(0x2C8FB6D9), Color(0x1A9FB2C9))
    )
    val imageScale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
        label = "goalImageScale"
    )
    val cardElevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = tween(durationMillis = 220),
        label = "goalCardElevation"
    )

    GlassCard(
        modifier = modifier
            .shadow(
                elevation = cardElevation,
                shape = RoundedCornerShape(20.dp)
            ),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp,
        enablePressScale = true,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.14f)
                .clip(RoundedCornerShape(20.dp))
                .background(if (useDarkCardPalette) Color(0xFF101A26) else Color(0xFFEAF2FB))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    brush = if (isSelected) selectedBorderBrush else idleBorderBrush,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Image(
                painter = painterResource(goalImage),
                contentDescription = "${goal.title} illustration",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = imageScale
                        scaleY = imageScale
                    }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x1A091829),
                                Color(0x330A1624),
                                Color(0xB0111824)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF4F8FF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = goal.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xDDE5EEFA),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(OnboardingAccentPrimary, OnboardingAccentSecondary)
                            )
                        )
                        .border(1.dp, Color(0x88D5E9FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Actions.CheckCircle,
                        contentDescription = "Selected",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFF0F8FF)
                    )
                }
            }
        }
    }
}

private fun goalImageResource(goal: OnboardingGoal): DrawableResource = when (goal) {
    OnboardingGoal.SLEEP_BETTER -> Res.drawable.onboarding_goal_sleep_better
    OnboardingGoal.MORE_ENERGY -> Res.drawable.onboarding_goal_more_energy
    OnboardingGoal.LESS_STRESS -> Res.drawable.onboarding_goal_less_stress
    OnboardingGoal.GET_HEALTHIER -> Res.drawable.onboarding_goal_get_healthier
    OnboardingGoal.BUILD_DISCIPLINE -> Res.drawable.onboarding_goal_build_discipline
    OnboardingGoal.FEEL_HAPPIER -> Res.drawable.onboarding_goal_feel_happier
}

private fun goalAccent(goal: OnboardingGoal): Color = when (goal) {
    OnboardingGoal.SLEEP_BETTER -> Color(0xFF6AA7FF)
    OnboardingGoal.MORE_ENERGY -> Color(0xFFFFB14A)
    OnboardingGoal.LESS_STRESS -> Color(0xFF8E8CFF)
    OnboardingGoal.GET_HEALTHIER -> Color(0xFF61C27A)
    OnboardingGoal.BUILD_DISCIPLINE -> Color(0xFFFF7A8A)
    OnboardingGoal.FEEL_HAPPIER -> Color(0xFFFFC96A)
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
    val actionInteractionSource = remember { MutableInteractionSource() }
    val actionPressed by actionInteractionSource.collectIsPressedAsState()
    val actionScale by animateFloatAsState(
        targetValue = if (actionPressed) 0.975f else 1f,
        animationSpec = tween(durationMillis = 130),
        label = "planActionScale"
    )
    val actionStart by animateColorAsState(
        targetValue = if (actionPressed) OnboardingCtaPressedStart else OnboardingCtaStart,
        animationSpec = tween(durationMillis = 150),
        label = "planActionStart"
    )
    val actionEnd by animateColorAsState(
        targetValue = if (actionPressed) OnboardingCtaPressedEnd else OnboardingCtaEnd,
        animationSpec = tween(durationMillis = 150),
        label = "planActionEnd"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (useDarkPalette) {
                        OnboardingDarkGradient
                    } else {
                        OnboardingLightGradient
                    }
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

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
                                    Color(0xFF1A2A3B),
                                    Color(0xFF1E3550)
                                )
                            } else {
                                listOf(
                                    Color(0xFFF8FCFF),
                                    Color(0xFFEAF3FF)
                                )
                            }
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PremiumSectionChip(
                    text = "Plan",
                    icon = DailyWellIcons.Onboarding.Ready
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your Personalized Plan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (useDarkPalette) Color(0xFFEAF2FD) else Color(0xFF1F2A3B)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We picked these based on your goal. Tap to adjust.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (useDarkPalette) Color(0xFFAFC3D9) else Color(0xFF667A90),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${selectedHabitIds.size} of $maxHabits selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedHabitIds.size == maxHabits) OnboardingAccentSecondary else OnboardingAccentPrimary
                )
                Text(
                    text = "Choose up to $maxHabits habits to start",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (useDarkPalette) Color(0xFFAFC3D9) else Color(0xFF667A90)
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
                .height(56.dp)
                .scale(actionScale),
            shape = RoundedCornerShape(16.dp),
            interactionSource = actionInteractionSource,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFFF2F8FF)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(actionStart, actionEnd)
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0x6695C5EC),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFFF2F8FF)
                    )
                } else {
                    Text(
                        text = "Start My Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF2F8FF)
                    )
                }
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
    val useDarkPalette = MaterialTheme.colorScheme.surface.luminance() < 0.45f
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> OnboardingAccentPrimary.copy(alpha = if (useDarkPalette) 0.18f else 0.12f)
            isDisabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        }
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) OnboardingAccentPrimary.copy(alpha = 0.78f) else Color(0x338CB6D6)
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
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
                    .background(
                        if (isSelected) {
                            OnboardingAccentPrimary.copy(alpha = if (useDarkPalette) 0.25f else 0.18f)
                        } else {
                            Color(0x14000000)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.getHabitIcon(habit.id),
                    contentDescription = habit.displayName,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) OnboardingAccentPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = OnboardingAccentPrimary,
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
                    tint = OnboardingAccentPrimary
                )
            }
        }
    }
}


