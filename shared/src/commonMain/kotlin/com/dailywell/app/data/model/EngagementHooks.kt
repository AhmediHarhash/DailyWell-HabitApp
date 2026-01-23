package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Engagement Hooks & Psychology Boosters
 *
 * These features implement the challenges identified in the competitor analysis:
 * - Social proof during trial
 * - Time-sensitive trial nudges
 * - First win psychology
 * - Loss aversion triggers
 */

// ============================================================
// SOCIAL PROOF - Show users they're part of something bigger
// ============================================================

@Serializable
data class SocialProofData(
    val totalUsersToday: Int = 0,
    val habitsCompletedToday: Long = 0,
    val streaksActiveNow: Int = 0,
    val perfectDaysThisWeek: Int = 0,
    val recentAchievements: List<SocialProofItem> = emptyList()
)

@Serializable
data class SocialProofItem(
    val id: String,
    val userName: String, // Anonymized: "Sarah M." or "A user in California"
    val action: SocialProofAction,
    val detail: String, // "30-day streak" or "completed all 7 habits"
    val timeAgo: String, // "2 minutes ago"
    val emoji: String
)

@Serializable
enum class SocialProofAction(val template: String) {
    STREAK_MILESTONE("%s just hit a %s!"),
    PERFECT_DAY("%s just achieved a perfect day!"),
    LEVEL_UP("%s just reached %s!"),
    BADGE_EARNED("%s just earned the %s badge!"),
    CHALLENGE_WON("%s just won a challenge!"),
    JOINED_COMMUNITY("Welcome %s to DailyWell!")
}

object SocialProofMessages {
    val communityStats = listOf(
        "🌍 %d people are building habits right now",
        "💪 %d habits completed today by our community",
        "🔥 %d active streaks happening right now",
        "⭐ Join %d others on their wellness journey"
    )

    val milestoneAnnouncements = listOf(
        "🎉 Someone just hit a 100-day streak!",
        "🏆 A user just earned their first badge!",
        "💯 Another perfect day achieved!",
        "🔥 Streak record broken: 365 days!"
    )

    // Fake but realistic social proof for new apps
    fun generateSocialProof(): List<SocialProofItem> {
        val names = listOf(
            "Sarah M.", "James K.", "Maria L.", "David R.", "Emma W.",
            "Michael T.", "Jennifer H.", "Chris P.", "Amanda S.", "Ryan D."
        )
        val locations = listOf(
            "California", "New York", "Texas", "Florida", "Colorado",
            "Washington", "Oregon", "Arizona", "Illinois", "Massachusetts"
        )

        return listOf(
            SocialProofItem(
                id = "sp1",
                userName = names.random(),
                action = SocialProofAction.STREAK_MILESTONE,
                detail = "${(7..30).random()}-day streak",
                timeAgo = "${(1..15).random()} minutes ago",
                emoji = "🔥"
            ),
            SocialProofItem(
                id = "sp2",
                userName = "A user in ${locations.random()}",
                action = SocialProofAction.PERFECT_DAY,
                detail = "all habits completed",
                timeAgo = "${(2..20).random()} minutes ago",
                emoji = "💯"
            ),
            SocialProofItem(
                id = "sp3",
                userName = names.random(),
                action = SocialProofAction.LEVEL_UP,
                detail = "Level ${(5..25).random()}",
                timeAgo = "${(5..30).random()} minutes ago",
                emoji = "⬆️"
            )
        )
    }
}

// ============================================================
// TRIAL NUDGES - Time-sensitive messages to drive conversion
// ============================================================

@Serializable
data class TrialNudge(
    val dayOfTrial: Int,
    val title: String,
    val message: String,
    val emoji: String,
    val ctaText: String,
    val urgency: NudgeUrgency,
    val showPaywall: Boolean = false
)

@Serializable
enum class NudgeUrgency {
    LOW,      // Informational
    MEDIUM,   // Encouraging
    HIGH,     // Time-sensitive
    CRITICAL  // Last chance
}

object TrialNudges {

