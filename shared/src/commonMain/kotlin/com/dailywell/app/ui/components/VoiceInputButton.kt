package com.dailywell.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.PrimaryLight
import com.dailywell.app.speech.SpeechRecognitionState
import com.dailywell.app.speech.VoiceInputMode

/**
 * Voice Input Button State
 */
enum class VoiceButtonState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

/**
 * Voice Input Button with recording animation
 */
@Composable
fun VoiceInputButton(
    state: SpeechRecognitionState,
    inputMode: VoiceInputMode,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onCancelListening: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val buttonState = when (state) {
        is SpeechRecognitionState.Idle -> VoiceButtonState.IDLE
        is SpeechRecognitionState.Listening, is SpeechRecognitionState.PartialResult -> VoiceButtonState.LISTENING
        is SpeechRecognitionState.Processing -> VoiceButtonState.PROCESSING
        is SpeechRecognitionState.Result -> VoiceButtonState.IDLE
        is SpeechRecognitionState.Error -> VoiceButtonState.ERROR
    }

    val isListening = buttonState == VoiceButtonState.LISTENING

    // Animation for pulse effect when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Background color animation
    val backgroundColor by animateColorAsState(
        targetValue = when (buttonState) {
            VoiceButtonState.IDLE -> if (enabled) Primary else Primary.copy(alpha = 0.4f)
            VoiceButtonState.LISTENING -> Color(0xFFE53935) // Red when recording
            VoiceButtonState.PROCESSING -> Primary.copy(alpha = 0.7f)
            VoiceButtonState.ERROR -> Color(0xFFE53935).copy(alpha = 0.7f)
        },
        label = "bgColor"
    )

    // Scale animation
    val scale by animateFloatAsState(
        targetValue = when (buttonState) {
            VoiceButtonState.LISTENING -> pulseScale
            else -> 1f
        },
        label = "scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Ripple effect when listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(pulseScale * 1.2f)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935).copy(alpha = 0.2f))
            )
        }

        // Main button
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(backgroundColor)
                .then(
                    if (inputMode == VoiceInputMode.PUSH_TO_TALK) {
                        Modifier.pointerInput(enabled) {
                            if (enabled) {
                                detectTapGestures(
                                    onPress = {
                                        onStartListening()
                                        tryAwaitRelease()
                                        onStopListening()
                                    }
                                )
                            }
                        }
                    } else {
                        Modifier.clickable(enabled = enabled) {
                            when (buttonState) {
                                VoiceButtonState.IDLE, VoiceButtonState.ERROR -> onStartListening()
                                VoiceButtonState.LISTENING -> onStopListening()
                                VoiceButtonState.PROCESSING -> onCancelListening()
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // Microphone icon (using emoji for KMP compatibility)
            Text(
                text = when (buttonState) {
                    VoiceButtonState.IDLE -> "🎤"
                    VoiceButtonState.LISTENING -> "⏹️"
                    VoiceButtonState.PROCESSING -> "⏳"
                    VoiceButtonState.ERROR -> "🎤"
                },
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Voice Input Status Indicator
 * Shows partial transcription or status messages
 */
@Composable
fun VoiceInputStatus(
    state: SpeechRecognitionState,
    modifier: Modifier = Modifier
) {
    val statusText = when (state) {
        is SpeechRecognitionState.Idle -> null
        is SpeechRecognitionState.Listening -> "Listening..."
        is SpeechRecognitionState.PartialResult -> state.text
        is SpeechRecognitionState.Processing -> "Processing..."
        is SpeechRecognitionState.Result -> null
        is SpeechRecognitionState.Error -> state.message
    }

    val isError = state is SpeechRecognitionState.Error

    if (statusText != null) {
        Surface(
            modifier = modifier,
            color = if (isError)
                Color(0xFFE53935).copy(alpha = 0.1f)
            else
                PrimaryLight.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = statusText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isError)
                    Color(0xFFE53935)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2
            )
        }
    }
}

/**
 * Waveform Visualizer for audio input
 */
@Composable
fun WaveformVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val delay = index * 100
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = if (isActive) 24f else 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isActive) Primary else Primary.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
