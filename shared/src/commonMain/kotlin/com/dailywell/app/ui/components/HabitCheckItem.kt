package com.dailywell.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.data.model.Habit
import com.dailywell.app.domain.model.TimeOfDay

/**
 * 2026 Premium Habit Card - Minimal & Elegant
 *
 * Design principles:
 * - CLEAN white/neutral cards - NO colored backgrounds
 * - Color only as subtle accents (checkmark, left stripe)
 * - Soft depth with shadows, not borders
 * - Apple-inspired minimal aesthetic
 * - Micro-interactions that feel premium
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitCheckItem(
    habit: Habit,
    timeOfDay: TimeOfDay = TimeOfDay.ANYTIME,
    isCompleted: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val habitColor = habit.habitType?.color ?: Color(0xFF4CAF50)

    // Track completion changes for mini celebration
    var showMiniConfetti by remember { mutableStateOf(false) }
    var previousCompleted by remember { mutableStateOf(isCompleted) }

    LaunchedEffect(isCompleted) {
        if (isCompleted && !previousCompleted) {
            showMiniConfetti = true
            kotlinx.coroutines.delay(1500)
            showMiniConfetti = false
        }
        previousCompleted = isCompleted
    }

    // Press animation
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )

    // Checkmark animations
    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkScale"
    )

    val checkRotation by animateFloatAsState(
        targetValue = if (isCompleted) 0f else -90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkRotation"
    )

    // Shadow elevation - completed items sink down slightly
    val shadowElevation by animateDpAsState(
        targetValue = if (isCompleted) 2.dp else 8.dp,
        animationSpec = tween(300),
        label = "shadow"
    )

    // Left accent stripe animation
    val stripeWidth by animateDpAsState(
        targetValue = if (isCompleted) 4.dp else 3.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "stripe"
    )

    // Text styling
    val titleAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.6f else 1f,
        animationSpec = tween(200),
        label = "titleAlpha"
    )

    // Swipe-to-complete
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart,
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggle(!isCompleted)
                    true
                }
                else -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.25f }
    )

    LaunchedEffect(isCompleted) {
        dismissState.reset()
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val swipeColor = if (isCompleted) Color(0xFFFF6B6B) else habitColor
            val swipeIcon = if (isCompleted) DailyWellIcons.Actions.Undo else DailyWellIcons.Actions.Check
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(swipeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(swipeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = swipeIcon,
                        contentDescription = if (isCompleted) "Undo" else "Complete",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Box(modifier = modifier.fillMaxWidth()) {
            val themeColors = LocalDailyWellColors.current
            val baseCardColor = if (isCompleted) {
                themeColors.surfaceSubtle
            } else {
                themeColors.glassBackground
            }
            val accentBase = when (timeOfDay) {
                TimeOfDay.MORNING -> Color(0xFF6AB5E0)
                TimeOfDay.AFTERNOON -> Color(0xFF57B89A)
                TimeOfDay.EVENING -> Color(0xFF9A86D3)
                TimeOfDay.ANYTIME -> Color(0xFFE29A6C)
            }
            val accentColor = if (isCompleted) {
                lerp(accentBase, Color(0xFF4A9E8F), 0.45f)
            } else {
                lerp(accentBase, habitColor, 0.35f)
            }
            val titleColor = if (isCompleted) Color(0xFF35534A) else Color(0xFF24423B)
            val thresholdText = if (isCompleted) Color(0xFF48685F) else Color(0xFF3A5B52)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pressScale)
                    .shadow(
                        elevation = if (isCompleted) 5.dp else 9.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = accentColor.copy(alpha = 0.22f),
                        ambientColor = accentColor.copy(alpha = 0.14f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(baseCardColor)
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = if (isCompleted) 0.4f else 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isPressed = true
                        onToggle(!isCompleted)
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left accent stripe - subtle color indicator
                    Box(
                        modifier = Modifier
                            .width(if (isCompleted) 6.dp else 5.dp)
                            .fillMaxHeight()
                            .background(accentColor)
                    )

                    // Main content
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Habit icon - clean, no extra effects
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PlatformHabitIcon(
                                habitId = habit.type,
                                size = 40.dp,
                                isCompleted = isCompleted,
                                streakDays = 0
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))

                        // Habit info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = titleColor.copy(alpha = titleAlpha),
                                modifier = Modifier.graphicsLayer {
                                    // Subtle strikethrough effect via scale
                                }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = habit.threshold,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = thresholdText,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Checkmark circle - the ONLY place with color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) {
                                        accentColor
                                    } else {
                                        Color.White.copy(alpha = 0.56f)
                                    }
                                )
                                .then(
                                    if (!isCompleted) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = accentColor.copy(alpha = 0.55f),
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Checkmark
                            if (isCompleted) {
                                Icon(
                                    imageVector = DailyWellIcons.Actions.Check,
                                    contentDescription = "Completed",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .scale(checkScale)
                                        .graphicsLayer {
                                            rotationZ = checkRotation
                                        },
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Completed state - subtle overlay
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.White.copy(alpha = 0.06f))
                    )
                }
            }

            // Mini confetti on completion
            ConfettiEffect(
                isActive = showMiniConfetti,
                particleCount = 20,
                colors = listOf(
                    habitColor,
                    Color(0xFFFFD700),
                    Color(0xFF4FC3F7),
                    Color(0xFFFF8A80)
                ),
                durationMs = 1200,
                modifier = Modifier.matchParentSize()
            )
        }
    }

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(80)
            isPressed = false
        }
    }
}
