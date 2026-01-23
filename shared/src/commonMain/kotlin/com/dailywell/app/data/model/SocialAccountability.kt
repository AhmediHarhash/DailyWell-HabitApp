package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * Social Accountability Features
 * Privacy-first approach: all habits private by default
 *
 * Research: 65% more likely to succeed with accountability partner
 * Focus on small groups (3-10 people) not large communities
 */
@Serializable
data class SocialAccountabilityData(
    val userId: String = "",
    val displayName: String = "",
    val profileEmoji: String = "🧑",
    val groups: List<AccountabilityGroup> = emptyList(),
    val partners: List<AccountabilityPartner> = emptyList(),
    val sharedHabits: List<SharedHabit> = emptyList(),
    val receivedHighFives: List<HighFive> = emptyList(),
    val sentHighFives: List<HighFive> = emptyList(),
    val commitmentContracts: List<CommitmentContract> = emptyList(),
    val privacySettings: PrivacySettings = PrivacySettings()
)

/**
 * Accountability Group (3-10 people)
 */
@Serializable
data class AccountabilityGroup(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String? = null,
    val createdBy: String,
    val createdAt: String,
    val members: List<GroupMember> = emptyList(),
    val maxMembers: Int = 10,
    val isPrivate: Boolean = true,           // Invite-only vs discoverable
    val inviteCode: String? = null,          // For private groups
    val groupType: GroupType = GroupType.GENERAL,
    val settings: GroupSettings = GroupSettings()
)

@Serializable
enum class GroupType(val label: String, val emoji: String) {
    GENERAL("General", "🎯"),
    FITNESS("Fitness Focus", "💪"),
    WELLNESS("Wellness", "🧘"),
    NUTRITION("Nutrition", "🥗"),
    SLEEP("Sleep Club", "😴"),
    PRODUCTIVITY("Productivity", "⚡"),
    MINDFULNESS("Mindfulness", "🧠")
}

@Serializable
data class GroupMember(
    val userId: String,
    val displayName: String,
    val profileEmoji: String,
    val role: GroupRole = GroupRole.MEMBER,
    val joinedAt: String,
    val sharedHabits: List<String> = emptyList(),   // habitIds shared with this group
    val currentStreak: Int = 0,                      // Overall streak for shared habits
    val weeklyCompletionRate: Float = 0f
)

@Serializable
enum class GroupRole {
    OWNER,
    ADMIN,
    MEMBER
}

@Serializable
data class GroupSettings(
    val showMemberStreaks: Boolean = true,
    val allowHighFives: Boolean = true,
    val allowComments: Boolean = false,      // Keep it simple initially
    val weeklyDigest: Boolean = true,
    val leaderboardEnabled: Boolean = false  // Can be motivating or demotivating
)

/**
 * Individual Accountability Partner (1-on-1)
 */
@Serializable
data class AccountabilityPartner(
    val partnerId: String,
    val displayName: String,
    val profileEmoji: String,
    val connectedAt: String,
    val status: PartnerStatus = PartnerStatus.ACTIVE,
    val sharedHabits: List<String> = emptyList(),
    val lastInteraction: String? = null,
    val mutualHighFives: Int = 0
)

@Serializable
enum class PartnerStatus {
    PENDING_SENT,       // You invited them
    PENDING_RECEIVED,   // They invited you
    ACTIVE,
    PAUSED,
    DECLINED
}

/**
 * A habit that is shared with groups or partners
 */
@Serializable
data class SharedHabit(
    val habitId: String,
    val habitName: String,
    val habitEmoji: String,
    val sharedWith: List<SharingTarget> = emptyList(),
    val shareStreak: Boolean = true,
    val shareCompletionTime: Boolean = false,
    val shareNotes: Boolean = false
)

@Serializable
data class SharingTarget(
    val targetId: String,          // groupId or partnerId
    val targetType: SharingTargetType,
    val targetName: String,
    val sharedAt: String
)

@Serializable
enum class SharingTargetType {
    GROUP,
    PARTNER
}

/**
 * High Five - simple encouragement system
 * Keeps it lightweight and positive
 */
