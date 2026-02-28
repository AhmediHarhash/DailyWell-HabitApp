package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Proactive AI Notification System
 * AI-initiated notifications that reach out to users at optimal times
 */

@Serializable
data class ProactiveNotification(
    val id: String,
    val type: ProactiveNotificationType,
    val title: String,
    val message: String,
    val aiGenerated: Boolean = true,
    val scheduledAt: Long,
    val sentAt: Long? = null,
    val dismissed: Boolean = false,
    val tappedAt: Long? = null,
    val deepLink: String? = null,
    val priority: NotificationPriority = NotificationPriority.MEDIUM,
    val metadata: NotificationMetadata? = null
)

@Serializable
enum class ProactiveNotificationType(
    val displayName: String,
    val emoji: String,
    val defaultEnabled: Boolean = true
) {
    // Morning Motivation - Start the day right
    MORNING_MOTIVATION(
        displayName = "Morning Motivation",
        emoji = "☀️",
        defaultEnabled = true
    ),

    // Midday Check-in - Gentle reminder
    MIDDAY_CHECKIN(
        displayName = "Midday Check-in",
        emoji = "🌤️",
        defaultEnabled = true
    ),

    // Evening Reminder - End of day prompt
    EVENING_REMINDER(
        displayName = "Evening Reminder",
        emoji = "🌙",
        defaultEnabled = true
    ),

    // Streak at Risk - Urgent alert before day ends
    STREAK_AT_RISK(
        displayName = "Streak Protection",
        emoji = "⚠️",
        defaultEnabled = true
    ),

    // Comeback Message - After 2+ days of inactivity
    COMEBACK_NUDGE(
        displayName = "Comeback Encouragement",
        emoji = "💪",
        defaultEnabled = true
    ),

    // Milestone Approaching - "Just 1 more day for 7-day streak!"
    MILESTONE_APPROACHING(
        displayName = "Milestone Alerts",
        emoji = "🎯",
        defaultEnabled = true
    ),

    // Achievement Unlocked - Celebrate immediately
    ACHIEVEMENT_UNLOCKED(
        displayName = "Achievement Celebrations",
        emoji = "🏆",
        defaultEnabled = true
    ),

    // AI Insight - Pattern discovered
    AI_INSIGHT(
        displayName = "AI Insights",
        emoji = "🧠",
        defaultEnabled = true
    ),

    // Habit-Specific - Custom timing per habit
    HABIT_SPECIFIC(
        displayName = "Habit Reminders",
        emoji = "📋",
        defaultEnabled = true
    ),

    // Social Activity - Friend actions
    SOCIAL_ACTIVITY(
        displayName = "Social Updates",
        emoji = "👋",
        defaultEnabled = false
    ),

    // Weekly Summary Ready
    WEEKLY_SUMMARY(
        displayName = "Weekly Summary",
        emoji = "📊",
        defaultEnabled = true
    ),

    // Coach Wants to Chat - Proactive AI outreach
    COACH_OUTREACH(
        displayName = "Coach Messages",
        emoji = "💬",
        defaultEnabled = true
    )
}

// NotificationPriority is defined in EngagementHooks.kt - reusing that enum

@Serializable
data class NotificationMetadata(
    val habitId: String? = null,
    val habitName: String? = null,
    val currentStreak: Int? = null,
    val missedDays: Int? = null,
    val milestoneDays: Int? = null,
    val achievementId: String? = null,
    val friendName: String? = null,
    val completionRate: Float? = null,
    val coachPersona: String? = null,
    val insightType: String? = null
)

/**
 * User preferences for proactive notifications
 */
@Serializable
data class ProactiveNotificationPreferences(
    val enabled: Boolean = true,

    // Time windows
    val morningWindowStart: Int = 7,   // 7 AM
    val morningWindowEnd: Int = 9,     // 9 AM
    val middayWindowStart: Int = 12,   // 12 PM
    val middayWindowEnd: Int = 14,     // 2 PM
    val eveningWindowStart: Int = 18,  // 6 PM
    val eveningWindowEnd: Int = 21,    // 9 PM

    // Do Not Disturb
    val dndStart: Int = 22,            // 10 PM
    val dndEnd: Int = 7,               // 7 AM
    val weekendDndStart: Int = 23,     // 11 PM on weekends
    val weekendDndEnd: Int = 9,        // 9 AM on weekends

    // Frequency caps — behavioral engine uses weekly cap, not daily
    val maxNotificationsPerDay: Int = 2,           // Hard daily ceiling (safety net)
    val maxNotificationsPerWeek: Int = 4,          // Primary cap: 4/week default
    val minMinutesBetweenNotifications: Int = 120, // 2 hour minimum gap

    // Type-specific settings
    val enabledTypes: Map<ProactiveNotificationType, Boolean> = ProactiveNotificationType.entries
        .associateWith { it.defaultEnabled },

    // Smart timing
    val useSmartTiming: Boolean = true,    // Learn optimal times from user behavior
    val respectCalendar: Boolean = false,  // Future: calendar integration

    // Tone preference
    val tone: NotificationTone = NotificationTone.ENCOURAGING
)

