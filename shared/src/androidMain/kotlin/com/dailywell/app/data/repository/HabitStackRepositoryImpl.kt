package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.HabitStack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * Implementation of HabitStackRepository using DataStore
 */
class HabitStackRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : HabitStackRepository {

    override fun getAllStacks(): Flow<List<HabitStack>> {
        return dataStoreManager.habitStacks.map { it }
    }

    override fun getStacksForTrigger(triggerHabitId: String): Flow<List<HabitStack>> {
        return dataStoreManager.habitStacks.map { stacks ->
            stacks.filter { it.triggerHabitId == triggerHabitId && it.isEnabled }
        }
    }

    override suspend fun addStack(stack: HabitStack) {
        val currentStacks = dataStoreManager.habitStacks.first()
        // Don't add duplicate stacks
        val exists = currentStacks.any {
            it.triggerHabitId == stack.triggerHabitId &&
                    it.targetHabitId == stack.targetHabitId &&
                    it.triggerType == stack.triggerType
        }
        if (!exists) {
            dataStoreManager.updateHabitStacks(currentStacks + stack)
        }
    }

    override suspend fun toggleStack(stackId: String) {
        val currentStacks = dataStoreManager.habitStacks.first()
        val updatedStacks = currentStacks.map { stack ->
            if (stack.id == stackId) {
                stack.copy(isEnabled = !stack.isEnabled)
            } else {
                stack
            }
        }
        dataStoreManager.updateHabitStacks(updatedStacks)
    }

    override suspend fun deleteStack(stackId: String) {
        val currentStacks = dataStoreManager.habitStacks.first()
        dataStoreManager.updateHabitStacks(currentStacks.filter { it.id != stackId })
    }

    override suspend fun recordStackCompletion(stackId: String) {
        val currentStacks = dataStoreManager.habitStacks.first()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

        val updatedStacks = currentStacks.map { stack ->
            if (stack.id == stackId) {
                stack.copy(
                    completionCount = stack.completionCount + 1,
                    lastCompletedAt = today
                )
            } else {
                stack
            }
        }
        dataStoreManager.updateHabitStacks(updatedStacks)
    }

    override suspend fun getNextHabitInChain(completedHabitId: String): String? {
        val stacks = dataStoreManager.habitStacks.first()
        return stacks.find {
            it.triggerHabitId == completedHabitId && it.isEnabled
        }?.targetHabitId
    }

    override suspend fun clearAllStacks() {
        dataStoreManager.updateHabitStacks(emptyList())
    }
}
