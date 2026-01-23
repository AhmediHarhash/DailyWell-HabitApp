package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

interface ChallengeRepository {
    // ============== SOLO CHALLENGES ==============

    fun getAvailableSoloChallenges(): Flow<List<Challenge>>
    fun getActiveSoloChallenges(): Flow<List<ActiveChallenge>>
    fun getCompletedChallenges(): Flow<List<ActiveChallenge>>

    suspend fun joinSoloChallenge(challengeId: String): ActiveChallenge?
    suspend fun updateChallengeProgress(challengeId: String, progress: Int): UserChallengeProgress?
    suspend fun checkChallengeCompletion(challengeId: String): Boolean
    suspend fun claimChallengeReward(challengeId: String): ChallengeRewards?
    suspend fun abandonChallenge(challengeId: String): Boolean

    // ============== FRIEND DUELS ==============

    fun getPendingDuelInvitations(): Flow<List<DuelInvitation>>
    fun getActiveDuels(): Flow<List<Duel>>
    fun getDuelHistory(): Flow<List<Duel>>

    suspend fun createDuel(
        opponentId: String,
        opponentName: String,
        opponentEmoji: String,
        templateId: String,
        stake: DuelStake = DuelStake.Friendly
    ): Duel?

    suspend fun acceptDuel(duelId: String): Duel?
    suspend fun declineDuel(duelId: String): Boolean
    suspend fun cancelDuel(duelId: String): Boolean
    suspend fun updateDuelProgress(duelId: String, progress: Int): Duel?
    suspend fun checkDuelCompletion(duelId: String): Duel?

    // ============== COMMUNITY CHALLENGES ==============

    fun getActiveCommunityChallenge(): Flow<CommunityChallenge?>
    fun getUserCommunityProgress(): Flow<UserCommunityProgress?>
    fun getCommunityLeaderboard(limit: Int = 100): Flow<List<ChallengeParticipant>>

    suspend fun joinCommunityChallenge(challengeId: String): UserCommunityProgress?
    suspend fun contributeToCommuntiy(amount: Long): UserCommunityProgress?
    suspend fun claimCommunityRewards(): List<CommunityRewards>

    // ============== SEASONAL EVENTS ==============

    fun getActiveSeasonalEvent(): Flow<SeasonalEvent?>
    fun getUserSeasonalProgress(): Flow<UserSeasonalProgress?>

    suspend fun joinSeasonalEvent(eventId: String): UserSeasonalProgress?
    suspend fun claimSeasonalReward(rewardId: String): SeasonalReward?

    // ============== CHALLENGE CREATOR ==============

    fun getMyCustomChallenges(): Flow<List<CustomChallengeTemplate>>
    fun getPublicCustomChallenges(): Flow<List<CustomChallengeTemplate>>

    suspend fun createCustomChallenge(
        title: String,
        description: String,
        emoji: String,
        goalType: ChallengeGoal,
        duration: ChallengeDuration,
        difficulty: ChallengeDifficulty,
        isPublic: Boolean = false
    ): CustomChallengeTemplate?

    suspend fun deleteCustomChallenge(templateId: String): Boolean
    suspend fun shareCustomChallenge(templateId: String, friendIds: List<String>): Boolean

    // ============== STATS ==============

    fun getChallengeStats(): Flow<ChallengeStats>
}

data class ChallengeStats(
    val soloChallengesCompleted: Int = 0,
    val soloChallengesActive: Int = 0,
    val duelsWon: Int = 0,
    val duelsLost: Int = 0,
    val duelsTied: Int = 0,
    val communityContribution: Long = 0,
    val seasonalChallengesCompleted: Int = 0,
    val customChallengesCreated: Int = 0,
    val totalXpFromChallenges: Long = 0
)
