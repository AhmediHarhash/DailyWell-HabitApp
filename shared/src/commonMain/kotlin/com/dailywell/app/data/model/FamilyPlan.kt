package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Family Plan Features ($99.99/year for 3 members)
 *
 * Budget Distribution (Owner gets 3x each member):
 * - Total monthly budget: $99.99/12 = $8.33/month
 * - Owner (60%): $5.00/month (soft $4.50, hard $5.00)
 * - Member 1 (20%): $1.67/month (soft $1.50, hard $1.67)
 * - Member 2 (20%): $1.67/month (soft $1.50, hard $1.67)
 *
 * Key benefits:
 * - Family accountability and shared challenges
 * - Owner has 3x the AI budget of each member
 * - Creates viral growth through sharing
 * - Reduces churn through family connections
 */
@Serializable
data class FamilyPlanData(
    val familyId: String? = null,
    val isOwner: Boolean = false,
    val members: List<FamilyMember> = emptyList(),
    val sharedChallenges: List<FamilyChallenge> = emptyList(),
    val sharedMilestones: List<FamilyMilestone> = emptyList(),
    val inviteCode: String? = null,
    val createdAt: String? = null,
    val maxMembers: Int = 3,  // 3 members max: Owner + 2 members
    val monthlyBudgetUsd: Float = 8.33f,  // $99.99/year / 12 months
    val ownerBudgetPercent: Float = 0.60f,  // 60% = $5.00 (3x member)
    val memberBudgetPercent: Float = 0.20f  // 20% = $1.67 each
)

@Serializable
data class FamilyMember(
    val id: String,
    val name: String,
    val avatar: String,           // Emoji avatar
    val role: FamilyRole,
    val joinedAt: String,
    val todayCompletedHabits: Int = 0,
    val currentStreak: Int = 0,
    val weeklyScore: Int = 0,     // 0-100
    val isActive: Boolean = true,
    val lastActiveAt: String? = null,
    val sharedHabits: List<String> = emptyList()  // Habit IDs they share with family
)

@Serializable
enum class FamilyRole(val displayName: String) {
    OWNER("Family Admin"),
    ADULT("Adult"),
    TEEN("Teen (13-17)"),
    CHILD("Child")
}

/**
 * Family challenges - compete or collaborate together
 */
@Serializable
data class FamilyChallenge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val type: FamilyChallengeType,
    val habitId: String? = null,  // Optional: specific habit to track
    val targetValue: Int,         // e.g., 7 days, 100 completions
    val currentProgress: Map<String, Int>,  // memberId -> progress
    val startDate: String,
    val endDate: String,
    val reward: String? = null,   // Fun reward description
    val status: ChallengeStatus = ChallengeStatus.ACTIVE,
    val winnerId: String? = null  // For competitive challenges
)

@Serializable
enum class FamilyChallengeType(val displayName: String) {
    COLLABORATIVE("Team Challenge"),    // Everyone contributes to shared goal
    COMPETITIVE("Friendly Competition"), // Family members compete
    STREAK("Streak Challenge"),          // Who can maintain longest streak
    WEEKLY_SCORE("Weekly Score Battle")  // Highest weekly completion %
}

@Serializable
enum class ChallengeStatus {
    UPCOMING,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

/**
 * Shared family milestones
 */
@Serializable
data class FamilyMilestone(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val achievedBy: List<String>,   // Member IDs who achieved it
    val achievedAt: String,
    val type: MilestoneType,
    val celebrationMessage: String? = null
)

@Serializable
enum class MilestoneType {
    FAMILY_STREAK,        // Everyone completed all habits
    MEMBER_ACHIEVEMENT,   // Individual achievement shared
    CHALLENGE_WON,        // Challenge completed
    COLLECTIVE_GOAL       // Family reached collective goal
}

/**
 * Family activity feed item
 */
@Serializable
data class FamilyActivity(
    val id: String,
    val memberId: String,
    val memberName: String,
    val memberAvatar: String,
    val activityType: FamilyActivityType,
    val message: String,
    val habitId: String? = null,
    val timestamp: String,
    val canHighFive: Boolean = true,
    val highFives: List<String> = emptyList()  // Member IDs who high-fived
)

@Serializable
enum class FamilyActivityType {
    HABIT_COMPLETED,
    STREAK_MILESTONE,
    CHALLENGE_PROGRESS,
    ACHIEVEMENT_UNLOCKED,
    JOINED_FAMILY,
    GAVE_HIGH_FIVE
}

/**
 * Family invite
 */
@Serializable
data class FamilyInvite(
    val code: String,
    val familyId: String,
    val familyName: String,
    val inviterName: String,
    val createdAt: String,
    val expiresAt: String,
    val maxUses: Int = 5,
    val usedCount: Int = 0
)

/**
 * Pre-built family challenge templates
 */
object FamilyChallengeTemplates {