    fun getNudgeForDay(day: Int, userName: String = "there"): TrialNudge? = when (day) {
        1 -> TrialNudge(
            dayOfTrial = 1,
            title = "Welcome to Premium! 🎉",
            message = "Hey $userName! You have 14 days of full access to EVERYTHING. AI coaching, audio guides, habit stacking, social features - it's all yours. Make the most of it!",
            emoji = "🎁",
            ctaText = "Explore Premium Features",
            urgency = NudgeUrgency.LOW
        )
        3 -> TrialNudge(
            dayOfTrial = 3,
            title = "You're Building Momentum!",
            message = "3 days in and you're doing great! Have you tried the AI coaching yet? It's one of our most loved features.",
            emoji = "🚀",
            ctaText = "Try AI Coaching",
            urgency = NudgeUrgency.LOW
        )
        5 -> TrialNudge(
            dayOfTrial = 5,
            title = "Streak Shield Available!",
            message = "Did you know premium includes streak protection? If you miss a day, you can freeze your streak. Try it!",
            emoji = "🛡️",
            ctaText = "Learn About Streak Shield",
            urgency = NudgeUrgency.MEDIUM
        )
        7 -> TrialNudge(
            dayOfTrial = 7,
            title = "One Week Down! 🎊",
            message = "You've been premium for a week! Your habits are forming. Check out your pattern insights to see how far you've come.",
            emoji = "📊",
            ctaText = "View My Insights",
            urgency = NudgeUrgency.MEDIUM
        )
        10 -> TrialNudge(
            dayOfTrial = 10,
            title = "4 Days Left of Trial",
            message = "Your free trial ends in 4 days. You've built real progress - don't lose access to your insights and premium features!",
            emoji = "⏰",
            ctaText = "Keep Premium Access",
            urgency = NudgeUrgency.HIGH,
            showPaywall = true
        )
        12 -> TrialNudge(
            dayOfTrial = 12,
            title = "48 Hours Remaining ⚠️",
            message = "Your trial ends in 2 days! Your streak data, AI insights, and audio coaching will be locked. Subscribe now to keep everything.",
            emoji = "⚠️",
            ctaText = "Subscribe Now - Save 50%",
            urgency = NudgeUrgency.HIGH,
            showPaywall = true
        )
        13 -> TrialNudge(
            dayOfTrial = 13,
            title = "Last Full Day! 🚨",
            message = "Tomorrow your premium access ends. You've worked so hard on your habits - don't let that progress slip away. Lock in your rate now.",
            emoji = "🚨",
            ctaText = "Keep My Progress",
            urgency = NudgeUrgency.CRITICAL,
            showPaywall = true
        )
        14 -> TrialNudge(
            dayOfTrial = 14,
            title = "Trial Ends Today",
            message = "This is it - your last day of premium access. Subscribe now to keep your streak protection, AI coaching, and all premium features. Thank you for trying DailyWell!",
            emoji = "💫",
            ctaText = "Continue Premium - \$2.50/mo",
            urgency = NudgeUrgency.CRITICAL,
            showPaywall = true
        )
        else -> null
    }

    // Loss aversion messages - what they'll lose
    val lossAversionMessages = listOf(
        "🔒 Your AI coaching sessions will be locked",
        "🔒 Streak protection will be disabled",
        "🔒 Audio coaching library becomes limited",
        "🔒 Pattern insights will be hidden",
        "🔒 Habit stacking features locked",
        "🔒 Smart reminders downgraded to basic",
        "🔒 Social features limited",
        "🔒 Family plan access removed"
    )

    // Value reminders - what they've gained during trial
    fun getValueSummary(
        habitsCompleted: Int,
        currentStreak: Int,
        insightsGenerated: Int,
        audioListened: Int
    ): String {
        return buildString {
            appendLine("📊 Your Trial Progress:")
            appendLine("• $habitsCompleted habits completed")
            if (currentStreak > 0) appendLine("• $currentStreak day streak built")
            if (insightsGenerated > 0) appendLine("• $insightsGenerated AI insights generated")
            if (audioListened > 0) appendLine("• $audioListened minutes of audio coaching")
            appendLine()
            appendLine("Don't lose this progress!")
        }
    }
}

// ============================================================
// FIRST WIN - Immediate dopamine hit during onboarding
// ============================================================

@Serializable
data class FirstWinChallenge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val actionLabel: String,
    val completedMessage: String,
    val xpReward: Int
)

object FirstWinChallenges {

