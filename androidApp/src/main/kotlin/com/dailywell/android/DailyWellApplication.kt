package com.dailywell.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.dailywell.app.di.appModule
import com.dailywell.app.notification.CalendarNotificationWorker
import com.dailywell.app.notification.ProactiveNotificationManager
import com.dailywell.app.worker.AIFeatureWorkScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

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
