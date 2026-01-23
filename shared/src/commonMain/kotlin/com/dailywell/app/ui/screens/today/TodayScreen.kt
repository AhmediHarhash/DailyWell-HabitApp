package com.dailywell.app.ui.screens.today

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.MoodLevel
import com.dailywell.app.ui.components.*
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onSettingsClick: () -> Unit,
    onUpgradeClick: () -> Unit = {},
    viewModel: TodayViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show celebration dialog
    uiState.celebrationMessage?.let { message ->
        CelebrationDialog(
            message = message,
            completedCount = uiState.completedCount,
            totalCount = uiState.totalCount,
            onDismiss = { viewModel.dismissCelebration() }
        )
    }

    // Show milestone dialog
    if (uiState.showMilestoneDialog && uiState.currentMilestone != null) {
        StreakMilestoneDialog(
            milestone = uiState.currentMilestone!!,
            streak = uiState.streakInfo.currentStreak,
            onDismiss = { viewModel.dismissMilestone() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "DailyWell",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    CompactStreakBadge(streak = uiState.streakInfo.currentStreak)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onSettingsClick) {
                        Text(text = "⚙️", fontSize = 20.sp)
                    }
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
        } else if (uiState.habits.isEmpty()) {
            EmptyHabitsView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trial banner - show when on trial with urgency
                if (uiState.isOnTrial && uiState.trialDaysRemaining <= 7) {
                    item {
                        TrialBanner(
                            daysRemaining = uiState.trialDaysRemaining,
                            onUpgradeClick = onUpgradeClick
                        )
                    }
                }

                // Date header
                item {
                    DateHeader(date = uiState.todayDate)
                }

                // Mood check card - FBI Psychology "Labeling Emotions"
                item {
                    MoodCheckCard(
                        hasCheckedMood = uiState.hasCheckedMood,
                        currentMood = uiState.currentMood,
                        onMoodSelected = { mood -> viewModel.selectMood(mood) },
                        onDismiss = { viewModel.dismissMoodCard() }
                    )
                }

                // Mood indicator if already checked
                if (uiState.hasCheckedMood && uiState.currentMood != null) {
                    item {
                        MoodIndicator(mood = uiState.currentMood!!)
                    }
                }

                // Social proof banner - show during trial
                uiState.socialProofMessage?.let { message ->
                    item {
                        SocialProofBanner(message = message)
                    }
                }

                // Habits list
                items(
                    items = uiState.habits,
                    key = { it.id }
                ) { habit ->
                    HabitCheckItem(
                        habit = habit,
                        isCompleted = uiState.completions[habit.id] == true,
                        onToggle = { completed ->
                            viewModel.toggleHabit(habit.id, completed)
                        }
                    )
                }

                // Week calendar section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    WeekSection(weekData = uiState.weekData)
                }

                // Streak section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    StreakSection(streakInfo = uiState.streakInfo)
                }

                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DateHeader(date: String) {
    val displayDate = try {
        val localDate = LocalDate.parse(date)
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        "${months[localDate.monthNumber - 1]} ${localDate.dayOfMonth}"
    } catch (e: Exception) {
        date
    }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = displayDate,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun WeekSection(weekData: com.dailywell.app.data.model.WeekData?) {
    if (weekData == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "THIS WEEK",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            WeekCalendar(weekData = weekData)
            Spacer(modifier = Modifier.height(8.dp))
            WeekSummaryText(
                weekData = weekData,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StreakSection(streakInfo: com.dailywell.app.data.model.StreakInfo) {
    if (streakInfo.currentStreak > 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StreakBadge(streakInfo = streakInfo)
            }
        }
    }
}

@Composable
private fun EmptyHabitsView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌱",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No habits yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Go to Settings to enable habits",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
