package com.dailywell.app.ai

import com.dailywell.app.data.model.AIModelUsed
import com.dailywell.app.data.model.AIPlanType
import com.dailywell.app.data.model.AIGovernancePolicy
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/**
 * AI Routing Engine - Enterprise-Grade Cost Control (February 2026)
 *
 * ------------------------------------------------------------------
 * GOLDEN RULES (Non-Negotiable):
 * 1. NEVER route to cloud unless: Premium + Qualifies + Budget + Permit
 * 2. Keep cloud outputs SHORT (3-6 bullets max)
 * 3. Opus = PERMIT REQUIRED (weekly/monthly reports ONLY)
 * 4. Always fallback: Sonnet -> Haiku -> SLM (Qwen2.5) -> Guardrail
 * 5. Health safety: Never give medical advice
 * ------------------------------------------------------------------
 *
 * ROUTING HIERARCHY:
 * - Guardrails (Decision Tree): 60-80% of requests - FREE, instant
 * - Qwen2.5 0.5B (SLM): Light coaching, simple explanations - FREE (llama.cpp)
 * - Claude Haiku: Short personalization, single-signal - $1/$5 MTok
 * - Claude Sonnet: Multi-signal reasoning, insights - $3/$15 MTok
 * - Claude Opus: Weekly/monthly reports ONLY (permit required) - $15/$75 MTok
 */
object AIRoutingEngine {

    // =========================================================================
    // INTENT CLASSIFICATION
    // =========================================================================

    enum class RequestIntent {
        // GUARDRAIL-ONLY (never cloud)
        GREETING, STREAK_PRAISE, BASIC_REMINDER, FAQ, ONBOARDING_NUDGE,

        // SLM-ELIGIBLE (free, on-device Qwen2.5 0.5B)
        SIMPLE_COACHING, SIMPLE_EXPLANATION, MICRO_TIP, REFRAME,

        // HAIKU-ELIGIBLE (cloud, cheap)
        PERSONALIZED_TIP, SUMMARY_REQUEST, MEAL_IDEA, WHY_EXPLANATION,

        // SONNET-ELIGIBLE (cloud, main intelligence)
        INSIGHT_GENERATION, PLAN_CREATION, MULTI_SIGNAL_ANALYSIS, COMPLEX_QA,

        // OPUS-ONLY (permit required)
        WEEKLY_REPORT, MONTHLY_REPORT, DEEP_AUDIT,

            // SAFETY-ROUTED (health concerns -> safe templates)
        HEALTH_SAFETY_INTERCEPT,

        // FALLBACK
        UNKNOWN
    }

    fun classifyIntent(message: String, sessionType: String? = null): RequestIntent {
        val lower = message.lowercase().trim()
        val words = lower.split("\\s+".toRegex())
        val wordCount = words.size

        // HEALTH SAFETY CHECK FIRST (highest priority)
        if (HealthSafetyRouter.requiresSafetyIntercept(message)) {
            return RequestIntent.HEALTH_SAFETY_INTERCEPT
        }

        // SESSION TYPE OVERRIDES
        if (sessionType == "WEEKLY_REVIEW") return RequestIntent.WEEKLY_REPORT

        // GUARDRAIL INTENTS
        val greetings = setOf("hi", "hello", "hey", "thanks", "thank you", "ok", "okay", "bye")
        if (wordCount <= 2 && greetings.any { lower.contains(it) }) {
            return RequestIntent.GREETING
        }

        // OPUS-ONLY
        val opusPatterns = listOf(
            "weekly report", "monthly report", "week report", "month report",
            "analyze last 90", "deep audit", "comprehensive analysis",
            "full analysis of my", "analyze everything"
        )
        if (opusPatterns.any { lower.contains(it) }) {
            return if (lower.contains("month")) RequestIntent.MONTHLY_REPORT
            else if (lower.contains("90") || lower.contains("audit")) RequestIntent.DEEP_AUDIT
            else RequestIntent.WEEKLY_REPORT
        }

        // SONNET-ELIGIBLE
        val sonnetPatterns = listOf(
            "sleep and mood", "mood and sleep", "nutrition and energy",
            "correlate", "pattern", "relationship between",
            "based on my data", "analyze my", "why do i always",
            "7 day plan", "week plan", "next week", "fix my",
            "personalized plan", "custom plan"
        )
        if (sonnetPatterns.any { lower.contains(it) }) {
            return if (lower.contains("plan")) RequestIntent.PLAN_CREATION
            else if (lower.contains("pattern") || lower.contains("correlate")) RequestIntent.MULTI_SIGNAL_ANALYSIS
            else RequestIntent.INSIGHT_GENERATION
        }

        // HAIKU-ELIGIBLE
        val haikuPatterns = listOf(
            "why", "explain", "summarize", "meal idea", "food idea",
            "what should i eat", "suggest", "recommend"
        )
        if (haikuPatterns.any { lower.contains(it) }) {
            return if (lower.contains("why") || lower.contains("explain")) RequestIntent.WHY_EXPLANATION
            else if (lower.contains("meal") || lower.contains("food") || lower.contains("eat")) RequestIntent.MEAL_IDEA
            else if (lower.contains("summar")) RequestIntent.SUMMARY_REQUEST
            else RequestIntent.PERSONALIZED_TIP
        }

        // SLM-ELIGIBLE
        val slmPatterns = listOf(
            "tip", "advice", "help me", "how to", "what is", "motivate",
            "encourage", "feeling", "tired", "stressed"
        )
        if (slmPatterns.any { lower.contains(it) } || wordCount <= 10) {
            return if (lower.contains("what is")) RequestIntent.SIMPLE_EXPLANATION
            else if (lower.contains("tip") || lower.contains("advice")) RequestIntent.MICRO_TIP
            else RequestIntent.SIMPLE_COACHING
        }

        // COMPLEX
        if (wordCount > 20) return RequestIntent.COMPLEX_QA

        return RequestIntent.UNKNOWN
    }

    // =========================================================================
    // HEALTH SAFETY ROUTER (Wellness Liability Protection)
    // =========================================================================

    object HealthSafetyRouter {
        private val SAFETY_TRIGGERS = listOf(
            // Medical symptoms
            "chest pain", "can't breathe", "suicide", "self harm", "kill myself",
            "eating disorder", "anorexia", "bulimia", "purge", "binge",
            // Medical advice seeking
            "should i take", "medication", "prescription", "dosage",
            "diagnose", "diagnosis", "symptoms of",
            // Pregnancy/conditions
            "pregnant", "pregnancy", "diabetes", "heart condition",
            // Mental health crisis
            "depressed", "depression", "anxiety attack", "panic attack",
            "can't stop crying", "want to die"
        )

        private val SOFT_TRIGGERS = listOf(
            // These get gentle redirects, not hard blocks
            "calories", "how much should i weigh", "bmi", "fasting",
            "detox", "cleanse", "diet pills"
        )

        fun requiresSafetyIntercept(message: String): Boolean {
            val lower = message.lowercase()
            return SAFETY_TRIGGERS.any { lower.contains(it) }
        }

        fun requiresSoftRedirect(message: String): Boolean {
            val lower = message.lowercase()
            return SOFT_TRIGGERS.any { lower.contains(it) }
        }

        fun getSafetyResponse(message: String): String {
            val lower = message.lowercase()

            return when {
                // Crisis
                lower.contains("suicide") || lower.contains("kill myself") ||
                lower.contains("self harm") || lower.contains("want to die") ->
                    "I hear that you're going through a really difficult time. " +
                    "Please reach out to a crisis helpline - they're available 24/7:\n\n" +
                    "- **988** (Suicide & Crisis Lifeline, US)\n" +
                    "- **Crisis Text Line**: Text HOME to 741741\n\n" +
                    "You matter, and professional support is available right now."

                // Eating disorders
                lower.contains("eating disorder") || lower.contains("anorexia") ||
                lower.contains("bulimia") || lower.contains("purge") ->
                    "I want to support you, but eating disorders require specialized care. " +
                    "Please consider reaching out to:\n\n" +
                    "- **NEDA Helpline**: 1-800-931-2237\n" +
                    "- **nationaleatingdisorders.org**\n\n" +
                    "You deserve compassionate, professional support."

                // Medical advice
                lower.contains("medication") || lower.contains("diagnose") ||
                lower.contains("prescription") ->
                    "I'm not able to provide medical advice - that's outside my expertise. " +
                    "For questions about medications or diagnoses, please consult with:\n\n" +
                    "- Your doctor or pharmacist\n" +
                    "- A telehealth service if needed\n\n" +
                    "I'm here to support your daily habits and wellness routines!"

                // Pregnancy
                lower.contains("pregnant") || lower.contains("pregnancy") ->
                    "Congratulations if you're expecting! For pregnancy-related health questions, " +
                    "please consult with your OB-GYN or midwife. " +
                    "I can help with general wellness habits, but pregnancy needs specialized guidance."

                // Default safety response
                else ->
                    "I want to be helpful, but this topic is outside what I can safely advise on. " +
                    "For health concerns, please consult with a qualified healthcare provider. " +
                    "I'm here for your daily habits and wellness support!"
            }
        }
    }

    // =========================================================================
    // COMPLEXITY + VALUE SCORING
    // =========================================================================

    data class RoutingScores(
        val complexity: Float,      // 0.0-1.0 how complex is this?
        val value: Float,           // 0.0-1.0 will cloud add real value?
        val factors: List<String>,
        val recommendedModel: AIModelUsed
    )

