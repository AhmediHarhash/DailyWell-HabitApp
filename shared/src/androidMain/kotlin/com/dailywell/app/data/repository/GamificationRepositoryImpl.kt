package com.dailywell.app.data.repository

import com.dailywell.app.api.FirebaseService
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.*

/**
 * Implementation of GamificationRepository with Firebase cloud sync
 */
class GamificationRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val firebaseService: FirebaseService
) : GamificationRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val GAMIFICATION_KEY = "gamification_data"
        private const val UNLOCKED_BADGES_KEY = "unlocked_badges"
        private const val LAST_SPIN_KEY = "last_spin_result"
    }

    private fun getGamificationDataFlow(): Flow<GamificationData> {
        return dataStoreManager.getString(GAMIFICATION_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<GamificationData>(it)
                } catch (e: Exception) {
                    GamificationData()
                }
            } ?: GamificationData()
        }
    }

    private suspend fun updateGamificationData(transform: (GamificationData) -> GamificationData, syncToCloud: Boolean = true) {
        val currentData = getGamificationDataFlow().first()
        val updatedData = transform(currentData)
        dataStoreManager.putString(GAMIFICATION_KEY, json.encodeToString(updatedData))

        // Sync key progress metrics to Firebase
        if (syncToCloud) {
            syncProgressToFirebase(updatedData)
        }
    }

    private suspend fun syncProgressToFirebase(data: GamificationData) {
        val userId = firebaseService.getCurrentUserId() ?: return
        try {
            firebaseService.syncUserProgress(
                userId = userId,
                totalXp = data.lifetimeXp,
                currentLevel = data.currentLevel,
                currentStreak = data.currentStreak,
                longestStreak = data.longestStreak,
                habitsCompleted = data.totalHabitsCompleted.toInt()
            )
        } catch (e: Exception) {
            // Continue with local data if Firebase sync fails
        }
    }

    private fun getUnlockedBadgesFlow(): Flow<List<UnlockedBadge>> {
        return dataStoreManager.getString(UNLOCKED_BADGES_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<List<UnlockedBadge>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    }

    private suspend fun updateUnlockedBadges(transform: (List<UnlockedBadge>) -> List<UnlockedBadge>) {
        val currentBadges = getUnlockedBadgesFlow().first()
        val updatedBadges = transform(currentBadges)
        dataStoreManager.putString(UNLOCKED_BADGES_KEY, json.encodeToString(updatedBadges))
    }

    // ============== XP & LEVELS ==============

    override fun getGamificationData(): Flow<GamificationData> = getGamificationDataFlow()

    override suspend fun addXp(amount: Long, reason: XpReason, habitId: String?): XpTransaction {
        val now = Clock.System.now().toString()
        val transaction = XpTransaction(
            id = "xp_${Clock.System.now().toEpochMilliseconds()}",
            amount = amount,
            reason = reason,
            habitId = habitId,
            timestamp = now
        )

        updateGamificationData(transform = { data ->
            val newTotalXp = data.totalXp + amount
            val newLevel = LevelSystem.levelForXp(newTotalXp)
            val leveledUp = newLevel > data.currentLevel

            data.copy(
                totalXp = newTotalXp,
                lifetimeXp = data.lifetimeXp + amount,
                dailyXp = data.dailyXp + amount,
                weeklyXp = data.weeklyXp + amount,
                monthlyXp = data.monthlyXp + amount,
                currentLevel = newLevel,
                xpToNextLevel = LevelSystem.xpToNextLevel(newTotalXp)
            )
        })

        // Check for level-up badge unlocks
        checkAndUnlockBadges()

        return transaction
    }

    override suspend fun getCurrentLevel(): Int {
        return getGamificationDataFlow().first().currentLevel
    }

    override suspend fun getXpProgress(): Float {
        val data = getGamificationDataFlow().first()
        return LevelSystem.progressToNextLevel(data.totalXp)
    }

    // ============== BADGES ==============

    override fun getUnlockedBadges(): Flow<List<UnlockedBadge>> = getUnlockedBadgesFlow()

    override fun getAllBadgesWithStatus(): Flow<List<Pair<Badge, Boolean>>> {
        return getUnlockedBadgesFlow().map { unlockedBadges ->
            val unlockedIds = unlockedBadges.map { it.badgeId }.toSet()
            BadgeLibrary.allBadges.map { badge ->
                badge to unlockedIds.contains(badge.id)
            }
        }
    }

    override suspend fun checkAndUnlockBadges(): List<Badge> {
        val data = getGamificationDataFlow().first()
        val unlockedBadges = getUnlockedBadgesFlow().first()
        val unlockedIds = unlockedBadges.map { it.badgeId }.toSet()
        val newlyUnlocked = mutableListOf<Badge>()

        for (badge in BadgeLibrary.allBadges) {
            if (badge.id in unlockedIds) continue

            val shouldUnlock = when (val req = badge.requirement) {
                is BadgeRequirement.StreakDays -> data.currentStreak >= req.days
                is BadgeRequirement.TotalHabits -> data.totalHabitsCompleted >= req.count
                is BadgeRequirement.PerfectDays -> data.perfectDays >= req.count
                is BadgeRequirement.PerfectWeeks -> data.perfectWeeks >= req.count
                is BadgeRequirement.Level -> data.currentLevel >= req.level
                is BadgeRequirement.XpEarned -> data.lifetimeXp >= req.xp
                is BadgeRequirement.ChallengesWon -> data.challengesCompleted >= req.count
                is BadgeRequirement.DuelsWon -> data.duelsWon >= req.count
                is BadgeRequirement.FriendsHelped -> data.friendsHelped >= req.count
                is BadgeRequirement.DailyLoginStreak -> data.dailyRewardStreak >= req.days
                is BadgeRequirement.FirstHabit -> data.totalHabitsCompleted >= 1
                is BadgeRequirement.FirstPerfectDay -> data.perfectDays >= 1
                is BadgeRequirement.FirstChallenge -> data.challengesCompleted >= 1
                is BadgeRequirement.FirstDuel -> data.duelsWon >= 1
                else -> false
            }

            if (shouldUnlock) {
                unlockBadge(badge.id)?.let {
                    newlyUnlocked.add(badge)
                }
            }
        }

        return newlyUnlocked
    }

    override suspend fun unlockBadge(badgeId: String): UnlockedBadge? {
        val badge = BadgeLibrary.getBadgeById(badgeId) ?: return null
        val now = Clock.System.now().toString()

        val unlockedBadge = UnlockedBadge(
            badgeId = badgeId,
            unlockedAt = now,
            xpAwarded = badge.xpReward
        )

        updateUnlockedBadges { badges ->
            if (badges.any { it.badgeId == badgeId }) {
                badges
            } else {
                badges + unlockedBadge
            }
        }

        // Award XP for badge
        addXp(badge.xpReward, XpReason.ACHIEVEMENT_UNLOCKED)

        return unlockedBadge
    }

    // ============== DAILY REWARDS ==============

    override suspend fun claimDailyReward(): DailyReward? {
        if (!canClaimDailyReward()) return null

        val data = getGamificationDataFlow().first()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

        val newStreak = if (isConsecutiveDay(data.lastDailyRewardDate, today)) {
            data.dailyRewardStreak + 1
        } else {
            1
        }

        val reward = DailyRewardSchedule.getRewardForDay(newStreak)

        updateGamificationData(transform = { d ->
            var updated = d.copy(
                lastDailyRewardDate = today,
                dailyRewardStreak = newStreak
            )

            // Apply bonus rewards
            when (val bonus = reward.bonusReward) {
                is DailyBonusReward.StreakShield -> {
                    updated = updated.copy(streakShields = updated.streakShields + bonus.count)
                }
                else -> { /* Handle other bonuses */ }
            }

            updated
        })

        // Award XP
        addXp(reward.xpReward, XpReason.DAILY_LOGIN)

        return reward
    }

    override suspend fun canClaimDailyReward(): Boolean {
        val data = getGamificationDataFlow().first()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        return data.lastDailyRewardDate != today
    }

    override fun getDailyRewardStreak(): Flow<Int> {
        return getGamificationDataFlow().map { it.dailyRewardStreak }
    }

    // ============== SPIN WHEEL ==============

    override suspend fun spin(): SpinWheelResult? {
        if (!canSpin()) return null

        val reward = SpinWheel.spin()
        val now = Clock.System.now().toString()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

        val result = SpinWheelResult(reward, now)

        // Save last spin
        dataStoreManager.putString(LAST_SPIN_KEY, json.encodeToString(result))

        // Update spin tracking
        updateGamificationData(transform = { data ->
            var updated = data.copy(
                lastSpinDate = today,
                totalSpins = data.totalSpins + 1
            )

            // Apply rewards
            when (reward) {
                is SpinReward.Xp -> { /* XP added separately */ }
                is SpinReward.StreakShields -> {
                    updated = updated.copy(streakShields = updated.streakShields + reward.count)
                }
                is SpinReward.ThemeTicket -> {
                    // Unlock a random locked theme
                    val lockedThemes = ThemeLibrary.allThemes.filter { theme ->
                        !theme.isUnlockedByDefault && !updated.unlockedThemes.contains(theme.id)
                    }
                    if (lockedThemes.isNotEmpty()) {
                        val randomTheme = lockedThemes.random()
                        updated = updated.copy(
                            unlockedThemes = updated.unlockedThemes + randomTheme.id
                        )
                    }
                }
                else -> { }
            }

            updated
        })

        // Award XP if applicable
        if (reward is SpinReward.Xp) {
            addXp(reward.amount, XpReason.DAILY_SPIN)
        }

        return result
    }

    override suspend fun canSpin(): Boolean {
        val data = getGamificationDataFlow().first()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        return data.lastSpinDate != today
    }

    override fun getLastSpinResult(): Flow<SpinWheelResult?> {
        return dataStoreManager.getString(LAST_SPIN_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<SpinWheelResult>(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    // ============== STREAK SHIELDS ==============

    override suspend fun useStreakShield(): Boolean {
        val data = getGamificationDataFlow().first()
        if (data.streakShields <= 0) return false

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

        updateGamificationData(transform = { d ->
            d.copy(
                streakShields = d.streakShields - 1,
                streakShieldsUsed = d.streakShieldsUsed + 1,
                lastShieldUsedDate = today
            )
        })

        return true
    }

    override fun getAvailableShields(): Flow<Int> {
        return getGamificationDataFlow().map { it.streakShields }
    }

    override suspend fun addStreakShield(count: Int) {
        updateGamificationData(transform = { data ->
            data.copy(streakShields = data.streakShields + count)
        })
    }

    // ============== THEMES ==============

    override fun getUnlockedThemes(): Flow<List<AppTheme>> {
        return getGamificationDataFlow().map { data ->
            getUnlockedBadgesFlow().first().let { badges ->
                val badgeIds = badges.map { it.badgeId }.toSet()
                ThemeLibrary.getUnlockedThemes(data.currentLevel, badgeIds, data.unlockedThemes)
            }
        }
    }

    override fun getSelectedTheme(): Flow<AppTheme> {
        return getGamificationDataFlow().map { data ->
            ThemeLibrary.getThemeById(data.selectedTheme) ?: ThemeLibrary.allThemes.first()
        }
    }

    override suspend fun selectTheme(themeId: String): Boolean {
        val data = getGamificationDataFlow().first()
        val unlockedThemes = getUnlockedThemes().first()

        if (unlockedThemes.none { it.id == themeId }) return false

        updateGamificationData(transform = { d ->
            d.copy(selectedTheme = themeId)
        })

        return true
    }

    override suspend fun unlockTheme(themeId: String): Boolean {
        val theme = ThemeLibrary.getThemeById(themeId) ?: return false

        updateGamificationData(transform = { data ->
            data.copy(unlockedThemes = data.unlockedThemes + themeId)
        })

        return true
    }

    // ============== STATS TRACKING ==============

    override suspend fun recordHabitCompletion(
        habitId: String,
        isAllCompleted: Boolean,
        isEarlyBird: Boolean,
        isMorning: Boolean
    ) {
        updateGamificationData(transform = { data ->
            data.copy(totalHabitsCompleted = data.totalHabitsCompleted + 1)
        })

        // Base XP
        addXp(XpValues.HABIT_COMPLETED, XpReason.HABIT_COMPLETED, habitId)

        // Bonus for completing all habits
        if (isAllCompleted) {
            addXp(XpValues.ALL_HABITS_BONUS, XpReason.ALL_HABITS_COMPLETED)
        }

        // Early bird bonus (before 9 AM)
        if (isEarlyBird) {
            addXp(XpValues.EARLY_BIRD_BONUS, XpReason.EARLY_BIRD)
        } else if (isMorning) {
            // Morning champion (before noon)
            addXp(XpValues.MORNING_CHAMPION_BONUS, XpReason.MORNING_CHAMPION)
        }

        // Check for new badges
        checkAndUnlockBadges()
    }

    override suspend fun recordPerfectDay() {
        updateGamificationData(transform = { data ->
            data.copy(perfectDays = data.perfectDays + 1)
        })
        checkAndUnlockBadges()
    }

    override suspend fun recordPerfectWeek() {
        updateGamificationData(transform = { data ->
            data.copy(perfectWeeks = data.perfectWeeks + 1)
        })
        addXp(XpValues.PERFECT_WEEK + XpValues.PERFECT_WEEK_BONUS, XpReason.PERFECT_WEEK)
        checkAndUnlockBadges()
    }

    override suspend fun recordLogin() {
        claimDailyReward()
    }

    override suspend fun updateStreak(currentStreak: Int, longestStreak: Int) {
        updateGamificationData(transform = { data ->
            data.copy(
                currentStreak = currentStreak,
                longestStreak = maxOf(data.longestStreak, longestStreak)
            )
        })

        // Streak XP bonus
        if (currentStreak > 0) {
            val streakBonus = XpValues.STREAK_MULTIPLIER * currentStreak
            addXp(streakBonus, XpReason.STREAK_BONUS)
        }

        checkAndUnlockBadges()
    }

    override suspend fun recordChallengeWin() {
        updateGamificationData(transform = { data ->
            data.copy(challengesCompleted = data.challengesCompleted + 1)
        })
        addXp(XpValues.CHALLENGE_WIN, XpReason.CHALLENGE_WIN)
        checkAndUnlockBadges()
    }

    override suspend fun recordDuelWin() {
        updateGamificationData(transform = { data ->
            data.copy(duelsWon = data.duelsWon + 1)
        })
        addXp(XpValues.DUEL_WIN, XpReason.DUEL_WIN)
        checkAndUnlockBadges()
    }

    override suspend fun recordFriendHelped() {
        updateGamificationData(transform = { data ->
            data.copy(friendsHelped = data.friendsHelped + 1)
        })
        addXp(XpValues.FRIEND_HELPED, XpReason.FRIEND_HELPED)
        checkAndUnlockBadges()
    }

    // ============== LEADERBOARD ==============

    override suspend fun getLeaderboardStats(): LeaderboardStats {
        val data = getGamificationDataFlow().first()
        return LeaderboardStats(
            totalXp = data.lifetimeXp,
            weeklyXp = data.weeklyXp,
            monthlyXp = data.monthlyXp,
            level = data.currentLevel,
            currentStreak = data.currentStreak,
            perfectDays = data.perfectDays,
            habitsCompleted = data.totalHabitsCompleted
        )
    }

    // ============== RESETS ==============

    override suspend fun resetDailyXp() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        updateGamificationData(transform = { data ->
            if (data.lastXpResetDate != today) {
                data.copy(dailyXp = 0, lastXpResetDate = today)
            } else {
                data
            }
        })
    }

    override suspend fun resetWeeklyXp() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weekStart = today.minus(today.dayOfWeek.ordinal, DateTimeUnit.DAY).toString()

        updateGamificationData(transform = { data ->
            if (data.lastWeeklyResetDate != weekStart) {
                data.copy(weeklyXp = 0, lastWeeklyResetDate = weekStart)
            } else {
                data
            }
        })
    }

    override suspend fun resetMonthlyXp() {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val monthStart = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}-01"

        updateGamificationData(transform = { data ->
            if (data.lastMonthlyResetDate != monthStart) {
                data.copy(monthlyXp = 0, lastMonthlyResetDate = monthStart)
            } else {
                data
            }
        })
    }

    // ============== HELPERS ==============

    private fun isConsecutiveDay(lastDate: String?, today: String): Boolean {
        if (lastDate == null) return false
        return try {
            val last = LocalDate.parse(lastDate)
            val current = LocalDate.parse(today)
            val daysDiff = current.toEpochDays() - last.toEpochDays()
            daysDiff == 1
        } catch (e: Exception) {
            false
        }
    }

    // ============== FIREBASE SYNC ==============

    /**
     * Sync current gamification data to Firebase cloud
     */
    suspend fun syncToCloud() {
        val data = getGamificationDataFlow().first()
        syncProgressToFirebase(data)
    }

    /**
     * Get friends leaderboard from Firebase
     */
    suspend fun getFriendsLeaderboard(): List<com.dailywell.app.data.model.LeaderboardEntry> {
        val userId = firebaseService.getCurrentUserId() ?: return emptyList()
        return try {
            val firebaseEntries = firebaseService.getFriendsLeaderboard(userId)
            firebaseEntries.map { entry ->
                com.dailywell.app.data.model.LeaderboardEntry(
                    odId = entry.userId,
                    userId = entry.userId,
                    displayName = entry.displayName,
                    avatarEmoji = entry.profileEmoji,
                    level = entry.currentLevel,
                    rank = entry.rank,
                    previousRank = null,
                    score = entry.totalXp,
                    streak = entry.currentStreak,
                    perfectDays = 0,
                    isFriend = true,
                    isCurrentUser = entry.userId == userId,
                    updatedAt = Clock.System.now().toString()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Force sync unlocked badges to Firebase
     */
    suspend fun syncBadgesToCloud() {
        val userId = firebaseService.getCurrentUserId() ?: return
        val badges = getUnlockedBadgesFlow().first()
        try {
            firebaseService.syncUserBadges(userId, badges.map { it.badgeId })
        } catch (e: Exception) {
            // Continue with local data if sync fails
        }
    }
}
