package com.dailywell.app.data.repository

import com.dailywell.app.data.content.DailyInsightsDatabase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.days

/**
 * DailyInsightsRepositoryImpl - PRODUCTION-READY Firebase Implementation
 *
 * All insight viewing and bookmarking data is stored in Firebase Firestore.
 * NO MOCK DATA - All persistence is real and synced across devices.
 *
 * Firestore Collections:
 * - users/{userId}/insight_views - View history records
 * - users/{userId}/insight_bookmarks - Bookmarked insights
 * - users/{userId}/insight_stats - Aggregated statistics
 */
class DailyInsightsRepositoryImpl : DailyInsightsRepository {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    // Repository scope for background operations
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    // Cached view and bookmark states for efficient lookups
    private val _viewedInsights = MutableStateFlow<Set<String>>(emptySet())
    private val _bookmarkedInsights = MutableStateFlow<Set<String>>(emptySet())
    private val _viewHistory = MutableStateFlow<List<InsightViewRecord>>(emptyList())

    private val insightsCollection
        get() = firestore.collection("users").document(userId).collection("insight_views")

    private val bookmarksCollection
        get() = firestore.collection("users").document(userId).collection("insight_bookmarks")

    private val statsDocument
        get() = firestore.collection("users").document(userId).collection("insight_stats").document("current")

    init {
        // Load initial state from Firebase
        loadViewedInsights()
        loadBookmarkedInsights()
        loadViewHistory()
    }

