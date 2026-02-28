package com.dailywell.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.core.theme.PremiumMotionTokens
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 2026 Modern UI Components
 *
 * Implements cutting-edge design patterns:
 * - Confetti celebrations
 * - Apple Watch-style progress rings
 * - Neumorphism 2.0
 * - Glowing accents
 * - Gradient typography
 * - Animated streak flames
 */

// ==================== CONFETTI CELEBRATION ====================

/**
 * Confetti explosion effect for habit completion
 * Simplified implementation using Canvas with sparkle particles
 */
@Composable
fun ConfettiEffect(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 50,
    colors: List<Color> = listOf(
        Color(0xFFFF6B6B), // Red
        Color(0xFF4ECDC4), // Teal
        Color(0xFFFFE66D), // Yellow
        Color(0xFF95E1D3), // Mint
        Color(0xFFF38181), // Coral
        Color(0xFFAA96DA), // Purple
        Color(0xFF7FD8BE), // Green
        Color(0xFFFFAA71)  // Orange
    ),
    durationMs: Int = 2500
) {
    // Animation progress
    val animatedProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMs, easing = LinearEasing),
        label = "confetti"
    )

    // Generate stable random values for particles
    val particleData = remember(isActive) {
        if (isActive) {
            List(particleCount) { index ->
                val random = Random(index * 1000 + System.currentTimeMillis().toInt())
                ParticleData(
                    startX = 0.5f + random.nextFloat() * 0.2f - 0.1f,
                    startY = 0.3f,
                    velocityX = random.nextFloat() * 0.4f - 0.2f,
                    velocityY = random.nextFloat() * -0.3f - 0.1f,
                    rotation = random.nextFloat() * 360f,
                    rotationSpeed = random.nextFloat() * 10f - 5f,
                    colorIndex = random.nextInt(colors.size),
                    particleSize = random.nextFloat() * 12f + 6f,
                    shapeType = random.nextInt(4)
                )
            }
        } else {
            emptyList()
        }
    }

    if (isActive && animatedProgress < 1f && particleData.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val gravity = 0.002f
            val canvasWidth = size.width
            val canvasHeight = size.height

            particleData.forEach { p ->
                val time = animatedProgress * 60f

                val currentX = p.startX + p.velocityX * time
                val currentY = p.startY + p.velocityY * time + gravity * time * time
                val currentRotation = p.rotation + p.rotationSpeed * time
                val alpha = (1f - animatedProgress).coerceIn(0f, 1f)
                val particleColor = colors[p.colorIndex].copy(alpha = alpha)

                if (currentY < 1.2f) {
                    val screenX = currentX * canvasWidth
                    val screenY = currentY * canvasHeight

                    rotate(currentRotation, pivot = Offset(screenX, screenY)) {
                        when (p.shapeType) {
                            0 -> drawCircle(
                                color = particleColor,
                                radius = p.particleSize,
                                center = Offset(screenX, screenY)
                            )
                            1 -> drawRect(
                                color = particleColor,
                                topLeft = Offset(screenX - p.particleSize / 2, screenY - p.particleSize / 2),
                                size = Size(p.particleSize, p.particleSize)
                            )
                            2 -> drawConfettiStar(particleColor, Offset(screenX, screenY), p.particleSize)
                            else -> drawRect(
                                color = particleColor,
                                topLeft = Offset(screenX - p.particleSize / 4, screenY - p.particleSize),
                                size = Size(p.particleSize / 2, p.particleSize * 2)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ParticleData(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val colorIndex: Int,
    val particleSize: Float,
    val shapeType: Int
)

private fun DrawScope.drawConfettiStar(color: Color, center: Offset, starSize: Float) {
    val path = Path()
    val points = 5
    val outerRadius = starSize
    val innerRadius = starSize * 0.4f

    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = kotlin.math.PI * i / points - kotlin.math.PI / 2
        val px = center.x + (radius * cos(angle)).toFloat()
        val py = center.y + (radius * sin(angle)).toFloat()

        if (i == 0) path.moveTo(px, py)
        else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color)
}

// ==================== ANIMATED PROGRESS RING (Apple Watch Style) ====================

/**
 * Apple Watch-inspired progress ring with smooth animations
 *
 * @param progress Current progress (0f to 1f)
 * @param modifier Modifier
 * @param ringColor Color of the progress ring
 * @param backgroundColor Color of the background ring
 * @param strokeWidth Width of the ring stroke
 * @param glowEnabled Whether to show glow effect
 */
@Composable
fun AnimatedProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    ringColor: Color = Color(0xFF4CD964), // Apple green
    backgroundColor: Color = Color(0xFF2C2C2E),
    strokeWidth: Dp = 12.dp,
    glowEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ringProgress"
    )

    // Glow pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "ringGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Background ring
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // Glow effect (drawn before progress ring)
            if (glowEnabled && animatedProgress > 0) {
                drawArc(
                    color = ringColor.copy(alpha = glowAlpha * 0.5f),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius - strokeWidthPx, center.y - radius - strokeWidthPx),
                    size = Size((radius + strokeWidthPx) * 2, (radius + strokeWidthPx) * 2),
                    style = Stroke(width = strokeWidthPx * 2, cap = StrokeCap.Round)
                )
            }

            // Progress ring
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )

            // End cap glow
            if (glowEnabled && animatedProgress > 0.01f) {
                val endAngle = Math.toRadians((-90f + animatedProgress * 360f).toDouble())
                val endX = center.x + radius * cos(endAngle).toFloat()
                val endY = center.y + radius * sin(endAngle).toFloat()

                drawCircle(
                    color = ringColor.copy(alpha = glowAlpha),
                    radius = strokeWidthPx,
                    center = Offset(endX, endY)
                )
            }
        }

        content()
    }
}