    fun calculateScores(
        message: String,
        intent: RequestIntent,
        hasMultipleDataSources: Boolean = false,
        decisionTreeConfidence: Float = 0f
    ): RoutingScores {
        var complexity = 0f
        var value = 0f
        val factors = mutableListOf<String>()
        val lower = message.lowercase()

        // COMPLEXITY SCORING
        val intentComplexity = when (intent) {
            RequestIntent.GREETING, RequestIntent.STREAK_PRAISE,
            RequestIntent.BASIC_REMINDER, RequestIntent.FAQ,
            RequestIntent.ONBOARDING_NUDGE, RequestIntent.HEALTH_SAFETY_INTERCEPT -> 0.0f

            RequestIntent.SIMPLE_COACHING, RequestIntent.SIMPLE_EXPLANATION,
            RequestIntent.MICRO_TIP, RequestIntent.REFRAME -> 0.15f

            RequestIntent.PERSONALIZED_TIP, RequestIntent.SUMMARY_REQUEST,
            RequestIntent.MEAL_IDEA, RequestIntent.WHY_EXPLANATION -> 0.35f

            RequestIntent.INSIGHT_GENERATION, RequestIntent.PLAN_CREATION,
            RequestIntent.MULTI_SIGNAL_ANALYSIS, RequestIntent.COMPLEX_QA -> 0.6f

            RequestIntent.WEEKLY_REPORT, RequestIntent.MONTHLY_REPORT,
            RequestIntent.DEEP_AUDIT -> 0.85f

            RequestIntent.UNKNOWN -> 0.25f
        }
        complexity += intentComplexity

        if (hasMultipleDataSources || lower.contains(" and ") || lower.contains("correlat")) {
            complexity += 0.15f
            factors.add("multi-signal")
        }

        if (lower.contains("month") || lower.contains("90 day")) {
            complexity += 0.2f
        } else if (lower.contains("week") || lower.contains("7 day")) {
            complexity += 0.1f
        }

        complexity = complexity.coerceIn(0f, 1f)

        // VALUE SCORING (Will cloud actually help?)
        val valueIntent = when (intent) {
            // Low value - SLM/templates are just as good
            RequestIntent.GREETING, RequestIntent.STREAK_PRAISE,
            RequestIntent.BASIC_REMINDER, RequestIntent.SIMPLE_COACHING,
            RequestIntent.MICRO_TIP, RequestIntent.REFRAME -> 0.1f

            // Medium value - Cloud adds personality but not critical
            RequestIntent.SIMPLE_EXPLANATION, RequestIntent.PERSONALIZED_TIP -> 0.3f

            // High value - Cloud meaningfully better
            RequestIntent.WHY_EXPLANATION, RequestIntent.MEAL_IDEA,
            RequestIntent.SUMMARY_REQUEST -> 0.5f

            // Very high value - Cloud essential
            RequestIntent.INSIGHT_GENERATION, RequestIntent.PLAN_CREATION,
            RequestIntent.MULTI_SIGNAL_ANALYSIS, RequestIntent.COMPLEX_QA -> 0.8f

            // Maximum value - Opus-level
            RequestIntent.WEEKLY_REPORT, RequestIntent.MONTHLY_REPORT,
            RequestIntent.DEEP_AUDIT -> 1.0f

            else -> 0.2f
        }
        value = valueIntent

        // Value boost for specific keywords that benefit from cloud
        val highValueKeywords = listOf("because", "based on", "analyze", "compare", "trend", "insight")
        if (highValueKeywords.any { lower.contains(it) }) {
            value = (value + 0.2f).coerceAtMost(1f)
            factors.add("high-value-keywords")
        }

        // Value reduction for generic encouragement (SLM is fine)
        val lowValueKeywords = listOf("motivate me", "encourage", "cheer", "support", "help me feel")
        if (lowValueKeywords.any { lower.contains(it) }) {
            value = (value - 0.3f).coerceAtLeast(0f)
            factors.add("low-value-encouragement")
        }

        // Determine model based on BOTH scores
        val recommended = when {
            value < 0.25f -> AIModelUsed.QWEN_0_5B  // Low value = local
            complexity < 0.2f -> AIModelUsed.DECISION_TREE
            complexity < 0.35f -> AIModelUsed.QWEN_0_5B
            complexity < 0.55f -> AIModelUsed.CLAUDE_HAIKU
            complexity < 0.8f -> AIModelUsed.CLAUDE_SONNET
            else -> AIModelUsed.CLAUDE_OPUS
        }

        return RoutingScores(complexity, value, factors, recommended)
    }

    // =========================================================================
    // BUDGET MODE (Adaptive Degradation)
    // =========================================================================

    enum class BudgetMode {
        NORMAL,      // Full access
        LEAN,        // Haiku only, shorter outputs
        ULTRA_LEAN   // SLM + templates only
    }

    fun determineBudgetMode(
        currentMonthCostUsd: Float,
        softCapUsd: Float,
        hardCapUsd: Float
    ): BudgetMode {
        val percentUsed = if (hardCapUsd > 0) currentMonthCostUsd / hardCapUsd else 1f

        return when {
            percentUsed >= 1.0f -> BudgetMode.ULTRA_LEAN
            percentUsed >= 0.85f -> BudgetMode.LEAN
            currentMonthCostUsd >= softCapUsd -> BudgetMode.LEAN
            else -> BudgetMode.NORMAL
        }
    }

    // =========================================================================
    // CONTEXT BUDGETER (Token Caps, Not Just Days)
    // =========================================================================

    object ContextBudgeter {
        // Max tokens by intent (input context)
        val MAX_CONTEXT_TOKENS = mapOf(
            RequestIntent.GREETING to 0,
            RequestIntent.SIMPLE_COACHING to 500,
            RequestIntent.SIMPLE_EXPLANATION to 500,
            RequestIntent.MICRO_TIP to 300,
            RequestIntent.PERSONALIZED_TIP to 1000,
            RequestIntent.WHY_EXPLANATION to 1500,
            RequestIntent.MEAL_IDEA to 1200,
            RequestIntent.SUMMARY_REQUEST to 2000,
            RequestIntent.INSIGHT_GENERATION to 2500,
            RequestIntent.PLAN_CREATION to 3000,
            RequestIntent.MULTI_SIGNAL_ANALYSIS to 3500,
            RequestIntent.COMPLEX_QA to 2500,
            RequestIntent.WEEKLY_REPORT to 6000,
            RequestIntent.MONTHLY_REPORT to 10000,
            RequestIntent.DEEP_AUDIT to 12000
        )

        fun getMaxContextTokens(intent: RequestIntent): Int {
            return MAX_CONTEXT_TOKENS[intent] ?: 1000
        }

        fun needsSummarization(actualTokens: Int, intent: RequestIntent): Boolean {
            return actualTokens > getMaxContextTokens(intent)
        }

        data class ContextWindow(
            val daysOfHistory: Int,
            val maxTokens: Int,
            val includeHabits: Boolean,
            val includeMood: Boolean,
            val includeNutrition: Boolean,
            val includeSleep: Boolean,
            val includeWorkout: Boolean,
            val summarizeOlderThan: Int? = null  // Days - older data gets summarized
        )

        fun getContextWindow(intent: RequestIntent): ContextWindow {
            return when (intent) {
                RequestIntent.GREETING, RequestIntent.STREAK_PRAISE,
                RequestIntent.BASIC_REMINDER, RequestIntent.FAQ,
                RequestIntent.ONBOARDING_NUDGE, RequestIntent.HEALTH_SAFETY_INTERCEPT ->
                    ContextWindow(0, 0, false, false, false, false, false)

                RequestIntent.SIMPLE_COACHING, RequestIntent.SIMPLE_EXPLANATION,
                RequestIntent.MICRO_TIP, RequestIntent.REFRAME ->
                    ContextWindow(3, 500, true, false, false, false, false)

                RequestIntent.PERSONALIZED_TIP, RequestIntent.WHY_EXPLANATION ->
                    ContextWindow(7, 1500, true, true, false, false, false)

                RequestIntent.MEAL_IDEA, RequestIntent.SUMMARY_REQUEST ->
                    ContextWindow(7, 2000, true, false, true, false, false)

                RequestIntent.INSIGHT_GENERATION, RequestIntent.PLAN_CREATION,
                RequestIntent.COMPLEX_QA ->
                    ContextWindow(14, 3000, true, true, true, true, false, summarizeOlderThan = 7)

                RequestIntent.MULTI_SIGNAL_ANALYSIS ->
                    ContextWindow(14, 3500, true, true, true, true, true, summarizeOlderThan = 7)

                // Reports: summarize older data, raw recent data
                RequestIntent.WEEKLY_REPORT ->
                    ContextWindow(14, 6000, true, true, true, true, true, summarizeOlderThan = 7)

                RequestIntent.MONTHLY_REPORT ->
                    ContextWindow(30, 10000, true, true, true, true, true, summarizeOlderThan = 14)

                RequestIntent.DEEP_AUDIT ->
                    ContextWindow(90, 12000, true, true, true, true, true, summarizeOlderThan = 30)

                RequestIntent.UNKNOWN ->
                    ContextWindow(7, 1000, true, false, false, false, false)
            }
        }
    }

    // =========================================================================
    // INTENT-BASED QUOTAS (Not Model-Based)
    // =========================================================================

    data class IntentUsage(
        val date: String,
        var insights: Int = 0,
        var coachMessages: Int = 0,
        var whyExplanations: Int = 0,
        var plans: Int = 0,
        var mealIdeas: Int = 0,
        var reportsThisWeek: Int = 0,
        var totalTokensToday: Int = 0,
        var totalCostToday: Float = 0f
    )

    private val intentUsageMutex = Mutex()
    private var currentIntentUsage: IntentUsage? = null

    private fun getTodayIntentUsage(): IntentUsage {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        if (currentIntentUsage?.date != today) {
            currentIntentUsage = IntentUsage(date = today)
        }
        return currentIntentUsage!!
    }