@Serializable
enum class NotificationTone(val displayName: String, val description: String) {
    GENTLE("Gentle", "Soft, no-pressure messages"),
    ENCOURAGING("Encouraging", "Warm, motivational messages"),
    PLAYFUL("Playful", "Fun, lighthearted messages"),
    DIRECT("Direct", "Straightforward, action-focused"),
    SCIENCE_BACKED("Science-backed", "Facts and research-based motivation")
}

/**
 * Notification history for analytics and frequency capping
 */
@Serializable
data class NotificationHistory(
    val notificationId: String,
    val type: ProactiveNotificationType,
    val sentAt: Long,
    val opened: Boolean = false,
    val openedAt: Long? = null,
    val dismissed: Boolean = false,
    val convertedToAction: Boolean = false  // Did user complete a habit after?
)

/**
 * Daily notification state tracking
 */
@Serializable
data class DailyNotificationState(
    val date: String,  // yyyy-MM-dd
    val notificationsSent: Int = 0,
    val lastNotificationAt: Long? = null,
    val typesSentToday: List<ProactiveNotificationType> = emptyList(),
    val userEngagement: Float = 0f  // 0-1 based on opens/dismisses
)

/**
 * Weekly notification state — the behavioral engine tracks per-week not per-day.
 * Max 4 notifications per week by default. At-risk escalation allows 1/day.
 */
@Serializable
data class WeeklyNotificationState(
    val weekStart: String,  // ISO date of Monday
    val notificationsSent: Int = 0,
    val typesSentThisWeek: List<ProactiveNotificationType> = emptyList(),
    val lastSentType: ProactiveNotificationType? = null,
    val lastSentDate: String? = null,  // ISO date
    val consecutiveSilentDays: Int = 0,
    val weeklyOpenRate: Float = 0f,  // 0-1, rolling
    val atRiskEscalationActive: Boolean = false
)

/**
 * Notification Value Score — behavioral gate for every notification.
 * Score 0-100. Must reach 65+ to send. Prevents spam, ensures every
 * notification delivers real value.
 *
 * Components:
 * - Risk (0-30):    How close to losing streak? Higher = more valuable
 * - Readiness (0-25): Is user likely to act now? Time-of-day + open history
 * - Novelty (0-20):  Has user seen this type recently? Rotation bonus
 * - Impact (0-15):   Will this drive meaningful behavior?
 * - Trust (0-10):    User's relationship health (open rate, opt-in level)
 */
@Serializable
data class NotificationValueScore(
    val risk: Int = 0,       // 0-30
    val readiness: Int = 0,  // 0-25
    val novelty: Int = 0,    // 0-20
    val impact: Int = 0,     // 0-15
    val trust: Int = 0,      // 0-10
    val reason: String = ""
) {
    val total: Int get() = risk + readiness + novelty + impact + trust
    val passes: Boolean get() = total >= VALUE_SCORE_THRESHOLD

    companion object {
        const val VALUE_SCORE_THRESHOLD = 65
    }
}

/**
 * 5 Behavioral Notification Categories (2026 psychology-based).
 * Maps the 12 ProactiveNotificationTypes into 5 behavioral buckets.
 */
enum class NotificationBehaviorCategory(val displayName: String) {
    /** Celebrate wins — XP, achievements, milestones */
    CELEBRATION("Celebration"),
    /** Curiosity gap — AI insights, weekly summaries, patterns */
    CURIOSITY_HOOK("Curiosity Hook"),
    /** Loss aversion — streak protection, evening reminders */
    STREAK_SHIELD("Streak Shield"),
    /** Social proof — friend activity, coach outreach */
    SOCIAL_WHISPER("Social Whisper"),
    /** Intentional silence — no notification, builds anticipation */
    SILENT_DAY("Silent Day");

    companion object {
        fun fromType(type: ProactiveNotificationType): NotificationBehaviorCategory {
            return when (type) {
                ProactiveNotificationType.ACHIEVEMENT_UNLOCKED,
                ProactiveNotificationType.MILESTONE_APPROACHING -> CELEBRATION

                ProactiveNotificationType.AI_INSIGHT,
                ProactiveNotificationType.WEEKLY_SUMMARY,
                ProactiveNotificationType.MORNING_MOTIVATION,
                ProactiveNotificationType.MIDDAY_CHECKIN -> CURIOSITY_HOOK

                ProactiveNotificationType.STREAK_AT_RISK,
                ProactiveNotificationType.EVENING_REMINDER,
                ProactiveNotificationType.COMEBACK_NUDGE -> STREAK_SHIELD

                ProactiveNotificationType.SOCIAL_ACTIVITY,
                ProactiveNotificationType.COACH_OUTREACH -> SOCIAL_WHISPER

                ProactiveNotificationType.HABIT_SPECIFIC -> CURIOSITY_HOOK
            }
        }
    }
}