@Serializable
data class HighFive(
    val id: String,
    val fromUserId: String,
    val fromDisplayName: String,
    val fromEmoji: String,
    val toUserId: String,
    val habitId: String? = null,
    val habitName: String? = null,
    val reason: HighFiveReason,
    val message: String? = null,       // Optional short message
    val createdAt: String,
    val isRead: Boolean = false
)

@Serializable
enum class HighFiveReason(val label: String, val emoji: String) {
    STREAK_MILESTONE("Streak milestone!", "🔥"),
    PERFECT_DAY("Perfect day!", "⭐"),
    COMEBACK("Great comeback!", "💪"),
    CONSISTENCY("Staying consistent!", "🎯"),
    ENCOURAGEMENT("Keep going!", "❤️"),
    CELEBRATION("Celebrating you!", "🎉")
}

/**
 * Commitment Contract - optional public commitment
 * Research: Public commitments increase follow-through
 */
@Serializable
data class CommitmentContract(
    val id: String,
    val userId: String,
    val habitId: String,
    val habitName: String,
    val commitment: String,          // "I commit to meditate daily for 30 days"
    val startDate: String,
    val endDate: String,
    val targetDays: Int,             // How many days to complete
    val currentProgress: Int = 0,
    val witnesses: List<String> = emptyList(),  // userIds who can see progress
    val stakes: String? = null,      // "If I fail, I'll donate $50"
    val status: ContractStatus = ContractStatus.ACTIVE,
    val createdAt: String
)

@Serializable
enum class ContractStatus {
    DRAFT,
    ACTIVE,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Privacy Settings - control what others can see
 */
@Serializable
data class PrivacySettings(
    val isProfilePublic: Boolean = false,
    val showOverallProgress: Boolean = true,     // High-level stats ok
    val showIndividualHabits: Boolean = false,   // Specific habits private by default
    val showStreaks: Boolean = true,
    val allowDiscovery: Boolean = false,         // Can others find you
    val allowGroupInvites: Boolean = true,
    val allowPartnerRequests: Boolean = true,
    val notifyOnHighFive: Boolean = true
)

/**
 * Social Feed Item - for group activity
 */
@Serializable
data class SocialFeedItem(
    val id: String,
    val userId: String,
    val displayName: String,
    val profileEmoji: String,
    val type: FeedItemType,
    val habitId: String? = null,
    val habitName: String? = null,
    val habitEmoji: String? = null,
    val message: String,
    val timestamp: String,
    val highFiveCount: Int = 0
)

@Serializable
enum class FeedItemType {
    HABIT_COMPLETED,
    STREAK_MILESTONE,
    PERFECT_DAY,
    JOINED_GROUP,
    COMEBACK
}

/**
 * Messages for social features
 */
object SocialMessages {

    fun getHighFiveMessage(reason: HighFiveReason, habitName: String? = null): String {
        return when (reason) {
            HighFiveReason.STREAK_MILESTONE -> "Amazing streak${habitName?.let { " on $it" } ?: ""}! Keep it going! 🔥"
            HighFiveReason.PERFECT_DAY -> "You crushed it today! Every habit complete! ⭐"
            HighFiveReason.COMEBACK -> "Love seeing you back${habitName?.let { " to $it" } ?: ""}! 💪"
            HighFiveReason.CONSISTENCY -> "Your consistency is inspiring! Keep showing up! 🎯"
            HighFiveReason.ENCOURAGEMENT -> "You've got this! One day at a time! ❤️"
            HighFiveReason.CELEBRATION -> "Celebrating your journey! 🎉"
        }
    }

    fun getGroupWelcomeMessage(groupName: String): String {
        return "Welcome to $groupName! This is a supportive space where we cheer each other on. " +
                "Remember: progress, not perfection. Let's grow together! 🌱"
    }

    fun getCommitmentPrompt(): String {
        return "Public commitments increase success by 65%. " +
                "What habit are you committing to, and for how long?"
    }

    val privacyReminder = "Your habits are private by default. " +
            "You choose what to share and with whom. " +
            "We believe in supportive accountability, not surveillance."
}
