package com.dailywell.app.data.local

import com.dailywell.app.ai.AIRoutingEngine
import com.dailywell.app.data.local.db.*
import com.dailywell.app.data.model.AIGovernancePolicy
import com.dailywell.app.data.model.AIModelUsed
import com.dailywell.app.data.model.AIPlanType
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * AI Feature Persistence Layer
 *
 * Provides persistent storage for all 5 advanced AI features,
 * replacing the in-memory mutableMapOf/mutableListOf storage.
 *
 * This ensures data survives app restarts and enables proper analytics.
 */
class AIFeaturePersistence(
    private val abTestDao: ABTestDao,
    private val insightSchedulerDao: InsightSchedulerDao,
    private val contextCacheDao: ContextCacheDao,
    private val opusSchedulerDao: OpusSchedulerDao,
    private val userAISettingsDao: UserAISettingsDao,
    private val aiUsageDao: AIUsageDao,
    private val aiInteractionDao: AIInteractionDao
) {
    // ========================================================================
    // 1. A/B TEST HOOK PERSISTENCE
    // ========================================================================

    suspend fun logABTestEvent(event: AIRoutingEngine.ABTestHook.RoutingEvent) {
        abTestDao.insertEvent(
            ABTestEventEntity(
                eventId = event.eventId,
                timestamp = event.timestamp,
                userId = event.userId,
                intent = event.intent.name,
                requestedModel = event.requestedModel.name,
                actualModel = event.actualModel.name,
                reason = event.reason,
                budgetMode = event.budgetMode.name,
                inputTokens = event.inputTokens,
                outputTokens = event.outputTokens,
                cost = event.cost,
                responseTimeMs = event.responseTimeMs,
                userFeedback = event.userFeedback?.name,
                feedbackTimestamp = null
            )
        )
    }

    suspend fun recordABTestFeedback(
        eventId: String,
        feedback: AIRoutingEngine.ABTestHook.UserFeedback
    ) {
        abTestDao.recordFeedback(
            eventId = eventId,
            feedback = feedback.name,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    suspend fun getABTestEvents(userId: String, limit: Int = 100): List<AIRoutingEngine.ABTestHook.RoutingEvent> {
        return abTestDao.getRecentEvents(userId, limit).map { it.toRoutingEvent() }
    }

    suspend fun getIntentAnalytics(): List<AIRoutingEngine.ABTestHook.IntentPerformance> {
        return abTestDao.getIntentAnalytics().map { result ->
            AIRoutingEngine.ABTestHook.IntentPerformance(
                intent = AIRoutingEngine.RequestIntent.valueOf(result.intent),
                callCount = result.count,
                avgResponseTimeMs = result.avgTime.toLong(),
                avgCost = if (result.count > 0) result.totalCost / result.count else 0f,
                positiveRate = 0f,  // Would need separate feedback query
                upgradeHintRate = 0f,
                modelDistribution = emptyMap()  // Would need separate query
            )
        }
    }

    // ========================================================================
    // 2. INSIGHT SCHEDULER PERSISTENCE
    // ========================================================================

    suspend fun scheduleInsightsForNewUser(userId: String, signupTimestamp: Long) {
        val insights = AIRoutingEngine.InsightScheduler.InsightMilestone.entries.map { milestone ->
            val scheduledTime = signupTimestamp + (milestone.dayNumber.toLong() * 24 * 60 * 60 * 1000)
            ScheduledInsightEntity(
                uniqueKey = "${userId}_${milestone.name}",
                userId = userId,
                milestone = milestone.name,
                scheduledTimestamp = scheduledTime,
                status = "PENDING"
            )
        }
        insightSchedulerDao.insertInsights(insights)
    }

    suspend fun getDueInsights(userId: String): List<AIRoutingEngine.InsightScheduler.ScheduledInsight> {
        val now = Clock.System.now().toEpochMilliseconds()
        return insightSchedulerDao.getDueInsights(userId, now).map { entity ->
            AIRoutingEngine.InsightScheduler.ScheduledInsight(
                milestone = AIRoutingEngine.InsightScheduler.InsightMilestone.valueOf(entity.milestone),
                userId = entity.userId,
                scheduledFor = entity.scheduledTimestamp,
                generated = entity.status == "GENERATED" || entity.status == "DELIVERED",
                generatedAt = entity.generatedTimestamp,
                content = entity.generatedContent
            )
        }
    }

    suspend fun markInsightGenerated(userId: String, milestone: AIRoutingEngine.InsightScheduler.InsightMilestone, content: String) {
        val key = "${userId}_${milestone.name}"
        insightSchedulerDao.markGenerated(key, content)
    }

    suspend fun markInsightDelivered(userId: String, milestone: AIRoutingEngine.InsightScheduler.InsightMilestone) {
        val key = "${userId}_${milestone.name}"
        insightSchedulerDao.markDelivered(key)
    }

    suspend fun hasGeneratedMilestone(userId: String, milestone: AIRoutingEngine.InsightScheduler.InsightMilestone): Boolean {
        return insightSchedulerDao.hasGeneratedMilestone(userId, milestone.name) > 0
    }

    // ========================================================================
    // 3. CONTEXT CACHE PERSISTENCE
    // ========================================================================

    suspend fun getCachedContext(userId: String): AIRoutingEngine.ContextCache.CachedContext? {
        val entity = contextCacheDao.getValidCache(userId) ?: return null

        // Reconstruct daily summaries from stored data
        val dailySummaries = getRecentDailySummaries(userId, 14)

        return AIRoutingEngine.ContextCache.CachedContext(
            userId = entity.userId,
            generatedAt = entity.cachedTimestamp,
            expiresAt = entity.expiresAt,
            dailySummaries = dailySummaries,
            weeklyTrend = determineWeeklyTrend(entity.avgCompletionRate),
            topStrengths = entity.topHabits.split(",").filter { it.isNotBlank() },
            areasForFocus = entity.missedHabits.split(",").filter { it.isNotBlank() },
            totalTokenEstimate = 200  // Estimated
        )
    }

    private fun determineWeeklyTrend(avgRate: Float): String = when {
        avgRate > 0.8f -> "Excellent consistency"
        avgRate > 0.6f -> "Good momentum"
        avgRate > 0.4f -> "Building foundations"
        else -> "Just getting started"
    }

    suspend fun cacheContext(context: AIRoutingEngine.ContextCache.CachedContext) {
        val avgRate = context.dailySummaries.map { it.habitCompletionRate }.average().toFloat()

        contextCacheDao.insertCache(
            ContextCacheEntity(
                userId = context.userId,
                cachedTimestamp = context.generatedAt,
                expiresAt = context.expiresAt,
                avgCompletionRate = avgRate,
                dominantMood = context.dailySummaries.mapNotNull { it.mood }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
                sleepAvgHours = context.dailySummaries.mapNotNull { it.sleepHours }.average().toFloat().takeIf { !it.isNaN() },
                sleepAvgQuality = null,  // Would need conversion
                nutritionAvgScore = context.dailySummaries.mapNotNull { it.nutritionScore }.average().toFloat().takeIf { !it.isNaN() },
                workoutTotalMinutes = context.dailySummaries.mapNotNull { it.workoutMinutes }.sum(),
                streakDays = context.dailySummaries.count { it.habitCompletionRate > 0.5f },
                topHabits = context.topStrengths.joinToString(","),
                missedHabits = context.areasForFocus.joinToString(","),
                condensedPrompt = context.toCondensedPrompt()
            )
        )

        // Also save individual daily summaries
        context.dailySummaries.forEach { summary ->
            saveDailySummary(summary)
        }
    }

    suspend fun invalidateContextCache(userId: String) {
        contextCacheDao.invalidateCache(userId)
    }

    suspend fun saveDailySummary(summary: AIRoutingEngine.ContextCache.DailySummary) {
        contextCacheDao.insertDailySummary(
            DailyContextSummaryEntity(
                id = "${summary.userId}_${summary.date}",
                userId = summary.userId,
                date = summary.date,
                habitCompletionRate = summary.habitCompletionRate,
                completedHabits = summary.completedHabits.joinToString(","),
                missedHabits = summary.missedHabits.joinToString(","),
                mood = summary.mood,
                sleepHours = summary.sleepHours,
                sleepQuality = null,  // String in DailySummary
                nutritionScore = summary.nutritionScore,
                workoutMinutes = summary.workoutMinutes,
                energyLevel = null,  // String in DailySummary
                notes = summary.notes
            )
        )
    }

    suspend fun getRecentDailySummaries(userId: String, days: Int = 14): List<AIRoutingEngine.ContextCache.DailySummary> {
        return contextCacheDao.getRecentDailySummaries(userId, days).map { entity ->
            AIRoutingEngine.ContextCache.DailySummary(
                date = entity.date,
                userId = entity.userId,
                habitCompletionRate = entity.habitCompletionRate,
                completedHabits = entity.completedHabits.split(",").filter { it.isNotBlank() },
                missedHabits = entity.missedHabits.split(",").filter { it.isNotBlank() },
                mood = entity.mood,
                sleepHours = entity.sleepHours,
                sleepQuality = entity.sleepQuality?.toString(),
                nutritionScore = entity.nutritionScore,
                workoutMinutes = entity.workoutMinutes,
                energyLevel = entity.energyLevel?.toString(),
                notes = entity.notes
            )
        }
    }

    // ========================================================================
    // 4. OPUS SCHEDULER PERSISTENCE
    // ========================================================================

    suspend fun scheduleWeeklyReport(userId: String, planType: AIPlanType): AIRoutingEngine.OpusScheduler.ScheduledReport? {
        // Weekly Opus reports are premium-only (1/week, max 4/month).
        if (planType == AIPlanType.FREE) return null

        val nextSunday = AIRoutingEngine.OpusScheduler.getNextSundayReportTime()
        val reportId = "report_${userId}_${nextSunday}"

        val nowLocal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentMonthKey = "${nowLocal.year}-${nowLocal.monthNumber.toString().padStart(2, '0')}"
        val monthlyReportsCount = opusSchedulerDao.getReportsForUser(userId).count { report ->
            val reportLocal = kotlinx.datetime.Instant.fromEpochMilliseconds(report.scheduledTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val reportMonthKey = "${reportLocal.year}-${reportLocal.monthNumber.toString().padStart(2, '0')}"
            reportMonthKey == currentMonthKey
        }
        if (monthlyReportsCount >= AIGovernancePolicy.WEEKLY_REPORT_LIMIT_PER_MONTH) {
            return null
        }

        val existingReport = opusSchedulerDao.getReport(reportId)
        if (existingReport != null) {
            return existingReport.toScheduledReport()
        }

        val report = ScheduledReportEntity(
            reportId = reportId,
            userId = userId,
            reportType = "WEEKLY_SUMMARY",
            scheduledTime = nextSunday,
            status = "SCHEDULED"
        )
        opusSchedulerDao.insertReport(report)

        return report.toScheduledReport()
    }

    suspend fun getDueReports(): List<AIRoutingEngine.OpusScheduler.ScheduledReport> {
        return opusSchedulerDao.getDueReports().map { it.toScheduledReport() }
    }

    suspend fun getReadyReportForUser(userId: String): AIRoutingEngine.OpusScheduler.ScheduledReport? {
        return opusSchedulerDao.getReadyReportForUser(userId)?.toScheduledReport()
    }

    suspend fun markReportGenerating(reportId: String) {
        opusSchedulerDao.markGenerating(reportId)
    }

    suspend fun storeGeneratedReport(reportId: String, content: String, tokensCost: Int, costUsd: Float) {
        opusSchedulerDao.storeGeneratedReport(reportId, content, tokensCost, costUsd)
    }

    suspend fun markReportDelivered(reportId: String) {
        opusSchedulerDao.markDelivered(reportId)
    }

    suspend fun markReportFailed(reportId: String) {
        opusSchedulerDao.markFailed(reportId)
    }

    // ========================================================================
    // 5. USER AI SETTINGS PERSISTENCE (Language, etc.)
    // ========================================================================

    suspend fun getUserLanguage(userId: String): String {
        return userAISettingsDao.getSettings(userId)?.preferredLanguage ?: "en"
    }

    suspend fun setUserLanguage(userId: String, language: String) {
        val existing = userAISettingsDao.getSettings(userId)
        if (existing != null) {
            userAISettingsDao.updateLanguage(userId, language)
        } else {
            userAISettingsDao.insertSettings(
                UserAISettingsEntity(userId = userId, preferredLanguage = language)
            )
        }
    }

    suspend fun setDetectedLanguage(userId: String, language: String) {
        val existing = userAISettingsDao.getSettings(userId)
        if (existing != null) {
            userAISettingsDao.updateDetectedLanguage(userId, language)
        } else {
            userAISettingsDao.insertSettings(
                UserAISettingsEntity(userId = userId, lastLanguageDetected = language)
            )
        }
    }

    // ========================================================================
    // 6. AI USAGE TRACKING PERSISTENCE
    // ========================================================================

    suspend fun getOrCreateCurrentUsage(userId: String, planType: AIPlanType): AIUsageEntity {
        val now = Clock.System.now()
        val localDate = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}"
        val usageId = "${userId}_$month"

        val existing = aiUsageDao.getUsage(usageId)
        if (existing != null) return existing

        // Calculate reset date (1st of next month)
        val nextMonth = if (localDate.monthNumber == 12) {
            "${localDate.year + 1}-01-01"
        } else {
            "${localDate.year}-${(localDate.monthNumber + 1).toString().padStart(2, '0')}-01"
        }

        val newUsage = AIUsageEntity(
            usageId = usageId,
            userId = userId,
            month = month,
            planType = planType.name,
            resetDate = nextMonth
        )
        aiUsageDao.insertUsage(newUsage)
        return newUsage
    }

    fun getCurrentUsageFlow(userId: String): Flow<AIUsageEntity?> {
        return aiUsageDao.getCurrentUsageFlow(userId)
    }

    suspend fun incrementFreeMessage(userId: String, planType: AIPlanType) {
        val usage = getOrCreateCurrentUsage(userId, planType)
        aiUsageDao.incrementFreeMessage(usage.usageId)
    }

    suspend fun incrementSLMMessage(userId: String, planType: AIPlanType) {
        val usage = getOrCreateCurrentUsage(userId, planType)
        aiUsageDao.incrementSLMMessage(usage.usageId)
    }

    suspend fun incrementAIMessage(userId: String, planType: AIPlanType, tokens: Int, cost: Float) {
        val usage = getOrCreateCurrentUsage(userId, planType)
        aiUsageDao.incrementAIMessage(usage.usageId, tokens, cost)
    }

    // ========================================================================
    // 7. AI INTERACTION LOG PERSISTENCE
    // ========================================================================

    suspend fun logInteraction(
        userId: String,
        inputTokens: Int,
        outputTokens: Int,
        model: AIModelUsed,
        category: String,
        durationMs: Long? = null
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val cost = when (model) {
            AIModelUsed.DECISION_TREE, AIModelUsed.QWEN_0_5B -> 0f
            AIModelUsed.CLAUDE_HAIKU -> (inputTokens * 1.0f + outputTokens * 5.0f) / 1_000_000
            AIModelUsed.CLAUDE_SONNET -> (inputTokens * 3.0f + outputTokens * 15.0f) / 1_000_000
            AIModelUsed.CLAUDE_OPUS -> (inputTokens * 15.0f + outputTokens * 75.0f) / 1_000_000
        }

        aiInteractionDao.insertInteraction(
            AIInteractionEntity(
                id = "int_${now}_${(0..9999).random()}",
                userId = userId,
                timestamp = now,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                totalTokens = inputTokens + outputTokens,
                modelUsed = model.name,
                responseCategory = category,
                durationMs = durationMs,
                estimatedCostUsd = cost
            )
        )
    }

    suspend fun getModelUsageStats(userId: String): Map<AIModelUsed, Pair<Int, Float>> {
        return aiInteractionDao.getModelUsageStats(userId).associate { stats ->
            parseAIModelUsed(stats.modelUsed) to Pair(stats.count, stats.cost)
        }
    }

    // ========================================================================
    // MAINTENANCE / CLEANUP
    // ========================================================================

    suspend fun pruneOldData() {
        val thirtyDaysAgo = Clock.System.now().toEpochMilliseconds() - 30L * 24 * 60 * 60 * 1000
        val ninetyDaysAgo = Clock.System.now().toEpochMilliseconds() - 90L * 24 * 60 * 60 * 1000

        abTestDao.pruneOldEvents(thirtyDaysAgo)
        contextCacheDao.pruneExpiredCaches()
        opusSchedulerDao.expireOldReports()
        opusSchedulerDao.pruneOldReports(ninetyDaysAgo)
        aiInteractionDao.pruneOldInteractions(ninetyDaysAgo)
    }
}

// ============================================================================
// EXTENSION FUNCTIONS FOR ENTITY CONVERSION
// ============================================================================

private fun ABTestEventEntity.toRoutingEvent(): AIRoutingEngine.ABTestHook.RoutingEvent {
    return AIRoutingEngine.ABTestHook.RoutingEvent(
        eventId = eventId,
        timestamp = timestamp,
        userId = userId,
        intent = AIRoutingEngine.RequestIntent.valueOf(intent),
        requestedModel = parseAIModelUsed(requestedModel),
        actualModel = parseAIModelUsed(actualModel),
        reason = reason,
        budgetMode = AIRoutingEngine.BudgetMode.valueOf(budgetMode),
        inputTokens = inputTokens,
        outputTokens = outputTokens,
        cost = cost,
        responseTimeMs = responseTimeMs,
        userFeedback = userFeedback?.let { AIRoutingEngine.ABTestHook.UserFeedback.valueOf(it) }
    )
}

private fun ScheduledReportEntity.toScheduledReport(): AIRoutingEngine.OpusScheduler.ScheduledReport {
    return AIRoutingEngine.OpusScheduler.ScheduledReport(
        reportId = reportId,
        userId = userId,
        reportType = try {
            AIRoutingEngine.OpusPermit.ReportType.valueOf(reportType)
        } catch (e: Exception) {
            AIRoutingEngine.OpusPermit.ReportType.WEEKLY_REPORT
        },
        scheduledFor = scheduledTime,
        status = try {
            AIRoutingEngine.OpusScheduler.ReportStatus.valueOf(status)
        } catch (e: Exception) {
            AIRoutingEngine.OpusScheduler.ReportStatus.SCHEDULED
        },
        generatedAt = generatedAt,
        content = generatedContent,
        tokensCost = tokensCost,
        costUsd = costUsd
    )
}

private fun parseAIModelUsed(raw: String): AIModelUsed {
    return when (raw) {
        "GEMMA_3N" -> AIModelUsed.QWEN_0_5B
        else -> runCatching { AIModelUsed.valueOf(raw) }.getOrDefault(AIModelUsed.DECISION_TREE)
    }
}

