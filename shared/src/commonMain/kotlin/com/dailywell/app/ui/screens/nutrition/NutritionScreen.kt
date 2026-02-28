package com.dailywell.app.ui.screens.nutrition

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.dailywell.app.data.model.*
import com.dailywell.app.ui.components.*
import kotlinx.coroutines.launch

/**
 * Nutrition Tracking Screen - FULLY WIRED
 * Beautiful UI with complete database integration
 *
 * PERFECTION MODE: Real-time updates from Firestore
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    userId: String,
    nutritionRepository: com.dailywell.app.data.repository.NutritionRepository,
    onScanFood: () -> Unit = {},
    onDetailLog: () -> Unit = {},
    onVoiceLog: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var dailyNutrition by remember { mutableStateOf<DailyNutrition?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load today's nutrition data
    LaunchedEffect(userId) {
        isLoading = true
        try {
            val data = nutritionRepository.getTodayNutrition(userId)
            dailyNutrition = data
        } finally {
            isLoading = false
        }
    }

    // Observe real-time updates
    LaunchedEffect(userId) {
        nutritionRepository.observeTodayNutrition(userId).collect { updated ->
            if (updated != null) {
                dailyNutrition = updated
            }
        }
    }

    GlassScreenWrapper {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                PremiumTopBar(
                    title = "Nutrition",
                    subtitle = "Today",
                    trailingActions = {
                        // Premium badge (optional)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = DailyWellIcons.Gamification.Crown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Premium",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(DailyWellIcons.Nav.Menu, "Menu")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (dailyNutrition == null) {
                EmptyNutritionState(onScanFood = onScanFood)
            } else {
                NutritionContent(
                    dailyNutrition = dailyNutrition!!,
                    onScanFood = onScanFood,
                    onDetailLog = onDetailLog,
                    onVoiceLog = onVoiceLog,
                    onAddWater = { amountMl ->
                        scope.launch {
                            nutritionRepository.updateWaterIntake(userId, amountMl).fold(
                                onSuccess = {
                                    snackbarHostState.showSnackbar("Water logged!")
                                },
                                onFailure = { error ->
                                    snackbarHostState.showSnackbar(
                                        error.message ?: "Failed to log water"
                                    )
                                }
                            )
                        }
                    },
                    padding = padding
                )
            }
        }
    }
}

/**
 * Empty State - No nutrition data yet
 */
@Composable
private fun EmptyNutritionState(
    onScanFood: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // TODO: Replace with custom illustration asset (nutrition empty state artwork)
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DailyWellIcons.Health.Nutrition,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Start Tracking Your Nutrition",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Scan your first meal to begin tracking calories and macros",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onScanFood,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = DailyWellIcons.Health.FoodScan,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Scan Your First Meal")
        }
    }
}

/**
 * Main Nutrition Content
 */
@Composable
private fun NutritionContent(
    dailyNutrition: DailyNutrition,
    onScanFood: () -> Unit,
    onDetailLog: () -> Unit,
    onVoiceLog: () -> Unit,
    onAddWater: (Int) -> Unit,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick add prompt
        item {
            StaggeredItem(index = 0) {
                QuickAddPrompt(onScanFood = onScanFood)
            }
        }

        // Main calorie display
        item {
            StaggeredItem(index = 1) {
                CalorieCard(
                    consumed = dailyNutrition.caloriesConsumed,
                    goal = dailyNutrition.calorieGoal
                )
            }
        }

        // Macros display
        item {
            StaggeredItem(index = 2) {
                MacrosCard(
                    macros = dailyNutrition.macros,
                    goals = dailyNutrition.macroGoals
                )
            }
        }

        // Water intake
        item {
            StaggeredItem(index = 3) {
                WaterCard(
                    consumed = dailyNutrition.waterIntake,
                    goal = dailyNutrition.waterGoal,
                    onAddWater = onAddWater
                )
            }
        }

        // Quick actions
        item {
            StaggeredItem(index = 4) {
                QuickActionsSection(
                    onScanFood = onScanFood,
                    onDetailLog = onDetailLog,
                    onVoiceLog = onVoiceLog
                )
            }
        }

        // Meals today
        if (dailyNutrition.meals.isNotEmpty()) {
            item {
                StaggeredItem(index = 5) {
                    PremiumSectionChip(
                        modifier = Modifier.padding(top = 8.dp),
                        text = "Today's meals",
                        icon = DailyWellIcons.Health.Nutrition
                    )
                }
            }

            items(dailyNutrition.meals) { meal ->
                MealCard(meal = meal)
            }
        }
    }
}


