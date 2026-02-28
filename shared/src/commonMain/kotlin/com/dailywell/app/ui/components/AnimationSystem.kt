package com.dailywell.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.dailywell.app.core.theme.PremiumMotionTokens

/**
 * 2026 Modern Animation System
 * Psychology-driven micro-interactions for premium UX
 *
 * Research: Micro-interactions increase engagement by 43%
 * and make apps feel 2.5x more responsive
 */

// ==================== SPRING CONFIGURATIONS ====================

/**
 * Pre-configured spring animations for consistent feel across the app
 * Based on Material Design 3 motion guidelines
 */
object DailyWellSprings {
    /**
     * Snappy spring for quick, responsive interactions
     * Use for: toggles, checkboxes, small buttons
     */
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Gentle spring for smooth, calming transitions
     * Use for: cards appearing, modals, health-related animations
     */
    val Gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Bouncy spring for celebration moments
     * Use for: completion celebrations, streak achievements
     */
    val Bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Ultra-responsive spring for press feedback
     * Use for: button press, tap feedback
     */
    val Responsive = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    /**
     * Elastic spring for dramatic effects
     * Use for: error shakes, attention grabbing
     */
    val Elastic = spring<Float>(
        dampingRatio = 0.25f,
        stiffness = Spring.StiffnessMedium
    )
}

// ==================== STAGGERED ANIMATION ====================

/**
 * Wrapper for staggered entrance animations
 * Items enter sequentially with configurable delay
 *
 * @param index Position in the list (determines delay)
 * @param delayPerItem Milliseconds between each item's entrance
 * @param baseDelay Initial delay before first item
 * @param content The composable to animate
 */
@Composable
fun StaggeredItem(
    index: Int,
    delayPerItem: Long = PremiumMotionTokens.listItemStaggerMs,
    baseDelay: Long = 0L,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(baseDelay + (index * delayPerItem))
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = PremiumMotionTokens.listEnterDurationMs,
            easing = EaseOutCubic
        ),
        label = "staggerAlpha"
    )

    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 24f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "staggerTranslation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.92f,
        animationSpec = DailyWellSprings.Gentle,
        label = "staggerScale"
    )

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
            this.scaleX = scale
            this.scaleY = scale
        }
    ) {
        content()
    }
}

// ==================== PRESS SCALE MODIFIER ====================

/**
 * Modifier for press-to-scale feedback
 * Makes elements feel touchable and responsive
 *
 * @param pressedScale Scale when pressed (0.95f = 5% smaller)
 * @param releasedScale Scale when released (1f = normal)
 * @param enabled Whether the effect is active
 */
fun Modifier.pressScale(
    pressedScale: Float = 0.95f,
    releasedScale: Float = 1f,
    enabled: Boolean = true
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else releasedScale,
        animationSpec = DailyWellSprings.Responsive,
        label = "pressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(enabled) {
            if (enabled) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
        }
}

/**
 * Alternative press scale that works with clickable modifier
 * Use when you need both press feedback and click handling
 */
@Composable
fun Modifier.pressScaleWithState(
    isPressed: Boolean,
    pressedScale: Float = 0.95f
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = DailyWellSprings.Responsive,
        label = "pressScaleState"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ==================== BREATHING ANIMATION ====================

/**
 * Subtle breathing animation for living, organic feel
 * Psychology: Creates sense of life and calm
 *
 * @param minScale Minimum scale (default 1f = no shrink)
 * @param maxScale Maximum scale (default 1.03f = 3% grow)
 * @param durationMs Duration of one breath cycle
 */
@Composable
fun rememberBreathingScale(
    minScale: Float = 1f,
    maxScale: Float = 1.03f,
    durationMs: Int = PremiumMotionTokens.breathingDurationMs
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )
    return scale
}

/**
 * Breathing modifier for direct application
 */
