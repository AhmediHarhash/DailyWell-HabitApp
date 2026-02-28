package com.dailywell.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.core.theme.Secondary

/**
 * Shareable Card Types for viral marketing
 */
enum class ShareableCardType {
    STREAK,           // Share your streak achievement
    WEEKLY_SCORE,     // Share your weekly completion percentage
    COACH_QUOTE,      // Share an AI coach quote
    MILESTONE         // Share milestone achievements
}

/**
 * Data class for shareable content
 */
data class ShareableContent(
    val type: ShareableCardType,
    val streak: Int = 0,
    val weeklyScore: Int = 0,
    val completedHabits: Int = 0,
    val totalHabits: Int = 0,
    val coachQuote: String? = null,
    val milestoneTitle: String? = null
)

/**
 * Dialog for sharing streak achievements
 */
@Composable
fun ShareStreakDialog(
    streak: Int,
    completedToday: Int,
    onDismiss: () -> Unit,
    onShare: (String, String) -> Unit  // (text, imageDescription)
) {
    val shareText = generateStreakShareText(streak, completedToday)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Share Your Achievement!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // The shareable card preview
                ShareableStreakCard(
                    streak = streak,
                    completedToday = completedToday
                )

                // Share text preview
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = shareText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Share buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onShare(shareText, "streak_$streak")
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary
                        )
                    ) {
                        Text("Share")
                    }
                }
            }
        }
    }
}

/**
 * The visual streak card designed for sharing on social media
 */
@Composable
fun ShareableStreakCard(
    streak: Int,
    completedToday: Int,
    modifier: Modifier = Modifier
) {
    val gradientColors = getStreakGradientColors(streak)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(colors = gradientColors)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: DailyWell branding
            Text(
                text = "DailyWell",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold
            )

            // Center: Streak number with flame
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        streak >= 100 -> DailyWellIcons.Gamification.Crown
                        streak >= 30 -> DailyWellIcons.Status.Premium
                        streak >= 14 -> DailyWellIcons.Status.Star
                        streak >= 7 -> DailyWellIcons.Analytics.Streak
                        else -> DailyWellIcons.Misc.Sparkle
                    },
                    contentDescription = "Streak",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$streak",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "DAY STREAK",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 2.sp
                    )
                }
            }

            // Bottom: Motivational text + stats
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = getStreakTagline(streak),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                if (completedToday > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedToday habits completed today",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Decorative elements
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = (-30).dp, y = (-30).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )

        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 20.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        )
    }
}

/**
 * Weekly score shareable card
 */
@Composable
fun ShareableWeeklyScoreCard(
    score: Int,
    completedHabits: Int,
    totalHabits: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DailyWell Weekly Report",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.SemiBold
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score%",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "COMPLETION RATE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 2.sp
                )
            }

            Text(
                text = "$completedHabits habits completed this week",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Coach quote shareable card
 */
@Composable
fun ShareableCoachQuoteCard(
    quote: String,
    coachName: String = "AI Coach",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF11998e),
                        Color(0xFF38ef7d)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "DailyWell",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Text(
                text = "\"$quote\"",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "- $coachName",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Generate share text for streak achievements
 */
fun generateStreakShareText(streak: Int, completedToday: Int): String {
    val hashtags = "#DailyWell #HabitTracking #SelfImprovement #Wellness"

    return when {
        streak >= 100 -> """
            |100+ DAY STREAK!
            |I've been crushing my habits for over 100 days straight with DailyWell!
            |
            |Today: $completedToday habits completed
            |
            |Building better habits one day at a time.
            |$hashtags
        """.trimMargin()

        streak >= 30 -> """
            |30 DAY STREAK!
            |One month of consistent habits!
            |
            |Current streak: $streak days
            |Today: $completedToday habits done
            |
            |My AI wellness coach is keeping me accountable!
            |$hashtags
        """.trimMargin()

        streak >= 7 -> """
            |$streak DAY STREAK!
            |One week down, lifetime to go!
            |
            |$completedToday habits completed today
            |
            |Track your wellness journey with DailyWell
            |$hashtags
        """.trimMargin()

        else -> """
            |$streak Day Streak!
            |Building momentum with my habits!
            |
            |$completedToday habits completed today
            |
            |Start your wellness journey with DailyWell
            |$hashtags
        """.trimMargin()
    }
}

/**
 * Get gradient colors based on streak length
 */
private fun getStreakGradientColors(streak: Int): List<Color> = when {
    streak >= 100 -> listOf(Color(0xFFf5af19), Color(0xFFf12711))  // Gold/Red - Legendary
    streak >= 30 -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))   // Purple - Epic
    streak >= 14 -> listOf(Color(0xFF11998e), Color(0xFF38ef7d))  // Teal/Green - Great
    streak >= 7 -> listOf(Color(0xFF2193b0), Color(0xFF6dd5ed))    // Blue - Good
    else -> listOf(Primary, Secondary)                              // Default
}

/**
 * Get emoji for streak
 */
private fun getStreakEmoji(streak: Int): String = when {
    streak >= 100 -> "👑"
    streak >= 30 -> "💎"
    streak >= 14 -> "🌟"
    streak >= 7 -> "🔥"
    else -> "✨"
}

/**
 * Get tagline based on streak
 */
private fun getStreakTagline(streak: Int): String = when {
    streak >= 100 -> "LEGENDARY STATUS ACHIEVED!"
    streak >= 30 -> "Habits are now part of who I am"
    streak >= 14 -> "Two weeks of consistency!"
    streak >= 7 -> "One week milestone reached!"
    streak >= 3 -> "Building momentum!"
    else -> "Every day counts!"
}
