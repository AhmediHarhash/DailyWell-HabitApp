package com.dailywell.app.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import com.dailywell.app.data.model.*
import com.dailywell.app.core.theme.Primary
import com.dailywell.app.core.theme.Success
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumTopBar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class TrackerExportMode {
    WEEKLY,
    MONTHLY
}

data class TrackerExportRow(
    val habitId: String,
    val habitName: String,
    val habitEmoji: String,
    val completionsByDate: Map<String, Boolean>,
    val weeklyCompleted: List<Int> = emptyList(),
    val weeklyTargets: List<Int> = emptyList(),
    val longestRun: Int = 0
)

data class TrackerExportPayload(
    val mode: TrackerExportMode,
    val title: String,
    val subtitle: String,
    val dates: List<String>,
    val rows: List<TrackerExportRow>,
    val generatedAtIso: String
)

private data class TrackerHeadlineStats(
    val completed: Int,
    val goal: Int,
    val streakDays: Int,
    val activeHabits: Int,
    val trackableDays: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarIntegrationScreen(
    viewModel: CalendarViewModel,
    onNavigateBack: () -> Unit,
    pendingOAuthCallback: CalendarOAuthCallback? = null,
    onOAuthCallbackConsumed: () -> Unit = {},
    onShareTrackerImage: (TrackerExportPayload) -> Unit = {},
    onExportTrackerPdf: (TrackerExportPayload) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    val trackerHeadline = remember(
        uiState.trackerMode,
        uiState.weekDates,
        uiState.weeklyHabitGrid,
        uiState.monthDates,
        uiState.monthlyHabitGrid
    ) {
        calculateTrackerHeadline(uiState)
    }

    LaunchedEffect(uiState.oauthUrl) {
        val oauthUrl = uiState.oauthUrl ?: return@LaunchedEffect
        runCatching { uriHandler.openUri(oauthUrl) }
        viewModel.dismissOAuthUrl()
    }

    LaunchedEffect(pendingOAuthCallback) {
        val callback = pendingOAuthCallback ?: return@LaunchedEffect
        viewModel.handleOAuthCallback(callback.provider, callback.authCode)
        onOAuthCallbackConsumed()
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Habit Tracker",
                    subtitle = "Complete habits with a simple checkbox matrix.",
                    onNavigationClick = onNavigateBack,
                    trailingActions = {
                        if (showAdvanced && uiState.isCalendarConnected) {
                            IconButton(onClick = { viewModel.syncCalendar() }) {
                                if (uiState.syncState.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Settings, contentDescription = "Sync")
                                }
                            }
                        }
                    }
                )
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
                    TrackerHeadlineCard(
                        completed = trackerHeadline.completed,
                        goal = trackerHeadline.goal,
                        streakDays = trackerHeadline.streakDays,
                        activeHabits = trackerHeadline.activeHabits,
                        trackableDays = trackerHeadline.trackableDays
                    )
                }

                item {
                    TrackerControlsCard(
                        mode = uiState.trackerMode,
                        visibleMonth = uiState.visibleMonth,
                        onModeChange = { viewModel.setTrackerMode(it) },
                        onPreviousMonth = { viewModel.showPreviousMonth() },
                        onNextMonth = { viewModel.showNextMonth() },
                        onShareImage = { onShareTrackerImage(buildTrackerExportPayload(uiState)) },
                        onExportPdf = { onExportTrackerPdf(buildTrackerExportPayload(uiState)) }
                    )
                }

                if (uiState.trackerMode == TrackerMode.WEEKLY) {
                    if (uiState.weeklyHabitGrid.isNotEmpty() && uiState.weekDates.isNotEmpty()) {
                        item {
                            WeeklyHabitGridCard(
                                weekDates = uiState.weekDates,
                                rows = uiState.weeklyHabitGrid,
                                selectedDate = uiState.selectedDate,
                                updatingCellKeys = uiState.updatingGridCells,
                                onToggleCompletion = { habitId, date, currentlyCompleted ->
                                    viewModel.toggleHabitCompletionForDate(
                                        habitId = habitId,
                                        date = date,
                                        currentlyCompleted = currentlyCompleted
                                    )
                                }
                            )
                        }
                    }
                } else {
                    if (uiState.monthlyHabitGrid.isNotEmpty() && uiState.monthDates.isNotEmpty()) {
                        item {
                            MonthlyHabitGridCard(
                                visibleMonth = uiState.visibleMonth,
                                monthDates = uiState.monthDates,
                                rows = uiState.monthlyHabitGrid,
                                selectedDate = uiState.selectedDate,
                                updatingCellKeys = uiState.updatingGridCells,
                                onToggleCompletion = { habitId, date, currentlyCompleted ->
                                    viewModel.toggleHabitCompletionForDate(
                                        habitId = habitId,
                                        date = date,
                                        currentlyCompleted = currentlyCompleted
                                    )
                                }
                            )
                        }
                    }
                }

                item {
                    AdvancedCalendarCard(
                        expanded = showAdvanced,
                        isConnected = uiState.isCalendarConnected,
                        onToggleExpanded = { showAdvanced = !showAdvanced }
                    )
                }

                if (showAdvanced) {
                    item {
                        CalendarConnectionCard(
                            isConnected = uiState.isCalendarConnected,
                            accounts = uiState.connectedAccounts,
                            onConnectClick = { viewModel.showConnectDialog(true) },
                            onDisconnectClick = { viewModel.disconnectCalendar(it) }
                        )
                    }

                    if (uiState.isCalendarConnected) {
                        item {
                            AdvancedSyncStatusCard(
                                syncState = uiState.syncState,
                                onSyncNow = { viewModel.syncCalendar() }
                            )
                        }
                    } else {
                        item {
                            CalendarBenefitsCard()
                        }
                    }
                }
            }
        }
    }

    // Connect Dialog
    if (uiState.showConnectDialog) {
        ConnectCalendarDialog(
            onDismiss = { viewModel.showConnectDialog(false) },
            onConnectGoogle = { viewModel.connectGoogleCalendar() },
            onConnectOutlook = { viewModel.connectOutlookCalendar() }
        )
    }

    // Habit Settings Dialog
    if (uiState.showHabitSettingsDialog && uiState.habitCalendarSettings != null) {
        HabitCalendarSettingsDialog(
            settings = uiState.habitCalendarSettings!!,
            onDismiss = { viewModel.showHabitSettingsDialog(null) },
            onSave = { viewModel.updateHabitCalendarSettings(it) }
        )
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error and clear after delay
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    // Success Snackbar
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSuccessMessage()
        }
    }
}

