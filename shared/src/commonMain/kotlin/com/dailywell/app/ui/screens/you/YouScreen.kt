package com.dailywell.app.ui.screens.you

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.core.theme.*
import com.dailywell.app.data.model.AIGovernancePolicy
import com.dailywell.app.data.model.AIPlanType
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.TodayViewMode
import com.dailywell.app.data.model.UserAIUsage
import com.dailywell.app.data.repository.AICoachingRepository
import com.dailywell.app.data.repository.EntryRepository
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import androidx.compose.ui.graphics.vector.ImageVector
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.components.pressScale
import org.koin.compose.koinInject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * YouScreen - 2026 Modern Profile & Transformation Journey
 *
 * Design: Warm, organic, glass-layered with staggered
 * entrance animations and micro-interactions throughout.
 */
@Composable
fun YouScreen(
    isPremium: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToMilestones: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToUsageDetails: () -> Unit = {},
    onNavigateToCustomHabit: () -> Unit = {}
) {
    val aiCoachingRepository: AICoachingRepository = koinInject()
    val habitRepository: HabitRepository = koinInject()
    val entryRepository: EntryRepository = koinInject()
    val settingsRepository: SettingsRepository = koinInject()

    val aiUsage by aiCoachingRepository.getAIUsage().collectAsState(
        initial = UserAIUsage(
            userId = "local",
            planType = AIPlanType.FREE,
            resetDate = "next cycle",
            lastUpdated = ""
        )
    )
    val habits by habitRepository.getEnabledHabits().collectAsState(initial = emptyList())
    val allHabits by habitRepository.getAllHabits().collectAsState(initial = emptyList())
    val customHabits by habitRepository.getCustomHabits().collectAsState(initial = emptyList())
    val streakInfo by entryRepository.getStreakInfo().collectAsState(initial = com.dailywell.app.data.model.StreakInfo())
    val settings by settingsRepository.getSettings().collectAsState(initial = com.dailywell.app.data.model.UserSettings())
    val weekData by entryRepository.getWeekData(0).collectAsState(initial = null)

    val currentStreak = streakInfo.currentStreak
    val simpleMode = settings.todayViewMode == TodayViewMode.SIMPLE
    val userName = settings.displayName?.takeIf { it.isNotBlank() }
        ?: settings.userName?.takeIf { it.isNotBlank() }
        ?: "You"
    val profileHint = settings.userEmail?.takeIf { it.isNotBlank() } ?: "Profile and preferences"
    val completionRate = weekData?.completionRate ?: 0f
    val daysOnJourney = remember(streakInfo) {
        if (streakInfo.currentStreak > 0) streakInfo.currentStreak else 1
    }

    val dailyWellColors = LocalDailyWellColors.current

    GlassScreenWrapper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dailyWellColors.timeGradientStart.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
            ) {
                // Journey spotlight hero
                item {
                    StaggeredItem(index = 1, delayPerItem = 60L) {
                        JourneySpotlightCard(
                            userName = userName,
                            currentStreak = currentStreak,
                            completionRate = completionRate,
                            daysOnJourney = daysOnJourney,
                            isPremium = isPremium
                        )
                    }
                }

            // Profile Header with animation
            item {
                StaggeredItem(index = 2, delayPerItem = 60L) {
                    ProfileHeader(
                        userName = userName,
                        profileHint = profileHint,
                        isPremium = isPremium,
                        onSettingsClick = onNavigateToSettings
                    )
                }
            }

                // Stats Overview with glass card
                item {
                    StaggeredItem(index = 3, delayPerItem = 60L) {
                        StatsOverview(
                            currentStreak = currentStreak,
                            completionRate = completionRate,
                            totalHabits = habits.size,
                            daysOnJourney = daysOnJourney
                        )
                    }
                }

                if (!simpleMode) {
                    item {
                        StaggeredItem(index = 4, delayPerItem = 60L) {
                            AIUsageOverviewCard(
                                aiUsage = aiUsage,
                                isPremium = isPremium,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToUsageDetails = onNavigateToUsageDetails,
                                onNavigateToPaywall = onNavigateToPaywall
                            )
                        }
                    }
                }

                // Transformation Journey
                item {
                    StaggeredItem(index = 5, delayPerItem = 60L) {
                        HabitManagementSection(
                            habits = habits,
                            totalHabits = allHabits.size,
                            customHabitCount = customHabits.size,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigateToCustomHabit = onNavigateToCustomHabit
                        )
                    }
                }

                if (!simpleMode) {
                    item {
                        StaggeredItem(index = 6, delayPerItem = 60L) {
                            TransformationJourneySection(
                                daysOnJourney = daysOnJourney,
                                completionRate = completionRate,
                                isPremium = isPremium,
                                onNavigateToPaywall = onNavigateToPaywall
                            )
                        }
                    }

                    item {
                        StaggeredItem(index = 7, delayPerItem = 60L) {
                            AchievementsSection(
                                currentStreak = currentStreak,
                                onNavigateToMilestones = onNavigateToMilestones
                            )
                        }
                    }

                    item {
                        StaggeredItem(index = 8, delayPerItem = 60L) {
                            QuickActionsSection(
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToMilestones = onNavigateToMilestones,
                                onNavigateToCustomHabit = onNavigateToCustomHabit
                            )
                        }
                    }
                } else {
                    item {
                        StaggeredItem(index = 6, delayPerItem = 60L) {
                            SimpleYouHintCard()
                        }
                    }
                }

                if (!isPremium && !simpleMode) {
                    item {
                        StaggeredItem(index = 9, delayPerItem = 60L) {
                            PremiumBanner(onNavigateToPaywall = onNavigateToPaywall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleYouHintCard() {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PremiumSectionChip(
                text = "Simple mode",
                icon = DailyWellIcons.Actions.CheckCircle
            )
            Text(
                text = "AI usage, advanced journey widgets, and premium actions are hidden to keep this screen focused.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Switch to Full mode from Today when you want deeper profile tools.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun JourneySpotlightCard(
    userName: String,
    currentStreak: Int,
    completionRate: Float,
    daysOnJourney: Int,
    isPremium: Boolean
) {
    val dailyWellColors = LocalDailyWellColors.current
    val colorScheme = MaterialTheme.colorScheme
    val useDarkSpotlightPalette = colorScheme.surface.luminance() < 0.45f
    val spotlightGradient = remember(
        useDarkSpotlightPalette,
        colorScheme.surfaceVariant,
        colorScheme.primary,
        colorScheme.secondary
    ) {
        if (useDarkSpotlightPalette) {
            listOf(
                colorScheme.surfaceVariant.copy(alpha = 0.94f),
                colorScheme.primary.copy(alpha = 0.30f),
                colorScheme.secondary.copy(alpha = 0.22f)
            )
        } else {
            listOf(
                AccentEmerald.copy(alpha = 0.24f),
                AccentSky.copy(alpha = 0.24f),
                AccentViolet.copy(alpha = 0.20f)
            )
        }
    }
    val completionPercent = (completionRate * 100f).toInt().coerceIn(0, 100)
    val journeyProgress = (daysOnJourney.coerceAtLeast(1).toFloat() / 30f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = dailyWellColors.shadowMedium
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = spotlightGradient
                )
            )
            .border(
                width = 0.8.dp,
                color = dailyWellColors.glassBorder.copy(alpha = 0.7f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress Snapshot",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isPremium) Success.copy(alpha = 0.16f) else AccentAmber.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (isPremium) "PREMIUM" else "FREE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremium) Success else AccentAmber
                    )
                }
            }

            val journeySummary = if (userName.equals("You", ignoreCase = true)) {
                "You're on day $daysOnJourney with a $currentStreak-day streak."
            } else {
                "$userName is on day $daysOnJourney with a $currentStreak-day streak."
            }
            Text(
                text = journeySummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(dailyWellColors.surfaceMuted)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(journeyProgress)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AccentEmerald,
                                    AccentSky
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completionPercent% completion this week",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(journeyProgress * 100).toInt()}% to day 30",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    profileHint: String,
    isPremium: Boolean,
    onSettingsClick: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    // Breathing animation on avatar
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarBreathing"
    )

    // Gradient rotation for premium ring
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated Avatar with glow
        Box(contentAlignment = Alignment.Center) {
            // Outer glow for premium
            if (isPremium) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .graphicsLayer {
                            rotationZ = ringRotation
                            alpha = 0.4f
                        }
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    AccentAmber,
                                    AccentRose,
                                    AccentViolet,
                                    AccentAmber
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = avatarScale
                        scaleY = avatarScale
                    }
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = dailyWellColors.shadowMedium
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isPremium) {
                                listOf(AccentAmber, AccentRose)
                            } else {
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            }
                        ),
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "Y",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = profileHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Settings button with press scale
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(dailyWellColors.surfaceSubtle)
                .pressScale()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DailyWellIcons.Nav.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatsOverview(
    currentStreak: Int,
    completionRate: Float,
    totalHabits: Int,
    daysOnJourney: Int
) {
    val dailyWellColors = LocalDailyWellColors.current

    // Glass card for stats
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = dailyWellColors.shadowLight,
                ambientColor = dailyWellColors.shadowLight
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassBackground,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassHighlight,
                        dailyWellColors.glassBorder
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = "$currentStreak",
                label = "Day Streak",
                icon = DailyWellIcons.Analytics.Streak,
                accentColor = StreakFire
            )
            StatDivider()
            StatItem(
                value = "${(completionRate * 100).toInt()}%",
                label = "Completion",
                icon = DailyWellIcons.Actions.CheckCircle,
                accentColor = Success
            )
            StatDivider()
            StatItem(
                value = "$totalHabits",
                label = "Habits",
                icon = DailyWellIcons.Analytics.BarChart,
                accentColor = MaterialTheme.colorScheme.primary
            )
            StatDivider()
            StatItem(
                value = "$daysOnJourney",
                label = "Days",
                icon = DailyWellIcons.Analytics.Calendar,
                accentColor = AccentSky
            )
        }
    }
}