    fun weeklyFamilyStreak(): FamilyChallenge {
        return FamilyChallenge(
            id = "challenge_family_streak_${System.currentTimeMillis()}",
            title = "Family Streak Week",
            description = "Can the whole family complete all habits every day this week?",
            emoji = "👨‍👩‍👧‍👦",
            type = FamilyChallengeType.COLLABORATIVE,
            targetValue = 7,
            currentProgress = emptyMap(),
            startDate = "",
            endDate = "",
            reward = "Family movie night!"
        )
    }

    fun stepChallenge(targetSteps: Int = 50000): FamilyChallenge {
        return FamilyChallenge(
            id = "challenge_steps_${System.currentTimeMillis()}",
            title = "Family Step Challenge",
            description = "Compete for the most steps this week!",
            emoji = "👟",
            type = FamilyChallengeType.COMPETITIVE,
            habitId = "move",
            targetValue = targetSteps,
            currentProgress = emptyMap(),
            startDate = "",
            endDate = "",
            reward = "Winner picks dinner!"
        )
    }

    fun hydrationTeamChallenge(targetGlasses: Int = 100): FamilyChallenge {
        return FamilyChallenge(
            id = "challenge_hydration_${System.currentTimeMillis()}",
            title = "Team Hydration Goal",
            description = "Together, drink $targetGlasses glasses of water this week!",
            emoji = "💧",
            type = FamilyChallengeType.COLLABORATIVE,
            habitId = "water",
            targetValue = targetGlasses,
            currentProgress = emptyMap(),
            startDate = "",
            endDate = "",
            reward = "Smoothie bar trip!"
        )
    }

    fun screenFreeEvening(): FamilyChallenge {
        return FamilyChallenge(
            id = "challenge_unplug_${System.currentTimeMillis()}",
            title = "Screen-Free Evenings",
            description = "No screens after 8pm for the whole family!",
            emoji = "📵",
            type = FamilyChallengeType.COLLABORATIVE,
            habitId = "unplug",
            targetValue = 5,  // 5 days
            currentProgress = emptyMap(),
            startDate = "",
            endDate = "",
            reward = "Board game night!"
        )
    }

    fun mindfulnessMarathon(): FamilyChallenge {
        return FamilyChallenge(
            id = "challenge_calm_${System.currentTimeMillis()}",
            title = "Mindfulness Marathon",
            description = "Who can maintain the longest daily mindfulness streak?",
            emoji = "🧘",
            type = FamilyChallengeType.STREAK,
            habitId = "calm",
            targetValue = 14,  // 14 day max
            currentProgress = emptyMap(),
            startDate = "",
            endDate = "",
            reward = "Spa day!"
        )
    }
}

/**
 * Family encouragement messages
 */
object FamilyMessages {

    val encouragementMessages = listOf(
        "Great job! 👏",
        "Keep it up! 💪",
        "You're crushing it! 🔥",
        "Proud of you! 🌟",
        "Way to go! 🎉",
        "Amazing progress! ✨",
        "You inspire us! 💫"
    )

    val familyStreakMessages = mapOf(
        1 to "Family synced! One day down! 👨‍👩‍👧‍👦",
        3 to "Three days strong as a family! 💪",
        7 to "One week of family wellness! 🎉",
        14 to "Two weeks together! You're unstoppable! 🔥",
        30 to "One month of family health! Incredible! 🏆"
    )

    fun getRandomEncouragement(): String {
        return encouragementMessages.random()
    }

    fun getFamilyStreakMessage(days: Int): String? {
        return familyStreakMessages.entries
            .filter { it.key <= days }
            .maxByOrNull { it.key }?.value
    }
}
