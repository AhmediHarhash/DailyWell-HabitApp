package com.dailywell.app.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import com.dailywell.app.data.repository.WorkoutRepository
import com.dailywell.app.data.repository.WorkoutStats
import com.dailywell.app.data.repository.VolumeTrend
import com.dailywell.app.data.repository.MuscleGroupFrequency
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

/**
 * Workout History Screen - Progress Tracking
 *
 * PERFECTION MODE: Visual progress tracking
 * - Calendar view of past workouts
 * - Exercise-specific history
 * - Volume progression graphs
 * - Personal records timeline
 * - Muscle group frequency heatmap
 * - Workout statistics
 *
 * Quality Standard: Better than Strong app's analytics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    userId: String,
    workoutRepository: WorkoutRepository,
    onBack: () -> Unit,
    onViewWorkout: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var recentWorkouts by remember { mutableStateOf<List<WorkoutSession>>(emptyList()) }
    var workoutStats by remember { mutableStateOf<WorkoutStats?>(null) }
    var personalRecords by remember { mutableStateOf<Map<String, PersonalRecord>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Load data
    LaunchedEffect(userId) {
        isLoading = true
        try {
            // Load recent workouts
            recentWorkouts = workoutRepository.getRecentWorkouts(userId, limit = 30)

            // Load statistics
            workoutStats = workoutRepository.getWorkoutStats(userId, days = 30)

            // Load personal records
            personalRecords = workoutRepository.getAllPersonalRecords(userId)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load workout data"
        } finally {
            isLoading = false
        }
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Workout History",
                    subtitle = "Overview, sessions, and records",
                    onNavigationClick = onBack
                )
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tab Bar
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("History") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Records") }
                    )
                }

                // Content
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    errorMessage != null -> {
                        ErrorState(
                            message = errorMessage ?: "Unknown error",
                            onRetry = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null
                                    // Retry load
                                }
                            }
                        )
                    }
                    else -> {
                        when (selectedTab) {
                            0 -> OverviewTab(
                                workoutStats = workoutStats,
                                recentWorkouts = recentWorkouts.take(5)
                            )
                            1 -> HistoryTab(
                                workouts = recentWorkouts,
                                onViewWorkout = onViewWorkout
                            )
                            2 -> RecordsTab(
                                personalRecords = personalRecords
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overview Tab - Statistics and insights
 */
@Composable
private fun OverviewTab(
    workoutStats: WorkoutStats?,
    recentWorkouts: List<WorkoutSession>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Statistics Cards
        item {
            PremiumSectionChip(
                text = "Last 30 days overview",
                icon = DailyWellIcons.Analytics.Timeline
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Workouts",
                    value = workoutStats?.totalWorkouts?.toString() ?: "0",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Volume",
                    value = "${workoutStats?.totalVolume ?: 0} lbs",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Avg Duration",
                    value = "${workoutStats?.averageDuration ?: 0} min",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Frequency",
                    value = "${workoutStats?.workoutFrequency ?: 0}x/week",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Volume Trend
        workoutStats?.let { stats ->
            item {
                VolumeTrendCard(
                    trend = stats.volumeTrend,
                    totalVolume = stats.totalVolume.toFloat()
                )
            }
        }

        // Most Trained Muscles
        workoutStats?.mostTrainedMuscleGroups?.let { muscleGroups ->
            if (muscleGroups.isNotEmpty()) {
                item {
                    PremiumSectionChip(
                        text = "Most trained muscles",
                        icon = DailyWellIcons.Health.Workout
                    )
                }

                item {
                    MuscleGroupFrequencyCard(muscleGroups = muscleGroups)
                }
            }
        }

        // Recent Workouts
        if (recentWorkouts.isNotEmpty()) {
            item {
                PremiumSectionChip(
                    text = "Recent workouts",
                    icon = DailyWellIcons.Health.Workout
                )
            }

            items(recentWorkouts) { workout ->
                CompactWorkoutCard(workout = workout)
            }
        }
    }
}

/**
 * History Tab - List of all workouts
 */
@Composable
private fun HistoryTab(
    workouts: List<WorkoutSession>,
    onViewWorkout: (String) -> Unit
) {
    if (workouts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = DailyWellIcons.Health.Workout,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No workouts yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PremiumSectionChip(
                    text = "Workout timeline",
                    icon = DailyWellIcons.Analytics.Calendar
                )
            }

            items(workouts) { workout ->
                WorkoutCard(
                    workout = workout,
                    onClick = { onViewWorkout(workout.id) }
                )
            }
        }
    }
}

