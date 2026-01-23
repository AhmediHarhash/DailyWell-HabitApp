package com.dailywell.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
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
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Text(
                            text = currentMood?.let { MoodPrompts.getFollowUp(it) } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Encouragement based on mood
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (currentMood) {
                                    MoodLevel.GREAT, MoodLevel.GOOD -> Success.copy(alpha = 0.2f)
                                    MoodLevel.OKAY -> Primary.copy(alpha = 0.2f)
                                    MoodLevel.LOW, MoodLevel.STRUGGLING -> Color(0xFFFFE0B2)
                                    null -> Color.Transparent
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodOption(
    mood: MoodLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
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
            Text(
                text = mood.emoji,
                fontSize = 24.sp
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
 * Compact mood display for after check-in
 */
@Composable
fun MoodIndicator(
    mood: MoodLevel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = mood.emoji, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Feeling ${mood.label.lowercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Trial banner showing days remaining with urgency
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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = urgencyColor.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUpgradeClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val emoji = when {
                daysRemaining <= 2 -> "⚠️"
                daysRemaining <= 5 -> "⏰"
                else -> "🎁"
            }

            Text(text = emoji, fontSize = 24.sp)
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

            Text(
                text = "→",
                fontSize = 20.sp,
                color = urgencyColor
            )
        }
    }
}

/**
 * Social proof notification
 */
@Composable
fun SocialProofBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
