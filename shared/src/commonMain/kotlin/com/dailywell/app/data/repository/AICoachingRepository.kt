package com.dailywell.app.data.repository

import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Advanced AI Coaching features
 * Includes cost control with hybrid Decision Tree + Claude AI system
 */
interface AICoachingRepository {
    // Daily insights
    fun getDailyInsight(): Flow<DailyCoachingInsight?>
    suspend fun generateDailyInsight()
    suspend fun markSuggestedActionDone(actionId: String)

    // Coaching sessions
    fun getActiveSessions(): Flow<List<AICoachingSession>>
    fun getSessionHistory(): Flow<List<AICoachingSession>>
    suspend fun startSession(type: CoachingSessionType): AICoachingSession
    suspend fun resumeSession(sessionId: String): AICoachingSession?
    suspend fun sendMessage(sessionId: String, message: String): CoachingMessage
    suspend fun selectQuickReply(sessionId: String, reply: String): CoachingMessage
    suspend fun completeSession(sessionId: String)
    suspend fun abandonSession(sessionId: String)
    suspend fun storePendingScanHandoff(handoff: ScanToCoachHandoff)
    suspend fun consumePendingScanHandoff(): ScanToCoachHandoff?
    suspend fun addScanContinuationMessage(sessionId: String, handoff: ScanToCoachHandoff): CoachingMessage

    // Action items
    fun getActionItems(): Flow<List<CoachingActionItem>>
    suspend fun completeActionItem(itemId: String)
    suspend fun dismissActionItem(itemId: String)

    // Weekly summary
    fun getWeeklySummary(): Flow<WeeklyCoachingSummary?>
    suspend fun generateWeeklySummary()

    // Contextual coaching
    suspend fun getMotivationBoost(): String
    suspend fun getRecoveryMessage(habitId: String): String
    suspend fun getCelebrationMessage(habitId: String, completionCount: Int): String

    // ==========================================
    // AI COST CONTROL - Token Tracking
    // ==========================================

    /**
     * Get current AI usage for the user
     * Used to display "AI credits remaining: X%" in UI
     */
    fun getAIUsage(): Flow<UserAIUsage>

    /**
     * Check if user can use AI (has credits remaining)
     * Returns detailed result with reason if blocked
     */
    suspend fun checkAIAvailability(): AIUsageCheckResult

    /**
     * Track token usage after an AI call
     * Called internally after Claude API calls
     */
    suspend fun trackTokenUsage(inputTokens: Int, outputTokens: Int, usedDecisionTree: Boolean)

    /**
     * Check if cloud AI spend can be added to the current monthly wallet.
     * Input is RAW API cost (multiplier is applied internally).
     */
    suspend fun canSpendCloudCost(rawCostUsd: Float): Boolean

    /**
     * Track cloud usage triggered outside chat flow (e.g., food scan, scheduled reports).
     */
    suspend fun trackExternalCloudUsage(
        inputTokens: Int,
        outputTokens: Int,
        model: AIModelUsed,
        category: String = "EXTERNAL"
    )

    /**
     * Update user's plan type (called when subscription changes)
     */
    suspend fun updatePlanType(planType: AIPlanType)

    /**
     * Get monthly usage report for analytics
     */
    suspend fun getMonthlyUsageReport(): MonthlyAIUsageReport?

    /**
     * Recent AI interaction events for usage details / observability.
     */
    suspend fun getRecentAIInteractions(limit: Int = 30): List<AIInteraction>

    /**
     * Aggregated routing stats by intent (model selection + cost/time profile).
     */
    suspend fun getRoutingIntentStats(limit: Int = 10): List<AIRoutingIntentStat>

    /**
     * Router optimization recommendations (admin/dev visibility).
     */
    suspend fun getRoutingRecommendations(): List<String>

    /**
     * Reset usage for new billing period
     * Called automatically at start of month or manually for testing
     */
    suspend fun resetMonthlyUsage()

    // ==========================================
    // AI COACH MEMORY - Feature #8
    // ==========================================

    /**
     * Store a conversation summary after each coaching session
     */
    suspend fun storeConversationMemory(memory: ConversationMemory)

    /**
     * Get recent conversation memories for context injection
     */
    suspend fun getConversationMemories(userId: String, limit: Int = 5): List<ConversationMemory>

    /**
     * Get user preferences for personalized coaching
     */
    suspend fun getUserPreferences(userId: String): UserPreferences

    /**
     * Update user preferences based on conversation patterns
     */
    suspend fun updateUserPreferences(preferences: UserPreferences)

    /**
     * Get user context for current conversation
     */
    suspend fun getUserContext(userId: String): UserContext

    /**
     * Store insights discovered during conversations
     */
    suspend fun storeInsight(insight: ConversationInsight)

    /**
     * Get relevant insights for current conversation topic
     */
    suspend fun getRelevantInsights(userId: String, topic: String, limit: Int = 3): List<ConversationInsight>

    /**
     * Build complete memory context for AI conversation
     */
    suspend fun buildMemoryContext(query: MemoryQuery): MemoryContext

    // ==========================================
    // PROACTIVE AI FEATURES (Insight Scheduler, Context Cache, etc.)
    // ==========================================

    /**
     * Schedule proactive insights for a new user (Day 3, 7, 14, 30, 90 milestones)
     * Call this after onboarding completes to set up retention-boosting insights
     */
    suspend fun scheduleNewUserInsights(userId: String)

    /**
     * Invalidate cached context after significant data changes
     * Call when user completes habits, logs moods, etc.
     */
    suspend fun invalidateContextCache(userId: String)
}
