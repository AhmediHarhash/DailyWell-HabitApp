package com.dailywell.app.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.dailywell.app.data.content.ReflectionPromptsDatabase
import com.dailywell.app.data.content.ReflectionPromptsDatabase.ReflectionPrompt
import com.dailywell.app.data.content.ReflectionPromptsDatabase.ReflectionTheme
import com.dailywell.app.data.content.ReflectionPromptsDatabase.JourneyStage
import com.dailywell.app.ui.components.DailyWellIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * ReflectionPromptsRepository - Production-Ready Weekly Reflection System
 *
 * Features:
 * - 120 unique prompts across 6 themes
 * - Firebase-backed persistence for user responses
 * - Streak tracking for weekly reflections
 * - AI-powered response analysis
 * - Journey stage awareness
 *
 * Firestore Collections:
 * - users/{userId}/reflections - Weekly reflection entries
 * - users/{userId}/reflection_stats - Aggregated statistics
 * - users/{userId}/reflection_streaks - Streak tracking
 */

/**
 * User's reflection response with metadata
 */
data class ReflectionResponse(
    val id: String,
    val promptId: String,
    val theme: String,
    val question: String,
    val response: String,
    val weekNumber: Int,
    val year: Int,
    val createdAt: Instant,
    val wordCount: Int,
    val sentimentScore: Float? = null,  // -1 to 1, analyzed by AI
    val keyInsights: List<String> = emptyList()  // AI-extracted insights
)

/**
 * Weekly reflection entry containing all theme responses
 */
data class WeeklyReflectionEntry(
    val id: String,
    val weekNumber: Int,
    val year: Int,
    val responses: Map<String, ReflectionResponse>,  // theme -> response
    val overallMood: String? = null,  // User-selected mood
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val isComplete: Boolean = false,
    val completionPercent: Float = 0f
)

/**
 * Reflection statistics
 */
data class ReflectionStats(
    val totalReflections: Int,
    val totalResponses: Int,
    val totalWords: Int,
    val averageWordsPerResponse: Float,
    val reflectionStreak: Int,
    val longestStreak: Int,
    val favoriteTheme: String?,
    val themeCounts: Map<String, Int>,
    val weeklyTrend: List<Int>,  // Last 4 weeks completion %
    val lastReflectionDate: Instant?
)

/**
 * Prompt with user context for smart selection
 */
data class ContextualPrompt(
    val prompt: ReflectionPrompt,
    val relevanceScore: Float,
    val reason: String
)

/**
 * Weekly prompt set with all 6 themed prompts
 */
data class WeeklyPromptSet(
    val weekNumber: Int,
    val year: Int,
    val prompts: List<ReflectionPrompt>,
    val isCompleted: Boolean,
    val existingResponses: Map<String, String>  // promptId -> response
)

interface ReflectionPromptsRepository {

    // ==================== PROMPT RETRIEVAL ====================

    /**
     * Get the current week's prompt set (one from each of 6 themes)
     */
    fun getCurrentWeekPromptSet(): Flow<WeeklyPromptSet>

    /**
     * Get prompts for a specific week
     */
    suspend fun getPromptSetForWeek(weekNumber: Int, year: Int): WeeklyPromptSet

    /**
     * Get a contextual prompt based on user's journey stage and mood
     */
    suspend fun getContextualPrompt(
        journeyStage: JourneyStage,
        currentStreak: Int,
        recentBreak: Boolean,
        moodLow: Boolean
    ): ContextualPrompt

    /**
     * Get prompts by theme for browsing
     */
    fun getPromptsByTheme(theme: ReflectionTheme): List<ReflectionPrompt>

    /**
     * Search prompts by keyword
     */
    fun searchPrompts(query: String): List<ReflectionPrompt>

    // ==================== RESPONSE MANAGEMENT ====================

    /**
     * Save a reflection response
     */
    suspend fun saveResponse(
        promptId: String,
        response: String,
        weekNumber: Int,
        year: Int
    ): Result<ReflectionResponse>

    /**
     * Update an existing response
     */
    suspend fun updateResponse(
        responseId: String,
        newResponse: String
    ): Result<ReflectionResponse>

    /**
     * Get response for a specific prompt
     */
    suspend fun getResponse(promptId: String, weekNumber: Int, year: Int): ReflectionResponse?

