package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.local.db.HabitDao
import com.dailywell.app.data.local.db.HabitEntity
import com.dailywell.app.data.model.Habit
import com.dailywell.app.domain.model.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val dataStoreManager: DataStoreManager,
    private val settingsRepository: SettingsRepository
) : HabitRepository {

    override fun getAllHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { it.toHabit() }
        }
    }

    override fun getEnabledHabits(): Flow<List<Habit>> {
        return habitDao.getEnabledHabits().map { entities ->
            entities.map { it.toHabit() }
        }
    }

    override fun getCustomHabits(): Flow<List<Habit>> {
        return habitDao.getCustomHabits().map { entities ->
            entities.map { it.toHabit() }
        }
    }

    override suspend fun getHabitById(id: String): Habit? {
        return habitDao.getHabitById(id)?.toHabit()
    }

    override suspend fun saveHabit(habit: Habit) {
        habitDao.insertHabit(HabitEntity.fromHabit(habit))
    }

    override suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(HabitEntity.fromHabit(habit))
    }

    override suspend fun deleteHabit(habitId: String) {
        habitDao.deleteHabit(habitId)
    }

    override suspend fun setHabitEnabled(habitId: String, enabled: Boolean) {
        habitDao.setHabitEnabled(habitId, enabled)
    }

    override suspend fun getCustomHabitCount(): Int {
        return habitDao.getCustomHabitCount()
    }

    override suspend fun initializeDefaultHabits() {
        val existingHabits = habitDao.getAllHabits().first()
        if (existingHabits.isEmpty()) {
            val defaultHabits = HabitType.entries.mapIndexed { index, type ->
                HabitEntity.fromHabit(
                    Habit.fromHabitType(
                        habitType = type,
                        order = index,
                        isEnabled = index < 3 // First 3 enabled by default for free users
                    )
                )
            }
            habitDao.insertHabits(defaultHabits)
        }
    }

    override suspend fun createCustomHabit(
        name: String,
        emoji: String,
        threshold: String,
        question: String
    ): Habit {
        val allHabits = habitDao.getAllHabits().first()
        val maxOrder = allHabits.maxOfOrNull { it.order } ?: 0

        val habit = Habit.createCustom(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            emoji = emoji,
            threshold = threshold,
            question = question,
            order = maxOrder + 1
        )

        habitDao.insertHabit(HabitEntity.fromHabit(habit))
        return habit
    }
}