@Composable
fun QuickAddPrompt(onScanFood: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        enablePressScale = true,
        onClick = { onScanFood() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = DailyWellIcons.Health.FoodScan,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Did you eat something? Add it",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalorieCard(consumed: Int, goal: Int) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Prominent
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DailyWellIcons.Health.Calories,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFFF5722)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Calorie",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main calorie number
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = consumed.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Cal",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { consumed.toFloat() / goal },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(Modifier.height(8.dp))

            // Remaining
            Text(
                text = "Remaining: ${goal - consumed} Cal",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MacrosCard(macros: MacroNutrients, goals: MacroNutrients) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MacroItem(
            name = "Fat",
            value = macros.fat,
            goal = goals.fat,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
        MacroItem(
            name = "Protein",
            value = macros.protein,
            goal = goals.protein,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        MacroItem(
            name = "Carb",
            value = macros.carbs,
            goal = goals.carbs,
            color = Color(0xFF9C27B0),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MacroItem(
    name: String,
    value: Int,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                name,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$value",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "g",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (value.toFloat() / goal).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun WaterCard(
    consumed: Int,
    goal: Int,
    onAddWater: (Int) -> Unit = {}
) {
    var showWaterDialog by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        enablePressScale = true,
        onClick = { showWaterDialog = true }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Health.WaterDrop,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = Color(0xFF2196F3)
                    )
                    Column {
                        Text(
                            "Water Intake",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${consumed}ml / ${goal}ml",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "${(consumed.toFloat() / goal * 100).toInt()}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Quick add water buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WaterQuickButton(
                    amount = 250,
                    onAdd = onAddWater,
                    modifier = Modifier.weight(1f)
                )
                WaterQuickButton(
                    amount = 500,
                    onAdd = onAddWater,
                    modifier = Modifier.weight(1f)
                )
                WaterQuickButton(
                    amount = 1000,
                    onAdd = onAddWater,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showWaterDialog) {
        WaterLogDialog(
            onDismiss = { showWaterDialog = false },
            onAdd = { amount ->
                onAddWater(amount)
                showWaterDialog = false
            }
        )
    }
}

/**
 * Water Quick Add Button
 */
@Composable
private fun WaterQuickButton(
    amount: Int,
    onAdd: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onAdd(amount) },
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2196F3),
            contentColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            "${amount}ml",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Water Log Dialog
 */
@Composable
private fun WaterLogDialog(
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit
) {
    var customAmount by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = ElevationLevel.Prominent
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log Water",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(DailyWellIcons.Nav.Close, "Close")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Custom amount input
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = {
                        if (it.isEmpty() || it.matches(Regex("^\\d+$"))) {
                            customAmount = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (ml)") },
                    placeholder = { Text("250") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(24.dp))

                // Preset amounts
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(250, 500, 750, 1000).forEach { amount ->
                        Button(
                            onClick = { onAdd(amount) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text("${amount}")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amount = customAmount.toIntOrNull()
                        if (amount != null && amount > 0) {
                            onAdd(amount)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = customAmount.toIntOrNull()?.let { it > 0 } == true
                ) {
                    Icon(DailyWellIcons.Actions.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log Water")
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onScanFood: () -> Unit,
    onDetailLog: () -> Unit,
    onVoiceLog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickActionButton(
            iconVector = DailyWellIcons.Health.FoodScan,
            text = "Scan",
            onClick = onScanFood
        )
        QuickActionButton(
            iconVector = DailyWellIcons.Health.Nutrition,
            text = "Detail Log",
            onClick = onDetailLog
        )
        QuickActionButton(
            iconVector = DailyWellIcons.Actions.Search,
            text = "Search",
            onClick = onDetailLog
        )

        // Voice log button (gradient)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = ElevationLevel.Subtle,
            enablePressScale = true,
            onClick = { onVoiceLog() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00C853), Color(0xFF00E676))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = DailyWellIcons.Coaching.Microphone,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.surface
                    )
                    Column {
                        Text(
                            "Log with Voice",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.surface
                        )
                        Text(
                            "Coming Soon",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    iconVector: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle,
        enablePressScale = true,
        onClick = { onClick() }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                DailyWellIcons.Nav.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MealCard(meal: MealEntry) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = ElevationLevel.Subtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    meal.mealName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    "${meal.totalCalories} Cal",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            meal.foods.forEach { food ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${food.name} (${food.serving})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${food.calories} Cal",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
