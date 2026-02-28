package com.dailywell.app.ui.screens.gamification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailywell.app.data.model.*
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.PrimaryLight
import com.dailywell.app.core.theme.Secondary
import com.dailywell.app.core.theme.SecondaryLight
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.ShimmerLoadingScreen
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationScreen(
    onBack: () -> Unit,
    viewModel: GamificationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Rewards & Progress",
                    subtitle = "XP, badges, and themes",
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
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // XP & Level Card
                    item {
                        LevelProgressCard(
                            level = uiState.gamificationData.currentLevel,
                            totalXp = uiState.gamificationData.totalXp,
                            xpProgress = uiState.xpProgress,
                            levelTitle = uiState.levelTitle,
                            xpToNext = uiState.gamificationData.xpToNextLevel
                        )
                    }

                    // Daily Actions Row
                    item {
                        DailyActionsRow(
                            canClaimReward = uiState.canClaimDailyReward,
                            canSpin = uiState.canSpin,
                            streakShields = uiState.gamificationData.streakShields,
                            onClaimReward = { viewModel.claimDailyReward() },
                            onSpin = { viewModel.openSpinWheel() }
                        )
                    }

                    // Stats Overview
                    item {
                        StatsOverviewCard(
                            data = uiState.gamificationData
                        )
                    }

                    item {
                        PremiumSectionChip(
                            text = if (selectedTab == 0) "Badge Collection" else "Theme Collection",
                            icon = if (selectedTab == 0) {
                                DailyWellIcons.Gamification.Badge
                            } else {
                                DailyWellIcons.Gamification.Crown
                            }
                        )
                    }

                    // Tab Selection
                    item {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Secondary
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("Badges") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("Themes") }
                            )
                        }
                    }

                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> {
                            item {
                                BadgesSection(
                                    allBadges = uiState.allBadgesWithStatus,
                                    getBadgeProgress = { badge -> viewModel.getBadgeProgress(badge) }
                                )
                            }
                        }
                        1 -> {
                            item {
                                ThemesSection(
                                    allThemes = ThemeLibrary.allThemes,
                                    unlockedThemes = uiState.unlockedThemes,
                                    selectedTheme = uiState.selectedTheme,
                                    currentLevel = uiState.gamificationData.currentLevel,
                                    onSelectTheme = { viewModel.selectTheme(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Daily Reward Dialog
    if (uiState.showDailyReward && uiState.dailyReward != null) {
        DailyRewardDialog(
            reward = uiState.dailyReward!!,
            streak = uiState.gamificationData.dailyRewardStreak,
            onDismiss = { viewModel.dismissDailyReward() }
        )
    }

    // Spin Wheel Dialog
    if (uiState.showSpinWheel) {
        SpinWheelDialog(
            isSpinning = uiState.isSpinning,
            result = uiState.spinResult,
            canSpin = uiState.canSpin,
            onSpin = { viewModel.spin() },
            onDismiss = { viewModel.closeSpinWheel() }
        )
    }

    // Badge Unlocked Dialog
    uiState.recentlyUnlockedBadge?.let { badge ->
        BadgeUnlockedDialog(
            badge = badge,
            onDismiss = { viewModel.dismissRecentBadge() }
        )
    }
}

@Composable
private fun LevelProgressCard(
    level: Int,
    totalXp: Long,
    xpProgress: Float,
    levelTitle: LevelTitle,
    xpToNext: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Secondary,
                            Secondary.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Icon(
                            imageVector = DailyWellIcons.Gamification.Level,
                            contentDescription = levelTitle.name,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = levelTitle.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = levelTitle.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "LVL",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "$level",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // XP Progress
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${totalXp.formatNumber()} XP",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${xpToNext.formatNumber()} to next level",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyActionsRow(
    canClaimReward: Boolean,
    canSpin: Boolean,
    streakShields: Int,
    onClaimReward: () -> Unit,
    onSpin: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daily Reward
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = if (canClaimReward) DailyWellIcons.Gamification.Gift else DailyWellIcons.Actions.Check,
            title = "Daily",
            subtitle = if (canClaimReward) "Claim!" else "Done",
            isActive = canClaimReward,
            onClick = { if (canClaimReward) onClaimReward() }
        )

        // Spin Wheel
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = if (canSpin) DailyWellIcons.Gamification.Spin else DailyWellIcons.Actions.Check,
            title = "Spin",
            subtitle = if (canSpin) "Free spin!" else "Done",
            isActive = canSpin,
            onClick = onSpin
        )

        // Streak Shields
        ActionCard(
            modifier = Modifier.weight(1f),
            icon = DailyWellIcons.Gamification.Shield,
            title = "Shields",
            subtitle = "$streakShields left",
            isActive = false,
            onClick = {}
        )
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(enabled = isActive || icon == DailyWellIcons.Gamification.Spin, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                PrimaryLight.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) Secondary else Color.Gray
            )
        }
    }
}

@Composable
private fun StatsOverviewCard(data: GamificationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Your Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = data.totalHabitsCompleted.toString(), label = "Habits Done")
                StatItem(value = data.perfectDays.toString(), label = "Perfect Days")
                StatItem(value = data.currentStreak.toString(), label = "Current Streak")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = data.longestStreak.toString(), label = "Best Streak")
                StatItem(value = data.lifetimeXp.formatNumber(), label = "Lifetime XP")
                StatItem(value = data.dailyRewardStreak.toString(), label = "Login Streak")
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Secondary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun BadgesSection(
    allBadges: List<Pair<Badge, Boolean>>,
    getBadgeProgress: (Badge) -> Float
) {
    var selectedCategory by remember { mutableStateOf<BadgeCategory?>(null) }

    Column {
        // Category filter
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") }
                )
            }
            items(BadgeCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = if (selectedCategory == category) null else category
                    },
                    label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Badges count
        val filteredBadges = if (selectedCategory != null) {
            allBadges.filter { it.first.category == selectedCategory }
        } else {
            allBadges
        }
        val unlockedCount = filteredBadges.count { it.second }

        Text(
            text = "$unlockedCount / ${filteredBadges.size} Unlocked",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Badges grid
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            filteredBadges.chunked(3).forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowBadges.forEach { (badge, isUnlocked) ->
                        BadgeItem(
                            modifier = Modifier.weight(1f),
                            badge = badge,
                            isUnlocked = isUnlocked,
                            progress = if (!isUnlocked) getBadgeProgress(badge) else 1f
                        )
                    }
                    // Fill empty space if row is incomplete
                    repeat(3 - rowBadges.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeItem(
    modifier: Modifier = Modifier,
    badge: Badge,
    isUnlocked: Boolean,
    progress: Float
) {
    var showDetail by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { showDetail = true },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                when (badge.tier) {
                    BadgeTier.BRONZE -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                    BadgeTier.SILVER -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                    BadgeTier.GOLD -> Color(0xFFFFD700).copy(alpha = 0.2f)
                    BadgeTier.PLATINUM -> Color(0xFFE5E4E2).copy(alpha = 0.3f)
                    BadgeTier.DIAMOND -> Color(0xFFB9F2FF).copy(alpha = 0.3f)
                }
            } else {
                Color.Gray.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isUnlocked || !badge.isSecret) DailyWellIcons.getBadgeIcon(badge.id) else DailyWellIcons.Misc.Help,
                    contentDescription = if (isUnlocked || !badge.isSecret) badge.name else "Secret badge",
                    modifier = Modifier.size(32.dp),
                    tint = if (isUnlocked) Color.Unspecified else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isUnlocked || !badge.isSecret) badge.name else "???",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    color = if (isUnlocked) Color.Unspecified else Color.Gray
                )

                if (!isUnlocked && progress > 0f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Secondary,
                        trackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                }
            }

            if (!isUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }

    if (showDetail) {
        BadgeDetailDialog(
            badge = badge,
            isUnlocked = isUnlocked,
            progress = progress,
            onDismiss = { showDetail = false }
        )
    }
}

