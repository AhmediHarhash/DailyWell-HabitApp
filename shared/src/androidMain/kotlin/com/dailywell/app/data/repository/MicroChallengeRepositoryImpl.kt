package com.dailywell.app.data.repository

import com.dailywell.app.data.content.MicroChallengesDatabase
import com.dailywell.app.data.content.MicroChallengesDatabase.MicroChallenge
import com.dailywell.app.data.content.MicroChallengesDatabase.ChallengeCategory
import com.dailywell.app.data.content.MicroChallengesDatabase.ChallengeDifficulty
import com.dailywell.app.data.local.DataStoreManager
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * MicroChallengeRepositoryImpl - Production-Ready 365 Micro-Challenges
 *
 * Features:
 * - Firebase Firestore for cloud persistence
 * - Local DataStore for offline cache
 * - Real-time sync between devices
 * - Challenge streak tracking
 * - Category-based statistics
 */
class MicroChallengeRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : MicroChallengeRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val firestore by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    private val userId: String?
        get() = auth.currentUser?.uid

    // Firebase collection paths
    private val challengeProgressCollection
        get() = userId?.let {
            firestore.collection("users").document(it).collection("microChallengeProgress")
        }

    // Cached data
    private val _todayChallenge = MutableStateFlow<DailyMicroChallengeWithMeta?>(null)
    private val _stats = MutableStateFlow(MicroChallengeStats())
    private val _completionHistory = MutableStateFlow<List<DailyMicroChallengeWithMeta>>(emptyList())

    // DataStore keys
    private object Keys {
        const val CHALLENGE_PROGRESS = "micro_challenge_progress"
        const val CHALLENGE_STATS = "micro_challenge_stats"
        const val COMPLETED_DATES = "micro_challenge_completed_dates"
    }

    init {
        scope.launch {
            loadTodayChallenge()
            loadStats()
            syncWithCloud()
        }
    }

    // ==================== DAILY CHALLENGE ====================

    override fun getTodayChallenge(): Flow<DailyMicroChallengeWithMeta> {
        return flow {
            // Get today's challenge from the 365 database (synchronous)
            val today = todayDayOfYear()
            val challenge = MicroChallengesDatabase.getChallengeForDay(today)

            // Emit immediately with default values (no DataStore blocking)
            val result = DailyMicroChallengeWithMeta(
                challenge = challenge,
                dayOfYear = today,
                isCompleted = false,
                completedAt = null,
                challengeStreak = 0,
                categoryProgress = emptyMap()
            )
            emit(result)

            // Then load full state from DataStore and emit update
            try {
                val completion = getCompletionForDay(today)
                val streak = calculateStreak()
                val categoryProgress = getCategoryProgress()

                val fullResult = DailyMicroChallengeWithMeta(
                    challenge = challenge,
                    dayOfYear = today,
                    isCompleted = completion?.completed ?: false,
                    completedAt = completion?.completedAt,
                    challengeStreak = streak,
                    categoryProgress = categoryProgress
                )
                emit(fullResult)
                _todayChallenge.value = fullResult
            } catch (e: Exception) {
                // DataStore failed - use default emitted above
                _todayChallenge.value = result
            }
        }
    }

    override suspend fun getChallengeForDay(dayOfYear: Int): DailyMicroChallengeWithMeta {
        val challenge = MicroChallengesDatabase.getChallengeForDay(dayOfYear)
        val completion = getCompletionForDay(dayOfYear)
        val streak = getChallengeStreak()

        return DailyMicroChallengeWithMeta(
            challenge = challenge,
            dayOfYear = dayOfYear,
            isCompleted = completion?.completed ?: false,
            completedAt = completion?.completedAt,
            challengeStreak = streak,
            categoryProgress = getCategoryProgress()
        )
    }

    override suspend fun completeTodayChallenge(): DailyMicroChallengeWithMeta {
        val today = todayDayOfYear()
        val now = Clock.System.now().toString()
        val challenge = MicroChallengesDatabase.getChallengeForDay(today)

        // Save completion locally
        val completion = ChallengeCompletion(
            dayOfYear = today,
            challengeId = challenge.id,
            category = challenge.category.name,
            difficulty = challenge.difficulty.name,
            completed = true,
            completedAt = now
        )
        saveCompletion(completion)

        // Save to Firebase
        saveChallengeToFirebase(completion)

        // Update streak
        val newStreak = calculateStreak()
        updateStats { stats ->
            stats.copy(
                totalCompleted = stats.totalCompleted + 1,
                currentStreak = newStreak,
                longestStreak = maxOf(stats.longestStreak, newStreak),
                categoryBreakdown = stats.categoryBreakdown.toMutableMap().apply {
                    val cat = challenge.category
                    this[cat] = (this[cat] ?: 0) + 1
                },
                difficultyBreakdown = stats.difficultyBreakdown.toMutableMap().apply {
                    val diff = challenge.difficulty
                    this[diff] = (this[diff] ?: 0) + 1
                }
            )
        }

        // Reload today's challenge with updated status
        loadTodayChallenge()

        return _todayChallenge.value!!
    }

    override suspend fun skipTodayChallenge() {
        val today = todayDayOfYear()
        val challenge = MicroChallengesDatabase.getChallengeForDay(today)

        // Mark as skipped (breaks streak)
        val completion = ChallengeCompletion(
            dayOfYear = today,
            challengeId = challenge.id,
            category = challenge.category.name,
            difficulty = challenge.difficulty.name,
            completed = false,
            skipped = true,
            completedAt = Clock.System.now().toString()
        )
        saveCompletion(completion)

        // Reset current streak
        updateStats { it.copy(currentStreak = 0) }

        loadTodayChallenge()
    }

    // ==================== CONTEXTUAL CHALLENGES ====================

    override suspend fun getContextualChallenge(
        preferIndoor: Boolean,
        preferredDifficulty: ChallengeDifficulty,
        isWeekend: Boolean
    ): MicroChallenge {
        return MicroChallengesDatabase.getContextualChallenge(
            isWeekend = isWeekend,
            preferIndoor = preferIndoor,
            preferredDifficulty = preferredDifficulty
        )
    }

    override fun getChallengesByCategory(category: ChallengeCategory): List<MicroChallenge> {
        return MicroChallengesDatabase.getChallengesByCategory(category)
    }

    override fun getQuickChallenges(): List<MicroChallenge> {
        return MicroChallengesDatabase.getQuickChallenges()
    }

    override fun searchChallenges(query: String): List<MicroChallenge> {
        return MicroChallengesDatabase.searchChallenges(query)
    }

    // ==================== HISTORY & STATS ====================

    override fun getCompletionHistory(days: Int): Flow<List<DailyMicroChallengeWithMeta>> {
        return flow {
            val today = todayDayOfYear()
            val history = mutableListOf<DailyMicroChallengeWithMeta>()

            for (i in 0 until days) {
                val dayOfYear = ((today - i - 1) % 365) + 1
                val completion = getCompletionForDay(dayOfYear)
                val challenge = MicroChallengesDatabase.getChallengeForDay(dayOfYear)

                if (completion != null) {
                    history.add(
                        DailyMicroChallengeWithMeta(
                            challenge = challenge,
                            dayOfYear = dayOfYear,
                            isCompleted = completion.completed,
                            completedAt = completion.completedAt
                        )
                    )
                }
            }

            emit(history)
        }
    }

    override fun getStats(): Flow<MicroChallengeStats> {
        return _stats.asStateFlow()
    }

    override suspend fun isTodayChallengeCompleted(): Boolean {
        val today = todayDayOfYear()
        return getCompletionForDay(today)?.completed ?: false
    }

    override suspend fun getChallengeStreak(): Int {
        return _stats.value.currentStreak
    }

    // ==================== SYNC ====================

    override suspend fun syncWithCloud() {
        val collection = challengeProgressCollection ?: return

        try {
            // Fetch from Firebase
            val docs = collection
                .orderBy("completedAt", dev.gitlive.firebase.firestore.Direction.DESCENDING)
                .limit(100)
                .get()
                .documents

            val completions = docs.mapNotNull { doc ->
                try {
                    ChallengeCompletion(
                        dayOfYear = doc.get<Int>("dayOfYear"),
                        challengeId = doc.get<String>("challengeId"),
                        category = doc.get<String>("category"),
                        difficulty = doc.get<String>("difficulty"),
                        completed = doc.get<Boolean>("completed"),
                        skipped = doc.get<Boolean?>("skipped") ?: false,
                        completedAt = doc.get<String>("completedAt")
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Merge with local data
            val localCompletions = getAllCompletions()
            val merged = (completions + localCompletions)
                .distinctBy { it.dayOfYear }
                .sortedByDescending { it.completedAt }

            // Save merged data locally
            saveAllCompletions(merged)

            // Recalculate stats
            recalculateStats(merged)

        } catch (e: Exception) {
            // Offline - use local data only
        }
    }

    // ==================== PRIVATE HELPERS ====================

    private suspend fun loadTodayChallenge() {
        val today = todayDayOfYear()
        val challenge = MicroChallengesDatabase.getChallengeForDay(today)
        val completion = getCompletionForDay(today)
        val streak = calculateStreak()

        _todayChallenge.value = DailyMicroChallengeWithMeta(
            challenge = challenge,
            dayOfYear = today,
            isCompleted = completion?.completed ?: false,
            completedAt = completion?.completedAt,
            challengeStreak = streak,
            categoryProgress = getCategoryProgress()
        )
    }

    private suspend fun loadStats() {
        val statsJson = dataStoreManager.getString(Keys.CHALLENGE_STATS).first()
        if (!statsJson.isNullOrEmpty()) {
            try {
                val saved = json.decodeFromString<SavedStats>(statsJson)
                _stats.value = MicroChallengeStats(
                    totalCompleted = saved.totalCompleted,
                    currentStreak = saved.currentStreak,
                    longestStreak = saved.longestStreak,
                    categoryBreakdown = saved.categoryBreakdown.mapKeys {
                        ChallengeCategory.valueOf(it.key)
                    },
                    difficultyBreakdown = saved.difficultyBreakdown.mapKeys {
                        ChallengeDifficulty.valueOf(it.key)
                    },
                    averageCompletionRate = saved.averageCompletionRate,
                    favoriteCategory = saved.favoriteCategory?.let { ChallengeCategory.valueOf(it) }
                )
            } catch (e: Exception) {
                // Use default stats
            }
        }
    }

    private suspend fun saveStats() {
        val stats = _stats.value
        val saved = SavedStats(
            totalCompleted = stats.totalCompleted,
            currentStreak = stats.currentStreak,
            longestStreak = stats.longestStreak,
            categoryBreakdown = stats.categoryBreakdown.mapKeys { it.key.name },
            difficultyBreakdown = stats.difficultyBreakdown.mapKeys { it.key.name },
            averageCompletionRate = stats.averageCompletionRate,
            favoriteCategory = stats.favoriteCategory?.name
        )
        dataStoreManager.putString(Keys.CHALLENGE_STATS, json.encodeToString(saved))
    }

    private suspend fun updateStats(update: (MicroChallengeStats) -> MicroChallengeStats) {
        _stats.value = update(_stats.value)
        saveStats()
    }

    private suspend fun getCompletionForDay(dayOfYear: Int): ChallengeCompletion? {
        val completions = getAllCompletions()
        return completions.find { it.dayOfYear == dayOfYear }
    }

    private suspend fun getAllCompletions(): List<ChallengeCompletion> {
        val json = dataStoreManager.getString(Keys.CHALLENGE_PROGRESS).first()
        return if (json.isNullOrEmpty()) emptyList()
        else {
            try {
                this.json.decodeFromString(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun saveCompletion(completion: ChallengeCompletion) {
        val completions = getAllCompletions().toMutableList()
        val index = completions.indexOfFirst { it.dayOfYear == completion.dayOfYear }
        if (index >= 0) {
            completions[index] = completion
        } else {
            completions.add(completion)
        }
        saveAllCompletions(completions)
    }

    private suspend fun saveAllCompletions(completions: List<ChallengeCompletion>) {
        dataStoreManager.putString(Keys.CHALLENGE_PROGRESS, json.encodeToString(completions))
    }

    private suspend fun saveChallengeToFirebase(completion: ChallengeCompletion) {
        val collection = challengeProgressCollection ?: return

        try {
            collection.document("day_${completion.dayOfYear}").set(
                hashMapOf(
                    "dayOfYear" to completion.dayOfYear,
                    "challengeId" to completion.challengeId,
                    "category" to completion.category,
                    "difficulty" to completion.difficulty,
                    "completed" to completion.completed,
                    "skipped" to completion.skipped,
                    "completedAt" to completion.completedAt
                )
            )
        } catch (e: Exception) {
            // Offline - local data is already saved
        }
    }

    private suspend fun calculateStreak(): Int {
        val completions = getAllCompletions()
            .filter { it.completed }
            .sortedByDescending { it.dayOfYear }

        if (completions.isEmpty()) return 0

        val today = todayDayOfYear()
        var streak = 0
        var expectedDay = today

        for (completion in completions) {
            if (completion.dayOfYear == expectedDay) {
                streak++
                expectedDay = if (expectedDay == 1) 365 else expectedDay - 1
            } else if (completion.dayOfYear == expectedDay - 1) {
                // Yesterday was completed, start counting
                streak++
                expectedDay = completion.dayOfYear - 1
            } else {
                break
            }
        }

        return streak
    }

    private suspend fun getCategoryProgress(): Map<ChallengeCategory, Int> {
        val completions = getAllCompletions().filter { it.completed }
        return completions.groupBy {
            ChallengeCategory.valueOf(it.category)
        }.mapValues { it.value.size }
    }

    private suspend fun recalculateStats(completions: List<ChallengeCompletion>) {
        val completed = completions.filter { it.completed }
        val categoryBreakdown = completed.groupBy {
            ChallengeCategory.valueOf(it.category)
        }.mapValues { it.value.size }

        val difficultyBreakdown = completed.groupBy {
            ChallengeDifficulty.valueOf(it.difficulty)
        }.mapValues { it.value.size }

        val favoriteCategory = categoryBreakdown.maxByOrNull { it.value }?.key

        val streak = calculateStreak()

        _stats.value = MicroChallengeStats(
            totalCompleted = completed.size,
            currentStreak = streak,
            longestStreak = maxOf(_stats.value.longestStreak, streak),
            categoryBreakdown = categoryBreakdown,
            difficultyBreakdown = difficultyBreakdown,
            averageCompletionRate = if (completions.isNotEmpty()) {
                completed.size.toFloat() / completions.size
            } else 0f,
            favoriteCategory = favoriteCategory
        )

        saveStats()
    }

    private fun todayDayOfYear(): Int {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.dayOfYear
    }
}

@Serializable
private data class ChallengeCompletion(
    val dayOfYear: Int,
    val challengeId: String,
    val category: String,
    val difficulty: String,
    val completed: Boolean,
    val skipped: Boolean = false,
    val completedAt: String
)

@Serializable
private data class SavedStats(
    val totalCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val categoryBreakdown: Map<String, Int> = emptyMap(),
    val difficultyBreakdown: Map<String, Int> = emptyMap(),
    val averageCompletionRate: Float = 0f,
    val favoriteCategory: String? = null
)
