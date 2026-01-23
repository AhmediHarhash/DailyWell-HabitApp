package com.dailywell.app.ui.screens.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FamilyUiState(
    val familyData: FamilyPlanData = FamilyPlanData(),
    val activityFeed: List<FamilyActivity> = emptyList(),
    val selectedTab: FamilyTab = FamilyTab.OVERVIEW,
    val isLoading: Boolean = true,
    val showCreateChallengeDialog: Boolean = false,
    val showInviteDialog: Boolean = false,
    val showJoinDialog: Boolean = false,
    val joinCode: String = "",
    val error: String? = null
)

enum class FamilyTab(val title: String, val emoji: String) {
    OVERVIEW("Family", "👨‍👩‍👧‍👦"),
    CHALLENGES("Challenges", "🏆"),
    ACTIVITY("Activity", "📱"),
    SETTINGS("Settings", "⚙️")
}

/**
 * ViewModel for Family Plan features
 */
class FamilyViewModel(
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    init {
        loadFamilyData()
    }

    private fun loadFamilyData() {
        viewModelScope.launch {
            familyRepository.getFamilyData().collect { data ->
                _uiState.value = _uiState.value.copy(
                    familyData = data,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            familyRepository.getActivityFeed().collect { activities ->
                _uiState.value = _uiState.value.copy(activityFeed = activities)
            }
        }
    }

    fun selectTab(tab: FamilyTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // Family management
    fun createFamily(familyName: String) {
        viewModelScope.launch {
            try {
                familyRepository.createFamily(familyName)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun showJoinDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showJoinDialog = show,
            joinCode = ""
        )
    }

    fun updateJoinCode(code: String) {
        _uiState.value = _uiState.value.copy(joinCode = code.uppercase())
    }

    fun joinFamily() {
        viewModelScope.launch {
            try {
                val success = familyRepository.joinFamily(_uiState.value.joinCode)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        showJoinDialog = false,
                        joinCode = ""
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = "Invalid invite code")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun leaveFamily() {
        viewModelScope.launch {
            familyRepository.leaveFamily()
        }
    }

    fun showInviteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showInviteDialog = show)
    }

    fun regenerateInviteCode() {
        viewModelScope.launch {
            familyRepository.generateInviteCode()
        }
    }

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            familyRepository.removeMember(memberId)
        }
    }

    fun updateMemberRole(memberId: String, role: FamilyRole) {
        viewModelScope.launch {
            familyRepository.updateMemberRole(memberId, role)
        }
    }

    // Challenges
    fun showCreateChallengeDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCreateChallengeDialog = show)
    }

    fun createChallenge(challenge: FamilyChallenge) {
        viewModelScope.launch {
            familyRepository.createChallenge(challenge)
            _uiState.value = _uiState.value.copy(showCreateChallengeDialog = false)
        }
    }

    fun createTemplateChallenge(template: ChallengeTemplate) {
        val challenge = when (template) {
            ChallengeTemplate.FAMILY_STREAK -> FamilyChallengeTemplates.weeklyFamilyStreak()
            ChallengeTemplate.STEP_CHALLENGE -> FamilyChallengeTemplates.stepChallenge()
            ChallengeTemplate.HYDRATION -> FamilyChallengeTemplates.hydrationTeamChallenge()
            ChallengeTemplate.SCREEN_FREE -> FamilyChallengeTemplates.screenFreeEvening()
            ChallengeTemplate.MINDFULNESS -> FamilyChallengeTemplates.mindfulnessMarathon()
        }
        createChallenge(challenge)
    }

    fun updateChallengeProgress(challengeId: String, progress: Int) {
        viewModelScope.launch {
            // Get the current user's member ID from family data
            val currentMemberId = _uiState.value.familyData.members
                .firstOrNull { it.role == FamilyRole.OWNER }?.id
                ?: _uiState.value.familyData.members.firstOrNull()?.id
                ?: return@launch
            familyRepository.updateChallengeProgress(challengeId, currentMemberId, progress)
        }
    }

    fun completeChallenge(challengeId: String) {
        viewModelScope.launch {
            familyRepository.completeChallenge(challengeId)
        }
    }

    fun cancelChallenge(challengeId: String) {
        viewModelScope.launch {
            familyRepository.cancelChallenge(challengeId)
        }
    }

    // Activity
    fun sendHighFive(activityId: String) {
        viewModelScope.launch {
            familyRepository.sendHighFive(activityId)
        }
    }

    // Settings
    fun toggleHabitSharing(habitId: String, shared: Boolean) {
        viewModelScope.launch {
            familyRepository.toggleHabitSharing(habitId, shared)
        }
    }

    // Helpers
    fun getTotalChallengeProgress(challenge: FamilyChallenge): Int {
        return challenge.currentProgress.values.sum()
    }

    fun getChallengeProgressPercent(challenge: FamilyChallenge): Float {
        val total = getTotalChallengeProgress(challenge)
        return (total.toFloat() / challenge.targetValue).coerceIn(0f, 1f)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

enum class ChallengeTemplate {
    FAMILY_STREAK,
    STEP_CHALLENGE,
    HYDRATION,
    SCREEN_FREE,
    MINDFULNESS
}
