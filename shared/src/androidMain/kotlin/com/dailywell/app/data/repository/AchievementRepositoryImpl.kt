package com.dailywell.app.data.repository

import com.dailywell.app.data.local.db.AchievementDao
import com.dailywell.app.data.local.db.AchievementEntity
import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.Achievements
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class AchievementRepositoryImpl(
    private val achievementDao: AchievementDao
) : AchievementRepository {

    private val _recentlyUnlocked = MutableStateFlow<Achievement?>(null)

    override fun getAllAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements().map { entities ->
            val unlockedIds = entities.map { it.id }.toSet()
            Achievements.all.map { achievement ->
                val entity = entities.find { it.id == achievement.id }
                achievement.copy(
                    isUnlocked = unlockedIds.contains(achievement.id),
                    unlockedAt = entity?.unlockedAt
                )
            }
        }
    }

    override fun getUnlockedAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements().map { entities ->
            entities.mapNotNull { entity ->
                Achievements.getById(entity.id)?.copy(
                    isUnlocked = true,
                    unlockedAt = entity.unlockedAt
                )
            }
        }
    }

    override fun getUnlockedAchievementIds(): Flow<Set<String>> {
        return achievementDao.getAllAchievements().map { entities ->
            entities.map { it.id }.toSet()
        }
    }

    override fun getRecentlyUnlocked(): Flow<Achievement?> = _recentlyUnlocked.asStateFlow()

    override suspend fun unlockAchievement(achievementId: String, habitId: String?) {
        if (!achievementDao.hasAchievement(achievementId)) {
            val timestamp = System.currentTimeMillis()
            achievementDao.insertAchievement(
                AchievementEntity(
                    id = achievementId,
                    unlockedAt = timestamp,
                    habitId = habitId
                )
            )
            // Set recently unlocked for celebration
            Achievements.getById(achievementId)?.let { achievement ->
                _recentlyUnlocked.value = achievement.copy(
                    isUnlocked = true,
                    unlockedAt = timestamp
                )
            }
        }
    }

    override suspend fun hasAchievement(achievementId: String): Boolean {
        return achievementDao.hasAchievement(achievementId)
    }

    override suspend fun checkAndUnlockStreakAchievements(currentStreak: Int): Achievement? {
        var newlyUnlocked: Achievement? = null
        Achievements.streakMilestones.forEach { (milestone, achievement) ->
            if (currentStreak >= milestone && !hasAchievement(achievement.id)) {
                unlockAchievement(achievement.id)
                newlyUnlocked = achievement
            }
        }
        return newlyUnlocked
    }

    override suspend fun checkAndUnlockHabitAchievements(habitId: String, consecutiveDays: Int): Achievement? {
        if (consecutiveDays >= 30) {
            val achievement = when (habitId) {
                "sleep" -> Achievements.SLEEP_CHAMPION
                "water" -> Achievements.HYDRATION_HERO
                "move" -> Achievements.MOVEMENT_MASTER
                "vegetables" -> Achievements.VEGGIE_VICTOR
                "calm" -> Achievements.ZEN_MASTER
                "connect" -> Achievements.SOCIAL_BUTTERFLY
                "unplug" -> Achievements.DIGITAL_DETOXER
                else -> null
            }
            achievement?.let {
                if (!hasAchievement(it.id)) {
                    unlockAchievement(it.id, habitId)
                    return it
                }
            }
        }
        return null
    }

    override suspend fun checkPerfectDayAchievement(allCompleted: Boolean): Achievement? {
        if (allCompleted && !hasAchievement(Achievements.PERFECT_WEEK.id)) {
            // For now, just check if all habits were completed today
            // Perfect week tracking would need more complex logic
            return null
        }
        return null
    }

    override suspend fun checkConsistencyAchievements(weeklyRate: Float, monthlyRate: Float): Achievement? {
        // Check for 80%+ consistency achievements
        if (weeklyRate >= 0.8f && !hasAchievement("consistency_starter")) {
            unlockAchievement("consistency_starter")
            return Achievements.all.find { it.id == "consistency_starter" }
        }
        if (monthlyRate >= 0.8f && !hasAchievement("consistency_master")) {
            unlockAchievement("consistency_master")
            return Achievements.all.find { it.id == "consistency_master" }
        }
        return null
    }

    override suspend fun getUnlockedCount(): Int {
        return achievementDao.getAchievementCount()
    }

    override suspend fun getTotalCount(): Int {
        return Achievements.all.size
    }

    override suspend fun clearRecentlyUnlocked() {
        _recentlyUnlocked.value = null
    }
}
