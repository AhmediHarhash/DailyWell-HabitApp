package com.dailywell.app.ui.screens.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.DailyWellIcons
import com.dailywell.app.ui.components.GlassScreenWrapper
import com.dailywell.app.ui.components.PremiumSectionChip
import com.dailywell.app.ui.components.PremiumTopBar
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProactiveNotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProactiveNotificationSettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Smart Notifications",
                    subtitle = "Proactive coach nudges",
                    onNavigationClick = onNavigateBack
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Master Toggle
            item {
                MasterToggleCard(
                    enabled = uiState.preferences.enabled,
                    onToggle = viewModel::toggleProactiveNotifications
                )
            }

            // Stats Card (if enabled)
            if (uiState.preferences.enabled && uiState.stats.totalSent > 0) {
                item {
                    StatsCard(stats = uiState.stats)
                }
            }

            // Notification Types
                if (uiState.preferences.enabled) {
                    item {
                        SectionHeaderChip(
                            title = "Notification types",
                            subtitle = "Choose which notifications you want to receive",
                            icon = DailyWellIcons.Status.Notification
                        )
                    }

                items(ProactiveNotificationType.entries) { type ->
                    NotificationTypeToggle(
                        type = type,
                        enabled = uiState.preferences.enabledTypes[type] ?: type.defaultEnabled,
                        onToggle = { viewModel.toggleNotificationType(type, it) }
                    )
                }

                    // Tone Selection
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeaderChip(
                            title = "Notification tone",
                            subtitle = "How should your coach talk to you?",
                            icon = DailyWellIcons.Coaching.Chat
                        )
                    }

                item {
                    ToneSelector(
                        selectedTone = uiState.preferences.tone,
                        onToneSelected = viewModel::updateTone
                    )
                }

                    // Timing Settings
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeaderChip(
                            title = "Timing",
                            subtitle = "When should we reach out?",
                            icon = DailyWellIcons.Misc.Time
                        )
                    }

                item {
                    TimingSettingsCard(
                        preferences = uiState.preferences,
                        onMorningWindowChange = viewModel::updateMorningWindow,
                        onEveningWindowChange = viewModel::updateEveningWindow,
                        onQuietHoursChange = viewModel::updateQuietHours,
                        onSmartTimingToggle = viewModel::toggleSmartTiming
                    )
                }

                    // Frequency Settings
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionHeaderChip(
                            title = "Frequency",
                            subtitle = "How often should we check in?",
                            icon = DailyWellIcons.Analytics.Pattern
                        )
                    }

                item {
                    FrequencySettingsCard(
                        maxPerDay = uiState.preferences.maxNotificationsPerDay,
                        onMaxChange = viewModel::updateMaxNotificationsPerDay
                    )
                }
                }

                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun MasterToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (enabled) DailyWellIcons.Status.Notification else DailyWellIcons.Status.NotificationOff,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Coach Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (enabled)
                        "Your coach will proactively reach out to you"
                    else
                        "Enable to receive personalized nudges",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun StatsCard(stats: NotificationStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Last 7 Days",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.totalSent.toString(),
                    label = "Sent"
                )
                StatItem(
                    value = stats.totalOpened.toString(),
                    label = "Opened"
                )
                StatItem(
                    value = "${(stats.openRate * 100).toInt()}%",
                    label = "Open Rate"
                )
            }

            if (stats.mostEngagingType != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Most engaging:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = getNotificationTypeIcon(stats.mostEngagingType),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stats.mostEngagingType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionHeaderChip(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column {
        PremiumSectionChip(
            text = title,
            icon = icon
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotificationTypeToggle(
    type: ProactiveNotificationType,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle(!enabled) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getNotificationTypeIcon(type),
            contentDescription = type.displayName,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun ToneSelector(
    selectedTone: NotificationTone,
    onToneSelected: (NotificationTone) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NotificationTone.entries.forEach { tone ->
            val isSelected = tone == selectedTone

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToneSelected(tone) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = if (isSelected) {
                    CardDefaults.outlinedCardBorder()
                } else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onToneSelected(tone) }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tone.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text = tone.description,
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
private fun TimingSettingsCard(
    preferences: ProactiveNotificationPreferences,
    onMorningWindowChange: (Int, Int) -> Unit,
    onEveningWindowChange: (Int, Int) -> Unit,
    onQuietHoursChange: (Int, Int) -> Unit,
    onSmartTimingToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Smart Timing Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = DailyWellIcons.Misc.Sparkle,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Smart Timing",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Learn optimal times from your behavior",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = preferences.useSmartTiming,
                    onCheckedChange = onSmartTimingToggle
                )
            }

            HorizontalDivider()

            // Morning Window
            TimeWindowRow(
                icon = DailyWellIcons.Misc.Sunrise,
                label = "Morning",
                startHour = preferences.morningWindowStart,
                endHour = preferences.morningWindowEnd,
                onTimeChange = onMorningWindowChange
            )

            // Evening Window
            TimeWindowRow(
                icon = DailyWellIcons.Misc.Night,
                label = "Evening",
                startHour = preferences.eveningWindowStart,
                endHour = preferences.eveningWindowEnd,
                onTimeChange = onEveningWindowChange
            )

            HorizontalDivider()

            // Quiet Hours
            TimeWindowRow(
                icon = DailyWellIcons.Status.NotificationOff,
                label = "Do Not Disturb",
                startHour = preferences.dndStart,
                endHour = preferences.dndEnd,
                onTimeChange = onQuietHoursChange,
                isQuietHours = true
            )
        }
    }
}

@Composable
private fun TimeWindowRow(
    icon: ImageVector,
    label: String,
    startHour: Int,
    endHour: Int,
    onTimeChange: (Int, Int) -> Unit,
    isQuietHours: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${formatHour(startHour)} - ${formatHour(endHour)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0 -> "12 AM"
        hour < 12 -> "$hour AM"
        hour == 12 -> "12 PM"
        else -> "${hour - 12} PM"
    }
}

@Composable
private fun FrequencySettingsCard(
    maxPerDay: Int,
    onMaxChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Maximum notifications per day",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 3, 5, 7, 10).forEach { count ->
                    val isSelected = count == maxPerDay

                    FilterChip(
                        selected = isSelected,
                        onClick = { onMaxChange(count) },
                        label = { Text(count.toString()) }
                    )
                }
            }

            Text(
                text = when (maxPerDay) {
                    2 -> "Minimal - only essential alerts"
                    3 -> "Light - morning, evening, and important"
                    5 -> "Balanced - regular check-ins"
                    7 -> "Active - frequent engagement"
                    else -> "High - maximum coaching support"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Maps ProactiveNotificationType to the appropriate Material Icon
 */
private fun getNotificationTypeIcon(type: ProactiveNotificationType): ImageVector = when (type) {
    ProactiveNotificationType.MORNING_MOTIVATION -> DailyWellIcons.Misc.Sunrise
    ProactiveNotificationType.MIDDAY_CHECKIN -> DailyWellIcons.Misc.Brightness
    ProactiveNotificationType.EVENING_REMINDER -> DailyWellIcons.Misc.Night
    ProactiveNotificationType.STREAK_AT_RISK -> DailyWellIcons.Analytics.AtRisk
    ProactiveNotificationType.COMEBACK_NUDGE -> DailyWellIcons.Health.Workout
    ProactiveNotificationType.MILESTONE_APPROACHING -> DailyWellIcons.Habits.Intentions
    ProactiveNotificationType.ACHIEVEMENT_UNLOCKED -> DailyWellIcons.Gamification.Trophy
    ProactiveNotificationType.AI_INSIGHT -> DailyWellIcons.Coaching.AICoach
    ProactiveNotificationType.HABIT_SPECIFIC -> DailyWellIcons.Onboarding.SelectHabits
    ProactiveNotificationType.SOCIAL_ACTIVITY -> DailyWellIcons.Social.HighFive
    ProactiveNotificationType.WEEKLY_SUMMARY -> DailyWellIcons.Analytics.BarChart
    ProactiveNotificationType.COACH_OUTREACH -> DailyWellIcons.Coaching.Chat
}
