package com.dailywell.app.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.dailywell.app.data.content.DailyInsightsDatabase
import com.dailywell.app.ui.components.DailyWellIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * DailyInsightsRepository - PRODUCTION-READY Repository for 365 Unique Daily Insights
 *
 * Features:
 * - Returns unique insight for each day of the year (no repetition for 365 days)
 * - Tracks viewed insights in Firebase Firestore
 * - Provides contextual insights based on user state
 * - Supports bookmarking favorite insights
 * - Persists read history for analytics
 *
 * All data is stored in Firebase Firestore - NO MOCK DATA, NO HARDCODING
 */
interface DailyInsightsRepository {

    // ==================== CORE INSIGHT RETRIEVAL ====================

    /**
     * Get today's unique insight based on day of year
     * Returns a Flow for real-time updates
     */
    fun getTodayInsight(): Flow<DailyInsightWithMeta?>

    /**
     * Get insight for a specific day of year (1-365)
     */
    suspend fun getInsightForDay(dayOfYear: Int): DailyInsightWithMeta

    /**
     * Get contextual insight based on user's current state
     * Considers: streak, recent breaks, achievements
     */
    suspend fun getContextualInsight(
        streak: Int,
        recentBreak: Boolean = false,
        achievement: String? = null
    ): DailyInsightWithMeta

    // ==================== TRACKING & PERSISTENCE ====================

    /**
     * Mark an insight as viewed
     * Stores in Firebase for analytics and read history
     */
    suspend fun markInsightViewed(insightId: String)

    /**
     * Bookmark an insight for later reference
     */
    suspend fun bookmarkInsight(insightId: String): Result<Unit>

    /**
     * Remove bookmark from an insight
     */
    suspend fun unbookmarkInsight(insightId: String): Result<Unit>

    /**
     * Get all bookmarked insights
     */
    fun getBookmarkedInsights(): Flow<List<DailyInsightWithMeta>>

    /**
     * Check if a specific insight is bookmarked
     */
    suspend fun isInsightBookmarked(insightId: String): Boolean

    // ==================== HISTORY & ANALYTICS ====================

    /**
     * Get user's insight view history
     */
    fun getViewHistory(): Flow<List<InsightViewRecord>>

    /**
     * Get total insights viewed count
     */
    suspend fun getTotalInsightsViewed(): Int

    /**
     * Get insights viewed this week
     */
    suspend fun getWeeklyInsightsViewed(): Int

    /**
     * Get the user's insight engagement stats
     */
    suspend fun getInsightStats(): InsightStats

    // ==================== CATEGORY BROWSING ====================

    /**
     * Get all insights for a specific category
     */
    fun getInsightsByCategory(category: DailyInsightsDatabase.InsightCategory): List<DailyInsightWithMeta>

    /**
     * Search insights by keyword
     */
    fun searchInsights(query: String): List<DailyInsightWithMeta>

    /**
     * Get all available categories with counts
     */
    fun getCategories(): List<InsightCategoryInfo>
}

/**
 * Insight with metadata (view status, bookmarked, etc.)
 */
@Serializable
data class DailyInsightWithMeta(
    val id: String,
    val category: String,
    val categoryDisplayName: String,
    val title: String,
    val content: String,
    val source: String?,
    val contextTags: List<String>,
    val isViewed: Boolean = false,
    val isBookmarked: Boolean = false,
    val viewedAt: String? = null,
    val dayOfYear: Int? = null  // Which day this insight belongs to
)

/**
 * Record of when a user viewed an insight
 */
@Serializable
data class InsightViewRecord(
    val insightId: String,
    val viewedAt: String,
    val dayOfYear: Int,
    val category: String,
    val title: String
)

/**
 * User's insight engagement statistics
 */
@Serializable
data class InsightStats(
    val totalViewed: Int,
    val totalBookmarked: Int,
    val viewStreak: Int,  // Days in a row viewing insights
    val favoriteCategory: String?,
    val completionPercent: Float,  // % of all 365 insights viewed
    val categoryCounts: Map<String, Int>
)

