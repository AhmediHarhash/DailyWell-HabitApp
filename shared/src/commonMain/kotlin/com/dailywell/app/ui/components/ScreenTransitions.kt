package com.dailywell.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.LocalDailyWellColors

/**
 * Screen transition and wrapper components for consistent glass UI
 */

/**
 * Standard glass screen wrapper
 * Provides consistent background with time-aware gradient and glass overlay
 */
@Composable
fun GlassScreenWrapper(
    modifier: Modifier = Modifier,
    showGradientHeader: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val dailyWellColors = LocalDailyWellColors.current
    val ambientTransition = rememberInfiniteTransition(label = "glassWrapperAmbient")
    val orbShiftX by ambientTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glassWrapperOrbShiftX"
    )
    val orbShiftY by ambientTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glassWrapperOrbShiftY"
    )
    val sheenShift by ambientTransition.animateFloat(
        initialValue = -240f,
        targetValue = 240f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glassWrapperSheenShift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.timeGradientStart.copy(alpha = 0.24f),
                        dailyWellColors.timeGradientEnd.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // Subtle time-aware gradient at top
        if (showGradientHeader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dailyWellColors.timeGradientStart.copy(alpha = 0.5f),
                                dailyWellColors.timeGradientEnd.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        // Ambient mesh blobs to keep non-dashboard pages visually premium.
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-90).dp + orbShiftX.dp, y = (-130).dp + orbShiftY.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dailyWellColors.meshColor1.copy(alpha = 0.22f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 38.dp - (orbShiftX * 0.65f).dp, y = 12.dp + (orbShiftY * 0.4f).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dailyWellColors.meshColor2.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.28f)
        ) {
            val y = size.height * 0.74f
            drawLine(
                color = dailyWellColors.glassHighlight.copy(alpha = 0.20f),
                start = Offset(x = -220f + sheenShift, y = y),
                end = Offset(x = size.width * 0.46f + sheenShift, y = y),
                strokeWidth = 3f
            )
        }

        content()
    }
}

/**
 * Animated screen entrance wrapper
 * Fades in and slides up content on appear
 */
@Composable
fun AnimatedScreenEntrance(
    modifier: Modifier = Modifier,
    delayMs: Int = 0,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs.toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = EaseOutCubic),
        label = "screenAlpha"
    )

    val translationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "screenTranslation"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}

/**
 * Glass panel for tab hub screens
 * Used as a card in the hub grids (Insights, Track, Coach tabs)
 */
@Composable
fun GlassHubCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 20.dp,
        enablePressScale = true,
        onClick = onClick,
        content = content
    )
}
