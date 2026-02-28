package com.dailywell.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.dailywell.app.ai.SLMService
import com.dailywell.app.di.appModule
import com.dailywell.app.notification.CalendarNotificationWorker
import com.dailywell.app.notification.ProactiveNotificationManager
import com.dailywell.app.worker.AIFeatureWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin

class DailyWellApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidLogger()
            androidContext(this@DailyWellApplication)
            modules(appModule)
        }

        // Create notification channels (legacy + proactive)
        createNotificationChannel()

        // Initialize Proactive AI Notification System
        initializeProactiveNotifications()

        // Schedule AI Feature background workers (Opus reports, data cleanup, insight checks)
        AIFeatureWorkScheduler.scheduleAll(this)

        // Schedule periodic calendar sync (free-slot notifications, habit scheduling)
        CalendarNotificationWorker.schedulePeriodicSync(this)

        // Initialize on-device SLM (Qwen2.5 0.5B) in background
        // Non-blocking — app works fine with pattern fallback while model loads
        initializeSLM()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun initializeSLM() {
        applicationScope.launch {
            try {
                val slmService: SLMService = getKoin().get()
                val initialized = slmService.initialize()
                if (initialized) {
                    Log.d("DailyWellApp", "SLM (Qwen2.5 0.5B) initialized successfully")
                } else {
                    Log.d("DailyWellApp", "SLM model not available — using pattern fallback")
                }
            } catch (e: Exception) {
                Log.w("DailyWellApp", "SLM initialization failed (pattern fallback active)", e)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to check in on your daily habits"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun initializeProactiveNotifications() {
        // Initialize the proactive notification system
        // Creates notification channels and schedules workers
        ProactiveNotificationManager.initialize(this)
    }

    companion object {
        const val CHANNEL_ID = "dailywell_reminders"
    }
}
