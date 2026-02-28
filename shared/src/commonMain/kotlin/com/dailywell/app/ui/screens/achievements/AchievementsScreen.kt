package com.dailywell.app.ui.screens.achievements

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.window.Dialog
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.AchievementCategory
import com.dailywell.app.data.repository.AchievementCelebration
import com.dailywell.app.data.repository.AchievementWithProgress
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PlatformAchievementBadge
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.ShimmerLoadingScreen
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import org.koin.compose.viewmodel.koinViewModel

/**
 * AchievementsScreen - Production-Ready Achievement Display
 *
 * Features:
 * - 75 creative achievements across 4 categories
 * - Real-time progress tracking
 * - Category filtering
 * - Celebration dialogs for newly unlocked
 * - Firebase-synced data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    onShareAchievement: (String, String) -> Unit = { _, _ -> },
    viewModel: AchievementsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle celebration dialog
    uiState.pendingCelebration?.let { celebration ->
        AchievementCelebrationDialog(
            celebration = celebration,
            onDismiss = { viewModel.dismissCelebration() },
            onShare = { name, desc -> onShareAchievement(name, desc) }
        )
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Milestones",
                    subtitle = "${uiState.unlockedCount} / ${uiState.totalCount} reached",
                    onNavigationClick = onBack
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Progress overview card
                    item {
                        AchievementProgressCard(
                            unlockedCount = uiState.unlockedCount,
                            totalCount = uiState.totalCount,
                            progressPercent = uiState.progressPercent
                        )
                    }

                    // Next achievements to unlock
                    if (uiState.nextAchievements.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            PremiumSectionChip(
                                text = "Up Next",
                                icon = DailyWellIcons.Habits.Intentions
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(uiState.nextAchievements) { achievementWithProgress ->
                            NextAchievementCard(achievementWithProgress = achievementWithProgress)
                        }
                    }

                    // Category filter chips
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryFilterRow(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.selectCategory(it) },
                            viewModel = viewModel
                        )
                    }

                    // Streak achievements
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryHeader(
                            title = "Streak Milestones",
                            icon = DailyWellIcons.Analytics.Streak,
                            progress = viewModel.getCategoryProgress(AchievementCategory.STREAK)
                        )
                    }
                    items(
                        uiState.filteredAchievements.filter {
                            it.achievement.category == AchievementCategory.STREAK
                        }
                    ) { achievementWithProgress ->
                        AchievementCard(achievementWithProgress = achievementWithProgress)
                    }

                    // Habit Mastery achievements
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryHeader(
                            title = "Habit Mastery",
                            icon = DailyWellIcons.Gamification.Trophy,
                            progress = viewModel.getCategoryProgress(AchievementCategory.HABIT)
                        )
                    }
                    items(
                        uiState.filteredAchievements.filter {
                            it.achievement.category == AchievementCategory.HABIT
                        }
                    ) { achievementWithProgress ->
                        AchievementCard(achievementWithProgress = achievementWithProgress)
                    }

                    // Consistency achievements
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryHeader(
                            title = "Consistency",
                            icon = DailyWellIcons.Misc.Sparkle,
                            progress = viewModel.getCategoryProgress(AchievementCategory.CONSISTENCY)
                        )
                    }
                    items(
                        uiState.filteredAchievements.filter {
                            it.achievement.category == AchievementCategory.CONSISTENCY
                        }
                    ) { achievementWithProgress ->
                        AchievementCard(achievementWithProgress = achievementWithProgress)
                    }

                    // Special achievements
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        CategoryHeader(
                            title = "Special",
                            icon = DailyWellIcons.Status.Star,
                            progress = viewModel.getCategoryProgress(AchievementCategory.SPECIAL)
                        )
                    }
                    items(
                        uiState.filteredAchievements.filter {
                            it.achievement.category == AchievementCategory.SPECIAL
                        }
                    ) { achievementWithProgress ->
                        AchievementCard(achievementWithProgress = achievementWithProgress)
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AchievementProgressCard(
    unlockedCount: Int,
    totalCount: Int,
    progressPercent: Float
) {
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
            Icon(
                imageVector = DailyWellIcons.Gamification.Medal,
                contentDescription = "Achievements",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(progressPercent * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your Journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$unlockedCount of $totalCount milestones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progressPercent },
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
private fun CategoryFilterRow(
    selectedCategory: AchievementCategory?,
    onCategorySelected: (AchievementCategory?) -> Unit,
    viewModel: AchievementsViewModel
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        items(AchievementCategory.values().toList()) { category ->
            val (unlocked, total) = viewModel.getCategoryProgress(category)
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                leadingIcon = {
                    Icon(
                        imageVector = viewModel.getCategoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text("$unlocked/$total")
                }
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    title: String,
    icon: ImageVector,
    progress: Pair<Int, Int>
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        PremiumSectionChip(
            modifier = Modifier.weight(1f),
            text = title,
            icon = icon
        )
        Text(
            text = "${progress.first}/${progress.second}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NextAchievementCard(
    achievementWithProgress: AchievementWithProgress
) {
    val achievement = achievementWithProgress.achievement

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.getBadgeIcon(achievement.id),
                    contentDescription = achievement.name,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { achievementWithProgress.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${(achievementWithProgress.progressPercent * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AchievementCard(
    achievementWithProgress: AchievementWithProgress
) {
    val achievement = achievementWithProgress.achievement
    val isUnlocked = achievementWithProgress.isUnlocked

    val alpha by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.6f,
        animationSpec = spring()
    )
    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.97f,
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
            // Achievement badge
            PlatformAchievementBadge(
                badgeName = achievement.id,
                size = 56.dp,
                isUnlocked = isUnlocked
            )

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

                // Progress bar for locked achievements
                if (!isUnlocked && achievementWithProgress.progressPercent > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { achievementWithProgress.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${achievementWithProgress.currentProgress}/${achievementWithProgress.targetProgress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isUnlocked) {
                Icon(
                    imageVector = DailyWellIcons.Status.Success,
                    contentDescription = "Unlocked",
                    modifier = Modifier.size(24.dp),
                    tint = Success
                )
            }
        }
    }
}

@Composable
private fun AchievementCelebrationDialog(
    celebration: AchievementCelebration,
    onDismiss: () -> Unit,
    onShare: (String, String) -> Unit = { _, _ -> }
) {
    val achievement = celebration.achievement

    // Celebration animation
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale),
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
                // Confetti or sparkle effect area
                if (celebration.isRare) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Misc.Sparkle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = DailyWellIcons.Gamification.Trophy,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = DailyWellIcons.Misc.Sparkle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color(0xFFFFD700)
                        )
                    }
                } else {
                    Icon(
                        imageVector = DailyWellIcons.Social.Cheer,
                        contentDescription = "Celebration",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (celebration.isRare) "RARE ACHIEVEMENT!" else "MILESTONE REACHED!",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (celebration.isRare) {
                        Color(0xFFFFD700) // Gold
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Achievement icon
                Icon(
                    imageVector = DailyWellIcons.getBadgeIcon(achievement.id),
                    contentDescription = achievement.name,
                    modifier = Modifier.size(64.dp),
                    tint = if (celebration.isRare) {
                        Color(0xFFFFD700)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Share button
                OutlinedButton(
                    onClick = {
                        onShare(
                            achievement.name,
                            "I just unlocked \"${achievement.name}\" on DailyWell! ${achievement.description}"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = DailyWellIcons.Social.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Achievement")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
