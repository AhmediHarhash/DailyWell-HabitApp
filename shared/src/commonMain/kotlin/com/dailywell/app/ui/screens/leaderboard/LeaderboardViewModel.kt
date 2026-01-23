package com.dailywell.app.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LeaderboardUiState(
    val selectedTab: LeaderboardTab = LeaderboardTab.FRIENDS,
    val selectedMetric: LeaderboardMetric = LeaderboardMetric.WEEKLY_XP,

    // Leaderboards
    val friendsLeaderboard: List<LeaderboardEntry> = emptyList(),
    val globalLeaderboard: List<LeaderboardEntry> = emptyList(),
    val habitLeaderboard: List<LeaderboardEntry> = emptyList(),
    val currentUserRank: LeaderboardEntry? = null,
    val selectedHabitType: String = "sleep",

    // Activity Feed
    val activityFeed: List<ActivityFeedItem> = emptyList(),
    val friendsActivityFeed: List<ActivityFeedItem> = emptyList(),
    val showFriendsOnlyFeed: Boolean = true,

    // Cheers
    val receivedCheers: List<Cheer> = emptyList(),
    val sentCheers: List<Cheer> = emptyList(),
    val unreadCheersCount: Int = 0,

    // Friends
    val friends: List<Friend> = emptyList(),
    val friendRequests: List<FriendRequest> = emptyList(),
    val sentFriendRequests: List<FriendRequest> = emptyList(),
    val searchResults: List<UserSearchResult> = emptyList(),
    val searchQuery: String = "",

    // Referrals
    val referralCode: ReferralCode? = null,
    val referralStats: ReferralStats? = null,

    // UI State
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val showCheerDialog: Boolean = false,
    val selectedUserForCheer: Pair<String, String>? = null, // userId, userName
    val showAddFriendDialog: Boolean = false,
    val showReferralDialog: Boolean = false
)

enum class LeaderboardTab {
    FRIENDS, GLOBAL, HABITS, ACTIVITY, CHEERS, REFERRALS
}