    val onboardingFirstWin = FirstWinChallenge(
        id = "first_win_onboarding",
        title = "Your First Win! 🎯",
        description = "Complete your first habit right now to start your journey.",
        emoji = "🌟",
        actionLabel = "Complete Now",
        completedMessage = "AMAZING! You just completed your first habit! This is the start of something great. 🎉",
        xpReward = 50
    )

    val quickWins = listOf(
        FirstWinChallenge(
            id = "quick_water",
            title = "Quick Hydration Win",
            description = "Drink a glass of water right now.",
            emoji = "💧",
            actionLabel = "I Drank Water",
            completedMessage = "Perfect! Hydration complete. That was easy, right?",
            xpReward = 10
        ),
        FirstWinChallenge(
            id = "quick_breath",
            title = "One Minute Calm",
            description = "Take 3 deep breaths right now.",
            emoji = "🧘",
            actionLabel = "I Breathed",
            completedMessage = "Wonderful! You just practiced mindfulness. See how simple it is?",
            xpReward = 10
        ),
        FirstWinChallenge(
            id = "quick_stretch",
            title = "Quick Stretch",
            description = "Stand up and stretch for 30 seconds.",
            emoji = "🙆",
            actionLabel = "I Stretched",
            completedMessage = "Great! Movement done. Your body thanks you!",
            xpReward = 10
        )
    )

    // Celebration messages for first wins
    val firstWinCelebrations = listOf(
        "🎉 FIRST WIN UNLOCKED!",
        "⭐ You did it! First habit complete!",
        "🏆 Champion! Your journey has begun!",
        "🔥 First win in the books!",
        "💪 That's how legends start!"
    )

    // Psychology: The first completion sets the tone
    val firstWinPsychology = """
        Research shows that completing even a tiny task creates momentum.
        Your brain just got a dopamine hit. That's the feeling of progress.
        Now, let's ride that wave into your habits!
    """.trimIndent()
}

// ============================================================
// ENGAGEMENT NOTIFICATIONS - Smart timing
// ============================================================

@Serializable
data class EngagementNotification(
    val id: String,
    val title: String,
    val body: String,
    val trigger: NotificationTrigger,
    val priority: NotificationPriority
)

@Serializable
enum class NotificationTrigger {
    MORNING_MOTIVATION,    // 7-9 AM
    MIDDAY_CHECKIN,        // 12-1 PM
    EVENING_REMINDER,      // 6-8 PM
    STREAK_AT_RISK,        // When user hasn't logged by usual time
    ACHIEVEMENT_UNLOCKED,  // Immediate
    FRIEND_ACTIVITY,       // When friends complete habits
    TRIAL_COUNTDOWN,       // Last days of trial
    COMEBACK_OPPORTUNITY   // After 2+ days missed
}

@Serializable
enum class NotificationPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

object EngagementNotifications {

    val morningMotivation = listOf(
        EngagementNotification(
            "morning_1",
            "New Day, New Wins 🌅",
            "Your habits are waiting. Start with just one!",
            NotificationTrigger.MORNING_MOTIVATION,
            NotificationPriority.MEDIUM
        ),
        EngagementNotification(
            "morning_2",
            "Good Morning! ☀️",
            "Ready to build something amazing today?",
            NotificationTrigger.MORNING_MOTIVATION,
            NotificationPriority.MEDIUM
        )
    )

    val streakAtRisk = listOf(
        EngagementNotification(
            "streak_risk_1",
            "🔥 Streak Alert!",
            "Don't let your %d-day streak slip! Quick check-in?",
            NotificationTrigger.STREAK_AT_RISK,
            NotificationPriority.HIGH
        ),
        EngagementNotification(
            "streak_risk_2",
            "⚠️ Your streak needs you!",
            "Just one habit to keep your momentum going.",
            NotificationTrigger.STREAK_AT_RISK,
            NotificationPriority.HIGH
        )
    )

    val comebackMessages = listOf(
        EngagementNotification(
            "comeback_1",
            "We miss you! 🌱",
            "It's been a few days. Ready to get back on track?",
            NotificationTrigger.COMEBACK_OPPORTUNITY,
            NotificationPriority.MEDIUM
        ),
        EngagementNotification(
            "comeback_2",
            "No judgment, just progress 💪",
            "Missing days happens. What matters is coming back. We're here.",
            NotificationTrigger.COMEBACK_OPPORTUNITY,
            NotificationPriority.MEDIUM
        )
    )
}
