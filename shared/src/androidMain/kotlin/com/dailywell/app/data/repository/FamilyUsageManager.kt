package com.dailywell.app.data.repository

import com.dailywell.app.api.FirebaseService
import com.dailywell.app.data.local.DataStoreManager
import com.dailywell.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Family Usage Manager
 * Manages AI usage quotas for family plans with 3x owner budget.
 *
 * Family Plan: $99.99/year for 3 members
 * Monthly Budget: $99.99 Ã· 12 = $8.33/month total
 *
 * Budget Distribution (Owner gets 3x each member):
 * - Owner (60%): $5.00/month (soft $4.50, hard $5.00)
 * - Member 1 (20%): $1.67/month (soft $1.50, hard $1.67)
 * - Member 2 (20%): $1.67/month (soft $1.50, hard $1.67)
 *
 * After hard cap â†’ SLM-only until next month refresh (1st of each month)
 */
class FamilyUsageManager(
    private val dataStoreManager: DataStoreManager,
    private val firebaseService: FirebaseService
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    companion object {
        private const val FAMILY_USAGE_KEY = "family_member_usage"

        // Budget constants (USD/month)
        const val FAMILY_MONTHLY_BUDGET = 8.33f  // $99.99/year / 12
        const val OWNER_BUDGET_PERCENT = 0.60f   // 60% = $5.00 (3x member)
        const val MEMBER_BUDGET_PERCENT = 0.20f  // 20% = $1.67 each

        // Owner limits (3x member)
        const val OWNER_SOFT_CAP = 4.50f
        const val OWNER_HARD_CAP = 5.00f  // 60% of $8.33

        // Member limits
        const val MEMBER_SOFT_CAP = 1.50f
        const val MEMBER_HARD_CAP = 1.67f  // 20% of $8.33
    }

    private val _familyUsage = MutableStateFlow<FamilyMemberUsage?>(null)

    /**
     * Get the AI plan type based on family role
     */
    suspend fun getFamilyPlanType(): AIPlanType {
        val familyData = getFamilyData()
        return when {
            familyData == null -> AIPlanType.FREE  // Not in a family
            familyData.isOwner -> AIPlanType.FAMILY_OWNER
            else -> AIPlanType.FAMILY_MEMBER
        }
    }

    /**
     * Get budget limits for current user based on family role
     */
    suspend fun getBudgetLimits(): FamilyBudgetLimits {
        val familyData = getFamilyData()

        return when {
            familyData == null -> {
                // Not in a family - use individual plan limits
                FamilyBudgetLimits(
                    softCapUsd = 5.50f,
                    hardCapUsd = 6.00f,
                    monthlyBudgetUsd = 6.00f,
                    budgetPercent = 1.0f,
                    role = FamilyBudgetRole.INDIVIDUAL
                )
            }
            familyData.isOwner -> {
                // Owner gets 60% (3x member)
                FamilyBudgetLimits(
                    softCapUsd = OWNER_SOFT_CAP,
                    hardCapUsd = OWNER_HARD_CAP,
                    monthlyBudgetUsd = FAMILY_MONTHLY_BUDGET * OWNER_BUDGET_PERCENT,
                    budgetPercent = OWNER_BUDGET_PERCENT,
                    role = FamilyBudgetRole.OWNER
                )
            }
            else -> {
                // Member gets 20%
                FamilyBudgetLimits(
                    softCapUsd = MEMBER_SOFT_CAP,
                    hardCapUsd = MEMBER_HARD_CAP,
                    monthlyBudgetUsd = FAMILY_MONTHLY_BUDGET * MEMBER_BUDGET_PERCENT,
                    budgetPercent = MEMBER_BUDGET_PERCENT,
                    role = FamilyBudgetRole.MEMBER
                )
            }
        }
    }

    /**
     * Get current month's usage for the family member
     */
    fun getMemberUsage(): Flow<FamilyMemberUsage?> {
        return dataStoreManager.getString(FAMILY_USAGE_KEY).map { jsonString ->
            jsonString?.let {
                try {
                    json.decodeFromString<FamilyMemberUsage>(it)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    /**
     * Track AI usage for family member
     */
    suspend fun trackUsage(costUsd: Float, inputTokens: Int, outputTokens: Int, model: AIModelUsed) {
        val currentUsage = getMemberUsage().first() ?: createDefaultUsage()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        // Check if month has changed - reset if needed
        val usage = if (currentUsage.resetDate <= today.toString()) {
            resetMonthlyUsage()
            createDefaultUsage()
        } else {
            currentUsage
        }

        val updatedUsage = usage.copy(
            currentMonthCostUsd = usage.currentMonthCostUsd + costUsd,
            tokensUsed = usage.tokensUsed + inputTokens + outputTokens,
            messagesCount = usage.messagesCount + 1,
            cloudMessagesCount = if (!model.isFree) usage.cloudMessagesCount + 1 else usage.cloudMessagesCount,
            slmMessagesCount = if (model == AIModelUsed.QWEN_0_5B) usage.slmMessagesCount + 1 else usage.slmMessagesCount,
            freeMessagesCount = if (model == AIModelUsed.DECISION_TREE) usage.freeMessagesCount + 1 else usage.freeMessagesCount,
            lastUpdated = Clock.System.now().toString()
        )

        saveUsage(updatedUsage)

        // Sync to Firebase for family view
        syncUsageToFirebase(updatedUsage)
    }

    /**
     * Check if user should use SLM (hard cap reached)
     */
    suspend fun shouldUseSLM(): Boolean {
        val usage = getMemberUsage().first() ?: return false
        val limits = getBudgetLimits()
        return usage.currentMonthCostUsd >= limits.hardCapUsd
    }

    /**
     * Check if user is approaching limit (soft cap reached)
     */
    suspend fun isApproachingLimit(): Boolean {
        val usage = getMemberUsage().first() ?: return false
        val limits = getBudgetLimits()
        return usage.currentMonthCostUsd >= limits.softCapUsd
    }

    /**
     * Get remaining budget for the month
     */
    suspend fun getRemainingBudget(): Float {
        val usage = getMemberUsage().first() ?: return getBudgetLimits().hardCapUsd
        val limits = getBudgetLimits()
        return (limits.hardCapUsd - usage.currentMonthCostUsd).coerceAtLeast(0f)
    }

    /**
     * Get usage status for display
     */
    suspend fun getUsageStatus(): FamilyUsageStatus {
        val usage = getMemberUsage().first() ?: createDefaultUsage()
        val limits = getBudgetLimits()

        val percentUsed = if (limits.hardCapUsd > 0) {
            (usage.currentMonthCostUsd / limits.hardCapUsd * 100).coerceIn(0f, 100f)
        } else 100f

        return FamilyUsageStatus(
            currentCostUsd = usage.currentMonthCostUsd,
            budgetUsd = limits.hardCapUsd,
            percentUsed = percentUsed,
            remainingUsd = getRemainingBudget(),
            isAtSoftCap = isApproachingLimit(),
            isAtHardCap = shouldUseSLM(),
            role = limits.role,
            resetDate = usage.resetDate,
            messagesCount = usage.messagesCount,
            cloudMessagesCount = usage.cloudMessagesCount,
            slmMessagesCount = usage.slmMessagesCount,
            freeMessagesCount = usage.freeMessagesCount
        )
    }

    /**
     * Get family-wide usage summary (for owner)
     */
    suspend fun getFamilyUsageSummary(): FamilyUsageSummary? {
        val familyData = getFamilyData() ?: return null
        if (!familyData.isOwner) return null  // Only owner can see family summary

        val familyId = familyData.familyId ?: return null

        // Fetch all member usage from Firebase
        val memberUsages = firebaseService.getFamilyMemberUsages(familyId)

        val totalUsed = memberUsages.sumOf { it.currentMonthCostUsd.toDouble() }.toFloat()
        val remainingBudget = (FAMILY_MONTHLY_BUDGET - totalUsed).coerceAtLeast(0f)

        return FamilyUsageSummary(
            familyId = familyId,
            totalMonthlyBudget = FAMILY_MONTHLY_BUDGET,
            totalUsedUsd = totalUsed,
            remainingUsd = remainingBudget,
            memberUsages = memberUsages.map { usage ->
                val member = familyData.members.find { it.id == usage.userId }
                MemberUsageSummary(
                    memberId = usage.userId,
                    memberName = member?.name ?: "Unknown",
                    memberAvatar = member?.avatar ?: "ðŸ‘¤",
                    role = if (member?.role == FamilyRole.OWNER) FamilyBudgetRole.OWNER else FamilyBudgetRole.MEMBER,
                    usedUsd = usage.currentMonthCostUsd,
                    budgetUsd = if (member?.role == FamilyRole.OWNER) OWNER_HARD_CAP else MEMBER_HARD_CAP,
                    percentUsed = calculatePercent(usage.currentMonthCostUsd,
                        if (member?.role == FamilyRole.OWNER) OWNER_HARD_CAP else MEMBER_HARD_CAP)
                )
            }
        )
    }

    /**
     * Reset monthly usage (called on 1st of each month)
     */
    suspend fun resetMonthlyUsage() {
        val newUsage = createDefaultUsage()
        saveUsage(newUsage)
        syncUsageToFirebase(newUsage)
    }

    private fun createDefaultUsage(): FamilyMemberUsage {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val nextMonth = LocalDate(
            year = if (today.monthNumber == 12) today.year + 1 else today.year,
            monthNumber = if (today.monthNumber == 12) 1 else today.monthNumber + 1,
            dayOfMonth = 1
        )

        return FamilyMemberUsage(
            userId = firebaseService.getCurrentUserId() ?: "unknown",
            familyId = null,
            currentMonthCostUsd = 0f,
            tokensUsed = 0,
            messagesCount = 0,
            cloudMessagesCount = 0,
            slmMessagesCount = 0,
            freeMessagesCount = 0,
            resetDate = nextMonth.toString(),
            lastUpdated = Clock.System.now().toString()
        )
    }

    private suspend fun saveUsage(usage: FamilyMemberUsage) {
        dataStoreManager.putString(FAMILY_USAGE_KEY, json.encodeToString(usage))
        _familyUsage.value = usage
    }

    private suspend fun syncUsageToFirebase(usage: FamilyMemberUsage) {
        val familyData = getFamilyData()
        val familyId = familyData?.familyId ?: return

        try {
            firebaseService.updateFamilyMemberUsage(familyId, usage)
        } catch (e: Exception) {
            // Silently fail - local data is the source of truth
        }
    }

    private suspend fun getFamilyData(): FamilyPlanData? {
        return dataStoreManager.getString("family_plan_data").first()?.let {
            try {
                json.decodeFromString<FamilyPlanData>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun calculatePercent(used: Float, budget: Float): Float {
        return if (budget > 0) (used / budget * 100).coerceIn(0f, 100f) else 100f
    }
}

/**
 * Budget limits for a family member
 */
data class FamilyBudgetLimits(
    val softCapUsd: Float,
    val hardCapUsd: Float,
    val monthlyBudgetUsd: Float,
    val budgetPercent: Float,
    val role: FamilyBudgetRole
)

enum class FamilyBudgetRole {
    OWNER,      // 60% = $5.00 (3x member)
    MEMBER,     // 20% = $1.67
    INDIVIDUAL  // Not in family plan
}

/**
 * Per-member usage tracking
 */
@Serializable
data class FamilyMemberUsage(
    val userId: String,
    val familyId: String?,
    val currentMonthCostUsd: Float = 0f,
    val tokensUsed: Int = 0,
    val messagesCount: Int = 0,
    val cloudMessagesCount: Int = 0,
    val slmMessagesCount: Int = 0,
    val freeMessagesCount: Int = 0,
    val resetDate: String,
    val lastUpdated: String
)

/**
 * Usage status for display
 */
data class FamilyUsageStatus(
    val currentCostUsd: Float,
    val budgetUsd: Float,
    val percentUsed: Float,
    val remainingUsd: Float,
    val isAtSoftCap: Boolean,
    val isAtHardCap: Boolean,
    val role: FamilyBudgetRole,
    val resetDate: String,
    val messagesCount: Int,
    val cloudMessagesCount: Int,
    val slmMessagesCount: Int,
    val freeMessagesCount: Int
) {
    val statusMessage: String
        get() = when {
            isAtHardCap -> "Using on-device AI until $resetDate"
            isAtSoftCap -> "Approaching limit - $${"%.2f".format(remainingUsd)} remaining"
            else -> "$${"%.2f".format(remainingUsd)} of $${"%.2f".format(budgetUsd)} remaining"
        }

    val statusEmoji: String
        get() = when {
            isAtHardCap -> "âš ï¸"
            isAtSoftCap -> "ðŸŸ¡"
            percentUsed > 50 -> "ðŸŸ¢"
            else -> "âœ…"
        }
}

/**
 * Family-wide usage summary (visible to owner)
 */
data class FamilyUsageSummary(
    val familyId: String,
    val totalMonthlyBudget: Float,
    val totalUsedUsd: Float,
    val remainingUsd: Float,
    val memberUsages: List<MemberUsageSummary>
) {
    val percentUsed: Float
        get() = if (totalMonthlyBudget > 0) (totalUsedUsd / totalMonthlyBudget * 100) else 0f
}

data class MemberUsageSummary(
    val memberId: String,
    val memberName: String,
    val memberAvatar: String,
    val role: FamilyBudgetRole,
    val usedUsd: Float,
    val budgetUsd: Float,
    val percentUsed: Float
)

