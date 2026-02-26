package com.dailywell.app.ui.screens.track

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.PremiumDesignTokens
import com.dailywell.app.core.theme.PremiumPalette
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.ElevationLevel
import com.dailywell.app.ui.components.GlassCard
import com.dailywell.app.ui.components.GlassProgressBar
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumActionTile
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTabHeader
import com.dailywell.app.ui.components.ShimmerLoadingScreen
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.navigation.Screen

@Composable
fun TrackTabScreen(
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
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

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        isLoading = false
    }

    GlassScreenWrapper {
        Crossfade(targetState = isLoading, label = "trackLoading") { loading ->
            if (loading) {
                ShimmerLoadingScreen(modifier = Modifier.fillMaxSize())
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    PremiumTabHeader(
                        title = "Track",
                        subtitle = "Core logs first. Scan is optional.",
                        includeStatusBarPadding = true
                    )

                    val quickActions = listOf(
                        QuickActionItem(
                            icon = DailyWellIcons.Health.Nutrition,
                            label = "Meals",
                            route = Screen.Nutrition,
                            requiresPremium = true,
                            palette = PremiumDesignTokens.trackQuickActionPalettes[0]
                        ),
                        QuickActionItem(
                            icon = DailyWellIcons.Health.WaterDrop,
                            label = "Water",
                            route = Screen.WaterTracking,
                            requiresPremium = false,
                            palette = PremiumDesignTokens.trackQuickActionPalettes[1]
                        ),
                        QuickActionItem(
                            icon = DailyWellIcons.Health.Weight,
                            label = "Metrics",
                            route = Screen.BodyMetrics,
                            requiresPremium = true,
                            palette = PremiumDesignTokens.trackQuickActionPalettes[2]
                        )
                    )

                    val palettes = PremiumDesignTokens.trackFeaturePalettes
                    fun feature(
                        title: String,
                        subtitle: String,
                        icon: ImageVector,
                        screen: Screen,
                        requiresPremium: Boolean,
                        paletteIndex: Int
                    ) = FeatureItem(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        screen = screen,
                        requiresPremium = requiresPremium,
                        palette = palettes[paletteIndex % palettes.size]
                    )

                    val primaryFeature = feature(
                        title = "Nutrition Tracker",
                        subtitle = "Meals, calories, and macro totals",
                        icon = DailyWellIcons.Health.Nutrition,
                        screen = Screen.Nutrition,
                        requiresPremium = true,
                        paletteIndex = 1
                    )
                    val mealFeatures = listOf(
                        feature(
                            title = "Water Tracker",
                            subtitle = "Hydration history and daily goal",
                            icon = DailyWellIcons.Health.WaterDrop,
                            screen = Screen.WaterTracking,
                            requiresPremium = false,
                            paletteIndex = 4
                        )
                    )
                    val historyFeatures = listOf(
                        feature(
                            title = "Workout Log",
                            subtitle = "Capture sessions fast with set details",
                            icon = DailyWellIcons.Health.Workout,
                            screen = Screen.WorkoutLog,
                            requiresPremium = true,
                            paletteIndex = 2
                        ),
                        feature(
                            title = "Workout History",
                            subtitle = "Review volume, progress, and previous sessions",
                            icon = DailyWellIcons.Analytics.Pattern,
                            screen = Screen.WorkoutHistory,
                            requiresPremium = true,
                            paletteIndex = 3
                        )
                    )
                    val metricFeatures = listOf(
                        feature(
                            title = "Body Metrics",
                            subtitle = "Weight, measurements, and progress photos",
                            icon = DailyWellIcons.Health.Weight,
                            screen = Screen.BodyMetrics,
                            requiresPremium = true,
                            paletteIndex = 5
                        ),
                        feature(
                            title = "Biometrics",
                            subtitle = "Sleep, HRV, and recovery signals",
                            icon = DailyWellIcons.Health.Biometric,
                            screen = Screen.Biometric,
                            requiresPremium = true,
                            paletteIndex = 0
                        )
                    )
                    val scanFeatures = listOf(
                        feature(
                            title = "AI Food Scanner",
                            subtitle = "Snap and log meals instantly",
                            icon = DailyWellIcons.Health.FoodScan,
                            screen = Screen.FoodScanning,
                            requiresPremium = true,
                            paletteIndex = 0
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            StaggeredItem(index = 0) {
                                TrackHealthHubHero(
                                    isPremium = isPremium,
                                    quickActionsCount = quickActions.size,
                                    moduleCount = 1 + mealFeatures.size + historyFeatures.size + metricFeatures.size + scanFeatures.size,
                                    heroGradient = heroGradient
                                )
                            }
                        }

                        item {
                            StaggeredItem(index = 1) {
                                PremiumSectionChip(
                                    text = "Quick actions",
                                    icon = DailyWellIcons.Nav.ChevronRight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                QuickActionsRow(
                                    actions = quickActions,
                                    isPremium = isPremium,
                                    onNavigateToFeature = onNavigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                        }

                        item {
                            StaggeredItem(index = 2) {
                                PremiumSectionChip(
                                    text = "Start here",
                                    icon = DailyWellIcons.Health.Nutrition
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FeatureCard(
                                    feature = primaryFeature,
                                    isPremium = isPremium,
                                    onNavigateToFeature = onNavigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                        }

                        item {
                            StaggeredItem(index = 3) {
                                PremiumSectionChip(
                                    text = "Daily essentials",
                                    icon = DailyWellIcons.Health.Nutrition
                                )
                            }
                        }

                        itemsIndexed(mealFeatures) { index, feature ->
                            StaggeredItem(index = index + 4) {
                                FeatureCard(
                                    feature = feature,
                                    isPremium = isPremium,
                                    onNavigateToFeature = onNavigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                        }

                        item {
                            StaggeredItem(index = 6) {
                                PremiumSectionChip(
                                    text = "Progress & history",
                                    icon = DailyWellIcons.Analytics.Pattern
                                )
                            }
                        }

                        itemsIndexed(historyFeatures) { index, feature ->
                            StaggeredItem(index = index + 7) {
                                FeatureCard(
                                    feature = feature,
                                    isPremium = isPremium,
                                    onNavigateToFeature = onNavigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                        }

                        item {
                            StaggeredItem(index = 9) {
                                PremiumSectionChip(
                                    text = "Body & recovery metrics",
                                    icon = DailyWellIcons.Health.Weight
                                )
                            }
                        }

                        itemsIndexed(metricFeatures) { index, feature ->
                            StaggeredItem(index = index + 10) {
                                FeatureCard(
                                    feature = feature,
                                    isPremium = isPremium,
                                    onNavigateToFeature = onNavigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                        }

                        item {
                            StaggeredItem(index = 12) {
                                PremiumSectionChip(
                                    text = "Food tools (advanced)",
                                    icon = DailyWellIcons.Health.FoodScan
                                )
                            }
                        }

                        itemsIndexed(scanFeatures) { index, feature ->
                            StaggeredItem(index = index + 13) {
                                FeatureCard(
                                    feature = feature,
                                    isPremium = isPremium,
                                    onNavigateToFeature = onNavigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
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
private fun TrackHealthHubHero(
    isPremium: Boolean,
    quickActionsCount: Int,
    moduleCount: Int,
    heroGradient: List<Color>
) {
    val unlockedRatio = remember(isPremium, moduleCount) {
        if (isPremium) 1f else 0.45f
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
                text = "Tracking overview",
                icon = DailyWellIcons.Nav.Track
            )
            Text(
                text = "$moduleCount tracking modules, $quickActionsCount quick actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (isPremium) {
                    "Everything is unlocked for fast daily logging."
                } else {
                    "Upgrade to unlock full scan, nutrition, and body intelligence."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GlassProgressBar(
                progress = unlockedRatio,
                progressColor = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.Actions.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "${(unlockedRatio * 100).toInt()}% unlocked",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                PremiumSectionChip(
                    text = if (isPremium) "Premium active" else "Premium required",
                    icon = if (isPremium) DailyWellIcons.Actions.CheckCircle else DailyWellIcons.Status.Lock
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    actions: List<QuickActionItem>,
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            QuickActionButton(
                action = action,
                isPremium = isPremium,
                onNavigateToFeature = onNavigateToFeature,
                onNavigateToPaywall = onNavigateToPaywall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    action: QuickActionItem,
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        elevation = ElevationLevel.Subtle,
        cornerRadius = 18.dp,
        enablePressScale = true,
        onClick = {
            if (action.requiresPremium && !isPremium) {
                onNavigateToPaywall()
            } else {
                onNavigateToFeature(action.route)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.horizontalGradient(action.palette.background),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = action.palette.iconColor
                )
            }

            Text(
                text = action.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            if (action.requiresPremium && !isPremium) {
                Text(
                    text = "Premium",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    PremiumActionTile(
        icon = feature.icon,
        title = feature.title,
        subtitle = feature.subtitle,
        accentColors = feature.palette.background,
        iconColor = feature.palette.iconColor,
        showLock = feature.requiresPremium && !isPremium,
        onClick = {
            if (feature.requiresPremium && !isPremium) {
                onNavigateToPaywall()
            } else {
                onNavigateToFeature(feature.screen)
            }
        }
    )
}

private data class FeatureItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val screen: Screen,
    val requiresPremium: Boolean,
    val palette: PremiumPalette
)

private data class QuickActionItem(
    val icon: ImageVector,
    val label: String,
    val route: Screen,
    val requiresPremium: Boolean,
    val palette: PremiumPalette
)
