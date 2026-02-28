package com.dailywell.app.data.model

import kotlinx.serialization.Serializable

/**
 * AI Usage Tracking for Cost Control (February 2026)
 *
 * Hybrid AI Model Hierarchy:
 * 1. Decision Tree (FREE) - Simple routing
 * 2. Qwen2.5 0.5B (FREE) - On-device SLM (~380MB, 15-30 tok/s via llama.cpp)
 * 3. Claude Haiku 4.5 (PAID) - Cloud API ($1/$5 per MTok)
 * 4. Claude Sonnet 4.5 (PAID) - Vision/complex only ($3/$15 per MTok)
 *
 * Monthly USD Caps (resets on 1st of each month):
 * - FREE: $0.10 hard cap -> SLM-only immediately
 * - MONTHLY: $4.50 soft, $5.00 hard -> SLM-only after
 * - ANNUAL: $3.60 soft, $4.00 hard -> SLM-only after
 * - LIFETIME: $5.00 soft, $5.50 hard -> SLM-only after
 * - FAMILY_OWNER (60%/3x): $2.50 soft, $2.75 hard -> SLM-only after
 * - FAMILY_MEMBER (20%): $0.80 soft, $0.92 hard -> SLM-only after
 *
 * Claude Haiku 4.5 pricing:
 * - Input: $1.00 per million tokens
 * - Output: $5.00 per million tokens
 * - Avg message (~500 tokens): ~$0.003
 *
 * At $6.00 hard cap = ~2,000 Haiku messages/month (plenty for most users)
 */

@Serializable
data class UserAIUsage(
    val userId: String,
    val planType: AIPlanType = AIPlanType.FREE,
    val monthlyTokenLimit: Int = AIPlanType.FREE.monthlyTokenLimit,
    val tokensUsed: Int = 0,
    val messagesCount: Int = 0,
    val freeMessagesCount: Int = 0,  // Messages handled by decision tree (FREE)
    val slmMessagesCount: Int = 0,   // Messages handled by Qwen 0.5B (FREE)
    val aiMessagesCount: Int = 0,    // Messages sent to Claude API (PAID)
    val cloudChatCalls: Int = 0,     // Cloud calls from coach chat replies
    val cloudScanCalls: Int = 0,     // Cloud calls from food scan analysis
    val cloudReportCalls: Int = 0,   // Cloud calls from weekly summaries/reports
    val currentMonthCostUsd: Float = 0f,  // Actual USD spent this month
    val resetDate: String,           // ISO date when usage resets (1st of month)
    val lastUpdated: String
) {
    val tokensRemaining: Int
        get() = (monthlyTokenLimit - tokensUsed).coerceAtLeast(0)

    val percentUsed: Float
        get() = if (monthlyTokenLimit > 0) {
            (tokensUsed.toFloat() / monthlyTokenLimit * 100).coerceIn(0f, 100f)
        } else 0f

    val percentRemaining: Float
        get() = (100f - percentUsed).coerceAtLeast(0f)

    val hasCreditsRemaining: Boolean
        get() = tokensRemaining > 0

    val isAtLimit: Boolean
        get() = tokensUsed >= monthlyTokenLimit

    // USD-based limits (primary cost control)
    val softCapUsd: Float
        get() = planType.softCapUsd

    val hardCapUsd: Float
        get() = planType.hardCapUsd

    val isAtSoftCap: Boolean
        get() = currentMonthCostUsd >= softCapUsd

    val isAtHardCap: Boolean
        get() = currentMonthCostUsd >= hardCapUsd

    val shouldUseSLM: Boolean
        get() = isAtHardCap

    val costRemainingUsd: Float
        get() = (hardCapUsd - currentMonthCostUsd).coerceAtLeast(0f)

    val costPercentUsed: Float
        get() = if (hardCapUsd > 0) {
            (currentMonthCostUsd / hardCapUsd * 100).coerceIn(0f, 100f)
        } else 100f

    val localMessagesCount: Int
        get() = freeMessagesCount + slmMessagesCount

    val cloudTotalCalls: Int
        get() = cloudChatCalls + cloudScanCalls + cloudReportCalls

    /**
     * Efficiency ratio: How many messages are handled for FREE vs PAID
     * Higher is better (means more decision tree + SLM usage)
     */
    val efficiencyRatio: Float
        get() = if (messagesCount > 0) {
            (freeMessagesCount + slmMessagesCount).toFloat() / messagesCount
        } else 0f

    /**
     * Average tokens per AI message
     */
    val avgTokensPerMessage: Float
        get() = if (aiMessagesCount > 0) {
            tokensUsed.toFloat() / aiMessagesCount
        } else 0f

    /**
     * Average cost per Claude API message
     */
    val avgCostPerMessage: Float
        get() = if (aiMessagesCount > 0) {
            currentMonthCostUsd / aiMessagesCount
        } else 0f
}

