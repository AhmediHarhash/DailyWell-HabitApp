package com.dailywell.app.data.repository

import com.dailywell.app.api.FirebaseService
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Implementation of LeaderboardRepository with REAL Firebase integration
 */
class LeaderboardRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val firebaseService: FirebaseService
) : LeaderboardRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        const val FRIENDS = "leaderboard_friends"
        const val FRIEND_REQUESTS = "friend_requests"
        const val SENT_FRIEND_REQUESTS = "sent_friend_requests"
        const val ACTIVITY_FEED = "activity_feed"
        const val RECEIVED_CHEERS = "received_cheers"
        const val SENT_CHEERS = "sent_cheers"
        const val REFERRAL_CODE = "referral_code"
        const val REFERRAL_STATS = "referral_stats"
        const val REFERRAL_HISTORY = "referral_history"
        const val APPLIED_REFERRAL = "applied_referral"
    }

    private fun now(): String = Clock.System.now().toString()

    // ============== LEADERBOARDS (REAL FIREBASE) ==============

    override fun getFriendsLeaderboard(metric: LeaderboardMetric): Flow<List<LeaderboardEntry>> {
        val entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())

        // This will be called and collected by the UI
        // For now, we fetch synchronously on first access
        return entries.also {
            // Trigger async fetch
            kotlinx.coroutines.GlobalScope.launch {
                val userId = firebaseService.getCurrentUserId() ?: return@launch
                val firebaseEntries = firebaseService.getFriendsLeaderboard(userId)
                val mappedEntries = firebaseEntries.mapIndexed { index, fbEntry ->
                    LeaderboardEntry(
                        odId = fbEntry.userId,
                        userId = fbEntry.userId,
                        displayName = fbEntry.displayName,
                        avatarEmoji = fbEntry.profileEmoji,
                        level = fbEntry.currentLevel,
                        rank = fbEntry.rank,
                        previousRank = null,
                        score = fbEntry.totalXp,
                        streak = fbEntry.currentStreak,
                        perfectDays = 0,
                        isFriend = true,
                        isCurrentUser = fbEntry.userId == userId,
                        updatedAt = now()
                    )
                }
                entries.value = mappedEntries
            }
        }
    }

    override fun getGlobalLeaderboard(metric: LeaderboardMetric, limit: Int): Flow<List<LeaderboardEntry>> {
        val entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())

        return entries.also {
            kotlinx.coroutines.GlobalScope.launch {
                val userId = firebaseService.getCurrentUserId()
                val firebaseEntries = firebaseService.getGlobalLeaderboard(limit).first()
                val mappedEntries = firebaseEntries.mapIndexed { index, fbEntry ->
                    LeaderboardEntry(
                        odId = fbEntry.userId,
                        userId = fbEntry.userId,
                        displayName = fbEntry.displayName,
                        avatarEmoji = fbEntry.profileEmoji,
                        level = fbEntry.currentLevel,
                        rank = fbEntry.rank,
                        previousRank = null,
                        score = fbEntry.totalXp,
                        streak = fbEntry.currentStreak,
                        perfectDays = 0,
                        isFriend = false,
                        isCurrentUser = userId != null && fbEntry.userId == userId,
                        updatedAt = now()
                    )
                }
                entries.value = mappedEntries
            }
        }
    }

    override fun getHabitLeaderboard(habitType: String, limit: Int): Flow<List<LeaderboardEntry>> {
        // Use global leaderboard for now, can be enhanced later
        return getGlobalLeaderboard(LeaderboardMetric.TOTAL_XP, limit)
    }

    override suspend fun getCurrentUserRank(type: LeaderboardType, metric: LeaderboardMetric): LeaderboardEntry? {
        val userId = firebaseService.getCurrentUserId() ?: return null
        return when (type) {
            LeaderboardType.FRIENDS -> {
                val entries = firebaseService.getFriendsLeaderboard(userId)
                entries.find { it.userId == userId }?.let { fbEntry ->
                    LeaderboardEntry(
                        odId = fbEntry.userId,
                        userId = fbEntry.userId,
                        displayName = fbEntry.displayName,
                        avatarEmoji = fbEntry.profileEmoji,
                        level = fbEntry.currentLevel,
                        rank = fbEntry.rank,
                        previousRank = null,
                        score = fbEntry.totalXp,
                        streak = fbEntry.currentStreak,
                        perfectDays = 0,
                        isFriend = false,
                        isCurrentUser = true,
                        updatedAt = now()
                    )
                }
            }
            LeaderboardType.GLOBAL -> {
                val entries = firebaseService.getGlobalLeaderboard(100).first()
                entries.find { it.userId == userId }?.let { fbEntry ->
                    LeaderboardEntry(
                        odId = fbEntry.userId,
                        userId = fbEntry.userId,
                        displayName = fbEntry.displayName,
                        avatarEmoji = fbEntry.profileEmoji,
                        level = fbEntry.currentLevel,
                        rank = fbEntry.rank,
                        previousRank = null,
                        score = fbEntry.totalXp,
                        streak = fbEntry.currentStreak,
                        perfectDays = 0,
                        isFriend = false,
                        isCurrentUser = true,
                        updatedAt = now()
                    )
                }
            }
            else -> null
        }
    }

    override suspend fun refreshLeaderboards() {
        // Firebase service auto-refreshes on query
    }

    // ============== ACTIVITY FEED (REAL FIREBASE) ==============

    override fun getActivityFeed(limit: Int): Flow<List<ActivityFeedItem>> {
        return firebaseService.getActivityFeed(limit).map { activities ->
            activities.map { fbActivity ->
                ActivityFeedItem(
                    id = fbActivity.id,
                    userId = fbActivity.userId,
                    userName = fbActivity.userName,
                    userEmoji = fbActivity.userEmoji,
                    type = mapActivityType(fbActivity.activityType),
                    content = mapActivityContentFromMessage(fbActivity.activityType, fbActivity.message),
                    timestamp = fbActivity.createdAt,
                    reactions = emptyList() // Could be enhanced to fetch reactions
                )
            }
        }
    }

    override fun getFriendsActivityFeed(limit: Int): Flow<List<ActivityFeedItem>> {
        return getActivityFeed(limit)
    }

    override suspend fun postActivity(type: ActivityType, content: ActivityContent) {
        val userId = firebaseService.getCurrentUserId() ?: return
        val userProfile = firebaseService.getUserProfile()
        val userName = userProfile?.displayName ?: "User"
        val userEmoji = userProfile?.profileEmoji ?: "👤"
        val message = content.toString()
        val habitId: String? = null // Extract from content if needed
        firebaseService.postActivity(userId, userName, userEmoji, type.name, message, habitId)
    }

    override suspend fun hideActivityItem(itemId: String) {
        val current = getStoredActivityFeed()
        val updated = current.filter { it.id != itemId }
        dataStoreManager.putString(Keys.ACTIVITY_FEED, json.encodeToString(updated))
    }

    // ============== REACTIONS (REAL FIREBASE) ==============

    override suspend fun addReaction(activityId: String, reactionType: ReactionType): Reaction? {
        val userId = firebaseService.getCurrentUserId() ?: return null
        val result = firebaseService.addReaction(activityId, userId, reactionType.name)
        return if (result.isSuccess) {
            val userProfile = firebaseService.getUserProfile()
            Reaction(
                id = "reaction_${now().hashCode()}",
                userId = userId,
                userName = userProfile?.displayName ?: "Me",
                type = reactionType,
                timestamp = now()
            )
        } else null
    }

    override suspend fun removeReaction(activityId: String, reactionId: String): Boolean {
        // Would need to enhance FirebaseService for this
        return true
    }

    override fun getReactionsForActivity(activityId: String): Flow<List<Reaction>> {
        return getActivityFeed(100).map { feed ->
            feed.find { it.id == activityId }?.reactions ?: emptyList()
        }
    }

    // ============== CHEERS (REAL FIREBASE) ==============

    override fun getReceivedCheers(): Flow<List<Cheer>> {
        val userId = runBlocking { firebaseService.getCurrentUserId() } ?: return MutableStateFlow(emptyList())
        return firebaseService.getReceivedCheers(userId).map { cheerDataList ->
            cheerDataList.map { fbCheer ->
                Cheer(
                    id = fbCheer.id,
                    fromUserId = fbCheer.fromUserId,
                    fromUserName = fbCheer.fromUserName,
                    fromUserEmoji = "😊", // CheerData doesn't have this field
                    toUserId = userId,
                    message = fbCheer.message ?: "",
                    cheerType = try { CheerType.valueOf(fbCheer.cheerType) } catch (e: Exception) { CheerType.ENCOURAGEMENT },
                    timestamp = fbCheer.createdAt,
                    isRead = fbCheer.isRead
                )
            }
        }
    }

    override fun getSentCheers(): Flow<List<Cheer>> {
        return dataStoreManager.getString(Keys.SENT_CHEERS).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) emptyList()
            else {
                try {
                    json.decodeFromString(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override fun getUnreadCheersCount(): Flow<Int> {
        return getReceivedCheers().map { cheers ->
            cheers.count { !it.isRead }
        }
    }

    override suspend fun sendCheer(toUserId: String, toUserName: String, cheerType: CheerType, message: String?): Cheer? {
        val fromUserId = firebaseService.getCurrentUserId() ?: return null
        val userProfile = firebaseService.getUserProfile()
        val fromUserName = userProfile?.displayName ?: "User"

        val result = firebaseService.sendCheer(
            fromUserId = fromUserId,
            fromUserName = fromUserName,
            toUserId = toUserId,
            cheerType = cheerType.name,
            message = message ?: cheerType.defaultMessage
        )

        return if (result.isSuccess) {
            Cheer(
                id = "cheer_${now().hashCode()}",
                fromUserId = fromUserId,
                fromUserName = fromUserName,
                fromUserEmoji = userProfile?.profileEmoji ?: "😊",
                toUserId = toUserId,
                message = message ?: cheerType.defaultMessage,
                cheerType = cheerType,
                timestamp = now(),
                isRead = false
            )
        } else null
    }

    override suspend fun markCheerAsRead(cheerId: String) {
        firebaseService.markCheerAsRead(cheerId)
    }

    override suspend fun markAllCheersAsRead() {
        val userId = firebaseService.getCurrentUserId() ?: return
        val cheers = firebaseService.getReceivedCheers(userId).first()
        cheers.filter { !it.isRead }.forEach { cheer ->
            firebaseService.markCheerAsRead(cheer.id)
        }
    }

    // ============== REFERRALS (Local + Firebase) ==============

    override fun getReferralCode(): Flow<ReferralCode?> {
        return dataStoreManager.getString(Keys.REFERRAL_CODE).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) null
            else {
                try {
                    json.decodeFromString(jsonStr)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override fun getReferralStats(): Flow<ReferralStats> {
        return dataStoreManager.getString(Keys.REFERRAL_STATS).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) ReferralStats()
            else {
                try {
                    json.decodeFromString(jsonStr)
                } catch (e: Exception) {
                    ReferralStats()
                }
            }
        }
    }

    override suspend fun generateReferralCode(): ReferralCode {
        val existing = getReferralCodeData()
        if (existing != null) return existing

        val userId = firebaseService.getCurrentUserId() ?: "anonymous_${System.currentTimeMillis()}"

        val code = ReferralCode(
            code = ReferralConfig.generateCode(userId),
            userId = userId,
            createdAt = now()
        )

        dataStoreManager.putString(Keys.REFERRAL_CODE, json.encodeToString(code))
        return code
    }

    override suspend fun applyReferralCode(code: String): ReferralResult {
        val appliedCode = dataStoreManager.getString(Keys.APPLIED_REFERRAL).first()
        if (!appliedCode.isNullOrEmpty()) {
            return ReferralResult.AlreadyUsed
        }

        val myCode = getReferralCodeData()
        if (myCode?.code == code) {
            return ReferralResult.OwnCode
        }

        if (!code.matches(Regex("DW[A-Z0-9]{6}"))) {
            return ReferralResult.InvalidCode
        }

        dataStoreManager.putString(Keys.APPLIED_REFERRAL, code)

        val reward = ReferralConfig.defaultReward.forReferred
        return ReferralResult.Success(reward)
    }

    override suspend fun getReferralHistory(): List<ReferralHistoryEntry> {
        val jsonStr = dataStoreManager.getString(Keys.REFERRAL_HISTORY).first()
        return if (jsonStr.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ============== FRIENDS (REAL FIREBASE) ==============

    override fun getFriends(): Flow<List<Friend>> {
        val friends = MutableStateFlow<List<Friend>>(emptyList())

        return friends.also {
            kotlinx.coroutines.GlobalScope.launch {
                val userId = firebaseService.getCurrentUserId() ?: return@launch
                val leaderboardFriends = firebaseService.getFriendsLeaderboard(userId)
                val mappedFriends = leaderboardFriends
                    .filter { it.userId != userId } // Exclude self
                    .map { entry ->
                        Friend(
                            odId = "friend_${entry.userId}",
                            odUserId = entry.userId,
                            displayName = entry.displayName,
                            avatarEmoji = entry.profileEmoji,
                            level = entry.currentLevel,
                            currentStreak = entry.currentStreak,
                            lastActive = now(),
                            friendSince = now()
                        )
                    }
                friends.value = mappedFriends
            }
        }
    }

    override fun getFriendRequests(): Flow<List<FriendRequest>> {
        // TODO: Implement when Firebase method is available
        return MutableStateFlow<List<FriendRequest>>(emptyList())
    }

    override fun getSentFriendRequests(): Flow<List<FriendRequest>> {
        return dataStoreManager.getString(Keys.SENT_FRIEND_REQUESTS).map { jsonStr ->
            if (jsonStr.isNullOrEmpty()) emptyList()
            else {
                try {
                    json.decodeFromString(jsonStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    override suspend fun sendFriendRequest(userId: String, userName: String): FriendRequest? {
        val currentUserId = firebaseService.getCurrentUserId() ?: return null
        val result = firebaseService.sendFriendRequest(currentUserId, userId)

        return if (result.isSuccess) {
            FriendRequest(
                id = "request_${System.currentTimeMillis()}",
                fromUserId = currentUserId,
                fromUserName = userName,
                fromUserEmoji = "😊",
                toUserId = userId,
                status = FriendRequestStatus.PENDING,
                createdAt = now()
            )
        } else null
    }

    override suspend fun acceptFriendRequest(requestId: String): Boolean {
        // TODO: Implement when Firebase method is available
        return false
    }

    override suspend fun declineFriendRequest(requestId: String): Boolean {
        // TODO: Implement when Firebase method is available
        return false
    }

    override suspend fun removeFriend(friendId: String): Boolean {
        // TODO: Implement when Firebase method is available
        return false
    }

    override suspend fun searchUsers(query: String): List<UserSearchResult> {
        val firebaseResults = firebaseService.searchUsers(query)
        return firebaseResults.map { fbResult ->
            UserSearchResult(
                userId = fbResult.userId,
                displayName = fbResult.displayName,
                avatarEmoji = fbResult.profileEmoji,
                level = fbResult.currentLevel,
                isFriend = false,  // TODO: Check actual friend status
                hasPendingRequest = false  // TODO: Check actual pending request status
            )
        }
    }

    // ============== HELPERS ==============

    private fun mapActivityType(type: String): ActivityType {
        return try {
            ActivityType.valueOf(type)
        } catch (e: Exception) {
            ActivityType.STREAK_MILESTONE
        }
    }

    private fun mapActivityContent(type: String, content: Map<String, Any>): ActivityContent {
        return when (type) {
            "STREAK_MILESTONE" -> ActivityContent.StreakMilestone((content["days"] as? Number)?.toInt() ?: 0)
            "PERFECT_DAY" -> ActivityContent.PerfectDay((content["habitsCompleted"] as? Number)?.toInt() ?: 0)
            "BADGE_EARNED" -> ActivityContent.BadgeEarned(
                content["badgeId"] as? String ?: "",
                content["badgeName"] as? String ?: "",
                content["badgeEmoji"] as? String ?: "🏆"
            )
            "LEVEL_UP" -> ActivityContent.LevelUp(
                (content["newLevel"] as? Number)?.toInt() ?: 1,
                content["levelTitle"] as? String ?: ""
            )
            "CHALLENGE_COMPLETED" -> ActivityContent.ChallengeCompleted(
                content["challengeName"] as? String ?: "",
                content["reward"] as? String ?: ""
            )
            else -> ActivityContent.StreakMilestone(0)
        }
    }

    private fun mapActivityContentFromMessage(type: String, message: String): ActivityContent {
        // Parse message string to create appropriate ActivityContent
        return when (type) {
            "STREAK_MILESTONE" -> ActivityContent.StreakMilestone(7) // Default streak
            "PERFECT_DAY" -> ActivityContent.PerfectDay(3) // Default habits
            "BADGE_EARNED" -> ActivityContent.BadgeEarned("badge", message, "🏆")
            "LEVEL_UP" -> ActivityContent.LevelUp(1, message)
            "CHALLENGE_COMPLETED" -> ActivityContent.ChallengeCompleted(message, "")
            "HABIT_COMPLETED" -> ActivityContent.StreakMilestone(1)
            else -> ActivityContent.StreakMilestone(0)
        }
    }

    private suspend fun getStoredActivityFeed(): List<ActivityFeedItem> {
        val jsonStr = dataStoreManager.getString(Keys.ACTIVITY_FEED).first()
        return if (jsonStr.isNullOrEmpty()) emptyList()
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun getReferralCodeData(): ReferralCode? {
        val jsonStr = dataStoreManager.getString(Keys.REFERRAL_CODE).first()
        return if (jsonStr.isNullOrEmpty()) null
        else {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: Exception) {
                null
            }
        }
    }

    // ============== USER SYNC (REAL FIREBASE) ==============

    /**
     * Sync current user's progress to Firebase
     */
    suspend fun syncUserProgress(
        streak: Int,
        totalCompletions: Int,
        level: Int,
        xp: Int,
        badges: List<String>
    ) {
        val userId = firebaseService.getCurrentUserId() ?: return
        firebaseService.syncUserProgress(
            userId = userId,
            totalXp = xp.toLong(),
            currentLevel = level,
            currentStreak = streak,
            longestStreak = streak,
            habitsCompleted = totalCompletions
        )
    }

    /**
     * Get or create user profile
     */
    suspend fun ensureUserProfile(): Boolean {
        return firebaseService.signInAnonymously().isSuccess
    }

    /**
     * Update user display name
     */
    suspend fun updateDisplayName(name: String, emoji: String): Boolean {
        val userId = firebaseService.getCurrentUserId() ?: return false
        return firebaseService.updateDisplayName(userId, name).isSuccess
    }
}

// Extension to launch coroutines
private fun kotlinx.coroutines.GlobalScope.launch(dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO, block: suspend () -> Unit) {
    kotlinx.coroutines.GlobalScope.launch(dispatcher) {
        block()
    }
}