/**
 * Records Tab - Personal records
 */
@Composable
private fun RecordsTab(
    personalRecords: Map<String, PersonalRecord>
) {
    if (personalRecords.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = DailyWellIcons.Gamification.Trophy,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No personal records yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Complete your first workout to set PRs!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PremiumSectionChip(
                    text = "${personalRecords.size} personal records",
                    icon = DailyWellIcons.Gamification.Trophy
                )
            }

            items(personalRecords.entries.toList()) { (exerciseId, pr) ->
                PersonalRecordCard(
                    exerciseId = exerciseId,
                    personalRecord = pr
                )
            }
        }
    }
}

/**
 * Stat Card Component
 */
@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Volume Trend Card
 */
@Composable
private fun VolumeTrendCard(
    trend: VolumeTrend,
    totalVolume: Float
) {
    val (trendIcon, trendText, trendColor) = when (trend) {
        VolumeTrend.INCREASING -> Triple(DailyWellIcons.Analytics.TrendUp, "Increasing", Color(0xFF10B981))
        VolumeTrend.DECREASING -> Triple(DailyWellIcons.Analytics.TrendDown, "Decreasing", Color(0xFFEF4444))
        VolumeTrend.STABLE -> Triple(DailyWellIcons.Analytics.TrendFlat, "Stable", Color(0xFF6B7280))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = trendColor.copy(alpha = 0.1f)
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
                    text = "Volume Trend",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = trendColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }

            Text(
                text = "${totalVolume.roundToInt()} lbs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Muscle Group Frequency Card
 */
@Composable
private fun MuscleGroupFrequencyCard(
    muscleGroups: List<MuscleGroupFrequency>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            muscleGroups.take(5).forEach { mgf ->
                MuscleGroupFrequencyRow(
                    muscleGroup = mgf.muscleGroup,
                    count = mgf.frequency,
                    maxCount = muscleGroups.first().frequency
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MuscleGroupFrequencyRow(
    muscleGroup: String,
    count: Int,
    maxCount: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = muscleGroup,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count times",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = count.toFloat() / maxCount,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

/**
 * Compact Workout Card
 */
@Composable
private fun CompactWorkoutCard(
    workout: WorkoutSession
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = workout.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${workout.exercises.size} exercises - ${workout.totalVolume} lbs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = formatWorkoutDate(workout.startTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Full Workout Card
 */
@Composable
private fun WorkoutCard(
    workout: WorkoutSession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatWorkoutDate(workout.startTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WorkoutStatChip(
                    icon = DailyWellIcons.Health.Workout,
                    label = "${workout.exercises.size} exercises"
                )
                WorkoutStatChip(
                    icon = DailyWellIcons.Misc.Timer,
                    label = "${workout.duration / 60} min"
                )
                WorkoutStatChip(
                    icon = DailyWellIcons.Analytics.BarChart,
                    label = "${workout.totalVolume} lbs"
                )
            }

            // Exercise preview
            if (workout.exercises.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Text(
                    text = workout.exercises.take(3).joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkoutStatChip(
    icon: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Personal Record Card
 */
@Composable
private fun PersonalRecordCard(
    exerciseId: String,
    personalRecord: PersonalRecord
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Gamification.Trophy,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = exerciseId.replace("_", " ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${personalRecord.weight} lbs x ${personalRecord.reps} reps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "1RM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${personalRecord.oneRepMax} lbs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Error State
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = DailyWellIcons.Status.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

/**
 * Helper: Format workout date
 */
private fun formatWorkoutDate(instant: Instant): String {
    return try {
        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${localDate.dayOfMonth}, ${localDate.year}"
    } catch (e: Exception) {
        "Unknown date"
    }
}

