package com.dailywell.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import com.dailywell.app.core.theme.*
import com.dailywell.app.core.theme.PremiumDesignTokens

/**
 * 2026 Glassmorphism UI Components
 *
 * Implements warm, organic frosted-glass aesthetic with:
 * - Warm semi-transparent backgrounds
 * - Subtle gradient overlays for depth
 * - Soft borders with warm highlights
 * - Layered elevation hierarchy
 *
 * Design inspiration: iOS 18, macOS Sequoia,
 * modern wellness apps (Oura, Calm, Rise)
 */

// ==================== ELEVATION SYSTEM ====================

/**
 * Standardized elevation levels for consistent depth hierarchy
 */
enum class ElevationLevel(val elevation: Dp, val shadowAlpha: Float) {
    Flat(0.dp, 0f),
    Subtle(2.dp, 0.06f),
    Medium(4.dp, 0.10f),
    Prominent(6.dp, 0.14f),
    High(10.dp, 0.18f),
    Floating(14.dp, 0.22f)
}

// ==================== GLASS CARD ====================

/**
 * Primary glassmorphism card component
 *
 * Features:
 * - Semi-transparent background with gradient
 * - Subtle border highlight
 * - Soft shadow for depth
 * - Press scale animation (optional)
 *
 * @param modifier Modifier for the card
 * @param elevation Elevation level for depth
 * @param cornerRadius Corner radius for the card
 * @param enablePressScale Whether to animate on press
 * @param content Content inside the card
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    elevation: ElevationLevel = ElevationLevel.Medium,
    cornerRadius: Dp = 22.dp,
    enablePressScale: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    val shape = RoundedCornerShape(cornerRadius)

    // Glassmorphism background with gradient overlay
    val glassBackground = Brush.verticalGradient(
        colors = listOf(
            dailyWellColors.glassBackground,
            dailyWellColors.glassBackground.copy(alpha = 0.5f)
        )
    )

    val baseModifier = modifier
        .shadow(
            elevation = elevation.elevation,
            shape = shape,
            spotColor = dailyWellColors.shadowMedium
        )
        .clip(shape)
        .background(glassBackground)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    dailyWellColors.glassHighlight,
                    dailyWellColors.glassBorder
                )
            ),
            shape = shape
        )

    val finalModifier = if (enablePressScale) {
        baseModifier.pressScale()
    } else {
        baseModifier
    }

    Column(
        modifier = if (onClick != null) {
            finalModifier.then(
                Modifier.pointerInput(Unit) {
                    detectTapGestures { onClick() }
                }
            )
        } else {
            finalModifier
        }
    ) {
        content()
    }
}

/**
 * Elevated glass card with animated entrance
 */
@Composable
fun AnimatedGlassCard(
    modifier: Modifier = Modifier,
    elevation: ElevationLevel = ElevationLevel.Medium,
    cornerRadius: Dp = 20.dp,
    enterDelay: Int = 0,
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (enterDelay > 0) kotlinx.coroutines.delay(enterDelay.toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "glassAlpha"
    )

    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glassTranslation"
    )

    GlassCard(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        },
        elevation = elevation,
        cornerRadius = cornerRadius,
        content = content
    )
}

// ==================== ANIMATED GLASS HEADER ====================

/**
 * Animated gradient header with glassmorphism
 * Uses time-of-day colors for personalization
 *
 * @param modifier Modifier for the header
 * @param content Content inside the header
 */
@Composable
fun AnimatedGlassHeader(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    // Animated gradient offset
    val gradientOffset = rememberAnimatedGradientOffset()

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            dailyWellColors.timeGradientStart,
            dailyWellColors.timeGradientEnd,
            dailyWellColors.timeGradientStart
        ),
        startX = gradientOffset * -300f,
        endX = gradientOffset * 1000f + 500f
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBrush)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        content()
    }
}

// ==================== GLASS SURFACE ====================

/**
 * Subtle glass surface for backgrounds
 * Less prominent than GlassCard
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassBackground.copy(alpha = 0.4f),
                        dailyWellColors.glassBackground.copy(alpha = 0.2f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = dailyWellColors.glassBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

// ==================== ELEVATED CARD 2026 ====================

/**
 * Modern elevated card with proper shadow hierarchy
 * For use in lists and grids
 */
@Composable
fun ElevatedCard2026(
    modifier: Modifier = Modifier,
    elevation: ElevationLevel = ElevationLevel.Medium,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    Card(
        modifier = modifier
            .shadow(
                elevation = elevation.elevation,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = dailyWellColors.shadowMedium
            ),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

// ==================== GLASS CHIP ====================

/**
 * Small glassmorphism chip for tags and badges
 */
@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        dailyWellColors.glassBackground.copy(alpha = 0.6f),
                        dailyWellColors.glassBackground.copy(alpha = 0.4f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = dailyWellColors.glassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        content()
    }
}

/**
 * Shared section chip for tabs and filters.
 */
@Composable
fun PremiumSectionChip(
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(colors = PremiumDesignTokens.sectionChipGradient)
            )
            .border(
                width = 0.5.dp,
                color = PremiumDesignTokens.sectionChipBorder,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = PremiumDesignTokens.sectionChipText
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = PremiumDesignTokens.sectionChipText
        )
    }
}

/**
 * Shared action tile used for core tabs and CTA rows.
 */