@Composable
private fun ThemesSection(
    allThemes: List<AppTheme>,
    unlockedThemes: List<AppTheme>,
    selectedTheme: AppTheme,
    currentLevel: Int,
    onSelectTheme: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        allThemes.forEach { theme ->
            val isUnlocked = unlockedThemes.any { it.id == theme.id }
            val isSelected = theme.id == selectedTheme.id

            ThemeItem(
                theme = theme,
                isUnlocked = isUnlocked,
                isSelected = isSelected,
                currentLevel = currentLevel,
                onClick = { if (isUnlocked) onSelectTheme(theme.id) }
            )
        }
    }
}

@Composable
private fun ThemeItem(
    theme: AppTheme,
    isUnlocked: Boolean,
    isSelected: Boolean,
    currentLevel: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked, onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = Secondary,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked)
                Color(theme.surfaceColor)
            else
                Color.Gray.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme preview
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(theme.primaryColor)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.Misc.Palette,
                    contentDescription = theme.name,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) Color.Unspecified else Color.Gray
                )
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (!isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val requirement = when (val req = theme.unlockRequirement) {
                        is ThemeUnlockRequirement.Level -> "Level ${req.level}"
                        is ThemeUnlockRequirement.Badge -> "Badge required"
                        is ThemeUnlockRequirement.Xp -> "${req.totalXp} XP"
                        is ThemeUnlockRequirement.SpinWheelTicket -> "Spin Wheel Ticket"
                        is ThemeUnlockRequirement.Premium -> "Premium"
                        null -> ""
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = DailyWellIcons.Status.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(14.dp),
                            tint = Secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = requirement,
                            style = MaterialTheme.typography.bodySmall,
                            color = Secondary
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = DailyWellIcons.Actions.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(24.dp),
                    tint = Secondary
                )
            }
        }
    }
}

