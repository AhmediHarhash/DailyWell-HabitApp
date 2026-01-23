package com.dailywell.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY `order` ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isEnabled = 1 ORDER BY `order` ASC")
    fun getEnabledHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE isCustom = 1 ORDER BY `order` ASC")
    fun getCustomHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): HabitEntity?

    @Query("SELECT COUNT(*) FROM habits WHERE isCustom = 1")
    suspend fun getCustomHabitCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isEnabled = :enabled WHERE id = :habitId")
    suspend fun setHabitEnabled(habitId: String, enabled: Boolean)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: String)

    @Query("DELETE FROM habits WHERE isCustom = 1")
    suspend fun deleteAllCustomHabits()
}
