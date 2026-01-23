package com.dailywell.app.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.Success
import com.dailywell.app.domain.model.HabitType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                0 -> WelcomePage(onNext = { viewModel.nextPage() })
                1 -> PhilosophyPage(onNext = { viewModel.nextPage() })
                2 -> HabitSelectionPage(
                    selectedHabits = uiState.selectedHabitIds,
                    allHabits = uiState.allHabits,
                    maxHabits = uiState.maxFreeHabits,
                    onToggleHabit = { viewModel.toggleHabit(it) },
                    onNext = { viewModel.nextPage() },
                    canProceed = viewModel.canProceedFromHabitSelection()
                )
                3 -> ReminderPage(
                    hour = uiState.reminderHour,
                    minute = uiState.reminderMinute,
                    onTimeChange = { h, m -> viewModel.setReminderTime(h, m) },
                    onNext = { viewModel.nextPage() }
                )
                4 -> ConsistencyPage(onNext = { viewModel.nextPage() })
                5 -> FirstWinPage(
                    isCelebrating = uiState.firstWinCelebrating,
                    isCompleted = uiState.firstWinCompleted,
                    onCompleteFirstWin = { viewModel.completeFirstWin() },
                    onNext = { viewModel.nextPage() }
                )
                6 -> ReadyPage(
                    selectedCount = uiState.selectedHabitIds.size,
                    isLoading = uiState.isCompleting,
                    onComplete = { viewModel.completeOnboarding(onComplete) }
                )
            }
        }

        // Page indicator
        if (uiState.currentPage > 0 && uiState.currentPage < uiState.totalPages - 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(uiState.totalPages) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (index == uiState.currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == uiState.currentPage) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )

        Text(
            text = "🌱",
            fontSize = 80.sp,
            modifier = Modifier.scale(scale)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Health doesn't have\nto be complicated.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to DailyWell",
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
                // "Let's Go" - energetic, inviting, creates excitement
                text = "Let's Go",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PhilosophyPage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "We believe in",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ENOUGH",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = Primary
        )

        Text(
            text = "not PERFECT",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                PhilosophyPoint(
                    text = "You don't need to track every calorie."
                )
                Spacer(modifier = Modifier.height(12.dp))
                PhilosophyPoint(
                    text = "You don't need perfect sleep scores."
                )
                Spacer(modifier = Modifier.height(12.dp))
                PhilosophyPoint(
                    text = "You need simple habits, done consistently."
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                // "I Like This" - agreement, emotional buy-in, personal
                text = "I Like This",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PhilosophyPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "→",
            style = MaterialTheme.typography.bodyLarge,
            color = Primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HabitSelectionPage(
    selectedHabits: Set<String>,
    allHabits: List<HabitType>,
    maxHabits: Int,
    onToggleHabit: (String) -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Choose Your Habits",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start with 1-$maxHabits habits. Add more later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${selectedHabits.size} / $maxHabits selected",
            style = MaterialTheme.typography.labelLarge,
            color = if (selectedHabits.size == maxHabits) Success else Primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(allHabits) { habit ->
                HabitSelectionCard(
                    habit = habit,
                    isSelected = selectedHabits.contains(habit.id),
                    isDisabled = !selectedHabits.contains(habit.id) && selectedHabits.size >= maxHabits,
                    onToggle = { onToggleHabit(habit.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                // "These Are Mine" - ownership, commitment, personal choice
                text = "These Are Mine",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun HabitSelectionCard(
    habit: HabitType,
    isSelected: Boolean,
    isDisabled: Boolean,
    onToggle: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> habit.color.copy(alpha = 0.15f)
            isDisabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.surface
        }
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> habit.color
            else -> Color.Transparent
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp))
                } else Modifier
            )
            .clickable(enabled = !isDisabled) { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = habit.emoji,
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = habit.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isDisabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (isSelected) {
                Text(
                    text = "✓",
                    color = habit.color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ReminderPage(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⏰",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "When should we check in?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Evening is recommended - reflect on your day",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Time picker simplified - just show common times
        val timeOptions = listOf(
            7 to 0, 8 to 0, 12 to 0, 18 to 0, 19 to 0, 20 to 0, 21 to 0, 22 to 0
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            timeOptions.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (h, m) ->
                        val isSelected = hour == h && minute == m
                        val timeStr = String.format("%02d:%02d", h, m)
                        val period = if (h < 12) "AM" else "PM"
                        val displayHour = if (h == 0) 12 else if (h > 12) h - 12 else h

                        FilterChip(
                            selected = isSelected,
                            onClick = { onTimeChange(h, m) },
                            label = {
                                Text("$displayHour:${String.format("%02d", m)} $period")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                // "Perfect" - confirms their choice, control, satisfaction
                text = "Perfect",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ConsistencyPage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "💪",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Remember",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Consistency > Perfection",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Success.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConsistencyPoint(emoji = "✓", text = "5 out of 7 days is a WIN")
                Spacer(modifier = Modifier.height(12.dp))
                ConsistencyPoint(emoji = "✓", text = "Better than last week is a WIN")
                Spacer(modifier = Modifier.height(12.dp))
                ConsistencyPoint(emoji = "✓", text = "Any improvement is a WIN")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                // "I'm In" - commitment, belonging, team mentality
                text = "I'm In",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ConsistencyPoint(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            color = Success
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * FIRST WIN PAGE - Immediate dopamine hit!
 * Psychology: Completing a task during onboarding creates:
 * 1. Instant gratification / dopamine release
 * 2. Psychological commitment (they've already "done" something)
 * 3. Proof that the app works
 * 4. Momentum for continued engagement
 */
@Composable
private fun FirstWinPage(
    isCelebrating: Boolean,
    isCompleted: Boolean,
    onCompleteFirstWin: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isCelebrating || isCompleted) {
            // CELEBRATION STATE
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Text(
                text = "🎉",
                fontSize = 100.sp,
                modifier = Modifier.scale(if (isCelebrating) scale else 1f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FIRST WIN!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Success
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You just did it! That feeling?\nThat's the start of something amazing.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // XP earned animation
            Card(
                modifier = Modifier.fillMaxWidth(0.6f),
                colors = CardDefaults.cardColors(
                    containerColor = Success.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⭐", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+50 XP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (isCompleted) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success)
                ) {
                    Text(
                        text = "Keep Going!",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        } else {
            // CHALLENGE STATE - Show the first win challenge
            Text(
                text = "💧",
                fontSize = 80.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your First Win",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Let's start with something simple.\nRight now.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Primary.copy(alpha = 0.1f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Drink a glass of water",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Go ahead, we'll wait! Stay hydrated.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "(It takes 30 seconds)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Why this matters
            Text(
                text = "\"A journey of a thousand miles\nbegins with a single step.\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onCompleteFirstWin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "I Did It! 💧",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Skip option (for those who really don't want to)
            TextButton(onClick = onNext) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ReadyPage(
    selectedCount: Int,
    isLoading: Boolean,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            )
        )

        Text(
            text = "🎉",
            fontSize = 80.sp,
            modifier = Modifier.scale(scale)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You're all set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$selectedCount habit${if (selectedCount != 1) "s" else ""} ready to track",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Primary.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your journey starts now",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Small steps, big changes.\nLet's build habits that last.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onComplete,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Success
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    // "Begin" - simple, powerful, action-oriented (final commitment)
                    text = "Begin",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
