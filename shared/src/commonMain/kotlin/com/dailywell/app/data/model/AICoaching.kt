package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Advanced AI Coaching Models
 * Personalized coaching based on behavioral psychology and user patterns
 */

@Serializable
data class AICoachingSession(
    val id: String,
    val type: CoachingSessionType,
    val title: String,
    val description: String,
    val messages: List<CoachingMessage> = emptyList(),
    val status: SessionStatus = SessionStatus.IN_PROGRESS,
    val startedAt: String,
    val completedAt: String? = null,
    val insights: List<String> = emptyList(),
    val actionItems: List<CoachingActionItem> = emptyList()
)

@Serializable
enum class CoachingSessionType(val displayName: String, val emoji: String) {
    DAILY_CHECKIN("Daily Check-in", "☀️"),
    WEEKLY_REVIEW("Weekly Review", "📊"),
    HABIT_COACHING("Habit Coaching", "🎯"),
    MOTIVATION_BOOST("Motivation Boost", "💪"),
    OBSTACLE_SOLVING("Obstacle Solving", "🧩"),
    CELEBRATION("Celebration", "🎉"),
    RECOVERY_SUPPORT("Recovery Support", "🌱")
}

@Serializable
enum class SessionStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED
}

@Serializable
data class CoachingMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: String,
    val suggestions: List<String> = emptyList(),
    val actionButtons: List<CoachingAction> = emptyList()
)

@Serializable
enum class MessageRole {
    COACH,
    USER,
    SYSTEM
}

@Serializable
data class CoachingAction(
    val id: String,
    val label: String,
    val emoji: String,
    val actionType: CoachingActionType
)

@Serializable
enum class CoachingActionType {
    QUICK_REPLY,
    SET_INTENTION,
    LOG_HABIT,
    ADJUST_GOAL,
    SCHEDULE_REMINDER,
    VIEW_INSIGHTS,
    SHARE_PROGRESS
}

@Serializable
data class CoachingActionItem(
    val id: String,
    val title: String,
    val description: String,
    val habitId: String? = null,
    val dueDate: String? = null,
    val isCompleted: Boolean = false,
    val priority: ActionPriority = ActionPriority.MEDIUM
)

@Serializable
enum class ActionPriority {
    HIGH, MEDIUM, LOW
}

/**
 * AI Coach persona and settings
 */
@Serializable
data class CoachPersona(
    val id: String,
    val name: String,
    val avatar: String,
    val style: CoachingStyle,
    val description: String,
    val strengthAreas: List<String>
)

@Serializable
enum class CoachingStyle(val displayName: String, val description: String) {
    ENCOURAGING("Encouraging", "Warm, supportive, celebrates every win"),
    DIRECT("Direct", "Straightforward, action-focused, no-nonsense"),
    ANALYTICAL("Analytical", "Data-driven, pattern-focused, insightful"),
    GENTLE("Gentle", "Compassionate, understanding, patient"),
    MOTIVATIONAL("Motivational", "Energetic, inspiring, pushes you forward")
}

/**
 * Daily coaching insights
 */
@Serializable
data class DailyCoachingInsight(
    val id: String,
    val date: String,
    val greeting: String,
    val mainMessage: String,
    val focusHabit: String? = null,
    val focusReason: String? = null,
    val motivationalQuote: String? = null,
    val suggestedActions: List<SuggestedAction> = emptyList(),
    val celebrationNote: String? = null
)

@Serializable
data class SuggestedAction(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val actionType: SuggestedActionType,
    val habitId: String? = null
)

@Serializable
enum class SuggestedActionType {
    COMPLETE_HABIT,
    STACK_HABITS,
    SET_INTENTION,
    REFLECT,
    REST,
    CELEBRATE
}

/**
 * Weekly coaching summary
 */
@Serializable
data class WeeklyCoachingSummary(
    val id: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val overallScore: Int,  // 0-100
    val completionRate: Float,
    val streakStatus: StreakSummary,
    val topWin: String,
    val growthArea: String,
    val patternDiscovered: String? = null,
    val nextWeekFocus: String,
    val personalizedAdvice: List<String>
)

@Serializable
data class StreakSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val streakTrend: TrendDirection
)

@Serializable
enum class TrendDirection {
    UP, DOWN, STABLE
}

/**
 * Coaching conversation templates
 */
object CoachingTemplates {

    val morningCheckins = listOf(
        "Good morning! ☀️ Ready to make today count?",
        "Rise and shine! 🌅 What's your first win going to be today?",
        "New day, fresh start! 🌱 Let's build on yesterday's momentum.",
        "Morning! ☕ I noticed you're on a {streak}-day streak. Let's keep it going!",
        "Hey there! 👋 Your consistency has been impressive lately."
    )

    val celebrationMessages = listOf(
        "🎉 Amazing! You just completed {habit}! That's {count} days in a row!",
        "✨ Look at you go! Another one checked off the list!",
        "💪 That's the spirit! {habit} - DONE!",
        "🌟 Excellent! You're building something special here.",
        "🔥 On fire! Keep this energy going!"
    )

