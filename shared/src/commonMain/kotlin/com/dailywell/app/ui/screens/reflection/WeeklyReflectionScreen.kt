package com.dailywell.app.ui.screens.reflection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.data.model.Habit

data class WeeklyReflectionData(
    val weekNumber: Int,
    val completionRate: Float,
    val bestHabit: Habit?,
    val bestHabitRate: Float,
    val focusHabit: Habit?,
    val focusHabitRate: Float,
    val perfectDays: Int,
    val totalDays: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReflectionScreen(
    reflectionData: WeeklyReflectionData,
    onDismiss: () -> Unit,
    onSaveReflection: (String, String, String) -> Unit // wins, challenges, intentions
) {
    var currentPage by remember { mutableStateOf(0) }
    var wins by remember { mutableStateOf("") }
    var challenges by remember { mutableStateOf("") }
    var intentions by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Week ${reflectionData.weekNumber} Reflection") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(4.dp),
                            color = if (index <= currentPage) {
                                Success
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (currentPage) {
                0 -> WeekSummaryPage(reflectionData)
                1 -> ReflectionInputPage(
                    title = "What went well?",
                    subtitle = "Celebrate your wins this week",
                    emoji = "🎉",
                    value = wins,
                    onValueChange = { wins = it },
                    placeholder = "I'm proud that I..."
                )
                2 -> ReflectionInputPage(
                    title = "What was challenging?",
                    subtitle = "Acknowledge your struggles",
                    emoji = "💪",
                    value = challenges,
                    onValueChange = { challenges = it },
                    placeholder = "I found it hard to..."
                )
                3 -> ReflectionInputPage(
                    title = "Next week intentions",
                    subtitle = "Set your focus for the week ahead",
                    emoji = "🎯",
                    value = intentions,
                    onValueChange = { intentions = it },
                    placeholder = "This week I will..."
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back")
                    }
                }

                Button(
                    onClick = {
                        if (currentPage < 3) {
                            currentPage++
                        } else {
                            onSaveReflection(wins, challenges, intentions)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Success
                    )
                ) {
                    Text(if (currentPage < 3) "Continue" else "Save Reflection")
                }
            }
        }
    }
}

@Composable
private fun WeekSummaryPage(data: WeeklyReflectionData) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📊", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your Week in Review",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(data.completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Success
                )
                Text(
                    text = "Overall Completion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Perfect days
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "✨", fontSize = 24.sp)
                    Text(
                        text = "${data.perfectDays}/${data.totalDays}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Perfect Days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Best habit
            if (data.bestHabit != null) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = data.bestHabit.emoji, fontSize = 24.sp)
                        Text(
                            text = "${(data.bestHabitRate * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Best Habit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Focus area
        if (data.focusHabit != null && data.focusHabitRate < 0.7f) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = data.focusHabit.emoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Focus Area",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${data.focusHabit.name} at ${(data.focusHabitRate * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReflectionInputPage(
    title: String,
    subtitle: String,
    emoji: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emoji, fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(500)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = { Text(placeholder) },
            shape = RoundedCornerShape(16.dp)
        )

        Text(
            text = "${value.length}/500",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 4.dp)
        )
    }
}
