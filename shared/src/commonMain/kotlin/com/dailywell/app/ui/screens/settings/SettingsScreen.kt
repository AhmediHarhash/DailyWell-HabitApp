package com.dailywell.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.UserSettings
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import com.dailywell.app.domain.model.HabitType
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToHabitStacking: (() -> Unit)? = null,
    onNavigateToIntentions: (() -> Unit)? = null,
    onNavigateToSmartReminders: (() -> Unit)? = null,
    onNavigateToAIInsights: (() -> Unit)? = null,
    onNavigateToSocial: (() -> Unit)? = null,
    onNavigateToAudioCoaching: (() -> Unit)? = null,
    onNavigateToBiometric: (() -> Unit)? = null,
    onNavigateToFamily: (() -> Unit)? = null,
    onNavigateToAICoaching: (() -> Unit)? = null,
    onNavigateToGamification: (() -> Unit)? = null,
    onNavigateToChallenges: (() -> Unit)? = null,
    onNavigateToLeaderboard: (() -> Unit)? = null,
    settingsRepository: SettingsRepository = koinInject(),
    habitRepository: HabitRepository = koinInject()
) {
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(UserSettings()) }
    var enabledHabits by remember { mutableStateOf<Set<String>>(emptySet()) }
    var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }
    var showCustomizeDialog by remember { mutableStateOf<HabitType?>(null) }
    var showAddCustomHabitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsRepository.getSettings().collect {
            settings = it
            enabledHabits = it.enabledHabitIds.toSet()
        }
    }

    LaunchedEffect(Unit) {
        habitRepository.getAllHabits().collect {
            habits = it
        }
    }

    // Customize habit dialog
    showCustomizeDialog?.let { habitType ->
        val habit = habits.find { it.id == habitType.id }
        val currentThreshold = settings.customThresholds[habitType.id] ?: habitType.defaultThreshold

        CustomizeHabitDialog(
            habitType = habitType,
            currentThreshold = currentThreshold,
            onDismiss = { showCustomizeDialog = null },
            onSave = { newThreshold ->
                scope.launch {
                    val updatedThresholds = settings.customThresholds + (habitType.id to newThreshold)
                    settingsRepository.updateSettings(settings.copy(customThresholds = updatedThresholds))
                }
                showCustomizeDialog = null
            }
        )
    }

    // Add custom habit dialog
    if (showAddCustomHabitDialog) {
        AddCustomHabitDialog(
            onDismiss = { showAddCustomHabitDialog = false },
            onSave = { name, emoji, threshold, question ->
                scope.launch {
                    habitRepository.createCustomHabit(name, emoji, threshold, question)
                }
                showAddCustomHabitDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // "My Habits" - personal ownership language vs mechanical "Settings"
                        text = "My Habits",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(text = "←", fontSize = 24.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            // Premium banner
            if (!settings.isPremium) {
                item {
                    PremiumBanner(onClick = onNavigateToPaywall)
                }
            }

            // Habits section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HABITS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    if (settings.isPremium) {
                        val customCount = habits.count { it.isCustom }
                        if (customCount < 3) {
                            TextButton(onClick = { showAddCustomHabitDialog = true }) {
                                Text("+ Add Custom")
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        HabitType.entries.forEachIndexed { index, habitType ->
                            val isEnabled = enabledHabits.contains(habitType.id)
                            val canEnable = settings.canAddMoreHabits() || isEnabled
                            val customThreshold = settings.customThresholds[habitType.id]

                            HabitSettingItem(
                                habit = habitType,
                                isEnabled = isEnabled,
                                canToggle = canEnable,
                                isPremium = settings.isPremium,
                                customThreshold = customThreshold,
                                onToggle = {
                                    scope.launch {
                                        if (isEnabled) {
                                            settingsRepository.disableHabit(habitType.id)
                                        } else if (canEnable) {
                                            settingsRepository.enableHabit(habitType.id)
                                        }
                                    }
                                },
                                onCustomize = if (settings.isPremium) {
                                    { showCustomizeDialog = habitType }
                                } else null
                            )
                            if (index < HabitType.entries.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }

                        // Custom habits
                        val customHabits = habits.filter { it.isCustom }
                        if (customHabits.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 2.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            customHabits.forEachIndexed { index, habit ->
                                CustomHabitSettingItem(
                                    habit = habit,
                                    onDelete = {
                                        scope.launch {
                                            habitRepository.deleteHabit(habit.id)
                                        }
                                    }
                                )
                                if (index < customHabits.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!settings.isPremium) {
                item {
                    Text(
                        text = "Free: ${enabledHabits.size}/3 habits • Upgrade for all 7 + custom habits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Premium Features section (Phase 2)
            if (settings.isPremium) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ADVANCED FEATURES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column {
                            // Habit Stacking
                            if (onNavigateToHabitStacking != null) {
                                FeatureRow(
                                    emoji = "🔗",
                                    title = "Habit Stacking",
                                    subtitle = "Chain habits together for 3.2x success",
                                    onClick = onNavigateToHabitStacking
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }

                            // Implementation Intentions
                            if (onNavigateToIntentions != null) {
                                FeatureRow(
                                    emoji = "🎯",
                                    title = "If-Then Plans",
                                    subtitle = "\"When X, I will Y\" planning",
                                    onClick = onNavigateToIntentions
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }

                            // Smart Reminders
                            if (onNavigateToSmartReminders != null) {
                                FeatureRow(
                                    emoji = "🧠",
                                    title = "Smart Reminders",
                                    subtitle = "AI-optimized timing for nudges",
                                    onClick = onNavigateToSmartReminders
                                )
                            }

                            // Phase 3 Features
                            // AI Insights
                            if (onNavigateToAIInsights != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "✨",
                                    title = "AI Insights",
                                    subtitle = "Pattern recognition & predictions",
                                    onClick = onNavigateToAIInsights
                                )
                            }

                            // Social Accountability
                            if (onNavigateToSocial != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "👥",
                                    title = "Accountability",
                                    subtitle = "Groups, partners & high-fives",
                                    onClick = onNavigateToSocial
                                )
                            }

                            // Audio Coaching
                            if (onNavigateToAudioCoaching != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "🎧",
                                    title = "Audio Coaching",
                                    subtitle = "2-3 min micro-lessons",
                                    onClick = onNavigateToAudioCoaching
                                )
                            }

                            // Phase 4 Features
                            // Biometric Dashboard
                            if (onNavigateToBiometric != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "💓",
                                    title = "Biometrics",
                                    subtitle = "Sleep, HRV & recovery insights",
                                    onClick = onNavigateToBiometric
                                )
                            }

                            // Family Plan
                            if (onNavigateToFamily != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "👨‍👩‍👧‍👦",
                                    title = "Family Plan",
                                    subtitle = "Up to 6 members & challenges",
                                    onClick = onNavigateToFamily
                                )
                            }

                            // AI Coaching
                            if (onNavigateToAICoaching != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "🤖",
                                    title = "AI Coach",
                                    subtitle = "Personalized coaching sessions",
                                    onClick = onNavigateToAICoaching
                                )
                            }

                            // Phase 5 - Gamification
                            if (onNavigateToGamification != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "🏆",
                                    title = "Rewards & Progress",
                                    subtitle = "XP, badges, daily rewards & more",
                                    onClick = onNavigateToGamification
                                )
                            }

                            // Phase 5B - Challenges
                            if (onNavigateToChallenges != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "🎯",
                                    title = "Challenges & Duels",
                                    subtitle = "Solo challenges, friend duels & community events",
                                    onClick = onNavigateToChallenges
                                )
                            }

                            // Phase 5C/D - Social & Leaderboards
                            if (onNavigateToLeaderboard != null) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                FeatureRow(
                                    emoji = "🏅",
                                    title = "Social & Rankings",
                                    subtitle = "Leaderboards, activity feed, cheers & referrals",
                                    onClick = onNavigateToLeaderboard
                                )
                            }
                        }
                    }
                }
            }

            // Reminders section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "REMINDERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        // Reminder enabled toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Reminder",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Get reminded to check in",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.reminderEnabled,
                                onCheckedChange = {
                                    scope.launch {
                                        settingsRepository.setReminderEnabled(it)
                                    }
                                }
                            )
                        }

                        if (settings.reminderEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            // Reminder time
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reminder Time",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val hour = settings.reminderHour
                                val period = if (hour < 12) "AM" else "PM"
                                val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                                Text(
                                    text = "$displayHour:${String.format("%02d", settings.reminderMinute)} $period",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // About section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ABOUT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        SettingsRow(
                            title = "Version",
                            subtitle = "1.0.0"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        SettingsRow(
                            title = "Philosophy",
                            subtitle = "Consistency > Perfection"
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PremiumBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Success.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "⭐", fontSize = 32.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
                Text(
                    text = "All 7 habits • Full history • Insights",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "→",
                fontSize = 24.sp,
                color = Success
            )
        }
    }
}