@Composable
private fun AIUsageOverviewCard(
    aiUsage: UserAIUsage,
    isPremium: Boolean,
    onNavigateToSettings: () -> Unit,
    onNavigateToUsageDetails: () -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    val usagePercent = max(aiUsage.percentUsed, aiUsage.costPercentUsed).coerceIn(0f, 100f)
    val progress = (usagePercent / 100f).coerceIn(0f, 1f)
    val usedPercent = usagePercent.roundToInt().coerceIn(0, 100)
    val availablePercent = (100 - usedPercent).coerceAtLeast(0)
    val localChat = aiUsage.localMessagesCount
    val cloudChat = aiUsage.cloudChatCalls
    val scanCalls = aiUsage.cloudScanCalls
    val reportCalls = aiUsage.cloudReportCalls
    val totalCloud = aiUsage.cloudTotalCalls
    val planName = if (isPremium && aiUsage.planType == AIPlanType.FREE) {
        "Premium"
    } else {
        aiUsage.planType.displayName
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = dailyWellColors.shadowLight
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AccentSky.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = dailyWellColors.glassBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Plan: $planName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isPremium) Success.copy(alpha = 0.16f) else AccentViolet.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (isPremium) "Premium" else "Free",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPremium) Success else AccentViolet
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = if (progress >= 0.8f) Error else Success,
                trackColor = dailyWellColors.surfaceMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Used $usedPercent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Available $availablePercent%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageMiniStat(
                    label = "Local chat",
                    value = localChat.toString(),
                    tint = Success,
                    modifier = Modifier.weight(1f)
                )
                UsageMiniStat(
                    label = "Cloud chat",
                    value = cloudChat.toString(),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                UsageMiniStat(
                    label = "Scans",
                    value = scanCalls.toString(),
                    tint = AccentSky,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsageMiniStat(
                    label = "Reports",
                    value = reportCalls.toString(),
                    tint = AccentViolet,
                    modifier = Modifier.weight(1f)
                )
                UsageMiniStat(
                    label = "Cloud total",
                    value = totalCloud.toString(),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                UsageMiniStat(
                    label = "Reset",
                    value = aiUsage.resetDate,
                    tint = AccentSky,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = if (isPremium) {
                    "One AI wallet across chat, scan, and reports."
                } else {
                    "Free scans: ${AIGovernancePolicy.FREE_SCAN_LIMIT_PER_MONTH}/month. Trial scans: ${AIGovernancePolicy.TRIAL_SCAN_LIMIT_TOTAL} total."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("AI Settings")
                }
                Button(
                    onClick = if (isPremium) onNavigateToUsageDetails else onNavigateToPaywall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPremium) MaterialTheme.colorScheme.primary else Success
                    )
                ) {
                    Text(if (isPremium) "Usage Insights" else "Upgrade")
                }
            }
        }
    }
}

