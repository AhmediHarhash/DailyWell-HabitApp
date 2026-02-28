package com.dailywell.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.*

/**
 * 2026 Enterprise-Grade Lively UX Components
 * Psychology-driven, animated, premium feel
 */

// ==================== PERSONALIZED GREETING HEADER ====================

/**
 * Time-aware personalized greeting with psychology-based messaging
 * Uses FBI emotion labeling + Noom-style positivity
 */
@Composable
fun PersonalizedGreetingHeader(
    userName: String = "",
    primaryGoal: String? = null,
    currentStreak: Int = 0,
    completionRate: Float = 0f,
    isDarkMode: Boolean = false,
    onThemeToggle: (Boolean) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentHour = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    }
    val daySeed = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfMonth
    }

    val (greeting, _, subMessage) = remember(currentHour, currentStreak, completionRate) {
        getTimeBasedGreeting(currentHour, currentStreak, completionRate)
    }
    val dailyQuote = remember(daySeed) {
        getDailyMotivationQuote(daySeed)
    }

    val gradientColors = when {
        currentHour in 5..11 -> listOf(
            Color(0xFF223A59),
            Color(0xFF2B4C71),
            Color(0xFF355E86)
        )
        currentHour in 12..17 -> listOf(
            Color(0xFF1E3B45),
            Color(0xFF27525E),
            Color(0xFF316974)
        )
        currentHour in 18..21 -> listOf(
            Color(0xFF2A2E4B),
            Color(0xFF393E63),
            Color(0xFF4A4E77)
        )
        else -> listOf(
            Color(0xFF212737),
            Color(0xFF2B3145),
            Color(0xFF343B54)
        )
    }
    val normalizedName = remember(userName) {
        userName
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull()
            .orEmpty()
    }
    val goalLabel = remember(primaryGoal) {
        when (primaryGoal) {
            "sleep_better" -> "Sleep better"
            "more_energy" -> "More energy"
            "less_stress" -> "Less stress"
            "get_healthier" -> "Get healthier"
            "build_discipline" -> "Build discipline"
            "feel_happier" -> "Feel happier"
            else -> null
        }
    }
    val greetingLine = if (normalizedName.isBlank()) "$greeting!" else "$greeting, $normalizedName!"
    val brandGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF74D7FF),
            Color(0xFF6EE7B7),
            Color(0xFFD9B4FF)
        )
    )

    val settingsInteractionSource = remember { MutableInteractionSource() }
    val isSettingsPressed by settingsInteractionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.linearGradient(gradientColors)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DailyWell",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            brush = brandGradient,
                            shadow = Shadow(
                                color = Color(0xFF9F7EFF).copy(alpha = 0.45f),
                                blurRadius = 10f,
                                offset = Offset(0f, 2f)
                            )
                        ),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = greetingLine,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 46.dp, height = 30.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(999.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Switch(
                                    checked = isDarkMode,
                                    onCheckedChange = onThemeToggle,
                                    modifier = Modifier.scale(0.72f),
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (isDarkMode) {
                                                DailyWellIcons.Misc.Night
                                            } else {
                                                DailyWellIcons.Misc.Sunrise
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF5FA4D4),
                                        checkedBorderColor = Color(0xFF8DC4EA),
                                        checkedIconColor = Color(0xFF27456A),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.32f),
                                        uncheckedBorderColor = Color.White.copy(alpha = 0.50f),
                                        uncheckedIconColor = Color(0xFF45637D)
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .graphicsLayer {
                                        scaleX = if (isSettingsPressed) 0.96f else 1f
                                        scaleY = if (isSettingsPressed) 0.96f else 1f
                                    }
                                    .clickable(
                                        interactionSource = settingsInteractionSource,
                                        indication = null,
                                        onClick = onSettingsClick
                                    )
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = DailyWellIcons.Nav.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(30.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            shadow = Shadow(
                                color = Color(0xFF9ECBFF).copy(alpha = 0.28f),
                                blurRadius = 10f,
                                offset = Offset.Zero
                            )
                        ),
                        color = Color.White.copy(alpha = 0.86f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dailyQuote,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (currentStreak > 0) {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = DailyWellIcons.Analytics.Streak,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFFFFC986)
                                    )
                                    Text(
                                        text = "$currentStreak day streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        goalLabel?.let { label ->
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = DailyWellIcons.Actions.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF8EF4D0)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun PulsingFireEmoji() {
    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireScale"
    )
    Icon(
        imageVector = DailyWellIcons.Analytics.Streak,
        contentDescription = "Streak",
        modifier = Modifier.size(16.dp).scale(scale),
        tint = Color(0xFFFF6B35)
    )
}

