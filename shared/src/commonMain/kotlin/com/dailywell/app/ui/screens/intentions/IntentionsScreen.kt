package com.dailywell.app.ui.screens.intentions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.*
import com.dailywell.app.domain.model.HabitType
import com.dailywell.app.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

/**
 * Implementation Intentions Screen
 * Helps users create specific "if-then" plans that double habit success rates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntentionsScreen(
    onBack: () -> Unit,
    viewModel: IntentionsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedHabitIdForAdd by remember { mutableStateOf<String?>(null) }

    if (showAddDialog && selectedHabitIdForAdd != null) {
        val habitType = HabitType.entries.find { it.id == selectedHabitIdForAdd }
        if (habitType != null) {
            AddIntentionDialog(
                habitId = habitType.id,
                habitName = habitType.displayName,
                onDismiss = {
                    showAddDialog = false
                    selectedHabitIdForAdd = null
                },
                onSave = { intention ->
                    viewModel.addIntention(intention)
                    showAddDialog = false
                    selectedHabitIdForAdd = null
                }
            )
        }
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "If-Then Plans",
                    subtitle = "Plan cues and actions",
                    onNavigationClick = onBack
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
                // Explanation card
                item {
                    StaggeredItem(index = 0) {
                        IntentionsExplanationCard()
                    }
                }

                // Existing intentions grouped by habit
                val intentionsByHabit = uiState.intentions.groupBy { it.habitId }

                if (intentionsByHabit.isEmpty()) {
                    item {
                        StaggeredItem(index = 1) {
                            EmptyIntentionsCard()
                        }
                    }
                }

                if (intentionsByHabit.isNotEmpty()) {
                    item {
                        StaggeredItem(index = 1) {
                            PremiumSectionChip(
                                text = "Your plans",
                                icon = DailyWellIcons.Habits.Intentions
                            )
                        }
                    }

                    intentionsByHabit.forEach { (habitId, intentions) ->
                        val habitType = HabitType.entries.find { it.id == habitId }
                        if (habitType != null) {
                            item {
                                HabitIntentionsCard(
                                    habitId = habitType.id,
                                    habitName = habitType.displayName,
                                    intentions = intentions,
                                    onToggle = { viewModel.toggleIntention(it) },
                                    onDelete = { viewModel.deleteIntention(it) },
                                    onAddMore = {
                                        selectedHabitIdForAdd = habitType.id
                                        showAddDialog = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Quick add section - habits without intentions
                val habitsWithoutIntentions = HabitType.entries.filter { habitType ->
                    !intentionsByHabit.containsKey(habitType.id)
                }

                if (habitsWithoutIntentions.isNotEmpty()) {
                    item {
                        StaggeredItem(index = 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                            PremiumSectionChip(
                                text = "Add plans for",
                                icon = DailyWellIcons.Actions.Add
                            )
                        }
                    }

                    items(habitsWithoutIntentions) { habitType ->
                        HabitAddIntentionCard(
                            habitId = habitType.id,
                            habitName = habitType.displayName,
                            templates = IntentionTemplates.getTemplatesForHabit(habitType.id),
                            onAddCustom = {
                                selectedHabitIdForAdd = habitType.id
                                showAddDialog = true
                            },
                            onApplyTemplate = { template ->
                                viewModel.applyTemplate(template)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyIntentionsCard() {
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
                imageVector = DailyWellIcons.Habits.Intentions,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "No if-then plans yet. Add one below to make your habits easier to start.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IntentionsExplanationCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Prominent
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Habits.Intentions,
                    contentDescription = "Intentions",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Make Habits Automatic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "2x more likely to follow through",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "\"When [situation], I will [action]\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Research shows that specific plans for when and where you'll do a habit " +
                        "doubles your chances of following through.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HabitIntentionsCard(
    habitId: String,
    habitName: String,
    intentions: List<ImplementationIntention>,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAddMore: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Habit header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.getHabitIcon(habitId),
                        contentDescription = habitName,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = habitName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onAddMore) {
                    Text("+ Add")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Intentions list
            intentions.forEach { intention ->
                IntentionItem(
                    intention = intention,
                    onToggle = { onToggle(intention.id) },
                    onDelete = { onDelete(intention.id) }
                )
                if (intention != intentions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun IntentionItem(
    intention: ImplementationIntention,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (intention.isEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getIntentionSituationIcon(intention.situation),
                    contentDescription = intention.situation.description,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Switch(
                    checked = intention.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main intention statement
            Text(
                text = intention.getIntentionStatement(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (intention.isEnabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Obstacle response if exists
            intention.getObstacleStatement()?.let { obstacleStatement ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = obstacleStatement,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Stats and delete
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (intention.completionCount > 0) {
                    Text(
                        text = "Triggered ${intention.completionCount} times",
                        style = MaterialTheme.typography.labelSmall,
                        color = Success
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                Text(
                    text = "Remove",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}

@Composable
private fun HabitAddIntentionCard(
    habitId: String,
    habitName: String,
    templates: List<IntentionTemplates.IntentionTemplate>,
    onAddCustom: () -> Unit,
    onApplyTemplate: (IntentionTemplates.IntentionTemplate) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        enablePressScale = true
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.getHabitIcon(habitId),
                    contentDescription = habitName,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = habitName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "No plans yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (templates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Suggested plans:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                templates.take(2).forEach { template ->
                    SuggestedTemplateChip(
                        template = template,
                        onApply = { onApplyTemplate(template) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAddCustom,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ Create Custom Plan")
            }
        }
    }
}

@Composable
private fun SuggestedTemplateChip(
    template: IntentionTemplates.IntentionTemplate,
    onApply: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onApply() },
        colors = CardDefaults.cardColors(
            containerColor = Success.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
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
                    text = "When ${template.situation.description}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = template.suggestedAction,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "+ Add",
                style = MaterialTheme.typography.labelMedium,
                color = Success,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIntentionDialog(
    habitId: String,
    habitName: String,
    onDismiss: () -> Unit,
    onSave: (ImplementationIntention) -> Unit
) {
    var selectedSituation by remember { mutableStateOf<IntentionSituation?>(null) }
    var action by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var obstacle by remember { mutableStateOf("") }
    var obstacleResponse by remember { mutableStateOf("") }
    var showObstacleSection by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.getHabitIcon(habitId),
                    contentDescription = habitName,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Plan for $habitName")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Situation selector
                Text(
                    text = "When...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // Common situations as chips
                Column {
                    IntentionSituation.entries.filter { it != IntentionSituation.CUSTOM }
                        .chunked(2).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { situation ->
                                    FilterChip(
                                        selected = selectedSituation == situation,
                                        onClick = { selectedSituation = situation },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = getIntentionSituationIcon(situation),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = situation.description,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                }

                // Action input
                Text(
                    text = "I will...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    placeholder = { Text("e.g., drink a glass of water") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Optional: Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Where? (optional)") },
                    placeholder = { Text("e.g., kitchen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Optional: Time
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("What time? (optional)") },
                    placeholder = { Text("e.g., 7:00 AM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Obstacle planning section
                TextButton(
                    onClick = { showObstacleSection = !showObstacleSection }
                ) {
                    Text(
                        text = if (showObstacleSection) "- Hide obstacle planning"
                        else "+ Add obstacle planning (recommended)"
                    )
                }

                if (showObstacleSection) {
                    Text(
                        text = "If this obstacle happens...",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = obstacle,
                        onValueChange = { obstacle = it },
                        placeholder = { Text("e.g., I feel too tired") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "I will...",
                        style = MaterialTheme.typography.labelMedium
                    )
                    OutlinedTextField(
                        value = obstacleResponse,
                        onValueChange = { obstacleResponse = it },
                        placeholder = { Text("e.g., do just 2 minutes instead") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedSituation != null && action.isNotBlank()) {
                        val intention = ImplementationIntention(
                            id = "intention_${System.currentTimeMillis()}",
                            habitId = habitId,
                            situation = selectedSituation!!,
                            action = action.trim(),
                            location = location.takeIf { it.isNotBlank() },
                            time = time.takeIf { it.isNotBlank() },
                            obstacle = obstacle.takeIf { it.isNotBlank() },
                            obstacleResponse = obstacleResponse.takeIf { it.isNotBlank() },
                            isEnabled = true,
                            createdAt = kotlinx.datetime.Clock.System.now().toString()
                        )
                        onSave(intention)
                    }
                },
                enabled = selectedSituation != null && action.isNotBlank()
            ) {
                Text("Save Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Maps IntentionSituation enum values to appropriate Material Icons
 */