@Composable
private fun DailyRewardDialog(
    reward: DailyReward,
    streak: Int,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = DailyWellIcons.Gamification.Gift,
                    contentDescription = "Daily Reward",
                    modifier = Modifier.size(64.dp),
                    tint = Secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Daily Reward!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Day $streak streak!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Reward display
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryLight.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "+${reward.xpReward} XP",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Secondary
                        )

                        reward.bonusReward?.let { bonus ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (bonus) {
                                    is DailyBonusReward.StreakShield -> "+${bonus.count} Streak Shield"
                                    is DailyBonusReward.XpMultiplier -> "${bonus.multiplier}x XP for ${bonus.durationHours}h"
                                    is DailyBonusReward.ThemeUnlock -> "Theme Unlocked!"
                                    is DailyBonusReward.BadgeProgress -> "Badge Progress!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    Text("Awesome!")
                }
            }
        }
    }
}

@Composable
private fun SpinWheelDialog(
    isSpinning: Boolean,
    result: SpinReward?,
    canSpin: Boolean,
    onSpin: () -> Unit,
    onDismiss: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isSpinning) 1800f else 0f,
        animationSpec = tween(
            durationMillis = 3000,
            easing = FastOutSlowInEasing
        ),
        label = "spin"
    )

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Daily Spin!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Spin Wheel Visual
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFFD700),
                                    Color(0xFF4CAF50),
                                    Color(0xFF2196F3),
                                    Color(0xFF9C27B0),
                                    Color(0xFFFF5722),
                                    Color(0xFF00BCD4),
                                    Color(0xFFE91E63),
                                    Color(0xFF8BC34A),
                                    Color(0xFFFFD700)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DailyWellIcons.Gamification.Spin,
                            contentDescription = "Spin",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Pointer
                Text(
                    text = "▼",
                    fontSize = 32.sp,
                    color = Secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Result
                AnimatedVisibility(visible = result != null && !isSpinning) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = PrimaryLight.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "You won!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (result) {
                                    is SpinReward.Xp -> "+${result.amount} XP"
                                    is SpinReward.StreakShields -> "+${result.count} Streak Shield"
                                    is SpinReward.XpBoost -> "${result.multiplier}x XP for ${result.hours}h"
                                    is SpinReward.ThemeTicket -> "Theme Ticket!"
                                    is SpinReward.TryAgain -> "Try again tomorrow!"
                                    null -> ""
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (result == null && !isSpinning) {
                    Button(
                        onClick = onSpin,
                        enabled = canSpin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Text(if (canSpin) "SPIN!" else "Already Spun Today")
                    }
                } else if (result != null) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                    ) {
                        Text("Awesome!")
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeUnlockedDialog(
    badge: Badge,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (badge.tier) {
                    BadgeTier.BRONZE -> Color(0xFFCD7F32).copy(alpha = 0.1f)
                    BadgeTier.SILVER -> Color(0xFFC0C0C0).copy(alpha = 0.1f)
                    BadgeTier.GOLD -> Color(0xFFFFD700).copy(alpha = 0.1f)
                    BadgeTier.PLATINUM -> Color(0xFFE5E4E2).copy(alpha = 0.2f)
                    BadgeTier.DIAMOND -> Color(0xFFB9F2FF).copy(alpha = 0.2f)
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = DailyWellIcons.Social.Cheer,
                    contentDescription = "Celebration",
                    modifier = Modifier.size(48.dp),
                    tint = Secondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Badge Unlocked!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Icon(
                    imageVector = DailyWellIcons.getBadgeIcon(badge.id),
                    contentDescription = badge.name,
                    modifier = Modifier.size(80.dp),
                    tint = Secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "+${badge.xpReward} XP",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                ) {
                    Text("Awesome!")
                }
            }
        }
    }
}

@Composable
private fun BadgeDetailDialog(
    badge: Badge,
    isUnlocked: Boolean,
    progress: Float,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (isUnlocked || !badge.isSecret) DailyWellIcons.getBadgeIcon(badge.id) else DailyWellIcons.Misc.Help,
                    contentDescription = if (isUnlocked || !badge.isSecret) badge.name else "Secret badge",
                    modifier = Modifier.size(64.dp),
                    tint = if (isUnlocked) Secondary else Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isUnlocked || !badge.isSecret) badge.name else "Secret Badge",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = badge.tier.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (badge.tier) {
                        BadgeTier.BRONZE -> Color(0xFFCD7F32)
                        BadgeTier.SILVER -> Color(0xFFC0C0C0)
                        BadgeTier.GOLD -> Color(0xFFFFD700)
                        BadgeTier.PLATINUM -> Color(0xFF9E9E9E)
                        BadgeTier.DIAMOND -> Color(0xFF00BCD4)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isUnlocked || !badge.isSecret) badge.description else "Keep exploring to unlock!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )

                if (!isUnlocked) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Progress: ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Secondary,
                        trackColor = Color.Gray.copy(alpha = 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Reward: +${badge.xpReward} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Secondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

private fun Long.formatNumber(): String {
    return when {
        this >= 1_000_000 -> "${this / 1_000_000}M"
        this >= 1_000 -> "${this / 1_000}K"
        else -> this.toString()
    }
}
