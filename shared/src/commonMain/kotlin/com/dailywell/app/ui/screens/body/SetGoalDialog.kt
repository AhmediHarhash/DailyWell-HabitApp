package com.dailywell.app.ui.screens.body

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dailywell.app.data.repository.BodyGoal
import kotlinx.datetime.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Set Goal Dialog - Motivating Goal Setting
 *
 * PERFECTION MODE: Beautiful goal configuration
 * - Target weight input
 * - Target date picker
 * - Height input (for BMI)
 * - Progress estimate
 * - Motivational messaging
 *
 * Quality Standard: Better than Lose It's goal setting
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetGoalDialog(
    currentWeight: Float, // in kg
    currentGoal: BodyGoal? = null,
    onDismiss: () -> Unit,
    onSave: (targetWeightKg: Float, targetDate: String, heightCm: Float) -> Unit
) {
    var targetWeightInput by remember {
        mutableStateOf(currentGoal?.targetWeightKg?.roundToInt()?.toString() ?: "")
    }
    var heightInput by remember {
        mutableStateOf(currentGoal?.heightCm?.roundToInt()?.toString() ?: "170")
    }
    var selectedDate by remember {
        mutableStateOf(
            if (currentGoal != null) {
                try {
                    Instant.parse(currentGoal.targetDate)
                } catch (e: Exception) {
                    Clock.System.now().plus(90, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                }
            } else {
                Clock.System.now().plus(90, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
            }
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Calculate estimates
    val targetWeight = targetWeightInput.toFloatOrNull() ?: currentWeight
    val weightToLose = currentWeight - targetWeight
    val today = Clock.System.now()
    val daysToGoal = ((selectedDate.toEpochMilliseconds() - today.toEpochMilliseconds()) / (1000 * 60 * 60 * 24)).toInt()
    val weeksToGoal = daysToGoal / 7
    val lbsPerWeek = if (weeksToGoal > 0) (weightToLose * 2.20462f) / weeksToGoal else 0f

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentGoal != null) "Update Goal" else "Set Your Goal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Current Weight Display
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
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
                                text = "Current Weight",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${(currentWeight * 2.20462f).roundToInt()} lbs",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            "💪",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Target Weight Input
                OutlinedTextField(
                    value = targetWeightInput,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            targetWeightInput = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Target Weight (kg)") },
                    placeholder = { Text("70") },
                    leadingIcon = {
                        Text("🎯", fontSize = 20.sp)
                    },
                    trailingIcon = {
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                // Height Input
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            heightInput = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Height (cm)") },
                    placeholder = { Text("170") },
                    leadingIcon = {
                        Text("📏", fontSize = 20.sp)
                    },
                    trailingIcon = {
                        Text(
                            text = "cm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                // Target Date Selector
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Target Date",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = formatGoalDateDisplay(selectedDate),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            "📅",
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Progress Estimate Card
                if (targetWeightInput.toFloatOrNull() != null && weightToLose != 0f) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (abs(lbsPerWeek) <= 2f) {
                            Color(0xFF10B981).copy(alpha = 0.1f)
                        } else {
                            Color(0xFFF59E0B).copy(alpha = 0.1f)
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    if (abs(lbsPerWeek) <= 2f) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (abs(lbsPerWeek) <= 2f) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                                Text(
                                    text = "Goal Estimate",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = buildString {
                                    append("${abs(weightToLose * 2.20462f).roundToInt()} lbs ")
                                    append(if (weightToLose > 0) "to lose" else "to gain")
                                    append(" in $weeksToGoal weeks")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "${abs(lbsPerWeek).roundToInt()} lbs per week",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = when {
                                    abs(lbsPerWeek) <= 1f -> "✨ Safe and sustainable pace"
                                    abs(lbsPerWeek) <= 2f -> "💪 Challenging but achievable"
                                    else -> "⚠️ Consider extending your timeline"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (abs(lbsPerWeek) <= 2f) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                // Save Button
                Button(
                    onClick = {
                        val targetWeightKg = targetWeightInput.toFloatOrNull()
                        val heightCm = heightInput.toFloatOrNull()

                        if (targetWeightKg != null && targetWeightKg > 0 && heightCm != null && heightCm > 0) {
                            onSave(
                                targetWeightKg,
                                selectedDate.toString(),
                                heightCm
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = targetWeightInput.toFloatOrNull()?.let { it > 0 } == true &&
                            heightInput.toFloatOrNull()?.let { it > 0 } == true
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (currentGoal != null) "Update Goal" else "Set Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete Goal (if exists)
                if (currentGoal != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { /* Delete goal logic */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Goal", color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }

    // Goal Date Picker
    if (showDatePicker) {
        GoalDatePickerDialog(
            currentDate = selectedDate,
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Goal Date Picker Dialog
 */
@Composable
private fun GoalDatePickerDialog(
    currentDate: Instant,
    onDateSelected: (Instant) -> Unit,
    onDismiss: () -> Unit
) {
    val today = Clock.System.now()
    val goalOptions = remember {
        listOf(
            "1 month" to 30,
            "2 months" to 60,
            "3 months" to 90,
            "6 months" to 180,
            "1 year" to 365
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Target Date",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                goalOptions.forEach { (label, days) ->
                    val targetDate = today.plus(days, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                    val isSelected = isSameDay(targetDate, currentDate)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onDateSelected(targetDate) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
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
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = formatGoalDateDisplay(targetDate),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Helper: Check if two instants are on the same day
 */
private fun isSameDay(date1: Instant, date2: Instant): Boolean {
    val tz = TimeZone.currentSystemDefault()
    val d1 = date1.toLocalDateTime(tz).date
    val d2 = date2.toLocalDateTime(tz).date
    return d1 == d2
}

/**
 * Helper: Format goal date for display
 */
private fun formatGoalDateDisplay(instant: Instant): String {
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    return "${monthNames[dateTime.month.ordinal]} ${dateTime.dayOfMonth}, ${dateTime.year}"
}
