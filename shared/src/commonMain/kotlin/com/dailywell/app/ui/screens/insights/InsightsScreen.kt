package com.dailywell.app.ui.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.core.theme.Warning
import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.StreakInfo
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onUpgradeClick: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    viewModel: InsightsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show achievement celebration dialog
    uiState.recentAchievement?.let { achievement ->
        AchievementCelebrationDialog(
            achievement = achievement,
            onDismiss = { viewModel.dismissRecentAchievement() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // "Patterns" - discovery-focused, less clinical than "Insights"
                        text = "Patterns",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (!uiState.isPremium) {
            PremiumLockedView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onUpgradeClick = onUpgradeClick
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streak overview
                item {
                    StreakOverviewCard(streakInfo = uiState.streakInfo)
                }

                // Milestones section - "Milestones" is journey language, less pressure than "Achievements"
                item {
                    Text(
                        text = "MILESTONES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    AchievementsCard(
                        achievements = uiState.achievements,
                        unlockedCount = uiState.unlockedAchievements.size,
                        totalCount = uiState.achievements.size,
                        onClick = onNavigateToAchievements
                    )
                }

                // Habit performance
                item {
                    Text(
                        text = "HABIT PERFORMANCE (30 DAYS)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            uiState.habitInsights.forEachIndexed { index, insight ->
                                HabitPerformanceRowWithTrend(insight = insight)
                                if (index < uiState.habitInsights.lastIndex) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }

                // Connections - "Connections" feels more human than "Correlations"
                if (uiState.correlations.isNotEmpty()) {
                    item {
                        Text(
                            text = "HABIT CONNECTIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        CorrelationsCard(correlations = uiState.correlations)
                    }
                }

                // Your Story - personal, narrative-focused
                item {
                    Text(
                        text = "YOUR STORY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    InsightCard(
                        bestHabit = uiState.bestHabit,
                        focusHabit = uiState.focusHabit,
                        rates = uiState.habitCompletionRates,
                        overallConsistency = uiState.overallConsistency
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AchievementCelebrationDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Awesome!")
            }
        },
        icon = {
            Text(text = achievement.emoji, fontSize = 48.sp)
        },
        title = {
            Text(
                text = "Achievement Unlocked!",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
private fun AchievementsCard(
    achievements: List<Achievement>,
    unlockedCount: Int,
    totalCount: Int,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$unlockedCount / $totalCount Unlocked",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f },
                        modifier = Modifier
                            .width(80.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Success,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "→",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements.take(10)) { achievement ->
                    AchievementBadge(achievement = achievement)
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .alpha(if (achievement.isUnlocked) 1f else 0.4f)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (achievement.isUnlocked)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (achievement.isUnlocked) achievement.emoji else "🔒",
                fontSize = 24.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = achievement.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun HabitPerformanceRowWithTrend(insight: HabitInsight) {
    val percentage = (insight.rate30Day * 100).toInt()
    val color = when {
        percentage >= 80 -> Success
        percentage >= 50 -> Warning
        else -> MaterialTheme.colorScheme.error
    }

    val trendEmoji = when (insight.trend) {
        Trend.IMPROVING -> "📈"
        Trend.DECLINING -> "📉"
        Trend.STABLE -> "➡️"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = insight.habit.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = insight.habit.name,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = trendEmoji, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { insight.rate30Day },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun CorrelationsCard(correlations: List<HabitCorrelation>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            correlations.forEachIndexed { index, correlation ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🔗", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${correlation.habit1.emoji} ${correlation.habit1.name} + ${correlation.habit2.emoji} ${correlation.habit2.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = correlation.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < correlations.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PremiumLockedView(
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🔒", fontSize = 64.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            // More inviting language - discovery vs locked feature
            text = "Discover Your Patterns",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            // Benefit-focused, personal language
            text = "See how your habits connect and what's working for you",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PremiumFeature(emoji = "📊", text = "30-day habit performance")
                PremiumFeature(emoji = "🔗", text = "Habit correlations")
                PremiumFeature(emoji = "📈", text = "Weekly & monthly trends")
                PremiumFeature(emoji = "🏆", text = "Achievement badges")
                PremiumFeature(emoji = "📝", text = "Weekly reflections")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onUpgradeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Success
            )
        ) {
            Text(
                text = "Upgrade to Premium",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$2.99/month or $19.99 lifetime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PremiumFeature(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun StreakOverviewCard(streakInfo: StreakInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StreakStat(
                emoji = "🔥",
                value = streakInfo.currentStreak.toString(),
                label = "Current Streak"
            )
            StreakStat(
                emoji = "🏆",
                value = streakInfo.longestStreak.toString(),
                label = "Longest Streak"
            )
        }
    }
}

@Composable
private fun StreakStat(
    emoji: String,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
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
private fun InsightCard(
    bestHabit: Habit?,
    focusHabit: Habit?,
    rates: Map<String, Float>,
    overallConsistency: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (bestHabit != null) {
                val bestRate = ((rates[bestHabit.id] ?: 0f) * 100).toInt()
                InsightRow(
                    emoji = "⭐",
                    title = "Best habit",
                    content = "${bestHabit.emoji} ${bestHabit.name} at $bestRate%"
                )
            }

            if (focusHabit != null && focusHabit != bestHabit) {
                Spacer(modifier = Modifier.height(16.dp))
                val focusRate = ((rates[focusHabit.id] ?: 0f) * 100).toInt()
                InsightRow(
                    emoji = "💪",
                    title = "Focus area",
                    content = "${focusHabit.emoji} ${focusHabit.name} at $focusRate%"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val avgRate = (overallConsistency * 100).toInt()

            InsightRow(
                emoji = "📊",
                title = "Overall consistency",
                content = "$avgRate% average across all habits"
            )
        }
    }
}

@Composable
private fun InsightRow(
    emoji: String,
    title: String,
    content: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
