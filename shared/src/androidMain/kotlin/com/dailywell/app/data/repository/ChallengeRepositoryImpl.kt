package com.dailywell.app.data.repository

import com.dailywell.app.api.FirebaseService
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChallengeRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val firebaseService: FirebaseService
) : ChallengeRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // Keys for DataStore
    private object Keys {
        const val ACTIVE_CHALLENGES = "active_challenges"
        const val COMPLETED_CHALLENGES = "completed_challenges"
        const val ACTIVE_DUELS = "active_duels"
        const val DUEL_HISTORY = "duel_history"
        const val DUEL_INVITATIONS = "duel_invitations"
        const val COMMUNITY_PROGRESS = "community_progress"
        const val COMMUNITY_CHALLENGE = "community_challenge"
        const val SEASONAL_PROGRESS = "seasonal_progress"
        const val SEASONAL_EVENT = "seasonal_event"
        const val CUSTOM_CHALLENGES = "custom_challenges"
        const val CHALLENGE_STATS = "challenge_stats"
    }

    // Helper to get current timestamp
    private fun now(): String = Clock.System.now().toString()

    private fun today(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    // ============== SOLO CHALLENGES ==============

    override fun getAvailableSoloChallenges(): Flow<List<Challenge>> {
        return getActiveSoloChallenges().map { active ->
            val activeIds = active.map { it.challenge.id }.toSet()
            ChallengeLibrary.soloChallenges.filter { it.id !in activeIds }
        }
    }

    override fun getActiveSoloChallenges(): Flow<List<ActiveChallenge>> {
        return dataStoreManager.getString(Keys.ACTIVE_CHALLENGES).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<ActiveChallenge>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override fun getCompletedChallenges(): Flow<List<ActiveChallenge>> {
        return dataStoreManager.getString(Keys.COMPLETED_CHALLENGES).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<ActiveChallenge>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override suspend fun joinSoloChallenge(challengeId: String): ActiveChallenge? {
        val challenge = ChallengeLibrary.getSoloChallengeById(challengeId) ?: return null

        val startDate = today()
        val endDate = startDate.plus(DatePeriod(days = challenge.duration.days))

        val targetValue = when (val goal = challenge.goal) {
            is ChallengeGoal.TotalHabits -> goal.count
            is ChallengeGoal.PerfectDays -> goal.days
            is ChallengeGoal.StreakDays -> goal.days
            is ChallengeGoal.SpecificHabit -> goal.count
            is ChallengeGoal.EarlyBird -> goal.days
            is ChallengeGoal.TotalXp -> goal.xp.toInt()
            is ChallengeGoal.ConsecutiveDays -> goal.days
            is ChallengeGoal.MultiGoal -> goal.goals.size * 10 // Simplified
        }

        val progress = UserChallengeProgress(
            odId = "${challengeId}_${now()}",
            challengeId = challengeId,
            targetValue = targetValue,
            lastUpdated = now()
        )

        val activeChallenge = ActiveChallenge(
            challenge = challenge.copy(
                startDate = startDate.toString(),
                endDate = endDate.toString()
            ),
            userProgress = progress
        )

        // Add to active challenges
        val currentActive = getActiveChallengesList()
        val updated = currentActive + activeChallenge
        dataStoreManager.putString(Keys.ACTIVE_CHALLENGES, json.encodeToString(updated))

        return activeChallenge
    }

    override suspend fun updateChallengeProgress(challengeId: String, progress: Int): UserChallengeProgress? {
        val active = getActiveChallengesList()
        val index = active.indexOfFirst { it.challenge.id == challengeId }
        if (index == -1) return null

        val challenge = active[index]
        val updatedProgress = challenge.userProgress.copy(
            currentValue = progress,
            progressPercent = (progress.toFloat() / challenge.userProgress.targetValue).coerceIn(0f, 1f),
            lastUpdated = now(),
            dailyProgress = challenge.userProgress.dailyProgress + (today().toString() to progress)
        )

        val updatedChallenge = challenge.copy(userProgress = updatedProgress)
        val updatedList = active.toMutableList().apply { set(index, updatedChallenge) }
        dataStoreManager.putString(Keys.ACTIVE_CHALLENGES, json.encodeToString(updatedList))

        return updatedProgress
    }

    override suspend fun checkChallengeCompletion(challengeId: String): Boolean {
        val active = getActiveChallengesList()
        val challenge = active.find { it.challenge.id == challengeId } ?: return false

        return challenge.userProgress.currentValue >= challenge.userProgress.targetValue
    }

    override suspend fun claimChallengeReward(challengeId: String): ChallengeRewards? {
        val active = getActiveChallengesList()
        val challenge = active.find { it.challenge.id == challengeId } ?: return null

        if (challenge.userProgress.currentValue < challenge.userProgress.targetValue) {
            return null // Not completed yet
        }

        // Move to completed
        val completedChallenge = challenge.copy(
            userProgress = challenge.userProgress.copy(
                isCompleted = true,
                completedAt = now()
            )
        )

        val updatedActive = active.filter { it.challenge.id != challengeId }
        val currentCompleted = getCompletedChallengesList()
        val updatedCompleted = currentCompleted + completedChallenge

        dataStoreManager.putString(Keys.ACTIVE_CHALLENGES, json.encodeToString(updatedActive))
        dataStoreManager.putString(Keys.COMPLETED_CHALLENGES, json.encodeToString(updatedCompleted))

        // Update stats
        updateStats { it.copy(
            soloChallengesCompleted = it.soloChallengesCompleted + 1,
            soloChallengesActive = it.soloChallengesActive - 1,
            totalXpFromChallenges = it.totalXpFromChallenges + challenge.challenge.rewards.xp
        )}

        return challenge.challenge.rewards
    }

    override suspend fun abandonChallenge(challengeId: String): Boolean {
        val active = getActiveChallengesList()
        val updated = active.filter { it.challenge.id != challengeId }
        if (updated.size == active.size) return false

        dataStoreManager.putString(Keys.ACTIVE_CHALLENGES, json.encodeToString(updated))
        updateStats { it.copy(soloChallengesActive = it.soloChallengesActive - 1) }
        return true
    }

    // ============== FRIEND DUELS ==============

    override fun getPendingDuelInvitations(): Flow<List<DuelInvitation>> {
        return dataStoreManager.getString(Keys.DUEL_INVITATIONS).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) emptyList()
            else {
                try {
                    json.decodeFromString<List<DuelInvitation>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override fun getActiveDuels(): Flow<List<Duel>> {
        return dataStoreManager.getString(Keys.ACTIVE_DUELS).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) emptyList()
            else {
                try {
                    json.decodeFromString<List<Duel>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override fun getDuelHistory(): Flow<List<Duel>> {
        return dataStoreManager.getString(Keys.DUEL_HISTORY).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) emptyList()
            else {
                try {
                    json.decodeFromString<List<Duel>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override suspend fun createDuel(
        opponentId: String,
        opponentName: String,
        opponentEmoji: String,
        templateId: String,
        stake: DuelStake
    ): Duel? {
        val template = ChallengeLibrary.getDuelTemplateById(templateId) ?: return null

        val userId = firebaseService.getCurrentUserId() ?: return null
        val userProfile = firebaseService.getUserProfile()

        val duel = Duel(
            id = "duel_${now().hashCode()}",
            challengerId = userId,
            challengerName = userProfile?.displayName ?: "Me",
            challengerEmoji = userProfile?.profileEmoji ?: "😊",
            opponentId = opponentId,
            opponentName = opponentName,
            opponentEmoji = opponentEmoji,
            goal = template.goal,
            duration = template.duration,
            stake = stake,
            status = DuelStatus.PENDING,
            createdAt = now(),
            rewards = template.rewards
        )

        // Add to active duels
        val currentDuels = getActiveDuelsList()
        dataStoreManager.putString(Keys.ACTIVE_DUELS, json.encodeToString(currentDuels + duel))

        return duel
    }

    override suspend fun acceptDuel(duelId: String): Duel? {
        val duels = getActiveDuelsList()
        val index = duels.indexOfFirst { it.id == duelId }
        if (index == -1) return null

        val duel = duels[index]
        val startDate = today()
        val endDate = startDate.plus(DatePeriod(days = duel.duration.days))

        val updatedDuel = duel.copy(
            status = DuelStatus.ACTIVE,
            startedAt = now(),
            endsAt = endDate.toString()
        )

        val updatedList = duels.toMutableList().apply { set(index, updatedDuel) }
        dataStoreManager.putString(Keys.ACTIVE_DUELS, json.encodeToString(updatedList))

        return updatedDuel
    }

    override suspend fun declineDuel(duelId: String): Boolean {
        val duels = getActiveDuelsList()
        val index = duels.indexOfFirst { it.id == duelId }
        if (index == -1) return false

        val duel = duels[index]
        val updatedDuel = duel.copy(status = DuelStatus.DECLINED)

        // Move to history
        val history = getDuelHistoryList() + updatedDuel
        val active = duels.filter { it.id != duelId }

        dataStoreManager.putString(Keys.ACTIVE_DUELS, json.encodeToString(active))
        dataStoreManager.putString(Keys.DUEL_HISTORY, json.encodeToString(history))

        return true
    }

    override suspend fun cancelDuel(duelId: String): Boolean {
        val duels = getActiveDuelsList()
        val index = duels.indexOfFirst { it.id == duelId }
        if (index == -1) return false

        val duel = duels[index]
        val updatedDuel = duel.copy(status = DuelStatus.CANCELLED)

        val history = getDuelHistoryList() + updatedDuel
        val active = duels.filter { it.id != duelId }

        dataStoreManager.putString(Keys.ACTIVE_DUELS, json.encodeToString(active))
        dataStoreManager.putString(Keys.DUEL_HISTORY, json.encodeToString(history))

        return true
    }

    override suspend fun updateDuelProgress(duelId: String, progress: Int): Duel? {
        val duels = getActiveDuelsList()
        val index = duels.indexOfFirst { it.id == duelId }
        if (index == -1) return null

        val duel = duels[index]
        val updatedDuel = duel.copy(challengerProgress = progress)

        val updatedList = duels.toMutableList().apply { set(index, updatedDuel) }
        dataStoreManager.putString(Keys.ACTIVE_DUELS, json.encodeToString(updatedList))

        return updatedDuel
    }

    override suspend fun checkDuelCompletion(duelId: String): Duel? {
        val duels = getActiveDuelsList()
        val duel = duels.find { it.id == duelId } ?: return null

        if (duel.status != DuelStatus.ACTIVE) return duel

        // Check if ended
        val endDate = duel.endsAt?.let { LocalDate.parse(it.substringBefore("T")) }
        if (endDate != null && today() > endDate) {
            // Determine winner
            val winnerId = when {
                duel.challengerProgress > duel.opponentProgress -> duel.challengerId
                duel.opponentProgress > duel.challengerProgress -> duel.opponentId
                else -> null // Tie
            }

            val completedDuel = duel.copy(
                status = DuelStatus.COMPLETED,
                winnerId = winnerId
            )

            // Move to history
            val active = duels.filter { it.id != duelId }
            val history = getDuelHistoryList() + completedDuel

            dataStoreManager.putString(Keys.ACTIVE_DUELS, json.encodeToString(active))
            dataStoreManager.putString(Keys.DUEL_HISTORY, json.encodeToString(history))

            // Update stats if won
            val currentUserId = firebaseService.getCurrentUserId()
            if (winnerId == currentUserId) {
                updateStats { it.copy(duelsWon = it.duelsWon + 1) }
            } else if (winnerId != null) {
                updateStats { it.copy(duelsLost = it.duelsLost + 1) }
            } else {
                updateStats { it.copy(duelsTied = it.duelsTied + 1) }
            }

            return completedDuel
        }

        return duel
    }

    // ============== COMMUNITY CHALLENGES ==============

    // Cached community challenge from Firebase
    private val _communityChallenge = MutableStateFlow<CommunityChallenge?>(null)

    override fun getActiveCommunityChallenge(): Flow<CommunityChallenge?> {
        return dataStoreManager.getString(Keys.COMMUNITY_CHALLENGE).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) null
            else {
                try {
                    json.decodeFromString<CommunityChallenge>(jsonStr)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Sync community challenge from Firebase
     */
    suspend fun syncCommunityChallenge() {
        val challenge = firebaseService.getActiveCommunityChallenge()
        if (challenge != null) {
            dataStoreManager.putString(Keys.COMMUNITY_CHALLENGE, json.encodeToString(challenge))
            _communityChallenge.value = challenge
        }
    }

    override fun getUserCommunityProgress(): Flow<UserCommunityProgress?> {
        return dataStoreManager.getString(Keys.COMMUNITY_PROGRESS).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) null
            else {
                try {
                    json.decodeFromString<UserCommunityProgress>(jsonStr)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override fun getCommunityLeaderboard(limit: Int): Flow<List<ChallengeParticipant>> {
        // Get leaderboard from Firebase via cached data
        return dataStoreManager.getString("community_leaderboard").map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) emptyList()
            else {
                try {
                    json.decodeFromString<List<ChallengeParticipant>>(jsonStr).take(limit)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    /**
     * Sync community leaderboard from Firebase
     */
    suspend fun syncCommunityLeaderboard(challengeId: String) {
        val leaderboard = firebaseService.getCommunityLeaderboard(challengeId, 50)
        if (leaderboard.isNotEmpty()) {
            dataStoreManager.putString("community_leaderboard", json.encodeToString(leaderboard))
        }
    }

    override suspend fun joinCommunityChallenge(challengeId: String): UserCommunityProgress? {
        val progress = UserCommunityProgress(
            challengeId = challengeId,
            contribution = 0,
            rank = null,
            percentile = null,
            tiersUnlocked = 0
        )
        dataStoreManager.putString(Keys.COMMUNITY_PROGRESS, json.encodeToString(progress))
        return progress
    }

    override suspend fun contributeToCommuntiy(amount: Long): UserCommunityProgress? {
        val current = getCommunityProgressData() ?: return null
        val updated = current.copy(
            contribution = current.contribution + amount
        )
        dataStoreManager.putString(Keys.COMMUNITY_PROGRESS, json.encodeToString(updated))
        updateStats { it.copy(communityContribution = it.communityContribution + amount) }
        return updated
    }

    override suspend fun claimCommunityRewards(): List<CommunityRewards> {
        val challenge = getActiveCommunityChallenge().first() ?: return emptyList()
        return listOf(challenge.rewards)
    }

    // ============== SEASONAL EVENTS ==============

    // Cached seasonal event from Firebase
    private val _seasonalEvent = MutableStateFlow<SeasonalEvent?>(null)

    override fun getActiveSeasonalEvent(): Flow<SeasonalEvent?> {
        return dataStoreManager.getString(Keys.SEASONAL_EVENT).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) null
            else {
                try {
                    json.decodeFromString<SeasonalEvent>(jsonStr)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Sync seasonal event from Firebase
     */
    suspend fun syncSeasonalEvent() {
        val event = firebaseService.getActiveSeasonalEvent()
        if (event != null) {
            dataStoreManager.putString(Keys.SEASONAL_EVENT, json.encodeToString(event))
            _seasonalEvent.value = event
        }
    }

    override fun getUserSeasonalProgress(): Flow<UserSeasonalProgress?> {
        return dataStoreManager.getString(Keys.SEASONAL_PROGRESS).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) null
            else {
                try {
                    json.decodeFromString<UserSeasonalProgress>(jsonStr)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun joinSeasonalEvent(eventId: String): UserSeasonalProgress? {
        val progress = UserSeasonalProgress(eventId = eventId)
        dataStoreManager.putString(Keys.SEASONAL_PROGRESS, json.encodeToString(progress))
        return progress
    }

    override suspend fun claimSeasonalReward(rewardId: String): SeasonalReward? {
        val currentProgress = getSeasonalProgressData() ?: return null
        val seasonalEvent = getActiveSeasonalEvent().first() ?: return null
        val reward = seasonalEvent.exclusiveRewards.find { it.id == rewardId } ?: return null

        val updatedProgress = currentProgress.copy(
            rewardsEarned = currentProgress.rewardsEarned + rewardId
        )
        dataStoreManager.putString(Keys.SEASONAL_PROGRESS, json.encodeToString(updatedProgress))

        updateStats { it.copy(seasonalChallengesCompleted = it.seasonalChallengesCompleted + 1) }

        return reward
    }

    // ============== CHALLENGE CREATOR ==============

    override fun getMyCustomChallenges(): Flow<List<CustomChallengeTemplate>> {
        return dataStoreManager.getString(Keys.CUSTOM_CHALLENGES).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) emptyList()
            else {
                try {
                    json.decodeFromString<List<CustomChallengeTemplate>>(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override fun getPublicCustomChallenges(): Flow<List<CustomChallengeTemplate>> {
        return getMyCustomChallenges().map { list -> list.filter { it.isPublic } }
    }

    override suspend fun createCustomChallenge(
        title: String,
        description: String,
        emoji: String,
        goalType: ChallengeGoal,
        duration: ChallengeDuration,
        difficulty: ChallengeDifficulty,
        isPublic: Boolean
    ): CustomChallengeTemplate? {
        val userId = firebaseService.getCurrentUserId() ?: return null
        val userProfile = firebaseService.getUserProfile()

        val template = CustomChallengeTemplate(
            id = "custom_${now().hashCode()}",
            creatorId = userId,
            creatorName = userProfile?.displayName ?: "Me",
            title = title,
            description = description,
            emoji = emoji,
            goalType = goalType,
            duration = duration,
            difficulty = difficulty,
            isPublic = isPublic,
            createdAt = now()
        )

        val current = getCustomChallengesList()
        dataStoreManager.putString(Keys.CUSTOM_CHALLENGES, json.encodeToString(current + template))

        updateStats { it.copy(customChallengesCreated = it.customChallengesCreated + 1) }

        return template
    }

    override suspend fun deleteCustomChallenge(templateId: String): Boolean {
        val current = getCustomChallengesList()
        val updated = current.filter { it.id != templateId }
        if (updated.size == current.size) return false

        dataStoreManager.putString(Keys.CUSTOM_CHALLENGES, json.encodeToString(updated))
        return true
    }

    override suspend fun shareCustomChallenge(templateId: String, friendIds: List<String>): Boolean {
        // In a real app, this would send invitations to friends
        // For now, just mark as shared/public
        val current = getCustomChallengesList()
        val index = current.indexOfFirst { it.id == templateId }
        if (index == -1) return false

        val updated = current.toMutableList().apply {
            set(index, current[index].copy(isPublic = true))
        }
        dataStoreManager.putString(Keys.CUSTOM_CHALLENGES, json.encodeToString(updated))
        return true
    }

    // ============== STATS ==============

    override fun getChallengeStats(): Flow<ChallengeStats> {
        return dataStoreManager.getString(Keys.CHALLENGE_STATS).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) ChallengeStats()
            else {
                try {
                    json.decodeFromString<ChallengeStats>(jsonStr)
                } catch (e: Exception) {
                    ChallengeStats()
                }
            }
        }
    }

    // ============== HELPERS ==============

    private suspend fun getActiveChallengesList(): List<ActiveChallenge> {
        val jsonStr = dataStoreManager.getString(Keys.ACTIVE_CHALLENGES).first()
        return if (jsonStr.isNullOrEmpty()) emptyList()
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun getCompletedChallengesList(): List<ActiveChallenge> {
        val jsonStr = dataStoreManager.getString(Keys.COMPLETED_CHALLENGES).first()
        return if (jsonStr.isNullOrEmpty()) emptyList()
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun getActiveDuelsList(): List<Duel> {
        val jsonStr = dataStoreManager.getString(Keys.ACTIVE_DUELS).first()
        return if (jsonStr.isNullOrEmpty()) emptyList()
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun getDuelHistoryList(): List<Duel> {
        val jsonStr = dataStoreManager.getString(Keys.DUEL_HISTORY).first()
        return if (jsonStr.isNullOrEmpty()) emptyList()
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun getCommunityProgressData(): UserCommunityProgress? {
        val jsonStr = dataStoreManager.getString(Keys.COMMUNITY_PROGRESS).first()
        return if (jsonStr.isNullOrEmpty()) null
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun getSeasonalProgressData(): UserSeasonalProgress? {
        val jsonStr = dataStoreManager.getString(Keys.SEASONAL_PROGRESS).first()
        return if (jsonStr.isNullOrEmpty()) null
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun getCustomChallengesList(): List<CustomChallengeTemplate> {
        val jsonStr = dataStoreManager.getString(Keys.CUSTOM_CHALLENGES).first()
        return if (jsonStr.isNullOrEmpty()) emptyList()
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun getStatsData(): ChallengeStats {
        val jsonStr = dataStoreManager.getString(Keys.CHALLENGE_STATS).first()
        return if (jsonStr.isNullOrEmpty()) ChallengeStats()
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                ChallengeStats()
            }
        }
    }

    private suspend fun updateStats(update: (ChallengeStats) -> ChallengeStats) {
        val current = getStatsData()
        val updated = update(current)
        dataStoreManager.putString(Keys.CHALLENGE_STATS, json.encodeToString(updated))
    }
}
