package com.dailywell.app.data.repository

import com.dailywell.app.api.FirebaseService
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock
import kotlin.random.Random

/**
 * Implementation of FamilyRepository with REAL Firebase integration
 */
class FamilyRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val firebaseService: FirebaseService
) : FamilyRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val FAMILY_DATA_KEY = "family_plan_data"
        private const val FAMILY_ACTIVITY_KEY = "family_activity_feed"
    }

    private fun getFamilyDataFlow(): Flow<FamilyPlanData> {
        return dataStoreManager.getString(FAMILY_DATA_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<FamilyPlanData>(it)
                } catch (e: Exception) {
                    FamilyPlanData()
                }
            } ?: FamilyPlanData()
        }
    }

    private suspend fun updateFamilyData(transform: (FamilyPlanData) -> FamilyPlanData) {
        val currentData = getFamilyDataFlow().first()
        val updatedData = transform(currentData)
        dataStoreManager.putString(FAMILY_DATA_KEY, json.encodeToString(updatedData))

        // Sync to Firebase if family exists
        if (!updatedData.familyId.isNullOrEmpty()) {
            syncFamilyToFirebase(updatedData)
        }
    }

    private fun getActivityFeedFlow(): Flow<List<FamilyActivity>> {
        return dataStoreManager.getString(FAMILY_ACTIVITY_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<List<FamilyActivity>>(it)
                } catch (e: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        }
    }

    private suspend fun updateActivityFeed(transform: (List<FamilyActivity>) -> List<FamilyActivity>) {
        val currentActivities = getActivityFeedFlow().first()
        val updatedActivities = transform(currentActivities)
        dataStoreManager.putString(FAMILY_ACTIVITY_KEY, json.encodeToString(updatedActivities))
    }

    private suspend fun syncFamilyToFirebase(data: FamilyPlanData) {
        // Sync family data to Firestore
        try {
            val familyData: Map<String, Any> = mapOf(
                "members" to data.members.map { member ->
                    mapOf<String, Any>(
                        "id" to member.id,
                        "name" to member.name,
                        "avatar" to member.avatar,
                        "role" to member.role.name,
                        "currentStreak" to member.currentStreak,
                        "weeklyScore" to member.weeklyScore,
                        "sharedHabits" to member.sharedHabits
                    )
                },
                "challenges" to data.sharedChallenges.map { challenge ->
                    mapOf<String, Any>(
                        "id" to challenge.id,
                        "title" to challenge.title,
                        "status" to challenge.status.name,
                        "progress" to challenge.currentProgress
                    )
                },
                "inviteCode" to (data.inviteCode ?: ""),
                "updatedAt" to Clock.System.now().toString()
            )
            firebaseService.updateFamilyData(data.familyId ?: return, familyData)
        } catch (e: Exception) {
            // Continue with local data if Firebase sync fails
        }
    }

    override fun getFamilyData(): Flow<FamilyPlanData> = getFamilyDataFlow()

    override fun getMembers(): Flow<List<FamilyMember>> {
        return getFamilyDataFlow().map { it.members }
    }

    override fun getActivityFeed(): Flow<List<FamilyActivity>> = getActivityFeedFlow()

    override suspend fun createFamily(familyName: String): FamilyPlanData {
        val now = Clock.System.now().toString()
        val userId = firebaseService.getCurrentUserId() ?: "user_${System.currentTimeMillis()}"
        val userProfile = firebaseService.getUserProfile()

        val newFamily = FamilyPlanData(
            familyId = "family_${System.currentTimeMillis()}",
            isOwner = true,
            members = listOf(
                FamilyMember(
                    id = userId,
                    name = userProfile?.displayName ?: "You",
                    avatar = userProfile?.profileEmoji ?: "🙂",
                    role = FamilyRole.OWNER,
                    joinedAt = now
                )
            ),
            inviteCode = generateCode(),
            createdAt = now
        )

        // Save to local storage
        dataStoreManager.putString(FAMILY_DATA_KEY, json.encodeToString(newFamily))

        // Create family in Firebase
        newFamily.familyId?.let { familyId ->
            firebaseService.createFamily(familyId, familyName, newFamily.inviteCode ?: "")
        }

        return newFamily
    }

    override suspend fun joinFamily(inviteCode: String): Boolean {
        // Validate invite code with Firebase
        val familyData = firebaseService.joinFamilyByInviteCode(inviteCode)
        if (familyData != null) {
            // Fetch family data and update local storage
            syncFromFirebase(familyData.familyId)
            return true
        }
        return false
    }

    override suspend fun leaveFamily() {
        val currentFamily = getFamilyDataFlow().first()
        currentFamily.familyId?.let { familyId ->
            firebaseService.leaveFamily(familyId)
        }
        dataStoreManager.putString(FAMILY_DATA_KEY, json.encodeToString(FamilyPlanData()))
        dataStoreManager.putString(FAMILY_ACTIVITY_KEY, json.encodeToString(emptyList<FamilyActivity>()))
    }

    private suspend fun syncFromFirebase(familyId: String) {
        val familyData = firebaseService.getFamilyData(familyId)
        if (familyData != null) {
            dataStoreManager.putString(FAMILY_DATA_KEY, json.encodeToString(familyData))
        }
    }

    override suspend fun generateInviteCode(): String {
        val code = generateCode()
        updateFamilyData { it.copy(inviteCode = code) }

        // Update in Firebase
        val currentData = getFamilyDataFlow().first()
        currentData.familyId?.let { familyId ->
            firebaseService.updateFamilyInviteCode(familyId, code)
        }

        return code
    }

    override suspend fun removeMember(memberId: String) {
        val currentData = getFamilyDataFlow().first()

        // Remove from Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.removeFamilyMember(familyId, memberId)
        }

        updateFamilyData { data ->
            data.copy(members = data.members.filter { it.id != memberId })
        }
    }

    override suspend fun updateMemberRole(memberId: String, role: FamilyRole) {
        val currentData = getFamilyDataFlow().first()

        // Update in Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.updateFamilyMemberRole(familyId, memberId, role.name)
        }

        updateFamilyData { data ->
            data.copy(
                members = data.members.map { member ->
                    if (member.id == memberId) member.copy(role = role) else member
                }
            )
        }
    }

    override fun getChallenges(): Flow<List<FamilyChallenge>> {
        return getFamilyDataFlow().map { it.sharedChallenges }
    }

    override fun getActiveChallenge(): Flow<FamilyChallenge?> {
        return getFamilyDataFlow().map { data ->
            data.sharedChallenges.firstOrNull { it.status == ChallengeStatus.ACTIVE }
        }
    }

    override suspend fun createChallenge(challenge: FamilyChallenge) {
        val currentData = getFamilyDataFlow().first()
        val now = Clock.System.now().toString().substringBefore("T")

        // Initialize progress for all members
        val initialProgress = currentData.members.associate { it.id to 0 }

        val newChallenge = challenge.copy(
            startDate = now,
            currentProgress = initialProgress,
            status = ChallengeStatus.ACTIVE
        )

        // Save to Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.createFamilyChallenge(familyId, newChallenge)
        }

        updateFamilyData { data ->
            data.copy(sharedChallenges = listOf(newChallenge) + data.sharedChallenges)
        }
    }

    override suspend fun updateChallengeProgress(challengeId: String, memberId: String, progress: Int) {
        val currentData = getFamilyDataFlow().first()

        // Update in Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.updateFamilyChallengeProgress(familyId, challengeId, memberId, progress)
        }

        updateFamilyData { data ->
            data.copy(
                sharedChallenges = data.sharedChallenges.map { challenge ->
                    if (challenge.id == challengeId) {
                        val newProgress = challenge.currentProgress + (memberId to progress)
                        challenge.copy(currentProgress = newProgress)
                    } else challenge
                }
            )
        }
    }

    override suspend fun completeChallenge(challengeId: String) {
        val currentData = getFamilyDataFlow().first()
        val challenge = currentData.sharedChallenges.find { it.id == challengeId } ?: return

        // Determine winner for competitive challenges
        val winnerId = if (challenge.type == FamilyChallengeType.COMPETITIVE ||
                          challenge.type == FamilyChallengeType.STREAK) {
            challenge.currentProgress.maxByOrNull { it.value }?.key
        } else null

        // Update in Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.completeFamilyChallenge(familyId, challengeId, winnerId)
        }

        updateFamilyData { data ->
            data.copy(
                sharedChallenges = data.sharedChallenges.map { ch ->
                    if (ch.id == challengeId) {
                        ch.copy(
                            status = ChallengeStatus.COMPLETED,
                            winnerId = winnerId,
                            endDate = Clock.System.now().toString().substringBefore("T")
                        )
                    } else ch
                }
            )
        }

        // Add milestone for completed challenge
        val milestone = FamilyMilestone(
            id = "milestone_${System.currentTimeMillis()}",
            title = "Challenge Complete: ${challenge.title}",
            description = if (winnerId != null) {
                "${currentData.members.find { it.id == winnerId }?.name} won!"
            } else {
                "The whole family reached the goal!"
            },
            emoji = "🏆",
            achievedBy = currentData.members.map { it.id },
            achievedAt = Clock.System.now().toString(),
            type = MilestoneType.CHALLENGE_WON,
            celebrationMessage = challenge.reward
        )
        addMilestone(milestone)
    }

    override suspend fun cancelChallenge(challengeId: String) {
        val currentData = getFamilyDataFlow().first()

        // Update in Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.cancelFamilyChallenge(familyId, challengeId)
        }

        updateFamilyData { data ->
            data.copy(
                sharedChallenges = data.sharedChallenges.map { challenge ->
                    if (challenge.id == challengeId) {
                        challenge.copy(status = ChallengeStatus.CANCELLED)
                    } else challenge
                }
            )
        }
    }

    override fun getMilestones(): Flow<List<FamilyMilestone>> {
        return getFamilyDataFlow().map { it.sharedMilestones }
    }

    override suspend fun addMilestone(milestone: FamilyMilestone) {
        val currentData = getFamilyDataFlow().first()

        // Save milestone to Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.addFamilyMilestone(familyId, milestone)
        }

        updateFamilyData { data ->
            data.copy(sharedMilestones = listOf(milestone) + data.sharedMilestones)
        }
    }

    override suspend fun sendHighFive(activityId: String) {
        val currentData = getFamilyDataFlow().first()
        val userId = firebaseService.getCurrentUserId() ?: return

        // Send high five via Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.sendFamilyHighFive(familyId, activityId, userId)
        }

        updateActivityFeed { activities ->
            activities.map { activity ->
                if (activity.id == activityId && userId !in activity.highFives) {
                    activity.copy(highFives = activity.highFives + userId)
                } else activity
            }
        }
    }

    override suspend fun logActivity(activity: FamilyActivity) {
        val currentData = getFamilyDataFlow().first()

        // Log activity to Firebase
        currentData.familyId?.let { familyId ->
            firebaseService.logFamilyActivity(familyId, activity)
        }

        updateActivityFeed { activities ->
            listOf(activity) + activities.take(49)
        }
    }

    override suspend fun updateSharedHabits(habitIds: List<String>) {
        val currentData = getFamilyDataFlow().first()

        // Update in Firebase
        currentData.familyId?.let { familyId ->
            val userId = firebaseService.getCurrentUserId() ?: return
            firebaseService.updateMemberSharedHabits(familyId, userId, habitIds)
        }

        updateFamilyData { data ->
            data.copy(
                members = data.members.map { member ->
                    if (member.role == FamilyRole.OWNER) {
                        member.copy(sharedHabits = habitIds)
                    } else member
                }
            )
        }
    }

    override suspend fun toggleHabitSharing(habitId: String, shared: Boolean) {
        val currentData = getFamilyDataFlow().first()
        val ownerMember = currentData.members.find { it.role == FamilyRole.OWNER }
        val newSharedHabits = if (shared) {
            (ownerMember?.sharedHabits ?: emptyList()) + habitId
        } else {
            (ownerMember?.sharedHabits ?: emptyList()) - habitId
        }

        // Update in Firebase
        currentData.familyId?.let { familyId ->
            val userId = firebaseService.getCurrentUserId() ?: return
            firebaseService.updateMemberSharedHabits(currentData.familyId, userId, newSharedHabits)
        }

        updateFamilyData { data ->
            data.copy(
                members = data.members.map { member ->
                    if (member.role == FamilyRole.OWNER) {
                        member.copy(sharedHabits = newSharedHabits)
                    } else member
                }
            )
        }
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
