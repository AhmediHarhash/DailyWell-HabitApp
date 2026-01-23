package com.dailywell.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE date = :date")
    fun getEntriesForDate(date: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date = :date")
    suspend fun getEntriesForDateSync(date: String): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getEntriesInRange(startDate: String, endDate: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getEntriesInRangeSync(startDate: String, endDate: String): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE habitId = :habitId ORDER BY date DESC")
    fun getEntriesForHabit(habitId: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate")
    suspend fun getEntriesForHabitInRange(habitId: String, startDate: String, endDate: String): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE date = :date AND habitId = :habitId LIMIT 1")
    suspend fun getEntry(date: String, habitId: String): EntryEntity?

    @Query("SELECT DISTINCT date FROM entries WHERE completed = 1 ORDER BY date DESC")
    fun getCompletedDates(): Flow<List<String>>

    @Query("""
        SELECT date FROM entries
        WHERE completed = 1
        GROUP BY date
        HAVING COUNT(DISTINCT habitId) = (SELECT COUNT(*) FROM habits WHERE isEnabled = 1)
        ORDER BY date DESC
    """)
    suspend fun getPerfectDays(): List<String>

    @Query("SELECT COUNT(*) FROM entries WHERE habitId = :habitId AND completed = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getCompletedCountForHabit(habitId: String, startDate: String, endDate: String): Int

    @Query("SELECT COUNT(DISTINCT date) FROM entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDaysInRange(startDate: String, endDate: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<EntryEntity>)

    @Update
    suspend fun updateEntry(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE date = :date AND habitId = :habitId")
    suspend fun deleteEntry(date: String, habitId: String)

    @Query("DELETE FROM entries WHERE habitId = :habitId")
    suspend fun deleteEntriesForHabit(habitId: String)

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()
}