private fun getTimeBasedGreeting(
    hour: Int,
    streak: Int,
    completionRate: Float
): Triple<String, String, String> {
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..21 -> "Good evening"
        else -> "Hey there"
    }

    val emoji = when (hour) {
        in 5..8 -> "\u2615" // Coffee
        in 9..11 -> "\u2600\uFE0F" // Sun
        in 12..14 -> "\uD83C\uDF1E" // Sun with face
        in 15..17 -> "\u2728" // Sparkles
        in 18..21 -> "\uD83C\uDF19" // Moon
        else -> "\uD83C\uDF1F" // Star
    }

    // Psychology-based sub-messages
    val subMessage = when {
        // Loss aversion trigger (don't lose your streak!)
        streak >= 7 && hour in 18..23 -> "Don't break your amazing $streak day streak!"

        // Morning motivation (fresh start psychology)
        hour in 5..9 -> "A fresh day, a fresh start"

        // Midday encouragement
        hour in 12..14 && completionRate < 0.5f -> "You're doing great - keep the momentum!"

        // Evening reflection
        hour in 18..21 && completionRate > 0.8f -> "What an incredible day you're having!"

        // Progress validation
        streak >= 3 -> "Your consistency is paying off"

        // Default welcoming
        else -> "A fresh day, a fresh start"
    }

    return Triple(greeting, emoji, subMessage)
}

private fun getDailyMotivationQuote(daySeed: Int): String {
    val quotes = listOf(
        "Small wins compound.",
        "One focused habit beats one perfect plan.",
        "Momentum starts with this one action.",
        "You can win today in one small step.",
        "Discipline is your daily compounding.",
        "Tiny choices make big outcomes.",
        "Stay consistent, stay unstoppable."
    )

    val index = (daySeed - 1).coerceIn(0, 30) % quotes.size
    return quotes[index]
}

// ==================== SHIMMER LOADING STATE ====================

/**
 * Premium shimmer loading skeleton
 * Better than boring CircularProgressIndicator
 */
@Composable
fun ShimmerLoadingScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header shimmer
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(20.dp)
        )

        // Date shimmer
        ShimmerBox(
            modifier = Modifier
                .width(150.dp)
                .height(24.dp),
            shape = RoundedCornerShape(8.dp)
        )

        // Habit cards shimmer
        repeat(4) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Week section shimmer
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shimmerOffset * 500
                }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// ==================== CONFETTI CELEBRATION OVERLAY ====================

/**
 * Full-screen confetti celebration
 * Triggers when all habits are completed
 */
@Composable
fun ConfettiCelebrationOverlay(
    isActive: Boolean,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf(emptyList<ConfettiParticle>()) }

    LaunchedEffect(isActive) {
        if (isActive) {
            particles = List(30) { ConfettiParticle.create() }
            kotlinx.coroutines.delay(3000)
            onComplete()
        } else {
            particles = emptyList()
        }
    }

    if (particles.isNotEmpty()) {
        Box(modifier = modifier.fillMaxSize()) {
            particles.forEachIndexed { index, particle ->
                AnimatedConfettiPiece(
                    particle = particle,
                    index = index
                )
            }
        }
    }
}

@Composable
private fun AnimatedConfettiPiece(
    particle: ConfettiParticle,
    index: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti$index")

    val yOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = particle.duration,
                delayMillis = particle.delay,
                easing = LinearOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "y$index"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f * particle.rotationSpeed,
        animationSpec = infiniteRepeatable(
            animation = tween(particle.duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation$index"
    )

    val xWobble by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xWobble$index"
    )

    Box(
        modifier = Modifier
            .offset(
                x = (particle.startX + xWobble).dp,
                y = yOffset.dp
            )
            .size(particle.size.dp)
            .graphicsLayer {
                rotationZ = rotation
                rotationY = rotation / 2
            }
            .background(
                color = particle.color,
                shape = if (particle.isCircle) CircleShape else RoundedCornerShape(2.dp)
            )
    )
}

