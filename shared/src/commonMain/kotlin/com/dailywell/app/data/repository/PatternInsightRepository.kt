package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for AI Pattern Recognition & Insights
 */
interface PatternInsightRepository {

    /**
     * Get all insights for the user
     */
    fun getAllInsights(): Flow<List<PatternInsight>>

    /**
     * Get insights for a specific habit
     */
    fun getInsightsForHabit(habitId: String): Flow<List<PatternInsight>>

    /**
     * Get correlations discovered by AI
     */
    fun getCorrelations(): Flow<List<HabitCorrelation>>

    /**
     * Get success predictions
     */
    fun getPredictions(): Flow<List<SuccessPrediction>>

    /**
     * Get weekly insight report
     */
    fun getWeeklyReport(): Flow<WeeklyInsightReport?>

    /**
     * Run pattern analysis on completion history
     */
    suspend fun analyzePatterns(completionHistory: Map<String, List<CompletionRecord>>)

    /**
     * Add a new insight
     */
    suspend fun addInsight(insight: PatternInsight)

    /**
     * Dismiss an insight (user doesn't want to see it)
     */
    suspend fun dismissInsight(insightId: String)

    /**
     * Add a correlation
     */
    suspend fun addCorrelation(correlation: HabitCorrelation)

    /**
     * Update weekly report
     */
    suspend fun updateWeeklyReport(report: WeeklyInsightReport)

    /**
     * Clear expired insights
     */
    suspend fun clearExpiredInsights()

    /**
     * Clear all insights
     */
    suspend fun clearAllInsights()
}
