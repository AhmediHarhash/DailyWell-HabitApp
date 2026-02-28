package com.dailywell.app.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Feature #7: Virtual Rewards (WellCoins)
 * Gamification currency system - earn coins for healthy habits, redeem for rewards
 */

@Serializable
data class WellCoinBalance(
    val userId: String,
    val totalCoins: Int = 0,
    val lifetimeEarned: Int = 0,
    val lifetimeSpent: Int = 0,
    val lastUpdated: Instant
)

@Serializable
data class CoinTransaction(
    val id: String,
    val userId: String,
    val amount: Int, // Positive = earned, Negative = spent
    val type: TransactionType,
    val source: String, // e.g., "habit_completion", "daily_checkin", "perfect_week", "store_purchase"
    val description: String,
    val relatedHabitId: String? = null,
    val relatedItemId: String? = null,
    val timestamp: Instant
)

@Serializable
enum class TransactionType {
    EARNED,
    SPENT,
    BONUS,
    REFUND
}

@Serializable
data class CoinEarningRule(
    val id: String,
    val name: String,
    val description: String,
    val coinAmount: Int,
    val trigger: EarningTrigger,
    val maxPerDay: Int? = null, // Limit daily earnings
    val isActive: Boolean = true
)

@Serializable
enum class EarningTrigger {
    HABIT_COMPLETION,      // 5 coins per habit
    DAILY_CHECKIN,         // 10 coins for checking app
    PERFECT_DAY,           // 20 coins for all habits complete
    STREAK_7,              // 50 coins for 7-day streak
    STREAK_30,             // 200 coins for 30-day streak
    VOICE_CHAT_SESSION,    // 15 coins for chatting with coach
    CALENDAR_SYNC,         // 10 coins for syncing calendar
    REFERRAL,              // 100 coins for referring a friend
    CHALLENGE_COMPLETE     // 30 coins for completing micro-challenge
}

@Serializable
data class RewardItem(
    val id: String,
    val name: String,
    val description: String,
    val category: RedemptionCategory,
    val coinCost: Int,
    val imageUrl: String? = null,
    val emoji: String, // Fallback visual
    val isAvailable: Boolean = true,
    val stockLimit: Int? = null, // null = unlimited
    val expiryDate: Instant? = null,
    val metadata: Map<String, String> = emptyMap() // For gift card codes, etc.
)

@Serializable
enum class RedemptionCategory {
    GIFT_CARDS,           // Amazon, Starbucks, etc.
    CHARITY_DONATION,     // Donate to causes
    IN_APP_THEMES,        // Premium themes/customizations
    IN_APP_FEATURES,      // Unlock advanced features
    WELLNESS_DISCOUNTS,   // Partner discounts (gyms, meal kits)
    BADGE_UPGRADES        // Cosmetic badge enhancements
}

@Serializable
data class RedemptionHistory(
    val id: String,
    val userId: String,
    val itemId: String,
    val itemName: String,
    val coinCost: Int,
    val redeemedAt: Instant,
    val status: RedemptionStatus,
    val fulfillmentData: String? = null // Gift card code, download link, etc.
)

@Serializable
enum class RedemptionStatus {
    PENDING,
    FULFILLED,
    FAILED,
    REFUNDED
}

@Serializable
data class DailyCoinSummary(
    val date: Instant,
    val coinsEarned: Int,
    val coinsSpent: Int,
    val netChange: Int,
    val topEarningSource: String
)

