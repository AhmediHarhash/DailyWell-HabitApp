package com.dailywell.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.dailywell.app.data.model.*
import com.dailywell.app.data.repository.ProactiveNotificationRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Main worker that checks triggers and sends proactive notifications
 * Runs periodically (every 30-60 minutes) to check for notification triggers
 */
class ProactiveNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val notificationRepository: ProactiveNotificationRepository by inject()

    companion object {
        const val WORK_NAME = "proactive_notifications_checker"
        const val CHANNEL_ID_PROACTIVE = "dailywell_proactive"
        const val CHANNEL_ID_URGENT = "dailywell_urgent"

        // Notification IDs by type
        private const val NOTIFICATION_ID_BASE = 2000
        fun getNotificationId(type: ProactiveNotificationType) = NOTIFICATION_ID_BASE + type.ordinal

        /**
         * Schedule the periodic notification checker
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            // Check every 30 minutes
            val workRequest = PeriodicWorkRequestBuilder<ProactiveNotificationWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)  // Start 5 min after app launch
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Cancel the periodic checker
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Create notification channels
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(NotificationManager::class.java)

                // Proactive channel (normal priority)
                val proactiveChannel = NotificationChannel(
                    CHANNEL_ID_PROACTIVE,
                    "Smart Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "AI-powered personalized reminders and insights"
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(proactiveChannel)

                // Urgent channel (high priority for streak protection)
                val urgentChannel = NotificationChannel(
                    CHANNEL_ID_URGENT,
                    "Streak Protection",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Urgent alerts when your streak is at risk"
                    enableVibration(true)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(urgentChannel)
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // Check all triggers
            val triggers = notificationRepository.checkAllTriggers()

            if (triggers.isEmpty()) {
                return Result.success()
            }

            // Get the highest priority trigger
            val topTrigger = triggers.maxByOrNull { it.priority.ordinal } ?: return Result.success()

            // Generate notification content
            val notification = notificationRepository.getNextScheduledNotification()
                ?: return Result.success()

            // Send the notification
            showNotification(notification)

            // Record that it was sent
            notificationRepository.recordNotificationSent(notification)

            Result.success()
        } catch (e: Exception) {
            // Don't retry on failure - wait for next scheduled check
            Result.success()
        }
    }

    private fun showNotification(notification: ProactiveNotification) {
        // Check permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Create intent for notification tap
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("notification_id", notification.id)
            putExtra("notification_type", notification.type.name)
            putExtra("deep_link", notification.deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Choose channel based on priority
        val channelId = when (notification.priority) {
            NotificationPriority.URGENT, NotificationPriority.HIGH -> CHANNEL_ID_URGENT
            else -> CHANNEL_ID_PROACTIVE
        }

        // Build notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        // Set priority for pre-Oreo
        builder.priority = when (notification.priority) {
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
        }

        // Add action buttons based on type
        addNotificationActions(builder, notification)

        // Show notification
        NotificationManagerCompat.from(context).notify(
            getNotificationId(notification.type),
            builder.build()
        )
    }

    private fun addNotificationActions(
        builder: NotificationCompat.Builder,
        notification: ProactiveNotification
    ) {
        when (notification.type) {
            ProactiveNotificationType.STREAK_AT_RISK -> {
                // Add "Check In Now" action
                val checkInIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    putExtra("action", "check_in")
                    putExtra("deep_link", "dailywell://today")
                }
                val checkInPending = PendingIntent.getActivity(
                    context,
                    100,
                    checkInIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_input_add,
                    "Check In Now",
                    checkInPending
                )
            }

            ProactiveNotificationType.COACH_OUTREACH -> {
                // Add "Chat" action
                val chatIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                    putExtra("action", "open_chat")
                    putExtra("deep_link", "dailywell://coaching")
                }
                val chatPending = PendingIntent.getActivity(
                    context,
                    101,
                    chatIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_dialog_info,
                    "Chat",
                    chatPending
                )
            }

            else -> {
                // Generic "Open" action for other types
            }
        }
    }
}

/**
 * One-time worker for specific scheduled notifications
 */
class ScheduledNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val notificationRepository: ProactiveNotificationRepository by inject()

    companion object {
        const val KEY_NOTIFICATION_TYPE = "notification_type"

        /**
         * Schedule a specific notification type at a specific time
         */
        fun scheduleNotification(
            context: Context,
            type: ProactiveNotificationType,
            delayMinutes: Long
        ) {
            val workRequest = OneTimeWorkRequestBuilder<ScheduledNotificationWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(
                    workDataOf(KEY_NOTIFICATION_TYPE to type.name)
                )
                .addTag("scheduled_notification_${type.name}")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        /**
         * Cancel a scheduled notification
         */
        fun cancelScheduled(context: Context, type: ProactiveNotificationType) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag("scheduled_notification_${type.name}")
        }
    }

    override suspend fun doWork(): Result {
        val typeName = inputData.getString(KEY_NOTIFICATION_TYPE) ?: return Result.failure()
        val type = try {
            ProactiveNotificationType.valueOf(typeName)
        } catch (e: Exception) {
            return Result.failure()
        }

        // Check if we should still send this notification
        if (!notificationRepository.shouldSendNotification(type)) {
            return Result.success()
        }

        // Generate and send notification
        val notification = notificationRepository.getNextScheduledNotification()
            ?: return Result.success()

        // Show the notification using shared display logic
        showScheduledNotification(notification)
        notificationRepository.recordNotificationSent(notification)

        return Result.success()
    }

    private fun showScheduledNotification(notification: ProactiveNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("notification_id", notification.id)
            putExtra("notification_type", notification.type.name)
            putExtra("deep_link", notification.deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.type.ordinal + 300,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (notification.priority) {
            NotificationPriority.URGENT, NotificationPriority.HIGH ->
                ProactiveNotificationWorker.CHANNEL_ID_URGENT
            else -> ProactiveNotificationWorker.CHANNEL_ID_PROACTIVE
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        NotificationManagerCompat.from(context).notify(
            ProactiveNotificationWorker.getNotificationId(notification.type),
            builder.build()
        )
    }
}

/**
 * Worker that runs at strategic times (morning, midday, evening)
 */
class TimeBasedNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val notificationRepository: ProactiveNotificationRepository by inject()

    companion object {
        const val WORK_NAME_MORNING = "morning_notification"
        const val WORK_NAME_MIDDAY = "midday_notification"
        const val WORK_NAME_EVENING = "evening_notification"

        const val KEY_TIME_SLOT = "time_slot"

        /**
         * Schedule all time-based notifications
         */
        fun scheduleAllTimeSlots(context: Context) {
            scheduleMorning(context)
            scheduleMidday(context)
            scheduleEvening(context)
        }

        fun scheduleMorning(context: Context, hour: Int = 8, minute: Int = 0) {
            scheduleAtTime(context, WORK_NAME_MORNING, "morning", hour, minute)
        }

        fun scheduleMidday(context: Context, hour: Int = 13, minute: Int = 0) {
            scheduleAtTime(context, WORK_NAME_MIDDAY, "midday", hour, minute)
        }

        fun scheduleEvening(context: Context, hour: Int = 19, minute: Int = 0) {
            scheduleAtTime(context, WORK_NAME_EVENING, "evening", hour, minute)
        }

        private fun scheduleAtTime(
            context: Context,
            workName: String,
            timeSlot: String,
            hour: Int,
            minute: Int
        ) {
            val currentTime = java.util.Calendar.getInstance()

            // ±30 min timing randomization — prevents pattern detection by the user,
            // creates variable reward timing (behavioral science)
            val jitterMinutes = Random.nextInt(-30, 31) // -30 to +30
            val adjustedMinute = minute + jitterMinutes
            val adjustedHour = hour + (adjustedMinute / 60) + (if (adjustedMinute < 0) -1 else 0)
            val finalMinute = ((adjustedMinute % 60) + 60) % 60
            val finalHour = ((adjustedHour % 24) + 24) % 24

            val targetTime = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, finalHour)
                set(java.util.Calendar.MINUTE, finalMinute)
                set(java.util.Calendar.SECOND, 0)
            }

            // If time already passed today, schedule for tomorrow
            if (targetTime.before(currentTime)) {
                targetTime.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }

            val initialDelay = targetTime.timeInMillis - currentTime.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<TimeBasedNotificationWorker>(
                24, TimeUnit.HOURS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_TIME_SLOT to timeSlot))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        /**
         * Cancel all time-based notifications
         */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_MORNING)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_MIDDAY)
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_EVENING)
        }
    }

    override suspend fun doWork(): Result {
        val timeSlot = inputData.getString(KEY_TIME_SLOT) ?: return Result.failure()

        val notificationType = when (timeSlot) {
            "morning" -> ProactiveNotificationType.MORNING_MOTIVATION
            "midday" -> ProactiveNotificationType.MIDDAY_CHECKIN
            "evening" -> ProactiveNotificationType.EVENING_REMINDER
            else -> return Result.failure()
        }

        // Check if we should send this notification
        if (!notificationRepository.shouldSendNotification(notificationType)) {
            return Result.success()
        }

        // Generate and show the notification
        return try {
            val notification = notificationRepository.getNextScheduledNotification()
                ?: return Result.success()
            showTimeBasedNotification(notification)
            notificationRepository.recordNotificationSent(notification)
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private fun showTimeBasedNotification(notification: ProactiveNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("notification_id", notification.id)
            putExtra("notification_type", notification.type.name)
            putExtra("deep_link", notification.deepLink)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.type.ordinal + 200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (notification.priority) {
            NotificationPriority.URGENT, NotificationPriority.HIGH ->
                ProactiveNotificationWorker.CHANNEL_ID_URGENT
            else -> ProactiveNotificationWorker.CHANNEL_ID_PROACTIVE
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        NotificationManagerCompat.from(context).notify(
            ProactiveNotificationWorker.getNotificationId(notification.type),
            builder.build()
        )
    }
}

/**
 * Manager class to coordinate all notification workers
 */
object ProactiveNotificationManager {

    /**
     * Initialize all proactive notification workers
     * Call this when app starts or when user enables notifications
     */
    fun initialize(context: Context) {
        // Create notification channels
        ProactiveNotificationWorker.createNotificationChannels(context)

        // Schedule the main periodic checker
        ProactiveNotificationWorker.schedule(context)

        // Schedule time-based notifications
        TimeBasedNotificationWorker.scheduleAllTimeSlots(context)
    }

    /**
     * Disable all proactive notifications
     */
    fun disable(context: Context) {
        ProactiveNotificationWorker.cancel(context)
        TimeBasedNotificationWorker.cancelAll(context)
    }

    /**
     * Update notification timing based on user preferences
     */
    fun updateTiming(
        context: Context,
        morningHour: Int,
        middayHour: Int,
        eveningHour: Int
    ) {
        TimeBasedNotificationWorker.scheduleMorning(context, morningHour)
        TimeBasedNotificationWorker.scheduleMidday(context, middayHour)
        TimeBasedNotificationWorker.scheduleEvening(context, eveningHour)
    }

    /**
     * Schedule an immediate notification check
     * Use this after significant events (habit completed, streak milestone, etc.)
     */
    fun checkNow(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ProactiveNotificationWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Schedule streak-at-risk check for a specific time
     * Call this in the evening if user hasn't completed all habits
     */
    fun scheduleStreakRiskCheck(context: Context, hoursUntilMidnight: Int) {
        if (hoursUntilMidnight <= 0) return

        // Schedule checks at strategic intervals
        val checkTimes = when {
            hoursUntilMidnight > 4 -> listOf(hoursUntilMidnight - 3, hoursUntilMidnight - 1)
            hoursUntilMidnight > 2 -> listOf(hoursUntilMidnight - 1)
            else -> listOf(0)  // Check now
        }

        checkTimes.forEach { hoursDelay ->
            val workRequest = OneTimeWorkRequestBuilder<ProactiveNotificationWorker>()
                .setInitialDelay(hoursDelay.toLong(), TimeUnit.HOURS)
                .addTag("streak_risk_check")
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
