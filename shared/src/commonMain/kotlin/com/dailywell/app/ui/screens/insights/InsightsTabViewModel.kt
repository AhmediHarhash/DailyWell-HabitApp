package com.dailywell.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.CompletionStatus
import com.dailywell.app.data.model.PatternInsight
import com.dailywell.app.data.repository.EntryRepository
import com.dailywell.app.data.repository.HabitRepository
import com.dailywell.app.data.repository.PatternInsightRepository
import com.dailywell.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InsightsTabUiState(
    val weekCompletionPercent: Int = 0,
    val bestDayOfWeek: String = "\u2014",
    val currentStreak: Int = 0,
    val featuredInsight: PatternInsight? = null,
    val isPremium: Boolean = false,
    val isLoading: Boolean = true
)

class InsightsTabViewModel(
    private val entryRepository: EntryRepository,
    private val habitRepository: HabitRepository,
    private val settingsRepository: SettingsRepository,
    private val patternInsightRepository: PatternInsightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsTabUiState())
    val uiState: StateFlow<InsightsTabUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        loadFeaturedInsight()
    }

    private fun loadStats() {
        viewModelScope.launch {
            combine(
                entryRepository.getWeekData(0),
                entryRepository.getStreakInfo(),
                settingsRepository.getSettings()
            ) { weekData, streakInfo, settings ->
                Triple(weekData, streakInfo, settings)
            }.collect { (weekData, streakInfo, settings) ->
                _uiState.update {
                    it.copy(
                        weekCompletionPercent = (weekData.completionRate * 100).toInt(),
                        currentStreak = streakInfo.currentStreak,
                        isPremium = settings.isPremium,
                        isLoading = false
                    )
                }
            }
        }
        loadBestDay()
    }

    /**
     * Computes best day of week from up to 4 weeks of data.
     * Guards: < 7 data points -> show "—" to avoid false confidence.
     */
    private fun loadBestDay() {
        viewModelScope.launch {
            try {
                val dayCompletions = mutableMapOf<Int, MutableList<Float>>()

                for (offset in 0 downTo -3) {
                    val weekData = entryRepository.getWeekData(offset).first()
                    for (day in weekData.days) {
                        if (!day.isFuture && day.status != CompletionStatus.NO_DATA && day.totalCount > 0) {
                            val rate = day.completedCount.toFloat() / day.totalCount
                            dayCompletions.getOrPut(day.dayOfWeek) { mutableListOf() }.add(rate)
                        }
                    }
                }

                val totalDataPoints = dayCompletions.values.sumOf { it.size }

                val bestDayLabel = when {
                    totalDataPoints < 7 -> "\u2014"
                    else -> {
                        val bestDay = dayCompletions.maxByOrNull { (_, rates) ->
                            if (rates.isNotEmpty()) rates.average().toFloat() else 0f
                        }
                        bestDay?.let { (dayOfWeek, _) ->
                            DAY_LABELS.getOrElse(dayOfWeek) { "\u2014" }
                        } ?: "\u2014"
                    }
                }

                _uiState.update { it.copy(bestDayOfWeek = bestDayLabel) }
            } catch (_: Exception) {
                _uiState.update { it.copy(bestDayOfWeek = "\u2014") }
            }
        }
    }

    /**
     * Stable featured insight selection:
     * 1. Non-dismissed only
     * 2. Most recent first (by createdAt)
     * 3. Tie-break by highest significance
     */
    private fun loadFeaturedInsight() {
        viewModelScope.launch {
            patternInsightRepository.getAllInsights().collect { insights ->
                val featured = insights
                    .filter { !it.isDismissed }
                    .sortedWith(
                        compareByDescending<PatternInsight> { it.createdAt }
                            .thenByDescending { it.significance }
                    )
                    .firstOrNull()

                _uiState.update { it.copy(featuredInsight = featured) }
            }
        }
    }

    companion object {
        private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }
}
