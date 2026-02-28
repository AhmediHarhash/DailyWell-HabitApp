package com.dailywell.app.api

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock

/**
 * Firebase service for real-time cloud sync, authentication, and social features
 */
class FirebaseService {

    companion object {
        private const val TAG = "FirebaseService"
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Auth state tracking
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _authState.value = firebaseAuth.currentUser.toAuthState()
        }
    }

    private fun FirebaseUser?.toAuthState(): AuthState {
        if (this == null) return AuthState.SignedOut
        if (isAnonymous) return AuthState.Anonymous(uid)
        return AuthState.Authenticated(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString(),
            isEmailVerified = isEmailVerified,
            providers = providerData.map { it.providerId }.filter { it != "firebase" }
        )
    }

    // Collections
    private val usersCollection = "users"
    private val habitsCollection = "habits"
    private val entriesCollection = "entries"
    private val leaderboardCollection = "leaderboard"
    private val socialCollection = "social"
    private val friendsCollection = "friends"
    private val cheersCollection = "cheers"
    private val activityCollection = "activity"
    private val groupsCollection = "groups"

    // ==================== ACCOUNTABILITY GROUPS ====================

    /**
     * Create a new accountability group
     * Structure: groups/{groupId} with members subcollection
     */
    suspend fun createGroup(group: com.dailywell.app.data.model.AccountabilityGroup): Result<String> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            val groupData = hashMapOf(
                "id" to group.id,
                "name" to group.name,
                "emoji" to group.emoji,
                "description" to (group.description ?: ""),
                "createdBy" to userId,
                "createdAt" to group.createdAt,
                "maxMembers" to group.maxMembers,
                "isPrivate" to group.isPrivate,
                "inviteCode" to (group.inviteCode ?: generateInviteCode()),
                "groupType" to group.groupType.name,
                "settings" to hashMapOf(
                    "showMemberStreaks" to group.settings.showMemberStreaks,
                    "allowHighFives" to group.settings.allowHighFives,
                    "allowComments" to group.settings.allowComments,
                    "weeklyDigest" to group.settings.weeklyDigest,
                    "leaderboardEnabled" to group.settings.leaderboardEnabled
                ),
                "memberIds" to listOf(userId) // Track member IDs for queries
            )

            firestore.collection(groupsCollection)
                .document(group.id)
                .set(groupData)
                .await()

            // Add creator as owner in members subcollection
            val creatorMember = hashMapOf(
                "userId" to userId,
                "displayName" to (getUserProfile(userId).getOrNull()?.displayName ?: "User"),
                "profileEmoji" to (getUserProfile(userId).getOrNull()?.profileEmoji ?: "😊"),
                "role" to "OWNER",
                "joinedAt" to group.createdAt,
                "sharedHabits" to emptyList<String>(),
                "currentStreak" to 0,
                "weeklyCompletionRate" to 0f
            )

            firestore.collection(groupsCollection)
                .document(group.id)
                .collection("members")
                .document(userId)
                .set(creatorMember)
                .await()

            Result.success(group.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get groups for a user (real-time flow)
     */
    fun getGroupsForUser(userId: String): Flow<List<com.dailywell.app.data.model.AccountabilityGroup>> = callbackFlow {
        val listener = firestore.collection(groupsCollection)
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val groups = snapshot?.documents?.mapNotNull { doc ->
                    parseGroupDocumentSync(doc)
                } ?: emptyList()
                trySend(groups)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Synchronous version of parseGroupDocument for use in callbacks
     * Fetches members in a blocking way (acceptable for small data)
     */
    private fun parseGroupDocumentSync(doc: com.google.firebase.firestore.DocumentSnapshot): com.dailywell.app.data.model.AccountabilityGroup? {
        return try {
            val groupId = doc.id
            val settingsMap = doc.get("settings") as? Map<*, *>

            // Parse members from the memberIds list (basic info from group doc)
            // Full member details will be fetched when needed
            val memberIds = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val members = memberIds.map { memberId ->
                com.dailywell.app.data.model.GroupMember(
                    userId = memberId,
                    displayName = "Member",
                    profileEmoji = "😊",
                    role = if (memberId == doc.getString("createdBy"))
                        com.dailywell.app.data.model.GroupRole.OWNER
                    else
                        com.dailywell.app.data.model.GroupRole.MEMBER,
                    joinedAt = doc.getString("createdAt") ?: "",
                    sharedHabits = emptyList(),
                    currentStreak = 0,
                    weeklyCompletionRate = 0f
                )
            }

            com.dailywell.app.data.model.AccountabilityGroup(
                id = groupId,
                name = doc.getString("name") ?: "",
                emoji = doc.getString("emoji") ?: "🎯",
                description = doc.getString("description"),
                createdBy = doc.getString("createdBy") ?: "",
                createdAt = doc.getString("createdAt") ?: "",
                members = members,
                maxMembers = doc.getLong("maxMembers")?.toInt() ?: 10,
                isPrivate = doc.getBoolean("isPrivate") ?: true,
                inviteCode = doc.getString("inviteCode"),
                groupType = try {
                    com.dailywell.app.data.model.GroupType.valueOf(doc.getString("groupType") ?: "GENERAL")
                } catch (e: Exception) {
                    com.dailywell.app.data.model.GroupType.GENERAL
                },
                settings = com.dailywell.app.data.model.GroupSettings(
                    showMemberStreaks = (settingsMap?.get("showMemberStreaks") as? Boolean) ?: true,
                    allowHighFives = (settingsMap?.get("allowHighFives") as? Boolean) ?: true,
                    allowComments = (settingsMap?.get("allowComments") as? Boolean) ?: false,
                    weeklyDigest = (settingsMap?.get("weeklyDigest") as? Boolean) ?: true,
                    leaderboardEnabled = (settingsMap?.get("leaderboardEnabled") as? Boolean) ?: false
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Full async version of parseGroupDocument with member details
     */
    private suspend fun parseGroupDocument(doc: com.google.firebase.firestore.DocumentSnapshot): com.dailywell.app.data.model.AccountabilityGroup? {
        return try {
            val groupId = doc.id
            val settingsMap = doc.get("settings") as? Map<*, *>

            // Fetch members from subcollection
            val membersSnapshot = firestore.collection(groupsCollection)
                .document(groupId)
                .collection("members")
                .get()
                .await()

            val members = membersSnapshot.documents.map { memberDoc ->
                com.dailywell.app.data.model.GroupMember(
                    userId = memberDoc.getString("userId") ?: "",
                    displayName = memberDoc.getString("displayName") ?: "User",
                    profileEmoji = memberDoc.getString("profileEmoji") ?: "😊",
                    role = try {
                        com.dailywell.app.data.model.GroupRole.valueOf(memberDoc.getString("role") ?: "MEMBER")
                    } catch (e: Exception) {
                        com.dailywell.app.data.model.GroupRole.MEMBER
                    },
                    joinedAt = memberDoc.getString("joinedAt") ?: "",
                    sharedHabits = (memberDoc.get("sharedHabits") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    currentStreak = memberDoc.getLong("currentStreak")?.toInt() ?: 0,
                    weeklyCompletionRate = memberDoc.getDouble("weeklyCompletionRate")?.toFloat() ?: 0f
                )
            }

            com.dailywell.app.data.model.AccountabilityGroup(
                id = groupId,
                name = doc.getString("name") ?: "",
                emoji = doc.getString("emoji") ?: "🎯",
                description = doc.getString("description"),
                createdBy = doc.getString("createdBy") ?: "",
                createdAt = doc.getString("createdAt") ?: "",
                members = members,
                maxMembers = doc.getLong("maxMembers")?.toInt() ?: 10,
                isPrivate = doc.getBoolean("isPrivate") ?: true,
                inviteCode = doc.getString("inviteCode"),
                groupType = try {
                    com.dailywell.app.data.model.GroupType.valueOf(doc.getString("groupType") ?: "GENERAL")
                } catch (e: Exception) {
                    com.dailywell.app.data.model.GroupType.GENERAL
                },
                settings = com.dailywell.app.data.model.GroupSettings(
                    showMemberStreaks = (settingsMap?.get("showMemberStreaks") as? Boolean) ?: true,
                    allowHighFives = (settingsMap?.get("allowHighFives") as? Boolean) ?: true,
                    allowComments = (settingsMap?.get("allowComments") as? Boolean) ?: false,
                    weeklyDigest = (settingsMap?.get("weeklyDigest") as? Boolean) ?: true,
                    leaderboardEnabled = (settingsMap?.get("leaderboardEnabled") as? Boolean) ?: false
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get a single group by ID
     */
    suspend fun getGroup(groupId: String): com.dailywell.app.data.model.AccountabilityGroup? {
        return try {
            val doc = firestore.collection(groupsCollection)
                .document(groupId)
                .get()
                .await()

            if (doc.exists()) parseGroupDocument(doc) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Find group by invite code
     */
    suspend fun getGroupByInviteCode(inviteCode: String): com.dailywell.app.data.model.AccountabilityGroup? {
        return try {
            val result = firestore.collection(groupsCollection)
                .whereEqualTo("inviteCode", inviteCode)
                .limit(1)
                .get()
                .await()

            result.documents.firstOrNull()?.let { parseGroupDocument(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Join a group
     */
    suspend fun joinGroup(groupId: String): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            val group = getGroup(groupId) ?: return Result.failure(Exception("Group not found"))

            // Check if group is full
            if (group.members.size >= group.maxMembers) {
                return Result.failure(Exception("Group is full"))
            }

            // Check if already a member
            if (group.members.any { it.userId == userId }) {
                return Result.success(Unit) // Already a member
            }

            val profile = getUserProfile(userId).getOrNull()
            val memberData = hashMapOf(
                "userId" to userId,
                "displayName" to (profile?.displayName ?: "User"),
                "profileEmoji" to (profile?.profileEmoji ?: "😊"),
                "role" to "MEMBER",
                "joinedAt" to Clock.System.now().toString(),
                "sharedHabits" to emptyList<String>(),
                "currentStreak" to (profile?.currentStreak ?: 0),
                "weeklyCompletionRate" to 0f
            )

            // Add to members subcollection
            firestore.collection(groupsCollection)
                .document(groupId)
                .collection("members")
                .document(userId)
                .set(memberData)
                .await()

            // Update memberIds array
            val groupRef = firestore.collection(groupsCollection).document(groupId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(groupRef)
                val memberIds = (snapshot.get("memberIds") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()
                if (userId !in memberIds) {
                    memberIds.add(userId)
                    transaction.update(groupRef, "memberIds", memberIds)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leave a group
     */
    suspend fun leaveGroup(groupId: String): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            // Remove from members subcollection
            firestore.collection(groupsCollection)
                .document(groupId)
                .collection("members")
                .document(userId)
                .delete()
                .await()

            // Update memberIds array
            val groupRef = firestore.collection(groupsCollection).document(groupId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(groupRef)
                val memberIds = (snapshot.get("memberIds") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()
                memberIds.remove(userId)

                if (memberIds.isEmpty()) {
                    // Delete group if no members left
                    transaction.delete(groupRef)
                } else {
                    transaction.update(groupRef, "memberIds", memberIds)

                    // If owner left, promote someone else
                    val createdBy = snapshot.getString("createdBy")
                    if (createdBy == userId && memberIds.isNotEmpty()) {
                        transaction.update(groupRef, "createdBy", memberIds.first())
                    }
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update group settings
     */
    suspend fun updateGroupSettings(groupId: String, settings: com.dailywell.app.data.model.GroupSettings): Result<Unit> {
        return try {
            val settingsMap = hashMapOf(
                "showMemberStreaks" to settings.showMemberStreaks,
                "allowHighFives" to settings.allowHighFives,
                "allowComments" to settings.allowComments,
                "weeklyDigest" to settings.weeklyDigest,
                "leaderboardEnabled" to settings.leaderboardEnabled
            )

            firestore.collection(groupsCollection)
                .document(groupId)
                .update("settings", settingsMap)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update group member's shared habits
     */
    suspend fun updateMemberSharedHabitsInGroup(groupId: String, habitIds: List<String>): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            firestore.collection(groupsCollection)
                .document(groupId)
                .collection("members")
                .document(userId)
                .update("sharedHabits", habitIds)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update member streak in group
     */
    suspend fun updateMemberStreak(groupId: String, streak: Int): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            firestore.collection(groupsCollection)
                .document(groupId)
                .collection("members")
                .document(userId)
                .update("currentStreak", streak)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Excluding confusing chars
        return (1..6).map { chars.random() }.joinToString("")
    }

    // ==================== MULTIPLAYER DUELS ====================

    private val duelsCollection = "duels"

    /**
     * Create a new duel and notify opponent
     * Structure: duels/{duelId} with progress subcollection
     */
    suspend fun createDuel(duel: com.dailywell.app.data.model.Duel): Result<String> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            // Serialize stake as a map
            val stakeMap = when (val stake = duel.stake) {
                is com.dailywell.app.data.model.DuelStake.Friendly -> mapOf("type" to "FRIENDLY")
                is com.dailywell.app.data.model.DuelStake.XpWager -> mapOf("type" to "XP_WAGER", "amount" to stake.amount)
                is com.dailywell.app.data.model.DuelStake.BraggingRights -> mapOf("type" to "BRAGGING_RIGHTS", "title" to stake.title)
            }

            // Serialize goal as a map
            val goalMap = serializeChallengeGoal(duel.goal)

            val duelData = hashMapOf(
                "id" to duel.id,
                "challengerId" to duel.challengerId,
                "challengerName" to duel.challengerName,
                "challengerEmoji" to duel.challengerEmoji,
                "opponentId" to duel.opponentId,
                "opponentName" to duel.opponentName,
                "opponentEmoji" to duel.opponentEmoji,
                "goal" to goalMap,
                "durationDays" to duel.duration.days,
                "stake" to stakeMap,
                "status" to duel.status.name,
                "createdAt" to duel.createdAt,
                "startedAt" to duel.startedAt,
                "endsAt" to duel.endsAt,
                "challengerProgress" to duel.challengerProgress,
                "opponentProgress" to duel.opponentProgress,
                "winnerId" to duel.winnerId,
                "rewardXp" to duel.rewards.xp,
                "rewardBadge" to duel.rewards.badge,
                "rewardTitle" to duel.rewards.title,
                // Track participants for queries
                "participants" to listOf(duel.challengerId, duel.opponentId)
            )

            firestore.collection(duelsCollection)
                .document(duel.id)
                .set(duelData)
                .await()

            Result.success(duel.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to serialize ChallengeGoal
     */
    private fun serializeChallengeGoal(goal: com.dailywell.app.data.model.ChallengeGoal): Map<String, Any> {
        return when (goal) {
            is com.dailywell.app.data.model.ChallengeGoal.TotalHabits -> mapOf("type" to "TOTAL_HABITS", "count" to goal.count)
            is com.dailywell.app.data.model.ChallengeGoal.PerfectDays -> mapOf("type" to "PERFECT_DAYS", "days" to goal.days)
            is com.dailywell.app.data.model.ChallengeGoal.StreakDays -> mapOf("type" to "STREAK_DAYS", "days" to goal.days)
            is com.dailywell.app.data.model.ChallengeGoal.SpecificHabit -> mapOf("type" to "SPECIFIC_HABIT", "habitType" to goal.habitType, "count" to goal.count)
            is com.dailywell.app.data.model.ChallengeGoal.EarlyBird -> mapOf("type" to "EARLY_BIRD", "days" to goal.days, "beforeHour" to goal.beforeHour)
            is com.dailywell.app.data.model.ChallengeGoal.TotalXp -> mapOf("type" to "TOTAL_XP", "xp" to goal.xp)
            is com.dailywell.app.data.model.ChallengeGoal.ConsecutiveDays -> mapOf("type" to "CONSECUTIVE_DAYS", "days" to goal.days)
            is com.dailywell.app.data.model.ChallengeGoal.MultiGoal -> mapOf("type" to "MULTI_GOAL", "goalCount" to goal.goals.size)
        }
    }

    /**
     * Helper to deserialize ChallengeGoal
     */
    private fun deserializeChallengeGoal(map: Map<*, *>?): com.dailywell.app.data.model.ChallengeGoal {
        if (map == null) return com.dailywell.app.data.model.ChallengeGoal.TotalHabits(10)
        return when (map["type"] as? String) {
            "TOTAL_HABITS" -> com.dailywell.app.data.model.ChallengeGoal.TotalHabits((map["count"] as? Long)?.toInt() ?: 10)
            "PERFECT_DAYS" -> com.dailywell.app.data.model.ChallengeGoal.PerfectDays((map["days"] as? Long)?.toInt() ?: 3)
            "STREAK_DAYS" -> com.dailywell.app.data.model.ChallengeGoal.StreakDays((map["days"] as? Long)?.toInt() ?: 7)
            "SPECIFIC_HABIT" -> com.dailywell.app.data.model.ChallengeGoal.SpecificHabit(
                habitType = map["habitType"] as? String ?: "",
                count = (map["count"] as? Long)?.toInt() ?: 5
            )
            "EARLY_BIRD" -> com.dailywell.app.data.model.ChallengeGoal.EarlyBird(
                days = (map["days"] as? Long)?.toInt() ?: 5,
                beforeHour = (map["beforeHour"] as? Long)?.toInt() ?: 8
            )
            "TOTAL_XP" -> com.dailywell.app.data.model.ChallengeGoal.TotalXp((map["xp"] as? Long) ?: 1000L)
            "CONSECUTIVE_DAYS" -> com.dailywell.app.data.model.ChallengeGoal.ConsecutiveDays((map["days"] as? Long)?.toInt() ?: 5)
            else -> com.dailywell.app.data.model.ChallengeGoal.TotalHabits(10)
        }
    }

    /**
     * Get pending duel invitations for user (where user is opponent, status=PENDING)
     */
    fun getDuelInvitations(userId: String): Flow<List<com.dailywell.app.data.model.DuelInvitation>> = callbackFlow {
        val listener = firestore.collection(duelsCollection)
            .whereEqualTo("opponentId", userId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val invitations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val goalMap = doc.get("goal") as? Map<*, *>
                        val stakeMap = doc.get("stake") as? Map<*, *>

                        com.dailywell.app.data.model.DuelInvitation(
                            duelId = doc.id,
                            challengerName = doc.getString("challengerName") ?: "User",
                            challengerEmoji = doc.getString("challengerEmoji") ?: "😊",
                            goal = deserializeChallengeGoal(goalMap),
                            duration = com.dailywell.app.data.model.ChallengeDuration(
                                days = doc.getLong("durationDays")?.toInt() ?: 7
                            ),
                            stake = deserializeDuelStake(stakeMap),
                            expiresAt = run {
                                val createdAt = doc.getString("createdAt") ?: ""
                                val durationDays = doc.getLong("durationDays")?.toInt() ?: 7
                                try {
                                    val createdInstant = kotlinx.datetime.Instant.parse(createdAt)
                                    val expiryInstant = createdInstant.plus(kotlin.time.Duration.parse("${durationDays}d"))
                                    expiryInstant.toString()
                                } catch (e: Exception) { createdAt }
                            }
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(invitations)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Helper to deserialize DuelStake
     */
    private fun deserializeDuelStake(map: Map<*, *>?): com.dailywell.app.data.model.DuelStake {
        if (map == null) return com.dailywell.app.data.model.DuelStake.Friendly
        return when (map["type"] as? String) {
            "FRIENDLY" -> com.dailywell.app.data.model.DuelStake.Friendly
            "XP_WAGER" -> com.dailywell.app.data.model.DuelStake.XpWager((map["amount"] as? Long) ?: 100L)
            "BRAGGING_RIGHTS" -> com.dailywell.app.data.model.DuelStake.BraggingRights(map["title"] as? String ?: "Champion")
            else -> com.dailywell.app.data.model.DuelStake.Friendly
        }
    }

    /**
     * Get active duels for user (as challenger OR opponent, status != PENDING/DECLINED/CANCELLED/EXPIRED)
     */
    fun getActiveDuelsForUser(userId: String): Flow<List<com.dailywell.app.data.model.Duel>> = callbackFlow {
        val listener = firestore.collection(duelsCollection)
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val duels = snapshot?.documents?.mapNotNull { doc ->
                    parseDuelDocument(doc)
                }?.filter {
                    it.status == com.dailywell.app.data.model.DuelStatus.ACTIVE ||
                    it.status == com.dailywell.app.data.model.DuelStatus.PENDING
                } ?: emptyList()

                trySend(duels)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get duel history for user (completed/declined/cancelled duels)
     */
    fun getDuelHistory(userId: String): Flow<List<com.dailywell.app.data.model.Duel>> = callbackFlow {
        val listener = firestore.collection(duelsCollection)
            .whereArrayContains("participants", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val duels = snapshot?.documents?.mapNotNull { doc ->
                    parseDuelDocument(doc)
                }?.filter {
                    it.status == com.dailywell.app.data.model.DuelStatus.COMPLETED ||
                    it.status == com.dailywell.app.data.model.DuelStatus.DECLINED ||
                    it.status == com.dailywell.app.data.model.DuelStatus.CANCELLED
                } ?: emptyList()

                trySend(duels)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Parse a Firestore document to a Duel object
     */
    private fun parseDuelDocument(doc: com.google.firebase.firestore.DocumentSnapshot): com.dailywell.app.data.model.Duel? {
        return try {
            val goalMap = doc.get("goal") as? Map<*, *>
            val stakeMap = doc.get("stake") as? Map<*, *>

            com.dailywell.app.data.model.Duel(
                id = doc.id,
                challengerId = doc.getString("challengerId") ?: "",
                challengerName = doc.getString("challengerName") ?: "User",
                challengerEmoji = doc.getString("challengerEmoji") ?: "😊",
                opponentId = doc.getString("opponentId") ?: "",
                opponentName = doc.getString("opponentName") ?: "Opponent",
                opponentEmoji = doc.getString("opponentEmoji") ?: "😊",
                goal = deserializeChallengeGoal(goalMap),
                duration = com.dailywell.app.data.model.ChallengeDuration(
                    days = doc.getLong("durationDays")?.toInt() ?: 7
                ),
                stake = deserializeDuelStake(stakeMap),
                status = try {
                    com.dailywell.app.data.model.DuelStatus.valueOf(doc.getString("status") ?: "PENDING")
                } catch (e: Exception) {
                    com.dailywell.app.data.model.DuelStatus.PENDING
                },
                createdAt = doc.getString("createdAt") ?: "",
                startedAt = doc.getString("startedAt"),
                endsAt = doc.getString("endsAt"),
                challengerProgress = doc.getLong("challengerProgress")?.toInt() ?: 0,
                opponentProgress = doc.getLong("opponentProgress")?.toInt() ?: 0,
                winnerId = doc.getString("winnerId"),
                rewards = com.dailywell.app.data.model.ChallengeRewards(
                    xp = doc.getLong("rewardXp") ?: 100L,
                    badge = doc.getString("rewardBadge"),
                    title = doc.getString("rewardTitle")
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Accept a duel invitation
     */
    suspend fun acceptDuel(duelId: String, startsAt: String, endsAt: String): Result<Unit> {
        return try {
            firestore.collection(duelsCollection)
                .document(duelId)
                .update(mapOf(
                    "status" to com.dailywell.app.data.model.DuelStatus.ACTIVE.name,
                    "startedAt" to startsAt,
                    "endsAt" to endsAt
                ))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decline a duel invitation
     */
    suspend fun declineDuel(duelId: String): Result<Unit> {
        return try {
            firestore.collection(duelsCollection)
                .document(duelId)
                .update("status", com.dailywell.app.data.model.DuelStatus.DECLINED.name)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel a duel (by challenger before it starts)
     */
    suspend fun cancelDuel(duelId: String): Result<Unit> {
        return try {
            firestore.collection(duelsCollection)
                .document(duelId)
                .update("status", com.dailywell.app.data.model.DuelStatus.CANCELLED.name)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update duel progress for current user
     */
    suspend fun updateDuelProgress(duelId: String, progress: Int): Result<Unit> {
        val userId = getCurrentUserId() ?: return Result.failure(Exception("Not signed in"))

        return try {
            // First get the duel to determine if user is challenger or opponent
            val doc = firestore.collection(duelsCollection)
                .document(duelId)
                .get()
                .await()

            val challengerId = doc.getString("challengerId")
            val progressField = if (userId == challengerId) "challengerProgress" else "opponentProgress"

            firestore.collection(duelsCollection)
                .document(duelId)
                .update(progressField, progress)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a single duel by ID
     */
    suspend fun getDuel(duelId: String): com.dailywell.app.data.model.Duel? {
        return try {
            val doc = firestore.collection(duelsCollection)
                .document(duelId)
                .get()
                .await()

            if (doc.exists()) parseDuelDocument(doc) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Watch a duel for real-time updates
     */
    fun watchDuel(duelId: String): Flow<com.dailywell.app.data.model.Duel?> = callbackFlow {
        val listener = firestore.collection(duelsCollection)
            .document(duelId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val duel = snapshot?.let { parseDuelDocument(it) }
                trySend(duel)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Complete a duel and set winner
     */
    suspend fun completeDuel(duelId: String, winnerId: String?): Result<Unit> {
        return try {
            firestore.collection(duelsCollection)
                .document(duelId)
                .update(mapOf(
                    "status" to com.dailywell.app.data.model.DuelStatus.COMPLETED.name,
                    "winnerId" to winnerId
                ))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== AUTHENTICATION ====================

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.let { user ->
                createUserProfile(user.uid)
                Result.success(user)
            } ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { Result.success(it) }
                ?: Result.failure(Exception("Sign in failed"))
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    suspend fun createAccount(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                // Set display name on Firebase Auth profile
                val profileUpdates = userProfileChangeRequest {
                    this.displayName = displayName
                }
                user.updateProfile(profileUpdates).await()
                // Create Firestore profile — don't let failure orphan the auth user
                try {
                    createUserProfile(user.uid, displayName, email)
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore profile creation failed, auth user exists: ${e.message}")
                }
                // Refresh auth state after profile update
                _authState.value = user.toAuthState()
                Result.success(user)
            } ?: Result.failure(Exception("Account creation failed"))
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    suspend fun signInWithEmailSimple(email: String, password: String): Result<Unit> {
        return signInWithEmail(email, password).map { }
    }

    suspend fun createAccountSimple(email: String, password: String, displayName: String): Result<Unit> {
        return createAccount(email, password, displayName).map { }
    }

    // ==================== GOOGLE SIGN-IN (Credential Manager) ====================

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        val webClientId = com.dailywell.shared.BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            return Result.failure(Exception("Google Sign-In is not configured. Add GOOGLE_WEB_CLIENT_ID to local.properties."))
        }

        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                authResult.user?.let { user ->
                    // Create/update Firestore profile — don't let failure break Google sign-in
                    try {
                        createUserProfile(
                            userId = user.uid,
                            displayName = user.displayName ?: "User",
                            email = user.email
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Firestore profile creation failed after Google sign-in: ${e.message}")
                    }
                    Result.success(user)
                } ?: Result.failure(Exception("Google sign-in failed"))
            } else {
                Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    suspend fun signInWithGoogleSimple(context: Context): Result<Unit> {
        return signInWithGoogle(context).map { }
    }

    // ==================== PASSWORD RESET & VERIFICATION ====================

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    suspend fun sendEmailVerification(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
        return try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    suspend fun refreshEmailVerificationStatus(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.reload().await()
            val verified = auth.currentUser?.isEmailVerified == true
            _authState.value = auth.currentUser.toAuthState()
            verified
        } catch (e: Exception) {
            false
        }
    }

    // ==================== PROFILE MANAGEMENT ====================

    suspend fun updateDisplayName(name: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
        return try {
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            user.updateProfile(profileUpdates).await()
            // Update Firestore profile too
            firestore.collection(usersCollection)
                .document(user.uid)
                .update("displayName", name)
                .await()
            _authState.value = user.toAuthState()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
        return try {
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    // ==================== RE-AUTHENTICATION ====================

    suspend fun reauthenticate(email: String, password: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
        return try {
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    suspend fun reauthenticateAndChangePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
        val email = user.email ?: return Result.failure(Exception("No email on account"))
        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    // ==================== ACCOUNT DELETION ====================

    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not signed in"))
        return try {
            // Delete Firestore user profile first
            try {
                firestore.collection(usersCollection)
                    .document(user.uid)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore profile deletion failed, proceeding with auth delete: ${e.message}")
            }
            // Delete Firebase Auth account
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(getAuthErrorMessage(e)))
        }
    }

    // ==================== SIGN OUT ====================

    fun signOut() {
        auth.signOut()
    }

    // ==================== ERROR MESSAGES ====================

    private fun getAuthErrorMessage(exception: Exception): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password. Please try again."
            is FirebaseAuthInvalidUserException -> when (exception.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                "ERROR_USER_DISABLED" -> "This account has been disabled."
                else -> "Account error. Please try again."
            }
            is FirebaseAuthUserCollisionException -> "An account already exists with this email."
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
            else -> {
                val msg = exception.message ?: "Something went wrong"
                when {
                    "CONFIGURATION_NOT_FOUND" in msg -> "Firebase is not configured yet. Enable Email/Password auth in Firebase Console."
                    "network" in msg.lowercase() -> "No internet connection. Please check your network."
                    "too-many-requests" in msg.lowercase() || "RATE_LIMIT" in msg ->
                        "Too many attempts. Please wait a moment and try again."
                    "requires-recent-login" in msg.lowercase() ->
                        "Please sign in again before making this change."
                    else -> msg
                }
            }
        }
    }

    // ==================== USER PROFILE (Firestore) ====================

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
                        currentLevel = doc.getLong("currentLevel")?.toInt() ?: 1,
                        perfectDays = (doc.get("perfectDays") as? Number)?.toInt() ?: 0
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
                            currentLevel = doc.getLong("currentLevel")?.toInt() ?: 1,
                            perfectDays = (doc.get("perfectDays") as? Number)?.toInt() ?: 0
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
     * Decline friend request
     */
    suspend fun declineFriendRequest(fromUserId: String, toUserId: String): Result<Unit> {
        return try {
            // Just delete the pending request
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
     * Remove a friend
     */
    suspend fun removeFriend(userId: String, friendId: String): Result<Unit> {
        return try {
            // Remove from both users' friends lists
            firestore.collection(usersCollection)
                .document(userId)
                .collection(friendsCollection)
                .document(friendId)
                .delete()
                .await()

            firestore.collection(usersCollection)
                .document(friendId)
                .collection(friendsCollection)
                .document(userId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get pending friend requests for a user
     */
    suspend fun getPendingFriendRequests(userId: String): List<FriendRequestData> {
        return try {
            val result = firestore.collection(socialCollection)
                .document("requests")
                .collection("pending")
                .whereEqualTo("toUserId", userId)
                .get()
                .await()

            result.documents.mapNotNull { doc ->
                val fromUserId = doc.getString("fromUserId") ?: return@mapNotNull null
                val fromProfile = getUserProfile(fromUserId).getOrNull()

                FriendRequestData(
                    requestId = doc.id,
                    fromUserId = fromUserId,
                    fromUserName = fromProfile?.displayName ?: "User",
                    fromUserEmoji = fromProfile?.profileEmoji ?: "😊",
                    fromUserLevel = fromProfile?.currentLevel ?: 1,
                    createdAt = doc.getString("createdAt") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if two users are friends
     */
    suspend fun checkFriendStatus(userId: String, otherUserId: String): FriendStatus {
        return try {
            // Check if already friends
            val friendDoc = firestore.collection(usersCollection)
                .document(userId)
                .collection(friendsCollection)
                .document(otherUserId)
                .get()
                .await()

            if (friendDoc.exists()) {
                return FriendStatus.FRIENDS
            }

            // Check for pending request (either direction)
            val sentRequest = firestore.collection(socialCollection)
                .document("requests")
                .collection("pending")
                .document("${userId}_$otherUserId")
                .get()
                .await()

            if (sentRequest.exists()) {
                return FriendStatus.REQUEST_SENT
            }

            val receivedRequest = firestore.collection(socialCollection)
                .document("requests")
                .collection("pending")
                .document("${otherUserId}_$userId")
                .get()
                .await()

            if (receivedRequest.exists()) {
                return FriendStatus.REQUEST_RECEIVED
            }

            FriendStatus.NOT_FRIENDS
        } catch (e: Exception) {
            FriendStatus.NOT_FRIENDS
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

    // ==================== FAMILY USAGE TRACKING ====================

    /**
     * Update family member AI usage
     * Tracks monthly cost, tokens, and message counts per member
     */
    suspend fun updateFamilyMemberUsage(
        familyId: String,
        usage: com.dailywell.app.data.repository.FamilyMemberUsage
    ): Result<Unit> {
        return try {
            val usageData = hashMapOf(
                "userId" to usage.userId,
                "currentMonthCostUsd" to usage.currentMonthCostUsd,
                "tokensUsed" to usage.tokensUsed,
                "messagesCount" to usage.messagesCount,
                "cloudMessagesCount" to usage.cloudMessagesCount,
                "slmMessagesCount" to usage.slmMessagesCount,
                "freeMessagesCount" to usage.freeMessagesCount,
                "resetDate" to usage.resetDate,
                "lastUpdated" to usage.lastUpdated
            )

            firestore.collection(familiesCollection)
                .document(familyId)
                .collection("usage")
                .document(usage.userId)
                .set(usageData, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all family member usages (for owner to see family summary)
     */
    suspend fun getFamilyMemberUsages(familyId: String): List<com.dailywell.app.data.repository.FamilyMemberUsage> {
        return try {
            val result = firestore.collection(familiesCollection)
                .document(familyId)
                .collection("usage")
                .get()
                .await()

            result.documents.mapNotNull { doc ->
                try {
                    com.dailywell.app.data.repository.FamilyMemberUsage(
                        userId = doc.getString("userId") ?: return@mapNotNull null,
                        familyId = familyId,
                        currentMonthCostUsd = doc.getDouble("currentMonthCostUsd")?.toFloat() ?: 0f,
                        tokensUsed = doc.getLong("tokensUsed")?.toInt() ?: 0,
                        messagesCount = doc.getLong("messagesCount")?.toInt() ?: 0,
                        cloudMessagesCount = doc.getLong("cloudMessagesCount")?.toInt() ?: 0,
                        slmMessagesCount = doc.getLong("slmMessagesCount")?.toInt() ?: 0,
                        freeMessagesCount = doc.getLong("freeMessagesCount")?.toInt() ?: 0,
                        resetDate = doc.getString("resetDate") ?: "",
                        lastUpdated = doc.getString("lastUpdated") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get single member's usage
     */
    suspend fun getFamilyMemberUsage(familyId: String, memberId: String): com.dailywell.app.data.repository.FamilyMemberUsage? {
        return try {
            val doc = firestore.collection(familiesCollection)
                .document(familyId)
                .collection("usage")
                .document(memberId)
                .get()
                .await()

            if (!doc.exists()) return null

            com.dailywell.app.data.repository.FamilyMemberUsage(
                userId = doc.getString("userId") ?: return null,
                familyId = familyId,
                currentMonthCostUsd = doc.getDouble("currentMonthCostUsd")?.toFloat() ?: 0f,
                tokensUsed = doc.getLong("tokensUsed")?.toInt() ?: 0,
                messagesCount = doc.getLong("messagesCount")?.toInt() ?: 0,
                cloudMessagesCount = doc.getLong("cloudMessagesCount")?.toInt() ?: 0,
                slmMessagesCount = doc.getLong("slmMessagesCount")?.toInt() ?: 0,
                freeMessagesCount = doc.getLong("freeMessagesCount")?.toInt() ?: 0,
                resetDate = doc.getString("resetDate") ?: "",
                lastUpdated = doc.getString("lastUpdated") ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Reset all family member usages (called on 1st of each month)
     */
    suspend fun resetFamilyUsages(familyId: String, newResetDate: String): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val usageCollection = firestore.collection(familiesCollection)
                .document(familyId)
                .collection("usage")

            val usageDocs = usageCollection.get().await()
            for (doc in usageDocs.documents) {
                batch.update(doc.reference, mapOf(
                    "currentMonthCostUsd" to 0f,
                    "tokensUsed" to 0,
                    "messagesCount" to 0,
                    "cloudMessagesCount" to 0,
                    "slmMessagesCount" to 0,
                    "freeMessagesCount" to 0,
                    "resetDate" to newResetDate,
                    "lastUpdated" to Clock.System.now().toString()
                ))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== USER BEHAVIOR PROFILE ====================

    /**
     * Update user behavior profile for AI personalization
     * Tracks chronotype, motivation style, habit correlations, etc.
     */
    suspend fun updateUserBehaviorProfile(
        profile: com.dailywell.app.data.model.UserBehaviorProfile
    ): Result<Unit> {
        val userId = profile.userId.takeIf { it != "unknown" } ?: getCurrentUserId()
            ?: return Result.failure(Exception("No user ID"))

        return try {
            val profileData = hashMapOf(
                "userId" to userId,
                "chronotype" to profile.chronotype.name,
                "motivationStyle" to profile.motivationStyle.name,
                "streakRecoveryRate" to profile.streakRecoveryRate,
                "averageCheckInHour" to profile.averageCheckInHour,
                "preferredNotificationHours" to profile.preferredNotificationHours,
                "attitudeScore" to profile.attitudeScore,
                "engagementLevel" to profile.engagementLevel.name,
                "weekdayVsWeekendRatio" to profile.weekdayVsWeekendRatio,
                "mostProductiveDay" to profile.mostProductiveDay,
                "leastProductiveDay" to profile.leastProductiveDay,
                "checkInConsistency" to profile.checkInConsistency,
                "totalCompletions" to profile.totalCompletions,
                "totalMissedDays" to profile.totalMissedDays,
                "currentMissedStreak" to profile.currentMissedStreak,
                "lastUpdated" to profile.lastUpdated,
                "profileVersion" to profile.profileVersion,
                // Feature usage
                "featureUsage" to hashMapOf(
                    "usesAICoaching" to profile.featureUsage.usesAICoaching,
                    "aiCoachingFrequency" to profile.featureUsage.aiCoachingFrequency,
                    "usesPatternInsights" to profile.featureUsage.usesPatternInsights,
                    "insightsViewCount" to profile.featureUsage.insightsViewCount,
                    "usesVoiceInput" to profile.featureUsage.usesVoiceInput,
                    "usesTTS" to profile.featureUsage.usesTTS,
                    "preferredCoachId" to profile.featureUsage.preferredCoachId,
                    "usesCalendarIntegration" to profile.featureUsage.usesCalendarIntegration,
                    "usesSocialFeatures" to profile.featureUsage.usesSocialFeatures,
                    "usesWidgets" to profile.featureUsage.usesWidgets,
                    "notificationResponseRate" to profile.featureUsage.notificationResponseRate,
                    "averageSessionDurationSeconds" to profile.featureUsage.averageSessionDurationSeconds,
                    "sessionsPerDay" to profile.featureUsage.sessionsPerDay,
                    "lastFeatureUsed" to profile.featureUsage.lastFeatureUsed
                ),
                // Habit correlations (top 5)
                "habitCorrelations" to profile.habitCorrelations.take(5).map { corr ->
                    hashMapOf(
                        "habit1" to corr.habit1,
                        "habit2" to corr.habit2,
                        "correlation" to corr.correlation,
                        "sampleSize" to corr.sampleSize,
                        "confidence" to corr.confidence
                    )
                }
            )

            firestore.collection(usersCollection)
                .document(userId)
                .collection("behavior")
                .document("profile")
                .set(profileData, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user behavior profile from Firebase
     */
    suspend fun getUserBehaviorProfile(userId: String): com.dailywell.app.data.model.UserBehaviorProfile? {
        return try {
            val doc = firestore.collection(usersCollection)
                .document(userId)
                .collection("behavior")
                .document("profile")
                .get()
                .await()

            if (!doc.exists()) return null

            val featureUsageMap = doc.get("featureUsage") as? Map<*, *>
            val correlationsList = doc.get("habitCorrelations") as? List<*>

            com.dailywell.app.data.model.UserBehaviorProfile(
                userId = doc.getString("userId") ?: userId,
                chronotype = try {
                    com.dailywell.app.data.model.Chronotype.valueOf(
                        doc.getString("chronotype") ?: "FLEXIBLE"
                    )
                } catch (e: Exception) {
                    com.dailywell.app.data.model.Chronotype.FLEXIBLE
                },
                motivationStyle = try {
                    com.dailywell.app.data.model.MotivationStyle.valueOf(
                        doc.getString("motivationStyle") ?: "SUPPORTIVE"
                    )
                } catch (e: Exception) {
                    com.dailywell.app.data.model.MotivationStyle.SUPPORTIVE
                },
                streakRecoveryRate = doc.getDouble("streakRecoveryRate")?.toFloat() ?: 0.5f,
                averageCheckInHour = doc.getLong("averageCheckInHour")?.toInt() ?: 12,
                preferredNotificationHours = (doc.get("preferredNotificationHours") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toInt() } ?: listOf(8, 20),
                attitudeScore = doc.getDouble("attitudeScore")?.toFloat() ?: 0f,
                engagementLevel = try {
                    com.dailywell.app.data.model.EngagementLevel.valueOf(
                        doc.getString("engagementLevel") ?: "MODERATE"
                    )
                } catch (e: Exception) {
                    com.dailywell.app.data.model.EngagementLevel.MODERATE
                },
                weekdayVsWeekendRatio = doc.getDouble("weekdayVsWeekendRatio")?.toFloat() ?: 1.0f,
                mostProductiveDay = doc.getLong("mostProductiveDay")?.toInt() ?: 1,
                leastProductiveDay = doc.getLong("leastProductiveDay")?.toInt() ?: 7,
                checkInConsistency = doc.getDouble("checkInConsistency")?.toFloat() ?: 0.5f,
                totalCompletions = doc.getLong("totalCompletions")?.toInt() ?: 0,
                totalMissedDays = doc.getLong("totalMissedDays")?.toInt() ?: 0,
                currentMissedStreak = doc.getLong("currentMissedStreak")?.toInt() ?: 0,
                lastUpdated = doc.getString("lastUpdated") ?: "",
                profileVersion = doc.getLong("profileVersion")?.toInt() ?: 1,
                featureUsage = com.dailywell.app.data.model.FeatureUsageProfile(
                    usesAICoaching = (featureUsageMap?.get("usesAICoaching") as? Boolean) ?: false,
                    aiCoachingFrequency = (featureUsageMap?.get("aiCoachingFrequency") as? Number)?.toFloat() ?: 0f,
                    usesPatternInsights = (featureUsageMap?.get("usesPatternInsights") as? Boolean) ?: false,
                    insightsViewCount = (featureUsageMap?.get("insightsViewCount") as? Number)?.toInt() ?: 0,
                    usesVoiceInput = (featureUsageMap?.get("usesVoiceInput") as? Boolean) ?: false,
                    usesTTS = (featureUsageMap?.get("usesTTS") as? Boolean) ?: false,
                    preferredCoachId = featureUsageMap?.get("preferredCoachId") as? String,
                    usesCalendarIntegration = (featureUsageMap?.get("usesCalendarIntegration") as? Boolean) ?: false,
                    usesSocialFeatures = (featureUsageMap?.get("usesSocialFeatures") as? Boolean) ?: false,
                    usesWidgets = (featureUsageMap?.get("usesWidgets") as? Boolean) ?: false,
                    notificationResponseRate = (featureUsageMap?.get("notificationResponseRate") as? Number)?.toFloat() ?: 0f,
                    averageSessionDurationSeconds = (featureUsageMap?.get("averageSessionDurationSeconds") as? Number)?.toInt() ?: 0,
                    sessionsPerDay = (featureUsageMap?.get("sessionsPerDay") as? Number)?.toFloat() ?: 0f,
                    lastFeatureUsed = (featureUsageMap?.get("lastFeatureUsed") as? String) ?: ""
                ),
                habitCorrelations = correlationsList?.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    com.dailywell.app.data.model.HabitCorrelationData(
                        habit1 = map["habit1"] as? String ?: return@mapNotNull null,
                        habit2 = map["habit2"] as? String ?: return@mapNotNull null,
                        correlation = (map["correlation"] as? Number)?.toFloat() ?: 0f,
                        sampleSize = (map["sampleSize"] as? Number)?.toInt() ?: 0,
                        confidence = (map["confidence"] as? Number)?.toFloat() ?: 0f
                    )
                } ?: emptyList()
            )
        } catch (e: Exception) {
            null
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
    val currentLevel: Int,
    val perfectDays: Int = 0
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

data class FriendRequestData(
    val requestId: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserEmoji: String,
    val fromUserLevel: Int,
    val createdAt: String
)

enum class FriendStatus {
    NOT_FRIENDS,
    FRIENDS,
    REQUEST_SENT,
    REQUEST_RECEIVED
}