@Composable
private fun TrackerHeadlineCard(
    completed: Int,
    goal: Int,
    streakDays: Int,
    activeHabits: Int,
    trackableDays: Int
) {
    val completionPercent = remember(completed, goal) {
        if (goal <= 0) 0 else ((completed.toFloat() / goal.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrackerMetricChip(
                modifier = Modifier.weight(1.25f),
                label = "Completed / Goal",
                value = "$completed/$goal",
                hint = if (goal > 0) "$activeHabits habits x $trackableDays days" else "No active goals"
            )
            TrackerMetricChip(
                modifier = Modifier.weight(0.95f),
                label = "Streak",
                value = "${streakDays}d",
                hint = "Longest current run"
            )
            TrackerMetricChip(
                modifier = Modifier.weight(0.95f),
                label = "Completion",
                value = "$completionPercent%",
                hint = "Across active days"
            )
        }
    }
}

@Composable
private fun AdvancedCalendarCard(
    expanded: Boolean,
    isConnected: Boolean,
    onToggleExpanded: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Calendar sync (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isConnected) "Connected" else "Optional",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onToggleExpanded) {
                Text(if (expanded) "Collapse" else "Expand")
            }
        }
    }
}

@Composable
private fun AdvancedSyncStatusCard(
    syncState: CalendarSyncState,
    onSyncNow: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Calendar Sync",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (syncState.lastSyncTime > 0L) {
                            "Last sync: ${formatSyncTimestamp(syncState.lastSyncTime)}"
                        } else {
                            "No sync yet"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    enabled = !syncState.isSyncing,
                    onClick = onSyncNow
                ) {
                    if (syncState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.6.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(if (syncState.isSyncing) "Syncing..." else "Sync now")
                }
            }
            Text(
                text = "Events synced: ${syncState.eventsCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            syncState.lastError?.takeIf { it.isNotBlank() }?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun calculateTrackerHeadline(uiState: CalendarUiState): TrackerHeadlineStats {
    val today = LocalDate.now()
    return if (uiState.trackerMode == TrackerMode.MONTHLY) {
        val trackableDates = uiState.monthDates.filter { date ->
            val localDate = runCatching { LocalDate.parse(date) }.getOrNull()
            localDate != null && !localDate.isAfter(today)
        }
        val completed = uiState.monthlyHabitGrid.sumOf { row ->
            trackableDates.count { date -> row.completionsByDate[date] == true }
        }
        val goal = uiState.monthlyHabitGrid.size * trackableDates.size
        val streak = uiState.monthlyHabitGrid.maxOfOrNull { it.longestRun } ?: 0
        TrackerHeadlineStats(
            completed = completed,
            goal = goal,
            streakDays = streak,
            activeHabits = uiState.monthlyHabitGrid.size,
            trackableDays = trackableDates.size
        )
    } else {
        val trackableDates = uiState.weekDates.filter { date ->
            val localDate = runCatching { LocalDate.parse(date) }.getOrNull()
            localDate != null && !localDate.isAfter(today)
        }
        val completed = uiState.weeklyHabitGrid.sumOf { row ->
            trackableDates.count { date -> row.completionsByDate[date] == true }
        }
        val goal = uiState.weeklyHabitGrid.size * trackableDates.size
        val streak = uiState.weeklyHabitGrid.maxOfOrNull { row ->
            calculateLongestRun(row, trackableDates)
        } ?: 0
        TrackerHeadlineStats(
            completed = completed,
            goal = goal,
            streakDays = streak,
            activeHabits = uiState.weeklyHabitGrid.size,
            trackableDays = trackableDates.size
        )
    }
}

