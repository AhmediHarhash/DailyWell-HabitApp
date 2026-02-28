package com.dailywell.app.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Room DAOs for AI Feature Persistence
 *
 * Provides database access for the 5 advanced AI features:
 * 1. A/B Test Hook
 * 2. Insight Scheduler
 * 3. Context Cache
 * 4. Opus Scheduler
 * 5. User AI Settings
 */

// ============================================================================
// 1. A/B TEST DAO
// ============================================================================

@Dao
interface ABTestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ABTestEventEntity)

    @Query("SELECT * FROM ab_test_events WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(userId: String, limit: Int = 100): List<ABTestEventEntity>

    @Query("SELECT * FROM ab_test_events WHERE eventId = :eventId")
    suspend fun getEvent(eventId: String): ABTestEventEntity?

    @Query("UPDATE ab_test_events SET userFeedback = :feedback, feedbackTimestamp = :timestamp WHERE eventId = :eventId")
    suspend fun recordFeedback(eventId: String, feedback: String, timestamp: Long)

    @Query("SELECT * FROM ab_test_events WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Long): List<ABTestEventEntity>

    @Query("SELECT intent, COUNT(*) as count, AVG(responseTimeMs) as avgTime, SUM(cost) as totalCost FROM ab_test_events GROUP BY intent")
    suspend fun getIntentAnalytics(): List<IntentAnalyticsResult>

    @Query("DELETE FROM ab_test_events WHERE timestamp < :before")
    suspend fun deleteOldEvents(before: Long)

    // Keep last 30 days of events
    @Query("DELETE FROM ab_test_events WHERE timestamp < :thirtyDaysAgo")
    suspend fun pruneOldEvents(thirtyDaysAgo: Long = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
}

data class IntentAnalyticsResult(
    val intent: String,
    val count: Int,
    val avgTime: Float,
    val totalCost: Float
)

// ============================================================================
// 2. INSIGHT SCHEDULER DAO
// ============================================================================

@Dao
interface InsightSchedulerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: ScheduledInsightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<ScheduledInsightEntity>)

    @Query("SELECT * FROM scheduled_insights WHERE userId = :userId ORDER BY scheduledTimestamp ASC")
    suspend fun getInsightsForUser(userId: String): List<ScheduledInsightEntity>

    @Query("SELECT * FROM scheduled_insights WHERE userId = :userId AND status = 'PENDING' AND scheduledTimestamp <= :now ORDER BY scheduledTimestamp ASC")
    suspend fun getDueInsights(userId: String, now: Long = System.currentTimeMillis()): List<ScheduledInsightEntity>

    @Query("SELECT * FROM scheduled_insights WHERE uniqueKey = :key")
    suspend fun getInsightByKey(key: String): ScheduledInsightEntity?

    @Query("UPDATE scheduled_insights SET status = 'GENERATED', generatedContent = :content, generatedTimestamp = :timestamp WHERE uniqueKey = :key")
    suspend fun markGenerated(key: String, content: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE scheduled_insights SET status = 'DELIVERED', deliveredTimestamp = :timestamp WHERE uniqueKey = :key")
    suspend fun markDelivered(key: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM scheduled_insights WHERE userId = :userId")
    suspend fun deleteUserInsights(userId: String)

    @Query("SELECT COUNT(*) FROM scheduled_insights WHERE userId = :userId AND milestone = :milestone AND status != 'PENDING'")
    suspend fun hasGeneratedMilestone(userId: String, milestone: String): Int
}

// ============================================================================
// 3. CONTEXT CACHE DAO
// ============================================================================

@Dao
interface ContextCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: ContextCacheEntity)

    @Query("SELECT * FROM context_cache WHERE userId = :userId AND expiresAt > :now")
    suspend fun getValidCache(userId: String, now: Long = System.currentTimeMillis()): ContextCacheEntity?

    @Query("DELETE FROM context_cache WHERE userId = :userId")
    suspend fun invalidateCache(userId: String)

    @Query("DELETE FROM context_cache WHERE expiresAt < :now")
    suspend fun pruneExpiredCaches(now: Long = System.currentTimeMillis())

    // Daily summaries for building cache
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailyContextSummaryEntity)

    @Query("SELECT * FROM daily_context_summaries WHERE userId = :userId ORDER BY date DESC LIMIT :days")
    suspend fun getRecentDailySummaries(userId: String, days: Int = 14): List<DailyContextSummaryEntity>

    @Query("DELETE FROM daily_context_summaries WHERE userId = :userId AND date < :before")
    suspend fun pruneOldSummaries(userId: String, before: String)
}

// ============================================================================
// 4. OPUS SCHEDULER DAO
// ============================================================================