    val motivationBoosts = listOf(
        "Remember: progress over perfection. Every small step counts! 🚶",
        "You've overcome harder things before. This is just another opportunity! 💪",
        "The best time to start was yesterday. The second best time is now! ⏰",
        "Your future self will thank you for the effort you put in today! 🙏",
        "Consistency beats intensity. Keep showing up! 🎯"
    )

    val recoveryMessages = listOf(
        "It's okay to have off days. What matters is that you're here now! 🌱",
        "Missing a day doesn't erase your progress. Let's focus on today! 💚",
        "Everyone stumbles. The champions are those who get back up! 🏆",
        "Be kind to yourself. Tomorrow is a fresh start! 🌅",
        "Rest is part of the journey. Ready to continue when you are! 🤗"
    )

    val weeklyReviewPrompts = listOf(
        "Let's look at your week! What are you most proud of?",
        "Time for our weekly review! 📊 Any surprises this week?",
        "Week in review! What worked well? What could be better?",
        "Reflection time! How did this week compare to last week?",
        "Weekly check-in! Let's celebrate your wins and plan for next week!"
    )

    fun getDailyGreeting(hour: Int, name: String, streak: Int): String {
        val timeGreeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        val streakNote = when {
            streak >= 30 -> "Your $streak-day streak is incredible! 🔥"
            streak >= 14 -> "Wow, $streak days strong! 💪"
            streak >= 7 -> "A whole week of consistency! 🌟"
            streak >= 3 -> "Nice $streak-day streak building! ✨"
            else -> "Let's build momentum today! 🚀"
        }

        return "$timeGreeting, $name! $streakNote"
    }

    fun getCompletionCelebration(habitName: String, completionCount: Int): String {
        return when {
            completionCount >= 100 -> "🏆 LEGENDARY! $habitName completed $completionCount times! You're unstoppable!"
            completionCount >= 50 -> "🌟 Amazing! $habitName - $completionCount completions! That's dedication!"
            completionCount >= 30 -> "🔥 $habitName done! That's $completionCount times now - you're on fire!"
            completionCount >= 14 -> "💪 Two weeks of $habitName! Keep this momentum going!"
            completionCount >= 7 -> "✨ One week of $habitName! The habit is taking root!"
            else -> "👏 $habitName - done! Every completion counts!"
        }
    }

    fun getRecoveryAdvice(missedDays: Int, habitName: String): String {
        return when {
            missedDays == 1 -> "No worries about missing yesterday's $habitName. Today's a fresh start! 🌱"
            missedDays <= 3 -> "Let's get back to $habitName. Start small - even 1 minute counts! 💚"
            missedDays <= 7 -> "It's been a few days since $habitName. Remember why you started? Let's reconnect with that motivation! 🎯"
            else -> "Life happens! Ready to restart $habitName when you are. We can adjust the goal if needed! 🤗"
        }
    }
}

/**
 * Pre-built coach personas
 */
object CoachPersonas {

    val supportiveSam = CoachPersona(
        id = "coach_sam",
        name = "Sam",
        avatar = "🌟",
        style = CoachingStyle.ENCOURAGING,
        description = "Your biggest cheerleader! Sam celebrates every win and helps you see progress even on tough days.",
        strengthAreas = listOf("Motivation", "Celebration", "Emotional Support")
    )

    val analyticalAlex = CoachPersona(
        id = "coach_alex",
        name = "Alex",
        avatar = "📊",
        style = CoachingStyle.ANALYTICAL,
        description = "Data-driven and insightful. Alex helps you understand patterns and optimize your habits.",
        strengthAreas = listOf("Pattern Analysis", "Optimization", "Strategic Planning")
    )

    val directDana = CoachPersona(
        id = "coach_dana",
        name = "Dana",
        avatar = "🎯",
        style = CoachingStyle.DIRECT,
        description = "Straight to the point! Dana focuses on action and results without fluff.",
        strengthAreas = listOf("Accountability", "Goal Setting", "Time Management")
    )

    val gentleGrace = CoachPersona(
        id = "coach_grace",
        name = "Grace",
        avatar = "🌸",
        style = CoachingStyle.GENTLE,
        description = "Compassionate and understanding. Grace creates a safe space for growth at your own pace.",
        strengthAreas = listOf("Self-Compassion", "Recovery", "Stress Management")
    )

    val motivationalMike = CoachPersona(
        id = "coach_mike",
        name = "Mike",
        avatar = "🔥",
        style = CoachingStyle.MOTIVATIONAL,
        description = "Brings the energy! Mike fires you up and helps you push through barriers.",
        strengthAreas = listOf("Energy Boost", "Challenge Pushing", "Peak Performance")
    )

    val allCoaches = listOf(supportiveSam, analyticalAlex, directDana, gentleGrace, motivationalMike)

    fun getCoach(style: CoachingStyle): CoachPersona {
        return allCoaches.find { it.style == style } ?: supportiveSam
    }
}