/**
 * Triple ring progress (like Apple Activity rings)
 */
@Composable
fun ActivityRings(
    moveProgress: Float,
    exerciseProgress: Float,
    standProgress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring - Move (Red)
        AnimatedProgressRing(
            progress = moveProgress,
            ringColor = Color(0xFFFA114F),
            strokeWidth = 14.dp,
            modifier = Modifier.fillMaxSize()
        )

        // Middle ring - Exercise (Green)
        AnimatedProgressRing(
            progress = exerciseProgress,
            ringColor = Color(0xFF92E82A),
            strokeWidth = 14.dp,
            modifier = Modifier.fillMaxSize(0.75f)
        )

        // Inner ring - Stand (Blue)
        AnimatedProgressRing(
            progress = standProgress,
            ringColor = Color(0xFF1EEAEF),
            strokeWidth = 14.dp,
            modifier = Modifier.fillMaxSize(0.5f)
        )
    }
}

// ==================== NEUMORPHISM 2.0 ====================

/**
 * Neumorphic card with soft pressed/raised effect
 *
 * @param isPressed Whether the card appears pressed inward
 * @param modifier Modifier
 * @param cornerRadius Corner radius
 * @param content Content
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    isPressed: Boolean = false,
    cornerRadius: Dp = 20.dp,
    lightShadowColor: Color = Color.White.copy(alpha = 0.7f),
    darkShadowColor: Color = Color.Black.copy(alpha = 0.15f),
    backgroundColor: Color = Color(0xFFF0F0F3),
    content: @Composable BoxScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current

    // Animate pressed state
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "neumorphElevation"
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = darkShadowColor,
                spotColor = darkShadowColor
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .drawBehind {
                // Inner highlight (top-left)
                if (!isPressed) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                lightShadowColor,
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width * 0.5f, size.height * 0.5f)
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx())
                    )
                }
            }
            .then(
                if (isPressed) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                darkShadowColor,
                                lightShadowColor
                            )
                        ),
                        shape = RoundedCornerShape(cornerRadius)
                    )
                } else Modifier
            ),
        content = content
    )
}

// ==================== GLOWING BUTTON ====================

/**
 * Button with neon glow effect
 */