@Serializable
enum class AIPlanType(
    val displayName: String,
    val monthlyTokenLimit: Int,
    val maxMessagesPerDay: Int,
    val softCapUsd: Float,   // Warning threshold
    val hardCapUsd: Float    // SLM-only after this (until next month)
) {
    // PRICING (February 2026):
    // Monthly: $9.99/mo, Annual: $79.99/yr ($6.67/mo), Lifetime: $349.99
    // Family: $99.99/yr ($8.33/mo for 6), Student: $49.99/yr ($4.17/mo)
    // AI budget = ~55% of monthly price

    FREE(
        displayName = "Free",
        monthlyTokenLimit = 25_000,
        maxMessagesPerDay = 5,
        softCapUsd = 0.10f,
        hardCapUsd = 0.10f   // SLM immediately for free users
    ),
    MONTHLY(
        displayName = "Premium Monthly",
        monthlyTokenLimit = 1_000_000,
        maxMessagesPerDay = 100,
        softCapUsd = 4.50f,
        hardCapUsd = AIGovernancePolicy.MONTHLY_WALLET_USD
    ),
    ANNUAL(
        displayName = "Premium Annual",
        monthlyTokenLimit = 800_000,
        maxMessagesPerDay = 80,
        softCapUsd = 3.60f,  // Alert threshold before annual hard stop
        hardCapUsd = AIGovernancePolicy.ANNUAL_WALLET_USD
    ),
    LIFETIME(
        displayName = "Lifetime",
        monthlyTokenLimit = 1_000_000,
        maxMessagesPerDay = 100,
        softCapUsd = 5.00f,  // Same as monthly (amortized over years)
        hardCapUsd = 5.50f   // ~1,800 Haiku messages/month
    ),
    STUDENT(
        displayName = "Student",
        monthlyTokenLimit = 500_000,
        maxMessagesPerDay = 50,
        softCapUsd = 2.00f,  // $49.99/yr = $4.17/mo Ã¢â€ â€™ 55% = $2.30
        hardCapUsd = 2.30f   // ~750 Haiku messages/month
    ),
    // Family Plan: $99.99/year / 12 = $8.33/month total budget
    // Owner: 60% (3x member) = $5.00, Members: 20% each = $1.67
    // AI budget = 55% of each share
    FAMILY_OWNER(
        displayName = "Family Owner",
        monthlyTokenLimit = 600_000,
        maxMessagesPerDay = 60,
        softCapUsd = 2.50f,  // 60% of $8.33 = $5.00 Ã¢â€ â€™ 55% = $2.75
        hardCapUsd = 2.75f   // ~900 Haiku messages/month
    ),
    FAMILY_MEMBER(
        displayName = "Family Member",
        monthlyTokenLimit = 200_000,
        maxMessagesPerDay = 20,
        softCapUsd = 0.80f,  // 20% of $8.33 = $1.67 Ã¢â€ â€™ 55% = $0.92
        hardCapUsd = 0.92f   // ~300 Haiku messages/month
    )
}

/**
 * Result of checking if AI can be used
 */
@Serializable
data class AIUsageCheckResult(
    val canUseAI: Boolean,
    val canUseCloudAI: Boolean = true,     // false = must use SLM
    val recommendedModel: AIModelUsed = AIModelUsed.CLAUDE_HAIKU,
    val reason: AIUsageBlockReason? = null,
    val tokensRemaining: Int,
    val costRemainingUsd: Float = 0f,
    val percentRemaining: Float,
    val isAtSoftCap: Boolean = false,
    val isAtHardCap: Boolean = false,
    val upgradeMessage: String? = null,
    val slmFallbackMessage: String? = null
)

@Serializable
enum class AIUsageBlockReason(val userMessage: String) {
    CREDITS_DEPLETED(
        "You've used all your AI credits for this month. Upgrade for more, or wait for your credits to reset."
    ),
    DAILY_LIMIT_REACHED(
        "You've reached your daily AI message limit. Come back tomorrow!"
    ),
    NOT_PREMIUM(
        "Unlock unlimited AI coaching with Premium!"
    ),
    RATE_LIMITED(
        "Too many messages too quickly. Please wait a moment."
    ),
    SOFT_CAP_REACHED(
        "Heads up! You're approaching your monthly AI limit. Responses may use on-device AI to conserve credits."
    ),
    HARD_CAP_REACHED(
        "You've reached your monthly AI budget. Using on-device AI until next month - still works great, just a bit different!"
    ),
    SLM_FALLBACK_ACTIVE(
        "Currently using on-device AI to stay within budget. Resets on the 1st!"
    )
}