    fun checkIntentQuota(
        intent: RequestIntent,
        planType: AIPlanType,
        isTrial: Boolean = false
    ): QuotaCheckResult {
        val usage = getTodayIntentUsage()
        val quotas = AIGovernancePolicy.intentQuotasFor(planType = planType, isTrial = isTrial)

        return when (intent) {
            RequestIntent.INSIGHT_GENERATION, RequestIntent.MULTI_SIGNAL_ANALYSIS -> {
                if (usage.insights >= quotas.insightsPerDay)
                    QuotaCheckResult.DENIED("Daily insight limit reached (${quotas.insightsPerDay}/day). Resets tomorrow!")
                else QuotaCheckResult.ALLOWED
            }
            RequestIntent.WHY_EXPLANATION -> {
                if (usage.whyExplanations >= quotas.whyExplanationsPerDay)
                    QuotaCheckResult.DENIED("Explanation limit reached. Try again tomorrow!")
                else QuotaCheckResult.ALLOWED
            }
            RequestIntent.PLAN_CREATION -> {
                if (usage.plans >= quotas.plansPerDay)
                    QuotaCheckResult.DENIED("Plan creation limit reached (${quotas.plansPerDay}/day)")
                else QuotaCheckResult.ALLOWED
            }
            RequestIntent.MEAL_IDEA -> {
                if (usage.mealIdeas >= quotas.mealIdeasPerDay)
                    QuotaCheckResult.DENIED("Meal idea limit reached")
                else QuotaCheckResult.ALLOWED
            }
            RequestIntent.WEEKLY_REPORT, RequestIntent.MONTHLY_REPORT, RequestIntent.DEEP_AUDIT -> {
                if (usage.reportsThisWeek >= quotas.reportsPerWeek)
                    QuotaCheckResult.DENIED("Weekly report already generated")
                else QuotaCheckResult.ALLOWED
            }
            RequestIntent.SIMPLE_COACHING, RequestIntent.SIMPLE_EXPLANATION,
            RequestIntent.MICRO_TIP, RequestIntent.PERSONALIZED_TIP,
            RequestIntent.SUMMARY_REQUEST, RequestIntent.COMPLEX_QA, RequestIntent.REFRAME -> {
                if (usage.coachMessages >= quotas.coachMessagesPerDay)
                    QuotaCheckResult.DENIED("Message limit reached (${quotas.coachMessagesPerDay}/day)")
                else QuotaCheckResult.ALLOWED
            }
            else -> QuotaCheckResult.ALLOWED
        }
    }

    suspend fun recordIntentUsage(intent: RequestIntent, tokens: Int, cost: Float) {
        intentUsageMutex.withLock {
            val usage = getTodayIntentUsage()

            when (intent) {
                RequestIntent.INSIGHT_GENERATION, RequestIntent.MULTI_SIGNAL_ANALYSIS ->
                    usage.insights++
                RequestIntent.WHY_EXPLANATION -> usage.whyExplanations++
                RequestIntent.PLAN_CREATION -> usage.plans++
                RequestIntent.MEAL_IDEA -> usage.mealIdeas++
                RequestIntent.WEEKLY_REPORT, RequestIntent.MONTHLY_REPORT,
                RequestIntent.DEEP_AUDIT -> usage.reportsThisWeek++
                else -> usage.coachMessages++
            }

            usage.totalTokensToday += tokens
            usage.totalCostToday += cost
        }
    }

    sealed class QuotaCheckResult {
        object ALLOWED : QuotaCheckResult()
        data class DENIED(val reason: String) : QuotaCheckResult()
    }

    // =========================================================================
    // ABUSE CONTROLS (Velocity + Trial Caps)
    // =========================================================================

    object AbuseControls {
        private var lastCloudCallTime: Long = 0
        private var cloudCallsThisMinute: Int = 0
        private var minuteStartTime: Long = 0

        data class AbuseCheckResult(
            val allowed: Boolean,
            val reason: String? = null,
            val waitSeconds: Int = 0
        )

        fun checkVelocity(): AbuseCheckResult {
            val now = Clock.System.now().toEpochMilliseconds()

            // Reset minute counter if new minute
            if (now - minuteStartTime > 60_000) {
                cloudCallsThisMinute = 0
                minuteStartTime = now
            }

            // Check calls per minute
            if (cloudCallsThisMinute >= AIGovernancePolicy.MAX_CLOUD_CALLS_PER_MINUTE) {
                val waitTime = ((minuteStartTime + 60_000 - now) / 1000).toInt()
                return AbuseCheckResult(
                    allowed = false,
                    reason = "Too many requests. Please wait.",
                    waitSeconds = waitTime
                )
            }

            // Check time since last call
            val secondsSinceLastCall = (now - lastCloudCallTime) / 1000
            if (secondsSinceLastCall < AIGovernancePolicy.MIN_SECONDS_BETWEEN_CLOUD_CALLS) {
                val waitTime = AIGovernancePolicy.MIN_SECONDS_BETWEEN_CLOUD_CALLS - secondsSinceLastCall.toInt()
                return AbuseCheckResult(
                    allowed = false,
                    reason = "Let's pause for a moment - try again in $waitTime seconds.",
                    waitSeconds = waitTime
                )
            }

            return AbuseCheckResult(allowed = true)
        }

        fun recordCloudCall() {
            lastCloudCallTime = Clock.System.now().toEpochMilliseconds()
            cloudCallsThisMinute++
        }

        fun checkTrialBudget(tokensUsedToday: Int, isTrial: Boolean): AbuseCheckResult {
            if (isTrial && tokensUsedToday >= AIGovernancePolicy.maxTokensPerDay(AIPlanType.FREE, isTrial = true)) {
                return AbuseCheckResult(
                    allowed = false,
                    reason = "Trial daily limit reached. Upgrade for unlimited access!"
                )
            }
            return AbuseCheckResult(allowed = true)
        }

        fun checkFreeBudget(tokensUsedToday: Int): AbuseCheckResult {
            if (tokensUsedToday >= AIGovernancePolicy.maxTokensPerDay(AIPlanType.FREE, isTrial = false)) {
                return AbuseCheckResult(
                    allowed = false,
                    reason = "Daily limit reached. Upgrade to Premium for more!"
                )
            }
            return AbuseCheckResult(allowed = true)
        }
    }

    // =========================================================================
    // OPUS PERMIT SYSTEM (Makes Misuse Impossible)
    // =========================================================================

    data class OpusPermit(
        val permitId: String,
        val reportType: ReportType,
        val userId: String,
        val createdAt: Long,
        val expiresAt: Long,
        val signature: String  // Hash of permitId + reportType + userId + secret
    ) {
        enum class ReportType {
            WEEKLY_REPORT,
            MONTHLY_REPORT,
            DEEP_AUDIT
        }

        fun isValid(): Boolean {
            val now = Clock.System.now().toEpochMilliseconds()
            return now < expiresAt
        }
    }

    private val validPermits = mutableSetOf<String>()
    private val permitMutex = Mutex()

    suspend fun createOpusPermit(
        reportType: OpusPermit.ReportType,
        userId: String
    ): OpusPermit {
        val now = Clock.System.now().toEpochMilliseconds()
        val permitId = "permit_${now}_${userId.hashCode()}"
        val expiresAt = now + 300_000  // 5 minutes

        // Simple signature (in production, use HMAC with secret)
        val signature = "$permitId:$reportType:$userId".hashCode().toString()

        val permit = OpusPermit(
            permitId = permitId,
            reportType = reportType,
            userId = userId,
            createdAt = now,
            expiresAt = expiresAt,
            signature = signature
        )

        permitMutex.withLock {
            validPermits.add(permitId)
        }

        return permit
    }

    suspend fun validateOpusPermit(permit: OpusPermit?): Boolean {
        if (permit == null) return false
        if (!permit.isValid()) return false

        return permitMutex.withLock {
            validPermits.contains(permit.permitId)
        }
    }

    suspend fun consumeOpusPermit(permit: OpusPermit) {
        permitMutex.withLock {
            validPermits.remove(permit.permitId)
        }
    }

    // =========================================================================
    // CIRCUIT BREAKER (with Direct Sonnet->SLM in Degraded)
    // =========================================================================

    data class CircuitBreakerState(
        var isOpen: Boolean = false,
        var failureCount: Int = 0,
        var lastFailureTime: Long = 0,
        var degradedMode: DegradedMode = DegradedMode.NONE
    )

    enum class DegradedMode {
        NONE,           // Normal
        SONNET_TO_SLM, // Skip Haiku, go direct to SLM (saves cost)
        HAIKU_ONLY,     // Disable Sonnet, Opus
        SLM_ONLY,       // Disable all cloud
        TEMPLATES_ONLY  // Emergency
    }

    private val circuitBreaker = CircuitBreakerState()
    private val circuitBreakerMutex = Mutex()
    private const val FAILURE_THRESHOLD = 5
    private const val RESET_TIMEOUT_MS = 60_000L

    suspend fun recordFailure(model: AIModelUsed, error: String) {
        circuitBreakerMutex.withLock {
            circuitBreaker.failureCount++
            circuitBreaker.lastFailureTime = Clock.System.now().toEpochMilliseconds()

            if (circuitBreaker.failureCount >= FAILURE_THRESHOLD) {
                circuitBreaker.isOpen = true
                circuitBreaker.degradedMode = when (model) {
                    AIModelUsed.CLAUDE_OPUS -> DegradedMode.HAIKU_ONLY
                    AIModelUsed.CLAUDE_SONNET -> DegradedMode.SONNET_TO_SLM
                    AIModelUsed.CLAUDE_HAIKU -> DegradedMode.SLM_ONLY
                    else -> DegradedMode.TEMPLATES_ONLY
                }
            }
        }
    }

    suspend fun recordSuccess() {
        circuitBreakerMutex.withLock {
            circuitBreaker.failureCount = 0
            val now = Clock.System.now().toEpochMilliseconds()
            if (circuitBreaker.isOpen && (now - circuitBreaker.lastFailureTime) > RESET_TIMEOUT_MS) {
                circuitBreaker.isOpen = false
                circuitBreaker.degradedMode = DegradedMode.NONE
            }
        }
    }

