package com.dailywell.app.ui.screens.water

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import kotlinx.datetime.*

/**
 * Water Tracking Screen - Complete hydration tracking feature
 * Actual implementation for Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun WaterTrackingScreen(
    viewModel: WaterTrackingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Water Tracker",
                    subtitle = "${uiState.todaySummary.totalMl} / ${uiState.todaySummary.goalMl} ml",
                    onNavigationClick = onNavigateBack,
                    trailingActions = {
                        IconButton(onClick = { viewModel.setShowSettingsSheet(true) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.quickAdd() },
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                ) {
                    Text("Quick Add", fontWeight = FontWeight.Bold)
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PremiumSectionChip(
                        text = "Hydration overview",
                        icon = DailyWellIcons.Habits.Water
                    )
                }

                // Progress Ring Card
                item {
                    HydrationProgressCard(
                        summary = uiState.todaySummary,
                        onAddClick = { viewModel.setShowAddDialog(true) }
                    )
                }

                // Quick Add Buttons
                item {
                    QuickAddSection(
                        onGlassSelected = { viewModel.logGlass(it) }
                    )
                }

                // Hydration Status Card
                item {
                    HydrationStatusCard(summary = uiState.todaySummary)
                }

                // Weekly Stats
                uiState.weeklyStats?.let { stats ->
                    item {
                        WeeklyStatsCard(stats = stats)
                    }
                }

                // Insights
                if (uiState.insights.isNotEmpty()) {
                    item {
                        InsightsSection(insights = uiState.insights)
                    }
                }

                // Today's Entries
                if (uiState.todaySummary.entries.isNotEmpty()) {
                    item {
                        PremiumSectionChip(
                            text = "Today's log",
                            icon = DailyWellIcons.Analytics.Calendar
                        )
                    }

                    items(uiState.todaySummary.entries.sortedByDescending { it.timestamp }) { entry ->
                        WaterEntryItem(
                            entry = entry,
                            onRemove = { viewModel.removeEntry(entry.id) }
                        )
                    }
                }

                // Bottom spacing for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Add Water Dialog
    if (uiState.showAddDialog) {
        AddWaterDialog(
            selectedSource = uiState.selectedSource,
            customAmount = uiState.customAmountMl,
            onSourceSelected = { viewModel.selectSource(it) },
            onCustomAmountChanged = { viewModel.updateCustomAmount(it) },
            onGlassSelected = { viewModel.logGlass(it) },
            onCustomConfirmed = {
                uiState.customAmountMl?.let { viewModel.logCustomAmount(it) }
            },
            onDismiss = { viewModel.setShowAddDialog(false) }
        )
    }

    // Settings Bottom Sheet
    if (uiState.showSettingsSheet) {
        WaterSettingsSheet(
            settings = uiState.settings,
            onSettingsChanged = { viewModel.updateSettings(it) },
            onDismiss = { viewModel.setShowSettingsSheet(false) }
        )
    }
}

@Composable
private fun HydrationProgressCard(
    summary: DailyWaterSummary,
    onAddClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (summary.progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Ring
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color(0xFFBBDEFB),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Progress ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color(0xFF2196F3),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${summary.totalMl}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                    Text(
                        text = "of ${summary.goalMl} ml",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1976D2)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress percentage
            Text(
                text = "${summary.progressPercent.toInt()}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (summary.isGoalReached) Color(0xFF4CAF50) else Color(0xFF2196F3)
            )

            if (summary.isGoalReached) {
                Text(
                    text = "Goal reached! Great job!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50)
                )
            } else {
                Text(
                    text = "${summary.remainingMl} ml to go",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1976D2)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add button
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("+ Add Water")
            }
        }
    }
}

@Composable
private fun QuickAddSection(
    onGlassSelected: (GlassSize) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    GlassSize.SMALL,
                    GlassSize.MEDIUM,
                    GlassSize.LARGE,
                    GlassSize.BOTTLE
                ).forEach { size ->
                    QuickAddButton(
                        size = size,
                        onClick = { onGlassSelected(size) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddButton(
    size: GlassSize,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFE3F2FD), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(size.icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${size.amountMl}ml",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HydrationStatusCard(summary: DailyWaterSummary) {
    val status = summary.hydrationStatus
    val statusColor = when (status) {
        HydrationStatus.EXCELLENT -> Color(0xFF4CAF50)
        HydrationStatus.GOOD -> Color(0xFF8BC34A)
        HydrationStatus.MODERATE -> Color(0xFFFFA726)
        HydrationStatus.LOW -> Color(0xFFFF7043)
        HydrationStatus.DEHYDRATED -> Color(0xFFE53935)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = status.icon,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = status.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    text = status.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = status.tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WeeklyStatsCard(stats: WeeklyHydrationStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "This Week",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = "${stats.averageDailyMl}",
                    label = "Avg Daily",
                    unit = "ml"
                )
                StatItem(
                    value = "${stats.goalReachedDays}",
                    label = "Goals Met",
                    unit = "/7"
                )
                StatItem(
                    value = "${stats.streakDays}",
                    label = "Streak",
                    unit = "days"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini bar chart for the week
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.dailySummaries.forEach { day ->
                    DayProgressBar(summary = day)
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun DayProgressBar(summary: DailyWaterSummary) {
    val progress = (summary.progressPercent / 100f).coerceIn(0f, 1f)
    val dayOfWeek = try {
        LocalDate.parse(summary.date).dayOfWeek.name.take(1)
    } catch (e: Exception) {
        "?"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(32.dp)
    ) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(60.dp)
                .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress)
                    .background(
                        if (summary.isGoalReached) Color(0xFF4CAF50) else Color(0xFF2196F3),
                        RoundedCornerShape(8.dp)
                    )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayOfWeek,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InsightsSection(insights: List<HydrationInsight>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            insights.forEach { insight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(insight.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = insight.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterEntryItem(
    entry: WaterEntry,
    onRemove: () -> Unit
) {
    val time = remember(entry.timestamp) {
        val instant = Instant.fromEpochMilliseconds(entry.timestamp)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(entry.source.icon, fontSize = 24.sp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.amountMl} ml",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${entry.source.displayName} at $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove) {
                Text("", fontSize = 20.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun AddWaterDialog(
    selectedSource: WaterSource,
    customAmount: Int?,
    onSourceSelected: (WaterSource) -> Unit,
    onCustomAmountChanged: (Int?) -> Unit,
    onGlassSelected: (GlassSize) -> Unit,
    onCustomConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Water") },
        text = {
            Column {
                // Source selection
                Text(
                    text = "Source",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(WaterSource.entries.toList()) { source ->
                        FilterChip(
                            selected = source == selectedSource,
                            onClick = { onSourceSelected(source) },
                            label = {
                                Text("${source.icon} ${source.displayName}")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preset sizes
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GlassSize.entries.take(4).forEach { size ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onGlassSelected(size) }
                                .padding(8.dp)
                        ) {
                            Text(size.icon, fontSize = 24.sp)
                            Text(
                                text = "${size.amountMl}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom amount
                Text(
                    text = "Custom Amount (ml)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customAmount?.toString() ?: "",
                    onValueChange = { value ->
                        onCustomAmountChanged(value.toIntOrNull())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter amount...") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            if (customAmount != null && customAmount > 0) {
                TextButton(onClick = onCustomConfirmed) {
                    Text("Add ${customAmount}ml")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WaterSettingsSheet(
    settings: WaterSettings,
    onSettingsChanged: (WaterSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Water Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Daily Goal
            Text(
                text = "Daily Goal: ${settings.dailyGoalMl} ml",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = settings.dailyGoalMl.toFloat(),
                onValueChange = {
                    onSettingsChanged(settings.copy(dailyGoalMl = it.toInt()))
                },
                valueRange = 1000f..4000f,
                steps = 11
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1L", style = MaterialTheme.typography.labelSmall)
                Text("4L", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hydration Reminders")
                Switch(
                    checked = settings.reminderEnabled,
                    onCheckedChange = {
                        onSettingsChanged(settings.copy(reminderEnabled = it))
                    }
                )
            }

            if (settings.reminderEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Remind every ${settings.reminderIntervalMinutes} minutes",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = settings.reminderIntervalMinutes.toFloat(),
                    onValueChange = {
                        onSettingsChanged(settings.copy(reminderIntervalMinutes = it.toInt()))
                    },
                    valueRange = 30f..120f,
                    steps = 5
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preferred glass size
            Text(
                text = "Preferred Quick Add Size",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GlassSize.entries.take(4).forEach { size ->
                    FilterChip(
                        selected = size == settings.preferredGlassSize,
                        onClick = {
                            onSettingsChanged(settings.copy(preferredGlassSize = size))
                        },
                        label = {
                            Text("${size.icon} ${size.amountMl}")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