// Predefined earning rules
object CoinEarningRules {
    val DEFAULT_RULES = listOf(
        CoinEarningRule(
            id = "habit_complete",
            name = "Habit Completion",
            description = "Complete any habit",
            coinAmount = 5,
            trigger = EarningTrigger.HABIT_COMPLETION,
            maxPerDay = null // No limit
        ),
        CoinEarningRule(
            id = "daily_checkin",
            name = "Daily Check-in",
            description = "Open the app and check in",
            coinAmount = 10,
            trigger = EarningTrigger.DAILY_CHECKIN,
            maxPerDay = 1
        ),
        CoinEarningRule(
            id = "perfect_day",
            name = "Perfect Day",
            description = "Complete all habits in one day",
            coinAmount = 20,
            trigger = EarningTrigger.PERFECT_DAY,
            maxPerDay = 1
        ),
        CoinEarningRule(
            id = "week_streak",
            name = "Week Streak",
            description = "Maintain a 7-day streak",
            coinAmount = 50,
            trigger = EarningTrigger.STREAK_7,
            maxPerDay = null
        ),
        CoinEarningRule(
            id = "month_streak",
            name = "Month Streak",
            description = "Maintain a 30-day streak",
            coinAmount = 200,
            trigger = EarningTrigger.STREAK_30,
            maxPerDay = null
        ),
        CoinEarningRule(
            id = "voice_chat",
            name = "Coach Conversation",
            description = "Have a voice chat with your AI coach",
            coinAmount = 15,
            trigger = EarningTrigger.VOICE_CHAT_SESSION,
            maxPerDay = 3 // Max 45 coins/day from chatting
        ),
        CoinEarningRule(
            id = "challenge_win",
            name = "Challenge Complete",
            description = "Complete a daily micro-challenge",
            coinAmount = 30,
            trigger = EarningTrigger.CHALLENGE_COMPLETE,
            maxPerDay = 1
        )
    )
}

// Predefined reward store items
object RewardStore {
    val REWARD_CATALOG = listOf(
        RewardItem(
            id = "amazon_5",
            name = "$5 Amazon Gift Card",
            description = "Redeem for a $5 Amazon gift card",
            category = RedemptionCategory.GIFT_CARDS,
            coinCost = 500,
            emoji = "🎁",
            isAvailable = true
        ),
        RewardItem(
            id = "starbucks_5",
            name = "$5 Starbucks Gift Card",
            description = "Enjoy your favorite coffee",
            category = RedemptionCategory.GIFT_CARDS,
            coinCost = 450,
            emoji = "☕",
            isAvailable = true
        ),
        RewardItem(
            id = "charity_tree",
            name = "Plant a Tree",
            description = "Donate to plant a tree through One Tree Planted",
            category = RedemptionCategory.CHARITY_DONATION,
            coinCost = 100,
            emoji = "🌳",
            isAvailable = true
        ),
        RewardItem(
            id = "charity_water",
            name = "Clean Water Donation",
            description = "Provide clean water to those in need",
            category = RedemptionCategory.CHARITY_DONATION,
            coinCost = 150,
            emoji = "💧",
            isAvailable = true
        ),
        RewardItem(
            id = "theme_ocean",
            name = "Ocean Theme",
            description = "Unlock the calming ocean theme",
            category = RedemptionCategory.IN_APP_THEMES,
            coinCost = 200,
            emoji = "🌊",
            isAvailable = true
        ),
        RewardItem(
            id = "theme_forest",
            name = "Forest Theme",
            description = "Unlock the serene forest theme",
            category = RedemptionCategory.IN_APP_THEMES,
            coinCost = 200,
            emoji = "🌲",
            isAvailable = true
        ),
        RewardItem(
            id = "theme_sunset",
            name = "Sunset Theme",
            description = "Unlock the warm sunset theme",
            category = RedemptionCategory.IN_APP_THEMES,
            coinCost = 200,
            emoji = "🌅",
            isAvailable = true
        ),
        RewardItem(
            id = "badge_gold",
            name = "Gold Badge Upgrade",
            description = "Make your badges shine in gold",
            category = RedemptionCategory.BADGE_UPGRADES,
            coinCost = 300,
            emoji = "🏆",
            isAvailable = true
        ),
        RewardItem(
            id = "feature_insights",
            name = "Advanced Insights",
            description = "Unlock detailed analytics and trends",
            category = RedemptionCategory.IN_APP_FEATURES,
            coinCost = 400,
            emoji = "📊",
            isAvailable = true
        ),
        RewardItem(
            id = "wellness_gym",
            name = "Gym Membership Discount",
            description = "20% off at partner gyms",
            category = RedemptionCategory.WELLNESS_DISCOUNTS,
            coinCost = 600,
            emoji = "💪",
            isAvailable = true
        )
    )
}
