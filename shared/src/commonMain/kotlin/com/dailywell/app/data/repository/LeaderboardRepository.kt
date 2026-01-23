package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

interface LeaderboardRepository {
    // ============== LEADERBOARDS ==============

    fun getFriendsLeaderboard(metric: LeaderboardMetric = LeaderboardMetric.WEEKLY_XP): Flow<List<LeaderboardEntry>>
    fun getGlobalLeaderboard(
        metric: LeaderboardMetric = LeaderboardMetric.WEEKLY_XP,
        limit: Int = 100
    ): Flow<List<LeaderboardEntry>>
    fun getHabitLeaderboard(habitType: String, limit: Int = 50): Flow<List<LeaderboardEntry>>

    suspend fun getCurrentUserRank(type: LeaderboardType, metric: LeaderboardMetric): LeaderboardEntry?
    suspend fun refreshLeaderboards()

    // ============== ACTIVITY FEED ==============

    fun getActivityFeed(limit: Int = 50): Flow<List<ActivityFeedItem>>
    fun getFriendsActivityFeed(limit: Int = 50): Flow<List<ActivityFeedItem>>

    suspend fun postActivity(type: ActivityType, content: ActivityContent)
    suspend fun hideActivityItem(itemId: String)

    // ============== REACTIONS ==============

    suspend fun addReaction(activityId: String, reactionType: ReactionType): Reaction?
    suspend fun removeReaction(activityId: String, reactionId: String): Boolean
    fun getReactionsForActivity(activityId: String): Flow<List<Reaction>>

    // ============== CHEERS ==============

    fun getReceivedCheers(): Flow<List<Cheer>>
    fun getSentCheers(): Flow<List<Cheer>>
    fun getUnreadCheersCount(): Flow<Int>

    suspend fun sendCheer(toUserId: String, toUserName: String, cheerType: CheerType, message: String? = null): Cheer?
    suspend fun markCheerAsRead(cheerId: String)
    suspend fun markAllCheersAsRead()

    // ============== REFERRALS ==============

    fun getReferralCode(): Flow<ReferralCode?>
    fun getReferralStats(): Flow<ReferralStats>

    suspend fun generateReferralCode(): ReferralCode
    suspend fun applyReferralCode(code: String): ReferralResult
    suspend fun getReferralHistory(): List<ReferralHistoryEntry>

    // ============== FRIENDS ==============

    fun getFriends(): Flow<List<Friend>>
    fun getFriendRequests(): Flow<List<FriendRequest>>
    fun getSentFriendRequests(): Flow<List<FriendRequest>>

    suspend fun sendFriendRequest(userId: String, userName: String): FriendRequest?
    suspend fun acceptFriendRequest(requestId: String): Boolean
    suspend fun declineFriendRequest(requestId: String): Boolean
    suspend fun removeFriend(friendId: String): Boolean
    suspend fun searchUsers(query: String): List<UserSearchResult>
}

// Supporting data classes

@kotlinx.serialization.Serializable
data class Friend(
    val odId: String,
    val odUserId: String,
    val displayName: String,
    val avatarEmoji: String = "😊",
    val level: Int = 1,
    val currentStreak: Int = 0,
    val lastActive: String? = null,
    val friendSince: String
)

@kotlinx.serialization.Serializable
data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserEmoji: String,
    val toUserId: String,
    val status: FriendRequestStatus,
    val createdAt: String
)

@kotlinx.serialization.Serializable
enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED
}

@kotlinx.serialization.Serializable
data class UserSearchResult(
    val userId: String,
    val displayName: String,
    val avatarEmoji: String,
    val level: Int,
    val isFriend: Boolean,
    val hasPendingRequest: Boolean
)

@kotlinx.serialization.Serializable
sealed class ReferralResult {
    @kotlinx.serialization.Serializable
    data class Success(val reward: ReferralBenefit) : ReferralResult()
    @kotlinx.serialization.Serializable
    data class Error(val message: String) : ReferralResult()
    @kotlinx.serialization.Serializable
    object AlreadyUsed : ReferralResult()
    @kotlinx.serialization.Serializable
    object InvalidCode : ReferralResult()
    @kotlinx.serialization.Serializable
    object OwnCode : ReferralResult()
}

@kotlinx.serialization.Serializable
data class ReferralHistoryEntry(
    val referredUserId: String,
    val referredUserName: String,
    val referredAt: String,
    val rewardClaimed: Boolean,
    val reward: ReferralBenefit?
)
