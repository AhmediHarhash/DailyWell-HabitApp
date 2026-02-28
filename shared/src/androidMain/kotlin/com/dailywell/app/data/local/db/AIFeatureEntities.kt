package com.dailywell.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dailywell.app.data.model.AIModelUsed
import com.dailywell.app.data.model.AIPlanType

/**
 * Room Entities for AI Feature Persistence
 *
 * These entities persist the 5 advanced AI features to survive app restarts:
 * 1. A/B Test Hook - Routing analytics
 * 2. Insight Scheduler - Proactive retention insights
 * 3. Context Cache - Token-efficient context summaries
 * 4. Opus Scheduler - Pre-generated weekly reports
 * 5. User AI Settings - Language preference, etc.
 */

// ============================================================================
// 1. A/B TEST HOOK - Routing Event Persistence
// ============================================================================

@Entity(tableName = "ab_test_events")
data class ABTestEventEntity(
    @PrimaryKey
    val eventId: String,
    val timestamp: Long,
    val userId: String,
    val intent: String,  // RequestIntent enum name
    val requestedModel: String,  // AIModelUsed enum name
    val actualModel: String,  // AIModelUsed enum name
    val reason: String,
    val budgetMode: String,  // BudgetMode enum name
    val inputTokens: Int,
    val outputTokens: Int,
    val cost: Float,
    val responseTimeMs: Long,
    val userFeedback: String? = null,  // UserFeedback enum name or null
    val feedbackTimestamp: Long? = null
)

// ============================================================================
// 2. INSIGHT SCHEDULER - Scheduled Insight Persistence
// ============================================================================

@Entity(tableName = "scheduled_insights")
data class ScheduledInsightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uniqueKey: String,  // "{userId}_{milestone}" for upsert
    val userId: String,
    val milestone: String,  // InsightMilestone enum name
    val scheduledTimestamp: Long,
    val status: String,  // PENDING, GENERATED, DELIVERED
    val generatedContent: String? = null,
    val generatedTimestamp: Long? = null,
    val deliveredTimestamp: Long? = null
)

// ============================================================================
// 3. CONTEXT CACHE - Cached Context Persistence
// ============================================================================

@Entity(tableName = "context_cache")
data class ContextCacheEntity(
    @PrimaryKey
    val userId: String,
    val cachedTimestamp: Long,
    val expiresAt: Long,  // TTL: cachedTimestamp + 4 hours
    // Aggregated stats (JSON would be cleaner but keeping it simple)
    val avgCompletionRate: Float,
    val dominantMood: String?,  // MoodLevel enum name
    val sleepAvgHours: Float?,
    val sleepAvgQuality: Float?,
    val nutritionAvgScore: Float?,
    val workoutTotalMinutes: Int?,
    val streakDays: Int,
    val topHabits: String,  // Comma-separated habit names
    val missedHabits: String,  // Comma-separated habit names
    val condensedPrompt: String  // Pre-built prompt for injection
)

// ============================================================================
// 4. OPUS SCHEDULER - Scheduled Report Persistence
// ============================================================================

@Entity(tableName = "scheduled_reports")
data class ScheduledReportEntity(
    @PrimaryKey
    val reportId: String,
    val userId: String,
    val reportType: String,  // "WEEKLY_SUMMARY", "MONTHLY_REVIEW", etc.
    val scheduledTime: Long,  // 2 AM Sunday
    val status: String,  // SCHEDULED, GENERATING, READY, DELIVERED, FAILED, EXPIRED
    val generatedContent: String? = null,
    val tokensCost: Int? = null,
    val costUsd: Float? = null,
    val generatedAt: Long? = null,
    val deliveredAt: Long? = null,
    val expiresAt: Long? = null  // Reports expire after 7 days
)

// ============================================================================
// 5. USER AI SETTINGS - Language & Preferences
// ============================================================================

@Entity(tableName = "user_ai_settings")
data class UserAISettingsEntity(
    @PrimaryKey
    val userId: String,
    val preferredLanguage: String = "en",  // ISO language code
    val slmEnabled: Boolean = true,
    val lastLanguageDetected: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

// ============================================================================
// 6. DAILY CONTEXT SUMMARY - For Context Cache Building
// ============================================================================

@Entity(tableName = "daily_context_summaries")
data class DailyContextSummaryEntity(
    @PrimaryKey
    val id: String,  // "{userId}_{date}"
    val userId: String,
    val date: String,  // ISO date YYYY-MM-DD
    val habitCompletionRate: Float,
    val completedHabits: String,  // Comma-separated
    val missedHabits: String,  // Comma-separated
    val mood: String?,  // MoodLevel enum name
    val sleepHours: Float?,
    val sleepQuality: Float?,
    val nutritionScore: Float?,
    val workoutMinutes: Int?,
    val energyLevel: Int?,  // 1-10
    val notes: String?,
    val createdAt: Long = System.currentTimeMillis()
)

// ============================================================================
// 7. AI USAGE TRACKING - Monthly Usage Persistence
// ============================================================================

@Entity(tableName = "ai_usage_tracking")
data class AIUsageEntity(
    @PrimaryKey
    val usageId: String,  // "{userId}_{month}" e.g., "user123_2026-02"
    val userId: String,
    val month: String,  // "2026-02"
    val planType: String,  // AIPlanType enum name
    val tokensUsed: Int = 0,
    val messagesCount: Int = 0,
    val freeMessagesCount: Int = 0,  // Decision tree
    val slmMessagesCount: Int = 0,   // Qwen 0.5B
    val aiMessagesCount: Int = 0,    // Claude API
    val currentMonthCostUsd: Float = 0f,
    val resetDate: String,  // ISO date when usage resets
    val lastUpdated: Long = System.currentTimeMillis()
)

// ============================================================================
// 8. AI INTERACTION LOG - Individual Interaction Tracking
// ============================================================================

@Entity(tableName = "ai_interactions")
data class AIInteractionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val timestamp: Long,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val modelUsed: String,  // AIModelUsed enum name
    val responseCategory: String,
    val durationMs: Long? = null,
    val estimatedCostUsd: Float = 0f
)