/**
 * Banned phrases — notifications containing these are rewritten or blocked.
 * Behavioral science: guilt/urgency language erodes trust and causes uninstalls.
 */
object NotificationGuardrails {
    val BANNED_PHRASES = listOf(
        "don't forget",
        "remember to",
        "you haven't",
        "you need to",
        "hurry",
        "last chance",
        "you're falling behind",
        "disappointed",
        "you failed",
        "you missed",
        "don't let",
        "shame",
        "must do",
        "time is running out",
        "you're losing"
    )

    /** Check if a message contains any banned phrase (case-insensitive) */
    fun containsBannedPhrase(message: String): Boolean {
        val lower = message.lowercase()
        return BANNED_PHRASES.any { lower.contains(it) }
    }

    /** Sanitize a message by removing banned phrases and softening language */
    fun sanitize(message: String): String {
        var result = message
        // Replace guilt phrases with empowering alternatives
        result = result.replace(Regex("(?i)don't forget"), "whenever you're ready")
        result = result.replace(Regex("(?i)remember to"), "you might enjoy")
        result = result.replace(Regex("(?i)you haven't"), "there's still time to")
        result = result.replace(Regex("(?i)you need to"), "you could")
        result = result.replace(Regex("(?i)hurry"), "at your pace")
        result = result.replace(Regex("(?i)last chance"), "great opportunity")
        result = result.replace(Regex("(?i)you're falling behind"), "you're on your own path")
        result = result.replace(Regex("(?i)you failed"), "tomorrow's a fresh start")
        result = result.replace(Regex("(?i)you missed"), "there's always another chance")
        result = result.replace(Regex("(?i)don't let"), "you can choose to")
        result = result.replace(Regex("(?i)time is running out"), "there's still time")
        result = result.replace(Regex("(?i)you're losing"), "you can protect")
        return result
    }
}

/**
 * Templates for AI to personalize
 */
object ProactiveNotificationTemplates {

    val morningMotivation = listOf(
        "Good morning! ☀️ Your {streak}-day streak is waiting. Ready to keep it going?",
        "Rise and shine! 🌅 {coach_name} here - let's make today count!",
        "New day, fresh start! 🌱 You've got {habits_remaining} habits to conquer.",
        "Morning! ☕ Yesterday you crushed it. Let's do it again!",
        "Hey early bird! 🐦 Your habits are ready when you are."
    )

    val middayCheckin = listOf(
        "Quick check-in! 🌤️ You've done {completed}/{total} habits. Keep the momentum!",
        "Halfway through the day! ⏰ {remaining} habits left - you've got this!",
        "Lunch break reminder: 🥗 Perfect time for a quick habit check-in!",
        "Afternoon boost! 💪 Your {weakest_habit} habit could use some love today.",
        "Hey! 👋 Just checking in. How's your day going?"
    )

    val eveningReminder = listOf(
        "Evening check-in! 🌙 Don't forget to log your day before bed.",
        "Day's almost done! 🌆 {remaining} habits waiting to be checked off.",
        "Wind-down time! 🧘 Have you reflected on today's wins?",
        "Before you relax... ✨ Quick habit check-in?",
        "Almost bedtime! 😴 Make sure your streak is safe for tomorrow."
    )

    val streakAtRisk = listOf(
        "⚠️ Your {streak}-day streak needs you! Just {remaining} habits to save it.",
        "Alert! 🚨 {hours_left} hours left to protect your {streak}-day streak!",
        "Don't let it slip! 💎 Your streak is on the line - check in now!",
        "This is {coach_name}: Your streak is at risk! Let's save it together.",
        "⏰ Time-sensitive: Your {streak}-day streak ends at midnight!"
    )

    val comebackNudge = listOf(
        "Hey, we miss you! 💚 It's been {days} days. Ready for a fresh start?",
        "Welcome back! 🌱 No judgment here - today is a perfect day to restart.",
        "{coach_name} here: Life happens! Whenever you're ready, I'm here.",
        "It's never too late! 🔄 Your habits are waiting patiently for you.",
        "Thinking of you! 💭 Even 1 habit today is a win. No pressure!"
    )

    val milestoneApproaching = listOf(
        "So close! 🎯 Just {days_remaining} more days to hit your {milestone}-day milestone!",
        "Almost there! 🏁 {milestone}-day streak is within reach!",
        "Countdown: {days_remaining} days to go! 🚀 You're doing amazing!",
        "Can you feel it? 🌟 {milestone} days is RIGHT THERE!",
        "{coach_name} is excited: Your {milestone}-day milestone is almost here!"
    )

