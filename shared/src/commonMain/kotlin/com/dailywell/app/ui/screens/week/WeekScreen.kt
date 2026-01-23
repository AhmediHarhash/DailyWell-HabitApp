package com.dailywell.app.ui.screens.week

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.core.theme.Warning
import com.dailywell.app.data.model.CompletionStatus
import com.dailywell.app.data.model.DayStatus
import com.dailywell.app.data.model.WeekData
import com.dailywell.app.ui.components.WeekCalendar
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    viewModel: WeekViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Week View",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Week navigation
                item {
                    WeekNavigationHeader(
                        weekData = uiState.currentWeekData,
                        isCurrentWeek = uiState.selectedWeekOffset == 0,
                        onPreviousWeek = { viewModel.goToPreviousWeek() },
                        onNextWeek = { viewModel.goToNextWeek() },
                        onCurrentWeek = { viewModel.goToCurrentWeek() }
                    )
                }

                // Week calendar
                item {
                    uiState.currentWeekData?.let { weekData ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                WeekCalendar(weekData = weekData)
                                Spacer(modifier = Modifier.height(16.dp))

                                // Summary message
                                Text(
                                    text = uiState.weeklySummaryMessage,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Statistics card
                item {
                    uiState.currentWeekData?.let { weekData ->
                        WeekStatsCard(weekData = weekData)
                    }
                }

                // Daily breakdown
                item {
                    Text(
                        text = "DAILY BREAKDOWN",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                uiState.currentWeekData?.let { weekData ->
                    items(weekData.days.filter { !it.isFuture }) { dayStatus ->
                        DayBreakdownCard(dayStatus = dayStatus)
                    }
                }

                // Comparison with previous week
                item {
                    ComparisonCard(
                        currentWeek = uiState.currentWeekData,
                        previousWeek = uiState.previousWeekData
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun WeekNavigationHeader(
    weekData: WeekData?,
    isCurrentWeek: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousWeek) {
            Text(text = "◀", fontSize = 20.sp)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatWeekRange(weekData),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!isCurrentWeek) {
                TextButton(onClick = onCurrentWeek) {
                    Text(
                        text = "Go to current week",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        IconButton(
            onClick = onNextWeek,
            enabled = !isCurrentWeek
        ) {
            Text(
                text = "▶",
                fontSize = 20.sp,
                color = if (isCurrentWeek) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun WeekStatsCard(weekData: WeekData) {
    val completedDays = weekData.days.count { it.status == CompletionStatus.COMPLETE }
    val partialDays = weekData.days.count { it.status == CompletionStatus.PARTIAL }
    val missedDays = weekData.days.count {
        it.status == CompletionStatus.NONE && !it.isFuture
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "WEEK STATS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    emoji = "✓",
                    value = completedDays.toString(),
                    label = "Complete",
                    color = Success
                )
                StatItem(
                    emoji = "◐",
                    value = partialDays.toString(),
                    label = "Partial",
                    color = Warning
                )
                StatItem(
                    emoji = "✗",
                    value = missedDays.toString(),
                    label = "Missed",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completion rate bar
            val rate = (weekData.completionRate * 100).toInt()
            LinearProgressIndicator(
                progress = { weekData.completionRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = when {
                    rate >= 80 -> Success
                    rate >= 50 -> Warning
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$rate% completion rate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatItem(
    emoji: String,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 16.sp, color = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayBreakdownCard(dayStatus: DayStatus) {
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (dayStatus.status) {
                CompletionStatus.COMPLETE -> Success.copy(alpha = 0.1f)
                CompletionStatus.PARTIAL -> Warning.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dayNames.getOrNull(dayStatus.dayOfWeek) ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (dayStatus.isToday) FontWeight.Bold else FontWeight.Normal
                )
                if (dayStatus.isToday) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${dayStatus.completedCount}/${dayStatus.totalCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = when (dayStatus.status) {
                        CompletionStatus.COMPLETE -> Success
                        CompletionStatus.PARTIAL -> Warning
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (dayStatus.status) {
                        CompletionStatus.COMPLETE -> "✓"
                        CompletionStatus.PARTIAL -> "◐"
                        CompletionStatus.NONE -> "✗"
                        else -> "–"
                    },
                    fontSize = 20.sp,
                    color = when (dayStatus.status) {
                        CompletionStatus.COMPLETE -> Success
                        CompletionStatus.PARTIAL -> Warning
                        CompletionStatus.NONE -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    currentWeek: WeekData?,
    previousWeek: WeekData?
) {
    if (currentWeek == null || previousWeek == null) return

    val currentRate = (currentWeek.completionRate * 100).toInt()
    val previousRate = (previousWeek.completionRate * 100).toInt()
    val difference = currentRate - previousRate

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VS LAST WEEK",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        difference > 0 -> "📈"
                        difference < 0 -> "📉"
                        else -> "➡️"
                    },
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        difference > 0 -> "+$difference%"
                        difference < 0 -> "$difference%"
                        else -> "Same"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        difference > 0 -> Success
                        difference < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Text(
                text = when {
                    difference > 10 -> "Great improvement!"
                    difference > 0 -> "Keep it up!"
                    difference == 0 -> "Consistent performance"
                    difference > -10 -> "Room for improvement"
                    else -> "Let's bounce back!"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatWeekRange(weekData: WeekData?): String {
    if (weekData == null) return ""
    return try {
        val start = LocalDate.parse(weekData.weekStartDate)
        val end = LocalDate.parse(weekData.weekEndDate)
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "${months[start.monthNumber - 1]} ${start.dayOfMonth} - ${months[end.monthNumber - 1]} ${end.dayOfMonth}"
    } catch (e: Exception) {
        "${weekData.weekStartDate} - ${weekData.weekEndDate}"
    }
}
