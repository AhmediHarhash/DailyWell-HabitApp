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
import com.dailywell.app.data.repository.WeightUnit
import kotlinx.datetime.*
import kotlin.math.roundToInt

/**
 * Weight Log Dialog - Lightning Fast Entry
 *
 * PERFECTION MODE: < 5 second logging
 * - Large number input
 * - Unit toggle (kg/lbs)
 * - Date selector
 * - Optional note
 * - One-tap save
 *
 * Quality Standard: Better than MyFitnessPal's weight entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightLogDialog(
    currentWeight: Float? = null,
    onDismiss: () -> Unit,
    onSave: (weight: Float, unit: WeightUnit, note: String) -> Unit
) {
    var weightInput by remember {
        mutableStateOf(currentWeight?.roundToInt()?.toString() ?: "")
    }
    var selectedUnit by remember { mutableStateOf(WeightUnit.LBS) }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Clock.System.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showNoteField by remember { mutableStateOf(false) }

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
                        text = "Log Weight",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Large Weight Input
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = {
                        // Only allow numbers and one decimal point
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            weightInput = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    placeholder = {
                        Text(
                            text = "0",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 56.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(Modifier.height(16.dp))

                // Unit Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UnitToggleButton(
                        text = "KG",
                        selected = selectedUnit == WeightUnit.KG,
                        onClick = { selectedUnit = WeightUnit.KG },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(8.dp))

                    UnitToggleButton(
                        text = "LBS",
                        selected = selectedUnit == WeightUnit.LBS,
                        onClick = { selectedUnit = WeightUnit.LBS },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Date Selector
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "📅",
                                fontSize = 20.sp
                            )
                            Text(
                                text = formatDateDisplay(selectedDate),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Note Toggle
                if (!showNoteField) {
                    TextButton(
                        onClick = { showNoteField = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Note")
                    }
                } else {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note (optional)") },
                        placeholder = { Text("How are you feeling?") },
                        maxLines = 3,
                        trailingIcon = {
                            IconButton(onClick = {
                                showNoteField = false
                                note = ""
                            }) {
                                Icon(Icons.Default.Close, "Clear note")
                            }
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        val weight = weightInput.toFloatOrNull()
                        if (weight != null && weight > 0) {
                            onSave(weight, selectedUnit, note)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = weightInput.toFloatOrNull()?.let { it > 0 } == true
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Save Weight",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Date Picker Dialog (Simple Implementation)
    if (showDatePicker) {
        DatePickerDialog(
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
 * Unit Toggle Button
 */
@Composable
private fun UnitToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Simple Date Picker Dialog
 */
@Composable
private fun DatePickerDialog(
    currentDate: Instant,
    onDateSelected: (Instant) -> Unit,
    onDismiss: () -> Unit
) {
    val today = Clock.System.now()
    val dates = remember {
        // Last 7 days
        (0..6).map { daysAgo ->
            today.minus(daysAgo, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
        }
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
                    text = "Select Date",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                dates.forEach { date ->
                    DateOption(
                        date = date,
                        selected = isSameDay(date, currentDate),
                        onClick = { onDateSelected(date) }
                    )
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
 * Date Option Row
 */
@Composable
private fun DateOption(
    date: Instant,
    selected: Boolean,
    onClick: () -> Unit
) {
    val dateTime = date.toLocalDateTime(TimeZone.currentSystemDefault())
    val isToday = isSameDay(date, Clock.System.now())
    val isYesterday = isSameDay(
        date.plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault()),
        Clock.System.now()
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
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
                    text = when {
                        isToday -> "Today"
                        isYesterday -> "Yesterday"
                        else -> formatDayOfWeek(dateTime)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = formatDateDisplay(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
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
 * Helper: Format date for display (e.g., "Feb 7, 2026")
 */
private fun formatDateDisplay(instant: Instant): String {
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    return "${monthNames[dateTime.month.ordinal]} ${dateTime.dayOfMonth}, ${dateTime.year}"
}

/**
 * Helper: Format day of week (e.g., "Monday")
 */
private fun formatDayOfWeek(dateTime: LocalDateTime): String {
    val dayNames = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday", "Sunday"
    )
    return dayNames[dateTime.dayOfWeek.ordinal]
}
