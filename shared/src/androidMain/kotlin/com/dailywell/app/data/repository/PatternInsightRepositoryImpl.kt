package com.dailywell.app.data.repository

import com.dailywell.app.api.ClaudeApiClient
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

class PatternInsightRepositoryImpl(
    private val dataStoreManager: DataStoreManager,
    private val claudeApiClient: ClaudeApiClient? = null  // Optional for backward compatibility
) : PatternInsightRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val INSIGHTS_KEY = "pattern_insights_data"
        private const val AI_INSIGHTS_CACHE_KEY = "ai_insights_last_refresh"
        private const val AI_CACHE_DURATION_HOURS = 24  // Refresh AI insights every 24 hours
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
        // Step 1: Always run rule-based analysis (free, fast)
        val ruleBasedInsights = PatternAnalyzer.analyzePatterns(completionHistory)

        // Step 2: Check if we should refresh AI insights (cached for 24h)
        val aiInsights = if (shouldRefreshAIInsights() && claudeApiClient != null) {
            generateAIInsights(completionHistory)
        } else {
            emptyList()
        }

        updateInsightsData { currentData ->
            // Merge new insights with existing, avoiding duplicates
            val existingIds = currentData.insights.map { it.id }.toSet()
            val allNewInsights = ruleBasedInsights + aiInsights
            val mergedInsights = currentData.insights + allNewInsights.filter { it.id !in existingIds }

            currentData.copy(
                insights = mergedInsights.takeLast(50), // Keep last 50 insights
                lastAnalyzedAt = Clock.System.now().toString()
            )
        }
    }

    /**
     * Check if AI insights should be refreshed (24h cache)
     */
    private suspend fun shouldRefreshAIInsights(): Boolean {
        val lastRefresh = dataStoreManager.getString(AI_INSIGHTS_CACHE_KEY).first()
        if (lastRefresh == null) return true

        return try {
            val lastRefreshTime = kotlinx.datetime.Instant.parse(lastRefresh)
            val now = Clock.System.now()
            val hoursSinceRefresh = (now - lastRefreshTime).inWholeHours
            hoursSinceRefresh >= AI_CACHE_DURATION_HOURS
        } catch (e: Exception) {
            true // Refresh if parsing fails
        }
    }

    /**
     * Generate AI-powered insights using Claude
     */
    private suspend fun generateAIInsights(
        completionHistory: Map<String, List<CompletionRecord>>
    ): List<PatternInsight> {
        if (claudeApiClient == null) return emptyList()

        return try {
            val result = claudeApiClient.generatePatternInsights(completionHistory)
            if (result.isSuccess) {
                // Update cache timestamp
                dataStoreManager.putString(AI_INSIGHTS_CACHE_KEY, Clock.System.now().toString())

                // Convert AI response to PatternInsight objects
                result.getOrNull()?.mapIndexed { index, aiInsight ->
                    PatternInsight(
                        id = "ai_${Clock.System.now().toEpochMilliseconds()}_$index",
                        type = parseInsightType(aiInsight.type),
                        title = aiInsight.title,
                        description = aiInsight.description,
                        emoji = aiInsight.emoji,
                        significance = aiInsight.significance.coerceIn(0f, 1f),
                        actionable = aiInsight.recommendation != null,
                        recommendation = aiInsight.recommendation,
                        relatedHabits = aiInsight.relatedHabits,
                        createdAt = Clock.System.now().toString(),
                        expiresAt = null,  // AI insights don't expire
                        isDismissed = false
                    )
                } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            // Silently fail - rule-based insights still work
            emptyList()
        }
    }

    /**
     * Parse insight type string to enum
     */
    private fun parseInsightType(typeString: String): InsightType {
        return try {
            InsightType.valueOf(typeString.uppercase())
        } catch (e: Exception) {
            InsightType.SUCCESS_FACTOR // Default fallback
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
