package com.dailywell.app.ui.screens.customhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywell.app.core.theme.Success
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomHabitScreen(
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, threshold: String, question: String) -> Unit,
    existingCustomCount: Int,
    isPremium: Boolean
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && selectedEmoji.isNotBlank() && threshold.isNotBlank()
    val maxCustomHabits = if (isPremium) 3 else 0
    val canAddMore = existingCustomCount < maxCustomHabits

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Custom Habit") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 20.sp)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (canSave && canAddMore) {
                                val finalQuestion = question.ifBlank { "Did you complete $name?" }
                                onSave(name, selectedEmoji, threshold, finalQuestion)
                            }
                        },
                        enabled = canSave && canAddMore
                    ) {
                        Text(
                            "Save",
                            color = if (canSave && canAddMore) Success else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!canAddMore && !isPremium) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Custom habits are a Premium feature",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else if (!canAddMore) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "You've reached the maximum of $maxCustomHabits custom habits",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Emoji selector
            Text(
                text = "Choose an emoji",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedEmoji.isNotEmpty()) {
                            Success.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable { showEmojiPicker = true }
                    .then(
                        if (selectedEmoji.isNotEmpty()) {
                            Modifier.border(2.dp, Success, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedEmoji.ifEmpty { "+" },
                    fontSize = if (selectedEmoji.isNotEmpty()) 36.sp else 24.sp
                )
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(30) },
                label = { Text("Habit name") },
                placeholder = { Text("e.g., Read, Meditate, Journal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            // Threshold
            OutlinedTextField(
                value = threshold,
                onValueChange = { threshold = it.take(50) },
                label = { Text("What counts?") },
                placeholder = { Text("e.g., 20+ minutes, 10 pages, Any amount") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            // Custom question (optional)
            OutlinedTextField(
                value = question,
                onValueChange = { question = it.take(100) },
                label = { Text("Custom question (optional)") },
                placeholder = { Text("e.g., Did you read today?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                supportingText = {
                    Text("Leave empty for default: \"Did you complete [name]?\"")
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Preview
            if (name.isNotBlank() && selectedEmoji.isNotBlank()) {
                Text(
                    text = "PREVIEW",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedEmoji, fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = threshold.ifBlank { "Set threshold" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "○", fontSize = 24.sp)
                        }
                    }
                }
            }
        }

        // Emoji picker dialog
        if (showEmojiPicker) {
            EmojiPickerDialog(
                onEmojiSelected = {
                    selectedEmoji = it
                    showEmojiPicker = false
                },
                onDismiss = { showEmojiPicker = false }
            )
        }
    }
}

@Composable
private fun EmojiPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf(
        // Activities
        "📚", "✍️", "🎨", "🎵", "🎸", "🎹", "📷", "🎬",
        // Health
        "🧘", "🏋️", "🚴", "🏊", "⚽", "🎾", "🏀", "🥊",
        // Nature
        "🌳", "🌻", "🌊", "⛰️", "🏕️", "🌅", "🌙", "⭐",
        // Food
        "🍎", "🥗", "🍳", "☕", "🍵", "💊", "🧃", "🥤",
        // Social
        "👋", "🤝", "💬", "📞", "💌", "🎁", "🎉", "❤️",
        // Self-care
        "🛁", "💤", "🧴", "💅", "🪥", "🧹", "📱", "💻",
        // Learning
        "🧠", "💡", "🎓", "📝", "📖", "🔬", "🌍", "💰"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an emoji") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEmojiSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
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
