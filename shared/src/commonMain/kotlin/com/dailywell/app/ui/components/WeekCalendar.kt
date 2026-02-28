package com.dailywell.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.core.theme.Warning
import com.dailywell.app.data.model.CompletionStatus
import com.dailywell.app.data.model.DayStatus
import com.dailywell.app.data.model.WeekData

@Composable
fun WeekCalendar(
    weekData: WeekData,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // C1: Staggered entrance for day indicators
            weekData.days.forEachIndexed { index, dayStatus ->
                Box(modifier = Modifier.weight(1f)) {
                    StaggeredItem(
                        index = index,
                        delayPerItem = 40L,
                        baseDelay = 50L
                    ) {
                        DayIndicator(dayStatus = dayStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayIndicator(
    dayStatus: DayStatus,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (dayStatus.status) {
            CompletionStatus.COMPLETE -> Success
            CompletionStatus.PARTIAL -> Warning
            CompletionStatus.NONE -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            CompletionStatus.FUTURE -> Color.Transparent
            CompletionStatus.NO_DATA -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val textColor = when {
        dayStatus.isToday -> MaterialTheme.colorScheme.primary
        dayStatus.isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        dayStatus.status == CompletionStatus.COMPLETE -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    // C2: Today indicator pulse
    val todayPulse = if (dayStatus.isToday) rememberPulseScale(1f, 1.06f, 1200) else 1f

    // C3: Completed day glow color
    val glowColor = if (dayStatus.status == CompletionStatus.COMPLETE) Success.copy(alpha = 0.3f) else Color.Transparent

    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayStatus.dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (dayStatus.isToday) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (dayStatus.isToday) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                // C3: Subtle glow behind completed circles
                .drawBehind {
                    if (glowColor != Color.Transparent) {
                        drawCircle(
                            color = glowColor,
                            radius = size.minDimension / 2 + 4.dp.toPx()
                        )
                    }
                }
                // C2: Apply pulse to today's indicator
                .graphicsLayer {
                    scaleX = todayPulse
                    scaleY = todayPulse
                }
                .clip(CircleShape)
                .background(backgroundColor)
                .then(
                    if (dayStatus.isToday && dayStatus.status != CompletionStatus.COMPLETE) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            when (dayStatus.status) {
                CompletionStatus.COMPLETE -> {
                    Icon(
                        imageVector = DailyWellIcons.Actions.Check,
                        contentDescription = "Complete",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
                CompletionStatus.PARTIAL -> {
                    Icon(
                        imageVector = DailyWellIcons.Actions.Remove,
                        contentDescription = "Partial",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
                CompletionStatus.NONE -> {
                    Icon(
                        imageVector = DailyWellIcons.Nav.Close,
                        contentDescription = "Missed",
                        modifier = Modifier.size(12.dp),
                        tint = Color.White
                    )
                }
                CompletionStatus.FUTURE -> {
                    Text(
                        text = "\u2022",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        fontSize = 14.sp
                    )
                }
                CompletionStatus.NO_DATA -> {
                    Text(
                        text = "\u2013",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WeekSummaryText(
    weekData: WeekData,
    modifier: Modifier = Modifier
) {
    val completedDays = weekData.days.count {
        it.status == CompletionStatus.COMPLETE || it.status == CompletionStatus.PARTIAL
    }
    val totalDays = weekData.days.count { !it.isFuture }

    val message = when {
        completedDays == 7 -> "Perfect week!"
        completedDays >= 5 -> "$completedDays/7 = GREAT!"
        completedDays >= 3 -> "$completedDays/7 - Keep going!"
        else -> "$completedDays/7 - Room to grow"
    }

    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = when {
            completedDays >= 5 -> Success
            completedDays >= 3 -> Warning
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
        textAlign = TextAlign.Center
    )
}
