package com.dailywell.app.api

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock

/**
 * Firebase service for real-time cloud sync, authentication, and social features
 */
class FirebaseService {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Collections
    private val usersCollection = "users"
    private val habitsCollection = "habits"
    private val entriesCollection = "entries"
    private val leaderboardCollection = "leaderboard"
    private val socialCollection = "social"
    private val friendsCollection = "friends"
    private val cheersCollection = "cheers"
    private val activityCollection = "activity"

    // ==================== AUTHENTICATION ====================

    /**
     * Get current user or null
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Sign in anonymously (for users who don't want to create account)
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.let { user ->
                // Create user profile in Firestore
                createUserProfile(user.uid)
                Result.success(user)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with email/password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create account with email/password
     */
    suspend fun createAccount(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                createUserProfile(user.uid, displayName, email)
                Result.success(user)
            } ?: Result.failure(Exception("Account creation failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        auth.signOut()
    }

    private suspend fun createUserProfile(
        userId: String,
        displayName: String = "User${userId.take(4)}",
        email: String? = null
    ) {
        val profile = hashMapOf(
            "userId" to userId,
            "displayName" to displayName,
            "email" to email,
            "profileEmoji" to listOf("😊", "🌟", "💪", "🎯", "🔥").random(),
            "createdAt" to Clock.System.now().toString(),
            "totalXp" to 0L,
            "currentLevel" to 1,
            "currentStreak" to 0,
            "longestStreak" to 0,
            "isPremium" to false
        )

        firestore.collection(usersCollection)
            .document(userId)
            .set(profile, SetOptions.merge())
            .await()
    }

    // ==================== USER SYNC ====================

    /**
     * Sync user progress to cloud
     */
    suspend fun syncUserProgress(
        userId: String,
        totalXp: Long,
        currentLevel: Int,
        currentStreak: Int,
        longestStreak: Int,
        habitsCompleted: Int
    ): Result<Unit> {
        return try {
            val data = hashMapOf(
                "totalXp" to totalXp,
                "currentLevel" to currentLevel,
                "currentStreak" to currentStreak,
                "longestStreak" to longestStreak,
                "habitsCompleted" to habitsCompleted,
                "lastSyncedAt" to Clock.System.now().toString()
            )

            firestore.collection(usersCollection)
                .document(userId)
                .set(data, SetOptions.merge())
                .await()

            // Also update leaderboard
            updateLeaderboardEntry(userId, totalXp, currentStreak, currentLevel)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync user badges to cloud
     */
    suspend fun syncUserBadges(userId: String, badgeIds: List<String>): Result<Unit> {
        return try {
            val data = hashMapOf(
                "unlockedBadges" to badgeIds,
                "badgesSyncedAt" to Clock.System.now().toString()
            )

            firestore.collection(usersCollection)
                .document(userId)
                .set(data, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user profile
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile> {
        return try {
            val doc = firestore.collection(usersCollection)
                .document(userId)
                .get()
                .await()

            val profile = UserProfile(
                userId = doc.getString("userId") ?: userId,
                displayName = doc.getString("displayName") ?: "User",
                profileEmoji = doc.getString("profileEmoji") ?: "😊",
                totalXp = doc.getLong("totalXp") ?: 0L,
                currentLevel = doc.getLong("currentLevel")?.toInt() ?: 1,
                currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                longestStreak = doc.getLong("longestStreak")?.toInt() ?: 0,
                isPremium = doc.getBoolean("isPremium") ?: false
            )

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update display name
     */
    suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit> {
        return try {
            firestore.collection(usersCollection)
                .document(userId)
                .update("displayName", displayName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== REAL LEADERBOARDS ====================

    private suspend fun updateLeaderboardEntry(
        userId: String,
        totalXp: Long,
        currentStreak: Int,
        currentLevel: Int
    ) {
        val profile = getUserProfile(userId).getOrNull() ?: return

        val entry = hashMapOf(
            "userId" to userId,
            "displayName" to profile.displayName,
            "profileEmoji" to profile.profileEmoji,
            "totalXp" to totalXp,
            "weeklyXp" to (totalXp % 10000), // Simplified weekly XP
            "currentStreak" to currentStreak,
            "currentLevel" to currentLevel,
            "updatedAt" to Clock.System.now().toString()
        )

        firestore.collection(leaderboardCollection)
            .document(userId)
            .set(entry, SetOptions.merge())
            .await()
    }

    /**
     * Get global leaderboard (real users!)
     */
    fun getGlobalLeaderboard(limit: Int = 50): Flow<List<LeaderboardEntry>> = callbackFlow {
        val listener = firestore.collection(leaderboardCollection)
            .orderBy("totalXp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapIndexed { index, doc ->
                    LeaderboardEntry(
                        rank = index + 1,
                        userId = doc.getString("userId") ?: "",
                        displayName = doc.getString("displayName") ?: "User",
                        profileEmoji = doc.getString("profileEmoji") ?: "😊",
                        totalXp = doc.getLong("totalXp") ?: 0L,
                        weeklyXp = doc.getLong("weeklyXp") ?: 0L,
                        currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                        currentLevel = doc.getLong("currentLevel")?.toInt() ?: 1
                    )
                } ?: emptyList()

                trySend(entries)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get friends leaderboard
     */
    suspend fun getFriendsLeaderboard(userId: String): List<LeaderboardEntry> {
        val friendIds = getFriendIds(userId)
        if (friendIds.isEmpty()) return emptyList()

        // Include self in friends leaderboard
        val allIds = friendIds + userId

        val entries = mutableListOf<LeaderboardEntry>()

        for (friendId in allIds) {
            try {
                val doc = firestore.collection(leaderboardCollection)
                    .document(friendId)
                    .get()
                    .await()

                if (doc.exists()) {
                    entries.add(
                        LeaderboardEntry(
                            rank = 0, // Will be set after sorting
                            userId = doc.getString("userId") ?: "",
                            displayName = doc.getString("displayName") ?: "User",
                            profileEmoji = doc.getString("profileEmoji") ?: "😊",
                            totalXp = doc.getLong("totalXp") ?: 0L,
                            weeklyXp = doc.getLong("weeklyXp") ?: 0L,
                            currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0,
                            currentLevel = doc.getLong("currentLevel")?.toInt() ?: 1
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip failed entries
            }
        }

        // Sort and assign ranks
        return entries.sortedByDescending { it.totalXp }
            .mapIndexed { index, entry -> entry.copy(rank = index + 1) }
    }

    // ==================== FRIENDS SYSTEM ====================

    private suspend fun getFriendIds(userId: String): List<String> {
        return try {
            val doc = firestore.collection(usersCollection)
                .document(userId)
                .collection(friendsCollection)
                .get()
                .await()

            doc.documents.mapNotNull { it.getString("friendId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Send friend request
     */
    suspend fun sendFriendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            val request = hashMapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "status" to "pending",
                "createdAt" to Clock.System.now().toString()
            )

            firestore.collection(socialCollection)
                .document("requests")
                .collection("pending")
                .document("${fromUserId}_$toUserId")
                .set(request)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Accept friend request
     */
    suspend fun acceptFriendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            // Add to both users' friends lists
            val friendData1 = hashMapOf(
                "friendId" to fromUserId,
                "addedAt" to Clock.System.now().toString()
            )
            val friendData2 = hashMapOf(
                "friendId" to toUserId,
                "addedAt" to Clock.System.now().toString()
            )

            // Add friend to requester's list
            firestore.collection(usersCollection)
                .document(toUserId)
                .collection(friendsCollection)
                .document(fromUserId)
                .set(friendData1)
                .await()

            // Add friend to accepter's list
            firestore.collection(usersCollection)
                .document(fromUserId)
                .collection(friendsCollection)
                .document(toUserId)
                .set(friendData2)
                .await()

            // Remove pending request
            firestore.collection(socialCollection)
                .document("requests")
                .collection("pending")
                .document("${fromUserId}_$toUserId")
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search for users by display name
     */
    suspend fun searchUsers(query: String, limit: Int = 10): List<UserSearchResult> {
        return try {
            val results = firestore.collection(usersCollection)
                .orderBy("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(limit.toLong())
                .get()
                .await()

            results.documents.map { doc ->
                UserSearchResult(
                    userId = doc.getString("userId") ?: doc.id,
                    displayName = doc.getString("displayName") ?: "User",
                    profileEmoji = doc.getString("profileEmoji") ?: "😊",
                    currentLevel = doc.getLong("currentLevel")?.toInt() ?: 1
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== CHEERS SYSTEM ====================

    /**
     * Send a cheer to another user
     */
    suspend fun sendCheer(
        fromUserId: String,
        fromUserName: String,
        toUserId: String,
        cheerType: String,
        message: String?
    ): Result<Unit> {
        return try {
            val cheer = hashMapOf(
                "fromUserId" to fromUserId,
                "fromUserName" to fromUserName,
                "toUserId" to toUserId,
                "cheerType" to cheerType,
                "message" to message,
                "isRead" to false,
                "createdAt" to Clock.System.now().toString()
            )

            firestore.collection(cheersCollection)
                .add(cheer)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get received cheers for a user
     */
    fun getReceivedCheers(userId: String): Flow<List<CheerData>> = callbackFlow {
        val listener = firestore.collection(cheersCollection)
            .whereEqualTo("toUserId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val cheers = snapshot?.documents?.map { doc ->
                    CheerData(
                        id = doc.id,
                        fromUserId = doc.getString("fromUserId") ?: "",
                        fromUserName = doc.getString("fromUserName") ?: "User",
                        cheerType = doc.getString("cheerType") ?: "wave",
                        message = doc.getString("message"),
                        isRead = doc.getBoolean("isRead") ?: false,
                        createdAt = doc.getString("createdAt") ?: ""
                    )
                } ?: emptyList()

                trySend(cheers)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Mark cheer as read
     */
    suspend fun markCheerAsRead(cheerId: String): Result<Unit> {
        return try {
            firestore.collection(cheersCollection)
                .document(cheerId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== FAMILY PLAN ====================

    private val familiesCollection = "families"

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    /**
     * Get current user profile
     */
    suspend fun getUserProfile(): UserProfile? {
        val userId = getCurrentUserId() ?: return null
        return getUserProfile(userId).getOrNull()
    }

    /**
     * Create a new family
     */
    suspend fun createFamily(familyId: String, familyName: String, inviteCode: String): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            val family = hashMapOf(
                "familyId" to familyId,
                "familyName" to familyName,
                "inviteCode" to inviteCode,
                "ownerId" to userId,
                "createdAt" to Clock.System.now().toString(),
                "members" to listOf(userId)
            )

            firestore.collection(familiesCollection)
                .document(familyId)
                .set(family)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Join family by invite code
     */
    suspend fun joinFamilyByInviteCode(inviteCode: String): FamilyJoinResult? {
        val userId = getCurrentUserId() ?: return null

        return try {
            val result = firestore.collection(familiesCollection)
                .whereEqualTo("inviteCode", inviteCode)
                .get()
                .await()

            val familyDoc = result.documents.firstOrNull() ?: return null
            val familyId = familyDoc.id

            // Add user to family members
            val members = (familyDoc.get("members") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                ?: mutableListOf()
            if (userId !in members) {
                members.add(userId)
                firestore.collection(familiesCollection)
                    .document(familyId)
                    .update("members", members)
                    .await()
            }

            FamilyJoinResult(familyId = familyId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Leave family
     */
    suspend fun leaveFamily(familyId: String): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            val doc = firestore.collection(familiesCollection)
                .document(familyId)
                .get()
                .await()

            val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                ?: mutableListOf()
            members.remove(userId)

            if (members.isEmpty()) {
                // Delete family if no members left
                firestore.collection(familiesCollection)
                    .document(familyId)
                    .delete()
                    .await()
            } else {
                firestore.collection(familiesCollection)
                    .document(familyId)
                    .update("members", members)
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get family data
     */
    suspend fun getFamilyData(familyId: String): com.dailywell.app.data.model.FamilyPlanData? {
        return try {
            val doc = firestore.collection(familiesCollection)
                .document(familyId)
                .get()
                .await()

            if (!doc.exists()) return null

            // Get member details
            val memberIds = (doc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val members = memberIds.mapNotNull { memberId ->
                getUserProfile(memberId).getOrNull()?.let { profile ->
                    com.dailywell.app.data.model.FamilyMember(
                        id = profile.userId,
                        name = profile.displayName,
                        avatar = profile.profileEmoji,
                        role = if (memberId == doc.getString("ownerId"))
                            com.dailywell.app.data.model.FamilyRole.OWNER
                        else
                            com.dailywell.app.data.model.FamilyRole.ADULT,
                        currentStreak = profile.currentStreak,
                        joinedAt = Clock.System.now().toString()
                    )
                }
            }

            com.dailywell.app.data.model.FamilyPlanData(
                familyId = familyId,
                isOwner = doc.getString("ownerId") == getCurrentUserId(),
                members = members,
                inviteCode = doc.getString("inviteCode") ?: "",
                createdAt = doc.getString("createdAt") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update family data
     */
    suspend fun updateFamilyData(familyId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update family invite code
     */
    suspend fun updateFamilyInviteCode(familyId: String, code: String): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .update("inviteCode", code)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove family member
     */
    suspend fun removeFamilyMember(familyId: String, memberId: String): Result<Unit> {
        return try {
            val doc = firestore.collection(familiesCollection)
                .document(familyId)
                .get()
                .await()

            val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                ?: mutableListOf()
            members.remove(memberId)

            firestore.collection(familiesCollection)
                .document(familyId)
                .update("members", members)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update family member role
     */
    suspend fun updateFamilyMemberRole(familyId: String, memberId: String, role: String): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("memberRoles")
                .document(memberId)
                .set(mapOf("role" to role))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create family challenge
     */
    suspend fun createFamilyChallenge(familyId: String, challenge: com.dailywell.app.data.model.FamilyChallenge): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("challenges")
                .document(challenge.id)
                .set(mapOf(
                    "id" to challenge.id,
                    "title" to challenge.title,
                    "description" to challenge.description,
                    "type" to challenge.type.name,
                    "status" to challenge.status.name,
                    "startDate" to challenge.startDate,
                    "targetValue" to challenge.targetValue,
                    "reward" to challenge.reward
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update challenge progress
     */
    suspend fun updateFamilyChallengeProgress(familyId: String, challengeId: String, memberId: String, progress: Int): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("challenges")
                .document(challengeId)
                .update("progress.$memberId", progress)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Complete family challenge
     */
    suspend fun completeFamilyChallenge(familyId: String, challengeId: String, winnerId: String?): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to "COMPLETED",
                "endDate" to Clock.System.now().toString().substringBefore("T")
            )
            winnerId?.let { updates["winnerId"] = it }

            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("challenges")
                .document(challengeId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel family challenge
     */
    suspend fun cancelFamilyChallenge(familyId: String, challengeId: String): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("challenges")
                .document(challengeId)
                .update("status", "CANCELLED")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add family milestone
     */
    suspend fun addFamilyMilestone(familyId: String, milestone: com.dailywell.app.data.model.FamilyMilestone): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("milestones")
                .document(milestone.id)
                .set(mapOf(
                    "id" to milestone.id,
                    "title" to milestone.title,
                    "description" to milestone.description,
                    "emoji" to milestone.emoji,
                    "achievedAt" to milestone.achievedAt,
                    "type" to milestone.type.name
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send family high five
     */
    suspend fun sendFamilyHighFive(familyId: String, activityId: String, userId: String): Result<Unit> {
        return try {
            val activityRef = firestore.collection(familiesCollection)
                .document(familyId)
                .collection("activities")
                .document(activityId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(activityRef)
                val highFives = (snapshot.get("highFives") as? List<*>)?.toMutableList() ?: mutableListOf()
                if (userId !in highFives) {
                    highFives.add(userId)
                    transaction.update(activityRef, "highFives", highFives)
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Log family activity
     */
    suspend fun logFamilyActivity(familyId: String, activity: com.dailywell.app.data.model.FamilyActivity): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("activities")
                .document(activity.id)
                .set(mapOf(
                    "id" to activity.id,
                    "memberId" to activity.memberId,
                    "memberName" to activity.memberName,
                    "type" to activity.activityType.name,
                    "message" to activity.message,
                    "timestamp" to activity.timestamp,
                    "highFives" to activity.highFives
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update member shared habits
     */
    suspend fun updateMemberSharedHabits(familyId: String, memberId: String, habitIds: List<String>): Result<Unit> {
        return try {
            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("memberData")
                .document(memberId)
                .set(mapOf("sharedHabits" to habitIds), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== COMMUNITY CHALLENGES ====================

    private val communityCollection = "community_challenges"
    private val seasonalCollection = "seasonal_events"

    /**
     * Get active community challenge from Firebase
     */
    suspend fun getActiveCommunityChallenge(): com.dailywell.app.data.model.CommunityChallenge? {
        return try {
            val now = Clock.System.now().toString().substringBefore("T")
            val result = firestore.collection(communityCollection)
                .whereGreaterThanOrEqualTo("endDate", now)
                .orderBy("endDate")
                .limit(1)
                .get()
                .await()

            val doc = result.documents.firstOrNull() ?: return null

            com.dailywell.app.data.model.CommunityChallenge(
                id = doc.id,
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                emoji = doc.getString("emoji") ?: "🎯",
                goal = com.dailywell.app.data.model.CommunityChallengeGoal(
                    type = com.dailywell.app.data.model.CommunityGoalType.TOTAL_HABITS,
                    target = doc.getLong("goalTarget") ?: 1000000L,
                    description = doc.getString("goalDescription") ?: ""
                ),
                startDate = doc.getString("startDate") ?: "",
                endDate = doc.getString("endDate") ?: "",
                globalProgress = doc.getLong("globalProgress") ?: 0L,
                globalTarget = doc.getLong("goalTarget") ?: 1000000L,
                rewards = com.dailywell.app.data.model.CommunityRewards(
                    baseXp = doc.getLong("rewardXp") ?: 500L,
                    completionBadge = doc.getString("rewardBadge") ?: "",
                    specialTitle = doc.getString("rewardTitle")
                ),
                tiers = emptyList(), // Would parse from sub-collection
                totalParticipants = doc.getLong("totalParticipants")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get community challenge leaderboard
     */
    suspend fun getCommunityLeaderboard(challengeId: String, limit: Int): List<com.dailywell.app.data.model.ChallengeParticipant> {
        return try {
            val result = firestore.collection(communityCollection)
                .document(challengeId)
                .collection("participants")
                .orderBy("contribution", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            result.documents.mapIndexed { index, doc ->
                com.dailywell.app.data.model.ChallengeParticipant(
                    odId = doc.id,
                    challengeId = challengeId,
                    userId = doc.getString("userId") ?: "",
                    displayName = doc.getString("displayName") ?: "User",
                    avatarEmoji = doc.getString("emoji") ?: "😊",
                    joinedAt = doc.getString("joinedAt") ?: "",
                    progress = (doc.getLong("contribution")?.toFloat() ?: 0f) / 1000f,
                    currentValue = doc.getLong("contribution")?.toInt() ?: 0,
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    completedAt = doc.getString("completedAt"),
                    rank = index + 1
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get active seasonal event from Firebase
     */
    suspend fun getActiveSeasonalEvent(): com.dailywell.app.data.model.SeasonalEvent? {
        return try {
            val now = Clock.System.now().toString().substringBefore("T")
            val result = firestore.collection(seasonalCollection)
                .whereGreaterThanOrEqualTo("endDate", now)
                .orderBy("endDate")
                .limit(1)
                .get()
                .await()

            val doc = result.documents.firstOrNull() ?: return null

            com.dailywell.app.data.model.SeasonalEvent(
                id = doc.id,
                name = doc.getString("name") ?: "",
                description = doc.getString("description") ?: "",
                emoji = doc.getString("emoji") ?: "🎉",
                theme = doc.getString("theme") ?: "default",
                startDate = doc.getString("startDate") ?: "",
                endDate = doc.getString("endDate") ?: "",
                challenges = emptyList(), // Would parse from sub-collection
                exclusiveRewards = emptyList(), // Would parse from sub-collection
                specialBadge = doc.getString("specialBadge")
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Contribute to community challenge
     */
    suspend fun contributeToCommuntiyChallenge(challengeId: String, amount: Int): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            // Update user's contribution
            val participantRef = firestore.collection(communityCollection)
                .document(challengeId)
                .collection("participants")
                .document(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(participantRef)
                val currentContribution = snapshot.getLong("contribution")?.toInt() ?: 0
                transaction.set(participantRef, mapOf(
                    "userId" to userId,
                    "contribution" to (currentContribution + amount),
                    "lastUpdated" to Clock.System.now().toString()
                ), SetOptions.merge())
            }.await()

            // Update global progress
            val challengeRef = firestore.collection(communityCollection).document(challengeId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(challengeRef)
                val globalProgress = snapshot.getLong("globalProgress")?.toInt() ?: 0
                transaction.update(challengeRef, "globalProgress", globalProgress + amount)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== ACTIVITY FEED ====================

    /**
     * Post activity to feed
     */
    suspend fun postActivity(
        userId: String,
        userName: String,
        userEmoji: String,
        activityType: String,
        message: String,
        habitId: String? = null
    ): Result<Unit> {
        return try {
            val activity = hashMapOf(
                "userId" to userId,
                "userName" to userName,
                "userEmoji" to userEmoji,
                "activityType" to activityType,
                "message" to message,
                "habitId" to habitId,
                "reactions" to emptyList<String>(),
                "createdAt" to Clock.System.now().toString()
            )

            firestore.collection(activityCollection)
                .add(activity)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get global activity feed
     */
    fun getActivityFeed(limit: Int = 50): Flow<List<ActivityData>> = callbackFlow {
        val listener = firestore.collection(activityCollection)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val activities = snapshot?.documents?.map { doc ->
                    ActivityData(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "User",
                        userEmoji = doc.getString("userEmoji") ?: "😊",
                        activityType = doc.getString("activityType") ?: "",
                        message = doc.getString("message") ?: "",
                        habitId = doc.getString("habitId"),
                        reactions = (doc.get("reactions") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        createdAt = doc.getString("createdAt") ?: ""
                    )
                } ?: emptyList()

                trySend(activities)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Add reaction to activity
     */
    suspend fun addReaction(activityId: String, userId: String, reaction: String): Result<Unit> {
        return try {
            val activityRef = firestore.collection(activityCollection).document(activityId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(activityRef)
                val reactions = (snapshot.get("reactions") as? List<*>)?.toMutableList() ?: mutableListOf()
                reactions.add("$userId:$reaction")
                transaction.update(activityRef, "reactions", reactions)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Data classes
data class UserProfile(
    val userId: String,
    val displayName: String,
    val profileEmoji: String,
    val totalXp: Long,
    val currentLevel: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val isPremium: Boolean
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val displayName: String,
    val profileEmoji: String,
    val totalXp: Long,
    val weeklyXp: Long,
    val currentStreak: Int,
    val currentLevel: Int
)

data class UserSearchResult(
    val userId: String,
    val displayName: String,
    val profileEmoji: String,
    val currentLevel: Int
)

data class CheerData(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val cheerType: String,
    val message: String?,
    val isRead: Boolean,
    val createdAt: String
)

data class ActivityData(
    val id: String,
    val userId: String,
    val userName: String,
    val userEmoji: String,
    val activityType: String,
    val message: String,
    val habitId: String?,
    val reactions: List<String>,
    val createdAt: String
)

data class FamilyJoinResult(
    val familyId: String
)
