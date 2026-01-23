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

/**
 * Implementation of SocialRepository with REAL Firebase integration
 */
class SocialRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val firebaseService: FirebaseService
) : SocialRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val SOCIAL_DATA_KEY = "social_accountability_data"
    }

    private fun getSocialDataFlow(): Flow<SocialAccountabilityData> {
        return dataStoreManager.getString(SOCIAL_DATA_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<SocialAccountabilityData>(it)
                } catch (e: Exception) {
                    SocialAccountabilityData()
                }
            } ?: SocialAccountabilityData()
        }
    }

    private suspend fun updateSocialData(transform: (SocialAccountabilityData) -> SocialAccountabilityData) {
        val currentData = getSocialDataFlow().first()
        val updatedData = transform(currentData)
        dataStoreManager.putString(SOCIAL_DATA_KEY, json.encodeToString(updatedData))
    }

    override fun getSocialData(): Flow<SocialAccountabilityData> = getSocialDataFlow()

    // Groups
    override fun getGroups(): Flow<List<AccountabilityGroup>> {
        return getSocialDataFlow().map { it.groups }
    }

    override fun getGroup(groupId: String): Flow<AccountabilityGroup?> {
        return getSocialDataFlow().map { data ->
            data.groups.find { it.id == groupId }
        }
    }

    override suspend fun createGroup(group: AccountabilityGroup) {
        updateSocialData { data ->
            data.copy(groups = data.groups + group)
        }
    }

    override suspend fun joinGroup(groupId: String, inviteCode: String?) {
        updateSocialData { data ->
            val updatedGroups = data.groups.map { group ->
                if (group.id == groupId) {
                    val newMember = GroupMember(
                        userId = data.userId.ifEmpty { "user_${Clock.System.now().toEpochMilliseconds()}" },
                        displayName = data.displayName.ifEmpty { "You" },
                        profileEmoji = data.profileEmoji,
                        role = GroupRole.MEMBER,
                        joinedAt = Clock.System.now().toString()
                    )
                    group.copy(members = group.members + newMember)
                } else group
            }
            data.copy(groups = updatedGroups)
        }
    }

    override suspend fun leaveGroup(groupId: String) {
        updateSocialData { data ->
            data.copy(groups = data.groups.filter { it.id != groupId })
        }
    }

    override suspend fun updateGroupSettings(groupId: String, settings: GroupSettings) {
        updateSocialData { data ->
            val updatedGroups = data.groups.map { group ->
                if (group.id == groupId) group.copy(settings = settings) else group
            }
            data.copy(groups = updatedGroups)
        }
    }

    // Partners - Using Firebase for real partner connections
    override fun getPartners(): Flow<List<AccountabilityPartner>> {
        return getSocialDataFlow().map { data ->
            // Combine local partners with Firebase friends as potential partners
            data.partners
        }
    }

    override suspend fun sendPartnerRequest(toUserId: String) {
        // Send real friend request via Firebase
        val userId = firebaseService.getCurrentUserId() ?: return
        firebaseService.sendFriendRequest(userId, toUserId)

        // Also update local data
        updateSocialData { data ->
            val newPartner = AccountabilityPartner(
                partnerId = toUserId,
                displayName = "Pending...",
                profileEmoji = "👤",
                connectedAt = Clock.System.now().toString(),
                status = PartnerStatus.PENDING_SENT
            )
            data.copy(partners = data.partners + newPartner)
        }
    }

    override suspend fun acceptPartnerRequest(partnerId: String) {
        // Accept via Firebase
        val userId = firebaseService.getCurrentUserId() ?: return
        firebaseService.acceptFriendRequest(partnerId, userId)

        // Update local data
        updateSocialData { data ->
            val updatedPartners = data.partners.map { partner ->
                if (partner.partnerId == partnerId && partner.status == PartnerStatus.PENDING_RECEIVED) {
                    partner.copy(status = PartnerStatus.ACTIVE)
                } else partner
            }
            data.copy(partners = updatedPartners)
        }
    }

    override suspend fun declinePartnerRequest(partnerId: String) {
        // TODO: Implement decline via Firebase when method is available
        // For now just update local data
        updateSocialData { data ->
            val updatedPartners = data.partners.map { partner ->
                if (partner.partnerId == partnerId) {
                    partner.copy(status = PartnerStatus.DECLINED)
                } else partner
            }
            data.copy(partners = updatedPartners)
        }
    }

    override suspend fun removePartner(partnerId: String) {
        // TODO: Implement remove via Firebase when method is available
        // For now just update local data
        updateSocialData { data ->
            data.copy(partners = data.partners.filter { it.partnerId != partnerId })
        }
    }

    // Shared Habits
    override fun getSharedHabits(): Flow<List<SharedHabit>> {
        return getSocialDataFlow().map { it.sharedHabits }
    }

    override suspend fun shareHabit(habitId: String, targets: List<SharingTarget>) {
        updateSocialData { data ->
            val existingIndex = data.sharedHabits.indexOfFirst { it.habitId == habitId }
            val updatedSharedHabits = if (existingIndex >= 0) {
                data.sharedHabits.toMutableList().apply {
                    val existing = get(existingIndex)
                    set(existingIndex, existing.copy(sharedWith = existing.sharedWith + targets))
                }
            } else {
                val habitName = when (habitId) {
                    "sleep" -> "Rest"
                    "water" -> "Hydrate"
                    "move" -> "Move"
                    "vegetables" -> "Nourish"
                    "calm" -> "Calm"
                    "connect" -> "Connect"
                    "unplug" -> "Unplug"
                    else -> habitId
                }
                val habitEmoji = when (habitId) {
                    "sleep" -> "🌙"
                    "water" -> "💧"
                    "move" -> "🏃"
                    "vegetables" -> "🥗"
                    "calm" -> "🧘"
                    "connect" -> "💬"
                    "unplug" -> "📴"
                    else -> "✨"
                }
                data.sharedHabits + SharedHabit(
                    habitId = habitId,
                    habitName = habitName,
                    habitEmoji = habitEmoji,
                    sharedWith = targets
                )
            }
            data.copy(sharedHabits = updatedSharedHabits)
        }
    }

    override suspend fun unshareHabit(habitId: String, targetId: String) {
        updateSocialData { data ->
            val updatedSharedHabits = data.sharedHabits.map { sharedHabit ->
                if (sharedHabit.habitId == habitId) {
                    sharedHabit.copy(sharedWith = sharedHabit.sharedWith.filter { it.targetId != targetId })
                } else sharedHabit
            }.filter { it.sharedWith.isNotEmpty() }
            data.copy(sharedHabits = updatedSharedHabits)
        }
    }

    override suspend fun updateSharingSettings(habitId: String, shareStreak: Boolean, shareCompletionTime: Boolean) {
        updateSocialData { data ->
            val updatedSharedHabits = data.sharedHabits.map { sharedHabit ->
                if (sharedHabit.habitId == habitId) {
                    sharedHabit.copy(shareStreak = shareStreak, shareCompletionTime = shareCompletionTime)
                } else sharedHabit
            }
            data.copy(sharedHabits = updatedSharedHabits)
        }
    }

    // High Fives - Using Firebase cheers
    override fun getReceivedHighFives(): Flow<List<HighFive>> {
        return getSocialDataFlow().map { data ->
            data.receivedHighFives.sortedByDescending { it.createdAt }
        }
    }

    override fun getSentHighFives(): Flow<List<HighFive>> {
        return getSocialDataFlow().map { data ->
            data.sentHighFives.sortedByDescending { it.createdAt }
        }
    }

    override fun getUnreadHighFiveCount(): Flow<Int> {
        return getSocialDataFlow().map { data ->
            data.receivedHighFives.count { !it.isRead }
        }
    }

    override suspend fun sendHighFive(toUserId: String, reason: HighFiveReason, habitId: String?, message: String?) {
        // Send cheer via Firebase
        val cheerType = when (reason) {
            HighFiveReason.STREAK_MILESTONE -> "ENCOURAGEMENT"
            HighFiveReason.PERFECT_DAY -> "CELEBRATION"
            HighFiveReason.COMEBACK -> "MOTIVATION"
            HighFiveReason.CONSISTENCY -> "ENCOURAGEMENT"
            HighFiveReason.ENCOURAGEMENT -> "ENCOURAGEMENT"
            HighFiveReason.CELEBRATION -> "CELEBRATION"
        }

        val cheerMessage = message ?: SocialMessages.getHighFiveMessage(reason, habitId)
        val currentData = getSocialDataFlow().first()
        val userId = currentData.userId.ifEmpty { firebaseService.getCurrentUserId() ?: "self" }
        val userName = currentData.displayName.ifEmpty { "You" }
        firebaseService.sendCheer(userId, userName, toUserId, cheerType, cheerMessage)

        // Also update local data
        updateSocialData { data ->
            val highFive = HighFive(
                id = "highfive_${Clock.System.now().toEpochMilliseconds()}",
                fromUserId = data.userId.ifEmpty { "self" },
                fromDisplayName = data.displayName.ifEmpty { "You" },
                fromEmoji = data.profileEmoji,
                toUserId = toUserId,
                habitId = habitId,
                habitName = habitId?.let {
                    when (it) {
                        "sleep" -> "Rest"
                        "water" -> "Hydrate"
                        "move" -> "Move"
                        else -> it
                    }
                },
                reason = reason,
                message = cheerMessage,
                createdAt = Clock.System.now().toString()
            )
            data.copy(sentHighFives = data.sentHighFives + highFive)
        }
    }

    override suspend fun markHighFiveAsRead(highFiveId: String) {
        updateSocialData { data ->
            val updatedHighFives = data.receivedHighFives.map { highFive ->
                if (highFive.id == highFiveId) highFive.copy(isRead = true) else highFive
            }
            data.copy(receivedHighFives = updatedHighFives)
        }
    }

    override suspend fun markAllHighFivesAsRead() {
        updateSocialData { data ->
            val updatedHighFives = data.receivedHighFives.map { it.copy(isRead = true) }
            data.copy(receivedHighFives = updatedHighFives)
        }
    }

    // Commitment Contracts
    override fun getCommitmentContracts(): Flow<List<CommitmentContract>> {
        return getSocialDataFlow().map { it.commitmentContracts }
    }

    override suspend fun createCommitmentContract(contract: CommitmentContract) {
        updateSocialData { data ->
            data.copy(commitmentContracts = data.commitmentContracts + contract)
        }
    }

    override suspend fun updateContractProgress(contractId: String, progress: Int) {
        updateSocialData { data ->
            val updatedContracts = data.commitmentContracts.map { contract ->
                if (contract.id == contractId) {
                    val newStatus = when {
                        progress >= contract.targetDays -> ContractStatus.COMPLETED
                        else -> ContractStatus.ACTIVE
                    }
                    contract.copy(currentProgress = progress, status = newStatus)
                } else contract
            }
            data.copy(commitmentContracts = updatedContracts)
        }
    }

    override suspend fun cancelContract(contractId: String) {
        updateSocialData { data ->
            val updatedContracts = data.commitmentContracts.map { contract ->
                if (contract.id == contractId) contract.copy(status = ContractStatus.CANCELLED) else contract
            }
            data.copy(commitmentContracts = updatedContracts)
        }
    }

    // Privacy
    override fun getPrivacySettings(): Flow<PrivacySettings> {
        return getSocialDataFlow().map { it.privacySettings }
    }

    override suspend fun updatePrivacySettings(settings: PrivacySettings) {
        updateSocialData { data ->
            data.copy(privacySettings = settings)
        }
    }

    // Profile - Synced with Firebase
    override suspend fun updateProfile(displayName: String, profileEmoji: String) {
        // Update Firebase profile
        firebaseService.updateDisplayName(displayName, profileEmoji)

        // Update local data
        updateSocialData { data ->
            data.copy(displayName = displayName, profileEmoji = profileEmoji)
        }
    }

    override suspend fun clearAllSocialData() {
        updateSocialData { SocialAccountabilityData() }
    }

    // ==================== NEW FIREBASE-SPECIFIC METHODS ====================

    /**
     * Sync local social data with Firebase
     */
    suspend fun syncWithFirebase() {
        // Fetch received cheers and convert to high fives
        val userId = firebaseService.getCurrentUserId() ?: return
        val cheers = firebaseService.getReceivedCheers(userId).first()
        val highFives = cheers.map { cheer ->
            HighFive(
                id = cheer.id,
                fromUserId = cheer.fromUserId,
                fromDisplayName = cheer.fromUserName,
                fromEmoji = "😊", // CheerData doesn't have emoji field
                toUserId = userId,
                habitId = null,
                habitName = null,
                reason = HighFiveReason.ENCOURAGEMENT, // Map generic cheer to encouragement
                message = cheer.message ?: "",
                createdAt = cheer.createdAt,
                isRead = cheer.isRead
            )
        }

        updateSocialData { data ->
            data.copy(receivedHighFives = highFives)
        }
    }

    /**
     * Search for users to connect with
     */
    suspend fun searchUsers(query: String): List<com.dailywell.app.data.repository.UserSearchResult> {
        return firebaseService.searchUsers(query).map { result ->
            com.dailywell.app.data.repository.UserSearchResult(
                userId = result.userId,
                displayName = result.displayName,
                avatarEmoji = result.profileEmoji,
                level = result.currentLevel,
                isFriend = false, // TODO: Check actual friend status
                hasPendingRequest = false // TODO: Check actual pending request status
            )
        }
    }

    /**
     * Ensure user is signed in to Firebase
     */
    suspend fun ensureSignedIn(): Boolean {
        return firebaseService.signInAnonymously().isSuccess
    }
}