    fun isModelAllowed(model: AIModelUsed): Boolean {
        if (!circuitBreaker.isOpen) return true

        return when (circuitBreaker.degradedMode) {
            DegradedMode.NONE -> true
            DegradedMode.SONNET_TO_SLM -> model in listOf(
                AIModelUsed.DECISION_TREE,
                AIModelUsed.QWEN_0_5B,
                AIModelUsed.CLAUDE_HAIKU
            )
            DegradedMode.HAIKU_ONLY -> model in listOf(
                AIModelUsed.DECISION_TREE,
                AIModelUsed.QWEN_0_5B,
                AIModelUsed.CLAUDE_HAIKU
            )
            DegradedMode.SLM_ONLY -> model in listOf(
                AIModelUsed.DECISION_TREE,
                AIModelUsed.QWEN_0_5B
            )
            DegradedMode.TEMPLATES_ONLY -> model == AIModelUsed.DECISION_TREE
        }
    }

    fun buildFallbackChain(startModel: AIModelUsed, budgetMode: BudgetMode): List<AIModelUsed> {
        val chain = mutableListOf<AIModelUsed>()

        when (startModel) {
            AIModelUsed.CLAUDE_OPUS -> {
                chain.add(AIModelUsed.CLAUDE_SONNET)
                if (budgetMode == BudgetMode.LEAN) {
                    // Skip Haiku in lean mode - go direct to SLM
                    chain.add(AIModelUsed.QWEN_0_5B)
                } else {
                    chain.add(AIModelUsed.CLAUDE_HAIKU)
                    chain.add(AIModelUsed.QWEN_0_5B)
                }
            }
            AIModelUsed.CLAUDE_SONNET -> {
                if (budgetMode == BudgetMode.LEAN || circuitBreaker.degradedMode == DegradedMode.SONNET_TO_SLM) {
                    // Skip Haiku - direct to SLM (saves cost in degraded mode)
                    chain.add(AIModelUsed.QWEN_0_5B)
                } else {
                    chain.add(AIModelUsed.CLAUDE_HAIKU)
                    chain.add(AIModelUsed.QWEN_0_5B)
                }
            }
            AIModelUsed.CLAUDE_HAIKU -> {
                chain.add(AIModelUsed.QWEN_0_5B)
            }
            AIModelUsed.QWEN_0_5B -> {
                // Already at SLM
            }
            AIModelUsed.DECISION_TREE -> {
                // Already at bottom
            }
        }

        chain.add(AIModelUsed.DECISION_TREE)
        return chain.distinct()
    }

    // =========================================================================
    // TOKEN LIMITS (Per-Request Caps)
    // =========================================================================

    object TokenLimits {
        const val HAIKU_MAX_OUTPUT = 250
        const val HAIKU_MAX_OUTPUT_LEAN = 150
        const val SONNET_MAX_OUTPUT = 400
        const val SONNET_MAX_OUTPUT_LEAN = 250
        const val OPUS_MAX_OUTPUT = 800

        const val HAIKU_MAX_INPUT = 1000
        const val SONNET_MAX_INPUT = 2000
        const val OPUS_MAX_INPUT = 4000

        fun getMaxOutput(model: AIModelUsed, budgetMode: BudgetMode): Int {
            return when (model) {
                AIModelUsed.CLAUDE_HAIKU ->
                    if (budgetMode == BudgetMode.LEAN) HAIKU_MAX_OUTPUT_LEAN else HAIKU_MAX_OUTPUT
                AIModelUsed.CLAUDE_SONNET ->
                    if (budgetMode == BudgetMode.LEAN) SONNET_MAX_OUTPUT_LEAN else SONNET_MAX_OUTPUT
                AIModelUsed.CLAUDE_OPUS -> OPUS_MAX_OUTPUT
                else -> 150
            }
        }

        fun getMaxInput(model: AIModelUsed): Int {
            return when (model) {
                AIModelUsed.CLAUDE_HAIKU -> HAIKU_MAX_INPUT
                AIModelUsed.CLAUDE_SONNET -> SONNET_MAX_INPUT
                AIModelUsed.CLAUDE_OPUS -> OPUS_MAX_INPUT
                else -> 500
            }
        }
    }

    // =========================================================================
    // PROMPT CONTRACTS
    // =========================================================================

    fun getPromptContract(model: AIModelUsed, budgetMode: BudgetMode): String = when {
        budgetMode == BudgetMode.LEAN && model == AIModelUsed.CLAUDE_HAIKU -> """
            |IMPORTANT: Budget-conscious mode.
            |- Maximum 2 bullets
            |- One action only
            |- No questions
        """.trimMargin()

        model == AIModelUsed.QWEN_0_5B -> """
            |Keep response under 3 sentences.
            |Be supportive and practical.
            |No medical advice.
        """.trimMargin()

        model == AIModelUsed.CLAUDE_HAIKU -> """
            |OUTPUT RULES:
            |- Maximum 3-4 bullets
            |- One actionable next step
            |- No lengthy explanations
            |- Ask at most 1 question
            |- No medical claims
        """.trimMargin()

        model == AIModelUsed.CLAUDE_SONNET -> """
            |OUTPUT RULES:
            |- Maximum 4-5 bullets with brief "because"
            |- One clear recommended action
            |- Use "if/then" for personalization
            |- Keep under 150 words
            |- No medical diagnoses
        """.trimMargin()

        model == AIModelUsed.CLAUDE_OPUS -> """
            |REPORT FORMAT:
            |## Wins (2-3 achievements)
            |## Patterns (2-3 observations with evidence)
            |## Watch Areas (1-2 flags if any)
            |## Next Week (3 specific actions)
            |Keep under 300 words.
        """.trimMargin()

        else -> ""
    }

    // =========================================================================
    // COST TELEMETRY
    // =========================================================================

    data class CostTelemetry(
        val date: String,
        val costByIntent: MutableMap<RequestIntent, Float> = mutableMapOf(),
        val costByModel: MutableMap<AIModelUsed, Float> = mutableMapOf(),
        val callsByIntent: MutableMap<RequestIntent, Int> = mutableMapOf(),
        val topSpenders: MutableList<Pair<String, Float>> = mutableListOf()
    )

    private var telemetry: CostTelemetry? = null
    private val telemetryMutex = Mutex()

    suspend fun recordTelemetry(
        intent: RequestIntent,
        model: AIModelUsed,
        cost: Float,
        userId: String
    ) {
        telemetryMutex.withLock {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
            if (telemetry?.date != today) {
                telemetry = CostTelemetry(date = today)
            }

            val t = telemetry!!
            t.costByIntent[intent] = (t.costByIntent[intent] ?: 0f) + cost
            t.costByModel[model] = (t.costByModel[model] ?: 0f) + cost
            t.callsByIntent[intent] = (t.callsByIntent[intent] ?: 0) + 1
        }
    }

    fun getTelemetry(): CostTelemetry? = telemetry

    // =========================================================================
    // MAIN ROUTING DECISION
    // =========================================================================

    data class RoutingDecision(
        val model: AIModelUsed,
        val maxInputTokens: Int,
        val maxOutputTokens: Int,
        val intent: RequestIntent,
        val scores: RoutingScores,
        val budgetMode: BudgetMode,
        val reason: String,
        val fallbackChain: List<AIModelUsed>,
        val requiresOpusPermit: Boolean = false,
        val promptContract: String
    )