class LeaderboardViewModel(
    private val leaderboardRepository: LeaderboardRepository,
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Collect friends leaderboard
            launch {
                leaderboardRepository.getFriendsLeaderboard(_uiState.value.selectedMetric)
                    .collect { entries ->
                        _uiState.update { it.copy(friendsLeaderboard = entries) }
                    }
            }

            // Collect global leaderboard
            launch {
                leaderboardRepository.getGlobalLeaderboard(_uiState.value.selectedMetric)
                    .collect { entries ->
                        _uiState.update { it.copy(globalLeaderboard = entries) }
                    }
            }

            // Collect habit leaderboard
            launch {
                leaderboardRepository.getHabitLeaderboard(_uiState.value.selectedHabitType)
                    .collect { entries ->
                        _uiState.update { it.copy(habitLeaderboard = entries) }
                    }
            }

            // Collect activity feeds
            launch {
                leaderboardRepository.getActivityFeed()
                    .collect { items ->
                        _uiState.update { it.copy(activityFeed = items) }
                    }
            }

            launch {
                leaderboardRepository.getFriendsActivityFeed()
                    .collect { items ->
                        _uiState.update { it.copy(friendsActivityFeed = items) }
                    }
            }

            // Collect cheers
            launch {
                leaderboardRepository.getReceivedCheers()
                    .collect { cheers ->
                        _uiState.update { it.copy(receivedCheers = cheers) }
                    }
            }

            launch {
                leaderboardRepository.getSentCheers()
                    .collect { cheers ->
                        _uiState.update { it.copy(sentCheers = cheers) }
                    }
            }

            launch {
                leaderboardRepository.getUnreadCheersCount()
                    .collect { count ->
                        _uiState.update { it.copy(unreadCheersCount = count) }
                    }
            }

            // Collect friends
            launch {
                leaderboardRepository.getFriends()
                    .collect { friends ->
                        _uiState.update { it.copy(friends = friends) }
                    }
            }

            launch {
                leaderboardRepository.getFriendRequests()
                    .collect { requests ->
                        _uiState.update { it.copy(friendRequests = requests) }
                    }
            }

            launch {
                leaderboardRepository.getSentFriendRequests()
                    .collect { requests ->
                        _uiState.update { it.copy(sentFriendRequests = requests) }
                    }
            }

            // Collect referral data
            launch {
                leaderboardRepository.getReferralCode()
                    .collect { code ->
                        _uiState.update { it.copy(referralCode = code) }
                    }
            }

            launch {
                leaderboardRepository.getReferralStats()
                    .collect { stats ->
                        _uiState.update { it.copy(referralStats = stats) }
                    }
            }

            // Get current user rank
            launch {
                val rank = leaderboardRepository.getCurrentUserRank(
                    LeaderboardType.GLOBAL,
                    _uiState.value.selectedMetric
                )
                _uiState.update { it.copy(currentUserRank = rank, isLoading = false) }
            }
        }
    }

    fun selectTab(tab: LeaderboardTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectMetric(metric: LeaderboardMetric) {
        _uiState.update { it.copy(selectedMetric = metric) }
        refreshLeaderboards()
    }

    fun selectHabitType(habitType: String) {
        _uiState.update { it.copy(selectedHabitType = habitType) }
        viewModelScope.launch {
            leaderboardRepository.getHabitLeaderboard(habitType)
                .collect { entries ->
                    _uiState.update { it.copy(habitLeaderboard = entries) }
                }
        }
    }

    fun toggleFriendsOnlyFeed() {
        _uiState.update { it.copy(showFriendsOnlyFeed = !it.showFriendsOnlyFeed) }
    }

    fun refreshLeaderboards() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            leaderboardRepository.refreshLeaderboards()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    // ============== REACTIONS ==============

    fun addReaction(activityId: String, reactionType: ReactionType) {
        viewModelScope.launch {
            leaderboardRepository.addReaction(activityId, reactionType)
        }
    }

    fun removeReaction(activityId: String, reactionId: String) {
        viewModelScope.launch {
            leaderboardRepository.removeReaction(activityId, reactionId)
        }
    }

    // ============== CHEERS ==============

    fun showCheerDialog(userId: String, userName: String) {
        _uiState.update {
            it.copy(
                showCheerDialog = true,
                selectedUserForCheer = Pair(userId, userName)
            )
        }
    }

    fun dismissCheerDialog() {
        _uiState.update {
            it.copy(
                showCheerDialog = false,
                selectedUserForCheer = null
            )
        }
    }

    fun sendCheer(cheerType: CheerType, message: String? = null) {
        val (userId, userName) = _uiState.value.selectedUserForCheer ?: return

        viewModelScope.launch {
            leaderboardRepository.sendCheer(userId, userName, cheerType, message)
            dismissCheerDialog()
        }
    }

    fun markCheerAsRead(cheerId: String) {
        viewModelScope.launch {
            leaderboardRepository.markCheerAsRead(cheerId)
        }
    }

    fun markAllCheersAsRead() {
        viewModelScope.launch {
            leaderboardRepository.markAllCheersAsRead()
        }
    }

    // ============== FRIENDS ==============

    fun showAddFriendDialog() {
        _uiState.update { it.copy(showAddFriendDialog = true, searchResults = emptyList(), searchQuery = "") }
    }

    fun dismissAddFriendDialog() {
        _uiState.update { it.copy(showAddFriendDialog = false, searchResults = emptyList(), searchQuery = "") }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length >= 2) {
            searchUsers(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    private fun searchUsers(query: String) {
        viewModelScope.launch {
            val results = leaderboardRepository.searchUsers(query)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun sendFriendRequest(userId: String, userName: String) {
        viewModelScope.launch {
            leaderboardRepository.sendFriendRequest(userId, userName)
            // Update search results to show pending request
            val updatedResults = _uiState.value.searchResults.map { result ->
                if (result.userId == userId) result.copy(hasPendingRequest = true) else result
            }
            _uiState.update { it.copy(searchResults = updatedResults) }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            leaderboardRepository.acceptFriendRequest(requestId)
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            leaderboardRepository.declineFriendRequest(requestId)
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            leaderboardRepository.removeFriend(friendId)
        }
    }

    // ============== REFERRALS ==============

    fun showReferralDialog() {
        _uiState.update { it.copy(showReferralDialog = true) }
    }

    fun dismissReferralDialog() {
        _uiState.update { it.copy(showReferralDialog = false) }
    }

    fun generateReferralCode() {
        viewModelScope.launch {
            leaderboardRepository.generateReferralCode()
        }
    }

    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            val result = leaderboardRepository.applyReferralCode(code)
            when (result) {
                is ReferralResult.Success -> {
                    _uiState.update { it.copy(error = null) }
                }
                is ReferralResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                ReferralResult.AlreadyUsed -> {
                    _uiState.update { it.copy(error = "You've already used a referral code") }
                }
                ReferralResult.InvalidCode -> {
                    _uiState.update { it.copy(error = "Invalid referral code") }
                }
                ReferralResult.OwnCode -> {
                    _uiState.update { it.copy(error = "You can't use your own referral code") }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ============== ACTIVITY ==============

    fun hideActivityItem(itemId: String) {
        viewModelScope.launch {
            leaderboardRepository.hideActivityItem(itemId)
        }
    }
}
