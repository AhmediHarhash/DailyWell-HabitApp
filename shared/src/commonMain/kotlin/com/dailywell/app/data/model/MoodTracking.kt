package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Mood Tracking Models
 *
 * FBI Psychology Hook: "Labeling emotions" - when users identify
 * and label their emotions, they feel understood and build connection
 * with the app. This also provides valuable data for correlations.
 */

@Serializable
enum class MoodLevel(val emoji: String, val label: String, val score: Int) {
    GREAT("😊", "Great", 5),
    GOOD("🙂", "Good", 4),
    OKAY("😐", "Okay", 3),
    LOW("😔", "Low", 2),
    STRUGGLING("😢", "Struggling", 1)
}

@Serializable
enum class EnergyLevel(val emoji: String, val label: String, val score: Int) {
    HIGH_ENERGY("⚡", "High Energy", 5),
    ENERGIZED("💪", "Energized", 4),
    NORMAL("🔋", "Normal", 3),
    TIRED("😴", "Tired", 2),
    EXHAUSTED("🪫", "Exhausted", 1)
}

@Serializable
enum class StressLevel(val emoji: String, val label: String, val score: Int) {
    RELAXED("😌", "Relaxed", 1),
    CALM("🧘", "Calm", 2),
    MODERATE("😤", "Moderate", 3),
    STRESSED("😰", "Stressed", 4),
    OVERWHELMED("🤯", "Overwhelmed", 5)
}

@Serializable
data class DailyMood(
    val date: String, // ISO format YYYY-MM-DD
    val mood: MoodLevel? = null,
    val energy: EnergyLevel? = null,
    val stress: StressLevel? = null,
    val note: String? = null,
    val timestamp: String? = null, // When mood was logged
    val factors: List<MoodFactor> = emptyList() // What affected mood
)

@Serializable
enum class MoodFactor(val emoji: String, val label: String) {
    // Positive factors
    GOOD_SLEEP("😴", "Good sleep"),
    EXERCISE("🏃", "Exercise"),
    SOCIAL("👥", "Social time"),
    NATURE("🌳", "Time in nature"),
    ACCOMPLISHMENT("✅", "Accomplished something"),
    RELAXATION("🧘", "Relaxation"),
    GOOD_FOOD("🥗", "Healthy eating"),

    // Negative factors
    POOR_SLEEP("😵", "Poor sleep"),
    WORK_STRESS("💼", "Work stress"),
    CONFLICT("😤", "Conflict"),
    HEALTH_ISSUE("🤒", "Health issue"),
    WEATHER("🌧️", "Weather"),
    SKIPPED_HABITS("❌", "Skipped habits"),
    SCREEN_TIME("📱", "Too much screen time")
}

@Serializable
data class MoodHistory(
    val entries: List<DailyMood> = emptyList(),
    val averageMood: Float = 0f,
    val averageEnergy: Float = 0f,
    val averageStress: Float = 0f,
    val moodTrend: MoodTrend = MoodTrend.STABLE
)

@Serializable
enum class MoodTrend {
    IMPROVING,
    STABLE,
    DECLINING
}

/**
 * Mood-Habit Correlations
 * Discover hidden connections between mood and habit completion
 */
@Serializable
data class MoodHabitCorrelation(
    val habitId: String,
    val habitName: String,
    val correlationType: MoodCorrelationType,
    val strength: Float, // 0.0 to 1.0
    val description: String,
    val sampleSize: Int // Days of data
)

@Serializable
enum class MoodCorrelationType {
    POSITIVE_MOOD_COMPLETION, // Completing habit correlates with better mood
    NEGATIVE_MOOD_COMPLETION, // Completing habit correlates with worse mood (rare)
    MOOD_AFFECTS_COMPLETION,  // Better mood leads to more completions
    COMPLETION_AFFECTS_MOOD   // Completion leads to better mood next day
}

/**
 * Mood insights generated from data
 */
@Serializable
data class MoodInsight(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val insightType: MoodInsightType,
    val actionSuggestion: String? = null,
    val relatedHabitId: String? = null,
    val confidence: Float = 0.8f // How confident we are in this insight
)

@Serializable
enum class MoodInsightType {
    PATTERN_DETECTED,      // "Your mood is better on days you exercise"
    STREAK_IMPACT,         // "Your 7-day streak improved your average mood by 15%"
    RISK_WARNING,          // "You tend to skip habits when stressed"
    CELEBRATION,           // "Your mood has improved 20% this month!"
    RECOMMENDATION         // "Based on your patterns, try meditating when stressed"
}

/**
 * Quick mood check prompts
 */
object MoodPrompts {

    val quickCheckQuestions = listOf(
        "How are you feeling right now?",
        "Quick check-in: How's your mood?",
        "Before we continue, how are you feeling?",
        "Let's check in - how's your day going?"
    )

    val followUpQuestions = mapOf(
        MoodLevel.GREAT to listOf(
            "That's wonderful! What's contributing to your great mood?",
            "Amazing! Keep that energy going!"
        ),
        MoodLevel.GOOD to listOf(
            "Nice! Anything specific making today good?",
            "Good to hear! Let's build on that."
        ),
        MoodLevel.OKAY to listOf(
            "Okay is okay. What would make today better?",
            "Fair enough. Remember, you're doing great just by showing up."
        ),
        MoodLevel.LOW to listOf(
            "I hear you. Would completing a habit help lift your mood?",
            "Tough days happen. What's one small win you could get today?"
        ),
        MoodLevel.STRUGGLING to listOf(
            "I'm sorry you're struggling. Remember, this too shall pass.",
            "Thank you for being honest. Be gentle with yourself today."
        )
    )

    val encouragementByMood = mapOf(
        MoodLevel.GREAT to "Channel that energy into crushing your habits today!",
        MoodLevel.GOOD to "Perfect state to build some momentum!",
        MoodLevel.OKAY to "Even on okay days, small wins add up.",
        MoodLevel.LOW to "One small habit can shift your whole day.",
        MoodLevel.STRUGGLING to "Just one habit today. That's enough. You're enough."
    )

    fun getRandomQuestion(): String = quickCheckQuestions.random()

    fun getFollowUp(mood: MoodLevel): String = followUpQuestions[mood]?.random() ?: ""

    fun getEncouragement(mood: MoodLevel): String = encouragementByMood[mood] ?: ""
}