@Composable
private fun HabitSettingItem(
    habit: HabitType,
    isEnabled: Boolean,
    canToggle: Boolean,
    isPremium: Boolean,
    customThreshold: String?,
    onToggle: () -> Unit,
    onCustomize: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canToggle) { onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = habit.emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = if (canToggle) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = customThreshold ?: habit.defaultThreshold,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (customThreshold != null) Success else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onCustomize != null && isEnabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onCustomize() }
                    )
                }
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = { if (canToggle) onToggle() },
            enabled = canToggle
        )
    }
}

@Composable
private fun CustomHabitSettingItem(
    habit: Habit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = habit.emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = habit.threshold,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Text(text = "🗑️", fontSize = 18.sp)
        }
    }
}

@Composable
private fun CustomizeHabitDialog(
    habitType: HabitType,
    currentThreshold: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var threshold by remember { mutableStateOf(currentThreshold) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = habitType.emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Customize ${habitType.displayName}")
            }
        },
        text = {
            Column {
                Text(
                    text = "What counts as \"done\" for you?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Your goal") },
                    placeholder = { Text(habitType.defaultThreshold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Default: ${habitType.defaultThreshold}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(threshold) },
                enabled = threshold.isNotBlank()
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

@Composable
private fun AddCustomHabitDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, threshold: String, question: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("✨") }
    var threshold by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 2) emoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("e.g., Read") }
                    )
                }
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Goal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., 20+ minutes") }
                )
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Check-in question") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Did you read today?") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        emoji.ifBlank { "✨" },
                        threshold.ifBlank { "Daily" },
                        question.ifBlank { "Did you complete $name?" }
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureRow(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "→",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
