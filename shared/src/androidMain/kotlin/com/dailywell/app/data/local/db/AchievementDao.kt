package com.dailywell.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievement(id: String): AchievementEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM achievements WHERE id = :id)")
    suspend fun hasAchievement(id: String): Boolean

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getAchievementCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()
}
