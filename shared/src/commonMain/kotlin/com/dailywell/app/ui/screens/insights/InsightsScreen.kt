package com.dailywell.app.ui.screens.insights

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.core.theme.Success
import com.dailywell.app.core.theme.Warning
import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.StreakInfo
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.DailyWellSprings
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.components.pressScale
import com.dailywell.app.ui.components.rememberAnimatedGradientOffset
import com.dailywell.app.ui.components.rememberBreathingScale
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onUpgradeClick: () -> Unit,
    onNavigateToAchievements: () -> Unit = {},
    viewModel: InsightsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasHabitInsights = uiState.habitInsights.isNotEmpty()
    val improvingCount = uiState.habitInsights.count { it.trend == Trend.IMPROVING }
    val decliningCount = uiState.habitInsights.count { it.trend == Trend.DECLINING }
    val stableCount = uiState.habitInsights.count { it.trend == Trend.STABLE }

    // Show achievement celebration dialog
    uiState.recentAchievement?.let { achievement ->
        AchievementCelebrationDialog(
            achievement = achievement,
            onDismiss = { viewModel.dismissRecentAchievement() }
        )
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Patterns",
                    subtitle = "Trends, correlations, and your story"
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
                    StaggeredItem(index = 0) {
                        StreakOverviewCard(streakInfo = uiState.streakInfo)
                    }
                }

                item {
                    StaggeredItem(index = 1) {
                        WeeklyNarrativeCard(
                            weeklyConsistency = uiState.weeklyConsistency,
                            overallConsistency = uiState.overallConsistency,
                            improvingCount = improvingCount,
                            decliningCount = decliningCount,
                            stableCount = stableCount
                        )
                    }
                }

                if (!hasHabitInsights) {
                    item {
                        StaggeredItem(index = 2) {
                            InsightsNoDataCard(
                                title = "We are still learning your rhythm",
                                message = "Log a few days of habits and this page will unlock trend lines, momentum signals, and personalized weekly notes.",
                                tips = listOf(
                                    "Track at least 3 habits for 7 days",
                                    "Use Today check-ins to avoid missing entries",
                                    "Come back after your first full week"
                                )
                            )
                        }
                    }
                }

                // Milestones section - "Milestones" is journey language, less pressure than "Achievements"
                item {
                    StaggeredItem(index = 3) {
                        PremiumSectionChip(
                            text = "Milestones",
                            icon = DailyWellIcons.Gamification.Trophy
                        )
                    }
                }

                item {
                    StaggeredItem(index = 4) {
                        AchievementsCard(
                            achievements = uiState.achievements,
                            unlockedCount = uiState.unlockedAchievements.size,
                            totalCount = uiState.achievements.size,
                            onClick = onNavigateToAchievements
                        )
                    }
                }

                // Habit performance
                item {
                    StaggeredItem(index = 5) {
                        PremiumSectionChip(
                            text = "Habit performance (30 days)",
                            icon = DailyWellIcons.Analytics.BarChart
                        )
                    }
                }

                item {
                    StaggeredItem(index = 6) {
                        if (hasHabitInsights) {
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
                        } else {
                            InsightsNoDataCard(
                                title = "No trend line yet",
                                message = "Your 30-day and 7-day charts appear after enough completions are logged.",
                                tips = listOf("Keep checking in daily to build your baseline")
                            )
                        }
                    }
                }

                // Connections - "Connections" feels more human than "Correlations"
                if (uiState.correlations.isNotEmpty()) {
                    item {
                        StaggeredItem(index = 7) {
                            PremiumSectionChip(
                                text = "Habit connections",
                                icon = DailyWellIcons.Analytics.Correlation
                            )
                        }
                    }

                    item {
                        StaggeredItem(index = 8) {
                            CorrelationsCard(correlations = uiState.correlations)
                        }
                    }
                } else if (hasHabitInsights) {
                    item {
                        StaggeredItem(index = 9) {
                            InsightsNoDataCard(
                                title = "No strong connections yet",
                                message = "As your patterns stabilize, we will highlight habits that rise or dip together.",
                                tips = listOf("Aim for consistent logging across at least two habits")
                            )
                        }
                    }
                }

                // Your Story - personal, narrative-focused
                item {
                    StaggeredItem(index = 10) {
                        PremiumSectionChip(
                            text = "Your story",
                            icon = DailyWellIcons.Coaching.Reflection
                        )
                    }
                }

                item {
                    StaggeredItem(index = 11) {
                        InsightCard(
                            bestHabit = uiState.bestHabit,
                            focusHabit = uiState.focusHabit,
                            rates = uiState.habitCompletionRates,
                            overallConsistency = uiState.overallConsistency
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
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
            Icon(
                imageVector = DailyWellIcons.getBadgeIcon(achievement.id),
                contentDescription = achievement.name,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
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
    // Animate the progress bar fill for unlocked count
    val animatedUnlockedProgress by animateFloatAsState(
        targetValue = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "achievementsProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale()
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
                        progress = { animatedUnlockedProgress },
                        modifier = Modifier
                            .width(80.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Success,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = DailyWellIcons.Nav.ChevronRight,
                        contentDescription = "View all",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
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
private fun WeeklyNarrativeCard(
    weeklyConsistency: Float,
    overallConsistency: Float,
    improvingCount: Int,
    decliningCount: Int,
    stableCount: Int
) {
    val weeklyPercent = (weeklyConsistency * 100).toInt().coerceIn(0, 100)
    val overallPercent = (overallConsistency * 100).toInt().coerceIn(0, 100)
    val headline = when {
        weeklyPercent == 0 && overallPercent == 0 -> "Your weekly summary unlocks after a few check-ins"
        improvingCount > decliningCount -> "Momentum is positive this week"
        decliningCount > improvingCount -> "This week dipped slightly, but it is recoverable"
        else -> "Your routine is steady this week"
    }
    val body = when {
        weeklyPercent == 0 && overallPercent == 0 ->
            "Once we have enough entries, you will see changes week-over-week and where to focus next."
        else ->
            "7-day consistency is $weeklyPercent% and your 30-day baseline is $overallPercent%."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "WEEKLY SNAPSHOT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryPill(label = "7d", value = "$weeklyPercent%")
                SummaryPill(label = "Improving", value = improvingCount.toString())
                SummaryPill(label = "Stable", value = stableCount.toString())
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InsightsNoDataCard(
    title: String,
    message: String,
    tips: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Analytics.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            tips.forEach { tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement) {
    // Subtle breathing scale for unlocked badges to give them a living feel
    val breathingScale = if (achievement.isUnlocked) {
        rememberBreathingScale(minScale = 1f, maxScale = 1.04f, durationMs = 3000)
    } else {
        1f
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .alpha(if (achievement.isUnlocked) 1f else 0.4f)
            .graphicsLayer {
                scaleX = breathingScale
                scaleY = breathingScale
            }
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
            Icon(
                imageVector = if (achievement.isUnlocked) DailyWellIcons.getBadgeIcon(achievement.id) else DailyWellIcons.Status.Lock,
                contentDescription = if (achievement.isUnlocked) achievement.name else "Locked",
                modifier = Modifier.size(24.dp),
                tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
    val percentage30Day = (insight.rate30Day * 100).toInt().coerceIn(0, 100)
    val percentage7Day = (insight.rate7Day * 100).toInt().coerceIn(0, 100)
    val deltaPoints = percentage7Day - percentage30Day
    val color = when {
        percentage30Day >= 80 -> Success
        percentage30Day >= 50 -> Warning
        else -> MaterialTheme.colorScheme.error
    }

    // Animated color transition for the progress bar
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 500, easing = EaseOutCubic),
        label = "habitPerformanceColor"
    )

    // Animate the progress bar fill
    val animatedProgress30Day by animateFloatAsState(
        targetValue = insight.rate30Day,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "habitPerformanceProgress30"
    )
    val animatedProgress7Day by animateFloatAsState(
        targetValue = insight.rate7Day,
        animationSpec = tween(durationMillis = 850, easing = EaseOutCubic),
        label = "habitPerformanceProgress7"
    )

    val trendIcon = when (insight.trend) {
        Trend.IMPROVING -> DailyWellIcons.Analytics.TrendUp
        Trend.DECLINING -> DailyWellIcons.Analytics.TrendDown
        Trend.STABLE -> DailyWellIcons.Analytics.TrendFlat
    }
    val deltaColor = when {
        deltaPoints > 0 -> Success
        deltaPoints < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val deltaText = when {
        deltaPoints > 0 -> "+$deltaPoints pts vs 30d"
        deltaPoints < 0 -> "$deltaPoints pts vs 30d"
        else -> "Flat vs 30d"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.getHabitIcon(insight.habit.id),
                    contentDescription = insight.habit.name,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = insight.habit.name,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = trendIcon,
                    contentDescription = insight.trend.name,
                    modifier = Modifier.size(14.dp),
                    tint = animatedColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$percentage30Day%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = animatedColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "30 days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$percentage30Day%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress30Day },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = animatedColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last 7 days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$percentage7Day% - $deltaText",
                style = MaterialTheme.typography.labelSmall,
                color = deltaColor,
                fontWeight = FontWeight.Medium
            )
        }

        LinearProgressIndicator(
            progress = { animatedProgress7Day },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
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
                    Icon(
                        imageVector = DailyWellIcons.Analytics.Correlation,
                        contentDescription = "Correlation",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = DailyWellIcons.getHabitIcon(correlation.habit1.id),
                                contentDescription = correlation.habit1.name,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${correlation.habit1.name} + ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = DailyWellIcons.getHabitIcon(correlation.habit2.id),
                                contentDescription = correlation.habit2.name,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = correlation.habit2.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
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
    // Fade-in and slide-up entrance animation
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutCubic),
        label = "premiumFadeIn"
    )

    val slideOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 60f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "premiumSlideUp"
    )

    // Subtle breathing scale on the lock icon
    val lockBreathingScale = rememberBreathingScale(minScale = 1f, maxScale = 1.06f, durationMs = 2500)

    Column(
        modifier = modifier
            .padding(32.dp)
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = slideOffset
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = DailyWellIcons.Status.Lock,
            contentDescription = "Premium locked",
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = lockBreathingScale
                    scaleY = lockBreathingScale
                },
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
                PremiumFeature(icon = DailyWellIcons.Analytics.BarChart, text = "30-day habit performance")
                PremiumFeature(icon = DailyWellIcons.Analytics.Correlation, text = "Habit correlations")
                PremiumFeature(icon = DailyWellIcons.Analytics.TrendUp, text = "Weekly & monthly trends")
                PremiumFeature(icon = DailyWellIcons.Gamification.Trophy, text = "Achievement badges")
                PremiumFeature(icon = DailyWellIcons.Coaching.Reflection, text = "Weekly reflections")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onUpgradeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pressScale(),
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
            text = "$9.99/month or $79.99/year",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PremiumFeature(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun StreakOverviewCard(streakInfo: StreakInfo) {
    // Animate streak numbers counting up from 0
    val animatedCurrentStreak by animateIntAsState(
        targetValue = streakInfo.currentStreak,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "currentStreakCount"
    )

    val animatedLongestStreak by animateIntAsState(
        targetValue = streakInfo.longestStreak,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "longestStreakCount"
    )

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
                icon = DailyWellIcons.Analytics.Streak,
                value = animatedCurrentStreak.toString(),
                label = "Current Streak"
            )
            StreakStat(
                icon = DailyWellIcons.Gamification.Trophy,
                value = animatedLongestStreak.toString(),
                label = "Longest Streak"
            )
        }
    }
}

@Composable
private fun StreakStat(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
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
                    icon = DailyWellIcons.Status.Star,
                    title = "Best habit",
                    content = "${bestHabit.name} at $bestRate%",
                    habitIcon = DailyWellIcons.getHabitIcon(bestHabit.id)
                )
            }

            if (focusHabit != null && focusHabit != bestHabit) {
                Spacer(modifier = Modifier.height(16.dp))
                val focusRate = ((rates[focusHabit.id] ?: 0f) * 100).toInt()
                InsightRow(
                    icon = DailyWellIcons.Health.Workout,
                    title = "Focus area",
                    content = "${focusHabit.name} at $focusRate%",
                    habitIcon = DailyWellIcons.getHabitIcon(focusHabit.id)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val avgRate = (overallConsistency * 100).toInt()

            InsightRow(
                icon = DailyWellIcons.Analytics.BarChart,
                title = "Overall consistency",
                content = "$avgRate% average across all habits"
            )
        }
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    title: String,
    content: String,
    habitIcon: ImageVector? = null
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (habitIcon != null) {
                    Icon(
                        imageVector = habitIcon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