private data class ConfettiParticle(
    val startX: Float,
    val size: Float,
    val duration: Int,
    val delay: Int,
    val color: Color,
    val isCircle: Boolean,
    val rotationSpeed: Float
) {
    companion object {
        private val colors = listOf(
            Color(0xFF6366F1), // Indigo
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF10B981), // Green
            Color(0xFFFFD700), // Gold
            Color(0xFFFF6B35), // Orange
            Color(0xFF3B82F6), // Blue
            Color(0xFFF43F5E)  // Rose
        )

        fun create() = ConfettiParticle(
            startX = (20..360).random().toFloat(),
            size = (6..14).random().toFloat(),
            duration = (2000..4000).random(),
            delay = (0..500).random(),
            color = colors.random(),
            isCircle = (0..1).random() == 0,
            rotationSpeed = (1..3).random().toFloat()
        )
    }
}

// ==================== ALWAYS-VISIBLE QUICK ACTIONS ROW ====================

/**
 * Always-visible quick actions row for dashboard core actions.
 * Uses a single-tone tile style with press feedback + glowing labels.
 */
@Composable
fun AlwaysVisibleQuickActionsRow(
    onWaterClick: () -> Unit,
    onLogClick: () -> Unit,
    onAICoachClick: () -> Unit,
    onAddHabitClick: () -> Unit,
    waterCount: Int = 0,
    waterGoal: Int = 8,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InlineQuickActionButton(
            icon = DailyWellIcons.Health.WaterDrop,
            label = "Hydrate",
            subtitle = if (waterCount > 0) "$waterCount / $waterGoal" else "Quick add",
            onClick = onWaterClick,
            tileColor = Color(0xFF3569B8),
            modifier = Modifier.weight(1f)
        )
        InlineQuickActionButton(
            icon = DailyWellIcons.Analytics.Pattern,
            label = "Log",
            subtitle = "Activity",
            onClick = onLogClick,
            tileColor = Color(0xFF5E54B5),
            modifier = Modifier.weight(1f)
        )
        InlineQuickActionButton(
            icon = DailyWellIcons.Coaching.AICoach,
            label = "Coach",
            subtitle = "Ask AI",
            onClick = onAICoachClick,
            tileColor = Color(0xFF2A8F79),
            modifier = Modifier.weight(1f)
        )
        InlineQuickActionButton(
            icon = DailyWellIcons.Habits.Add,
            label = "Add",
            subtitle = "New habit",
            onClick = onAddHabitClick,
            tileColor = Color(0xFFB67A2E),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InlineQuickActionButton(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    tileColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "quickActionScale"
    )

    val cardElevation by animateDpAsState(
        targetValue = if (isPressed) 5.dp else 12.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "quickActionElevation"
    )

    val tileGlow = tileColor.copy(alpha = if (isPressed) 0.40f else 0.26f)
    val tileLabel = tileColor

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .shadow(
                    elevation = cardElevation,
                    shape = shape,
                    ambientColor = tileGlow,
                    spotColor = tileGlow
                )
                .clip(shape)
                .background(color = tileLabel)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 11.dp, horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(17.dp),
                    tint = Color.White
                )
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium.copy(
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.35f),
                            blurRadius = 18f,
                            offset = Offset.Zero
                        )
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.labelSmall.copy(
                        shadow = Shadow(
                            color = Color.White.copy(alpha = 0.24f),
                            blurRadius = 10f,
                            offset = Offset.Zero
                        )
                    ),
                    color = Color.White.copy(alpha = 0.94f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// Keep backward compat - delegates to new row
@Composable
fun QuickActionBar(
    onWaterClick: () -> Unit,
    onScanClick: () -> Unit,
    onAIClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlwaysVisibleQuickActionsRow(
        onWaterClick = onWaterClick,
        onLogClick = onScanClick,
        onAICoachClick = onAIClick,
        onAddHabitClick = {},
        modifier = modifier
    )
}

// ==================== AI DAILY INSIGHT CARD ====================

/**
 * AI-generated daily insight card
 * Shows personalized motivation based on patterns
 */
@Composable
fun AIDailyInsightCard(
    insight: String,
    insightType: InsightType = InsightType.MOTIVATION,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = insightType.backgroundColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI sparkle icon with animation
                val infiniteTransition = rememberInfiniteTransition(label = "ai")
                val sparkleRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "sparkle"
                )

                Icon(
                    imageVector = when (insightType) {
                        InsightType.MOTIVATION -> DailyWellIcons.Health.Workout
                        InsightType.STREAK_WARNING -> DailyWellIcons.Analytics.Streak
                        InsightType.AI_TIP -> DailyWellIcons.Misc.Sparkle
                        InsightType.MILESTONE_NEAR -> DailyWellIcons.Gamification.Trophy
                    },
                    contentDescription = insightType.title,
                    modifier = Modifier.size(28.dp).graphicsLayer {
                        if (insightType == InsightType.AI_TIP) {
                            rotationZ = sparkleRotation
                        }
                    },
                    tint = insightType.titleColor
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insightType.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = insightType.titleColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        isVisible = false
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Nav.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

enum class InsightType(
    val emoji: String,
    val title: String,
    val backgroundColor: Color,
    val titleColor: Color
) {
    MOTIVATION(
        emoji = "\uD83D\uDCAA",
        title = "Daily Boost",
        backgroundColor = Color(0xFFE8F5E9),
        titleColor = Color(0xFF2E7D32)
    ),
    STREAK_WARNING(
        emoji = "\uD83D\uDD25",
        title = "Keep It Going!",
        backgroundColor = Color(0xFFFFF3E0),
        titleColor = Color(0xFFE65100)
    ),
    AI_TIP(
        emoji = "\u2728",
        title = "AI Insight",
        backgroundColor = Color(0xFFE8EAF6),
        titleColor = Color(0xFF3949AB)
    ),
    MILESTONE_NEAR(
        emoji = "\uD83C\uDFC6",
        title = "Almost There!",
        backgroundColor = Color(0xFFFFF8E1),
        titleColor = Color(0xFFF57F17)
    )
}

// ==================== 365 DAILY INSIGHT CARD ====================

/**
 * DailyInsightCard - PRODUCTION-READY card for 365 Unique Daily Insights
 *
 * Displays research-backed insights from the DailyInsightsDatabase with:
 * - Title and content from psychology/neuroscience research
 * - Category label
 * - Source attribution
 * - Bookmark functionality
 *
 * Part of Task #5: Full 365 Daily Insights Integration
 */
@Composable
fun DailyInsightCard(
    title: String,
    content: String,
    category: String,
    source: String? = null,
    isBookmarked: Boolean = false,
    onBookmarkClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // D1: Premium card with entrance fade-in animation
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fadeInOnAppear(durationMs = 500, delay = 200)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header row with category badge and bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge - subtle neutral background
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5)  // Light gray instead of green tint
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.getCategoryIcon(category),
                            contentDescription = category,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // D2: Bookmark button with bounce animation
                val bookmarkScale by animateFloatAsState(
                    targetValue = if (isBookmarked) 1f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "bookmarkBounce"
                )
                // Track previous bookmark state for bounce trigger
                var previousBookmark by remember { mutableStateOf(isBookmarked) }
                var bounceActive by remember { mutableStateOf(false) }
                val bounceScale by animateFloatAsState(
                    targetValue = if (bounceActive) 1.3f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "bookmarkBounce2"
                )
                LaunchedEffect(isBookmarked) {
                    if (isBookmarked != previousBookmark) {
                        bounceActive = true
                        kotlinx.coroutines.delay(200)
                        bounceActive = false
                        previousBookmark = isBookmarked
                    }
                }

                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) DailyWellIcons.Status.Star else DailyWellIcons.Status.StarOutline,
                        contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark",
                        modifier = Modifier
                            .size(20.dp)
                            .scale(bounceScale),
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Insight title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Insight content
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 24.sp
            )

            // Source attribution
            source?.let { src ->
                if (src.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Coaching.Lesson,
                            contentDescription = "Source",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = src,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // D3: Daily Insight label with sparkle breathing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = DailyWellIcons.Misc.Sparkle,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .breathingAnimation(minScale = 1f, maxScale = 1.1f, durationMs = 1500),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Today's Insight",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Get emoji for insight category
 */
private fun getCategoryEmoji(category: String): String {
    return when (category.lowercase()) {
        "habit psychology" -> "\uD83E\uDDE0"
        "neuroscience" -> "\u26A1"
        "behavioral triggers" -> "\uD83C\uDFAF"
        "progress mindset" -> "\uD83D\uDCC8"
        "social psychology" -> "\uD83D\uDC65"
        "recovery & compassion" -> "\uD83D\uDC9A"
        "advanced techniques" -> "\uD83D\uDD27"
        else -> "\uD83D\uDCA1"
    }
}

// ==================== ANIMATED PROGRESS RING ====================

/**
 * Animated circular progress for daily completion
 */
@Composable
fun AnimatedProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(size),
            strokeWidth = strokeWidth,
            color = backgroundColor
        )

        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(size),
            strokeWidth = strokeWidth,
            color = progressColor
        )

        // Percentage text
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ==================== ANIMATED COUNTER ====================

