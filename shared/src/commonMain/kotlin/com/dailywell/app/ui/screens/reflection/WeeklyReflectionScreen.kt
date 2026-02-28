package com.dailywell.app.ui.screens.reflection

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.Habit
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.ShimmerLoadingScreen
import org.koin.compose.viewmodel.koinViewModel

data class WeeklyReflectionData(
    val weekNumber: Int,
    val completionRate: Float,
    val bestHabit: Habit?,
    val bestHabitRate: Float,
    val focusHabit: Habit?,
    val focusHabitRate: Float,
    val perfectDays: Int,
    val totalDays: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReflectionScreen(
    reflectionData: WeeklyReflectionData,
    onDismiss: () -> Unit,
    onSaveReflection: (String, String, String) -> Unit,
    viewModel: WeeklyReflectionViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Completion celebration dialog
    if (uiState.showCompletionDialog) {
        ReflectionCompletionDialog(
            streak = uiState.reflectionStreak + 1, // +1 for the just-completed one
            onDismiss = { viewModel.dismissCompletionDialog() },
            onConfirm = {
                viewModel.completeReflection()
                onDismiss()
            }
        )
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Week ${uiState.weekNumber} Reflection",
                    subtitle = viewModel.getCurrentThemeName(),
                    onNavigationClick = onDismiss,
                    trailingActions = {
                        // Streak badge
                        if (uiState.reflectionStreak > 0) {
                            Surface(
                                color = Success.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = DailyWellIcons.Analytics.Streak,
                                        contentDescription = "Streak",
                                        modifier = Modifier.size(14.dp),
                                        tint = Success
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${uiState.reflectionStreak}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Success
                                    )
                                }
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                ShimmerLoadingScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    PremiumSectionChip(
                        text = "Reflection prompts",
                        icon = DailyWellIcons.Coaching.Reflection
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Theme progress indicators
                    ThemeProgressIndicator(
                        prompts = uiState.prompts,
                        currentIndex = uiState.currentPromptIndex,
                        onThemeClick = { viewModel.navigateToPrompt(it) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Current prompt content
                    AnimatedContent(
                        targetState = uiState.currentPromptIndex,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { promptIndex ->
                        val currentPrompt = uiState.prompts.getOrNull(promptIndex)

                        if (currentPrompt != null) {
                            ReflectionPromptContent(
                                promptWithResponse = currentPrompt,
                                onResponseChange = { viewModel.updateResponse(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigation buttons
                    NavigationButtons(
                        currentIndex = uiState.currentPromptIndex,
                        totalPrompts = uiState.prompts.size,
                        isCurrentAnswered = uiState.prompts.getOrNull(uiState.currentPromptIndex)?.isAnswered == true,
                        isSaving = uiState.isSaving,
                        allAnswered = uiState.prompts.all { it.isAnswered },
                        onPrevious = { viewModel.previousPrompt() },
                        onNext = {
                            viewModel.saveCurrentResponse()
                            if (uiState.currentPromptIndex < uiState.prompts.size - 1) {
                                viewModel.nextPrompt()
                            }
                        },
                        onComplete = {
                            viewModel.saveCurrentResponse()
                            viewModel.completeReflection()
                            // Collect the three main responses for backward compatibility
                            val wins = uiState.prompts.find { it.prompt.theme.name == "PROGRESS" }?.response ?: ""
                            val challenges = uiState.prompts.find { it.prompt.theme.name == "CHALLENGES" }?.response ?: ""
                            val intentions = uiState.prompts.find { it.prompt.theme.name == "FUTURE" }?.response ?: ""
                            onSaveReflection(wins, challenges, intentions)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeProgressIndicator(
    prompts: List<ReflectionPromptWithResponse>,
    currentIndex: Int,
    onThemeClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        prompts.forEachIndexed { index, prompt ->
            val isActive = index == currentIndex
            val isCompleted = prompt.isAnswered

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
                    .padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = when {
                            isCompleted -> Success.copy(alpha = 0.2f)
                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        onClick = { onThemeClick(index) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Success,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = getReflectionThemeIcon(prompt.themeInfo.displayName),
                                    contentDescription = prompt.themeInfo.displayName,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isActive) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = prompt.themeInfo.displayName.take(8),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ReflectionPromptContent(
    promptWithResponse: ReflectionPromptWithResponse,
    onResponseChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Theme icon (large)
        Icon(
            imageVector = getReflectionThemeIcon(promptWithResponse.themeInfo.displayName),
            contentDescription = promptWithResponse.themeInfo.displayName,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main question
        Text(
            text = promptWithResponse.prompt.question,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        // Sub-prompt if available
        promptWithResponse.prompt.subPrompt?.let { subPrompt ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Response input
        OutlinedTextField(
            value = promptWithResponse.response,
            onValueChange = { onResponseChange(it.take(1000)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = {
                Text(
                    text = "Take a moment to reflect...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(promptWithResponse.themeInfo.color.toInt()),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Character count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Difficulty badge
            Surface(
                color = when (promptWithResponse.prompt.difficulty) {
                    com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.EASY ->
                        Success.copy(alpha = 0.1f)
                    com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.MEDIUM ->
                        Color(0xFFFF9800).copy(alpha = 0.1f)
                    com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.DEEP ->
                        Color(0xFF9C27B0).copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when (promptWithResponse.prompt.difficulty) {
                        com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.EASY -> "Light"
                        com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.MEDIUM -> "Moderate"
                        com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.DEEP -> "Deep"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = when (promptWithResponse.prompt.difficulty) {
                        com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.EASY -> Success
                        com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.MEDIUM -> Color(0xFFFF9800)
                        com.dailywell.app.data.content.ReflectionPromptsDatabase.PromptDifficulty.DEEP -> Color(0xFF9C27B0)
                    }
                )
            }

            Text(
                text = "${promptWithResponse.response.length}/1000",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationButtons(
    currentIndex: Int,
    totalPrompts: Int,
    isCurrentAnswered: Boolean,
    isSaving: Boolean,
    allAnswered: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit
) {
    val isLast = currentIndex == totalPrompts - 1

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back button
        if (currentIndex > 0) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back")
            }
        }

        // Next/Complete button
        Button(
            onClick = {
                if (isLast && allAnswered) {
                    onComplete()
                } else {
                    onNext()
                }
            },
            modifier = Modifier.weight(if (currentIndex > 0) 1f else 2f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLast && allAnswered) Success else MaterialTheme.colorScheme.primary
            ),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                when {
                    isLast && allAnswered -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Complete Reflection")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = DailyWellIcons.Misc.Sparkle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    isLast -> Text("Finish")
                    else -> Text("Next")
                }
            }
        }
    }
}

@Composable
private fun ReflectionCompletionDialog(
    streak: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = DailyWellIcons.Social.Cheer,
                contentDescription = "Celebration",
                modifier = Modifier.size(48.dp),
                tint = Success
            )
        },
        title = {
            Text(
                text = "Reflection Complete!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "You've completed your weekly reflection.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (streak > 1) {
                    Surface(
                        color = Success.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = DailyWellIcons.Analytics.Streak,
                                contentDescription = "Streak",
                                modifier = Modifier.size(24.dp),
                                tint = Success
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "$streak Week Streak!",
                                    fontWeight = FontWeight.Bold,
                                    color = Success
                                )
                                Text(
                                    text = "Keep reflecting weekly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Success)
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Editing")
            }
        }
    )
}

// Legacy support for simple reflection screen without ViewModel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleWeeklyReflectionScreen(
    reflectionData: WeeklyReflectionData,
    onDismiss: () -> Unit,
    onSaveReflection: (String, String, String) -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    var wins by remember { mutableStateOf("") }
    var challenges by remember { mutableStateOf("") }
    var intentions by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            PremiumTopBar(
                title = "Week ${reflectionData.weekNumber} Reflection",
                subtitle = "Simple reflection flow",
                onNavigationClick = onDismiss
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(4.dp),
                            color = if (index <= currentPage) {
                                Success
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (currentPage) {
                0 -> WeekSummaryPage(reflectionData)
                1 -> ReflectionInputPage(
                    title = "What went well?",
                    subtitle = "Celebrate your wins this week",
                    icon = DailyWellIcons.Social.Cheer,
                    value = wins,
                    onValueChange = { wins = it },
                    placeholder = "I'm proud that I..."
                )
                2 -> ReflectionInputPage(
                    title = "What was challenging?",
                    subtitle = "Acknowledge your struggles",
                    icon = DailyWellIcons.Health.Workout,
                    value = challenges,
                    onValueChange = { challenges = it },
                    placeholder = "I found it hard to..."
                )
                3 -> ReflectionInputPage(
                    title = "Next week intentions",
                    subtitle = "Set your focus for the week ahead",
                    icon = DailyWellIcons.Habits.Intentions,
                    value = intentions,
                    onValueChange = { intentions = it },
                    placeholder = "This week I will..."
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back")
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < 3) {
                            currentPage++
                        } else {
                            onSaveReflection(wins, challenges, intentions)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Success
                    )
                ) {
                    Text(if (currentPage < 3) "Continue" else "Save Reflection")
                }
            }
        }
    }
}

@Composable
private fun WeekSummaryPage(data: WeeklyReflectionData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = DailyWellIcons.Analytics.BarChart,
            contentDescription = "Week review",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your Week in Review",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(data.completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
                Text(
                    text = "Overall Completion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Perfect days
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Misc.Sparkle,
                        contentDescription = "Perfect days",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${data.perfectDays}/${data.totalDays}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Perfect Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Best habit
            if (data.bestHabit != null) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.getHabitIcon(data.bestHabit.id),
                            contentDescription = data.bestHabit.name,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(data.bestHabitRate * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Best Habit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Focus area
        if (data.focusHabit != null && data.focusHabitRate < 0.7f) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DailyWellIcons.getHabitIcon(data.focusHabit.id),
                        contentDescription = data.focusHabit.name,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Focus Area",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${data.focusHabit.name} at ${(data.focusHabitRate * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReflectionInputPage(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(500)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(16.dp)
        )

        Text(
            text = "${value.length}/500",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp)
        )
    }
}

private fun getReflectionThemeIcon(themeName: String): ImageVector {
    return when (themeName.uppercase()) {
        "PROGRESS" -> DailyWellIcons.Analytics.TrendUp
        "CHALLENGES" -> DailyWellIcons.Gamification.Challenge
        "FUTURE", "INTENTIONS" -> DailyWellIcons.Habits.Intentions
        "GRATITUDE" -> DailyWellIcons.Health.Heart
        "GROWTH" -> DailyWellIcons.Onboarding.Philosophy
        "PATTERNS" -> DailyWellIcons.Analytics.Pattern
        "CELEBRATE", "CELEBRATION" -> DailyWellIcons.Social.Cheer
        "MINDSET" -> DailyWellIcons.Coaching.AICoach
        else -> DailyWellIcons.Coaching.Reflection
    }
}
