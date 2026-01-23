package com.dailywell.app.ui.screens.gamification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.GamificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GamificationUiState(
    val gamificationData: GamificationData = GamificationData(),
    val unlockedBadges: List<UnlockedBadge> = emptyList(),
    val allBadgesWithStatus: List<Pair<Badge, Boolean>> = emptyList(),
    val unlockedThemes: List<AppTheme> = emptyList(),
    val selectedTheme: AppTheme = ThemeLibrary.allThemes.first(),
    val canClaimDailyReward: Boolean = false,
    val canSpin: Boolean = false,
    val lastSpinResult: SpinWheelResult? = null,
    val levelTitle: LevelTitle = LevelSystem.getLevelTitle(1),
    val xpProgress: Float = 0f,
    val recentlyUnlockedBadge: Badge? = null,
    val showSpinWheel: Boolean = false,
    val spinResult: SpinReward? = null,
    val isSpinning: Boolean = false,
    val showDailyReward: Boolean = false,
    val dailyReward: DailyReward? = null,
    val isLoading: Boolean = true
)

class GamificationViewModel(
    private val gamificationRepository: GamificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    init {
        loadGamificationData()
    }

    private fun loadGamificationData() {
        viewModelScope.launch {
            // Combine first 5 flows
            combine(
                gamificationRepository.getGamificationData(),
                gamificationRepository.getUnlockedBadges(),
                gamificationRepository.getAllBadgesWithStatus(),
                gamificationRepository.getUnlockedThemes(),
                gamificationRepository.getSelectedTheme()
            ) { data, unlockedBadges, allBadges, themes, selectedTheme ->
                GamificationUiState(
                    gamificationData = data,
                    unlockedBadges = unlockedBadges,
                    allBadgesWithStatus = allBadges,
                    unlockedThemes = themes,
                    selectedTheme = selectedTheme,
                    levelTitle = LevelSystem.getLevelTitle(data.currentLevel),
                    xpProgress = LevelSystem.progressToNextLevel(data.totalXp),
                    isLoading = false
                )
            }.collect { state ->
                val canClaim = gamificationRepository.canClaimDailyReward()
                val canSpinNow = gamificationRepository.canSpin()
                _uiState.value = state.copy(
                    canClaimDailyReward = canClaim,
                    canSpin = canSpinNow
                )
            }
        }
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            val reward = gamificationRepository.claimDailyReward()
            if (reward != null) {
                _uiState.update { it.copy(
                    showDailyReward = true,
                    dailyReward = reward,
                    canClaimDailyReward = false
                ) }
            }
        }
    }

    fun dismissDailyReward() {
        _uiState.update { it.copy(showDailyReward = false, dailyReward = null) }
    }

    fun openSpinWheel() {
        _uiState.update { it.copy(showSpinWheel = true, spinResult = null) }
    }

    fun closeSpinWheel() {
        _uiState.update { it.copy(showSpinWheel = false, spinResult = null, isSpinning = false) }
    }

    fun spin() {
        if (!_uiState.value.canSpin || _uiState.value.isSpinning) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSpinning = true) }

            val result = gamificationRepository.spin()

            // Delay to let animation play
            kotlinx.coroutines.delay(3000)

            _uiState.update { it.copy(
                isSpinning = false,
                spinResult = result?.reward,
                canSpin = false
            ) }
        }
    }

    fun selectTheme(themeId: String) {
        viewModelScope.launch {
            gamificationRepository.selectTheme(themeId)
        }
    }

    fun useStreakShield() {
        viewModelScope.launch {
            gamificationRepository.useStreakShield()
        }
    }

    fun dismissRecentBadge() {
        _uiState.update { it.copy(recentlyUnlockedBadge = null) }
    }

    // For habit completion integration
    fun onHabitCompleted(
        habitId: String,
        isAllCompleted: Boolean,
        isEarlyBird: Boolean,
        isMorning: Boolean
    ) {
        viewModelScope.launch {
            gamificationRepository.recordHabitCompletion(habitId, isAllCompleted, isEarlyBird, isMorning)

            // Check for newly unlocked badges
            val newBadges = gamificationRepository.checkAndUnlockBadges()
            if (newBadges.isNotEmpty()) {
                _uiState.update { it.copy(recentlyUnlockedBadge = newBadges.first()) }
            }
        }
    }

    fun onPerfectDay() {
        viewModelScope.launch {
            gamificationRepository.recordPerfectDay()
        }
    }

    fun onPerfectWeek() {
        viewModelScope.launch {
            gamificationRepository.recordPerfectWeek()
        }
    }

    fun updateStreak(current: Int, longest: Int) {
        viewModelScope.launch {
            gamificationRepository.updateStreak(current, longest)
        }
    }

    fun onAppOpen() {
        viewModelScope.launch {
            gamificationRepository.recordLogin()
            gamificationRepository.resetDailyXp()
            gamificationRepository.resetWeeklyXp()
            gamificationRepository.resetMonthlyXp()
        }
    }

    // Badge filtering helpers
    fun getBadgesByCategory(category: BadgeCategory): List<Pair<Badge, Boolean>> {
        return _uiState.value.allBadgesWithStatus.filter { it.first.category == category }
    }

    fun getBadgeProgress(badge: Badge): Float {
        val data = _uiState.value.gamificationData
        return when (val req = badge.requirement) {
            is BadgeRequirement.StreakDays -> (data.currentStreak.toFloat() / req.days).coerceIn(0f, 1f)
            is BadgeRequirement.TotalHabits -> (data.totalHabitsCompleted.toFloat() / req.count).coerceIn(0f, 1f)
            is BadgeRequirement.PerfectDays -> (data.perfectDays.toFloat() / req.count).coerceIn(0f, 1f)
            is BadgeRequirement.PerfectWeeks -> (data.perfectWeeks.toFloat() / req.count).coerceIn(0f, 1f)
            is BadgeRequirement.Level -> (data.currentLevel.toFloat() / req.level).coerceIn(0f, 1f)
            is BadgeRequirement.XpEarned -> (data.lifetimeXp.toFloat() / req.xp).coerceIn(0f, 1f)
            is BadgeRequirement.ChallengesWon -> (data.challengesCompleted.toFloat() / req.count).coerceIn(0f, 1f)
            is BadgeRequirement.DuelsWon -> (data.duelsWon.toFloat() / req.count).coerceIn(0f, 1f)
            is BadgeRequirement.FriendsHelped -> (data.friendsHelped.toFloat() / req.count).coerceIn(0f, 1f)
            is BadgeRequirement.DailyLoginStreak -> (data.dailyRewardStreak.toFloat() / req.days).coerceIn(0f, 1f)
            else -> 0f
        }
    }
}
