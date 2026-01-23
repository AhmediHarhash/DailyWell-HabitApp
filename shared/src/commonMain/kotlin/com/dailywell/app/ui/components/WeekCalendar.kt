package com.dailywell.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
            weekData.days.forEach { dayStatus ->
                DayIndicator(
                    dayStatus = dayStatus,
                    modifier = Modifier.weight(1f)
                )
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
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                CompletionStatus.PARTIAL -> {
                    Text(
                        text = "◐",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                CompletionStatus.NONE -> {
                    Text(
                        text = "✗",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
                CompletionStatus.FUTURE -> {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        fontSize = 14.sp
                    )
                }
                CompletionStatus.NO_DATA -> {
                    Text(
                        text = "–",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
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