@Composable
fun GlowingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF4CD964),
    backgroundColor: Color = Color(0xFF4CD964),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buttonGlow")
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowRadius"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                // Outer glow
                if (enabled) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = glowAlpha * 0.3f),
                        cornerRadius = CornerRadius(28.dp.toPx()),
                        size = Size(size.width + glowRadius * 2, size.height + glowRadius * 2),
                        topLeft = Offset(-glowRadius, -glowRadius)
                    )
                }
            }
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (enabled) {
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor,
                            backgroundColor.copy(alpha = 0.8f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Gray,
                            Color.Gray.copy(alpha = 0.8f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// ==================== GRADIENT TEXT ====================

/**
 * Text with gradient fill - 2026 bold typography trend
 */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF667eea),
            Color(0xFF764ba2)
        )
    ),
    fontSize: androidx.compose.ui.unit.TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            brush = gradient
        )
    )
}

// ==================== ANIMATED STREAK FLAME ====================

/**
 * Animated flame icon that grows with streak count
 */
@Composable
fun AnimatedStreakFlame(
    streakCount: Int,
    modifier: Modifier = Modifier,
    baseSize: Dp = 32.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")

    // Flame flicker animation
    val flickerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerScale"
    )

    val flickerRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flickerRotation"
    )

    // Size grows with streak (max 2x at 30+ days)
    val sizeMultiplier = 1f + (streakCount.coerceAtMost(30) / 30f) * 0.5f
    val actualSize = baseSize * sizeMultiplier

    // Color intensity based on streak
    val flameColors = when {
        streakCount >= 30 -> listOf(Color(0xFFFF4500), Color(0xFFFFD700), Color(0xFFFF6347)) // Hot!
        streakCount >= 14 -> listOf(Color(0xFFFF6B35), Color(0xFFFFAB00), Color(0xFFFF8C00))
        streakCount >= 7 -> listOf(Color(0xFFFF8C00), Color(0xFFFFD54F), Color(0xFFFFA726))
        else -> listOf(Color(0xFFFFA500), Color(0xFFFFD700), Color(0xFFFFE082))
    }

    Box(
        modifier = modifier.size(actualSize),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(actualSize * 1.5f)
                .graphicsLayer {
                    scaleX = flickerScale
                    scaleY = flickerScale
                    alpha = 0.3f
                }
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            flameColors[0].copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Flame icon with animation
        Icon(
            imageVector = DailyWellIcons.Analytics.Streak,
            contentDescription = "Streak fire",
            modifier = Modifier
                .size((actualSize.value * 0.7f).dp)
                .graphicsLayer {
                    scaleX = flickerScale
                    scaleY = flickerScale * 1.1f
                    rotationZ = flickerRotation
                },
            tint = flameColors[0]
        )

        // Streak count badge
        if (streakCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(flameColors.take(2))
                    )
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (streakCount > 99) "99+" else streakCount.toString(),
                    color = Color.White,
                    fontSize = if (streakCount > 99) 6.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==================== SHIMMER LOADING ====================

/**
 * Shimmer loading effect for skeleton screens
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(PremiumMotionTokens.shimmerDurationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
        label = "shimmerOffset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE0E0E0),
                        Color(0xFFF5F5F5),
                        Color(0xFFE0E0E0)
                    ),
                    start = Offset(shimmerOffset * 1000f - 500f, 0f),
                    end = Offset(shimmerOffset * 1000f + 500f, 0f)
                )
            )
    )
}

// ==================== FLOATING LABEL INPUT ====================

/**
 * Achievement badge with glow
 */
@Composable
fun AchievementBadge(
    icon: ImageVector,
    title: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val dailyWellColors = LocalDailyWellColors.current

    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badgeScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "badgeGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeGlowAlpha"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .then(
                    if (isUnlocked) {
                        Modifier.drawBehind {
                            drawCircle(
                                color = Color(0xFFFFD700).copy(alpha = glowAlpha),
                                radius = size.minDimension / 2 + 8.dp.toPx()
                            )
                        }
                    } else Modifier
                )
                .clip(CircleShape)
                .background(
                    if (isUnlocked) {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFFA500)
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE0E0E0),
                                Color(0xFFBDBDBD)
                            )
                        )
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (isUnlocked) Color(0xFFFFD700) else Color(0xFFBDBDBD),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp).graphicsLayer {
                    alpha = if (isUnlocked) 1f else 0.4f
                },
                tint = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isUnlocked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            },
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
