package com.dailywell.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.MoodLevel
import com.dailywell.app.data.model.MoodPrompts

/**
 * Mood Check Card - FBI Psychology "Labeling Emotions"
 *
 * When users identify and label their emotions:
 * 1. They feel UNDERSTOOD by the app
 * 2. Creates emotional connection
 * 3. Provides valuable data for mood-habit correlations
 * 4. Triggers different encouragement based on state
 */
@Composable
fun MoodCheckCard(
    hasCheckedMood: Boolean,
    currentMood: MoodLevel?,
    onMoodSelected: (MoodLevel) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFollowUp by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !hasCheckedMood,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // B1: Glass card wrapper with entrance animation
        GlassCard(
            modifier = modifier
                .fillMaxWidth()
                .fadeInOnAppear(durationMs = 400, delay = 50),
            elevation = ElevationLevel.Medium,
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Question
                Text(
                    text = MoodPrompts.getRandomQuestion(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mood options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MoodLevel.entries.forEach { mood ->
                        MoodOption(
                            mood = mood,
                            isSelected = currentMood == mood,
                            onClick = { onMoodSelected(mood) }
                        )
                    }
                }

                // Follow-up message after selection
                AnimatedVisibility(visible = currentMood != null) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = currentMood?.let { MoodPrompts.getFollowUp(it) } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // B3: Encouragement with glass-tinted background using mood color
                        val moodTintColor = when (currentMood) {
                            MoodLevel.GREAT, MoodLevel.GOOD -> Success
                            MoodLevel.OKAY -> Primary
                            MoodLevel.LOW, MoodLevel.STRUGGLING -> Color(0xFFFF9800)
                            null -> Color.Transparent
                        }
                        val dailyWellColors = LocalDailyWellColors.current
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            moodTintColor.copy(alpha = 0.15f),
                                            dailyWellColors.glassBackground
                                        )
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = moodTintColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Text(
                                text = currentMood?.let { MoodPrompts.getEncouragement(it) } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = onDismiss) {
                            Text("Continue to habits")
                        }
                    }
                }

                // Dismiss option if no mood selected yet
                if (currentMood == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Skip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// B2: More dramatic bounce animation on mood selection
@Composable
private fun MoodOption(
    mood: MoodLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // B2: Dramatic bounce: 1f -> 1.3f -> 1f spring when selected
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "moodBounce"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        when (mood) {
                            MoodLevel.GREAT -> Success.copy(alpha = 0.3f)
                            MoodLevel.GOOD -> Success.copy(alpha = 0.2f)
                            MoodLevel.OKAY -> Primary.copy(alpha = 0.2f)
                            MoodLevel.LOW -> Color(0xFFFFE0B2)
                            MoodLevel.STRUGGLING -> Color(0xFFFFCDD2)
                        }
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DailyWellIcons.getMoodIcon(mood.name),
                contentDescription = mood.label,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = mood.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * B4: Compact mood display with GlassChip, entrance animation, and subtle pulse
 */
@Composable
fun MoodIndicator(
    mood: MoodLevel,
    modifier: Modifier = Modifier
) {
    // B4: Entrance animation
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    AnimatedVisibility(
        visible = appeared,
        enter = scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        // B4: Subtle pulse on emoji
        val emojiPulse = rememberPulseScale(1f, 1.06f, 1200)

        GlassChip(
            modifier = modifier
        ) {
            Icon(
                imageVector = DailyWellIcons.getMoodIcon(mood.name),
                contentDescription = mood.label,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { scaleX = emojiPulse; scaleY = emojiPulse },
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Feeling ${mood.label.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * B5: Trial banner with urgency breathing animation and glass treatment
 */
@Composable
fun TrialBanner(
    daysRemaining: Int,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val urgencyColor = when {
        daysRemaining <= 2 -> Color(0xFFD32F2F) // Red - critical
        daysRemaining <= 5 -> Color(0xFFFF9800) // Orange - warning
        else -> Primary // Normal
    }

    val dailyWellColors = LocalDailyWellColors.current

    // B5: Glass card with colored border and entrance slide-in
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .fadeInOnAppear(durationMs = 400, delay = 50)
            .then(
                // B5: Breathing animation for critical urgency
                if (daysRemaining <= 2) Modifier.breathingAnimation(
                    minScale = 1f, maxScale = 1.01f, durationMs = 1500
                ) else Modifier
            ),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = urgencyColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onUpgradeClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bannerIcon = when {
                daysRemaining <= 2 -> DailyWellIcons.Status.Warning
                daysRemaining <= 5 -> DailyWellIcons.Misc.Timer
                else -> DailyWellIcons.Gamification.Gift
            }

            Icon(
                imageVector = bannerIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = urgencyColor
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        daysRemaining == 1 -> "Last day of free trial!"
                        daysRemaining <= 3 -> "Trial ends in $daysRemaining days"
                        else -> "Premium trial: $daysRemaining days left"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = urgencyColor
                )
                Text(
                    text = "Tap to keep all features unlocked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = DailyWellIcons.Nav.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = urgencyColor
            )
        }
    }
}

/**
 * B6: Social proof banner with GlassCard, community icon, PulsingDot, and fade-in
 */
@Composable
fun SocialProofBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    // B6: Glass card with entrance fade-in
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .fadeInOnAppear(durationMs = 500, delay = 100),
        elevation = ElevationLevel.Subtle,
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // B6: Community icon on the left
            Icon(
                imageVector = DailyWellIcons.Social.People,
                contentDescription = "Community",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // B6: PulsingDot to indicate "live" social proof
            PulsingDot(
                color = Color(0xFF10B981),
                size = 8.dp
            )
        }
    }
}
