package com.dailywell.app.ui.screens.recovery

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.PrimaryLight
import com.dailywell.app.core.theme.Secondary
import com.dailywell.app.core.theme.SecondaryLight
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecoveryScreen(
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: RecoveryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
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
                            Text("Not now", color = Color.Gray)
                        }
                    } else {
                        TextButton(onClick = { viewModel.goBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
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
                            containerColor = Secondary
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
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
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
                                isCompleted -> Secondary
                                isCurrent -> PrimaryLight
                                else -> Color.LightGray
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            color = if (isCurrent) Secondary else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = phase.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent || isCompleted) Secondary else Color.Gray,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (index < phases.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(2.dp)
                        .background(if (isCompleted) Secondary else Color.LightGray)
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
                color = Color.Gray
            )
        }

        if (previousStreak > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryLight.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 28.sp
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
                                color = Color.Gray
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
                        Secondary.copy(alpha = 0.1f)
                    else
                        Color.LightGray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected)
                    CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Secondary)
                    )
                else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reason.emoji,
                        fontSize = 24.sp
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
                                color = Secondary
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Secondary
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
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Show the normalizing message again
        if (selectedReason != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF8E1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "💡", fontSize = 20.sp)
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
            color = Color.Gray
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
            color = Color.Gray
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
                        Secondary.copy(alpha = 0.1f)
                    else
                        Color.LightGray.copy(alpha = 0.3f)
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
                            selectedColor = Secondary
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
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = PrimaryLight.copy(alpha = 0.3f)
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
        Text(
            text = "🎉",
            fontSize = 64.sp
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
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Secondary.copy(alpha = 0.1f)
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
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Resilience",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
                Text(
                    text = "Coming back is harder than staying. You did it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