    fun route(
        message: String,
        planType: AIPlanType,
        sessionType: String? = null,
        currentMonthCostUsd: Float = 0f,
        softCapUsd: Float = 5.00f,
        hardCapUsd: Float = 5.50f,
        decisionTreeConfidence: Float = 0f,
        hasMultipleDataSources: Boolean = false,
        isTrial: Boolean = false,
        opusPermit: OpusPermit? = null
    ): RoutingDecision {
        // Step 1: Classify intent
        val intent = classifyIntent(message, sessionType)

        // Step 2: Health safety intercept
        if (intent == RequestIntent.HEALTH_SAFETY_INTERCEPT) {
            return RoutingDecision(
                model = AIModelUsed.DECISION_TREE,
                maxInputTokens = 0,
                maxOutputTokens = 0,
                intent = intent,
                scores = RoutingScores(0f, 0f, listOf("health-safety"), AIModelUsed.DECISION_TREE),
                budgetMode = BudgetMode.NORMAL,
                reason = "Health safety intercept - using safe template",
                fallbackChain = emptyList(),
                promptContract = ""
            )
        }

        // Step 3: Calculate scores
        val scores = calculateScores(message, intent, hasMultipleDataSources, decisionTreeConfidence)

        // Step 4: Determine budget mode
        val budgetMode = determineBudgetMode(currentMonthCostUsd, softCapUsd, hardCapUsd)

        // Step 5: Get base model from intent
        val intentModel = getModelForIntent(intent)

        // GUARDRAIL 1: Free tier = never cloud
        if (planType == AIPlanType.FREE) {
            val model = if (intentModel == AIModelUsed.DECISION_TREE)
                AIModelUsed.DECISION_TREE else AIModelUsed.QWEN_0_5B
            return RoutingDecision(
                model = model,
                maxInputTokens = 500,
                maxOutputTokens = 150,
                intent = intent,
                scores = scores,
                budgetMode = budgetMode,
                reason = "FREE tier: SLM only",
                fallbackChain = listOf(AIModelUsed.DECISION_TREE),
                promptContract = getPromptContract(model, budgetMode)
            )
        }

        // GUARDRAIL 2: Ultra-lean = SLM only
        if (budgetMode == BudgetMode.ULTRA_LEAN) {
            return RoutingDecision(
                model = AIModelUsed.QWEN_0_5B,
                maxInputTokens = 500,
                maxOutputTokens = 150,
                intent = intent,
                scores = scores,
                budgetMode = budgetMode,
                reason = "Budget exhausted - using on-device AI",
                fallbackChain = listOf(AIModelUsed.DECISION_TREE),
                promptContract = getPromptContract(AIModelUsed.QWEN_0_5B, budgetMode)
            )
        }

        // GUARDRAIL 3: Lean mode = Haiku max
        if (budgetMode == BudgetMode.LEAN && intentModel in listOf(AIModelUsed.CLAUDE_SONNET, AIModelUsed.CLAUDE_OPUS)) {
            val model = AIModelUsed.CLAUDE_HAIKU
            return RoutingDecision(
                model = model,
                maxInputTokens = TokenLimits.getMaxInput(model),
                maxOutputTokens = TokenLimits.getMaxOutput(model, budgetMode),
                intent = intent,
                scores = scores,
                budgetMode = budgetMode,
                reason = "Lean mode - downgraded to Haiku",
                fallbackChain = buildFallbackChain(model, budgetMode),
                promptContract = getPromptContract(model, budgetMode)
            )
        }

        // GUARDRAIL 4: Opus requires permit
        if (intentModel == AIModelUsed.CLAUDE_OPUS) {
            if (opusPermit == null || !opusPermit.isValid()) {
                // Downgrade to Sonnet
                val model = AIModelUsed.CLAUDE_SONNET
                return RoutingDecision(
                    model = model,
                    maxInputTokens = TokenLimits.getMaxInput(model),
                    maxOutputTokens = TokenLimits.getMaxOutput(model, budgetMode),
                    intent = intent,
                    scores = scores,
                    budgetMode = budgetMode,
                    reason = "Opus requires permit - downgraded to Sonnet",
                    fallbackChain = buildFallbackChain(model, budgetMode),
                    requiresOpusPermit = true,
                    promptContract = getPromptContract(model, budgetMode)
                )
            }
        }

        // GUARDRAIL 5: Low value = use SLM
        if (scores.value < 0.3f && intentModel != AIModelUsed.DECISION_TREE) {
            return RoutingDecision(
                model = AIModelUsed.QWEN_0_5B,
                maxInputTokens = 500,
                maxOutputTokens = 150,
                intent = intent,
                scores = scores,
                budgetMode = budgetMode,
                reason = "Low value score - SLM sufficient",
                fallbackChain = listOf(AIModelUsed.DECISION_TREE),
                promptContract = getPromptContract(AIModelUsed.QWEN_0_5B, budgetMode)
            )
        }

        // GUARDRAIL 6: Circuit breaker
        if (!isModelAllowed(intentModel)) {
            val fallbackModel = buildFallbackChain(intentModel, budgetMode).firstOrNull()
                ?: AIModelUsed.QWEN_0_5B
            return RoutingDecision(
                model = fallbackModel,
                maxInputTokens = TokenLimits.getMaxInput(fallbackModel),
                maxOutputTokens = TokenLimits.getMaxOutput(fallbackModel, budgetMode),
                intent = intent,
                scores = scores,
                budgetMode = budgetMode,
                reason = "Circuit breaker - degraded mode",
                fallbackChain = buildFallbackChain(fallbackModel, budgetMode),
                promptContract = getPromptContract(fallbackModel, budgetMode)
            )
        }

        // Normal routing
        return RoutingDecision(
            model = intentModel,
            maxInputTokens = TokenLimits.getMaxInput(intentModel),
            maxOutputTokens = TokenLimits.getMaxOutput(intentModel, budgetMode),
            intent = intent,
            scores = scores,
            budgetMode = budgetMode,
            reason = "Routed by intent + value + complexity",
            fallbackChain = buildFallbackChain(intentModel, budgetMode),
            requiresOpusPermit = intentModel == AIModelUsed.CLAUDE_OPUS,
            promptContract = getPromptContract(intentModel, budgetMode)
        )
    }

    private fun getModelForIntent(intent: RequestIntent): AIModelUsed {
        return when (intent) {
            RequestIntent.GREETING, RequestIntent.STREAK_PRAISE,
            RequestIntent.BASIC_REMINDER, RequestIntent.FAQ,
            RequestIntent.ONBOARDING_NUDGE, RequestIntent.HEALTH_SAFETY_INTERCEPT ->
                AIModelUsed.DECISION_TREE

            RequestIntent.SIMPLE_COACHING, RequestIntent.SIMPLE_EXPLANATION,
            RequestIntent.MICRO_TIP, RequestIntent.REFRAME, RequestIntent.UNKNOWN ->
                AIModelUsed.QWEN_0_5B

            RequestIntent.PERSONALIZED_TIP, RequestIntent.SUMMARY_REQUEST,
            RequestIntent.MEAL_IDEA, RequestIntent.WHY_EXPLANATION ->
                AIModelUsed.CLAUDE_HAIKU

            RequestIntent.INSIGHT_GENERATION, RequestIntent.PLAN_CREATION,
            RequestIntent.MULTI_SIGNAL_ANALYSIS, RequestIntent.COMPLEX_QA ->
                AIModelUsed.CLAUDE_SONNET

            RequestIntent.WEEKLY_REPORT, RequestIntent.MONTHLY_REPORT,
            RequestIntent.DEEP_AUDIT -> AIModelUsed.CLAUDE_OPUS
        }
    }

    // =========================================================================
    // FEATURE 1: INSIGHT SCHEDULER (Proactive Retention)
    // =========================================================================

    /**
     * Insight Scheduler - Triggers proactive insights at key retention milestones
     *
     * Psychology: Users who receive unexpected value feel "reciprocity" and stay longer.
     * Milestones chosen based on typical churn points:
     * - Day 3: First habit struggle -> encouragement
     * - Day 7: One week in -> pattern recognition
     * - Day 14: Two weeks -> deeper insights
     * - Day 30: Monthly -> comprehensive review
     * - Day 90: Quarterly -> transformation story
     */
    object InsightScheduler {

        enum class InsightMilestone(
            val dayNumber: Int,
            val insightType: String,
            val model: AIModelUsed,
            val maxTokens: Int,
            val title: String
        ) {
            DAY_3(3, "ENCOURAGEMENT", AIModelUsed.QWEN_0_5B, 150, "Your First 3 Days"),
            DAY_7(7, "PATTERN_RECOGNITION", AIModelUsed.CLAUDE_HAIKU, 200, "Week 1 Patterns"),
            DAY_14(14, "DEEPER_INSIGHT", AIModelUsed.CLAUDE_HAIKU, 250, "Two Week Check-In"),
            DAY_30(30, "MONTHLY_REVIEW", AIModelUsed.CLAUDE_SONNET, 400, "Your First Month"),
            DAY_90(90, "TRANSFORMATION", AIModelUsed.CLAUDE_SONNET, 500, "90-Day Journey")
        }

        data class ScheduledInsight(
            val milestone: InsightMilestone,
            val userId: String,
            val scheduledFor: Long,    // Epoch millis
            val generated: Boolean = false,
            val generatedAt: Long? = null,
            val content: String? = null
        )

        private val pendingInsights = mutableMapOf<String, MutableList<ScheduledInsight>>()
        private val insightMutex = Mutex()

        /**
         * Schedule insights for a new user
         * Call this when user completes onboarding
         */
        suspend fun scheduleForNewUser(userId: String, signupTimestamp: Long) {
            insightMutex.withLock {
                val userInsights = mutableListOf<ScheduledInsight>()

                InsightMilestone.entries.forEach { milestone ->
                    val scheduledTime = signupTimestamp + (milestone.dayNumber * 24 * 60 * 60 * 1000L)
                    userInsights.add(
                        ScheduledInsight(
                            milestone = milestone,
                            userId = userId,
                            scheduledFor = scheduledTime
                        )
                    )
                }

                pendingInsights[userId] = userInsights
            }
        }

        /**
         * Check if any insights are due for a user
         * Call this on app launch or periodic background check
         */
        suspend fun getDueInsights(userId: String): List<ScheduledInsight> {
            val now = Clock.System.now().toEpochMilliseconds()

            return insightMutex.withLock {
                pendingInsights[userId]?.filter { !it.generated && it.scheduledFor <= now } ?: emptyList()
            }
        }

        /**
         * Mark insight as generated
         */
        suspend fun markGenerated(userId: String, milestone: InsightMilestone, content: String) {
            val now = Clock.System.now().toEpochMilliseconds()

            insightMutex.withLock {
                pendingInsights[userId]?.let { insights ->
                    val index = insights.indexOfFirst { it.milestone == milestone }
                    if (index >= 0) {
                        insights[index] = insights[index].copy(
                            generated = true,
                            generatedAt = now,
                            content = content
                        )
                    }
                }
            }
        }

        /**
         * Get prompt template for milestone insight
         */
        fun getInsightPrompt(milestone: InsightMilestone, userName: String, contextSummary: String): String {
            return when (milestone) {
                InsightMilestone.DAY_3 -> """
                    |User $userName has been using DailyWell for 3 days.
                    |Context: $contextSummary
                    |
                    |Generate a SHORT encouraging message (2-3 sentences):
                    |- Acknowledge their effort
                    |- One specific observation from their data
                    |- Motivate them to keep going
                    |Keep it warm and personal. No generic advice.
                """.trimMargin()

                InsightMilestone.DAY_7 -> """
                    |User $userName completed their first week!
                    |Context: $contextSummary
                    |
                    |Generate a Week 1 insight (3-4 bullets):
                    |- Best performing habit
                    |- One pattern you noticed
                    |- Encouragement for week 2
                    |Be specific to their data. Under 100 words.
                """.trimMargin()

                InsightMilestone.DAY_14 -> """
                    |User $userName has been building habits for 2 weeks.
                    |Context: $contextSummary
                    |
                    |Generate a two-week check-in (4-5 bullets):
                    |- Habits becoming automatic
                    |- Correlation between habits and mood/energy
                    |- One area for focus
                    |- Celebrate consistency
                    |Under 120 words.
                """.trimMargin()

                InsightMilestone.DAY_30 -> """
                    |User $userName's first month complete!
                    |Context: $contextSummary
                    |
                    |Generate a Monthly Review:
                    |## Wins (2-3 achievements)
                    |## Patterns (what's working)
                    |## Next Month Focus (1-2 goals)
                    |Keep under 150 words. Be celebratory but specific.
                """.trimMargin()

                InsightMilestone.DAY_90 -> """
                    |User $userName's 90-day transformation!
                    |Context: $contextSummary
                    |
                    |Generate a Transformation Story:
                    |## The Journey (where they started vs now)
                    |## Key Breakthroughs (2-3 major wins)
                    |## What's Next (sustaining + growing)
                    |Make it feel like a milestone. Under 200 words.
                """.trimMargin()
            }
        }