@Composable
fun CalendarConnectionCard(
    isConnected: Boolean,
    accounts: List<CalendarAccount>,
    onConnectClick: () -> Unit,
    onDisconnectClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                Color(0xFF1A2C1A)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = if (isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Calendar Connected" else "Connect Your Calendar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isConnected) {
                    Button(
                        onClick = onConnectClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Success
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect")
                    }
                }
            }

            if (isConnected && accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                accounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (account.provider) {
                                    CalendarProvider.GOOGLE -> Icons.Default.AccountCircle
                                    CalendarProvider.OUTLOOK -> Icons.Default.Email
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = account.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = account.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { onDisconnectClick(account.id) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Disconnect",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                TextButton(onClick = onConnectClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Another Calendar")
                }
            }

            if (!isConnected) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sync your calendar to get smart habit scheduling suggestions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WeekCalendarView(
    selectedDate: String,
    schedules: List<CalendarDaySchedule>,
    onDateSelected: (String) -> Unit
) {
    val today = LocalDate.now()
    val selected = LocalDate.parse(selectedDate)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = selected.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)

                items(7) { index ->
                    val date = startOfWeek.plusDays(index.toLong())
                    val dateStr = date.toString()
                    val schedule = schedules.find { it.date == dateStr }
                    val isSelected = dateStr == selectedDate
                    val isToday = date == today

                    DayCell(
                        date = date,
                        isSelected = isSelected,
                        isToday = isToday,
                        busyPercentage = schedule?.busyPercentage ?: 0f,
                        eventCount = schedule?.events?.size ?: 0,
                        onClick = { onDateSelected(dateStr) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackerControlsCard(
    mode: TrackerMode,
    visibleMonth: String,
    onModeChange: (TrackerMode) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShareImage: () -> Unit,
    onExportPdf: () -> Unit
) {
    val monthLabel = remember(visibleMonth) { formatMonthLabel(visibleMonth) }
    var exportMenuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onNextMonth) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next month")
                    }
                    Box {
                        IconButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Export options")
                        }
                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share image") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                onClick = {
                                    exportMenuExpanded = false
                                    onShareImage()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share PDF") },
                                leadingIcon = {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                },
                                onClick = {
                                    exportMenuExpanded = false
                                    onExportPdf()
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == TrackerMode.WEEKLY,
                    onClick = { onModeChange(TrackerMode.WEEKLY) },
                    label = { Text("Weekly") }
                )
                FilterChip(
                    selected = mode == TrackerMode.MONTHLY,
                    onClick = { onModeChange(TrackerMode.MONTHLY) },
                    label = { Text("Monthly") }
                )
            }
        }
    }
}