fun Modifier.breathingAnimation(
    minScale: Float = 1f,
    maxScale: Float = 1.03f,
    durationMs: Int = PremiumMotionTokens.breathingDurationMs
): Modifier = composed {
    val scale = rememberBreathingScale(minScale, maxScale, durationMs)
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ==================== PULSE ANIMATION ====================

/**
 * Attention-grabbing pulse animation
 * Use for: CTAs, important notifications, incomplete tasks
 */
@Composable
fun rememberPulseScale(
    minScale: Float = 1f,
    maxScale: Float = 1.08f,
    durationMs: Int = PremiumMotionTokens.pulseDurationMs
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    return scale
}

// ==================== SHAKE ANIMATION ====================

/**
 * Error shake animation for validation feedback
 * Psychology: Clear physical feedback for errors
 */
@Composable
fun rememberShakeOffset(
    trigger: Boolean,
    intensity: Float = 10f,
    durationMs: Int = PremiumMotionTokens.shakeDurationMs
): Float {
    var offset by remember { mutableStateOf(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            val shakePattern = listOf(1f, -1f, 0.8f, -0.8f, 0.5f, -0.5f, 0.2f, -0.2f, 0f)
            shakePattern.forEach { factor ->
                offset = intensity * factor
                kotlinx.coroutines.delay((durationMs / shakePattern.size).toLong())
            }
            offset = 0f
        }
    }

    return offset
}

fun Modifier.shakeOnError(
    hasError: Boolean,
    intensity: Float = 10f
): Modifier = composed {
    val shakeOffset = rememberShakeOffset(hasError, intensity)
    this.graphicsLayer {
        translationX = shakeOffset
    }
}

// ==================== CELEBRATION ANIMATION ====================

/**
 * Celebration scale burst for achievements
 * Psychology: Dopamine hit for positive reinforcement
 */
@Composable
fun rememberCelebrationScale(
    trigger: Boolean,
    peakScale: Float = 1.15f
): Float {
    var scale by remember { mutableStateOf(1f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            // Quick burst up
            val upAnimation = animate(
                initialValue = 1f,
                targetValue = peakScale,
                animationSpec = tween(
                    PremiumMotionTokens.fastFadeMs,
                    easing = EaseOutBack
                )
            )
            scale = upAnimation

            // Settle back with bounce
            val downAnimation = animate(
                initialValue = peakScale,
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            scale = downAnimation
        }
    }

    return scale
}

// ==================== CHECKMARK ANIMATION ====================

/**
 * Animated checkmark rotation for completion
 * Rotates from -45° to 0° with bounce
 */
@Composable
fun rememberCheckmarkRotation(
    isChecked: Boolean
): Float {
    val rotation by animateFloatAsState(
        targetValue = if (isChecked) 0f else -45f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkRotation"
    )
    return rotation
}

// ==================== FADE IN/OUT HELPERS ====================

/**
 * Animated fade in on appearance
 */
@Composable
fun Modifier.fadeInOnAppear(
    durationMs: Int = PremiumMotionTokens.fadeInDurationMs,
    delay: Int = 0
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (delay > 0) kotlinx.coroutines.delay(delay.toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMs, easing = EaseOutCubic),
        label = "fadeIn"
    )

    this.graphicsLayer { this.alpha = alpha }
}

// ==================== GRADIENT ANIMATION ====================

/**
 * Animated gradient offset for flowing backgrounds
 */
@Composable
fun rememberAnimatedGradientOffset(
    durationMs: Int = PremiumMotionTokens.gradientShiftDurationMs
): Float {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )
    return offset
}

// ==================== HELPER SUSPEND FUNCTION ====================

/**
 * Suspend function to animate between values
 */
private suspend fun animate(
    initialValue: Float,
    targetValue: Float,
    animationSpec: AnimationSpec<Float>
): Float {
    var result = initialValue
    val animatable = Animatable(initialValue)
    animatable.animateTo(targetValue, animationSpec)
    result = animatable.value
    return result
}
