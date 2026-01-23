package com.dailywell.app.data.repository

import com.dailywell.app.data.model.Habit
import kotlinx.coroutines.flow.Flow

interface HabitRepository {
    fun getEnabledHabits(): Flow<List<Habit>>
    fun getAllHabits(): Flow<List<Habit>>
    fun getCustomHabits(): Flow<List<Habit>>
    suspend fun getHabitById(id: String): Habit?
    suspend fun initializeDefaultHabits()
    suspend fun saveHabit(habit: Habit)
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habitId: String)
    suspend fun setHabitEnabled(habitId: String, enabled: Boolean)
    suspend fun getCustomHabitCount(): Int
    suspend fun createCustomHabit(
        name: String,
        emoji: String,
        threshold: String,
        question: String
    ): Habit
}