@Composable
fun WeeklyHabitGridCard(
    weekDates: List<String>,
    rows: List<WeeklyHabitGridRow>,
    selectedDate: String,
    updatingCellKeys: Set<String>,
    onToggleCompletion: (habitId: String, date: String, currentlyCompleted: Boolean) -> Unit
) {
    val today = LocalDate.now()
    val parsedDates = remember(weekDates) {
        weekDates.associateWith { date ->
            runCatching { LocalDate.parse(date) }.getOrNull()
        }
    }
    val trackableDates = weekDates.filter { date ->
        parsedDates[date]?.isAfter(today) != true
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Success.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Weekly checkbox matrix - tap boxes to mark done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Habit",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(170.dp)
                    )
                    weekDates.forEach { date ->
                        val localDate = parsedDates[date]
                        val label = localDate?.dayOfWeek
                            ?.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            ?: ""
                        val isFuture = localDate?.isAfter(today) == true
                        val isSelected = date == selectedDate
                        Column(
                            modifier = Modifier
                                .width(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> Success.copy(alpha = 0.2f)
                                        isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = localDate?.dayOfMonth?.toString() ?: "-",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(58.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(170.dp)) {
                            Text(
                                text = "${row.habitEmoji} ${row.habitName}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        weekDates.forEach { date ->
                            val completed = row.completionsByDate[date] == true
                            val localDate = parsedDates[date]
                            val isFuture = localDate?.isAfter(today) == true
                            val cellKey = buildGridCellKey(row.habitId, date)
                            val isUpdating = updatingCellKeys.contains(cellKey)
                            val isSelected = date == selectedDate
                            val canToggle = !isFuture && !isUpdating

                            Box(
                                modifier = Modifier
                                    .width(38.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(
                                            when {
                                                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                                                completed -> Success.copy(alpha = 0.24f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = when {
                                                isSelected -> Success
                                                completed -> Success.copy(alpha = 0.9f)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                            },
                                            shape = RoundedCornerShape(7.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(11.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                    } else if (completed) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Success,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else if (!isFuture) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(7.dp))
                                        .clickable(enabled = canToggle) {
                                            onToggleCompletion(row.habitId, date, completed)
                                        }
                                )
                            }
                        }
                        val rowCompleted = trackableDates.count { date ->
                            row.completionsByDate[date] == true
                        }
                        val rowTarget = trackableDates.size
                        Text(
                            text = if (rowTarget == 0) "--" else "$rowCompleted/$rowTarget",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(58.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Day total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(170.dp)
                    )
                    weekDates.forEach { date ->
                        val done = rows.count { row -> row.completionsByDate[date] == true }
                        val future = parsedDates[date]?.isAfter(today) == true
                        Text(
                            text = if (future) "--" else "$done/${rows.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (future) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(38.dp)
                        )
                    }
                    val weeklyDone = rows.sumOf { row ->
                        trackableDates.count { date -> row.completionsByDate[date] == true }
                    }
                    val weeklyTarget = rows.size * trackableDates.size
                    Text(
                        text = if (weeklyTarget == 0) "--" else "$weeklyDone/$weeklyTarget",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(58.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyHabitGridCard(
    visibleMonth: String,
    monthDates: List<String>,
    rows: List<MonthlyHabitGridRow>,
    selectedDate: String,
    updatingCellKeys: Set<String>,
    onToggleCompletion: (habitId: String, date: String, currentlyCompleted: Boolean) -> Unit
) {
    val today = LocalDate.now()
    val parsedDates = remember(monthDates) {
        monthDates.associateWith { date ->
            runCatching { LocalDate.parse(date) }.getOrNull()
        }
    }
    val trackableDates = monthDates.filter { date ->
        parsedDates[date]?.isAfter(today) != true
    }
    val weekCount = remember(monthDates) {
        if (monthDates.isEmpty()) 0 else ((monthDates.size - 1) / 7) + 1
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Monthly checkbox matrix - ${formatMonthLabel(visibleMonth)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Habit",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(180.dp)
                    )
                    monthDates.forEach { date ->
                        val localDate = parsedDates[date]
                        val isFuture = localDate?.isAfter(today) == true
                        val isSelected = date == selectedDate
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isSelected -> Success.copy(alpha = 0.22f)
                                        isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    }
                                )
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = localDate?.dayOfMonth?.toString() ?: "-",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFuture) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    repeat(weekCount) { index ->
                        Text(
                            text = "W${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(42.dp)
                        )
                    }
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(180.dp)) {
                            Text(
                                text = "${row.habitEmoji} ${row.habitName}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        monthDates.forEach { date ->
                            val completed = row.completionsByDate[date] == true
                            val localDate = parsedDates[date]
                            val isFuture = localDate?.isAfter(today) == true
                            val isSelected = date == selectedDate
                            val cellKey = buildGridCellKey(row.habitId, date)
                            val isUpdating = updatingCellKeys.contains(cellKey)
                            val canToggle = !isFuture && !isUpdating

                            Box(
                                modifier = Modifier.width(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(
                                            when {
                                                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                                                completed -> Success.copy(alpha = 0.25f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.8.dp else 1.dp,
                                            color = when {
                                                isSelected -> Success
                                                completed -> Success.copy(alpha = 0.9f)
                                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                            },
                                            shape = RoundedCornerShape(5.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUpdating) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(9.dp),
                                            strokeWidth = 1.3.dp
                                        )
                                    } else if (completed) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Success,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(5.dp))
                                        .clickable(enabled = canToggle) {
                                            onToggleCompletion(row.habitId, date, completed)
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        repeat(weekCount) { weekIndex ->
                            val done = row.weeklyCompleted.getOrNull(weekIndex) ?: 0
                            val target = row.weeklyTargets.getOrNull(weekIndex) ?: 0
                            Text(
                                text = if (target == 0) "--" else "$done/$target",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = if (target == 0) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.width(42.dp)
                            )
                        }
                        val rowCompleted = trackableDates.count { date ->
                            row.completionsByDate[date] == true
                        }
                        val rowTarget = trackableDates.size
                        Text(
                            text = if (rowTarget == 0) "--" else "$rowCompleted/$rowTarget",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Day total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(180.dp)
                    )
                    monthDates.forEach { date ->
                        val done = rows.count { row -> row.completionsByDate[date] == true }
                        val future = parsedDates[date]?.isAfter(today) == true
                        Text(
                            text = if (future) "--" else "$done",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = if (future) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.width(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    repeat(weekCount) { weekIndex ->
                        val weekDates = monthDates.filter { date ->
                            val local = parsedDates[date] ?: return@filter false
                            ((local.dayOfMonth - 1) / 7) == weekIndex
                        }
                        val weekDone = rows.sumOf { row ->
                            weekDates.count { date -> row.completionsByDate[date] == true }
                        }
                        val weekTarget = rows.sumOf { row ->
                            weekDates.count { date ->
                                val localDate = parsedDates[date]
                                localDate != null && !localDate.isAfter(today)
                            }
                        }
                        Text(
                            text = if (weekTarget == 0) "--" else "$weekDone/$weekTarget",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(42.dp)
                        )
                    }
                    val monthDone = rows.sumOf { row ->
                        trackableDates.count { date -> row.completionsByDate[date] == true }
                    }
                    val monthTarget = rows.size * trackableDates.size
                    Text(
                        text = if (monthTarget == 0) "--" else "$monthDone/$monthTarget",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(56.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackerMetricChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    hint: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun buildGridCellKey(habitId: String, date: String): String = "$habitId|$date"

private fun calculateLongestRun(
    row: WeeklyHabitGridRow,
    orderedDates: List<String>
): Int {
    var longest = 0
    var current = 0
    orderedDates.forEach { date ->
        if (row.completionsByDate[date] == true) {
            current += 1
            if (current > longest) longest = current
        } else {
            current = 0
        }
    }
    return longest
}

private fun buildTrackerExportPayload(uiState: CalendarUiState): TrackerExportPayload {
    return if (uiState.trackerMode == TrackerMode.MONTHLY) {
        TrackerExportPayload(
            mode = TrackerExportMode.MONTHLY,
            title = "DailyWell Monthly Tracker",
            subtitle = formatMonthLabel(uiState.visibleMonth),
            dates = uiState.monthDates,
            rows = uiState.monthlyHabitGrid.map { row ->
                TrackerExportRow(
                    habitId = row.habitId,
                    habitName = row.habitName,
                    habitEmoji = row.habitEmoji,
                    completionsByDate = row.completionsByDate,
                    weeklyCompleted = row.weeklyCompleted,
                    weeklyTargets = row.weeklyTargets,
                    longestRun = row.longestRun
                )
            },
            generatedAtIso = Instant.now().toString()
        )
    } else {
        val firstDate = uiState.weekDates.firstOrNull()
        val lastDate = uiState.weekDates.lastOrNull()
        val subtitle = if (firstDate != null && lastDate != null) {
            "${formatDateForSubtitle(firstDate)} - ${formatDateForSubtitle(lastDate)}"
        } else {
            "Weekly snapshot"
        }
        TrackerExportPayload(
            mode = TrackerExportMode.WEEKLY,
            title = "DailyWell Weekly Tracker",
            subtitle = subtitle,
            dates = uiState.weekDates,
            rows = uiState.weeklyHabitGrid.map { row ->
                TrackerExportRow(
                    habitId = row.habitId,
                    habitName = row.habitName,
                    habitEmoji = row.habitEmoji,
                    completionsByDate = row.completionsByDate,
                    weeklyCompleted = listOf(row.completedCount),
                    weeklyTargets = listOf(uiState.weekDates.count())
                )
            },
            generatedAtIso = Instant.now().toString()
        )
    }
}

private fun formatSyncTimestamp(epochMillis: Long): String {
    return runCatching {
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }.getOrDefault("--")
}

private fun formatMonthLabel(visibleMonth: String): String {
    val parsed = runCatching { YearMonth.parse(visibleMonth) }.getOrNull() ?: return visibleMonth
    val monthName = parsed.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$monthName ${parsed.year}"
}

private fun formatDateForSubtitle(date: String): String {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
    val monthName = parsed.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$monthName ${parsed.dayOfMonth}"
}

@Composable
fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    busyPercentage: Float,
    eventCount: Int,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> Success
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        isSelected -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.7f)
        )

        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Busy indicator
        if (eventCount > 0) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = when {
                            busyPercentage > 70 -> Color(0xFFE53935)
                            busyPercentage > 40 -> Color(0xFFFFA726)
                            else -> Color(0xFF4CAF50)
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun DayScheduleCard(schedule: CalendarDaySchedule) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${schedule.events.size} events scheduled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${schedule.busyPercentage.toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            schedule.busyPercentage > 70 -> Color(0xFFE53935)
                            schedule.busyPercentage > 40 -> Color(0xFFFFA726)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                    Text(
                        text = "busy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (schedule.freeSlots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                val totalFreeMinutes = schedule.freeSlots.sumOf { it.durationMinutes }
                val hours = totalFreeMinutes / 60
                val minutes = totalFreeMinutes % 60

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (hours > 0) "${hours}h " else ""}${minutes}min of free time available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Success
                    )
                }
            }
        }
    }
}

@Composable
fun FreeSlotCard(
    slot: FreeTimeSlot,
    onScheduleHabit: (String) -> Unit
) {
    val startTime = Instant.ofEpochMilli(slot.startTime)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))
    val endTime = Instant.ofEpochMilli(slot.endTime)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2C1A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$startTime - $endTime",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${slot.durationMinutes} minutes free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quality indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    val filled = index < (slot.qualityScore * 5).toInt()
                    Icon(
                        imageVector = if (filled) Icons.Default.Star else Icons.Default.Star,
                        contentDescription = null,
                        tint = if (filled) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HabitSuggestionCard(
    suggestion: HabitTimeSuggestion,
    onAccept: () -> Unit,
    onConfigure: () -> Unit,
    onSchedule30Days: () -> Unit,
    onCustomPlan: (CalendarSchedulePlan) -> Unit,
    planProgress: HabitPlanProgress?
) {
    var showCustomPlanDialog by remember { mutableStateOf(false) }
    val suggestedTime = Instant.ofEpochMilli(suggestion.suggestedTime)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (suggestion.isOptimal)
                Color(0xFF1A3C1A)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.getHabitIcon(suggestion.habitId),
                        contentDescription = suggestion.habitName,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = suggestion.habitName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (suggestion.isOptimal) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Success,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "BEST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Suggested: $suggestedTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success
                        )
                    }
                }

                IconButton(onClick = onConfigure) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Configure",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = suggestion.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (planProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Plan Progress: ${planProgress.completionPercent.toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        LinearProgressIndicator(
                            progress = { (planProgress.completionPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Success,
                            trackColor = Success.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${planProgress.completedDays}/${planProgress.targetDays} days completed | ${planProgress.scheduledDays}/${planProgress.targetDays} scheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${planProgress.planStartDate} to ${planProgress.planEndDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onConfigure,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Later")
                }

                OutlinedButton(
                    onClick = onSchedule30Days,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Plan 30 Days")
                }

                OutlinedButton(
                    onClick = { showCustomPlanDialog = true },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Customize")
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Success
                    )
                ) {
                    Icon(
                        Icons.Outlined.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule")
                }
            }
        }
    }

    if (showCustomPlanDialog) {
        CustomPlanDialog(
            habitName = suggestion.habitName,
            onDismiss = { showCustomPlanDialog = false },
            onApply = { plan ->
                showCustomPlanDialog = false
                onCustomPlan(plan)
            }
        )
    }
}

@Composable
private fun CustomPlanDialog(
    habitName: String,
    onDismiss: () -> Unit,
    onApply: (CalendarSchedulePlan) -> Unit
) {
    var days by remember { mutableStateOf(30f) }
    var duration by remember { mutableStateOf(30f) }
    var startHour by remember { mutableStateOf(9f) }
    var endHour by remember { mutableStateOf(17f) }
    var blockTitle by remember { mutableStateOf("") }
    var weekdays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Plan for $habitName") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Days: ${days.toInt()}")
                Slider(
                    value = days,
                    onValueChange = { days = it },
                    valueRange = 7f..90f,
                    steps = 82
                )

                Text("Duration: ${duration.toInt()} min")
                Slider(
                    value = duration,
                    onValueChange = { duration = it },
                    valueRange = 15f..120f,
                    steps = 20
                )

                Text("Start time: ${formatHourLabel(startHour.toInt())}")
                Slider(
                    value = startHour,
                    onValueChange = { value ->
                        startHour = value
                        if (endHour <= startHour + 1f) {
                            endHour = (startHour + 1f).coerceAtMost(24f)
                        }
                    },
                    valueRange = 5f..22f,
                    steps = 16
                )

                Text("End time: ${formatHourLabel(endHour.toInt())}")
                Slider(
                    value = endHour,
                    onValueChange = { value ->
                        endHour = value.coerceAtLeast(startHour + 1f)
                    },
                    valueRange = 6f..24f,
                    steps = 17
                )

                Text(
                    text = "Weekdays",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                val dayLabels = listOf(
                    1 to "Mon",
                    2 to "Tue",
                    3 to "Wed",
                    4 to "Thu",
                    5 to "Fri",
                    6 to "Sat",
                    7 to "Sun"
                )
                dayLabels.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (dayValue, label) ->
                            val selected = weekdays.contains(dayValue)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    weekdays = if (selected) {
                                        (weekdays - dayValue).ifEmpty { weekdays }
                                    } else {
                                        weekdays + dayValue
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = blockTitle,
                    onValueChange = { blockTitle = it },
                    label = { Text("Custom title (optional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalizedDays = days.toInt().coerceIn(7, 90)
                    val normalizedDuration = duration.toInt().coerceIn(15, 120)
                    val normalizedStart = startHour.toInt().coerceIn(0, 23)
                    val normalizedEnd = endHour.toInt().coerceIn(normalizedStart + 1, 24)
                    onApply(
                        CalendarSchedulePlan(
                            days = normalizedDays,
                            durationMinutes = normalizedDuration,
                            startHour = normalizedStart,
                            endHour = normalizedEnd,
                            weekdays = weekdays,
                            blockTitle = blockTitle
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatHourLabel(hour24: Int): String {
    return when {
        hour24 == 0 || hour24 == 24 -> "12:00 AM"
        hour24 < 12 -> "$hour24:00 AM"
        hour24 == 12 -> "12:00 PM"
        else -> "${hour24 - 12}:00 PM"
    }
}

@Composable
fun CalendarEventCard(event: CalendarEvent) {
    val startTime = Instant.ofEpochMilli(event.startTime)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))
    val endTime = Instant.ofEpochMilli(event.endTime)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a"))

    val eventColor = when {
        event.isHabitBlock -> Success
        event.busyStatus == BusyStatus.FREE -> Color(0xFF4CAF50)
        event.busyStatus == BusyStatus.TENTATIVE -> Color(0xFFFFA726)
        else -> Color(0xFFE53935)
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(eventColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (event.isAllDay) "All day" else "$startTime - $endTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                event.location?.let { location ->
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (event.isHabitBlock) {
                Surface(
                    color = Success.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "HABIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Success,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarBenefitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.DateRange,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Smart Calendar Integration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            val benefits = listOf(
                "Find the best time for habits based on your schedule",
                "Auto-block time for habits on your calendar",
                "Get notified about free time slots",
                "Reschedule suggestions when conflicts arise"
            )

            benefits.forEach { benefit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectCalendarDialog(
    onDismiss: () -> Unit,
    onConnectGoogle: () -> Unit,
    onConnectOutlook: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect Calendar") },
        text = {
            Column {
                Text(
                    text = "Choose a calendar service to connect:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Google Calendar Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onConnectGoogle),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFF4285F4),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Google Calendar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connect your Google account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Outlook Calendar Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onConnectOutlook),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = Color(0xFF0078D4),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Outlook Calendar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connect your Microsoft account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HabitCalendarSettingsDialog(
    settings: HabitCalendarSettings,
    onDismiss: () -> Unit,
    onSave: (HabitCalendarSettings) -> Unit
) {
    var autoBlockEnabled by remember { mutableStateOf(settings.autoBlockEnabled) }
    var preferredDuration by remember { mutableStateOf(settings.preferredDuration) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Habit Calendar Settings") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-block time",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Automatically reserve time on your calendar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoBlockEnabled,
                        onCheckedChange = { autoBlockEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Preferred duration: $preferredDuration min",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = preferredDuration.toFloat(),
                    onValueChange = { preferredDuration = it.toInt() },
                    valueRange = 15f..120f,
                    steps = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(settings.copy(
                        autoBlockEnabled = autoBlockEnabled,
                        preferredDuration = preferredDuration
                    ))
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

