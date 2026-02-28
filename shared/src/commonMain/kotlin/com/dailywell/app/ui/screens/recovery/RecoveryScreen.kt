package com.dailywell.app.ui.screens.recovery

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecoveryScreen(
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: RecoveryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Recovery Reset",
                    subtitle = "Rebuild momentum after a miss",
                    onNavigationClick = onDismiss
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.16f))
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.9f)
                        .align(Alignment.Center),
                    elevation = ElevationLevel.Prominent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        PremiumSectionChip(
                            text = "Recovery flow",
                            icon = DailyWellIcons.Social.Cheer
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Reset with compassion, then recommit with a clear next step.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Progress indicator
                        RecoveryProgressBar(currentPhase = uiState.currentPhase)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Phase content
                        AnimatedContent(
                            targetState = uiState.currentPhase,
                            transitionSpec = {
                                slideInHorizontally { it } + fadeIn() togetherWith
                                        slideOutHorizontally { -it } + fadeOut()
                            },
                            modifier = Modifier.weight(1f)
                        ) { phase ->
                            when (phase) {
                                RecoveryPhase.ACKNOWLEDGE -> AcknowledgePhase(
                                    previousStreak = uiState.previousStreak,
                                    onSelectReason = { viewModel.selectReason(it) },
                                    selectedReason = uiState.selectedReason
                                )
                                RecoveryPhase.REFLECT -> ReflectPhase(
                                    selectedReason = uiState.selectedReason,
                                    reflectionAnswer = uiState.reflectionAnswer,
                                    onReflectionChange = { viewModel.setReflection(it) }
                                )
                                RecoveryPhase.RECOMMIT -> RecommitPhase(
                                    selectedCommitment = uiState.commitmentLevel,
                                    onSelectCommitment = { viewModel.setCommitmentLevel(it) }
                                )
                                RecoveryPhase.CELEBRATE -> CelebratePhase()
                                RecoveryPhase.NONE -> Box(modifier = Modifier.fillMaxSize())
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Navigation buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (uiState.currentPhase == RecoveryPhase.ACKNOWLEDGE) {
                                TextButton(onClick = onDismiss) {
                                    Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                TextButton(onClick = { viewModel.goBack() }) {
                                    Icon(DailyWellIcons.Nav.Back, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Back")
                                }
                            }

                            Button(
                                onClick = {
                                    if (uiState.currentPhase == RecoveryPhase.CELEBRATE) {
                                        viewModel.completeRecovery()
                                        onComplete()
                                    } else {
                                        viewModel.advancePhase()
                                    }
                                },
                                enabled = viewModel.canAdvance(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    if (uiState.currentPhase == RecoveryPhase.CELEBRATE)
                                        "Back to Habits"
                                    else
                                        "Continue"
                                )
                                if (uiState.currentPhase != RecoveryPhase.CELEBRATE) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(DailyWellIcons.Nav.ArrowForward, contentDescription = null)
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
private fun RecoveryProgressBar(currentPhase: RecoveryPhase) {
    val phases = listOf(
        RecoveryPhase.ACKNOWLEDGE,
        RecoveryPhase.REFLECT,
        RecoveryPhase.RECOMMIT,
        RecoveryPhase.CELEBRATE
    )
    val currentIndex = phases.indexOf(currentPhase)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        phases.forEachIndexed { index, phase ->
            val isCompleted = index < currentIndex
            val isCurrent = index == currentIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            DailyWellIcons.Actions.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phase.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent || isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (index < phases.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

@Composable
private fun AcknowledgePhase(
    previousStreak: Int,
    selectedReason: StreakBreakReason?,
    onSelectReason: (StreakBreakReason) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Let's talk about it",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = RecoveryMessages.getAcknowledgmentMessage(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (previousStreak > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Analytics.Streak,
                            contentDescription = "Streak",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Your $previousStreak day streak",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "That progress still counts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "What happened?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(StreakBreakReason.entries.toList()) { reason ->
            val isSelected = selectedReason == reason
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectReason(reason) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected)
                    CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getStreakBreakReasonIcon(reason),
                        contentDescription = reason.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reason.label,
                            fontWeight = FontWeight.Medium
                        )
                        if (isSelected) {
                            Text(
                                text = reason.normalizer,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            DailyWellIcons.Actions.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReflectPhase(
    selectedReason: StreakBreakReason?,
    reflectionAnswer: String,
    onReflectionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Reflect & Learn",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = RecoveryMessages.getReflectionPrompt(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Show the normalizing message again
        if (selectedReason != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Onboarding.Philosophy,
                        contentDescription = "Insight",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedReason.normalizer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Quick thought (optional):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = reflectionAnswer,
            onValueChange = onReflectionChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("What would help next time?") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tip: Even thinking about this question builds self-awareness.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecommitPhase(
    selectedCommitment: CommitmentLevel,
    onSelectCommitment: (CommitmentLevel) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Set Your Path Forward",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = RecoveryMessages.getRecommitmentMessage(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "How would you like to continue?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(16.dp))

        CommitmentLevel.entries.forEach { level ->
            val isSelected = selectedCommitment == level
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onSelectCommitment(level) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectCommitment(level) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = level.label,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = level.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "\"The goal isn't to be perfect. It's to never give up.\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun CelebratePhase() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = DailyWellIcons.Social.Cheer,
            contentDescription = "Celebration",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "You're Back!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = RecoveryMessages.getCelebrationMessage(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your superpower:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resilience",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Coming back is harder than staying. You did it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Maps StreakBreakReason enum values to appropriate Material Icons
 */
private fun getStreakBreakReasonIcon(reason: StreakBreakReason): androidx.compose.ui.graphics.vector.ImageVector {
    return when (reason) {
        StreakBreakReason.BUSY_DAY -> DailyWellIcons.Habits.Move
        StreakBreakReason.FORGOT -> DailyWellIcons.Coaching.Reflection
        StreakBreakReason.SICK -> DailyWellIcons.Health.Temperature
        StreakBreakReason.TRAVELING -> DailyWellIcons.Misc.Location
        StreakBreakReason.OVERWHELMED -> DailyWellIcons.Mood.Struggling
        StreakBreakReason.LOW_ENERGY -> DailyWellIcons.Gamification.XP
        StreakBreakReason.SOCIAL -> DailyWellIcons.Social.People
        StreakBreakReason.OTHER -> DailyWellIcons.Misc.Sparkle
    }
}