    /**
     * Get all responses for a week
     */
    suspend fun getWeeklyResponses(weekNumber: Int, year: Int): List<ReflectionResponse>

    /**
     * Complete weekly reflection (marks as finished)
     */
    suspend fun completeWeeklyReflection(weekNumber: Int, year: Int): Result<Unit>

    // ==================== HISTORY & ANALYTICS ====================

    /**
     * Get reflection history
     */
    fun getReflectionHistory(): Flow<List<WeeklyReflectionEntry>>

    /**
     * Get reflection stats
     */
    suspend fun getReflectionStats(): ReflectionStats

    /**
     * Get reflection streak info
     */
    suspend fun getReflectionStreak(): Int

    /**
     * Check if current week has been completed
     */
    suspend fun isCurrentWeekComplete(): Boolean

    /**
     * Get responses by theme for insights
     */
    suspend fun getResponsesByTheme(theme: ReflectionTheme): List<ReflectionResponse>

    /**
     * Export all reflections (for backup/sharing)
     */
    suspend fun exportReflections(): String  // Returns JSON

    // ==================== AI FEATURES ====================

    /**
     * Analyze response sentiment (calls AI)
     */
    suspend fun analyzeResponseSentiment(response: String): Float

    /**
     * Extract key insights from response (calls AI)
     */
    suspend fun extractInsights(response: String): List<String>

    /**
     * Get personalized prompt recommendation
     */
    suspend fun getRecommendedPrompt(): ContextualPrompt

    // ==================== UTILITY ====================

    /**
     * Get total prompt count
     */
    fun getTotalPromptCount(): Int

    /**
     * Get theme display information
     */
    fun getThemeInfo(theme: ReflectionTheme): ThemeInfo
}

/**
 * Theme metadata for UI display
 */
data class ThemeInfo(
    val theme: ReflectionTheme,
    val displayName: String,
    val emoji: String,
    val icon: ImageVector,
    val description: String,
    val color: Long,  // Compose color value
    val promptCount: Int,
    val completedCount: Int
)

/**
 * Extension to get theme display info
 */
fun ReflectionTheme.toInfo(completedCount: Int = 0): ThemeInfo {
    return when (this) {
        ReflectionTheme.GRATITUDE -> ThemeInfo(
            theme = this,
            displayName = "Gratitude",
            emoji = "🙏",
            icon = Icons.Filled.Spa,
            description = "Appreciate the good in your journey",
            color = 0xFF4CAF50,
            promptCount = 20,
            completedCount = completedCount
        )
        ReflectionTheme.CHALLENGES -> ThemeInfo(
            theme = this,
            displayName = "Challenges",
            emoji = "💪",
            icon = Icons.Filled.FitnessCenter,
            description = "Learn from your obstacles",
            color = 0xFFFF9800,
            promptCount = 20,
            completedCount = completedCount
        )
        ReflectionTheme.PROGRESS -> ThemeInfo(
            theme = this,
            displayName = "Progress",
            emoji = "📈",
            icon = DailyWellIcons.Analytics.TrendUp,
            description = "Celebrate your wins",
            color = 0xFF2196F3,
            promptCount = 20,
            completedCount = completedCount
        )
        ReflectionTheme.FUTURE -> ThemeInfo(
            theme = this,
            displayName = "Future Planning",
            emoji = "🎯",
            icon = Icons.Filled.TrackChanges,
            description = "Set intentions for growth",
            color = 0xFF9C27B0,
            promptCount = 20,
            completedCount = completedCount
        )
        ReflectionTheme.SELF_DISCOVERY -> ThemeInfo(
            theme = this,
            displayName = "Self-Discovery",
            emoji = "🔍",
            icon = Icons.Filled.Search,
            description = "Understand yourself deeper",
            color = 0xFF00BCD4,
            promptCount = 20,
            completedCount = completedCount
        )
        ReflectionTheme.RELATIONSHIPS -> ThemeInfo(
            theme = this,
            displayName = "Relationships",
            emoji = "💕",
            icon = Icons.Filled.Favorite,
            description = "Connect with others",
            color = 0xFFE91E63,
            promptCount = 20,
            completedCount = completedCount
        )
    }
}
