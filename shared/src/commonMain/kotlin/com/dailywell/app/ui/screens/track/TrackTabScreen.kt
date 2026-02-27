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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun TrackTabScreen(
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.getSettings().collectAsState(initial = UserSettings())
    val simpleMode = settings.todayViewMode == TodayViewMode.SIMPLE

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        isLoading = false
    }

    GlassScreenWrapper {
        Crossfade(targetState = isLoading, label = "trackLoading") { loading ->
            if (loading) {
                ShimmerLoadingScreen(modifier = Modifier.fillMaxSize())
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
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

                    val coreFeatures = listOf(
                        feature(
                            title = "Food Scan",
                            subtitle = "Scan meals and log quickly",
                            icon = DailyWellIcons.Health.FoodScan,
                            screen = Screen.FoodScanning,
                            requiresPremium = false,
                            paletteIndex = 0
                        ),
                        feature(
                            title = "Water Tracker",
                            subtitle = "Hydration history and daily goal",
                            icon = DailyWellIcons.Health.WaterDrop,
                            screen = Screen.WaterTracking,
                            requiresPremium = false,
                            paletteIndex = 4
                        ),
                        feature(
                            title = "Nutrition Tracker",
                            subtitle = "Meals, calories, and macro totals",
                            icon = DailyWellIcons.Health.Nutrition,
                            screen = Screen.Nutrition,
                            requiresPremium = true,
                            paletteIndex = 1
                        )
                    )
                    val fullModeFeatures = listOf(
                        feature(
                            title = "Workout Log",
                            subtitle = "Capture sessions fast with set details",
                            icon = DailyWellIcons.Health.Workout,
                            screen = Screen.WorkoutLog,
                            requiresPremium = true,
                            paletteIndex = 2
                        ),
                        feature(
                            title = "Body Metrics",
                            subtitle = "Weight, measurements, and progress photos",
                            icon = DailyWellIcons.Health.Weight,
                            screen = Screen.BodyMetrics,
                            requiresPremium = true,
                            paletteIndex = 5
                        ),
                        feature(
                            title = "Workout History",
                            subtitle = "Review volume, progress, and previous sessions",
                            icon = DailyWellIcons.Analytics.Pattern,
                            screen = Screen.WorkoutHistory,
                            requiresPremium = true,
                            paletteIndex = 3
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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            StaggeredItem(index = 0) {
                                PremiumSectionChip(
                                    text = "Core tracking",
                                    icon = DailyWellIcons.Health.FoodScan
                                )
                            }
                        }

                        itemsIndexed(coreFeatures) { index, feature ->
                            StaggeredItem(index = index + 1) {
                                FeatureCard(
                                    feature = feature,
                                    isPremium = isPremium,
                                    onNavigateToFeature = onNavigateToFeature,
                                    onNavigateToPaywall = onNavigateToPaywall
                                )
                            }
                        }

                        if (!simpleMode) {
                            item {
                                StaggeredItem(index = 5) {
                                    PremiumSectionChip(
                                        text = "Advanced tracking",
                                        icon = DailyWellIcons.Analytics.Pattern
                                    )
                                }
                            }

                            itemsIndexed(fullModeFeatures) { index, feature ->
                                StaggeredItem(index = index + 6) {
                                    FeatureCard(
                                        feature = feature,
                                        isPremium = isPremium,
                                        onNavigateToFeature = onNavigateToFeature,
                                        onNavigateToPaywall = onNavigateToPaywall
                                    )
                                }
                            }
                        } else {
                            item {
                                StaggeredItem(index = 5) {
                                    SimpleModeHintCard()
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
private fun SimpleModeHintCard() {
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
                text = "Track is reduced to core tools only: Scan, Water, and Nutrition.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Switch to Full mode from Today for workouts, body metrics, and biometrics.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
