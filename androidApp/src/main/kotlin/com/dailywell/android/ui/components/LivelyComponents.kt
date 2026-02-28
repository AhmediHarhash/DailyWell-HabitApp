package com.dailywell.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lively UI Components with beautiful animations
 * Makes the app feel alive, modern, and premium
 */

// ==================== COACH AVATAR COMPONENT ====================

/**
 * Animated coach avatar with subtle breathing animation
 */
@Composable
fun CoachAvatar(
    coachId: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showBorder: Boolean = true,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val drawableRes = DrawableResources.getCoachDrawable(coachId)

    // Subtle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.03f else 1.01f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Glow animation for active state
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect for active coaches
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(size + 8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Coach Avatar",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (showBorder) Modifier.border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6),
                                Color(0xFFEC4899)
                            )
                        ),
                        shape = CircleShape
                    ) else Modifier
                )
                .shadow(8.dp, CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

// ==================== HABIT ICON COMPONENT ====================

/**
 * Animated habit icon with completion celebration
 */
@Composable
fun HabitIcon(
    habitId: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isCompleted: Boolean = false,
    streakDays: Int = 0,
    onClick: (() -> Unit)? = null
) {
    val drawableRes = DrawableResources.getHabitDrawable(habitId)

    // Completion animation
    var showCelebration by remember { mutableStateOf(false) }
    val celebrationScale by animateFloatAsState(
        targetValue = if (showCelebration) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { showCelebration = false },
        label = "celebration"
    )

    // Streak fire animation
    val infiniteTransition = rememberInfiniteTransition(label = "streak")
    val fireGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire"
    )

    LaunchedEffect(isCompleted) {
        if (isCompleted) showCelebration = true
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(celebrationScale)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Streak glow for 7+ days
        if (streakDays >= 7) {
            Box(
                modifier = Modifier
                    .size(size + 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6B35).copy(alpha = fireGlow * 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Habit Icon",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp))
                .shadow(if (isCompleted) 12.dp else 4.dp, RoundedCornerShape(16.dp))
                .then(
                    if (isCompleted) Modifier.border(
                        width = 2.dp,
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(16.dp)
                    ) else Modifier
                ),
            contentScale = ContentScale.Crop,
            alpha = if (isCompleted) 1f else 0.8f
        )

        // Completion checkmark overlay
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2713",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Streak badge
        if (streakDays >= 3) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            streakDays >= 30 -> Color(0xFFFFD700)
                            streakDays >= 7 -> Color(0xFFFF6B35)
                            else -> Color(0xFF6366F1)
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$streakDays",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==================== ACHIEVEMENT BADGE COMPONENT ====================

/**
 * Premium animated achievement badge
 */
@Composable
fun AchievementBadge(
    badgeName: String,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isUnlocked: Boolean = true,
    showShine: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val drawableRes = DrawableResources.getBadgeDrawable(badgeName)

    // Shine animation
    val infiniteTransition = rememberInfiniteTransition(label = "shine")
    val shineOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shineOffset"
    )

    // Hover/bounce animation
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = if (isUnlocked) -bounceOffset else 0f
            }
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Achievement Badge",
            modifier = Modifier
                .size(size)
                .shadow(if (isUnlocked) 16.dp else 4.dp, CircleShape)
                .graphicsLayer {
                    alpha = if (isUnlocked) 1f else 0.4f
                },
            contentScale = ContentScale.Fit
        )

        // Shine overlay for unlocked badges
        if (isUnlocked && showShine) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .graphicsLayer {
                        rotationZ = 45f
                        translationX = shineOffset * size.value * 2
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ==================== SECTION HEADER WITH BACKGROUND ====================

/**
 * Premium section header with animated background
 */
@Composable
fun SectionHeader(
    section: DrawableResources.AppSection,
    title: String,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    val backgroundRes = DrawableResources.getSectionBackground(section)

    // Parallax breathing effect
    val infiniteTransition = rememberInfiniteTransition(label = "parallax")
    val parallaxScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "parallax"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
        contentAlignment = Alignment.BottomStart
    ) {
        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .scale(parallaxScale),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        Text(
            text = title,
            modifier = Modifier.padding(24.dp),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

// ==================== LOADING SHIMMER ====================

/**
 * Premium loading shimmer effect
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFFE5E7EB))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shimmerOffset * 500
                }
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// ==================== CONFETTI CELEBRATION ====================

/**
 * Celebration confetti for achievements
 * Call this when user unlocks something
 */
@Composable
fun ConfettiCelebration(
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    if (!isActive) return

    val particles = List(20) { ConfettiParticle() }

    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val infiniteTransition = rememberInfiniteTransition(label = "confetti")
            val yOffset by infiniteTransition.animateFloat(
                initialValue = -100f,
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = particle.duration,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "y"
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(particle.duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = particle.x.dp.roundToPx(),
                            y = yOffset.dp.roundToPx()
                        )
                    }
                    .size(particle.size.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .background(particle.color, CircleShape)
            )
        }
    }
}

private data class ConfettiParticle(
    val x: Float = (0..400).random().toFloat(),
    val size: Float = (4..12).random().toFloat(),
    val duration: Int = (1500..3000).random(),
    val color: Color = listOf(
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF10B981),
        Color(0xFFFFD700),
        Color(0xFFFF6B35)
    ).random()
)

// ==================== PULSING DOT INDICATOR ====================

/**
 * Pulsing dot indicator for "live" status
 */
@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color = Color(0xFF10B981)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ==================== ANIMATED GRADIENT CARD ====================

/**
 * Card with animated gradient border
 */
@Composable
fun GradientBorderCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientShift"
    )

    val colors = listOf(
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF6366F1)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = colors,
                    start = androidx.compose.ui.geometry.Offset(
                        gradientShift * 500,
                        0f
                    ),
                    end = androidx.compose.ui.geometry.Offset(
                        gradientShift * 500 + 500,
                        500f
                    )
                )
            )
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            content()
        }
    }
}
