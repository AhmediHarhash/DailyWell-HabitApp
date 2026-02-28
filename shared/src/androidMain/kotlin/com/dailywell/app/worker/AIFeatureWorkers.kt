package com.dailywell.app.worker

import android.content.Context
import androidx.work.*
import com.dailywell.app.data.local.AIFeaturePersistence
import com.dailywell.app.data.local.db.DailyWellDatabase
import com.dailywell.app.data.repository.AICoachingRepository
import com.dailywell.app.data.repository.AICoachingRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * WorkManager Workers for AI Feature Background Tasks
 *
 * These workers run periodically to:
 * 1. Generate Opus weekly reports (Sunday 2 AM)
 * 2. Prune old data (weekly cleanup)
 * 3. Check for due insights (daily)
 */

// ============================================================================
// 1. OPUS REPORT GENERATION WORKER (Sunday 2 AM)
// ============================================================================

class OpusReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val aiCoachingRepository: AICoachingRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Generate the weekly summary via the AI coaching repository
            // This uses the Claude API (or decision-tree fallback) to produce
            // real weekly reports based on user habit data
            aiCoachingRepository.generateWeeklySummary()

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "opus_report_generation"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            // Schedule for early morning (around 2 AM)
            val request = PeriodicWorkRequestBuilder<OpusReportWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .addTag("opus_report")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        private fun calculateInitialDelay(): Long {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                // Next Sunday at 2 AM
                while (get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
                set(java.util.Calendar.HOUR_OF_DAY, 2)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            val targetTime = calendar.timeInMillis
            return if (targetTime > now) targetTime - now else targetTime + 7 * 24 * 60 * 60 * 1000 - now
        }
    }
}

// ============================================================================
// 2. DATA CLEANUP WORKER (Weekly)
// ============================================================================

class DataCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = DailyWellDatabase.getInstance(applicationContext)
            val persistence = AIFeaturePersistence(
                abTestDao = database.abTestDao(),
                insightSchedulerDao = database.insightSchedulerDao(),
                contextCacheDao = database.contextCacheDao(),
                opusSchedulerDao = database.opusSchedulerDao(),
                userAISettingsDao = database.userAISettingsDao(),
                aiUsageDao = database.aiUsageDao(),
                aiInteractionDao = database.aiInteractionDao()
            )

            // Prune old data
            persistence.pruneOldData()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "data_cleanup"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DataCleanupWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .addTag("cleanup")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}

// ============================================================================
// 3. INSIGHT CHECK WORKER (Daily)
// ============================================================================

class InsightCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val aiCoachingRepository: AICoachingRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Generate the daily coaching insight via the AI coaching repository.
            // This uses the Claude API (or decision-tree fallback) to produce
            // real daily insights based on user habit data.
            aiCoachingRepository.generateDailyInsight()

            Result.success()
        } catch (e: Exception) {
            android.util.Log.w("InsightCheckWorker", "Failed to generate daily insight", e)
            Result.success() // Don't retry — insight generation is best-effort
        }
    }

    companion object {
        const val WORK_NAME = "insight_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<InsightCheckWorker>(1, TimeUnit.DAYS)
                .addTag("insights")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}

// ============================================================================
// 4. CONTEXT CACHE REFRESH WORKER (Every 4 hours)
// ============================================================================

class ContextCacheRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = DailyWellDatabase.getInstance(applicationContext)

            // Prune expired caches
            database.contextCacheDao().pruneExpiredCaches()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "context_cache_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ContextCacheRefreshWorker>(4, TimeUnit.HOURS)
                .addTag("cache")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}

// ============================================================================
// SCHEDULER - Call this on app startup
// ============================================================================

object AIFeatureWorkScheduler {
    /**
     * Schedule all AI feature background workers
     * Call this once on app startup (e.g., in Application.onCreate())
     */
    fun scheduleAll(context: Context) {
        OpusReportWorker.schedule(context)
        DataCleanupWorker.schedule(context)
        InsightCheckWorker.schedule(context)
        ContextCacheRefreshWorker.schedule(context)
    }

    /**
     * Cancel all AI feature workers (e.g., when user logs out)
     */
    fun cancelAll(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(OpusReportWorker.WORK_NAME)
        workManager.cancelUniqueWork(DataCleanupWorker.WORK_NAME)
        workManager.cancelUniqueWork(InsightCheckWorker.WORK_NAME)
        workManager.cancelUniqueWork(ContextCacheRefreshWorker.WORK_NAME)
    }
}
