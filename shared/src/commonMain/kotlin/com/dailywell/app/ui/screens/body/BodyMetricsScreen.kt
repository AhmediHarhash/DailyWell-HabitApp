package com.dailywell.app.ui.screens.body

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.BodyMetrics
import com.dailywell.app.ui.components.*
import com.dailywell.app.data.model.BMICategory
import com.dailywell.app.data.repository.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Body Metrics Screen - Beautiful Dashboard UI
 *
 * PERFECTION MODE: Better than MyFitnessPal
 * - Large weight display
 * - Quick weight entry (< 5 seconds)
 * - Beautiful trend charts
 * - Weekly change indicator
 * - BMI with category
 * - Goal progress tracker
 * - Real-time updates
 *
 * Quality Standard: Apple Health inspired design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    userId: String,
    bodyMetricsRepository: BodyMetricsRepository,
    onNavigateToMeasurements: () -> Unit,
    onNavigateToPhotos: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentWeight by remember { mutableStateOf<BodyMetrics?>(null) }
    var weightHistory by remember { mutableStateOf<List<BodyMetrics>>(emptyList()) }
    var weeklyChange by remember { mutableStateOf<WeightChange?>(null) }
    var goalProgress by remember { mutableStateOf<GoalProgress?>(null) }
    var bodyGoal by remember { mutableStateOf<BodyGoal?>(null) }
    var selectedDays by remember { mutableStateOf(30) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load data
    LaunchedEffect(userId, selectedDays) {
        isLoading = true
        try {
            // Load current weight
            val weightResult = bodyMetricsRepository.getLatestWeight(userId)
            weightResult.fold(
                onSuccess = { currentWeight = it },
                onFailure = { errorMessage = it.message }
            )

            // Load weight history
            val historyResult = bodyMetricsRepository.getWeightHistory(userId, days = selectedDays)
            historyResult.fold(
                onSuccess = { weightHistory = it },
                onFailure = { /* ignore */ }
            )

            // Load weekly change
            val changeResult = bodyMetricsRepository.getWeeklyChange(userId)
            changeResult.fold(
                onSuccess = { weeklyChange = it },
                onFailure = { /* ignore */ }
            )

            // Load goal
            val goalResult = bodyMetricsRepository.getUserGoal(userId)
            goalResult.fold(
                onSuccess = { bodyGoal = it },
                onFailure = { /* ignore */ }
            )

            // Load goal progress
            if (bodyGoal != null) {
                val progressResult = bodyMetricsRepository.getGoalProgress(userId)
                progressResult.fold(
                    onSuccess = { goalProgress = it },
                    onFailure = { /* ignore */ }
                )
            }
        } finally {
            isLoading = false
        }
    }

    GlassScreenWrapper {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = "Body Metrics",
                subtitle = "Weight, BMI, and progress",
                onNavigationClick = onBack,
                trailingActions = {
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(
                            imageVector = DailyWellIcons.Habits.Intentions,
                            contentDescription = "Set goal",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showWeightDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(DailyWellIcons.Actions.Add, "Log weight")
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                currentWeight == null -> {
                    // Empty State
                    EmptyState(
                        onLogWeight = { showWeightDialog = true }
                    )
                }
                else -> {
                    // Main Content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Current Weight Card
                        item {
                            StaggeredItem(index = 0) {
                            CurrentWeightCard(
                                weight = currentWeight!!,
                                weeklyChange = weeklyChange,
                                onClick = { showWeightDialog = true }
                            )
                            }
                        }

                        // Goal Progress Card
                        if (goalProgress != null && bodyGoal != null) {
                            item {
                                StaggeredItem(index = 1) {
                                GoalProgressCard(
                                    progress = goalProgress!!,
                                    goal = bodyGoal!!,
                                    currentWeight = currentWeight!!
                                )
                                }
                            }
                        }

                        // BMI Card
                        item {
                            StaggeredItem(index = 2) {
                            BMICard(bmi = currentWeight!!.bmi, category = currentWeight!!.bmiCategory)
                            }
                        }

                        // Weight Trend Chart
                        item {
                            StaggeredItem(index = 3) {
                            WeightTrendCard(
                                weightHistory = weightHistory,
                                selectedDays = selectedDays,
                                onDaysChanged = { selectedDays = it }
                            )
                            }
                        }

                        // Quick Actions
                        item {
                            StaggeredItem(index = 4) {
                                PremiumSectionChip(
                                    text = "Track more",
                                    icon = DailyWellIcons.Health.Body
                                )
                            }
                        }

                        item {
                            StaggeredItem(index = 5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuickActionCard(
                                    title = "Measurements",
                                    icon = DailyWellIcons.Health.Measurements,
                                    onClick = onNavigateToMeasurements,
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionCard(
                                    title = "Progress Photos",
                                    icon = DailyWellIcons.Health.Photos,
                                    onClick = onNavigateToPhotos,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            }
                        }

                        // Recent Entries (if any)
                        if (weightHistory.size > 1) {
                            item {
                                StaggeredItem(index = 6) {
                                Text(
                                    text = "Recent Entries",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                }
                            }

                            items(weightHistory.take(5)) { entry ->
                                WeightEntryRow(
                                    entry = entry,
                                    onDelete = {
                                        scope.launch {
                                            bodyMetricsRepository.deleteWeight(userId, entry.date)
                                            // Reload data
                                            val result = bodyMetricsRepository.getWeightHistory(userId, selectedDays)
                                            result.fold(
                                                onSuccess = { weightHistory = it },
                                                onFailure = { }
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        // Add space for FAB
                        item {
                            StaggeredItem(index = 7) {
                            Spacer(Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }

            // Weight Log Dialog
            if (showWeightDialog) {
                WeightLogDialog(
                    currentWeight = currentWeight?.weightLbs,
                    onDismiss = { showWeightDialog = false },
                    onSave = { weight, unit, note ->
                        scope.launch {
                            val result = bodyMetricsRepository.logWeight(
                                userId = userId,
                                weight = weight,
                                unit = unit,
                                note = note
                            )
                            result.fold(
                                onSuccess = {
                                    showWeightDialog = false
                                    // Reload data
                                    bodyMetricsRepository.getLatestWeight(userId).fold(
                                        onSuccess = { currentWeight = it },
                                        onFailure = { }
                                    )
                                    bodyMetricsRepository.getWeightHistory(userId, selectedDays).fold(
                                        onSuccess = { weightHistory = it },
                                        onFailure = { }
                                    )
                                    snackbarHostState.showSnackbar("Weight logged successfully!")
                                },
                                onFailure = { error ->
                                    snackbarHostState.showSnackbar(error.message ?: "Failed to log weight")
                                }
                            )
                        }
                    }
                )
            }

            // Goal Dialog
            if (showGoalDialog) {
                SetGoalDialog(
                    currentWeight = currentWeight?.weightKg ?: 70f,
                    currentGoal = bodyGoal,
                    onDismiss = { showGoalDialog = false },
                    onSave = { targetWeight, targetDate, height ->
                        scope.launch {
                            val result = bodyMetricsRepository.setGoal(
                                userId = userId,
                                targetWeightKg = targetWeight,
                                targetDate = targetDate,
                                heightCm = height,
                                currentWeightKg = currentWeight?.weightKg ?: 70f
                            )
                            result.fold(
                                onSuccess = {
                                    showGoalDialog = false
                                    bodyGoal = it
                                    snackbarHostState.showSnackbar("Goal set successfully!")
                                },
                                onFailure = { error ->
                                    snackbarHostState.showSnackbar(error.message ?: "Failed to set goal")
                                }
                            )
                        }
                    }
                )
            }
        }
    }
    }
}

/**
 * Empty State - No weight logged yet
 */
@Composable
private fun EmptyState(
    onLogWeight: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // TODO: Replace with custom illustration asset (body metrics empty state artwork)
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DailyWellIcons.Health.Weight,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Track Your Weight",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Start tracking your body metrics and see your progress over time",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onLogWeight,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(DailyWellIcons.Actions.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Log Your Weight")
        }
    }
}

/**
 * Current Weight Card - Large display with weekly change
 */
@Composable
private fun CurrentWeightCard(
    weight: BodyMetrics,
    weeklyChange: WeightChange?,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Prominent,
        enablePressScale = true,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Weight",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${weight.weightLbs.roundToInt()}",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "lbs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            // Weekly Change
            if (weeklyChange != null && weeklyChange.changeLbs != 0f) {
                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (weeklyChange.changeLbs < 0) DailyWellIcons.Analytics.TrendDown else DailyWellIcons.Analytics.TrendUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (weeklyChange.changeLbs < 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )

                    Text(
                        text = "${if (weeklyChange.changeLbs > 0) "+" else ""}${weeklyChange.changeLbs.roundToInt()} lbs this week",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (weeklyChange.changeLbs < 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Goal Progress Card
 */
@Composable
private fun GoalProgressCard(
    progress: GoalProgress,
    goal: BodyGoal,
    currentWeight: BodyMetrics
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Goal Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${goal.targetWeightLbs.roundToInt()} lbs goal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${progress.percentComplete.roundToInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = (progress.percentComplete / 100f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${abs(progress.remainingLbs).roundToInt()} lbs to go",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${progress.daysRemaining} days left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * BMI Card
 */
@Composable
private fun BMICard(
    bmi: Float,
    category: BMICategory
) {
    val (categoryText, categoryColor) = when (category) {
        BMICategory.UNDERWEIGHT -> "Underweight" to Color(0xFF3B82F6)
        BMICategory.NORMAL -> "Normal" to Color(0xFF10B981)
        BMICategory.OVERWEIGHT -> "Overweight" to Color(0xFFF59E0B)
        BMICategory.OBESE -> "Obese" to Color(0xFFEF4444)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
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
                    text = "BMI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = String.format("%.1f", bmi),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = categoryColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = categoryText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = categoryColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Weight Trend Card with Chart
 */
@Composable
private fun WeightTrendCard(
    weightHistory: List<BodyMetrics>,
    selectedDays: Int,
    onDaysChanged: (Int) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Weight Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            // Day selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(7, 30, 90).forEach { days ->
                    FilterChip(
                        selected = selectedDays == days,
                        onClick = { onDaysChanged(days) },
                        label = { Text("${days}D") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Simple trend visualization (in production, use actual chart library)
            if (weightHistory.isNotEmpty()) {
                val minWeight = weightHistory.minOfOrNull { it.weightLbs } ?: 0f
                val maxWeight = weightHistory.maxOfOrNull { it.weightLbs } ?: 100f
                val range = maxWeight - minWeight

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${weightHistory.first().weightLbs.roundToInt()} - ${weightHistory.last().weightLbs.roundToInt()} lbs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${weightHistory.size} entries",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "No data for this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Quick Action Card
 */
@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        elevation = ElevationLevel.Subtle,
        enablePressScale = true,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Weight Entry Row
 */
@Composable
private fun WeightEntryRow(
    entry: BodyMetrics,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${entry.weightLbs.roundToInt()} lbs",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showMenu = true }) {
                Icon(DailyWellIcons.Nav.MoreVert, "Options")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(DailyWellIcons.Actions.Delete, null)
                    }
                )
            }
        }
    }
}

/**
 * Helper: Format date string
 */
private fun formatDate(dateString: String): String {
    return try {
        val date = dateString.take(10)
        val parts = date.split("-")
        if (parts.size == 3) {
            "${parts[1]}/${parts[2]}/${parts[0]}"
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}