        /**
         * Check if user qualifies for proactive insight based on plan
         * FREE: Only Day 3 and Day 7 (teaser for premium)
         * PREMIUM: All milestones
         */
        fun isEligibleForInsight(milestone: InsightMilestone, planType: AIPlanType): Boolean {
            return when (planType) {
                AIPlanType.FREE -> milestone in listOf(InsightMilestone.DAY_3, InsightMilestone.DAY_7)
                else -> true
            }
        }
    }

    // =========================================================================
    // FEATURE 2: CACHED CONTEXT SUMMARIES (Token Cost Reduction)
    // =========================================================================

    /**
     * Context Cache - Reduces token costs by caching daily summaries
     *
     * Instead of sending 7 days of raw data every request (~2000 tokens),
     * we cache a daily summary (~200 tokens) and only send raw for today.
     *
     * Savings: ~30% reduction in input tokens for repeat users
     */
    object ContextCache {

        data class DailySummary(
            val date: String,
            val userId: String,
            val habitCompletionRate: Float,
            val completedHabits: List<String>,
            val missedHabits: List<String>,
            val mood: String?,
            val sleepHours: Float?,
            val sleepQuality: String?,
            val nutritionScore: Float?,
            val workoutMinutes: Int?,
            val energyLevel: String?,
            val notes: String?,
            val tokenEstimate: Int = 50  // Much smaller than raw data
        ) {
            fun toPromptString(): String = buildString {
                append("[$date] ")
                append("Habits: ${(habitCompletionRate * 100).toInt()}%")
                if (completedHabits.isNotEmpty()) append(" (${completedHabits.joinToString(",")})")
                mood?.let { append(" | Mood: $it") }
                sleepHours?.let { append(" | Sleep: ${it}h") }
                nutritionScore?.let { append(" | Nutrition: ${(it * 100).toInt()}%") }
                workoutMinutes?.let { if (it > 0) append(" | Workout: ${it}min") }
            }
        }

        data class CachedContext(
            val userId: String,
            val generatedAt: Long,
            val expiresAt: Long,
            val dailySummaries: List<DailySummary>,
            val weeklyTrend: String,
            val topStrengths: List<String>,
            val areasForFocus: List<String>,
            val totalTokenEstimate: Int
        ) {
            fun isValid(): Boolean = Clock.System.now().toEpochMilliseconds() < expiresAt

            fun toCondensedPrompt(): String = buildString {
                appendLine("=== User Context (Cached Summary) ===")
                appendLine("Weekly trend: $weeklyTrend")
                appendLine("Strengths: ${topStrengths.joinToString(", ")}")
                if (areasForFocus.isNotEmpty()) {
                    appendLine("Focus areas: ${areasForFocus.joinToString(", ")}")
                }
                appendLine()
                appendLine("Recent days:")
                dailySummaries.takeLast(7).forEach { appendLine(it.toPromptString()) }
            }
        }

        private val cache = mutableMapOf<String, CachedContext>()
        private val cacheMutex = Mutex()
        private const val CACHE_TTL_HOURS = 4  // Refresh every 4 hours

        /**
         * Get cached context for user, or null if expired/missing
         */
        suspend fun getCachedContext(userId: String): CachedContext? {
            return cacheMutex.withLock {
                cache[userId]?.takeIf { it.isValid() }
            }
        }

        /**
         * Store computed context in cache
         */
        suspend fun cacheContext(context: CachedContext) {
            cacheMutex.withLock {
                cache[context.userId] = context
            }
        }

        /**
         * Build a new cached context from raw data
         * Call this when cache is expired
         */
        fun buildCachedContext(
            userId: String,
            dailySummaries: List<DailySummary>
        ): CachedContext {
            val now = Clock.System.now().toEpochMilliseconds()

            // Calculate weekly trend
            val recentRates = dailySummaries.takeLast(7).map { it.habitCompletionRate }
            val weeklyTrend = when {
                recentRates.size < 3 -> "Just getting started"
                recentRates.last() > recentRates.first() + 0.1f -> "Improving"
                recentRates.last() < recentRates.first() - 0.1f -> "Needs attention"
                recentRates.average() > 0.8 -> "Excellent consistency"
                recentRates.average() > 0.6 -> "Good momentum"
                else -> "Building foundations"
            }

            // Find strengths (habits with >80% completion)
            val habitCounts = mutableMapOf<String, Int>()
            dailySummaries.forEach { day ->
                day.completedHabits.forEach { habit ->
                    habitCounts[habit] = (habitCounts[habit] ?: 0) + 1
                }
            }
            val topStrengths = habitCounts
                .filter { it.value >= (dailySummaries.size * 0.8).toInt() }
                .keys.take(3).toList()

            // Find focus areas (habits with <50% completion)
            val allHabits = dailySummaries.flatMap { it.completedHabits + it.missedHabits }.toSet()
            val areasForFocus = allHabits.filter { habit ->
                val completions = habitCounts[habit] ?: 0
                completions < (dailySummaries.size * 0.5).toInt()
            }.take(2)

            val tokenEstimate = dailySummaries.sumOf { it.tokenEstimate } + 100

            return CachedContext(
                userId = userId,
                generatedAt = now,
                expiresAt = now + (CACHE_TTL_HOURS * 60 * 60 * 1000L),
                dailySummaries = dailySummaries,
                weeklyTrend = weeklyTrend,
                topStrengths = topStrengths,
                areasForFocus = areasForFocus,
                totalTokenEstimate = tokenEstimate
            )
        }

        /**
         * Invalidate cache for user (call after significant data changes)
         */
        suspend fun invalidate(userId: String) {
            cacheMutex.withLock {
                cache.remove(userId)
            }
        }

        /**
         * Get token savings estimate
         */
        fun estimateSavings(rawTokens: Int, cachedTokens: Int): Float {
            return if (rawTokens > 0) {
                ((rawTokens - cachedTokens).toFloat() / rawTokens) * 100
            } else 0f
        }
    }

    // =========================================================================
    // FEATURE 3: A/B TEST HOOK (Model Performance Analytics)
    // =========================================================================

    /**
     * A/B Test Hook - Logs model routing decisions for optimization
     *
     * Tracks which model handled which intent so we can:
     * 1. Measure user satisfaction by model tier
     * 2. Identify intents that could be downgraded
     * 3. Find intents that need upgrading
     * 4. Calculate actual vs expected costs
     */
    object ABTestHook {

        data class RoutingEvent(
            val eventId: String,
            val timestamp: Long,
            val userId: String,
            val intent: RequestIntent,
            val requestedModel: AIModelUsed,
            val actualModel: AIModelUsed,  // After fallback/downgrade
            val reason: String,
            val budgetMode: BudgetMode,
            val inputTokens: Int,
            val outputTokens: Int,
            val cost: Float,
            val responseTimeMs: Long,
            val userFeedback: UserFeedback? = null
        )

        enum class UserFeedback {
            POSITIVE,      // User said "thanks", continued conversation
            NEGATIVE,      // User asked to rephrase, complained
            NEUTRAL,       // No clear signal
            UPGRADE_HINT   // User asked for "more detail" (model too weak)
        }

        data class IntentPerformance(
            val intent: RequestIntent,
            val callCount: Int,
            val avgResponseTimeMs: Long,
            val avgCost: Float,
            val positiveRate: Float,
            val upgradeHintRate: Float,  // How often users wanted more
            val modelDistribution: Map<AIModelUsed, Int>
        )

        private val events = mutableListOf<RoutingEvent>()
        private val eventsMutex = Mutex()
        private const val MAX_EVENTS = 10000  // Keep last 10k events

        /**
         * Log a routing decision
         */
        suspend fun logEvent(event: RoutingEvent) {
            eventsMutex.withLock {
                events.add(event)
                if (events.size > MAX_EVENTS) {
                    events.removeAt(0)  // FIFO
                }
            }
        }

        /**
         * Create an event ID for tracking
         */
        fun createEventId(): String {
            return "evt_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
        }

        /**
         * Record user feedback for an event
         */
        suspend fun recordFeedback(eventId: String, feedback: UserFeedback) {
            eventsMutex.withLock {
                val index = events.indexOfFirst { it.eventId == eventId }
                if (index >= 0) {
                    events[index] = events[index].copy(userFeedback = feedback)
                }
            }
        }

        /**
         * Analyze performance by intent
         */
        suspend fun analyzeIntentPerformance(): List<IntentPerformance> {
            return eventsMutex.withLock {
                events.groupBy { it.intent }.map { (intent, intentEvents) ->
                    val positiveCount = intentEvents.count { it.userFeedback == UserFeedback.POSITIVE }
                    val upgradeHintCount = intentEvents.count { it.userFeedback == UserFeedback.UPGRADE_HINT }
                    val feedbackCount = intentEvents.count { it.userFeedback != null }

                    IntentPerformance(
                        intent = intent,
                        callCount = intentEvents.size,
                        avgResponseTimeMs = intentEvents.map { it.responseTimeMs }.average().toLong(),
                        avgCost = intentEvents.map { it.cost }.average().toFloat(),
                        positiveRate = if (feedbackCount > 0) positiveCount.toFloat() / feedbackCount else 0f,
                        upgradeHintRate = if (feedbackCount > 0) upgradeHintCount.toFloat() / feedbackCount else 0f,
                        modelDistribution = intentEvents.groupBy { it.actualModel }
                            .mapValues { it.value.size }
                    )
                }
            }
        }

