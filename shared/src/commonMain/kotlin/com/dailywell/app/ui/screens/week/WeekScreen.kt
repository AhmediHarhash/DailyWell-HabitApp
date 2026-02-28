package com.dailywell.app.ui.screens.week

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.*
import com.dailywell.app.data.model.CompletionStatus
import com.dailywell.app.data.model.DayStatus
import com.dailywell.app.data.model.WeekData
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTabHeader
import com.dailywell.app.ui.components.StaggeredItem
import com.dailywell.app.ui.components.ShimmerBox
import com.dailywell.app.ui.components.pressScale
import com.dailywell.app.ui.components.WeekCalendar
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

/**
 * WeekScreen - 2026 Modern Journey View
 *
 * Design: Clean, data-rich with warm glass cards,
 * animated progress bars, and staggered daily breakdowns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(
    viewModel: WeekViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dailyWellColors = LocalDailyWellColors.current

    GlassScreenWrapper {
        Column(modifier = Modifier.fillMaxSize()) {
            PremiumTabHeader(
                title = "Week",
                subtitle = "Consistency and progression",
                includeStatusBarPadding = true
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                dailyWellColors.timeGradientStart.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title header
                item {
                    StaggeredItem(index = 0, delayPerItem = 50L) {
                        PremiumSectionChip(
                            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 4.dp),
                            text = "Your Journey",
                            icon = DailyWellIcons.Analytics.Streak
                        )
                    }
                }

                if (uiState.isLoading) {
                    // Shimmer loading skeleton
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ShimmerBox(
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                shape = RoundedCornerShape(16.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                shape = RoundedCornerShape(20.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                } else {
                    // Week navigation
                    item {
                        StaggeredItem(index = 1, delayPerItem = 50L) {
                            WeekNavigationHeader(
                                weekData = uiState.currentWeekData,
                                isCurrentWeek = uiState.selectedWeekOffset == 0,
                                onPreviousWeek = { viewModel.goToPreviousWeek() },
                                onNextWeek = { viewModel.goToNextWeek() },
                                onCurrentWeek = { viewModel.goToCurrentWeek() }
                            )
                        }
                    }

                    // Week calendar card
                    item {
                        StaggeredItem(index = 2, delayPerItem = 50L) {
                            uiState.currentWeekData?.let { weekData ->
                                WeekCalendarCard(
                                    weekData = weekData,
                                    summaryMessage = uiState.weeklySummaryMessage
                                )
                            }
                        }
                    }

                    // Stats card
                    item {
                        StaggeredItem(index = 3, delayPerItem = 50L) {
                            uiState.currentWeekData?.let { weekData ->
                                WeekStatsCard(weekData = weekData)
                            }
                        }
                    }

                    // Daily breakdown header
                    item {
                        StaggeredItem(index = 4, delayPerItem = 50L) {
                            PremiumSectionChip(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                text = "Daily Breakdown",
                                icon = DailyWellIcons.Analytics.Calendar
                            )
                        }
                    }

                    // Daily breakdown items with staggered animation
                    uiState.currentWeekData?.let { weekData ->
                        val pastDays = weekData.days.filter { !it.isFuture }
                        itemsIndexed(pastDays) { index, dayStatus ->
                            StaggeredItem(index = index, delayPerItem = 60L, baseDelay = 200L) {
                                DayBreakdownCard(
                                    dayStatus = dayStatus,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }

                    // Comparison card
                    item {
                        StaggeredItem(index = 5, delayPerItem = 50L) {
                            ComparisonCard(
                                currentWeek = uiState.currentWeekData,
                                previousWeek = uiState.previousWeekData
                            )
                        }
                    }
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
    val dailyWellColors = LocalDailyWellColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(dailyWellColors.surfaceSubtle)
                .pressScale()
                .then(Modifier),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onPreviousWeek, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = DailyWellIcons.Nav.Back,
                    contentDescription = "Previous week",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatWeekRange(weekData),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!isCurrentWeek) {
                TextButton(onClick = onCurrentWeek) {
                    Text(
                        text = "Go to current week",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Next button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isCurrentWeek) dailyWellColors.surfaceMuted
                    else dailyWellColors.surfaceSubtle
                )
                .pressScale(enabled = !isCurrentWeek),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onNextWeek,
                enabled = !isCurrentWeek,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = DailyWellIcons.Nav.ArrowForward,
                    contentDescription = "Next week",
                    modifier = Modifier.size(18.dp),
                    tint = if (isCurrentWeek) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun WeekCalendarCard(
    weekData: WeekData,
    summaryMessage: String
) {
    val dailyWellColors = LocalDailyWellColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = dailyWellColors.shadowLight
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassBackground,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        dailyWellColors.glassHighlight,
                        dailyWellColors.glassBorder
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            WeekCalendar(weekData = weekData)
            Spacer(modifier = Modifier.height(14.dp))

            // Summary with subtle background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(dailyWellColors.surfaceSubtle.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Text(
                    text = summaryMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun WeekStatsCard(weekData: WeekData) {
    val dailyWellColors = LocalDailyWellColors.current

    val completedDays = weekData.days.count { it.status == CompletionStatus.COMPLETE }
    val partialDays = weekData.days.count { it.status == CompletionStatus.PARTIAL }
    val missedDays = weekData.days.count {
        it.status == CompletionStatus.NONE && !it.isFuture
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = dailyWellColors.shadowLight
            )
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 0.5.dp,
                color = dailyWellColors.glassBorder,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Week Stats",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatPill(
                    value = completedDays.toString(),
                    label = "Complete",
                    color = Success,
                    icon = DailyWellIcons.Status.Success
                )
                StatPill(
                    value = partialDays.toString(),
                    label = "Partial",
                    color = Warning,
                    icon = DailyWellIcons.Misc.Time
                )
                StatPill(
                    value = missedDays.toString(),
                    label = "Missed",
                    color = Error,
                    icon = DailyWellIcons.Status.Error
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Animated completion rate bar
            val rate = (weekData.completionRate * 100).toInt()
            val animatedProgress by animateFloatAsState(
                targetValue = weekData.completionRate,
                animationSpec = tween(1000, easing = EaseOutCubic),
                label = "weekProgress"
            )

            val barColor = when {
                rate >= 80 -> Success
                rate >= 50 -> Warning
                else -> Error
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(dailyWellColors.surfaceMuted)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    barColor,
                                    barColor.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "$rate% completion rate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatPill(
    value: String,
    label: String,
    color: Color,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Colored circle with value
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayBreakdownCard(
    dayStatus: DayStatus,
    modifier: Modifier = Modifier
) {
    val dailyWellColors = LocalDailyWellColors.current
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val statusColor = when (dayStatus.status) {
        CompletionStatus.COMPLETE -> Success
        CompletionStatus.PARTIAL -> Warning
        CompletionStatus.NONE -> Error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (dayStatus.isToday) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = if (dayStatus.isToday) 1.dp else 0.5.dp,
                color = if (dayStatus.isToday) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                } else {
                    dailyWellColors.glassBorder.copy(alpha = 0.3f)
                },
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = dayNames.getOrNull(dayStatus.dayOfWeek) ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (dayStatus.isToday) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (dayStatus.isToday) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Completion count with mini progress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${dayStatus.completedCount}/${dayStatus.totalCount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )

                // Mini status badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = when (dayStatus.status) {
                            CompletionStatus.COMPLETE -> DailyWellIcons.Actions.Check
                            CompletionStatus.PARTIAL -> DailyWellIcons.Misc.Time
                            CompletionStatus.NONE -> DailyWellIcons.Nav.Close
                            else -> DailyWellIcons.Actions.Remove
                        },
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = statusColor
                    )
                }
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

    val dailyWellColors = LocalDailyWellColors.current
    val currentRate = (currentWeek.completionRate * 100).toInt()
    val previousRate = (previousWeek.completionRate * 100).toInt()
    val difference = currentRate - previousRate

    val trendColor = when {
        difference > 0 -> Success
        difference < 0 -> Error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = dailyWellColors.shadowLight
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        trendColor.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = dailyWellColors.glassBorder,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "vs Last Week",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        difference > 0 -> DailyWellIcons.Analytics.TrendUp
                        difference < 0 -> DailyWellIcons.Analytics.TrendDown
                        else -> DailyWellIcons.Analytics.TrendFlat
                    },
                    contentDescription = "Trend",
                    modifier = Modifier.size(28.dp),
                    tint = trendColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when {
                        difference > 0 -> "+$difference%"
                        difference < 0 -> "$difference%"
                        else -> "Same"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

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
