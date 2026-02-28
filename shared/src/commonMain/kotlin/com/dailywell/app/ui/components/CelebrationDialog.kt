package com.dailywell.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailywell.app.core.theme.Success
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.HabitStack
import com.dailywell.app.data.model.StreakMilestone

@Composable
fun CelebrationDialog(
    message: String,
    completedCount: Int,
    totalCount: Int,
    onDismiss: () -> Unit
) {
    val isAllComplete = completedCount == totalCount && totalCount > 0

    Dialog(onDismissRequest = onDismiss) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                // E1: Confetti inside dialog when all habits complete
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    // Emoji animation
                    val infiniteTransition = rememberInfiniteTransition()
                    val bounce by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Icon(
                        imageVector = if (isAllComplete) DailyWellIcons.Social.Cheer else DailyWellIcons.Health.Workout,
                        contentDescription = if (isAllComplete) "Celebration" else "Keep going",
                        modifier = Modifier.size(64.dp).scale(bounce),
                        tint = if (isAllComplete) com.dailywell.app.core.theme.Success else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Completion count
                    Text(
                        text = "$completedCount / $totalCount",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllComplete) Success else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAllComplete) Success else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Continue",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                    }

                    // E1: Confetti inside dialog when all habits complete
                    if (isAllComplete) {
                        ConfettiEffect(
                            isActive = true,
                            modifier = Modifier.matchParentSize(),
                            particleCount = 30,
                            durationMs = 3000
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakMilestoneDialog(
    milestone: StreakMilestone,
    streak: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                )
            ) + fadeIn()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Success.copy(alpha = 0.1f)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Icon(
                        imageVector = DailyWellIcons.Gamification.Trophy,
                        contentDescription = milestone.title,
                        modifier = Modifier.size(72.dp).scale(scale),
                        tint = com.dailywell.app.core.theme.Success
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = milestone.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Analytics.Streak,
                            contentDescription = "Streak",
                            modifier = Modifier.size(20.dp),
                            tint = com.dailywell.app.core.theme.Success
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$streak day streak!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = milestone.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Amazing!",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * HABIT STACKING (Task #9): Nudge Dialog
 * "After I [X], I will [Y]" - 3.2x higher success rate (James Clear, Atomic Habits)
 *
 * This dialog appears when a user completes a trigger habit that has a linked target habit.
 * It encourages them to continue their chain for maximum habit formation.
 */
@Composable
fun HabitStackNudgeDialog(
    triggerHabit: Habit?,
    targetHabit: Habit,
    stack: HabitStack?,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    Dialog(onDismissRequest = onSkip) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Chain emoji animation
                    val infiniteTransition = rememberInfiniteTransition()
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = -5f,
                        targetValue = 5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Completed habit
                        if (triggerHabit != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // E2: Breathing animation on habit icons
                                Icon(
                                    imageVector = DailyWellIcons.getHabitIcon(triggerHabit.type),
                                    contentDescription = triggerHabit.name,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .breathingAnimation(minScale = 1f, maxScale = 1.06f, durationMs = 2000),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Done!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Success
                                )
                            }
                        }

                        // Chain link
                        Icon(
                            imageVector = DailyWellIcons.Habits.HabitStacking,
                            contentDescription = "Chain",
                            modifier = Modifier.size(32.dp).padding(horizontal = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        // Next habit - E2: Breathing animation
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = DailyWellIcons.getHabitIcon(targetHabit.type),
                                contentDescription = targetHabit.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .breathingAnimation(minScale = 1f, maxScale = 1.06f, durationMs = 2000),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Next up!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = "Keep the Chain Going!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stack description
                    if (stack != null && triggerHabit != null) {
                        Text(
                            text = stack.getStackDescription(triggerHabit.name, targetHabit.name),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Science note
                    Text(
                        text = "Stacked habits are 3.2x more likely to stick!",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Success
                    )

                    // Completion count if any
                    if (stack != null && stack.completionCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You've completed this chain ${stack.completionCount} times!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Skip button
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Maybe Later")
                        }

                        // Complete button
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Success
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Do It!")
                        }
                    }
                }
            }
        }
    }
}
