package com.dailywell.app.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.CelebrationMessages
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.AchievementRepository
import com.dailywell.app.data.repository.EntryRepository
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

data class TodayUiState(
    val habits: List<Habit> = emptyList(),
    val completions: Map<String, Boolean> = emptyMap(),
    val streakInfo: StreakInfo = StreakInfo(),
    val weekData: WeekData? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val celebrationMessage: String? = null,
    val showMilestoneDialog: Boolean = false,
    val currentMilestone: StreakMilestone? = null,
    val newAchievement: Achievement? = null,
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val todayDate: String = "",
    // Mood tracking state
    val hasCheckedMood: Boolean = false,
    val currentMood: MoodLevel? = null,
    val showMoodCard: Boolean = true,
    // Trial state
    val isOnTrial: Boolean = false,
    val trialDaysRemaining: Int = 0,
    // Social proof
    val socialProofMessage: String? = null
)

class TodayViewModel(
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    private var previousStreak = 0

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            _uiState.update { it.copy(todayDate = today) }

            // Initialize default habits if needed
            habitRepository.initializeDefaultHabits()

            // Combine all the flows
            combine(
                habitRepository.getEnabledHabits(),
                entryRepository.getTodayEntry(),
                entryRepository.getStreakInfo(),
                entryRepository.getWeekData(0),
                settingsRepository.getSettings()
            ) { habits, todayEntry, streakInfo, weekData, settings ->
                val completions = todayEntry?.completions ?: emptyMap()
                val completedCount = completions.count { it.value }

                // Calculate trial status
                val hasFullAccess = settings.hasPremiumAccess(today)
                val isOnTrial = !settings.isPremium && settings.isTrialActive(today)
                val trialDays = settings.trialDaysRemaining(today)

                // Generate social proof message occasionally
                val socialProof = if (isOnTrial && (0..3).random() == 0) {
                    SocialProofMessages.communityStats.random()
                        .format((1000..5000).random())
                } else null

                TodayUiState(
                    habits = habits,
                    completions = completions,
                    streakInfo = streakInfo,
                    weekData = weekData,
                    completedCount = completedCount,
                    totalCount = habits.size,
                    isPremium = hasFullAccess,
                    isLoading = false,
                    todayDate = today,
                    hasCheckedMood = _uiState.value.hasCheckedMood,
                    currentMood = _uiState.value.currentMood,
                    showMoodCard = _uiState.value.showMoodCard,
                    isOnTrial = isOnTrial,
                    trialDaysRemaining = trialDays,
                    socialProofMessage = socialProof
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleHabit(habitId: String, completed: Boolean) {
        viewModelScope.launch {
            val today = _uiState.value.todayDate
            val previousStreakValue = _uiState.value.streakInfo.currentStreak

            entryRepository.setHabitCompletion(today, habitId, completed)

            // Check for celebration
            val newCompletions = _uiState.value.completions + (habitId to completed)
            val newCompletedCount = newCompletions.count { it.value }
            val total = _uiState.value.totalCount

            // Show celebration when all habits completed
            if (completed && newCompletedCount == total && total > 0) {
                val message = CelebrationMessages.getCompletionMessage(newCompletedCount, total)
                _uiState.update { it.copy(celebrationMessage = message) }
            }

            // Check for streak milestone and unlock achievements
            val newStreakInfo = entryRepository.getStreakInfo().first()
            if (newStreakInfo.currentStreak > previousStreakValue) {
                // Check for streak achievements
                achievementRepository.checkAndUnlockStreakAchievements(newStreakInfo.currentStreak)
            }

            if (newStreakInfo.isNewMilestone(previousStreakValue)) {
                newStreakInfo.getMilestone()?.let { milestone ->
                    _uiState.update {
                        it.copy(
                            showMilestoneDialog = true,
                            currentMilestone = milestone
                        )
                    }
                }
            }
        }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(celebrationMessage = null) }
    }

    fun dismissMilestone() {
        _uiState.update {
            it.copy(
                showMilestoneDialog = false,
                currentMilestone = null
            )
        }
    }

    fun dismissNewAchievement() {
        _uiState.update { it.copy(newAchievement = null) }
    }

    /**
     * Mood Tracking - FBI Psychology "Labeling Emotions"
     * When users label emotions, they feel understood and connected
     */
    fun selectMood(mood: MoodLevel) {
        _uiState.update { it.copy(currentMood = mood) }
        // Persist the mood selection
        viewModelScope.launch {
            val today = _uiState.value.todayDate
            entryRepository.setMood(today, mood)
        }
    }

    fun dismissMoodCard() {
        _uiState.update {
            it.copy(
                hasCheckedMood = true,
                showMoodCard = false
            )
        }
    }

    fun dismissSocialProof() {
        _uiState.update { it.copy(socialProofMessage = null) }
    }
}
