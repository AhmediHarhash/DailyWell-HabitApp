package com.dailywell.app.ui.screens.customhabit

import androidx.compose.animation.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.dailywell.app.core.theme.Success
import com.dailywell.app.ui.components.*

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

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
            PremiumTopBar(
                title = "Add Custom Habit",
                subtitle = "Create your own check-in",
                onNavigationClick = onDismiss,
                trailingActions = {
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
            AnimatedVisibility(
                visible = !canAddMore && !isPremium,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
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
            }

            AnimatedVisibility(
                visible = !canAddMore && isPremium,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
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

            // Icon selector
            PremiumSectionChip(
                text = "Choose an icon",
                icon = DailyWellIcons.Actions.Edit
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .pressScale()
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
                if (selectedEmoji.isNotEmpty()) {
                    Icon(
                        imageVector = getCustomHabitIcon(selectedEmoji),
                        contentDescription = "Selected icon",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = DailyWellIcons.Actions.Add,
                        contentDescription = "Select icon",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
            AnimatedVisibility(
                visible = name.isNotBlank() && selectedEmoji.isNotBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    PremiumSectionChip(
                        text = "Preview",
                        icon = DailyWellIcons.Actions.CheckCircle
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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
                            Icon(
                                imageVector = getCustomHabitIcon(selectedEmoji),
                                contentDescription = name,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                                Icon(
                                    imageVector = Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = "Not completed",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
}

@Composable
private fun EmojiPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val iconOptions = listOf(
        // Activities
        "book" to Icons.Filled.MenuBook,
        "edit" to Icons.Filled.Edit,
        "palette" to Icons.Filled.Palette,
        "music" to Icons.Filled.MusicNote,
        "headphones" to Icons.Filled.Headphones,
        "piano" to Icons.Filled.Piano,
        "camera" to Icons.Filled.CameraAlt,
        "movie" to Icons.Filled.Movie,
        // Health
        "yoga" to Icons.Filled.SelfImprovement,
        "fitness" to Icons.Filled.FitnessCenter,
        "cycling" to Icons.Filled.DirectionsBike,
        "pool" to Icons.Filled.Pool,
        "sports" to Icons.Filled.SportsSoccer,
        "run" to Icons.Filled.DirectionsRun,
        "walk" to Icons.Filled.DirectionsWalk,
        "heart" to Icons.Filled.Favorite,
        // Nature
        "nature" to Icons.Filled.Park,
        "flower" to Icons.Filled.LocalFlorist,
        "water" to Icons.Filled.WaterDrop,
        "terrain" to Icons.Filled.Terrain,
        "camping" to Icons.Filled.NaturePeople,
        "sunrise" to Icons.Filled.WbSunny,
        "night" to Icons.Filled.NightsStay,
        "star" to Icons.Filled.Star,
        // Food
        "apple" to Icons.Filled.Spa,
        "salad" to Icons.Filled.Restaurant,
        "cooking" to Icons.Filled.OutdoorGrill,
        "coffee" to Icons.Filled.Coffee,
        "tea" to Icons.Filled.EmojiFoodBeverage,
        "medication" to Icons.Filled.Medication,
        "drink" to Icons.Filled.LocalDrink,
        "nutrition" to Icons.Filled.Egg,
        // Social
        "wave" to Icons.Filled.WavingHand,
        "handshake" to Icons.Filled.Handshake,
        "chat" to Icons.Filled.Chat,
        "phone" to Icons.Filled.Phone,
        "mail" to Icons.Filled.Mail,
        "gift" to Icons.Filled.CardGiftcard,
        "celebration" to Icons.Filled.Celebration,
        "love" to Icons.Filled.Favorite,
        // Self-care
        "spa" to Icons.Filled.Spa,
        "sleep" to Icons.Filled.Bedtime,
        "clean" to Icons.Filled.CleaningServices,
        "home" to Icons.Filled.Home,
        "mobile" to Icons.Filled.PhoneAndroid,
        "laptop" to Icons.Filled.Laptop,
        // Learning
        "brain" to Icons.Filled.Psychology,
        "idea" to Icons.Filled.Lightbulb,
        "school" to Icons.Filled.School,
        "notes" to Icons.Filled.EditNote,
        "reading" to Icons.Filled.AutoStories,
        "science" to Icons.Filled.Science,
        "globe" to Icons.Filled.Public,
        "savings" to Icons.Filled.Savings
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an icon") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(iconOptions) { (key, icon) ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEmojiSelected(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = key,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
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

/**
 * Maps a stored icon key string to the corresponding Material Icon.
 * Falls back to DailyWellIcons.Habits.Custom (Star) for unknown keys.
 */
private fun getCustomHabitIcon(key: String): ImageVector {
    return when (key) {
        "book" -> Icons.Filled.MenuBook
        "edit" -> Icons.Filled.Edit
        "palette" -> Icons.Filled.Palette
        "music" -> Icons.Filled.MusicNote
        "headphones" -> Icons.Filled.Headphones
        "piano" -> Icons.Filled.Piano
        "camera" -> Icons.Filled.CameraAlt
        "movie" -> Icons.Filled.Movie
        "yoga" -> Icons.Filled.SelfImprovement
        "fitness" -> Icons.Filled.FitnessCenter
        "cycling" -> Icons.Filled.DirectionsBike
        "pool" -> Icons.Filled.Pool
        "sports" -> Icons.Filled.SportsSoccer
        "run" -> Icons.Filled.DirectionsRun
        "walk" -> Icons.Filled.DirectionsWalk
        "heart" -> Icons.Filled.Favorite
        "nature" -> Icons.Filled.Park
        "flower" -> Icons.Filled.LocalFlorist
        "water" -> Icons.Filled.WaterDrop
        "terrain" -> Icons.Filled.Terrain
        "camping" -> Icons.Filled.NaturePeople
        "sunrise" -> Icons.Filled.WbSunny
        "night" -> Icons.Filled.NightsStay
        "star" -> Icons.Filled.Star
        "apple" -> Icons.Filled.Spa
        "salad" -> Icons.Filled.Restaurant
        "cooking" -> Icons.Filled.OutdoorGrill
        "coffee" -> Icons.Filled.Coffee
        "tea" -> Icons.Filled.EmojiFoodBeverage
        "medication" -> Icons.Filled.Medication
        "drink" -> Icons.Filled.LocalDrink
        "nutrition" -> Icons.Filled.Egg
        "wave" -> Icons.Filled.WavingHand
        "handshake" -> Icons.Filled.Handshake
        "chat" -> Icons.Filled.Chat
        "phone" -> Icons.Filled.Phone
        "mail" -> Icons.Filled.Mail
        "gift" -> Icons.Filled.CardGiftcard
        "celebration" -> Icons.Filled.Celebration
        "love" -> Icons.Filled.Favorite
        "spa" -> Icons.Filled.Spa
        "sleep" -> Icons.Filled.Bedtime
        "clean" -> Icons.Filled.CleaningServices
        "home" -> Icons.Filled.Home
        "mobile" -> Icons.Filled.PhoneAndroid
        "laptop" -> Icons.Filled.Laptop
        "brain" -> Icons.Filled.Psychology
        "idea" -> Icons.Filled.Lightbulb
        "school" -> Icons.Filled.School
        "notes" -> Icons.Filled.EditNote
        "reading" -> Icons.Filled.AutoStories
        "science" -> Icons.Filled.Science
        "globe" -> Icons.Filled.Public
        "savings" -> Icons.Filled.Savings
        else -> DailyWellIcons.Habits.Custom
    }
}