@Composable
private fun UsageMiniStat(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HabitManagementSection(
    habits: List<Habit>,
    totalHabits: Int,
    customHabitCount: Int,
    onNavigateToSettings: () -> Unit,
    onNavigateToCustomHabit: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    val activeHabits = habits.sortedBy { it.order }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Habit Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = AccentSky.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "${activeHabits.size}/$totalHabits active",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = dailyWellColors.shadowLight
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AccentEmerald.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = dailyWellColors.glassBorder,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UsageMiniStat(
                        label = "Enabled",
                        value = activeHabits.size.toString(),
                        tint = Success,
                        modifier = Modifier.weight(1f)
                    )
                    UsageMiniStat(
                        label = "Custom",
                        value = customHabitCount.toString(),
                        tint = AccentViolet,
                        modifier = Modifier.weight(1f)
                    )
                    UsageMiniStat(
                        label = "Slots",
                        value = "${activeHabits.size}/12",
                        tint = AccentSky,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (activeHabits.isEmpty()) {
                    Text(
                        text = "No active habits yet. Add your first custom habit to get started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Active habits",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val visibleHabits = activeHabits.take(8)
                        items(visibleHabits.size) { index ->
                            HabitPill(habit = visibleHabits[index])
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Manage")
                    }
                    Button(
                        onClick = onNavigateToCustomHabit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Custom")
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitPill(habit: Habit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = habit.emoji, style = MaterialTheme.typography.bodySmall)
            Text(
                text = habit.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (habit.isCustom) {
                Text(
                    text = "Custom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    // Animate value on appearance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "statAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    ) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(16.dp), tint = accentColor)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDivider() {
    val dailyWellColors = LocalDailyWellColors.current
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(44.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        dailyWellColors.divider,
                        Color.Transparent
                    )
                )
            )
    )
}

@Composable
private fun TransformationJourneySection(
    daysOnJourney: Int,
    completionRate: Float,
    isPremium: Boolean,
    onNavigateToPaywall: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Transformation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!isPremium) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentViolet.copy(alpha = 0.1f),
                    modifier = Modifier
                        .pressScale()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onNavigateToPaywall() }
                ) {
                    Text(
                        text = "Premium",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentViolet,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Milestone timeline with staggered entrance
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val milestones = listOf(
                Triple(1, "Day 1", "The Beginning"),
                Triple(7, "Day 7", "One Week"),
                Triple(14, "Day 14", "Habit Forming"),
                Triple(30, "Day 30", "Monthly Master"),
                Triple(60, "Day 60", "Two Months"),
                Triple(90, "Day 90", "Quarterly")
            )

            items(milestones.size) { index ->
                val (day, title, subtitle) = milestones[index]
                val isReached = daysOnJourney >= day
                val isLocked = !isPremium && day > 7

                StaggeredItem(index = index, delayPerItem = 80L, baseDelay = 150L) {
                    MilestoneCard(
                        day = day,
                        title = title,
                        subtitle = subtitle,
                        isReached = isReached,
                        isLocked = isLocked,
                        completionRate = if (isReached) completionRate else 0f,
                        onClick = {
                            if (isLocked) onNavigateToPaywall()
                        }
                    )
                }
            }
        }

        // Progress card with glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = dailyWellColors.shadowLight
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AccentEmerald.copy(alpha = 0.08f),
                            AccentIndigo.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = dailyWellColors.glassBorder,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Day $daysOnJourney of your journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val nextMilestone = when {
                    daysOnJourney < 7 -> 7
                    daysOnJourney < 14 -> 14
                    daysOnJourney < 30 -> 30
                    daysOnJourney < 60 -> 60
                    daysOnJourney < 90 -> 90
                    else -> 365
                }
                val progress = daysOnJourney.toFloat() / nextMilestone

                Text(
                    text = "${nextMilestone - daysOnJourney} days until Day $nextMilestone milestone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Animated progress bar
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(1200, easing = EaseOutCubic),
                    label = "milestoneProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dailyWellColors.surfaceMuted)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AccentEmerald, AccentSky)
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(
    day: Int,
    title: String,
    subtitle: String,
    isReached: Boolean,
    isLocked: Boolean,
    completionRate: Float,
    onClick: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    // Subtle glow for reached milestones
    val infiniteTransition = rememberInfiniteTransition(label = "milestone$day")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "milestoneGlow$day"
    )

    Card(
        modifier = Modifier
            .width(120.dp)
            .pressScale()
            .then(
                if (isReached) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = AccentEmerald.copy(alpha = glowAlpha * 0.15f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReached) {
                dailyWellColors.surfaceElevated
            } else {
                dailyWellColors.surfaceSubtle.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isReached) 3.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon area
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isReached) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            dailyWellColors.surfaceMuted
                        }
                    )
            ) {
                Icon(
                    imageVector = when {
                        isLocked -> DailyWellIcons.Status.Lock
                        isReached -> DailyWellIcons.Misc.Sparkle
                        else -> DailyWellIcons.Misc.Timer
                    },
                    contentDescription = when {
                        isLocked -> "Locked"
                        isReached -> "Reached"
                        else -> "Upcoming"
                    },
                    modifier = Modifier.size(22.dp).graphicsLayer { alpha = if (isLocked) 0.5f else 1f },
                    tint = when {
                        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
                        isReached -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isReached) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (isReached && completionRate > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentEmerald
                )
            }
        }
    }
}