@Composable
fun PremiumActionTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColors: List<Color>,
    iconColor: Color = Color.White,
    onClick: () -> Unit,
    showLock: Boolean = false
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp,
        enablePressScale = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(colors = accentColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (showLock) {
                    DailyWellIcons.Status.Lock
                } else {
                    DailyWellIcons.Nav.ChevronRight
                },
                contentDescription = if (showLock) "Locked" else "Open",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Shared primary CTA button with frozen premium gradient tokens.
 */
@Composable
fun PremiumCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    val textColor = if (enabled) Color.White else Color.White.copy(alpha = 0.75f)
    val gradientColors = if (enabled) {
        PremiumDesignTokens.ctaGradient
    } else {
        PremiumDesignTokens.ctaGradient.map { it.copy(alpha = 0.5f) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(colors = gradientColors))
            .border(
                width = 0.5.dp,
                color = PremiumDesignTokens.headerBorder.copy(alpha = if (enabled) 1f else 0.6f),
                shape = shape
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { onClick() }
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// ==================== FLOATING GLASS BUTTON ====================

/**
 * Floating action button with glassmorphism
 */
@Composable
fun GlassFloatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: ElevationLevel = ElevationLevel.Prominent,
    content: @Composable BoxScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = DailyWellSprings.Responsive,
        label = "fabScale"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = elevation.elevation,
                shape = RoundedCornerShape(28.dp),
                spotColor = dailyWellColors.shadowStrong
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        dailyWellColors.glassHighlight,
                        dailyWellColors.glassBackground
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
                shape = RoundedCornerShape(28.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ==================== GLASS DIVIDER ====================

/**
 * Subtle glass divider for section separation
 */
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier
) {
    val dailyWellColors = LocalDailyWellColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        dailyWellColors.glassBorder,
                        dailyWellColors.glassBorder,
                        Color.Transparent
                    )
                )
            )
    )
}

// ==================== TIME-OF-DAY GRADIENT BOX ====================

/**
 * Full-width gradient box that adapts to time of day
 * Use for headers and hero sections
 */
@Composable
fun TimeAwareGradientBox(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    val gradientBrush = if (animated) {
        val offset = rememberAnimatedGradientOffset(durationMs = 12000)
        Brush.horizontalGradient(
            colors = listOf(
                dailyWellColors.timeGradientStart,
                dailyWellColors.timeGradientEnd,
                dailyWellColors.timeGradientStart
            ),
            startX = offset * -500f,
            endX = offset * 1500f + 500f
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                dailyWellColors.timeGradientStart,
                dailyWellColors.timeGradientEnd
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBrush)
    ) {
        content()
    }
}

// ==================== GLASS SHEET ====================

/**
 * Bottom sheet style glass panel
 * For modal content, action sheets, overlays
 */
@Composable
fun GlassSheet(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    val shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = ElevationLevel.High.elevation,
                shape = shape,
                spotColor = dailyWellColors.shadowStrong
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassSheet,
                        dailyWellColors.glassSheet.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassHighlight,
                        Color.Transparent
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

// ==================== GLASS NAV BAR ====================

/**
 * Frosted glass navigation bar
 * Replaces traditional bottom nav with translucent glass
 */
@Composable
fun GlassNavBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = dailyWellColors.shadowMedium,
                ambientColor = dailyWellColors.shadowLight
            )
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassNavBar,
                        dailyWellColors.glassNavBar.copy(alpha = 0.95f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassHighlight.copy(alpha = 0.3f),
                        dailyWellColors.glassBorder
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

// ==================== GLASS LIST ITEM ====================

/**
 * Glass-styled list row for settings, features, etc.
 */
@Composable
fun GlassListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        dailyWellColors.glassBackground.copy(alpha = 0.5f),
                        dailyWellColors.glassBackground.copy(alpha = 0.3f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = dailyWellColors.glassBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures { onClick() }
                    }
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        content()
    }
}

// ==================== GLASS PROGRESS BAR ====================

/**
 * Glass-styled progress bar with glow effect
 */
@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    trackColor: Color = Color.Transparent,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    val dailyWellColors = LocalDailyWellColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glassProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(
                if (trackColor == Color.Transparent) {
                    dailyWellColors.glassBorder.copy(alpha = 0.3f)
                } else trackColor
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            progressColor,
                            progressColor.copy(alpha = 0.8f)
                        )
                    )
                )
        )
    }
}

// ==================== GLASS ICON BUTTON ====================

/**
 * Glass-styled icon button
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = DailyWellSprings.Responsive,
        label = "glassIconScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(size / 2))
            .background(dailyWellColors.glassBackground.copy(alpha = 0.6f))
            .border(
                width = 0.5.dp,
                color = dailyWellColors.glassBorder,
                shape = RoundedCornerShape(size / 2)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ==================== GLASS TEXT FIELD ====================

/**
 * Glass-styled text field wrapper
 * Wraps content with a glass border appearance
 */
@Composable
fun GlassTextFieldContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(dailyWellColors.glassBackground.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = dailyWellColors.glassBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}

// ==================== COLLAPSIBLE SECTION ====================

/**
 * Collapsible section for Settings and long lists
 * With animated chevron and smooth expand/collapse
 */
@Composable
fun CollapsibleSection(
    title: String,
    icon: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    // Animated chevron rotation
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chevron"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isExpanded) {
                        dailyWellColors.timeGradientStart.copy(alpha = 0.1f)
                    } else {
                        Color.Transparent
                    }
                )
                .pointerInput(Unit) {
                    detectTapGestures { onToggle() }
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Animated chevron
            Text(
                text = "\u25BC",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    rotationZ = chevronRotation
                }
            )
        }

        // Animated content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                content()
            }
        }
    }
}
