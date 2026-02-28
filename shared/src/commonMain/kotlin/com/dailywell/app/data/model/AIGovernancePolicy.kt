package com.dailywell.app.data.model

/**
 * Centralized AI governance policy values.
 * Keep wallet, scan, and report rules in one place so pricing behavior stays consistent.
 */
object AIGovernancePolicy {
    const val INTERNAL_COST_MULTIPLIER = 1.30f

    // Monthly wallet envelopes (internal billed cost, not raw API cost).
    const val MONTHLY_WALLET_USD = 5.00f
    const val ANNUAL_WALLET_USD = 4.00f

    // Food scan limits for acquisition/activation.
    const val FREE_SCAN_LIMIT_PER_MONTH = 5
    const val TRIAL_SCAN_LIMIT_TOTAL = 2

    // Weekly report policy.
    const val WEEKLY_REPORT_LIMIT_PER_WEEK = 1
    const val WEEKLY_REPORT_LIMIT_PER_MONTH = 4

    // Conservative token estimate for one Sonnet vision meal scan.
    const val FOOD_SCAN_ESTIMATED_INPUT_TOKENS = 3500
    const val FOOD_SCAN_ESTIMATED_OUTPUT_TOKENS = 700

    // Cloud abuse controls (shared across routing/chat/scan).
    const val MIN_SECONDS_BETWEEN_CLOUD_CALLS = 3
    const val MAX_CLOUD_CALLS_PER_MINUTE = 10
    const val MAX_TOKENS_PER_DAY_TRIAL = 50_000
    const val MAX_TOKENS_PER_DAY_FREE = 10_000

    data class IntentQuotas(
        val insightsPerDay: Int = 3,
        val coachMessagesPerDay: Int = 15,
        val whyExplanationsPerDay: Int = 5,
        val plansPerDay: Int = 2,
        val mealIdeasPerDay: Int = 5,
        val reportsPerWeek: Int = 1
    )

    /**
     * Single source of truth for daily/weekly AI intent quotas.
     */
    fun intentQuotasFor(planType: AIPlanType, isTrial: Boolean): IntentQuotas {
        if (isTrial) {
            return IntentQuotas(
                insightsPerDay = 2,
                coachMessagesPerDay = 10,
                whyExplanationsPerDay = 3,
                plansPerDay = 1,
                mealIdeasPerDay = 3,
                reportsPerWeek = 0
            )
        }

        return when (planType) {
            AIPlanType.FREE -> IntentQuotas(
                insightsPerDay = 0,
                coachMessagesPerDay = 10,
                whyExplanationsPerDay = 0,
                plansPerDay = 0,
                mealIdeasPerDay = 0,
                reportsPerWeek = 0
            )
            AIPlanType.MONTHLY,
            AIPlanType.LIFETIME -> IntentQuotas(
                insightsPerDay = 5,
                coachMessagesPerDay = 40,
                whyExplanationsPerDay = 10,
                plansPerDay = 3,
                mealIdeasPerDay = 10,
                reportsPerWeek = WEEKLY_REPORT_LIMIT_PER_WEEK
            )
            else -> IntentQuotas(
                insightsPerDay = 3,
                coachMessagesPerDay = 20,
                whyExplanationsPerDay = 5,
                plansPerDay = 2,
                mealIdeasPerDay = 5,
                reportsPerWeek = 1
            )
        }
    }

    fun maxTokensPerDay(planType: AIPlanType, isTrial: Boolean): Int {
        return when {
            isTrial -> MAX_TOKENS_PER_DAY_TRIAL
            planType == AIPlanType.FREE -> MAX_TOKENS_PER_DAY_FREE
            else -> Int.MAX_VALUE
        }
    }

    fun rawCloudCostUsd(model: AIModelUsed, inputTokens: Int, outputTokens: Int): Float {
        return when (model) {
            AIModelUsed.DECISION_TREE, AIModelUsed.QWEN_0_5B -> 0f
            AIModelUsed.CLAUDE_HAIKU ->
                (inputTokens * 1.00f + outputTokens * 5.00f) / 1_000_000
            AIModelUsed.CLAUDE_SONNET ->
                (inputTokens * 3.00f + outputTokens * 15.00f) / 1_000_000
            AIModelUsed.CLAUDE_OPUS ->
                (inputTokens * 15.00f + outputTokens * 75.00f) / 1_000_000
        }
    }

    fun chargedCloudCostUsd(model: AIModelUsed, inputTokens: Int, outputTokens: Int): Float {
        return rawCloudCostUsd(model, inputTokens, outputTokens) * INTERNAL_COST_MULTIPLIER
    }
}
