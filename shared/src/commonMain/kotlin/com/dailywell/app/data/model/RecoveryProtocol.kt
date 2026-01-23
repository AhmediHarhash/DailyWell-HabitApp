package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Recovery Protocol for streak breaks
 * Psychology: How people recover from setbacks determines long-term success
 *
 * Key insight: "Never miss twice" is more effective than perfect streaks
 */
@Serializable
data class RecoveryState(
    val isInRecovery: Boolean = false,
    val streakBrokenAt: String? = null,           // When the streak broke
    val previousStreak: Int = 0,                   // How long the streak was
    val recoveryStartedAt: String? = null,
    val recoveryPhase: RecoveryPhase = RecoveryPhase.NONE,
    val reflectionAnswer: String? = null,          // User's reflection
    val selectedReason: StreakBreakReason? = null,
    val commitmentLevel: CommitmentLevel = CommitmentLevel.SAME,
    val recoveryCompletedAt: String? = null
)

@Serializable
enum class RecoveryPhase(val title: String, val description: String) {
    NONE("", ""),
    ACKNOWLEDGE(
        "Acknowledge",
        "First, let's understand what happened. No judgment."
    ),
    REFLECT(
        "Reflect",
        "What can we learn from this?"
    ),
    RECOMMIT(
        "Recommit",
        "Set your intention for moving forward."
    ),
    CELEBRATE(
        "Celebrate Recovery",
        "You're back on track. That takes strength."
    )
}

@Serializable
enum class StreakBreakReason(val label: String, val emoji: String, val normalizer: String) {
    BUSY_DAY(
        "Life got busy",
        "🏃",
        "Even the most organized people have chaotic days."
    ),
    FORGOT(
        "Simply forgot",
        "💭",
        "Our brains aren't designed for perfect consistency."
    ),
    SICK(
        "Wasn't feeling well",
        "🤒",
        "Your body needed rest - that's self-care too."
    ),
    TRAVELING(
        "Travel/schedule change",
        "✈️",
        "New environments disrupt everyone's routines."
    ),
    OVERWHELMED(
        "Felt overwhelmed",
        "😮‍💨",
        "Sometimes we need to pull back to move forward."
    ),
    LOW_ENERGY(
        "Low energy/motivation",
        "🔋",
        "Energy fluctuates - that's being human."
    ),
    SOCIAL(
        "Social situation",
        "👥",
        "Connection is important too."
    ),
    OTHER(
        "Something else",
        "💫",
        "Life is unpredictable. That's okay."
    )
}

@Serializable
enum class CommitmentLevel(val label: String, val description: String) {
    SMALLER(
        "Start smaller",
        "Scale back to rebuild momentum"
    ),
    SAME(
        "Same commitment",
        "Ready to continue where I left off"
    ),
    STRONGER(
        "Even stronger",
        "Use this as fuel to level up"
    )
}

/**
 * Recovery messages based on psychology research
 */
object RecoveryMessages {

    val acknowledgmentMessages = listOf(
        "A broken streak doesn't erase your progress.",
        "This is a moment, not a definition.",
        "The path to success includes setbacks.",
        "You noticed you missed it. That awareness matters."
    )

    val reflectionPrompts = listOf(
        "What was different about yesterday?",
        "What would have made it easier to complete?",
        "Is there a pattern to when you miss habits?",
        "What's one small adjustment that could help?"
    )

    val recommitmentMessages = listOf(
        "The magic isn't in never falling - it's in always getting back up.",
        "\"Never miss twice\" is more powerful than \"never miss.\"",
        "Today is a fresh start, not a failure recovery.",
        "Your consistency muscle just got a workout."
    )

    val celebrationMessages = listOf(
        "You came back. That's the whole point.",
        "Recovery completed. You're stronger for this.",
        "Most people don't return after a break. You did.",
        "This resilience is your real habit."
    )

    fun getAcknowledgmentMessage(): String = acknowledgmentMessages.random()
    fun getReflectionPrompt(): String = reflectionPrompts.random()
    fun getRecommitmentMessage(): String = recommitmentMessages.random()
    fun getCelebrationMessage(): String = celebrationMessages.random()
}

/**
 * Recovery statistics for insights
 */
@Serializable
data class RecoveryStats(
    val totalRecoveries: Int = 0,
    val fastestRecoveryDays: Int? = null,
    val averageRecoveryDays: Float = 0f,
    val longestStreakAfterRecovery: Int = 0,
    val recoveryHistory: List<RecoveryEvent> = emptyList()
)

@Serializable
data class RecoveryEvent(
    val brokenAt: String,
    val recoveredAt: String,
    val previousStreak: Int,
    val reason: StreakBreakReason,
    val daysToRecover: Int
)
