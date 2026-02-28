package com.dailywell.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.LocalDailyWellColors

/**
 * Platform icon components using Material Icons
 * Replaces AI-generated PNGs and emoji fallbacks with vector icons
 */

private val HabitCompletedColor = Color(0xFF10B981)
private val HabitPendingColor = Color(0xFFF59E0B)
private val CoachGradientStart = Color(0xFF6366F1)
private val CoachGradientEnd = Color(0xFF8B5CF6)
private val BadgeGoldColor = Color(0xFFFFD700)
private val BadgeSilverColor = Color(0xFFC0C0C0)

/**
 * Habit icon using Material Icons with completion state
 */
@Composable
fun PlatformHabitIcon(
    habitId: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isCompleted: Boolean = false,
    streakDays: Int = 0
) {
    val icon = DailyWellIcons.getHabitIcon(habitId)

    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "habit_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect for completed habits
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .size(size * 1.2f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                HabitCompletedColor.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main icon container
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = if (isCompleted) 8.dp else 4.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isCompleted) {
                            listOf(HabitCompletedColor, HabitCompletedColor.copy(alpha = 0.8f))
                        } else {
                            listOf(Color.White, Color(0xFFF8FAFC))
                        }
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = if (isCompleted) {
                            listOf(HabitCompletedColor, Color(0xFF059669))
                        } else {
                            listOf(HabitPendingColor, Color(0xFFD97706))
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Habit: $habitId",
                modifier = Modifier.size(size * 0.55f),
                tint = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }

        // Streak badge
        if (streakDays > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(24.dp)
                    .shadow(4.dp, CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E53))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (streakDays > 99) "99+" else streakDays.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Coach avatar using Material Icons
 */
@Composable
fun PlatformCoachAvatar(
    coachId: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isActive: Boolean = false
) {
    val icon = DailyWellIcons.getCoachIcon(coachId)
    val glowAlpha = if (isActive) 0.4f else 0f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow for active coach
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(size * 1.3f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CoachGradientStart.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Gradient border
        Box(
            modifier = Modifier
                .size(size)
                .shadow(8.dp, CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(CoachGradientStart, CoachGradientEnd)
                    ),
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Coach: $coachId",
                modifier = Modifier.size(size * 0.5f),
                tint = CoachGradientStart
            )
        }

        // Active indicator
        if (isActive) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.25f)
                    .shadow(4.dp, CircleShape)
                    .background(Color(0xFF10B981), CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Achievement badge using Material Icons
 */
@Composable
fun PlatformAchievementBadge(
    badgeName: String,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isUnlocked: Boolean = true
) {
    val icon = DailyWellIcons.getBadgeIcon(badgeName)

    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badge_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = if (isUnlocked) 8.dp else 2.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = if (isUnlocked) {
                        Brush.linearGradient(
                            colors = listOf(BadgeGoldColor, Color(0xFFFFA500))
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(BadgeSilverColor, Color(0xFF808080))
                        )
                    }
                )
                .then(
                    if (isUnlocked) {
                        Modifier.border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.8f), BadgeGoldColor)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Badge: $badgeName",
                modifier = Modifier.size(size * 0.5f),
                tint = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.5f)
            )
        }

        // Lock overlay for locked badges
        if (!isUnlocked) {
            Box(
                modifier = Modifier
                    .size(size * 0.4f)
                    .background(Color(0xFF374151), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyWellIcons.Status.Lock,
                    contentDescription = "Locked",
                    modifier = Modifier.size(size * 0.2f),
                    tint = Color.White
                )
            }
        }
    }
}
