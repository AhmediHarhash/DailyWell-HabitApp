package com.dailywell.app.data.repository

import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PatternInsightRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : PatternInsightRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val INSIGHTS_KEY = "pattern_insights_data"
    }

    private fun getInsightsData(): Flow<PatternInsightsData> {
        return dataStoreManager.getString(INSIGHTS_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<PatternInsightsData>(it)
                } catch (e: Exception) {
                    PatternInsightsData()
                }
            } ?: PatternInsightsData()
        }
    }

    private suspend fun updateInsightsData(transform: (PatternInsightsData) -> PatternInsightsData) {
        val currentData = getInsightsData().first()
        val updatedData = transform(currentData)
        dataStoreManager.putString(INSIGHTS_KEY, json.encodeToString(updatedData))
    }

    override fun getAllInsights(): Flow<List<PatternInsight>> {
        return getInsightsData().map { data ->
            data.insights.filter { !it.isDismissed }
                .sortedByDescending { it.significance }
        }
    }

    override fun getInsightsForHabit(habitId: String): Flow<List<PatternInsight>> {
        return getInsightsData().map { data ->
            data.insights.filter { !it.isDismissed && habitId in it.relatedHabits }
                .sortedByDescending { it.significance }
        }
    }

    override fun getCorrelations(): Flow<List<HabitCorrelation>> {
        return getInsightsData().map { data ->
            data.correlations.sortedByDescending { kotlin.math.abs(it.strength) }
        }
    }

    override fun getPredictions(): Flow<List<SuccessPrediction>> {
        return getInsightsData().map { data ->
            data.predictions.sortedBy { it.predictedDate }
        }
    }

    override fun getWeeklyReport(): Flow<WeeklyInsightReport?> {
        return getInsightsData().map { it.weeklyReport }
    }

    override suspend fun analyzePatterns(completionHistory: Map<String, List<CompletionRecord>>) {
        val newInsights = PatternAnalyzer.analyzePatterns(completionHistory)

        updateInsightsData { currentData ->
            // Merge new insights with existing, avoiding duplicates
            val existingIds = currentData.insights.map { it.id }.toSet()
            val mergedInsights = currentData.insights + newInsights.filter { it.id !in existingIds }

            currentData.copy(
                insights = mergedInsights.takeLast(50), // Keep last 50 insights
                lastAnalyzedAt = kotlinx.datetime.Clock.System.now().toString()
            )
        }
    }

    override suspend fun addInsight(insight: PatternInsight) {
        updateInsightsData { currentData ->
            currentData.copy(
                insights = (currentData.insights + insight).takeLast(50)
            )
        }
    }

    override suspend fun dismissInsight(insightId: String) {
        updateInsightsData { currentData ->
            currentData.copy(
                insights = currentData.insights.map { insight ->
                    if (insight.id == insightId) insight.copy(isDismissed = true) else insight
                }
            )
        }
    }

    override suspend fun addCorrelation(correlation: HabitCorrelation) {
        updateInsightsData { currentData ->
            // Replace if exists, add if new
            val existingIndex = currentData.correlations.indexOfFirst { it.id == correlation.id }
            val updatedCorrelations = if (existingIndex >= 0) {
                currentData.correlations.toMutableList().apply {
                    set(existingIndex, correlation)
                }
            } else {
                currentData.correlations + correlation
            }

            currentData.copy(correlations = updatedCorrelations.takeLast(20))
        }
    }

    override suspend fun updateWeeklyReport(report: WeeklyInsightReport) {
        updateInsightsData { currentData ->
            currentData.copy(weeklyReport = report)
        }
    }

    override suspend fun clearExpiredInsights() {
        val now = kotlinx.datetime.Clock.System.now().toString()
        updateInsightsData { currentData ->
            currentData.copy(
                insights = currentData.insights.filter { insight ->
                    insight.expiresAt == null || insight.expiresAt > now
                }
            )
        }
    }

    override suspend fun clearAllInsights() {
        updateInsightsData { PatternInsightsData() }
    }
}
