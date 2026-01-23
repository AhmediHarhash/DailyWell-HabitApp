package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Social Accountability features
 * Privacy-first: all data stays local, sharing is opt-in
 */
interface SocialRepository {

    /**
     * Get social data
     */
    fun getSocialData(): Flow<SocialAccountabilityData>

    /**
     * Groups
     */
    fun getGroups(): Flow<List<AccountabilityGroup>>
    fun getGroup(groupId: String): Flow<AccountabilityGroup?>
    suspend fun createGroup(group: AccountabilityGroup)
    suspend fun joinGroup(groupId: String, inviteCode: String? = null)
    suspend fun leaveGroup(groupId: String)
    suspend fun updateGroupSettings(groupId: String, settings: GroupSettings)

    /**
     * Partners
     */
    fun getPartners(): Flow<List<AccountabilityPartner>>
    suspend fun sendPartnerRequest(toUserId: String)
    suspend fun acceptPartnerRequest(partnerId: String)
    suspend fun declinePartnerRequest(partnerId: String)
    suspend fun removePartner(partnerId: String)

    /**
     * Shared Habits
     */
    fun getSharedHabits(): Flow<List<SharedHabit>>
    suspend fun shareHabit(habitId: String, targets: List<SharingTarget>)
    suspend fun unshareHabit(habitId: String, targetId: String)
    suspend fun updateSharingSettings(habitId: String, shareStreak: Boolean, shareCompletionTime: Boolean)

    /**
     * High Fives
     */
    fun getReceivedHighFives(): Flow<List<HighFive>>
    fun getSentHighFives(): Flow<List<HighFive>>
    fun getUnreadHighFiveCount(): Flow<Int>
    suspend fun sendHighFive(toUserId: String, reason: HighFiveReason, habitId: String? = null, message: String? = null)
    suspend fun markHighFiveAsRead(highFiveId: String)
    suspend fun markAllHighFivesAsRead()

    /**
     * Commitment Contracts
     */
    fun getCommitmentContracts(): Flow<List<CommitmentContract>>
    suspend fun createCommitmentContract(contract: CommitmentContract)
    suspend fun updateContractProgress(contractId: String, progress: Int)
    suspend fun cancelContract(contractId: String)

    /**
     * Privacy
     */
    fun getPrivacySettings(): Flow<PrivacySettings>
    suspend fun updatePrivacySettings(settings: PrivacySettings)

    /**
     * Profile
     */
    suspend fun updateProfile(displayName: String, profileEmoji: String)

    /**
     * Clear all social data
     */
    suspend fun clearAllSocialData()
}
