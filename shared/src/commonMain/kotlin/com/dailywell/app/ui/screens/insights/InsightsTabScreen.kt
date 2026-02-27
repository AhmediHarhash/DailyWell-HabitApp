package com.dailywell.app.ui.screens.insights

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.PremiumDesignTokens
import com.dailywell.app.core.theme.PremiumPalette
import com.dailywell.app.data.model.TodayViewMode
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.GlassProgressBar
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumActionTile
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.ShimmerLoadingScreen
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.navigation.Screen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** Hub screen for the Insights tab with a stronger premium analytics presentation. */
@Composable
fun InsightsTabScreen(
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit,
    viewModel: InsightsTabViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState.isLoading
    val effectivePremium = uiState.isPremium || isPremium
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.getSettings().collectAsState(initial = UserSettings())
    val simpleMode = settings.todayViewMode == TodayViewMode.SIMPLE
    val colorScheme = MaterialTheme.colorScheme
    val useDarkHeroPalette = colorScheme.surface.luminance() < 0.45f
    val heroGradient = remember(
        useDarkHeroPalette,
        colorScheme.surfaceVariant,
        colorScheme.primary,
        colorScheme.secondary
    ) {
        if (useDarkHeroPalette) {
            listOf(
                colorScheme.surfaceVariant.copy(alpha = 0.94f),
                colorScheme.primary.copy(alpha = 0.32f),
                colorScheme.secondary.copy(alpha = 0.24f)
            )
        } else {
            PremiumDesignTokens.heroCardGradient
        }
    }

    GlassScreenWrapper {
        Crossfade(targetState = isLoading, label = "insightsLoading") { loading ->
            if (loading) {
                ShimmerLoadingScreen(modifier = Modifier.fillMaxSize())
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            StaggeredItem(index = 0) {
                                InsightsMomentumHero(
                                    weekCompletionPercent = uiState.weekCompletionPercent,
                                    currentStreak = uiState.currentStreak,
                                    bestDayOfWeek = uiState.bestDayOfWeek,
                                    heroGradient = heroGradient
                                )
                            }
                        }

                        item {
                            StaggeredItem(index = 1) {
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
                                        QuickStatItem(
                                            label = "This Week",
                                            value = "${uiState.weekCompletionPercent}%"
                                        )
                                        QuickStatItem(label = "Best Day", value = uiState.bestDayOfWeek)
                                        QuickStatItem(label = "Streak", value = "${uiState.currentStreak}d")
                                    }
                                }
                            }
                        }

                        item {
                            StaggeredItem(index = 2) {
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = ElevationLevel.Prominent,
                                    cornerRadius = 22.dp,
                                    enablePressScale = true,
                                    onClick = {
                                        if (effectivePremium) {
                                            onNavigateToFeature(Screen.Patterns)
                                        } else {
                                            onNavigateToPaywall()
                                        }
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = heroGradient
                                                )
                                            )
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PremiumSectionChip(
                                            text = if (effectivePremium) {
                                                "Analysis ready"
                                            } else {
                                                "Unlock full analysis"
                                            },
                                            icon = DailyWellIcons.Analytics.Pattern
                                        )
                                        Text(
                                            text = uiState.featuredInsight?.title
                                                ?: "Discover patterns in your habits",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = uiState.featuredInsight?.description
                                                ?: "See what causes your best and weakest days, then optimize your routine.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Open full insight board",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = if (effectivePremium) {
                                                    DailyWellIcons.Nav.ChevronRight
                                                } else {
                                                    DailyWellIcons.Status.Lock
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!simpleMode) {
                            item {
                                InsightFeatureCard(
                                    index = 3,
                                    icon = DailyWellIcons.Coaching.AICoach,
                                    title = "AI Insights",
                                    subtitle = "Personalized discoveries powered by AI",
                                    accentPalette = PremiumDesignTokens.insightsFeaturePalettes[0],
                                    effectivePremium = effectivePremium,
                                    onNavigateToFeature = { onNavigateToFeature(Screen.AIInsights) },
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                            item {
                                InsightFeatureCard(
                                    index = 4,
                                    icon = DailyWellIcons.Analytics.Calendar,
                                    title = "Calendar Sync",
                                    subtitle = "See habits overlaid on your calendar",
                                    accentPalette = PremiumDesignTokens.insightsFeaturePalettes[1],
                                    effectivePremium = effectivePremium,
                                    onNavigateToFeature = { onNavigateToFeature(Screen.Calendar) },
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                            item {
                                InsightFeatureCard(
                                    index = 5,
                                    icon = DailyWellIcons.Analytics.AtRisk,
                                    title = "At-Risk Alerts",
                                    subtitle = "Get warned before you break a streak",
                                    accentPalette = PremiumDesignTokens.insightsFeaturePalettes[2],
                                    effectivePremium = effectivePremium,
                                    onNavigateToFeature = { onNavigateToFeature(Screen.AtRisk) },
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                            item {
                                InsightFeatureCard(
                                    index = 6,
                                    icon = DailyWellIcons.Coaching.Reflection,
                                    title = "Reflections",
                                    subtitle = "Weekly review of your journey",
                                    accentPalette = PremiumDesignTokens.insightsFeaturePalettes[3],
                                    effectivePremium = effectivePremium,
                                    onNavigateToFeature = { onNavigateToFeature(Screen.Reflections) },
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }

                            if (!effectivePremium) {
                                item {
                                    StaggeredItem(index = 7) {
                                        GlassCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            elevation = ElevationLevel.Subtle,
                                            cornerRadius = 18.dp,
                                            enablePressScale = true,
                                            onClick = onNavigateToPaywall
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Unlock all premium insight engines",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Icon(
                                                    imageVector = DailyWellIcons.Status.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                StaggeredItem(index = 3) {
                                    SimpleInsightsHintCard()
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsMomentumHero(
    weekCompletionPercent: Int,
    currentStreak: Int,
    bestDayOfWeek: String,
    heroGradient: List<Color>
) {
    val cappedProgress = remember(weekCompletionPercent) {
        weekCompletionPercent.coerceIn(0, 100) / 100f
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Prominent,
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(heroGradient))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PremiumSectionChip(
                text = "Insights overview",
                icon = DailyWellIcons.Analytics.BarChart
            )
            Text(
                text = "${(cappedProgress * 100).toInt()}% weekly momentum",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            GlassProgressBar(
                progress = cappedProgress,
                progressColor = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Best Day",
                    value = bestDayOfWeek
                )
                MiniStatChip(
                    modifier = Modifier.weight(1f),
                    label = "Streak",
                    value = "${currentStreak}d"
                )
            }
        }
    }
}

@Composable
private fun MiniStatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    GlassCard(
        modifier = modifier,
        elevation = ElevationLevel.Subtle,
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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
private fun SimpleInsightsHintCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
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
                text = "Advanced analytics cards are hidden to reduce noise.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Switch to Full mode from Today to access AI Insights, Calendar, and At-Risk tools.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InsightFeatureCard(
    index: Int,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentPalette: PremiumPalette,
    effectivePremium: Boolean,
    onNavigateToFeature: () -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    StaggeredItem(index = index) {
        PremiumActionTile(
            icon = icon,
            title = title,
            subtitle = subtitle,
            accentColors = accentPalette.background,
            iconColor = accentPalette.iconColor,
            onClick = {
                if (effectivePremium) {
                    onNavigateToFeature()
                } else {
                    onNavigateToPaywall()
                }
            },
            showLock = !effectivePremium
        )
    }
}
