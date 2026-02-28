package com.dailywell.app.ui.screens.discover

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTabHeader
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.components.rememberAnimatedGradientOffset
import com.dailywell.app.ui.navigation.FeatureCategory
import com.dailywell.app.ui.navigation.FeatureId
import com.dailywell.app.ui.navigation.Screen
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Discover Screen - Features hub replacing hidden settings navigation
 * Shows core features organized by category with premium indicators
 */
@Composable
fun DiscoverScreen(
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTabHeader(
                    title = "Explore",
                    subtitle = "Discover tools for better habits"
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Header
                item {
                    DiscoverHeader()
                }

                item {
                    PremiumSectionChip(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        text = "Feature Hub",
                        icon = DailyWellIcons.Habits.HabitStacking
                    )
                }

                // Featured Card - AI Coach highlight
                item {
                    FeaturedFeatureCard(
                        isPremium = isPremium,
                        onNavigateToFeature = onNavigateToFeature,
                        onNavigateToPaywall = onNavigateToPaywall
                    )
                }

                // Quick Actions Row
                item {
                    QuickActionsRow(
                        isPremium = isPremium,
                        onNavigateToFeature = onNavigateToFeature,
                        onNavigateToPaywall = onNavigateToPaywall
                    )
                }

                // Feature categories
                FeatureCategory.entries.forEach { category ->
                    item {
                        FeatureCategorySection(
                            category = category,
                            isPremium = isPremium,
                            onNavigateToFeature = onNavigateToFeature,
                            onNavigateToPaywall = onNavigateToPaywall
                        )
                    }
                }

                // Premium upgrade card for free users
                if (!isPremium) {
                    item {
                        PremiumUpgradeCard(onNavigateToPaywall = onNavigateToPaywall)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverHeader() {
    val dailyWellColors = LocalDailyWellColors.current
    val gradientOffset = rememberAnimatedGradientOffset(durationMs = 8000)

    // Animated time-of-day gradient header
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        dailyWellColors.timeGradientStart,
                        dailyWellColors.timeGradientEnd,
                        dailyWellColors.timeGradientStart
                    ),
                    startX = gradientOffset * -300f,
                    endX = gradientOffset * 1000f + 500f
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = dailyWellColors.highContrastOnPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Explore all the ways to build better habits",
                style = MaterialTheme.typography.bodyMedium,
                color = dailyWellColors.highContrastOnPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun FeaturedFeatureCard(
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    var isPressed by remember { mutableStateOf(false) }

    // Press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "featuredScale"
    )

    // Animated gradient shimmer
    val infiniteTransition = rememberInfiniteTransition(label = "featuredShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .scale(scale)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0xFF6366F1).copy(alpha = glowAlpha)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFF6366F1)
                    ),
                    startX = shimmerOffset * -500f,
                    endX = shimmerOffset * 1500f + 500f
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                if (isPremium) {
                    onNavigateToFeature(Screen.AICoaching)
                } else {
                    onNavigateToPaywall()
                }
            }
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI Coach",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!isPremium) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Get personalized advice from your AI habit coach",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // CTA Button with shadow
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "Start chatting →",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Animated icon with scale
            val iconScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "iconScale"
            )
            Icon(
                imageVector = DailyWellIcons.Coaching.Chat,
                contentDescription = "AI Coach",
                modifier = Modifier.size(56.dp).scale(iconScale),
                tint = Color.White
            )
        }
    }

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun QuickActionsRow(
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val quickActions = listOf(
                Triple(FeatureId.AI_COACHING, "Chat", Color(0xFF6366F1)),
                Triple(FeatureId.PATTERNS, "Insights", Color(0xFF10B981)),
                Triple(FeatureId.GAMIFICATION, "Rewards", Color(0xFFF59E0B)),
                Triple(FeatureId.BIOMETRIC, "Recovery", Color(0xFFEF4444))
            )

            items(quickActions) { (feature, label, color) ->
                QuickActionButton(
                    icon = feature.icon,
                    label = label,
                    color = color,
                    isLocked = feature.isPremium && !isPremium,
                    onClick = {
                        if (feature.isPremium && !isPremium) {
                            onNavigateToPaywall()
                        } else {
                            onNavigateToFeature(feature.screen)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    var isPressed by remember { mutableStateOf(false) }

    // Press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "quickActionScale"
    )

    // Subtle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "quickActionBreathing")
    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
            .padding(8.dp)
    ) {
        // Glassmorphism circle with shadow
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = dailyWellColors.shadowMedium
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = breathingAlpha + 0.08f),
                            color.copy(alpha = breathingAlpha)
                        )
                    ),
                    CircleShape
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            dailyWellColors.glassHighlight,
                            dailyWellColors.glassBorder
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = color
            )
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .shadow(2.dp, CircleShape)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, dailyWellColors.glassBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Status.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun FeatureCategorySection(
    category: FeatureCategory,
    isPremium: Boolean,
    onNavigateToFeature: (Screen) -> Unit,
    onNavigateToPaywall: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    val features = FeatureId.entries.filter { it.category == category }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        // Category header with subtle accent
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dailyWellColors.timeGradientStart,
                                dailyWellColors.timeGradientEnd
                            )
                        )
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            features.forEachIndexed { index, feature ->
                // Staggered entrance animation for each feature
                StaggeredItem(
                    index = index,
                    delayPerItem = 50L,
                    baseDelay = 100L
                ) {
                    FeatureCard(
                        feature = feature,
                        isLocked = feature.isPremium && !isPremium,
                        onClick = {
                            if (feature.isPremium && !isPremium) {
                                onNavigateToPaywall()
                            } else {
                                onNavigateToFeature(feature.screen)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: FeatureId,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    var isPressed by remember { mutableStateOf(false) }

    // Press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "featureCardScale"
    )

    // Elevation animation
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        animationSpec = tween(150),
        label = "featureCardElevation"
    )

    // Glassmorphism card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                spotColor = dailyWellColors.shadowMedium
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassBackground,
                        dailyWellColors.glassBackground.copy(alpha = 0.6f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassHighlight,
                        dailyWellColors.glassBorder
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            }
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with gradient background
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = dailyWellColors.glassHighlight,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = feature.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = dailyWellColors.timeAccent.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "PRO",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = dailyWellColors.timeAccent
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = feature.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Animated chevron
            val chevronTranslation by animateFloatAsState(
                targetValue = if (isPressed) 4f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "chevronTranslation"
            )

            Icon(
                imageVector = if (isLocked) DailyWellIcons.Status.Lock else DailyWellIcons.Nav.ChevronRight,
                contentDescription = if (isLocked) "Locked" else "Open",
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        translationX = chevronTranslation
                    },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun PremiumUpgradeCard(onNavigateToPaywall: () -> Unit) {
    val dailyWellColors = LocalDailyWellColors.current
    var isPressed by remember { mutableStateOf(false) }

    // Press animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "premiumScale"
    )

    // Animated gradient
    val infiniteTransition = rememberInfiniteTransition(label = "premiumShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "premiumShimmerOffset"
    )

    // Glow pulse
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premiumGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .scale(scale)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0xFFEF4444).copy(alpha = glowAlpha)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFF59E0B),
                        Color(0xFFEF4444),
                        Color(0xFFF59E0B)
                    ),
                    startX = shimmerOffset * -500f,
                    endX = shimmerOffset * 1500f + 500f
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onNavigateToPaywall()
            }
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Crown emoji with scale animation
                val crownScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "crownScale"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.Gamification.Crown,
                        contentDescription = "Premium",
                        modifier = Modifier.size(28.dp).scale(crownScale),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unlock All Features",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Get AI coaching, advanced insights, and more with DailyWell Premium",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.95f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "Upgrade Now →",
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "$9.99/month",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Cancel anytime",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Reset press state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}
