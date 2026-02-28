package com.dailywell.app.ui.screens.reflections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.StaggeredItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionsScreen(
    viewModel: ReflectionsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Weekly Reflections",
                    subtitle = if (uiState.weekNumber > 0) "Week ${uiState.weekNumber}" else null,
                    onNavigationClick = {
                        viewModel.saveAll()
                        onNavigateBack()
                    }
                )
            }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Stats row
                item {
                    StaggeredItem(index = 0) {
                        ReflectionStatsRow(
                            streak = uiState.reflectionStats?.reflectionStreak ?: 0,
                            totalReflections = uiState.reflectionStats?.totalReflections ?: 0,
                            totalWords = uiState.reflectionStats?.totalWords ?: 0
                        )
                    }
                }

                // Week completion indicator
                if (uiState.isWeekComplete) {
                    item {
                        StaggeredItem(index = 1) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = ElevationLevel.Subtle,
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Success.copy(alpha = 0.08f))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Success,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "This week's reflection is complete!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Success
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    StaggeredItem(index = 2) {
                        PremiumSectionChip(
                            text = "Weekly prompts",
                            icon = DailyWellIcons.Coaching.Reflection
                        )
                    }
                }

                // Prompt cards (collapsed, tap to expand)
                itemsIndexed(uiState.promptCards) { index, card ->
                    StaggeredItem(index = index + 3) {
                        ExpandablePromptCard(
                            card = card,
                            isExpanded = uiState.expandedPromptId == card.prompt.id,
                            onToggle = { viewModel.toggleExpand(card.prompt.id) },
                            onResponseChange = { viewModel.updateResponse(card.prompt.id, it) }
                        )
                    }
                }

                // Save button
                if (uiState.promptCards.any { it.response.isNotBlank() } && !uiState.isWeekComplete) {
                    item {
                        val answeredCount = uiState.promptCards.count { it.isAnswered }
                        val totalCount = uiState.promptCards.size

                        Button(
                            onClick = { viewModel.saveAll() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            enabled = !uiState.isSaving,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (answeredCount == totalCount) Success
                                else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = if (answeredCount == totalCount) "Complete Reflection"
                                    else "Save Progress ($answeredCount/$totalCount)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
    }
}

@Composable
private fun ReflectionStatsRow(
    streak: Int,
    totalReflections: Int,
    totalWords: Int
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Medium,
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = DailyWellIcons.Analytics.Streak,
                value = if (streak > 0) "${streak}w" else "\u2014",
                label = "Streak"
            )
            StatItem(
                icon = DailyWellIcons.Coaching.Reflection,
                value = "$totalReflections",
                label = "Reflections"
            )
            StatItem(
                icon = DailyWellIcons.Coaching.Reflection,
                value = if (totalWords > 0) formatWordCount(totalWords) else "\u2014",
                label = "Words"
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExpandablePromptCard(
    card: PromptCardState,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onResponseChange: (String) -> Unit
) {
    val themeColor = Color(card.themeInfo.color.toInt())

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = if (isExpanded) ElevationLevel.Medium else ElevationLevel.Subtle,
        cornerRadius = 18.dp,
        enablePressScale = true,
        onClick = onToggle
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Collapsed header (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(themeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (card.isAnswered) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Answered",
                            tint = Success,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = getThemeIcon(card.themeInfo.displayName),
                            contentDescription = card.themeInfo.displayName,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.themeInfo.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = card.prompt.question,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2
                    )
                }

                Icon(
                    imageVector = if (isExpanded) DailyWellIcons.Nav.ExpandLess
                    else DailyWellIcons.Nav.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded text input
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    card.prompt.subPrompt?.let { subPrompt ->
                        Text(
                            text = subPrompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = card.response,
                        onValueChange = { onResponseChange(it.take(1000)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = {
                            Text(
                                text = "Take a moment to reflect...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Text(
                        text = "${card.response.length}/1000",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

private fun getThemeIcon(themeName: String): ImageVector {
    return when (themeName.uppercase()) {
        "PROGRESS" -> DailyWellIcons.Analytics.TrendUp
        "CHALLENGES" -> DailyWellIcons.Gamification.Challenge
        "FUTURE", "INTENTIONS" -> DailyWellIcons.Habits.Intentions
        "GRATITUDE" -> DailyWellIcons.Health.Heart
        "GROWTH", "SELF DISCOVERY", "SELF_DISCOVERY" -> DailyWellIcons.Onboarding.Philosophy
        "PATTERNS" -> DailyWellIcons.Analytics.Pattern
        "RELATIONSHIPS" -> DailyWellIcons.Social.Cheer
        else -> DailyWellIcons.Coaching.Reflection
    }
}

private fun formatWordCount(count: Int): String {
    return when {
        count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}k"
        else -> "$count"
    }
}
