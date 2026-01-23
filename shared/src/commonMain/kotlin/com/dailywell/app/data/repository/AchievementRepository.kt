package com.dailywell.app.data.repository

import com.dailywell.app.data.model.Achievement
import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    fun getAllAchievements(): Flow<List<Achievement>>
    fun getUnlockedAchievements(): Flow<List<Achievement>>
    fun getUnlockedAchievementIds(): Flow<Set<String>>
    fun getRecentlyUnlocked(): Flow<Achievement?>
    suspend fun unlockAchievement(achievementId: String, habitId: String? = null)
    suspend fun hasAchievement(achievementId: String): Boolean
    suspend fun checkAndUnlockStreakAchievements(currentStreak: Int): Achievement?
    suspend fun checkAndUnlockHabitAchievements(habitId: String, consecutiveDays: Int): Achievement?
    suspend fun checkPerfectDayAchievement(allCompleted: Boolean): Achievement?
    suspend fun checkConsistencyAchievements(weeklyRate: Float, monthlyRate: Float): Achievement?
    suspend fun getUnlockedCount(): Int
    suspend fun getTotalCount(): Int
    suspend fun clearRecentlyUnlocked()
}