private fun getIntentionSituationIcon(situation: IntentionSituation): androidx.compose.ui.graphics.vector.ImageVector {
    return when (situation) {
        IntentionSituation.WAKE_UP -> DailyWellIcons.Misc.Sunrise
        IntentionSituation.AFTER_BREAKFAST -> DailyWellIcons.Health.Nutrition
        IntentionSituation.LUNCH_BREAK -> DailyWellIcons.Habits.Nourish
        IntentionSituation.AFTER_WORK -> DailyWellIcons.Misc.Time
        IntentionSituation.BEFORE_DINNER -> DailyWellIcons.Health.Nutrition
        IntentionSituation.BEFORE_BED -> DailyWellIcons.Habits.Sleep
        IntentionSituation.FEELING_STRESSED -> DailyWellIcons.Mood.Struggling
        IntentionSituation.FEELING_TIRED -> DailyWellIcons.Mood.Low
        IntentionSituation.FEELING_BORED -> DailyWellIcons.Mood.Okay
        IntentionSituation.FEELING_ANXIOUS -> DailyWellIcons.Mood.Struggling
        IntentionSituation.FEELING_ENERGETIC -> DailyWellIcons.Gamification.XP
        IntentionSituation.ARRIVE_HOME -> DailyWellIcons.Misc.Location
        IntentionSituation.ARRIVE_WORK -> DailyWellIcons.Misc.Location
        IntentionSituation.IN_KITCHEN -> DailyWellIcons.Health.Nutrition
        IntentionSituation.AT_GYM -> DailyWellIcons.Health.Workout
        IntentionSituation.PHONE_RINGS -> DailyWellIcons.Misc.Phone
        IntentionSituation.MEETING_ENDS -> DailyWellIcons.Analytics.Calendar
        IntentionSituation.COFFEE_READY -> DailyWellIcons.Misc.Time
        IntentionSituation.CUSTOM -> DailyWellIcons.Actions.Edit
    }
}
