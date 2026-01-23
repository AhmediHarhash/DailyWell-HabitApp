package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Habit Stacking Model
 * Based on James Clear's "Atomic Habits" - link new habits to existing routines
 * Formula: "After I [CURRENT HABIT], I will [NEW HABIT]"
 *
 * Research shows 3.2x higher success rate when habits are stacked
 */
@Serializable
data class HabitStack(
    val id: String,
    val triggerHabitId: String,      // The existing habit that triggers the stack
    val targetHabitId: String,        // The new habit to perform after trigger
    val triggerType: StackTriggerType = StackTriggerType.AFTER,
    val isEnabled: Boolean = true,
    val createdAt: String,            // ISO date
    val completionCount: Int = 0,     // How many times this stack was completed
    val lastCompletedAt: String? = null
) {
    fun getStackDescription(
        triggerHabitName: String,
        targetHabitName: String
    ): String {
        return when (triggerType) {
            StackTriggerType.AFTER -> "After $triggerHabitName, I will $targetHabitName"
            StackTriggerType.BEFORE -> "Before $triggerHabitName, I will $targetHabitName"
            StackTriggerType.DURING -> "While $triggerHabitName, I will $targetHabitName"
        }
    }
}

@Serializable
enum class StackTriggerType {
    AFTER,   // "After I [X], I will [Y]" - most common
    BEFORE,  // "Before I [X], I will [Y]"
    DURING   // "While I [X], I will [Y]"
}

/**
 * Pre-built habit stack templates based on behavioral science research
 */
object HabitStackTemplates {

    data class StackTemplate(
        val name: String,
        val description: String,
        val triggerHabitId: String,
        val targetHabitId: String,
        val triggerType: StackTriggerType = StackTriggerType.AFTER,
        val scienceNote: String
    )

    val morningRoutineStacks = listOf(
        StackTemplate(
            name = "Morning Hydration",
            description = "Start your day with water",
            triggerHabitId = "sleep",  // After waking up
            targetHabitId = "water",
            scienceNote = "Hydrating immediately after sleep replenishes fluids lost overnight"
        ),
        StackTemplate(
            name = "Morning Movement",
            description = "Move after your morning drink",
            triggerHabitId = "water",
            targetHabitId = "move",
            scienceNote = "Light movement after hydration increases blood flow and alertness"
        ),
        StackTemplate(
            name = "Morning Calm",
            description = "Center yourself after movement",
            triggerHabitId = "move",
            targetHabitId = "calm",
            scienceNote = "Post-exercise meditation leverages elevated endorphins for deeper practice"
        )
    )

    val eveningRoutineStacks = listOf(
        StackTemplate(
            name = "Evening Wind-Down",
            description = "Unplug after dinner",
            triggerHabitId = "vegetables",  // After eating
            targetHabitId = "unplug",
            scienceNote = "Blue light reduction 2-3 hours before bed improves sleep quality by 58%"
        ),
        StackTemplate(
            name = "Evening Connection",
            description = "Connect with loved ones after unplugging",
            triggerHabitId = "unplug",
            targetHabitId = "connect",
            scienceNote = "Face-to-face connection without screens increases oxytocin release"
        ),
        StackTemplate(
            name = "Pre-Sleep Calm",
            description = "Calm your mind before bed",
            triggerHabitId = "connect",
            targetHabitId = "calm",
            scienceNote = "Evening relaxation practices reduce cortisol and improve sleep onset"
        )
    )

    val healthOptimizationStacks = listOf(
        StackTemplate(
            name = "Hydrate Before Meals",
            description = "Drink water before eating",
            triggerHabitId = "vegetables",
            targetHabitId = "water",
            triggerType = StackTriggerType.BEFORE,
            scienceNote = "Drinking water 30 min before meals aids digestion and portion control"
        ),
        StackTemplate(
            name = "Move to Connect",
            description = "Exercise with others",
            triggerHabitId = "move",
            targetHabitId = "connect",
            triggerType = StackTriggerType.DURING,
            scienceNote = "Social exercise increases adherence by 40% and enjoyment by 65%"
        )
    )

    fun getAllTemplates(): List<StackTemplate> {
        return morningRoutineStacks + eveningRoutineStacks + healthOptimizationStacks
    }

    fun getTemplatesForHabit(habitId: String): List<StackTemplate> {
        return getAllTemplates().filter {
            it.triggerHabitId == habitId || it.targetHabitId == habitId
        }
    }
}

/**
 * Represents a user's saved habit stacking configuration
 */
@Serializable
data class UserHabitStacks(
    val stacks: List<HabitStack> = emptyList(),
    val activeRoutine: RoutineType = RoutineType.CUSTOM
) {
    fun getStacksForHabit(habitId: String): List<HabitStack> {
        return stacks.filter { it.triggerHabitId == habitId && it.isEnabled }
    }

    fun getNextHabitInChain(completedHabitId: String): String? {
        return stacks.find {
            it.triggerHabitId == completedHabitId && it.isEnabled
        }?.targetHabitId
    }

    fun withStackAdded(stack: HabitStack): UserHabitStacks {
        return copy(stacks = stacks + stack)
    }

    fun withStackRemoved(stackId: String): UserHabitStacks {
        return copy(stacks = stacks.filter { it.id != stackId })
    }

    fun withStackUpdated(stackId: String, update: (HabitStack) -> HabitStack): UserHabitStacks {
        return copy(stacks = stacks.map { if (it.id == stackId) update(it) else it })
    }
}

@Serializable
enum class RoutineType {
    MORNING,
    EVENING,
    CUSTOM
}
