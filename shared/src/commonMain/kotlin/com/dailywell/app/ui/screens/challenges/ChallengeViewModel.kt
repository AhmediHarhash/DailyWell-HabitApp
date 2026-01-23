package com.dailywell.app.ui.screens.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.ChallengeRepository
import com.dailywell.app.data.repository.ChallengeStats
import com.dailywell.app.data.repository.GamificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChallengeUiState(
    // Tab selection
    val selectedTab: ChallengeTab = ChallengeTab.SOLO,

    // Solo Challenges
    val availableChallenges: List<Challenge> = emptyList(),
    val activeChallenges: List<ActiveChallenge> = emptyList(),
    val completedChallenges: List<ActiveChallenge> = emptyList(),
    val selectedDifficulty: ChallengeDifficulty? = null,

    // Duels
    val pendingInvitations: List<DuelInvitation> = emptyList(),
    val activeDuels: List<Duel> = emptyList(),
    val duelHistory: List<Duel> = emptyList(),

    // Community
    val communityChallenge: CommunityChallenge? = null,
    val userCommunityProgress: UserCommunityProgress? = null,
    val communityLeaderboard: List<ChallengeParticipant> = emptyList(),

    // Seasonal
    val seasonalEvent: SeasonalEvent? = null,
    val seasonalProgress: UserSeasonalProgress? = null,

    // Custom Challenges
    val customChallenges: List<CustomChallengeTemplate> = emptyList(),

    // Stats
    val stats: ChallengeStats = ChallengeStats(),

    // UI State
    val isLoading: Boolean = true,
    val showChallengeDetail: Challenge? = null,
    val showDuelDetail: Duel? = null,
    val showCreateChallenge: Boolean = false,
    val showCreateDuel: Boolean = false,
    val rewardClaimed: ChallengeRewards? = null,
    val errorMessage: String? = null
)

enum class ChallengeTab {
    SOLO,
    DUELS,
    COMMUNITY,
    SEASONAL,
    CREATE
}

