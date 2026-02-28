package com.dailywell.app.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.dailywell.app.data.content.AchievementsDatabase
import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.AchievementCategory
import com.dailywell.app.data.repository.*
import com.dailywell.app.ui.components.DailyWellIcons
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * AchievementsViewModel - Production-Ready Achievement UI Management
 *
 * Features:
 * - Real-time achievement tracking from Firebase + Room
 * - Category filtering and sorting
 * - Progress tracking for locked achievements
 * - Celebration handling for newly unlocked
 * - Statistics and insights
 */

data class AchievementsUiState(
    val allAchievements: List<AchievementWithProgress> = emptyList(),
    val filteredAchievements: List<AchievementWithProgress> = emptyList(),
    val selectedCategory: AchievementCategory? = null,
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val progressPercent: Float = 0f,
    val stats: AchievementStats? = null,
    val nextAchievements: List<AchievementWithProgress> = emptyList(),
    val pendingCelebration: AchievementCelebration? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class AchievementsViewModel(
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
        observeCelebrations()
        loadStats()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            achievementRepository.getAllAchievements()
                .collect { achievements ->
                    val sorted = achievements.sortedWith(
                        compareByDescending<AchievementWithProgress> { it.isUnlocked }
                            .thenByDescending { it.unlockedAt ?: 0 }
                            .thenByDescending { it.progressPercent }
                    )

                    val unlockedCount = achievements.count { it.isUnlocked }
                    val totalCount = achievements.size

                    _uiState.update { state ->
                        val filtered = if (state.selectedCategory == null) {
                            sorted
                        } else {
                            sorted.filter { it.achievement.category == state.selectedCategory }
                        }

                        state.copy(
                            allAchievements = sorted,
                            filteredAchievements = filtered,
                            unlockedCount = unlockedCount,
                            totalCount = totalCount,
                            progressPercent = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun observeCelebrations() {
        viewModelScope.launch {
            achievementRepository.getRecentlyUnlocked()
                .collect { celebration ->
                    _uiState.update { it.copy(pendingCelebration = celebration) }
                }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = achievementRepository.getAchievementStats()
                val nextAchievements = achievementRepository.getNextAchievements(3)

                _uiState.update {
                    it.copy(
                        stats = stats,
                        nextAchievements = nextAchievements
                    )
                }
            } catch (e: Exception) {
                // Stats loading is non-critical
            }
        }
    }

    fun selectCategory(category: AchievementCategory?) {
        _uiState.update { state ->
            val filtered = if (category == null) {
                state.allAchievements
            } else {
                state.allAchievements.filter { it.achievement.category == category }
            }

            state.copy(
                selectedCategory = category,
                filteredAchievements = filtered
            )
        }
    }

    fun dismissCelebration() {
        viewModelScope.launch {
            _uiState.value.pendingCelebration?.let { celebration ->
                achievementRepository.dismissCelebration(celebration.achievement.id)
            }
            achievementRepository.clearRecentlyUnlocked()
        }
    }

    fun refreshStats() {
        loadStats()
    }

    fun syncAchievements() {
        viewModelScope.launch {
            try {
                achievementRepository.syncWithCloud()
                loadStats()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // ==================== HELPER METHODS ====================

    fun getAchievementsByCategory(): Map<AchievementCategory, List<AchievementWithProgress>> {
        return _uiState.value.allAchievements.groupBy { it.achievement.category }
    }

    fun getCategoryProgress(category: AchievementCategory): Pair<Int, Int> {
        val categoryAchievements = _uiState.value.allAchievements.filter {
            it.achievement.category == category
        }
        val unlocked = categoryAchievements.count { it.isUnlocked }
        return unlocked to categoryAchievements.size
    }

    fun getCategoryEmoji(category: AchievementCategory): String {
        return when (category) {
            AchievementCategory.STREAK -> "🔥"
            AchievementCategory.CONSISTENCY -> "✨"
            AchievementCategory.HABIT -> "🏆"
            AchievementCategory.SPECIAL -> "⭐"
        }
    }

    fun getCategoryIcon(category: AchievementCategory): ImageVector {
        return when (category) {
            AchievementCategory.STREAK -> DailyWellIcons.Analytics.Streak
            AchievementCategory.CONSISTENCY -> DailyWellIcons.Misc.Sparkle
            AchievementCategory.HABIT -> DailyWellIcons.Gamification.Trophy
            AchievementCategory.SPECIAL -> DailyWellIcons.Status.Star
        }
    }

    fun getCategoryName(category: AchievementCategory): String {
        return when (category) {
            AchievementCategory.STREAK -> "Streak Milestones"
            AchievementCategory.CONSISTENCY -> "Consistency"
            AchievementCategory.HABIT -> "Habit Mastery"
            AchievementCategory.SPECIAL -> "Special"
        }
    }

    fun getStreakProgress(currentStreak: Int): Float {
        return AchievementsDatabase.getStreakProgress(currentStreak)
    }

    fun getNextStreakMilestone(currentStreak: Int): Pair<Int, Achievement>? {
        return AchievementsDatabase.getNextStreakMilestone(currentStreak)
    }
}
