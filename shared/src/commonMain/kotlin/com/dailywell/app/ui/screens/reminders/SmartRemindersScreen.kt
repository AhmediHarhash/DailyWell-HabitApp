package com.dailywell.app.ui.screens.reminders

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRemindersScreen(
    onBack: () -> Unit,
    viewModel: SmartRemindersViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Smart Reminders",
                    subtitle = "Adaptive reminder timing",
                    onNavigationClick = onBack
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Explanation Card
                item {
                    StaggeredItem(index = 0) {
                        SmartRemindersExplanationCard()
                    }
                }

                // Global Toggle
                item {
                    StaggeredItem(index = 1) {
                        GlobalReminderToggle(
                            isEnabled = uiState.isGlobalEnabled,
                            onToggle = { viewModel.toggleGlobalReminders() }
                        )
                    }
                }

                // Habit-specific reminder settings
                item {
                    StaggeredItem(index = 2) {
                        PremiumSectionChip(
                            text = "Habit reminders",
                            icon = DailyWellIcons.Habits.SmartReminders,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                if (uiState.habitSettings.isEmpty()) {
                    item {
                        EmptyReminderSettingsCard()
                    }
                } else {
                    items(uiState.habitSettings) { settings ->
                        HabitReminderCard(
                            habitId = settings.habitId,
                            settings = settings,
                            onToggle = { viewModel.toggleHabitReminder(settings.habitId) },
                            onTimeChange = { time -> viewModel.setPreferredTime(settings.habitId, time) },
                            onToneChange = { tone -> viewModel.setReminderTone(settings.habitId, tone) },
                            onFrequencyChange = { freq -> viewModel.setReminderFrequency(settings.habitId, freq) }
                        )
                    }
                }

                // Science explanation
                item {
                    StaggeredItem(index = 3) {
                        SmartTimingExplanation()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReminderSettingsCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = DailyWellIcons.Habits.SmartReminders,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "No habit reminders yet. Add habits first, then tune reminder timing here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmartRemindersExplanationCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Prominent
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    DailyWellIcons.Habits.SmartReminders,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Adaptive Intelligence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Smart reminders learn when you're most likely to act. " +
                        "They adapt to your patterns over time, sending nudges when you're " +
                        "naturally receptive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Analytics.BarChart,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reminders timed to activity windows work 3x better",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GlobalReminderToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    DailyWellIcons.Habits.SmartReminders,
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Enable Reminders",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isEnabled) "Active" else "Paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun HabitReminderCard(
    habitId: String,
    settings: HabitReminderSettings,
    onToggle: () -> Unit,
    onTimeChange: (String?) -> Unit,
    onToneChange: (ReminderTone) -> Unit,
    onFrequencyChange: (ReminderFrequency) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTonePicker by remember { mutableStateOf(false) }
    var showFrequencyPicker by remember { mutableStateOf(false) }

    val habitInfo = getHabitInfo(habitId)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        enablePressScale = true,
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.getHabitIcon(habitId),
                        contentDescription = habitInfo.second,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = habitInfo.second,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            AnimatedVisibility(
                visible = settings.isEnabled && expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Preferred Time
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                DailyWellIcons.Habits.SmartReminders,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Preferred Time", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = settings.preferredTime ?: "Smart timing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Tone
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTonePicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reminder Tone", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = getReminderToneIcon(settings.tone),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = settings.tone.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Frequency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showFrequencyPicker = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Frequency", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = settings.frequency.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Smart timing indicator
                    if (settings.smartTimingEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                DailyWellIcons.Habits.SmartReminders,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Learning your patterns...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        TimePickerDialog(
            currentTime = settings.preferredTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                onTimeChange(time)
                showTimePicker = false
            }
        )
    }

    // Tone picker dialog
    if (showTonePicker) {
        TonePickerDialog(
            currentTone = settings.tone,
            onDismiss = { showTonePicker = false },
            onSelect = { tone ->
                onToneChange(tone)
                showTonePicker = false
            }
        )
    }

    // Frequency picker dialog
    if (showFrequencyPicker) {
        FrequencyPickerDialog(
            currentFrequency = settings.frequency,
            onDismiss = { showFrequencyPicker = false },
            onSelect = { freq ->
                onFrequencyChange(freq)
                showFrequencyPicker = false
            }
        )
    }
}

@Composable
private fun SmartTimingExplanation() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Coaching.AICoach,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "How Smart Timing Works",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your reminders learn from your behavior:\n" +
                        "- When you typically complete each habit\n" +
                        "- What times you're most active\n" +
                        "- How quickly you respond to nudges\n\n" +
                        "Over time, reminders arrive at your optimal moments.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TimePickerDialog(
    currentTime: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    val times = listOf(
        null to "Smart timing (recommended)",
        "06:00" to "6:00 AM",
        "07:00" to "7:00 AM",
        "08:00" to "8:00 AM",
        "09:00" to "9:00 AM",
        "10:00" to "10:00 AM",
        "12:00" to "12:00 PM",
        "14:00" to "2:00 PM",
        "17:00" to "5:00 PM",
        "18:00" to "6:00 PM",
        "19:00" to "7:00 PM",
        "20:00" to "8:00 PM",
        "21:00" to "9:00 PM"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            LazyColumn {
                items(times) { (time, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConfirm(time) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = time == currentTime,
                            onClick = { onConfirm(time) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TonePickerDialog(
    currentTone: ReminderTone,
    onDismiss: () -> Unit,
    onSelect: (ReminderTone) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder Tone") },
        text = {
            Column {
                ReminderTone.entries.forEach { tone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tone) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = tone == currentTone,
                            onClick = { onSelect(tone) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = getReminderToneIcon(tone),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tone.description)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FrequencyPickerDialog(
    currentFrequency: ReminderFrequency,
    onDismiss: () -> Unit,
    onSelect: (ReminderFrequency) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder Frequency") },
        text = {
            Column {
                ReminderFrequency.entries.forEach { freq ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(freq) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = freq == currentFrequency,
                            onClick = { onSelect(freq) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(freq.description)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getHabitInfo(habitId: String): Pair<String, String> {
    return when (habitId) {
        "sleep" -> habitId to "Rest"
        "water" -> habitId to "Hydrate"
        "move" -> habitId to "Move"
        "vegetables" -> habitId to "Nourish"
        "calm" -> habitId to "Calm"
        "connect" -> habitId to "Connect"
        "unplug" -> habitId to "Unplug"
        else -> habitId to habitId.replaceFirstChar { it.uppercase() }
    }
}

private fun getReminderToneIcon(tone: ReminderTone): androidx.compose.ui.graphics.vector.ImageVector {
    return when (tone) {
        ReminderTone.GENTLE -> DailyWellIcons.Onboarding.Welcome
        ReminderTone.ENCOURAGING -> DailyWellIcons.Health.Workout
        ReminderTone.PLAYFUL -> DailyWellIcons.Habits.Intentions
        ReminderTone.DIRECT -> DailyWellIcons.Social.Contract
        ReminderTone.SCIENCE -> DailyWellIcons.Coaching.AICoach
    }
}