    private fun loadViewedInsights() {
        repositoryScope.launch {
            try {
                insightsCollection.snapshots.collect { querySnapshot ->
                    val ids = querySnapshot.documents.map { it.id }.toSet()
                    _viewedInsights.value = ids
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadBookmarkedInsights() {
        repositoryScope.launch {
            try {
                bookmarksCollection.snapshots.collect { querySnapshot ->
                    val ids = querySnapshot.documents.map { it.id }.toSet()
                    _bookmarkedInsights.value = ids
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadViewHistory() {
        repositoryScope.launch {
            try {
                insightsCollection.snapshots.collect { querySnapshot ->
                    val records = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            InsightViewRecord(
                                insightId = doc.get<String>("insightId"),
                                viewedAt = doc.get<String>("viewedAt"),
                                dayOfYear = doc.get<Int>("dayOfYear"),
                                category = doc.get<String>("category"),
                                title = doc.get<String>("title")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.viewedAt }
                    _viewHistory.value = records
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==================== CORE INSIGHT RETRIEVAL ====================

    override fun getTodayInsight(): Flow<DailyInsightWithMeta?> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val dayOfYear = today.dayOfYear

        return _viewedInsights.map { viewed ->
            val insight = DailyInsightsDatabase.getInsightForDay(dayOfYear)
            val isBookmarked = _bookmarkedInsights.value.contains(insight.id)
            val isViewed = viewed.contains(insight.id)

            insight.toMeta(
                isViewed = isViewed,
                isBookmarked = isBookmarked,
                dayOfYear = dayOfYear
            )
        }
    }

    override suspend fun getInsightForDay(dayOfYear: Int): DailyInsightWithMeta {
        val insight = DailyInsightsDatabase.getInsightForDay(dayOfYear)
        val isViewed = _viewedInsights.value.contains(insight.id)
        val isBookmarked = _bookmarkedInsights.value.contains(insight.id)

        return insight.toMeta(
            isViewed = isViewed,
            isBookmarked = isBookmarked,
            dayOfYear = dayOfYear
        )
    }

    override suspend fun getContextualInsight(
        streak: Int,
        recentBreak: Boolean,
        achievement: String?
    ): DailyInsightWithMeta {
        val insight = DailyInsightsDatabase.getContextualInsight(streak, recentBreak, achievement)
        val isViewed = _viewedInsights.value.contains(insight.id)
        val isBookmarked = _bookmarkedInsights.value.contains(insight.id)

        return insight.toMeta(
            isViewed = isViewed,
            isBookmarked = isBookmarked
        )
    }

    // ==================== TRACKING & PERSISTENCE ====================

    override suspend fun markInsightViewed(insightId: String) {
        try {
            val now = Clock.System.now()
            val today = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val dayOfYear = today.dayOfYear

            // Find the insight details
            val insight = DailyInsightsDatabase.allInsights.find { it.id == insightId }
                ?: return

            val viewData = hashMapOf(
                "insightId" to insightId,
                "viewedAt" to now.toString(),
                "dayOfYear" to dayOfYear,
                "category" to insight.category.name,
                "title" to insight.title,
                "year" to today.year
            )

            // Store in Firebase
            insightsCollection.document(insightId).set(viewData)

            // Update local cache
            _viewedInsights.value = _viewedInsights.value + insightId

            // Update stats
            updateStats()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun bookmarkInsight(insightId: String): Result<Unit> {
        return try {
            val now = Clock.System.now()
            val insight = DailyInsightsDatabase.allInsights.find { it.id == insightId }
                ?: return Result.failure(Exception("Insight not found"))

            val bookmarkData = hashMapOf(
                "insightId" to insightId,
                "bookmarkedAt" to now.toString(),
                "category" to insight.category.name,
                "title" to insight.title,
                "content" to insight.content,
                "source" to (insight.source ?: "")
            )

            bookmarksCollection.document(insightId).set(bookmarkData)
            _bookmarkedInsights.value = _bookmarkedInsights.value + insightId

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unbookmarkInsight(insightId: String): Result<Unit> {
        return try {
            bookmarksCollection.document(insightId).delete()
            _bookmarkedInsights.value = _bookmarkedInsights.value - insightId
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getBookmarkedInsights(): Flow<List<DailyInsightWithMeta>> {
        return _bookmarkedInsights.map { bookmarkedIds ->
            bookmarkedIds.mapNotNull { id ->
                DailyInsightsDatabase.allInsights.find { it.id == id }?.toMeta(
                    isViewed = _viewedInsights.value.contains(id),
                    isBookmarked = true
                )
            }
        }
    }

    override suspend fun isInsightBookmarked(insightId: String): Boolean {
        return _bookmarkedInsights.value.contains(insightId)
    }

    // ==================== HISTORY & ANALYTICS ====================

    override fun getViewHistory(): Flow<List<InsightViewRecord>> = _viewHistory

    override suspend fun getTotalInsightsViewed(): Int {
        return _viewedInsights.value.size
    }

    override suspend fun getWeeklyInsightsViewed(): Int {
        val now = Clock.System.now()
        val weekAgo = now.minus(7.days)
        val weekAgoStr = weekAgo.toString()

        return _viewHistory.value.count { record ->
            record.viewedAt >= weekAgoStr
        }
    }

    override suspend fun getInsightStats(): InsightStats {
        val viewedIds = _viewedInsights.value
        val bookmarkedIds = _bookmarkedInsights.value
        val history = _viewHistory.value

        // Calculate view streak (consecutive days with at least one view)
        val viewStreak = calculateViewStreak(history)

        // Find favorite category
        val categoryCounts = history
            .groupBy { it.category }
            .mapValues { it.value.size }

        val favoriteCategory = categoryCounts.maxByOrNull { it.value }?.key

        // Calculate completion percent
        val totalInsights = DailyInsightsDatabase.getTotalCount()
        val completionPercent = if (totalInsights > 0) {
            (viewedIds.size.toFloat() / totalInsights) * 100f
        } else 0f

        return InsightStats(
            totalViewed = viewedIds.size,
            totalBookmarked = bookmarkedIds.size,
            viewStreak = viewStreak,
            favoriteCategory = favoriteCategory,
            completionPercent = completionPercent,
            categoryCounts = categoryCounts
        )
    }

    private fun calculateViewStreak(history: List<InsightViewRecord>): Int {
        if (history.isEmpty()) return 0

        val tz = TimeZone.currentSystemDefault()
        val todayEpochDay = Clock.System.todayIn(tz).toEpochDays()

        // Convert each record's viewedAt ISO timestamp to epoch day
        // This correctly handles year boundaries (Dec 31 -> Jan 1)
        val viewEpochDays = history.mapNotNull { record ->
            try {
                Instant.parse(record.viewedAt)
                    .toLocalDateTime(tz)
                    .date
                    .toEpochDays()
            } catch (_: Exception) {
                null
            }
        }.toSet().sorted().reversed()

        if (viewEpochDays.isEmpty()) return 0

        // Check if viewed today or yesterday to start the streak
        if (!viewEpochDays.contains(todayEpochDay) && !viewEpochDays.contains(todayEpochDay - 1)) {
            return 0
        }

        var streak = 0
        var currentDay = if (viewEpochDays.contains(todayEpochDay)) todayEpochDay else todayEpochDay - 1

        for (day in viewEpochDays) {
            if (day == currentDay) {
                streak++
                currentDay--
            } else if (day < currentDay) {
                break
            }
        }

        return streak
    }

    private suspend fun updateStats() {
        try {
            val stats = getInsightStats()
            val statsData = hashMapOf(
                "totalViewed" to stats.totalViewed,
                "totalBookmarked" to stats.totalBookmarked,
                "viewStreak" to stats.viewStreak,
                "favoriteCategory" to (stats.favoriteCategory ?: ""),
                "completionPercent" to stats.completionPercent,
                "lastUpdated" to Clock.System.now().toString()
            )
            statsDocument.set(statsData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== CATEGORY BROWSING ====================

    override fun getInsightsByCategory(category: DailyInsightsDatabase.InsightCategory): List<DailyInsightWithMeta> {
        return DailyInsightsDatabase.getInsightsByCategory(category).map { insight ->
            insight.toMeta(
                isViewed = _viewedInsights.value.contains(insight.id),
                isBookmarked = _bookmarkedInsights.value.contains(insight.id)
            )
        }
    }

    override fun searchInsights(query: String): List<DailyInsightWithMeta> {
        return DailyInsightsDatabase.searchInsights(query).map { insight ->
            insight.toMeta(
                isViewed = _viewedInsights.value.contains(insight.id),
                isBookmarked = _bookmarkedInsights.value.contains(insight.id)
            )
        }
    }

    override fun getCategories(): List<InsightCategoryInfo> {
        return DailyInsightsDatabase.InsightCategory.entries.map { category ->
            val insights = DailyInsightsDatabase.getInsightsByCategory(category)
            val viewedCount = insights.count { _viewedInsights.value.contains(it.id) }

            InsightCategoryInfo(
                category = category,
                displayName = when (category) {
                    DailyInsightsDatabase.InsightCategory.HABIT_PSYCHOLOGY -> "Habit Psychology"
                    DailyInsightsDatabase.InsightCategory.NEUROSCIENCE -> "Neuroscience"
                    DailyInsightsDatabase.InsightCategory.BEHAVIORAL_TRIGGERS -> "Behavioral Triggers"
                    DailyInsightsDatabase.InsightCategory.PROGRESS_MINDSET -> "Progress Mindset"
                    DailyInsightsDatabase.InsightCategory.SOCIAL_PSYCHOLOGY -> "Social Psychology"
                    DailyInsightsDatabase.InsightCategory.RECOVERY_COMPASSION -> "Recovery & Compassion"
                    DailyInsightsDatabase.InsightCategory.ADVANCED_TECHNIQUES -> "Advanced Techniques"
                },
                emoji = category.emoji(),
                count = insights.size,
                viewedCount = viewedCount
            )
        }
    }
}
