package com.dailywell.app.ui.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class SocialUiState(
    val groups: List<AccountabilityGroup> = emptyList(),
    val partners: List<AccountabilityPartner> = emptyList(),
    val sharedHabits: List<SharedHabit> = emptyList(),
    val receivedHighFives: List<HighFive> = emptyList(),
    val unreadHighFiveCount: Int = 0,
    val commitmentContracts: List<CommitmentContract> = emptyList(),
    val privacySettings: PrivacySettings = PrivacySettings(),
    val displayName: String = "",
    val profileEmoji: String = "🧑",
    val isLoading: Boolean = true,
    val selectedTab: SocialTab = SocialTab.GROUPS
)

enum class SocialTab {
    GROUPS,
    PARTNERS,
    HIGH_FIVES,
    CONTRACTS
}

/**
 * ViewModel for Social Accountability features
 */
class SocialViewModel(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    init {
        loadSocialData()
    }

    private fun loadSocialData() {
        viewModelScope.launch {
            socialRepository.getSocialData().collect { data ->
                _uiState.value = _uiState.value.copy(
                    groups = data.groups,
                    partners = data.partners,
                    sharedHabits = data.sharedHabits,
                    receivedHighFives = data.receivedHighFives,
                    commitmentContracts = data.commitmentContracts,
                    privacySettings = data.privacySettings,
                    displayName = data.displayName,
                    profileEmoji = data.profileEmoji,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            socialRepository.getUnreadHighFiveCount().collect { count ->
                _uiState.value = _uiState.value.copy(unreadHighFiveCount = count)
            }
        }
    }

    fun selectTab(tab: SocialTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // Group actions
    fun createGroup(name: String, emoji: String, type: GroupType) {
        viewModelScope.launch {
            val group = AccountabilityGroup(
                id = "group_${Clock.System.now().toEpochMilliseconds()}",
                name = name,
                emoji = emoji,
                createdBy = _uiState.value.displayName.ifEmpty { "You" },
                createdAt = Clock.System.now().toString(),
                groupType = type,
                members = listOf(
                    GroupMember(
                        userId = "self",
                        displayName = _uiState.value.displayName.ifEmpty { "You" },
                        profileEmoji = _uiState.value.profileEmoji,
                        role = GroupRole.OWNER,
                        joinedAt = Clock.System.now().toString()
                    )
                ),
                inviteCode = generateInviteCode()
            )
            socialRepository.createGroup(group)
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            socialRepository.leaveGroup(groupId)
        }
    }

    // Partner actions
    fun sendPartnerRequest(toUserId: String) {
        viewModelScope.launch {
            socialRepository.sendPartnerRequest(toUserId)
        }
    }

    fun acceptPartnerRequest(partnerId: String) {
        viewModelScope.launch {
            socialRepository.acceptPartnerRequest(partnerId)
        }
    }

    fun declinePartnerRequest(partnerId: String) {
        viewModelScope.launch {
            socialRepository.declinePartnerRequest(partnerId)
        }
    }

    fun removePartner(partnerId: String) {
        viewModelScope.launch {
            socialRepository.removePartner(partnerId)
        }
    }

    // Sharing actions
    fun shareHabitWithGroup(habitId: String, groupId: String, groupName: String) {
        viewModelScope.launch {
            val target = SharingTarget(
                targetId = groupId,
                targetType = SharingTargetType.GROUP,
                targetName = groupName,
                sharedAt = Clock.System.now().toString()
            )
            socialRepository.shareHabit(habitId, listOf(target))
        }
    }

    fun shareHabitWithPartner(habitId: String, partnerId: String, partnerName: String) {
        viewModelScope.launch {
            val target = SharingTarget(
                targetId = partnerId,
                targetType = SharingTargetType.PARTNER,
                targetName = partnerName,
                sharedAt = Clock.System.now().toString()
            )
            socialRepository.shareHabit(habitId, listOf(target))
        }
    }

    fun unshareHabit(habitId: String, targetId: String) {
        viewModelScope.launch {
            socialRepository.unshareHabit(habitId, targetId)
        }
    }

    // High Five actions
    fun sendHighFive(toUserId: String, reason: HighFiveReason, habitId: String? = null) {
        viewModelScope.launch {
            socialRepository.sendHighFive(toUserId, reason, habitId)
        }
    }

    fun markHighFiveAsRead(highFiveId: String) {
        viewModelScope.launch {
            socialRepository.markHighFiveAsRead(highFiveId)
        }
    }

    fun markAllHighFivesAsRead() {
        viewModelScope.launch {
            socialRepository.markAllHighFivesAsRead()
        }
    }

    // Commitment Contract actions
    fun createCommitmentContract(
        habitId: String,
        habitName: String,
        commitment: String,
        targetDays: Int,
        startDate: String,
        endDate: String,
        stakes: String? = null
    ) {
        viewModelScope.launch {
            val contract = CommitmentContract(
                id = "contract_${Clock.System.now().toEpochMilliseconds()}",
                userId = "self",
                habitId = habitId,
                habitName = habitName,
                commitment = commitment,
                startDate = startDate,
                endDate = endDate,
                targetDays = targetDays,
                stakes = stakes,
                createdAt = Clock.System.now().toString()
            )
            socialRepository.createCommitmentContract(contract)
        }
    }

    fun cancelContract(contractId: String) {
        viewModelScope.launch {
            socialRepository.cancelContract(contractId)
        }
    }

    // Privacy actions
    fun updatePrivacySettings(settings: PrivacySettings) {
        viewModelScope.launch {
            socialRepository.updatePrivacySettings(settings)
        }
    }

    // Profile actions
    fun updateProfile(displayName: String, profileEmoji: String) {
        viewModelScope.launch {
            socialRepository.updateProfile(displayName, profileEmoji)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
