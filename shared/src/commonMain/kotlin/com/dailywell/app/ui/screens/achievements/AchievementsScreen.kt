package com.dailywell.app.ui.screens.achievements

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.AchievementCategory
import com.dailywell.app.data.model.Achievements

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    unlockedAchievementIds: Set<String>,
    onBack: () -> Unit
) {
    val allAchievements = Achievements.all
    val unlockedCount = unlockedAchievementIds.size
    val totalCount = allAchievements.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            // "Milestones" - journey language, celebrates progress not competition
                            text = "Milestones",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$unlockedCount / $totalCount reached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress overview
            item {
                AchievementProgressCard(
                    unlockedCount = unlockedCount,
                    totalCount = totalCount
                )
            }

            // Streak achievements
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CategoryHeader(title = "STREAK MILESTONES", emoji = "🔥")
            }
            items(allAchievements.filter { it.category == AchievementCategory.STREAK }) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    isUnlocked = unlockedAchievementIds.contains(achievement.id)
                )
            }

            // Consistency achievements
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CategoryHeader(title = "CONSISTENCY", emoji = "✨")
            }
            items(allAchievements.filter { it.category == AchievementCategory.CONSISTENCY }) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    isUnlocked = unlockedAchievementIds.contains(achievement.id)
                )
            }

            // Habit achievements
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CategoryHeader(title = "HABIT MASTERY", emoji = "🏆")
            }
            items(allAchievements.filter { it.category == AchievementCategory.HABIT }) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    isUnlocked = unlockedAchievementIds.contains(achievement.id)
                )
            }

            // Special achievements
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CategoryHeader(title = "SPECIAL", emoji = "⭐")
            }
            items(allAchievements.filter { it.category == AchievementCategory.SPECIAL }) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    isUnlocked = unlockedAchievementIds.contains(achievement.id)
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AchievementProgressCard(
    unlockedCount: Int,
    totalCount: Int
) {
    val progress = unlockedCount.toFloat() / totalCount.toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "🏅", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                // "Your Journey" - personal, narrative-focused
                text = "Your Journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Success,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryHeader(title: String, emoji: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    isUnlocked: Boolean
) {
    val alpha by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.5f,
        animationSpec = spring()
    )
    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.95f,
        animationSpec = spring()
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isUnlocked) {
            Success.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) {
                            Success.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) achievement.emoji else "🔒",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isUnlocked) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isUnlocked) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isUnlocked) {
                Text(
                    text = "✓",
                    fontSize = 24.sp,
                    color = Success,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
