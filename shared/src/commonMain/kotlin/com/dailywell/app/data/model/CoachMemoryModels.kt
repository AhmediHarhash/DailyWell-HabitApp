package com.dailywell.app.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Feature #8: AI Coach Memory
 * Stores conversation history, user preferences, and context for personalized coaching
 */

@Serializable
data class ConversationMemory(
    val id: String,
    val userId: String,
    val conversationSummary: String, // AI-generated summary of the conversation
    val keyTopics: List<String>, // e.g., ["stress", "sleep", "work-life balance"]
    val userGoals: List<String>, // Goals mentioned by the user
    val userChallenges: List<String>, // Challenges discussed
    val emotionalTone: EmotionalTone, // Overall emotional tone of conversation
    val timestamp: Instant,
    val messageCount: Int // Number of messages in this conversation
)

@Serializable
enum class EmotionalTone {
    POSITIVE,      // User was upbeat, optimistic
    NEUTRAL,       // Casual, informative conversation
    STRUGGLING,    // User expressed difficulties
    FRUSTRATED,    // User seemed frustrated or upset
    MOTIVATED,     // User was highly motivated
    REFLECTIVE     // User was thoughtful, introspective
}

@Serializable
data class UserPreferences(
    val userId: String,
    val preferredCoachId: String? = null, // User's favorite coach persona
    val communicationStyle: CommunicationStyle = CommunicationStyle.BALANCED,
    val topicsOfInterest: List<String> = emptyList(), // Topics user frequently discusses
    val avoidTopics: List<String> = emptyList(), // Topics user doesn't want to discuss
    val preferredTime: String? = null, // Best time for coaching (e.g., "morning", "evening")
    val motivationalApproach: MotivationalApproach = MotivationalApproach.SUPPORTIVE,
    val lastUpdated: Instant
)

@Serializable
enum class CommunicationStyle {
    CONCISE,       // Brief, to-the-point
    DETAILED,      // Comprehensive, thorough
    BALANCED,      // Mix of both
    CONVERSATIONAL // Friendly, chatty
}

@Serializable
enum class MotivationalApproach {
    SUPPORTIVE,    // Gentle encouragement
    CHALLENGING,   // Push harder, set ambitious goals
    DATA_DRIVEN,   // Focus on metrics and progress
    STORY_BASED    // Use stories and examples
}

@Serializable
data class UserContext(
    val userId: String,
    val recentHabits: List<String>, // Habits user is currently tracking
    val currentStreak: Int,
    val totalCompletions: Int,
    val recentChallenges: List<String>, // Challenges from last 7 days
    val recentMoods: List<MoodLevel>, // Moods from last 7 days
    val lifeSituation: String? = null, // e.g., "New parent", "Studying for exams", "Starting new job"
    val lastConversationDate: Instant? = null,
    val conversationCount: Int = 0
)

@Serializable
data class ConversationInsight(
    val id: String,
    val userId: String,
    val insight: String, // AI-discovered pattern or insight
    val category: InsightCategory,
    val confidence: Double, // 0.0 to 1.0
    val basedOnConversations: List<String>, // IDs of conversations this insight came from
    val discoveredAt: Instant,
    val isActionable: Boolean // Can we suggest an action based on this?
)

@Serializable
enum class InsightCategory {
    HABIT_PATTERN,        // Discovered habit pattern
    BEHAVIORAL_TRIGGER,   // Identified trigger for habits
    MOTIVATION_SOURCE,    // What motivates the user
    OBSTACLE,            // Recurring obstacle
    SUCCESS_FACTOR,      // What helps user succeed
    PREFERENCE,          // User preference discovered
    GOAL_ALIGNMENT       // How goals align with habits
}

@Serializable
data class MemoryQuery(
    val userId: String,
    val currentTopic: String,
    val lookbackDays: Int = 30, // How far back to look
    val maxMemories: Int = 5 // Max number of memories to retrieve
)

@Serializable
data class MemoryContext(
    val recentConversations: List<ConversationMemory>,
    val userPreferences: UserPreferences,
    val userContext: UserContext,
    val relevantInsights: List<ConversationInsight>
) {
    /**
     * Generate a context prompt to inject into AI coaching conversation
     */
    fun toContextPrompt(): String = buildString {
        appendLine("=== USER CONTEXT ===")

        // User situation
        userContext.lifeSituation?.let {
            appendLine("Life Situation: $it")
        }

        // Habits and progress
        appendLine("Current Habits: ${userContext.recentHabits.joinToString(", ")}")
        appendLine("Current Streak: ${userContext.currentStreak} days")

        // Recent mood
        if (userContext.recentMoods.isNotEmpty()) {
            val avgMood = userContext.recentMoods.groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key?.name ?: "Mixed"
            appendLine("Recent Mood: $avgMood")
        }

        // Communication preferences
        appendLine("\n=== PREFERENCES ===")
        appendLine("Communication Style: ${userPreferences.communicationStyle.name.lowercase()}")
        appendLine("Motivational Approach: ${userPreferences.motivationalApproach.name.lowercase()}")

        if (userPreferences.topicsOfInterest.isNotEmpty()) {
            appendLine("Interested in: ${userPreferences.topicsOfInterest.joinToString(", ")}")
        }

        if (userPreferences.avoidTopics.isNotEmpty()) {
            appendLine("Avoid topics: ${userPreferences.avoidTopics.joinToString(", ")}")
        }

        // Recent conversation context
        if (recentConversations.isNotEmpty()) {
            appendLine("\n=== RECENT CONVERSATIONS ===")
            recentConversations.take(3).forEach { memory ->
                appendLine("- ${memory.conversationSummary}")
                if (memory.userChallenges.isNotEmpty()) {
                    appendLine("  Challenges: ${memory.userChallenges.joinToString(", ")}")
                }
            }
        }

        // Key insights
        if (relevantInsights.isNotEmpty()) {
            appendLine("\n=== INSIGHTS ===")
            relevantInsights.take(3).forEach { insight ->
                appendLine("- ${insight.insight}")
            }
        }

        appendLine("\nUse this context to provide personalized, relevant coaching.")
    }
}

// Preset conversation templates for common scenarios
object ConversationTemplates {

    fun getMorningCheckIn(userContext: UserContext): String = """
        Good morning! I see you're on day ${userContext.currentStreak} of your streak.
        ${if (userContext.currentStreak >= 7) "That's amazing consistency!" else "Keep building that momentum!"}

        How are you feeling today?
    """.trimIndent()

    fun getEncouragementAfterSetback(userContext: UserContext, previousStreak: Int): String = """
        I noticed your streak reset. Remember that ${if (previousStreak >= 7) "$previousStreak days" else "your progress"}
        isn't lost - it's proof you can do this. What made things difficult recently?
    """.trimIndent()

    fun getCelebration(achievement: String): String = """
        🎉 Congratulations on $achievement! This is a significant milestone.
        What do you think helped you get here?
    """.trimIndent()

    fun getPersonalizedReminder(habits: List<String>, preferredTime: String?): String = """
        ${if (preferredTime == "morning") "Good morning!" else "Hey there!"}
        Just a gentle reminder about your habits today: ${habits.joinToString(", ")}.

        Which one would you like to tackle first?
    """.trimIndent()
}