@Composable
private fun AchievementsSection(
    currentStreak: Int,
    onNavigateToMilestones: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onNavigateToMilestones) {
                Text(
                    "See all",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val achievements = listOf(
                Triple(DailyWellIcons.Analytics.Streak, "First Flame", currentStreak >= 1),
                Triple(DailyWellIcons.Analytics.Calendar, "Week Warrior", currentStreak >= 7),
                Triple(DailyWellIcons.Health.Workout, "Two Week Strong", currentStreak >= 14),
                Triple(DailyWellIcons.Gamification.Trophy, "Monthly Master", currentStreak >= 30)
            )

            items(achievements.size) { index ->
                val (icon, title, unlocked) = achievements[index]
                StaggeredItem(index = index, delayPerItem = 100L, baseDelay = 100L) {
                    AchievementBadge(
                        icon = icon,
                        title = title,
                        unlocked = unlocked,
                        onClick = onNavigateToMilestones
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    icon: ImageVector,
    title: String,
    unlocked: Boolean,
    onClick: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    // Glow for unlocked badges
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeGlow"
    )

    Card(
        modifier = Modifier
            .width(92.dp)
            .pressScale()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) {
                dailyWellColors.surfaceElevated
            } else {
                dailyWellColors.surfaceSubtle.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (unlocked) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .then(
                        if (unlocked) {
                            Modifier.drawBehind {
                                drawCircle(
                                    color = AchievementGold.copy(alpha = glowAlpha * 0.3f),
                                    radius = size.minDimension / 2 + 6.dp.toPx()
                                )
                            }
                        } else Modifier
                    )
                    .clip(CircleShape)
                    .background(
                        if (unlocked) {
                            Brush.radialGradient(
                                colors = listOf(
                                    AchievementGold.copy(alpha = 0.25f),
                                    AchievementGold.copy(alpha = 0.08f)
                                )
                            )
                        } else {
                            Brush.radialGradient(
                                colors = listOf(
                                    dailyWellColors.surfaceMuted,
                                    dailyWellColors.surfaceSubtle
                                )
                            )
                        }
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp).graphicsLayer { alpha = if (unlocked) 1f else 0.35f },
                    tint = if (unlocked) AchievementGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = if (unlocked) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                },
                maxLines = 2,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToSettings: () -> Unit,
    onNavigateToMilestones: () -> Unit,
    onNavigateToCustomHabit: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, start = 20.dp, end = 20.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QuickActionCard(
                icon = DailyWellIcons.Nav.Settings,
                label = "Settings",
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = DailyWellIcons.Gamification.Medal,
                label = "Achievements",
                onClick = onNavigateToMilestones,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            QuickActionCard(
                icon = DailyWellIcons.Analytics.BarChart,
                label = "Manage Habits",
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = DailyWellIcons.Actions.CheckCircle,
                label = "Add Custom",
                onClick = onNavigateToCustomHabit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyWellColors = LocalDailyWellColors.current

    Box(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = dailyWellColors.shadowLight
            )
            .clip(RoundedCornerShape(16.dp))
            .background(dailyWellColors.surfaceElevated)
            .border(
                width = 0.5.dp,
                color = dailyWellColors.glassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .pressScale()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(dailyWellColors.surfaceSubtle)
            ) {
                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PremiumBanner(onNavigateToPaywall: () -> Unit) {
    val dailyWellColors = LocalDailyWellColors.current

    // Animated gradient shimmer
    val infiniteTransition = rememberInfiniteTransition(label = "premium")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premiumShimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AccentViolet.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AccentIndigo,
                        AccentViolet,
                        AccentIndigo
                    ),
                    startX = shimmerOffset * -300f,
                    endX = shimmerOffset * 1200f + 600f
                )
            )
            .pressScale()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onNavigateToPaywall() }
            .padding(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unlock Full Journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Compare Day 1 to Day 90 with Premium",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = DailyWellIcons.Nav.ArrowForward,
                    contentDescription = "Go",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
            }
        }
    }
}