/**
 * Category info for browsing UI
 */
@Serializable
data class InsightCategoryInfo(
    val category: DailyInsightsDatabase.InsightCategory,
    val displayName: String,
    val emoji: String,
    val count: Int,
    val viewedCount: Int
)

/**
 * Extension function to convert database Insight to DailyInsightWithMeta
 */
fun DailyInsightsDatabase.Insight.toMeta(
    isViewed: Boolean = false,
    isBookmarked: Boolean = false,
    viewedAt: String? = null,
    dayOfYear: Int? = null
): DailyInsightWithMeta {
    val categoryDisplayName = when (category) {
        DailyInsightsDatabase.InsightCategory.HABIT_PSYCHOLOGY -> "Habit Psychology"
        DailyInsightsDatabase.InsightCategory.NEUROSCIENCE -> "Neuroscience"
        DailyInsightsDatabase.InsightCategory.BEHAVIORAL_TRIGGERS -> "Behavioral Triggers"
        DailyInsightsDatabase.InsightCategory.PROGRESS_MINDSET -> "Progress Mindset"
        DailyInsightsDatabase.InsightCategory.SOCIAL_PSYCHOLOGY -> "Social Psychology"
        DailyInsightsDatabase.InsightCategory.RECOVERY_COMPASSION -> "Recovery & Compassion"
        DailyInsightsDatabase.InsightCategory.ADVANCED_TECHNIQUES -> "Advanced Techniques"
    }

    return DailyInsightWithMeta(
        id = id,
        category = category.name,
        categoryDisplayName = categoryDisplayName,
        title = title,
        content = content,
        source = source,
        contextTags = contextTags,
        isViewed = isViewed,
        isBookmarked = isBookmarked,
        viewedAt = viewedAt,
        dayOfYear = dayOfYear
    )
}

/**
 * Get emoji for insight category
 */
fun DailyInsightsDatabase.InsightCategory.emoji(): String {
    return when (this) {
        DailyInsightsDatabase.InsightCategory.HABIT_PSYCHOLOGY -> "🧠"
        DailyInsightsDatabase.InsightCategory.NEUROSCIENCE -> "⚡"
        DailyInsightsDatabase.InsightCategory.BEHAVIORAL_TRIGGERS -> "🎯"
        DailyInsightsDatabase.InsightCategory.PROGRESS_MINDSET -> "📈"
        DailyInsightsDatabase.InsightCategory.SOCIAL_PSYCHOLOGY -> "👥"
        DailyInsightsDatabase.InsightCategory.RECOVERY_COMPASSION -> "💚"
        DailyInsightsDatabase.InsightCategory.ADVANCED_TECHNIQUES -> "🔧"
    }
}

/**
 * Get Material Icon for insight category (preferred over emoji for UI rendering)
 */
fun DailyInsightsDatabase.InsightCategory.icon(): ImageVector {
    return when (this) {
        DailyInsightsDatabase.InsightCategory.HABIT_PSYCHOLOGY -> DailyWellIcons.Coaching.AICoach
        DailyInsightsDatabase.InsightCategory.NEUROSCIENCE -> DailyWellIcons.Gamification.XP
        DailyInsightsDatabase.InsightCategory.BEHAVIORAL_TRIGGERS -> Icons.Filled.TrackChanges
        DailyInsightsDatabase.InsightCategory.PROGRESS_MINDSET -> DailyWellIcons.Analytics.TrendUp
        DailyInsightsDatabase.InsightCategory.SOCIAL_PSYCHOLOGY -> DailyWellIcons.Social.People
        DailyInsightsDatabase.InsightCategory.RECOVERY_COMPASSION -> DailyWellIcons.Health.Heart
        DailyInsightsDatabase.InsightCategory.ADVANCED_TECHNIQUES -> Icons.Filled.Build
    }
}