    val achievementUnlocked = listOf(
        "🏆 Achievement Unlocked: {achievement_name}! Come celebrate!",
        "You did it! 🎉 New badge earned: {achievement_name}!",
        "MILESTONE! 🌟 {achievement_name} is now yours!",
        "Incredible! 🔥 You just unlocked {achievement_name}!",
        "{coach_name} is cheering: 🎊 {achievement_name} achieved!"
    )

    val aiInsight = listOf(
        "🧠 Pattern discovered: {insight_preview}. Tap to learn more!",
        "Interesting! 📊 Your data revealed something about {habit_name}.",
        "AI Update: 💡 I noticed {insight_preview}",
        "New insight ready! 🔍 Your {habit_name} patterns are fascinating.",
        "{coach_name} found something: {insight_preview}"
    )

    val coachOutreach = listOf(
        "💬 {coach_name} wants to chat! Got a minute?",
        "Hey! 👋 I was thinking about your progress. Can we talk?",
        "{coach_name} here: Something on my mind about your {habit_name} habit.",
        "Quick thought from {coach_name} 💭 - open when you're ready!",
        "Your coach is here! 🤗 Just checking in on you."
    )

    val weeklySummary = listOf(
        "📊 Your weekly summary is ready! See how you did.",
        "Week in review! 📈 Tap to see your highlights.",
        "7 days of progress summarized! 🗓️ Check it out.",
        "{coach_name} prepared your weekly report! 📋",
        "Your week at a glance 👀 - insights waiting inside!"
    )

    /**
     * Get a random template for the given type
     */
    fun getTemplate(type: ProactiveNotificationType): String {
        return when (type) {
            ProactiveNotificationType.MORNING_MOTIVATION -> morningMotivation.random()
            ProactiveNotificationType.MIDDAY_CHECKIN -> middayCheckin.random()
            ProactiveNotificationType.EVENING_REMINDER -> eveningReminder.random()
            ProactiveNotificationType.STREAK_AT_RISK -> streakAtRisk.random()
            ProactiveNotificationType.COMEBACK_NUDGE -> comebackNudge.random()
            ProactiveNotificationType.MILESTONE_APPROACHING -> milestoneApproaching.random()
            ProactiveNotificationType.ACHIEVEMENT_UNLOCKED -> achievementUnlocked.random()
            ProactiveNotificationType.AI_INSIGHT -> aiInsight.random()
            ProactiveNotificationType.COACH_OUTREACH -> coachOutreach.random()
            ProactiveNotificationType.WEEKLY_SUMMARY -> weeklySummary.random()
            ProactiveNotificationType.HABIT_SPECIFIC -> "Time for {habit_name}! {habit_emoji}"
            ProactiveNotificationType.SOCIAL_ACTIVITY -> "{friend_name} just {action}! 👋"
        }
    }

    /**
     * Get notification title for type
     */
    fun getTitle(type: ProactiveNotificationType, coachName: String = "Your Coach"): String {
        return when (type) {
            ProactiveNotificationType.MORNING_MOTIVATION -> "Good Morning!"
            ProactiveNotificationType.MIDDAY_CHECKIN -> "Midday Check-in"
            ProactiveNotificationType.EVENING_REMINDER -> "Evening Reminder"
            ProactiveNotificationType.STREAK_AT_RISK -> "Streak Alert!"
            ProactiveNotificationType.COMEBACK_NUDGE -> "We Miss You!"
            ProactiveNotificationType.MILESTONE_APPROACHING -> "Milestone Ahead!"
            ProactiveNotificationType.ACHIEVEMENT_UNLOCKED -> "Achievement Unlocked!"
            ProactiveNotificationType.AI_INSIGHT -> "New Insight"
            ProactiveNotificationType.COACH_OUTREACH -> "$coachName Says..."
            ProactiveNotificationType.WEEKLY_SUMMARY -> "Weekly Summary"
            ProactiveNotificationType.HABIT_SPECIFIC -> "Habit Reminder"
            ProactiveNotificationType.SOCIAL_ACTIVITY -> "Friend Activity"
        }
    }
}

/**
 * Smart timing data - learns optimal notification times
 */
@Serializable
data class SmartNotificationTiming(
    val userId: String,
    val optimalMorningHour: Int = 8,
    val optimalMiddayHour: Int = 13,
    val optimalEveningHour: Int = 19,
    val bestDaysForEngagement: List<Int> = listOf(1, 2, 3, 4, 5),  // Mon-Fri
    val averageOpenDelayMinutes: Int = 15,
    val mostResponsiveHours: List<Int> = listOf(8, 9, 12, 13, 18, 19, 20),
    val lastUpdated: Long = 0
)