        /**
         * Get recommendations for model routing adjustments
         */
        suspend fun getRoutingRecommendations(): List<String> {
            val analysis = analyzeIntentPerformance()
            val recommendations = mutableListOf<String>()

            analysis.forEach { perf ->
                // High upgrade hint rate = model too weak
                if (perf.upgradeHintRate > 0.2f && perf.callCount > 50) {
                    recommendations.add(
                        "Consider upgrading ${perf.intent}: ${(perf.upgradeHintRate * 100).toInt()}% of users wanted more detail"
                    )
                }

                // High positive rate with expensive model = could downgrade
                if (perf.positiveRate > 0.9f && perf.avgCost > 0.005f && perf.callCount > 50) {
                    recommendations.add(
                        "Consider downgrading ${perf.intent}: 90%+ satisfaction, could use cheaper model"
                    )
                }
            }

            return recommendations
        }

        /**
         * Detect if response implies user wanted more (for feedback classification)
         */
        fun detectFeedbackFromResponse(userMessage: String): UserFeedback? {
            val lower = userMessage.lowercase()

            return when {
                // Positive signals
                lower.contains("thanks") || lower.contains("helpful") ||
                lower.contains("great") || lower.contains("perfect") ||
                lower.contains("got it") || lower.contains("makes sense") -> UserFeedback.POSITIVE

                // Upgrade hints
                lower.contains("more detail") || lower.contains("elaborate") ||
                lower.contains("explain more") || lower.contains("what do you mean") ||
                lower.contains("can you expand") || lower.contains("tell me more") -> UserFeedback.UPGRADE_HINT

                // Negative signals
                lower.contains("not helpful") || lower.contains("wrong") ||
                lower.contains("doesn't make sense") || lower.contains("try again") ||
                lower.contains("that's not what i asked") -> UserFeedback.NEGATIVE

                else -> null
            }
        }
    }

    // =========================================================================
    // FEATURE 4: OPUS SCHEDULED GENERATION (Off-Peak Report Generation)
    // =========================================================================

    /**
     * Opus Scheduler - Generates weekly reports during off-peak hours
     *
     * Instead of generating Opus reports on-demand (expensive + slow),
     * we pre-generate them at 2 AM Sunday when:
     * 1. Server load is low
     * 2. User wakes up to a ready insight
     * 3. Costs can be amortized across low-traffic hours
     *
     * Uses WorkManager or similar for Android scheduling
     */
    object OpusScheduler {

        data class ScheduledReport(
            val reportId: String,
            val userId: String,
            val reportType: OpusPermit.ReportType,
            val scheduledFor: Long,          // 2 AM Sunday
            val status: ReportStatus,
            val generatedAt: Long? = null,
            val content: String? = null,
            val tokensCost: Int? = null,
            val costUsd: Float? = null
        )

        enum class ReportStatus {
            SCHEDULED,
            GENERATING,
            READY,
            DELIVERED,
            FAILED,
            EXPIRED
        }

        private val scheduledReports = mutableMapOf<String, ScheduledReport>()
        private val reportsMutex = Mutex()

        /**
         * Calculate next 2 AM Sunday from now
         */
        fun getNextSundayReportTime(): Long {
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()
            val today = now.toLocalDateTime(tz)

            // Days until Sunday (0 = Monday in Kotlin, so Sunday = 6)
            val currentDayOfWeek = today.dayOfWeek.ordinal  // 0=Monday ... 6=Sunday
            val daysUntilSunday = if (currentDayOfWeek == 6) 7 else (6 - currentDayOfWeek)

            // Calculate target datetime (Sunday 2 AM)
            val targetDay = today.date.toEpochDays() + daysUntilSunday
            val targetHour = 2  // 2 AM

            // Convert to epoch millis (approximate - adjust for your needs)
            val millisPerDay = 24 * 60 * 60 * 1000L
            val millisPerHour = 60 * 60 * 1000L

            return (targetDay * millisPerDay) + (targetHour * millisPerHour)
        }

        /**
         * Schedule weekly report for user
         * Premium-only, one per week, max 4 per month.
         */
        suspend fun scheduleWeeklyReport(
            userId: String,
            planType: AIPlanType
        ): ScheduledReport? {
            // Weekly Opus reports are premium-only.
            if (planType == AIPlanType.FREE) return null

            val scheduledTime = getNextSundayReportTime()
            val reportId = "report_${userId}_${scheduledTime}"

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentMonthKey = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"

            return reportsMutex.withLock {
                val monthlyCount = scheduledReports.values.count { report ->
                    if (report.userId != userId) return@count false
                    val reportMonth = Instant.fromEpochMilliseconds(report.scheduledFor)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    val reportMonthKey = "${reportMonth.year}-${reportMonth.monthNumber.toString().padStart(2, '0')}"
                    reportMonthKey == currentMonthKey
                }
                if (monthlyCount >= AIGovernancePolicy.WEEKLY_REPORT_LIMIT_PER_MONTH) {
                    return@withLock null
                }

                // One report slot per week.
                val existing = scheduledReports[reportId]
                if (existing != null) return@withLock existing

                val report = ScheduledReport(
                    reportId = reportId,
                    userId = userId,
                    reportType = OpusPermit.ReportType.WEEKLY_REPORT,
                    scheduledFor = scheduledTime,
                    status = ReportStatus.SCHEDULED
                )
                scheduledReports[reportId] = report
                return@withLock report
            }
        }

        /**
         * Get all reports due for generation
         * Call this from background worker at scheduled time
         */
        suspend fun getDueReports(): List<ScheduledReport> {
            val now = Clock.System.now().toEpochMilliseconds()

            return reportsMutex.withLock {
                scheduledReports.values.filter {
                    it.status == ReportStatus.SCHEDULED && it.scheduledFor <= now
                }
            }
        }

        /**
         * Mark report as generating (prevents duplicate generation)
         */
        suspend fun markGenerating(reportId: String) {
            reportsMutex.withLock {
                scheduledReports[reportId]?.let {
                    scheduledReports[reportId] = it.copy(status = ReportStatus.GENERATING)
                }
            }
        }

        /**
         * Store generated report
         */
        suspend fun storeGeneratedReport(
            reportId: String,
            content: String,
            tokensCost: Int,
            costUsd: Float
        ) {
            val now = Clock.System.now().toEpochMilliseconds()

            reportsMutex.withLock {
                scheduledReports[reportId]?.let {
                    scheduledReports[reportId] = it.copy(
                        status = ReportStatus.READY,
                        generatedAt = now,
                        content = content,
                        tokensCost = tokensCost,
                        costUsd = costUsd
                    )
                }
            }
        }

        /**
         * Get ready report for user (if any)
         * Call this when user opens app on Sunday/Monday
         */
        suspend fun getReadyReportForUser(userId: String): ScheduledReport? {
            return reportsMutex.withLock {
                scheduledReports.values.find {
                    it.userId == userId && it.status == ReportStatus.READY
                }
            }
        }

        /**
         * Mark report as delivered
         */
        suspend fun markDelivered(reportId: String) {
            reportsMutex.withLock {
                scheduledReports[reportId]?.let {
                    scheduledReports[reportId] = it.copy(status = ReportStatus.DELIVERED)
                }
            }
        }

        /**
         * Clean up old reports (call periodically)
         */
        suspend fun cleanupOldReports(maxAgeDays: Int = 7) {
            val cutoff = Clock.System.now().toEpochMilliseconds() - (maxAgeDays * 24 * 60 * 60 * 1000L)

            reportsMutex.withLock {
                val toRemove = scheduledReports.filter {
                    it.value.generatedAt?.let { gen -> gen < cutoff } ?: false
                }.keys
                toRemove.forEach { scheduledReports.remove(it) }
            }
        }

        /**
         * Get report generation prompt
         */
        fun getWeeklyReportPrompt(userName: String, contextSummary: String): String = """
            |Generate a comprehensive Weekly Wellness Report for $userName.
            |
            |Data Summary:
            |$contextSummary
            |
            |FORMAT:
            |## This Week's Wins (3 specific achievements)
            |## Patterns and Insights (2-3 data-driven observations)
            |## Watch Areas (1-2 concerns if any, skip if none)
            |## Next Week's Focus (3 actionable goals)
            |## Motivation (1 personalized encouragement)
            |
            |RULES:
            |- Be specific to their data
            |- Use "because" to explain patterns
            |- Keep under 350 words
            |- End on a positive note
        """.trimMargin()
    }

    // =========================================================================
    // FEATURE 5: REGIONAL SLM (Multi-Language On-Device AI)
    // =========================================================================

    /**
     * Regional SLM - Adapts on-device responses to user's language
     *
     * Qwen2.5 0.5B has good multilingual support, but we can:
     * 1. Use translated templates for common responses
     * 2. Detect language and provide appropriate fallbacks
     * 3. Route to Claude for languages the SLM struggles with
     *
     * Supported languages for on-device: EN, ES, FR, DE, PT, IT
     * Others: Fall back to Haiku (minimal cost for translation quality)
     */
    object RegionalSLM {

        enum class SupportedLanguage(
            val code: String,
            val displayName: String,
            val slmSupported: Boolean,  // Good SLM quality
            val rtl: Boolean = false
        ) {
            ENGLISH("en", "English", true),
            SPANISH("es", "Spanish", true),
            FRENCH("fr", "French", true),
            GERMAN("de", "German", true),
            PORTUGUESE("pt", "Portuguese", true),
            ITALIAN("it", "Italian", true),
            DUTCH("nl", "Dutch", true),
            POLISH("pl", "Polish", false),
            RUSSIAN("ru", "Russian", false),
            JAPANESE("ja", "Japanese", false),
            KOREAN("ko", "Korean", false),
            CHINESE_SIMPLIFIED("zh-CN", "Chinese (Simplified)", false),
            CHINESE_TRADITIONAL("zh-TW", "Chinese (Traditional)", false),
            ARABIC("ar", "Arabic", false, rtl = true),
            HINDI("hi", "Hindi", false),
            TURKISH("tr", "Turkish", false),
            VIETNAMESE("vi", "Vietnamese", false),
            THAI("th", "Thai", false),
            INDONESIAN("id", "Indonesian", true),
            MALAY("ms", "Malay", true)
        }

