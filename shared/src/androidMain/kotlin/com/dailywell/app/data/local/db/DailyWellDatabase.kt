package com.dailywell.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        // Core entities
        HabitEntity::class,
        EntryEntity::class,
        AchievementEntity::class,
        // AI Feature entities (v2)
        ABTestEventEntity::class,
        ScheduledInsightEntity::class,
        ContextCacheEntity::class,
        DailyContextSummaryEntity::class,
        ScheduledReportEntity::class,
        UserAISettingsEntity::class,
        AIUsageEntity::class,
        AIInteractionEntity::class
    ],
    version = 2,  // Incremented for new AI feature tables
    exportSchema = false
)
abstract class DailyWellDatabase : RoomDatabase() {
    // Core DAOs
    abstract fun habitDao(): HabitDao
    abstract fun entryDao(): EntryDao
    abstract fun achievementDao(): AchievementDao

    // AI Feature DAOs (v2)
    abstract fun abTestDao(): ABTestDao
    abstract fun insightSchedulerDao(): InsightSchedulerDao
    abstract fun contextCacheDao(): ContextCacheDao
    abstract fun opusSchedulerDao(): OpusSchedulerDao
    abstract fun userAISettingsDao(): UserAISettingsDao
    abstract fun aiUsageDao(): AIUsageDao
    abstract fun aiInteractionDao(): AIInteractionDao

    companion object {
        @Volatile
        private var INSTANCE: DailyWellDatabase? = null

        /** Migration from v1 (core tables) to v2 (+ AI feature tables) */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ab_test_events (
                        eventId TEXT NOT NULL PRIMARY KEY,
                        timestamp INTEGER NOT NULL,
                        userId TEXT NOT NULL,
                        intent TEXT NOT NULL,
                        requestedModel TEXT NOT NULL,
                        actualModel TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        budgetMode TEXT NOT NULL,
                        inputTokens INTEGER NOT NULL,
                        outputTokens INTEGER NOT NULL,
                        cost REAL NOT NULL,
                        responseTimeMs INTEGER NOT NULL,
                        userFeedback TEXT,
                        feedbackTimestamp INTEGER
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scheduled_insights (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uniqueKey TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        milestone TEXT NOT NULL,
                        scheduledTimestamp INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        generatedContent TEXT,
                        generatedTimestamp INTEGER,
                        deliveredTimestamp INTEGER
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS context_cache (
                        userId TEXT NOT NULL PRIMARY KEY,
                        cachedTimestamp INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        avgCompletionRate REAL NOT NULL,
                        dominantMood TEXT,
                        sleepAvgHours REAL,
                        sleepAvgQuality REAL,
                        nutritionAvgScore REAL,
                        workoutTotalMinutes INTEGER,
                        streakDays INTEGER NOT NULL,
                        topHabits TEXT NOT NULL,
                        missedHabits TEXT NOT NULL,
                        condensedPrompt TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS scheduled_reports (
                        reportId TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        reportType TEXT NOT NULL,
                        scheduledTime INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        generatedContent TEXT,
                        tokensCost INTEGER,
                        costUsd REAL,
                        generatedAt INTEGER,
                        deliveredAt INTEGER,
                        expiresAt INTEGER
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_ai_settings (
                        userId TEXT NOT NULL PRIMARY KEY,
                        preferredLanguage TEXT NOT NULL DEFAULT 'en',
                        slmEnabled INTEGER NOT NULL DEFAULT 1,
                        lastLanguageDetected TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_context_summaries (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        date TEXT NOT NULL,
                        habitCompletionRate REAL NOT NULL,
                        completedHabits TEXT NOT NULL,
                        missedHabits TEXT NOT NULL,
                        mood TEXT,
                        sleepHours REAL,
                        sleepQuality REAL,
                        nutritionScore REAL,
                        workoutMinutes INTEGER,
                        energyLevel INTEGER,
                        notes TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_usage_tracking (
                        usageId TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        month TEXT NOT NULL,
                        planType TEXT NOT NULL,
                        tokensUsed INTEGER NOT NULL DEFAULT 0,
                        messagesCount INTEGER NOT NULL DEFAULT 0,
                        freeMessagesCount INTEGER NOT NULL DEFAULT 0,
                        slmMessagesCount INTEGER NOT NULL DEFAULT 0,
                        aiMessagesCount INTEGER NOT NULL DEFAULT 0,
                        currentMonthCostUsd REAL NOT NULL DEFAULT 0,
                        resetDate TEXT NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ai_interactions (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        inputTokens INTEGER NOT NULL,
                        outputTokens INTEGER NOT NULL,
                        totalTokens INTEGER NOT NULL,
                        modelUsed TEXT NOT NULL,
                        responseCategory TEXT NOT NULL,
                        durationMs INTEGER,
                        estimatedCostUsd REAL NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): DailyWellDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DailyWellDatabase::class.java,
                    "dailywell_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
