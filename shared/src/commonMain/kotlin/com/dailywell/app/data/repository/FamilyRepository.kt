package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Family Plan features
 */
interface FamilyRepository {
    // Family data
    fun getFamilyData(): Flow<FamilyPlanData>
    fun getMembers(): Flow<List<FamilyMember>>
    fun getActivityFeed(): Flow<List<FamilyActivity>>

    // Family management
    suspend fun createFamily(familyName: String): FamilyPlanData
    suspend fun joinFamily(inviteCode: String): Boolean
    suspend fun leaveFamily()
    suspend fun generateInviteCode(): String
    suspend fun removeMember(memberId: String)
    suspend fun updateMemberRole(memberId: String, role: FamilyRole)

    // Challenges
    fun getChallenges(): Flow<List<FamilyChallenge>>
    fun getActiveChallenge(): Flow<FamilyChallenge?>
    suspend fun createChallenge(challenge: FamilyChallenge)
    suspend fun updateChallengeProgress(challengeId: String, memberId: String, progress: Int)
    suspend fun completeChallenge(challengeId: String)
    suspend fun cancelChallenge(challengeId: String)

    // Milestones
    fun getMilestones(): Flow<List<FamilyMilestone>>
    suspend fun addMilestone(milestone: FamilyMilestone)

    // Activity & engagement
    suspend fun sendHighFive(activityId: String)
    suspend fun logActivity(activity: FamilyActivity)

    // Settings
    suspend fun updateSharedHabits(habitIds: List<String>)
    suspend fun toggleHabitSharing(habitId: String, shared: Boolean)
}
