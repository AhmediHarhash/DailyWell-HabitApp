package com.dailywell.app.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywell.app.data.model.CompletionRecord
import com.dailywell.app.data.model.HabitCorrelation
import com.dailywell.app.data.model.InsightType
import com.dailywell.app.data.model.PatternInsight
import com.dailywell.app.data.model.WeeklyInsightReport
import com.dailywell.app.data.repository.PatternInsightRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AIInsightsUiState(
    val insights: List<PatternInsight> = emptyList(),
    val correlations: List<HabitCorrelation> = emptyList(),
    val weeklyReport: WeeklyInsightReport? = null,
    val selectedInsight: PatternInsight? = null,
    val isLoading: Boolean = true,
    val isAnalyzing: Boolean = false
)

/**
 * ViewModel for AI Pattern Recognition & Insights
 */
class AIInsightsViewModel(
    private val patternInsightRepository: PatternInsightRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIInsightsUiState())
    val uiState: StateFlow<AIInsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
        loadCorrelations()
        loadWeeklyReport()
    }

    private fun loadInsights() {
        viewModelScope.launch {
            patternInsightRepository.getAllInsights().collect { insights ->
                _uiState.value = _uiState.value.copy(
                    insights = insights,
                    isLoading = false
                )
            }
        }
    }

    private fun loadCorrelations() {
        viewModelScope.launch {
            patternInsightRepository.getCorrelations().collect { correlations ->
                _uiState.value = _uiState.value.copy(correlations = correlations)
            }
        }
    }

    private fun loadWeeklyReport() {
        viewModelScope.launch {
            patternInsightRepository.getWeeklyReport().collect { report ->
                _uiState.value = _uiState.value.copy(weeklyReport = report)
            }
        }
    }

    fun selectInsight(insight: PatternInsight?) {
        _uiState.value = _uiState.value.copy(selectedInsight = insight)
    }

    fun dismissInsight(insightId: String) {
        viewModelScope.launch {
            patternInsightRepository.dismissInsight(insightId)
        }
    }

    fun runAnalysis(completionHistory: Map<String, List<CompletionRecord>>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            patternInsightRepository.analyzePatterns(completionHistory)
            _uiState.value = _uiState.value.copy(isAnalyzing = false)
        }
    }

    fun getInsightsForHabit(habitId: String): List<PatternInsight> {
        return _uiState.value.insights.filter { habitId in it.relatedHabits }
    }

    fun getTopInsight(): PatternInsight? {
        return _uiState.value.insights
            .filter { it.actionable }
            .maxByOrNull { it.significance }
    }

    fun getCelebrationInsights(): List<PatternInsight> {
        return _uiState.value.insights.filter { it.type == InsightType.CELEBRATION }
    }

    fun getWarningInsights(): List<PatternInsight> {
        return _uiState.value.insights.filter { it.type == InsightType.STREAK_RISK }
    }
}