/**
 * AI Model used for the interaction
 *
 * Routing Hierarchy (cost optimization):
 * - DECISION_TREE: Pattern matching, ~70% of messages (FREE)
 * - QWEN_0_5B: On-device SLM (Qwen2.5 0.5B via llama.cpp) for simple reasoning (FREE)
 * - CLAUDE_HAIKU: Standard cloud responses, $1/$5 per MTok (PAID)
 * - CLAUDE_SONNET: Complex analysis & vision, $3/$15 per MTok (PAID)
 * - CLAUDE_OPUS: Heavy reports & deep insights, $15/$75 per MTok (PAID)
 *
 * Tier Access:
 * - FREE: DECISION_TREE + QWEN_0_5B (SLM) only (never Claude)
 * - PREMIUM: All models with intelligent routing
 */
@Serializable
enum class AIModelUsed(val displayName: String, val isFree: Boolean) {
    DECISION_TREE("Decision Tree", true),
    QWEN_0_5B("Qwen2.5 (On-Device)", true),
    CLAUDE_HAIKU("Claude Haiku 4.5", false),
    CLAUDE_SONNET("Claude Sonnet 4.5", false),
    CLAUDE_OPUS("Claude Opus 4.5", false)
}

/**
 * Tracks a single AI interaction for billing purposes
 */
@Serializable
data class AIInteraction(
    val id: String,
    val userId: String,
    val timestamp: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val modelUsed: AIModelUsed = AIModelUsed.DECISION_TREE,
    val responseCategory: String,   // From ResponseCategory enum
    val durationMs: Long? = null
) {
    // For backwards compatibility
    val usedDecisionTree: Boolean
        get() = modelUsed == AIModelUsed.DECISION_TREE

    val isFreeModel: Boolean
        get() = modelUsed.isFree

    val estimatedCostUsd: Float
        get() = when (modelUsed) {
            AIModelUsed.DECISION_TREE -> 0f
            AIModelUsed.QWEN_0_5B -> 0f  // On-device = FREE
            AIModelUsed.CLAUDE_HAIKU -> {
                // Claude Haiku 4.5 pricing (2026): $1/$5 per MTok
                val inputCost = inputTokens * 1.00f / 1_000_000
                val outputCost = outputTokens * 5.00f / 1_000_000
                inputCost + outputCost
            }
            AIModelUsed.CLAUDE_SONNET -> {
                // Claude Sonnet 4.5 pricing (2026): $3/$15 per MTok
                val inputCost = inputTokens * 3.00f / 1_000_000
                val outputCost = outputTokens * 15.00f / 1_000_000
                inputCost + outputCost
            }
            AIModelUsed.CLAUDE_OPUS -> {
                // Claude Opus 4.5 pricing (2026): $15/$75 per MTok
                val inputCost = inputTokens * 15.00f / 1_000_000
                val outputCost = outputTokens * 75.00f / 1_000_000
                inputCost + outputCost
            }
        }
}

/**
 * Daily usage summary for analytics
 */
@Serializable
data class DailyAIUsageSummary(
    val date: String,
    val totalMessages: Int,
    val freeMessages: Int,
    val paidMessages: Int,
    val totalTokens: Int,
    val estimatedCostUsd: Float,
    val efficiencyPercent: Float  // % handled by decision tree
)

/**
 * Monthly usage report
 */
@Serializable
data class MonthlyAIUsageReport(
    val userId: String,
    val month: String,  // "2024-01"
    val planType: AIPlanType,
    val totalMessages: Int,
    val freeMessages: Int,
    val paidMessages: Int,
    val totalTokens: Int,
    val tokenLimit: Int,
    val estimatedCostUsd: Float,
    val efficiencyPercent: Float,
    val peakUsageDay: String?,
    val mostUsedCategory: String?,
    val dailyBreakdown: List<DailyAIUsageSummary> = emptyList()
) {
    val savingsFromDecisionTree: Float
        get() {
            // Estimate: each free message would have cost ~$0.002 if sent to Claude
            val potentialCost = freeMessages * 0.002f
            return potentialCost
        }

    val utilizationPercent: Float
        get() = if (tokenLimit > 0) {
            (totalTokens.toFloat() / tokenLimit * 100).coerceIn(0f, 100f)
        } else 0f
}

@Serializable
data class AIRoutingIntentStat(
    val intent: String,
    val callCount: Int,
    val avgResponseTimeMs: Long,
    val avgCostUsd: Float
)


