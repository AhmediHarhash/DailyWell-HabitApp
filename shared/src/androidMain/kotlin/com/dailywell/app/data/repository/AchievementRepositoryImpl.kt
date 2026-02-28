package com.dailywell.app.data.repository

import com.dailywell.app.data.content.AchievementsDatabase
import com.dailywell.app.data.local.db.AchievementDao
import com.dailywell.app.data.local.db.AchievementEntity
import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.AchievementCategory
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * AchievementRepositoryImpl - Production-Ready Achievement System
 *
 * Hybrid storage approach:
 * - Room (AchievementDao) for fast offline access
 * - Firebase Firestore for cloud backup and sync
 *
 * Features:
 * - 75 creative achievements from AchievementsDatabase
 * - Real-time progress tracking
 * - Automatic cloud sync
 * - Celebration system for newly unlocked achievements
 */
class AchievementRepositoryImpl(
    private val achievementDao: AchievementDao
) : AchievementRepository {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Recently unlocked for celebration
    private val _recentlyUnlocked = MutableStateFlow<AchievementCelebration?>(null)
    private val _pendingCelebrations = MutableStateFlow<List<AchievementCelebration>>(emptyList())

    // Progress cache for achievements not yet unlocked
    private val progressCache = MutableStateFlow<Map<String, Int>>(emptyMap())

    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    private val achievementsCollection
        get() = firestore.collection("users").document(userId).collection("achievements")

    private val progressCollection
        get() = firestore.collection("users").document(userId).collection("achievement_progress")

    init {
        // Initial sync with cloud on repository creation
        repositoryScope.launch {
            try {
                syncWithCloud()
                loadProgressFromCloud()
            } catch (e: Exception) {
                // Silent fail - will work offline
            }
        }
    }

    // ==================== CORE ACHIEVEMENT DATA ====================

    override fun getAllAchievements(): Flow<List<AchievementWithProgress>> {
        return combine(
            achievementDao.getAllAchievements(),
            progressCache
        ) { entities, progress ->
            val unlockedIds = entities.associate { it.id to it.unlockedAt }

            AchievementsDatabase.all.map { achievement ->
                val isUnlocked = unlockedIds.containsKey(achievement.id)
                val unlockedAt = unlockedIds[achievement.id]
                val currentProgress = if (isUnlocked) achievement.target else (progress[achievement.id] ?: 0)

                AchievementWithProgress(
                    achievement = achievement,
                    isUnlocked = isUnlocked,
                    unlockedAt = unlockedAt,
                    currentProgress = currentProgress,
                    targetProgress = achievement.target,
                    progressPercent = if (achievement.target > 0) {
                        (currentProgress.toFloat() / achievement.target).coerceIn(0f, 1f)
                    } else 0f
                )
            }
        }
    }

    override fun getUnlockedAchievements(): Flow<List<Achievement>> {
        return achievementDao.getAllAchievements().map { entities ->
            entities.mapNotNull { entity ->
                AchievementsDatabase.getById(entity.id)?.copy(
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

    override fun getAchievementsByCategory(category: AchievementCategory): Flow<List<AchievementWithProgress>> {
        return getAllAchievements().map { achievements ->
            achievements.filter { it.achievement.category == category }
        }
    }

    override suspend fun getAchievement(achievementId: String): AchievementWithProgress? {
        val achievement = AchievementsDatabase.getById(achievementId) ?: return null
        val entity = achievementDao.getAchievement(achievementId)
        val progress = progressCache.value[achievementId] ?: 0

        return AchievementWithProgress(
            achievement = achievement,
            isUnlocked = entity != null,
            unlockedAt = entity?.unlockedAt,
            currentProgress = if (entity != null) achievement.target else progress,
            targetProgress = achievement.target,
            progressPercent = if (achievement.target > 0) {
                (if (entity != null) 1f else (progress.toFloat() / achievement.target).coerceIn(0f, 1f))
            } else 0f
        )
    }

    // ==================== ACHIEVEMENT UNLOCKING ====================

    override suspend fun unlockAchievement(
        achievementId: String,
        habitId: String?
    ): AchievementCelebration? {
        // Check if already unlocked
        if (achievementDao.hasAchievement(achievementId)) {
            return null
        }

        val achievement = AchievementsDatabase.getById(achievementId) ?: return null
        val timestamp = System.currentTimeMillis()

        // Save to local Room database
        achievementDao.insertAchievement(
            AchievementEntity(
                id = achievementId,
                unlockedAt = timestamp,
                habitId = habitId
            )
        )

        // Sync to Firebase
        repositoryScope.launch {
            try {
                achievementsCollection.document(achievementId).set(
                    mapOf(
                        "id" to achievementId,
                        "unlockedAt" to timestamp,
                        "habitId" to habitId,
                        "name" to achievement.name,
                        "emoji" to achievement.emoji
                    )
                )
            } catch (e: Exception) {
                // Will sync later
            }
        }

        // Create celebration
        val isRare = achievement.target >= 90 // Rare if 90+ days required
        val celebration = AchievementCelebration(
            achievement = achievement.copy(isUnlocked = true, unlockedAt = timestamp),
            unlockedAt = timestamp,
            isRare = isRare,
            shareMessage = buildShareMessage(achievement)
        )

        // Add to pending celebrations
        _pendingCelebrations.update { it + celebration }
        _recentlyUnlocked.value = celebration

        return celebration
    }

    override suspend fun hasAchievement(achievementId: String): Boolean {
        return achievementDao.hasAchievement(achievementId)
    }

    // ==================== AUTOMATIC ACHIEVEMENT TRIGGERS ====================

    override suspend fun checkAndUnlockStreakAchievements(currentStreak: Int): List<AchievementCelebration> {
        val celebrations = mutableListOf<AchievementCelebration>()

        AchievementsDatabase.streakMilestones.forEach { (milestone, achievement) ->
            if (currentStreak >= milestone && !hasAchievement(achievement.id)) {
                unlockAchievement(achievement.id)?.let { celebrations.add(it) }
            }
        }

        // Update progress for next streak achievement
        val nextMilestone = AchievementsDatabase.getNextStreakMilestone(currentStreak)
        if (nextMilestone != null) {
            updateAchievementProgress(nextMilestone.second.id, currentStreak)
        }

        return celebrations
    }

    override suspend fun checkAndUnlockHabitAchievements(
        habitId: String,
        consecutiveDays: Int
    ): List<AchievementCelebration> {
        val celebrations = mutableListOf<AchievementCelebration>()

        AchievementsDatabase.habitMilestones[habitId]?.forEach { (milestone, achievement) ->
            if (consecutiveDays >= milestone && !hasAchievement(achievement.id)) {
                unlockAchievement(achievement.id, habitId)?.let { celebrations.add(it) }
            }
        }

        // Update progress for habit achievements
        val habitAchievements = AchievementsDatabase.habitMilestones[habitId]
        habitAchievements?.forEach { (_, achievement) ->
            if (!hasAchievement(achievement.id)) {
                updateAchievementProgress(achievement.id, consecutiveDays)
            }
        }

        return celebrations
    }

    override suspend fun checkPerfectDayAchievement(
        allCompleted: Boolean,
        consecutivePerfectDays: Int
    ): AchievementCelebration? {
        if (!allCompleted) return null

        AchievementsDatabase.perfectDayMilestones.forEach { (milestone, achievement) ->
            if (consecutivePerfectDays >= milestone && !hasAchievement(achievement.id)) {
                return unlockAchievement(achievement.id)
            }
        }

        // Update progress for next perfect day achievement
        val nextMilestone = AchievementsDatabase.perfectDayMilestones.entries
            .filter { it.key > consecutivePerfectDays }
            .minByOrNull { it.key }

        if (nextMilestone != null) {
            updateAchievementProgress(nextMilestone.value.id, consecutivePerfectDays)
        }

        return null
    }

    override suspend fun checkConsistencyAchievements(
        weeklyRate: Float,
        monthlyRate: Float
    ): AchievementCelebration? {
        if (weeklyRate >= 0.8f && !hasAchievement("consistency_80_week")) {
            return unlockAchievement("consistency_80_week")
        }
        if (monthlyRate >= 0.8f && !hasAchievement("consistency_80_month")) {
            return unlockAchievement("consistency_80_month")
        }
        return null
    }

    override suspend fun checkTotalEntriesAchievements(totalEntries: Int): AchievementCelebration? {
        AchievementsDatabase.totalEntriesMilestones.forEach { (milestone, achievement) ->
            if (totalEntries >= milestone && !hasAchievement(achievement.id)) {
                return unlockAchievement(achievement.id)
            }
        }

        // Update progress
        AchievementsDatabase.totalEntriesMilestones.forEach { (_, achievement) ->
            if (!hasAchievement(achievement.id)) {
                updateAchievementProgress(achievement.id, totalEntries)
            }
        }

        return null
    }

    override suspend fun checkTimeBasedAchievements(
        checkInHour: Int,
        consecutiveEarlyDays: Int,
        consecutiveLateDays: Int
    ): AchievementCelebration? {
        // Early bird (before 7 AM)
        if (checkInHour < 7) {
            if (consecutiveEarlyDays >= 7 && !hasAchievement("early_bird_7")) {
                return unlockAchievement("early_bird_7")
            }
            if (consecutiveEarlyDays >= 30 && !hasAchievement("early_bird_30")) {
                return unlockAchievement("early_bird_30")
            }
            updateAchievementProgress("early_bird_7", consecutiveEarlyDays)
            updateAchievementProgress("early_bird_30", consecutiveEarlyDays)
        }

        // Night owl (after 10 PM)
        if (checkInHour >= 22) {
            if (consecutiveLateDays >= 7 && !hasAchievement("night_owl_7")) {
                return unlockAchievement("night_owl_7")
            }
            if (consecutiveLateDays >= 30 && !hasAchievement("night_owl_30")) {
                return unlockAchievement("night_owl_30")
            }
            updateAchievementProgress("night_owl_7", consecutiveLateDays)
            updateAchievementProgress("night_owl_30", consecutiveLateDays)
        }

        return null
    }

    override suspend fun checkComebackAchievement(daysMissed: Int): AchievementCelebration? {
        return when {
            daysMissed >= 30 && !hasAchievement("comeback_30") -> unlockAchievement("comeback_30")
            daysMissed >= 7 && !hasAchievement("comeback_7") -> unlockAchievement("comeback_7")
            daysMissed >= 3 && !hasAchievement("comeback_3") -> unlockAchievement("comeback_3")
            else -> null
        }
    }

    override suspend fun checkSpecialDateAchievements(
        month: Int,
        dayOfMonth: Int
    ): AchievementCelebration? {
        // New Year's Day
        if (month == 1 && dayOfMonth == 1 && !hasAchievement("new_year_new_you")) {
            return unlockAchievement("new_year_new_you")
        }

        // Could add more holiday checks here
        return null
    }

    // ==================== PROGRESS TRACKING ====================

    override suspend fun updateAchievementProgress(achievementId: String, progress: Int) {
        val currentProgress = progressCache.value.toMutableMap()
        currentProgress[achievementId] = progress
        progressCache.value = currentProgress

        // Sync to Firebase
        repositoryScope.launch {
            try {
                progressCollection.document(achievementId).set(
                    mapOf(
                        "achievementId" to achievementId,
                        "progress" to progress,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                // Will sync later
            }
        }
    }

    override suspend fun incrementAchievementProgress(achievementId: String) {
        val current = progressCache.value[achievementId] ?: 0
        updateAchievementProgress(achievementId, current + 1)
    }

    override suspend fun getAchievementProgress(achievementId: String): Int {
        return progressCache.value[achievementId] ?: 0
    }

    private suspend fun loadProgressFromCloud() {
        try {
            val snapshot = progressCollection.get()
            val progressMap = mutableMapOf<String, Int>()

            snapshot.documents.forEach { doc ->
                val id = doc.get<String>("achievementId")
                val progress = doc.get<Long>("progress").toInt()
                progressMap[id] = progress
            }

            progressCache.value = progressMap
        } catch (e: Exception) {
            // Use cached values
        }
    }

    // ==================== CELEBRATION SYSTEM ====================

    override fun getRecentlyUnlocked(): Flow<AchievementCelebration?> = _recentlyUnlocked.asStateFlow()

    override suspend fun clearRecentlyUnlocked() {
        _recentlyUnlocked.value = null
    }

    override fun getPendingCelebrations(): Flow<List<AchievementCelebration>> = _pendingCelebrations.asStateFlow()

    override suspend fun dismissCelebration(achievementId: String) {
        _pendingCelebrations.update { celebrations ->
            celebrations.filter { it.achievement.id != achievementId }
        }
        if (_recentlyUnlocked.value?.achievement?.id == achievementId) {
            _recentlyUnlocked.value = null
        }
    }

    // ==================== STATISTICS ====================

    override suspend fun getAchievementStats(): AchievementStats {
        val unlockedEntities = achievementDao.getAllAchievements().first()
        val unlockedIds = unlockedEntities.map { it.id }.toSet()
        val allAchievements = AchievementsDatabase.all

        // Category breakdown
        val categoryBreakdown = AchievementCategory.values().associate { category ->
            val categoryAchievements = allAchievements.filter { it.category == category }
            val unlockedInCategory = categoryAchievements.count { it.id in unlockedIds }
            category to (unlockedInCategory to categoryAchievements.size)
        }

        // Recent unlocks (last 5)
        val recentUnlocks = unlockedEntities
            .sortedByDescending { it.unlockedAt }
            .take(5)
            .mapNotNull { entity -> AchievementsDatabase.getById(entity.id) }

        // Next achievements to unlock (closest to completion)
        val nextAchievements = allAchievements
            .filter { it.id !in unlockedIds }
            .map { achievement ->
                val progress = progressCache.value[achievement.id] ?: 0
                AchievementWithProgress(
                    achievement = achievement,
                    isUnlocked = false,
                    currentProgress = progress,
                    targetProgress = achievement.target,
                    progressPercent = if (achievement.target > 0) {
                        (progress.toFloat() / achievement.target).coerceIn(0f, 1f)
                    } else 0f
                )
            }
            .sortedByDescending { it.progressPercent }
            .take(3)

        // Rare achievements (90+ days required)
        val rareAchievements = allAchievements
            .filter { it.target >= 90 && it.id in unlockedIds }

        return AchievementStats(
            totalAchievements = allAchievements.size,
            unlockedCount = unlockedIds.size,
            progressPercent = unlockedIds.size.toFloat() / allAchievements.size,
            categoryBreakdown = categoryBreakdown,
            recentUnlocks = recentUnlocks,
            nextAchievements = nextAchievements,
            rareAchievements = rareAchievements
        )
    }

    override suspend fun getUnlockedCount(): Int {
        return achievementDao.getAchievementCount()
    }

    override fun getTotalCount(): Int {
        return AchievementsDatabase.totalCount
    }

    override suspend fun getNextAchievements(limit: Int): List<AchievementWithProgress> {
        val unlockedIds = achievementDao.getAllAchievements().first().map { it.id }.toSet()

        return AchievementsDatabase.all
            .filter { it.id !in unlockedIds }
            .map { achievement ->
                val progress = progressCache.value[achievement.id] ?: 0
                AchievementWithProgress(
                    achievement = achievement,
                    isUnlocked = false,
                    currentProgress = progress,
                    targetProgress = achievement.target,
                    progressPercent = if (achievement.target > 0) {
                        (progress.toFloat() / achievement.target).coerceIn(0f, 1f)
                    } else 0f
                )
            }
            .sortedByDescending { it.progressPercent }
            .take(limit)
    }

    // ==================== SYNC & BACKUP ====================

    override suspend fun syncWithCloud() {
        try {
            // Get local achievements
            val localAchievements = achievementDao.getAllAchievements().first()

            // Get cloud achievements
            val cloudSnapshot = achievementsCollection.get()
            val cloudAchievementIds = cloudSnapshot.documents.map { it.id }.toSet()

            // Upload local achievements that aren't in cloud
            localAchievements.forEach { entity ->
                if (entity.id !in cloudAchievementIds) {
                    val achievement = AchievementsDatabase.getById(entity.id)
                    achievementsCollection.document(entity.id).set(
                        mapOf(
                            "id" to entity.id,
                            "unlockedAt" to entity.unlockedAt,
                            "habitId" to entity.habitId,
                            "name" to (achievement?.name ?: "Unknown"),
                            "emoji" to (achievement?.emoji ?: "?")
                        )
                    )
                }
            }

            // Download cloud achievements that aren't local
            val localIds = localAchievements.map { it.id }.toSet()
            cloudSnapshot.documents.forEach { doc ->
                if (doc.id !in localIds) {
                    val unlockedAt = doc.get<Long>("unlockedAt")
                    val habitId = try { doc.get<String?>("habitId") } catch (e: Exception) { null }
                    achievementDao.insertAchievement(
                        AchievementEntity(
                            id = doc.id,
                            unlockedAt = unlockedAt,
                            habitId = habitId
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Silent fail - works offline
        }
    }

    override suspend fun restoreFromCloud(): Int {
        var restored = 0
        try {
            val cloudSnapshot = achievementsCollection.get()
            val localIds = achievementDao.getAllAchievements().first().map { it.id }.toSet()

            cloudSnapshot.documents.forEach { doc ->
                if (doc.id !in localIds) {
                    val unlockedAt = doc.get<Long>("unlockedAt")
                    val habitId = try { doc.get<String?>("habitId") } catch (e: Exception) { null }
                    achievementDao.insertAchievement(
                        AchievementEntity(
                            id = doc.id,
                            unlockedAt = unlockedAt,
                            habitId = habitId
                        )
                    )
                    restored++
                }
            }
        } catch (e: Exception) {
            // Failed to restore
        }
        return restored
    }

    override suspend fun exportAchievements(): String {
        val unlockedEntities = achievementDao.getAllAchievements().first()
        val exportData = unlockedEntities.map { entity ->
            val achievement = AchievementsDatabase.getById(entity.id)
            mapOf(
                "id" to entity.id,
                "name" to (achievement?.name ?: "Unknown"),
                "description" to (achievement?.description ?: ""),
                "emoji" to (achievement?.emoji ?: "?"),
                "unlockedAt" to entity.unlockedAt,
                "habitId" to entity.habitId
            )
        }

        return Json.encodeToString(exportData)
    }

    // ==================== HELPER METHODS ====================

    private fun buildShareMessage(achievement: Achievement): String {
        return "${achievement.emoji} I just unlocked \"${achievement.name}\" on DailyWell! ${achievement.description} #DailyWell #Habits #HealthyLiving"
    }
}