class ChallengeViewModel(
    private val challengeRepository: ChallengeRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChallengeUiState())
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            // Load solo challenges
            combine(
                challengeRepository.getAvailableSoloChallenges(),
                challengeRepository.getActiveSoloChallenges(),
                challengeRepository.getCompletedChallenges()
            ) { available, active, completed ->
                Triple(available, active, completed)
            }.collect { (available, active, completed) ->
                _uiState.update {
                    it.copy(
                        availableChallenges = available,
                        activeChallenges = active,
                        completedChallenges = completed,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            // Load duels
            combine(
                challengeRepository.getPendingDuelInvitations(),
                challengeRepository.getActiveDuels(),
                challengeRepository.getDuelHistory()
            ) { invitations, active, history ->
                Triple(invitations, active, history)
            }.collect { (invitations, active, history) ->
                _uiState.update {
                    it.copy(
                        pendingInvitations = invitations,
                        activeDuels = active,
                        duelHistory = history
                    )
                }
            }
        }

        viewModelScope.launch {
            // Load community challenge
            combine(
                challengeRepository.getActiveCommunityChallenge(),
                challengeRepository.getUserCommunityProgress(),
                challengeRepository.getCommunityLeaderboard(10)
            ) { challenge, progress, leaderboard ->
                Triple(challenge, progress, leaderboard)
            }.collect { (challenge, progress, leaderboard) ->
                _uiState.update {
                    it.copy(
                        communityChallenge = challenge,
                        userCommunityProgress = progress,
                        communityLeaderboard = leaderboard
                    )
                }
            }
        }

        viewModelScope.launch {
            // Load seasonal event
            combine(
                challengeRepository.getActiveSeasonalEvent(),
                challengeRepository.getUserSeasonalProgress()
            ) { event, progress ->
                Pair(event, progress)
            }.collect { (event, progress) ->
                _uiState.update {
                    it.copy(
                        seasonalEvent = event,
                        seasonalProgress = progress
                    )
                }
            }
        }

        viewModelScope.launch {
            // Load custom challenges
            challengeRepository.getMyCustomChallenges().collect { customs ->
                _uiState.update { it.copy(customChallenges = customs) }
            }
        }

        viewModelScope.launch {
            // Load stats
            challengeRepository.getChallengeStats().collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
    }

    // Tab navigation
    fun selectTab(tab: ChallengeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    // Filter by difficulty
    fun filterByDifficulty(difficulty: ChallengeDifficulty?) {
        _uiState.update { it.copy(selectedDifficulty = difficulty) }
    }

    fun getFilteredChallenges(): List<Challenge> {
        val state = _uiState.value
        return if (state.selectedDifficulty == null) {
            state.availableChallenges
        } else {
            state.availableChallenges.filter { it.difficulty == state.selectedDifficulty }
        }
    }

    // Solo Challenge actions
    fun joinChallenge(challengeId: String) {
        viewModelScope.launch {
            val result = challengeRepository.joinSoloChallenge(challengeId)
            if (result == null) {
                _uiState.update { it.copy(errorMessage = "Failed to join challenge") }
            }
        }
    }

    fun abandonChallenge(challengeId: String) {
        viewModelScope.launch {
            challengeRepository.abandonChallenge(challengeId)
        }
    }

    fun claimReward(challengeId: String) {
        viewModelScope.launch {
            val rewards = challengeRepository.claimChallengeReward(challengeId)
            if (rewards != null) {
                // Add XP through gamification repository
                gamificationRepository.addXp(rewards.xp, com.dailywell.app.data.model.XpReason.CHALLENGE_WIN)

                // Add streak shields if any
                if (rewards.streakShields > 0) {
                    gamificationRepository.addStreakShield(rewards.streakShields)
                }

                // Unlock badge if any
                rewards.badge?.let { badgeId ->
                    gamificationRepository.unlockBadge(badgeId)
                }

                // Show reward claimed
                _uiState.update { it.copy(rewardClaimed = rewards) }
            }
        }
    }

    // Duel actions
    fun createDuel(opponentId: String, opponentName: String, opponentEmoji: String, templateId: String, stake: DuelStake = DuelStake.Friendly) {
        viewModelScope.launch {
            val result = challengeRepository.createDuel(opponentId, opponentName, opponentEmoji, templateId, stake)
            if (result == null) {
                _uiState.update { it.copy(errorMessage = "Failed to create duel") }
            } else {
                _uiState.update { it.copy(showCreateDuel = false) }
            }
        }
    }

    fun acceptDuel(duelId: String) {
        viewModelScope.launch {
            challengeRepository.acceptDuel(duelId)
        }
    }

    fun declineDuel(duelId: String) {
        viewModelScope.launch {
            challengeRepository.declineDuel(duelId)
        }
    }

    fun cancelDuel(duelId: String) {
        viewModelScope.launch {
            challengeRepository.cancelDuel(duelId)
        }
    }

    // Community actions
    fun joinCommunityChallenge() {
        viewModelScope.launch {
            _uiState.value.communityChallenge?.let { challenge ->
                challengeRepository.joinCommunityChallenge(challenge.id)
            }
        }
    }

    // Seasonal actions
    fun joinSeasonalEvent() {
        viewModelScope.launch {
            _uiState.value.seasonalEvent?.let { event ->
                challengeRepository.joinSeasonalEvent(event.id)
            }
        }
    }

    fun claimSeasonalReward(rewardId: String) {
        viewModelScope.launch {
            challengeRepository.claimSeasonalReward(rewardId)
        }
    }

    // Custom challenge creation
    fun createCustomChallenge(
        title: String,
        description: String,
        emoji: String,
        goalType: ChallengeGoal,
        duration: ChallengeDuration,
        difficulty: ChallengeDifficulty,
        isPublic: Boolean = false
    ) {
        viewModelScope.launch {
            val result = challengeRepository.createCustomChallenge(
                title, description, emoji, goalType, duration, difficulty, isPublic
            )
            if (result != null) {
                _uiState.update { it.copy(showCreateChallenge = false) }
            } else {
                _uiState.update { it.copy(errorMessage = "Failed to create challenge") }
            }
        }
    }

    fun deleteCustomChallenge(templateId: String) {
        viewModelScope.launch {
            challengeRepository.deleteCustomChallenge(templateId)
        }
    }

    fun shareCustomChallenge(templateId: String, friendIds: List<String>) {
        viewModelScope.launch {
            challengeRepository.shareCustomChallenge(templateId, friendIds)
        }
    }

    // UI actions
    fun showChallengeDetail(challenge: Challenge) {
        _uiState.update { it.copy(showChallengeDetail = challenge) }
    }

    fun hideChallengeDetail() {
        _uiState.update { it.copy(showChallengeDetail = null) }
    }

    fun showDuelDetail(duel: Duel) {
        _uiState.update { it.copy(showDuelDetail = duel) }
    }

    fun hideDuelDetail() {
        _uiState.update { it.copy(showDuelDetail = null) }
    }

    fun showCreateChallenge() {
        _uiState.update { it.copy(showCreateChallenge = true) }
    }

    fun hideCreateChallenge() {
        _uiState.update { it.copy(showCreateChallenge = false) }
    }

    fun showCreateDuel() {
        _uiState.update { it.copy(showCreateDuel = true) }
    }

    fun hideCreateDuel() {
        _uiState.update { it.copy(showCreateDuel = false) }
    }

    fun dismissRewardClaimed() {
        _uiState.update { it.copy(rewardClaimed = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
