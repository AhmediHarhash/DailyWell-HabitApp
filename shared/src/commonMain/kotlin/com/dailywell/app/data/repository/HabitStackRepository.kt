package com.dailywell.app.data.repository

import com.dailywell.app.data.model.HabitStack
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Habit Stacking feature
 * Habit stacking links habits together: "After X, I will Y"
 */
interface HabitStackRepository {

    /**
     * Get all habit stacks for the user
     */
    fun getAllStacks(): Flow<List<HabitStack>>

    /**
     * Get stacks that are triggered by a specific habit
     */
    fun getStacksForTrigger(triggerHabitId: String): Flow<List<HabitStack>>

    /**
     * Add a new habit stack
     */
    suspend fun addStack(stack: HabitStack)

    /**
     * Toggle a stack's enabled status
     */
    suspend fun toggleStack(stackId: String)

    /**
     * Delete a stack
     */
    suspend fun deleteStack(stackId: String)

    /**
     * Record that a stack was completed (trigger habit done, then target habit done)
     */
    suspend fun recordStackCompletion(stackId: String)

    /**
     * Get the next habit in a chain after completing a trigger habit
     * Returns null if no stacks are linked to this habit
     */
    suspend fun getNextHabitInChain(completedHabitId: String): String?

    /**
     * Clear all stacks (for testing or reset)
     */
    suspend fun clearAllStacks()
}
