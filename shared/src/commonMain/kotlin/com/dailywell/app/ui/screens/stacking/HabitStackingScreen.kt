package com.dailywell.app.ui.screens.stacking

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitStackingScreen(
    onBack: () -> Unit,
    viewModel: HabitStackingViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Habit Stacking",
                    subtitle = "Build automatic chains",
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
                        StackingExplanationCard()
                    }
                }

                if (uiState.activeStacks.isEmpty()) {
                    item {
                        StaggeredItem(index = 1) {
                            EmptyStacksCard()
                        }
                    }
                }

                // Current stacks
                if (uiState.activeStacks.isNotEmpty()) {
                    item {
                        StaggeredItem(index = 1) {
                            PremiumSectionChip(
                                text = "Your habit chains",
                                icon = DailyWellIcons.Habits.HabitStacking
                            )
                        }
                    }

                    items(uiState.activeStacks) { stack ->
                        ActiveStackCard(
                            stack = stack,
                            triggerHabit = uiState.habits.find { it.id == stack.triggerHabitId },
                            targetHabit = uiState.habits.find { it.id == stack.targetHabitId },
                            onToggle = { viewModel.toggleStack(stack.id) },
                            onDelete = { viewModel.deleteStack(stack.id) }
                        )
                    }
                }

                // Templates section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    PremiumSectionChip(
                        text = "Quick start templates",
                        icon = DailyWellIcons.Actions.CheckCircle
                    )
                }

                item {
                    StaggeredItem(index = 2) {
                        RoutineTemplateCard(
                            title = "Morning Routine",
                            icon = DailyWellIcons.Misc.Sunrise,
                            description = "Start your day with energy",
                            templates = HabitStackTemplates.morningRoutineStacks,
                            habits = uiState.habits,
                            onApply = { viewModel.applyMorningRoutine() }
                        )
                    }
                }

                item {
                    StaggeredItem(index = 3) {
                        RoutineTemplateCard(
                            title = "Evening Routine",
                            icon = DailyWellIcons.Misc.Night,
                            description = "Wind down for better sleep",
                            templates = HabitStackTemplates.eveningRoutineStacks,
                            habits = uiState.habits,
                            onApply = { viewModel.applyEveningRoutine() }
                        )
                    }
                }

                // Custom stack builder
                item {
                    StaggeredItem(index = 4) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PremiumSectionChip(
                            text = "Build your own",
                            icon = DailyWellIcons.Actions.Add
                        )
                    }
                }

                item {
                    StaggeredItem(index = 5) {
                        CustomStackBuilder(
                            habits = uiState.habits,
                            onCreateStack = { trigger, target, type ->
                                viewModel.createCustomStack(trigger, target, type)
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
private fun EmptyStacksCard() {
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
                imageVector = DailyWellIcons.Habits.HabitStacking,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "No active chains yet. Start with a template or create one below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StackingExplanationCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Prominent
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DailyWellIcons.Habits.HabitStacking,
                    contentDescription = "Habit Stacking",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Link Habits Together",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "3.2x more likely to succeed",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "\"After I [current habit], I will [new habit]\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Linking habits to existing routines makes them automatic. " +
                        "Your brain already has the trigger built in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveStackCard(
    stack: HabitStack,
    triggerHabit: Habit?,
    targetHabit: Habit?,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    if (triggerHabit == null || targetHabit == null) return

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        enablePressScale = true
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Visual chain representation
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trigger habit
                    HabitChip(
                        habitId = triggerHabit.id,
                        name = triggerHabit.name,
                        isEnabled = stack.isEnabled
                    )

                    // Arrow
                    Icon(
                        imageVector = when (stack.triggerType) {
                            StackTriggerType.AFTER -> DailyWellIcons.Nav.ArrowForward
                            StackTriggerType.BEFORE -> DailyWellIcons.Nav.Back
                            StackTriggerType.DURING -> DailyWellIcons.Analytics.Correlation
                        },
                        contentDescription = stack.triggerType.name,
                        modifier = Modifier.size(24.dp),
                        tint = if (stack.isEnabled) Success else MaterialTheme.colorScheme.outline
                    )

                    // Target habit
                    HabitChip(
                        habitId = targetHabit.id,
                        name = targetHabit.name,
                        isEnabled = stack.isEnabled
                    )
                }

                // Toggle switch
                Switch(
                    checked = stack.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = stack.getStackDescription(triggerHabit.name, targetHabit.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Stats
            if (stack.completionCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Completed ${stack.completionCount} times",
                    style = MaterialTheme.typography.labelSmall,
                    color = Success
                )
            }

            // Delete option
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Remove",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { onDelete() }
            )
        }
    }
}

@Composable
private fun HabitChip(
    habitId: String,
    name: String,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = DailyWellIcons.getHabitIcon(habitId),
            contentDescription = name,
            modifier = Modifier.size(16.dp),
            tint = if (isEnabled)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isEnabled)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RoutineTemplateCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    templates: List<HabitStackTemplates.StackTemplate>,
    habits: List<Habit>,
    onApply: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        enablePressScale = true
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Success
                    )
                ) {
                    Text("Apply")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Show chain preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                templates.forEachIndexed { index, template ->
                    val habit = habits.find { it.id == template.targetHabitId }
                    if (habit != null) {
                        Icon(
                            imageVector = DailyWellIcons.getHabitIcon(habit.id),
                            contentDescription = habit.name,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        if (index < templates.lastIndex) {
                            Icon(
                                imageVector = DailyWellIcons.Nav.ArrowForward,
                                contentDescription = "then",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomStackBuilder(
    habits: List<Habit>,
    onCreateStack: (triggerHabitId: String, targetHabitId: String, type: StackTriggerType) -> Unit
) {
    var selectedTrigger by remember { mutableStateOf<Habit?>(null) }
    var selectedTarget by remember { mutableStateOf<Habit?>(null) }
    var selectedType by remember { mutableStateOf(StackTriggerType.AFTER) }
    var showTriggerDropdown by remember { mutableStateOf(false) }
    var showTargetDropdown by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Create Custom Chain",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Trigger type selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StackTriggerType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = {
                            Text(
                                text = when (type) {
                                    StackTriggerType.AFTER -> "After"
                                    StackTriggerType.BEFORE -> "Before"
                                    StackTriggerType.DURING -> "During"
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trigger habit selector
            Text(
                text = when (selectedType) {
                    StackTriggerType.AFTER -> "After I..."
                    StackTriggerType.BEFORE -> "Before I..."
                    StackTriggerType.DURING -> "While I..."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTriggerDropdown = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTrigger != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = DailyWellIcons.getHabitIcon(selectedTrigger!!.id),
                                    contentDescription = selectedTrigger!!.name,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = selectedTrigger!!.name)
                            }
                        } else {
                            Text(
                                text = "Select trigger habit",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = DailyWellIcons.Nav.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTriggerDropdown,
                    onDismissRequest = { showTriggerDropdown = false }
                ) {
                    habits.filter { it.id != selectedTarget?.id }.forEach { habit ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = DailyWellIcons.getHabitIcon(habit.id),
                                        contentDescription = habit.name,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = habit.name)
                                }
                            },
                            onClick = {
                                selectedTrigger = habit
                                showTriggerDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target habit selector
            Text(
                text = "I will...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTargetDropdown = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedTarget != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = DailyWellIcons.getHabitIcon(selectedTarget!!.id),
                                    contentDescription = selectedTarget!!.name,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = selectedTarget!!.name)
                            }
                        } else {
                            Text(
                                text = "Select target habit",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = DailyWellIcons.Nav.ExpandMore,
                            contentDescription = "Expand",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTargetDropdown,
                    onDismissRequest = { showTargetDropdown = false }
                ) {
                    habits.filter { it.id != selectedTrigger?.id }.forEach { habit ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = DailyWellIcons.getHabitIcon(habit.id),
                                        contentDescription = habit.name,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = habit.name)
                                }
                            },
                            onClick = {
                                selectedTarget = habit
                                showTargetDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview
            if (selectedTrigger != null && selectedTarget != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Success.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Preview:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = HabitStack(
                                id = "",
                                triggerHabitId = selectedTrigger!!.id,
                                targetHabitId = selectedTarget!!.id,
                                triggerType = selectedType,
                                createdAt = ""
                            ).getStackDescription(selectedTrigger!!.name, selectedTarget!!.name),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create button
            Button(
                onClick = {
                    if (selectedTrigger != null && selectedTarget != null) {
                        onCreateStack(selectedTrigger!!.id, selectedTarget!!.id, selectedType)
                        selectedTrigger = null
                        selectedTarget = null
                    }
                },
                enabled = selectedTrigger != null && selectedTarget != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create Habit Chain")
            }
        }
    }
}
