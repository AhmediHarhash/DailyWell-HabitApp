package com.dailywell.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.dailywell.app.data.local.db.DailyWellDatabase
import com.dailywell.app.data.local.db.EntryDao
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * DailyWell Home Screen Widget
 * Shows today's habits with one-tap completion
 */
class HabitWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),  // Small
            DpSize(180.dp, 180.dp),  // Medium
            DpSize(280.dp, 180.dp),  // Wide
            DpSize(280.dp, 280.dp),  // Large
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load habit data from DataStore/Repository
        val habitData = loadHabitData(context)

        provideContent {
            GlanceTheme {
                HabitWidgetContent(
                    habits = habitData.habits,
                    completedCount = habitData.completedCount,
                    totalCount = habitData.totalCount,
                    streakDays = habitData.streakDays
                )
            }
        }
    }

    private suspend fun loadHabitData(context: Context): WidgetHabitData {
        return try {
            val database = DailyWellDatabase.getInstance(context)
            val habitDao = database.habitDao()
            val entryDao = database.entryDao()

            // Get today's date
            val now = Clock.System.now()
            val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
            val today = "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"

            // Load enabled habits from database
            val habits = habitDao.getEnabledHabits().firstOrNull() ?: emptyList()

            // Load today's entries (one per habit)
            val todayEntries = entryDao.getEntriesForDateSync(today)
            val completedHabitIds = todayEntries.filter { it.completed }.map { it.habitId }.toSet()

            // Map to widget habits
            val widgetHabits = habits.map { habit ->
                WidgetHabit(
                    id = habit.id,
                    name = habit.name,
                    emoji = habit.emoji,
                    isCompleted = habit.id in completedHabitIds
                )
            }

            val completedCount = widgetHabits.count { it.isCompleted }

            // Calculate streak from perfect days
            val streakDays = calculateCurrentStreak(entryDao)

            WidgetHabitData(
                habits = widgetHabits,
                completedCount = completedCount,
                totalCount = widgetHabits.size,
                streakDays = streakDays
            )
        } catch (e: Exception) {
            // Fallback to empty state if database access fails
            WidgetHabitData(
                habits = emptyList(),
                completedCount = 0,
                totalCount = 0,
                streakDays = 0
            )
        }
    }

    private suspend fun calculateCurrentStreak(entryDao: EntryDao): Int {
        val perfectDays = entryDao.getPerfectDays()
        if (perfectDays.isEmpty()) return 0

        // Count consecutive days starting from most recent
        var streak = 0
        var expectedDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        for (dateStr in perfectDays) {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0].toIntOrNull() ?: continue
                val month = parts[1].toIntOrNull() ?: continue
                val day = parts[2].toIntOrNull() ?: continue
                val date = kotlinx.datetime.LocalDate(year, month, day)

                if (date == expectedDate || date == expectedDate.minus(DatePeriod(days = 1))) {
                    streak++
                    expectedDate = date.minus(DatePeriod(days = 1))
                } else {
                    break
                }
            }
        }
        return streak
    }
}

@Composable
private fun HabitWidgetContent(
    habits: List<WidgetHabit>,
    completedCount: Int,
    totalCount: Int,
    streakDays: Int
) {
    val size = LocalSize.current

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.White))
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DailyWell",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ColorProvider(Color(0xFF1B5E20))
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                // Streak badge
                if (streakDays > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            style = TextStyle(fontSize = 12.sp)
                        )
                        Text(
                            text = "$streakDays",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ColorProvider(Color(0xFFFF5722))
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Progress indicator
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedCount/$totalCount",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = ColorProvider(Color.Gray)
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                // Progress bar background
                Box(
                    modifier = GlanceModifier
                        .height(4.dp)
                        .defaultWeight()
                        .background(ColorProvider(Color(0xFFE0E0E0)))
                        .cornerRadius(2.dp)
                ) {
                    // Progress bar fill (simplified - shows full when any progress)
                    if (completedCount > 0) {
                        Box(
                            modifier = GlanceModifier
                                .height(4.dp)
                                .fillMaxWidth()
                                .background(ColorProvider(Color(0xFF4CAF50)))
                                .cornerRadius(2.dp),
                            content = {}
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Habit list - adapts based on widget size
            if (size.height >= 180.dp) {
                // Show habit list for larger widgets
                Column(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    habits.take(if (size.height >= 280.dp) 5 else 3).forEach { habit ->
                        HabitRow(habit = habit)
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            } else {
                // Compact view for small widgets
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    habits.take(3).forEach { habit ->
                        CompactHabitItem(habit = habit)
                        Spacer(modifier = GlanceModifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitRow(habit: WidgetHabit) {
    val habitIdKey = ActionParameters.Key<String>("habitId")

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(
                ColorProvider(
                    if (habit.isCompleted) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                )
            )
            .cornerRadius(8.dp)
            .padding(8.dp)
            .clickable(
                actionRunCallback<ToggleHabitAction>(
                    actionParametersOf(habitIdKey to habit.id)
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = habit.emoji,
            style = TextStyle(fontSize = 18.sp)
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = habit.name,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = ColorProvider(Color.Black)
            ),
            modifier = GlanceModifier.defaultWeight()
        )
        // Checkbox
        Box(
            modifier = GlanceModifier
                .size(24.dp)
                .background(
                    ColorProvider(
                        if (habit.isCompleted) Color(0xFF4CAF50) else Color.White
                    )
                )
                .cornerRadius(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (habit.isCompleted) {
                Text(
                    text = "✓",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun CompactHabitItem(habit: WidgetHabit) {
    val habitIdKey = ActionParameters.Key<String>("habitId")

    Box(
        modifier = GlanceModifier
            .size(40.dp)
            .background(
                ColorProvider(
                    if (habit.isCompleted) Color(0xFF4CAF50) else Color(0xFFF5F5F5)
                )
            )
            .cornerRadius(20.dp)
            .clickable(
                actionRunCallback<ToggleHabitAction>(
                    actionParametersOf(habitIdKey to habit.id)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (habit.isCompleted) "✓" else habit.emoji,
            style = TextStyle(
                fontSize = if (habit.isCompleted) 18.sp else 20.sp,
                color = ColorProvider(if (habit.isCompleted) Color.White else Color.Black)
            )
        )
    }
}

/**
 * Action callback for toggling habit completion from widget
 */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitIdKey = ActionParameters.Key<String>("habitId")
        val habitId = parameters[habitIdKey] ?: return

        // Toggle the habit in database
        val database = DailyWellDatabase.getInstance(context)
        val entryDao = database.entryDao()

        // Get today's date
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val today = "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"

        // Check current entry status and toggle
        val existingEntry = entryDao.getEntry(today, habitId)
        if (existingEntry != null) {
            // Toggle completion status
            entryDao.insertEntry(
                com.dailywell.app.data.local.db.EntryEntity(
                    id = existingEntry.id,
                    date = today,
                    habitId = habitId,
                    completed = !existingEntry.completed,
                    createdAt = existingEntry.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            // Create new completed entry
            entryDao.insertEntry(
                com.dailywell.app.data.local.db.EntryEntity(
                    date = today,
                    habitId = habitId,
                    completed = true
                )
            )
        }

        // Update the widget
        HabitWidget().update(context, glanceId)
    }
}

/**
 * Widget data classes
 */
data class WidgetHabitData(
    val habits: List<WidgetHabit>,
    val completedCount: Int,
    val totalCount: Int,
    val streakDays: Int
)

data class WidgetHabit(
    val id: String,
    val name: String,
    val emoji: String,
    val isCompleted: Boolean
)