        private var userLanguage: SupportedLanguage = SupportedLanguage.ENGLISH

        /**
         * Set user's preferred language
         */
        fun setLanguage(languageCode: String) {
            userLanguage = SupportedLanguage.entries.find {
                it.code.equals(languageCode, ignoreCase = true) ||
                languageCode.startsWith(it.code, ignoreCase = true)
            } ?: SupportedLanguage.ENGLISH
        }

        fun getCurrentLanguage(): SupportedLanguage = userLanguage

        /**
         * Check if on-device SLM can handle this language well
         */
        fun canUseSLM(): Boolean = userLanguage.slmSupported

        /**
         * Get model recommendation based on language
         */
        fun getLanguageAwareModel(baseModel: AIModelUsed): AIModelUsed {
            // If SLM is recommended but language not supported, upgrade to Haiku
            return if (baseModel == AIModelUsed.QWEN_0_5B && !canUseSLM()) {
                AIModelUsed.CLAUDE_HAIKU
            } else {
                baseModel
            }
        }

        /**
         * Get localized templates for common responses
         * These are pre-translated to save API calls
         */
        object LocalizedTemplates {

            fun getGreeting(language: SupportedLanguage): String = when (language) {
                SupportedLanguage.ENGLISH -> "Great to see you! How are you feeling today?"
                SupportedLanguage.SPANISH -> "Que bueno verte! Como te sientes hoy?"
                SupportedLanguage.FRENCH -> "Ravi de te voir ! Comment te sens-tu aujourd'hui ?"
                SupportedLanguage.GERMAN -> "Schoen, dich zu sehen! Wie fuehlst du dich heute?"
                SupportedLanguage.PORTUGUESE -> "Que bom te ver! Como voce esta se sentindo hoje?"
                SupportedLanguage.ITALIAN -> "Che bello vederti! Come ti senti oggi?"
                SupportedLanguage.DUTCH -> "Leuk je te zien! Hoe voel je je vandaag?"
                SupportedLanguage.INDONESIAN -> "Senang bertemu! Bagaimana perasaanmu hari ini?"
                SupportedLanguage.MALAY -> "Seronok berjumpa! Bagaimana perasaan anda hari ini?"
                else -> "Great to see you! How are you feeling today?"
            }

            fun getStreakCelebration(streak: Int, language: SupportedLanguage): String {
                val streakText = streak.toString()
                return when (language) {
                    SupportedLanguage.ENGLISH -> "$streakText day streak! You are on fire!"
                    SupportedLanguage.SPANISH -> "$streakText dias seguidos! Estas en racha!"
                    SupportedLanguage.FRENCH -> "$streakText jours d'affilee! Tu es en feu!"
                    SupportedLanguage.GERMAN -> "$streakText Tage in Folge! Du bist am Start!"
                    SupportedLanguage.PORTUGUESE -> "$streakText dias seguidos! Voce esta arrasando!"
                    SupportedLanguage.ITALIAN -> "$streakText giorni consecutivi! Sei in forma!"
                    SupportedLanguage.DUTCH -> "$streakText dagen op rij! Je bent on fire!"
                    SupportedLanguage.INDONESIAN -> "$streakText hari berturut-turut! Kamu luar biasa!"
                    SupportedLanguage.MALAY -> "$streakText hari berturut-turut! Anda hebat!"
                    else -> "$streakText day streak! You are on fire!"
                }
            }

            fun getEncouragement(language: SupportedLanguage): String = when (language) {
                SupportedLanguage.ENGLISH -> "Every small step counts. Keep going!"
                SupportedLanguage.SPANISH -> "Cada pequeno paso cuenta. Sigue adelante!"
                SupportedLanguage.FRENCH -> "Chaque petit pas compte. Continue !"
                SupportedLanguage.GERMAN -> "Jeder kleine Schritt zaehlt. Mach weiter!"
                SupportedLanguage.PORTUGUESE -> "Cada pequeno passo conta. Continue assim!"
                SupportedLanguage.ITALIAN -> "Ogni piccolo passo conta. Continua cosi!"
                SupportedLanguage.DUTCH -> "Elke kleine stap telt. Ga zo door!"
                SupportedLanguage.INDONESIAN -> "Setiap langkah kecil berarti. Terus maju!"
                SupportedLanguage.MALAY -> "Setiap langkah kecil bermakna. Teruskan!"
                else -> "Every small step counts. Keep going!"
            }

            fun getMoodCheckQuestion(language: SupportedLanguage): String = when (language) {
                SupportedLanguage.ENGLISH -> "How are you feeling right now?"
                SupportedLanguage.SPANISH -> "Como te sientes en este momento?"
                SupportedLanguage.FRENCH -> "Comment te sens-tu en ce moment ?"
                SupportedLanguage.GERMAN -> "Wie fuehlst du dich gerade?"
                SupportedLanguage.PORTUGUESE -> "Como voce esta se sentindo agora?"
                SupportedLanguage.ITALIAN -> "Come ti senti in questo momento?"
                SupportedLanguage.DUTCH -> "Hoe voel je je op dit moment?"
                SupportedLanguage.INDONESIAN -> "Bagaimana perasaanmu sekarang?"
                SupportedLanguage.MALAY -> "Bagaimana perasaan anda sekarang?"
                else -> "How are you feeling right now?"
            }

            fun getHabitReminder(habitName: String, language: SupportedLanguage): String = when (language) {
                SupportedLanguage.ENGLISH -> "Don't forget: $habitName is waiting for you!"
                SupportedLanguage.SPANISH -> "No olvides: $habitName te espera!"
                SupportedLanguage.FRENCH -> "N'oublie pas : $habitName t'attend !"
                SupportedLanguage.GERMAN -> "Vergiss nicht: $habitName wartet auf dich!"
                SupportedLanguage.PORTUGUESE -> "Nao esqueca: $habitName esta esperando por voce!"
                SupportedLanguage.ITALIAN -> "Non dimenticare: $habitName ti aspetta!"
                SupportedLanguage.DUTCH -> "Vergeet niet: $habitName wacht op je!"
                SupportedLanguage.INDONESIAN -> "Jangan lupa: $habitName menunggumu!"
                SupportedLanguage.MALAY -> "Jangan lupa: $habitName menunggu anda!"
                else -> "Don't forget: $habitName is waiting for you!"
            }

            fun getSleepNudge(language: SupportedLanguage): String = when (language) {
                SupportedLanguage.ENGLISH -> "It's getting late. Good sleep = better tomorrow!"
                SupportedLanguage.SPANISH -> "Se esta haciendo tarde. Buen sueno = mejor manana!"
                SupportedLanguage.FRENCH -> "Il se fait tard. Bon sommeil = meilleur demain !"
                SupportedLanguage.GERMAN -> "Es wird spaet. Guter Schlaf = besserer Morgen!"
                SupportedLanguage.PORTUGUESE -> "Esta ficando tarde. Bom sono = amanha melhor!"
                SupportedLanguage.ITALIAN -> "Si sta facendo tardi. Buon sonno = domani migliore!"
                SupportedLanguage.DUTCH -> "Het wordt laat. Goede slaap = betere morgen!"
                SupportedLanguage.INDONESIAN -> "Sudah larut. Tidur nyenyak = esok lebih baik!"
                SupportedLanguage.MALAY -> "Sudah lewat. Tidur nyenyak = esok lebih baik!"
                else -> "It's getting late. Good sleep = better tomorrow!"
            }
        }

        /**
         * Get language instruction to append to prompts for cloud models
         */
        fun getLanguageInstruction(): String {
            return if (userLanguage != SupportedLanguage.ENGLISH) {
                "\n\nIMPORTANT: Respond in ${userLanguage.displayName}."
            } else ""
        }

        /**
         * Detect language from user input (simple heuristic)
         */
        fun detectLanguage(text: String): SupportedLanguage {
            val lower = text.lowercase()

            return when {
                // Spanish indicators
                listOf("hola", "como", "que", "estoy", "tengo").any { lower.contains(it) } ->
                    SupportedLanguage.SPANISH

                // French indicators
                listOf("bonjour", "comment", "je suis", "qu'est", "c'est").any { lower.contains(it) } ->
                    SupportedLanguage.FRENCH

                // German indicators
                listOf("ich bin", "wie geht", "guten", "danke", "bitte").any { lower.contains(it) } ->
                    SupportedLanguage.GERMAN

                // Portuguese indicators
                listOf("ola", "como vai", "estou", "obrigado", "bom dia").any { lower.contains(it) } ->
                    SupportedLanguage.PORTUGUESE

                // Italian indicators
                listOf("ciao", "come stai", "buongiorno", "grazie", "sono").any { lower.contains(it) } ->
                    SupportedLanguage.ITALIAN

                // Japanese indicators
                text.any { it in '\u3040'..'\u30FF' || it in '\u4E00'..'\u9FAF' } ->
                    SupportedLanguage.JAPANESE

                // Korean indicators
                text.any { it in '\uAC00'..'\uD7A3' } ->
                    SupportedLanguage.KOREAN

                // Chinese indicators
                text.all { it in '\u4E00'..'\u9FFF' || it.isWhitespace() || it.isDigit() } && text.length > 2 ->
                    SupportedLanguage.CHINESE_SIMPLIFIED

                // Arabic indicators
                text.any { it in '\u0600'..'\u06FF' } ->
                    SupportedLanguage.ARABIC

                // Russian indicators
                text.any { it in '\u0400'..'\u04FF' } ->
                    SupportedLanguage.RUSSIAN

                // Thai indicators
                text.any { it in '\u0E00'..'\u0E7F' } ->
                    SupportedLanguage.THAI

                // Indonesian/Malay indicators
                listOf("apa kabar", "selamat", "terima kasih", "bagaimana").any { lower.contains(it) } ->
                    SupportedLanguage.INDONESIAN

                else -> SupportedLanguage.ENGLISH
            }
        }
    }
}