@Dao
interface OpusSchedulerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ScheduledReportEntity)

    @Query("SELECT * FROM scheduled_reports WHERE userId = :userId ORDER BY scheduledTime DESC")
    suspend fun getReportsForUser(userId: String): List<ScheduledReportEntity>

    @Query("SELECT * FROM scheduled_reports WHERE reportId = :reportId")
    suspend fun getReport(reportId: String): ScheduledReportEntity?

    @Query("SELECT * FROM scheduled_reports WHERE status = 'SCHEDULED' AND scheduledTime <= :now")
    suspend fun getDueReports(now: Long = System.currentTimeMillis()): List<ScheduledReportEntity>

    @Query("SELECT * FROM scheduled_reports WHERE userId = :userId AND status = 'READY' AND (expiresAt IS NULL OR expiresAt > :now) ORDER BY generatedAt DESC LIMIT 1")
    suspend fun getReadyReportForUser(userId: String, now: Long = System.currentTimeMillis()): ScheduledReportEntity?

    @Query("UPDATE scheduled_reports SET status = 'GENERATING' WHERE reportId = :reportId")
    suspend fun markGenerating(reportId: String)

    @Query("UPDATE scheduled_reports SET status = 'READY', generatedContent = :content, tokensCost = :tokens, costUsd = :cost, generatedAt = :timestamp, expiresAt = :expires WHERE reportId = :reportId")
    suspend fun storeGeneratedReport(
        reportId: String,
        content: String,
        tokens: Int,
        cost: Float,
        timestamp: Long = System.currentTimeMillis(),
        expires: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000  // 7 days
    )

    @Query("UPDATE scheduled_reports SET status = 'DELIVERED', deliveredAt = :timestamp WHERE reportId = :reportId")
    suspend fun markDelivered(reportId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE scheduled_reports SET status = 'FAILED' WHERE reportId = :reportId")
    suspend fun markFailed(reportId: String)

    @Query("UPDATE scheduled_reports SET status = 'EXPIRED' WHERE expiresAt < :now AND status = 'READY'")
    suspend fun expireOldReports(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM scheduled_reports WHERE status IN ('DELIVERED', 'EXPIRED', 'FAILED') AND scheduledTime < :before")
    suspend fun pruneOldReports(before: Long)
}

// ============================================================================
// 5. USER AI SETTINGS DAO
// ============================================================================

@Dao
interface UserAISettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: UserAISettingsEntity)

    @Query("SELECT * FROM user_ai_settings WHERE userId = :userId")
    suspend fun getSettings(userId: String): UserAISettingsEntity?

    @Query("SELECT * FROM user_ai_settings WHERE userId = :userId")
    fun getSettingsFlow(userId: String): Flow<UserAISettingsEntity?>

    @Query("UPDATE user_ai_settings SET preferredLanguage = :language, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateLanguage(userId: String, language: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_ai_settings SET lastLanguageDetected = :language, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateDetectedLanguage(userId: String, language: String, timestamp: Long = System.currentTimeMillis())
}

// ============================================================================
// 6. AI USAGE TRACKING DAO
// ============================================================================

@Dao
interface AIUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: AIUsageEntity)

    @Query("SELECT * FROM ai_usage_tracking WHERE usageId = :usageId")
    suspend fun getUsage(usageId: String): AIUsageEntity?

    @Query("SELECT * FROM ai_usage_tracking WHERE userId = :userId ORDER BY month DESC LIMIT 1")
    suspend fun getCurrentUsage(userId: String): AIUsageEntity?

    @Query("SELECT * FROM ai_usage_tracking WHERE userId = :userId ORDER BY month DESC LIMIT 1")
    fun getCurrentUsageFlow(userId: String): Flow<AIUsageEntity?>

    @Query("UPDATE ai_usage_tracking SET tokensUsed = tokensUsed + :tokens, messagesCount = messagesCount + 1, currentMonthCostUsd = currentMonthCostUsd + :cost, lastUpdated = :timestamp WHERE usageId = :usageId")
    suspend fun incrementUsage(usageId: String, tokens: Int, cost: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE ai_usage_tracking SET freeMessagesCount = freeMessagesCount + 1, messagesCount = messagesCount + 1, lastUpdated = :timestamp WHERE usageId = :usageId")
    suspend fun incrementFreeMessage(usageId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE ai_usage_tracking SET slmMessagesCount = slmMessagesCount + 1, messagesCount = messagesCount + 1, lastUpdated = :timestamp WHERE usageId = :usageId")
    suspend fun incrementSLMMessage(usageId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE ai_usage_tracking SET aiMessagesCount = aiMessagesCount + 1, tokensUsed = tokensUsed + :tokens, currentMonthCostUsd = currentMonthCostUsd + :cost, messagesCount = messagesCount + 1, lastUpdated = :timestamp WHERE usageId = :usageId")
    suspend fun incrementAIMessage(usageId: String, tokens: Int, cost: Float, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM ai_usage_tracking WHERE userId = :userId ORDER BY month DESC")
    suspend fun getUsageHistory(userId: String): List<AIUsageEntity>
}

// ============================================================================
// 7. AI INTERACTION LOG DAO
// ============================================================================

@Dao
interface AIInteractionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: AIInteractionEntity)

    @Query("SELECT * FROM ai_interactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentInteractions(userId: String, limit: Int = 100): List<AIInteractionEntity>

    @Query("SELECT * FROM ai_interactions WHERE userId = :userId AND timestamp >= :start AND timestamp < :end")
    suspend fun getInteractionsInRange(userId: String, start: Long, end: Long): List<AIInteractionEntity>

    @Query("SELECT modelUsed, COUNT(*) as count, SUM(totalTokens) as tokens, SUM(estimatedCostUsd) as cost FROM ai_interactions WHERE userId = :userId GROUP BY modelUsed")
    suspend fun getModelUsageStats(userId: String): List<ModelUsageStats>

    @Query("DELETE FROM ai_interactions WHERE timestamp < :before")
    suspend fun pruneOldInteractions(before: Long)
}

data class ModelUsageStats(
    val modelUsed: String,
    val count: Int,
    val tokens: Int,
    val cost: Float
)
