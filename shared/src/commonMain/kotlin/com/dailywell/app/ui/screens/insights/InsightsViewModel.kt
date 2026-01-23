package com.dailywell.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.Achievement
import com.dailywell.app.data.model.Habit
import com.dailywell.app.data.model.StreakInfo
import com.dailywell.app.data.repository.AchievementRepository
import com.dailywell.app.data.repository.EntryRepository
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Trend {
    IMPROVING,
    DECLINING,
    STABLE
}

data class HabitInsight(
    val habit: Habit,
    val rate30Day: Float,
    val rate7Day: Float,
    val trend: Trend
)

data class InsightsUiState(
    val habits: List<Habit> = emptyList(),
    val streakInfo: StreakInfo = StreakInfo(),
    val habitCompletionRates: Map<String, Float> = emptyMap(),
    val habitInsights: List<HabitInsight> = emptyList(),
    val correlations: List<HabitCorrelation> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val unlockedAchievements: List<Achievement> = emptyList(),
    val recentAchievement: Achievement? = null,
    val bestHabit: Habit? = null,
    val focusHabit: Habit? = null,
    val overallConsistency: Float = 0f,
    val weeklyConsistency: Float = 0f,
    val isPremium: Boolean = false,
    val isLoading: Boolean = true
)

data class HabitCorrelation(
    val habit1: Habit,
    val habit2: Habit,
    val correlation: Float, // -1 to 1
    val description: String
)

class InsightsViewModel(
    private val habitRepository: HabitRepository,
    private val entryRepository: EntryRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadAchievements()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                habitRepository.getEnabledHabits(),
                entryRepository.getStreakInfo(),
                settingsRepository.getSettings()
            ) { habits, streakInfo, settings ->
                Triple(habits, streakInfo, settings)
            }.collect { (habits, streakInfo, settings) ->
                _uiState.update {
                    it.copy(
                        habits = habits,
                        streakInfo = streakInfo,
                        isPremium = settings.isPremium,
                        isLoading = false
                    )
                }

                // Load completion rates and insights for each habit
                loadHabitInsights(habits)

                // Calculate correlations
                if (settings.isPremium && habits.size >= 2) {
                    calculateCorrelations(habits)
                }
            }
        }
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            achievementRepository.getAllAchievements().collect { achievements ->
                val unlocked = achievements.filter { it.isUnlocked }
                _uiState.update {
                    it.copy(
                        achievements = achievements,
                        unlockedAchievements = unlocked
                    )
                }
            }
        }

        viewModelScope.launch {
            achievementRepository.getRecentlyUnlocked().collect { achievement ->
                _uiState.update { it.copy(recentAchievement = achievement) }
            }
        }
    }

    private suspend fun loadHabitInsights(habits: List<Habit>) {
        val rates = mutableMapOf<String, Float>()
        val insights = mutableListOf<HabitInsight>()

        habits.forEach { habit ->
            val rate30Day = entryRepository.getCompletionRateForHabit(habit.id, 30)
            val rate7Day = entryRepository.getCompletionRateForHabit(habit.id, 7)
            val ratePrev7Day = calculatePreviousWeekRate(habit.id)

            rates[habit.id] = rate30Day

            val trend = when {
                rate7Day > ratePrev7Day + 0.1f -> Trend.IMPROVING
                rate7Day < ratePrev7Day - 0.1f -> Trend.DECLINING
                else -> Trend.STABLE
            }

            insights.add(
                HabitInsight(
                    habit = habit,
                    rate30Day = rate30Day,
                    rate7Day = rate7Day,
                    trend = trend
                )
            )
        }

        val bestHabit = habits.maxByOrNull { rates[it.id] ?: 0f }
        val focusHabit = habits.minByOrNull { rates[it.id] ?: 0f }
        val overallConsistency = if (rates.isNotEmpty()) rates.values.average().toFloat() else 0f
        val weeklyConsistency = if (insights.isNotEmpty()) {
            insights.map { it.rate7Day }.average().toFloat()
        } else 0f

        _uiState.update {
            it.copy(
                habitCompletionRates = rates,
                habitInsights = insights,
                bestHabit = bestHabit,
                focusHabit = focusHabit,
                overallConsistency = overallConsistency,
                weeklyConsistency = weeklyConsistency
            )
        }
    }

    private suspend fun calculatePreviousWeekRate(habitId: String): Float {
        // Get rate for days 8-14 (previous week)
        val rate14Day = entryRepository.getCompletionRateForHabit(habitId, 14)
        val rate7Day = entryRepository.getCompletionRateForHabit(habitId, 7)
        // Approximate previous week rate
        return (rate14Day * 14 - rate7Day * 7) / 7
    }

    private suspend fun calculateCorrelations(habits: List<Habit>) {
        val correlations = mutableListOf<HabitCorrelation>()

        // Simple correlation: habits completed together vs separately
        for (i in habits.indices) {
            for (j in i + 1 until habits.size) {
                val habit1 = habits[i]
                val habit2 = habits[j]

                val rate1 = _uiState.value.habitCompletionRates[habit1.id] ?: 0f
                val rate2 = _uiState.value.habitCompletionRates[habit2.id] ?: 0f

                // Simplified correlation based on completion rate similarity
                val correlation = 1f - kotlin.math.abs(rate1 - rate2)

                if (correlation > 0.7f && rate1 > 0.5f && rate2 > 0.5f) {
                    val description = generateCorrelationDescription(habit1, habit2, correlation)
                    correlations.add(
                        HabitCorrelation(
                            habit1 = habit1,
                            habit2 = habit2,
                            correlation = correlation,
                            description = description
                        )
                    )
                }
            }
        }

        _uiState.update { it.copy(correlations = correlations.sortedByDescending { it.correlation }.take(3)) }
    }

    private fun generateCorrelationDescription(habit1: Habit, habit2: Habit, correlation: Float): String {
        return when {
            correlation > 0.9f -> "You're great at doing ${habit1.name} and ${habit2.name} together!"
            correlation > 0.8f -> "When you ${habit1.name.lowercase()}, you usually ${habit2.name.lowercase()} too"
            else -> "These habits often go together for you"
        }
    }

    fun dismissRecentAchievement() {
        viewModelScope.launch {
            achievementRepository.clearRecentlyUnlocked()
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadData()
        loadAchievements()
    }
}