/**
 * Animated number counter for stats
 */
@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    prefix: String = "",
    suffix: String = "",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    var currentValue by remember { mutableStateOf(0) }

    LaunchedEffect(targetValue) {
        val startValue = currentValue
        val duration = 1000L
        val startTime = System.currentTimeMillis()

        while (currentValue != targetValue) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            currentValue = (startValue + (targetValue - startValue) * progress).toInt()

            if (progress >= 1f) break
            kotlinx.coroutines.delay(16)
        }
        currentValue = targetValue
    }

    Text(
        text = "$prefix$currentValue$suffix",
        style = style,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

// ==================== PULSING NOTIFICATION DOT ====================

/**
 * Pulsing dot for indicating new content or live status
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color = Color(0xFF10B981)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ==================== DAILY MICRO-CHALLENGE CARD ====================

/**
 * DailyMicroChallengeCard - 365 Unique Daily Micro-Challenges (Task #8)
 *
 * Features:
 * - Unique challenge each day (365 total)
 * - Duration and difficulty indicators
 * - Category-based styling
 * - Completion tracking with streak display
 * - Smooth animations
 *
 * Part of Task #8: Full 365 Micro-Challenges Integration
 */
@Composable
fun DailyMicroChallengeCard(
    title: String,
    description: String,
    category: String,
    duration: Int,
    difficulty: String,
    isCompleted: Boolean = false,
    challengeStreak: Int = 0,
    onComplete: () -> Unit = {},
    onSkip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 2026 Premium: Use category color ONLY for accent stripe, NOT background
    val categoryAccentColor = when (category.uppercase()) {
        "PHYSICAL" -> Color(0xFF10B981)
        "MENTAL" -> Color(0xFF8B5CF6)
        "SOCIAL" -> Color(0xFFF59E0B)
        "CREATIVE" -> Color(0xFFEC4899)
        else -> Color(0xFF6366F1)
    }

    val categoryIcon = when (category.uppercase()) {
        "PHYSICAL" -> DailyWellIcons.Health.Workout
        "MENTAL" -> DailyWellIcons.Coaching.AICoach
        "SOCIAL" -> DailyWellIcons.Social.People
        "CREATIVE" -> DailyWellIcons.Misc.Palette
        else -> DailyWellIcons.Gamification.Challenge
    }

    val difficultyColor = when (difficulty.uppercase()) {
        "EASY" -> Color(0xFF10B981)
        "MEDIUM" -> Color(0xFFF59E0B)
        "HARD" -> Color(0xFFEF4444)
        else -> Color(0xFF10B981)
    }

    // Completion celebration animation
    var showCelebration by remember { mutableStateOf(false) }
    LaunchedEffect(isCompleted) {
        if (isCompleted) {
            showCelebration = true
            kotlinx.coroutines.delay(2000)
            showCelebration = false
        }
    }

    val cardScale by animateFloatAsState(
        targetValue = if (showCelebration) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cardScale"
    )

    // D4: Premium card with entrance fade-in animation
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fadeInOnAppear(durationMs = 500, delay = 300)
            .scale(cardScale)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.08f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)  // Enable fillMaxHeight for children
        ) {
            // Left accent stripe - the ONLY place with category color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(categoryAccentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header row with category badge and streak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = categoryIcon,
                                contentDescription = category,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = category.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // D5: Streak badge with pulse when active
                    if (challengeStreak > 0) {
                        val streakBadgePulse = rememberPulseScale(1f, 1.05f, 1000)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF6B35).copy(alpha = 0.15f),
                            modifier = Modifier.graphicsLayer {
                                scaleX = streakBadgePulse
                                scaleY = streakBadgePulse
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = DailyWellIcons.Analytics.Streak,
                                    contentDescription = "Streak",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFFFF6B35)
                                )
                                Text(
                                    text = "$challengeStreak",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B35)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Challenge title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Challenge description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Duration and difficulty row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = DailyWellIcons.Misc.Timer,
                                contentDescription = "Duration",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "${duration}min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Difficulty chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(difficultyColor)
                            )
                            Text(
                                text = difficulty.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Completion status
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = DailyWellIcons.Actions.CheckCircle,
                                    contentDescription = "Done",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Done",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }

                // Action buttons if not completed
                if (!isCompleted) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Skip button
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        ) {
                            Text("Skip Today")
                        }

                        // D6: Glowing Complete button to draw attention
                        GlowingButton(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            glowColor = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = DailyWellIcons.Actions.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Complete",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Daily Challenge label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Gamification.Challenge,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Today's Challenge",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
